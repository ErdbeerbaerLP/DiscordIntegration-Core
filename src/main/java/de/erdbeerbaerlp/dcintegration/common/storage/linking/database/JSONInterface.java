package de.erdbeerbaerlp.dcintegration.common.storage.linking.database;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;

import java.io.*;

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
                try (Writer writer = new FileWriter(jsonFile)) {
                    gson.toJson(new JsonArray(), writer);
                }
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
        final JsonArray json = getJson();
        for (final JsonElement e : json) {
            final PlayerLink o = gson.fromJson(e, PlayerLink.class);
            if (o.discordID.equals(link.discordID) || o.floodgateUUID.equals(link.floodgateUUID) || o.mcPlayerUUID.equals(link.mcPlayerUUID)) {
                json.remove(e);
                break;
            }
        }
        json.add(gson.toJson(link));
        try (Writer writer = new FileWriter(jsonFile)) {
            gson.toJson(json, writer);
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
                try (Writer writer = new FileWriter(jsonFile)) {
                    gson.toJson(json, writer);
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

}
