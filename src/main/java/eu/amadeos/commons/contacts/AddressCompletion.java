package eu.amadeos.commons.contacts;

import eu.amadeos.commons.i18n.Messages;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Address completion for a field that holds e-mail addresses separated by commas, such as To and Cc: while the
 * user types, a list below the field offers the matching contacts and groups. Up and Down choose, Enter or Tab
 * takes the suggestion (a group puts in all of its members), Escape closes the list; a click takes one too.
 *
 * <pre>
 * AddressCompletion.install(toField, ContactDirectory.of(xdg), locale);
 * </pre>
 */
public final class AddressCompletion {
    /** At most this many suggestions are shown. */
    public static final int LIMIT = 8;

    private final JTextComponent field;
    private final Function<String, List<Recipient>> suggestions;
    private final Messages texts;
    private final DefaultListModel<Recipient> model = new DefaultListModel<>();
    private final JList<Recipient> list = new JList<>(model);
    private final JPopupMenu popup = new JPopupMenu();
    private boolean replacing;

    private AddressCompletion(JTextComponent field, Function<String, List<Recipient>> suggestions, Locale locale) {
        this.field = Objects.requireNonNull(field, "field");
        this.suggestions = Objects.requireNonNull(suggestions, "suggestions");
        this.texts = Messages.commons(locale);
        list.setFocusable(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(l, describe((Recipient) value), index, selected, focus);
                setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
                return this;
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) accept(model.get(index));
            }
        });
        popup.setFocusable(false);
        popup.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow") == null
                ? Color.GRAY : UIManager.getColor("controlShadow")));
        popup.add(new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                key(e);
            }
        });
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hide();
            }
        });
        // Tab must reach the key listener while the list is open instead of moving the focus.
        field.setFocusTraversalKeysEnabled(false);
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB && !e.isConsumed()) {
                    if (e.isShiftDown()) field.transferFocusBackward();
                    else field.transferFocus();
                    e.consume();
                }
            }
        });
    }

    /** Completes from the address books; the first reading of them starts at once, in the background. */
    public static AddressCompletion install(JTextComponent field, ContactDirectory directory, Locale locale) {
        Thread.ofVirtual().name("contacts-preload").start(directory::contacts);
        return install(field, typed -> directory.complete(typed, LIMIT), locale);
    }

    /** Completes with {@code suggestions}, which gets the address being typed (already stripped). */
    public static AddressCompletion install(JTextComponent field, Function<String, List<Recipient>> suggestions,
                                            Locale locale) {
        return new AddressCompletion(field, suggestions, locale);
    }

    /** True while the list of suggestions is open. */
    public boolean isShowing() {
        return popup.isVisible();
    }

    /** The suggestions now in the list. */
    public List<Recipient> shown() {
        return java.util.Collections.list(model.elements());
    }

    // --8<-- [start:token]
    /** Where the address under the caret starts and ends: between the commas (or semicolons) around it. */
    record Token(int start, int end, String text) {
    }

    static Token token(String text, int caret) {
        int start = 0;
        boolean quoted = false;
        for (int i = 0; i < caret; i++) {
            char c = text.charAt(i);
            if (c == '"') quoted = !quoted;
            else if ((c == ',' || c == ';') && !quoted) start = i + 1;
        }
        int end = caret;
        while (end < text.length() && text.charAt(end) != ',' && text.charAt(end) != ';') end++;
        return new Token(start, end, text.substring(start, caret).strip());
    }
    // --8<-- [end:token]

    private void changed() {
        if (replacing) return;
        SwingUtilities.invokeLater(this::update);
    }

    private void update() {
        if (!field.isShowing() || !field.hasFocus()) {
            hide();
            return;
        }
        Token token = token(field.getText(), Math.min(field.getCaretPosition(), field.getText().length()));
        if (token.text().isEmpty() || token.text().contains("<")) {
            hide();
            return;
        }
        String lower = field.getText().toLowerCase(Locale.ROOT);
        model.clear();
        for (Recipient r : suggestions.apply(token.text())) {
            // An address that is in the field already is not offered again.
            if (!r.group() && lower.contains(r.address().toLowerCase(Locale.ROOT))
                    && !token.text().equalsIgnoreCase(r.address())) {
                continue;
            }
            model.addElement(r);
        }
        if (model.isEmpty()) {
            hide();
            return;
        }
        list.setSelectedIndex(0);
        list.setVisibleRowCount(Math.min(LIMIT, model.size()));
        int x = 0;
        try {
            Rectangle at = field.modelToView2D(token.start()).getBounds();
            x = Math.max(0, at.x - field.getInsets().left);
        } catch (BadLocationException | NullPointerException e) {
            // keep x = 0
        }
        popup.setPopupSize(Math.max(field.getWidth() - x, list.getPreferredScrollableViewportSize().width + 8),
                list.getPreferredScrollableViewportSize().height + 6);
        popup.show(field, x, field.getHeight());
    }

    private void key(KeyEvent e) {
        if (!popup.isVisible()) return;
        int index = list.getSelectedIndex();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN -> list.setSelectedIndex(Math.min(model.size() - 1, index + 1));
            case KeyEvent.VK_UP -> list.setSelectedIndex(Math.max(0, index - 1));
            case KeyEvent.VK_ENTER, KeyEvent.VK_TAB -> {
                if (index >= 0) accept(model.get(index));
            }
            case KeyEvent.VK_ESCAPE -> hide();
            default -> {
                return;
            }
        }
        list.ensureIndexIsVisible(list.getSelectedIndex());
        e.consume();
    }

    /** Puts the suggestion in place of the address being typed, followed by ", " for the next one. */
    void accept(Recipient recipient) {
        String text = field.getText();
        Token token = token(text, Math.min(field.getCaretPosition(), text.length()));
        String before = text.substring(0, token.start());
        String after = text.substring(token.end());
        String lead = before.isEmpty() || before.endsWith(" ") ? "" : " ";
        String insert = lead + recipient.text() + ", ";
        String rest = after.startsWith(",") || after.startsWith(";") ? after.substring(1).stripLeading() : after.stripLeading();
        replacing = true;
        try {
            field.setText(before + insert + rest);
            field.setCaretPosition(before.length() + insert.length());
        } finally {
            replacing = false;
        }
        hide();
    }

    private void hide() {
        popup.setVisible(false);
    }

    private String describe(Recipient r) {
        if (r.group()) return r.name() + " (" + texts.format("contacts.group-members", r.address()) + ")";
        return r.name().isEmpty() ? r.address() : r.name() + " <" + r.address() + ">";
    }
}
