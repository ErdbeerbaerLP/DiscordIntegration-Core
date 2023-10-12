package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.configCmd.ConfigCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.concurrent.CompletableFuture;


public class CommandFromConfig extends DiscordCommand {
    private final String mcCmd;
    private final boolean admin;
    private final ConfigCommand.CommandArgument[] args;
    private final boolean hidden;
    private final String textToSend;

    public CommandFromConfig(String cmd, String description, String mcCommand, boolean adminOnly, ConfigCommand.CommandArgument[] args, boolean hidden, String textToSend) throws IllegalArgumentException {
        super(cmd, description);
        this.textToSend = textToSend;
        this.isConfigCmd = true;
        this.admin = adminOnly;
        this.mcCmd = mcCommand;
        this.hidden = hidden;
        if (args != null) {
            this.args = args;
            for (ConfigCommand.CommandArgument argument : args) {
                addOption(OptionType.STRING, argument.name, argument.description, !argument.optional);
            }
        } else {
            this.args = new ConfigCommand.CommandArgument[0];
        }
    }

    @Override
    public boolean adminOnly() {
        return admin;
    }

    @Override
    public void execute(final SlashCommandInteractionEvent ev, ReplyCallbackAction reply) {
        reply = reply.setEphemeral(hidden);
        if (!textToSend.isEmpty()) {
            reply = reply.setContent(textToSend);
        }
        final CompletableFuture<InteractionHook> submit = reply.submit();
        if (!mcCmd.isEmpty()) {
            String cmd = mcCmd;
            for (ConfigCommand.CommandArgument arg : args) {
                final OptionMapping option = ev.getOption(arg.name);
                cmd = cmd.replace("%" + arg.name + "%", option == null ? "" : option.getAsString());

            }
            DiscordIntegration.INSTANCE.getServerInterface().runMcCommand(cmd, submit, ev.getUser());
        }
    }

}
