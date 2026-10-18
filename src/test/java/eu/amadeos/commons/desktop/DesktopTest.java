package eu.amadeos.commons.desktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DesktopTest {
    @Test
    void riskyAttachmentsAreRecognised() {
        assertTrue(DesktopLauncher.isRisky("setup.sh"));
        assertTrue(DesktopLauncher.isRisky("Invoice.PDF.desktop"));
        assertTrue(DesktopLauncher.isRisky("tool.JAR"));
        assertTrue(DesktopLauncher.isRisky("README"), "no extension: unknown, treat as risky");
        assertFalse(DesktopLauncher.isRisky("report.pdf"));
        assertFalse(DesktopLauncher.isRisky("photo.jpeg"));
    }

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
}
