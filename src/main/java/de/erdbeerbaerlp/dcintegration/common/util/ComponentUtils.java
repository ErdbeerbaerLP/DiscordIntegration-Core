package de.erdbeerbaerlp.dcintegration.common.util;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.PatternReplacementResult;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.intellij.lang.annotations.RegExp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class ComponentUtils {

    public static Style addUserHoverClick(final Style styleIn, Member m) {
        return addUserHoverClick(styleIn, m.getId(), m.getEffectiveName(), m.getUser().getAsTag());
    }

    public static Style addUserHoverClick(final Style styleIn, User u, Member m) {
        return addUserHoverClick(styleIn, u.getId(), m == null ? u.getName() : m.getEffectiveName(), u.getAsTag());

    }

    public static Style addUserHoverClick(final Style styleIn, String userID, String displayName, String tag) {
        return styleIn.clickEvent(ClickEvent.suggestCommand("<@" + userID + ">")).hoverEvent(HoverEvent.showText(Component.text(Localization.instance().discordUserHover.replace("%user#tag%", tag).replace("%user%", displayName))));
    }

    /**
     * Makes URLs in {@link Component}s clickable by adding click event and formatting
     *
     * @param in Component which might contain a URL
     * @return Component with all URLs clickable
     */

    public static Component makeURLsClickable(final Component in) {
        return in.replaceText(TextReplacementConfig.builder().match(MessageUtils.URL_PATTERN).replacement(url -> url.decorate(TextDecoration.UNDERLINED).color(TextColor.color(0x06, 0x45, 0xAD)).clickEvent(ClickEvent.openUrl(url.content()))).build());
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     *
     * @param a String/Regex which should be replaced
     * @param b Replacement String
     * @return Configured {@link TextReplacementConfig}
     */

    public static TextReplacementConfig replace(@RegExp String a, String b) {
        return TextReplacementConfig.builder().match(a).replacement(b).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     *
     * @param a Literal String which should be replaced
     * @param b Replacement String
     * @return Configured {@link TextReplacementConfig}
     */

    public static TextReplacementConfig replaceLiteral(String a, String b) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     *
     * @param a     String/Regex which should be replaced
     * @param b     Replacement String
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */

    public static TextReplacementConfig replace(@RegExp String a, String b, int times) {
        return TextReplacementConfig.builder().match(a).replacement(b).times(times).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     *
     * @param a     Literal String which should be replaced
     * @param b     Replacement String
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */

    public static TextReplacementConfig replaceLiteral(String a, String b, int times) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).times(times).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     *
     * @param a String/Regex which should be replaced
     * @param b Replacement Component
     * @return Configured {@link TextReplacementConfig}
     */

    public static TextReplacementConfig replace(@RegExp String a, Component b) {
        return TextReplacementConfig.builder().match(a).replacement(b).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     *
     * @param a Literal String which should be replaced
     * @param b Replacement Component
     * @return Configured {@link TextReplacementConfig}
     */

    public static TextReplacementConfig replaceLiteral(String a, Component b) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     *
     * @param a     String/Regex which should be replaced
     * @param b     Replacement Component
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */

    public static TextReplacementConfig replace(@RegExp String a, Component b, int times) {
        return TextReplacementConfig.builder().match(a).replacement(b).times(times).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     *
     * @param a     Literal String which should be replaced
     * @param b     Replacement Component
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */

    public static TextReplacementConfig replaceLiteral(String a, Component b, int times) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).times(times).build();
    }

    /**
     * Parses and formats Pings/Mentions<br>Every found mention will get bold and colored in {@linkplain TextColors#PING}
     *
     * @param msg  Message where Mentions should be formatted from
     * @param uuid {@link UUID} of the receiving player
     * @param name Name of the receiving player
     * @return {@link Map.Entry} containing a boolean, which is true, when there was a mention found, as key and the formatted {@link Component} as value
     */

    public static Map.Entry<Boolean, Component> parsePing(Component msg, UUID uuid, String name) {
        AtomicBoolean hasPing = new AtomicBoolean(false);
        msg = msg.replaceText(TextReplacementConfig.builder().matchLiteral("@" + name).replacement(Component.text("@" + name).style(Style.style(TextColors.PING).decorate(TextDecoration.BOLD))).condition((a, b) -> {
            hasPing.set(true);
            return PatternReplacementResult.REPLACE;
        }).build());
        if (!hasPing.get() && LinkManager.isPlayerLinked(uuid)) {
            String dcname = DiscordIntegration.INSTANCE.getChannel().getGuild().retrieveMemberById(LinkManager.getLink(null,uuid).discordID).complete().getEffectiveName();
            msg = msg.replaceText(TextReplacementConfig.builder().matchLiteral("@" + dcname).replacement(Component.text("@" + dcname).style(Style.style(TextColors.PING).decorate(TextDecoration.BOLD))).condition((a, b) -> {
                hasPing.set(true);
                return PatternReplacementResult.REPLACE;
            }).build());
        }
        return new DefaultMapEntry<>(hasPing.get(), msg);
    }


    public static Component append(final Component base, final Component toAppend) {
        return base.append(toAppend);
    }
}