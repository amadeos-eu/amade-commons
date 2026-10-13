package org.datazuul.amadeos.commons.swing;

import org.datazuul.amadeos.commons.theme.DesktopTheme;
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

    @Test
    void colorDotHasTheRequestedSize() {
        assertEquals(10, AmadeUI.colorDot(Color.RED, 10).getIconWidth());
    }
}
