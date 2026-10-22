package eu.amadeos.commons.desktop;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

// --8<-- [start:class]
/**
 * A picture on the clipboard. Java offers it to other programs as PNG and other image types. When another
 * program puts something on the clipboard, {@code lost} runs; on X11 the picture is gone from then on.
 */
public record ImageSelection(Image image, Runnable lost) implements Transferable, ClipboardOwner {
    /** Puts the picture on the system clipboard. */
    public static void copy(Image image) {
        put(Toolkit.getDefaultToolkit().getSystemClipboard(), image, () -> { });
    }

    /** Puts the picture on {@code clipboard}; {@code lost} runs when another program takes the clipboard over. */
    public static void put(Clipboard clipboard, Image image, Runnable lost) {
        ImageSelection selection = new ImageSelection(image, lost);
        clipboard.setContents(selection, selection);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.imageFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
        return image;
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        lost.run();
    }
}
// --8<-- [end:class]
