package eu.amadeos.commons.widget;

import javax.swing.JComponent;

/**
 * The settings page of a widget copy: the form the host shows, and what OK does with it.
 *
 * @param form  the fields
 * @param apply stores the values ({@link WidgetContext#set}) and updates the widget
 */
public record WidgetSettings(JComponent form, Runnable apply) {
}
