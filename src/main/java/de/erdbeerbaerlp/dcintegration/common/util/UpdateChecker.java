package de.erdbeerbaerlp.dcintegration.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.WorkThread;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class UpdateChecker {


    /**
     * Checks for updates and prints the update message to console
     */
    public static boolean runUpdateCheckBlocking(String url, String versionString) {
        if (!Configuration.instance().general.enableUpdateChecker) return false;
        final StringBuilder changelog = new StringBuilder();
        if (versionString.endsWith("-SNAPSHOT")) {
            DiscordIntegration.LOGGER.info("You are using a development version of the mod. Will not check for updates!");
        } else
            try {
                final Version curVer = Version.parse(versionString);
                final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                final InputStreamReader r = new InputStreamReader(conn.getInputStream());
                final JsonArray parse = JsonParser.parseReader(r).getAsJsonArray();
                if (parse == null) {
                    DiscordIntegration.LOGGER.error("Could not check for updates");
                    return false;
                }
                final AtomicBoolean shouldNotify = new AtomicBoolean(false);
                final AtomicInteger versionsBehind = new AtomicInteger();
                parse.forEach((elm) -> {
                    if (elm != null && elm.isJsonObject()) {
                        final JsonObject versionDetails = elm.getAsJsonObject();
                        final Version version = Version.parse(versionDetails.get("version").getAsString());
                        try {
                            final int n = curVer.compareTo(version);
                            if (n < 0) {
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
                    DiscordIntegration.LOGGER.info("Updates available! You are " + versionsBehind.get() + " version" + (versionsBehind.get() == 1 ? "" : "s") + " behind\nChangelog since last update:\n" + changelogString);
                    return true;
                }
            } catch (IOException e) {
                DiscordIntegration.LOGGER.info("Could not check for updates");
                e.printStackTrace();
            }
        return false;
    }

    /**
     * Checks for updates and prints the update message to console
     */
    public static void runUpdateCheck(String url) {
        if (!Configuration.instance().general.enableUpdateChecker) return;
        WorkThread.executeJob(() -> {
            runUpdateCheckBlocking(url, DiscordIntegration.VERSION);
        });
    }

    /**
     * Types of releases
     */
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