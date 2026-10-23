package eu.amadeos.commons.swing;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The toolbar glyphs of the commands most applications share. Glyphs only one application needs stay in
 * that application, as an enum of its own that implements {@link ToolIcon.Glyph}.
 */
public enum StandardGlyph implements ToolIcon.Glyph {
    NEW, OPEN, SAVE, PRINT, CUT, COPY, PASTE, UNDO, REDO, FIND, ZOOM_IN, ZOOM_OUT, REFRESH,
    EDIT, DELETE, IMPORT, EXPORT, MAIL,
    ZOOM_FIT, ZOOM_ACTUAL, ROTATE_LEFT, ROTATE_RIGHT, FULL_SCREEN, LEAVE_FULL_SCREEN;

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
            case EDIT -> edit(pen);
            case DELETE -> delete(pen);
            case IMPORT -> tray(pen, true);
            case EXPORT -> tray(pen, false);
            case MAIL -> mail(pen);
            case ZOOM_FIT -> zoomFit(pen);
            case ZOOM_ACTUAL -> {
                pen.magnifier(6.5, 6.5, 5);
                pen.g().setColor(pen.ink());
                pen.g().setFont(pen.g().getFont().deriveFont(Font.BOLD, 6f));
                pen.g().drawString("1:1", 3.2f, 8.6f);
            }
            case ROTATE_LEFT -> rotate(pen);
            case ROTATE_RIGHT -> {
                pen.mirror();
                rotate(pen);
            }
            case FULL_SCREEN -> fullScreen(pen);
            case LEAVE_FULL_SCREEN -> {
                pen.fillAndDraw(new Rectangle2D.Double(1.5, 2.5, 13, 11), pen.paper());
                pen.fillAndDraw(new Rectangle2D.Double(5.5, 6.5, 5, 3), pen.light());
            }
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

    /** A pencil writing on a sheet. */
    private static void edit(ToolIcon.Pen pen) {
        pen.page(1.5, 1.5, 9, 12);
        Path2D pencil = new Path2D.Double();
        pencil.moveTo(6, 13.5);
        pencil.lineTo(6.8, 10.6);
        pencil.lineTo(13.2, 4.2);
        pencil.lineTo(15.3, 6.3);
        pencil.lineTo(8.9, 12.7);
        pencil.closePath();
        pen.fillAndDraw(pencil, pen.light());
        pen.line(12.2, 5.2, 14.3, 7.3);
    }

    /** A waste bin with a lid. */
    private static void delete(ToolIcon.Pen pen) {
        Path2D bin = new Path2D.Double();
        bin.moveTo(3.5, 4.5);
        bin.lineTo(4.5, 14.5);
        bin.lineTo(11.5, 14.5);
        bin.lineTo(12.5, 4.5);
        bin.closePath();
        pen.fillAndDraw(bin, pen.paper());
        pen.line(6.5, 6.5, 6.5, 12.5);
        pen.line(9.5, 6.5, 9.5, 12.5);
        pen.fillAndDraw(new Rectangle2D.Double(1.5, 2.5, 13, 2), pen.ink());
        pen.draw(new Rectangle2D.Double(6.5, 0.5, 3, 2));
    }

    // --8<-- [start:tray]
    /** An open tray with an arrow: into it for import, out of it for export. */
    private static void tray(ToolIcon.Pen pen, boolean in) {
        Path2D tray = new Path2D.Double();
        tray.moveTo(1.5, 9.5);
        tray.lineTo(1.5, 14.5);
        tray.lineTo(14.5, 14.5);
        tray.lineTo(14.5, 9.5);
        tray.lineTo(11.5, 9.5);
        tray.lineTo(10.5, 11.5);
        tray.lineTo(5.5, 11.5);
        tray.lineTo(4.5, 9.5);
        tray.closePath();
        pen.fillAndDraw(tray, pen.paper());
        pen.bold(2f);
        pen.line(8, in ? 0.5 : 10, 8, in ? 6 : 4.5);
        pen.thin();
        if (in) {
            pen.triangle(4.5, 5.5, 11.5, 5.5, 8, 10);
        } else {
            pen.triangle(4.5, 5, 11.5, 5, 8, 0.5);
        }
    }
    // --8<-- [end:tray]

    /** A closed envelope. */
    private static void mail(ToolIcon.Pen pen) {
        pen.fillAndDraw(new Rectangle2D.Double(1.5, 3.5, 13, 9), pen.paper());
        Path2D flap = new Path2D.Double();
        flap.moveTo(1.5, 3.5);
        flap.lineTo(8, 9);
        flap.lineTo(14.5, 3.5);
        pen.draw(flap);
        pen.line(1.5, 12.5, 6.3, 7.6);
        pen.line(14.5, 12.5, 9.7, 7.6);
    }

    /** Four corner marks around the picture: it grows to fill the window. */
    private static void zoomFit(ToolIcon.Pen pen) {
        pen.g().setStroke(new BasicStroke(1.4f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        double[][] corners = {{1.5, 1.5, 1, 1}, {14.5, 1.5, -1, 1}, {14.5, 14.5, -1, -1}, {1.5, 14.5, 1, -1}};
        for (double[] c : corners) {
            Path2D mark = new Path2D.Double();
            mark.moveTo(c[0], c[1] + 4 * c[3]);
            mark.lineTo(c[0], c[1]);
            mark.lineTo(c[0] + 4 * c[2], c[1]);
            pen.draw(mark);
        }
        pen.thin();
        pen.fillAndDraw(new Rectangle2D.Double(4.5, 4.5, 7, 7), pen.paper());
    }

    /** A sheet with a curved arrow over it, turning left; the right turn is its mirror image. */
    private static void rotate(ToolIcon.Pen pen) {
        pen.page(5.5, 5.5, 8, 9);
        pen.bold(1.6f);
        pen.draw(new Arc2D.Double(2, 2.5, 9, 9, 180, -100, Arc2D.OPEN));
        pen.thin();
        pen.triangle(0.5, 6, 4.5, 6, 2.5, 9.5);
    }

    /** A screen with arrows into its four corners. */
    private static void fullScreen(ToolIcon.Pen pen) {
        pen.fillAndDraw(new Rectangle2D.Double(1.5, 2.5, 13, 11), pen.paper());
        pen.g().setStroke(new BasicStroke(1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        double[][] corners = {{4, 5, 1, 1}, {12, 5, -1, 1}, {4, 11, 1, -1}, {12, 11, -1, -1}};
        for (double[] c : corners) {
            pen.line(c[0], c[1], c[0] + 2.5 * c[2], c[1]);
            pen.line(c[0], c[1], c[0], c[1] + 2 * c[3]);
            pen.line(c[0], c[1], c[0] + 2 * c[2], c[1] + 1.6 * c[3]);
        }
        pen.thin();
    }
}
