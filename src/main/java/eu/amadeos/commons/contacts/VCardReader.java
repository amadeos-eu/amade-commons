package eu.amadeos.commons.contacts;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the few vCard properties other programs need, without a vCard library: {@code UID}, {@code FN}, {@code N},
 * {@code ORG}, {@code NICKNAME}, {@code EMAIL}, {@code BDAY} and {@code CATEGORIES}, from vCard 2.1, 3.0 and 4.0.
 * It unfolds lines, drops group prefixes ({@code item1.EMAIL}), unescapes values and decodes vCard 2.1's
 * {@code QUOTED-PRINTABLE}. Everything else in a card is skipped.
 */
final class VCardReader {
    private static final Pattern ISO_DATE = Pattern.compile("(\\d{4}|--|-)-?(\\d{2})-?(\\d{2}).*");

    private VCardReader() {
    }

    /** One content line: its name in upper case, its parameters (names in upper case) and its raw value. */
    record Property(String name, Map<String, List<String>> parameters, String value) {
        List<String> parameter(String key) {
            return parameters.getOrDefault(key, List.of());
        }
    }

    /** Every card in {@code text}, which may hold several; {@code fallbackUid} names a card without a UID. */
    static List<ContactCard> read(String text, String book, String fallbackUid) {
        List<ContactCard> cards = new ArrayList<>();
        List<Property> card = null;
        for (Property p : properties(text)) {
            if (p.name().equals("BEGIN") && p.value().equalsIgnoreCase("VCARD")) {
                card = new ArrayList<>();
            } else if (p.name().equals("END") && p.value().equalsIgnoreCase("VCARD")) {
                if (card != null) {
                    String fallback = cards.isEmpty() ? fallbackUid : fallbackUid + "-" + cards.size();
                    cards.add(toCard(card, book, fallback));
                }
                card = null;
            } else if (card != null) {
                card.add(p);
            }
        }
        return cards;
    }

    private static ContactCard toCard(List<Property> properties, String book, String fallbackUid) {
        String uid = "";
        String fn = "";
        String n = "";
        String org = "";
        String nickname = "";
        ContactCard.Birthday birthday = null;
        List<ContactCard.Email> emails = new ArrayList<>();
        Set<String> groups = new LinkedHashSet<>();
        for (Property p : properties) {
            switch (p.name()) {
                case "UID" -> uid = text(p).replaceFirst("(?i)^urn:uuid:", "");
                case "FN" -> fn = text(p);
                case "N" -> n = name(components(p, ';'));
                case "ORG" -> org = String.join(", ", components(p, ';').stream().filter(s -> !s.isBlank()).toList());
                case "NICKNAME" -> nickname = components(p, ',').stream().findFirst().orElse("");
                case "EMAIL" -> email(p).ifPresent(emails::add);
                case "BDAY" -> birthday = birthday(text(p)).orElse(birthday);
                case "CATEGORIES" -> components(p, ',').stream().filter(s -> !s.isBlank()).forEach(groups::add);
                default -> {
                }
            }
        }
        // Preferred addresses first, the order of the file otherwise (List.sort is stable).
        emails.sort(java.util.Comparator.comparingInt(ContactCard.Email::preference));
        String label = !fn.isBlank() ? fn : !n.isBlank() ? n : !org.isBlank() ? org : !nickname.isBlank() ? nickname : "";
        return new ContactCard(uid.isBlank() ? fallbackUid : uid, book, label, nickname, org, emails, birthday,
                List.copyOf(groups));
    }

    /** "Dr. Anna Maria Berger" from {@code N:Berger;Anna;Maria;Dr.;}. */
    private static String name(List<String> parts) {
        List<String> words = new ArrayList<>();
        int[] order = {3, 1, 2, 0, 4}; // prefix, given, additional, family, suffix
        for (int i : order) if (i < parts.size() && !parts.get(i).isBlank()) words.add(parts.get(i).strip());
        return String.join(" ", words);
    }

    private static Optional<ContactCard.Email> email(Property p) {
        String address = text(p).replaceFirst("(?i)^mailto:", "").strip();
        if (address.isEmpty() || !address.contains("@")) return Optional.empty();
        int preference = 100;
        for (String pref : p.parameter("PREF")) {
            try {
                preference = Math.min(preference, Integer.parseInt(pref.strip()));
            } catch (NumberFormatException e) {
                preference = Math.min(preference, 1);
            }
        }
        Set<String> types = new LinkedHashSet<>();
        for (String type : p.parameter("TYPE")) {
            for (String t : type.split(",")) {
                String lower = t.strip().toLowerCase(Locale.ROOT);
                if (lower.equals("pref")) preference = Math.min(preference, 1);
                else if (!lower.isEmpty() && !lower.equals("internet")) types.add(lower);
            }
        }
        return Optional.of(new ContactCard.Email(address, preference, List.copyOf(types)));
    }

    /**
     * {@code 1986-11-14}, {@code 19861114}, {@code --1114} and {@code --11-14}; a time after the date is ignored.
     * Apple and Outlook write the year 1604 for a birthday without a year.
     */
    static Optional<ContactCard.Birthday> birthday(String value) {
        Matcher m = ISO_DATE.matcher(value.strip());
        if (!m.matches()) return Optional.empty();
        try {
            int year = m.group(1).startsWith("-") ? 0 : Integer.parseInt(m.group(1));
            MonthDay day = MonthDay.of(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            if (year == 1604) year = 0;
            if (year != 0 && !day.isValidYear(year)) return Optional.empty();
            return Optional.of(new ContactCard.Birthday(day, year));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    // ---- content lines --------------------------------------------------------------------------

    static List<Property> properties(String text) {
        List<Property> result = new ArrayList<>();
        for (String line : unfold(text)) {
            Property p = parse(line);
            if (p != null) result.add(p);
        }
        return result;
    }

    /** Joins folded lines (a line break followed by a space or tab) and vCard 2.1's soft breaks ({@code =} at the end). */
    private static List<String> unfold(String text) {
        String[] raw = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<String> lines = new ArrayList<>();
        StringBuilder current = null;
        boolean quotedPrintable = false;
        for (String line : raw) {
            if (current != null && (!line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t'))) {
                current.append(line, 1, line.length());
            } else if (current != null && quotedPrintable && current.toString().endsWith("=")) {
                current.setLength(current.length() - 1);
                current.append(line);
            } else {
                if (current != null) lines.add(current.toString());
                current = new StringBuilder(line);
                quotedPrintable = line.toUpperCase(Locale.ROOT).contains("QUOTED-PRINTABLE");
            }
        }
        if (current != null) lines.add(current.toString());
        return lines;
    }

    private static Property parse(String line) {
        int colon = valueStart(line);
        if (colon < 0) return null;
        List<String> head = split(line.substring(0, colon), ';', true);
        if (head.isEmpty() || head.getFirst().isBlank()) return null;
        String name = head.getFirst().strip();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) name = name.substring(dot + 1);
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        for (String parameter : head.subList(1, head.size())) {
            int eq = parameter.indexOf('=');
            // vCard 2.1 writes types without a name: EMAIL;INTERNET;PREF:...
            String key = eq < 0 ? (isEncoding(parameter) ? "ENCODING" : "TYPE") : parameter.substring(0, eq);
            String value = eq < 0 ? parameter : parameter.substring(eq + 1);
            value = value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                    ? value.substring(1, value.length() - 1) : value;
            parameters.computeIfAbsent(key.strip().toUpperCase(Locale.ROOT), k -> new ArrayList<>()).add(value);
        }
        String value = line.substring(colon + 1);
        if (parameters.getOrDefault("ENCODING", List.of()).stream().anyMatch(e -> e.equalsIgnoreCase("QUOTED-PRINTABLE"))) {
            value = quotedPrintable(value, charset(parameters));
        }
        return new Property(name.toUpperCase(Locale.ROOT), parameters, value);
    }

    private static boolean isEncoding(String parameter) {
        String p = parameter.strip().toUpperCase(Locale.ROOT);
        return p.equals("QUOTED-PRINTABLE") || p.equals("BASE64") || p.equals("8BIT") || p.equals("7BIT");
    }

    /** The colon that ends the name and parameters, outside quoted parameter values. */
    private static int valueStart(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (c == ':' && !quoted) return i;
        }
        return -1;
    }

    private static Charset charset(Map<String, List<String>> parameters) {
        try {
            return Charset.forName(parameters.getOrDefault("CHARSET", List.of("UTF-8")).getFirst());
        } catch (RuntimeException e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static String quotedPrintable(String value, Charset charset) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '=' && i + 2 < value.length()) {
                try {
                    bytes.write(Integer.parseInt(value.substring(i + 1, i + 3), 16));
                    i += 2;
                    continue;
                } catch (NumberFormatException e) {
                    // not an escape: keep the '='
                }
            }
            byte[] plain = String.valueOf(c).getBytes(charset);
            bytes.write(plain, 0, plain.length);
        }
        return bytes.toString(charset);
    }

    // ---- values ---------------------------------------------------------------------------------

    /** The whole value, unescaped. */
    static String text(Property p) {
        return unescape(p.value()).strip();
    }

    /** The value split at unescaped {@code separator}s, each part unescaped. */
    static List<String> components(Property p, char separator) {
        return split(p.value(), separator, false).stream().map(VCardReader::unescape).map(String::strip).toList();
    }

    private static List<String> split(String text, char separator, boolean respectQuotes) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length() && !respectQuotes) {
                current.append(c).append(text.charAt(++i));
            } else if (c == '"' && respectQuotes) {
                quoted = !quoted;
                current.append(c);
            } else if (c == separator && !quoted) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                out.append(next == 'n' || next == 'N' ? '\n' : next);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
