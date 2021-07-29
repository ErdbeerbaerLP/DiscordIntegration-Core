package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class CommandUptime extends DiscordCommand {
    public CommandUptime() {
        super(Configuration.instance().advanced.uptimeCmdChannelIDs, "uptime",Configuration.instance().localization.commands.descriptions.uptime);
    }

    @Override
    public String[] getAliases() {
        return new String[]{"up"};
    }


    @Override
    public void execute(SlashCommandEvent ev) {
        ev.reply(Configuration.instance().localization.commands.cmdUptime_message.replace("%uptime%", MessageUtils.getFullUptime())).queue();
    }
}
