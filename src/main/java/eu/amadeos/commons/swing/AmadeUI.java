package eu.amadeos.commons.swing;

import eu.amadeos.commons.theme.DesktopTheme;
import eu.amadeos.commons.theme.ThemeStore;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.*;
import java.util.function.BiConsumer;

/**
 * The amadeOS Motif look for Swing: 3D bevels, Motif buttons, check boxes and radio buttons, and the
 * colours of the current {@link DesktopTheme} for every standard component.
 *
 * <p>Typical use (done by {@link eu.amadeos.commons.app.AmadeApplication}):
 * <pre>
 * ThemeStore themes = ThemeStore.system();
 * AmadeUI.install(themes.current());
 * AmadeUI.follow(themes);              // switch together with the desktop
 * </pre>
 *
 * <p>Components that need colours beyond the standard ones register them with
 * {@link #style(JComponent, BiConsumer)}, so that they are repainted correctly after a theme change.
 * Never pass theme colours to {@code setBackground} directly: such colours would stay when the theme
 * changes.
 */
public final class AmadeUI {
    private static final String STYLER = "amade.styler";
    private static volatile DesktopTheme theme = DesktopTheme.WARM_MOTIF;

    private AmadeUI() {
    }

    /** The theme the look was last installed with. */
    public static DesktopTheme theme() {
        return theme;
    }

    /** Installs the look with the given theme; call on the EDT before creating components. */
    public static void install(DesktopTheme newTheme) {
        DesktopTheme t = newTheme == null ? DesktopTheme.WARM_MOTIF : newTheme;
        theme = t;
        try {
            if (!(UIManager.getLookAndFeel() instanceof javax.swing.plaf.metal.MetalLookAndFeel)) {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            org.slf4j.LoggerFactory.getLogger(AmadeUI.class).warn("Cannot install the cross-platform look and feel", e);
        }

        ColorUIResource base = res(t.base());
        ColorUIResource highlight = res(t.highlight());
        ColorUIResource softHighlight = res(t.softHighlight());
        ColorUIResource shadow = res(t.shadow());
        ColorUIResource darkShadow = res(t.darkShadow());
        ColorUIResource sunken = res(t.sunkenBg());
        ColorUIResource text = res(t.textActive());
        ColorUIResource inactive = res(t.textInactive());
        ColorUIResource field = res(t.fieldBackground());
        ColorUIResource fieldText = res(t.fieldText());
        ColorUIResource selection = res(t.selectionBackground());
        ColorUIResource selectionText = res(t.selectionText());

        put("control", base);
        put("controlHighlight", highlight);
        put("controlLtHighlight", highlight);
        put("controlShadow", shadow);
        put("controlDkShadow", darkShadow);
        put("controlText", text);
        put("textInactiveText", inactive);

        put("Panel.background", base);
        put("Panel.foreground", text);
        put("Viewport.background", base);
        put("Label.foreground", text);
        put("Label.disabledForeground", inactive);
        put("TitledBorder.titleColor", text);
        put("OptionPane.background", base);
        put("OptionPane.messageForeground", text);
        put("ToolTip.background", softHighlight);
        put("ToolTip.foreground", res(DesktopTheme.isDark(t.softHighlight()) ? Color.WHITE : Color.BLACK));

        put("ButtonUI", AmadeButtonUI.class.getName());
        put("Button.background", base);
        put("Button.foreground", text);
        put("Button.select", sunken);
        put("Button.focus", shadow);
        put("Button.border", new MotifButtonBorder(2));
        put("ToggleButtonUI", AmadeToggleButtonUI.class.getName());
        put("ToggleButton.background", base);
        put("ToggleButton.foreground", text);
        put("ToggleButton.select", sunken);
        put("ToggleButton.border", new MotifButtonBorder(2));

        // Flat Metal gradients: no Ocean blue, and two ratios + three colours as Metal's painter expects.
        java.util.List<?> flat = java.util.Arrays.asList(0.0f, 0.0f, base, base, base);
        for (String key : new String[]{"MenuBar", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem", "Button",
                "ToggleButton", "CheckBox", "RadioButton", "ScrollBar", "Slider"}) {
            put(key + ".gradient", flat);
        }

        put("MenuBarUI", "javax.swing.plaf.basic.BasicMenuBarUI");
        put("MenuBar.background", base);
        put("MenuBar.border", new MotifBevelBorder(true, 2));
        for (String key : new String[]{"Menu", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem"}) {
            put(key + "UI", "javax.swing.plaf.basic.Basic" + key + "UI");
            put(key + ".background", base);
            put(key + ".foreground", text);
            put(key + ".selectionBackground", softHighlight);
            put(key + ".selectionForeground", text);
            put(key + ".acceleratorForeground", inactive);
            put(key + ".acceleratorSelectionForeground", text);
            put(key + ".disabledForeground", inactive);
            put(key + ".border", BorderFactory.createEmptyBorder(2, 6, 2, 6));
        }
        put("CheckBoxMenuItem.checkIcon", new MotifMenuCheckIcon());
        put("RadioButtonMenuItem.checkIcon", new MotifRadioButtonIcon());
        put("PopupMenu.background", base);
        put("PopupMenu.border", new MotifBevelBorder(true, 2));
        put("Separator.background", shadow);
        put("Separator.foreground", highlight);
        put("PopupMenuSeparator.background", shadow);
        put("PopupMenuSeparator.foreground", highlight);

        put("CheckBoxUI", "javax.swing.plaf.basic.BasicCheckBoxUI");
        put("CheckBox.background", base);
        put("CheckBox.foreground", text);
        put("CheckBox.focus", shadow);
        put("CheckBox.icon", new MotifCheckBoxIcon());
        put("RadioButtonUI", "javax.swing.plaf.basic.BasicRadioButtonUI");
        put("RadioButton.background", base);
        put("RadioButton.foreground", text);
        put("RadioButton.focus", shadow);
        put("RadioButton.icon", new MotifRadioButtonIcon());

        for (String key : new String[]{"TextField", "FormattedTextField", "PasswordField", "TextArea", "EditorPane",
                "TextPane"}) {
            put(key + ".background", field);
            put(key + ".foreground", fieldText);
            put(key + ".caretForeground", fieldText);
            put(key + ".selectionBackground", selection);
            put(key + ".selectionForeground", selectionText);
            put(key + ".inactiveForeground", inactive);
        }
        put("TextField.border", new MotifBevelBorder(false, 2));
        put("FormattedTextField.border", new MotifBevelBorder(false, 2));
        put("PasswordField.border", new MotifBevelBorder(false, 2));
        put("TextField.inactiveBackground", base);

        for (String key : new String[]{"List", "Table", "Tree"}) {
            put(key + ".background", field);
            put(key + ".foreground", fieldText);
            put(key + ".selectionBackground", selection);
            put(key + ".selectionForeground", selectionText);
        }
        put("Table.gridColor", res(t.isDark() ? t.shadow() : t.softHighlight()));
        put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder(1, 1, 1, 1));
        put("TableHeader.background", base);
        put("TableHeader.foreground", text);
        put("Tree.textBackground", field);
        put("Tree.textForeground", fieldText);
        put("Tree.selectionBorderColor", selection);
        put("Tree.hash", shadow);

        put("ComboBox.background", base);
        put("ComboBox.foreground", text);
        put("ComboBox.selectionBackground", selection);
        put("ComboBox.selectionForeground", selectionText);
        put("Spinner.background", base);
        put("Spinner.foreground", text);

        put("ScrollBarUI", "javax.swing.plaf.basic.BasicScrollBarUI");
        put("ScrollPane.background", base);
        put("ScrollPane.border", new MotifBevelBorder(false, 2));
        put("ScrollBar.background", sunken);
        put("ScrollBar.thumb", base);
        put("ScrollBar.thumbHighlight", highlight);
        put("ScrollBar.thumbShadow", shadow);
        put("ScrollBar.thumbDarkShadow", darkShadow);
        put("ScrollBar.track", sunken);

        put("SplitPane.background", base);
        put("SplitPane.dividerSize", 8);
        put("TabbedPane.background", base);
        put("TabbedPane.foreground", text);
        put("TabbedPane.selected", softHighlight);
        put("TabbedPane.contentBorderInsets", new Insets(2, 2, 2, 2));
    }

    // --8<-- [start:apply]
    /**
     * Installs a new theme and updates every open window; call on the EDT. Components registered with
     * {@link #style(JComponent, BiConsumer)} get their extra colours again.
     */
    public static void apply(DesktopTheme newTheme) {
        install(newTheme);
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            restyle(window);
            window.repaint();
        }
    }
    // --8<-- [end:apply]

    // --8<-- [start:follow]
    /** Follows theme changes of the desktop: watches the theme files and applies changes on the EDT. */
    public static void follow(ThemeStore store) {
        store.addListener(changed -> SwingUtilities.invokeLater(() -> apply(changed)));
        store.watch();
    }
    // --8<-- [end:follow]

    // --8<-- [start:style]
    /**
     * Gives a component colours beyond the standard ones, e.g. a white message pane or a highlighted bar.
     * The styler runs now and after every theme change.
     */
    public static <C extends JComponent> C style(C component, BiConsumer<? super C, DesktopTheme> styler) {
        @SuppressWarnings("unchecked")
        BiConsumer<JComponent, DesktopTheme> stored = (BiConsumer<JComponent, DesktopTheme>) (BiConsumer<?, ?>) styler;
        component.putClientProperty(STYLER, stored);
        stored.accept(component, theme);
        return component;
    }
    // --8<-- [end:style]

    /** Runs the stylers of the component and all its children; public for tests and custom windows. */
    @SuppressWarnings("unchecked")
    public static void restyle(Component root) {
        if (root instanceof JComponent c && c.getClientProperty(STYLER) instanceof BiConsumer<?, ?> styler) {
            ((BiConsumer<JComponent, DesktopTheme>) styler).accept(c, theme);
        }
        if (root instanceof JMenu menu) restyle(menu.getPopupMenu());
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) restyle(child);
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------

    /** A sunken bevel as around lists, text areas and scroll panes. */
    public static Border sunken() {
        return new MotifBevelBorder(false, 2);
    }

    /** A raised bevel as around panels and the menu bar. */
    public static Border raised() {
        return new MotifBevelBorder(true, 2);
    }

    /** A small filled circle, e.g. an account or category marker. */
    public static Icon colorDot(Color color, int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(x, y, size, size);
                g2.setColor(theme.darkShadow());
                g2.drawOval(x, y, size - 1, size - 1);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /** Paints a Motif bevel in the colours of the current theme. */
    public static void paintBevel(Graphics g, int x, int y, int width, int height, boolean raised, int thickness) {
        DesktopTheme t = theme;
        Color tl1 = raised ? t.highlight() : t.darkShadow();
        Color tl2 = raised ? t.softHighlight() : t.shadow();
        Color br1 = raised ? t.darkShadow() : t.highlight();
        Color br2 = raised ? t.shadow() : t.softHighlight();
        for (int i = 0; i < thickness; i++) {
            g.setColor(i == 0 ? tl1 : tl2);
            g.drawLine(x + i, y + i, x + width - 1 - i, y + i);
            g.drawLine(x + i, y + i, x + i, y + height - 1 - i);
            g.setColor(i == 0 ? br1 : br2);
            g.drawLine(x + i, y + height - 1 - i, x + width - 1 - i, y + height - 1 - i);
            g.drawLine(x + width - 1 - i, y + i, x + width - 1 - i, y + height - 1 - i);
        }
    }

    private static ColorUIResource res(Color color) {
        return new ColorUIResource(color);
    }

    private static void put(String key, Object value) {
        UIManager.put(key, value);
    }

    /** Sets a colour only if the application did not choose its own. */
    private static void defaultColors(AbstractButton b) {
        if (b.getBackground() == null || b.getBackground() instanceof UIResource) b.setBackground(res(theme.base()));
        if (b.getForeground() == null || b.getForeground() instanceof UIResource) b.setForeground(res(theme.textActive()));
    }

    // ---- borders ---------------------------------------------------------------------------------

    /** Motif 3D bevel border. */
    public static class MotifBevelBorder extends AbstractBorder implements UIResource {
        private final boolean raised;
        private final int thickness;

        public MotifBevelBorder(boolean raised, int thickness) {
            this.raised = raised;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            paintBevel(g, x, y, width, height, raised, thickness);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            if (c instanceof JScrollPane || c instanceof JMenuBar || c instanceof JPopupMenu) {
                return new Insets(thickness, thickness, thickness, thickness);
            }
            return new Insets(thickness + 3, thickness + 8, thickness + 3, thickness + 8);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            Insets ins = getBorderInsets(c);
            insets.set(ins.top, ins.left, ins.bottom, ins.right);
            return insets;
        }
    }

    /** Button border that is sunken while the button is pressed or selected. */
    public static class MotifButtonBorder extends AbstractBorder implements UIResource {
        private final int thickness;

        public MotifButtonBorder(int thickness) {
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            boolean sunken = false;
            if (c instanceof AbstractButton b) {
                ButtonModel model = b.getModel();
                sunken = (model.isArmed() && model.isPressed()) || model.isSelected();
            }
            paintBevel(g, x, y, width, height, !sunken, thickness);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            Insets margin = c instanceof AbstractButton b ? b.getMargin() : null;
            if (margin != null) {
                return new Insets(thickness + margin.top, thickness + margin.left,
                        thickness + margin.bottom, thickness + margin.right);
            }
            return new Insets(thickness + 2, thickness + 6, thickness + 2, thickness + 6);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            Insets ins = getBorderInsets(c);
            insets.set(ins.top, ins.left, ins.bottom, ins.right);
            return insets;
        }
    }

    // ---- button UIs ------------------------------------------------------------------------------

    /** Motif push button: bold label, lighter while hovered, sunken and shifted by one pixel while pressed. */
    public static class AmadeButtonUI extends BasicButtonUI {
        private static final AmadeButtonUI INSTANCE = new AmadeButtonUI();

        public static ComponentUI createUI(JComponent c) {
            return INSTANCE;
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(true);
            b.setFocusPainted(false);
            b.setRolloverEnabled(true);
            defaultColors(b);
            if (b.getBorder() == null || b.getBorder() instanceof UIResource) b.setBorder(new MotifButtonBorder(2));
            b.setFont(b.getFont().deriveFont(Font.BOLD, 12.0f));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            DesktopTheme t = theme;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean pressed = model.isArmed() && model.isPressed();
            Color bg;
            if (pressed) bg = t.sunkenBg();
            else if (model.isRollover() && b.isEnabled()) bg = t.softHighlight();
            else bg = b.getBackground() != null ? b.getBackground() : t.base();
            g2.setColor(bg);
            g2.fillRect(0, 0, c.getWidth(), c.getHeight());
            if (pressed) g2.translate(1, 1);
            super.paint(g2, c);
            if (pressed) g2.translate(-1, -1);
        }
    }

    /** Motif toggle button: sunken while selected. */
    public static class AmadeToggleButtonUI extends BasicToggleButtonUI {
        private static final AmadeToggleButtonUI INSTANCE = new AmadeToggleButtonUI();

        public static ComponentUI createUI(JComponent c) {
            return INSTANCE;
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(true);
            b.setFocusPainted(false);
            b.setRolloverEnabled(true);
            defaultColors(b);
            if (b.getBorder() == null || b.getBorder() instanceof UIResource) b.setBorder(new MotifButtonBorder(2));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            DesktopTheme t = theme;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean down = model.isSelected() || (model.isArmed() && model.isPressed());
            Color bg = down ? t.sunkenBg() : model.isRollover() && b.isEnabled() ? t.softHighlight() : t.base();
            g2.setColor(bg);
            g2.fillRect(0, 0, c.getWidth(), c.getHeight());
            if (down) g2.translate(1, 1);
            super.paint(g2, c);
            if (down) g2.translate(-1, -1);
        }
    }

    // ---- icons -----------------------------------------------------------------------------------

    /** Check mark of check box menu items; drawn only for selected items. */
    public static class MotifMenuCheckIcon implements Icon, UIResource {
        private static final int SIZE = 12;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            paintBevel(g2, x, y, SIZE, SIZE, false, 1);
            g2.setColor(theme.sunkenBg());
            g2.fillRect(x + 1, y + 1, SIZE - 2, SIZE - 2);
            // BasicMenuItemUI paints the check icon for selected and unselected items alike.
            if (c instanceof AbstractButton b && b.isSelected()) {
                g2.setColor(theme.textActive());
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(x + 2, y + 6, x + 5, y + 9);
                g2.drawLine(x + 5, y + 9, x + 9, y + 3);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }

    /** Motif check box. */
    public static class MotifCheckBoxIcon implements Icon, UIResource {
        private static final int SIZE = 13;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            paintBevel(g2, x, y, SIZE, SIZE, false, 1);
            g2.setColor(theme.sunkenBg());
            g2.fillRect(x + 1, y + 1, SIZE - 2, SIZE - 2);
            if (c instanceof AbstractButton b && b.isSelected()) {
                g2.setColor(theme.textActive());
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawLine(x + 2, y + 6, x + 5, y + 10);
                g2.drawLine(x + 5, y + 10, x + 10, y + 3);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }

    /** Motif radio button (round, as in later CDE versions). */
    public static class MotifRadioButtonIcon implements Icon, UIResource {
        private static final int SIZE = 13;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(theme.sunkenBg());
            g2.fillOval(x + 1, y + 1, SIZE - 2, SIZE - 2);
            g2.setColor(theme.darkShadow());
            g2.drawArc(x, y, SIZE - 1, SIZE - 1, 45, 180);
            g2.setColor(theme.highlight());
            g2.drawArc(x, y, SIZE - 1, SIZE - 1, 225, 180);
            if (c instanceof AbstractButton b && b.isSelected()) {
                g2.setColor(theme.textActive());
                g2.fillOval(x + SIZE / 2 - 2, y + SIZE / 2 - 2, 5, 5);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
