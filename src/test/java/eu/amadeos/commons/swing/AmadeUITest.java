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
