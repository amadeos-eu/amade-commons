package org.datazuul.amadeos.commons.theme;

import java.awt.Color;
import java.util.Objects;

/**
 * Colour scheme of the amadeOS desktop. Missing colours are derived from the base colour, as CDE did
 * for its palettes. The built-in themes are the same in the desktop (window manager, front panel) and in
 * every application.
 */
public final class DesktopTheme {
    /**
     * The icon theme used unless a theme or {@code icon_theme.conf} names another: the NsCDE theme
     * (package {@code nscde-icon-theme}, {@code /usr/share/icons/NsCDE}), which the amadeOS image installs.
     */
    public static final String DEFAULT_ICON_THEME = "NsCDE";

    private final String id;
    private final String name;
    private final Color base;
    private final Color highlight;
    private final Color softHighlight;
    private final Color shadow;
    private final Color darkShadow;
    private final Color sunkenBg;
    private final Color activeTitleBg;
    private final Color inactiveTitleBg;
    private final Color textActive;
    private final Color textInactive;
    private final String iconTheme;
    private final Color panelFrameBg;
    private final Color panelFrameHighlight;
    private final Color panelFrameShadow;

    public DesktopTheme(String id, String name, Color base, Color highlight, Color softHighlight,
                        Color shadow, Color darkShadow, Color sunkenBg, Color activeTitleBg,
                        Color inactiveTitleBg, Color textActive, Color textInactive, String iconTheme) {
        this(id, name, base, highlight, softHighlight, shadow, darkShadow, sunkenBg,
                activeTitleBg, inactiveTitleBg, textActive, textInactive, iconTheme,
                null, null, null);
    }

    public DesktopTheme(String id, String name, Color base, Color highlight, Color softHighlight,
                        Color shadow, Color darkShadow, Color sunkenBg, Color activeTitleBg,
                        Color inactiveTitleBg, Color textActive, Color textInactive, String iconTheme,
                        Color panelFrameBg, Color panelFrameHighlight, Color panelFrameShadow) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.base = Objects.requireNonNull(base, "base");
        this.highlight = highlight != null ? highlight : deriveHighlight(base);
        this.softHighlight = softHighlight != null ? softHighlight : deriveSoftHighlight(base);
        this.shadow = shadow != null ? shadow : deriveShadow(base);
        this.darkShadow = darkShadow != null ? darkShadow : deriveDarkShadow(base);
        this.sunkenBg = sunkenBg != null ? sunkenBg : deriveSunken(base);
        this.activeTitleBg = activeTitleBg != null ? activeTitleBg : base;
        this.inactiveTitleBg = inactiveTitleBg != null ? inactiveTitleBg : deriveInactive(base);
        this.textActive = textActive != null ? textActive : (isDark(base) ? Color.WHITE : Color.BLACK);
        this.textInactive = textInactive != null ? textInactive : (isDark(base) ? new Color(0xAA, 0xAA, 0xAA) : new Color(0x70, 0x70, 0x70));
        this.iconTheme = iconTheme != null && !iconTheme.isBlank() ? iconTheme : DEFAULT_ICON_THEME;
        this.panelFrameBg = panelFrameBg != null ? panelFrameBg : derivePanelFrameBg(base, shadow);
        this.panelFrameHighlight = panelFrameHighlight != null ? panelFrameHighlight : derivePanelFrameHighlight(this.panelFrameBg);
        this.panelFrameShadow = panelFrameShadow != null ? panelFrameShadow : derivePanelFrameShadow(this.panelFrameBg);
    }

    public static DesktopTheme fromBase(String id, String name, Color base) {
        return new DesktopTheme(id, name, base, null, null, null, null, null, null, null, null, null, DEFAULT_ICON_THEME);
    }

    public DesktopTheme withIconTheme(String newIconTheme) {
        return new DesktopTheme(id, name, base, highlight, softHighlight, shadow, darkShadow, sunkenBg,
                activeTitleBg, inactiveTitleBg, textActive, textInactive, newIconTheme,
                panelFrameBg, panelFrameHighlight, panelFrameShadow);
    }

    // Built-in theme definitions
    public static final DesktopTheme WARM_MOTIF = new DesktopTheme(
            "warm-motif", "Warm Motif (Default)",
            new Color(0xCE, 0xC2, 0xBB),
            Color.WHITE,
            new Color(0xEA, 0xE5, 0xE2),
            new Color(0x8C, 0x82, 0x7A),
            new Color(0x42, 0x3B, 0x35),
            new Color(0xBA, 0xAF, 0xA8),
            new Color(0x4E, 0x7D, 0x96), // authentic UNIX teal/slate active titlebar
            new Color(0xBF, 0xB6, 0xAF),
            Color.BLACK,
            new Color(0x75, 0x6E, 0x68),
            DEFAULT_ICON_THEME,
            new Color(0x99, 0x99, 0x99), // Authentic CDE neutral gray frame
            new Color(0xD1, 0xD1, 0xD2),
            new Color(0x4E, 0x4E, 0x4E)
    );

    public static final DesktopTheme CDE_BROCADE = new DesktopTheme(
            "cde-brocade", "CDE Brocade (Teal / Blue)",
            new Color(0x66, 0x80, 0x99),
            new Color(0xA8, 0xBD, 0xD1),
            new Color(0x85, 0x9E, 0xB8),
            new Color(0x46, 0x5A, 0x6E),
            new Color(0x25, 0x34, 0x42),
            new Color(0x54, 0x6C, 0x82),
            new Color(0x7A, 0x96, 0xB0),
            new Color(0x5C, 0x73, 0x8A),
            Color.WHITE,
            new Color(0xCA, 0xD6, 0xE2),
            DEFAULT_ICON_THEME,
            new Color(0x4E, 0x5F, 0x70), // Darker slate-blue frame
            new Color(0x76, 0x8C, 0xA1),
            new Color(0x27, 0x33, 0x3D)
    );

    public static final DesktopTheme CDE_DESERT = new DesktopTheme(
            "cde-desert", "CDE Desert (Sand / Amber)",
            new Color(0xC4, 0xA6, 0x7A),
            new Color(0xF5, 0xE0, 0xC2),
            new Color(0xDB, 0xBE, 0x93),
            new Color(0x8A, 0x70, 0x4D),
            new Color(0x47, 0x36, 0x21),
            new Color(0xB0, 0x92, 0x68),
            new Color(0xC4, 0xA6, 0x7A),
            new Color(0xA8, 0x8D, 0x65),
            Color.BLACK,
            new Color(0x59, 0x44, 0x2C),
            DEFAULT_ICON_THEME,
            new Color(0x8A, 0x70, 0x4D), // Amber shadow frame
            new Color(0xB8, 0x9C, 0x73),
            new Color(0x4A, 0x3B, 0x26)
    );

    public static final DesktopTheme CLASSIC_GRAY = new DesktopTheme(
            "classic-gray", "Classic Gray",
            new Color(0xC0, 0xC0, 0xC0),
            Color.WHITE,
            new Color(0xE0, 0xE0, 0xE0),
            new Color(0x80, 0x80, 0x80),
            new Color(0x40, 0x40, 0x40),
            new Color(0xA8, 0xA8, 0xA8),
            new Color(0xC0, 0xC0, 0xC0),
            new Color(0xAF, 0xAF, 0xAF),
            Color.BLACK,
            new Color(0x60, 0x60, 0x60),
            DEFAULT_ICON_THEME,
            new Color(0x99, 0x99, 0x99),
            new Color(0xD1, 0xD1, 0xD2),
            new Color(0x4E, 0x4E, 0x4E)
    );

    public static final DesktopTheme DARK_SLATE = new DesktopTheme(
            "dark-slate", "Dark Slate (Dark Mode)",
            new Color(0x38, 0x3E, 0x4A),
            new Color(0x65, 0x6F, 0x82),
            new Color(0x4B, 0x53, 0x63),
            new Color(0x24, 0x28, 0x30),
            new Color(0x13, 0x15, 0x1A),
            new Color(0x2B, 0x30, 0x3A),
            new Color(0x47, 0x4E, 0x5D),
            new Color(0x30, 0x35, 0x3F),
            new Color(0xE6, 0xE9, 0xF0),
            new Color(0x8E, 0x95, 0xA5),
            DEFAULT_ICON_THEME,
            new Color(0x24, 0x28, 0x30), // Dark charcoal frame
            new Color(0x47, 0x4E, 0x5D),
            new Color(0x13, 0x15, 0x1A)
    );

    public String id() { return id; }
    public String name() { return name; }
    public Color base() { return base; }
    public Color highlight() { return highlight; }
    public Color softHighlight() { return softHighlight; }
    public Color shadow() { return shadow; }
    public Color darkShadow() { return darkShadow; }
    public Color sunkenBg() { return sunkenBg; }
    public Color activeTitleBg() { return activeTitleBg; }
    public Color inactiveTitleBg() { return inactiveTitleBg; }
    public Color textActive() { return textActive; }
    public Color textInactive() { return textInactive; }
    public String iconTheme() { return iconTheme; }
    public Color panelFrameBg() { return panelFrameBg; }
    public Color panelFrameHighlight() { return panelFrameHighlight; }
    public Color panelFrameShadow() { return panelFrameShadow; }

    /** True for themes with light text on a dark base, e.g. Dark Slate. */
    public boolean isDark() {
        return isDark(base);
    }

    /**
     * Background of editable and list content (text fields, tables, trees, message bodies): white in light
     * themes, a dark tone in dark themes so that the light {@link #textActive()} stays readable.
     */
    public Color fieldBackground() {
        return isDark() ? mix(darkShadow, base, 0.45) : Color.WHITE;
    }

    /** Text on {@link #fieldBackground()}. */
    public Color fieldText() {
        return isDark() ? textActive : new Color(0x11, 0x11, 0x11);
    }

    /** Selection in lists, tables and text: the active title bar colour, as in CDE. */
    public Color selectionBackground() {
        return activeTitleBg;
    }

    /** Text on {@link #selectionBackground()}, black or white whichever contrasts more. */
    public Color selectionText() {
        return isDark(activeTitleBg) ? Color.WHITE : Color.BLACK;
    }

    private static Color mix(Color a, Color b, double weightOfB) {
        return new Color(
                (int) Math.round(a.getRed() * (1 - weightOfB) + b.getRed() * weightOfB),
                (int) Math.round(a.getGreen() * (1 - weightOfB) + b.getGreen() * weightOfB),
                (int) Math.round(a.getBlue() * (1 - weightOfB) + b.getBlue() * weightOfB));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DesktopTheme other && id.equals(other.id) && iconTheme.equals(other.iconTheme);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, iconTheme);
    }

    @Override
    public String toString() {
        return name;
    }

    public static boolean isDark(Color c) {
        double luminance = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
        return luminance < 0.5;
    }

    private static Color deriveHighlight(Color c) {
        return Color.WHITE;
    }

    private static Color deriveSoftHighlight(Color c) {
        return new Color(
                Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * 0.45)),
                Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * 0.45)),
                Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * 0.45))
        );
    }

    private static Color deriveShadow(Color c) {
        return new Color(
                (int) (c.getRed() * 0.68),
                (int) (c.getGreen() * 0.68),
                (int) (c.getBlue() * 0.68)
        );
    }

    private static Color deriveDarkShadow(Color c) {
        return new Color(
                (int) (c.getRed() * 0.35),
                (int) (c.getGreen() * 0.35),
                (int) (c.getBlue() * 0.35)
        );
    }

    private static Color deriveSunken(Color c) {
        return new Color(
                (int) (c.getRed() * 0.88),
                (int) (c.getGreen() * 0.88),
                (int) (c.getBlue() * 0.88)
        );
    }

    private static Color deriveInactive(Color c) {
        return new Color(
                (int) (c.getRed() * 0.92),
                (int) (c.getGreen() * 0.92),
                (int) (c.getBlue() * 0.92)
        );
    }

    private static Color derivePanelFrameBg(Color base, Color shadow) {
        if (isDark(base)) {
            return new Color((int) (base.getRed() * 0.75), (int) (base.getGreen() * 0.75), (int) (base.getBlue() * 0.75));
        }
        return new Color(0x99, 0x99, 0x99);
    }

    private static Color derivePanelFrameHighlight(Color frameBg) {
        return new Color(
                Math.min(255, (int) (frameBg.getRed() + (255 - frameBg.getRed()) * 0.55)),
                Math.min(255, (int) (frameBg.getGreen() + (255 - frameBg.getGreen()) * 0.55)),
                Math.min(255, (int) (frameBg.getBlue() + (255 - frameBg.getBlue()) * 0.55))
        );
    }

    private static Color derivePanelFrameShadow(Color frameBg) {
        return new Color(
                (int) (frameBg.getRed() * 0.51),
                (int) (frameBg.getGreen() * 0.51),
                (int) (frameBg.getBlue() * 0.51)
        );
    }
}
