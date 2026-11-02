package eu.amadeos.commons.typeset;

/**
 * How the reader wants the text: these choices win over the book's style sheet.
 *
 * @param family        the font family; {@link #PUBLISHER} uses the fonts the book brings, where it has any
 * @param size          the size of body text in pixels
 * @param lineSpacing   1 for solid lines, 1.5 for half a line more between them
 * @param margin        the side margins as a fraction of the page width, 0 to 0.25
 * @param justify       paragraphs the book sets left aligned or justified are justified; false sets them left
 * @param hyphenate     break words at the end of lines where the language's patterns allow it
 */
public record Appearance(String family, int size, double lineSpacing, double margin, boolean justify,
                         boolean hyphenate) {
    public static final String PUBLISHER = "*publisher*";
    public static final Appearance DEFAULT = new Appearance("DejaVu Serif", 18, 1.4, 0.08, true, true);

    public Appearance {
        size = Math.clamp(size, 8, 48);
        lineSpacing = Math.clamp(lineSpacing, 1.0, 2.5);
        margin = Math.clamp(margin, 0.0, 0.25);
    }

    public boolean publisherFont() {
        return PUBLISHER.equals(family);
    }

    public Appearance withFamily(String newFamily) {
        return new Appearance(newFamily, size, lineSpacing, margin, justify, hyphenate);
    }

    public Appearance withSize(int newSize) {
        return new Appearance(family, newSize, lineSpacing, margin, justify, hyphenate);
    }

    public Appearance withLineSpacing(double spacing) {
        return new Appearance(family, size, spacing, margin, justify, hyphenate);
    }

    public Appearance withMargin(double newMargin) {
        return new Appearance(family, size, lineSpacing, newMargin, justify, hyphenate);
    }

    public Appearance withJustify(boolean on) {
        return new Appearance(family, size, lineSpacing, margin, on, hyphenate);
    }

    public Appearance withHyphenate(boolean on) {
        return new Appearance(family, size, lineSpacing, margin, justify, on);
    }
}
