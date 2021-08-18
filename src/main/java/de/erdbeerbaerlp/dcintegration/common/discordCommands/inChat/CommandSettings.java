package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.text.html.Option;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class CommandSettings extends DiscordCommand {
    public CommandSettings() {
        super("settings", Configuration.instance().localization.commands.descriptions.settings);
        final ArrayList<Command.Choice> settings = new ArrayList<>();
        final Field[] settingsFields = PlayerSettings.class.getDeclaredFields();
        for (Field f : settingsFields)
            settings.add(new Command.Choice(f.getName(), f.getName()));

        addSubcommands(new SubcommandData("get", Configuration.instance().localization.commands.cmdSett_get).addOptions(
                new OptionData(OptionType.STRING, "key", Configuration.instance().localization.commands.cmdSett_key, false).addChoices(settings)),
                new SubcommandData("set", Configuration.instance().localization.commands.cmdSett_set).addOptions(
                        new OptionData(OptionType.STRING, "key", Configuration.instance().localization.commands.cmdSett_key, true).addChoices(settings),
                        new OptionData(OptionType.BOOLEAN, "value", Configuration.instance().localization.commands.cmdSett_val)));
    }

    @Override
    public void execute(SlashCommandEvent ev) {
        final CompletableFuture<InteractionHook> reply = ev.deferReply(true).submit();
        if (!PlayerLinkController.isDiscordLinked(ev.getUser().getId())) {
            reply.thenAccept((c) -> c.sendMessage(Configuration.instance().localization.linking.notLinked.replace("%method%", Configuration.instance().linking.whitelistMode ? (Configuration.instance().localization.linking.linkMethodWhitelistCode.replace("%prefix%", Configuration.instance().commands.dmPrefix)) : Configuration.instance().localization.linking.linkMethodIngame)).queue());
            return;
        }
        final OptionMapping key = ev.getOption("key");
        final OptionMapping value = ev.getOption("value");
        final String subcommandName = ev.getSubcommandName();
        if (subcommandName != null) {
            switch (subcommandName) {
                case "get":
                    if (key != null) {
                        if (discord_instance.getSettings().containsKey(key.getAsString())) {
                            final PlayerSettings settings = PlayerLinkController.getSettings(ev.getUser().getId(), null);
                            reply.thenAccept((c) -> {
                                try {
                                    c.sendMessage(Configuration.instance().localization.personalSettings.personalSettingGet.replace("%bool%", settings.getClass().getField(key.getAsString()).getBoolean(settings) ? "true" : "false")).queue();
                                } catch (IllegalAccessException | NoSuchFieldException e) {
                                    e.printStackTrace();
                                }
                            });

                        } else
                            reply.thenAccept((c) -> c.sendMessage(Configuration.instance().localization.personalSettings.invalidPersonalSettingKey.replace("%key%", key.getAsString())).queue());
                    } else {
                        final EmbedBuilder b = new EmbedBuilder();
                        final PlayerSettings settings = PlayerLinkController.getSettings(ev.getUser().getId(), null);
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
                        reply.thenAccept((c) -> c.editOriginalEmbeds(b.build()).queue());
                    }
                    break;
                case "set":
                    if (key != null) {
                        final String keyStr = key.getAsString();
                        if (discord_instance.getSettings().containsKey(keyStr)) {
                            if (ArrayUtils.contains(Configuration.instance().linking.settingsBlacklist, keyStr)) {
                                reply.thenAccept((c) -> c.sendMessage(Configuration.instance().localization.personalSettings.settingUpdateBlocked).queue());
                                return;
                            }
                            final PlayerSettings settings = PlayerLinkController.getSettings(ev.getUser().getId(), null);
                            boolean newval;
                            try {
                                newval = Boolean.parseBoolean(value.getAsString());
                            } catch (NumberFormatException e) {
                                newval = false;
                            }
                            final boolean newValue = newval;
                            try {
                                settings.getClass().getDeclaredField(keyStr).set(settings, newValue);
                                PlayerLinkController.updatePlayerSettings(ev.getUser().getId(), null, settings);
                            } catch (IllegalAccessException | NoSuchFieldException e) {
                                e.printStackTrace();
                                reply.thenAccept((c) -> c.sendMessage(Configuration.instance().localization.personalSettings.settingUpdateFailed).queue());
                            }
                            reply.thenAccept((c) -> c.sendMessage(Configuration.instance().localization.personalSettings.settingUpdateSuccessful).queue());
                        } else
                            reply.thenAccept((c) -> c.sendMessage(Configuration.instance().localization.personalSettings.invalidPersonalSettingKey.replace("%key%", keyStr)).queue());

                    }
                    break;
            }
        } else {
            System.err.println("SUBCOMMAND == NULL");
        }
    }
}
