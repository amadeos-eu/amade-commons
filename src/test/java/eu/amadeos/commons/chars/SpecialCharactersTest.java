package eu.amadeos.commons.chars;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class SpecialCharactersTest {
    @Test
    void theMenuItemCarriesTheTextAndTheKey() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            JMenu edit = new JMenu("Edit");
            edit.add(new JMenuItem("Copy"));
            JMenuItem item = SpecialCharacters.install(frame, edit, Locale.GERMAN);
            assertEquals("Sonderzeichen…", item.getText());
            assertEquals(SpecialCharacters.KEY, item.getAccelerator());
            assertEquals(4, edit.getMenuComponentCount(), "Copy, separator, Sonderzeichen, Smileys und Emojis");
            JMenuItem emoji = (JMenuItem) edit.getMenuComponent(3);
            assertEquals("Smileys und Emojis…", emoji.getText());
            assertEquals(SpecialCharacters.EMOJI_KEY, emoji.getAccelerator());
            frame.dispose();
        });
    }

    @Test
    void theDialogSearchesAndInsertsIntoTheTarget(@TempDir Path dir) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            JTextArea text = new JTextArea("Preis: 49 ", 5, 20);
            frame.add(text);
            frame.pack();
            SpecialCharacters.setTarget(frame, text);
            text.setCaretPosition(text.getText().length());
            List<String> inserted = new ArrayList<>();
            SpecialCharactersDialog dialog = new SpecialCharactersDialog(frame, CharacterCatalog.of(Locale.ENGLISH),
                    new RecentCharacters(dir.resolve("recent.properties")), s -> {
                        inserted.add(s);
                        text.replaceSelection(s);
                        return true;
                    });
            dialog.searchField().setText("euro");
            assertEquals(0x20AC, dialog.grid().selectedCharacter());
            dialog.searchField().postActionEvent();
            assertEquals("Preis: 49 €", text.getText());
            assertEquals(List.of("€"), inserted);
            assertEquals(0x20AC, new RecentCharacters(dir.resolve("recent.properties")).list().getFirst());

            dialog.searchField().setText("");
            assertEquals(CharacterCatalog.Category.RECENT, dialog.categoryList().getSelectedValue());
            dialog.categoryList().setSelectedValue(CharacterCatalog.Category.GREEK, false);
            assertEquals(0x3B1, dialog.grid().selectedCharacter());

            dialog.open(CharacterCatalog.Category.EMOJI);
            assertEquals(CharacterCatalog.Category.EMOJI, dialog.categoryList().getSelectedValue(), "Smileys and Emoji… opens there");
            assertEquals(0x1F600, dialog.grid().selectedCharacter());
            dialog.setVisible(false);
            dialog.dispose();
            frame.dispose();
        });
    }

    @Test
    void emojiAreDrawnInTheEmojiFontWhenItIsInstalled() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        Font emoji = CharacterGrid.emojiFont(20);
        org.junit.jupiter.api.Assumptions.assumeTrue(emoji != null, "fonts-symbola is not installed");
        CharacterGrid grid = new CharacterGrid();
        assertEquals(CharacterGrid.EMOJI_FAMILY, grid.fontFor(0x1F600).getFamily());
        for (int cp : CharacterCatalog.Category.EMOJI.characters()) {
            assertTrue(emoji.canDisplay(cp), "Symbola draws " + CharacterCatalog.code(cp));
        }
        assertNotEquals(CharacterGrid.EMOJI_FAMILY, grid.fontFor('ä').getFamily(), "letters stay in the text's font");
    }

    @Test
    void invisibleCharactersGetAStandIn() {
        assertEquals("⍽", CharacterGrid.visible(0xA0));
        assertEquals("ä", CharacterGrid.visible(0xE4));
    }
}
