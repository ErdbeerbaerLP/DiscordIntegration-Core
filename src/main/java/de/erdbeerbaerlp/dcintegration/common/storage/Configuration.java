package de.erdbeerbaerlp.dcintegration.common.storage;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlComment;
import com.moandjiezana.toml.TomlIgnore;
import com.moandjiezana.toml.TomlWriter;
import de.erdbeerbaerlp.dcintegration.common.storage.configCmd.ConfigCommand;
import de.erdbeerbaerlp.dcintegration.common.util.GameType;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import de.erdbeerbaerlp.dcintegration.common.util.UpdateChecker;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.configFile;

@SuppressWarnings("unused")
public class Configuration {

    @TomlIgnore
    private static final ConfigCommand[] defaultCommands;
    @TomlIgnore
    private static Configuration INSTANCE;

    static {
        final ArrayList<ConfigCommand> defaultCmds = new ArrayList<>();
        final ConfigCommand kick = new ConfigCommand();
        kick.name = "kick";
        kick.description = "Kicks an player from the Server";
        kick.mcCommand = "kick %player% %reason%";
        kick.args = new ConfigCommand.CommandArgument[]{
                new ConfigCommand.CommandArgument("player", "The player to be kicked"),
                new ConfigCommand.CommandArgument("reason", "Reason for the kick", true)
        };
        kick.adminOnly = true;
        defaultCmds.add(kick);

        final ConfigCommand stop = new ConfigCommand();
        stop.name = "stop";
        stop.description = "Stops the server";
        stop.mcCommand = "stop";
        stop.adminOnly = true;
        defaultCmds.add(stop);
        final ConfigCommand kill = new ConfigCommand();
        kill.name = "kill";
        kill.description = "Kills an Player or Entity";
        kill.mcCommand = "kill %target%";
        kill.adminOnly = true;
        kill.args = new ConfigCommand.CommandArgument[]{
                new ConfigCommand.CommandArgument("target", "The target(s) for the kill command.")
        };
        defaultCmds.add(kill);

        defaultCommands = defaultCmds.toArray(new ConfigCommand[0]);

        //First instance of the Config
        INSTANCE = new Configuration();
    }

    @TomlComment("General options for the bot")
    public General general = new General();

    @TomlComment("Configuration options for commands")
    public Commands commands = new Commands();

    @TomlComment("Toggle some message related features")
    public Messages messages = new Messages();

    @TomlComment("Settings for embed mode")
    public EmbedMode embedMode = new EmbedMode();

    @TomlComment("Advanced options")
    public Advanced advanced = new Advanced();

    @TomlComment("Config options which only have an effect when using forge")
    public ForgeSpecific forgeSpecific = new ForgeSpecific();

    @TomlComment("Configuration for linking")
    public Linking linking = new Linking();

    @TomlComment("Webhook configuration")
    public Webhook webhook = new Webhook();

    @TomlComment("Configuration for the in-game command '/discord'")
    public IngameCommand ingameCommand = new IngameCommand();

    @TomlComment("The command log channel is an channel where every command execution gets logged")
    public CommandLog commandLog = new CommandLog();

    @TomlComment({"Configure votifier integration here", "(Spigot only)"})
    public Votifier votifier = new Votifier();

    @TomlComment("Configure Dynmap integration here")
    public Dynmap dynmap = new Dynmap();

    @TomlComment({"Configure some plugin-specific BStats settings here", "Everything can be seen here: https://bstats.org/plugin/bukkit/DiscordIntegration/9765", "", "Does not apply to fabric yet, as there is no bstats for it"})
    public BStats bstats = new BStats();

    @TomlComment({"Settings for servers running as Bungeecord-suberver"})
    public Bungee bungee = new Bungee();

    public static Configuration instance() {
        return INSTANCE;
    }

    public void loadConfig() throws IOException, IllegalStateException {
        if (!configFile.exists()) {
            INSTANCE = new Configuration();
            INSTANCE.saveConfig();
            return;
        }
        INSTANCE = new Toml().read(configFile).to(Configuration.class);
        INSTANCE.saveConfig(); //Re-write the config so new values get added after updates
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "DuplicatedCode"})
    public void saveConfig() throws IOException {
        if (!configFile.exists()) {
            if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
            configFile.createNewFile();
        }
        final TomlWriter w = new TomlWriter.Builder()
                .indentValuesBy(2)
                .indentTablesBy(4)
                .padArrayDelimitersBy(2)
                .build();
        w.write(this, configFile);
    }

    public static class General {
        @TomlComment({"Insert your Bot Token here!", "DO NOT SHARE IT WITH ANYONE!"})
        public String botToken = "INSERT BOT TOKEN HERE";

        @TomlComment("The channel ID where the bot will be working in")
        public String botChannel = "000000000";

        @TomlComment({"The bot's status message", "", "PLACEHOLDERS:", "%online% - Online Players", "%max% - Maximum Player Amount"})
        public String botStatusName = "%online% players online";
        @TomlComment({"The bot's status message for 1 online player, set to empty to use botStatusName", "PLACEHOLDERS:", "%online% - Online Players", "%max% - Maximum Player Amount"})
        public String botStatusNameSingular = "%online player online";
        @TomlComment({"The bot's status message for no online players, set to empty to use botStatusName", "PLACEHOLDERS:", "%online% - Online Players", "%max% - Maximum Player Amount"})
        public String botStatusNameEmpty = "No-one is online";
        @TomlComment({"Type of the bot's status", "Allowed Values: DISABLED,PLAYING,WATCHING,LISTENING,STREAMING,COMPETING"})
        public GameType botStatusType = GameType.PLAYING;

        @TomlComment({"URL of the bot's stream when using the status type 'STREAMING'", "Has to start with https://twitch.tv/ or https://www.youtube.com/watch?v="})
        public String streamingURL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

        @TomlComment({"Enable checking for updates?", "Notification will be shown after every server start in log when update is available"})
        public boolean enableUpdateChecker = true;

        @TomlComment({"The minimum release type for the update checker to notify", "Allowed values: release, beta, alpha"})
        public UpdateChecker.ReleaseType updateCheckerMinimumReleaseType = UpdateChecker.ReleaseType.beta;

        @TomlComment({"Set to false to disable config migration from other mods/plugins to this one", "This does not prevent updating of this config after mod updates"})
        public boolean allowConfigMigration = true;

        @TomlComment("Attempt to parse id-based mentions to names in in-game chat")
        public boolean parseMentionsIngame = true;
        @TomlComment("Set to true to supress warning of unsafe mod download location")
        public boolean ignoreFileSource = false;

        @TomlComment("Set to true to allow relaying webhook messages")
        public boolean allowWebhookMessages = false;
    }

    public static class Messages {
        @TomlComment({"Changing this to an language key (like en-US, de-DE) will always attempt to download the latest language file from https://github.com/ErdbeerbaerLP/Discord-Integration-Translations", "Setting to 'local' disables downloading"})
        public String language = "local";

        @TomlComment("Should /say output be sent to discord?")
        public boolean sendOnSayCommand = true;

        @TomlComment("Should /me output be sent to discord?")
        public boolean sendOnMeCommand = true;

        @TomlComment({"When an /say command's message starts with this prefix, it will not be sent to discord", "Useful for hiding system messages by prepending it with this"})
        public String sayCommandIgnoredPrefix = "§4§6§k§r";

        @TomlComment("Should Discord users have their role color ingame?")
        public boolean discordRoleColorIngame = true;
        @TomlComment("Should you be able to hover and click on the discord username ingame?")
        public boolean enableHoverMessage = true;
    }

    public static class EmbedMode {
        @TomlComment({"Enabling this will send configured messages as embed messages","See below configuration options of this category to see what messages can be moved to embeds"})
        public boolean enabled = false;

        @TomlComment("Starting & Started Messages")
        public EmbedEntry startMessages = new EmbedEntry(true, TextColors.DISCORD_GREEN.asHexString());
        @TomlComment("Stop & Crash Messages")
        public EmbedEntry stopMessages = new EmbedEntry(true, TextColors.DISCORD_RED.asHexString());
        @TomlComment("Player join message")
        public EmbedEntry playerJoinMessage = new EmbedEntry(true, TextColors.DISCORD_GREEN.asHexString());
        @TomlComment({"Player leave messages", "Also containing timeouts"})
        public EmbedEntry playerLeaveMessages = new EmbedEntry(true, TextColors.DISCORD_RED.asHexString());
        @TomlComment("Player Death message")
        public EmbedEntry deathMessage = new EmbedEntry(true, TextColors.BLACK.asHexString());
        @TomlComment("Advancement messages")
        public EmbedEntry advancementMessage = new EmbedEntry(true, TextColors.DISCORD_YELLOW.asHexString());
        @TomlComment("Player chat messages")
        public ChatEmbedEntry chatMessages = new ChatEmbedEntry(true, TextColors.of(Color.GRAY).asHexString());

        public static class EmbedEntry{
            @TomlComment("Send as embed?")
            public boolean asEmbed;
            @TomlComment("Color of embed bar")
            public String colorHexCode;
            EmbedEntry(boolean defaultEnabled, String defaultColor){
                this.asEmbed = defaultEnabled;
                this.colorHexCode = defaultColor;
            }

            public EmbedBuilder toEmbed(){
                return new EmbedBuilder()
                      .setColor(Color.decode(this.colorHexCode));
            }
        }
        public static class ChatEmbedEntry extends EmbedEntry{
            @TomlComment("Generate unique chat colors from player uuid?")
            public boolean generateUniqueColors = true;

            ChatEmbedEntry(boolean defaultEnabled, String defaultColor) {
                super(defaultEnabled, defaultColor);
            }
        }
    }

    public static class Commands {
        @TomlComment({"Toggle the entire command feature", "Disabling this will disable registering any commands to discord"})
        public boolean enabled = true;
        @TomlComment({"The Role IDs of your Admin Roles", "Now supports multiple roles which can access admin commands"})
        public String[] adminRoleIDs = new String[0];

        @TomlComment({"Add your custom commands here", "You can also generate some on https://erdbeerbaerlp.de/dcintegration-commands/"})
        public ConfigCommand[] customCommands = defaultCommands;

        @TomlComment("You must op this UUID in the ops.txt or some custom commands won't work!")
        public String senderUUID = "8d8982a5-8cf9-4604-8feb-3dd5ee1f83a3";

        @TomlComment({"Enable the list command in discord"})
        public boolean listCmdEnabled = true;

        @TomlComment({"Show list command only for the user who runs it"})
        public boolean hideListCmd = true;

        @TomlComment({"Enable the uptime command in discord"})
        public boolean uptimeCmdEnabled = true;

        @TomlComment({"Show uptime command only for the user who runs it"})
        public boolean hideUptimeCmd = false;

        @TomlComment({"Enables using local commands for faster registration","Local Commands will register all slash commands directly to the server instead of to the bot","Setting this to true requires the bot to be invited with the scope 'application.commands' to work"})
        public boolean useLocalCommands = false;
    }

    public static class Advanced {
        @TomlComment({"Custom channel ID for server specific messages (like Join/leave)", "Leave 'default' to use default channel"})
        public String serverChannelID = "default";

        @TomlComment({"Custom channel ID for death messages", "Leave 'default' to use default channel"})
        public String deathsChannelID = "default";

        @TomlComment({"Custom channel for for ingame messages", "Leave 'default' to use default channel"})
        public String chatOutputChannelID = "default";

        @TomlComment({"Custom channel where messages get sent to minecraft", "Leave 'default' to use default channel"})
        public String chatInputChannelID = "default";

        @TomlComment({"Allows you to change the target URL for the API to make it usable with custom discord instances like Spacebar", "DO NOT CHANGE if you don't know what you are doing!!"})
        public String baseAPIUrl = "https://discord.com";


    }

    public static class ForgeSpecific {
        @TomlComment({"A list of blacklisted modids", "Adding one will prevent the mod to send messages to discord using forges IMC system"})
        public String[] IMC_modIdBlacklist = new String[]{"examplemod"};

        @TomlComment("Show item information, which is visible on hover ingame, as embed in discord?")
        public boolean sendItemInfo = true;
    }

    public static class IngameCommand {
        @TomlComment("Enable the /discord command to show an custom message with invite URL?")
        public boolean enabled = true;

        @TomlComment("The message displayed when typing /discord in the server chat")
        public String message = "Join our discord! https://discord.gg/myserver";

        @TomlComment("The message shown when hovering the /discord command message")
        public String hoverMessage = "Click to open the invite url";

        @TomlComment("The url to open when clicking the /discord command text")
        public String inviteURL = "https://discord.gg/myserver";
    }


    public static class Webhook {
        @TomlComment({"Whether or not the bot should use a webhook (it will create one)", "This will only work in standard channels"})
        public boolean enable = false;

        @TomlComment("The avatar to be used for server messages")
        public String serverAvatarURL = "https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Chat-Integration/master/images/srv.png";

        @TomlComment("The name to be used for server messages")
        public String serverName = "Minecraft Server";

        @TomlComment({"The URL where the player avatar gets fetched from", "", "PLACEHOLDERS:", "%uuid% - Returns the player's UUID with dashes", "%uuid_dashless% - Returns the player's UUID without dashes", "%name% - Returns the player's name", "%randomUUID% - Returns an random UUID which can be used to prevent discord cache"})
        public String playerAvatarURL = "https://minotar.net/avatar/%uuid%?randomuuid=%randomUUID%";

        public String webhookName = "MC_DC_INTEGRATION";
    }

    public static class Linking {

        @TomlComment("Unlink players when they leave the discord server for whatever reason (ex. leave,kick,ban)?")
        public boolean unlinkOnLeave = true;
        @TomlComment({"Should discord linking be enabled?", "If whitelist is on, this can NOT be disabled", "DOES NOT WORK IN OFFLINE MODE!"})
        public boolean enableLinking = true;

        @TomlComment({"Set Discord nicknames to match Minecraft usernames when linked"})
        public boolean shouldNickname = false;

        @TomlComment({"Enable global linking?", "Does not work in offline mode"})
        public boolean globalLinking = true;

        @TomlComment({"Database interface class", "This allows you to change your database implementation", "Add database implementations using the addon system", "Do not change without knowing what you are doing"})
        public String databaseClass = "de.erdbeerbaerlp.dcintegration.common.storage.linking.database.JSONInterface";

        @TomlComment({"Role ID of an role an player should get when he links his discord account", "Leave as 0 to disable"})
        public String linkedRoleID = "0";

        @TomlComment({"Enable discord based whitelist?", "This will override the link config!", "To whitelist use the whitelist command in the bot DMs"})
        public boolean whitelistMode = false;

        @TomlComment("Adding Role IDs here will require the players to have at least ONE of these roles to link account")
        public String[] requiredRoles = new String[0];
        @TomlComment({"Adding setting keys to this array will prevent thoose settings to be changed", "They will still show up in the list though"})
        public String[] settingsBlacklist = new String[0];
        @TomlComment("Allows you to configure the default values of some personal settings")
        public PersonalSettingsDefaults personalSettingsDefaults = new PersonalSettingsDefaults();

        public static class PersonalSettingsDefaults {
            public boolean default_useDiscordNameInChannel = true;
            public boolean default_ignoreReactions = false;
            public boolean default_pingSound = true;
        }
    }

    public static class CommandLog {
        @TomlComment({"Channel ID for the command log channel", "Leave 0 to disable"})
        public String channelID = "0";

        @TomlComment({"The format of the log messages", "", "PLACEHOLDERS:", "%sender% - The name of the Command Source", "%cmd% - executed command (e.g. \"say Hello World\"", "%cmd-no-args% - Command without arguments (e.g. \"say\""})
        public String message = "%sender% executed command `%cmd%`";

        @TomlComment("A list of commands that should NOT be logged")
        public String[] ignoredCommands = new String[]{"list", "help", "?"};
    }

    public static class Votifier {
        @TomlComment("Should votifier messages be sent to discord?")
        public boolean enabled = true;

        @TomlComment({"Custom channel ID for Votifier messages", "Leave 'default' to use default channel"})
        public String votifierChannelID = "default";

        @TomlComment({"The message format of the votifier message", "", "PLACEHOLDERS:", "%player% - The player´s name", "%site% - The name of the vote site", "%addr% - (IP) Address of the site"})
        public String message = ":ballot_box: %player% just voted on %site%";

        @TomlComment("Name of the webhook title")
        public String name = "Votifier";

        @TomlComment("URL of the webhook avatar image")
        public String avatarURL = "https://www.cubecraft.net/attachments/bkjvmqn-png.126824/";
    }

    public static class Dynmap {
        @TomlComment({"The message format of the message forwarded to discord", "", "PLACEHOLDERS:", "%sender% - The sender´s name", "%msg% - The Message"})
        public String dcMessage = "<%sender%> %msg%";

        @TomlComment({"Custom channel ID for dynmap chat", "Leave 'default' to use default channel"})
        public String dynmapChannelID = "default";

        @TomlComment("Name of the webhook title")
        public String name = "Dynmap Web-Chat";

        @TomlComment("URL of the webhook avatar image")
        public String avatarURL = "https://static.wikia.nocookie.net/minecraft_gamepedia/images/9/91/Map_Zoom_0.png/revision/latest?cb=20200311153330";

        @TomlComment({"The name format of the message forwarded to the dynmap webchat", "", "PLACEHOLDERS:", "%name% - The discord name of the sender (including nickname)", "%name#tag% - The discord name with tag of the sender (without nickname)"})
        public String webName = "%name% (discord)";
        @TomlComment("Name shown in discord when no name was specified on the website")
        public String unnamed = "Unnamed";
    }

    public static class BStats {
        @TomlComment("Allow sending of installed addon stats (Name and version of installed addons)")
        public boolean sendAddonStats = true;
    }

    public static class Bungee {
        @TomlComment({"Set this to true if the server is running as an subserver of an bungeecord network and therefore needs to be in offline mode", "Setting this will force account linking in offline mode", "Do NOT use for actual offline mode servers, as this will break the linking feature because of the UUIDs!", "", "Currently no support for floodgate running on bungee"})
        public boolean isBehindBungee = false;
    }
}
