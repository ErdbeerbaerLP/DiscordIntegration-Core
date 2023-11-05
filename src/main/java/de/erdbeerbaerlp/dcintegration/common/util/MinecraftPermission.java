package de.erdbeerbaerlp.dcintegration.common.util;

public enum MinecraftPermission {
    USER("user"),
    ADMIN("admin"),
    SEMD_MESSAGES("messages.send"),
    READ_MESSAGES("messages.read"),
    BYPASS_WHITELIST("whitelist.bypass"),
    RUN_DISCORD_COMMAND("command"),
    RUN_DISCORD_COMMAND_ADMIN("command.admin");

    private final String permission;

    MinecraftPermission(final String perm){
        permission = "dcintegration."+perm;
    };

    public String getAsString() {
        return permission;
    }
}
