package eu.amadeos.commons.widget;

import eu.amadeos.commons.theme.DesktopTheme;
import eu.amadeos.commons.theme.ThemeStore;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Shows a widget in an ordinary window, without the desktop's widget host: for trying a widget while it is
 * written. The settings live only as long as the window; the right mouse button opens the settings.
 *
 * <pre>{@code
 * public static void main(String[] args) {
 *     WidgetPreview.show(new MonthWidget());
 * }
 * }</pre>
 */
public final class WidgetPreview {
    private WidgetPreview() {
    }

    public static void show(DesktopWidget widget) {
        SwingUtilities.invokeLater(() -> {
            ThemeStore store = ThemeStore.system();
            Supplier<DesktopTheme> theme = store::current;
            Context context = new Context(() -> WidgetColors.of(theme.get()));
            WidgetCard card = new WidgetCard(widget.create(context), context.colors);
            JFrame frame = new JFrame(widget.name());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(card);
            card.setPreferredSize(widget.defaultSize());
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    WidgetSettings s = widget.settings(context);
                    if (SwingUtilities.isRightMouseButton(e) && s != null
                            && JOptionPane.showConfirmDialog(frame, s.form(), widget.name(),
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                        s.apply().run();
                    }
                }
            });
            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }

    /** A context that keeps the settings in memory. */
    static final class Context implements WidgetContext {
        final Supplier<WidgetColors> colors;
        final Map<String, String> settings = new HashMap<>();
        final List<Timer> timers = new ArrayList<>();

        Context(Supplier<WidgetColors> colors) {
            this.colors = colors;
        }

        @Override
        public WidgetColors colors() {
            return colors.get();
        }

        @Override
        public Locale locale() {
            return Locale.getDefault();
        }

        @Override
        public String get(String key, String fallback) {
            return settings.getOrDefault(key, fallback);
        }

        @Override
        public void set(String key, String value) {
            settings.put(key, value);
        }

        @Override
        public void every(Duration period, Runnable task) {
            task.run();
            Timer t = new Timer((int) Math.max(50, period.toMillis()), e -> task.run());
            t.start();
            timers.add(t);
        }

        @Override
        public void onThemeChange(Runnable task) {
        }

        @Override
        public void open(String... command) {
            try {
                new ProcessBuilder(command).inheritIO().start();
            } catch (IOException e) {
                System.err.println("Cannot start " + String.join(" ", command) + ": " + e.getMessage());
            }
        }
    }
}
