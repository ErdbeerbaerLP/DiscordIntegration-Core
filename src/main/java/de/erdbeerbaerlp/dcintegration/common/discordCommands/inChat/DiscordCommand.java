package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


/**
 * Abstract class used for discord commands
 */
public abstract class DiscordCommand extends CommandData {

    boolean isConfigCmd = false;

    protected boolean useArgs = false;
    protected String argText = "<undefined>";

    public final boolean isUsingArgs() {
        return useArgs;
    }

    public final String getArgText() {
        return argText;
    }


    protected DiscordCommand(String name, String desc) {
        super(name,desc);
    }

    /**
     * Sets the name of the command
     */
    @Nonnull
    public String getName(){
        return name;
    }

    /**
     * Sets the aliases of the command<br>
     *  no longer used
     */
    @Nonnull
    @Deprecated
    public String[] getAliases(){return new String[0];}

    /**
     * Sets the description for the help command
     */
    @Nonnull
    public String getDescription(){
        return description;
    }

    /**
     * Is this command only for admins?
     */
    public boolean adminOnly() {
        return false;
    }

    /**
     * Method called when executing this command
     * <p>
     *  @param ev the SlashCommandEvent
     */
    public abstract void execute(SlashCommandEvent ev);

    /**
     * Wether or not this command should be visible in help
     */
    public boolean includeInHelp() {
        return true;
    }

    /**
     * Should the user be able to execute this command?
     * <p>
     *
     * @param user The user being handled
     * @return wether or not the user can execute this command
     */
    public boolean canUserExecuteCommand(@Nonnull User user) {
        Member m = null;
        for (final Member me : discord_instance.getChannel().getMembers()) {
            if (me.getUser().equals(user)) {
                m = me;
                break;
            }
        }
        if (m == null) return false;
        return !this.adminOnly() || discord_instance.hasAdminRole(m.getRoles());
    }


    /**
     * Override to customize the command usage, which is being displayed in help (ex. to add arguments)
     */
    public String getCommandUsage() {
        return "/"+getName();
    }

    public final boolean equals(DiscordCommand cmd) {
        return cmd.getName().equals(this.getName());
    }


    /**
     * Generates an Player not found message to send to discord
     *
     * @param playerName Name of the player
     * @return The message
     */
    public final String getPlayerNotFoundMsg(String playerName) {
        return Configuration.instance().localization.commands.playerNotFound.replace("%player%", playerName);
    }

    public final boolean isConfigCommand() {
        return isConfigCmd;
    }

}
