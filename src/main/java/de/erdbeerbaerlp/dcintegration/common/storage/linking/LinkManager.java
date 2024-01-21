package de.erdbeerbaerlp.dcintegration.common.storage.linking;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class LinkManager {

    private static final String API_URL = "https://api.erdbeerbaerlp.de/dcintegration/link";
    private static ArrayList<PlayerLink> linkCache = new ArrayList<>();

    /**
     * Player UUID cache for players not on global linking API
     */
    private static final ArrayList<String> nonexistentPlayerUUIDs = new ArrayList<>();

    /**
     * Pending /discord link requests
     */
    public static final HashMap<Integer, KeyValue<Instant, UUID>> pendingLinks = new HashMap<>();
    /**
     * Pending /discord link requests for floodgate users
     */
    public static final HashMap<Integer, KeyValue<Instant, UUID>> pendingBedrockLinks = new HashMap<>();

    public static void load() {
        if (Configuration.instance().linking.enableLinking) {
            linkCache = new ArrayList<>(Arrays.asList(DiscordIntegration.INSTANCE.getDatabaseInterface().getAllLinks()));
            DiscordIntegration.LOGGER.debug("LinkManager load | cache: " + linkCache);
        }
    }

    public static void save() {
        if (Configuration.instance().linking.enableLinking)
            linkCache.forEach((l) -> DiscordIntegration.INSTANCE.getDatabaseInterface().addLink(l));
    }

    /**
     * Checks Global Linking API and stores player in local database if it exists
     *
     * @param uuid Player UUID to check
     * @return true if link exists and was created
     */
    public static boolean checkGlobalAPI(final UUID uuid) {
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) return false;
        if (!Configuration.instance().linking.enableLinking) return false;
        if (!Configuration.instance().linking.globalLinking) return false;
        if (nonexistentPlayerUUIDs.contains(uuid.toString())) return false;
        if (isJavaPlayerLinked(uuid)) return true;
        try {
            final HttpsURLConnection connection = (HttpsURLConnection) new URL(API_URL + "?uuid=" + uuid.toString().replace("-", "")).openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setRequestMethod("GET");
            final JsonObject o = DiscordIntegration.gson.fromJson(new JsonReader(new InputStreamReader(connection.getInputStream())), JsonObject.class);
            if (o.has("dcID") && !o.get("dcID").getAsString().isEmpty()) {
                connection.disconnect();
                if (addLink(new PlayerLink(o.get("dcID").getAsString(), uuid.toString(), "", o.get("settings") == null ? new PlayerSettings() : DiscordIntegration.gson.fromJson(o.get("settings"), PlayerSettings.class)))) {
                    save();
                    return true;
                }
            }
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        nonexistentPlayerUUIDs.add(uuid.toString());
        return false;

    }

    public static ArrayList<PlayerLink> getAllLinks() {
        return linkCache;
    }

    /**
     * Unlinks a player from the local database
     *
     * @param discordID discord ID of the player to unlink
     * @return true if the unlink process was successful
     */
    public static boolean unlinkPlayer(String discordID) {
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) return false;
        if (!Configuration.instance().linking.enableLinking) return false;
        linkCache.removeIf(link -> link.discordID.equals(discordID));
        DiscordIntegration.INSTANCE.getDatabaseInterface().removeLink(discordID);
        return true;
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
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode() || player.equals(DiscordIntegration.dummyUUID))
            return false;
        if (isDiscordUserLinkedToJava(discordID) || isPlayerLinked(player))
            throw new IllegalArgumentException("One link side already exists");

        DiscordIntegration.LOGGER.info("LinkManager linkPlayer | discordID:" + discordID + ", player:" + player);
        return addLink(new PlayerLink(discordID, player.toString(), "", new PlayerSettings()));
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
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode() || bedrockPlayer.equals(DiscordIntegration.dummyUUID))
            return false;
        if (isDiscordUserLinkedToBedrock(discordID) || isPlayerLinked(bedrockPlayer))
            throw new IllegalArgumentException("One link side already exists");
        final boolean ignoringMessages = DiscordIntegration.INSTANCE.ignoringPlayers.contains(bedrockPlayer);
        final PlayerLink link = isDiscordUserLinkedToJava(discordID) ? getLink(discordID, null) : new PlayerLink(discordID, "", bedrockPlayer.toString(), new PlayerSettings());
        link.floodgateUUID = bedrockPlayer.toString();
        link.settings.ignoreDiscordChatIngame = ignoringMessages;
        if (ignoringMessages) DiscordIntegration.INSTANCE.ignoringPlayers.remove(bedrockPlayer);
        return addLink(link);
    }

    /**
     * Checks if a player has linked their minecraft account with discord
     *
     * @param player {@link UUID} of the player to check
     * @return The player's link status
     */
    public static boolean isJavaPlayerLinked(UUID player) {
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) return false;
        if (!Configuration.instance().linking.enableLinking) return false;
        for (final PlayerLink o : getAllLinks()) {
            if (o.mcPlayerUUID != null && !o.mcPlayerUUID.isEmpty() && o.mcPlayerUUID.equals(player.toString())) {
                return true;
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
    public static boolean isDiscordUserLinkedToJava(String discordID) {
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) return false;
        if (!Configuration.instance().linking.enableLinking) return false;
        for (final PlayerLink o : getAllLinks()) {
            if (!o.discordID.isEmpty() && o.discordID.equals(discordID)) {
                return !(o.mcPlayerUUID == null || o.mcPlayerUUID.isEmpty());
            }
        }
        return false;
    }

    /**
     * Checks if a user has linked their discord account with minecraft
     *
     * @param discordID The discord ID to check
     * @return The user's link status
     */
    public static boolean isDiscordUserLinked(String discordID) {
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) return false;
        if (!Configuration.instance().linking.enableLinking) return false;
        if (isDiscordUserLinkedToJava(discordID)) return true;
        return isDiscordUserLinkedToBedrock(discordID);
    }

    /**
     * Checks if a user has linked their discord account with minecraft bedrock
     *
     * @param discordID The discord ID to check
     * @return The user's link status
     */
    public static boolean isDiscordUserLinkedToBedrock(String discordID) {
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) return false;
        if (!Configuration.instance().linking.enableLinking) return false;
        for (final PlayerLink o : getAllLinks()) {
            if (!o.discordID.isEmpty() && o.discordID.equals(discordID)) {
                return o.floodgateUUID != null && !o.floodgateUUID.isEmpty();
            }
        }
        return false;
    }


    /**
     * Checks if a player has linked their minecraft account with discord
     *
     * @param player {@link UUID} of the player to check
     * @return The player's link status
     */
    public static boolean isPlayerLinked(UUID player) {
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) return false;
        if (!Configuration.instance().linking.enableLinking) return false;
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
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) return false;
        if (!Configuration.instance().linking.enableLinking) return false;

        for (final PlayerLink o : getAllLinks()) {
            if (o.floodgateUUID != null && !o.floodgateUUID.isEmpty() && o.floodgateUUID.equals(player.toString())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Adds a player link to the local database
     *
     * @param l PlayerLink object to save to the database
     * @return true if successful
     */
    public static boolean addLink(final PlayerLink l) {
        if (!Configuration.instance().linking.enableLinking) return false;
        if (l.discordID == null) return false;
        PlayerLink tmp = null;
        for (final PlayerLink link : getAllLinks()) {
            if (link.discordID.equals(l.discordID) || (link.mcPlayerUUID != null && !link.mcPlayerUUID.isEmpty() && link.mcPlayerUUID.equals(l.mcPlayerUUID)) || (link.floodgateUUID != null && !link.floodgateUUID.isEmpty() && link.floodgateUUID.equals(l.floodgateUUID)))
                tmp = link;
        }
        if (tmp != null) linkCache.remove(tmp);
        DiscordIntegration.LOGGER.debug("LinkManager addLink | tmp:" + tmp + ", l:" + l + ", linkCache:" + linkCache);
        linkCache.add(l);
        return true;
    }

    /**
     * Gets the player link from one of the given parameters, if it exists
     *
     * @param discordID Discord User ID of the user
     * @param uuid      Minecraft UUID of the player
     * @return PlayerLink object
     */
    public static PlayerLink getLink(final String discordID, final UUID uuid) {
        if (!Configuration.instance().linking.enableLinking) return null;
        if (uuid == null && discordID == null) return null;
        if (discordID != null) {
            for (final PlayerLink l : getAllLinks()) {
                if (l.discordID.equals(discordID))
                    return l;
            }
        }
        if (uuid != null) {
            for (final PlayerLink l : getAllLinks()) {
                if (isFloodgateUUID(uuid) ? l.floodgateUUID.equals(uuid.toString()) : l.mcPlayerUUID.equals(uuid.toString()))
                    return l;
            }

        }
        return null;
    }

    private static boolean isFloodgateUUID(final UUID uuid) {
        return uuid.getMostSignificantBits() == 0;
    }


    /**
     * Generates or gets a unique link number for a player
     *
     * @param uniqueID The player's {@link UUID} to generate the number for
     * @return Link number for this player
     */
    public static int genLinkNumber(UUID uniqueID) {
        return genLinkNumber(uniqueID, pendingLinks);
    }

    /**
     * Generates or gets a unique link number for a player
     *
     * @param uniqueID The player's {@link UUID} to generate the number for
     * @return Link number for this player
     */
    public static int genBedrockLinkNumber(UUID uniqueID) {
        return genLinkNumber(uniqueID, pendingBedrockLinks);
    }

    private static int genLinkNumber(UUID uniqueID, HashMap<Integer, KeyValue<Instant, UUID>> pendingBedrockLinks) {
        final AtomicInteger r = new AtomicInteger(-1);
        pendingBedrockLinks.forEach((k, v) -> {
            if (v.getValue().equals(uniqueID))
                r.set(k);
        });
        if (r.get() != -1) return r.get();
        do {
            r.set(new Random().nextInt(99999));
        } while (pendingBedrockLinks.containsKey(r.get()));
        pendingBedrockLinks.put(r.get(), new DefaultKeyValue<>(Instant.now(), uniqueID));
        return r.get();
    }
}
