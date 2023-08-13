package de.erdbeerbaerlp.dcintegration.common;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.erdbeerbaerlp.dcintegration.common.addon.AddonLoader;
import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.McCommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.database.DBInterface;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.database.JSONInterface;
import de.erdbeerbaerlp.dcintegration.common.threads.MessageQueueThread;
import de.erdbeerbaerlp.dcintegration.common.threads.StatusUpdateThread;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.McServerInterface;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializerOptions;
import dev.vankka.mcdiscordreserializer.rules.DiscordMarkdownRules;
import dev.vankka.simpleast.core.node.Node;
import dev.vankka.simpleast.core.node.TextNode;
import dev.vankka.simpleast.core.parser.ParseSpec;
import dev.vankka.simpleast.core.parser.Parser;
import dev.vankka.simpleast.core.parser.Rule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestConfig;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordIntegration {


    /**
     * DiscordIntegration instance. Initially null
     */
    public static DiscordIntegration INSTANCE;
    /**
     * Mod/Plugin version
     */
    public static final String VERSION = "3.0.0";

    /**
     * Discord Integration data directory
     */
    public static final File discordDataDir = new File("./DiscordIntegration-Data/");

    /**
     * Cache file for players which ignore discord chat
     */
    private static final File IGNORED_PLAYERS = new File(discordDataDir, ".PlayerIgnores");
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * ArrayList with players which ignore the discord chat
     */
    public final ArrayList<UUID> ignoringPlayers = new ArrayList<>();
    /**
     * The primary {@link Logger} instance
     */
    public static final Logger LOGGER = LogManager.getLogger("Discord Integration");

    @SuppressWarnings("rawtypes")
    public static final MinecraftSerializerOptions mcSerializerOptions;
    /**
     * API Version of the mod. Gets changed on every plugin breaking update
     */
    public static final int apiVersion = 3;

    static {
        List<Rule<Object, Node<Object>, Object>> rules = new ArrayList<>(DiscordMarkdownRules.createAllRulesForDiscord(false));
        rules.add(new Rule<>(Pattern.compile("(.*)")) {
            @Override
            public ParseSpec<Object, Node<Object>, Object> parse(Matcher matcher, Parser<Object, Node<Object>, Object> parser, Object state) {
                return ParseSpec.createTerminal(new TextNode<>(matcher.group()), state);
            }
        });
        mcSerializerOptions = MinecraftSerializerOptions.defaults().withRules(rules);
    }


    /**
     * Path to the config file
     */
    public static File configFile = new File("./config/Discord-Integration.toml");
    /**
     * Path to the message configuration
     */
    public static File messagesFile = new File("./DiscordIntegration-Data/Messages.toml");

    /**
     * Message sent when the server is starting (in non-webhook mode!), stored for editing
     */
    public static CompletableFuture<Message> startingMsg;


    /**
     * Dummy UUID for unknown players or server messages
     */
    public static final UUID dummyUUID = new UUID(0L, 0L);
    /**
     * Time in milliseconds when the server started
     */
    public static long started = -1;


    /**
     * Current JDA instance
     */
    private JDA jda = null;
    /**
     * Cache of channels so that they don't need to be fetched every single time
     */
    private final HashMap<String, GuildMessageChannel> channelCache = new HashMap<>();
    /**
     * Cache of members so that they don't need to be fetched every single time
     */
    static final Map<Long, Member> memberCache = new HashMap<>();
    /**
     * Instance of the default event listener
     */
    private DiscordEventListener listener;

    /**
     * Holds messages recently forwarded to discord in format MessageID, Sender UUID
     */
    final HashMap<String, UUID> recentMessages = new HashMap<>(150);

    final ArrayList<DiscordEventHandler> eventHandlers = new ArrayList<>();


    /**
     * Instance of the Database Interface for the linking database
     */
    private DBInterface linkDbInterface;
    /**
     * Instance of the Minecraft Server Interface
     */
    private final McServerInterface serverInterface;

    private Thread messageSender, statusUpdater, launchThread;


    public DiscordIntegration(final McServerInterface serverInterface) {
        this.serverInterface = serverInterface;
        try {
            loadConfigs();
        } catch (IOException e) {
            LOGGER.error("Error loading config");
            e.printStackTrace();
            return;
        }
        launchThread = new LaunchThread();
        launchThread.start();
    }

    /**
     * Registers an event handler
     *
     * @param handler Event handler to register
     */
    public void registerEventHandler(final DiscordEventHandler handler) {
        if (!eventHandlers.contains(handler))
            eventHandlers.add(handler);
    }

    /**
     * Unregisters an event handler
     *
     * @param handler Event handler to unregister
     */
    public void unregisterEventHandler(final DiscordEventHandler handler) {
        eventHandlers.remove(handler);
    }

    /**
     * Unregisters ALL events handlers from this {@link DiscordIntegration} instance
     */
    private void unregisterAllEventHandlers() {
        eventHandlers.clear();
    }


    /**
     * Calls an event and returns true, if one of the handler returned true
     *
     * @param func function to run on every event handler
     * @return if a handler returned true
     */
    public boolean callEvent(Function<DiscordEventHandler, Boolean> func) {
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
    public void callEventC(Consumer<DiscordEventHandler> consumer) {
        for (DiscordEventHandler h : eventHandlers) {
            consumer.accept(h);
        }
    }

    /**
     * Loads all configuration files
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void loadConfigs() throws IOException {
        if (!discordDataDir.exists()) {
            discordDataDir.mkdirs();
        }
        Configuration.instance().loadConfig();
        if (!Configuration.instance().messages.language.equals("local")) {
            final File backupFile = new File(messagesFile, ".bak");
            if (backupFile.exists()) backupFile.delete();
            try {
                final URL langURL = new URL("https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Integration-Translations/main/" + Configuration.instance().messages.language + ".toml");
                final HttpsURLConnection urlConnection = (HttpsURLConnection) langURL.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                if (urlConnection.getResponseCode() == 200) {
                    messagesFile.renameTo(backupFile);
                    try (final InputStream in = urlConnection.getInputStream()) {
                        Files.copy(in, messagesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (final IOException ex) {
                if (backupFile.exists())
                    backupFile.renameTo(messagesFile);
            }
        }
        Localization.instance().loadConfig();

        if (StringUtils.containsIgnoreCase(Configuration.instance().webhook.webhookName, "discord")) {
            StringUtils.replaceIgnoreCase(Configuration.instance().webhook.webhookName, "discord", "dc");
            LOGGER.info("Fixed webhook name containing the word \"Discord\".");
            Configuration.instance().saveConfig();
        }
    }

    /**
     * Starts all sub-threads
     */
    public void startThreads() {
        if (Configuration.instance().commands.enabled) {
            WorkThread.executeJob(() -> {
                try {
                    CommandRegistry.updateSlashCommands();
                } catch (IllegalStateException e) {
                    LOGGER.error(e);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error("Failed to register slash commands! Please re-invite the bot to all servers the bot is on using this link: " + jda.getInviteUrl(Permission.getPermissions(2953964624L)).replace("scope=", "scope=applications.commands%20"));
                }
            });
        }
        if (statusUpdater == null) statusUpdater = new StatusUpdateThread(this);
        if (messageSender == null) messageSender = new MessageQueueThread(this);
        if (!messageSender.isAlive()) messageSender.start();
        if (!statusUpdater.isAlive()) statusUpdater.start();

        if (JSONInterface.jsonFile.exists() && !Configuration.instance().linking.databaseClass.equals(JSONInterface.class.getCanonicalName())) {
            LOGGER.info("PlayerLinks.json found, but using custom database implementation");
            LOGGER.info("If you want to use the old data, please enter \"discord migrate\" into the server console");
        }
    }

    /**
     * Stops all sub-threads
     */
    public void stopThreads() {
        if (messageSender != null && messageSender.isAlive()) messageSender.interrupt();
        if (statusUpdater != null && statusUpdater.isAlive()) statusUpdater.interrupt();
        if (launchThread.isAlive()) launchThread.interrupt();
    }


    /**
     * Kills this DiscordIntegration instance (and the corresponding bot)
     */
    public void kill(final boolean instant) {
        LOGGER.info("Unloading addons...");
        AddonLoader.unloadAddons(this);
        LOGGER.info("Unloaded addons");
        if (jda != null) {
            LOGGER.info("Unloading instance: " + jda);
            if (listener != null) {
                LOGGER.info("Unloading listener: " + listener);
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
            INSTANCE = null;
        }
    }

    /**
     * @return the specified text channel
     */

    public GuildMessageChannel getChannel() {
        return getChannel("default");
    }

    /**
     * Fetches a text channel from discord
     *
     * @return the specified text channel (supports "default" to return the default server channel)
     */

    private GuildMessageChannel retrieveChannel(final String id2) {
        final StandardGuildMessageChannel chan = jda.getTextChannelById(id2);
        if (chan == null) {
            for (final Guild g : jda.getGuilds()) {
                for (final GuildChannel gChannel : g.getChannels(true)) {
                    if (gChannel == null) continue;
                    if (gChannel.getId().equals(id2)) {
                        if (gChannel instanceof StandardGuildMessageChannel)
                            return (StandardGuildMessageChannel) gChannel;
                        else
                            LOGGER.error("Target Channel ID is not a valid message channel!");
                    } else {
                        if (gChannel instanceof StandardGuildMessageChannel)
                            for (final ThreadChannel c : ((StandardGuildMessageChannel) gChannel).getThreadChannels()) {
                                if (c.getId().equals(id2)) {
                                    return c;
                                }
                            }
                    }
                }
            }
        }
        return chan;
    }

    /**
     * Retrieves a channel from cache or, if not available, fetches the channel from discord (saving it to cache)
     *
     * @return the specified text channel (supports "default" to return the default server channel)
     */
    public GuildMessageChannel getChannel(String id) {
        if (jda == null) return null;
        GuildMessageChannel channel;
        final boolean isDefault = id.equals("default") || id.equals(Configuration.instance().general.botChannel);
        if (isDefault) id = Configuration.instance().general.botChannel;
        if (id.isEmpty()) {
            LOGGER.error("Cannot get channel from empty ID! Check your config!");
            if (isDefault) return null;
            LOGGER.info("Falling back to default channel!");
            return getChannel();
        }
        channel = channelCache.computeIfAbsent(id, this::retrieveChannel);
        if (channel == null) {
            LOGGER.error("Failed to get Channel with ID '" + id + "', falling back to default channel");
            channel = channelCache.computeIfAbsent(Configuration.instance().general.botChannel, this::retrieveChannel);
        }
        return channel;
    }

    /**
     * Get member by ID from cache or from discord, saving the member to cache
     *
     * @param userid ID of the member
     * @return Fetched member, or null
     */
    public Member getMemberById(final String userid) {
        return getMemberById(Long.parseLong(userid));
    }

    /**
     * Get member by ID from cache or from discord, saving the member to cache
     *
     * @param userid ID of the member
     * @return Fetched member, or null
     */
    public Member getMemberById(final Long userid) {
        if (memberCache.containsKey(userid)) return memberCache.get(userid);
        else {
            final Member out = getChannel().getGuild().retrieveMember(UserSnowflake.fromId(userid)).complete();
            memberCache.put(userid, out);
            return out;
        }
    }

    public JDA getJDA() {
        return jda;
    }

    public McServerInterface getServerInterface() {
        return serverInterface;
    }

    public DBInterface getDatabaseInterface() {
        return linkDbInterface;
    }

    /**
     * Gets the sender's {@link UUID} from a recently sent message
     *
     * @param messageID Message ID to get the {@link UUID} from
     * @return The sender's {@link UUID}, or {@linkplain DiscordIntegration#dummyUUID}
     */

    public UUID getSenderUUIDFromMessageID(String messageID) {
        return recentMessages.getOrDefault(messageID, dummyUUID);
    }

    public boolean hasAdminRole(List<Role> roles) {
        for (final Role role : roles) {
            if (ArrayUtils.contains(Configuration.instance().commands.adminRoleIDs, role.getId())) return true;
        }
        return false;
    }

    /**
     * Thread used to start the discord bot
     */
    public class LaunchThread extends Thread {
        private final JDABuilder b;

        public LaunchThread() {
            setDaemon(true);
            setName("DiscordIntegration Launch Thread");
            b = JDABuilder.createDefault(Configuration.instance().general.botToken);
            if (!Configuration.instance().advanced.baseAPIUrl.equals("https://discord.com"))
                try {
                    b.setRestConfig(new RestConfig().setBaseUrl(Configuration.instance().advanced.baseAPIUrl));
                    LOGGER.info("Now using " + Configuration.instance().advanced.baseAPIUrl + " as target Discord!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }

        @Override
        public void run() {
            while (true) {
                b.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.MESSAGE_CONTENT);
                b.setAutoReconnect(true);
                b.setEnableShutdownHook(true);
                try {
                    jda = b.build();
                    jda.awaitReady();
                    break;
                } catch (InvalidTokenException e) {
                    if (e.getMessage().equals("The provided token is invalid!")) {
                        LOGGER.error("Invalid token, please set correct token in the config file!");
                        return;
                    }
                    LOGGER.error("Login failed, retrying");
                    try {
                        //noinspection BusyWait
                        sleep(6000);
                    } catch (InterruptedException ignored) {
                        return;
                    }
                } catch (InterruptedException | IllegalStateException e) {
                    return;
                }
            }
            if (getChannel() == null) {
                LOGGER.error("ERROR! Channel ID of the default bot channel not valid!");
                kill(true);
                return;
            }
            if (!PermissionUtil.checkPermission(getChannel().getPermissionContainer(), getMemberById(jda.getSelfUser().getIdLong()), Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)) {
                LOGGER.error("ERROR! Bot does not have all permissions to work!");
                kill(true);
                throw new PermissionException("Bot requires message read, message write, embed links and manage messages");
            }
            if (Configuration.instance().webhook.enable)
                if (!PermissionUtil.checkPermission(getChannel().getPermissionContainer(), getMemberById(jda.getSelfUser().getIdLong()), Permission.MANAGE_WEBHOOKS)) {
                    LOGGER.error("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
                    Configuration.instance().webhook.enable = false;
                    try {
                        Configuration.instance().saveConfig();
                    } catch (IOException e) {
                        LOGGER.error("FAILED TO SAVE CONFIG");
                        e.printStackTrace();
                    }
                }

            LOGGER.info("Bot ready");
            jda.addEventListener(listener = new DiscordEventListener());
            try {
                loadIgnoreList();
            } catch (IOException e) {
                LOGGER.error("Error while loading the ignoring players list!");
                e.printStackTrace();
            }
            McCommandRegistry.registerDefaultCommands();
            LOGGER.info("Loading DiscordIntegration Addons...");
            AddonLoader.loadAddons(DiscordIntegration.this);
            LOGGER.info("Addon loading complete!");

            if (Configuration.instance().linking.enableLinking) {
                LOGGER.info("Loading Linking Database...");
                final boolean sqLite = Configuration.instance().linking.databaseClass.equals("de.erdbeerbaerlp.dcintegration.common.storage.linking.database.SQLiteInterface");
                try {
                    linkDbInterface = (DBInterface) Class.forName(Configuration.instance().linking.databaseClass, true, AddonLoader.getAddonClassLoader()).getDeclaredConstructor().newInstance();
                    linkDbInterface.connect();
                    linkDbInterface.initialize();
                    LinkManager.load();
                    LOGGER.info("Linking Database loaded");
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException |
                         ClassNotFoundException e) {
                    if (sqLite) {
                        Configuration.instance().linking.databaseClass = "de.erdbeerbaerlp.dcintegration.common.storage.linking.database.JSONInterface";
                        linkDbInterface = new JSONInterface();
                        linkDbInterface.connect();
                        linkDbInterface.initialize();
                    } else
                        e.printStackTrace();
                }


                if (Configuration.instance().linking.unlinkOnLeave)
                    WorkThread.executeJob(() -> {
                        for (final PlayerLink p : LinkManager.getAllLinks()) {
                            try {
                                getChannel().getGuild().retrieveMemberById(p.discordID).submit();
                            } catch (ErrorResponseException e) {
                                LinkManager.unlinkPlayer(p.discordID);
                            }
                        }
                    });
            }
        }

    }

    /**
     * Loads the last known players who ignored discord messages from file
     */
    public void loadIgnoreList() throws IOException {
        if (IGNORED_PLAYERS.exists()) {
            BufferedReader r = new BufferedReader(new FileReader(IGNORED_PLAYERS));
            r.lines().iterator().forEachRemaining((s) -> {
                try {
                    ignoringPlayers.add(UUID.fromString(s));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Found invalid entry for ignoring player, skipping");
                }
            });
            r.close();
        }
    }

    /**
     * Toggles a player's ignore status
     *
     * @param uuid Player's UUID
     * @return new ignore status
     */
    public boolean togglePlayerIgnore(UUID uuid) {
        if (LinkManager.isPlayerLinked(uuid)) {
            final PlayerLink link = LinkManager.getLink(null, uuid);
            link.settings.ignoreDiscordChatIngame = !link.settings.ignoreDiscordChatIngame;
            LinkManager.addLink(link);
            return !link.settings.ignoreDiscordChatIngame;
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
     * Saves the ignore-list for unlinked players
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveIgnoreList() throws IOException {
        if (!IGNORED_PLAYERS.exists() && !ignoringPlayers.isEmpty()) IGNORED_PLAYERS.createNewFile();
        if (!IGNORED_PLAYERS.exists() && ignoringPlayers.isEmpty()) {
            IGNORED_PLAYERS.delete();
            return;
        }
        FileWriter w = new FileWriter(IGNORED_PLAYERS);
        w.write("");
        for (UUID a : ignoringPlayers) {
            if (!LinkManager.isPlayerLinked(a))
                w.append(a.toString()).append("\n");
        }
        w.close();
    }


    private final HashMap<String, Webhook> webhookHashMap = new HashMap<>();

    /**
     * @return an instance of the webhook or null
     */

    public Webhook getWebhook(final GuildMessageChannel ic) {
        if (!Configuration.instance().webhook.enable || ic == null) return null;
        if (ic instanceof ThreadChannel c) {
            return webhookHashMap.computeIfAbsent(c.getId(), cid -> {
                if (!PermissionUtil.checkPermission(c.getParentChannel(), getMemberById(jda.getSelfUser().getIdLong()), Permission.MANAGE_WEBHOOKS)) {
                    LOGGER.info("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
                    Configuration.instance().webhook.enable = false;
                    try {
                        Configuration.instance().saveConfig();
                    } catch (IOException e) {
                        LOGGER.error("FAILED TO SAVE CONFIGURATION");
                        e.printStackTrace();
                    }
                    return null;
                }
                for (final Webhook web : c.getParentMessageChannel().asStandardGuildMessageChannel().retrieveWebhooks().complete()) {
                    if (web.getName().equals(Configuration.instance().webhook.webhookName)) {
                        return web;
                    }
                }

                return c.getParentMessageChannel().asStandardGuildMessageChannel().createWebhook(Configuration.instance().webhook.webhookName).complete();
            });
        } else if (ic instanceof StandardGuildMessageChannel c) {
            return webhookHashMap.computeIfAbsent(c.getId(), cid -> {
                if (!PermissionUtil.checkPermission(c, getMemberById(jda.getSelfUser().getIdLong()), Permission.MANAGE_WEBHOOKS)) {
                    LOGGER.info("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
                    Configuration.instance().webhook.enable = false;
                    try {
                        Configuration.instance().saveConfig();
                    } catch (IOException e) {
                        LOGGER.error("FAILED TO SAVE CONFIGURATION");
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
        return null;
    }

    private final HashMap<String, JDAWebhookClient> webhookClis = new HashMap<>();

    /**
     * Returns the corresponding {@link WebhookClient} for the given Channel ID
     *
     * @param channelID Channel ID
     * @return Webhook Client for the Channel ID
     */

    public JDAWebhookClient getWebhookCli(String channelID) {
        return webhookClis.computeIfAbsent(channelID, (id) -> {
            final GuildMessageChannel channel = getChannel(id);
            final Webhook wh = getWebhook(channel);
            if (wh == null) return null;
            JDAWebhookClient cli = JDAWebhookClient.from(wh);
            if (channel instanceof ThreadChannel c) {
                cli = cli.onThread(c.getIdLong());
            }
            return cli;
        });
    }


    /**
     * Adds messages to send in the next half second
     * Used by config commands
     *
     * @param msg       message
     * @param channelID the channel ID the message should get sent to
     */
    public void sendMessageFuture(String msg, String channelID) {
        if (msg.isEmpty() || channelID.isEmpty()) return;
        final ArrayList<String> msgs;
        if (MessageQueueThread.messages.containsKey(channelID))
            msgs = MessageQueueThread.messages.get(channelID);
        else
            msgs = new ArrayList<>();
        msgs.add(msg);
        MessageQueueThread.messages.put(channelID, msgs);
    }

    /**
     * Sends a message embed as player
     *
     * @param playerName Player Name
     * @param embed      Discord embed
     * @param channel    Target channel
     * @param uuid       Player UUID
     */
    public void sendMessage(final String playerName, String uuid, MessageEmbed embed, MessageChannel channel) {
        sendMessage(playerName, uuid, new DiscordMessage(embed), channel);
    }

    /**
     * Sends a message as server
     *
     * @param msg Message
     */
    public void sendMessage(String msg) {
        sendMessage(Configuration.instance().webhook.serverName, "0000000", msg, getChannel(Configuration.instance().advanced.serverChannelID));
    }

    /**
     * Sends a message as server
     *
     * @param msg Message
     */
    public void sendMessage(DiscordMessage msg) {
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
    public void sendMessage(final String playerName, String uuid, String msg, MessageChannel channel) {
        sendMessage(playerName, uuid, new DiscordMessage(msg), channel);
    }

    /**
     * Sends a generic message to discord with custom avatar url (when using a webhook)
     *
     * @param channel   target channel
     * @param message   message
     * @param avatarURL URL of the avatar image for the webhook
     * @param name      Webhook name
     */
    public void sendMessage(MessageChannel channel, String message, String avatarURL, String name) {
        sendMessage(name, new DiscordMessage(message), avatarURL, channel, false);
    }

    /**
     * Sends a CHAT message to discord with custom avatar url (when using a webhook)
     *
     * @param msg       Message
     * @param avatarURL URL of the avatar image
     * @param name      Name of the fake player
     */
    public void sendMessage(String msg, String avatarURL, String name) {
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
    public void sendMessage(String name, String msg, MessageChannel channel, String avatarURL) {
        sendMessage(name, new DiscordMessage(msg), avatarURL, channel, true);
    }

    /**
     * Sends an discord message
     *
     * @param name          Player name or Webhook username
     * @param message       Message to send
     * @param avatarURL     Avatar URL for the webhook
     * @param channel       Target channel
     * @param isChatMessage true to send it as chat message (when not using webhook)
     */
    public void sendMessage(String name, DiscordMessage message, String avatarURL, MessageChannel channel, boolean isChatMessage) {
        sendMessage(name, message, avatarURL, channel, isChatMessage, dummyUUID.toString());
    }

    /**
     * Sends an discord message
     *
     * @param name          Player name or Webhook username
     * @param message       Message to send
     * @param avatarURL     Avatar URL for the webhook
     * @param channel       Target channel
     * @param isChatMessage true to send it as chat message (when not using webhook)
     * @param uuid          UUID of the player (required for in-game pinging)
     */
    public void sendMessage(String name, DiscordMessage message, String avatarURL, MessageChannel channel, boolean isChatMessage, String uuid) {
        if (jda == null || channel == null) return;
        WorkThread.executeJob(() -> {
            try {
                if (Configuration.instance().webhook.enable) {
                    if (isChatMessage) message.setIsChatMessage();
                    final ArrayList<WebhookMessageBuilder> messages = message.buildWebhookMessages();
                    messages.forEach((builder) -> {
                        builder.setUsername(name);
                        builder.setAvatarUrl(avatarURL);
                        final JDAWebhookClient webhookCli = getWebhookCli(channel.getId());
                        if (webhookCli != null)
                            webhookCli.send(builder.build()).thenAccept((a) -> rememberRecentMessage(String.valueOf(a.getId()), UUID.fromString(uuid)));
                    });
                } else if (isChatMessage) {
                    message.setMessage(Localization.instance().discordChatMessage.replace("%player%", name).replace("%msg%", message.getMessage()));
                    message.setIsChatMessage();
                    channel.sendMessage(message.buildMessages()).submit().thenAccept((a) -> rememberRecentMessage(a.getId(), UUID.fromString(uuid)));
                } else {
                    channel.sendMessage(message.buildMessages()).submit().thenAccept((a) -> rememberRecentMessage(a.getId(), UUID.fromString(uuid)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Sends a message when *not* using a webhook and returns it as RequestFuture<Message> or null when using a webhook<br>
     * only used by starting message
     *
     * @param msg message
     * @return Sent message
     */

    public CompletableFuture<Message> sendMessageReturns(MessageCreateData msg, GuildMessageChannel c) {
        if (Configuration.instance().webhook.enable || c == null) return null;
        else return c.sendMessage(msg).submit();
    }

    /**
     * Checks if a Player can join (also checking roles)
     *
     * @param uuid Player UUID
     * @return true if the player can join<br>
     * Also returns true if whitelist mode is off
     */
    public boolean canPlayerJoin(UUID uuid) {
        if (!Configuration.instance().linking.whitelistMode) return true;
        if (LinkManager.isPlayerLinked(uuid)) {
            if (Configuration.instance().linking.requiredRoles.length != 0) {
                final Member mem = getMemberById(LinkManager.getLink(null, uuid).discordID);
                if (mem == null) return false;
                final Guild g = getChannel().getGuild();
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
     * Sends a message to discord
     *
     * @param msg         the message to send
     * @param textChannel the channel where the message should arrive
     */
    public void sendMessage(String msg, MessageChannel textChannel) {
        sendMessage(new DiscordMessage(msg), textChannel);
    }

    /**
     * Sends a message to discord
     *
     * @param msg     the message to send
     * @param channel the channel where the message should arrive
     */
    public void sendMessage(DiscordMessage msg, MessageChannel channel) {
        sendMessage(Configuration.instance().webhook.serverName, "0000000", msg, channel);
    }

    /**
     * Sends a message to discord
     *
     * @param playerName the name of the player
     * @param uuid       the player uuid
     * @param msg        the message to send
     */
    @SuppressWarnings("ConstantConditions")
    public void sendMessage(String playerName, String uuid, DiscordMessage msg, MessageChannel channel) {
        WorkThread.executeJob(() -> {
            String pName = playerName;
            if (channel == null) return;
            final boolean isServerMessage = pName.equals(Configuration.instance().webhook.serverName) && uuid.equals("0000000");
            final UUID uUUID = uuid.equals("0000000") ? null : UUID.fromString(uuid);
            String avatarURL = "";
            if (!isServerMessage && uUUID != null) {
                if (LinkManager.isPlayerLinked(uUUID)) {
                    final PlayerLink l = LinkManager.getLink(null, uUUID);
                    final Member dc = getMemberById(Long.parseLong(l.discordID));
                    if (dc != null)
                        if (l.settings.useDiscordNameInChannel) {
                            pName = dc.getEffectiveName();
                            avatarURL = dc.getUser().getAvatarUrl();
                        }
                }
                if (avatarURL != null && avatarURL.isEmpty())
                    avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", uUUID.toString()).replace("%uuid_dashless%", uUUID.toString().replace("-", "")).replace("%name%", pName).replace("%randomUUID%", UUID.randomUUID().toString());
            }
            if (isServerMessage) {
                avatarURL = Configuration.instance().webhook.serverAvatarURL;
            }
            sendMessage(pName, msg, avatarURL, channel, !isServerMessage, uuid);
        });
    }

    /**
     * Saves a new Message into the recent messages list
     *
     * @param msgID Message ID
     * @param uuid  Sender UUID
     */
    public void rememberRecentMessage(String msgID, UUID uuid) {
        if (recentMessages.size() + 1 >= 150) {
            do {
                recentMessages.remove(recentMessages.keySet().toArray(new String[0])[0]);
            } while (recentMessages.size() + 1 >= 150);
        }
        recentMessages.put(msgID, uuid);
    }

}

