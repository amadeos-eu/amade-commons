package eu.amadeos.commons.swing;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import java.awt.Insets;

/**
 * Square Motif buttons for toolbars, raised in 3D like the buttons of the Front Panel: icon only (the
 * action's {@link Action#LARGE_ICON_KEY}, usually a {@link ToolIcon}), tooltip from the action, never take
 * the focus, so the keyboard stays in the document. The bevel turns sunken while a button is pressed or a
 * switch is on. The button's name is the action command, so tests can find it.
 */
public final class ToolButtons {
    private ToolButtons() {
    }

    public static JButton button(Action action) {
        return configure(new JButton(action), action);
    }

    /** For actions with {@link Action#SELECTED_KEY}: pressed in while the switch is on. */
    public static JToggleButton toggle(Action action) {
        return configure(new JToggleButton(action), action);
    }

    // --8<-- [start:configure]
    private static <B extends AbstractButton> B configure(B button, Action action) {
        button.setHideActionText(true);
        button.setFocusable(false);
        button.setMargin(new Insets(2, 2, 2, 2));
        // Not a UIResource, so the toolbar's look and feel does not swap in its flat rollover border.
        button.setBorder(BorderFactory.createCompoundBorder(new AmadeUI.MotifButtonBorder(2),
                BorderFactory.createEmptyBorder()));
        button.setName(String.valueOf(action.getValue(Action.ACTION_COMMAND_KEY)));
        return button;
    }
    // --8<-- [end:configure]
}
