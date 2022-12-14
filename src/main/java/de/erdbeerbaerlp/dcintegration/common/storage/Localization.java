package de.erdbeerbaerlp.dcintegration.common.storage;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlComment;
import com.moandjiezana.toml.TomlIgnore;
import com.moandjiezana.toml.TomlWriter;

import java.io.IOException;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.messagesFile;

@SuppressWarnings("unused")
public class Localization {

    @TomlIgnore
    private static Localization INSTANCE;

    static {

        //First instance of the Config
        INSTANCE = new Localization();
    }

    @TomlComment({"This is what will be displayed ingame when someone types into the bot's channel", "PLACEHOLDERS:", "%user% - The username", "%id% - The user ID", "%msg% - The message"})
    public String ingame_discordMessage = "\u00a76[\u00a75DISCORD\u00a76]\u00a7r <%user%> %msg%";
    @TomlComment({"This is what will be displayed ingame when someone sends an reply into the bot's channel", "PLACEHOLDERS:", "%user% - The username", "%id% - The user ID", "%msg% - The reply message", "%ruser% - The username of the message that got the reply", "%rmsg% - The replied message"})
    public String ingame_discordReplyMessage = "\u00a76[\u00a75DISCORD\u00a76]\u00a7r \u00a7a%user%\u00a7r in reply to \u00a73%ruser%\u00a7r: %msg%";
    @TomlComment({"Message shown when hovering over the username of an discord message", "PLACEHOLDERS:", "%user% - The username/nickname (Someone123)", "%user#tag% - The username with tag (someone#0001)", "%id% - The user ID", "", "NOTE: using an @ here can cause ping sounds ingame"})
    public String discordUserHover = "\u00a73Discord User %user#tag%\n\u00a7aClick to mention";
    @TomlComment("This message will edited in / sent when the server finished starting")
    public String serverStarted = "Server Started!";
    @TomlComment({"Message to show while the server is starting", "This will be edited to SERVER_STARTED_MSG when webhook is false"})
    public String serverStarting = "Server Starting...";
    @TomlComment("This message will be sent when the server was stopped")
    public String serverStopped = "Server Stopped!";
    @TomlComment("The message to print to discord when it was possible to detect a server crash")
    public String serverCrash = "Server Crash Detected :thinking:";
    @TomlComment({"Gets sent when an player joins", "", "PLACEHOLDERS:", "%player% - The player's name"})
    public String playerJoin = "%player% joined";
    @TomlComment({"Gets sent when an player leaves", "", "PLACEHOLDERS:", "%player% - The player's name"})
    public String playerLeave = "%player% left";
    @TomlComment({"Gets sent when an player dies", "", "PLACEHOLDERS:", "%player% - The player's name", "%msg% - The death message"})
    public String playerDeath = "%player% %msg%";
    @TomlComment({"Message sent instead of playerLeave, when the player times out", "", "PLACEHOLDERS:", "%player% - The player's name"})
    public String playerTimeout = "%player% timed out!";
    @TomlComment({"Gets sent when an player finishes an advancement", "Supports MulitLined messages using \\n", "", "PLACEHOLDERS:", "%player% - The player's name", "%name% - The advancement name", "%desc% - The advancement description"})
    public String advancementMessage = "%player% just made the advancement **%name%**\n_%desc%_";
    @TomlComment({"The chat message in discord, sent from an player in-game", "", "PLACEHOLDERS:", "%player% - The player's name", "%msg% - The chat message"})
    public String discordChatMessage = "%player%: %msg%";
    @TomlComment({"Sent to a player when someone reacts to his messages", "PLACEHOLDERS:", "%name% - (Nick-)Name of the user who reacted (format: 'SomeNickName')", "%name2% - Name of the user who reacted with discord discriminator (format: 'SomeName#0123')", "%msg% - Content of the message which got the reaction", "%emote% - The reacted emote"})
    public String reactionMessage = "\u00a76[\u00a75DISCORD\u00a76]\u00a7r\u00a77 %name% reacted to your message \"\u00a79%msg%\u00a77\" with '%emote%'";
    @TomlComment("Message shown for attachments")
    public String attachment = "Attachment";
    @TomlComment("Message shown for stickers")
    public String sticker = "Sticker";
    @TomlComment("Header for Embeds")
    public String embed = "Embed";
    @TomlComment("Message shown for embed images")
    public String embedImage = "Image";
    @TomlComment("Message shown for embed messages")
    public String embedMessage = "Message";
    @TomlComment("Hover message for the bot tag ingame")
    public String bot = "This user is an bot";
    @TomlComment("Strings about the discord commands")
    public Commands commands = new Commands();
    @TomlComment("Strings about the account linking feature")
    public Linking linking = new Linking();
    @TomlComment("Strings about the personal settings feature")
    public PersonalSettings personalSettings = new PersonalSettings();

    public static Localization instance() {
        return INSTANCE;
    }

    public void loadConfig() throws IOException, IllegalStateException {
        if (!messagesFile.exists()) {
            INSTANCE = new Localization();
            INSTANCE.saveConfig();
            return;
        }
        INSTANCE = new Toml().read(messagesFile).to(Localization.class);
        INSTANCE.saveConfig(); //Re-write the config so new values get added after updates
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "DuplicatedCode"})
    public void saveConfig() throws IOException {
        if (!messagesFile.exists()) {
            if (!messagesFile.getParentFile().exists()) messagesFile.getParentFile().mkdirs();
            messagesFile.createNewFile();
        }
        final TomlWriter w = new TomlWriter.Builder()
                .indentValuesBy(2)
                .indentTablesBy(4)
                .padArrayDelimitersBy(2)
                .build();
        w.write(this, messagesFile);
    }

    public static class Linking {

        @TomlComment({"Sent to the user when he linked his discord successfully", "PLACEHOLDERS:", "%player% - The in-game player name"})
        public String linkSuccessful = "Your account is now linked with %player%.\nUse /settings to view and set some user-specific settings";

        @TomlComment({"Sent to the user when linking fails"})
        public String linkFailed = "Account link failed";

        @TomlComment({"Sent when an already linked user attempts to link an account", "PLACEHOLDERS:", "%player% - The in-game player name"})
        public String alreadyLinked = "Your account is already linked with %player%";

        @TomlComment({"Sent when attempting to use personal commands while not linked", "PLACEHOLDERS:", "%method% - The currently enabled method for linking"})
        public String notLinked = "Your account is not linked! Link it first using %method%";


        @TomlComment({"Message of the link method in whitelist mode", "Used by %method% placeholder"})
        public String linkMethodWhitelistCode = "joining the server and then using `/link <whitelist-code>` here";

        @TomlComment({"Message of the link method in normal mode", "Used by %method% placeholder"})
        public String linkMethodIngame = "`/discord link` ingame";

        @TomlComment({"Sent when attempting to whitelist-link with an non uuid string", "PLACEHOLDERS:", "%arg% - The provided argument"})
        public String link_argumentNotUUID = "Argument \"%arg%\" is not an valid UUID or Name.";

        @TomlComment("Sent when attempting to link with an unknown number")
        public String invalidLinkNumber = "Invalid link number!";

        @TomlComment("Sent when attempting to link with an invalid number")
        public String linkNumberNAN = "This is not a number!";

        @TomlComment({"Message shown to players who are not whitelisted using discord", "No effect if discord whitelist is off"})
        public String notWhitelistedCode = "\u00a7cYou are not whitelisted.\nJoin the discord server for more information\nhttps://discord.gg/someserver\nYour Whitelist-Code is: \u00a76%code%";
        @TomlComment({"Message shown to players who are whitelisted using discord but don't have the required role anymore", "No effect if discord whitelist is off"})
        public String notWhitelistedRole = "\u00a7cYou are whitelisted, but you need an role to join.\nSee the discord server for more information";

        @TomlComment("Sent when trying to link without an required role")
        public String link_requiredRole = "You need to have an role to use this";

        @TomlComment("Sent when trying to link as an non-member")
        public String link_notMember = "You are not member of the Discord-Server this bot is operating in!";
        @TomlComment({"Sent to the user when he linked his discord successfully", "PLACEHOLDERS:", "%name% - The linked discord name", "%name#tag% - The linked discord name with tag"})
        public String linkSuccessfulIngame = "Your account is now linked with discord-user %name#tag%";
        @TomlComment({"Message shown to players who want to link their discord account ingame", "", "PLACEHOLDERS:", "%num% - The link number"})
        public String linkMsgIngame = "Send this command to the server channel to link your account: /link %num%\nThis number will expire after 10 minutes";

        @TomlComment("Shown when hovering over the link message")
        public String hoverMsg_copyClipboard = "Click to copy command to clipboard";
    }

    public static class Commands {
        @TomlComment("Shown in console when trying to use a in-game only command")
        public String ingameOnly = "This command can only be executed ingame!";

        @TomlComment("Shown in-game when trying to use a console only command")
        public String consoleOnly = "This command can only be executed from console!";

        @TomlComment("Shown when successfully reloading the config file")
        public String configReloaded = "Config reloaded!";

        @TomlComment("Shown when an subcommand is disabled")
        public String subcommandDisabled = "This subcommand is disabled!";

        @TomlComment("Message sent when user does not have permission to run a command")
        public String noPermission = "You don't have permission to execute this command!";

        @TomlComment({"Message sent when an invalid command was typed", "", "PLACEHOLDERS:"})
        public String unknownCommand = "Unknown command, try `/help` for a list of commands";

        @TomlComment("Message if a player provides less arguments than required")
        public String notEnoughArguments = "Not enough arguments";

        @TomlComment("Message if a player provides too many arguments")
        public String tooManyArguments = "Too many arguments";

        @TomlComment({"Message if a player can not be found", "", "PLACEHOLDERS:", "%player% - The player's name"})
        public String playerNotFound = "Can not find player \"%player%\"";

        @TomlComment("The message for 'list' when no player is online")
        public String cmdList_empty = "There is no player online...";

        @TomlComment("The header for 'list' when one player is online")
        public String cmdList_one = "There is 1 player online:";

        @TomlComment({"The header for 'list'", "PLACEHOLDERS:", "%amount% - The amount of players online"})
        public String cmdList_header = "There are %amount% players online:";

        @TomlComment("Header of the help command")
        public String cmdHelp_header = "Your available commands in this channel:";

        @TomlComment("Message sent when ignoring Discord messages")
        public String commandIgnore_ignore = "You are now ignoring Discord messages!";

        @TomlComment("Message sent when unignoring Discord messages")
        public String commandIgnore_unignore = "You are no longer ignoring Discord messages!";

        @TomlComment({"Message sent when using the uptime command", "", "PLACEHOLDERS:", "%uptime% - Uptime in uptime format, see uptimeFormat"})
        public String cmdUptime_message = "The server is running for %uptime%";

        @TomlComment({"The format of the uptime command", "For more help with the formatting visit https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/time/DurationFormatUtils.html"})
        public String uptimeFormat = "dd 'days' HH 'hours' mm 'minutes'";

        @TomlComment("Shows when running slash commands as command response")
        public String executing = "Executing...";

        @TomlComment("Command argument description for the linkcheck command's Discord user parameter")
        public String cmdLinkcheck_userargDesc = "The discord user to check";

        @TomlComment("Command argument description for the linkcheck command's minecraft player parameter")
        public String cmdLinkcheck_mcargDesc = "The minecraft player's UUID or Name to check";

        @TomlComment("Sent when checked user is not linked")
        public String cmdLinkcheck_notlinked = "This account is not linked!";

        @TomlComment("")
        public String cmdLinkcheck_discordAcc = "Discord-Account: ";

        public String cmdLinkcheck_minecraftAcc = "Minecraft Account: ";

        public String cmdLinkcheck_cannotGetPlayer = "Error getting player information! Maybe you used an invalid name / UUID";

        public String cmdSett_key = "Destination settings key";
        public String cmdSett_val = "Settings value";
        public String cmdSett_set = "Change an setting";
        public String cmdSett_get = "Retrieve an setting value";

        @TomlComment("Command descriptions")
        public Descriptions descriptions = new Descriptions();

        public static class Descriptions {
            public String settings = "Allows you to edit your personal settings";
            public String uptime = "Displays the server uptime";
            public String help = "Displays a list of all commands";
            public String list = "Lists all players currently online";
            public String link = "Links your Discord account with your Minecraft account";
            public String whitelist = "Whitelists you on the server by linking with Discord";
            public String linkcheck = "Shows info about an linked discord user or an ingame player";
        }
    }

    public static class PersonalSettings {


        @TomlComment("Message for getting an setting's value")
        public String personalSettingGet = "This settings value is `%bool%`";

        @TomlComment("Sent when user sucessfully updates an prersonal setting")
        public String settingUpdateSuccessful = "Successfully updated setting!";

        @TomlComment("Header of the personal settings list")
        public String personalSettingsHeader = "Personal Settings list:";

        @TomlComment("Error message when providing an invalid personal setting name")
        public String invalidPersonalSettingKey = "`%key%` is not an valid setting!";

        @TomlComment({})
        public String settingsCommandUsage = "Usages:\n\n/settings - lists all available keys\n/settings get <key> - Gets the current settings value\n/settings set <key> <value> - Sets an Settings value";

        @TomlComment("Sent when setting an personal setting fails")
        public String settingUpdateFailed = "Failed to set value :/";

        @TomlComment("Sent when attempting to change an blacklisted setting")
        public String settingUpdateBlocked = "The server owner disabled changing of this setting";

        @TomlComment("Descriptions of the settings")
        public Descriptions descriptons = new Descriptions();

        public static class Descriptions {
            public String ignoreDiscordChatIngame = "Configure if you want to ignore discord chat ingame";
            public String useDiscordNameInChannel = "Should the bot send messages using your discord name and avatar instead of your in-game name and skin?";
            public String ignoreReactions = "Configure if you want to ignore discord reactions ingame";
            public String pingSound = "Toggle the ingame ping sound";
            public String hideFromDiscord = "Setting this to true will hide all of your minecraft messages from discord";
        }
    }
}
