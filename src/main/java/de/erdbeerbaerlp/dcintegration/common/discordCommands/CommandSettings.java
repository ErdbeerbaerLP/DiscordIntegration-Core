package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;


public class CommandSettings extends DiscordCommand {
    protected CommandSettings(String name, String desc) {
        super(name, desc);
    }

    public CommandSettings() {
        super("settings", Localization.instance().commands.descriptions.settings);
        final ArrayList<Command.Choice> settings = new ArrayList<>();
        final Field[] settingsFields = PlayerSettings.class.getDeclaredFields();
        for (Field f : settingsFields)
            settings.add(new Command.Choice(f.getName(), f.getName()));

        addSubcommands(new SubcommandData("get", Localization.instance().commands.cmdSett_get).addOptions(
                        new OptionData(OptionType.STRING, "key", Localization.instance().commands.cmdSett_key, false).addChoices(settings)),
                new SubcommandData("set", Localization.instance().commands.cmdSett_set).addOptions(
                        new OptionData(OptionType.STRING, "key", Localization.instance().commands.cmdSett_key, true).addChoices(settings),
                        new OptionData(OptionType.BOOLEAN, "value", Localization.instance().commands.cmdSett_val)));
    }

    @Override
    public void execute(SlashCommandInteractionEvent ev, ReplyCallbackAction replyCallbackAction) {
        final CompletableFuture<InteractionHook> reply = replyCallbackAction.setEphemeral(true).submit();
        if (!LinkManager.isDiscordUserLinked(ev.getUser().getId())) {
            reply.thenAccept((c) -> c.sendMessage(Localization.instance().linking.notLinked.replace("%method%", Configuration.instance().linking.whitelistMode ? (Localization.instance().linking.linkMethodWhitelistCode.replace("%prefix%", "/")) : Localization.instance().linking.linkMethodIngame)).queue());
            return;
        }
        final OptionMapping key = ev.getOption("key");
        final OptionMapping value = ev.getOption("value");
        final String subcommandName = ev.getSubcommandName();
        if (subcommandName != null) {
            switch (subcommandName) {
                case "get":
                    if (key != null) {
                        if (CommandSettings.getSettings().containsKey(key.getAsString())) {
                            final PlayerLink link = LinkManager.getLink(ev.getUser().getId(), null);
                            reply.thenAccept((c) -> {
                                try {
                                    c.sendMessage(Localization.instance().personalSettings.personalSettingGet.replace("%bool%", link.settings.getClass().getField(key.getAsString()).getBoolean(link) ? "true" : "false")).queue();
                                } catch (IllegalAccessException | NoSuchFieldException e) {
                                    e.printStackTrace();
                                }
                            });

                        } else
                            reply.thenAccept((c) -> c.sendMessage(Localization.instance().personalSettings.invalidPersonalSettingKey.replace("%key%", key.getAsString())).queue());
                    } else {
                        final EmbedBuilder b = new EmbedBuilder();
                        final PlayerLink link = LinkManager.getLink(ev.getUser().getId(), null);
                        CommandSettings.getSettings().forEach((name, desc) -> {
                            if (!(!Configuration.instance().webhook.enable && name.equals("useDiscordNameInChannel"))) {
                                try {
                                    b.addField(name + " == " + (((boolean) link.settings.getClass().getDeclaredField(name).get(link)) ? "true" : "false"), desc, false);
                                } catch (IllegalAccessException | NoSuchFieldException e) {
                                    b.addField(name + " == Unknown", desc, false);
                                }
                            }
                        });
                        b.setAuthor(Localization.instance().personalSettings.personalSettingsHeader);
                        reply.thenAccept((c) -> c.editOriginalEmbeds(b.build()).queue());
                    }
                    break;
                case "set":
                    if (key != null) {
                        final String keyStr = key.getAsString();
                        if (getSettings().containsKey(keyStr)) {
                            if (ArrayUtils.contains(Configuration.instance().linking.settingsBlacklist, keyStr)) {
                                reply.thenAccept((c) -> c.sendMessage(Localization.instance().personalSettings.settingUpdateBlocked).queue());
                                return;
                            }
                            final PlayerLink link = LinkManager.getLink(ev.getUser().getId(), null);
                            boolean newval;
                            try {
                                if (value != null) {
                                    newval = Boolean.parseBoolean(value.getAsString());
                                } else newval = false;
                            } catch (NumberFormatException e) {
                                newval = false;
                            }
                            final boolean newValue = newval;
                            try {
                                link.settings.getClass().getDeclaredField(keyStr).set(link, newValue);
                                LinkManager.addLink(link);
                            } catch (IllegalAccessException | NoSuchFieldException e) {
                                e.printStackTrace();
                                reply.thenAccept((c) -> c.sendMessage(Localization.instance().personalSettings.settingUpdateFailed).queue());
                            }
                            reply.thenAccept((c) -> c.sendMessage(Localization.instance().personalSettings.settingUpdateSuccessful).queue());
                        } else
                            reply.thenAccept((c) -> c.sendMessage(Localization.instance().personalSettings.invalidPersonalSettingKey.replace("%key%", keyStr)).queue());

                    }
                    break;
            }
        } else {
            DiscordIntegration.LOGGER.error("SUBCOMMAND == NULL");
        }
    }

    /**
     * Gets a list of all personal settings and their descriptions
     *
     * @return HashMap with the setting keys as key and the setting descriptions as value
     */

    private static HashMap<String, String> getSettings() {
        final HashMap<String, String> out = new HashMap<>();
        final Field[] fields = PlayerSettings.class.getFields();
        final Field[] descFields = PlayerSettings.Descriptions.class.getDeclaredFields();
        for (Field f : fields) {
            out.put(f.getName(), "No Description Provided");
        }
        for (Field f : descFields) {
            f.setAccessible(true);
            try {
                out.put(f.getName(), (String) f.get(new PlayerSettings.Descriptions()));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return out;
    }

}
