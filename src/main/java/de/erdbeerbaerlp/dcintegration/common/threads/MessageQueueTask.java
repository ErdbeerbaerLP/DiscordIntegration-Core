package de.erdbeerbaerlp.dcintegration.common.threads;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;

public class MessageQueueTask extends TimerTask {
    private final DiscordIntegration dc;
    public static final HashMap<String, ArrayList<String>> messages = new HashMap<>();

    public MessageQueueTask(final DiscordIntegration dc) {
        this.dc = dc;
    }

    @Override
    public void run() {
        if (!messages.isEmpty()) {
            messages.forEach((channel, msgs) -> {
                StringBuilder s = new StringBuilder();
                for (final String msg : msgs)
                    s.append(msg).append("\n");
                dc.sendMessage(s.toString().trim(), dc.getChannel(channel));
            });
            messages.clear();
        }

    }
}