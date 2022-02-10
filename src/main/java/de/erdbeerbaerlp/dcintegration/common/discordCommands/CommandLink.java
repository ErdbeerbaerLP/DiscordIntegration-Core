package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class CommandLink extends DiscordCommand {

    public CommandLink() {
        super("link", Localization.instance().commands.descriptions.link);
        addOption(OptionType.INTEGER, "code", "Link Code", true);
    }


    @Override
    public void execute(SlashCommandEvent ev) {
        final CompletableFuture<InteractionHook> reply = ev.deferReply(true).submit();
        Member m = ev.getMember();
        if (Configuration.instance().linking.requiredRoles.length != 0) {
            AtomicBoolean ok = new AtomicBoolean(false);
            m.getRoles().forEach((role) -> {
                for (String s : Configuration.instance().linking.requiredRoles) {
                    if (s.equals(role.getId())) ok.set(true);
                }
            });
            if (!ok.get()) {
                reply.thenAccept((c) -> c.sendMessage(Localization.instance().linking.link_requiredRole).queue());
                return;
            }
        }
        final OptionMapping code = ev.getOption("code");
        if(code != null){
            try {
                int num = Integer.parseInt(code.getAsString());
                if (PlayerLinkController.isDiscordLinked(ev.getUser().getId()) && (discord_instance.pendingBedrockLinks.isEmpty() && PlayerLinkController.isDiscordLinkedBedrock(ev.getUser().getId()))) {
                    reply.thenAccept((c) -> c.sendMessage(Localization.instance().linking.alreadyLinked.replace("%player%", MessageUtils.getNameFromUUID(PlayerLinkController.getPlayerFromDiscord(ev.getUser().getId())))).queue());
                    return;
                }
                if (discord_instance.pendingLinks.containsKey(num)) {
                    final boolean linked = PlayerLinkController.linkPlayer(ev.getUser().getId(), discord_instance.pendingLinks.get(num).getValue());
                    if (linked) {
                        reply.thenAccept((c) -> c.sendMessage(Localization.instance().linking.linkSuccessful.replace("%prefix%", "/").replace("%player%", MessageUtils.getNameFromUUID(PlayerLinkController.getPlayerFromDiscord(ev.getUser().getId())))).queue());
                        discord_instance.srv.sendMCMessage(Localization.instance().linking.linkSuccessfulIngame.replace("%name%", ev.getUser().getName()).replace("%name#tag%", ev.getUser().getAsTag()), discord_instance.pendingLinks.get(num).getValue());
                    } else
                        reply.thenAccept((c) -> c.sendMessage(Localization.instance().linking.linkFailed).queue());
                } else if (discord_instance.pendingBedrockLinks.containsKey(num)) {
                    final boolean linked = PlayerLinkController.linkBedrockPlayer(ev.getUser().getId(), discord_instance.pendingBedrockLinks.get(num).getValue());
                    if (linked) {
                        reply.thenAccept((c) -> c.sendMessage(Localization.instance().linking.linkSuccessful.replace("%prefix%", "/").replace("%player%", MessageUtils.getNameFromUUID(PlayerLinkController.getBedrockPlayerFromDiscord(ev.getUser().getId())))).queue());
                        discord_instance.srv.sendMCMessage(Localization.instance().linking.linkSuccessfulIngame.replace("%name%", ev.getUser().getName()).replace("%name#tag%", ev.getUser().getAsTag()), discord_instance.pendingBedrockLinks.get(num).getValue());
                    } else
                        reply.thenAccept((c) -> c.sendMessage(Localization.instance().linking.linkFailed).queue());
                } else {
                    reply.thenAccept((c) -> c.sendMessage(Localization.instance().linking.invalidLinkNumber).queue());
                }
            } catch (NumberFormatException nfe) {
                reply.thenAccept((c) -> c.sendMessage(Localization.instance().linking.linkNumberNAN).queue());
            }
        }


    }

    @Override
    public boolean canUserExecuteCommand(User user) {
        return true;
    }
}
