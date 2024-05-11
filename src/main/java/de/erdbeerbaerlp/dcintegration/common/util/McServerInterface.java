package de.erdbeerbaerlp.dcintegration.common.util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface McServerInterface {
    /**
     * @return Maximum player amount on this server
     */
    int getMaxPlayers();

    /**
     * @return Online player count on this server
     */
    int getOnlinePlayers();

    /**
     * Sends a {@link Component} as ingame message to all players who are not ignoring messages
     *
     * @param msg Message to send
     */
    void sendIngameMessage(Component msg);

    /**
     * Sends a message to that specific player
     *
     * @param msg    Message
     * @param player Target player's {@link UUID}
     */
    void sendIngameMessage(String msg, UUID player);

    /**
     * Sends a message for reactions from discord
     *
     * @param member          Discord {@link Member}, who reacted
     * @param retrieveMessage Message which got the reaction
     * @param targetUUID      Original sender's {@link UUID}
     * @param reactionEmote   Emote that was added to the message
     */
    void sendIngameReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, EmojiUnion reactionEmote);

    /**
     * Runs a command on the server
     *
     * @param cmd    Command to execute
     * @param cmdMsg the message to edit the command output in
     */
    void runMcCommand(String cmd, final CompletableFuture<InteractionHook> cmdMsg, User user);

    /**
     * @return all online players on this server in format <{@link UUID}, PlayerName>
     */

    HashMap<UUID, String> getPlayers();


    /**
     * Checks if the server is running in online mode
     *
     * @return online mode status
     */
    boolean isOnlineMode();

    /**
     * Gets the display name of the player's UUID
     *
     * @param uuid {@link UUID} to get the name from
     * @return The player's name, or null if the player was not found
     */

    String getNameFromUUID(UUID uuid);

    /**
     * @return The mod/plugin loader name<br>Currently unused, but may be used by addons
     */
    String getLoaderName();

    /**
     * Checks if a player has a server permission
     * @param player Player to check
     * @param permissions Permissions to check
     * @return true if the player has *at least one* of the permissions provided, false otherwise
     */
    boolean playerHasPermissions(final UUID player, final String... permissions);

    /**
     * Checks if a player has a server permission
     * @param player Player to check
     * @param permissions Permissions to check
     * @return true if the player has *at least one* of the permissions provided, false otherwise
     */
    default boolean playerHasPermissions(final UUID player, final MinecraftPermission... permissions){
        final String[] permissionStrings = new String[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            permissionStrings[i] = permissions[i].getAsString();
        }
        return playerHasPermissions(player, permissionStrings);
    }


    /**
     * Runs a command on the server
     *
     * @param cmdString    Command to execute
     * @return Command response
     */
    String runMCCommand(final String cmdString);



}
