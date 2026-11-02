package eu.amadeos.commons.typeset;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sets a chapter into pages: breaks paragraphs into lines with {@link LineBreakMeasurer}, justifies them,
 * hyphenates the word that does not fit, keeps headings with the next lines, avoids single first and last lines
 * of a paragraph at a page edge, places pictures and lets text flow around floating ones. A picture that no
 * longer fits on the page moves to the top of the next one while the text goes on before it.
 * Works on any thread; nothing here touches Swing.
 */
public final class Typesetter {
    /** Gap between a floating picture and the text beside it, in em. */
    static final double FLOAT_GAP = 0.8;
    /** The share of the text width a floating picture may take at most. */
    static final double FLOAT_MAX = 0.5;
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    /** The size of the pictures of the book in pixels, empty when one cannot be read. */
    @FunctionalInterface
    public interface PictureSizes {
        Optional<Dimension> size(String path);
    }

    private final Appearance appearance;
    private final Hyphenators hyphenators;
    private final PictureSizes pictures;
    private final Map<String, Font> bookFonts;

    /**
     * @param bookFonts the fonts the book brings by the family names its style sheets use; used with the
     *                  publisher's font only
     */
    public Typesetter(Appearance appearance, Hyphenators hyphenators, PictureSizes pictures, Map<String, Font> bookFonts) {
        this.appearance = appearance;
        this.hyphenators = hyphenators;
        this.pictures = pictures;
        this.bookFonts = Map.copyOf(bookFonts);
    }

    public Appearance appearance() {
        return appearance;
    }

    /** Sets the chapter into pages whose text area is {@code width} by {@code height} pixels. */
    public ChapterLayout layout(ChapterText chapter, float width, float height) {
        return new Run(width, height).set(chapter);
    }

    /** The state of one layout run: the page being filled and where on it. */
    private final class Run {
        private final float width;
        private final float height;
        private final float em = appearance.size();
        private final List<Page> pages = new ArrayList<>();
        private List<Page.Line> lines = new ArrayList<>();
        private List<Page.Picture> placed = new ArrayList<>();
        private List<Page.Rule> rules = new ArrayList<>();
        private final List<Float> floats = new ArrayList<>();
        private final List<Figure> deferred = new ArrayList<>();
        /** Pictures and captions moved to a later page, {from, to}: they do not count for where that page starts. */
        private final List<int[]> moved = new ArrayList<>();
        private float y;
        private boolean empty = true;
        private int lastEnd;

        Run(float width, float height) {
            this.width = Math.max(40, width);
            this.height = Math.max(40, height);
        }

        /** A picture the text flows around on the current page. */
        record Float(boolean left, float x, float top, float bottom, float width) {
        }

        /** A picture with the captions that follow it. */
        record Figure(Block.Image image, List<Block.Text> captions) {
        }

        ChapterLayout set(ChapterText chapter) {
            List<Block> blocks = chapter.blocks();
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                if (block.breakBefore() && !empty) newPage();
                switch (block) {
                    case Block.Text t -> text(t);
                    case Block.Image image -> {
                        List<Block.Text> captions = new ArrayList<>();
                        while (i + 1 < blocks.size() && blocks.get(i + 1) instanceof Block.Text t && t.caption()) {
                            captions.add(t);
                            i++;
                        }
                        picture(new Figure(image, captions), false);
                    }
                    case Block.Rule r -> rule(r);
                }
            }
            while (!deferred.isEmpty()) newPage();
            finishPage();
            if (pages.isEmpty()) pages.add(new Page(List.of(), List.of(), List.of(), 0, Math.max(1, chapter.length())));
            return new ChapterLayout(pages);
        }

        // ------------------------------------------------------------ pages

        private void newPage() {
            finishPage();
            lines = new ArrayList<>();
            placed = new ArrayList<>();
            rules = new ArrayList<>();
            floats.clear();
            y = 0;
            empty = true;
            List<Figure> waiting = new ArrayList<>(deferred);
            deferred.clear();
            for (Figure figure : waiting) picture(figure, true);
        }

        // --8<-- [start:finish]
        private void finishPage() {
            if (empty && lines.isEmpty() && placed.isEmpty() && rules.isEmpty()) return;
            int start = Integer.MAX_VALUE;
            int end = 0;
            for (Page.Line l : lines) {
                if (moved(l.start())) continue;
                start = Math.min(start, l.start());
                end = Math.max(end, l.end());
            }
            for (Page.Picture p : placed) {
                if (moved(p.offset())) continue;
                start = Math.min(start, p.offset());
                end = Math.max(end, p.offset() + 1);
            }
            for (Page.Rule r : rules) {
                start = Math.min(start, r.offset());
                end = Math.max(end, r.offset() + 1);
            }
            if (start == Integer.MAX_VALUE) start = lastEnd;
            lastEnd = Math.max(lastEnd, end);
            pages.add(new Page(lines, placed, rules, start, Math.max(end, start)));
            lines = new ArrayList<>();
            placed = new ArrayList<>();
            rules = new ArrayList<>();
            empty = true;
        }
        // --8<-- [end:finish]

        private void space(double before) {
            if (!empty) y += (float) (before * em);
        }

        // ------------------------------------------------------------ pictures and rules

        // --8<-- [start:picture]
        private void picture(Figure figure, boolean pageTop) {
            Block.Image image = figure.image();
            Optional<Dimension> natural = pictures.size(image.path());
            if (natural.isEmpty()) return;
            float left = (float) (image.left() * em);
            float available = width - left - (float) (image.right() * em);
            boolean floating = image.floats();
            float w;
            if (image.width() < 0) w = (float) (-image.width() * width);
            else if (image.width() > 0) w = (float) (image.width() * em);
            else w = natural.get().width;
            if (image.width() == 0 && image.height() > 0) {
                w = (float) (image.height() * em) * natural.get().width / Math.max(1, natural.get().height);
            }
            w = Math.min(w, floating ? (float) (available * FLOAT_MAX) : available);
            float h = w * natural.get().height / Math.max(1, natural.get().width);
            float maxHeight = height * (floating ? 0.6f : 1f);
            if (h > maxHeight) {
                w = w * maxHeight / h;
                h = maxHeight;
            }
            float captionWidth = floating ? Math.max(w, 6 * em) : available;
            List<List<TextLayout>> captions = new ArrayList<>();
            float captionHeight = 0;
            for (Block.Text caption : figure.captions()) {
                List<TextLayout> captionLines = captionLines(caption, captionWidth);
                captions.add(captionLines);
                captionHeight += (float) (caption.spaceBefore() * em);
                for (TextLayout l : captionLines) captionHeight += lineHeight(l);
            }
            float top = y + (empty ? 0 : (float) (image.spaceBefore() * em));
            if (!floating) top = Math.max(top, floatBottom());
            else top = Math.max(top, floatBottom(image.floating().equals("left")));
            if (top + h + captionHeight > height && !empty && !pageTop) {
                // Does not fit any more: the text goes on here, the picture and its caption open the next page.
                deferred.add(figure);
                Block last = figure.captions().isEmpty() ? image : figure.captions().getLast();
                moved.add(new int[]{image.offset(), last.offset() + last.length()});
                return;
            }
            float x = floating
                    ? (image.floating().equals("left") ? left : left + available - w)
                    : left + (available - w) / 2;
            placed.add(new Page.Picture(image.path(), image.alt(), x, top, w, h, image.offset()));
            // the captions centred below the picture
            float boxX = floating ? (image.floating().equals("left") ? left : left + available - captionWidth) : left;
            float cy = top + h;
            for (int c = 0; c < captions.size(); c++) {
                Block.Text caption = figure.captions().get(c);
                cy += (float) (caption.spaceBefore() * em);
                int start = caption.offset();
                for (TextLayout l : captions.get(c)) {
                    float lh = lineHeight(l);
                    int length = l.getCharacterCount();
                    lines.add(new Page.Line(l, boxX + (captionWidth - l.getVisibleAdvance()) / 2,
                            cy + l.getAscent() + (lh - l.getAscent() - l.getDescent()) / 2, start, start + length, false));
                    start += length;
                    cy += lh;
                }
            }
            if (floating) {
                floats.add(new Float(image.floating().equals("left"), Math.min(x, boxX), top, cy, Math.max(w, captionWidth)));
            } else {
                y = cy + (float) (0.5 * em);
            }
            empty = false;
        }
        // --8<-- [end:picture]

        private boolean moved(int offset) {
            for (int[] range : moved) {
                if (offset >= range[0] && offset <= range[1]) return true;
            }
            return false;
        }

        private float lineHeight(TextLayout l) {
            return (l.getAscent() + l.getDescent()) * (float) appearance.lineSpacing();
        }

        /** A caption broken into centred lines of at most the width, without hyphenation. */
        private List<TextLayout> captionLines(Block.Text caption, float lineWidth) {
            List<TextLayout> result = new ArrayList<>();
            if (caption.length() == 0) return result;
            LineBreakMeasurer measurer = new LineBreakMeasurer(attributed(caption).getIterator(), FRC);
            while (measurer.getPosition() < caption.length()) {
                result.add(measurer.nextLayout(Math.max(lineWidth, em)));
            }
            return result;
        }

        private void rule(Block.Rule rule) {
            space(rule.spaceBefore());
            y = Math.max(y, floatBottom());
            if (y + em > height && !empty) newPage();
            float mid = y + em / 2;
            rules.add(new Page.Rule(width * 0.35f, width * 0.65f, mid, rule.offset()));
            y += em;
            empty = false;
        }

        private float floatBottom() {
            float bottom = 0;
            for (Float f : floats) bottom = Math.max(bottom, f.bottom());
            return bottom;
        }

        private float floatBottom(boolean left) {
            float bottom = 0;
            for (Float f : floats) {
                if (f.left() == left) bottom = Math.max(bottom, f.bottom());
            }
            return bottom;
        }

        /** Left and right edge of the text between {@code top} and {@code bottom}, around the floats. */
        private float[] edges(float top, float bottom, float left, float right) {
            float l = left;
            float r = width - right;
            float gap = (float) (FLOAT_GAP * em);
            for (Float f : floats) {
                if (f.bottom() <= top || f.top() >= bottom) continue;
                if (f.left()) l = Math.max(l, f.x() + f.width() + gap);
                else r = Math.min(r, f.x() - gap);
            }
            return new float[]{l, r};
        }

        // ------------------------------------------------------------ text

        // --8<-- [start:text]
        private void text(Block.Text block) {
            AttributedString attributed = attributed(block);
            AttributedCharacterIterator it = attributed.getIterator();
            int n = block.length();
            if (n == 0) return;
            LineBreakMeasurer measurer = new LineBreakMeasurer(it, FRC);
            Optional<Hyphenator> hyphenator = appearance.hyphenate() && !block.preformatted()
                    ? hyphenators.forLanguage(block.language()) : Optional.empty();
            String text = block.text();
            float left = (float) (block.left() * em);
            float right = (float) (block.right() * em);
            float lineHeight = (float) (em * block.fontSize() * 1.2 * appearance.lineSpacing());
            float spaceBefore = (float) (block.spaceBefore() * em);
            if (block.keepWithNext() && !empty && y + spaceBefore + lineHeight + 2.2f * em * appearance.lineSpacing() > height) {
                newPage();
            }
            space(block.spaceBefore());
            int pos = 0;
            int lineNumber = 0;
            int linesOnPage = 0;
            while (pos < n) {
                int limit = text.indexOf('\n', pos);
                if (limit < 0) limit = n;
                if (limit == pos) {
                    // an empty line from <br/><br/>
                    y += lineHeight;
                    pos++;
                    continue;
                }
                float indent = lineNumber == 0 ? (float) (block.indent() * em) : 0;
                Placed line = null;
                while (line == null) {
                    float[] e = edges(y, y + lineHeight, left, right);
                    float available = Math.max(em * 2, e[1] - e[0] - indent);
                    Placed measured = line(block, attributed, measurer, hyphenator, pos, limit, available);
                    float h = (measured.layout.getAscent() + measured.layout.getDescent()) * (float) appearance.lineSpacing();
                    boolean last = measured.end >= n;
                    boolean orphan = lineNumber == 0 && !last && y + 2 * h > height;
                    if (!empty && (y + h > height || orphan)) {
                        boolean widow = last && linesOnPage >= 3;
                        newPage();
                        linesOnPage = 0;
                        if (widow) moveLastLineFromPreviousPage();
                        continue;
                    }
                    line = measured;
                    float x = e[0] + indent;
                    TextLayout layout = line.layout;
                    if (!last && line.end < limit && justified(block)) {
                        layout = justify(attributed, pos, line.end, line.hyphen ? "-" : null, available);
                    }
                    float visible = layout.getVisibleAdvance();
                    if (block.align().equals("center")) x += (available - visible) / 2;
                    else if (block.align().equals("right")) x += available - visible;
                    float baseline = y + layout.getAscent() + (h - layout.getAscent() - layout.getDescent()) / 2;
                    lines.add(new Page.Line(layout, x, baseline, block.offset() + pos, block.offset() + line.end, line.hyphen));
                    y += h;
                    empty = false;
                    linesOnPage++;
                }
                pos = line.end;
                if (pos < n && pos == limit) pos++;
                lineNumber++;
            }
        }
        // --8<-- [end:text]

        /** A single last line at the top of a page takes the line before it along. */
        private void moveLastLineFromPreviousPage() {
            Page previous = pages.getLast();
            if (previous.lines().size() < 2) return;
            Page.Line moved = previous.lines().getLast();
            List<Page.Line> remaining = new ArrayList<>(previous.lines().subList(0, previous.lines().size() - 1));
            int end = remaining.stream().mapToInt(Page.Line::end).max().orElse(previous.start());
            for (Page.Picture p : previous.pictures()) end = Math.max(end, p.offset() + 1);
            pages.set(pages.size() - 1, new Page(remaining, previous.pictures(), previous.rules(), previous.start(), end));
            float h = moved.bottom() - moved.top();
            float shift = moved.top();
            lines.add(new Page.Line(moved.layout(), moved.x(), moved.baseline() - shift + y, moved.start(), moved.end(),
                    moved.hyphen()));
            y += h * (float) appearance.lineSpacing();
            empty = false;
        }

        private boolean justified(Block.Text block) {
            return appearance.justify() && (block.align().equals("justify") || block.align().equals("left"))
                    && !block.preformatted();
        }
    }

    /** A measured line before it is placed. */
    private record Placed(TextLayout layout, int end, boolean hyphen) {
    }

    // --8<-- [start:line]
    /**
     * The next line from {@code pos}: as many words as fit, and when the next word does not fit, as much of it
     * as fits before a hyphenation point.
     */
    private static Placed line(Block.Text block, AttributedString attributed, LineBreakMeasurer measurer,
                               Optional<Hyphenator> hyphenator, int pos, int limit, float available) {
        measurer.setPosition(pos);
        int end = measurer.nextOffset(available, limit, false);
        if (end <= pos) end = Math.min(limit, pos + 1);
        String text = block.text();
        // Java may break after the apostrophe in "I’ve"; the word goes to the next line as a whole
        if (end < limit && end - 1 > pos && "’'".indexOf(text.charAt(end - 1)) >= 0 && Character.isLetter(text.charAt(end))) {
            int space = text.lastIndexOf(' ', end - 1);
            if (space >= pos) end = space + 1;
        }
        if (hyphenator.isPresent() && end < limit && Character.isLetter(text.charAt(end))) {
            int wordEnd = end;
            while (wordEnd < limit && Character.isLetter(text.charAt(wordEnd))) wordEnd++;
            int wordStart = end;
            // the measurer may have broken inside a word that is longer than the line
            while (wordStart > pos && Character.isLetter(text.charAt(wordStart - 1))) wordStart--;
            List<Integer> points = hyphenator.get().points(text.substring(wordStart, wordEnd));
            for (int i = points.size() - 1; i >= 0; i--) {
                int cut = wordStart + points.get(i);
                if (cut <= pos) continue;
                TextLayout candidate = new TextLayout(sub(attributed, pos, cut, "-").getIterator(), FRC);
                if (candidate.getVisibleAdvance() <= available) return new Placed(candidate, cut, true);
            }
        }
        return new Placed(new TextLayout(sub(attributed, pos, end, null).getIterator(), FRC), end, false);
    }
    // --8<-- [end:line]

    // --8<-- [start:justify]
    /**
     * The line set to the full width. Java's justification counts glyphs where it means characters, so after a
     * ligature such as "fl" it would widen the wrong gaps; justified lines are therefore set without ligatures.
     */
    private static TextLayout justify(AttributedString attributed, int from, int to, String suffix, float width) {
        AttributedString text = sub(attributed, from, to, suffix);
        text.addAttribute(TextAttribute.LIGATURES, 0);
        return new TextLayout(text.getIterator(), FRC).getJustifiedLayout(width);
    }
    // --8<-- [end:justify]

    /** Characters {@code from} to {@code to} with their attributes, and a suffix in the style of the last one. */
    static AttributedString sub(AttributedString whole, int from, int to, String suffix) {
        AttributedCharacterIterator it = whole.getIterator();
        StringBuilder sb = new StringBuilder(to - from + 1);
        for (int i = from; i < to; i++) sb.append(it.setIndex(i));
        if (suffix != null) sb.append(suffix);
        AttributedString result = new AttributedString(sb.toString());
        int i = from;
        while (i < to) {
            it.setIndex(i);
            int limit = Math.min(it.getRunLimit(), to);
            result.addAttributes(it.getAttributes(), i - from, limit - from);
            i = limit;
        }
        if (suffix != null && to > from) {
            it.setIndex(to - 1);
            result.addAttributes(it.getAttributes(), to - from, to - from + suffix.length());
        }
        return result;
    }

    // --8<-- [start:attributes]
    /**
     * The paragraph's text with fonts: the reader's family and size, the book's emphasis, sizes, small capitals
     * and superscripts. Small capitals become capitals at 80 %; the characters keep their places so offsets stay
     * the same.
     */
    AttributedString attributed(Block.Text block) {
        String text = block.text();
        char[] shown = text.toCharArray();
        for (StyledRun run : block.runs()) {
            if (run.style().uppercase() || run.style().smallCaps()) {
                for (int i = run.start(); i < run.end(); i++) {
                    char upper = Character.toUpperCase(shown[i]);
                    if (Character.toString(upper).length() == 1) shown[i] = upper;
                }
            }
        }
        AttributedString as = new AttributedString(new String(shown));
        as.addAttribute(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        as.addAttribute(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        float base = appearance.size();
        for (StyledRun run : block.runs()) {
            RunStyle s = run.style();
            float size = (float) (base * s.fontSize());
            Font bookFont = appearance.publisherFont() ? bookFont(s) : null;
            int i = run.start();
            while (i < run.end()) {
                boolean lower = s.smallCaps() && Character.isLowerCase(text.charAt(i));
                int j = i + 1;
                while (j < run.end() && (s.smallCaps() && Character.isLowerCase(text.charAt(j))) == lower) j++;
                float charSize = lower ? size * 0.8f : size;
                if (bookFont != null) {
                    as.addAttribute(TextAttribute.FONT, bookFont.deriveFont(style(s), charSize), i, j);
                } else {
                    as.addAttribute(TextAttribute.FAMILY, s.monospace() ? Font.MONOSPACED : family(), i, j);
                    as.addAttribute(TextAttribute.SIZE, charSize, i, j);
                    if (s.italic()) as.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, i, j);
                    if (s.bold()) as.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, i, j);
                }
                if (s.verticalAlign() != 0) {
                    as.addAttribute(TextAttribute.SUPERSCRIPT, s.verticalAlign() > 0
                            ? TextAttribute.SUPERSCRIPT_SUPER : TextAttribute.SUPERSCRIPT_SUB, i, j);
                }
                i = j;
            }
        }
        return as;
    }
    // --8<-- [end:attributes]

    private String family() {
        return appearance.publisherFont() ? Font.SERIF : appearance.family();
    }

    private Font bookFont(RunStyle s) {
        if (s.monospace()) return null;
        for (String family : s.families()) {
            Font font = bookFonts.get(family + (s.bold() ? "|b" : "") + (s.italic() ? "|i" : ""));
            if (font != null) return font;
            font = bookFonts.get(family);
            if (font != null) return font;
        }
        return null;
    }

    private static int style(RunStyle s) {
        return (s.bold() ? Font.BOLD : 0) | (s.italic() ? Font.ITALIC : 0);
    }
}
