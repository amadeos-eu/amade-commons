package eu.amadeos.commons.widget;

import javax.swing.JComponent;
import java.awt.Dimension;

/**
 * A kind of desktop widget, such as the clock or the month of the calendar. The widget host may place it
 * more than once; each copy gets its own {@link WidgetContext} (and with it its own settings) and its own
 * component from {@link #create(WidgetContext)}.
 *
 * <p>A widget is found with {@link java.util.ServiceLoader}: its jar lists the class in
 * {@code META-INF/services/eu.amadeos.commons.widget.DesktopWidget}, and a {@code .widget} file in
 * {@code /usr/share/amadeos/widgets/} names the jar. The class needs a public constructor without
 * arguments. All methods are called on the Swing event thread.
 */
public interface DesktopWidget {
    /** A short, stable name, unique among all widgets: {@code clock}, {@code calendar-month}. */
    String id();

    /** The name in the gallery, in the language of the session. */
    String name();

    /** One line for the gallery: what the widget shows. */
    String description();

    /** The size of a new copy, in pixels; the user can make it larger or smaller on the desktop. */
    Dimension defaultSize();

    /**
     * The content of one copy. The host puts it on a {@link WidgetCard}, which paints the background, so the
     * component should not be opaque. Use {@link WidgetContext#every} for anything that changes with time.
     */
    JComponent create(WidgetContext context);

    /**
     * The settings of one copy, shown by the host in a dialog with OK and Cancel; null if the widget has none.
     * On OK the host calls {@code apply}, which should store the values with {@link WidgetContext#set}.
     */
    default WidgetSettings settings(WidgetContext context) {
        return null;
    }
}
