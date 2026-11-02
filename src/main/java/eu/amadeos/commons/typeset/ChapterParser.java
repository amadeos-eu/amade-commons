package eu.amadeos.commons.typeset;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the XHTML of a chapter into {@link Block}s: block elements become paragraphs, inline elements styled
 * runs, pictures picture blocks. Whitespace collapses as in a browser, {@code pre} keeps it. Lists get their
 * bullets or numbers, table rows become one paragraph each with the cells side by side. Footnotes in
 * {@code aside epub:type="footnote"} leave the text and wait for their link to be clicked.
 */
public final class ChapterParser {
    /** Stands for a picture in the plain text. */
    public static final char OBJECT = '￼';

    private final String path;
    private final Css css;
    private final String bookLanguage;
    private final List<Block> blocks = new ArrayList<>();
    private final StringBuilder plain = new StringBuilder();
    private final Map<String, Integer> anchors = new HashMap<>();
    private final Map<String, String> footnotes = new HashMap<>();
    private final Deque<Style> blockStyles = new ArrayDeque<>();
    private final Deque<int[]> listCounters = new ArrayDeque<>();
    private final Deque<String> languages = new ArrayDeque<>();
    /** For each {@code figure} or floating box around the current element: whether it had a picture yet. */
    private final Deque<boolean[]> figures = new ArrayDeque<>();
    private final StringBuilder text = new StringBuilder();
    private final List<StyledRun> runs = new ArrayList<>();
    private boolean lastSpace = true;
    private double pendingSpace;
    private boolean pendingBreak;
    private boolean keepWithNext;
    private boolean firstInRow;

    private ChapterParser(String path, Css css, String bookLanguage) {
        this.path = path;
        this.css = css;
        this.bookLanguage = bookLanguage;
    }

    /**
     * @param path         the chapter's path inside the book, to resolve links and pictures
     * @param css          the default style, the book's style sheets and those the chapter links to
     * @param bookLanguage the book's language, used when the chapter does not name one
     */
    public static ChapterText parse(String xhtml, String path, Css css, String bookLanguage) {
        return parse(Jsoup.parse(xhtml), path, css, bookLanguage);
    }

    /** The same for a chapter already parsed, e.g. to read the style sheets it links to first. */
    public static ChapterText parse(Document doc, String path, Css css, String bookLanguage) {
        ChapterParser parser = new ChapterParser(path, css, bookLanguage == null ? "" : bookLanguage);
        String lang = doc.selectFirst("html") == null ? "" : doc.selectFirst("html").attr("xml:lang");
        if (lang.isEmpty() && doc.selectFirst("html") != null) lang = doc.selectFirst("html").attr("lang");
        parser.languages.push(lang.isEmpty() ? parser.bookLanguage : lang);
        Style root = css.style(doc.body(), Style.ROOT);
        parser.blockStyles.push(root);
        parser.walk(doc.body(), root, null, false);
        parser.flush();
        return new ChapterText(path, parser.blocks, parser.plain.toString(), parser.anchors, parser.footnotes);
    }

    // --8<-- [start:walk]
    private void walk(Element parent, Style parentStyle, String link, boolean noteRef) {
        for (Node node : parent.childNodes()) {
            if (node instanceof TextNode t) {
                append(t.getWholeText(), parentStyle, link, noteRef);
            } else if (node instanceof Element e) {
                element(e, parentStyle, link, noteRef);
            }
        }
    }

    private void element(Element e, Style parentStyle, String link, boolean noteRef) {
        String tag = Css.localName(e).toLowerCase(Locale.ROOT);
        if (isFootnote(e)) {
            if (!e.id().isEmpty()) footnotes.put(e.id(), e.text());
            return;
        }
        Style style = css.style(e, parentStyle);
        if (style.hidden()) return;
        if (!e.id().isEmpty()) anchors.putIfAbsent(e.id(), plain.length() + text.length());
        if (tag.equals("a") && !e.attr("name").isEmpty()) anchors.putIfAbsent(e.attr("name"), plain.length() + text.length());
        String lang = e.hasAttr("xml:lang") ? e.attr("xml:lang") : e.attr("lang");
        if (!lang.isEmpty()) languages.push(lang);
        try {
            switch (tag) {
                case "br" -> {
                    text.append('\n');
                    addRun(1, style, link, noteRef);
                    lastSpace = true;
                }
                case "img" -> image(e.hasAttr("src") ? e.attr("src") : e.attr("xlink:href"), e.attr("alt"), box(e, style));
                case "image" -> image(e.hasAttr("xlink:href") ? e.attr("xlink:href") : e.attr("href"), "", box(e, style));
                case "hr" -> {
                    flush();
                    pendingSpace = Math.max(pendingSpace, style.marginTop());
                    blocks.add(new Block.Rule(plain.length(), pendingSpace, takeBreak(style)));
                    plain.append(OBJECT).append('\n');
                    pendingSpace = style.marginBottom();
                }
                case "svg" -> walk(e, style, link, noteRef);
                default -> {
                    String childLink = link;
                    boolean childNote = noteRef;
                    if (tag.equals("a") && !e.attr("href").isEmpty()) {
                        String href = e.attr("href");
                        childLink = ZipPaths.external(href) ? href
                                : ZipPaths.resolve(path, href) + (ZipPaths.fragment(href).isEmpty() ? "" : "#" + ZipPaths.fragment(href));
                        childNote = e.attr("epub:type").contains("noteref") || e.attr("role").equals("doc-noteref");
                    }
                    if (tag.equals("td") || tag.equals("th")) cell(e, style, childLink, childNote);
                    else if (style.block()) block(e, tag, style, childLink, childNote);
                    else walk(e, style, childLink, childNote);
                }
            }
        } finally {
            if (!lang.isEmpty()) languages.pop();
        }
    }
    // --8<-- [end:walk]

    private static boolean isFootnote(Element e) {
        String type = e.attr("epub:type");
        String role = e.attr("role");
        return Css.localName(e).equals("aside")
                && (type.matches(".*\\b(footnote|endnote|rearnote|note)\\b.*") || role.matches("doc-(foot|end)note"));
    }

    private void block(Element e, String tag, Style style, String link, boolean noteRef) {
        flush();
        pendingSpace = Math.max(pendingSpace, style.marginTop());
        if (style.breakBefore()) pendingBreak = true;
        blockStyles.push(style);
        boolean heading = tag.matches("h[1-6]");
        boolean figure = tag.equals("figure") || !style.floating().equals("none");
        if (figure) figures.push(new boolean[]{false});
        try {
            if (tag.equals("ol") || tag.equals("ul")) {
                int start = 1;
                try {
                    if (!e.attr("start").isEmpty()) start = Integer.parseInt(e.attr("start").strip());
                } catch (NumberFormatException ignored) {
                    // a broken start counts from 1
                }
                listCounters.push(new int[]{tag.equals("ol") ? start : 0});
            }
            if (style.display().equals("list-item")) {
                int[] counter = listCounters.peek();
                String type = e.parent() != null && Css.localName(e.parent()).equals("ol") ? "ol" : "ul";
                String marker = counter != null && type.equals("ol") ? (counter[0]++) + ". " : "• ";
                text.append(marker);
                addRun(marker.length(), style, null, false);
                lastSpace = true;
            }
            if (tag.equals("tr")) firstInRow = true;
            walk(e, style, link, noteRef);
            if (heading) keepWithNext = true;
            flush();
            keepWithNext = false;
        } finally {
            blockStyles.pop();
            if (tag.equals("ol") || tag.equals("ul")) listCounters.pop();
            if (figure) figures.pop();
        }
        pendingSpace = Math.max(pendingSpace, style.marginBottom());
    }

    /** A table cell: its text joins the row's paragraph, separated by a wide space. */
    private void cell(Element e, Style style, String link, boolean noteRef) {
        blockStyles.pop();
        try {
            if (!firstInRow) {
                text.append("  ");
                addRun(2, style, null, false);
            }
            firstInRow = false;
            lastSpace = true;
            walk(e, style, link, noteRef);
        } finally {
            blockStyles.push(style);
        }
    }

    /**
     * The style that decides how a picture floats and how wide it is: its own, or that of the element around it
     * ({@code <div class="right"><img/></div>}) when that one floats.
     */
    private Style box(Element e, Style style) {
        if (!style.floating().equals("none")) return style;
        Element p = e.parent();
        for (int i = 0; i < 2 && p != null; i++, p = p.parent()) {
            Style ps = css.style(p, Style.ROOT);
            if (!ps.floating().equals("none")) {
                return style.width() != 0 ? withFloat(style, ps.floating()) : ps;
            }
        }
        return style;
    }

    private static Style withFloat(Style s, String floating) {
        return new Style(s.fontSize(), s.display(), s.align(), s.indent(), s.marginTop(), s.marginBottom(),
                s.marginLeft(), s.marginRight(), s.italic(), s.bold(), s.smallCaps(), s.monospace(), s.preformatted(),
                s.transform(), s.breakBefore(), floating, s.width(), s.height(), s.families(), s.verticalAlign());
    }

    private void image(String src, String alt, Style style) {
        String floating = style.floating();
        if (src.isEmpty() || ZipPaths.external(src)) return;
        // Small pictures inside a line (a symbol, a decorated initial) are left out rather than breaking the line.
        if (style.height() > 0 && style.height() < 1.6 && hasText()) return;
        flush();
        pendingSpace = Math.max(pendingSpace, style.marginTop());
        Style block = blockStyles.peek();
        blocks.add(new Block.Image(plain.length(), ZipPaths.resolve(path, src), alt, floating, style.width(),
                style.height(), block.marginLeft(), block.marginRight(), pendingSpace, takeBreak(style)));
        plain.append(OBJECT).append('\n');
        pendingSpace = 0;
        if (!figures.isEmpty()) figures.peek()[0] = true;
    }

    private boolean takeBreak(Style style) {
        boolean b = pendingBreak || style.breakBefore();
        pendingBreak = false;
        return b;
    }

    // --8<-- [start:append]
    private void append(String raw, Style style, String link, boolean noteRef) {
        if (style.preformatted()) {
            String t = raw.replace("\r\n", "\n").replace('\t', ' ');
            text.append(t);
            addRun(t.length(), style, link, noteRef);
            lastSpace = t.endsWith("\n");
            return;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '\f') {
                if (!lastSpace) {
                    out.append(' ');
                    lastSpace = true;
                }
            } else if (c == '­') {
                // soft hyphens of the book: the hyphenator finds the places itself
            } else {
                out.append(c);
                lastSpace = false;
            }
        }
        if (out.isEmpty()) return;
        text.append(out);
        addRun(out.length(), style, link, noteRef);
    }
    // --8<-- [end:append]

    private void addRun(int length, Style style, String link, boolean noteRef) {
        RunStyle rs = RunStyle.of(style, link, noteRef);
        int end = text.length();
        if (!runs.isEmpty() && runs.getLast().style().equals(rs) && runs.getLast().end() == end - length) {
            StyledRun last = runs.removeLast();
            runs.add(new StyledRun(last.start(), end, rs));
        } else {
            runs.add(new StyledRun(end - length, end, rs));
        }
    }

    private boolean hasText() {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i)) && text.charAt(i) != ' ') return true;
        }
        return false;
    }

    /** Ends the paragraph being collected and adds it as a block, if it has any text. */
    private void flush() {
        Style style = blockStyles.peek();
        if (!hasText()) {
            // An empty paragraph (often <p>&nbsp;</p>) is the book's way to leave a blank line.
            if (text.length() > 0 && style != null) pendingSpace = Math.max(pendingSpace, 1.2 * style.fontSize());
            text.setLength(0);
            runs.clear();
            lastSpace = true;
            return;
        }
        int end = text.length();
        while (end > 0 && (text.charAt(end - 1) == ' ' || text.charAt(end - 1) == '\n')) end--;
        String content = text.substring(0, end);
        List<StyledRun> clipped = new ArrayList<>();
        for (StyledRun r : runs) {
            if (r.start() < end) clipped.add(new StyledRun(r.start(), Math.min(end, r.end()), r.style()));
        }
        blocks.add(new Block.Text(plain.length(), content, clipped, style.align(), style.indent(), style.marginLeft(),
                style.marginRight(), pendingSpace, takeBreak(style), keepWithNext, style.preformatted(),
                style.fontSize(), languages.isEmpty() ? bookLanguage : languages.peek(),
                !figures.isEmpty() && figures.peek()[0]));
        plain.append(content).append('\n');
        pendingSpace = 0;
        text.setLength(0);
        runs.clear();
        lastSpace = true;
    }
}
