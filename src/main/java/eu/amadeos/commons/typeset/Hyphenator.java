package eu.amadeos.commons.typeset;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hyphenation with Frank Liang's patterns, the method of TeX, read from the dictionaries LibreOffice uses
 * ({@code /usr/share/hyphen/hyph_de_DE.dic} from the Debian packages hyphen-de, hyphen-en-us and so on).
 * Patterns of all levels are merged; the extended patterns with {@code /} are skipped.
 */
public final class Hyphenator {
    private final Map<String, byte[]> patterns;
    private final int maxLength;
    private final int leftMin;
    private final int rightMin;

    Hyphenator(Map<String, byte[]> patterns, int leftMin, int rightMin) {
        this.patterns = Map.copyOf(patterns);
        this.maxLength = patterns.keySet().stream().mapToInt(String::length).max().orElse(0);
        this.leftMin = Math.max(1, leftMin);
        this.rightMin = Math.max(1, rightMin);
    }

    /** Reads a dictionary; its first line names the encoding. */
    public static Hyphenator load(Path dictionary) throws IOException {
        byte[] bytes = Files.readAllBytes(dictionary);
        int firstLine = 0;
        while (firstLine < bytes.length && bytes[firstLine] != '\n') firstLine++;
        String encoding = new String(bytes, 0, firstLine, StandardCharsets.US_ASCII).strip();
        Charset charset;
        try {
            charset = Charset.forName(encoding);
        } catch (IllegalArgumentException e) {
            charset = StandardCharsets.UTF_8;
        }
        return parse(new String(bytes, Math.min(bytes.length, firstLine + 1), Math.max(0, bytes.length - firstLine - 1), charset));
    }

    /** Patterns in the dictionary format, without the encoding line. */
    public static Hyphenator parse(String text) {
        Map<String, byte[]> patterns = new HashMap<>();
        int left = 2;
        int right = 2;
        for (String raw : text.split("\r?\n")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("%") || line.startsWith("#")) continue;
            if (line.startsWith("LEFTHYPHENMIN")) {
                left = number(line, left);
                continue;
            }
            if (line.startsWith("RIGHTHYPHENMIN")) {
                right = number(line, right);
                continue;
            }
            if (line.matches("[A-Z]+.*") || line.contains("/")) continue;
            StringBuilder letters = new StringBuilder();
            List<Byte> values = new ArrayList<>();
            values.add((byte) 0);
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c >= '0' && c <= '9') {
                    values.set(values.size() - 1, (byte) (c - '0'));
                } else {
                    letters.append(Character.toLowerCase(c));
                    values.add((byte) 0);
                }
            }
            byte[] v = new byte[values.size()];
            for (int i = 0; i < v.length; i++) v[i] = values.get(i);
            patterns.merge(letters.toString(), v, Hyphenator::max);
        }
        return new Hyphenator(patterns, left, right);
    }

    private static byte[] max(byte[] a, byte[] b) {
        byte[] r = a.clone();
        for (int i = 0; i < Math.min(a.length, b.length); i++) r[i] = (byte) Math.max(a[i], b[i]);
        return r;
    }

    private static int number(String line, int fallback) {
        try {
            return Integer.parseInt(line.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // --8<-- [start:points]
    /**
     * The places where the word may be broken: {@code k} means after its {@code k}-th character, so
     * "hyphenation" gives 2, 6 and 7 (hy-phen-ation... as the patterns decide). Words with digits or fewer than
     * five letters are not broken.
     */
    public List<Integer> points(String word) {
        int n = word.length();
        List<Integer> result = new ArrayList<>();
        if (n < Math.max(5, leftMin + rightMin)) return result;
        for (int i = 0; i < n; i++) {
            if (!Character.isLetter(word.charAt(i))) return result;
        }
        String w = "." + word.toLowerCase(Locale.ROOT) + ".";
        byte[] values = new byte[w.length() + 1];
        for (int start = 0; start < w.length(); start++) {
            for (int end = start + 1; end <= Math.min(w.length(), start + maxLength); end++) {
                byte[] p = patterns.get(w.substring(start, end));
                if (p == null) continue;
                for (int k = 0; k < p.length; k++) {
                    if (p[k] > values[start + k]) values[start + k] = p[k];
                }
            }
        }
        for (int k = leftMin; k <= n - rightMin; k++) {
            if (values[k + 1] % 2 == 1) result.add(k);
        }
        return result;
    }
    // --8<-- [end:points]

    /** The word with {@code -} at every place it may break, for tests and the handbook. */
    public String show(String word) {
        StringBuilder sb = new StringBuilder(word);
        List<Integer> points = points(word);
        for (int i = points.size() - 1; i >= 0; i--) sb.insert((int) points.get(i), '-');
        return sb.toString();
    }
}
