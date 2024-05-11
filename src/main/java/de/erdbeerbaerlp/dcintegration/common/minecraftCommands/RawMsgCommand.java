package de.erdbeerbaerlp.dcintegration.common.minecraftCommands;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RawMsgCommand implements MCSubCommand {
    @Override
    public String getName() {
        return "rawmsg";
    }

    @Override
    public Component execute(String[] params, UUID playerUUID) {
        if (params.length == 0) return Component.text("Not enough arguments");

        final GuildMessageChannel c;
        if (params[0].startsWith("{")) c = DiscordIntegration.INSTANCE.getChannel();
        else {
            try {
                long id = Long.parseLong(params[0]);
                c = DiscordIntegration.INSTANCE.getChannel(String.valueOf(id));
                params[0] = "";
            } catch (NumberFormatException e) {
                return Component.text("Invalid argument. First argument must be either the channel or the json.");
            }
        }

        final StringBuilder b = new StringBuilder();
        for (String param : params) {
            b.append(param).append(" ");
        }


        final DataObject dataObject = DataObject.fromJson(b.toString().trim());
        c.sendMessage(fromJson(dataObject)).queue();
        return Component.empty();
    }

    private MessageCreateData fromJson(DataObject json) {
        final MessageCreateBuilder message = new MessageCreateBuilder();

        if (json.hasKey("content"))
            message.setContent(json.getString("content"));
        if (json.hasKey("embeds")) {
            final DataArray embedsArray = json.getArray("embeds");
            final List<MessageEmbed> embeds = new ArrayList<>();
            for (int i = 0; i < embedsArray.length(); i++) {

                final DataObject embedJson = embedsArray.getObject(i);
                final EmbedBuilder embed = EmbedBuilder.fromData(embedJson);
                embeds.add(embed.build());
            }
            message.setEmbeds(embeds);
        }
        if (json.hasKey("tts"))
            message.setTTS(json.getBoolean("tts"));
        return message.build();
    }

    @Override
    public CommandType getType() {
        return CommandType.BOTH;
    }

    @Override
    public boolean needsOP() {
        return true;
    }
}
