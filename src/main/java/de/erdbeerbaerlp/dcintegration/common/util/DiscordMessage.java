package de.erdbeerbaerlp.dcintegration.common.util;

import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;

@SuppressWarnings("unused")
public final class DiscordMessage {
    private final boolean isNotRaw;
    private MessageEmbed embed;
    private boolean isSystemMessage = true;
    private String message;

    /**
     * @param embed    Embed to attach to message
     * @param message  Message to send
     * @param isNotRaw set to true to enable markdown escaping and mc color conversion (default: false)
     */
    public DiscordMessage(final MessageEmbed embed, final String message, boolean isNotRaw) {
        this.embed = embed;
        this.message = message;
        this.isNotRaw = isNotRaw;
    }

    /**
     * @param embed   Embed to attach to message
     * @param message Message to send
     */
    public DiscordMessage(final MessageEmbed embed, final String message) {
        this(embed, message, false);
    }

    /**
     * @param message Message to send
     */
    public DiscordMessage(final String message) {
        this(null, message, false);
    }

    /**
     * @param embed Embed to send
     */
    public DiscordMessage(final MessageEmbed embed) {
        this(embed, "", false);
    }

    /**
     * Gets the raw message
     *
     * @return Message String
     */
    public String getMessage() {
        return message;
    }

    /**
     * Changes the raw message
     *
     * @param message Message to set
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    public void setIsChatMessage() {
        this.isSystemMessage = false;
    }

    /**
     * @return The set embed
     */
    public MessageEmbed getEmbed() {
        return embed;
    }

    /**
     * Sets the embed of this Message
     *
     * @param embed Embed to set
     */
    public void setEmbed(final MessageEmbed embed) {
        this.embed = embed;
    }

    /**
     * Builds messages to send
     *
     * @return {@link Queue} of messages
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")

    public MessageCreateData buildMessages() {
        final MessageCreateBuilder out = new MessageCreateBuilder();
        if (!isSystemMessage) {
            final ArrayList<Message.MentionType> mentions = new ArrayList<>();
            mentions.add(Message.MentionType.USER);
            mentions.add(Message.MentionType.CHANNEL);
            mentions.add(Message.MentionType.EMOJI);
            out.setAllowedMentions(mentions);
        } else
            out.setAllowedMentions(Arrays.asList(Message.MentionType.values()));
        if (!message.isEmpty()) {
            if (isNotRaw) {
                out.setContent(MessageUtils.convertMCToMarkdown(message));
            } else {
                out.setContent(message);
            }
        }
        if (embed != null)
            out.setEmbeds(embed);
        return out.build();
    }

    /**
     * Splits messages for character limits of the webhook
     *
     * @param inMsg Message to split
     * @return Split messages
     */
    private String[] splitMessages(final String inMsg) {
        if (inMsg.length() <= 2000)
            return new String[]{inMsg};
        else {
            String[] split = inMsg.split(" ");
            final ArrayList<String> outStrings = new ArrayList<>();
            StringBuilder bufferString = new StringBuilder();
            for (String s : split) {
                if ((bufferString + " " + s).length() > 2000) {
                    outStrings.add(bufferString.toString());
                    bufferString = new StringBuilder(" ");
                } else
                    bufferString.append(s);
            }
            outStrings.add(bufferString.toString());
            return outStrings.toArray(new String[0]);
        }
    }

    /**
     * Builds webhook messages
     *
     * @return List containing webhook messages
     */
    @SuppressWarnings("ConstantConditions")

    public ArrayList<WebhookMessageBuilder> buildWebhookMessages() {
        final ArrayList<WebhookMessageBuilder> out = new ArrayList<>();
        String content;
        if (!message.isEmpty()) {
            if (isNotRaw) {
                content = MessageUtils.convertMCToMarkdown(message);
            } else {
                content = message;
            }
            for (String msg : splitMessages(content)) {
                out.add(new WebhookMessageBuilder().setContent(msg).setAllowedMentions(isSystemMessage ? AllowedMentions.all() : AllowedMentions.none().withParseUsers(true)));
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
            if (out.isEmpty())
                out.add(new WebhookMessageBuilder().setAllowedMentions(isSystemMessage ? AllowedMentions.all() : AllowedMentions.none().withParseUsers(true)).addEmbeds(eb.build()));
            else
                out.set(out.size() - 1, out.get(out.size() - 1).setAllowedMentions(isSystemMessage ? AllowedMentions.all() : AllowedMentions.none().withParseUsers(true)).addEmbeds(eb.build()));

        }
        return out;
    }
}