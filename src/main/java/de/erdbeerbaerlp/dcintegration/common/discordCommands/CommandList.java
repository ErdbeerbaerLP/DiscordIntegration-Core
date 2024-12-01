package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.INSTANCE;


public class CommandList extends DiscordCommand {
    public CommandList() {
        super("list", Localization.instance().commands.descriptions.list);
    }

    @Override
    public void execute(final SlashCommandInteractionEvent ev, final ReplyCallbackAction reply) {
        final HashMap<UUID, String> players = DiscordIntegration.INSTANCE.getServerInterface().getPlayers();
        final ArrayList<UUID> vanishedPlayers = new ArrayList<>();
        for (UUID p : players.keySet()) {
            if(INSTANCE.getServerInterface().isPlayerVanish(p)) vanishedPlayers.add(p);
        }
        for (UUID vanishedPlayer : vanishedPlayers) {
            players.remove(vanishedPlayer);
        }
        if (players.isEmpty()) {
            reply.setContent(Localization.instance().commands.cmdList_empty).setEphemeral(Configuration.instance().commands.hideListCmd).queue();
            return;
        }
        StringBuilder out = new StringBuilder((players.size() == 1 ? Localization.instance().commands.cmdList_one
                : Localization.instance().commands.cmdList_header.replace("%amount%", "" + players.size())) + "\n```\n");

        for (Map.Entry<UUID, String> p : players.entrySet()) {
            out.append(DiscordIntegration.INSTANCE.getServerInterface().getNameFromUUID(p.getKey())).append(",");
        }


        out = new StringBuilder(out.substring(0, out.length() - 1));
        reply.setContent(out + "\n```").setEphemeral(Configuration.instance().commands.hideListCmd).queue();
    }
}
