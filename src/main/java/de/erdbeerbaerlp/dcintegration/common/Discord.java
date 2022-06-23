package de.erdbeerbaerlp.dcintegration.common;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import de.erdbeerbaerlp.dcintegration.common.addon.AddonLoader;
import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.common.storage.*;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.FieldHelper;
import de.erdbeerbaerlp.dcintegration.common.util.ServerInterface;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.internal.requests.Requester;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.configFile;
import static de.erdbeerbaerlp.dcintegration.common.util.Variables.messagesFile;

public class Discord extends Thread {

    /**
     * Cache file for players which ignore discord chat
     */
    private static final File IGNORED_PLAYERS = new File(Variables.discordDataDir, ".PlayerIgnores");
    /**
     * Dummy UUID for unknown players or server messages
     */
    public static final UUID dummyUUID = new UUID(0L, 0L);
    /**
     * Instance of the ServerInterface
     */
    public final ServerInterface srv;
    /**
     * Holds messages recently forwarded to discord in format MessageID,Sender UUID
     */
    private final HashMap<String, UUID> recentMessages = new HashMap<>(150);

    /**
     * ArrayList with players which ignore the discord chat
     */
    public final ArrayList<UUID> ignoringPlayers = new ArrayList<>();
    /**
     * Pending /discord link requests
     */
    public final HashMap<Integer, KeyValue<Instant, UUID>> pendingLinks = new HashMap<>();
    /**
     * Pending /discord link requests for floodgate users
     */
    public final HashMap<Integer, KeyValue<Instant, UUID>> pendingBedrockLinks = new HashMap<>();
    final ArrayList<DiscordEventHandler> eventHandlers = new ArrayList<>();
    /**
     * Pending messages from command sender
     */
    private final HashMap<String, ArrayList<String>> messages = new HashMap<>();

    private final HashMap<String, Webhook> webhookHashMap = new HashMap<>();
    private final HashMap<String, WebhookClient> webhookClis = new HashMap<>();
    /**
     * Current JDA instance
     */
    private JDA jda = null;
    private Thread messageSender, statusUpdater;
    private DiscordEventListener listener;

    public Discord(@Nonnull ServerInterface srv) {
        this.srv = srv;
        setDaemon(true);
        setName("Discord Integration - Launch Thread");
        if (!Configuration.instance().advanced.baseAPIUrl.equals("https://discord.com"))
            try {
                Field field = Requester.class.getDeclaredField("DISCORD_API_PREFIX");
                field.setAccessible(true);
                FieldHelper.makeNonFinal(field);
                field.set(null, Configuration.instance().advanced.baseAPIUrl);
                Variables.LOGGER.info("Now using " + Configuration.instance().advanced.baseAPIUrl + " as target Discord!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        start();
    }

    /**
     * Checks if a Player can join (also checking roles)
     * @param uuid Player UUID
     * @return true if the player can join<br>
     * Also returns true if whitelist mode is off
     */
    public boolean canPlayerJoin(UUID uuid) {
        if (!Configuration.instance().linking.whitelistMode) return true;
        if (PlayerLinkController.isPlayerLinked(uuid)) {
            if (Configuration.instance().linking.requiredRoles.length != 0) {
                final User usr = getJDA().getUserById(PlayerLinkController.getDiscordFromPlayer(uuid));
                if (usr == null) return false;
                final Guild g = getChannel().getGuild();
                final Member mem = g.retrieveMember(usr).complete();
                if (mem == null) return false;
                for (String requiredRole : Configuration.instance().linking.requiredRoles) {
                    final Role role = g.getRoleById(requiredRole);
                    if (role == null) continue;
                    if (mem.getRoles().contains(role)) {
                        return true;
                    }
                }
                return false;
            } else return true;
        }
        return false;
    }

    /**
     * Registers an event handler
     *
     * @param handler Event handler to register
     */
    public void registerEventHandler(@Nonnull final DiscordEventHandler handler) {
        if (!eventHandlers.contains(handler))
            eventHandlers.add(handler);
    }

    /**
     * Unregisters an event handler
     *
     * @param handler Event handler to unregister
     */
    public void unregisterEventHandler(@Nonnull final DiscordEventHandler handler) {
        eventHandlers.remove(handler);
    }

    /**
     * Unregisters ALL events handlers from this {@link Discord} instance
     */
    private void unregisterAllEventHandlers() {
        eventHandlers.clear();
    }


    /**
     * Saves an new Message into the recent messages list
     *
     * @param msgID Message ID
     * @param uuid  Sender UUID
     */
    public void addRecentMessage(@Nonnull String msgID, @Nonnull UUID uuid) {
        if (recentMessages.size() + 1 >= 150) {
            do {
                recentMessages.remove(recentMessages.keySet().toArray(new String[0])[0]);
            } while (recentMessages.size() + 1 >= 150);
        }
        recentMessages.put(msgID, uuid);
    }

    /**
     * Gets the sender's {@link UUID} from an recently sent message
     *
     * @param messageID Message ID to get the {@link UUID} from
     * @return The sender's {@link UUID}, or {@linkplain Discord#dummyUUID}
     */
    @Nonnull
    public UUID getSenderUUIDFromMessageID(@Nonnull String messageID) {
        return recentMessages.getOrDefault(messageID, dummyUUID);
    }


    @Override
    public void run() {
        while (true) {

            final JDABuilder b = JDABuilder.createDefault(Configuration.instance().general.botToken);
            b.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_EMOJIS);
            b.setAutoReconnect(true);
            b.setEnableShutdownHook(true);
            try {
                jda = b.build();
                jda.awaitReady();
                break;
            } catch (LoginException e) {
                if (e.getMessage().equals("The provided token is invalid!")) {
                    Variables.LOGGER.error("Invalid token, please set correct token in the config file!");
                    return;
                }
                Variables.LOGGER.error("Login failed, retrying");
                try {
                    sleep(6000);
                } catch (InterruptedException ignored) {
                    return;
                }
            } catch (InterruptedException | IllegalStateException e) {
                return;
            }
        }
        if (getChannel() == null) {
            Variables.LOGGER.error("ERROR! Channel ID of the default bot channel not valid!");
            kill(true);
            return;
        }
        if (!PermissionUtil.checkPermission(getChannel(), getChannel().getGuild().retrieveMember(jda.getSelfUser()).complete(), Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)) {
            Variables.LOGGER.error("ERROR! Bot does not have all permissions to work!");
            kill(true);
            throw new PermissionException("Bot requires message read, message write, embed links and manage messages");
        }
        if (Configuration.instance().webhook.enable)
            if (!PermissionUtil.checkPermission(getChannel(), getChannel().getGuild().retrieveMember(jda.getSelfUser()).complete(), Permission.MANAGE_WEBHOOKS)) {
                Variables.LOGGER.error("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
                Configuration.instance().webhook.enable = false;
                try {
                    Configuration.instance().saveConfig();
                } catch (IOException e) {
                    Variables.LOGGER.error("FAILED TO SAVE CONFIG");
                    e.printStackTrace();
                }
            }

        Variables.LOGGER.info("Bot Ready");
        jda.addEventListener(listener = new DiscordEventListener());
        try {
            loadIgnoreList();
        } catch (IOException e) {
            Variables.LOGGER.error("Error while loading the ignoring players list!");
            e.printStackTrace();
        }

        //Cache all users and (nick-)names
        Variables.LOGGER.info("Caching members...");
        jda.getGuilds().forEach((g) -> g.loadMembers().onSuccess((m) -> Variables.LOGGER.info("All " + m.size() + " members cached for Guild " + g.getName())).onError((t) -> {
            Variables.LOGGER.error("Encountered an error while caching members:");
            t.printStackTrace();
        }));

        final Thread t = new Thread(() -> {
            Variables.LOGGER.info("Loading DiscordIntegration addons...");
            AddonLoader.loadAddons(this);
            Variables.LOGGER.info("Addon loading complete!");
        });
        t.setName("Discord Integration - Addon-Loader");
        t.setDaemon(true);
        t.start();

        final Thread unlink = new Thread(() -> {
            for (PlayerLink p : PlayerLinkController.getAllLinks()) {
                try {
                    getChannel().getGuild().retrieveMemberById(p.discordID).submit();
                } catch (ErrorResponseException e) {
                    PlayerLinkController.unlinkPlayer(p.discordID);
                }
            }
        });
        unlink.setName("Discord Integration Link Check");
        unlink.setDaemon(true);
        if (Configuration.instance().linking.unlinkOnLeave) unlink.start();

    }

    /**
     * Returns the corresponding {@link WebhookClient} for the given Channel ID
     *
     * @param channel Channel ID
     * @return Webhook Client for the Channel ID
     */
    @Nullable
    public WebhookClient getWebhookCli(@Nonnull String channel) {
        return webhookClis.computeIfAbsent(channel, (id) -> WebhookClient.withUrl(getWebhook(getChannel(id)).getUrl()));
    }

    /**
     * Kills the discord bot
     */
    public void kill(boolean instant) {
        Variables.LOGGER.info("Unloading addons...");
        AddonLoader.unloadAddons(this);
        Variables.LOGGER.info("Unloaded addons");
        if (jda != null) {
            Variables.LOGGER.info("Unloading instance: " + jda);
            if (listener != null) {
                Variables.LOGGER.info("Unloading listener: " + listener);
                jda.removeEventListener(listener);
            }
            stopThreads();
            unregisterAllEventHandlers();
            webhookClis.forEach((i, w) -> w.close());
            try {
                if (instant) jda.shutdownNow();
                else jda.shutdown();
            } catch (LinkageError ignored) { //Fix exception logged when reloading
            }
            jda = null;
            Variables.discord_instance = null;
        }
    }

    /**
     * Kills the discord bot
     */
    public void kill() {
        kill(true);
    }

    private final HashMap<String, TextChannel> channelCache = new HashMap<>();

    /**
     * @return the specified text channel
     */
    @Nullable
    public TextChannel getChannel() {
        return getChannel("default");
    }

    /**
     * @return the specified text channel (supports "default" to return the default server channel)
     */
    @Nullable
    public TextChannel getChannel(@Nonnull String id) {
        if (jda == null) return null;
        TextChannel channel;
        final boolean deflt = id.equals("default") || id.equals(Configuration.instance().general.botChannel);
        if (deflt) id = Configuration.instance().general.botChannel;
        if (id.isEmpty()) {
            Variables.LOGGER.error("Cannot get channel from empty ID! Check your config!");
            if (deflt) return null;
            Variables.LOGGER.info("Falling back to default channel!");
            return getChannel();
        }
        channel = channelCache.computeIfAbsent(id, (id2) -> jda.getTextChannelById(id2));
        if (channel == null) {
            Variables.LOGGER.error("Failed to get channel with ID '" + id + "', falling back to default channel");
            channel = channelCache.computeIfAbsent(Configuration.instance().general.botChannel, jda::getTextChannelById);
        }
        return channel;
    }


    /**
     * Loads the last known players who ignored discord messages from file
     *
     * @throws IOException
     */
    public void loadIgnoreList() throws IOException {
        if (IGNORED_PLAYERS.exists()) {
            BufferedReader r = new BufferedReader(new FileReader(IGNORED_PLAYERS));
            r.lines().iterator().forEachRemaining((s) -> {
                try {
                    ignoringPlayers.add(UUID.fromString(s));
                } catch (IllegalArgumentException e) {
                    Variables.LOGGER.error("Found invalid entry for ignoring player, skipping");
                }
            });
            r.close();
        }
    }

    /**
     * Starts all sub-threads
     */
    public void startThreads() {
        final Thread t = new Thread(() -> {
            try {
                CommandRegistry.updateSlashCommands();
            } catch (Exception e) {
                e.printStackTrace();
                Variables.LOGGER.error("Failed to register slash commands! Please re-invite the bot to all servers the bot is on using this link: " + jda.getInviteUrl(Permission.getPermissions(2953964624L)).replace("scope=", "scope=applications.commands%20"));
            }
        });
        t.setDaemon(true);
        t.start();
        if (statusUpdater == null) statusUpdater = new StatusUpdateThread();
        if (messageSender == null) messageSender = new MessageQueueThread();
        if (!messageSender.isAlive()) messageSender.start();
        if (!statusUpdater.isAlive()) statusUpdater.start();
    }

    /**
     * Stops all sub-threads
     */
    public void stopThreads() {
        if (messageSender != null && messageSender.isAlive()) messageSender.interrupt();
        if (statusUpdater != null && statusUpdater.isAlive()) statusUpdater.interrupt();
        if (isAlive()) interrupt();
    }

    /**
     * @return Current JDA instance, if it exists
     */
    @Nullable
    public JDA getJDA() {
        return jda;
    }

    /**
     * Adds messages to send in the next half second
     * Used by config commands
     *
     * @param msg       message
     * @param channelID the channel ID the message should get sent to
     */
    public void sendMessageFuture(@Nonnull String msg, @Nonnull String channelID) {
        if (msg.isEmpty() || channelID.isEmpty()) return;
        final ArrayList<String> msgs;
        if (messages.containsKey(channelID))
            msgs = messages.get(channelID);
        else
            msgs = new ArrayList<>();
        msgs.add(msg);
        messages.put(channelID, msgs);
    }

    /**
     * Sends a message embed as player
     *
     * @param playerName Player Name
     * @param embed      Discord embed
     * @param channel    Target channel
     * @param uuid       Player UUID
     */
    public void sendMessage(@Nonnull final String playerName, @Nonnull String uuid, @Nonnull MessageEmbed embed, @Nonnull MessageChannel channel) {
        sendMessage(playerName, uuid, new DiscordMessage(embed), channel);
    }

    /**
     * Sends a message as server
     *
     * @param msg Message
     */
    public void sendMessage(@Nonnull String msg) {
        sendMessage(Configuration.instance().webhook.serverName, "0000000", msg, getChannel(Configuration.instance().advanced.serverChannelID));
    }

    /**
     * Sends a message embed as player
     *
     * @param playerName Player Name
     * @param msg        Message
     * @param channel    Target channel
     * @param uuid       Player UUID
     */
    public void sendMessage(@Nonnull final String playerName, @Nonnull String uuid, @Nonnull String msg, MessageChannel channel) {
        sendMessage(playerName, uuid, new DiscordMessage(msg), channel);
    }

    /**
     * Sends an generic message to discord with custom avatar url (when using a webhook)
     *
     * @param channel   target channel
     * @param message   message
     * @param avatarURL URL of the avatar image for the webhook
     * @param name      Webhook name
     */
    public void sendMessage(MessageChannel channel, @Nonnull String message, @Nonnull String avatarURL, @Nonnull String name) {
        sendMessage(name, new DiscordMessage(message), avatarURL, channel, false);
    }

    /**
     * Sends a CHAT message to discord with custom avatar url (when using a webhook)
     *
     * @param msg       Message
     * @param avatarURL URL of the avatar image
     * @param name      Name of the fake player
     */
    public void sendMessage(@Nonnull String msg, @Nonnull String avatarURL, @Nonnull String name) {
        sendMessage(name, new DiscordMessage(msg), avatarURL, getChannel(Configuration.instance().advanced.serverChannelID), true);
    }

    /**
     * Sends a CHAT message to discord with custom avatar url (when using a webhook)
     *
     * @param name      Name of the fake player
     * @param msg       Message
     * @param channel   Channel to send message into
     * @param avatarURL URL of the avatar image
     */
    public void sendMessage(@Nonnull String name, @Nonnull String msg, MessageChannel channel, @Nonnull String avatarURL) {
        sendMessage(name, new DiscordMessage(msg), avatarURL, channel, true);
    }

    /**
     * Sends an discord message
     *
     * @param name          Player name or Webhook user name
     * @param message       Message to send
     * @param avatarURL     Avatar URL for the webhook
     * @param channel       Target channel
     * @param isChatMessage true to send it as chat message (when not using webhook)
     */
    public void sendMessage(@Nonnull String name, @Nonnull DiscordMessage message, @Nonnull String avatarURL, MessageChannel channel, boolean isChatMessage) {
        sendMessage(name, message, avatarURL, channel, isChatMessage, dummyUUID.toString());
    }

    /**
     * Loads all configuration files
     *
     * @throws IOException
     */
    public static void loadConfigs() throws IOException {
        // === Migration ===

        if (configFile.exists()) {

            if (!messagesFile.exists()) messagesFile.createNewFile();

            // Migrate localization to new file
            final Toml toml = new Toml().read(configFile).getTable("localization");

            if (toml != null) {
                final Localization localization = toml.to(Localization.class);
                Variables.LOGGER.info("Starting Translation migration");
                final TomlWriter w = new TomlWriter.Builder()
                        .indentValuesBy(2)
                        .indentTablesBy(4)
                        .padArrayDelimitersBy(2)
                        .build();
                w.write(localization, messagesFile);
                Variables.LOGGER.info("Translation migration complete");

            }
        }

        // === Load everything ===

        Localization.instance().loadConfig();
        Configuration.instance().loadConfig();
    }

    /**
     * Sends an discord message
     *
     * @param name          Player name or Webhook user name
     * @param message       Message to send
     * @param avatarURL     Avatar URL for the webhook
     * @param channel       Target channel
     * @param isChatMessage true to send it as chat message (when not using webhook)
     * @param uuid          UUID of the player (required for in-game pinging)
     */
    public void sendMessage(@Nonnull String name, @Nonnull DiscordMessage message, @Nonnull String avatarURL, MessageChannel channel, boolean isChatMessage, @Nonnull String uuid) {
        if (jda == null || channel == null) return;
        final Thread t = new Thread(() -> {
            try {
                if (Configuration.instance().webhook.enable) {
                    final ArrayList<WebhookMessageBuilder> messages = message.buildWebhookMessages();
                    messages.forEach((builder) -> {
                        builder.setUsername(name);
                        builder.setAvatarUrl(avatarURL);
                        getWebhookCli(channel.getId()).send(builder.build()).thenAccept((a) -> {
                            addRecentMessage(a.getId() + "", UUID.fromString(uuid));
                        });
                    });
                } else if (isChatMessage) {
                    message.setMessage(Localization.instance().discordChatMessage.replace("%player%", name).replace("%msg%", message.getMessage()));
                    for (Message m : message.buildMessages())
                        channel.sendMessage(m).submit().thenAccept((a) -> addRecentMessage(a.getId(), UUID.fromString(uuid)));
                } else {
                    for (Message m : message.buildMessages())
                        channel.sendMessage(m).submit().thenAccept((a) -> addRecentMessage(a.getId(), UUID.fromString(uuid)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.setName("Discord Integration - SendMessage");
        t.start();
    }

    /**
     * @return an instance of the webhook or null
     */
    @Nullable
    public Webhook getWebhook(final TextChannel c) {
        if (!Configuration.instance().webhook.enable || c == null) return null;
        return webhookHashMap.computeIfAbsent(c.getId(), cid -> {
            if (!PermissionUtil.checkPermission(c, c.getGuild().getMember(jda.getSelfUser()), Permission.MANAGE_WEBHOOKS)) {
                Variables.LOGGER.info("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
                Configuration.instance().webhook.enable = false;
                try {
                    Configuration.instance().saveConfig();
                } catch (IOException e) {
                    Variables.LOGGER.error("FAILED TO SAVE CONFIGURATION");
                    e.printStackTrace();
                }
                return null;
            }
            for (final Webhook web : c.retrieveWebhooks().complete()) {
                if (web.getName().equals(Configuration.instance().webhook.webhookName)) {
                    return web;
                }
            }
            return c.createWebhook(Configuration.instance().webhook.webhookName).complete();
        });
    }

    /**
     * Sends a message when *not* using a webhook and returns it as RequestFuture<Message> or null when using a webhook<br>
     * only used by starting message
     *
     * @param msg message
     * @return Sent message
     */
    @Nullable
    public CompletableFuture<Message> sendMessageReturns(@Nonnull String msg, TextChannel c) {
        if (Configuration.instance().webhook.enable || msg.isEmpty() || c == null) return null;
        else return c.sendMessage(msg).submit();
    }


    /**
     * Sends a message to discord
     *
     * @param msg         the message to send
     * @param textChannel the channel where the message should arrive
     */
    public void sendMessage(@Nonnull String msg, MessageChannel textChannel) {
        sendMessage(new DiscordMessage(msg), textChannel);
    }

    /**
     * Sends a message to discord
     *
     * @param msg     the message to send
     * @param channel the channel where the message should arrive
     */
    public void sendMessage(@Nonnull DiscordMessage msg, MessageChannel channel) {
        sendMessage(Configuration.instance().webhook.serverName, "0000000", msg, channel);
    }

    /**
     * Restarts the discord bot (used by reload command)
     */
    public boolean restart() {
        try {
            if (Variables.discord_instance.isAlive()) Variables.discord_instance.interrupt();
            kill();
            Variables.discord_instance = new Discord(srv);
            CommandRegistry.reRegisterAllCommands();
            CommandRegistry.registerConfigCommands();
            Variables.discord_instance.startThreads();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets a list of all personal settings and their descriptions
     *
     * @return HashMap with the setting keys as key and the setting descriptions as value
     */
    @Nonnull
    public HashMap<String, String> getSettings() {
        final HashMap<String, String> out = new HashMap<>();
        final Field[] fields = PlayerSettings.class.getFields();
        final Field[] descFields = PlayerSettings.Descriptions.class.getDeclaredFields();
        for (Field f : fields) {
            out.put(f.getName(), "No Description Provided");
        }
        for (Field f : descFields) {
            f.setAccessible(true);
            try {
                out.put(f.getName(), (String) f.get(new PlayerSettings.Descriptions()));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return out;
    }


    /**
     * Sends a message to discord
     *
     * @param playerName the name of the player
     * @param uuid       the player uuid
     * @param msg        the message to send
     */
    @SuppressWarnings("ConstantConditions")
    public void sendMessage(@Nonnull String playerName, @Nonnull String uuid, @Nonnull DiscordMessage msg, MessageChannel channel) {
        if (channel == null) return;
        final boolean isServerMessage = playerName.equals(Configuration.instance().webhook.serverName) && uuid.equals("0000000");
        final UUID uUUID = uuid.equals("0000000") ? null : UUID.fromString(uuid);
        String avatarURL = "";
        if (!isServerMessage && uUUID != null) {
            if (PlayerLinkController.isPlayerLinked(uUUID)) {
                final PlayerSettings s = PlayerLinkController.getSettings(null, uUUID);
                final Member dc = getChannel().getGuild().retrieveMemberById(PlayerLinkController.getDiscordFromPlayer(uUUID)).complete();
                if (dc != null)
                    if (s.useDiscordNameInChannel) {
                        playerName = dc.getEffectiveName();
                        avatarURL = dc.getUser().getAvatarUrl();
                    }
            }
            if (avatarURL != null && avatarURL.isEmpty())
                avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", uUUID.toString()).replace("%uuid_dashless%", uUUID.toString().replace("-", "")).replace("%name%", playerName).replace("%randomUUID%", UUID.randomUUID().toString());
        }
        if (isServerMessage) {
            avatarURL = Configuration.instance().webhook.serverAvatarURL;
        }
        sendMessage(playerName, msg, avatarURL, channel, !isServerMessage, uuid);
    }

    /**
     * Toggles an player's ignore status
     *
     * @param uuid Player's UUID
     * @return new ignore status
     */
    public boolean togglePlayerIgnore(@Nonnull UUID uuid) {
        if (PlayerLinkController.isPlayerLinked(uuid)) {
            final PlayerSettings settings = PlayerLinkController.getSettings(null, uuid);
            settings.ignoreDiscordChatIngame = !settings.ignoreDiscordChatIngame;
            PlayerLinkController.updatePlayerSettings(null, uuid, settings);
            return !settings.ignoreDiscordChatIngame;
        } else {
            if (ignoringPlayers.contains(uuid)) {
                ignoringPlayers.remove(uuid);
                try {
                    saveIgnoreList();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            } else {
                ignoringPlayers.add(uuid);
                return false;
            }
        }
    }

    /**
     * Generates or gets an unique link number for an player
     *
     * @param uniqueID The player's {@link UUID} to generate the number for
     * @return Link number for this player
     */
    public int genLinkNumber(@Nonnull UUID uniqueID) {
        final AtomicInteger r = new AtomicInteger(-1);
        pendingLinks.forEach((k, v) -> {
            if (v.getValue().equals(uniqueID))
                r.set(k);
        });
        if (r.get() != -1) return r.get();
        do {
            r.set(new Random().nextInt(99999));
        } while (pendingLinks.containsKey(r.get()));
        pendingLinks.put(r.get(), new DefaultKeyValue<>(Instant.now(), uniqueID));
        return r.get();
    }

    /**
     * Generates or gets an unique link number for an player
     *
     * @param uniqueID The player's {@link UUID} to generate the number for
     * @return Link number for this player
     */
    public int genBedrockLinkNumber(@Nonnull UUID uniqueID) {
        final AtomicInteger r = new AtomicInteger(-1);
        pendingBedrockLinks.forEach((k, v) -> {
            if (v.getValue().equals(uniqueID))
                r.set(k);
        });
        if (r.get() != -1) return r.get();
        do {
            r.set(new Random().nextInt(99999));
        } while (pendingBedrockLinks.containsKey(r.get()));
        pendingBedrockLinks.put(r.get(), new DefaultKeyValue<>(Instant.now(), uniqueID));
        return r.get();
    }

    /**
     * Saves the ignore list for unlinked players
     *
     * @throws IOException
     */
    private void saveIgnoreList() throws IOException {
        if (!IGNORED_PLAYERS.exists() && !ignoringPlayers.isEmpty()) IGNORED_PLAYERS.createNewFile();
        if (!IGNORED_PLAYERS.exists() && ignoringPlayers.isEmpty()) {
            IGNORED_PLAYERS.delete();
            return;
        }
        FileWriter w = new FileWriter(IGNORED_PLAYERS);
        w.write("");
        for (UUID a : ignoringPlayers) {
            if (!PlayerLinkController.isPlayerLinked(a))
                w.append(a.toString()).append("\n");
        }
        w.close();
    }

    /**
     * Checks if the list of {@link Role}s contains an admin role
     *
     * @param roles List to check for admin roles
     * @return true, if the given {@link Role} list has an admin role
     */
    public boolean hasAdminRole(@Nonnull List<Role> roles) {
        final AtomicBoolean ret = new AtomicBoolean(false);
        roles.forEach((r) -> {
            for (String id : Configuration.instance().commands.adminRoleIDs) {
                if (id.equals(r.getId())) ret.set(true);
            }
        });
        return ret.get();
    }

    /**
     * Calls an event and returns true, if one of the handler returned true
     *
     * @param func function to run on every event handler
     * @return if an handler returned true
     */
    public boolean callEvent(@Nonnull Function<DiscordEventHandler, Boolean> func) {
        for (DiscordEventHandler h : eventHandlers) {
            if (func.apply(h)) return true;
        }
        return false;
    }

    /**
     * Calls an event and does not return any value
     *
     * @param consumer function to run on every event handler
     */
    public void callEventC(@Nonnull Consumer<DiscordEventHandler> consumer) {
        for (DiscordEventHandler h : eventHandlers) {
            consumer.accept(h);
        }
    }


    private class MessageQueueThread extends Thread {
        MessageQueueThread() {
            setName("Discord Integration - Message Queue");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                if (!messages.isEmpty()) {
                    messages.forEach((channel, msgs) -> {
                        StringBuilder s = new StringBuilder();
                        for (final String msg : msgs)
                            s.append(msg).append("\n");
                        Discord.this.sendMessage(s.toString().trim(), getChannel(channel));
                    });
                    messages.clear();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }

        }
    }

    private class StatusUpdateThread extends Thread {
        StatusUpdateThread() {
            setName("Discord Integration - Status updater and link cleanup");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                if (jda != null) {
                    final String game = Configuration.instance().general.botStatusName
                            .replace("%online%", "" + srv.getOnlinePlayers())
                            .replace("%max%", "" + srv.getMaxPlayers());
                    switch (Configuration.instance().general.botStatusType) {
                        case DISABLED:
                            break;
                        case LISTENING:
                            jda.getPresence().setActivity(Activity.listening(game));
                            break;
                        case PLAYING:
                            jda.getPresence().setActivity(Activity.playing(game));
                            break;
                        case WATCHING:
                            jda.getPresence().setActivity(Activity.watching(game));
                            break;
                        case COMPETING:
                            jda.getPresence().setActivity(Activity.competing(game));
                            break;
                        case STREAMING:
                            jda.getPresence().setActivity(Activity.streaming(game, Configuration.instance().general.streamingURL)); //URL is required to show up as "Streaming"
                            break;
                    }
                }
                // Removing of expired numbers
                final ArrayList<Integer> remove = new ArrayList<>();
                pendingLinks.forEach((k, v) -> {
                    final Instant now = Instant.now();
                    Duration d = Duration.between(v.getKey(), now);
                    if (d.toMinutes() > 10) remove.add(k);
                });
                for (int i : remove)
                    pendingLinks.remove(i);
                remove.clear();
                pendingBedrockLinks.forEach((k, v) -> {
                    final Instant now = Instant.now();
                    Duration d = Duration.between(v.getKey(), now);
                    if (d.toMinutes() > 10) remove.add(k);
                });
                for (int i : remove)
                    pendingBedrockLinks.remove(i);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }

            }
        }
    }

}
