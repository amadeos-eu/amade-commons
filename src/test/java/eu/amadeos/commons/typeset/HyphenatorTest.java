package eu.amadeos.commons.typeset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyphenatorTest {
    // --8<-- [start:patterns]
    /** Liang's example patterns from the TeXbook, enough for "hyphenation". */
    static final String PATTERNS = """
            LEFTHYPHENMIN 2
            RIGHTHYPHENMIN 3
            hy3ph
            he2n
            hena4
            hen5at
            1na
            n2at
            1tio
            2io
            o2n
            """;

    @Test
    void breaksAsThePatternsSay() {
        Hyphenator h = Hyphenator.parse(PATTERNS);
        assertEquals("hy-phen-ation", h.show("hyphenation"));
        assertEquals(List.of(2, 6), h.points("Hyphenation"));
    }
    // --8<-- [end:patterns]

    @Test
    void shortWordsAndWordsWithDigitsStayWhole() {
        Hyphenator h = Hyphenator.parse(PATTERNS);
        assertEquals(List.of(), h.points("hyph"));
        assertEquals(List.of(), h.points("hyphen4tion"));
    }

    @Test
    void findsDictionariesByLanguage(@TempDir Path dir) throws Exception {
        Files.write(dir.resolve("hyph_de_DE.dic"), ("ISO8859-1\n" + PATTERNS).getBytes(StandardCharsets.ISO_8859_1));
        Hyphenators all = new Hyphenators(dir);
        assertTrue(all.forLanguage("de").isPresent());
        assertTrue(all.forLanguage("de-AT").isPresent());
        assertTrue(all.forLanguage("fr").isEmpty());
        assertTrue(all.forLanguage("").isEmpty());
    }

    @Test
    void readsTheInstalledDictionariesWhenThereAreAny() throws Exception {
        Path english = Hyphenators.SYSTEM.resolve("hyph_en_US.dic");
        if (!Files.isRegularFile(english)) return;
        Hyphenator h = Hyphenator.load(english);
        assertTrue(h.show("hyphenation").contains("-"));
    }
}
