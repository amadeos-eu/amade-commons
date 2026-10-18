package eu.amadeos.commons.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The language of one application: its own setting in {@code ~/.config/amadeos/<app>/language.conf}
 * ({@code desktop} or a language code such as {@code fr}), the desktop language and the environment,
 * resolved by {@link LocaleResolution#resolve}. {@code AmadeApplication} applies the result once at start-up;
 * a changed setting takes effect at the next start ({@link #restartRequired()}).
 */
public final class LocaleSettings {
    private static final Logger LOG = LoggerFactory.getLogger(LocaleSettings.class);
    public static final String LANGUAGE_FILE = "language.conf";
    /** Value of {@code language.conf} for "use the desktop language". */
    public static final String FOLLOW_DESKTOP = "desktop";

    private final LocaleStore desktop;
    private final Path languageFile;
    private final Map<String, String> environment;
    private final LocaleResolution applied;

    /**
     * @param desktop      the desktop language
     * @param appConfig    the application's configuration directory, e.g. {@code ~/.config/amadeos/audio}
     * @param environment  the process environment
     */
    public LocaleSettings(LocaleStore desktop, Path appConfig, Map<String, String> environment) {
        this.desktop = desktop;
        this.languageFile = appConfig.resolve(LANGUAGE_FILE);
        this.environment = Map.copyOf(environment);
        this.applied = resolve();
    }

    /** The resolution the application started with. */
    public LocaleResolution applied() {
        return applied;
    }

    /** The resolution with the settings as they are now. */
    public LocaleResolution resolve() {
        return LocaleResolution.resolve(applicationLanguage(), desktop.current(), environment);
    }

    /** What the application would use if it followed the desktop; shown next to "Desktop setting". */
    public LocaleResolution desktopResolution() {
        return LocaleResolution.resolve(Optional.empty(), desktop.current(), environment);
    }

    /** The application's own language; empty means it follows the desktop. */
    public Optional<Language> applicationLanguage() {
        try {
            if (!Files.isRegularFile(languageFile)) return Optional.empty();
            String value = Files.readString(languageFile, StandardCharsets.UTF_8).strip();
            if (value.isEmpty() || value.equalsIgnoreCase(FOLLOW_DESKTOP)) return Optional.empty();
            Optional<Language> language = Language.parseLocale(value).flatMap(Language::of);
            if (language.isEmpty()) LOG.warn("Ignoring unknown language {} in {}", value, languageFile);
            return language;
        } catch (IOException e) {
            LOG.debug("Cannot read {}", languageFile, e);
            return Optional.empty();
        }
    }

    /** Stores the application's own language; empty to follow the desktop again. */
    public void setApplicationLanguage(Optional<Language> language) throws IOException {
        Path directory = languageFile.getParent();
        Files.createDirectories(directory);
        Path temp = Files.createTempFile(directory, LANGUAGE_FILE, ".tmp");
        try {
            Files.writeString(temp, language.map(Language::code).orElse(FOLLOW_DESKTOP) + "\n",
                    StandardCharsets.UTF_8);
            try {
                Files.move(temp, languageFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, languageFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** True if the settings now lead to another language than the application started with. */
    public boolean restartRequired() {
        return resolve().language() != applied.language();
    }

    /** The desktop language store, for applications that want to follow changes. */
    public LocaleStore desktop() {
        return desktop;
    }

    /** {@link #applied()}'s locale. */
    public Locale locale() {
        return applied.locale();
    }
}
