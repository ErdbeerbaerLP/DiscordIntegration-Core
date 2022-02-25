package de.erdbeerbaerlp.dcintegration.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UpdateChecker {


    /**
     * Checks for updates and prints the
     */
    public static void runUpdateCheck(String url) {
        if (!Configuration.instance().general.enableUpdateChecker) return;
        final Thread thread = new Thread(() -> {
            final StringBuilder changelog = new StringBuilder();
            try {
                final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                final InputStreamReader r = new InputStreamReader(conn.getInputStream());
                final JsonArray parse = JsonParser.parseReader(r).getAsJsonArray();
                if (parse == null) {
                    System.err.println("Could not check for updates");
                    return;
                }
                final AtomicBoolean shouldNotify = new AtomicBoolean(false);
                final AtomicInteger versionsBehind = new AtomicInteger();
                parse.forEach((elm) -> {
                    if (elm != null && elm.isJsonObject()) {
                        final JsonObject versionDetails = elm.getAsJsonObject();
                        final String version = versionDetails.get("version").getAsString();
                        try {
                            if (Integer.parseInt(version.replace(".", "")) > Integer.parseInt(Variables.VERSION.replace(".", ""))) {
                                versionsBehind.getAndIncrement();
                                changelog.append("\n").append(version).append(":\n").append(versionDetails.get("changelog").getAsString()).append("\n");
                                if (!shouldNotify.get()) {
                                    if (ReleaseType.getFromName(versionDetails.get("type").getAsString()).value >= Configuration.instance().general.updateCheckerMinimumReleaseType.value)
                                        shouldNotify.set(true);
                                }
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                });
                final String changelogString = changelog.toString();
                if (shouldNotify.get()) {
                    System.out.println("[Discord Integration] Updates available! You are " + versionsBehind.get() + " version" + (versionsBehind.get() == 1 ? "" : "s") + " behind\nChangelog since last update:\n" + changelogString);
                }
            } catch (IOException e) {
                System.out.println("Could not check for updates");
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.setName("Discord Integration - Update checker");
        thread.start();

    }

    public enum ReleaseType {
        alpha(0), beta(1), release(2);
        public final int value;

        ReleaseType(int val) {
            this.value = val;
        }

        public static ReleaseType getFromName(String name) {
            for (ReleaseType t : values()) {
                if (name.equalsIgnoreCase(t.name())) return t;
            }
            return ReleaseType.beta;
        }
    }
}
