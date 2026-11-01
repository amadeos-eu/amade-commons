package eu.amadeos.commons.widget;

import eu.amadeos.commons.theme.DesktopTheme;
import eu.amadeos.commons.theme.ThemeRegistry;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WidgetTest {
    @Test
    void cardColoursFollowTheTheme() {
        for (DesktopTheme theme : new ThemeRegistry().all()) {
            WidgetColors c = WidgetColors.of(theme);
            assertEquals(theme.panelFrameBg(), c.card(), theme.id());
            assertEquals(theme.activeTitleBg(), c.accent(), theme.id());
            assertNotEquals(DesktopTheme.isDark(c.card()), DesktopTheme.isDark(c.text()), theme.id() + " text is readable");
        }
        assertEquals(new Color(128, 128, 128), WidgetColors.mix(Color.BLACK, Color.WHITE, 0.5));
    }

    @Test
    void thePreviewKeepsSettingsAndRunsATimerAtOnce() {
        WidgetPreview.Context context = new WidgetPreview.Context(() -> null);
        assertEquals("analog", context.get("style", "analog"));
        context.set("style", "digital");
        assertEquals("digital", context.get("style", "analog"));
        AtomicInteger runs = new AtomicInteger();
        context.every(Duration.ofHours(1), runs::incrementAndGet);
        assertEquals(1, runs.get(), "the first run is right away");
        context.timers.forEach(javax.swing.Timer::stop);
    }

    @Test
    void theCardShapeIsRounded() {
        var shape = WidgetCard.shape(200, 100);
        assertEquals(WidgetCard.ARC, shape.getArcWidth());
        assertFalse(shape.contains(0.5, 0.5), "a corner shows the desktop");
        assertTrue(shape.contains(100, 50));
    }
}
