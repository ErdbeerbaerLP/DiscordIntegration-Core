package de.erdbeerbaerlp.dcintegration.common.util;

import de.erdbeerbaerlp.dcintegration.common.Discord;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class Variables {

    /**
     * Mod/Plugin version
     */
    public static final String VERSION = "2.4.0";
    /**
     * Discord Integration data directory
     */
    public static final File discordDataDir = new File("./DiscordIntegration-Data/");
    /**
     * Time in milliseconds when the server started
     */
    public static long started = -1;
    /**
     * Path to the config file
     */
    public static File configFile = new File("./config/Discord-Integration.toml");
    public static File messagesFile = new File(discordDataDir,"Messages.toml");
    /**
     * Message sent when the server is starting (in non-webhook mode!), stored for editing
     */
    public static CompletableFuture<Message> startingMsg;
    /**
     * The currently active {@link Discord} instance
     */
    public static Discord discord_instance;
    /**
     * The primary {@link org.slf4j.Logger} instance
     */
    public static final Logger LOGGER = LoggerFactory.getLogger("Discord-Integration-Core");
}
