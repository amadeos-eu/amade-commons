package eu.amadeos.commons.swing;

import javax.swing.*;
import java.util.concurrent.Executor;

public final class EdtExecutor implements Executor {
    public static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Swing access must run on the EDT");
        }
    }

    @Override
    public void execute(Runnable command) {
        if (SwingUtilities.isEventDispatchThread()) command.run();
        else SwingUtilities.invokeLater(command);
    }
}
