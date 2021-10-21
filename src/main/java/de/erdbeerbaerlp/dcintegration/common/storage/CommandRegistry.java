package de.erdbeerbaerlp.dcintegration.common.storage;

import de.erdbeerbaerlp.dcintegration.common.discordCommands.*;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.configCmd.ConfigCommand;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
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

    public static final CommandListUpdateAction cmdList = Variables.discord_instance.getChannel().getGuild().updateCommands();

    private static final HashMap<String, Collection<? extends CommandPrivilege>> permissionsByName = new HashMap<>();
    private static final HashMap<String, Collection<? extends CommandPrivilege>> permissionsByID = new HashMap<>();

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
                    System.err.println("Failed Registering command \"" + cmd.name + "\" because it would override an existing command!");
            } catch (IllegalArgumentException e) {
                System.err.println("Failed Registering command \"" + cmd.name + "\":");
                e.printStackTrace();
            }
        }
        System.out.println("Finished registering! Registered " + commands.size() + " commands");
    }

    /**
     * Registers an {@link DiscordCommand}
     *
     * @param cmd command
     * @return true if the registration was successful
     */
    public static boolean registerCommand(@Nonnull DiscordCommand cmd) {
        final ArrayList<Role> adminRoles = getAdminRoles(Variables.discord_instance.getChannel().getGuild());
        final Member owner = Variables.discord_instance.getChannel().getGuild().retrieveOwner().complete();

        final ArrayList<DiscordCommand> toRemove = new ArrayList<>();
        for (DiscordCommand c : commands) {
            if (!cmd.isConfigCommand() && cmd.equals(c)) return false;
            else if (cmd.isConfigCommand() && cmd.equals(c)) toRemove.add(c);
        }
        for (DiscordCommand cm : toRemove)
            commands.remove(cm);
        boolean ret = commands.add(cmd);
        if (ret && cmdList != null && cmd instanceof CommandFromCFG) {
            if (cmd.isUsingArgs()) cmd.addOption(OptionType.STRING, "args", cmd.getArgText());
        }
        if (cmd.adminOnly()) {
            cmd.setDefaultEnabled(false);
            final HashMap<String, Collection<? extends CommandPrivilege>> perm = new HashMap<>();
            final ArrayList<CommandPrivilege> privileges = new ArrayList<>();
            adminRoles.forEach((r) -> privileges.add(new CommandPrivilege(CommandPrivilege.Type.ROLE, true, r.getIdLong())));
            privileges.add(new CommandPrivilege(CommandPrivilege.Type.USER, true, owner.getIdLong()));
            permissionsByName.put(cmd.getName(), privileges);
        }
        return ret;

    }

    public static void updateSlashCommands() throws ErrorResponseException {
        cmdList.queue();
        cmdList.addCommands(commands).complete().forEach((cmd) -> {
            if (permissionsByName.containsKey(cmd.getName())) {
                permissionsByID.put(cmd.getId(), permissionsByName.get(cmd.getName()));
            }
        });
        Variables.discord_instance.getChannel().getGuild().updateCommandPrivileges(permissionsByID).queue();
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
        System.out.println("Reloading " + cmds.size() + " commands");
        commands = new ArrayList<>();

        for (DiscordCommand cmd : cmds) {
            if (cmd.isConfigCommand()) continue;
            commands.add(cmd);
        }

        System.out.println("Registered " + commands.size() + " commands");
    }

    /**
     * @return A list of all registered commands
     */
    @Nonnull
    public static List<DiscordCommand> getCommandList() {
        return commands;
    }

}
