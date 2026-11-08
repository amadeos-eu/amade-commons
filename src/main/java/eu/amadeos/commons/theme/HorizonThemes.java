package eu.amadeos.commons.theme;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The colours of the Horizon look: a dark or a light scheme in one of its palettes, with one of six accent
 * colours. Each combination is a {@link DesktopTheme} with the id {@code horizon-<scheme>-<accent>} (for example
 * {@code horizon-dark-teal}) for the scheme's default palette and {@code horizon-<scheme>-<accent>-<palette>}
 * (for example {@code horizon-light-teal-mist}) for the others, so that the desktop processes exchange it like a
 * classic palette. The accent is the theme's active title colour: the line on the active window, selections and
 * the default button.
 */
public final class HorizonThemes {
    /** The prefix of every Horizon theme id. */
    public static final String PREFIX = "horizon-";
    /** The icons of the Horizon look (package amadeos-icons-horizon); NsCDE fills in what it does not have. */
    public static final String ICON_THEME = "amadeos-horizon";

    /** Dark or light. */
    public enum Scheme {
        DARK, LIGHT;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Scheme of(String id) {
            for (Scheme s : values()) if (s.id().equalsIgnoreCase(id == null ? "" : id.strip())) return s;
            return DARK;
        }
    }

    /** The accent colours. */
    public enum Accent {
        TEAL(new Color(0x2F, 0xB8, 0xB0)),
        VIOLET(new Color(0x8A, 0x6E, 0xE6)),
        AMBER(new Color(0xE6, 0x9A, 0x2E)),
        RED(new Color(0xE0, 0x5A, 0x5A)),
        GREEN(new Color(0x4C, 0xB0, 0x6A)),
        BLUE(new Color(0x3E, 0x8E, 0xE0));

        private final Color color;

        Accent(Color color) {
            this.color = color;
        }

        public Color color() {
            return color;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Accent of(String id) {
            for (Accent a : values()) if (a.id().equalsIgnoreCase(id == null ? "" : id.strip())) return a;
            return TEAL;
        }
    }

    // --8<-- [start:palette]
    /**
     * The palettes of the schemes: the colours of windows, fields and bars. The first palette of each scheme is
     * its default (Night and Paper, the colours Horizon started with); the others are calmer variations, for
     * example a light scheme that is less bright.
     */
    public enum Palette {
        //    scheme        base      highlight soft      shadow    dark sh.  fields    inactive  text      inact.txt bar       bar hi.   bar shadow
        NIGHT(Scheme.DARK, 0x242531, 0x4A4C5E, 0x343646, 0x181922, 0x0E0F15, 0x1C1D27, 0x2C2D3A, 0xE8E9F1, 0x8C8EA2, 0x14151D, 0x3A3C4C, 0x0A0A10),
        GRAPHITE(Scheme.DARK, 0x2A2A2D, 0x505054, 0x3A3A3E, 0x1C1C1F, 0x101012, 0x212124, 0x333337, 0xEAEAEC, 0x909096, 0x1A1A1D, 0x404044, 0x0B0B0D),
        OCEAN(Scheme.DARK, 0x1E2633, 0x43526A, 0x2C3748, 0x141A24, 0x0B0F16, 0x18202B, 0x263040, 0xE4EAF2, 0x8A97AA, 0x121822, 0x344156, 0x080B10),
        PAPER(Scheme.LIGHT, 0xF3F3F6, 0xFFFFFF, 0xFAFAFC, 0xC6C7D0, 0x8A8C99, 0xFFFFFF, 0xE6E6EB, 0x1E1F29, 0x8A8C99, 0xECECF1, 0xFFFFFF, 0xB8B9C4),
        MIST(Scheme.LIGHT, 0xE4E6EB, 0xF2F3F6, 0xEBECF0, 0xB4B7C2, 0x7C7F8C, 0xF4F5F8, 0xD8DAE0, 0x1E1F29, 0x737684, 0xDCDEE4, 0xEEF0F3, 0xA8ABB6),
        SAND(Scheme.LIGHT, 0xEAE4DA, 0xF6F2EB, 0xF0EBE3, 0xC2B9AA, 0x8A8172, 0xF8F5EF, 0xDDD6CA, 0x2A261F, 0x7D7568, 0xE1DACE, 0xF3EEE6, 0xB2A898),
        SAGE(Scheme.LIGHT, 0xE1E8E2, 0xF0F4F0, 0xE8EEE9, 0xAFBCB1, 0x758479, 0xF4F7F4, 0xD4DDD5, 0x1F2A22, 0x6E7D72, 0xD8E0D9, 0xECF1EC, 0xA3B0A5),
        STONE(Scheme.LIGHT, 0xD3D5DB, 0xE5E6EB, 0xDBDDE2, 0xA2A5B0, 0x6A6D79, 0xEAEBEF, 0xC5C8CF, 0x15161E, 0x585B68, 0xC9CCD3, 0xDEE0E5, 0x9599A5);

        private final Scheme scheme;
        final Color base, highlight, softHighlight, shadow, darkShadow, sunken, inactiveTitle, text, inactiveText,
                bar, barHighlight, barShadow;

        Palette(Scheme scheme, int base, int highlight, int softHighlight, int shadow, int darkShadow, int sunken,
                int inactiveTitle, int text, int inactiveText, int bar, int barHighlight, int barShadow) {
            this.scheme = scheme;
            this.base = new Color(base);
            this.highlight = new Color(highlight);
            this.softHighlight = new Color(softHighlight);
            this.shadow = new Color(shadow);
            this.darkShadow = new Color(darkShadow);
            this.sunken = new Color(sunken);
            this.inactiveTitle = new Color(inactiveTitle);
            this.text = new Color(text);
            this.inactiveText = new Color(inactiveText);
            this.bar = new Color(bar);
            this.barHighlight = new Color(barHighlight);
            this.barShadow = new Color(barShadow);
        }

        public Scheme scheme() {
            return scheme;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The English name, for example {@code Mist}; the programs translate {@link #id()} themselves. */
        public String displayName() {
            return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
        }

        /** Whether this is its scheme's default palette. */
        public boolean isStandard() {
            return this == standard(scheme);
        }

        /** The window background, for previews. */
        public Color base() {
            return base;
        }

        /** The background of fields and lists, for previews. */
        public Color fields() {
            return sunken;
        }

        /** The background of the top bar and the dock, for previews. */
        public Color bar() {
            return bar;
        }

        /** The text colour, for previews. */
        public Color text() {
            return text;
        }

        /** The default palette of a scheme: Night or Paper. */
        public static Palette standard(Scheme scheme) {
            return scheme == Scheme.LIGHT ? PAPER : NIGHT;
        }

        /** The palettes of a scheme, the default first. */
        public static List<Palette> of(Scheme scheme) {
            return java.util.Arrays.stream(values()).filter(p -> p.scheme == scheme).toList();
        }

        /** The palette with this id in the scheme; the scheme's default if there is none. */
        public static Palette of(Scheme scheme, String id) {
            for (Palette p : values()) {
                if (p.scheme == scheme && p.id().equalsIgnoreCase(id == null ? "" : id.strip())) return p;
            }
            return standard(scheme);
        }
    }
    // --8<-- [end:palette]

    public static final Scheme DEFAULT_SCHEME = Scheme.DARK;
    public static final Accent DEFAULT_ACCENT = Accent.TEAL;

    private HorizonThemes() {
    }

    /** The theme id of a combination in the scheme's default palette: {@code horizon-dark-teal}. */
    public static String id(Scheme scheme, Accent accent) {
        return PREFIX + scheme.id() + "-" + accent.id();
    }

    /**
     * The theme id of a combination: {@code horizon-light-teal-mist}, or without the palette for the scheme's
     * default one ({@code horizon-light-teal}), the id older amadeOS versions know.
     */
    public static String id(Scheme scheme, Accent accent, Palette palette) {
        Palette p = palette == null || palette.scheme() != scheme ? Palette.standard(scheme) : palette;
        return id(scheme, accent) + (p.isStandard() ? "" : "-" + p.id());
    }

    /**
     * The theme id that {@code look.conf} names with its {@code scheme}, {@code accent} and {@code palette}
     * lines.
     */
    public static String id(java.util.Properties look) {
        Scheme scheme = Scheme.of(look.getProperty("scheme"));
        return id(scheme, Accent.of(look.getProperty("accent")), Palette.of(scheme, look.getProperty("palette")));
    }

    /** Whether the id names a Horizon theme. */
    public static boolean isHorizon(String themeId) {
        return themeId != null && themeId.startsWith(PREFIX);
    }

    /** The scheme of a Horizon theme id; {@link #DEFAULT_SCHEME} if it is none. */
    public static Scheme scheme(String themeId) {
        String[] parts = parts(themeId);
        return parts == null ? DEFAULT_SCHEME : Scheme.of(parts[0]);
    }

    /** The accent of a Horizon theme id; {@link #DEFAULT_ACCENT} if it is none. */
    public static Accent accent(String themeId) {
        String[] parts = parts(themeId);
        return parts == null ? DEFAULT_ACCENT : Accent.of(parts[1]);
    }

    /** The palette of a Horizon theme id; the default palette of its scheme if the id names none. */
    public static Palette palette(String themeId) {
        String[] parts = parts(themeId);
        Scheme scheme = parts == null ? DEFAULT_SCHEME : Scheme.of(parts[0]);
        return Palette.of(scheme, parts == null || parts.length < 3 ? null : parts[2]);
    }

    private static String[] parts(String themeId) {
        if (!isHorizon(themeId)) return null;
        String[] parts = themeId.strip().substring(PREFIX.length()).split("-", 3);
        return parts.length >= 2 ? parts : null;
    }

    /** The theme of a Horizon id such as {@code horizon-light-amber} or {@code horizon-light-amber-sand}. */
    public static Optional<DesktopTheme> find(String themeId) {
        String[] parts = parts(themeId);
        if (parts == null) return Optional.empty();
        Scheme scheme = Scheme.of(parts[0]);
        return Optional.of(theme(scheme, Accent.of(parts[1]), Palette.of(scheme, parts.length > 2 ? parts[2] : null)));
    }

    // --8<-- [start:theme]
    /** The colours of a scheme with an accent, in the scheme's default palette. */
    public static DesktopTheme theme(Scheme scheme, Accent accent) {
        return theme(scheme, accent, Palette.standard(scheme));
    }

    /** The colours of a scheme with an accent and a palette; a palette of the other scheme gives the default. */
    public static DesktopTheme theme(Scheme scheme, Accent accent, Palette palette) {
        Palette p = palette.scheme() == scheme ? palette : Palette.standard(scheme);
        Color a = accent.color();
        String name = "Horizon " + (scheme == Scheme.DARK ? "Dark" : "Light")
                + (p.isStandard() ? "" : " (" + p.displayName() + ")");
        List<Color> workspaces = scheme == Scheme.DARK
                ? List.of(a, new Color(0x5A, 0x6A, 0x9A), new Color(0x4A, 0x8A, 0x8A), new Color(0x8A, 0x6A, 0x9A))
                : List.of(a, new Color(0x7A, 0x8A, 0xC0), new Color(0x6A, 0xAA, 0xAA), new Color(0xAA, 0x8A, 0xBA));
        return new DesktopTheme(id(scheme, accent, p), name,
                p.base, p.highlight, p.softHighlight, p.shadow, p.darkShadow, p.sunken,
                a,                                 // active title: the accent
                p.inactiveTitle, p.text, p.inactiveText,
                ICON_THEME,
                p.bar, p.barHighlight, p.barShadow,
                workspaces);
    }
    // --8<-- [end:theme]
}
