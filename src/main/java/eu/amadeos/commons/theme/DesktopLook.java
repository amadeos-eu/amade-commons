package eu.amadeos.commons.theme;

import java.util.Locale;

/**
 * The overall look of the desktop, chosen in Settings &gt; Appearance and stored in
 * {@code ~/.config/amadeos/look.conf}: the classic CDE look (Front Panel, Motif frames, the classic palettes)
 * or Horizon (top bar, flat frames, dark or light colours with an accent colour).
 */
public enum DesktopLook {
    CLASSIC, HORIZON;

    /** The value in {@code look.conf}: {@code classic} or {@code horizon}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The look of a {@code look.conf} value; {@link #CLASSIC} for anything unknown. */
    public static DesktopLook of(String id) {
        if (id != null) {
            for (DesktopLook look : values()) {
                if (look.id().equalsIgnoreCase(id.strip())) return look;
            }
        }
        return CLASSIC;
    }
}
