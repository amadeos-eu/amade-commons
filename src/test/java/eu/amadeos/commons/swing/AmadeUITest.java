package eu.amadeos.commons.swing;

import eu.amadeos.commons.theme.DesktopTheme;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class AmadeUITest {
    @Test
    void installUsesTheThemeColours() {
        AmadeUI.install(DesktopTheme.CDE_DESERT);
        assertEquals(DesktopTheme.CDE_DESERT.base(), UIManager.getColor("Panel.background"));
        assertEquals(DesktopTheme.CDE_DESERT.fieldBackground(), UIManager.getColor("Table.background"));
        assertEquals(DesktopTheme.CDE_DESERT.selectionBackground(), UIManager.getColor("Tree.selectionBackground"));
        assertSame(DesktopTheme.CDE_DESERT, AmadeUI.theme());

        AmadeUI.install(DesktopTheme.DARK_SLATE);
        assertEquals(DesktopTheme.DARK_SLATE.fieldText(), UIManager.getColor("TextArea.foreground"));
        AmadeUI.install(DesktopTheme.WARM_MOTIF);
    }

    @Test
    void slidersAndProgressBarsTakeTheThemeColours() {
        AmadeUI.install(DesktopTheme.CDE_BROCADE);
        javax.swing.JSlider slider = new javax.swing.JSlider();
        assertInstanceOf(AmadeUI.AmadeSliderUI.class, slider.getUI());
        assertEquals(DesktopTheme.CDE_BROCADE.base(), slider.getBackground());
        assertEquals(DesktopTheme.CDE_BROCADE.textActive(), slider.getForeground());
        javax.swing.JProgressBar bar = new javax.swing.JProgressBar();
        assertEquals(DesktopTheme.CDE_BROCADE.sunkenBg(), bar.getBackground());
        assertEquals(AmadeUI.fillColor(DesktopTheme.CDE_BROCADE), bar.getForeground());

        // Where the selection colour is the base colour, the fill falls back to a tone that stands out
        Color fill = AmadeUI.fillColor(DesktopTheme.CLASSIC_GRAY);
        assertNotEquals(DesktopTheme.CLASSIC_GRAY.selectionBackground(), fill);
        assertEquals(DesktopTheme.WARM_MOTIF.selectionBackground(), AmadeUI.fillColor(DesktopTheme.WARM_MOTIF));
        AmadeUI.install(DesktopTheme.WARM_MOTIF);
    }

    @Test
    void switchedOffButtonsAndComboBoxesStayReadableOnADarkPalette() {
        DesktopTheme dark = eu.amadeos.commons.theme.HorizonThemes.find("horizon-dark-teal").orElseThrow();
        AmadeUI.install(dark);
        try {
            javax.swing.JButton button = new javax.swing.JButton("Picture");
            javax.swing.JToggleButton toggle = new javax.swing.JToggleButton("B");
            javax.swing.JComboBox<String> combo = new javax.swing.JComboBox<>(new String[]{"Text"});
            for (javax.swing.JComponent c : new javax.swing.JComponent[]{button, toggle, combo}) {
                c.setEnabled(false);
                c.setSize(c.getPreferredSize());
                c.doLayout();
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(c.getWidth(), c.getHeight(),
                        java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g = img.createGraphics();
                c.printAll(g);
                g.dispose();
                int brightest = 0;
                for (int y = 3; y < img.getHeight() - 3; y++) {
                    for (int x = 3; x < img.getWidth() - 3; x++) {
                        Color px = new Color(img.getRGB(x, y));
                        brightest = Math.max(brightest, (px.getRed() + px.getGreen() + px.getBlue()) / 3);
                    }
                }
                // The label is drawn in the inactive text colour, well apart from the dark face
                int face = (dark.base().getRed() + dark.base().getGreen() + dark.base().getBlue()) / 3;
                assertTrue(brightest - face > 60, c.getClass().getSimpleName() + " text hardly differs: " + brightest);
            }
            assertEquals(dark.base(), UIManager.getColor("ComboBox.disabledBackground"));
        } finally {
            AmadeUI.install(DesktopTheme.WARM_MOTIF);
        }
    }

    // --8<-- [start:stylers]
    @Test
    void stylersRunAgainAfterAThemeChange() {
        AmadeUI.install(DesktopTheme.WARM_MOTIF);
        JPanel root = new JPanel();
        JLabel label = AmadeUI.style(new JLabel("x"), (l, t) -> l.setForeground(t.activeTitleBg()));
        root.add(label);
        assertEquals(DesktopTheme.WARM_MOTIF.activeTitleBg(), label.getForeground());

        AmadeUI.install(DesktopTheme.CDE_BROCADE);
        AmadeUI.restyle(root);
        assertEquals(DesktopTheme.CDE_BROCADE.activeTitleBg(), label.getForeground());
        AmadeUI.install(DesktopTheme.WARM_MOTIF);
    }
    // --8<-- [end:stylers]

    // --8<-- [start:font-size]
    @Test
    void fontSizeScalesTheLookAndFeelFontsWithoutCompounding() {
        AmadeUI.install(DesktopTheme.WARM_MOTIF);
        float normal = UIManager.getFont("Label.font").getSize2D();
        assertEquals(1f, AmadeUI.fontScale());

        AmadeUI.install(DesktopTheme.WARM_MOTIF.withFontSize(7));
        assertEquals(Math.round(normal * 1.5f), UIManager.getFont("Label.font").getSize2D());
        AmadeUI.install(DesktopTheme.WARM_MOTIF.withFontSize(7));
        assertEquals(Math.round(normal * 1.5f), UIManager.getFont("Label.font").getSize2D(), "installing twice");
        assertEquals(18f, AmadeUI.scaled(12f));

        AmadeUI.install(DesktopTheme.WARM_MOTIF);
        assertEquals(normal, UIManager.getFont("Label.font").getSize2D());
    }
    // --8<-- [end:font-size]

    @Test
    void colorDotHasTheRequestedSize() {
        assertEquals(10, AmadeUI.colorDot(Color.RED, 10).getIconWidth());
    }
}
