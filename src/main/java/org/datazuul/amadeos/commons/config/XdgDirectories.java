package org.datazuul.amadeos.commons.config;

import java.nio.file.Path;
import java.util.Map;

/**
 * Per-user directories following the XDG Base Directory specification. amadeOS keeps its settings in
 * {@code $XDG_CONFIG_HOME/amadeos}, applications in a sub-directory of it.
 */
public final class XdgDirectories {
    private final Map<String, String> environment;
    private final String userHome;

    public XdgDirectories(Map<String, String> environment, String userHome) {
        this.environment = Map.copyOf(environment);
        this.userHome = userHome;
    }

    /** The directories of the running user. */
    public static XdgDirectories system() {
        return new XdgDirectories(System.getenv(), System.getProperty("user.home", "/tmp"));
    }

    /** {@code $XDG_CONFIG_HOME}, by default {@code ~/.config}. */
    public Path configHome() {
        return base("XDG_CONFIG_HOME", ".config");
    }

    /** {@code $XDG_DATA_HOME}, by default {@code ~/.local/share}. */
    public Path dataHome() {
        return base("XDG_DATA_HOME", ".local/share");
    }

    /** {@code $XDG_CACHE_HOME}, by default {@code ~/.cache}. */
    public Path cacheHome() {
        return base("XDG_CACHE_HOME", ".cache");
    }

    /** {@code ~/.config/amadeos}: desktop settings such as {@code theme.conf}. */
    public Path amadeosConfig() {
        return configHome().resolve("amadeos");
    }

    /** {@code ~/.config/amadeos/<name>}: settings of one application. */
    public Path appConfig(String name) {
        return amadeosConfig().resolve(name);
    }

    /** {@code ~/.cache/amadeos/<name>}: data an application can rebuild. */
    public Path appCache(String name) {
        return cacheHome().resolve("amadeos").resolve(name);
    }

    /**
     * The variable if it holds an absolute path; the specification says relative values must be ignored.
     */
    private Path base(String variable, String fallback) {
        String configured = environment.get(variable);
        if (configured != null && configured.startsWith("/")) return Path.of(configured);
        return Path.of(userHome).resolve(fallback);
    }
}
