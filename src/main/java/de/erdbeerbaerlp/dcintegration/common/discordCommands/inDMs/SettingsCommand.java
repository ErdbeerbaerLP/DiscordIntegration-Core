package de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class SettingsCommand extends DMCommand {

    @Override
    public String getName() {
        return "settings";
    }


    @Override
    public String getDescription() {
        return Configuration.instance().localization.commands.descriptions.settings;
    }

    @Override
    public void execute(String[] args, final MessageChannel channel, User sender) {
        if (!PlayerLinkController.isDiscordLinked(sender.getId())) {
            channel.sendMessage(Configuration.instance().localization.linking.notLinked.replace("%method%", Configuration.instance().linking.whitelistMode ? (Configuration.instance().localization.linking.linkMethodWhitelist.replace("%prefix%", Configuration.instance().commands.dmPrefix)) : Configuration.instance().localization.linking.linkMethodIngame)).queue();
            return;
        }
        if (args.length == 2 && args[0].equals("get")) {
            if (discord_instance.getSettings().containsKey(args[1])) {
                final PlayerSettings settings = PlayerLinkController.getSettings(sender.getId(), null);
                try {
                    channel.sendMessage(Configuration.instance().localization.personalSettings.personalSettingGet.replace("%bool%", settings.getClass().getField(args[1]).getBoolean(settings) ? "true" : "false")).queue();
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            } else
                channel.sendMessage(Configuration.instance().localization.personalSettings.invalidPersonalSettingKey.replace("%key%", args[1])).queue();
        } else if (args.length == 3 && args[0].equals("set")) {
            if (discord_instance.getSettings().containsKey(args[1])) {
                if(ArrayUtils.contains(Configuration.instance().linking.settingsBlacklist,args[1])){
                    channel.sendMessage(Configuration.instance().localization.personalSettings.settingUpdateBlocked).queue();
                    return;
                }
                final PlayerSettings settings = PlayerLinkController.getSettings(sender.getId(), null);
                boolean newval;
                try {
                    newval = Boolean.parseBoolean(args[2]);
                } catch (NumberFormatException e) {
                    newval = false;
                }
                final boolean newValue = newval;
                try {
                    settings.getClass().getDeclaredField(args[1]).set(settings, newValue);
                    PlayerLinkController.updatePlayerSettings(sender.getId(), null, settings);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                    channel.sendMessage(Configuration.instance().localization.personalSettings.settingUpdateFailed).queue();
                }
                channel.sendMessage(Configuration.instance().localization.personalSettings.settingUpdateSuccessful).queue();
            } else
                channel.sendMessage(Configuration.instance().localization.personalSettings.invalidPersonalSettingKey.replace("%key%", args[1])).queue();
        } else if (args.length == 1) {
            final MessageBuilder msg = new MessageBuilder(Configuration.instance().localization.personalSettings.settingsCommandUsage.replace("%prefix%", Configuration.instance().commands.dmPrefix));
            final EmbedBuilder b = new EmbedBuilder();
            final PlayerSettings settings = PlayerLinkController.getSettings(sender.getId(), null);
            discord_instance.getSettings().forEach((name, desc) -> {
                if (!(!Configuration.instance().webhook.enable && name.equals("useDiscordNameInChannel"))) {
                    try {
                        b.addField(name + " == " + (((boolean) settings.getClass().getDeclaredField(name).get(settings)) ? "true" : "false"), desc, false);
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        b.addField(name + " == Unknown", desc, false);
                    }
                }
            });
            b.setAuthor(Configuration.instance().localization.personalSettings.personalSettingsHeader);
            msg.setEmbed(b.build());
            channel.sendMessage(msg.build()).queue();
        } else {
            channel.sendMessage(Configuration.instance().localization.personalSettings.settingsCommandUsage.replace("%prefix%", Configuration.instance().commands.dmPrefix)).queue();
        }
    }
}
