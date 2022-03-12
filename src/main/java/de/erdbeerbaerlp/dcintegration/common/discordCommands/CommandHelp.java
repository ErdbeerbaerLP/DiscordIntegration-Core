package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.storage.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;


public class CommandHelp extends DiscordCommand {

    public CommandHelp() {
        super( "help",Localization.instance().commands.descriptions.help);
    }

    @Override
    public void execute(SlashCommandInteractionEvent ev) {
        StringBuilder out = new StringBuilder(Localization.instance().commands.cmdHelp_header + " \n```\n");
        for (final DiscordCommand cmd : CommandRegistry.getCommandList()) {
            if (cmd.canUserExecuteCommand(ev.getUser()) && cmd.includeInHelp())
                out.append(cmd.getCommandUsage()).append(" - ").append(cmd.getDescription()).append("\n");
        }
        ev.reply(out + "\n```").queue();

    }
}
