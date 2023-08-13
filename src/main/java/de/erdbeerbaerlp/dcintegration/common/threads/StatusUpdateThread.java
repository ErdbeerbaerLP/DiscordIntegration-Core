package de.erdbeerbaerlp.dcintegration.common.threads;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import org.apache.commons.collections4.KeyValue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class StatusUpdateThread extends Thread {
    private final DiscordIntegration dc;

    public StatusUpdateThread(final DiscordIntegration dc) {
        this.dc = dc;
        setName("Discord Integration - Status updater and link cleanup");
        setDaemon(true);
    }

    @Override
    public void run() {
        final JDA jda = dc.getJDA();
        while (true) {
            if (jda != null) {
                String game;
                if (dc.getServerInterface().getOnlinePlayers() == 1 && !Configuration.instance().general.botStatusNameSingular.isBlank()) {
                    game = Configuration.instance().general.botStatusNameSingular
                            .replace("%online%", String.valueOf(dc.getServerInterface().getOnlinePlayers()))
                            .replace("%max%", String.valueOf(dc.getServerInterface().getMaxPlayers()));
                } else if (dc.getServerInterface().getOnlinePlayers() == 0 && !Configuration.instance().general.botStatusNameEmpty.isBlank()) {
                    game = Configuration.instance().general.botStatusNameEmpty
                            .replace("%online%", String.valueOf(dc.getServerInterface().getOnlinePlayers()))
                            .replace("%max%", String.valueOf(dc.getServerInterface().getMaxPlayers()));
                } else {
                    game = Configuration.instance().general.botStatusName
                            .replace("%online%", String.valueOf(dc.getServerInterface().getOnlinePlayers()))
                            .replace("%max%", String.valueOf(dc.getServerInterface().getMaxPlayers()));
                }
                switch (Configuration.instance().general.botStatusType) {
                    case DISABLED:
                        break;
                    case LISTENING:
                        jda.getPresence().setActivity(Activity.listening(game));
                        break;
                    case PLAYING:
                        jda.getPresence().setActivity(Activity.playing(game));
                        break;
                    case WATCHING:
                        jda.getPresence().setActivity(Activity.watching(game));
                        break;
                    case COMPETING:
                        jda.getPresence().setActivity(Activity.competing(game));
                        break;
                    case STREAMING:
                        jda.getPresence().setActivity(Activity.streaming(game, Configuration.instance().general.streamingURL)); //URL is required to show up as "Streaming"
                        break;
                }
            }
            // Removing of expired numbers
            final ArrayList<Integer> remove = new ArrayList<>();
            //clearLinks(remove, pendingLinks);
            //clearLinks(remove, pendingBedrockLinks); TODO
            remove.clear();
            try {
                //noinspection BusyWait
                sleep(1000);
            } catch (InterruptedException e) {
                return;
            }

        }
    }

    private void clearLinks(ArrayList<Integer> remove, HashMap<Integer, KeyValue<Instant, UUID>> pendingLinks) {
        pendingLinks.forEach((k, v) -> {
            final Instant now = Instant.now();
            Duration d = Duration.between(v.getKey(), now);
            if (d.toMinutes() > 10) remove.add(k);
        });
        for (int i : remove)
            pendingLinks.remove(i);
    }
}