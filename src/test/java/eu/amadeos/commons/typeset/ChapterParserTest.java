package eu.amadeos.commons.typeset;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChapterParserTest {
    private static ChapterText parse(String body) {
        return ChapterParser.parse(TestText.xhtml("t", body), "OEBPS/text/ch1.xhtml", new Css().add(TestText.CSS), "en");
    }

    @Test
    void paragraphsWithCollapsedWhitespaceAndRuns() {
        ChapterText t = parse("<p class='first'>  Alice   was <em>very</em>\n tired. </p><p>Down</p>");
        Block.Text first = (Block.Text) t.blocks().get(0);
        assertEquals("Alice was very tired.", first.text());
        assertEquals(0, first.indent());
        assertEquals("justify", first.align());
        StyledRun italic = first.runs().stream().filter(r -> r.style().italic()).findFirst().orElseThrow();
        assertEquals("very", first.text().substring(italic.start(), italic.end()));
        assertEquals(1.5, ((Block.Text) t.blocks().get(1)).indent());
        assertEquals("Alice was very tired.\nDown\n", t.plain());
        assertEquals(22, t.blocks().get(1).offset());
    }

    @Test
    void anchorsLinksAndFootnotes() {
        ChapterText t = parse("""
                <p id='p1'>See <a epub:type='noteref' href='#n1'>1</a> and <a href='ch2.xhtml#x'>more</a>
                and <a href='https://example.org'>web</a>.</p>
                <aside epub:type='footnote' id='n1'><p>The note.</p></aside>
                <h2 id='h'>Heading</h2>""");
        assertEquals(0, t.anchor("p1").orElseThrow());
        assertEquals("The note.", t.footnotes().get("n1"));
        assertFalse(t.plain().contains("The note"));
        Block.Text p = (Block.Text) t.blocks().getFirst();
        List<String> links = p.runs().stream().map(r -> r.style().link()).filter(java.util.Objects::nonNull).toList();
        assertEquals(List.of("OEBPS/text/ch1.xhtml#n1", "OEBPS/text/ch2.xhtml#x", "https://example.org"), links);
        assertTrue(p.runs().stream().anyMatch(r -> r.style().noteRef()));
        assertTrue(((Block.Text) t.blocks().get(1)).keepWithNext());
        assertEquals(t.blocks().get(1).offset(), t.anchor("h").orElseThrow());
        assertTrue(t.linkAt(4).isPresent());
    }

    @Test
    void picturesBecomeBlocksAndKeepTheirFloat() {
        ChapterText t = parse("<p>Before</p><div class='right'><img src='../images/a.png' alt='A'/></div>"
                + "<figure><img src='../images/b.png'/><figcaption>Caption</figcaption></figure>");
        Block.Image a = assertInstanceOf(Block.Image.class, t.blocks().get(1));
        assertEquals("OEBPS/images/a.png", a.path());
        assertEquals("right", a.floating());
        Block.Image b = assertInstanceOf(Block.Image.class, t.blocks().get(2));
        assertFalse(b.floats());
        assertEquals("center", ((Block.Text) t.blocks().get(3)).align());
        assertEquals("Before\n￼\n￼\nCaption\n", t.plain());
    }

    @Test
    void listsRulesBreaksAndTables() {
        ChapterText t = parse("<ol><li>One</li><li>Two</li></ol><ul><li>Dot</li></ul><hr/>"
                + "<p>Line<br/>break</p><table><tr><td>a</td><td>b</td></tr></table>");
        assertEquals("1. One", ((Block.Text) t.blocks().get(0)).text());
        assertEquals("2. Two", ((Block.Text) t.blocks().get(1)).text());
        assertEquals("• Dot", ((Block.Text) t.blocks().get(2)).text());
        assertInstanceOf(Block.Rule.class, t.blocks().get(3));
        assertEquals("Line\nbreak", ((Block.Text) t.blocks().get(4)).text());
        assertEquals("a  b", ((Block.Text) t.blocks().get(5)).text());
    }

    @Test
    void emptyParagraphsLeaveSpaceAndLanguagesAreKept() {
        ChapterText t = parse("<p>A</p><p> </p><p xml:lang='de'>B</p>");
        Block.Text b = (Block.Text) t.blocks().get(1);
        assertTrue(b.spaceBefore() >= 1.2);
        assertEquals("de", b.language());
        assertEquals("en", ((Block.Text) t.blocks().get(0)).language());
    }
}
