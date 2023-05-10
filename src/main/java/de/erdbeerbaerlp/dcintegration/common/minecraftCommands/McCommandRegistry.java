package de.erdbeerbaerlp.dcintegration.common.minecraftCommands;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;

import java.util.ArrayList;
import java.util.List;

public class McCommandRegistry {

    /**
     * Registered commands
     */
    private static final List<MCSubCommand> commands = new ArrayList<>();

    /**
     * Registers an {@link MCSubCommand}<br>
     * This has to be done before the server is fully started!
     *
     * @param cmd command
     * @return true if the registration was successful
     */
    public static boolean registerCommand(MCSubCommand cmd) {
        if (DiscordIntegration.started != -1) {
            DiscordIntegration.LOGGER.info("Attempted to register mc command " + cmd.getName() + "after server finished loading");
            return false;
        }
        commands.add(cmd);
        return true;
    }

    public static List<MCSubCommand> getCommands() {
        return commands;
    }
    public static void registerDefaultCommands(){
        registerCommand(new LinkCommand());
        registerCommand(new UnlinkCommand());
        registerCommand(new IgnoreCommand());
        registerCommand(new ReloadCommand());
        registerCommand(new MigrateCommand());
    }

}
