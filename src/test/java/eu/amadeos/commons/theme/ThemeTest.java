package eu.amadeos.commons.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ThemeTest {
    @TempDir
    Path dir;

    private static double contrast(Color a, Color b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color c) {
        double[] rgb = {c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0};
        for (int i = 0; i < 3; i++) {
            rgb[i] = rgb[i] <= 0.03928 ? rgb[i] / 12.92 : Math.pow((rgb[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
    }

    @Test
    void everyBuiltInThemeHasReadableFieldsAndSelection() {
        for (DesktopTheme theme : ThemeRegistry.BUILT_IN) {
            assertTrue(contrast(theme.fieldBackground(), theme.fieldText()) >= 7,
                    theme.id() + " field text contrast " + contrast(theme.fieldBackground(), theme.fieldText()));
            assertTrue(contrast(theme.selectionBackground(), theme.selectionText()) >= 3,
                    theme.id() + " selection contrast");
        }
    }

    @Test
    void everyBuiltInThemeColoursFourWorkspacesDifferently() {
        for (DesktopTheme theme : ThemeRegistry.BUILT_IN) {
            assertEquals(DesktopTheme.WORKSPACE_COLORS, theme.workspaceColors().size(), theme.id());
            assertEquals(DesktopTheme.WORKSPACE_COLORS, Set.copyOf(theme.workspaceColors()).size(),
                    theme.id() + " workspace colours " + theme.workspaceColors());
            assertEquals(theme.workspaceColor(0), theme.workspaceColor(4), "workspace Five repeats One");
        }
        // CDE's default palette: One blue-grey, Two the dialog colour, Three teal, Four rose
        assertEquals(new Color(0x49, 0x92, 0xA7), DesktopTheme.WARM_MOTIF.workspaceColor(2));
        assertEquals(DesktopTheme.WARM_MOTIF.base(), DesktopTheme.WARM_MOTIF.workspaceColor(1));
    }

    @Test
    void darkSlateDoesNotPutLightTextOnWhite() {
        DesktopTheme dark = DesktopTheme.DARK_SLATE;
        assertTrue(dark.isDark());
        assertNotEquals(Color.WHITE, dark.fieldBackground());
        assertEquals(Color.WHITE, DesktopTheme.WARM_MOTIF.fieldBackground());
    }

    // --8<-- [start:custom-theme]
    @Test
    void registryLoadsCustomThemesAndDerivesMissingColours() throws Exception {
        Path midnight = Files.createDirectories(dir.resolve("midnight"));
        Files.writeString(midnight.resolve("theme.properties"), "name=Midnight\nbase=#2B3A55\nworkspace3=#C08040\n");
        Path broken = Files.createDirectories(dir.resolve("broken"));
        Files.writeString(broken.resolve("theme.properties"), "base=not-a-colour\n");

        ThemeRegistry registry = ThemeRegistry.withCustomThemes(List.of(dir, dir.resolve("missing")));
        DesktopTheme theme = registry.find("midnight").orElseThrow();
        assertEquals("Midnight", theme.name());
        assertEquals(new Color(0x2B, 0x3A, 0x55), theme.base());
        assertNotNull(theme.shadow());
        assertEquals(DesktopTheme.DEFAULT_ICON_THEME, theme.iconTheme());
        assertEquals(new Color(0xC0, 0x80, 0x40), theme.workspaceColor(2), "given workspace colour");
        assertEquals(theme.base(), theme.workspaceColor(1), "derived workspace colour");
        assertEquals(theme.workspaceColors(), theme.withIconTheme("Other").workspaceColors());
        assertTrue(registry.find("broken").isEmpty());
        assertEquals(6, registry.all().size());
    }
    // --8<-- [end:custom-theme]

    @Test
    void storeReadsThemeAndIconThemeAndFallsBack() throws Exception {
        ThemeStore store = new ThemeStore(dir, new ThemeRegistry());
        assertEquals(DesktopTheme.WARM_MOTIF, store.current());
        assertEquals("NsCDE", store.current().iconTheme(), "NsCDE is the default icon theme");

        Files.writeString(dir.resolve(ThemeStore.THEME_FILE), "cde-brocade\n");
        Files.writeString(dir.resolve(ThemeStore.ICON_THEME_FILE), "NsCDE");
        List<DesktopTheme> seen = new ArrayList<>();
        store.addListener(seen::add);
        assertTrue(store.reload());
        assertEquals("cde-brocade", store.current().id());
        assertEquals("NsCDE", store.current().iconTheme());
        assertEquals(1, seen.size());
        assertFalse(store.reload(), "unchanged files are no change");

        Files.writeString(dir.resolve(ThemeStore.THEME_FILE), "does-not-exist");
        Files.delete(dir.resolve(ThemeStore.ICON_THEME_FILE));
        assertTrue(store.reload());
        assertEquals(DesktopTheme.WARM_MOTIF, store.current());
    }

    // --8<-- [start:follow]
    @Test
    void storeFollowsFileChanges() throws Exception {
        try (ThemeStore store = new ThemeStore(dir, new ThemeRegistry())) {
            List<DesktopTheme> seen = new java.util.concurrent.CopyOnWriteArrayList<>();
            store.addListener(seen::add);
            store.watch();
            Files.writeString(dir.resolve(ThemeStore.THEME_FILE), "dark-slate");
            long deadline = System.currentTimeMillis() + 15_000;
            while (seen.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(50);
            assertEquals("dark-slate", store.current().id());
            assertEquals(1, seen.size());
        }
    }
    // --8<-- [end:follow]
}
