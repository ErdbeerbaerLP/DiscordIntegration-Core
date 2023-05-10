package de.erdbeerbaerlp.dcintegration.common.storage.linking;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class LinkManager {

    private static final String API_URL = "https://api.erdbeerbaerlp.de/dcintegration/link";
    private static final ArrayList<PlayerLink> linkCache = new ArrayList<>();


    /**
     * Pending /discord link requests
     */
    public static final HashMap<Integer, KeyValue<Instant, UUID>> pendingLinks = new HashMap<>();
    /**
     * Pending /discord link requests for floodgate users
     */
    public static final HashMap<Integer, KeyValue<Instant, UUID>> pendingBedrockLinks = new HashMap<>();

    public static void load() {
        linkCache.clear();
        linkCache.addAll(Arrays.asList(DiscordIntegration.INSTANCE.getDatabaseInterface().getAllLinks()));
    }

    public static void save() {
        linkCache.forEach((l) -> DiscordIntegration.INSTANCE.getDatabaseInterface().addLink(l));
    }

    public static ArrayList<PlayerLink> getAllLinks() {
        return linkCache;
    }


    public static boolean unlinkPlayer(String discordID) {
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) return false;
        linkCache.removeIf(link -> link.discordID.equals(discordID));
        DiscordIntegration.INSTANCE.getDatabaseInterface().removeLink(discordID);
        return true;
    }


    /**
     * Checks if a player has linked their minecraft account with discord
     *
     * @param player {@link UUID} of the player to check
     * @return The player's link status
     */
    public static boolean isJavaPlayerLinked(UUID player) {
        if (!DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) return false;

        for (final PlayerLink o : getAllLinks()) {
            if (!o.mcPlayerUUID.isEmpty() && o.mcPlayerUUID.equals(player.toString())) {
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
        for (final PlayerLink o : getAllLinks()) {
            if (!o.discordID.isEmpty() && o.discordID.equals(discordID)) {
                return !o.mcPlayerUUID.isEmpty();
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
        for (final PlayerLink o : getAllLinks()) {
            if (!o.discordID.isEmpty() && o.discordID.equals(discordID)) {
                return !o.floodgateUUID.isEmpty();
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

        for (final PlayerLink o : getAllLinks()) {
            if (!o.floodgateUUID.isEmpty() && o.floodgateUUID.equals(player.toString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean addLink(final PlayerLink l) {
        if (l.discordID == null) return false;
        PlayerLink tmp = null;
        for (final PlayerLink link : getAllLinks()) {
            if (link.discordID.equals(l.discordID) || link.mcPlayerUUID.equals(l.mcPlayerUUID) || link.floodgateUUID.equals(l.floodgateUUID))
                tmp = link;
        }
        if (tmp != null) linkCache.remove(tmp);
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
