package de.erdbeerbaerlp.dcintegration.common.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class FloodgateUtils {
    /**
     * Checks for a player UUID being an floodgate UUID or not.
     * @param playerUUID
     * @return true if the player joined with floodgate, false otherwise. Also false if floodgate API is not available
     */
    public static boolean isBedrockPlayer(UUID playerUUID){
        try {
            final Class<?> aClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            final Object getInstance = aClass.getDeclaredMethod("getInstance").invoke(null);
            final Method isBedrockPlayer = getInstance.getClass().getDeclaredMethod("isFloodgateId", UUID.class);
            final Object invoke = isBedrockPlayer.invoke(getInstance, playerUUID);
            return (boolean) invoke;
        }catch (RuntimeException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
            return false;
        }
    }
}