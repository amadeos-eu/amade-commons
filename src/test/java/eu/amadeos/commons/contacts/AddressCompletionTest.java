package eu.amadeos.commons.contacts;

import eu.amadeos.commons.i18n.Messages;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AddressCompletionTest {
    @Test
    void tokenIsTheAddressBetweenTheCommas() {
        assertEquals(new AddressCompletion.Token(0, 3, "ann"), AddressCompletion.token("ann", 3));
        String text = "anna@example.org, jö, x";
        assertEquals(new AddressCompletion.Token(17, 20, "jö"), AddressCompletion.token(text, 20));
        assertEquals("\"Berger, Anna\" <a", AddressCompletion.token("\"Berger, Anna\" <a", 17).text(),
                "a comma in quotes is part of the name");
        assertEquals("b", AddressCompletion.token("a; b", 4).text());
    }

    @Test
    void groupTextIsTranslated() {
        assertEquals("Gruppe, 4 Personen", Messages.commons(Locale.GERMAN).format("contacts.group-members", "4"));
    }

    @Test
    void acceptReplacesTheTypedAddressAndAddsAComma() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        Recipient anna = new Recipient("Anna Berger", "anna@example.org", List.of("Anna Berger <anna@example.org>"), false);
        Recipient choir = new Recipient("Chor", "2", List.of("a@example.org", "b@example.org"), true);
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            JTextField field = new JTextField(30);
            frame.add(field);
            frame.pack();
            AddressCompletion completion = AddressCompletion.install(field, typed -> List.of(anna, choir), Locale.ENGLISH);

            field.setText("bert@example.de, an");
            field.setCaretPosition(field.getText().length());
            completion.accept(anna);
            assertEquals("bert@example.de, Anna Berger <anna@example.org>, ", field.getText());
            assertEquals(field.getText().length(), field.getCaretPosition());

            field.setText("cho, bert@example.de");
            field.setCaretPosition(3);
            completion.accept(choir);
            assertEquals("a@example.org, b@example.org, bert@example.de", field.getText());
            assertFalse(completion.isShowing());
            frame.dispose();
        });
    }
}
