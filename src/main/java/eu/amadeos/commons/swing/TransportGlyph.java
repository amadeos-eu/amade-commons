package eu.amadeos.commons.swing;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

/**
 * The symbols of a player's transport buttons, as {@link ToolIcon} glyphs: filled shapes in the button's
 * text colour, the record dot in red. Their tooltips come from the application's texts under
 * {@link #key()}, because the same symbol means different things: Previous is the previous track in a
 * music player and the previous chapter in a video player.
 */
public enum TransportGlyph implements ToolIcon.Glyph {
    PREVIOUS, NEXT, REWIND, FORWARD, STOP, PLAY, PAUSE, RECORD, EJECT;

    /** The record dot; the inactive text colour when the button is disabled. */
    static final Color RECORD_RED = new Color(0xC8, 0x28, 0x1C);

    /** The key of the tooltip in the application's texts: {@code transport.play}, ... */
    public String key() {
        return "transport." + name().toLowerCase(Locale.ROOT);
    }

    // --8<-- [start:paint]
    @Override
    public void paint(ToolIcon.Pen pen) {
        switch (this) {
            case PREVIOUS -> {
                bar(pen, 2, 2, 2, 12);
                pen.triangle(14, 2, 14, 14, 5, 8);
            }
            case NEXT -> {
                pen.mirror();
                PREVIOUS.paint(pen);
            }
            case REWIND -> {
                pen.triangle(8, 2, 8, 14, 1, 8);
                pen.triangle(15, 2, 15, 14, 8, 8);
            }
            case FORWARD -> {
                pen.mirror();
                REWIND.paint(pen);
            }
            case STOP -> bar(pen, 3, 3, 10, 10);
            case PLAY -> pen.triangle(4, 2, 4, 14, 14, 8);
            case PAUSE -> {
                bar(pen, 3, 2, 4, 12);
                bar(pen, 9, 2, 4, 12);
            }
            case RECORD -> pen.fill(new Ellipse2D.Double(2, 2, 12, 12), pen.enabled() ? RECORD_RED : pen.ink());
            case EJECT -> {
                pen.triangle(8, 2, 15, 9, 1, 9);
                bar(pen, 1, 11, 14, 3);
            }
        }
    }
    // --8<-- [end:paint]

    private static void bar(ToolIcon.Pen pen, double x, double y, double w, double h) {
        pen.fill(new Rectangle2D.Double(x, y, w, h), pen.ink());
    }
}
