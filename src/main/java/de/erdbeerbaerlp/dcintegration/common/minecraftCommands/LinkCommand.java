package de.erdbeerbaerlp.dcintegration.common.minecraftCommands;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;

import java.awt.*;
import java.util.UUID;

public class LinkCommand implements MCSubCommand{
    @Override
    public String getName() {
        return "link";
    }

    @Override
    public Component execute(String[] params, UUID playerUUID) {
        if (Configuration.instance().linking.enableLinking && Variables.discord_instance.srv.isOnlineMode() && !Configuration.instance().linking.whitelistMode) {
            if (PlayerLinkController.isPlayerLinked(playerUUID)) {
                return Component.text(Localization.instance().linking.alreadyLinked.replace("%player%", Variables.discord_instance.getJDA().getUserById(PlayerLinkController.getDiscordFromPlayer(playerUUID)).getAsTag())).style(Style.style(TextColors.of(Color.RED)));

            }
            final int r = Variables.discord_instance.genLinkNumber(playerUUID);
            return Component.text(Localization.instance().linking.linkMsgIngame.replace("%num%", r + "").replace("%prefix%", "/")).style(Style.style(TextColors.of(Color.ORANGE)).clickEvent(ClickEvent.copyToClipboard("" + r)).hoverEvent(HoverEvent.showText(Component.text(Localization.instance().linking.hoverMsg_copyClipboard))));
        } else {
            return Component.text(Localization.instance().commands.subcommandDisabled).style(Style.style(TextColors.of(Color.RED)));
        }
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
