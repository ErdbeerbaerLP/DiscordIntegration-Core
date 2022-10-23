package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.storage.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;


public class CommandHelp extends DiscordCommand {

    public CommandHelp() {
        super("help", Localization.instance().commands.descriptions.help);
    }

    @Override
    public void execute(SlashCommandInteractionEvent ev, ReplyCallbackAction reply) {
        StringBuilder out = new StringBuilder(Localization.instance().commands.cmdHelp_header + " \n```\n");
        for (final DiscordCommand cmd : CommandRegistry.getCommandList()) {
            if (cmd.canUserExecuteCommand(ev.getUser()) && cmd.includeInHelp())
                out.append(cmd.getCommandUsage()).append(" - ").append(cmd.getDescription()).append("\n");
        }
        reply.setContent(out + "\n```").setEphemeral(true).queue();

    }
}
