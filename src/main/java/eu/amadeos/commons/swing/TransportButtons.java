package eu.amadeos.commons.swing;

import static eu.amadeos.commons.i18n.Texts.tr;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;

/**
 * The wide buttons of a player's transport bar, Play, Pause, Stop and the rest, with a
 * {@link TransportGlyph} and the tooltip and accessible name from the application's texts. Unlike
 * {@link ToolButtons} they take the focus, because in a player they are the main controls, not shortcuts
 * for menu commands.
 *
 * <pre>
 * TransportButtons transport = TransportButtons.NORMAL;          // or new TransportButtons(0.8)
 * JButton play = transport.button(TransportGlyph.PLAY, presenter::play);
 * panel.add(TransportButtons.bar(previous, stop, play, pause, next));
 * </pre>
 *
 * @param scale size of the buttons and symbols, 1 for 100 %
 */
public record TransportButtons(double scale) {
    /** Buttons at 100 %: 50 × 34 pixels with a 16-pixel symbol. */
    public static final TransportButtons NORMAL = new TransportButtons(1);

    // --8<-- [start:sizes]
    /** The symbol: 16 pixels at 100 %, never less than 10. */
    public ToolIcon icon(TransportGlyph glyph) {
        return new ToolIcon(glyph, Math.max(10, px(16)));
    }

    /** A push button that runs the action. */
    public JButton button(TransportGlyph glyph, Runnable action) {
        JButton b = style(new JButton(icon(glyph)), glyph);
        b.addActionListener(e -> action.run());
        return b;
    }

    /** A button that stays pressed in, e.g. Record. */
    public JToggleButton toggle(TransportGlyph glyph) {
        return style(new JToggleButton(icon(glyph)), glyph);
    }

    private <B extends AbstractButton> B style(B b, TransportGlyph glyph) {
        b.setToolTipText(tr(glyph.key()));
        b.getAccessibleContext().setAccessibleName(tr(glyph.key()));
        b.setMargin(new Insets(px(6), px(14), px(6), px(14)));
        Dimension size = new Dimension(px(50), px(34));
        b.setPreferredSize(size);
        b.setMinimumSize(size);
        b.setName(glyph.key());
        return b;
    }
    // --8<-- [end:sizes]

    /** Buttons side by side without gaps, as one bar. */
    public static JPanel bar(AbstractButton... buttons) {
        JPanel bar = new JPanel(new GridLayout(1, buttons.length, 0, 0));
        bar.setOpaque(false);
        for (AbstractButton b : buttons) bar.add(b);
        return bar;
    }

    private int px(int value) {
        return Math.round((float) (value * scale));
    }
}
