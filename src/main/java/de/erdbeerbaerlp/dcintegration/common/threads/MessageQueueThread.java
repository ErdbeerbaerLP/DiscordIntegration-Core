package de.erdbeerbaerlp.dcintegration.common.threads;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;

import java.util.ArrayList;
import java.util.HashMap;

public class MessageQueueThread extends Thread {
    private final DiscordIntegration dc;
    public static final HashMap<String, ArrayList<String>> messages = new HashMap<>();
    public MessageQueueThread(final DiscordIntegration dc) {
        this.dc = dc;
        setName("Discord Integration - Message Queue");
        setDaemon(true);
    }

    @Override
    public void run() {
        while (true) {

            if (!messages.isEmpty()) {
                messages.forEach((channel, msgs) -> {
                    StringBuilder s = new StringBuilder();
                    for (final String msg : msgs)
                        s.append(msg).append("\n");
                    dc.sendMessage(s.toString().trim(), dc.getChannel(channel));
                });
                messages.clear();
            }
            try {
                //noinspection BusyWait
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }
        }

    }
}