package eu.amadeos.commons.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static eu.amadeos.commons.i18n.Texts.tr;
import static org.junit.jupiter.api.Assertions.*;

class TextsTest {
    private final Locale before = Locale.getDefault();

    @AfterEach
    void restore() {
        Locale.setDefault(before);
        Texts.use(TextsTest.class, "eu.amadeos.commons.i18n.texts-test");
    }

    // --8<-- [start:declared]
    @Test
    void theDeclaredTextsFollowTheDefaultLocale() {
        assertEquals("eu.amadeos.commons.i18n.texts-test", Texts.bundle());
        Locale.setDefault(Locale.ENGLISH);
        assertEquals("Hello", tr("greeting"));
        assertEquals("3 messages", tr("count", 3));
        Locale.setDefault(Locale.GERMAN);
        assertEquals("Hallo", tr("greeting"));
        assertEquals(Locale.GERMAN, Texts.messages().locale());
    }
    // --8<-- [end:declared]

    @Test
    void useSwitchesToOtherTexts() {
        Locale.setDefault(Locale.ENGLISH);
        Texts.use(Messages.class, "eu.amadeos.commons.i18n.commons");
        assertTrue(Texts.messages().has("language.label"));
        assertFalse(Texts.messages().has("greeting"));
    }

    @Test
    void withoutADeclarationTheErrorSaysWhatIsMissing() {
        ClassLoader empty = new ClassLoader(null) {
        };
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> Texts.declared(empty));
        assertTrue(e.getMessage().contains(Texts.DECLARATION), e.getMessage());
    }
}
