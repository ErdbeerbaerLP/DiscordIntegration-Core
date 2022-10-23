package de.erdbeerbaerlp.dcintegration.common.addon;

@SuppressWarnings({"unused", "FieldMayBeFinal", "FieldCanBeLocal"})
public class DiscordAddonMeta {
    /**
     * Path to the main class of the addon<br>
     * Example: {@code com.example.exampleaddon.ExampleAddon}<br><br>
     * <b>If this parameter is missing, your addon will not load</b>
     */
    private String classPath;

    /**
     * Name of the addon
     */
    private String name;
    /**
     * Version of the addon
     */
    private String version;
    /**
     * Author of the Addon (optional)
     */
    private String author = "Unknown";
    /**
     * Description of the Addon (optional)
     */
    private String description = "";

    public String getClassPath() {
        return classPath;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }
}
