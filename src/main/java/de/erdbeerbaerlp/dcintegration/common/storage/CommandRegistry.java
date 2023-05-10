package de.erdbeerbaerlp.dcintegration.common.storage;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.*;
import de.erdbeerbaerlp.dcintegration.common.storage.configCmd.ConfigCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandRegistry {
    /**
     * Commands registered to Discord
     */
    public static final HashMap<String, DiscordCommand> registeredCMDs = new HashMap<>();
    /**
     * Registered commands
     */
    private static List<DiscordCommand> commands = new ArrayList<>();

    /**
     * Registers all default commands and custom commands from config
     */
    public static void registerDefaultCommands() {
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
     * Registers all commands to discord if changed
     */
    public static void updateSlashCommands() throws IllegalStateException {
        final GuildMessageChannel channel = DiscordIntegration.INSTANCE.getChannel();
        if (channel == null)
            throw new IllegalStateException("Channel does not exist, check channel ID and bot permissions on both channel and category. Also make sure to enable all intents for the bot on https://discord.com/developers/applications/" + DiscordIntegration.INSTANCE.getJDA().getSelfUser().getApplicationId() + "/bot");
        final List<Command> localCmds = channel.getGuild().retrieveCommands().complete();
        final List<Command> globalCmds = DiscordIntegration.INSTANCE.getJDA().retrieveCommands().complete();
        boolean regenLocalCommands = localCmds.size() > 0 && !Configuration.instance().commands.useLocalCommands;
        boolean regenGlobalCommands = globalCmds.size() > 0 && Configuration.instance().commands.useLocalCommands;

        if (commands.size() == localCmds.size())
            for (DiscordCommand cmd : commands) {
                Command cm = null;
                for (Command c : localCmds) {
                    if (((CommandData) cmd).getName().equals(c.getName())) {
                        cm = c;
                        break;
                    }
                }
                if (cm == null) {
                    regenLocalCommands = true;
                    break;
                }
                if (!optionsEqual(cmd.getOptions(), cm.getOptions())) {
                    regenLocalCommands = true;
                    break;
                }
            }
        else regenLocalCommands = true;
        if (regenLocalCommands) {
            DiscordIntegration.LOGGER.info("Regenerating commands...");
            CommandListUpdateAction commandListUpdateAction = channel.getGuild().updateCommands();

            if (Configuration.instance().commands.useLocalCommands)
                for (DiscordCommand cmd : commands) {
                    commandListUpdateAction = commandListUpdateAction.addCommands(cmd);
                }
            final CompletableFuture<List<Command>> submit = commandListUpdateAction.submit();

            if (Configuration.instance().commands.useLocalCommands)
                submit.thenAccept(CommandRegistry::addCmds);
        } else {
            DiscordIntegration.LOGGER.info("No need to regenerate commands");
            addCmds(localCmds);
        }
        if (regenGlobalCommands) {
            DiscordIntegration.LOGGER.info("Regenerating commands...");
            CommandListUpdateAction commandListUpdateAction = DiscordIntegration.INSTANCE.getJDA().updateCommands();
            if (!Configuration.instance().commands.useLocalCommands)
                for (DiscordCommand cmd : commands) {
                    commandListUpdateAction = commandListUpdateAction.addCommands(cmd);
                }
            final CompletableFuture<List<Command>> submit = commandListUpdateAction.submit();
            if (!Configuration.instance().commands.useLocalCommands)
                submit.thenAccept(CommandRegistry::addCmds);
        } else {
            DiscordIntegration.LOGGER.info("No need to regenerate commands");
            addCmds(localCmds);
        }
    }

    @SuppressWarnings({"LoopStatementThatDoesntLoop", "UnusedAssignment"})
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
     * Registers all custom commands from config
     */
    private static void registerConfigCommands() {

        for (ConfigCommand cmd : Configuration.instance().commands.customCommands) {
            try {
                final DiscordCommand regCmd = new CommandFromConfig(cmd.name, cmd.description, cmd.mcCommand, cmd.adminOnly, cmd.args, cmd.hidden, cmd.textToSend);
                if (!registerCommand(regCmd))
                    DiscordIntegration.LOGGER.error("Failed Registering command \"" + cmd.name + "\" because it would override an existing command!");
            } catch (IllegalArgumentException e) {
                DiscordIntegration.LOGGER.error("Failed Registering command \"" + cmd.name + "\":");
                e.printStackTrace();
            }
        }
        DiscordIntegration.LOGGER.info("Finished registering! Registered " + commands.size() + " commands");
    }

    /**
     * Registers an {@link DiscordCommand}<br>
     * This has to be done before the server is fully started!
     *
     * @param cmd command
     * @return true if the registration was successful
     */
    public static boolean registerCommand(DiscordCommand cmd) {
        if (DiscordIntegration.started != -1) {
            DiscordIntegration.LOGGER.info("Attempted to register command " + cmd.getName() + "after server finished loading");
            return false;
        }

        final ArrayList<DiscordCommand> toRemove = new ArrayList<>();
        for (final DiscordCommand c : commands) {
            if (!cmd.isConfigCommand() && cmd.equals(c)) return false;
            else if (cmd.isConfigCommand() && cmd.equals(c)) toRemove.add(c);
        }
        for (DiscordCommand cm : toRemove)
            commands.remove(cm);
        commands.add(cmd);
        if (cmd instanceof CommandFromConfig) {
            if (cmd.isUsingArgs()) cmd.addOption(OptionType.STRING, "args", cmd.getArgText());
        }
        return true;

    }

    private static void addCmds(List<Command> cmds) {
        for (final Command cmd : cmds) {
            for (final DiscordCommand cfcmd : commands) {
                if (cmd.getName().equals(((CommandData) cfcmd).getName())) {
                    registeredCMDs.put(cmd.getId(), cfcmd);
                    DiscordIntegration.LOGGER.info("Added command " + cmd.getName() + " with ID " + cmd.getIdLong());
                }
            }
        }
    }


    private static ArrayList<Role> getAdminRoles(Guild g) {
        final List<Role> gRoles = g.getRoles();
        final ArrayList<Role> adminRoles = new ArrayList<>();

        for (final Role r : gRoles) {
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
        DiscordIntegration.LOGGER.info("Reloading " + cmds.size() + " commands");
        commands = new ArrayList<>();

        for (final DiscordCommand cmd : cmds) {
            if (cmd.isConfigCommand()) continue;
            commands.add(cmd);
        }

        DiscordIntegration.LOGGER.info("Registered " + commands.size() + " commands");
    }

    /**
     * @return A list of all registered commands
     */

    public static List<DiscordCommand> getCommandList() {
        return commands;
    }
}
