package eu.amadeos.commons.typeset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The hyphenation dictionaries installed on the system, loaded on first use per language. A language without a
 * dictionary is not hyphenated.
 */
public class Hyphenators {
    /** Where Debian's hyphen-* packages put their dictionaries. */
    public static final Path SYSTEM = Path.of("/usr/share/hyphen");
    private static final Logger LOG = LoggerFactory.getLogger(Hyphenators.class);

    private final Path directory;
    private final Map<String, Optional<Hyphenator>> loaded = new HashMap<>();

    public Hyphenators(Path directory) {
        this.directory = directory;
    }

    /**
     * The hyphenator for a language tag such as {@code de}, {@code de-AT} or {@code en_GB}: the dictionary for
     * language and country, else for the language alone, else any of that language.
     */
    public synchronized Optional<Hyphenator> forLanguage(String tag) {
        if (tag == null || tag.isBlank()) return Optional.empty();
        Locale locale = Locale.forLanguageTag(tag.strip().replace('_', '-'));
        String language = locale.getLanguage();
        if (language.isEmpty()) return Optional.empty();
        String key = language + "_" + locale.getCountry();
        return loaded.computeIfAbsent(key, k -> find(language, locale.getCountry()).flatMap(this::load));
    }

    private Optional<Path> find(String language, String country) {
        if (!country.isEmpty()) {
            Path exact = directory.resolve("hyph_" + language + "_" + country + ".dic");
            if (Files.isRegularFile(exact)) return Optional.of(exact);
        }
        Path plain = directory.resolve("hyph_" + language + ".dic");
        if (Files.isRegularFile(plain)) return Optional.of(plain);
        if (!Files.isDirectory(directory)) return Optional.empty();
        try (DirectoryStream<Path> all = Files.newDirectoryStream(directory, "hyph_" + language + "_*.dic")) {
            for (Path p : all) return Optional.of(p);
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<Hyphenator> load(Path file) {
        try {
            long start = System.nanoTime();
            Hyphenator h = Hyphenator.load(file);
            LOG.debug("Loaded {} in {} ms", file, (System.nanoTime() - start) / 1_000_000);
            return Optional.of(h);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Cannot read the hyphenation dictionary {}: {}", file, e.toString());
            return Optional.empty();
        }
    }
}
