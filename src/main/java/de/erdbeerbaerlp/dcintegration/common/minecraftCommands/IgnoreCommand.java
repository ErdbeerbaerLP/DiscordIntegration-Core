package de.erdbeerbaerlp.dcintegration.common.minecraftCommands;

import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class IgnoreCommand implements MCSubCommand {
    @Override
    public String getName() {
        return "ignore";
    }

    @Override
    public Component execute(String[] params, UUID playerUUID) {
        return Component.text(Variables.discord_instance.togglePlayerIgnore(playerUUID) ? Localization.instance().commands.commandIgnore_unignore : Localization.instance().commands.commandIgnore_ignore);
    }

    @Override
    public CommandType getType() {
        return CommandType.PLAYER_ONLY;
    }

    @Override
    public boolean needsOP() {
        return false;
    }
}
