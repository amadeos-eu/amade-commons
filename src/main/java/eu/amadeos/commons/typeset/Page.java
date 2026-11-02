package eu.amadeos.commons.typeset;

import java.awt.Shape;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * One page of a laid-out chapter: lines, pictures and rules at their places inside the text area, whose top
 * left corner is (0, 0).
 *
 * @param start the offset of the first character or picture on the page
 * @param end   the offset after the last one
 */
public record Page(List<Line> lines, List<Picture> pictures, List<Rule> rules, int start, int end) {
    public Page {
        lines = List.copyOf(lines);
        pictures = List.copyOf(pictures);
        rules = List.copyOf(rules);
    }

    public boolean contains(int offset) {
        return offset >= start && offset < end;
    }

    /**
     * A line of text.
     *
     * @param start  the chapter offset of the line's first character
     * @param end    the offset after its last character; a hyphen added at a break is not counted
     * @param hyphen the line ends with a hyphen the typesetter added
     */
    public record Line(TextLayout layout, float x, float baseline, int start, int end, boolean hyphen) {
        public float top() {
            return baseline - layout.getAscent();
        }

        public float bottom() {
            return baseline + layout.getDescent();
        }

        /** The chapter offset nearest to the x position. */
        public int offsetAt(float px) {
            TextHitInfo hit = layout.hitTestChar(px - x, 0);
            return start + Math.clamp(hit.getInsertionIndex(), 0, end - start);
        }

        /** The area behind the characters from {@code from} to {@code to}; null when the line has none of them. */
        public Shape highlight(int from, int to) {
            int a = Math.max(from, start) - start;
            int b = Math.min(to, end) - start;
            if (a >= b) return null;
            Shape shape = layout.getLogicalHighlightShape(a, b);
            return AffineTransform.getTranslateInstance(x, baseline).createTransformedShape(shape);
        }
    }

    /** A picture of the book, scaled into the box. */
    public record Picture(String path, String alt, float x, float y, float width, float height, int offset) {
    }

    /** A horizontal rule from x1 to x2. */
    public record Rule(float x1, float x2, float y, int offset) {
    }
}
