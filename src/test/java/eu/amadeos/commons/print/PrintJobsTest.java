package eu.amadeos.commons.print;

import org.junit.jupiter.api.Test;

import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import java.awt.Graphics2D;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class PrintJobsTest {

    @Test
    void paperSizesAreInPoints() {
        assertEquals(595.28, PaperSize.A4.width(), 0.01);
        assertEquals(841.89, PaperSize.A4.height(), 0.01);
        assertEquals(612, PaperSize.LETTER.width(), 0.01);
        assertEquals(792, PaperSize.LETTER.height(), 0.01);
        assertEquals("A5", PaperSize.A5.toString());
    }

    @Test
    void letterInNorthAmericaA4Elsewhere() {
        assertEquals(PaperSize.LETTER, PaperSize.forLocale(Locale.US));
        assertEquals(PaperSize.LETTER, PaperSize.forLocale(Locale.CANADA_FRENCH));
        assertEquals(PaperSize.A4, PaperSize.forLocale(Locale.GERMANY));
        assertEquals(PaperSize.A4, PaperSize.forLocale(Locale.UK));
        assertEquals(PaperSize.A4, PaperSize.forLocale(Locale.ENGLISH), "no country");
    }

    @Test
    void portraitFormatWithMargins() {
        PageFormat format = PrintJobs.pageFormat(PaperSize.A4, false, 36);
        assertEquals(PageFormat.PORTRAIT, format.getOrientation());
        assertEquals(PaperSize.A4.width(), format.getWidth(), 0.01);
        assertEquals(36, format.getImageableX(), 0.01);
        assertEquals(PaperSize.A4.width() - 72, format.getImageableWidth(), 0.01);
    }

    // --8<-- [start:landscape]
    @Test
    void landscapeTurnsTheFormatButNotThePaper() {
        PageFormat format = PrintJobs.pageFormat(PaperSize.A5, true, 0);
        assertEquals(PageFormat.LANDSCAPE, format.getOrientation());
        assertEquals(PaperSize.A5.height(), format.getWidth(), 0.01);
        assertEquals(PaperSize.A5.width(), format.getHeight(), 0.01);
        assertEquals(PaperSize.A5.width(), format.getPaper().getWidth(), 0.01);
    }
    // --8<-- [end:landscape]

    @Test
    void marginMustFit() {
        assertThrows(IllegalArgumentException.class, () -> PrintJobs.pageFormat(PaperSize.A5, false, -1));
        assertThrows(IllegalArgumentException.class, () -> PrintJobs.pageFormat(PaperSize.A5, false, 300));
    }

    @Test
    void printerGetsThePaperButNoSecondRotation() {
        PrintRequestAttributeSet attributes = PrintJobs.attributes(PaperSize.A5, "test");
        assertEquals(MediaSizeName.ISO_A5, attributes.get(Media.class));
        assertNull(attributes.get(OrientationRequested.class));
    }

    // --8<-- [start:post-script]
    @Test
    void writesPostScriptWithoutAPrinter() throws Exception {
        Path file = Files.createTempFile("print-jobs-", ".ps");
        try {
            Book book = new Book();
            Printable page = (graphics, format, index) -> {
                ((Graphics2D) graphics).drawString("Hello", 100, 100);
                return Printable.PAGE_EXISTS;
            };
            book.append(page, PrintJobs.pageFormat(PaperSize.A4, false, 36), 2);
            PrintJobs.writePostScript(book, file, "test");
            String ps = Files.readString(file);
            assertTrue(ps.startsWith("%!PS"), ps.substring(0, Math.min(40, ps.length())));
            assertTrue(ps.contains("%%Page: 2 2") && !ps.contains("%%Page: 3"), "two pages");
        } finally {
            Files.deleteIfExists(file);
        }
    }
    // --8<-- [end:post-script]

    @Test
    void printerNamesNeverFail() {
        assertNotNull(PrintJobs.printerNames());
    }
}
