package eu.amadeos.commons.contacts;

import eu.amadeos.commons.config.XdgDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The address books of amade-contacts, read-only, for other programs: Mail completes addresses from them, Calendar
 * shows the birthdays. Every folder in {@code ~/.local/share/amadeos/contacts} is one address book and every
 * {@code .vcf} file in it one contact (a "vdir"), so contacts that vdirsyncer or khard keep there count too.
 *
 * <p>The folder is looked at again at most every two seconds, and only files whose modification time changed are
 * read again, so calling {@link #contacts()} on every key press is cheap. Thread-safe.
 *
 * <pre>
 * ContactDirectory contacts = ContactDirectory.of(xdg);
 * List&lt;Recipient&gt; suggestions = contacts.complete("ann", 8);
 * </pre>
 */
public final class ContactDirectory {
    private static final Logger LOG = LoggerFactory.getLogger(ContactDirectory.class);
    private static final Duration RESCAN = Duration.ofSeconds(2);
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;

    private final Path root;
    private final Clock clock;
    private final Map<Path, CachedFile> files = new HashMap<>();
    private List<ContactCard> contacts = List.of();
    private Instant scanned = Instant.MIN;

    private record CachedFile(FileTime modified, List<ContactCard> cards) {
    }

    public ContactDirectory(Path root, Clock clock) {
        this.root = Objects.requireNonNull(root, "root");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** The address books of the running user, in {@code $XDG_DATA_HOME/amadeos/contacts}. */
    public static ContactDirectory of(XdgDirectories xdg) {
        return new ContactDirectory(folder(xdg), Clock.systemUTC());
    }

    /** {@code ~/.local/share/amadeos/contacts}, where amade-contacts keeps its address books. */
    public static Path folder(XdgDirectories xdg) {
        return xdg.dataHome().resolve("amadeos").resolve("contacts");
    }

    public Path root() {
        return root;
    }

    /** Every contact of every address book, sorted by name; empty if there are no address books. */
    public synchronized List<ContactCard> contacts() {
        Instant now = clock.instant();
        if (scanned.equals(Instant.MIN) || !now.isBefore(scanned.plus(RESCAN))) {
            scanned = now;
            rescan();
        }
        return contacts;
    }

    /** Reads the folder again on the next call, whenever that is. */
    public synchronized void refresh() {
        scanned = Instant.MIN;
    }

    // --8<-- [start:rescan]
    private void rescan() {
        Map<Path, CachedFile> seen = new LinkedHashMap<>();
        for (Path file : vcardFiles()) {
            try {
                FileTime modified = Files.getLastModifiedTime(file);
                CachedFile cached = files.get(file);
                if (cached == null || !cached.modified().equals(modified)) {
                    cached = new CachedFile(modified, read(file));
                }
                seen.put(file, cached);
            } catch (IOException e) {
                LOG.debug("Cannot read {}: {}", file, e.toString());
            }
        }
        files.clear();
        files.putAll(seen);
        List<ContactCard> all = new ArrayList<>();
        for (CachedFile f : seen.values()) all.addAll(f.cards());
        all.sort(Comparator.comparing((ContactCard c) -> fold(c.label())).thenComparing(ContactCard::uid));
        contacts = List.copyOf(all);
    }
    // --8<-- [end:rescan]

    private List<Path> vcardFiles() {
        if (!Files.isDirectory(root)) return List.of();
        List<Path> result = new ArrayList<>();
        try (Stream<Path> books = Files.list(root)) {
            for (Path book : books.filter(Files::isDirectory).sorted().toList()) {
                try (Stream<Path> cards = Files.list(book)) {
                    cards.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".vcf"))
                            .filter(Files::isRegularFile).sorted().forEach(result::add);
                }
            }
        } catch (IOException e) {
            LOG.debug("Cannot list {}: {}", root, e.toString());
        }
        return result;
    }

    private static List<ContactCard> read(Path file) throws IOException {
        if (Files.size(file) > MAX_FILE_SIZE) return List.of();
        byte[] bytes = Files.readAllBytes(file);
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            text = new String(bytes, StandardCharsets.ISO_8859_1); // old vCard 2.1 files
        }
        String name = file.getFileName().toString();
        return VCardReader.read(text, file.getParent().getFileName().toString(), name.substring(0, name.length() - 4));
    }

    // ---- address completion ---------------------------------------------------------------------

    // --8<-- [start:complete]
    /**
     * Suggestions for what was typed into an address field: contacts whose name, nickname or address has a word
     * starting with {@code typed} (ignoring case and accents, so "mull" finds "Müller"), and groups whose name starts
     * with it. Contacts whose whole name starts with it come first, groups after the people.
     *
     * @param limit at most this many suggestions
     */
    public List<Recipient> complete(String typed, int limit) {
        String needle = fold(typed.strip());
        if (needle.isEmpty() || limit <= 0) return List.of();
        record Scored(int score, Recipient recipient) {
        }
        List<Scored> found = new ArrayList<>();
        Map<String, List<ContactCard>> groups = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ContactCard c : contacts()) {
            for (String g : c.groups()) groups.computeIfAbsent(g, k -> new ArrayList<>()).add(c);
            for (ContactCard.Email e : c.emails()) {
                int score = score(needle, c, e.address());
                if (score >= 0) {
                    found.add(new Scored(score, new Recipient(c.name(), e.address(),
                            List.of(Recipient.mailbox(c.name(), e.address())), false)));
                }
            }
        }
        groups.forEach((group, members) -> {
            if (!fold(group).startsWith(needle)) return;
            Set<String> mailboxes = new LinkedHashSet<>();
            for (ContactCard m : members) m.email().ifPresent(a -> mailboxes.add(Recipient.mailbox(m.name(), a)));
            if (mailboxes.size() > 0) {
                found.add(new Scored(3, new Recipient(group, String.valueOf(mailboxes.size()), List.copyOf(mailboxes), true)));
            }
        });
        // Stable sort: within a score, the contacts stay in name order and a contact's preferred address first.
        found.sort(Comparator.comparingInt(Scored::score));
        Set<String> addresses = new LinkedHashSet<>();
        List<Recipient> result = new ArrayList<>();
        for (Scored s : found) {
            if (result.size() >= limit) break;
            if (s.recipient().group() || addresses.add(s.recipient().address().toLowerCase(Locale.ROOT))) {
                result.add(s.recipient());
            }
        }
        return result;
    }
    // --8<-- [end:complete]

    /** 0: the name starts with it, 1: a word of the name or nickname, 2: the address or a part of it; -1: no match. */
    private static int score(String needle, ContactCard c, String address) {
        String name = fold(c.name());
        if (name.startsWith(needle)) return 0;
        if (startsAWord(name, needle) || startsAWord(fold(c.nickname()), needle)) return 1;
        String a = fold(address);
        if (a.startsWith(needle) || startsAWord(a, needle)) return 2;
        return -1;
    }

    private static boolean startsAWord(String text, String needle) {
        for (String word : text.split("[\\s.,_@+\"'()-]+")) if (word.startsWith(needle)) return true;
        return false;
    }

    /** Lower case without accents: "Müller" and "muller" compare equal. */
    static String fold(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return decomposed.replace("ß", "ss").toLowerCase(Locale.ROOT);
    }

    // ---- birthdays ------------------------------------------------------------------------------

    // --8<-- [start:birthdays]
    /** A contact's birthday on one day. */
    public record BirthdayOn(ContactCard contact, LocalDate date) {
        /** The age reached that day, if the contact's year of birth is known. */
        public java.util.Optional<Integer> age() {
            return contact.birthday().ageIn(date.getYear());
        }
    }

    /** The birthdays from {@code from} to {@code to} (inclusive), by date, then by name. */
    public List<BirthdayOn> birthdays(LocalDate from, LocalDate to) {
        List<BirthdayOn> result = new ArrayList<>();
        if (to.isBefore(from)) return result;
        for (ContactCard c : contacts()) {
            if (c.birthday() == null) continue;
            for (int year = from.getYear(); year <= to.getYear(); year++) {
                LocalDate day = c.birthday().on(year);
                if (day.isBefore(from) || day.isAfter(to)) continue;
                if (c.birthday().hasYear() && year < c.birthday().year()) continue;
                result.add(new BirthdayOn(c, day));
            }
        }
        result.sort(Comparator.comparing(BirthdayOn::date)); // stable: name order within a day
        return result;
    }
    // --8<-- [end:birthdays]
}
