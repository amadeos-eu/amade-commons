package org.datazuul.amadeos.commons.theme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * The available themes: the five built-in ones plus custom themes from
 * {@code /etc/amadeos/themes/<id>/theme.properties} and {@code ~/.config/amadeos/themes/<id>/theme.properties}.
 *
 * <pre>
 * name=Midnight
 * base=#2B3A55
 * activeTitleBg=#C08040
 * iconTheme=NsCDE
 * </pre>
 *
 * Only {@code base} is required; every other colour is derived from it when missing, and the icon theme
 * defaults to {@link DesktopTheme#DEFAULT_ICON_THEME}.
 */
public final class ThemeRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(ThemeRegistry.class);

    public static final List<DesktopTheme> BUILT_IN = List.of(
            DesktopTheme.WARM_MOTIF, DesktopTheme.CDE_BROCADE, DesktopTheme.CDE_DESERT,
            DesktopTheme.CLASSIC_GRAY, DesktopTheme.DARK_SLATE);

    private final Map<String, DesktopTheme> themes = new LinkedHashMap<>();

    public ThemeRegistry() {
        BUILT_IN.forEach(this::register);
    }

    /** Built-in themes plus the custom themes found in the given directories (later ones win). */
    public static ThemeRegistry withCustomThemes(List<Path> directories) {
        ThemeRegistry registry = new ThemeRegistry();
        for (Path directory : directories) registry.discover(directory);
        return registry;
    }

    public synchronized void register(DesktopTheme theme) {
        themes.put(theme.id(), theme);
    }

    public synchronized List<DesktopTheme> all() {
        return List.copyOf(themes.values());
    }

    public synchronized Optional<DesktopTheme> find(String id) {
        return Optional.ofNullable(id == null ? null : themes.get(id.strip()));
    }

    /** Registers every {@code <id>/theme.properties} below {@code directory}. */
    public void discover(Path directory) {
        if (!Files.isDirectory(directory)) return;
        List<Path> candidates = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, Files::isDirectory)) {
            stream.forEach(candidates::add);
        } catch (IOException e) {
            LOG.warn("Cannot list themes in {}", directory, e);
            return;
        }
        candidates.sort(null);
        for (Path themeDirectory : candidates) {
            Path file = themeDirectory.resolve("theme.properties");
            if (!Files.isRegularFile(file)) continue;
            try {
                register(load(themeDirectory.getFileName().toString(), file));
            } catch (IOException | RuntimeException e) {
                LOG.warn("Ignoring invalid theme {}: {}", file, e.getMessage());
            }
        }
    }

    static DesktopTheme load(String id, Path file) throws IOException {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
        }
        return new DesktopTheme(id, p.getProperty("name", id),
                Color.decode(p.getProperty("base", "#CEC2BB").strip()),
                color(p, "highlight"), color(p, "softHighlight"), color(p, "shadow"), color(p, "darkShadow"),
                color(p, "sunkenBg"), color(p, "activeTitleBg"), color(p, "inactiveTitleBg"),
                color(p, "textActive"), color(p, "textInactive"), p.getProperty("iconTheme", DesktopTheme.DEFAULT_ICON_THEME).strip(),
                color(p, "panelFrameBg"), color(p, "panelFrameHighlight"), color(p, "panelFrameShadow"));
    }

    private static Color color(Properties p, String key) {
        String value = p.getProperty(key);
        return value == null || value.isBlank() ? null : Color.decode(value.strip());
    }
}
