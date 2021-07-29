package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class CommandFromCFG extends DiscordCommand {
    private final String mcCmd;
    private final boolean admin;
    private final String[] aliases;
    private final String[] channelIDs;

    public CommandFromCFG(@Nonnull String cmd, @Nonnull String description, @Nonnull String mcCommand, boolean adminOnly, @Nonnull String[] aliases, boolean useArgs, @Nonnull String argText, @Nonnull String[] channelIDs) {
        super("",cmd,description);
        this.channelIDs = channelIDs;
        this.isConfigCmd = true;
        this.admin = adminOnly;
        this.mcCmd = mcCommand;
        this.aliases = aliases;
        this.useArgs = useArgs;
        this.argText = argText;
    }

    @Override
    public boolean worksInChannel(String channelID) {
        return Arrays.equals(channelIDs, new String[]{"00"}) || Arrays.equals(channelIDs, new String[]{"0"}) && channelID.equals(Configuration.instance().general.botChannel) || ArrayUtils.contains(channelIDs, channelID);
    }

    @Override
    public boolean adminOnly() {
        return admin;
    }

    /**
     * Sets the aliases of the command
     */
    @Override
    public String[] getAliases() {
        return aliases;
    }

    /**
     * Sets the description for the help command
     */

    @Override
    public String getCommandUsage() {
        if (useArgs) return super.getCommandUsage() + " " + argText;
        else return super.getCommandUsage();
    }

    @Override
    public void execute(SlashCommandEvent ev) {
        String cmd = mcCmd;
        String argString = ev.getOption("args") != null ? ev.getOption("args").getAsString() : "";
        String[] args = ArrayUtils.addAll(new String[]{cmd}, argString.split(" "));
        int argsCount = useArgs ? args.length : 0;
        if (argsCount > 0) {
            for (int i = 0; i < argsCount; i++) {
                argString += (" " + args[i]);
            }
        }
        if (!cmd.contains("%args%")) cmd = cmd + argString;
        else cmd = cmd.replace("%args%", argString.trim());
        ev.reply(Configuration.instance().localization.commands.executing).queue();
        discord_instance.srv.runMcCommand(cmd, ev.getChannel(),ev.getUser());
    }

}
