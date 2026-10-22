package eu.amadeos.commons.swing;

import eu.amadeos.commons.theme.DesktopTheme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.Objects;

/**
 * Toolbar icons drawn in Java2D on a 16 × 16 grid, so they need no icon theme, scale to any size and
 * follow the desktop theme: lines in the button's text colour (the inactive text colour when the button is
 * disabled), paper in the theme's field colour, highlights in the selection colour.
 *
 * <p>What is drawn is a {@link Glyph}. {@link StandardGlyph} has the commands most applications share
 * (open, save, print, undo, zoom, ...); an application adds its own glyphs as an enum that implements
 * {@link Glyph}:
 * <pre>
 * enum FractalGlyph implements ToolIcon.Glyph {
 *     HOME;
 *     public void paint(ToolIcon.Pen pen) { ... }
 * }
 * action.putValue(Action.LARGE_ICON_KEY, new ToolIcon(FractalGlyph.HOME));
 * </pre>
 */
public final class ToolIcon implements Icon {
    /** Size in toolbars at 100 % scaling. The glyphs are drawn on a 16-unit grid and scaled to this size. */
    public static final int SIZE = 22;

    // --8<-- [start:glyph]
    /** Draws one icon on the 16-unit grid with the colours of the {@link Pen}. */
    @FunctionalInterface
    public interface Glyph {
        void paint(Pen pen);
    }
    // --8<-- [end:glyph]

    private final Glyph glyph;
    private final int size;

    public ToolIcon(Glyph glyph) {
        this(glyph, SIZE);
    }

    public ToolIcon(Glyph glyph, int size) {
        this.glyph = Objects.requireNonNull(glyph, "glyph");
        this.size = size;
    }

    public Glyph glyph() {
        return glyph;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    // --8<-- [start:paint]
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        DesktopTheme theme = AmadeUI.theme();
        boolean enabled = c == null || c.isEnabled();
        Color ink = !enabled ? theme.textInactive()
                : c != null && c.getForeground() != null ? c.getForeground() : theme.textActive();
        Color light = enabled ? theme.selectionBackground() : theme.textInactive();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 16.0, size / 16.0);
            g2.setStroke(Pen.THIN);
            glyph.paint(new Pen(g2, ink, theme.fieldBackground(), light, enabled));
        } finally {
            g2.dispose();
        }
    }
    // --8<-- [end:paint]

    /**
     * The graphics of one icon, already scaled to the 16-unit grid, with the theme's colours and the
     * shapes several glyphs share. Lines lie on half units (1.5, 2.5, ...), so they are crisp at 100 %.
     *
     * @param g       for anything the helpers do not cover; the stroke starts 1 unit wide
     * @param ink     lines and dark areas: the button's text colour
     * @param paper   light areas: the theme's field colour
     * @param light   highlights: the selection colour
     * @param enabled false for a disabled button; ink and light are then the inactive text colour
     */
    public record Pen(Graphics2D g, Color ink, Color paper, Color light, boolean enabled) {
        static final BasicStroke THIN = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

        /** Fills a shape and outlines it in ink. */
        public void fillAndDraw(Shape shape, Color fill) {
            g.setColor(fill);
            g.fill(shape);
            g.setColor(ink);
            g.draw(shape);
        }

        public void fill(Shape shape, Color color) {
            g.setColor(color);
            g.fill(shape);
        }

        public void draw(Shape shape) {
            g.setColor(ink);
            g.draw(shape);
        }

        public void line(double x1, double y1, double x2, double y2) {
            draw(new Line2D.Double(x1, y1, x2, y2));
        }

        /** A wider line with round ends and corners, until {@link #thin()}. */
        public void bold(float width) {
            g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }

        /** Back to the 1-unit stroke. */
        public void thin() {
            g.setStroke(THIN);
        }

        /** Mirrors left and right: draws the left-pointing form of a glyph as its right-pointing twin. */
        public void mirror() {
            g.translate(16, 0);
            g.scale(-1, 1);
        }

        /** A filled triangle, for arrow heads. */
        public void triangle(double x1, double y1, double x2, double y2, double x3, double y3) {
            Path2D head = new Path2D.Double();
            head.moveTo(x1, y1);
            head.lineTo(x2, y2);
            head.lineTo(x3, y3);
            head.closePath();
            fill(head, ink);
        }

        /** A sheet with a folded corner. */
        public void page(double x, double y, double w, double h) {
            double fold = Math.min(3, w / 3);
            Path2D sheet = new Path2D.Double();
            sheet.moveTo(x, y);
            sheet.lineTo(x + w - fold, y);
            sheet.lineTo(x + w, y + fold);
            sheet.lineTo(x + w, y + h);
            sheet.lineTo(x, y + h);
            sheet.closePath();
            fillAndDraw(sheet, paper);
            Path2D corner = new Path2D.Double();
            corner.moveTo(x + w - fold, y);
            corner.lineTo(x + w - fold, y + fold);
            corner.lineTo(x + w, y + fold);
            draw(corner);
        }

        /** A lens around (cx, cy) with its handle to the lower right. */
        public void magnifier(double cx, double cy, double r) {
            fillAndDraw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r), paper);
            bold(2.2f);
            double edge = r * Math.sqrt(0.5);
            line(cx + edge, cy + edge, Math.min(15, cx + r + 3.5), Math.min(15, cy + r + 3.5));
            thin();
        }

        /** A letter centred horizontally, its baseline at {@code baseline}. */
        public void letter(String text, Font font, double baseline) {
            g.setColor(ink);
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (float) ((16 - fm.stringWidth(text)) / 2.0), (float) baseline);
        }
    }
}
