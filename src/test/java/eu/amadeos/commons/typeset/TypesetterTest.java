package eu.amadeos.commons.typeset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypesetterTest {
    static final Appearance PLAIN = new Appearance("DejaVu Serif", 16, 1.4, 0.05, true, false);

    @TempDir
    Path dir;

    private static ChapterText chapter(String body) {
        return ChapterParser.parse(TestText.xhtml("t", body), "OEBPS/ch.xhtml", new Css().add(TestText.CSS), "en");
    }

    private static Typesetter setter(Appearance a, Map<String, Dimension> pictures) {
        return new Typesetter(a, new Hyphenators(Path.of("/nonexistent")), p -> Optional.ofNullable(pictures.get(p)), Map.of());
    }

    @Test
    void fillsPagesInOrderWithoutLosingText() {
        ChapterText text = chapter("<p class='first'>" + TestText.words(400) + "</p><p>" + TestText.words(300) + "</p>");
        ChapterLayout layout = setter(PLAIN, Map.of()).layout(text, 300, 300);
        assertTrue(layout.pageCount() > 3, "pages: " + layout.pageCount());
        int expected = 0;
        for (Page page : layout.pages()) {
            for (Page.Line line : page.lines()) {
                if (line.start() > expected) {
                    // the line break between the two paragraphs
                    assertEquals(text.plain().charAt(expected), '\n');
                    expected++;
                }
                assertEquals(expected, line.start());
                expected = line.end();
                while (expected < text.length() && text.plain().charAt(expected) == ' ') expected++;
                assertTrue(line.bottom() <= 300.5f, "line below the page");
            }
        }
        assertEquals(text.length() - 1, expected);
    }

    @Test
    void justifiesAllButTheLastLine() {
        ChapterText text = chapter("<p class='first'>" + TestText.words(80) + "</p>");
        Page page = setter(PLAIN, Map.of()).layout(text, 300, 2000).pages().getFirst();
        List<Page.Line> lines = page.lines();
        for (int i = 0; i < lines.size() - 1; i++) {
            assertEquals(300, lines.get(i).layout().getVisibleAdvance(), 1.5, "line " + i);
        }
        assertTrue(lines.getLast().layout().getVisibleAdvance() <= 300.5, "last " + lines.getLast().layout().getVisibleAdvance());
        ChapterLayout ragged = setter(PLAIN.withJustify(false), Map.of()).layout(text, 300, 2000);
        assertTrue(ragged.pages().getFirst().lines().stream().anyMatch(l -> l.layout().getVisibleAdvance() < 290),
                "a left aligned paragraph has shorter lines");
    }

    /** After "fl" the extra space of a justified line goes into the gaps between words, not into a word. */
    @Test
    void justifiedLinesWithLigaturesWidenOnlyTheGaps() {
        String words = "flashed across her mind that she had never before seen a rabbit with either a pocket";
        ChapterText text = chapter("<p class='first'>" + words + " " + words + "</p>");
        Page.Line line = setter(PLAIN, Map.of()).layout(text, 330, 2000).pages().getFirst().lines().getFirst();
        String shown = text.plain().substring(line.start(), line.end()).strip();
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(340, 30, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, 340, 30);
        g.setColor(java.awt.Color.BLACK);
        line.layout().draw(g, 0, 22);
        g.dispose();
        // blank stretches of at least 4 pixels between the first and the last ink are the gaps between words
        int gaps = 0;
        int blank = 0;
        boolean ink = false;
        for (int x = 0; x < 340; x++) {
            boolean empty = true;
            for (int y = 0; y < 30 && empty; y++) empty = (image.getRGB(x, y) & 0xff) > 0xc0;
            if (empty) {
                blank++;
            } else {
                if (ink && blank >= 4) gaps++;
                ink = true;
                blank = 0;
            }
        }
        assertEquals(shown.chars().filter(c -> c == ' ').count(), gaps, shown);
    }

    @Test
    void neverBreaksAfterAnApostropheInsideAWord() {
        ChapterText text = chapter("<p class='first'>I wonder what Latitude or Longitude I’ve got to, she said, and isn’t it odd.</p>");
        for (int width = 60; width <= 400; width += 3) {
            for (Page page : setter(PLAIN, Map.of()).layout(text, width, 2000).pages()) {
                for (Page.Line line : page.lines()) {
                    char before = line.start() > 0 ? text.plain().charAt(line.start() - 1) : ' ';
                    assertTrue(before != '’', "width " + width + ": a line starts after an apostrophe");
                }
            }
        }
    }

    @Test
    void indentsCentresAndKeepsHeadingsWithTheirText() {
        ChapterText text = chapter("<h1>Chapter</h1><p>" + TestText.words(30) + "</p>");
        Page page = setter(PLAIN, Map.of()).layout(text, 300, 2000).pages().getFirst();
        Page.Line heading = page.lines().getFirst();
        float centre = heading.x() + heading.layout().getVisibleAdvance() / 2;
        assertEquals(150, centre, 2);
        assertEquals(1.5f * 16, page.lines().get(1).x(), 0.5f);
        // A heading near the bottom of a page moves to the next page with its first lines.
        ChapterText late = chapter("<p class='first'>" + TestText.words(95) + "</p><h1>Next</h1><p>" + TestText.words(60) + "</p>");
        ChapterLayout layout = setter(PLAIN, Map.of()).layout(late, 300, 200);
        for (Page p : layout.pages()) {
            Page.Line last = p.lines().getLast();
            assertFalse(late.plain().substring(last.start(), last.end()).equals("Next"), "heading alone at the bottom");
        }
    }

    // --8<-- [start:hyphenation]
    @Test
    void hyphenatesTheWordThatDoesNotFit() throws Exception {
        Files.write(dir.resolve("hyph_en.dic"), ("UTF-8\n" + HyphenatorTest.PATTERNS).getBytes(StandardCharsets.UTF_8));
        Typesetter setter = new Typesetter(PLAIN.withHyphenate(true), new Hyphenators(dir), p -> Optional.empty(), Map.of());
        ChapterText text = chapter("<p class='first'>" + "hyphenation ".repeat(12) + "</p>");
        List<Page.Line> lines = setter.layout(text, 170, 2000).pages().getFirst().lines();
        assertTrue(lines.stream().anyMatch(Page.Line::hyphen), "no line ends with a hyphen");
        for (Page.Line line : lines) {
            if (line.hyphen()) {
                String piece = text.plain().substring(line.start(), line.end());
                assertTrue(piece.endsWith("hy") || piece.endsWith("hyphen"), piece);
            }
        }
    }
    // --8<-- [end:hyphenation]

    // --8<-- [start:floats]
    @Test
    void textFlowsAroundAFloatingPicture() {
        ChapterText text = chapter("<div class='right'><img src='a.png'/></div><p class='first'>" + TestText.words(120) + "</p>");
        Page page = setter(PLAIN, Map.of("OEBPS/a.png", new Dimension(200, 200))).layout(text, 400, 2000).pages().getFirst();
        Page.Picture picture = page.pictures().getFirst();
        assertEquals(160, picture.width(), 0.5);
        assertEquals(400 - 160, picture.x(), 0.5);
        Page.Line beside = page.lines().getFirst();
        Page.Line below = page.lines().getLast();
        assertTrue(beside.x() + beside.layout().getVisibleAdvance() < picture.x(), "text runs into the picture");
        assertTrue(below.top() > picture.y() + picture.height());
        assertTrue(below.layout().getVisibleAdvance() > 300 || below == page.lines().getLast());
    }
    // --8<-- [end:floats]

    @Test
    void aPictureThatDoesNotFitMovesToTheNextPageAndTheTextGoesOn() {
        ChapterText text = chapter("<p class='first'>" + TestText.words(60) + "</p><img src='big.png'/><p>"
                + TestText.words(40) + "</p>");
        ChapterLayout layout = setter(PLAIN, Map.of("OEBPS/big.png", new Dimension(300, 250))).layout(text, 300, 330);
        Page first = layout.pages().getFirst();
        assertTrue(first.pictures().isEmpty());
        int pictureOffset = text.blocks().get(1).offset();
        assertTrue(first.lines().stream().anyMatch(l -> l.start() > pictureOffset), "the text after the picture moved up");
        Page.Picture moved = layout.pages().get(1).pictures().getFirst();
        assertEquals(0, moved.y(), 0.01);
    }

    // --8<-- [start:captions]
    /** A caption stays below its picture, also when both move to the next page; the text goes on before them. */
    @Test
    void captionsTravelWithTheirPicture() {
        ChapterText text = chapter("<p class='first'>" + TestText.words(60) + "</p><figure><img src='big.png'/>"
                + "<figcaption>The White Rabbit</figcaption></figure><p>" + TestText.words(40) + "</p>");
        assertTrue(((Block.Text) text.blocks().get(2)).caption());
        assertFalse(((Block.Text) text.blocks().get(3)).caption());
        ChapterLayout layout = setter(PLAIN, Map.of("OEBPS/big.png", new Dimension(300, 200))).layout(text, 300, 330);
        int captionOffset = text.blocks().get(2).offset();
        Page first = layout.pages().getFirst();
        assertTrue(first.lines().stream().noneMatch(l -> l.start() == captionOffset), "the caption stayed behind");
        Page second = layout.pages().get(1);
        Page.Picture picture = second.pictures().getFirst();
        Page.Line caption = second.lines().stream().filter(l -> l.start() == captionOffset).findFirst().orElseThrow();
        assertTrue(caption.top() >= picture.y() + picture.height() - 0.5, "the caption is below the picture");
        // the text after the figure that stayed on the first page is still found there
        int after = text.blocks().get(3).offset();
        assertEquals(0, layout.pageOf(after));
    }

    @Test
    void aFloatingPictureTakesItsCaptionAlong() {
        ChapterText text = chapter("<div class='right'><img src='a.png'/><p>Late!</p></div><p class='first'>"
                + TestText.words(120) + "</p>");
        Page page = setter(PLAIN, Map.of("OEBPS/a.png", new Dimension(200, 200))).layout(text, 400, 2000).pages().getFirst();
        Page.Picture picture = page.pictures().getFirst();
        Page.Line caption = page.lines().getFirst();
        assertEquals("Late!", text.plain().substring(caption.start(), caption.end()));
        assertTrue(caption.x() >= picture.x() - 0.5, "the caption is in the picture's column");
        Page.Line beside = page.lines().get(1);
        assertTrue(beside.x() + beside.layout().getVisibleAdvance() < picture.x(), "text runs into the picture");
        assertTrue(beside.top() < caption.top(), "the text starts beside the picture");
    }
    // --8<-- [end:captions]

    @Test
    void picturesShrinkToThePage() {
        ChapterText text = chapter("<img src='tall.png'/>");
        Page.Picture p = setter(PLAIN, Map.of("OEBPS/tall.png", new Dimension(1000, 3000))).layout(text, 300, 400)
                .pages().getFirst().pictures().getFirst();
        assertEquals(400, p.height(), 0.5);
        assertEquals(400 / 3.0, p.width(), 0.5);
    }

    @Test
    void findsThePageOfAnOffsetAndTheTextUnderTheMouse() {
        ChapterText text = chapter("<p class='first'>" + TestText.words(300) + "</p>");
        ChapterLayout layout = setter(PLAIN, Map.of()).layout(text, 300, 300);
        Page second = layout.pages().get(1);
        assertEquals(1, layout.pageOf(second.start()));
        assertEquals(1, layout.pageOf(second.start() + 5));
        assertEquals(0, layout.pageOf(second.start() - 1));
        Page.Line line = second.lines().getFirst();
        assertEquals(line.start(), line.offsetAt(line.x() + 1));
        assertTrue(line.highlight(line.start(), line.start() + 3).getBounds().width > 5);
    }
}
