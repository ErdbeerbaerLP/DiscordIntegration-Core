package de.erdbeerbaerlp.dcintegration.common.util;

import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.ArrayList;

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
    public DiscordMessage(MessageEmbed embed, final String message, boolean isNotRaw) {
        if (embed != null)
            for (char c : Configuration.instance().messages.charBlacklist) {
                final EmbedBuilder b = EmbedBuilder.fromData(embed.toData());
                if (embed.getDescription() != null)
                    b.setDescription(embed.getDescription().replace(c, '\u0000'));
                if (embed.getTitle() != null)
                    b.setTitle(embed.getTitle().replace(c, '\u0000'), embed.getUrl());
                if (embed.getAuthor() != null && embed.getAuthor().getName() != null)
                    b.setAuthor(embed.getAuthor().getName().replace(c, '\u0000'), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl());
                if (embed.getFooter() != null && embed.getFooter().getText() != null)
                    b.setFooter(embed.getFooter().getText().replace(c, '\u0000'), embed.getFooter().getIconUrl());

                b.clearFields();
                for (MessageEmbed.Field field : embed.getFields()) {

                    b.addField(field.getName() == null ? null : field.getName().replace(c, '\u0000'), field.getValue() == null ? null : field.getValue().replace(c, '\u0000'), field.isInline());
                }
                embed = b.build();
            }
        this.embed = embed;

        this.message = message;
        for (char c : Configuration.instance().messages.charBlacklist) {
            this.message = this.message.replace(c, '\u0000');
        }
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
        for (char c : Configuration.instance().messages.charBlacklist) {
            this.message = this.message.replace(c, '\u0000');
        }
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
    public void setEmbed(MessageEmbed embed) {
        for (char c : Configuration.instance().messages.charBlacklist) {
            final EmbedBuilder b = EmbedBuilder.fromData(embed.toData());
            if (embed.getDescription() != null)
                b.setDescription(embed.getDescription().replace(c, '\u0000'));
            if (embed.getTitle() != null)
                b.setTitle(embed.getTitle().replace(c, '\u0000'), embed.getUrl());
            if (embed.getAuthor() != null && embed.getAuthor().getName() != null)
                b.setAuthor(embed.getAuthor().getName().replace(c, '\u0000'), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl());
            if (embed.getFooter() != null && embed.getFooter().getText() != null)
                b.setFooter(embed.getFooter().getText().replace(c, '\u0000'), embed.getFooter().getIconUrl());
            b.clearFields();
            for (MessageEmbed.Field field : embed.getFields()) {
                b.addField(field.getName().replace(c, '\u0000'), field.getValue().replace(c, '\u0000'), field.isInline());
            }
            embed = b.build();
        }
        this.embed = embed;
    }

    /**
     * Builds messages to send
     *
     * @return Creating messages
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")

    public MessageCreateData buildMessages() {
        final MessageCreateBuilder out = new MessageCreateBuilder();
            final ArrayList<Message.MentionType> mentions = new ArrayList<>();
            mentions.add(Message.MentionType.USER);
            mentions.add(Message.MentionType.CHANNEL);
            mentions.add(Message.MentionType.EMOJI);
            out.setAllowedMentions(mentions);
        if (!message.isEmpty()) {
            if (isNotRaw) {
                out.setContent(MessageUtils.convertMCToMarkdown(message));
            } else {
                out.setContent(message);
            }
        }
        if (embed != null) {
            out.setEmbeds(embed);

        }
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
            if (out.isEmpty())
                out.add(new WebhookMessageBuilder().setAllowedMentions(AllowedMentions.none().withParseUsers(true)).addEmbeds(eb.build()));
            else
                out.set(out.size() - 1, out.get(out.size() - 1).setAllowedMentions(AllowedMentions.none().withParseUsers(true)).addEmbeds(eb.build()));

        }
        return out;
    }
}