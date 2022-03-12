package de.erdbeerbaerlp.dcintegration.common.util;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.kyori.adventure.text.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ServerInterface {

    /**
     * @return Maximum player amount on this server
     */
    int getMaxPlayers();
    /**
     * @return Online player count on this server
     */
    int getOnlinePlayers();

    /**
     * Sends an {@link Component} as ingame message to all players who are not ignoring messages
     * @param msg Message to send
     */
    void sendMCMessage(@Nonnull Component msg);

    /**
     * Sends an message for reactions from discord
     * @param member Discord {@link Member}, who reacted
     * @param retrieveMessage Message which got the reaction
     * @param targetUUID Original sender's {@link UUID}
     * @param reactionEmote Emote that was added to the message
     */
    void sendMCReaction(Member member, @Nonnull RestAction<Message> retrieveMessage, @Nonnull UUID targetUUID, @Nonnull MessageReaction.ReactionEmote reactionEmote);

    /**
     * Runs an command on the server
     * @param cmd Command to execute
     * @param cmdMsg the message to edit the command output in
     */
    void runMcCommand(@Nonnull String cmd, final CompletableFuture<InteractionHook> cmdMsg, User user);

    /**
     * @return all online players on this server in format <{@link UUID}, PlayerName>
     */
    @Nonnull
    HashMap<UUID, String> getPlayers();

    /**
     * Sends an message to that specific player
     * @param msg Message
     * @param player Target player's {@link UUID}
     */
    void sendMCMessage(String msg, UUID player);

    /**
     * Checks if the server is running in online mode
     * @return online mode status
     */
    boolean isOnlineMode();

    /**
     * Gets the display name of the player's UUID
     * @param uuid {@link UUID} to get the name from
     * @return The player's name, or null if the player was not found
     */
    @Nullable
    String getNameFromUUID(@Nonnull UUID uuid);
}
