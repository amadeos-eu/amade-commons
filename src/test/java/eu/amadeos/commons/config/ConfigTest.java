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
