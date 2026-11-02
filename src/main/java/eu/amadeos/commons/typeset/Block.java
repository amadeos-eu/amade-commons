package eu.amadeos.commons.typeset;

import java.util.List;

/**
 * A piece of a chapter the typesetter places as a whole: a paragraph, a picture or a rule. Every block starts at
 * an {@code offset} in the chapter's plain text, which is the blocks' texts each followed by a line break
 * (a picture counts as one character). Reading positions, bookmarks and search hits are such offsets.
 * Lengths are in em of the reader's size.
 */
public sealed interface Block {
    int offset();

    /** The characters this block takes in the plain text, without the line break after it. */
    int length();

    /** Space above the block, the larger of the margins that meet there. */
    double spaceBefore();

    /** The block begins a new page. */
    boolean breakBefore();

    /**
     * A paragraph, heading, list item or line of a table.
     *
     * @param align        left, right, center or justify
     * @param keepWithNext a heading: not alone at the bottom of a page
     * @param language     the language for hyphenation, empty for the book's
     * @param caption      the caption of the picture before it, in a {@code figure} or a floating box with the
     *                     picture: the typesetter keeps the two together
     */
    record Text(int offset, String text, List<StyledRun> runs, String align, double indent, double left,
                double right, double spaceBefore, boolean breakBefore, boolean keepWithNext, boolean preformatted,
                double fontSize, String language, boolean caption) implements Block {
        public Text {
            runs = List.copyOf(runs);
        }

        @Override
        public int length() {
            return text.length();
        }
    }

    /**
     * A picture of the book.
     *
     * @param path     inside the book
     * @param floating left or right when the text flows around it, else none
     * @param width    the width the book asks for: below 0 a fraction of the page width, above 0 in em, 0 the
     *                 picture's own size
     */
    record Image(int offset, String path, String alt, String floating, double width, double height, double left,
                 double right, double spaceBefore, boolean breakBefore) implements Block {
        @Override
        public int length() {
            return 1;
        }

        public boolean floats() {
            return !floating.equals("none");
        }
    }

    /** A horizontal rule ({@code hr}): a short line in the middle. */
    record Rule(int offset, double spaceBefore, boolean breakBefore) implements Block {
        @Override
        public int length() {
            return 1;
        }
    }
}
