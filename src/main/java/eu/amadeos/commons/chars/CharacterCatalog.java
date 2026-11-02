package eu.amadeos.commons.chars;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The characters the Special Characters dialog offers: the categories, the words a user can search for in their
 * own language, and the search itself. Plain data, no Swing, so it is easy to test.
 *
 * <pre>
 * CharacterCatalog catalog = CharacterCatalog.of(Locale.GERMAN);
 * catalog.search("gedankenstrich");   // [0x2014, ...]
 * catalog.search("U+20AC");           // [0x20AC]
 * catalog.search("e");                // é è ê ë ē ... every e with a mark
 * </pre>
 */
public final class CharacterCatalog {

    // --8<-- [start:categories]
    /** The groups on the left of the dialog. {@link #RECENT} and {@link #ALL} have no fixed characters. */
    public enum Category {
        RECENT(""),
        ACCENTED("àáâãäåæçèéêëìíîïñòóôõöøœùúûüýÿßÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÑÒÓÔÕÖØŒÙÚÛÜÝŸ"
                + "āăąćĉċčďđēĕėęěĝğġģĥħĩīĭįıĵķĺļľŀłńņňŋōŏőŕŗřśŝşšţťŧũūŭůűųŵŷźżžșțẞ"
                + "ĀĂĄĆĈĊČĎĐĒĔĖĘĚĜĞĠĢĤĦĨĪĬĮİĴĶĹĻĽĿŁŃŅŇŊŌŎŐŔŖŘŚŜŞŠŢŤŦŨŪŬŮŰŲŴŶŹŻŽȘȚðþÐÞ"),
        PUNCTUATION("„“”‚‘’«»‹›–—‐‑‒―…·•‣◦¡¿§¶†‡′″‴‰‱‖⁄※‽№℃℉"
                + "\u00A0\u202F\u2009\u00AD"),
        CURRENCY("€£¥¢$₹₽₺₩₪₫₱₴₸₿¤₣₤₦₡₲₵₭₮₼₾₠"),
        MATHS("±×÷≠≈≡≤≥<>∞√∛∑∏∫∂∆∇∈∉∋∩∪⊂⊃⊆⊇∧∨¬∀∃∅∝∠⊥∥°′″%‰µπ"
                + "⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻₀₁₂₃₄₅₆₇₈₉½⅓⅔¼¾⅕⅙⅛⅜⅝⅞ⅠⅡⅢⅣⅤⅥⅦⅧⅨⅩⅪⅫⅬⅭⅮⅯ"),
        ARROWS("←↑→↓↔↕↖↗↘↙⇐⇑⇒⇓⇔⇕↩↪↺↻⤴⤵➔➜➝➞➡⬅⬆⬇▲▼◀▶△▽◁▷"),
        GREEK("αβγδεζηθικλμνξοπρσςτυφχψωΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ"),
        SYMBOLS("©®™℗℠✓✔✗✘★☆♥♡♦♣♠●○■□◆◇☐☑☒☺☹☀☁☂☃☎✆✈✉✂✎✏⚠⚡♪♫♀♂⚥⌘⌥⏎⌫⎋☕⚙☯☮✝☪✡☘⚓⌛⌚"),
        // Smileys and emoji: single code points only (no skin tones or joined sequences), the ones the
        // black and white emoji font Symbola draws
        EMOJI("😀😁😂😃😄😅😆😇😈😉😊😋😌😍😎😏😐😑😒😓😔😕😖😗😘😙😚😛😜😝😞😟😠😡😢😣😤😥😦😧😨😩😪😫😬😭😮😯😰😱😲😳😴😵😶😷😸😹😺😻😼😽😾😿🙀🙁🙂🙃🙄🙅🙆🙇🙈🙉🙊🙋🙌🙍🙎🙏🤐🤑🤒🤓🤔🤕🤗🤠🤡🤢🤣🤤🤥🤧"
                + "👍👎👌✌🤞👏🙌🙏💪👋✋☝👆👇👈👉🤝❤💔💕💖💗💙💚💛💜🖤💯🐶🐱🐭🐹🐰🦊🐻🐼🐨🐯🦁🐮🐷🐸🐵🐔🐧🐦🐤🦆🦅🦉🐴🦄🐝🐛🦋🐌🐞🐢🐍🐙🐠🐟🐬🐳🍏🍎🍐🍊🍋🍌🍉🍇🍓🍒🍑🍍🥝🍅🥕🌽🥔🍞🧀🍕🍔🍟🌭🍝🍰🎂🍩🍪🍫🍦☕🍵🍺🍷🥂🎉🎊🎁🎈🎄🎃⚽🏀🎾🎮🎲🎵🎶🎸🚗🚌🚲✈🚀🏠⛰🌍☀⛅☁🌧⛄🌈⭐🌙🔥💧❄📱💻⌨📷📚📎🔑🔒💡⏰📅📦🎓✅❌❓❗⛔🚫♻➕➖"),
        ALL("");
        // --8<-- [end:categories]

        private final String characters;

        Category(String characters) {
            this.characters = characters;
        }

        /** The fixed characters of this category, in order. Empty for {@link #RECENT} and {@link #ALL}. */
        public List<Integer> characters() {
            return characters.codePoints().distinct().boxed().toList();
        }

        /** The key of the category's name in the amade-commons texts, e.g. {@code chars.category.currency}. */
        public String textKey() {
            return "chars.category." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** Search results are cut after this many characters. */
    public static final int MAX_RESULTS = 600;
    /** The highest code point the dialog offers: the Basic and Supplementary Multilingual Planes. */
    static final int LAST = 0x1FFFF;

    private static final Pattern PREFIXED_CODE = Pattern.compile("(?i)(?:u\\+|0x)([0-9a-f]{1,6})");
    private static final Pattern BARE_CODE = Pattern.compile("[0-9a-fA-F]{4,6}");
    private static volatile int[] all;
    private static volatile String[] names;

    private final Locale locale;
    /** Code point → words of the user's language, then the English words: what the search looks at. */
    private final Map<Integer, String> words;
    /** Code point → words of the user's language only: what the preview shows. */
    private final Map<Integer, String> own;

    private CharacterCatalog(Locale locale, Map<Integer, String> words, Map<Integer, String> own) {
        this.locale = locale;
        this.words = words;
        this.own = own;
    }

    /** The catalog with the search words of the locale's language (English words always count too). */
    public static CharacterCatalog of(Locale locale) {
        Map<Integer, String> english = readWords("words.properties");
        String language = locale.getLanguage();
        Map<Integer, String> own = language.isEmpty() || language.equals("en")
                ? english : readWords("words_" + language + ".properties");
        Map<Integer, String> words = new TreeMap<>(english);
        if (own != english) {
            // The user's language first, then the English words
            own.forEach((cp, w) -> words.merge(cp, w, (en, mine) -> mine + " " + en));
        }
        return new CharacterCatalog(locale, words, own);
    }

    private static Map<Integer, String> readWords(String resource) {
        Map<Integer, String> words = new HashMap<>();
        try (InputStream in = CharacterCatalog.class.getResourceAsStream(resource)) {
            if (in == null) return words;
            Properties p = new Properties();
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                p.load(r);
            }
            for (String key : p.stringPropertyNames()) {
                words.put(Integer.parseInt(key, 16), p.getProperty(key).strip());
            }
            return words;
        } catch (IOException | NumberFormatException e) {
            throw new IllegalStateException("Broken word list " + resource, e);
        }
    }

    public Locale locale() {
        return locale;
    }

    /** The characters of a category; {@link Category#ALL} gives every character the dialog offers. */
    public List<Integer> characters(Category category) {
        if (category == Category.ALL) {
            int[] a = all();
            List<Integer> list = new ArrayList<>(a.length);
            for (int cp : a) list.add(cp);
            return Collections.unmodifiableList(list);
        }
        return category.characters();
    }

    /** True for a character that is drawn as an emoji: the emoji category and everything from U+1F000. */
    public static boolean isEmoji(int cp) {
        return cp >= 0x1F000 || Category.EMOJI.characters.indexOf(cp) >= 0;
    }

    /** The Unicode name in sentence case: {@code Latin small letter a with diaeresis}. */
    public static String name(int cp) {
        String n = Character.getName(cp);
        if (n == null) return String.format("U+%04X", cp);
        n = n.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    /**
     * The words of the user's language for the character, the first ones are its usual name; empty if none, and
     * always empty in English, where the Unicode name says it already.
     */
    public String localWords(int cp) {
        if (locale.getLanguage().isEmpty() || locale.getLanguage().equals("en")) return "";
        return own.getOrDefault(cp, "");
    }

    /** {@code U+00E4}. */
    public static String code(int cp) {
        return String.format("U+%04X", cp);
    }

    // --8<-- [start:search]
    /**
     * The characters that fit what the user typed:
     * <ul>
     *     <li>a code: {@code U+20AC}, {@code u+20ac}, {@code 20AC} (four to six hex digits);</li>
     *     <li>a single letter: the letter with every mark on it ({@code e} gives é è ê ë ē ...);</li>
     *     <li>words: every word must start a word of the Unicode name or of our word list, in the user's
     *     language or in English ({@code dash}, {@code gedankenstrich}, {@code euro}, {@code a umlaut}).</li>
     * </ul>
     * Characters from the categories come before the rest of Unicode.
     */
    public List<Integer> search(String query) {
        String q = query == null ? "" : query.strip();
        if (q.isEmpty()) return List.of();
        Set<Integer> found = new LinkedHashSet<>();
        Matcher prefixed = PREFIXED_CODE.matcher(q);
        if (prefixed.matches()) {
            int cp = Integer.parseInt(prefixed.group(1), 16);
            return offered(cp) ? List.of(cp) : List.of();
        }
        if (BARE_CODE.matcher(q).matches()) {
            // 20ac is a code; "face" is a code and a word, so the words are searched too
            int cp = Integer.parseInt(q, 16);
            if (offered(cp)) found.add(cp);
        }
        if (q.codePointCount(0, q.length()) == 1) {
            int typed = q.codePointAt(0);
            if (Character.isLetter(typed)) {
                found.addAll(withMarks(typed));
                return cut(found);
            }
            found.add(typed);
        }
        String[] terms = normalise(q).split(" +");
        List<Integer> rest = new ArrayList<>();
        // Our own words first: they are the characters people mean
        for (Map.Entry<Integer, String> e : words.entrySet()) {
            if (matches(terms, normalise(e.getValue()))) found.add(e.getKey());
        }
        for (Category c : Category.values()) {
            for (int cp : c.characters()) {
                if (!found.contains(cp) && matches(terms, normalise(name(cp)))) found.add(cp);
            }
        }
        int[] a = all();
        String[] n = names();
        for (int i = 0; i < a.length && found.size() + rest.size() < MAX_RESULTS; i++) {
            if (!found.contains(a[i]) && matches(terms, n[i])) rest.add(a[i]);
        }
        found.addAll(rest);
        return cut(found);
    }
    // --8<-- [end:search]

    private static List<Integer> cut(Set<Integer> found) {
        return found.stream().limit(MAX_RESULTS).toList();
    }

    /** Every offered character whose base letter (after removing marks) is the given letter, either case. */
    static List<Integer> withMarks(int letter) {
        String lower = new String(Character.toChars(Character.toLowerCase(letter)));
        String upper = new String(Character.toChars(Character.toUpperCase(letter)));
        List<Integer> lowers = new ArrayList<>(), uppers = new ArrayList<>();
        for (int cp : Category.ACCENTED.characters()) collect(cp, lower, upper, lowers, uppers);
        for (int cp = 0xC0; cp <= 0x24F; cp++) collect(cp, lower, upper, lowers, uppers);
        for (int cp = 0x1E00; cp <= 0x1EFF; cp++) collect(cp, lower, upper, lowers, uppers);
        Set<Integer> result = new LinkedHashSet<>(lowers);
        result.addAll(uppers);
        return List.copyOf(result);
    }

    private static void collect(int cp, String lower, String upper, List<Integer> lowers, List<Integer> uppers) {
        String s = new String(Character.toChars(cp));
        String base = Normalizer.normalize(s, Normalizer.Form.NFD);
        if (base.equals(s) && !special(cp, lower)) return;
        if (base.startsWith(lower) || special(cp, lower)) {
            if (!lowers.contains(cp)) lowers.add(cp);
        } else if (base.startsWith(upper) || special(cp, upper)) {
            if (!uppers.contains(cp)) uppers.add(cp);
        }
    }

    /** Letters Unicode does not decompose but people look for under their base letter: ø under o, ß under s ... */
    private static boolean special(int cp, String letter) {
        String extra = switch (letter) {
            case "a" -> "æ";
            case "A" -> "Æ";
            case "o" -> "øœ";
            case "O" -> "ØŒ";
            case "s" -> "ß";
            case "S" -> "ẞ";
            case "d" -> "đð";
            case "D" -> "ĐÐ";
            case "l" -> "ł";
            case "L" -> "Ł";
            case "t" -> "þŧ";
            case "T" -> "ÞŦ";
            case "h" -> "ħ";
            case "H" -> "Ħ";
            case "i" -> "ı";
            default -> "";
        };
        return extra.indexOf(cp) >= 0;
    }

    private static boolean matches(String[] terms, String text) {
        for (String term : terms) {
            if (!startsAWord(text, term)) return false;
        }
        return true;
    }

    private static boolean startsAWord(String text, String term) {
        int from = 0;
        while (true) {
            int i = text.indexOf(term, from);
            if (i < 0) return false;
            if (i == 0 || text.charAt(i - 1) == ' ' || text.charAt(i - 1) == '-') return true;
            from = i + 1;
        }
    }

    /** Lower case, no marks (so "Wahrung" finds "Währung"), only letters, digits and single spaces. */
    static String normalise(String s) {
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('ß', 's');
        return n.replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }

    /** True for a character the dialog offers: assigned, visible, not a control, surrogate or private one. */
    static boolean offered(int cp) {
        if (cp < 0x20 || cp > LAST || !Character.isDefined(cp)) return false;
        return switch (Character.getType(cp)) {
            case Character.CONTROL, Character.SURROGATE, Character.PRIVATE_USE, Character.UNASSIGNED -> false;
            default -> true;
        };
    }

    private static int[] all() {
        int[] a = all;
        if (a == null) {
            a = java.util.stream.IntStream.rangeClosed(0x20, LAST).filter(CharacterCatalog::offered).toArray();
            all = a;
        }
        return a;
    }

    private static String[] names() {
        String[] n = names;
        if (n == null) {
            int[] a = all();
            n = new String[a.length];
            for (int i = 0; i < a.length; i++) {
                String name = Character.getName(a[i]);
                n[i] = name == null ? "" : normalise(name);
            }
            names = n;
        }
        return n;
    }

    /** Builds the name index in the background, so that the first search does not wait for it. */
    public static void warmUp() {
        Thread t = new Thread(CharacterCatalog::names, "special-characters-index");
        t.setDaemon(true);
        t.start();
    }
}
