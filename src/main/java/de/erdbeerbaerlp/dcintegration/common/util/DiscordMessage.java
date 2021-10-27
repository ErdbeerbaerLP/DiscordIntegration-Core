package de.erdbeerbaerlp.dcintegration.common.util;

import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.internal.utils.AllowedMentionsImpl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Queue;

public final class DiscordMessage {
    private final boolean isNotRaw;
    private MessageEmbed embed;

    private String message = "";

    /**
     * @param isNotRaw set to true to enable markdown escaping and mc color conversion (default: false)
     */
    public DiscordMessage(final MessageEmbed embed, @Nonnull final String message, boolean isNotRaw) {
        this.embed = embed;
        this.message = message;
        this.isNotRaw = isNotRaw;
    }

    public DiscordMessage(final MessageEmbed embed, @Nonnull final String message) {
        this(embed, message, false);
    }

    public DiscordMessage(@Nonnull final String message) {
        this(null, message, false);
    }

    public DiscordMessage(@Nonnull final MessageEmbed embed) {
        this(embed, "", false);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(@Nonnull final String message) {
        this.message = message;
    }

    public MessageEmbed getEmbed() {
        return embed;
    }

    public void setEmbed(@Nonnull final MessageEmbed embed) {
        this.embed = embed;
    }

    @Nonnull
    public Queue<Message> buildMessages() {
        final MessageBuilder out = new MessageBuilder();
        final ArrayList<Message.MentionType> mentions = new ArrayList<>();
        mentions.add(Message.MentionType.USER);
        mentions.add(Message.MentionType.CHANNEL);
        mentions.add(Message.MentionType.EMOTE);
        out.setAllowedMentions(mentions);
        if (!message.isEmpty()) {
            if (isNotRaw) {
                if (Configuration.instance().messages.formattingCodesToDiscord)
                    out.setContent(MessageUtils.convertMCToMarkdown(message));
                else
                    out.setContent(MessageUtils.removeFormatting(MessageUtils.convertMCToMarkdown(message)));
            } else {
                out.setContent(message);
            }
        }
        if (embed != null)
            out.setEmbeds(embed);
        return out.buildAll(MessageBuilder.SplitPolicy.NEWLINE, MessageBuilder.SplitPolicy.SPACE);
    }

    private String[] splitMessages(final String inMsg) {
        if (inMsg.length() <= 2000)
            return new String[]{inMsg};
        else {
            String[] split = inMsg.split(" ");
            final ArrayList<String> outStrings = new ArrayList<>();
            String bufferString = "";
            for (String s : split) {
                if ((bufferString + " " + s).length() > 2000) {
                    outStrings.add(bufferString);
                    bufferString = " ";
                } else
                    bufferString += s;
            }
            outStrings.add(bufferString);
            return outStrings.toArray(new String[0]);
        }
    }

    @Nonnull
    public ArrayList<WebhookMessageBuilder> buildWebhookMessages() {
        final ArrayList<WebhookMessageBuilder> out = new ArrayList<>();
        String content;
        if (!message.isEmpty()) {
            if (isNotRaw) {
                if (Configuration.instance().messages.formattingCodesToDiscord)
                    content = MessageUtils.convertMCToMarkdown(message);
                else
                    content = MessageUtils.removeFormatting(MessageUtils.convertMCToMarkdown(message));
            } else {
                content = message;
            }
            for (String msg : splitMessages(content)) {
                out.add(new WebhookMessageBuilder().setContent(msg).setAllowedMentions(AllowedMentions.none().withParseUsers(true)));
            }
        }
        if (embed != null) {
            final WebhookEmbedBuilder eb = new WebhookEmbedBuilder();
            if (embed.getAuthor() != null)
                eb.setAuthor(new WebhookEmbed.EmbedAuthor(embed.getAuthor().getName(), embed.getAuthor().getIconUrl(), embed.getAuthor().getUrl()));
            eb.setColor(embed.getColorRaw());
            eb.setDescription(embed.getDescription());
            if (embed.getFooter() != null)
                eb.setFooter(new WebhookEmbed.EmbedFooter(embed.getFooter().getText(), embed.getFooter().getIconUrl()));
            if (embed.getImage() != null)
                eb.setImageUrl(embed.getImage().getUrl());
            if (embed.getThumbnail() != null)
                eb.setThumbnailUrl(embed.getThumbnail().getUrl());
            for (MessageEmbed.Field f : embed.getFields()) {
                eb.addField(new WebhookEmbed.EmbedField(f.isInline(), f.getName(), f.getValue()));
            }
            eb.setTimestamp(embed.getTimestamp());
            if (embed.getTitle() != null)
                eb.setTitle(new WebhookEmbed.EmbedTitle(embed.getTitle(), embed.getUrl()));
            out.set(out.size()-1,out.get(out.size()-1).addEmbeds(eb.build()));
        }
        return out;
    }
}
