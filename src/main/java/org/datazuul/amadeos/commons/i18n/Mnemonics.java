package org.datazuul.amadeos.commons.i18n;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import java.awt.event.KeyEvent;

/**
 * Mnemonics inside translated texts: {@code &File}, {@code &Datei}, {@code &Fichier} mark the letter after
 * {@code &}, so that every language chooses its own. {@code &&} is a literal ampersand.
 *
 * <pre>{@code
 * Mnemonics.apply(new JMenu(), msg.get("menu.file"));   // menu.file=&File
 * }</pre>
 */
public final class Mnemonics {
    private Mnemonics() {
    }

    /** A text without its markers and the index of the mnemonic letter, -1 if it has none. */
    public record Text(String text, int index) {
        /** The key code for the mnemonic, {@link KeyEvent#VK_UNDEFINED} if there is none. */
        public int keyCode() {
            if (index < 0) return KeyEvent.VK_UNDEFINED;
            return KeyEvent.getExtendedKeyCodeForChar(text.charAt(index));
        }
    }

    public static Text parse(String marked) {
        StringBuilder text = new StringBuilder(marked.length());
        int index = -1;
        for (int i = 0; i < marked.length(); i++) {
            char c = marked.charAt(i);
            if (c == '&' && i + 1 < marked.length()) {
                char next = marked.charAt(++i);
                if (next != '&' && index < 0 && !Character.isWhitespace(next)) index = text.length();
                text.append(next);
            } else {
                text.append(c);
            }
        }
        return new Text(text.toString(), index);
    }

    /** The text without markers, e.g. for tooltips. */
    public static String strip(String marked) {
        return parse(marked).text();
    }

    /** Sets text and mnemonic of a button, menu or menu item; returns it for chaining. */
    public static <B extends AbstractButton> B apply(B button, String marked) {
        Text t = parse(marked);
        button.setText(t.text());
        button.setMnemonic(t.keyCode());
        if (t.index() >= 0) button.setDisplayedMnemonicIndex(t.index());
        return button;
    }

    /** Sets text and mnemonic of a label (pair it with {@link JLabel#setLabelFor}). */
    public static JLabel apply(JLabel label, String marked) {
        Text t = parse(marked);
        label.setText(t.text());
        label.setDisplayedMnemonic(t.keyCode());
        if (t.index() >= 0) label.setDisplayedMnemonicIndex(t.index());
        return label;
    }
}
