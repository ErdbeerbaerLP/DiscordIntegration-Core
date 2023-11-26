package de.erdbeerbaerlp.dcintegration.common.compat;

import de.erdbeerbaerlp.dcintegration.common.util.MinecraftPermission;
import net.luckperms.api.LuckPerms;
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

    public void registerPermissions(){
        final LuckPerms luckPerms = LuckPermsProvider.get();
        luckPerms.getNodeBuilderRegistry().forKey(MinecraftPermission.ADMIN.getAsString()).value(false).build();

    }
}
