package eu.amadeos.commons.i18n;

import eu.amadeos.commons.config.XdgDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The formats of dates, times, numbers, money, paper and measures, when they differ from the language of
 * the desktop: English texts with German dates, for example. Settings &gt; Date and Time keeps the region in
 * {@code ~/.config/amadeos/formats.conf}, one line with a POSIX locale such as {@code de_AT.UTF-8}; without
 * the file the formats follow the language.
 *
 * <p>{@code AmadeApplication} makes the region Java's {@link Locale.Category#FORMAT} locale, so that
 * {@code DateTimeFormatter.ofLocalizedDate}, {@code NumberFormat} and the like use it. Programs that are not
 * written in Java get it through {@link #applyTo(Map)}: {@code LC_TIME}, {@code LC_NUMERIC},
 * {@code LC_MONETARY}, {@code LC_PAPER} and {@code LC_MEASUREMENT}.
 */
public final class RegionalFormats {
    private static final Logger LOG = LoggerFactory.getLogger(RegionalFormats.class);
    public static final String FORMATS_FILE = "formats.conf";
    /** The locale categories a region sets for programs that are not written in Java. */
    public static final List<String> CATEGORIES = List.of("LC_TIME", "LC_NUMERIC", "LC_MONETARY", "LC_PAPER",
            "LC_MEASUREMENT");

    /**
     * The regions offered in Settings, in the order of the list: the languages of amadeOS with their neighbours,
     * then more of Europe and North America. The amadeOS image generates their glibc locales.
     */
    public static final List<Locale> REGIONS = List.of(
            Locale.of("en", "US"), Locale.of("en", "GB"), Locale.of("en", "IE"), Locale.of("en", "CA"),
            Locale.of("de", "DE"), Locale.of("de", "AT"), Locale.of("de", "CH"), Locale.of("de", "LU"),
            Locale.of("fr", "FR"), Locale.of("fr", "BE"), Locale.of("fr", "CH"), Locale.of("fr", "LU"),
            Locale.of("fr", "CA"), Locale.of("es", "ES"), Locale.of("es", "MX"), Locale.of("it", "IT"),
            Locale.of("it", "CH"), Locale.of("nl", "NL"), Locale.of("nl", "BE"), Locale.of("pt", "PT"),
            Locale.of("da", "DK"), Locale.of("sv", "SE"), Locale.of("nb", "NO"), Locale.of("fi", "FI"),
            Locale.of("pl", "PL"), Locale.of("cs", "CZ"));

    private final Path configDirectory;

    /** @param configDirectory {@code ~/.config/amadeos} */
    public RegionalFormats(Path configDirectory) {
        this.configDirectory = configDirectory;
    }

    /** The formats of the running user. */
    public static RegionalFormats system() {
        return new RegionalFormats(XdgDirectories.system().amadeosConfig());
    }

    public Path file() {
        return configDirectory.resolve(FORMATS_FILE);
    }

    /** The region chosen for the formats; empty if they follow the language. Read from the file each time. */
    public Optional<Locale> region() {
        Path file = file();
        try {
            if (!Files.isRegularFile(file)) return Optional.empty();
            String value = Files.readString(file, StandardCharsets.UTF_8).lines().findFirst().orElse("").strip();
            if (value.isEmpty()) return Optional.empty();
            Optional<Locale> locale = parse(value);
            if (locale.isEmpty()) LOG.warn("Ignoring {} in {}", value, file);
            return locale;
        } catch (IOException e) {
            LOG.debug("Cannot read {}", file, e);
            return Optional.empty();
        }
    }

    /** The locale for formats: the region, or else the language's locale. */
    public Locale formatLocale(Locale language) {
        return region().orElse(language);
    }

    /** Stores the region; empty removes the file, so that the formats follow the language again. */
    public void write(Optional<Locale> region) throws IOException {
        Path target = file();
        if (region.isEmpty()) {
            Files.deleteIfExists(target);
            return;
        }
        Files.createDirectories(configDirectory);
        Path temp = Files.createTempFile(configDirectory, FORMATS_FILE, ".tmp");
        try {
            Files.writeString(temp, posix(region.get()) + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    // --8<-- [start:apply]
    /**
     * Sets the locale variables of a region for a program that is about to start (e.g. LibreOffice or Firefox),
     * leaving {@code LANG} and so the language alone. Without a region, removes them so that the language rules.
     */
    public void applyTo(Map<String, String> environment) {
        Optional<Locale> region = region();
        for (String category : CATEGORIES) {
            if (region.isPresent()) environment.put(category, posix(region.get()));
            else environment.remove(category);
        }
    }
    // --8<-- [end:apply]

    /** {@code de_AT.UTF-8} for {@code de-AT}. */
    public static String posix(Locale locale) {
        return locale.getLanguage() + (locale.getCountry().isEmpty() ? "" : "_" + locale.getCountry()) + ".UTF-8";
    }

    /** {@code de_AT.UTF-8}, {@code de_AT.utf8}, {@code de_AT} or {@code de-AT}; empty for anything else. */
    public static Optional<Locale> parse(String value) {
        if (value == null) return Optional.empty();
        String s = value.strip();
        int dot = s.indexOf('.');
        if (dot >= 0) s = s.substring(0, dot);
        int at = s.indexOf('@');
        if (at >= 0) s = s.substring(0, at);
        String[] parts = s.split("[_-]");
        if (parts.length != 2 || !parts[0].matches("[a-z]{2,3}") || !parts[1].matches("[A-Z]{2}")) {
            return Optional.empty();
        }
        return Optional.of(Locale.of(parts[0], parts[1]));
    }

    /** The name of a region in its own language, e.g. {@code Deutsch (Österreich)}. */
    public static String nativeName(Locale region) {
        String name = region.getDisplayName(region);
        return name.isEmpty() ? region.toLanguageTag() : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
