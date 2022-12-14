package de.erdbeerbaerlp.dcintegration.common.minecraftCommands;

import de.erdbeerbaerlp.dcintegration.common.addon.AddonLoader;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.UUID;

public class ReloadCommand implements MCSubCommand{
    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public Component execute(String[] params, UUID playerUUID) {
        try {
            Configuration.instance().loadConfig();
        } catch (IOException e) {
            System.err.println("Config loading failed");
            e.printStackTrace();
        }
        AddonLoader.reloadAll();
        return Component.text(Localization.instance().commands.configReloaded);
    }

    @Override
    public CommandType getType() {
        return CommandType.BOTH;
    }

    @Override
    public boolean needsOP() {
        return true;
    }
}
