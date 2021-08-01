package de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs;

import de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat.DiscordCommand;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import javax.annotation.Nonnull;

public abstract class DMCommand extends DiscordCommand {

    protected DMCommand() {
        super("dmcommand", "An DirectMessage command");
    }

    @Override
    public final boolean adminOnly() {
        return false;
    }

    /**
     * Method called when executing this command
     * <p>
     *  @param args   arguments passed by the player
     * @param channel Text channel where command was executed
     * @param sender Command sender
     */
    public abstract void execute(@Nonnull String[] args, @Nonnull final MessageChannel channel, User sender);

    @Override
    @Deprecated
    public final void execute(SlashCommandEvent ev) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canUserExecuteCommand(User user) {
        if (user == null) return false;
        return PlayerLinkController.isDiscordLinked(user.getId());
    }
}
