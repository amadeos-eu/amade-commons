package eu.amadeos.commons.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegionalFormatsTest {
    @TempDir
    Path dir;

    @Test
    void withoutFileTheFormatsFollowTheLanguage() {
        RegionalFormats formats = new RegionalFormats(dir);
        assertEquals(Optional.empty(), formats.region());
        assertEquals(Locale.of("fr", "FR"), formats.formatLocale(Locale.of("fr", "FR")));
    }

    @Test
    void writesAndReadsTheRegion() throws Exception {
        RegionalFormats formats = new RegionalFormats(dir);
        formats.write(Optional.of(Locale.of("de", "AT")));
        assertEquals("de_AT.UTF-8\n", Files.readString(dir.resolve("formats.conf"), StandardCharsets.UTF_8));
        assertEquals(Optional.of(Locale.of("de", "AT")), formats.region());
        assertEquals(Locale.of("de", "AT"), formats.formatLocale(Locale.of("en", "US")));
        formats.write(Optional.empty());
        assertFalse(Files.exists(dir.resolve("formats.conf")));
    }

    @Test
    void parsesPosixAndTags() {
        assertEquals(Optional.of(Locale.of("en", "GB")), RegionalFormats.parse("en_GB.utf8"));
        assertEquals(Optional.of(Locale.of("fr", "CH")), RegionalFormats.parse("fr-CH"));
        assertEquals(Optional.of(Locale.of("de", "DE")), RegionalFormats.parse("de_DE@euro"));
        assertEquals(Optional.empty(), RegionalFormats.parse("C.UTF-8"));
        assertEquals(Optional.empty(), RegionalFormats.parse("rubbish"));
    }

    @Test
    void appliesTheRegionToTheEnvironmentOfOtherPrograms() throws Exception {
        RegionalFormats formats = new RegionalFormats(dir);
        Map<String, String> env = new HashMap<>(Map.of("LANG", "en_US.UTF-8", "LC_TIME", "C"));
        formats.applyTo(env);
        assertFalse(env.containsKey("LC_TIME"), "without a region the language rules");
        formats.write(Optional.of(Locale.of("de", "CH")));
        formats.applyTo(env);
        assertEquals("en_US.UTF-8", env.get("LANG"));
        for (String category : RegionalFormats.CATEGORIES) assertEquals("de_CH.UTF-8", env.get(category));
    }

    @Test
    void namesRegionsInTheirOwnLanguage() {
        assertEquals("Deutsch (Österreich)", RegionalFormats.nativeName(Locale.of("de", "AT")));
        assertEquals("English (United Kingdom)", RegionalFormats.nativeName(Locale.of("en", "GB")));
        assertEquals(RegionalFormats.REGIONS.size(), RegionalFormats.REGIONS.stream().distinct().count());
    }
}
