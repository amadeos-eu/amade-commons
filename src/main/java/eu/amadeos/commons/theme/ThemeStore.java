package eu.amadeos.commons.theme;

import eu.amadeos.commons.config.ConfigWatcher;
import eu.amadeos.commons.config.XdgDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The desktop theme as chosen in the desktop's Settings. Reads {@code ~/.config/amadeos/look.conf} (the look,
 * classic or Horizon, and Horizon's scheme and accent), {@code theme.conf} (the classic palette's id),
 * {@code icon_theme.conf} (icon theme) and {@code font.conf} (font size 1 to 7) and, once {@link #watch()} was called, follows changes to these
 * files, so that applications switch theme together with the desktop. Applications only read the theme;
 * the desktop writes it.
 */
public final class ThemeStore implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ThemeStore.class);
    public static final String THEME_FILE = "theme.conf";
    public static final String ICON_THEME_FILE = "icon_theme.conf";
    public static final String FONT_FILE = "font.conf";
    /** The look and Horizon's colours: {@code look=horizon}, {@code scheme=dark}, {@code accent=teal}. */
    public static final String LOOK_FILE = "look.conf";
    private static final Set<String> WATCHED = Set.of(THEME_FILE, ICON_THEME_FILE, FONT_FILE, LOOK_FILE);

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
        LOG.info("Desktop theme changed to {} (icons: {}, font size {})", theme.id(), theme.iconTheme(),
                theme.fontSize());
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
        Properties look = readLook(configDirectory);
        DesktopTheme theme = DesktopLook.of(look.getProperty("look")) == DesktopLook.HORIZON
                ? HorizonThemes.find(HorizonThemes.id(look)).orElseThrow()
                : readLine(THEME_FILE).flatMap(registry::find).filter(t -> t.look() == DesktopLook.CLASSIC)
                        .orElse(DesktopTheme.WARM_MOTIF);
        theme = theme.withIconTheme(iconTheme(configDirectory, theme));
        return theme.withFontSize(readLine(FONT_FILE).map(ThemeStore::fontSize).orElse(DesktopTheme.DEFAULT_FONT_SIZE));
    }
    // --8<-- [end:read]

    /**
     * The contents of {@code look.conf} in a configuration directory ({@code look}, {@code scheme},
     * {@code accent}, {@code palette}); empty if there is no such file, which means the classic look.
     */
    public static Properties readLook(Path configDirectory) {
        Properties p = new Properties();
        Path file = configDirectory.resolve(LOOK_FILE);
        if (!Files.isRegularFile(file)) return p;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            LOG.debug("Cannot read {}", file, e);
        }
        return p;
    }

    // --8<-- [start:icons]
    /**
     * The icon theme that goes with a theme: each look keeps its own choice. The classic look takes
     * {@code icon_theme.conf}, Horizon the {@code icons} line of {@code look.conf}; without a choice, the
     * theme's own icons (NsCDE for the classic palettes, amadeos-horizon for Horizon).
     */
    public static String iconTheme(Path configDirectory, DesktopTheme theme) {
        String chosen = theme.look() == DesktopLook.HORIZON
                ? readLook(configDirectory).getProperty("icons", "").strip()
                : readFirstLine(configDirectory.resolve(ICON_THEME_FILE));
        return chosen == null || chosen.isEmpty() ? theme.iconTheme() : chosen;
    }
    // --8<-- [end:icons]

    private static String readFirstLine(Path file) {
        try {
            return Files.exists(file) ? Files.readAllLines(file).stream().map(String::strip)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#")).findFirst().orElse(null) : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * The text of {@code look.conf} for a theme: its look and, for a Horizon theme, its scheme, accent and
     * palette; the icon theme chosen for Horizon ({@code icons}) is kept.
     */
    public static String lookFile(DesktopTheme theme, Properties previous) {
        String scheme = previous.getProperty("scheme", HorizonThemes.DEFAULT_SCHEME.id());
        String accent = previous.getProperty("accent", HorizonThemes.DEFAULT_ACCENT.id());
        String palette = previous.getProperty("palette", "").strip();
        if (theme.look() == DesktopLook.HORIZON) {
            scheme = HorizonThemes.scheme(theme.id()).id();
            accent = HorizonThemes.accent(theme.id()).id();
            palette = HorizonThemes.palette(theme.id()).id();
        }
        String icons = previous.getProperty("icons", "").strip();
        return "# The desktop look (Settings > Appearance): classic or horizon, and Horizon's colours\n"
                + "look=" + theme.look().id() + "\nscheme=" + scheme + "\naccent=" + accent + "\n"
                + (palette.isEmpty() ? "" : "palette=" + palette + "\n")
                + (icons.isEmpty() ? "" : "icons=" + icons + "\n");
    }

    private static int fontSize(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOG.debug("Not a font size: {}", value);
            return DesktopTheme.DEFAULT_FONT_SIZE;
        }
    }

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
