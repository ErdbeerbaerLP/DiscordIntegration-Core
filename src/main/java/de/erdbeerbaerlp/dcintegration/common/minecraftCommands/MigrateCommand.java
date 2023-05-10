package de.erdbeerbaerlp.dcintegration.common.minecraftCommands;

import com.google.gson.*;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.database.JSONInterface;
import net.kyori.adventure.text.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

public class MigrateCommand implements MCSubCommand{
    @Override
    public String getName() {
        return "migrate";
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public Component execute(String[] params, UUID playerUUID) {
        final Gson gson = new GsonBuilder().create();
        DiscordIntegration.LOGGER.info("LinkedPlayers.toml migration started");
        if (!JSONInterface.jsonFile.exists()) {
            DiscordIntegration.LOGGER.info("LinkedPlayers.toml migration failed - file does not exist");
            return Component.empty();
        }
        try {
            final FileReader is = new FileReader(JSONInterface.jsonFile);
            final JsonArray a = JsonParser.parseReader(is).getAsJsonArray();
            is.close();
            for (JsonElement e : a) {
                final PlayerLink l = gson.fromJson(e, PlayerLink.class);
                LinkManager.addLink(l);
            }
            LinkManager.save();
            JSONInterface.jsonFile.renameTo(new File(JSONInterface.jsonFile.getAbsolutePath() + ".backup"));
        } catch (IOException e) {
            DiscordIntegration.LOGGER.info("LinkedPlayers.json migration failed - " + e.getMessage());
        }
        DiscordIntegration.LOGGER.info("LinkedPlayers.json migration complete");
        return Component.empty();
    }

    @Override
    public CommandType getType() {
        return CommandType.CONSOLE_ONLY;
    }

    @Override
    public boolean needsOP() {
        return true;
    }
}
