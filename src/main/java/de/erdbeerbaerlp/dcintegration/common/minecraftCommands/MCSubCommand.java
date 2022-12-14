package de.erdbeerbaerlp.dcintegration.common.minecraftCommands;

import net.kyori.adventure.text.Component;

import java.util.UUID;

public interface MCSubCommand {
    String getName();
    Component execute(String[] params, UUID playerUUID);
    CommandType getType();
    boolean needsOP();

    enum CommandType{
        BOTH,PLAYER_ONLY,CONSOLE_ONLY;
    }
}
