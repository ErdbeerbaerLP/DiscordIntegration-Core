package de.erdbeerbaerlp.dcintegration.common.addon;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;

public interface DiscordIntegrationAddon {

    /**
     * Gets called after loading an Addon<br>
     * Use it to register event handlers or semilar
     *
     * @param dc {@link DiscordIntegration} instance
     */
    void load(final DiscordIntegration dc);

    /**
     * Gets called when Discord Integration is reloading by the /discord reload command. Can be used to reload configs
     */
    default void reload() {

    }

    /**
     * Gets called before unloading an Addon
     *
     * @param dc {@link DiscordIntegration} instance
     */
    default void unload(final DiscordIntegration dc) {
    }
}
