package eu.amadeos.commons.print;

import javax.print.attribute.standard.MediaSizeName;
import java.util.Locale;
import java.util.Set;

/**
 * Paper sizes in portrait, in points (1/72 inch), the unit of {@link java.awt.print.Paper} and of the
 * document formats.
 */
public enum PaperSize {
    A5("A5", 148, 210, MediaSizeName.ISO_A5),
    A4("A4", 210, 297, MediaSizeName.ISO_A4),
    LETTER("Letter", 215.9, 279.4, MediaSizeName.NA_LETTER),
    LEGAL("Legal", 215.9, 355.6, MediaSizeName.NA_LEGAL);

    /** Countries where Letter is the usual paper; everywhere else it is A4. */
    private static final Set<String> LETTER_COUNTRIES = Set.of("US", "CA", "MX", "PH", "CL", "CO", "VE", "GT", "PR");

    private final String label;
    private final double widthMm;
    private final double heightMm;
    private final MediaSizeName media;

    PaperSize(String label, double widthMm, double heightMm, MediaSizeName media) {
        this.label = label;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.media = media;
    }

    // --8<-- [start:for-locale]
    /** The usual paper where the user lives: Letter in North America and a few other countries, else A4. */
    public static PaperSize forLocale(Locale locale) {
        return LETTER_COUNTRIES.contains(locale.getCountry()) ? LETTER : A4;
    }
    // --8<-- [end:for-locale]

    public String label() {
        return label;
    }

    /** Width in portrait, in points. */
    public double width() {
        return toPoints(widthMm);
    }

    /** Height in portrait, in points. */
    public double height() {
        return toPoints(heightMm);
    }

    /** The paper the printer should take. */
    public MediaSizeName media() {
        return media;
    }

    @Override
    public String toString() {
        return label;
    }

    private static double toPoints(double mm) {
        return mm / 25.4 * 72;
    }
}
