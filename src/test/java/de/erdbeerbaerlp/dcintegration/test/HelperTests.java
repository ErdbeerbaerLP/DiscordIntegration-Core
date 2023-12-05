package de.erdbeerbaerlp.dcintegration.test;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.GameType;
import de.erdbeerbaerlp.dcintegration.common.util.UpdateChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class HelperTests {
    @Test
    public void testConfigLoadingAndSaving() throws IOException {
        DiscordIntegration.configFile = File.createTempFile("testConfigLoadingAndSaving","config");
        DiscordIntegration.configFile.deleteOnExit();
        Configuration.instance().loadConfig();
        Assertions.assertEquals(Configuration.instance().general.botStatusType, GameType.CUSTOM);
        Configuration.instance().general.botStatusType = GameType.PLAYING;
        Configuration.instance().saveConfig();
        Configuration.instance().general.botStatusType = GameType.DISABLED;
        Configuration.instance().loadConfig();
        Assertions.assertEquals(Configuration.instance().general.botStatusType, GameType.PLAYING);
    }

    @Test
    public void testUpdateCheck() throws IOException {
        final String url = "https://raw.githubusercontent.com/ErdbeerbaerLP/DiscordIntegration-Forge/1.20.1/update_checker.json";
        DiscordIntegration.configFile = File.createTempFile("testUpdateCheck","config");
        DiscordIntegration.configFile.deleteOnExit();
        Configuration.instance().loadConfig();
        Configuration.instance().general.enableUpdateChecker = true;
        Configuration.instance().saveConfig();


        //There is no new version available
        Assertions.assertFalse(UpdateChecker.runUpdateCheckBlocking(url, "9999999.9.9"));

        //There is a new version available
        Assertions.assertTrue(UpdateChecker.runUpdateCheckBlocking(url, "0.0.0"));

        //Test longer version numbers
        Assertions.assertTrue(UpdateChecker.runUpdateCheckBlocking(url, "1.0.2.4"));
        Assertions.assertFalse(UpdateChecker.runUpdateCheckBlocking(url, "999999.0.2.4"));

        //Test snapshot version numbers
        Assertions.assertFalse(UpdateChecker.runUpdateCheckBlocking(url, "9999999.9.9-SNAPSHOT"));

        // == Disabled update checker ==
        Configuration.instance().general.enableUpdateChecker = false;
        Configuration.instance().saveConfig();

        //There is no new version available
        Assertions.assertFalse(UpdateChecker.runUpdateCheckBlocking(url, "9999999.9.9"));

        //There is a new version available
        Assertions.assertFalse(UpdateChecker.runUpdateCheckBlocking(url, "0.0.0"));

    }
}
