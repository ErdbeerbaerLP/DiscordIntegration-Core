package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class CommandList extends DiscordCommand {
    public CommandList() {
        super("list", Configuration.instance().localization.commands.descriptions.list);
    }



    @Override
    public void execute(SlashCommandEvent ev) {
        final HashMap<UUID, String> players = discord_instance.srv.getPlayers();
        if (players.isEmpty()) {
            ev.reply(Configuration.instance().localization.commands.cmdList_empty).queue();
            return;
        }
        StringBuilder out = new StringBuilder((players.size() == 1 ? Configuration.instance().localization.commands.cmdList_one
                : Configuration.instance().localization.commands.cmdList_header.replace("%amount%", "" + players.size())) + "\n```\n");

        for (Map.Entry<UUID, String> p : players.entrySet()) {
            out.append(discord_instance.srv.getNameFromUUID(p.getKey())).append(",");
        }


        out = new StringBuilder(out.substring(0, out.length() - 1));
        ev.reply(out + "\n```").queue();
    }
}
