package de.erdbeerbaerlp.dcintegration.common.storage.linking.database;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Default JSON database implementation
 */
public class JSONInterface extends DBInterface {

    public static final File jsonFile = new File(DiscordIntegration.discordDataDir, "LinkedPlayers.json");

    @Override
    public void connect() {
        //Not required for json files
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void initialize() {
        try {
            if (!jsonFile.getParentFile().exists())
                jsonFile.getParentFile().mkdirs();
            if (!jsonFile.exists()) {
                jsonFile.createNewFile();
                writeJson(new JsonArray());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isConnected() {
        return jsonFile.exists();
    }

    @Override
    public void addLink(final PlayerLink link) {
        DiscordIntegration.LOGGER.debug("JSONInterface addLink | Saving "+link);
        final JsonArray json = getJson();
        DiscordIntegration.LOGGER.debug("JSONInterface addLink | json (old): " + json);
        for (final JsonElement e : json) {
            final PlayerLink o = gson.fromJson(e, PlayerLink.class);
            if (o.discordID.equals(link.discordID) || (o.floodgateUUID != null && !o.floodgateUUID.isEmpty() && o.floodgateUUID.equals(link.floodgateUUID)) || (o.mcPlayerUUID != null && !o.mcPlayerUUID.isEmpty() && o.mcPlayerUUID.equals(link.mcPlayerUUID))) {
                json.remove(e);
                DiscordIntegration.LOGGER.debug("JSONInterface addLink | Removing old link from json "+o.discordID);
                break;
            }
        }
        json.add(gson.toJsonTree(link).getAsJsonObject());
        DiscordIntegration.LOGGER.debug("JSONInterface addLink | json (new): " + json);
        try {
            writeJson(json);
            DiscordIntegration.LOGGER.debug("JSONInterface addLink | Written to File");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void removeLink(final String id) {
        for (final JsonElement e : getJson()) {
            final PlayerLink o = gson.fromJson(e, PlayerLink.class);
            if (o.discordID != null && o.discordID.equals(id)) {
                final JsonArray json = getJson();
                json.remove(e);
                try {
                    writeJson(json);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }
            }
        }

    }

    @Override
    public PlayerLink[] getAllLinks() {
        return gson.fromJson(getJson(), PlayerLink[].class);
    }

    private static JsonArray getJson() {
        final FileReader is;
        try {
            is = new FileReader(jsonFile);
            final JsonArray a = JsonParser.parseReader(is).getAsJsonArray();
            is.close();
            return a;
        } catch (IOException e) {
            return new JsonArray();
        }
    }

    private void writeJson(JsonArray json) throws IOException {
        final File tempFile = new File(jsonFile.getAbsolutePath() + ".tmp");
        try (
            FileOutputStream fos = new FileOutputStream(tempFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos)
        ){
            gson.toJson(json, writer);
            writer.flush();
        }
        Files.move(
                tempFile.toPath(),
                jsonFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
        );
    }
}
