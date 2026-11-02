package eu.amadeos.commons.typeset;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/** Paths inside the ZIP container of a book: always with {@code /}, relative to the archive's root. */
public final class ZipPaths {
    private ZipPaths() {
    }

    /**
     * Resolves a link of a file inside the book against that file, without its fragment.
     * {@code resolve("OEBPS/text/ch1.xhtml", "../images/a%20b.png#x")} is {@code OEBPS/images/a b.png}.
     */
    public static String resolve(String from, String href) {
        String target = stripFragment(href);
        if (target.isEmpty()) return from;
        target = decode(target);
        if (target.startsWith("/")) return normalize(target.substring(1));
        int slash = from.lastIndexOf('/');
        return normalize((slash >= 0 ? from.substring(0, slash + 1) : "") + target);
    }

    /** The part after {@code #}, empty when there is none. */
    public static String fragment(String href) {
        int hash = href.indexOf('#');
        return hash >= 0 ? decode(href.substring(hash + 1)) : "";
    }

    /** True for links that leave the book: web, mail and other schemes. */
    public static boolean external(String href) {
        return href.matches("(?i)^[a-z][a-z0-9+.-]*:.*");
    }

    static String stripFragment(String href) {
        String s = href.strip();
        int cut = s.length();
        int hash = s.indexOf('#');
        if (hash >= 0) cut = hash;
        int query = s.indexOf('?');
        if (query >= 0 && query < cut) cut = query;
        return s.substring(0, cut);
    }

    private static String decode(String s) {
        if (!s.contains("%")) return s;
        try {
            return URLDecoder.decode(s.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return s;
        }
    }

    static String normalize(String path) {
        Deque<String> parts = new ArrayDeque<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                if (!parts.isEmpty()) parts.removeLast();
            } else {
                parts.addLast(part);
            }
        }
        return String.join("/", parts);
    }
}
