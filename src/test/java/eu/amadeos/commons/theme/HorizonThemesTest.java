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

    // --8<-- [start:icons]
    @Test
    void eachLookKeepsItsOwnIconTheme() throws Exception {
        ThemeStore store = new ThemeStore(dir, new ThemeRegistry());
        Files.writeString(dir.resolve(ThemeStore.ICON_THEME_FILE), "Papirus");
        store.reload();
        assertEquals("Papirus", store.current().iconTheme(), "the classic look takes icon_theme.conf");

        Files.writeString(dir.resolve(ThemeStore.LOOK_FILE), "look=horizon\nscheme=dark\naccent=teal\n");
        store.reload();
        assertEquals(HorizonThemes.ICON_THEME, store.current().iconTheme(), "Horizon has its own icons");

        Properties previous = ThemeStore.readLook(dir);
        previous.setProperty("icons", "NsCDE");
        String text = ThemeStore.lookFile(HorizonThemes.theme(HorizonThemes.Scheme.LIGHT, HorizonThemes.Accent.RED),
                previous);
        assertTrue(text.contains("icons=NsCDE"), text);
        Files.writeString(dir.resolve(ThemeStore.LOOK_FILE), text);
        store.reload();
        assertEquals("NsCDE", store.current().iconTheme(), "a choice made in Horizon");
        assertEquals("light", ThemeStore.readLook(dir).getProperty("scheme"));
    }
    // --8<-- [end:icons]

    @Test
    void palettesAddTheirIdOnlyWhenTheyAreNotTheDefault() {
        HorizonThemes.Scheme light = HorizonThemes.Scheme.LIGHT;
        assertEquals("horizon-light-teal",
                HorizonThemes.id(light, HorizonThemes.Accent.TEAL, HorizonThemes.Palette.PAPER));
        assertEquals("horizon-light-amber-mist",
                HorizonThemes.id(light, HorizonThemes.Accent.AMBER, HorizonThemes.Palette.MIST));
        // A palette of the other scheme falls back to the scheme's default
        assertEquals("horizon-light-teal",
                HorizonThemes.id(light, HorizonThemes.Accent.TEAL, HorizonThemes.Palette.OCEAN));
        assertEquals(HorizonThemes.Palette.MIST, HorizonThemes.palette("horizon-light-amber-mist"));
        assertEquals(HorizonThemes.Accent.AMBER, HorizonThemes.accent("horizon-light-amber-mist"));
        assertEquals(light, HorizonThemes.scheme("horizon-light-amber-mist"));
        assertEquals(HorizonThemes.Palette.PAPER, HorizonThemes.palette("horizon-light-amber"));
        assertEquals(HorizonThemes.Palette.NIGHT, HorizonThemes.palette("horizon-dark-amber-unknown"));
        assertEquals(HorizonThemes.Palette.PAPER, HorizonThemes.Palette.of(light).get(0));
        assertEquals(HorizonThemes.Palette.NIGHT, HorizonThemes.Palette.of(HorizonThemes.Scheme.DARK).get(0));
        assertEquals("Horizon Light (Sand)", HorizonThemes.theme(light, HorizonThemes.Accent.RED,
                HorizonThemes.Palette.SAND).name());
    }

    @Test
    void everyPaletteKeepsItsSchemeAndReadableText() {
        for (HorizonThemes.Palette p : HorizonThemes.Palette.values()) {
            for (HorizonThemes.Accent accent : HorizonThemes.Accent.values()) {
                DesktopTheme t = HorizonThemes.theme(p.scheme(), accent, p);
                assertEquals(p.scheme() == HorizonThemes.Scheme.DARK, t.isDark(), p.id());
                assertEquals(t, HorizonThemes.find(t.id()).orElseThrow(), t.id());
                assertTrue(contrast(t.textActive(), t.base()) >= 7, p.id() + " text");
                assertTrue(contrast(t.textActive(), t.sunkenBg()) >= 7, p.id() + " field text");
                assertTrue(contrast(t.textInactive(), t.base()) >= 3, p.id() + " inactive text");
            }
        }
        // The light palettes other than Paper are less bright than Paper
        for (HorizonThemes.Palette p : HorizonThemes.Palette.of(HorizonThemes.Scheme.LIGHT)) {
            if (p.isStandard()) continue;
            assertTrue(luminance(p.base()) < luminance(HorizonThemes.Palette.PAPER.base()), p.id());
            assertTrue(luminance(p.fields()) < luminance(HorizonThemes.Palette.PAPER.fields()), p.id());
        }
    }

    @Test
    void storeReadsAndWritesThePalette() throws Exception {
        Files.writeString(dir.resolve(ThemeStore.LOOK_FILE), "look=horizon\nscheme=light\naccent=green\npalette=sage\n");
        assertEquals("horizon-light-green-sage", new ThemeStore(dir, new ThemeRegistry()).current().id());
        String text = ThemeStore.lookFile(HorizonThemes.theme(HorizonThemes.Scheme.LIGHT, HorizonThemes.Accent.RED,
                HorizonThemes.Palette.STONE), new Properties());
        assertTrue(text.contains("palette=stone"), text);
        Properties previous = new Properties();
        previous.setProperty("palette", "stone");
        assertTrue(ThemeStore.lookFile(DesktopTheme.WARM_MOTIF, previous).contains("palette=stone"));
    }

    private static double contrast(Color a, Color b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color c) {
        double[] v = {c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0};
        for (int i = 0; i < 3; i++) v[i] = v[i] <= 0.03928 ? v[i] / 12.92 : Math.pow((v[i] + 0.055) / 1.055, 2.4);
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
    }
}
