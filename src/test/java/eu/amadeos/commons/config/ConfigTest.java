package eu.amadeos.commons.config;

import eu.amadeos.commons.app.AppInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @TempDir
    Path dir;

    // --8<-- [start:xdg]
    @Test
    void xdgDefaultsAndOverrides() {
        XdgDirectories defaults = new XdgDirectories(Map.of(), "/home/ralf");
        assertEquals(Path.of("/home/ralf/.config/amadeos/mail"), defaults.appConfig("mail"));
        assertEquals(Path.of("/home/ralf/.cache/amadeos/mail"), defaults.appCache("mail"));
        assertEquals(Path.of("/home/ralf/.local/share"), defaults.dataHome());

        XdgDirectories custom = new XdgDirectories(Map.of("XDG_CONFIG_HOME", "/cfg", "XDG_CACHE_HOME", "relative"), "/home/ralf");
        assertEquals(Path.of("/cfg/amadeos"), custom.amadeosConfig());
        assertEquals(Path.of("/home/ralf/.cache"), custom.cacheHome(), "relative values are ignored");
    }
    // --8<-- [end:xdg]

    // --8<-- [start:pictures]
    @Test
    void picturesFolderFollowsTheUserDirectories() throws Exception {
        XdgDirectories xdg = new XdgDirectories(Map.of("XDG_CONFIG_HOME", dir.resolve("cfg").toString()), "/home/anna");
        assertEquals(Path.of("/home/anna/Pictures"), xdg.pictures(), "no user-dirs.dirs");

        Files.createDirectories(dir.resolve("cfg"));
        Files.writeString(dir.resolve("cfg/user-dirs.dirs"),
                "# written by xdg-user-dirs-update\nXDG_DESKTOP_DIR=\"$HOME/Schreibtisch\"\nXDG_PICTURES_DIR=\"$HOME/Bilder\"\n");
        assertEquals(Path.of("/home/anna/Bilder"), xdg.pictures());

        Files.writeString(dir.resolve("cfg/user-dirs.dirs"), "XDG_PICTURES_DIR=\"/data/photos\"\n");
        assertEquals(Path.of("/data/photos"), xdg.pictures());
    }

    @Test
    void videosFolderFollowsTheUserDirectories() throws Exception {
        XdgDirectories xdg = new XdgDirectories(Map.of("XDG_CONFIG_HOME", dir.resolve("cfg").toString()), "/home/anna");
        assertEquals(Path.of("/home/anna/Videos"), xdg.videos(), "no user-dirs.dirs");

        Files.createDirectories(dir.resolve("cfg"));
        Files.writeString(dir.resolve("cfg/user-dirs.dirs"),
                "XDG_PICTURES_DIR=\"$HOME/Bilder\"\nXDG_VIDEOS_DIR=\"$HOME/Filme\"\n");
        assertEquals(Path.of("/home/anna/Filme"), xdg.videos());
        assertEquals(Path.of("/home/anna/Bilder"), xdg.pictures());
    }
    // --8<-- [end:pictures]

    @Test
    void appDirectories() {
        AppDirectories dirs = AppDirectories.of(new XdgDirectories(Map.of(), "/h"), "mail");
        assertEquals(Path.of("/h/.config/amadeos/mail/accounts.conf"), dirs.configFile("accounts.conf"));
    }

    @Test
    void appInfoRejectsPathsAsConfigName() {
        assertThrows(IllegalArgumentException.class, () -> new AppInfo("x", "X", "../etc"));
        assertEquals("mail", new AppInfo("amade-mail", "Mailer", "mail").configName());
    }

    // --8<-- [start:properties]
    @Test
    void propertiesFileIsPrivateAndAtomic() throws Exception {
        Path file = dir.resolve("sub/settings.conf");
        Properties p = new Properties();
        p.setProperty("key", "Grüße");
        PropertiesFile.store(file, p, "test");
        assertEquals("Grüße", PropertiesFile.load(file).getProperty("key"));
        if (Files.getFileAttributeView(file, PosixFileAttributeView.class) != null) {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
        }
        try (var files = Files.list(file.getParent())) {
            assertEquals(1L, files.count(), "no temporary files left behind");
        }
        assertTrue(PropertiesFile.load(dir.resolve("missing.conf")).isEmpty());
    }
    // --8<-- [end:properties]
}
