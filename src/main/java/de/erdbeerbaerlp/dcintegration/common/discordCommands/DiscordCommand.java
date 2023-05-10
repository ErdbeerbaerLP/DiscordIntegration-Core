package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;



/**
 * Abstract class used for discord commands
 */
@SuppressWarnings("unused")
public abstract class DiscordCommand extends CommandDataImpl {

    protected boolean useArgs = false;
    protected String argText = "<undefined>";
    boolean isConfigCmd = false;

    protected DiscordCommand(String name, String desc) {
        super(name, desc);
    }

    public final boolean isUsingArgs() {
        return useArgs;
    }

    public final String getArgText() {
        return argText;
    }

    /**
     * Sets the name of the command
     */

    public @NotNull String getName() {
        return name;
    }

    /**
     * Sets the aliases of the command<br>
     * no longer used
     */

    @Deprecated
    public String[] getAliases() {
        return new String[0];
    }

    /**
     * Sets the description for the help command
     */

    public @NotNull String getDescription() {
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
     *
     * @param ev the SlashCommandInteractionEvent
     */
    public abstract void execute(SlashCommandInteractionEvent ev, ReplyCallbackAction reply);

    /**
     * Whether this command should be visible in help
     */
    @SuppressWarnings("SameReturnValue")
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
    public boolean canUserExecuteCommand(User user) {
        Member m = DiscordIntegration.INSTANCE.getMemberById(user.getIdLong());
        if (m == null) return false;
        //TODO return !this.adminOnly() || DiscordIntegration.INSTANCE.hasAdminRole(m.getRoles());
        return false;
    }

    public final boolean equals(DiscordCommand cmd) {
        return cmd.getName().equals(this.getName());
    }


    /**
     * Generates a Player not found message to send to discord
     *
     * @param playerName Name of the player
     * @return The message
     */
    public final String getPlayerNotFoundMsg(String playerName) {
        return Localization.instance().commands.playerNotFound.replace("%player%", playerName);
    }

    public final boolean isConfigCommand() {
        return isConfigCmd;
    }

}
