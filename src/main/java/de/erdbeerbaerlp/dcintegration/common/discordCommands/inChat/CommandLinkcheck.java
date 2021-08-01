package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommandLinkcheck extends DiscordCommand {

    public CommandLinkcheck() {
        super("linkcheck", Configuration.instance().localization.commands.descriptions.linkcheck);
        addOption(OptionType.USER, "discorduser", Configuration.instance().localization.commands.cmdLinkcheck_userargDesc, false);
        addOption(OptionType.STRING, "mcplayer", Configuration.instance().localization.commands.cmdLinkcheck_mcargDesc, false);
    }

    @Override
    public void execute(SlashCommandEvent ev) {
        final CompletableFuture<InteractionHook> reply = ev.deferReply(true).submit();
        final OptionMapping discorduser = ev.getOption("discorduser");
        final OptionMapping mcplayer = ev.getOption("mcplayer");
        if (discorduser == null && mcplayer == null) {
            reply.thenAccept((i) -> i.editOriginal(Configuration.instance().localization.commands.notEnoughArguments).queue());
        } else if (discorduser != null && mcplayer != null) {
            reply.thenAccept((i) -> i.editOriginal(Configuration.instance().localization.commands.tooManyArguments).queue());
        } else {
            if (discorduser != null) {
                if (PlayerLinkController.isDiscordLinked(discorduser.getAsUser().getId())) {
                    reply.thenAccept((i) -> i.editOriginalEmbeds(buildEmbed(PlayerLinkController.getPlayerFromDiscord(discorduser.getAsUser().getId()), discorduser.getAsUser())).queue());
                } else {
                    reply.thenAccept((i) -> i.editOriginal(Configuration.instance().localization.commands.cmdLinkcheck_notlinked).queue());
                }
            } else {
                UUID uuid;
                try {
                    uuid = UUID.fromString(mcplayer.getAsString().replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
                    ));
                } catch (IllegalArgumentException e) {
                    try {
                        final URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + mcplayer.getAsString().trim());
                        final URLConnection urlConnection = url.openConnection();
                        urlConnection.addRequestProperty("User-Agent", "DiscordIntegration-by-ErdbeerbaerLP");
                        urlConnection.addRequestProperty("Accept", "application/json");
                        urlConnection.connect();
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        final StringBuilder buffer = new StringBuilder();
                        int read;
                        char[] chars = new char[1024];
                        while ((read = reader.read(chars)) != -1)
                            buffer.append(chars, 0, read);
                        reader.close();
                        JSONObject mc_json = new JSONObject(buffer.toString());
                        if (mc_json.has("error")) {
                            reply.thenAccept((i) -> i.editOriginal(new MessageBuilder().setContent(Configuration.instance().localization.commands.cmdLinkcheck_cannotGetPlayer).build()).queue());
                        }
                        System.out.println(mc_json);
                        uuid = UUID.fromString(mc_json.getString("id").replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
                    } catch (Exception er) {
                        er.printStackTrace();
                        reply.thenAccept((i) -> i.editOriginal(new MessageBuilder().setContent(Configuration.instance().localization.commands.cmdLinkcheck_cannotGetPlayer).build()).queue());
                        return;
                    }
                }
                if (PlayerLinkController.isPlayerLinked(uuid)) {
                    final UUID Uuid = uuid;
                    reply.thenAccept((i) -> i.editOriginalEmbeds(buildEmbed(Uuid, Variables.discord_instance.getJDA().getUserById(PlayerLinkController.getDiscordFromPlayer(Uuid)))).queue());
                } else {
                    reply.thenAccept((i) -> i.editOriginal(Configuration.instance().localization.commands.cmdLinkcheck_notlinked).queue());
                }
            }
        }
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    private MessageEmbed buildEmbed(UUID uuid, User user) {
        final EmbedBuilder b = new EmbedBuilder();
        b.addField(Configuration.instance().localization.commands.cmdLinkcheck_discordAcc, "<@!" + user.getId() + ">", true);
        final String mcname = Variables.discord_instance.srv.getNameFromUUID(uuid);
        b.addField(Configuration.instance().localization.commands.cmdLinkcheck_minecraftAcc, mcname + "\n" + uuid, true);
        return b.build();
    }
}
