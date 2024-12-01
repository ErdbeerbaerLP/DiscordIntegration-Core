package de.erdbeerbaerlp.dcintegration.common.threads;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.TimerTask;
import java.util.UUID;

public class APITestTask extends TimerTask {
    private final DiscordIntegration dc;
    private String url = Configuration.instance().webhook.playerAvatarURL;

    // == Values used for testing the URL
    private static final UUID testUUID = UUID.fromString("210f7275-c79f-44f8-a7a0-7da71c751bb9");
    private static final String testName = "ErdbeerbaerLP";
    // ==

    public APITestTask(final DiscordIntegration dc) {
        this.dc = dc;
    }

    @Override
    public void run() {
        final String urlToTest = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", testUUID.toString()).replace("%uuid_dashless%", testUUID.toString().replace("-", "")).replace("%name%", testName).replace("%randomUUID%", UUID.randomUUID().toString());
        try {
            final HttpURLConnection c = (HttpURLConnection) new URL(urlToTest).openConnection();
            c.connect();
            if(c.getResponseCode() == 200){
                url = Configuration.instance().webhook.playerAvatarURL;
                return;
            }
            c.disconnect();
        } catch (IOException e) {

        }
        url = Configuration.instance().webhook.fallbackAvatarURL;
    }

    public String getSkinURL(){
        return url;
    }
}
