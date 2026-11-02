package eu.amadeos.commons.typeset;

import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The part of CSS that books use for their text: rules with type, class and id selectors, descendant and child
 * combinators, ordered by specificity. Rules with pseudo-classes, attribute selectors or media queries other than
 * screen and all are left out; so are properties the reader does not set (colours, borders, positions). The
 * reader's own choices (font, size, spacing, colours) are applied on top, not here.
 */
public final class Css {
    /** What browsers give HTML without a style sheet, in the units this class understands. */
    static final String DEFAULTS = """
            head, script, style, title, meta, link { display: none }
            h1 { font-size: 2em; font-weight: bold; margin: 0.67em 0 }
            h2 { font-size: 1.5em; font-weight: bold; margin: 0.83em 0 }
            h3 { font-size: 1.17em; font-weight: bold; margin: 1em 0 }
            h4 { font-weight: bold; margin: 1.33em 0 }
            h5 { font-size: 0.83em; font-weight: bold; margin: 1.67em 0 }
            h6 { font-size: 0.67em; font-weight: bold; margin: 2.33em 0 }
            p { margin: 1em 0 }
            blockquote { margin: 1em 2.5em }
            figure { margin: 1em 2.5em }
            figcaption { text-align: center; font-size: 0.9em }
            ul, ol { margin: 1em 0; padding-left: 2.5em }
            dd { margin-left: 2.5em }
            pre { font-family: monospace; white-space: pre; margin: 1em 0 }
            code, tt, kbd, samp { font-family: monospace }
            em, i, cite, var, dfn { font-style: italic }
            strong, b, th { font-weight: bold }
            small { font-size: 0.83em }
            big { font-size: 1.2em }
            sup { vertical-align: super; font-size: 0.83em }
            sub { vertical-align: sub; font-size: 0.83em }
            center { text-align: center }
            hr { margin: 0.5em 0 }
            """;

    private static final Pattern RULE = Pattern.compile("([^{}]+)\\{([^{}]*)}");
    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern MEDIA = Pattern.compile("@media([^{]*)\\{((?:[^{}]*\\{[^{}]*})*[^{}]*)}");
    private static final Pattern AT_RULE_BLOCK = Pattern.compile("@[a-z-]+[^{;]*\\{((?:[^{}]*\\{[^{}]*})*[^{}]*)}");
    private static final Pattern AT_STATEMENT = Pattern.compile("@[a-z-]+[^{;]*;");
    private static final Pattern FONT_FACE = Pattern.compile("@font-face\\s*\\{([^}]*)}", Pattern.CASE_INSENSITIVE);

    private final List<Rule> rules = new ArrayList<>();
    private final List<FontFace> fontFaces = new ArrayList<>();
    private int order;

    /** A rule set with the declarations of one selector. */
    record Rule(Selector selector, Map<String, String> declarations, int specificity, int order) {
    }

    /** {@code @font-face}: a family name of the book and the font file it points to (relative to the sheet). */
    public record FontFace(String family, String src, boolean bold, boolean italic) {
    }

    public Css() {
        add(DEFAULTS);
    }

    /** Adds a style sheet; later sheets win over earlier ones at the same specificity. */
    public Css add(String sheet) {
        return add(sheet, null);
    }

    /**
     * Adds a style sheet whose {@code @font-face} sources are resolved by {@code resolver}.
     *
     * @param resolver turns a {@code url(...)} of the sheet into a path inside the book; null keeps it as is
     */
    public Css add(String sheet, java.util.function.UnaryOperator<String> resolver) {
        String text = COMMENT.matcher(sheet).replaceAll(" ");
        Matcher faces = FONT_FACE.matcher(text);
        while (faces.find()) {
            Map<String, String> d = declarations(faces.group(1));
            String family = unquote(d.getOrDefault("font-family", ""));
            Matcher url = Pattern.compile("url\\(\\s*['\"]?([^'\")]+)['\"]?\\s*\\)").matcher(d.getOrDefault("src", ""));
            if (!family.isEmpty() && url.find()) {
                String src = resolver == null ? url.group(1) : resolver.apply(url.group(1));
                String weight = d.getOrDefault("font-weight", "normal");
                fontFaces.add(new FontFace(family, src, weight.equals("bold") || number(weight) >= 600,
                        d.getOrDefault("font-style", "normal").matches("italic|oblique")));
            }
        }
        text = FONT_FACE.matcher(text).replaceAll(" ");
        // Media queries: keep the rules for screen and all, drop print, braille and the like.
        StringBuilder flat = new StringBuilder();
        Matcher media = MEDIA.matcher(text);
        int last = 0;
        while (media.find()) {
            flat.append(text, last, media.start());
            String query = media.group(1).toLowerCase(Locale.ROOT);
            if (query.isBlank() || query.contains("screen") || query.contains("all")) flat.append(media.group(2));
            last = media.end();
        }
        flat.append(text.substring(last));
        text = AT_RULE_BLOCK.matcher(flat).replaceAll(" ");
        text = AT_STATEMENT.matcher(text).replaceAll(" ");
        Matcher m = RULE.matcher(text);
        while (m.find()) {
            Map<String, String> declarations = declarations(m.group(2));
            if (declarations.isEmpty()) continue;
            int ruleOrder = order++;
            for (String selectorText : m.group(1).split(",")) {
                Selector selector = Selector.parse(selectorText.strip());
                if (selector != null) {
                    rules.add(new Rule(selector, declarations, selector.specificity(), ruleOrder));
                }
            }
        }
        rules.sort(Comparator.comparingInt(Rule::specificity).thenComparingInt(Rule::order));
        return this;
    }

    public List<FontFace> fontFaces() {
        return List.copyOf(fontFaces);
    }

    static Map<String, String> declarations(String block) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String d : block.split(";")) {
            int colon = d.indexOf(':');
            if (colon <= 0) continue;
            String name = d.substring(0, colon).strip().toLowerCase(Locale.ROOT);
            String value = d.substring(colon + 1).replace("!important", "").strip();
            if (!name.isEmpty() && !value.isEmpty()) result.put(name, value);
        }
        return result;
    }

    // --8<-- [start:cascade]
    /** The declarations that apply to the element, the winning value of each property. */
    public Map<String, String> declared(Element element) {
        Map<String, String> result = new HashMap<>();
        for (Rule rule : rules) {
            if (rule.selector().matches(element)) result.putAll(expand(rule.declarations()));
        }
        String inline = element.attr("style");
        if (!inline.isEmpty()) result.putAll(expand(declarations(inline)));
        return result;
    }

    /** The computed style of the element, inheriting from its parent's. */
    public Style style(Element element, Style parent) {
        return parent.child(declared(element), element.tagName().toLowerCase(Locale.ROOT));
    }
    // --8<-- [end:cascade]

    /** Shorthands the layout needs in their long form: margin, padding and break-before. */
    static Map<String, String> expand(Map<String, String> declarations) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : declarations.entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            if (name.equals("margin") || name.equals("padding")) {
                String[] p = value.split("\\s+");
                String top = p[0];
                String right = p.length > 1 ? p[1] : top;
                String bottom = p.length > 2 ? p[2] : top;
                String left = p.length > 3 ? p[3] : right;
                result.put(name + "-top", top);
                result.put(name + "-right", right);
                result.put(name + "-bottom", bottom);
                result.put(name + "-left", left);
            } else if (name.equals("break-before")) {
                result.put("page-break-before", value);
            } else if (name.equals("font")) {
                // "font: italic bold 1.2em serif": only the style, weight and size matter here
                for (String part : value.split("\\s+")) {
                    if (part.equals("italic") || part.equals("oblique")) result.put("font-style", "italic");
                    else if (part.equals("bold")) result.put("font-weight", "bold");
                    else if (part.matches("[0-9.]+(em|%|px|pt|rem)(/.*)?")) result.put("font-size", part.replaceAll("/.*", ""));
                }
            } else {
                result.put(name, value);
            }
        }
        return result;
    }

    static String unquote(String s) {
        String t = s.strip();
        if (t.length() >= 2 && (t.startsWith("\"") && t.endsWith("\"") || t.startsWith("'") && t.endsWith("'"))) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    static double number(String s) {
        try {
            return Double.parseDouble(s.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ---------------------------------------------------------------- selectors

    /** A selector: compound parts (type, classes, id) joined by descendant or child combinators. */
    record Selector(List<Compound> parts, List<Character> combinators) {
        record Compound(String tag, List<String> classes, String id) {
            boolean matches(Element e) {
                if (tag != null && !tag.equals("*") && !tag.equalsIgnoreCase(localName(e))) return false;
                if (id != null && !id.equals(e.id())) return false;
                for (String c : classes) {
                    if (!e.hasClass(c)) return false;
                }
                return true;
            }
        }

        private static final Pattern COMPOUND = Pattern.compile("^([a-zA-Z][a-zA-Z0-9-]*|\\*)?((?:[.#][a-zA-Z0-9_-]+)*)$");

        static Selector parse(String text) {
            if (text.isEmpty() || text.contains(":") || text.contains("[") || text.contains("+") || text.contains("~")) {
                return null;
            }
            String spaced = text.replace(">", " > ").strip();
            List<Compound> parts = new ArrayList<>();
            List<Character> combinators = new ArrayList<>();
            char pending = ' ';
            for (String token : spaced.split("\\s+")) {
                if (token.equals(">")) {
                    pending = '>';
                    continue;
                }
                Matcher m = COMPOUND.matcher(token);
                if (!m.matches()) return null;
                List<String> classes = new ArrayList<>();
                String id = null;
                for (String piece : m.group(2).split("(?=[.#])")) {
                    if (piece.startsWith(".")) classes.add(piece.substring(1));
                    else if (piece.startsWith("#")) id = piece.substring(1);
                }
                if (!parts.isEmpty()) combinators.add(pending);
                parts.add(new Compound(m.group(1), classes, id));
                pending = ' ';
            }
            return parts.isEmpty() ? null : new Selector(parts, combinators);
        }

        int specificity() {
            int s = 0;
            for (Compound c : parts) {
                if (c.id() != null) s += 10000;
                s += 100 * c.classes().size();
                if (c.tag() != null && !c.tag().equals("*")) s += 1;
            }
            return s;
        }

        boolean matches(Element element) {
            return matches(element, parts.size() - 1);
        }

        private boolean matches(Element element, int index) {
            if (!parts.get(index).matches(element)) return false;
            if (index == 0) return true;
            char combinator = combinators.get(index - 1);
            Element parent = element.parent();
            if (combinator == '>') return parent != null && matches(parent, index - 1);
            for (Element ancestor = parent; ancestor != null; ancestor = ancestor.parent()) {
                if (matches(ancestor, index - 1)) return true;
            }
            return false;
        }
    }

    static String localName(Element e) {
        String tag = e.tagName();
        int colon = tag.indexOf(':');
        return colon >= 0 ? tag.substring(colon + 1) : tag;
    }
}
