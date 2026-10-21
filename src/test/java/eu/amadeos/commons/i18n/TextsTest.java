package eu.amadeos.commons.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static eu.amadeos.commons.i18n.Texts.tr;
import static org.junit.jupiter.api.Assertions.*;

class TextsTest {
    private static final String BUNDLE = "eu.amadeos.commons.i18n.texts-test";
    private final Locale before = Locale.getDefault();

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(before);
    }

    @Test
    void textsFollowTheDefaultLocale() {
        Texts.use(TextsTest.class, BUNDLE);
        Locale.setDefault(Locale.ENGLISH);
        assertEquals("Hello", tr("greeting"));
        assertEquals("3 messages", tr("count", 3));
        Locale.setDefault(Locale.GERMAN);
        assertEquals("Hallo", tr("greeting"));
        assertEquals(Locale.GERMAN, Texts.messages().locale());
    }

    @Test
    void anotherBundleIsLoadedAfterUse() {
        Locale.setDefault(Locale.ENGLISH);
        Texts.use(TextsTest.class, BUNDLE);
        assertEquals("Hello", tr("greeting"));
        Texts.use(Messages.class, "eu.amadeos.commons.i18n.commons");
        assertTrue(Texts.messages().has("language.label"));
        assertFalse(Texts.messages().has("greeting"));
    }
}
