package de.erdbeerbaerlp.dcintegration.common.util;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;

import java.io.*;
import java.util.stream.Collectors;

public class DownloadSourceChecker {

    /**
     * Wildcards for trusted sources
     */
    public static final String[] trustedSources = new String[]{
            "mediafiles.forgecdn.net/files/",
            "cdn-raw.modrinth.com/data/rbJ7eS5V/versions",
            "erdbeerbaerlp.de"
    };

    /**
     * Checks if the mod was downloaded from a trusted source using NTFS alternate file streams
     * @param f File to check on
     * @return true if file source is trusted or unknown, false if download source is untrusted
     */
    public static boolean checkDownloadSource(File f){
        if(Configuration.instance().general.ignoreFileSource) return true;
        final File file = new File(f.getAbsolutePath()+":Zone.Identifier:$DATA");
        try (BufferedReader bf = new BufferedReader( new FileReader(file))) {
            for (String line : bf.lines().collect(Collectors.toList())) {
                if (line.contains("HostUrl=")) {
                    for (final String s : trustedSources) {
                        if (line.contains(s)) return true;
                    }
                }
            }
        }catch(FileNotFoundException ignored){
            return true; //File missing, cannot check
        } catch (IOException e) {
            e.printStackTrace();
            return true; //File unreadable, cannot check
        }
        return false;
    }
}
