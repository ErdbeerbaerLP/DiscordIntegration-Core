package de.erdbeerbaerlp.dcintegration.common.util;

import net.kyori.adventure.text.format.TextColor;

import java.awt.*;

@SuppressWarnings("unused")
public class TextColors {


    /**
     * Color used for mentions / pings ingame
     */
    public static final TextColor PING = TextColor.color(209, 170, 63);
    /**
     * Official discord color, see <a href="https://discord.com/branding">https://discord.com/branding</a>
     */
    public static final TextColor DISCORD_BLURPLE = TextColor.color(0x58, 0x65, 0xF2);
    /**
     * Official discord color, see <a href="https://discord.com/branding">https://discord.com/branding</a>
     */
    public static final TextColor DISCORD_GREEN = TextColor.color(0x57, 0xF2, 0x87);
    /**
     * Official discord color, see <a href="https://discord.com/branding">https://discord.com/branding</a>
     */
    public static final TextColor DISCORD_YELLOW = TextColor.color(0xFE, 0xE7, 0x5C);
    /**
     * Official discord color, see <a href="https://discord.com/branding">https://discord.com/branding</a>
     */
    public static final TextColor DISCORD_FUCHSIA = TextColor.color(0xEB, 0x45, 0x9E);
    /**
     * Official discord color, see <a href="https://discord.com/branding">https://discord.com/branding</a>
     */
    public static final TextColor DISCORD_RED = TextColor.color(0xED, 0x42, 0x45);
    /**
     * Simple white
     */
    public static final TextColor WHITE = TextColor.color(0xFF, 0xFF, 0xFF);
    /**
     * Simple black
     */
    public static final TextColor BLACK = TextColor.color(0, 0, 0);

    /**
     * Converts the given {@link Color} to an {@link TextColor}
     *
     * @param c {@link Color} to convert
     * @return Converted {@link TextColor}
     */

    public static TextColor of(Color c) {
        return TextColor.color(c.getRed(), c.getGreen(), c.getBlue());
    }

}
