package de.erdbeerbaerlp.dcintegration.common.util;

import com.google.gson.JsonArray;
import com.vdurmont.emoji.EmojiParser;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class MessageUtils {


    @SuppressWarnings({"RegExpRedundantEscape", "RegExpUnnecessaryNonCapturingGroup", "RegExpSimplifiable"})
    static final Pattern URL_PATTERN = Pattern.compile(
            //              schema                          ipv4            OR        namespace                 port     path         ends
            //        |-----------------|        |-------------------------|  |-------------------------|    |---------| |--|   |---------------|
            "((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"\u00A7 \n]|$))",
            Pattern.CASE_INSENSITIVE);
    /**
     * Regex matching formatting codes like thos: §4
     */
    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)\u00a7[0-9A-FK-OR]");
    /**
     * This regex will match all user mentions (<@userid>)
     */
    private static final Pattern USER_PING_REGEX = Pattern.compile("(<@!?([0-9]{17,18})>)");
    /**
     * This regex will match all role mentions (<@&roleid>)
     */
    private static final Pattern ROLE_PING_REGEX = Pattern.compile("(<@&([0-9]{17,20})>)");
    /**
     * This regex will match all channel mentions (<#channelid>)
     */
    private static final Pattern CHANNEL_REGEX = Pattern.compile("(<#([0-9]{17,20})>)");
    /**
     * This regex will match ANY type of mention
     */
    private static final Pattern ANYPING_REGEX = Pattern.compile("(<..?([0-9]{17,20})>)");

    public static String[] makeStringArray(final JsonArray channelID) {
        final String[] out = new String[channelID.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = channelID.get(i).getAsString();
        }
        return out;
    }

    /**
     * Gets the Discord name for the player
     *
     * @param p Player UUID
     * @return Discord name, or null of the player did not link his discord account OR disabled useDiscordNameInChannel
     */
    @SuppressWarnings("ConstantConditions")

    public static String getDiscordName(final UUID p) {
        if (DiscordIntegration.INSTANCE == null) return null;
        if (Configuration.instance().linking.enableLinking && LinkManager.isPlayerLinked(p)) {
            final PlayerLink link = LinkManager.getLink(null, p);
            if (link.settings.useDiscordNameInChannel) {
                return DiscordIntegration.INSTANCE.getChannel().getGuild().getMemberById(LinkManager.getLink(null,p).discordID).getEffectiveName();
            }
        }
        return null;
    }

    /**
     * Escapes markdown from String
     *
     * @param in String with markdown
     * @return Input string without markdown
     */

    public static String escapeMarkdown(String in) {
        return in.replace("(?<!\\\\)[`*_|~]/g", "\\\\$0");
    }

    /**
     * Escapes markdown codeblocks from String
     *
     * @param in String with markdown codeblocks
     * @return Input string without markdown codeblocks
     */

    public static String escapeMarkdownCodeBlocks(String in) {
        return in.replace("(?<!\\\\)`/g", "\\\\$0");
    }

    /**
     * Gets the full server uptime formatted as specified in the config at {@link Localization.Commands#uptimeFormat}
     *
     * @return Uptime String
     */

    public static String getFullUptime() {
        if (DiscordIntegration.started == 0) {
            return "?????";
        }
        final Duration duration = Duration.between(Instant.ofEpochMilli(DiscordIntegration.started), Instant.now());
        return DurationFormatUtils.formatDuration(duration.toMillis(), Localization.instance().commands.uptimeFormat);
    }

    /**
     * Converts minecraft formatting codes into discord markdown
     *
     * @param in String with mc formatting codes
     * @return String with markdown
     */

    public static String convertMCToMarkdown(String in) {
        in = escapeMarkdownCodeBlocks(in);
        try {
            return DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacySection().deserialize(in));
        } catch (NullPointerException | ConcurrentModificationException ex) {
            ex.printStackTrace();
            return in;
        }
    }

    /**
     * Removes all Minecraft formatting codes
     *
     * @param text Formatted String
     * @return Unformatted String
     */

    public static String removeFormatting(String text) {
        return FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Translates ID mentions (like <@userid> to human-readable mentions (like @SomeName123)
     *
     * @param in          Component that should be formatted
     * @param targetGuild Target {@link Guild}, where the nicknames should be taken from
     * @return Formatted {@link Component}
     */

    public static Component mentionsToNames(Component in, final Guild targetGuild) {
        in = in.replaceText(TextReplacementConfig.builder().match(ANYPING_REGEX).replacement((result, builder) -> {
            builder.content(mentionsToNames(builder.content(), targetGuild));
            return builder;
        }).build());
        return in;
    }

    /**
     * Translates ID mentions (like <@userid> to human-readable mentions (like @SomeName123)
     *
     * @param in          String that should be formatted
     * @param targetGuild Target {@link Guild}, where the nicknames should be taken from
     * @return Formatted String
     */

    public static String mentionsToNames(String in, final Guild targetGuild) {
        final JDA jda = DiscordIntegration.INSTANCE.getJDA();
        if (jda == null) return in;  //Skip this if JDA wasn't initialized
        final Matcher userMatcher = USER_PING_REGEX.matcher(in);
        final Matcher roleMatcher = ROLE_PING_REGEX.matcher(in);
        final Matcher channelMatcher = CHANNEL_REGEX.matcher(in);
        while (userMatcher.find()) {
            final String str = userMatcher.group(1);
            final String id = userMatcher.group(2);
            String name;
            final User u = jda.getUserById(id);
            if (u != null) {
                final Member m = DiscordIntegration.INSTANCE.getMemberById(u.getIdLong());
                if (m != null)
                    name = m.getEffectiveName();
                else
                    name = u.getName();
                in = in.replace(str, "@" + name);
            }

        }
        while (roleMatcher.find()) {
            final String str = roleMatcher.group(1);
            final String id = roleMatcher.group(2);
            final Role role = targetGuild.getRoleById(id);
            if (role == null) continue;
            in = in.replace(str, "@" + role.getName());
        }
        while (channelMatcher.find()) {
            final String str = channelMatcher.group(1);
            final String id = channelMatcher.group(2);
            final TextChannel channel = targetGuild.getTextChannelById(id);
            if (channel == null) continue;
            in = in.replace(str, "#" + channel.getName());
        }
        return in;
    }

    /**
     * Translates emotes and emojis into their text-form
     *
     * @param emotes Array list of emotes that should be replaced (can be empty to only replace emojis)
     * @param msg    Message with emotes and/or emojis
     * @return Formatted message
     */

    public static String formatEmoteMessage(List<CustomEmoji> emotes, String msg) {
        msg = EmojiParser.parseToAliases(msg);
        for (final Emoji e : emotes) {
            msg = msg.replace(e.toString(), ":" + e.getName() + ":");
        }
        return msg;
    }

    /**
     * Gets the display name of the player's UUID
     *
     * @param uuid {@link UUID} to get the name from
     * @return The player's name, or null if the player was not found
     */
    public static @NotNull String getNameFromUUID(UUID uuid) {
        final String name = DiscordIntegration.INSTANCE.getServerInterface().getNameFromUUID(uuid);
        return name == null || name.isEmpty() ? uuid.toString() : name;
    }
}