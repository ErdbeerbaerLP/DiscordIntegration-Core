package de.erdbeerbaerlp.dcintegration.common.compat;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class DynmapListener extends DynmapCommonAPIListener {

    private final boolean workaroundEnabled;
    private final DynmapSender sender = new DynmapSender();
    private DynmapCommonAPI api;

    public DynmapListener() {
        this.workaroundEnabled = false;
    }

    public DynmapListener(boolean workaroundEnabled) {

        this.workaroundEnabled = workaroundEnabled;
    }

    @Override
    public void apiEnabled(org.dynmap.DynmapCommonAPI api) {
        this.api = api;
        if (DiscordIntegration.INSTANCE != null)
            DiscordIntegration.INSTANCE.registerEventHandler(sender);
        DiscordIntegration.LOGGER.info("Dynmap listener registered");
    }

    @Override
    public void apiDisabled(org.dynmap.DynmapCommonAPI api) {
        if (DiscordIntegration.INSTANCE != null)
            DiscordIntegration.INSTANCE.unregisterEventHandler(sender);
    }


    @Override
    public boolean webChatEvent(String source, String name, String message) {
        if (!this.workaroundEnabled)
            sendMessage(name, message);
        return super.webChatEvent(source, name, message);
    }

    public void sendMessage(String name, String message) {
        DiscordIntegration.INSTANCE.sendMessage(DiscordIntegration.INSTANCE.getChannel(Configuration.instance().dynmap.dynmapChannelID), Configuration.instance().dynmap.dcMessage.replace("%msg%", message).replace("%sender%", name.isEmpty() ? Configuration.instance().dynmap.unnamed : name), Configuration.instance().dynmap.avatarURL, Configuration.instance().dynmap.name);
    }

    /**
     * Registers this class as an {@link DynmapCommonAPIListener}
     */
    public void register() {
        DynmapCommonAPIListener.register(this);
    }

    public class DynmapSender extends DiscordEventHandler {
        @Override
        public void onDiscordMessagePost(@NotNull MessageReceivedEvent event) {
            if (event.getChannel().getId().equals(Configuration.instance().dynmap.dynmapChannelID.equals("default") ? Configuration.instance().general.botChannel : Configuration.instance().dynmap.dynmapChannelID)) {
                api.sendBroadcastToWeb(Configuration.instance().dynmap.webName.replace("%name#tag%", event.getAuthor().getAsTag()).replace("%name%", event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName()), event.getMessage().getContentDisplay());
            }
        }
    }

}
