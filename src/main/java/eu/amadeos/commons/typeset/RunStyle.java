package eu.amadeos.commons.typeset;

import java.util.List;

/**
 * How a stretch of text inside a paragraph looks, relative to the reader's choices.
 *
 * @param fontSize      relative to the reader's size
 * @param uppercase     shown in capitals ({@code text-transform: uppercase})
 * @param families      the book's font families, for the publisher's font
 * @param verticalAlign 1 superscript, -1 subscript, 0 baseline
 * @param link          where a link leads: {@code chapter-path#fragment} inside the book or a web address; null
 *                      for plain text
 * @param noteRef       the link points to a footnote ({@code epub:type="noteref"})
 */
public record RunStyle(double fontSize, boolean italic, boolean bold, boolean smallCaps, boolean uppercase,
                       boolean monospace, List<String> families, int verticalAlign, String link, boolean noteRef) {

    static RunStyle of(Style style, String link, boolean noteRef) {
        return new RunStyle(style.fontSize(), style.italic(), style.bold(), style.smallCaps(),
                style.transform().equals("uppercase"), style.monospace(), style.families(), style.verticalAlign(),
                link, noteRef);
    }
}
