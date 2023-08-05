package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommandLinkcheck extends DiscordCommand {

    public CommandLinkcheck() {
        super("linkcheck", Localization.instance().commands.descriptions.linkcheck);
        addOption(OptionType.USER, "discorduser", Localization.instance().commands.cmdLinkcheck_userargDesc, false);
        addOption(OptionType.STRING, "mcplayer", Localization.instance().commands.cmdLinkcheck_mcargDesc, false);
    }
    @Override
    public void execute(SlashCommandInteractionEvent ev, ReplyCallbackAction replyCallbackAction) {
        final CompletableFuture<InteractionHook> reply = replyCallbackAction.setEphemeral(true).submit();
        final OptionMapping discorduser = ev.getOption("discorduser");
        final OptionMapping mcplayer = ev.getOption("mcplayer");
        if (discorduser == null && mcplayer == null) {
            reply.thenAccept((i) -> i.editOriginal(Localization.instance().commands.notEnoughArguments).queue());
        } else if (discorduser != null && mcplayer != null) {
            reply.thenAccept((i) -> i.editOriginal(Localization.instance().commands.tooManyArguments).queue());
        } else {
            if (discorduser != null) {
                if (LinkManager.isDiscordUserLinked(discorduser.getAsUser().getId())) {
                    reply.thenAccept((i) -> i.editOriginalEmbeds(buildEmbed(LinkManager.getLink(discorduser.getAsUser().getId(),null))).queue());
                } else {
                    reply.thenAccept((i) -> i.editOriginal(Localization.instance().commands.cmdLinkcheck_notlinked).queue());
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
                            reply.thenAccept((i) -> i.editOriginal(new MessageEditBuilder().setContent(Localization.instance().commands.cmdLinkcheck_cannotGetPlayer).build()).queue());
                        }
                        //Variables.LOGGER.info(mc_json);
                        uuid = UUID.fromString(mc_json.getString("id").replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
                    } catch (Exception er) {
                        er.printStackTrace();
                        reply.thenAccept((i) -> i.editOriginal(new MessageEditBuilder().setContent(Localization.instance().commands.cmdLinkcheck_cannotGetPlayer).build()).queue());
                        return;
                    }
                }
                if (LinkManager.isPlayerLinked(uuid)) {
                    final UUID Uuid = uuid;
                    //noinspection ConstantConditions
                    reply.thenAccept((i) -> i.editOriginalEmbeds(buildEmbed(LinkManager.getLink(null,Uuid))));
                } else {
                    reply.thenAccept((i) -> i.editOriginal(Localization.instance().commands.cmdLinkcheck_notlinked).queue());
                }
            }
        }
    }

    @Override
    public boolean adminOnly() {
        return true;
    }


    private MessageEmbed buildEmbed(PlayerLink link) {
        final Member user = DiscordIntegration.INSTANCE.getMemberById(link.discordID);
        final EmbedBuilder b = new EmbedBuilder();
        b.addField(Localization.instance().commands.cmdLinkcheck_discordAcc, "<@!" + user.getId() + ">", true);
        if(link.mcPlayerUUID != null) {
            final String mcname = DiscordIntegration.INSTANCE.getServerInterface().getNameFromUUID(UUID.fromString(link.mcPlayerUUID));
            b.addField(Localization.instance().commands.cmdLinkcheck_minecraftAcc, mcname + "\n" + link.mcPlayerUUID, true);
        }
        if(link.floodgateUUID != null) {
            b.addField(Localization.instance().commands.cmdLinkcheck_minecraftAccFloodgate, link.floodgateUUID, true);
        }
        return b.build();
    }
}
