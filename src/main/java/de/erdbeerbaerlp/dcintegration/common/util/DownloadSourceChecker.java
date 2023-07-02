package de.erdbeerbaerlp.dcintegration.common.util;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;

@SuppressWarnings("unused")
public class DownloadSourceChecker {

    /**
     * Wildcards for trusted sources
     */
    public static final ArrayList<String> trustedSources = new ArrayList<>();

    /**
     * Checks if the mod was downloaded from a trusted source using NTFS alternate file streams
     *
     * @param f File to check on
     * @return true if file source is trusted or unknown, false if download source is untrusted
     */
    public static boolean checkDownloadSource(File f) {
        if (Configuration.instance().general.ignoreFileSource) return true;
        try {
            final HttpsURLConnection url = (HttpsURLConnection) new URL("https://api.erdbeerbaerlp.de/trusted-urls.txt").openConnection();
            url.setConnectTimeout(3000);
            url.setReadTimeout(3000);
            url.connect();
            if(url.getResponseCode() != 200) return true; //Ignore on error

            // Read all lines and save to array
            final BufferedReader reader = new BufferedReader(new InputStreamReader(url.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                trustedSources.add(line);
            }

            // Close reader
            reader.close();
            url.disconnect();
        } catch (IOException e) {
            return true; //Ignore on error
        }
        final File file = new File(f.getAbsolutePath() + ":Zone.Identifier:$DATA");
        try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
            for (String line : bf.lines().toList()) {
                if (line.contains("HostUrl=")) {
                    for (final String s : trustedSources) {
                        if (line.contains(s)) return true;
                    }
                }
            }
        } catch (FileNotFoundException ignored) {
            return true; //File missing, cannot check
        } catch (IOException e) {
            e.printStackTrace();
            return true; //File unreadable, cannot check
        }
        return false;
    }
}
