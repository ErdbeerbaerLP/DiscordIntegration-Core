package de.erdbeerbaerlp.dcintegration.common.compat;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.UUID;

public class LuckpermsUtils {
    public static boolean uuidHasPermission(String permission, UUID uuid) {
        final User user = LuckPermsProvider.get().getUserManager().getUser(uuid);
        if(user != null) return userHasPermission(permission,user);
        return false;
    }
    public static boolean userHasPermission(String permission, User user) {
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }
}
