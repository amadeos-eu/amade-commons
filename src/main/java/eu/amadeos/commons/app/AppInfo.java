package eu.amadeos.commons.app;

import java.util.Objects;

/**
 * Identity of an amadeOS application.
 *
 * @param id         technical name, also the Debian package and keyring service, e.g. {@code amade-mail}
 * @param title      window title in the CDE style, e.g. {@code Mailer}
 * @param configName directory below {@code ~/.config/amadeos/}, e.g. {@code mail}
 */
public record AppInfo(String id, String title, String configName) {
    public AppInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(configName, "configName");
        if (!configName.matches("[a-z0-9][a-z0-9._-]*")) {
            throw new IllegalArgumentException("Invalid config directory name: " + configName);
        }
    }
}
