package eu.amadeos.commons.swing;

import eu.amadeos.commons.theme.DesktopLook;
import eu.amadeos.commons.theme.DesktopTheme;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The flat look of the amadeOS applications in the Horizon desktop look: rounded buttons with the accent colour
 * for the default button, fields with a thin line and an accent ring while they have the keyboard, round check
 * boxes and radio buttons, slim scroll bars, menus and tabs without bevels. {@link AmadeUI#install} switches to
 * it whenever the theme's {@link DesktopTheme#look()} is {@link DesktopLook#HORIZON}, and back to the Motif look
 * for a classic theme, so an application needs no code of its own.
 *
 * <p>The Motif helpers keep working: {@link AmadeUI#sunken()}, {@link AmadeUI#raised()} and
 * {@link AmadeUI#paintBevel} draw a thin line instead of a bevel in Horizon, with the same insets, so layouts
 * built for the classic look stay as they are.
 */
public final class HorizonUI {
    /** Rounding of buttons, fields and menus. */
    public static final int ARC = 8;
    /** Client property of toolbar buttons (set by {@link ToolButtons}): no frame, only a background on hover. */
    public static final String TOOL_BUTTON = "amade.toolButton";

    /** Keys only Horizon sets; switching back to the classic look removes them again. */
    private static final String[] OWN_KEYS = {
            "ScrollBarUI", "TabbedPaneUI", "ComboBoxUI", "SpinnerUI", "ScrollBar.width", "TableHeader.cellBorder",
            "SplitPaneDivider.border", "SplitPane.border", "ToolTip.border", "TitledBorder.border",
            "ComboBox.border", "Spinner.border", "Spinner.arrowButtonBorder", "ProgressBar.border",
            "ProgressBar.foreground", "ProgressBar.background", "ProgressBar.selectionForeground",
            "ProgressBar.selectionBackground", "Table.rowHeight", "Tree.rowHeight", "List.cellHeight",
            "ToolBar.border", "TextArea.border", "Slider.foreground", "Slider.focus", "TabbedPane.tabInsets",
            "ToolBar.background", "ToolBar.foreground", "SplitPaneUI", "SliderUI", "ProgressBarUI",
            "List.cellNoFocusBorder", "List.focusCellHighlightBorder", "List.focusSelectedCellHighlightBorder", "Slider.background",
            "Tree.expandedIcon", "Tree.collapsedIcon", "Tree.paintLines"};

    private HorizonUI() {
    }

    /** True while the given theme is one of Horizon's. */
    public static boolean active(DesktopTheme t) {
        return t != null && t.look() == DesktopLook.HORIZON;
    }

    /** True while the installed theme is one of Horizon's. */
    public static boolean active() {
        return active(AmadeUI.theme());
    }

    // ---- colours ---------------------------------------------------------------------------------

    /**
     * The colours of the flat look, worked out from a Horizon theme.
     *
     * @param control  the face of a button or check box
     * @param hover    the face under the mouse
     * @param pressed  the face while pressed
     * @param line     thin lines: the edge of buttons and fields, separators
     * @param accent   the accent colour: default button, focus ring, selection, ticked boxes
     * @param onAccent text and marks on the accent colour
     */
    public record Palette(Color control, Color hover, Color pressed, Color line, Color accent, Color onAccent,
                          Color thumb, Color thumbHover) {
        public static Palette of(DesktopTheme t) {
            Color text = t.textActive();
            Color base = t.base();
            Color accent = t.activeTitleBg();
            Color dark = new Color(0x14, 0x15, 0x1D);
            // White on the accent, as the top bar and the dock have it, unless the accent is too light for it
            Color onAccent = contrast(accent, Color.WHITE) >= 2.2 ? Color.WHITE : dark;
            if (t.isDark()) {
                return new Palette(mix(base, text, 0.08), mix(base, text, 0.14), mix(base, text, 0.03),
                        mix(base, text, 0.17), accent, onAccent, mix(base, text, 0.22), mix(base, text, 0.36));
            }
            return new Palette(Color.WHITE, mix(Color.WHITE, text, 0.05), mix(Color.WHITE, text, 0.11),
                    mix(base, text, 0.16), accent, onAccent, mix(base, text, 0.22), mix(base, text, 0.38));
        }
    }

    /** The contrast ratio of two colours after WCAG 2: 1 to 21. */
    static double contrast(Color a, Color b) {
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

    public static Palette palette() {
        return Palette.of(AmadeUI.theme());
    }

    static Color mix(Color a, Color b, double weightOfB) {
        return new Color(
                (int) Math.round(a.getRed() * (1 - weightOfB) + b.getRed() * weightOfB),
                (int) Math.round(a.getGreen() * (1 - weightOfB) + b.getGreen() * weightOfB),
                (int) Math.round(a.getBlue() * (1 - weightOfB) + b.getBlue() * weightOfB));
    }

    // ---- installing ------------------------------------------------------------------------------

    /** Removes the keys only Horizon sets, so that the classic look gets the look and feel's own again. */
    static void reset() {
        for (String key : OWN_KEYS) UIManager.put(key, null);
    }

    // --8<-- [start:install]
    /** Puts the flat look over the Motif one that {@link AmadeUI#install} has just put in place. */
    static void install(DesktopTheme t) {
        Palette p = Palette.of(t);
        ColorUIResource base = res(t.base());
        ColorUIResource text = res(t.textActive());
        ColorUIResource line = res(p.line());
        ColorUIResource accent = res(p.accent());
        ColorUIResource onAccent = res(p.onAccent());

        // Buttons: AmadeButtonUI paints the rounded face; the border only makes room
        put("Button.border", new FlatButtonBorder());
        put("ToggleButton.border", new FlatButtonBorder());

        // Fields, lists and scroll panes: a thin line, an accent ring around the focused field
        for (String key : new String[]{"TextField", "FormattedTextField", "PasswordField"}) {
            put(key + ".border", new FieldBorder());
        }
        put("TextField.inactiveBackground", base);
        put("TextArea.border", BorderFactory.createEmptyBorder(4, 6, 4, 6));
        put("ScrollPane.border", new LineBorder(false));
        put("TitledBorder.border", new LineBorder(true));
        put("ToolTip.border", BorderFactory.createCompoundBorder(new LineBorder(true),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        put("ToolTip.background", res(p.control()));
        put("ToolTip.foreground", text);

        // Selection in the accent colour
        for (String key : new String[]{"List", "Table", "Tree"}) {
            put(key + ".selectionBackground", accent);
            put(key + ".selectionForeground", onAccent);
        }
        put("Tree.selectionBorderColor", accent);
        put("ComboBox.selectionBackground", accent);
        put("ComboBox.selectionForeground", onAccent);
        for (String key : new String[]{"TextField", "FormattedTextField", "PasswordField", "TextArea", "EditorPane",
                "TextPane"}) {
            put(key + ".selectionBackground", res(mix(t.sunkenBg(), p.accent(), 0.45)));
            put(key + ".selectionForeground", res(t.fieldText()));
        }
        Border cell = BorderFactory.createEmptyBorder(3, 8, 3, 8);
        put("List.cellNoFocusBorder", cell);
        put("List.focusCellHighlightBorder", cell);
        put("List.focusSelectedCellHighlightBorder", cell);
        put("Tree.expandedIcon", new TreeChevron(true));
        put("Tree.collapsedIcon", new TreeChevron(false));
        put("Tree.paintLines", Boolean.FALSE);
        put("Table.gridColor", line);
        put("Table.rowHeight", 24);
        put("Tree.rowHeight", 22);
        put("TableHeader.cellBorder", BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, p.line()), BorderFactory.createEmptyBorder(3, 8, 3, 8)));

        // Menus: no bevels, the accent colour under the mouse
        put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, p.line()));
        put("PopupMenu.border", BorderFactory.createCompoundBorder(new LineBorder(true),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)));
        for (String key : new String[]{"Menu", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem"}) {
            put(key + ".selectionBackground", accent);
            put(key + ".selectionForeground", onAccent);
            put(key + ".acceleratorSelectionForeground", onAccent);
            put(key + ".border", BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }
        put("CheckBoxMenuItem.checkIcon", new MenuCheckIcon());
        put("RadioButtonMenuItem.checkIcon", new MenuRadioIcon());
        put("Separator.background", base);
        put("Separator.foreground", line);
        put("PopupMenuSeparator.background", base);
        put("PopupMenuSeparator.foreground", line);
        put("ToolBar.border", BorderFactory.createEmptyBorder(2, 4, 2, 4));
        put("ToolBar.background", base);
        put("ToolBar.foreground", text);

        // Check boxes and radio buttons
        put("CheckBox.icon", new CheckBoxIcon());
        put("RadioButton.icon", new RadioButtonIcon());

        // Scroll bars, tabs, combo boxes, spinners
        put("ScrollBarUI", FlatScrollBarUI.class.getName());
        put("ScrollBar.width", 12);
        put("TabbedPaneUI", FlatTabbedPaneUI.class.getName());
        put("TabbedPane.contentBorderInsets", new Insets(1, 0, 0, 0));
        put("TabbedPane.tabInsets", new Insets(5, 12, 6, 12));
        put("TabbedPane.selected", base);
        put("ComboBoxUI", FlatComboBoxUI.class.getName());
        put("ComboBox.border", new FieldBorder());
        put("ComboBox.background", res(t.fieldBackground()));
        put("ComboBox.foreground", res(t.fieldText()));
        put("SpinnerUI", FlatSpinnerUI.class.getName());
        put("Spinner.border", new FieldBorder());
        put("Spinner.arrowButtonBorder", BorderFactory.createEmptyBorder());

        put("SplitPaneUI", "javax.swing.plaf.basic.BasicSplitPaneUI");
        put("SplitPane.border", BorderFactory.createEmptyBorder());
        put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());
        put("SplitPane.dividerSize", 6);
        put("ProgressBarUI", FlatProgressBarUI.class.getName());
        put("SliderUI", FlatSliderUI.class.getName());
        put("ProgressBar.border", BorderFactory.createEmptyBorder());
        put("ProgressBar.foreground", accent);
        put("ProgressBar.background", res(p.control()));
        put("ProgressBar.selectionForeground", text);
        put("ProgressBar.selectionBackground", text);
        put("Slider.foreground", accent);
        put("Slider.background", base);
        put("Slider.focus", base);
    }
    // --8<-- [end:install]

    private static void put(String key, Object value) {
        UIManager.put(key, value);
    }

    private static ColorUIResource res(Color c) {
        return new ColorUIResource(c);
    }

    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** Fills what lies outside a rounded rectangle with the parent's colour, so that a square background does
     * not show in the corners of a rounded field. */
    private static void fillCorners(Graphics2D g2, Component c, int x, int y, int w, int h, int arc) {
        Container parent = c.getParent();
        if (parent == null || !c.isOpaque()) return;
        Area corners = new Area(new Rectangle(x, y, w, h));
        corners.subtract(new Area(new RoundRectangle2D.Float(x, y, w, h, arc, arc)));
        g2.setColor(parent.getBackground());
        g2.fill(corners);
    }

    // ---- the bevel replacement -------------------------------------------------------------------

    /** What {@link AmadeUI#paintBevel} draws in Horizon: a thin line in place of the bevel. */
    static void paintFrame(Graphics g, int x, int y, int width, int height) {
        g.setColor(palette().line());
        g.drawRect(x, y, width - 1, height - 1);
    }

    // ---- buttons ---------------------------------------------------------------------------------

    static boolean isToolButton(AbstractButton b) {
        return Boolean.TRUE.equals(b.getClientProperty(TOOL_BUTTON)) || b.getParent() instanceof JToolBar;
    }

    // --8<-- [start:button]
    /** The face of a push or toggle button; {@code AmadeButtonUI} and {@code AmadeToggleButtonUI} call it. */
    static void paintButton(Graphics g, AbstractButton b) {
        ButtonModel model = b.getModel();
        Palette p = palette();
        Graphics2D g2 = smooth(g);
        int w = b.getWidth(), h = b.getHeight();
        boolean pressed = model.isArmed() && model.isPressed();
        boolean hover = model.isRollover() && b.isEnabled();
        boolean selected = b instanceof JToggleButton && model.isSelected();
        boolean isDefault = b instanceof JButton jb && jb.isDefaultButton() && b.isEnabled();
        Color own = b.getBackground() instanceof UIResource ? null : b.getBackground();

        if (b.isOpaque() && b.getParent() != null) {
            g2.setColor(b.getParent().getBackground());
            g2.fillRect(0, 0, w, h);
        }
        RoundRectangle2D face = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, ARC, ARC);
        if (isToolButton(b) || !b.isContentAreaFilled()) {
            // Toolbar buttons: flat until the mouse is over them
            if (pressed || hover || selected) {
                g2.setColor(selected ? mix(p.control(), p.accent(), 0.35) : pressed ? p.pressed() : p.hover());
                g2.fill(face);
            }
        } else {
            Color fill;
            if (isDefault) fill = pressed ? mix(p.accent(), Color.BLACK, 0.15)
                    : hover ? mix(p.accent(), Color.WHITE, 0.12) : p.accent();
            else if (selected) fill = mix(p.control(), p.accent(), pressed ? 0.45 : 0.35);
            else if (own != null) fill = pressed ? mix(own, Color.BLACK, 0.12) : hover ? mix(own, Color.WHITE, 0.10) : own;
            else fill = pressed ? p.pressed() : hover ? p.hover() : p.control();
            g2.setColor(fill);
            g2.fill(face);
            if (!isDefault) {
                g2.setColor(b.hasFocus() && b.isFocusPainted() ? p.accent() : p.line());
                g2.draw(face);
            }
        }
        g2.dispose();
        if (isDefault) b.setForeground(new ColorUIResource(p.onAccent()));
        else if (b.getForeground() instanceof UIResource) b.setForeground(new ColorUIResource(AmadeUI.theme().textActive()));
    }
    // --8<-- [end:button]

    /** Makes room around the label: 5 × 14 pixels, or the button's margin. */
    public static class FlatButtonBorder extends AbstractBorder implements UIResource {
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            Insets m = c instanceof AbstractButton b ? b.getMargin() : null;
            if (m != null && !(m instanceof UIResource)) {
                insets.set(m.top + 1, m.left + 1, m.bottom + 1, m.right + 1);
            } else {
                insets.set(5, 14, 5, 14);
            }
            return insets;
        }
    }

    // ---- borders ---------------------------------------------------------------------------------

    /** The border of text fields, combo boxes and spinners: a rounded thin line, the accent while focused. */
    public static class FieldBorder extends AbstractBorder implements UIResource {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Palette p = palette();
            Graphics2D g2 = smooth(g);
            fillCorners(g2, c, x, y, width, height, ARC);
            boolean focused = c.hasFocus() || c instanceof JComboBox<?> combo && combo.isPopupVisible()
                    || c instanceof JSpinner s && s.getEditor() instanceof JSpinner.DefaultEditor e
                    && e.getTextField().hasFocus();
            if (focused && c.isEnabled()) {
                g2.setColor(p.accent());
                g2.setStroke(new BasicStroke(1.6f));
                g2.draw(new RoundRectangle2D.Float(x + 0.8f, y + 0.8f, width - 1.6f, height - 1.6f, ARC, ARC));
            } else {
                g2.setColor(p.line());
                g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, width - 1f, height - 1f, ARC, ARC));
            }
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            if (c instanceof JComboBox<?> || c instanceof JSpinner) insets.set(2, 2, 2, 2);
            else insets.set(4, 8, 4, 8);
            return insets;
        }
    }

    /** A thin line around scroll panes, menus and tooltips; {@code rounded} for the ones that float. */
    public static class LineBorder extends AbstractBorder implements UIResource {
        private final boolean rounded;

        public LineBorder(boolean rounded) {
            this.rounded = rounded;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = smooth(g);
            g2.setColor(palette().line());
            if (rounded) g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, width - 1f, height - 1f, 4, 4));
            else g2.drawRect(x, y, width - 1, height - 1);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }
    }

    // ---- check boxes and radio buttons -----------------------------------------------------------

    private static boolean selected(Component c) {
        return c instanceof AbstractButton b && b.isSelected();
    }

    private static void paintTick(Graphics2D g2, float x, float y, float size, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(size / 8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D tick = new Path2D.Float();
        tick.moveTo(x + size * 0.25, y + size * 0.52);
        tick.lineTo(x + size * 0.43, y + size * 0.70);
        tick.lineTo(x + size * 0.76, y + size * 0.32);
        g2.draw(tick);
    }

    /** A rounded box, filled with the accent colour and a white tick while ticked. */
    public static class CheckBoxIcon implements Icon, UIResource {
        private static final int SIZE = 16;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Palette p = palette();
            Graphics2D g2 = smooth(g);
            RoundRectangle2D box = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, SIZE - 1f, SIZE - 1f, 6, 6);
            boolean enabled = c == null || c.isEnabled();
            if (selected(c)) {
                g2.setColor(enabled ? p.accent() : p.line());
                g2.fill(box);
                paintTick(g2, x, y, SIZE, p.onAccent());
            } else {
                g2.setColor(AmadeUI.theme().fieldBackground());
                g2.fill(box);
                g2.setColor(enabled && c instanceof AbstractButton b && b.getModel().isRollover() ? p.accent() : p.thumb());
                g2.draw(box);
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

    /** A circle with a thick accent ring around a white dot while chosen. */
    public static class RadioButtonIcon implements Icon, UIResource {
        private static final int SIZE = 16;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Palette p = palette();
            Graphics2D g2 = smooth(g);
            boolean enabled = c == null || c.isEnabled();
            if (selected(c)) {
                g2.setColor(enabled ? p.accent() : p.line());
                g2.fillOval(x, y, SIZE, SIZE);
                g2.setColor(p.onAccent());
                g2.fillOval(x + 5, y + 5, SIZE - 10, SIZE - 10);
            } else {
                g2.setColor(AmadeUI.theme().fieldBackground());
                g2.fillOval(x, y, SIZE - 1, SIZE - 1);
                g2.setColor(enabled && c instanceof AbstractButton b && b.getModel().isRollover() ? p.accent() : p.thumb());
                g2.drawOval(x, y, SIZE - 1, SIZE - 1);
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

    /** A tick in front of a ticked menu item, nothing in front of the others. */
    public static class MenuCheckIcon implements Icon, UIResource {
        private static final int SIZE = 14;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (!selected(c)) return;
            Graphics2D g2 = smooth(g);
            boolean armed = c instanceof AbstractButton b && b.getModel().isArmed();
            paintTick(g2, x, y, SIZE, armed ? palette().onAccent() : AmadeUI.theme().textActive());
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

    /** A dot in front of the chosen menu item. */
    public static class MenuRadioIcon implements Icon, UIResource {
        private static final int SIZE = 14;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (!selected(c)) return;
            Graphics2D g2 = smooth(g);
            boolean armed = c instanceof AbstractButton b && b.getModel().isArmed();
            g2.setColor(armed ? palette().onAccent() : AmadeUI.theme().textActive());
            g2.fillOval(x + 4, y + 4, 6, 6);
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

    /** The open and closed marks of trees: a small chevron pointing down or right. */
    public static class TreeChevron implements Icon, UIResource {
        private static final int SIZE = 12;
        private final boolean expanded;

        public TreeChevron(boolean expanded) {
            this.expanded = expanded;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = smooth(g);
            g2.setColor(mix(AmadeUI.theme().textActive(), AmadeUI.theme().base(), 0.3));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            float cx = x + SIZE / 2f, cy = y + SIZE / 2f;
            if (expanded) {
                p.moveTo(cx - 3.5, cy - 1.5);
                p.lineTo(cx, cy + 2);
                p.lineTo(cx + 3.5, cy - 1.5);
            } else {
                p.moveTo(cx - 1.5, cy - 3.5);
                p.lineTo(cx + 2, cy);
                p.lineTo(cx - 1.5, cy + 3.5);
            }
            g2.draw(p);
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

    // ---- scroll bars -----------------------------------------------------------------------------

    /** A slim scroll bar without arrows: a rounded thumb on the window's colour. */
    public static class FlatScrollBarUI extends BasicScrollBarUI {
        public static ComponentUI createUI(JComponent c) {
            return new FlatScrollBarUI();
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return noButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return noButton();
        }

        private static JButton noButton() {
            JButton b = new JButton();
            Dimension none = new Dimension(0, 0);
            b.setPreferredSize(none);
            b.setMinimumSize(none);
            b.setMaximumSize(none);
            b.setFocusable(false);
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(c.getParent() instanceof JScrollPane sp && sp.getViewport().getView() != null
                    ? sp.getViewport().getView().getBackground() : AmadeUI.theme().base());
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Palette p = palette();
            Graphics2D g2 = smooth(g);
            g2.setColor(isThumbRollover() || isDragging ? p.thumbHover() : p.thumb());
            int pad = 3;
            boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
            int w = vertical ? r.width - 2 * pad : r.width - 2;
            int h = vertical ? r.height - 2 : r.height - 2 * pad;
            int arc = Math.min(vertical ? w : h, 8);
            g2.fillRoundRect(r.x + (vertical ? pad : 1), r.y + (vertical ? 1 : pad), w, h, arc, arc);
            g2.dispose();
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            return new Dimension(12, 24);
        }
    }

    // ---- tabs ------------------------------------------------------------------------------------

    /** Tabs as words with an accent line under the chosen one, and one thin line under all of them. */
    public static class FlatTabbedPaneUI extends BasicTabbedPaneUI {
        public static ComponentUI createUI(JComponent c) {
            return new FlatTabbedPaneUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets = new Insets(2, 6, 0, 6);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
        }

        @Override
        protected void paintTabBackground(Graphics g, int placement, int index, int x, int y, int w, int h,
                                          boolean selected) {
            if (index == getRolloverTab() && !selected) {
                Graphics2D g2 = smooth(g);
                g2.setColor(palette().hover());
                g2.fillRoundRect(x + 2, y + 2, w - 4, h - 4, ARC, ARC);
                g2.dispose();
            }
        }

        @Override
        protected void paintTabBorder(Graphics g, int placement, int index, int x, int y, int w, int h,
                                      boolean selected) {
            if (!selected) return;
            Graphics2D g2 = smooth(g);
            g2.setColor(palette().accent());
            switch (placement) {
                case LEFT -> g2.fillRoundRect(x, y + 4, 3, h - 8, 3, 3);
                case RIGHT -> g2.fillRoundRect(x + w - 3, y + 4, 3, h - 8, 3, 3);
                case BOTTOM -> g2.fillRoundRect(x + 8, y, w - 16, 3, 3, 3);
                default -> g2.fillRoundRect(x + 8, y + h - 3, w - 16, 3, 3, 3);
            }
            g2.dispose();
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects, int index,
                                           Rectangle iconRect, Rectangle textRect, boolean selected) {
        }

        @Override
        protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
            Rectangle area = tabPane.getBounds();
            Insets in = tabPane.getInsets();
            int tabsHeight = calculateTabAreaHeight(placement, runCount, maxTabHeight);
            g.setColor(palette().line());
            if (placement == TOP) g.drawLine(in.left, in.top + tabsHeight, area.width - in.right - 1, in.top + tabsHeight);
            else if (placement == BOTTOM) {
                int y = area.height - in.bottom - tabsHeight - 1;
                g.drawLine(in.left, y, area.width - in.right - 1, y);
            }
        }

        @Override
        protected void paintText(Graphics g, int placement, Font font, FontMetrics metrics, int index, String title,
                                 Rectangle textRect, boolean selected) {
            g.setFont(selected ? font.deriveFont(Font.BOLD) : font);
            Color fg = tabPane.isEnabledAt(index) ? (selected ? AmadeUI.theme().textActive()
                    : mix(AmadeUI.theme().textActive(), AmadeUI.theme().base(), 0.25)) : AmadeUI.theme().textInactive();
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(fg);
            int mnemonic = tabPane.getDisplayedMnemonicIndexAt(index);
            javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(tabPane, (Graphics2D) g, title,
                    mnemonic, textRect.x, textRect.y + metrics.getAscent());
        }

        @Override
        protected int calculateTabWidth(int placement, int index, FontMetrics metrics) {
            // room for the bold title of the chosen tab
            return super.calculateTabWidth(placement, index, metrics) + 6;
        }
    }

    // ---- combo boxes and spinners ----------------------------------------------------------------

    /** A small chevron on a flat button, for combo boxes and spinners. */
    static final class ChevronButton extends BasicArrowButton implements UIResource {
        ChevronButton(int direction) {
            super(direction);
            setBorder(BorderFactory.createEmptyBorder());
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusable(false);
            setRolloverEnabled(true);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight();
            if (getModel().isRollover() || getModel().isPressed()) {
                g2.setColor(getModel().isPressed() ? palette().pressed() : palette().hover());
                g2.fillRoundRect(1, 1, w - 2, h - 2, 6, 6);
            }
            g2.setColor(isEnabled() ? AmadeUI.theme().textActive() : AmadeUI.theme().textInactive());
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = w / 2f, cy = h / 2f, s = Math.min(4f, Math.min(w, h) / 4f);
            Path2D p = new Path2D.Float();
            if (getDirection() == NORTH) {
                p.moveTo(cx - s, cy + s / 2);
                p.lineTo(cx, cy - s / 2);
                p.lineTo(cx + s, cy + s / 2);
            } else {
                p.moveTo(cx - s, cy - s / 2);
                p.lineTo(cx, cy + s / 2);
                p.lineTo(cx + s, cy - s / 2);
            }
            g2.draw(p);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(22, 20);
        }
    }

    /** A combo box like a field with a chevron. */
    public static class FlatComboBoxUI extends BasicComboBoxUI {
        public static ComponentUI createUI(JComponent c) {
            return new FlatComboBoxUI();
        }

        @Override
        protected JButton createArrowButton() {
            return new ChevronButton(SwingConstants.SOUTH);
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            padding = new Insets(3, 7, 3, 4);
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(comboBox.isEnabled() ? comboBox.getBackground() : AmadeUI.theme().base());
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            // never the selection colour in the closed box: the ring shows the focus
            super.paintCurrentValue(g, bounds, false);
        }

        @Override
        protected javax.swing.plaf.basic.ComboPopup createPopup() {
            javax.swing.plaf.basic.BasicComboPopup popup = new javax.swing.plaf.basic.BasicComboPopup(comboBox);
            popup.setBorder(new LineBorder(true));
            return popup;
        }
    }

    /** A spinner like a field with two chevrons. */
    public static class FlatSpinnerUI extends BasicSpinnerUI {
        public static ComponentUI createUI(JComponent c) {
            return new FlatSpinnerUI();
        }

        @Override
        protected Component createNextButton() {
            ChevronButton b = new ChevronButton(SwingConstants.NORTH);
            b.setName("Spinner.nextButton");
            installNextButtonListeners(b);
            return b;
        }

        @Override
        protected Component createPreviousButton() {
            ChevronButton b = new ChevronButton(SwingConstants.SOUTH);
            b.setName("Spinner.previousButton");
            installPreviousButtonListeners(b);
            return b;
        }

        @Override
        protected JComponent createEditor() {
            JComponent editor = super.createEditor();
            if (editor instanceof JSpinner.DefaultEditor e) {
                e.getTextField().setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 2));
                e.setOpaque(false);
                e.getTextField().addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override
                    public void focusGained(java.awt.event.FocusEvent ev) {
                        spinner.repaint();
                    }

                    @Override
                    public void focusLost(java.awt.event.FocusEvent ev) {
                        spinner.repaint();
                    }
                });
            }
            return editor;
        }
    }

    // ---- progress bars and sliders ----------------------------------------------------------------

    /** A rounded track with the done part in the accent colour. */
    public static class FlatProgressBarUI extends javax.swing.plaf.basic.BasicProgressBarUI {
        public static ComponentUI createUI(JComponent c) {
            return new FlatProgressBarUI();
        }

        @Override
        protected void paintDeterminate(Graphics g, JComponent c) {
            Insets in = progressBar.getInsets();
            int w = progressBar.getWidth() - in.left - in.right;
            int h = progressBar.getHeight() - in.top - in.bottom;
            Palette p = palette();
            Graphics2D g2 = smooth(g);
            int bar = Math.min(h, 8);
            int y = in.top + (h - bar) / 2;
            g2.setColor(p.thumb());
            g2.fillRoundRect(in.left, y, w, bar, bar, bar);
            boolean horizontal = progressBar.getOrientation() == JProgressBar.HORIZONTAL;
            int done = getAmountFull(in, w, h);
            g2.setColor(p.accent());
            if (horizontal && done > 0) g2.fillRoundRect(in.left, y, Math.max(done, bar), bar, bar, bar);
            g2.dispose();
            if (progressBar.isStringPainted()) paintString(g, in.left, in.top, w, h, done, in);
        }

        @Override
        protected void paintIndeterminate(Graphics g, JComponent c) {
            Insets in = progressBar.getInsets();
            int w = progressBar.getWidth() - in.left - in.right;
            int h = progressBar.getHeight() - in.top - in.bottom;
            Palette p = palette();
            Graphics2D g2 = smooth(g);
            int bar = Math.min(h, 8);
            int y = in.top + (h - bar) / 2;
            g2.setColor(p.thumb());
            g2.fillRoundRect(in.left, y, w, bar, bar, bar);
            boxRect = getBox(boxRect);
            if (boxRect != null) {
                g2.setColor(p.accent());
                g2.fillRoundRect(boxRect.x, y, boxRect.width, bar, bar, bar);
            }
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            Dimension d = super.getPreferredSize(c);
            if (progressBar.getOrientation() == JProgressBar.HORIZONTAL && !progressBar.isStringPainted()) {
                d.height = Math.max(d.height, 12);
            }
            return d;
        }
    }

    /** A thin rounded track, the chosen part in the accent colour, and a round knob. */
    public static class FlatSliderUI extends javax.swing.plaf.basic.BasicSliderUI {
        public static ComponentUI createUI(JComponent c) {
            return new FlatSliderUI((JSlider) c);
        }

        public FlatSliderUI(JSlider slider) {
            super(slider);
        }

        @Override
        public void paintTrack(Graphics g) {
            Palette p = palette();
            Graphics2D g2 = smooth(g);
            Rectangle t = trackRect;
            if (slider.getOrientation() == JSlider.HORIZONTAL) {
                int y = t.y + t.height / 2 - 2;
                g2.setColor(p.thumb());
                g2.fillRoundRect(t.x, y, t.width, 4, 4, 4);
                g2.setColor(p.accent());
                int x = thumbRect.x + thumbRect.width / 2;
                g2.fillRoundRect(t.x, y, Math.max(0, x - t.x), 4, 4, 4);
            } else {
                int x = t.x + t.width / 2 - 2;
                g2.setColor(p.thumb());
                g2.fillRoundRect(x, t.y, 4, t.height, 4, 4);
                g2.setColor(p.accent());
                int y = thumbRect.y + thumbRect.height / 2;
                g2.fillRoundRect(x, y, 4, t.y + t.height - y, 4, 4);
            }
            g2.dispose();
        }

        @Override
        public void paintThumb(Graphics g) {
            Palette p = palette();
            Graphics2D g2 = smooth(g);
            Rectangle r = thumbRect;
            int d = Math.min(16, Math.min(r.width, r.height));
            int x = r.x + (r.width - d) / 2, y = r.y + (r.height - d) / 2;
            g2.setColor(slider.isEnabled() ? p.accent() : p.line());
            g2.fillOval(x, y, d, d);
            g2.setColor(p.onAccent());
            g2.fillOval(x + d / 2 - 3, y + d / 2 - 3, 6, 6);
            g2.dispose();
        }

        @Override
        protected Dimension getThumbSize() {
            return new Dimension(16, 16);
        }

        @Override
        public void paintFocus(Graphics g) {
        }
    }

    /** The border {@link AmadeUI#sunken()} and {@link AmadeUI#raised()} stand for in Horizon. */
    static Border frame() {
        return new LineBorder(false);
    }
}
