package eu.amadeos.commons.swing;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.theme.DesktopTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A dialog to pick a date: one month as a grid of days, buttons for the previous and next month and
 * year, Today, OK and Cancel. Arrow keys move the selection, Enter accepts, a double-click too.
 *
 * <pre>
 * DateChooser.choose(field, current, locale).ifPresent(field::setDate);
 * panel.add(DateChooser.button(field::date, field::setDate, () -> locale));   // the small icon next to a date field
 * </pre>
 */
public final class DateChooser extends JDialog {
    private final MonthGrid grid;
    private final JLabel title = new JLabel("", SwingConstants.CENTER);
    private final Locale locale;
    private final Messages texts;
    private LocalDate result;

    /** Opens the dialog over {@code parent}; empty if the user cancelled. */
    public static Optional<LocalDate> choose(Component parent, LocalDate initial, Locale locale) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        DateChooser chooser = new DateChooser(owner, initial, locale);
        chooser.setLocationRelativeTo(parent == null ? owner : parent);
        chooser.setVisible(true);
        return chooser.result();
    }

    /**
     * A small button with a calendar icon that opens the chooser on the current date of a field and
     * hands the chosen date back.
     */
    public static JButton button(Supplier<LocalDate> current, Consumer<LocalDate> chosen, Supplier<Locale> locale) {
        JButton button = new JButton(new CalendarIcon());
        button.setMargin(new Insets(1, 3, 1, 3));
        button.setFocusable(false);
        button.setToolTipText(Messages.commons(locale.get()).get("date.choose"));
        button.addActionListener(e -> {
            LocalDate start = current.get();
            choose(button, start == null ? LocalDate.now() : start, locale.get()).ifPresent(chosen);
        });
        return button;
    }

    DateChooser(Window owner, LocalDate initial, Locale locale) {
        super(owner, Messages.commons(locale).get("date.choose"), ModalityType.APPLICATION_MODAL);
        this.locale = locale;
        this.texts = Messages.commons(locale);
        grid = new MonthGrid(locale, this::accept);
        grid.setSelected(initial == null ? LocalDate.now() : initial);

        JPanel navigation = new JPanel(new BorderLayout(4, 0));
        navigation.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        left.setOpaque(false);
        left.add(navButton("◀◀", "date.previous-year", () -> step(-12)));
        left.add(navButton("◀", "date.previous-month", () -> step(-1)));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        right.setOpaque(false);
        right.add(navButton("▶", "date.next-month", () -> step(1)));
        right.add(navButton("▶▶", "date.next-year", () -> step(12)));
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        navigation.add(left, BorderLayout.WEST);
        navigation.add(title, BorderLayout.CENTER);
        navigation.add(right, BorderLayout.EAST);

        JButton today = new JButton(texts.get("date.today"));
        today.addActionListener(e -> {
            grid.setSelected(LocalDate.now());
            updateTitle();
        });
        JButton ok = new JButton(UIManager.getString("OptionPane.okButtonText"));
        ok.addActionListener(e -> accept(grid.selected()));
        JButton cancel = new JButton(UIManager.getString("OptionPane.cancelButtonText"));
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        buttons.add(today);
        buttons.add(ok);
        buttons.add(cancel);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        content.add(navigation, BorderLayout.NORTH);
        JPanel gridHolder = new JPanel(new BorderLayout());
        gridHolder.setBorder(AmadeUI.sunken());
        gridHolder.add(grid, BorderLayout.CENTER);
        content.add(gridHolder, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(ok);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        updateTitle();
        pack();
    }

    private JButton navButton(String text, String key, Runnable action) {
        JButton b = new JButton(text);
        b.setToolTipText(texts.get(key));
        b.setMargin(new Insets(1, 4, 1, 4));
        b.setFocusable(false);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void step(int months) {
        grid.showMonth(grid.month().plusMonths(months));
        updateTitle();
    }

    private void updateTitle() {
        YearMonth m = grid.month();
        title.setText(m.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, locale) + " " + m.getYear());
    }

    void accept(LocalDate date) {
        result = date;
        dispose();
    }

    Optional<LocalDate> result() {
        return Optional.ofNullable(result);
    }

    MonthGrid grid() {
        return grid;
    }

    /** One month as a grid of day numbers; the selection moves with the arrow keys. */
    static final class MonthGrid extends JComponent {
        private final Locale locale;
        private final Consumer<LocalDate> accept;
        private final DayOfWeek firstDay;
        private YearMonth month;
        private LocalDate selected;

        MonthGrid(Locale locale, Consumer<LocalDate> accept) {
            this.locale = locale;
            this.accept = accept;
            this.firstDay = WeekFields.of(locale).getFirstDayOfWeek();
            this.selected = LocalDate.now();
            this.month = YearMonth.from(selected);
            setOpaque(true);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    LocalDate date = dateAt(e.getX(), e.getY());
                    if (date == null) return;
                    setSelected(date);
                    if (e.getClickCount() == 2) accept.accept(date);
                }
            });
            bind("LEFT", -1);
            bind("RIGHT", 1);
            bind("UP", -7);
            bind("DOWN", 7);
            bind("PAGE_UP", -31);
            bind("PAGE_DOWN", 31);
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "accept");
            getActionMap().put("accept", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    accept.accept(selected);
                }
            });
        }

        private void bind(String key, int days) {
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), key);
            getActionMap().put(key, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setSelected(days == 31 || days == -31 ? selected.plusMonths(days / 31) : selected.plusDays(days));
                    Window w = SwingUtilities.getWindowAncestor(MonthGrid.this);
                    if (w instanceof DateChooser c) c.updateTitle();
                }
            });
        }

        YearMonth month() {
            return month;
        }

        LocalDate selected() {
            return selected;
        }

        void showMonth(YearMonth newMonth) {
            month = newMonth;
            if (!YearMonth.from(selected).equals(month)) {
                selected = month.atDay(Math.min(selected.getDayOfMonth(), month.lengthOfMonth()));
            }
            repaint();
        }

        void setSelected(LocalDate date) {
            selected = date;
            month = YearMonth.from(date);
            repaint();
        }

        /** The day at the grid position, counted from the first cell of the first row; null if empty. */
        static LocalDate dayAt(YearMonth month, DayOfWeek firstDay, int index) {
            int offset = (month.atDay(1).getDayOfWeek().getValue() - firstDay.getValue() + 7) % 7;
            int day = index - offset + 1;
            return day >= 1 && day <= month.lengthOfMonth() ? month.atDay(day) : null;
        }

        private int cellWidth() {
            return Math.max(getFontMetrics(getFont()).stringWidth("00") + 14, getWidth() / 7);
        }

        private int cellHeight() {
            return getFontMetrics(getFont()).getHeight() + 8;
        }

        private LocalDate dateAt(int x, int y) {
            int header = cellHeight();
            if (y < header) return null;
            int col = x / cellWidth();
            int row = (y - header) / cellHeight();
            if (col < 0 || col > 6 || row < 0 || row > 5) return null;
            return dayAt(month, firstDay, row * 7 + col);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(cellWidth() * 7, cellHeight() * 7);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            DesktopTheme theme = AmadeUI.theme();
            int w = cellWidth();
            int h = cellHeight();
            g.setColor(theme.fieldBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(theme.base());
            g.fillRect(0, 0, getWidth(), h);
            FontMetrics fm = g.getFontMetrics();
            g.setFont(getFont().deriveFont(Font.BOLD));
            for (int i = 0; i < 7; i++) {
                String name = firstDay.plus(i).getDisplayName(TextStyle.SHORT_STANDALONE, locale);
                g.setColor(theme.textActive());
                g.drawString(name, i * w + (w - g.getFontMetrics().stringWidth(name)) / 2, fm.getAscent() + 4);
            }
            g.setFont(getFont());
            LocalDate today = LocalDate.now();
            for (int index = 0; index < 42; index++) {
                LocalDate date = dayAt(month, firstDay, index);
                if (date == null) continue;
                int x = (index % 7) * w;
                int y = h + (index / 7) * h;
                boolean isSelected = date.equals(selected);
                if (isSelected) {
                    g.setColor(theme.selectionBackground());
                    g.fillRect(x, y, w, h);
                }
                if (date.equals(today)) {
                    g.setColor(isSelected ? theme.selectionText() : theme.fieldText());
                    g.drawRect(x + 1, y + 1, w - 3, h - 3);
                }
                g.setColor(isSelected ? theme.selectionText() : theme.fieldText());
                String s = Integer.toString(date.getDayOfMonth());
                g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + fm.getAscent() + 4);
            }
            if (hasFocus()) {
                g.setColor(theme.selectionBackground());
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
            g.dispose();
        }
    }

    /** A small desk calendar, drawn in the theme's colours. */
    static final class CalendarIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            DesktopTheme theme = AmadeUI.theme();
            g.setColor(theme.fieldBackground());
            g.fillRect(x + 1, y + 3, 12, 10);
            g.setColor(theme.activeTitleBg());
            g.fillRect(x + 1, y + 3, 12, 3);
            g.setColor(theme.textActive());
            g.drawRect(x + 1, y + 3, 12, 10);
            g.drawLine(x + 4, y + 1, x + 4, y + 4);
            g.drawLine(x + 10, y + 1, x + 10, y + 4);
            g.setColor(theme.fieldText());
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 4; col++) g.fillRect(x + 3 + col * 3, y + 8 + row * 3, 1, 1);
            }
            g.dispose();
        }

        @Override
        public int getIconWidth() {
            return 15;
        }

        @Override
        public int getIconHeight() {
            return 15;
        }
    }
}
