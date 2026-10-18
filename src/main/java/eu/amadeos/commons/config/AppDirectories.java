package eu.amadeos.commons.config;

import java.nio.file.Path;

/**
 * Where an application keeps its files.
 *
 * @param config settings, e.g. {@code ~/.config/amadeos/mail}
 * @param cache  data that can be rebuilt, e.g. {@code ~/.cache/amadeos/mail}
 */
public record AppDirectories(Path config, Path cache) {

    public static AppDirectories of(XdgDirectories xdg, String name) {
        return new AppDirectories(xdg.appConfig(name), xdg.appCache(name));
    }

    /** A file in the configuration directory. */
    public Path configFile(String fileName) {
        return config.resolve(fileName);
    }
}
