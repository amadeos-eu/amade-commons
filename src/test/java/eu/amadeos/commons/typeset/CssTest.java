package eu.amadeos.commons.typeset;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CssTest {
    private static Style style(Css css, Element e) {
        Style s = Style.ROOT;
        List<Element> chain = new java.util.ArrayList<>(e.parents());
        java.util.Collections.reverse(chain);
        for (Element p : chain) s = css.style(p, s);
        return css.style(e, s);
    }

    @Test
    void moreSpecificSelectorsWin() {
        Css css = new Css().add("p { text-indent: 1.5em } p.first { text-indent: 0 } .note p { text-align: center }");
        Document doc = Jsoup.parse("<div class='note'><p class='first' id='a'>x</p><p id='b'>y</p></div><p id='c'>z</p>");
        assertEquals(0, style(css, doc.getElementById("a")).indent());
        assertEquals(1.5, style(css, doc.getElementById("b")).indent());
        assertEquals("center", style(css, doc.getElementById("b")).align());
        assertEquals("left", style(css, doc.getElementById("c")).align());
    }

    @Test
    void childCombinatorAndInlineStyle() {
        Css css = new Css().add("div > p { font-style: italic }");
        Document doc = Jsoup.parse("<div><p id='a'>x</p><blockquote><p id='b' style='font-weight: bold'>y</p></blockquote></div>");
        assertTrue(style(css, doc.getElementById("a")).italic());
        assertFalse(style(css, doc.getElementById("b")).italic());
        assertTrue(style(css, doc.getElementById("b")).bold());
    }

    @Test
    void unitsBecomeEmOfTheReadersSize() {
        Css css = new Css().add("h1 { font-size: 150%; margin: 24px 0 12pt } .small { font-size: 0.5em }");
        Document doc = Jsoup.parse("<h1 id='h'>T<span class='small' id='s'>x</span></h1>");
        Style h = style(css, doc.getElementById("h"));
        assertEquals(1.5, h.fontSize(), 1e-9);
        assertEquals(1.5, h.marginTop(), 1e-9);
        assertEquals(1.0, h.marginBottom(), 1e-9);
        assertEquals(0.75, style(css, doc.getElementById("s")).fontSize(), 1e-9);
    }

    @Test
    void leavesOutWhatTheLayoutDoesNotUnderstand() {
        Css css = new Css().add("""
                /* comment p { font-style: italic } */
                @media print { p { font-weight: bold } }
                @media screen { p { text-align: right } }
                p:first-child { font-style: italic }
                @import url(x.css);
                """);
        Style p = style(css, Jsoup.parse("<p id='p'>x</p>").getElementById("p"));
        assertFalse(p.italic());
        assertFalse(p.bold());
        assertEquals("right", p.align());
    }

    @Test
    void collectsFontFaces() {
        Css css = new Css().add("@font-face { font-family: 'Book Serif'; font-weight: bold; src: url(../fonts/b.ttf) }",
                url -> "OEBPS/" + url.replace("../", ""));
        assertEquals(List.of(new Css.FontFace("Book Serif", "OEBPS/fonts/b.ttf", true, false)), css.fontFaces());
    }

    @Test
    void floatsWidthsAndPageBreaks() {
        Css css = new Css().add(".pic { float: left; width: 30% } h2 { page-break-before: always }");
        Document doc = Jsoup.parse("<img class='pic' id='i' src='a.png'><h2 id='h'>x</h2>");
        Style img = style(css, doc.getElementById("i"));
        assertEquals("left", img.floating());
        assertEquals(-0.3, img.width(), 1e-9);
        assertTrue(style(css, doc.getElementById("h")).breakBefore());
    }
}
