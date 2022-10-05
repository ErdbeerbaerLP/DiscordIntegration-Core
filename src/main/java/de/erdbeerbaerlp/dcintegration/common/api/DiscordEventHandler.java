package de.erdbeerbaerlp.dcintegration.common.api;

import de.erdbeerbaerlp.dcintegration.common.discordCommands.DiscordCommand;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@SuppressWarnings("unused")
public abstract class DiscordEventHandler {
    /**
     * Gets called when someone DMs the bot before any code gets executed
     *
     * @return true to cancel default code execution
     */
    public boolean onDiscordPrivateMessage(@Nonnull final MessageReceivedEvent event){
        return false;
    }

    /**
     * Gets called on discord message in any channel other than private before anything processed (like commands)
     *
     * @return true to cancel default code execution
     */
    public boolean onDiscordMessagePre(@Nonnull final MessageReceivedEvent event){
        return false;
    }

    /**
     * Gets called when a command was entered, invalid or not
     *
     * @param channel Text channel where command was executed
     * @param sender Command sender
     * @param command the executed command or null if the command was invalid, or the user had no permission for any command
     * @return true to cancel default code execution
     */
    public boolean onDiscordCommand(final MessageChannelUnion channel, User sender, @Nullable final DiscordCommand command){
        return false;
    }

    /**
     * Gets called after command execution or message forwarding in any channel
     */
    public void onDiscordMessagePost(@Nonnull final MessageReceivedEvent event){}

    /**
     * Gets called when an player successfully links their Discord and Minecraft account
     */
    public void onPlayerLink(@Nonnull final UUID mcUUID, @Nonnull final String discordID){}

    /**
     * Gets called when an player successfully links their Discord and Minecraft (Bedrock) account
     */
    public void onBedrockPlayerLink(@Nonnull final UUID bedrockUUID, @Nonnull final String discordID){}
}
