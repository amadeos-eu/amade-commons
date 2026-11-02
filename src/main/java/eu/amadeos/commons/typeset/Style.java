package eu.amadeos.commons.typeset;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The computed style of an element as far as the layout uses it. Lengths are in "em of the reader's size": the
 * size the reader chose is 1, so a heading of {@code 1.5em} has {@code fontSize} 1.5 and its {@code 1em} margin
 * is 1.5 as well. Pixel and point values count 16 px or 12 pt as one em.
 *
 * @param fontSize    font size relative to the reader's size
 * @param display     block, inline, none or list-item
 * @param align       left, right, center or justify
 * @param indent      first-line indent
 * @param marginTop   space above the block (margin plus padding)
 * @param marginLeft  left indent of the block, summed up over the enclosing blocks
 * @param breakBefore the element starts a new page
 * @param floating    left or right for a floating element, else none
 * @param width       an explicit width: a fraction of the page width when below 0, else em; 0 when not given
 * @param families    the font families the book names, the first one first
 * @param verticalAlign 1 for superscript, -1 for subscript, 0 on the baseline
 */
public record Style(double fontSize, String display, String align, double indent, double marginTop,
                    double marginBottom, double marginLeft, double marginRight, boolean italic, boolean bold,
                    boolean smallCaps, boolean monospace, boolean preformatted, String transform,
                    boolean breakBefore, String floating, double width, double height, List<String> families,
                    int verticalAlign) {

    /** The style of the root element: the reader's size, left aligned, nothing else. */
    public static final Style ROOT = new Style(1, "block", "left", 0, 0, 0, 0, 0, false, false, false, false,
            false, "none", false, "none", 0, 0, List.of(), 0);

    static final List<String> BLOCKS = List.of("html", "body", "p", "div", "h1", "h2", "h3", "h4", "h5", "h6",
            "blockquote", "section", "article", "aside", "header", "footer", "nav", "main", "figure", "figcaption",
            "ul", "ol", "li", "dl", "dt", "dd", "pre", "hr", "table", "thead", "tbody", "tfoot", "tr", "center",
            "address", "hgroup", "details", "summary", "caption");

    public boolean block() {
        return display.equals("block") || display.equals("list-item") || display.equals("table")
                || display.equals("table-row");
    }

    public boolean hidden() {
        return display.equals("none");
    }

    /** The style of a child element with these declared values. */
    Style child(Map<String, String> d, String tag) {
        double size = fontSize;
        if (d.containsKey("font-size")) size = fontSize(d.get("font-size"), fontSize);
        String disp = d.getOrDefault("display", BLOCKS.contains(tag) ? (tag.equals("li") ? "list-item" : "block") : "inline");
        disp = disp.toLowerCase(Locale.ROOT);
        if (disp.startsWith("inline-")) disp = "inline";
        if (disp.equals("table-cell") || disp.equals("flex") || disp.equals("grid")) disp = "block";
        String a = d.containsKey("text-align") ? align(d.get("text-align")) : align;
        double ind = d.containsKey("text-indent") ? indent(d.get("text-indent"), size) : indent;
        boolean blockLevel = !disp.equals("inline");
        double top = blockLevel ? length(d.get("margin-top"), size, 0) + length(d.get("padding-top"), size, 0) : 0;
        double bottom = blockLevel ? length(d.get("margin-bottom"), size, 0) + length(d.get("padding-bottom"), size, 0) : 0;
        double left = marginLeft + (blockLevel ? Math.max(0, length(d.get("margin-left"), size, 0))
                + Math.max(0, length(d.get("padding-left"), size, 0)) : 0);
        double right = marginRight + (blockLevel ? Math.max(0, length(d.get("margin-right"), size, 0))
                + Math.max(0, length(d.get("padding-right"), size, 0)) : 0);
        boolean it = d.containsKey("font-style") ? d.get("font-style").matches("(?i)italic|oblique") : italic;
        boolean b = bold;
        if (d.containsKey("font-weight")) {
            String w = d.get("font-weight").toLowerCase(Locale.ROOT);
            b = w.equals("bold") || w.equals("bolder") || Css.number(w) >= 600;
        }
        boolean caps = d.containsKey("font-variant") ? d.get("font-variant").contains("small-caps") : smallCaps;
        List<String> fams = families;
        boolean mono = monospace;
        if (d.containsKey("font-family")) {
            fams = java.util.Arrays.stream(d.get("font-family").split(",")).map(Css::unquote).filter(s -> !s.isEmpty()).toList();
            mono = fams.stream().anyMatch(f -> f.equalsIgnoreCase("monospace") || f.toLowerCase(Locale.ROOT).contains("courier"));
        }
        boolean pre = d.containsKey("white-space") ? d.get("white-space").startsWith("pre") : preformatted;
        String tr = d.containsKey("text-transform") ? d.get("text-transform").toLowerCase(Locale.ROOT) : transform;
        String brk = d.getOrDefault("page-break-before", "auto").toLowerCase(Locale.ROOT);
        String fl = d.getOrDefault("float", "none").toLowerCase(Locale.ROOT);
        double w = d.containsKey("width") ? length(d.get("width"), size, -1) : 0;
        double h = d.containsKey("height") ? length(d.get("height"), size, 0) : 0;
        int va = verticalAlign;
        if (d.containsKey("vertical-align")) {
            String v = d.get("vertical-align");
            va = v.equals("super") ? 1 : v.equals("sub") ? -1 : 0;
        }
        return new Style(size, disp, a, ind, top, bottom, left, right, it, b, caps, mono, pre, tr,
                brk.matches("always|page|left|right|recto|verso"), fl.equals("left") || fl.equals("right") ? fl : "none",
                w, h, fams, va);
    }

    static String align(String value) {
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "center" -> "center";
            case "right", "end" -> "right";
            case "justify" -> "justify";
            default -> "left";
        };
    }

    /** A first-line indent; a percentage counts of a line of about 30 em. */
    static double indent(String value, double em) {
        String v = value.strip();
        if (v.endsWith("%")) {
            try {
                return Double.parseDouble(v.substring(0, v.length() - 1)) / 100 * 30;
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return length(v, em, 0);
    }

    /** A font size relative to the reader's size. */
    static double fontSize(String value, double parent) {
        String v = value.strip().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "xx-small" -> 0.6;
            case "x-small" -> 0.75;
            case "small" -> 0.89;
            case "medium" -> 1;
            case "large" -> 1.2;
            case "x-large" -> 1.5;
            case "xx-large" -> 2;
            case "smaller" -> parent * 0.83;
            case "larger" -> parent * 1.2;
            default -> {
                double l = length(v, parent, Double.NaN);
                yield Double.isNaN(l) || l <= 0 ? parent : Math.min(6, l);
            }
        };
    }

    /**
     * A length in em of the reader's size.
     *
     * @param em       the element's font size, for {@code em} and percentages of font sizes
     * @param fallback when the value is missing or not understood; -1 for widths makes percentages a negative
     *                 fraction of the page width, as {@link #width} expects
     */
    static double length(String value, double em, double fallback) {
        if (value == null) return fallback == -1 ? 0 : fallback;
        String v = value.strip().toLowerCase(Locale.ROOT);
        if (v.equals("0") || v.equals("auto") || v.equals("none")) return 0;
        try {
            if (v.endsWith("rem")) return Double.parseDouble(v.substring(0, v.length() - 3));
            if (v.endsWith("em")) return Double.parseDouble(v.substring(0, v.length() - 2)) * em;
            if (v.endsWith("ex")) return Double.parseDouble(v.substring(0, v.length() - 2)) * em / 2;
            if (v.endsWith("px")) return Double.parseDouble(v.substring(0, v.length() - 2)) / 16;
            if (v.endsWith("pt")) return Double.parseDouble(v.substring(0, v.length() - 2)) / 12;
            if (v.endsWith("%")) {
                double percent = Double.parseDouble(v.substring(0, v.length() - 1)) / 100;
                // widths and indents: a fraction of the page; font sizes: of the parent's size
                return fallback == -1 ? -percent : percent * em;
            }
            return Double.parseDouble(v) / 16;
        } catch (NumberFormatException e) {
            return fallback == -1 ? 0 : fallback;
        }
    }
}
