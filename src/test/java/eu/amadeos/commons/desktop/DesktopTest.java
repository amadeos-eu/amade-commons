package eu.amadeos.commons.desktop;

import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DesktopTest {
    // --8<-- [start:risky]
    @Test
    void riskyAttachmentsAreRecognised() {
        assertTrue(DesktopLauncher.isRisky("setup.sh"));
        assertTrue(DesktopLauncher.isRisky("Invoice.PDF.desktop"));
        assertTrue(DesktopLauncher.isRisky("tool.JAR"));
        assertTrue(DesktopLauncher.isRisky("README"), "no extension: unknown, treat as risky");
        assertFalse(DesktopLauncher.isRisky("report.pdf"));
        assertFalse(DesktopLauncher.isRisky("photo.jpeg"));
    }
    // --8<-- [end:risky]

    // --8<-- [start:session]
    @Test
    void withoutSecretToolPasswordsLiveForTheSession() {
        SecretToolPasswordStore store = new SecretToolPasswordStore("amade-test", null);
        assertFalse(store.isPersistent());
        assertTrue(store.lookup("a1").isEmpty());
        store.store("a1", "label", "secret");
        assertEquals("secret", store.lookup("a1").orElseThrow());
        store.delete("a1");
        assertTrue(store.lookup("a1").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new SecretToolPasswordStore(" "));
    }
    // --8<-- [end:session]

    // --8<-- [start:image]
    @Test
    void anImageOnTheClipboardKnowsWhenItIsReplaced() throws Exception {
        Clipboard clipboard = new Clipboard("test");
        BufferedImage picture = new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB);
        AtomicInteger lost = new AtomicInteger();
        ImageSelection.put(clipboard, picture, lost::incrementAndGet);
        assertSame(picture, clipboard.getData(DataFlavor.imageFlavor));
        assertFalse(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor));

        clipboard.setContents(new StringSelection("text"), null);
        EventQueue.invokeAndWait(() -> { }); // the clipboard tells the old owner on the event thread
        assertEquals(1, lost.get());
    }
    // --8<-- [end:image]
}
