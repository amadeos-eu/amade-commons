/**
 * Desktop widgets: small live views on the desktop of the Horizon look, such as a clock, the month of the
 * calendar or the song that is playing. A widget is a {@link eu.amadeos.commons.widget.DesktopWidget}; the
 * widget host of the desktop ({@code amadeos-widgets}) finds it, puts each placed copy on a
 * {@link eu.amadeos.commons.widget.WidgetCard} and hands it a {@link eu.amadeos.commons.widget.WidgetContext}
 * for its colours, its settings, a timer and starting its program. An application ships its widgets in its
 * own jar and announces them with a {@code .widget} file in {@code /usr/share/amadeos/widgets/}; during
 * development {@link eu.amadeos.commons.widget.WidgetPreview} shows a widget without the desktop.
 */
package eu.amadeos.commons.widget;
