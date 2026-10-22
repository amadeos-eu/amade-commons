package eu.amadeos.commons.swing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.amadeos.commons.swing.ToolIconTest.count;
import static eu.amadeos.commons.swing.ToolIconTest.paint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportButtonsTest {
    private final Locale before = Locale.getDefault();

    @AfterEach
    void restore() {
        Locale.setDefault(before);
    }

    @Test
    void everyTransportGlyphDrawsSomething() {
        JButton button = new JButton();
        button.setForeground(Color.BLACK);
        for (TransportGlyph glyph : TransportGlyph.values()) {
            BufferedImage image = paint(new ToolIcon(glyph, 16), button);
            int drawn = count(image, rgb -> (rgb & 0xFFFFFF) != 0xC0C0C0);
            assertTrue(drawn >= 40, glyph + " draws only " + drawn + " pixels");
        }
    }

    // --8<-- [start:mirror]
    @Test
    void nextIsTheMirrorImageOfPrevious() {
        JButton button = new JButton();
        button.setForeground(Color.BLACK);
        BufferedImage previous = paint(new ToolIcon(TransportGlyph.PREVIOUS, 16), button);
        BufferedImage next = paint(new ToolIcon(TransportGlyph.NEXT, 16), button);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                assertEquals(previous.getRGB(x, y), next.getRGB(15 - x, y), "pixel " + x + "," + y);
            }
        }
    }
    // --8<-- [end:mirror]

    @Test
    void recordIsRedOnlyWhileTheButtonIsEnabled() {
        JButton button = new JButton();
        int red = TransportGlyph.RECORD_RED.getRGB() & 0xFFFFFF;
        assertTrue(count(paint(new ToolIcon(TransportGlyph.RECORD, 16), button), rgb -> (rgb & 0xFFFFFF) == red) > 50);
        button.setEnabled(false);
        assertEquals(0, count(paint(new ToolIcon(TransportGlyph.RECORD, 16), button), rgb -> (rgb & 0xFFFFFF) == red));
    }

    @Test
    void buttonsTakeTheirTooltipFromTheApplicationAndScale() {
        Locale.setDefault(Locale.ENGLISH);
        AtomicInteger pressed = new AtomicInteger();
        JButton play = TransportButtons.NORMAL.button(TransportGlyph.PLAY, pressed::incrementAndGet);
        assertEquals("Play", play.getToolTipText());
        assertEquals("Play", play.getAccessibleContext().getAccessibleName());
        assertEquals("transport.play", play.getName());
        assertEquals(50, play.getPreferredSize().width);
        assertEquals(16, play.getIcon().getIconWidth());
        play.doClick();
        assertEquals(1, pressed.get());

        JToggleButton record = new TransportButtons(0.6).toggle(TransportGlyph.RECORD);
        assertEquals(30, record.getPreferredSize().width);
        assertEquals(10, record.getIcon().getIconWidth());
        record.doClick();
        assertTrue(record.isSelected());

        JPanel bar = TransportButtons.bar(play, record);
        assertEquals(2, bar.getComponentCount());
    }
}
