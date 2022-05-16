package de.erdbeerbaerlp.dcintegration.common.storage;

import de.erdbeerbaerlp.dcintegration.common.discordCommands.*;
import de.erdbeerbaerlp.dcintegration.common.storage.configCmd.ConfigCommand;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.*;

public class CommandRegistry {
    /**
     * Registered commands
     */
    private static List<DiscordCommand> commands = new ArrayList<>();
    /**
     * Commands registered to Discord
     */
    public static final HashMap<Long, DiscordCommand> registeredCMDs = new HashMap<>();


    /**
     * Registers all commands to discord if changed
     */
    public static void updateSlashCommands() {
        final TextChannel channel = Variables.discord_instance.getChannel();
        final List<Command> cmds = channel.getGuild().retrieveCommands().complete();
        boolean regenCommands = false;
        if (commands.size() == cmds.size())
            for (DiscordCommand cmd : commands) {
                Command cm = null;
                for (Command c : cmds) {
                    if (((CommandData) cmd).getName().equals(c.getName())) {
                        cm = c;
                        break;
                    }
                }
                if(cm == null){
                    regenCommands = true;
                    break;
                }
                if (!optionsEqual(cmd.getOptions(), cm.getOptions())) {
                    regenCommands = true;
                    break;
                }
            }
        else regenCommands = true;
        if (regenCommands) {
            Variables.LOGGER.info("Regenerating commands...");
            CommandListUpdateAction commandListUpdateAction = channel.getGuild().updateCommands();
            for (DiscordCommand cmd : commands) {
                commandListUpdateAction = commandListUpdateAction.addCommands(cmd);
            }
            commandListUpdateAction.submit().thenAccept(CommandRegistry::addCmds);
        } else {
            Variables.LOGGER.info("No need to regenerate commands");
            addCmds(cmds);
        }
    }

    private static boolean optionsEqual(List<OptionData> data, List<Command.Option> options) {
        if (data.size() != options.size()) return false;
        for (int i = 0; i < data.size(); i++) {
            final OptionData optionData = data.get(i);
            final Command.Option option = options.get(i);
            return option.getName().equals(optionData.getName()) && option.getChoices().equals(optionData.getChoices()) && option.getDescription().equals(optionData.getDescription()) && option.isRequired() == optionData.isRequired() && option.getType().equals(optionData.getType());
        }
        return true;
    }

    /**
     * Registers all default commands and custom commands from config
     */
    public static void registerDefaultCommandsFromConfig() {
        if (Configuration.instance().commands.helpCmdEnabled)
            registerCommand(new CommandHelp());
        if (Configuration.instance().commands.listCmdEnabled)
            registerCommand(new CommandList());
        if (Configuration.instance().commands.uptimeCmdEnabled)
            registerCommand(new CommandUptime());

        if (Configuration.instance().linking.enableLinking) {
            registerCommand(new CommandSettings());
            registerCommand(new CommandLinkcheck());
            registerCommand(new CommandLink());
        }
        registerConfigCommands();
    }

    /**
     * Registers all custom commands from config
     */
    public static void registerConfigCommands() {

        for (ConfigCommand cmd : Configuration.instance().commands.customCommands) {
            try {
                final DiscordCommand regCmd = new CommandFromCFG(cmd.name, cmd.description, cmd.mcCommand, cmd.adminOnly, cmd.args, cmd.hidden);
                if (!registerCommand(regCmd))
                    Variables.LOGGER.error("Failed Registering command \"" + cmd.name + "\" because it would override an existing command!");
            } catch (IllegalArgumentException e) {
                Variables.LOGGER.error("Failed Registering command \"" + cmd.name + "\":");
                e.printStackTrace();
            }
        }
        Variables.LOGGER.info("Finished registering! Registered " + commands.size() + " commands");
    }

    /**
     * Registers an {@link DiscordCommand}<br>
     * This has to be done before the server is fully started!
     *
     * @param cmd command
     * @return true if the registration was successful
     */
    public static boolean registerCommand(@Nonnull DiscordCommand cmd) throws IllegalStateException {
        if (Variables.started != -1) {
            Variables.LOGGER.info("Attempted to register command " + cmd.getName() + "after server finished loading");
            return false;
        }
        final TextChannel channel = Variables.discord_instance.getChannel();
        if (channel == null) throw new IllegalStateException("Channel does not exist");
        final Guild guild = channel.getGuild();

        final ArrayList<DiscordCommand> toRemove = new ArrayList<>();
        for (DiscordCommand c : commands) {
            if (!cmd.isConfigCommand() && cmd.equals(c)) return false;
            else if (cmd.isConfigCommand() && cmd.equals(c)) toRemove.add(c);
        }
        for (DiscordCommand cm : toRemove)
            commands.remove(cm);
        boolean ret = commands.add(cmd);
        if (ret && cmd instanceof CommandFromCFG) {
            if (cmd.isUsingArgs()) cmd.addOption(OptionType.STRING, "args", cmd.getArgText());
        }
        return ret;

    }

    private static void addCmds(List<Command> cmds) {
        for (Command cmd : cmds) {
            for (DiscordCommand cfcmd : commands) {
                if (cmd.getName().equals(((CommandData) cfcmd).getName())) {
                    registeredCMDs.put(cmd.getIdLong(), cfcmd);
                    Variables.LOGGER.info("Added command " + cmd.getName() + " with ID " + cmd.getIdLong());
                }
            }
        }
    }


    private static ArrayList<Role> getAdminRoles(Guild g) {
        final List<Role> gRoles = g.getRoles();
        final ArrayList<Role> adminRoles = new ArrayList<>();

        for (Role r : gRoles) {
            if (ArrayUtils.contains(Configuration.instance().commands.adminRoleIDs, r.getId()))
                adminRoles.add(r);
        }

        return adminRoles;
    }

    /**
     * Attempts to reload all commands
     */
    public static void reRegisterAllCommands() {
        final List<DiscordCommand> cmds = commands;
        Variables.LOGGER.info("Reloading " + cmds.size() + " commands");
        commands = new ArrayList<>();

        for (DiscordCommand cmd : cmds) {
            if (cmd.isConfigCommand()) continue;
            commands.add(cmd);
        }

        Variables.LOGGER.info("Registered " + commands.size() + " commands");
    }

    /**
     * @return A list of all registered commands
     */
    @Nonnull
    public static List<DiscordCommand> getCommandList() {
        return commands;
    }

}
