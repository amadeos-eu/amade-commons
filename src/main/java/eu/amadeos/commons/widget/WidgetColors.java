package eu.amadeos.commons.widget;

import eu.amadeos.commons.theme.DesktopTheme;

import java.awt.Color;

/**
 * The colours of a widget card, taken from the desktop colours as Horizon's top bar takes them: the card,
 * its text, dimmed text, a faint line and the accent colour.
 */
public record WidgetColors(Color card, Color text, Color dim, Color line, Color accent) {
    public static WidgetColors of(DesktopTheme theme) {
        Color card = theme.panelFrameBg();
        Color text = DesktopTheme.isDark(card) ? new Color(0xEC, 0xED, 0xF4) : new Color(0x1E, 0x1F, 0x29);
        return new WidgetColors(card, text, mix(card, text, 0.55), mix(card, text, 0.16), theme.activeTitleBg());
    }

    /** {@code a} with {@code weightOfB} of {@code b} mixed in. */
    public static Color mix(Color a, Color b, double weightOfB) {
        return new Color(
                (int) Math.round(a.getRed() * (1 - weightOfB) + b.getRed() * weightOfB),
                (int) Math.round(a.getGreen() * (1 - weightOfB) + b.getGreen() * weightOfB),
                (int) Math.round(a.getBlue() * (1 - weightOfB) + b.getBlue() * weightOfB));
    }
}
