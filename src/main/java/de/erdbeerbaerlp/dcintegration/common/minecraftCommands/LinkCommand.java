package de.erdbeerbaerlp.dcintegration.common.minecraftCommands;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;

import java.awt.*;
import java.util.UUID;

public class LinkCommand implements MCSubCommand {
    @Override
    public String getName() {
        return "link";
    }

    @Override
    public Component execute(String[] params, UUID playerUUID) {
        if (Configuration.instance().linking.enableLinking && DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode() && !Configuration.instance().linking.whitelistMode) {
            if (LinkManager.isPlayerLinked(playerUUID)) {
                return Component.text(Localization.instance().linking.alreadyLinked.replace("%player%", DiscordIntegration.INSTANCE.getJDA().getUserById(LinkManager.getLink(null, playerUUID).discordID).getAsTag())).style(Style.style(TextColors.of(Color.RED)));
            }
            final int r = LinkManager.genLinkNumber(playerUUID);
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
