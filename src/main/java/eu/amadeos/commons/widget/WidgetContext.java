package eu.amadeos.commons.widget;

import java.time.Duration;
import java.util.Locale;

/**
 * What the host gives one copy of a widget. Everything the context starts (timers, theme listeners) ends
 * when the copy is removed from the desktop or the host ends.
 */
// --8<-- [start:interface]
public interface WidgetContext {
    /** The colours of the card, from the current desktop colours; they change with the theme. */
    WidgetColors colors();

    /** The language of the session. */
    Locale locale();

    /** A setting of this copy, or {@code fallback}. */
    String get(String key, String fallback);

    /** Stores a setting of this copy; the host writes it to disk. */
    void set(String key, String value);

    /** Runs {@code task} on the Swing event thread now and then every {@code period}, while the copy lives. */
    void every(Duration period, Runnable task);

    /** Runs {@code task} on the Swing event thread when the desktop colours change. */
    void onThemeChange(Runnable task);

    /** Starts a program, such as the widget's application: {@code open("amade-calendar")}. */
    void open(String... command);
}
// --8<-- [end:interface]
