package eu.amadeos.commons.theme;

import eu.amadeos.commons.config.ConfigWatcher;
import eu.amadeos.commons.config.XdgDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The desktop theme as chosen in the Style Manager. Reads {@code ~/.config/amadeos/theme.conf} (theme id)
 * and {@code icon_theme.conf} (icon theme) and, once {@link #watch()} was called, follows changes to these
 * files, so that applications switch theme together with the desktop. Applications only read the theme;
 * the desktop writes it.
 */
public final class ThemeStore implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ThemeStore.class);
    public static final String THEME_FILE = "theme.conf";
    public static final String ICON_THEME_FILE = "icon_theme.conf";
    private static final Set<String> WATCHED = Set.of(THEME_FILE, ICON_THEME_FILE);

    private final Path configDirectory;
    private final ThemeRegistry registry;
    private final List<Consumer<DesktopTheme>> listeners = new CopyOnWriteArrayList<>();
    private volatile DesktopTheme current;
    private ConfigWatcher watcher;

    /**
     * @param configDirectory {@code ~/.config/amadeos}
     * @param registry        the themes that can be selected
     */
    public ThemeStore(Path configDirectory, ThemeRegistry registry) {
        this.configDirectory = configDirectory;
        this.registry = registry;
        this.current = read();
    }

    /** The store of the running user, with custom themes from {@code /etc/amadeos/themes} and the user's config. */
    public static ThemeStore system() {
        XdgDirectories xdg = XdgDirectories.system();
        ThemeRegistry registry = ThemeRegistry.withCustomThemes(
                List.of(Path.of("/etc/amadeos/themes"), xdg.amadeosConfig().resolve("themes")));
        return new ThemeStore(xdg.amadeosConfig(), registry);
    }

    public DesktopTheme current() {
        return current;
    }

    public ThemeRegistry registry() {
        return registry;
    }

    /** Called with the new theme on a background thread; Swing code must switch to the EDT itself. */
    public void addListener(Consumer<DesktopTheme> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<DesktopTheme> listener) {
        listeners.remove(listener);
    }

    // --8<-- [start:reload]
    /** Reads the files again and notifies the listeners if the theme changed. */
    public synchronized boolean reload() {
        DesktopTheme theme = read();
        if (theme.equals(current)) return false;
        current = theme;
        LOG.info("Desktop theme changed to {} (icons: {})", theme.id(), theme.iconTheme());
        for (Consumer<DesktopTheme> listener : listeners) {
            try {
                listener.accept(theme);
            } catch (RuntimeException e) {
                LOG.warn("Theme listener failed", e);
            }
        }
        return true;
    }
    // --8<-- [end:reload]

    // --8<-- [start:read]
    private DesktopTheme read() {
        DesktopTheme theme = readLine(THEME_FILE).flatMap(registry::find).orElse(DesktopTheme.WARM_MOTIF);
        String icons = readLine(ICON_THEME_FILE).orElse(null);
        return icons == null ? theme : theme.withIconTheme(icons);
    }
    // --8<-- [end:read]

    private Optional<String> readLine(String fileName) {
        Path file = configDirectory.resolve(fileName);
        try {
            if (!Files.isRegularFile(file)) return Optional.empty();
            String value = Files.readString(file, StandardCharsets.UTF_8).strip();
            return value.isEmpty() ? Optional.empty() : Optional.of(value);
        } catch (IOException e) {
            LOG.debug("Cannot read {}", file, e);
            return Optional.empty();
        }
    }

    /** Starts following the theme files in a daemon thread. Calling it again has no effect. */
    public synchronized void watch() {
        if (watcher == null) {
            watcher = new ConfigWatcher(configDirectory, WATCHED, "amade-theme-watcher", this::reload);
        }
        watcher.start();
    }

    @Override
    public synchronized void close() {
        if (watcher != null) watcher.close();
        watcher = null;
    }
}
