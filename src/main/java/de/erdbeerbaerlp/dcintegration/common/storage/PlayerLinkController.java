package de.erdbeerbaerlp.dcintegration.common.storage;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import de.erdbeerbaerlp.dcintegration.common.Discord;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.*;

@SuppressWarnings("unused")
public class PlayerLinkController {
    /**
     * Path to the old json containing linked players
     */
    public static final File playerLinkedFile = new File(discordDataDir, "LinkedPlayers.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final ArrayList<PlayerLink> playerLinkCache = new ArrayList<>();

    /**
     * Used to (re-)load all links from the database
     */
    public static void loadLinksFromDatabase() {
        playerLinkCache.clear();
        playerLinkCache.addAll(Arrays.asList(discord_instance.linkDatabase.getAllLinks()));
    }

    public static void saveLinksToDatabase() {
        playerLinkCache.forEach((l) -> discord_instance.linkDatabase.addLink(l));
    }


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

        for (final PlayerLink o : getAllLinks()) {
            if (!o.floodgateUUID.isEmpty() && o.floodgateUUID.equals(player.toString())) {
                return true;
            }
        }
        return false;
    }


    private static final String API_URL = "https://api.erdbeerbaerlp.de/dcintegration/link";


    /**
     * Checks if a player has linked their minecraft account with discord
     *
     * @param player {@link UUID} of the player to check
     * @return The player's link status
     */
    public static boolean isJavaPlayerLinked(UUID player) {
        if (!discord_instance.srv.isOnlineMode()) return false;

        for (final PlayerLink o : getAllLinks()) {
            if (!o.mcPlayerUUID.isEmpty() && o.mcPlayerUUID.equals(player.toString())) {
                return true;
            }
        }

        //Check from global linking API
        if (Configuration.instance().linking.globalLinking)
            try {
                final HttpsURLConnection connection = (HttpsURLConnection) new URL(API_URL + "?uuid=" + player.toString().replace("-", "")).openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.setRequestMethod("GET");
                final JsonObject o = gson.fromJson(new JsonReader(new InputStreamReader(connection.getInputStream())), JsonObject.class);
                if (o.has("dcID") && !o.get("dcID").getAsString().isEmpty()) {
                    linkWithoutChecking(o.get("dcID").getAsString(), player);
                    connection.disconnect();
                    return true;
                }


                connection.disconnect();
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
        for (final PlayerLink o : getAllLinks()) {
            if (!o.discordID.isEmpty() && o.discordID.equals(discordID)) {
                return !o.floodgateUUID.isEmpty();
            }
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
        for (final PlayerLink o : getAllLinks()) {
            if (!o.discordID.isEmpty() && o.discordID.equals(discordID)) {
                return !o.mcPlayerUUID.isEmpty();
            }
        }
        //Check from global linking API
        if (Configuration.instance().linking.globalLinking)
            try {
                final HttpsURLConnection connection = (HttpsURLConnection) new URL(API_URL + "?dcID=" + discordID).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                final JsonObject o = gson.fromJson(new JsonReader(new InputStreamReader(connection.getInputStream())), JsonObject.class);
                if (o.has("uuid") && !o.get("uuid").getAsString().isEmpty()) {
                    linkWithoutChecking(discordID, UUID.fromString(o.get("uuid").getAsString().replaceAll(
                            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                            "$1-$2-$3-$4-$5")));
                    connection.disconnect();
                    return true;
                }
                connection.disconnect();
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
        for (final PlayerLink o : getAllLinks()) {
            if (!o.discordID.isEmpty() && !o.mcPlayerUUID.isEmpty() && o.discordID.equals(discordID)) {
                return UUID.fromString(o.mcPlayerUUID);
            }
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
        for (final PlayerLink o : getAllLinks()) {
            if (!o.discordID.isEmpty() && !o.floodgateUUID.isEmpty() && o.discordID.equals(discordID)) {
                return UUID.fromString(o.floodgateUUID);
            }
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
        for (final PlayerLink o : getAllLinks()) {
            if (!o.discordID.isEmpty() && !o.mcPlayerUUID.isEmpty() && o.mcPlayerUUID.equals(player.toString())) {
                return o.discordID;
            }
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
        for (final PlayerLink o : getAllLinks()) {
            if (!o.discordID.isEmpty() && !o.floodgateUUID.isEmpty() && o.floodgateUUID.equals(player.toString())) {
                return o.discordID;
            }
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
        for (final PlayerLink o : getAllLinks()) {
            if (!o.discordID.isEmpty() && (!o.mcPlayerUUID.isEmpty() || !o.floodgateUUID.isEmpty()) && o.discordID.equals(discordID) && (o.mcPlayerUUID.equals(player.toString()) || o.floodgateUUID.equals(player.toString()))) {
                return o.settings;
            }
        }

        return new PlayerSettings();
    }

    public static void migrateToDatabase() {
        LOGGER.info("LinkedPlayers.toml migration started");
        if (!playerLinkedFile.exists()) {
            LOGGER.info("LinkedPlayers.toml migration failed - file does not exist");
            return;
        }
        try {
            final FileReader is = new FileReader(playerLinkedFile);
            final JsonArray a = JsonParser.parseReader(is).getAsJsonArray();
            is.close();
            for (JsonElement e : a) {
                final PlayerLink l = gson.fromJson(e, PlayerLink.class);
                playerLinkCache.add(l);
            }
            saveLinksToDatabase();
            playerLinkedFile.renameTo(new File(playerLinkedFile.getAbsolutePath() + ".backup"));
        } catch (IOException e) {
            LOGGER.info("LinkedPlayers.json migration failed - " + e.getMessage());
        }
        LOGGER.info("LinkedPlayers.json migration complete");
    }

    /**
     * Only to be used for migration<br>
     * Does not add role and nothing, only saves link into json<br><br>
     * Use {@linkplain PlayerLinkController#linkPlayer(String, UUID)} for linking instead
     */
    public static void migrateLinkPlayer(String discordID, UUID player) {
        final PlayerLink link = new PlayerLink();
        link.discordID = discordID;
        link.mcPlayerUUID = player.toString();
        playerLinkCache.add(link);
        saveLinksToDatabase();
    }

    private static PlayerLink getLinkByID(String id) {
        for (PlayerLink playerLink : playerLinkCache) {
            if (playerLink.discordID.equals(id)) return playerLink;
        }
        return null;
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
        return linkWithoutChecking(discordID, player);
    }

    private static boolean linkWithoutChecking(String discordID, UUID player) {
        try {
            final PlayerLink link = isDiscordLinkedBedrock(discordID) ? getUser(discordID, getPlayerFromDiscord(discordID)) : new PlayerLink();
            link.discordID = discordID;
            link.mcPlayerUUID = player.toString();
            final boolean ignoringMessages = discord_instance.ignoringPlayers.contains(player);
            link.settings.ignoreDiscordChatIngame = ignoringMessages;
            if (ignoringMessages) discord_instance.ignoringPlayers.remove(player);

            if (isDiscordLinkedBedrock(discordID)) { //Remove previous occurences
                final PlayerLink linkByID = getLinkByID(discordID);
                if (linkByID != null) {
                    playerLinkCache.remove(linkByID);
                }
            }
            playerLinkCache.add(link);
            saveLinksToDatabase();
            discord_instance.callEventC((e) -> e.onPlayerLink(player, discordID));
            final Guild guild = discord_instance.getChannel().getGuild();
            final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
            final Member member = guild.retrieveMemberById(PlayerLinkController.getDiscordFromPlayer(UUID.fromString(link.mcPlayerUUID))).complete();
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
            final PlayerLink link = isDiscordLinkedJava(discordID) ? getUser(discordID, getPlayerFromDiscord(discordID)) : new PlayerLink();
            link.discordID = discordID;
            link.floodgateUUID = bedrockPlayer.toString();
            final boolean ignoringMessages = discord_instance.ignoringPlayers.contains(bedrockPlayer);
            link.settings.ignoreDiscordChatIngame = ignoringMessages;
            if (ignoringMessages) discord_instance.ignoringPlayers.remove(bedrockPlayer);
            if (isDiscordLinkedJava(discordID)) { //Remove previous occurences
                final PlayerLink linkByID = getLinkByID(discordID);
                if (linkByID != null) {
                    playerLinkCache.remove(linkByID);
                }
            }
            playerLinkCache.add(link);
            saveLinksToDatabase();
            discord_instance.callEventC((e) -> e.onBedrockPlayerLink(bedrockPlayer, discordID));
            final Guild guild = discord_instance.getChannel().getGuild();
            final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
            final Member member = guild.retrieveMemberById(PlayerLinkController.getDiscordFromPlayer(UUID.fromString(link.mcPlayerUUID))).complete();
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
                final PlayerLink link = isDiscordLinkedBedrock(discordID) ? getBedrockUser(discordID, player) : getUser(discordID, player);
                if (link == null) return false;
                removePlayerLink(discordID);
                link.settings = settings;
                playerLinkCache.add(link);
                saveLinksToDatabase();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        return false;
    }

    private static void removePlayerLink(String discordID) {
        playerLinkCache.removeIf(link -> link.discordID.equals(discordID));
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

        for (final PlayerLink o : getAllLinks()) {
            if (o.discordID != null && o.discordID.equals(discordID)) {
                removePlayerLink(o.discordID);
                discord_instance.linkDatabase.removeLink(o.discordID);
                discord_instance.callEventC((a) -> a.onPlayerUnlink(UUID.fromString(o.mcPlayerUUID), discordID));
                try {
                    final Guild guild = discord_instance.getChannel().getGuild();
                    guild.retrieveMemberById(discordID).submit().thenAccept((member) -> {
                        final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
                        if (member.getRoles().contains(linkedRole))
                            guild.removeRoleFromMember(member, linkedRole).queue();
                    });
                } catch (ErrorResponseException ignored) {
                }
                return true;
            }
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
        for (final PlayerLink l : getAllLinks()) {
            if (l.discordID.equals(discordID) && l.mcPlayerUUID.equals(player.toString()))
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

    private static PlayerLink getBedrockUser(String discordID, UUID bedrockPlayer) throws IOException {
        if (!discord_instance.srv.isOnlineMode()) return null;
        for (final PlayerLink l : getAllLinks()) {
            if (l.discordID.equals(discordID) && l.floodgateUUID.equals(bedrockPlayer.toString()))
                return l;
        }
        return null;
    }

    public static PlayerLink[] getAllLinks() {
        return playerLinkCache.toArray(new PlayerLink[0]);
    }
}
