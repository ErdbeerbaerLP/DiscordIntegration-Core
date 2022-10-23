package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;


public class CommandUptime extends DiscordCommand {
    public CommandUptime() {
        super("uptime", Localization.instance().commands.descriptions.uptime);
    }

    @Override
    public void execute(SlashCommandInteractionEvent ev, ReplyCallbackAction reply) {
        reply.setContent(Localization.instance().commands.cmdUptime_message.replace("%uptime%", MessageUtils.getFullUptime())).queue();
    }
}
