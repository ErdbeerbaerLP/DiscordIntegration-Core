package de.erdbeerbaerlp.dcintegration.common.minecraftCommands;

import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class UnlinkCommand implements MCSubCommand {

    @Override
    public String getName() {
        return "unlink";
    }

    @Override
    public Component execute(String[] params, UUID playerUUID) {
        if (LinkManager.unlinkPlayer(params[0]))
            return Component.text("Successfully unlinked");
        else return Component.text("Failed to unlink");
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
