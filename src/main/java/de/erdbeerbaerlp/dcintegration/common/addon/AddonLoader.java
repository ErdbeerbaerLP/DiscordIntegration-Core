package de.erdbeerbaerlp.dcintegration.common.addon;

import com.moandjiezana.toml.Toml;
import de.erdbeerbaerlp.dcintegration.common.Discord;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AddonLoader {

    private static final File addonDir = new File(Variables.discordDataDir, "addons");
    private static final FilenameFilter jarFilter = (dir, name) -> !new File(dir, name).isDirectory() && name.toLowerCase().endsWith(".jar");
    private static final HashMap<DiscordAddonMeta, DiscordIntegrationAddon> addons = new HashMap<>();

    private static final AddonClassLoader classLoader = new AddonClassLoader(AddonLoader.class.getClassLoader());

    public static AddonClassLoader getAddonClassLoader() {
        return classLoader;
    }

    public static void loadAddon(final Discord dc, final File jar) {
        try {
            final JarFile jf = new JarFile(jar);
            final JarEntry entry = jf.getJarEntry("DiscordIntegrationAddon.toml");
            if (entry != null) {
                final InputStream is = jf.getInputStream(entry);
                final DiscordAddonMeta addonMeta = new Toml().read(is).to(DiscordAddonMeta.class);
                is.close();
                if (addonMeta.getClassPath() == null || addonMeta.getName() == null || addonMeta.getVersion() == null) {
                    Variables.LOGGER.error("Failed to load Addon '" + jar.getName() + "'! Toml is missing parameters! (Required are name, version, classPath)");
                }
                try {
                    classLoader.add(jar.toURI().toURL());
                    final Class<? extends DiscordIntegrationAddon> addonClass = Class.forName(addonMeta.getClassPath(), true, classLoader).asSubclass(DiscordIntegrationAddon.class);
                    final DiscordIntegrationAddon addon = addonClass.getDeclaredConstructor().newInstance();
                    addons.put(addonMeta, addon);
                    try {
                        addon.load(dc);
                    } catch (Exception e) {
                        Variables.LOGGER.error("An exception occurred while loading addon " + addonMeta.getName());
                        e.printStackTrace();
                    }
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                    Variables.LOGGER.error("Failed to load Addon '" + addonMeta.getName() + "' version '" + addonMeta.getVersion() + "'!");
                    e.printStackTrace();
                } catch (InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Variables.LOGGER.error("Found non-addon jar file " + jar.getName());
            }
        } catch (IOException e) {
            Variables.LOGGER.error("Failed to load addon " + jar.getName());
            e.printStackTrace();
        }
    }

    public static void loadAddons(final Discord dc) {
        if (!addonDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            addonDir.mkdirs();
        }
        final File[] jars = addonDir.listFiles(jarFilter);
        if (jars != null) {
            for (final File jar : jars) {
                loadAddon(dc, jar);
            }
        }
    }

    public static void unloadAddons(Discord discord) {
        for (final DiscordIntegrationAddon addon : addons.values()) {
            try {
                addon.unload(discord);
            } catch (Exception e) {
                Variables.LOGGER.error("An exception occurred while unloading addon class " + addon.getClass().getName());
                e.printStackTrace();
            }
        }
        addons.clear();
    }

    /**
     * @param inst Instance of the Addon
     * @return Addon Metadata, or null if plugin instance is missing
     */

    public static DiscordAddonMeta getAddonMeta(DiscordIntegrationAddon inst) {
        AtomicReference<DiscordAddonMeta> meta = new AtomicReference<>();
        addons.forEach((a, b) -> {
            if (b.equals(inst)) meta.set(a);
        });
        return meta.get();
    }

    /**
     * @return Addon metadata of all loaded Addons
     */

    public static Set<DiscordAddonMeta> getAddonMetas() {
        return addons.keySet();
    }

    /**
     * @return Addon instances of all loaded Addons
     */

    public static Collection<DiscordIntegrationAddon> getAddons() {
        return addons.values();
    }

    /**
     * Sends an {@link DiscordIntegrationAddon#reload()} to all Addons
     */
    public static void reloadAll() {
        getAddons().forEach(DiscordIntegrationAddon::reload);
    }

    /**
     * @return The current Addon directory
     */

    public static File getAddonDir() {
        return addonDir;
    }
}
