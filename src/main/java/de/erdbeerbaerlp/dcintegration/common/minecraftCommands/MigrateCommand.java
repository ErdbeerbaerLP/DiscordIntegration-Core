package de.erdbeerbaerlp.dcintegration.common.minecraftCommands;

import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class MigrateCommand implements MCSubCommand{
    @Override
    public String getName() {
        return "migrate";
    }

    @Override
    public Component execute(String[] params, UUID playerUUID) {
        PlayerLinkController.migrateToDatabase();
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
