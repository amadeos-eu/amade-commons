package eu.amadeos.commons.typeset;

/**
 * Where pages go in the reading area: one or two columns, each page with its side margins, a band at the top for
 * the running head and one at the bottom for the page number. Pages never get wider than a comfortable line
 * length; on a wide screen they stay in the middle.
 *
 * @param columns    1 or 2 pages side by side
 * @param pageWidth  width of one page including its margins
 * @param left       x of the first page within the reading area
 * @param textInset  distance from a page's left edge to its text
 * @param textTop    distance from the top of the reading area to the text
 * @param textWidth  width of the text area the typesetter fills
 * @param textHeight height of the text area
 */
public record PageGeometry(int columns, int pageWidth, int left, int textInset, int textTop, int textWidth,
                           int textHeight) {
    /** A page is at most this many em wide, about 75 characters a line. */
    static final double MAX_PAGE_EM = 40;
    /** Two pages need at least this many em each. */
    static final double MIN_TWO_PAGE_EM = 20;

    // --8<-- [start:of]
    public static PageGeometry of(int areaWidth, int areaHeight, boolean twoPages, Appearance appearance) {
        double em = appearance.size();
        int columns = twoPages && areaWidth / 2.0 >= MIN_TWO_PAGE_EM * em ? 2 : 1;
        int pageWidth = (int) Math.min(areaWidth / (double) columns, MAX_PAGE_EM * em);
        int left = (areaWidth - columns * pageWidth) / 2;
        int inset = (int) Math.max(em, Math.round(pageWidth * appearance.margin()));
        int top = (int) Math.round(2.6 * em);
        int bottom = (int) Math.round(2.8 * em);
        return new PageGeometry(columns, pageWidth, left, inset, top, Math.max(40, pageWidth - 2 * inset),
                Math.max(40, areaHeight - top - bottom));
    }
    // --8<-- [end:of]

    /** The x of the text of the page in column {@code column}. */
    public int textX(int column) {
        return left + column * pageWidth + textInset;
    }
}
