package de.erdbeerbaerlp.dcintegration.common.storage;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlComment;
import com.moandjiezana.toml.TomlIgnore;
import com.moandjiezana.toml.TomlWriter;
import de.erdbeerbaerlp.dcintegration.common.storage.configCmd.ConfigCommand;

import java.io.IOException;
import java.util.ArrayList;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.commandsFile;

@SuppressWarnings("unused")
public class Commands {

    @TomlIgnore
    private static Commands INSTANCE;
    @TomlIgnore
    private static final ConfigCommand[] defaultCommands;

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
        INSTANCE = new Commands();
    }

    public static Commands instance() {
        return INSTANCE;
    }

    public void loadConfig() throws IOException, IllegalStateException {
        if (!commandsFile.exists()) {
            INSTANCE = new Commands();
            INSTANCE.saveConfig();
            return;
        }
        INSTANCE = new Toml().read(commandsFile).to(Commands.class);
        if(!INSTANCE.configGenerated) {
            INSTANCE.commands.customCommands = defaultCommands;
            INSTANCE.configGenerated = true;
        }
        INSTANCE.saveConfig(); //Re-write the config so new values get added after updates
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "DuplicatedCode"})
    public void saveConfig() throws IOException {
        if (!commandsFile.exists()) {
            if (!commandsFile.getParentFile().exists()) commandsFile.getParentFile().mkdirs();
            commandsFile.createNewFile();
        }
        final TomlWriter w = new TomlWriter.Builder()
                .indentValuesBy(2)
                .indentTablesBy(4)
                .padArrayDelimitersBy(2)
                .build();
        w.write(this, commandsFile);
    }


    @TomlComment({"Add your custom commands here", "You can also generate some on https://erdbeerbaerlp.de/dcintegration-commands/", "If empty, there are no commands to generate"})
    public CommandsCont commands = new CommandsCont();

    @TomlComment("Set this to false to regenerate default config commands")
    public boolean configGenerated = false;

    public static class CommandsCont{
        public ConfigCommand[] customCommands = new ConfigCommand[0];
    }

}
