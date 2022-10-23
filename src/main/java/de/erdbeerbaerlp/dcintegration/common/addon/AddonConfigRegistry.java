package de.erdbeerbaerlp.dcintegration.common.addon;


import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import de.erdbeerbaerlp.dcintegration.common.Discord;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class AddonConfigRegistry {

    /**
     * Loads all values from the config file
     *
     * @param cfg        Configuration instance
     * @param configFile Target File
     * @return The config file with (re-)loaded values
     */
    public static <T> T loadConfig(T cfg, final File configFile) {
        if (configFile == null) return null;
        if (!configFile.exists()) {
            saveConfig(cfg, configFile);
        }
        @SuppressWarnings("unchecked") final T conf = (T) new Toml().read(configFile).to(cfg.getClass());
        saveConfig(conf, configFile); //Re-write the config so new values get added after updates
        return conf;

    }

    /**
     * Saves all values to the config file
     *
     * @param cfg        Configuration instance
     * @param configFile Target File
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static <T> void saveConfig(T cfg, final File configFile) {
        if (configFile == null) return;
        try {
            if (!configFile.exists()) {
                if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
            final TomlWriter w = new TomlWriter.Builder()
                    .indentValuesBy(2)
                    .indentTablesBy(4)
                    .padArrayDelimitersBy(2)
                    .build();
            w.write(cfg, configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Legacy version of {@link AddonConfigRegistry#loadConfig(Class, DiscordIntegrationAddon)}
     *
     * @deprecated Use {@link AddonConfigRegistry#loadConfig(Class, DiscordIntegrationAddon)} instead
     */

    @Deprecated
    public static <T extends AddonConfiguration> T registerConfig(Class<T> cfg, DiscordIntegrationAddon inst) {
        return loadConfig(cfg, inst);
    }

    /**
     * Creates an Instance of your configuration and loads it<br>
     * Should be called in {@link DiscordIntegrationAddon#load(Discord)}
     *
     * @param cfg  Class of your Configuration
     * @param inst Instance of your Addon
     * @return Configuration file, if existing
     */


    public static <T> T loadConfig(Class<T> cfg, DiscordIntegrationAddon inst) {
        try {
            final T conf = cfg.getDeclaredConstructor().newInstance();
            return loadConfig(conf, inst);
        } catch (RuntimeException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            Variables.LOGGER.error("An exception occurred while loading addon configuration " + cfg.getName());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Loads the contents of the config file by using an addon instance
     *
     * @param cfg  Class of your Configuration
     * @param inst Instance of your Addon
     * @return Configuration file, if existing
     */
    public static <T> T loadConfig(T cfg, DiscordIntegrationAddon inst) {
        return loadConfig(cfg, new File(AddonLoader.getAddonDir(), AddonLoader.getAddonMeta(inst).getName() + ".toml"));
    }
}
