package de.erdbeerbaerlp.dcintegration.common.storage;

import com.google.gson.*;
import de.erdbeerbaerlp.dcintegration.common.Discord;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.io.*;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.*;

@SuppressWarnings("unused")
public class PlayerLinkController {
    /**
     * Path to the json containing linked players
     */
    private static final File playerLinkedFile = new File(discordDataDir, "LinkedPlayers.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();


    /**
     * Checks if a player has linked their minecraft account with discord
     *
     * @param player {@link UUID} of the player to check
     * @return The player's link status
     */
    public static boolean isPlayerLinked(UUID player) {
        if (!discord_instance.srv.isOnlineMode()) return false;
        if (isJavaPlayerLinked(player)) return true;
        return isBedrockPlayerLinked(player);
    }

    /**
     * Checks if a player has linked their minecraft account with discord
     *
     * @param player {@link UUID} of the player to check
     * @return The player's link status
     */
    public static boolean isBedrockPlayerLinked(UUID player) {
        if (!discord_instance.srv.isOnlineMode()) return false;
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.floodgateUUID.isEmpty() && o.floodgateUUID.equals(player.toString())) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if a player has linked their minecraft account with discord
     *
     * @param player {@link UUID} of the player to check
     * @return The player's link status
     */
    public static boolean isJavaPlayerLinked(UUID player) {
        if (!discord_instance.srv.isOnlineMode()) return false;
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.mcPlayerUUID.isEmpty() && o.mcPlayerUUID.equals(player.toString())) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if a user has linked their discord account with minecraft
     *
     * @param discordID The discord ID to check
     * @return The user's link status
     */
    public static boolean isDiscordLinked(String discordID) {
        if (!discord_instance.srv.isOnlineMode()) return false;
        if (isDiscordLinkedJava(discordID)) return true;
        return isDiscordLinkedBedrock(discordID);
    }

    /**
     * Checks if a user has linked their discord account with minecraft bedrock
     *
     * @param discordID The discord ID to check
     * @return The user's link status
     */
    public static boolean isDiscordLinkedBedrock(String discordID) {
        if (!discord_instance.srv.isOnlineMode()) return false;
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.discordID.isEmpty() && o.discordID.equals(discordID)) {
                    return !o.floodgateUUID.isEmpty();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if a user has linked their discord account with minecraft java
     *
     * @param discordID The discord ID to check
     * @return The user's link status
     */
    public static boolean isDiscordLinkedJava(String discordID) {
        if (!discord_instance.srv.isOnlineMode()) return false;
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.discordID.isEmpty() && o.discordID.equals(discordID)) {
                    return !o.mcPlayerUUID.isEmpty();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gets the linked {@link UUID} of a discord id
     *
     * @param discordID The Discord ID to get the player {@link UUID}  from
     * @return Linked player {@link UUID}, or null if the discord user is not linked
     */

    public static UUID getPlayerFromDiscord(String discordID) {
        if (!discord_instance.srv.isOnlineMode()) return null;
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.discordID.isEmpty() && !o.mcPlayerUUID.isEmpty() && o.discordID.equals(discordID)) {
                    return UUID.fromString(o.mcPlayerUUID);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the linked bedrock {@link UUID} of a discord id
     *
     * @param discordID The Discord ID to get the player {@link UUID}  from
     * @return Linked player {@link UUID}, or null if the discord user is not linked
     */

    public static UUID getBedrockPlayerFromDiscord(String discordID) {
        if (!discord_instance.srv.isOnlineMode()) return null;
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.discordID.isEmpty() && !o.floodgateUUID.isEmpty() && o.discordID.equals(discordID)) {
                    return UUID.fromString(o.floodgateUUID);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the linked discord id of an player {@link UUID}
     *
     * @param player The player's {@link UUID} to get the discord ID from
     * @return Linked discord ID, or null if the player is not linked
     */

    public static String getDiscordFromPlayer(UUID player) {
        if (!discord_instance.srv.isOnlineMode()) return null;
        if (isJavaPlayerLinked(player)) return getDiscordFromJavaPlayer(player);
        if (isBedrockPlayerLinked(player)) return getDiscordFromBedrockPlayer(player);
        return null;
    }

    /**
     * Gets the linked discord id of an player {@link UUID}
     *
     * @param player The player's {@link UUID} to get the discord ID from
     * @return Linked discord ID, or null if the player is not linked
     */

    public static String getDiscordFromJavaPlayer(UUID player) {
        if (!discord_instance.srv.isOnlineMode()) return null;
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.discordID.isEmpty() && !o.mcPlayerUUID.isEmpty() && o.mcPlayerUUID.equals(player.toString())) {
                    return o.discordID;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the linked discord id of an player {@link UUID}
     *
     * @param player The player's {@link UUID} to get the discord ID from
     * @return Linked discord ID, or null if the player is not linked
     */

    public static String getDiscordFromBedrockPlayer(UUID player) {
        if (!discord_instance.srv.isOnlineMode()) return null;
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.discordID.isEmpty() && !o.floodgateUUID.isEmpty() && o.floodgateUUID.equals(player.toString())) {
                    return o.discordID;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the
     *
     * @param discordID The discord id to get the settings from, or null if {@code player} is set
     * @param player    The player's {@link UUID} to get the settings from, or null if {@code discordID} is set
     * @return The {@link PlayerSettings} of the player/discord user, or a default instance of {@link PlayerSettings}, if the user/player is not linked
     * @throws IllegalArgumentException if both arguments were null or the player
     */

    public static PlayerSettings getSettings(String discordID, UUID player) {
        if (!discord_instance.srv.isOnlineMode()) return new PlayerSettings();
        if (player == null && discordID == null) throw new IllegalArgumentException();
        else if (discordID == null) discordID = getDiscordFromPlayer(player);
        else if (player == null)
            player = isDiscordLinkedJava(discordID) ? getPlayerFromDiscord(discordID) : (isDiscordLinkedBedrock(discordID) ? getBedrockPlayerFromDiscord(discordID) : null);
        if (player == null || discordID == null) return new PlayerSettings();
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (!o.discordID.isEmpty() && (!o.mcPlayerUUID.isEmpty() || !o.floodgateUUID.isEmpty()) && o.discordID.equals(discordID) && (o.mcPlayerUUID.equals(player.toString()) || o.floodgateUUID.equals(player.toString()))) {
                    return o.settings;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new PlayerSettings();
    }

    /**
     * Only to be used for migration<br>
     * Does not add role and nothing, only saves link into json<br><br>
     * Use {@linkplain PlayerLinkController#linkPlayer(String, UUID)} for linking instead
     */
    public static void migrateLinkPlayer(String discordID, UUID player) {
        try {
            final JsonArray a = getJson();
            final PlayerLink link = new PlayerLink();
            link.discordID = discordID;
            link.mcPlayerUUID = player.toString();
            a.add(gson.toJsonTree(link));
            saveJSON(a);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Links a discord user ID with a player's {@link UUID}
     *
     * @param discordID Discord ID to link
     * @param player    {@link UUID} to link
     * @return true, if linking was successful
     * @throws IllegalArgumentException if one side is already linked
     */
    @SuppressWarnings({"ConstantConditions", "DuplicatedCode"})
    public static boolean linkPlayer(String discordID, UUID player) throws IllegalArgumentException {
        if (!discord_instance.srv.isOnlineMode() || player.equals(Discord.dummyUUID)) return false;
        if (isDiscordLinkedJava(discordID) || isPlayerLinked(player))
            throw new IllegalArgumentException("One link side already exists");
        try {
            final JsonArray a = getJson();
            final PlayerLink link = isDiscordLinkedBedrock(discordID) ? getUser(discordID, getPlayerFromDiscord(discordID)) : new PlayerLink();
            link.discordID = discordID;
            link.mcPlayerUUID = player.toString();
            final boolean ignoringMessages = discord_instance.ignoringPlayers.contains(player);
            link.settings.ignoreDiscordChatIngame = ignoringMessages;
            if (ignoringMessages) discord_instance.ignoringPlayers.remove(player);

            if (isDiscordLinkedBedrock(discordID)) { //Remove previous occurences
                final JsonElement el = getBedrockUserRaw(discordID, getBedrockPlayerFromDiscord(discordID));
                if (a.contains(el)) {
                    a.remove(el);
                }
            }
            a.add(gson.toJsonTree(link));
            saveJSON(a);
            discord_instance.callEventC((e) -> e.onPlayerLink(player, discordID));
            final Guild guild = discord_instance.getChannel().getGuild();
            final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
            final Member member = discord_instance.getMemberById(Long.valueOf(PlayerLinkController.getDiscordFromPlayer(UUID.fromString(link.mcPlayerUUID))));
            if (linkedRole != null && !member.getRoles().contains(linkedRole))
                guild.addRoleToMember(member, linkedRole).queue();
            if (Configuration.instance().linking.shouldNickname) {
                String playerName = MessageUtils.getNameFromUUID(player);
                String discordName = MessageUtils.getDiscordName(player);
                try {
                    member.modifyNickname(MessageUtils.getNameFromUUID(player)).queue();
                } catch (HierarchyException e) {
                    LOGGER.info("Unable to change nickname of player {} ({}) with higher or equal highest role.", playerName, discordName);
                } catch (InsufficientPermissionException e) {
                    LOGGER.error("Insufficient Permissions. 'Manage Nicknames' permissions required.");
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Links a discord user ID with a player's {@link UUID}
     *
     * @param discordID     Discord ID to link
     * @param bedrockPlayer {@link UUID} to link
     * @return true, if linking was successful
     * @throws IllegalArgumentException if one side is already linked
     */
    @SuppressWarnings({"ConstantConditions", "DuplicatedCode"})
    public static boolean linkBedrockPlayer(String discordID, UUID bedrockPlayer) throws IllegalArgumentException {
        if (!discord_instance.srv.isOnlineMode() || bedrockPlayer.equals(Discord.dummyUUID)) return false;
        if (isDiscordLinkedBedrock(discordID) || isPlayerLinked(bedrockPlayer))
            throw new IllegalArgumentException("One link side already exists");
        try {
            final JsonArray a = getJson();
            final PlayerLink link = isDiscordLinkedJava(discordID) ? getUser(discordID, getPlayerFromDiscord(discordID)) : new PlayerLink();
            link.discordID = discordID;
            link.floodgateUUID = bedrockPlayer.toString();
            final boolean ignoringMessages = discord_instance.ignoringPlayers.contains(bedrockPlayer);
            link.settings.ignoreDiscordChatIngame = ignoringMessages;
            if (ignoringMessages) discord_instance.ignoringPlayers.remove(bedrockPlayer);
            if (isDiscordLinkedJava(discordID)) { //Remove previous occurences
                final JsonElement el = getUserRaw(discordID, getPlayerFromDiscord(discordID));
                if (a.contains(el)) {
                    a.remove(el);
                }
            }
            a.add(gson.toJsonTree(link));
            saveJSON(a);
            discord_instance.callEventC((e) -> e.onBedrockPlayerLink(bedrockPlayer, discordID));
            final Guild guild = discord_instance.getChannel().getGuild();
            final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
            final Member member = discord_instance.getMemberById(Long.valueOf(PlayerLinkController.getDiscordFromPlayer(UUID.fromString(link.mcPlayerUUID))));
            if (linkedRole != null && !member.getRoles().contains(linkedRole))
                guild.addRoleToMember(member, linkedRole).queue();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Updates and saves personal settings
     *
     * @param discordID The discord id to set the settings for, or null if {@code player} is set
     * @param player    The player's {@link UUID} to set the settings for, or null if {@code discordID} is set
     * @param settings  The {@link PlayerSettings} instance to save
     * @return true, if saving was successful
     * @throws NullPointerException if player/user is not linked
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean updatePlayerSettings(String discordID, UUID player, PlayerSettings settings) {
        if (!discord_instance.srv.isOnlineMode()) return false;
        if (player == null && discordID == null) throw new NullPointerException();
        else if (discordID == null) discordID = getDiscordFromPlayer(player);
        else if (player == null)
            player = isDiscordLinkedJava(discordID) ? getPlayerFromDiscord(discordID) : (isDiscordLinkedBedrock(discordID) ? getBedrockPlayerFromDiscord(discordID) : null);
        if (player == null || discordID == null) throw new NullPointerException();
        if (isDiscordLinked(discordID) && isPlayerLinked(player))
            try {
                final JsonArray a = getJson();
                final PlayerLink link = isDiscordLinkedBedrock(discordID) ? getBedrockUser(discordID, player) : getUser(discordID, player);
                for (JsonElement e : a) {
                    final PlayerLink l = gson.fromJson(e, PlayerLink.class);
                    if (l.equals(link)) {
                        a.remove(e);
                        break;
                    }
                }
                if (link == null) return false;
                link.settings = settings;
                a.add(gson.toJsonTree(link));
                saveJSON(a);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        return false;
    }

    /**
     * Writes player links and settings into the json file
     *
     * @param a Json to save
     */
    private static void saveJSON(JsonArray a) throws IOException {
        try (Writer writer = new FileWriter(playerLinkedFile)) {
            gson.toJson(a, writer);
        }
    }

    /**
     * Unlinks a player and discord id
     *
     * @param discordID The discord ID to unlink
     * @return true, if unlinking was successful
     */
    @SuppressWarnings({"UnusedReturnValue", "ConstantConditions"})
    public static boolean unlinkPlayer(String discordID) {
        if (!discord_instance.srv.isOnlineMode()) return false;
        if (!isDiscordLinked(discordID)) return false;
        try {
            for (JsonElement e : getJson()) {
                final PlayerLink o = gson.fromJson(e, PlayerLink.class);
                if (o.discordID != null && o.discordID.equals(discordID)) {
                    final JsonArray json = getJson();
                    json.remove(e);
                    try (Writer writer = new FileWriter(playerLinkedFile)) {
                        gson.toJson(json, writer);
                    }
                    discord_instance.callEventC((a) -> a.onPlayerUnlink(UUID.fromString(o.mcPlayerUUID), discordID));
                    try {

                        final Guild guild = discord_instance.getChannel().getGuild();
                        final Member member = discord_instance.getMemberById(Long.valueOf(discordID));
                        final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
                        if (member.getRoles().contains(linkedRole))
                            guild.removeRoleFromMember(member, linkedRole).queue();

                    } catch (ErrorResponseException ignored) {
                    }
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gets the {@link PlayerLink} instance of the link
     *
     * @param discordID The discord ID of the link
     * @param player    The player {@link UUID} of the link
     * @return The {@link PlayerLink} instance
     */
    @SuppressWarnings("DuplicatedCode")
    private static PlayerLink getUser(String discordID, UUID player) throws IOException {
        if (!discord_instance.srv.isOnlineMode()) return null;
        final JsonArray a = getJson();
        for (JsonElement e : a) {
            final PlayerLink l = gson.fromJson(e, PlayerLink.class);
            if (l.discordID.equals(discordID) && l.mcPlayerUUID.equals(player.toString()))
                return l;
        }
        return null;
    }

    /**
     * Gets the {@link PlayerLink} instance of the link
     *
     * @param discordID The discord ID of the link
     * @param player    The player {@link UUID} of the link
     * @return The {@link PlayerLink} instance
     */
    @SuppressWarnings("DuplicatedCode")

    private static JsonElement getUserRaw(String discordID, UUID player) throws IOException {
        if (!discord_instance.srv.isOnlineMode()) return null;
        final JsonArray a = getJson();
        for (JsonElement e : a) {
            final PlayerLink l = gson.fromJson(e, PlayerLink.class);
            if (l.discordID.equals(discordID) && l.mcPlayerUUID.equals(player.toString()))
                return e;
        }
        return null;
    }

    /**
     * Gets the {@link PlayerLink} instance of the link
     *
     * @param discordID     The discord ID of the link
     * @param bedrockPlayer The player {@link UUID} of the link
     * @return The {@link PlayerLink} instance
     */
    @SuppressWarnings("DuplicatedCode")

    private static PlayerLink getBedrockUser(String discordID, UUID bedrockPlayer) throws IOException {
        if (!discord_instance.srv.isOnlineMode()) return null;
        final JsonArray a = getJson();
        for (JsonElement e : a) {
            final PlayerLink l = gson.fromJson(e, PlayerLink.class);
            if (l.discordID.equals(discordID) && l.floodgateUUID.equals(bedrockPlayer.toString()))
                return l;
        }
        return null;
    }

    /**
     * Gets the {@link PlayerLink} instance of the link
     *
     * @param discordID     The discord ID of the link
     * @param bedrockPlayer The player {@link UUID} of the link
     * @return The {@link PlayerLink} instance
     */
    @SuppressWarnings("DuplicatedCode")

    private static JsonElement getBedrockUserRaw(String discordID, UUID bedrockPlayer) throws IOException {
        if (!discord_instance.srv.isOnlineMode()) return null;
        final JsonArray a = getJson();
        for (JsonElement e : a) {
            final PlayerLink l = gson.fromJson(e, PlayerLink.class);
            if (l.discordID.equals(discordID) && l.floodgateUUID.equals(bedrockPlayer.toString()))
                return e;
        }
        return null;
    }

    /**
     * Loads links and settings from json into an {@link JsonArray}
     *
     * @return {@link JsonArray} containing links and settings
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")

    private static JsonArray getJson() throws IOException, IllegalStateException {
        if (!playerLinkedFile.exists()) {
            playerLinkedFile.createNewFile();
            try (Writer writer = new FileWriter(playerLinkedFile)) {
                gson.toJson(new JsonArray(), writer);
            }
            return new JsonArray();
        }
        final FileReader is = new FileReader(playerLinkedFile);
        final JsonArray a = JsonParser.parseReader(is).getAsJsonArray();
        is.close();
        return a;
    }

    /**
     * Unused for now, might be needed in the future
     *
     * @return All Player links as array or an empty array if parsing the json fails
     */

    public static PlayerLink[] getAllLinks() {
        try {
            return gson.fromJson(getJson(), PlayerLink[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new PlayerLink[0];
    }

}
