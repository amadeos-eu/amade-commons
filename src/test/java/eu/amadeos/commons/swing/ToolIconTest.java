package eu.amadeos.commons.swing;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolIconTest {
    @Test
    void everyStandardGlyphDrawsSomethingInTheButtonColour() {
        JButton button = new JButton();
        button.setForeground(Color.BLACK);
        for (StandardGlyph glyph : StandardGlyph.values()) {
            BufferedImage image = paint(new ToolIcon(glyph), button);
            int ink = count(image, rgb -> (rgb & 0xFFFFFF) < 0x404040);
            assertTrue(ink >= 10, glyph + " draws only " + ink + " dark pixels");
        }
    }

    @Test
    void disabledButtonsUseTheInactiveColour() {
        JButton button = new JButton();
        button.setForeground(Color.BLACK);
        button.setEnabled(false);
        int inactive = AmadeUI.theme().textInactive().getRGB() & 0xFFFFFF;
        BufferedImage image = paint(new ToolIcon(StandardGlyph.SAVE), button);
        assertEquals(0, count(image, rgb -> (rgb & 0xFFFFFF) == 0));
        assertTrue(count(image, rgb -> (rgb & 0xFFFFFF) == inactive) > 20);
    }

    @Test
    void scalesToTheRequestedSize() {
        ToolIcon icon = new ToolIcon(StandardGlyph.OPEN, 32);
        assertEquals(32, icon.getIconWidth());
        assertEquals(32, icon.getIconHeight());
        assertEquals(ToolIcon.SIZE, new ToolIcon(StandardGlyph.OPEN).getIconWidth());
        // The 16-unit grid fills the whole icon: a glyph that covers the grid covers all 32 × 32 pixels.
        BufferedImage image = paint(new ToolIcon(pen -> pen.fill(new Rectangle2D.Double(0, 0, 16, 16), Color.RED), 32),
                new JButton());
        assertEquals(32 * 32, count(image, rgb -> (rgb & 0xFFFFFF) == 0xFF0000));
    }

    // --8<-- [start:app-glyph]
    @Test
    void anApplicationGlyphGetsThePenWithTheThemeColours() {
        AtomicReference<ToolIcon.Pen> seen = new AtomicReference<>();
        JButton button = new JButton();
        button.setForeground(new Color(0x123456));
        paint(new ToolIcon(seen::set), button);
        ToolIcon.Pen pen = seen.get();
        assertEquals(new Color(0x123456), pen.ink());
        assertEquals(AmadeUI.theme().fieldBackground(), pen.paper());
        assertEquals(AmadeUI.theme().selectionBackground(), pen.light());
        assertTrue(pen.enabled());

        button.setEnabled(false);
        paint(new ToolIcon(seen::set), button);
        assertFalse(seen.get().enabled());
        assertEquals(AmadeUI.theme().textInactive(), seen.get().light());
    }
    // --8<-- [end:app-glyph]

    @Test
    void toolButtonsShowOnlyTheIconAndNeverTakeTheFocus() {
        Action action = new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        ToolIcon icon = new ToolIcon(StandardGlyph.SAVE);
        action.putValue(Action.LARGE_ICON_KEY, icon);
        action.putValue(Action.ACTION_COMMAND_KEY, "save");
        JButton button = ToolButtons.button(action);
        assertTrue(button.getHideActionText());
        assertEquals("", button.getText() == null ? "" : button.getText());
        assertSame(icon, button.getIcon());
        assertFalse(button.isFocusable());
        assertEquals("save", button.getName());

        action.putValue(Action.SELECTED_KEY, false);
        JToggleButton toggle = ToolButtons.toggle(action);
        assertFalse(toggle.isSelected());
        action.putValue(Action.SELECTED_KEY, true);
        assertTrue(toggle.isSelected());
        assertNull(toggle.getText());
    }

    static BufferedImage paint(ToolIcon icon, JButton button) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0xC0C0C0));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        icon.paintIcon(button, g, 0, 0);
        g.dispose();
        return image;
    }

    static int count(BufferedImage image, IntPredicate test) {
        int n = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (test.test(image.getRGB(x, y))) n++;
            }
        }
        return n;
    }
}
