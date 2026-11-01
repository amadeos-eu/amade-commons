package eu.amadeos.commons.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class HorizonThemesTest {
    @TempDir
    Path dir;

    @Test
    void idsNameSchemeAndAccent() {
        assertEquals("horizon-dark-teal", HorizonThemes.id(HorizonThemes.Scheme.DARK, HorizonThemes.Accent.TEAL));
        assertTrue(HorizonThemes.isHorizon("horizon-light-amber"));
        assertFalse(HorizonThemes.isHorizon("warm-motif"));
        assertEquals(HorizonThemes.Scheme.LIGHT, HorizonThemes.scheme("horizon-light-amber"));
        assertEquals(HorizonThemes.Accent.AMBER, HorizonThemes.accent("horizon-light-amber"));
        assertEquals(HorizonThemes.Accent.TEAL, HorizonThemes.accent("warm-motif"));
        assertTrue(HorizonThemes.find("horizon-dark").isEmpty());
    }

    @Test
    void everyCombinationIsAHorizonThemeWithItsAccentAndReadableText() {
        for (HorizonThemes.Scheme scheme : HorizonThemes.Scheme.values()) {
            for (HorizonThemes.Accent accent : HorizonThemes.Accent.values()) {
                DesktopTheme t = HorizonThemes.theme(scheme, accent);
                assertEquals(DesktopLook.HORIZON, t.look());
                assertEquals(accent.color(), t.activeTitleBg());
                assertEquals(scheme == HorizonThemes.Scheme.DARK, DesktopTheme.isDark(t.base()));
                assertNotEquals(DesktopTheme.isDark(t.base()), DesktopTheme.isDark(t.textActive()));
                assertEquals(t, HorizonThemes.find(t.id()).orElseThrow());
            }
        }
    }

    @Test
    void registryFindsHorizonIdsButListsOnlyTheClassicPalettes() {
        ThemeRegistry registry = new ThemeRegistry();
        assertEquals(DesktopLook.HORIZON, registry.find("horizon-dark-violet").orElseThrow().look());
        assertTrue(registry.all().stream().allMatch(t -> t.look() == DesktopLook.CLASSIC));
        assertEquals(DesktopLook.CLASSIC, DesktopTheme.WARM_MOTIF.look());
    }

    @Test
    void storeReadsTheLookFileAndKeepsTheClassicPaletteApart() throws Exception {
        Files.writeString(dir.resolve(ThemeStore.THEME_FILE), "cde-brocade");
        assertEquals("cde-brocade", new ThemeStore(dir, new ThemeRegistry()).current().id());

        Files.writeString(dir.resolve(ThemeStore.LOOK_FILE), "look=horizon\nscheme=light\naccent=green\n");
        ThemeStore store = new ThemeStore(dir, new ThemeRegistry());
        assertEquals("horizon-light-green", store.current().id());

        Files.writeString(dir.resolve(ThemeStore.LOOK_FILE), "look=classic\nscheme=light\naccent=green\n");
        assertTrue(store.reload());
        assertEquals("cde-brocade", store.current().id());
    }

    @Test
    void aHorizonIdInTheThemeFileIsNotTakenForAClassicPalette() throws Exception {
        Files.writeString(dir.resolve(ThemeStore.THEME_FILE), "horizon-dark-teal");
        assertEquals(DesktopTheme.WARM_MOTIF.id(), new ThemeStore(dir, new ThemeRegistry()).current().id());
    }

    @Test
    void lookFileKeepsHorizonColoursWhileClassic() {
        Properties previous = new Properties();
        previous.setProperty("scheme", "light");
        previous.setProperty("accent", "red");
        String classic = ThemeStore.lookFile(DesktopTheme.WARM_MOTIF, previous);
        assertTrue(classic.contains("look=classic"));
        assertTrue(classic.contains("scheme=light"));
        assertTrue(classic.contains("accent=red"));
        String horizon = ThemeStore.lookFile(HorizonThemes.theme(HorizonThemes.Scheme.DARK, HorizonThemes.Accent.BLUE), previous);
        assertTrue(horizon.contains("look=horizon"));
        assertTrue(horizon.contains("scheme=dark"));
        assertTrue(horizon.contains("accent=blue"));
    }

    @Test
    void unknownLookIsClassic() {
        assertEquals(DesktopLook.CLASSIC, DesktopLook.of("fancy"));
        assertEquals(DesktopLook.HORIZON, DesktopLook.of(" Horizon "));
        assertEquals(List.of(new Color(0x2F, 0xB8, 0xB0)).getFirst(), HorizonThemes.Accent.of("nope").color());
    }
}
