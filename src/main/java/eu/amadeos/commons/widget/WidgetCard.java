package eu.amadeos.commons.widget;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;

/**
 * The card a widget sits on: a rounded area in the card colour with a faint edge and a margin of
 * {@link #PADDING} pixels around the widget's component. The host shapes the window to {@link #shape}, so
 * the corners show the desktop behind.
 */
public class WidgetCard extends JPanel {
    public static final int PADDING = 12;
    public static final int ARC = 20;

    private final Supplier<WidgetColors> colors;

    public WidgetCard(JComponent content, Supplier<WidgetColors> colors) {
        super(new BorderLayout());
        this.colors = colors;
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        content.setOpaque(false);
        add(content, BorderLayout.CENTER);
    }

    /** The rounded outline of a card of the given size. */
    public static RoundRectangle2D shape(int width, int height) {
        return new RoundRectangle2D.Double(0, 0, width, height, ARC, ARC);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        WidgetColors c = colors.get();
        g.setColor(c.card());
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(c.line());
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, ARC, ARC));
        g.dispose();
    }
}
