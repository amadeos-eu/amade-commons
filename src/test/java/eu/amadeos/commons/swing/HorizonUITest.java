package eu.amadeos.commons.swing;

import eu.amadeos.commons.theme.DesktopTheme;
import eu.amadeos.commons.theme.HorizonThemes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class HorizonUITest {
    private static final DesktopTheme DARK = HorizonThemes.theme(HorizonThemes.Scheme.DARK, HorizonThemes.Accent.TEAL);
    private static final DesktopTheme LIGHT = HorizonThemes.theme(HorizonThemes.Scheme.LIGHT, HorizonThemes.Accent.VIOLET);

    @AfterEach
    void classicAgain() {
        AmadeUI.install(DesktopTheme.WARM_MOTIF);
    }

    // --8<-- [start:switch]
    @Test
    void aHorizonThemeMakesTheLookFlatAndAClassicOneMakesItMotifAgain() {
        AmadeUI.install(DARK);
        assertTrue(HorizonUI.active());
        assertEquals(HorizonUI.FlatScrollBarUI.class.getName(), UIManager.get("ScrollBarUI"));
        assertInstanceOf(HorizonUI.FieldBorder.class, UIManager.getBorder("TextField.border"));
        assertInstanceOf(HorizonUI.CheckBoxIcon.class, UIManager.getIcon("CheckBox.icon"));
        assertEquals(DARK.activeTitleBg(), UIManager.getColor("List.selectionBackground"), "the accent");
        assertEquals(Font.PLAIN, UIManager.getFont("Label.font").getStyle());

        AmadeUI.install(DesktopTheme.WARM_MOTIF);
        assertFalse(HorizonUI.active());
        assertEquals("javax.swing.plaf.basic.BasicScrollBarUI", UIManager.get("ScrollBarUI"));
        assertNull(UIManager.getDefaults().get("TabbedPaneUI") instanceof String s && s.contains("Flat") ? s : null,
                "Horizon's own keys are gone");
        assertInstanceOf(AmadeUI.MotifBevelBorder.class, UIManager.getBorder("TextField.border"));
        assertInstanceOf(AmadeUI.MotifCheckBoxIcon.class, UIManager.getIcon("CheckBox.icon"));
    }
    // --8<-- [end:switch]

    @Test
    void thePaletteIsReadableInBothSchemes() {
        HorizonUI.Palette dark = HorizonUI.Palette.of(DARK);
        assertTrue(DesktopTheme.isDark(dark.control()));
        assertNotEquals(dark.control(), dark.hover());
        HorizonUI.Palette light = HorizonUI.Palette.of(LIGHT);
        assertEquals(Color.WHITE, light.control());
        assertEquals(LIGHT.activeTitleBg(), light.accent());
        assertEquals(Color.WHITE, light.onAccent(), "white on the violet accent");
        for (HorizonThemes.Accent a : HorizonThemes.Accent.values()) {
            assertEquals(Color.WHITE, HorizonUI.Palette.of(HorizonThemes.theme(HorizonThemes.Scheme.DARK, a)).onAccent(), a.id());
        }
        assertTrue(HorizonUI.contrast(Color.BLACK, Color.WHITE) > 20);
    }

    private static BufferedImage paint(JComponent c) {
        c.setSize(c.getPreferredSize());
        c.doLayout();
        BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        c.paint(g);
        g.dispose();
        return image;
    }

    @Test
    void theDefaultButtonIsFilledWithTheAccent() {
        AmadeUI.install(DARK);
        JPanel panel = new JPanel();
        JButton ok = new JButton("OK");
        JRootPane root = new JRootPane();
        root.getContentPane().add(panel);
        panel.add(ok);
        root.setDefaultButton(ok);
        BufferedImage image = paint(ok);
        assertEquals(DARK.activeTitleBg().getRGB(), image.getRGB(4, image.getHeight() / 2), "the face");
        assertNotEquals(DARK.activeTitleBg().getRGB(), image.getRGB(0, 0), "a rounded corner");

        JButton cancel = new JButton("Cancel");
        panel.add(cancel);
        BufferedImage plain = paint(cancel);
        assertEquals(HorizonUI.Palette.of(DARK).control().getRGB(), plain.getRGB(4, plain.getHeight() / 2));
    }

    @Test
    void theOldBevelHelpersDrawAThinLine() {
        AmadeUI.install(LIGHT);
        JPanel box = new JPanel();
        box.setBorder(AmadeUI.sunken());
        box.setPreferredSize(new Dimension(40, 30));
        BufferedImage image = paint(box);
        int line = HorizonUI.Palette.of(LIGHT).line().getRGB();
        assertEquals(line, image.getRGB(0, 10));
        assertEquals(line, image.getRGB(39, 10), "the same on both sides: no light and dark edge");
        assertEquals(new Insets(5, 10, 5, 10), AmadeUI.sunken().getBorderInsets(new JPanel()), "insets as before");
    }

    @Test
    void aTickedBoxIsFilledWithTheAccent() {
        AmadeUI.install(DARK);
        JCheckBox box = new JCheckBox("All day", true);
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        UIManager.getIcon("CheckBox.icon").paintIcon(box, g, 0, 0);
        g.dispose();
        assertEquals(DARK.activeTitleBg().getRGB(), image.getRGB(3, 12));
    }
}
