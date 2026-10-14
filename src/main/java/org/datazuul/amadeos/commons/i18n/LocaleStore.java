package org.datazuul.amadeos.commons.i18n;

import org.datazuul.amadeos.commons.config.ConfigWatcher;
import org.datazuul.amadeos.commons.config.XdgDirectories;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The language of the amadeOS desktop. The desktop keeps it in {@code ~/.config/amadeos/locale.conf}, one
 * line with a POSIX locale such as {@code de_DE.UTF-8} (a bare {@code de} works too); the login greeter
 * exports it as {@code LANG} for the session. Applications read it at start-up and, once {@link #watch()}
 * was called, hear about changes.
 */
public final class LocaleStore implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(LocaleStore.class);
    public static final String LOCALE_FILE = "locale.conf";

    private final Path configDirectory;
    private final List<Consumer<Optional<Locale>>> listeners = new CopyOnWriteArrayList<>();
    private volatile Optional<Locale> current;
    private ConfigWatcher watcher;

    /** @param configDirectory {@code ~/.config/amadeos} */
    public LocaleStore(Path configDirectory) {
        this.configDirectory = configDirectory;
        this.current = read();
    }

    /** The store of the running user. */
    public static LocaleStore system() {
        return new LocaleStore(XdgDirectories.system().amadeosConfig());
    }

    /** The desktop locale; empty if none is set or amadeOS is not translated into its language. */
    public Optional<Locale> current() {
        return current;
    }

    /** Language of {@link #current()}. */
    public Optional<Language> language() {
        return current.flatMap(Language::of);
    }

    public Path file() {
        return configDirectory.resolve(LOCALE_FILE);
    }

    /** Called with the new desktop locale on a background thread. */
    public void addListener(Consumer<Optional<Locale>> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<Optional<Locale>> listener) {
        listeners.remove(listener);
    }

    /**
     * Sets the desktop language; for the desktop's own language switcher, applications do not call it.
     * Writes the usual POSIX locale of the language, so that the next login exports it as {@code LANG}.
     */
    public void write(Language language) throws IOException {
        Objects.requireNonNull(language, "language");
        Files.createDirectories(configDirectory);
        Path target = file();
        Path temp = Files.createTempFile(configDirectory, LOCALE_FILE, ".tmp");
        try {
            Files.writeString(temp, language.posixLocale() + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
        reload();
    }

    /** Reads the file again and notifies the listeners if the locale changed. */
    public synchronized boolean reload() {
        Optional<Locale> locale = read();
        if (locale.equals(current)) return false;
        current = locale;
        LOG.info("Desktop locale changed to {}", locale.map(Locale::toLanguageTag).orElse("(none)"));
        for (Consumer<Optional<Locale>> listener : listeners) {
            try {
                listener.accept(locale);
            } catch (RuntimeException e) {
                LOG.warn("Locale listener failed", e);
            }
        }
        return true;
    }

    private Optional<Locale> read() {
        Path file = file();
        try {
            if (!Files.isRegularFile(file)) return Optional.empty();
            String value = Files.readString(file, StandardCharsets.UTF_8).strip();
            Optional<Locale> locale = Language.parseLocale(value);
            if (locale.isEmpty() && !value.isEmpty()) LOG.debug("Ignoring unsupported desktop locale {}", value);
            return locale;
        } catch (IOException e) {
            LOG.debug("Cannot read {}", file, e);
            return Optional.empty();
        }
    }

    /** Starts following {@code locale.conf} in a daemon thread. Calling it again has no effect. */
    public synchronized void watch() {
        if (watcher == null) {
            watcher = new ConfigWatcher(configDirectory, Set.of(LOCALE_FILE), "amade-locale-watcher", this::reload);
        }
        watcher.start();
    }

    @Override
    public synchronized void close() {
        if (watcher != null) watcher.close();
        watcher = null;
    }
}
