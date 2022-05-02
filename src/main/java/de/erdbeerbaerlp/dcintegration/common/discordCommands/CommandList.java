package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class CommandList extends DiscordCommand {
    public CommandList() {
        super("list", Localization.instance().commands.descriptions.list);
    }

    @Override
    public void execute(SlashCommandInteractionEvent ev, ReplyCallbackAction reply) {
        final HashMap<UUID, String> players = discord_instance.srv.getPlayers();
        if (players.isEmpty()) {
            ev.reply(Localization.instance().commands.cmdList_empty).queue();
            return;
        }
        StringBuilder out = new StringBuilder((players.size() == 1 ? Localization.instance().commands.cmdList_one
                : Localization.instance().commands.cmdList_header.replace("%amount%", "" + players.size())) + "\n```\n");

        for (Map.Entry<UUID, String> p : players.entrySet()) {
            out.append(discord_instance.srv.getNameFromUUID(p.getKey())).append(",");
        }


        out = new StringBuilder(out.substring(0, out.length() - 1));
        reply.setContent(out + "\n```").queue();
    }
}
