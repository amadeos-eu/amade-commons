package eu.amadeos.commons.chars;

import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The Special Characters dialog of amade-commons: a window next to the document with accented letters, quotes,
 * dashes, currency signs, maths, arrows, Greek, symbols and all of Unicode, with a search. A double-click or Enter
 * puts the character at the cursor of the text field that last had the focus in the program's window.
 *
 * <pre>
 * // a menu item "Special Characters…" with Ctrl+Shift+U; inserts into the window's text fields
 * SpecialCharacters.install(window, editMenu, locale);
 *
 * // a program that inserts somewhere else, e.g. a terminal that types it into the shell
 * SpecialCharacters.open(window, text -> terminal.type(text), locale);
 * </pre>
 *
 * <p>The characters inserted last are shared by all programs ({@link RecentCharacters}).
 */
public final class SpecialCharacters {
    // --8<-- [start:key]
    /** Ctrl+Shift+U opens the dialog in every amade program. */
    public static final KeyStroke KEY = KeyStroke.getKeyStroke(KeyEvent.VK_U,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    // --8<-- [end:key]

    private static final String DIALOG = "amade.specialCharacters.dialog";
    private static final String TARGET = "amade.specialCharacters.target";

    private SpecialCharacters() {
    }

    // --8<-- [start:install]
    /**
     * Adds "Special Characters…" with {@link #KEY} to the menu (after a separator if the menu has items) and
     * returns the item. The dialog inserts into the editable text field of the window that had the focus last.
     */
    public static JMenuItem install(Window window, JMenu menu, Locale locale) {
        Action action = action(window, locale);
        if (menu.getMenuComponentCount() > 0) menu.addSeparator();
        JMenuItem item = menu.add(action);
        Mnemonics.apply(item, Messages.commons(locale).get("chars.menu"));
        return item;
    }
    // --8<-- [end:install]

    /**
     * The action behind the menu item, with name, mnemonic text and {@link #KEY}; for programs that place it
     * themselves (a toolbar, a context menu). It starts following the window's text fields.
     */
    public static Action action(Window window, Locale locale) {
        Objects.requireNonNull(window, "window");
        followFocus(window);
        Action action = new AbstractAction(Mnemonics.strip(Messages.commons(locale).get("chars.menu"))) {
            @Override
            public void actionPerformed(ActionEvent e) {
                open(window, text -> insertIntoTarget(window, text), locale);
            }
        };
        action.putValue(Action.ACCELERATOR_KEY, KEY);
        return action;
    }

    /**
     * Only the key {@link #KEY}, for a window without a fitting menu: the dialog inserts into the window's text
     * fields as with {@link #install(Window, JMenu, Locale)}.
     */
    public static void installKey(RootPaneContainer window, Locale locale) {
        Action action = action((Window) window, locale);
        JRootPane root = window.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KEY, "amade.specialCharacters");
        root.getActionMap().put("amade.specialCharacters", action);
    }

    // --8<-- [start:open]
    /**
     * Opens the dialog for the window (one per window, it is reused). Each inserted character is handed to
     * {@code insert}.
     */
    public static void open(Window owner, Consumer<String> insert, Locale locale) {
        open(owner, (Predicate<String>) text -> {
            insert.accept(text);
            return true;
        }, locale, null);
    }
    // --8<-- [end:open]

    private static void open(Window owner, Predicate<String> insert, Locale locale, Font textFont) {
        SpecialCharactersDialog dialog = dialogOf(owner, locale);
        dialog.setTarget(insert);
        JTextComponent target = target(owner);
        Font font = textFont != null ? textFont : target != null ? target.getFont() : null;
        if (font != null) dialog.setTextFont(font);
        dialog.open();
    }

    private static SpecialCharactersDialog dialogOf(Window owner, Locale locale) {
        JRootPane root = owner instanceof RootPaneContainer c ? c.getRootPane() : null;
        Object existing = root == null ? null : root.getClientProperty(DIALOG);
        if (existing instanceof SpecialCharactersDialog d) return d;
        SpecialCharactersDialog d = new SpecialCharactersDialog(owner, CharacterCatalog.of(locale),
                RecentCharacters.of(XdgDirectories.system()), text -> false);
        if (root != null) root.putClientProperty(DIALOG, d);
        CharacterCatalog.warmUp();
        return d;
    }

    private static boolean insertIntoTarget(Window window, String text) {
        JTextComponent target = target(window);
        if (target == null || !target.isEditable() || !target.isEnabled() || !target.isShowing()) return false;
        target.replaceSelection(text);
        return true;
    }

    // --8<-- [start:focus]
    /** Remembers the editable text field of the window that had the focus last, to insert into it. */
    private static void followFocus(Window window) {
        JRootPane root = window instanceof RootPaneContainer c ? c.getRootPane() : null;
        if (root == null || root.getClientProperty(TARGET + ".listener") != null) return;
        PropertyChangeListener listener = e -> {
            if (e.getNewValue() instanceof JTextComponent t && t.isEditable()
                    && SwingUtilities.getWindowAncestor(t) == window) {
                root.putClientProperty(TARGET, new WeakReference<>(t));
            }
        };
        KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focus.addPropertyChangeListener("permanentFocusOwner", listener);
        root.putClientProperty(TARGET + ".listener", listener);
        // A closed window lets go of the focus manager and of its dialog (Mail opens many compose windows)
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                focus.removePropertyChangeListener("permanentFocusOwner", listener);
                root.putClientProperty(TARGET + ".listener", null);
                root.putClientProperty(TARGET, null);
                if (root.getClientProperty(DIALOG) instanceof Window d) d.dispose();
                root.putClientProperty(DIALOG, null);
                window.removeWindowListener(this);
            }
        });
        Component now = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        if (now instanceof JTextComponent t && SwingUtilities.getWindowAncestor(t) == window) {
            root.putClientProperty(TARGET, new WeakReference<>(t));
        }
    }
    // --8<-- [end:focus]

    /** The text field the dialog inserts into for the window, null if none had the focus yet. */
    static JTextComponent target(Window window) {
        JRootPane root = window instanceof RootPaneContainer c ? c.getRootPane() : null;
        if (root == null) return null;
        Object ref = root.getClientProperty(TARGET);
        return ref instanceof WeakReference<?> w && w.get() instanceof JTextComponent t ? t : null;
    }

    /** Sets the text field to insert into, e.g. when a program creates its editor before the window is shown. */
    public static void setTarget(Window window, JTextComponent target) {
        followFocus(window);
        if (window instanceof RootPaneContainer c) c.getRootPane().putClientProperty(TARGET, new WeakReference<>(target));
    }
}
