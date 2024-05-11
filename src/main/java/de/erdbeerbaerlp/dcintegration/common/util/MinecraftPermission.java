package de.erdbeerbaerlp.dcintegration.common.util;

public enum MinecraftPermission {
    USER("user", true, "Default user permissions package"),
    ADMIN("admin", false, "Admin permissions package, unlocks everything"),
    SEMD_MESSAGES("messages.send", false, "Allows users to send messages to discord"),
    READ_MESSAGES("messages.read", false, "Allows users to read messages from discord"),
    BYPASS_WHITELIST("whitelist.bypass", false, "Bypasses discord whitelist"),
    RUN_DISCORD_COMMAND("command", false, "Allows the usage of /discord's user commands"),
    RUN_DISCORD_COMMAND_ADMIN("command.admin", false, "Allows the usage of /discord's admin sub commands");

    private final String permission, description;
    private final boolean defaultValue;

    MinecraftPermission(final String perm, final boolean defaultValue, String description){
        permission = "dcintegration."+perm;
        this.defaultValue = defaultValue;
        this.description = description;
    };

    public String getAsString() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }
}
