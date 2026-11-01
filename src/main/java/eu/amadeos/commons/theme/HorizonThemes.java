package eu.amadeos.commons.theme;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The colours of the Horizon look: a dark or a light scheme with one of six accent colours. Each combination
 * is a {@link DesktopTheme} with the id {@code horizon-<scheme>-<accent>} (for example
 * {@code horizon-dark-teal}), so that the desktop processes exchange it like a classic palette. The accent is
 * the theme's active title colour: the line on the active window, selections and the default button.
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

    public static final Scheme DEFAULT_SCHEME = Scheme.DARK;
    public static final Accent DEFAULT_ACCENT = Accent.TEAL;

    private HorizonThemes() {
    }

    /** The theme id of a combination: {@code horizon-dark-teal}. */
    public static String id(Scheme scheme, Accent accent) {
        return PREFIX + scheme.id() + "-" + accent.id();
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

    private static String[] parts(String themeId) {
        if (!isHorizon(themeId)) return null;
        String[] parts = themeId.strip().substring(PREFIX.length()).split("-", 2);
        return parts.length == 2 ? parts : null;
    }

    /** The theme of a Horizon id such as {@code horizon-light-amber}, if it is one. */
    public static Optional<DesktopTheme> find(String themeId) {
        String[] parts = parts(themeId);
        if (parts == null) return Optional.empty();
        return Optional.of(theme(Scheme.of(parts[0]), Accent.of(parts[1])));
    }

    // --8<-- [start:theme]
    /** The colours of a scheme with an accent. */
    public static DesktopTheme theme(Scheme scheme, Accent accent) {
        Color a = accent.color();
        String name = "Horizon " + (scheme == Scheme.DARK ? "Dark" : "Light");
        if (scheme == Scheme.DARK) {
            return new DesktopTheme(id(scheme, accent), name,
                    new Color(0x24, 0x25, 0x31),   // base: window background
                    new Color(0x4A, 0x4C, 0x5E),   // highlight
                    new Color(0x34, 0x36, 0x46),   // soft highlight
                    new Color(0x18, 0x19, 0x22),   // shadow
                    new Color(0x0E, 0x0F, 0x15),   // dark shadow
                    new Color(0x1C, 0x1D, 0x27),   // sunken: fields and lists
                    a,                             // active title: the accent
                    new Color(0x2C, 0x2D, 0x3A),   // inactive title
                    new Color(0xE8, 0xE9, 0xF1),   // text
                    new Color(0x8C, 0x8E, 0xA2),   // inactive text
                    ICON_THEME,
                    new Color(0x14, 0x15, 0x1D),   // bars (top bar, dock)
                    new Color(0x3A, 0x3C, 0x4C),
                    new Color(0x0A, 0x0A, 0x10),
                    List.of(a, new Color(0x5A, 0x6A, 0x9A), new Color(0x4A, 0x8A, 0x8A), new Color(0x8A, 0x6A, 0x9A)));
        }
        return new DesktopTheme(id(scheme, accent), name,
                new Color(0xF3, 0xF3, 0xF6),
                Color.WHITE,
                new Color(0xFA, 0xFA, 0xFC),
                new Color(0xC6, 0xC7, 0xD0),
                new Color(0x8A, 0x8C, 0x99),
                new Color(0xFF, 0xFF, 0xFF),
                a,
                new Color(0xE6, 0xE6, 0xEB),
                new Color(0x1E, 0x1F, 0x29),
                new Color(0x8A, 0x8C, 0x99),
                ICON_THEME,
                new Color(0xEC, 0xEC, 0xF1),
                Color.WHITE,
                new Color(0xB8, 0xB9, 0xC4),
                List.of(a, new Color(0x7A, 0x8A, 0xC0), new Color(0x6A, 0xAA, 0xAA), new Color(0xAA, 0x8A, 0xBA)));
    }
    // --8<-- [end:theme]
}
