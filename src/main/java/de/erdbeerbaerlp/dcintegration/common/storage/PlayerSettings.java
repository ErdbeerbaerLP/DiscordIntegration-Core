package de.erdbeerbaerlp.dcintegration.common.storage;

public class PlayerSettings {
    public boolean useDiscordNameInChannel = Configuration.instance().linking.personalSettingsDefaults.default_useDiscordNameInChannel;
    public boolean ignoreDiscordChatIngame = false;
    public boolean ignoreReactions = Configuration.instance().linking.personalSettingsDefaults.default_ignoreReactions;
    public boolean pingSound = Configuration.instance().linking.personalSettingsDefaults.default_pingSound;
    public boolean hideFromDiscord = false;

    /**
     * Class used for key descriptions using reflection
     */
    @SuppressWarnings("unused")
    public static final class Descriptions {
        private final String useDiscordNameInChannel = Localization.instance().personalSettings.descriptons.useDiscordNameInChannel;
        private final String ignoreDiscordChatIngame = Localization.instance().personalSettings.descriptons.ignoreDiscordChatIngame;
        private final String ignoreReactions = Localization.instance().personalSettings.descriptons.ignoreReactions;
        private final String pingSound = Localization.instance().personalSettings.descriptons.pingSound;
        private final String hideFromDiscord = Localization.instance().personalSettings.descriptons.hideFromDiscord;
    }
}
