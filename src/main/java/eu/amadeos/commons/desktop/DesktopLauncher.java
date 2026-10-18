package eu.amadeos.commons.desktop;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * Opens links and files with the user's applications: {@code xdg-open} on amadeOS (AWT's
 * {@link Desktop} needs GNOME libraries there), {@link Desktop} elsewhere. The amadeOS window
 * manager shows the busy cursor meanwhile ({@link LaunchFeedback}).
 */
public class DesktopLauncher {
    /** Attachments with these extensions are never opened directly, only saved. */
    private static final Set<String> RISKY = Set.of("desktop", "sh", "bash", "run", "bin", "jar", "py", "pl",
            "appimage", "deb", "exe", "bat", "cmd", "com", "msi", "ps1", "vbs", "js", "jnlp", "lnk", "scr");

    public static boolean isRisky(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 || RISKY.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    public void browse(String uri) throws IOException {
        LaunchFeedback.announce();
        if (!xdgOpen(uri)) {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IOException("No web browser available");
            }
            Desktop.getDesktop().browse(URI.create(uri));
        }
    }

    public void open(Path file) throws IOException {
        LaunchFeedback.announce();
        if (!xdgOpen(file.toAbsolutePath().toString())) {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new IOException("No application available to open " + file.getFileName());
            }
            Desktop.getDesktop().open(file.toFile());
        }
    }

    private static boolean xdgOpen(String target) {
        try {
            new ProcessBuilder("xdg-open", target).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            return true;
        } catch (IOException e) {
            return false; // xdg-open not installed
        }
    }
}
