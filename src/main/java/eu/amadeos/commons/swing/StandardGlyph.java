package eu.amadeos.commons.swing;

import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The toolbar glyphs of the commands most applications share. Glyphs only one application needs stay in
 * that application, as an enum of its own that implements {@link ToolIcon.Glyph}.
 */
public enum StandardGlyph implements ToolIcon.Glyph {
    NEW, OPEN, SAVE, PRINT, CUT, COPY, PASTE, UNDO, REDO, FIND, ZOOM_IN, ZOOM_OUT, REFRESH;

    // --8<-- [start:paint]
    @Override
    public void paint(ToolIcon.Pen pen) {
        switch (this) {
            case NEW -> pen.page(3.5, 1.5, 10, 13);
            case OPEN -> open(pen);
            case SAVE -> save(pen);
            case PRINT -> print(pen);
            case CUT -> cut(pen);
            case COPY -> {
                pen.page(1.5, 1.5, 8, 10);
                pen.page(6.5, 4.5, 8, 10);
            }
            case PASTE -> paste(pen);
            case UNDO -> undo(pen);
            case REDO -> {
                pen.mirror();
                undo(pen);
            }
            case FIND -> pen.magnifier(6.5, 6.5, 4.5);
            case ZOOM_IN -> {
                pen.magnifier(6.5, 6.5, 5);
                pen.line(4, 6.5, 9, 6.5);
                pen.line(6.5, 4, 6.5, 9);
            }
            case ZOOM_OUT -> {
                pen.magnifier(6.5, 6.5, 5);
                pen.line(4, 6.5, 9, 6.5);
            }
            case REFRESH -> refresh(pen);
        }
    }
    // --8<-- [end:paint]

    // --8<-- [start:open]
    /** A folder, its front flap open. */
    private static void open(ToolIcon.Pen pen) {
        Path2D back = new Path2D.Double();
        back.moveTo(1.5, 13.5);
        back.lineTo(1.5, 3.5);
        back.lineTo(5.5, 3.5);
        back.lineTo(7, 5.5);
        back.lineTo(12.5, 5.5);
        back.lineTo(12.5, 8.5);
        pen.fillAndDraw(back, pen.paper());
        Path2D front = new Path2D.Double();
        front.moveTo(1.5, 13.5);
        front.lineTo(4, 8.5);
        front.lineTo(15, 8.5);
        front.lineTo(12.5, 13.5);
        front.closePath();
        pen.fillAndDraw(front, pen.ink());
    }
    // --8<-- [end:open]

    /** A floppy disk. */
    private static void save(ToolIcon.Pen pen) {
        Path2D disk = new Path2D.Double();
        disk.moveTo(1.5, 1.5);
        disk.lineTo(12.5, 1.5);
        disk.lineTo(14.5, 3.5);
        disk.lineTo(14.5, 14.5);
        disk.lineTo(1.5, 14.5);
        disk.closePath();
        pen.fillAndDraw(disk, pen.ink());
        pen.fill(new Rectangle2D.Double(4, 2, 7, 4), pen.paper());
        pen.fill(new Rectangle2D.Double(3.5, 8.5, 9, 6), pen.paper());
        pen.fill(new Rectangle2D.Double(8, 2.5, 2, 3), pen.ink());
        pen.line(5, 10.5, 11, 10.5);
        pen.line(5, 12.5, 11, 12.5);
    }

    private static void print(ToolIcon.Pen pen) {
        pen.fillAndDraw(new Rectangle2D.Double(4.5, 1.5, 7, 5), pen.paper());
        pen.fillAndDraw(new Rectangle2D.Double(1.5, 6.5, 13, 6), pen.ink());
        pen.fillAndDraw(new Rectangle2D.Double(4.5, 10.5, 7, 4), pen.paper());
        pen.fill(new Rectangle2D.Double(11.5, 8, 1.5, 1.5), pen.paper());
    }

    private static void cut(ToolIcon.Pen pen) {
        pen.draw(new Ellipse2D.Double(1.5, 10.5, 4.5, 4.5));
        pen.draw(new Ellipse2D.Double(10, 10.5, 4.5, 4.5));
        pen.bold(1.4f);
        pen.line(5.3, 10.8, 11.5, 1.5);
        pen.line(10.7, 10.8, 4.5, 1.5);
        pen.thin();
    }

    private static void paste(ToolIcon.Pen pen) {
        pen.fillAndDraw(new Rectangle2D.Double(1.5, 2.5, 10, 12), pen.ink());
        pen.fillAndDraw(new Rectangle2D.Double(4.5, 1.5, 4, 2.5), pen.paper());
        pen.page(6.5, 5.5, 8, 9.5);
    }

    /** An arrow turning back to the left; redo is its mirror image. */
    private static void undo(ToolIcon.Pen pen) {
        pen.bold(1.8f);
        Path2D arc = new Path2D.Double();
        arc.moveTo(5, 6.5);
        arc.lineTo(9.5, 6.5);
        arc.curveTo(13.5, 6.5, 14.5, 9.5, 14, 11);
        arc.curveTo(13.5, 13, 11.5, 13.5, 9, 13.5);
        pen.draw(arc);
        pen.thin();
        pen.triangle(1, 6.5, 6, 2.5, 6, 10.5);
    }

    private static void refresh(ToolIcon.Pen pen) {
        pen.bold(1.8f);
        pen.draw(new Arc2D.Double(2.5, 2.5, 11, 11, 60, 270, Arc2D.OPEN));
        pen.thin();
        pen.triangle(10, 0.8, 14.8, 3.6, 10, 6.6);
    }
}
