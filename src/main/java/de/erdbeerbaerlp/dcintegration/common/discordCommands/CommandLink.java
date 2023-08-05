package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;


public class CommandLink extends DiscordCommand {

    public CommandLink() {
        super("link", Configuration.instance().linking.whitelistMode ? Localization.instance().commands.descriptions.whitelist : Localization.instance().commands.descriptions.link);
        addOption(OptionType.INTEGER, "code", "Link Code", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent ev, ReplyCallbackAction replyCallbackAction) {
        final CompletableFuture<InteractionHook> reply = replyCallbackAction.setEphemeral(true).submit();
        Member m = ev.getMember();
        if (m == null) m = DiscordIntegration.INSTANCE.getMemberById(ev.getUser().getIdLong());
        if (m != null)
            if (Configuration.instance().linking.requiredRoles.length != 0) {
                AtomicBoolean ok = new AtomicBoolean(false);
                m.getRoles().forEach((role) -> {
                    for (String s : Configuration.instance().linking.requiredRoles) {
                        if (s.equals(role.getId())) ok.set(true);
                    }
                });
                if (!ok.get()) {
                    reply.thenAccept((c) -> c.editOriginal(MessageEditData.fromContent(Localization.instance().linking.link_requiredRole)).queue());
                    return;
                }
            }
        final OptionMapping code = ev.getOption("code");
        if (code != null) {
            try {
                int num = Integer.parseInt(code.getAsString());
                if (LinkManager.isDiscordUserLinked(ev.getUser().getId()) && (LinkManager.pendingBedrockLinks.isEmpty() && LinkManager.isDiscordUserLinkedToBedrock(ev.getUser().getId()))) {
                    reply.thenAccept((c) -> c.editOriginal(Localization.instance().linking.alreadyLinked.replace("%player%", MessageUtils.getNameFromUUID(UUID.fromString(LinkManager.getLink(ev.getUser().getId(), null).floodgateUUID)))).queue());
                    return;
                }
                if (LinkManager.pendingLinks.containsKey(num)) {
                    final boolean linked = LinkManager.linkPlayer(ev.getUser().getId(), LinkManager.pendingLinks.get(num).getValue());
                    if (linked) {
                        reply.thenAccept((c) -> c.editOriginal(Localization.instance().linking.linkSuccessful.replace("%prefix%", "/").replace("%player%", MessageUtils.getNameFromUUID(UUID.fromString(LinkManager.getLink(ev.getUser().getId(), null).mcPlayerUUID)))).queue());
                        DiscordIntegration.INSTANCE.getServerInterface().sendIngameMessage(Localization.instance().linking.linkSuccessfulIngame.replace("%name%", ev.getUser().getName()).replace("%name#tag%", ev.getUser().getAsTag()), LinkManager.pendingLinks.get(num).getValue());
                    } else
                        reply.thenAccept((c) -> c.editOriginal(Localization.instance().linking.linkFailed).queue());
                } else if (LinkManager.pendingBedrockLinks.containsKey(num)) {
                    final boolean linked = LinkManager.linkBedrockPlayer(ev.getUser().getId(), LinkManager.pendingBedrockLinks.get(num).getValue());
                    if (linked) {
                        reply.thenAccept((c) -> c.editOriginal(Localization.instance().linking.linkSuccessful.replace("%prefix%", "/").replace("%player%", MessageUtils.getNameFromUUID(UUID.fromString(LinkManager.getLink(ev.getUser().getId(), null).floodgateUUID)))).queue());
                        DiscordIntegration.INSTANCE.getServerInterface().sendIngameMessage(Localization.instance().linking.linkSuccessfulIngame.replace("%name%", ev.getUser().getName()).replace("%name#tag%", ev.getUser().getAsTag()), LinkManager.pendingBedrockLinks.get(num).getValue());
                    } else
                        reply.thenAccept((c) -> c.editOriginal(Localization.instance().linking.linkFailed).queue());
                } else {
                    reply.thenAccept((c) -> c.editOriginal(Localization.instance().linking.invalidLinkNumber).queue());
                }
            } catch (NumberFormatException nfe) {
                reply.thenAccept((c) -> c.editOriginal(Localization.instance().linking.linkNumberNAN).queue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }


    @Override
    public boolean canUserExecuteCommand(@NotNull User user) {
        return true;
    }
}
