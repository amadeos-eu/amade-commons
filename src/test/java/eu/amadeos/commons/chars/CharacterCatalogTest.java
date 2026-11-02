package eu.amadeos.commons.chars;

import eu.amadeos.commons.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class CharacterCatalogTest {
    private final CharacterCatalog english = CharacterCatalog.of(Locale.ENGLISH);
    private final CharacterCatalog german = CharacterCatalog.of(Locale.GERMAN);

    private static int cp(String s) {
        return s.codePointAt(0);
    }

    @Test
    void codesFindTheirCharacter() {
        assertEquals(List.of(0x20AC), english.search("U+20AC"));
        assertEquals(List.of(0x20AC), english.search("u+20ac"));
        assertEquals(0x2014, english.search("2014").getFirst());
        assertEquals(List.of(), english.search("U+D800"), "a surrogate is not offered");
    }

    @Test
    void englishNamesAndOwnWordsAreSearched() {
        assertEquals(cp("€"), english.search("euro").getFirst());
        List<Integer> dash = english.search("dash");
        assertTrue(dash.containsAll(List.of(0x2013, 0x2014)));
        assertTrue(english.search("a umlaut").contains(cp("ä")));
        assertTrue(english.search("em dash").contains(0x2014));
    }

    @Test
    void wordsOfTheUsersLanguageCountAndMarksDoNotMatter() {
        assertEquals(0x2013, german.search("gedankenstrich").getFirst());
        assertEquals(cp("€"), german.search("Wahrung").getFirst(), "Währung without the umlaut");
        assertEquals(cp("€"), german.search("euro").getFirst(), "English words work in German too");
        assertEquals("Gedankenstrich Halbgeviertstrich Bis-Strich", german.localWords(0x2013));
        assertEquals("", german.localWords(cp("q")));
        assertEquals("", english.localWords(0x2013), "English has the Unicode name already");
    }

    @Test
    void aLetterGivesTheLetterWithEveryMark() {
        List<Integer> e = english.search("e");
        assertTrue(e.containsAll(List.of(cp("é"), cp("è"), cp("ê"), cp("ë"), cp("ě"), cp("É"))));
        assertFalse(e.contains(cp("e")), "the plain letter is on the keyboard already");
        assertTrue(e.indexOf(cp("é")) < e.indexOf(cp("É")), "small letters first");
        assertTrue(english.search("s").contains(cp("ß")));
        assertTrue(english.search("o").contains(cp("ø")));
    }

    @Test
    void wordsMustStartAWord() {
        assertFalse(english.search("ash").contains(0x2014), "'ash' is inside 'dash' but starts no word");
        assertTrue(english.search("xqzv").isEmpty());
        assertTrue(english.search("   ").isEmpty());
    }

    @Test
    void categoriesAndAllCharacters() {
        assertTrue(english.characters(CharacterCatalog.Category.CURRENCY).contains(cp("€")));
        List<Integer> all = english.characters(CharacterCatalog.Category.ALL);
        assertTrue(all.size() > 50_000);
        assertFalse(all.contains(0x0A), "no control characters");
        assertFalse(all.contains(0xE000), "no private use");
        for (CharacterCatalog.Category c : CharacterCatalog.Category.values()) {
            for (int ch : c.characters()) assertTrue(CharacterCatalog.offered(ch), CharacterCatalog.code(ch));
        }
    }

    @Test
    void namesAreSentenceCase() {
        assertEquals("Latin small letter a with diaeresis", CharacterCatalog.name(cp("ä")));
        assertEquals("U+00E4", CharacterCatalog.code(cp("ä")));
    }

    @Test
    void everyCategoryHasATextInEveryLanguage() {
        for (Locale l : List.of(Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH, Locale.of("es"), Locale.ITALIAN)) {
            Messages m = Messages.commons(l);
            for (CharacterCatalog.Category c : CharacterCatalog.Category.values()) assertTrue(m.has(c.textKey()), c + " " + l);
            assertEquals(0x20AC, CharacterCatalog.of(l).search(l == Locale.ENGLISH ? "euro" : CharacterCatalog.of(l).localWords(0x20AC)).getFirst(),
                    "word list of " + l);
        }
        assertEquals("Sonderzeichen", Messages.commons(Locale.GERMAN).get("chars.title"));
        assertEquals("3 Zeichen gefunden", Messages.commons(Locale.GERMAN).format("chars.found", 3));
    }

    @Test
    void recentCharactersAreSharedNewestFirstAndLimited(@TempDir Path dir) {
        Path file = dir.resolve("amadeos/special-characters.properties");
        RecentCharacters recent = new RecentCharacters(file);
        assertEquals(cp(RecentCharacters.START), recent.list().getFirst(), "common characters while empty");
        recent.add("€");
        recent.add("—");
        recent.add("€");
        assertEquals(List.of(cp("€"), 0x2014), new RecentCharacters(file).list(), "another program sees the same list");
        for (int i = 0; i < 40; i++) recent.add(new String(Character.toChars(0x3B1 + i)));
        assertEquals(RecentCharacters.SIZE, recent.list().size());
        assertEquals(0x3B1 + 39, recent.list().getFirst());
    }
}
