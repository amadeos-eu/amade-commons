package eu.amadeos.commons.desktop;

import java.awt.Image;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A desktop notification, sent with {@link Notifications#send}: a pop-up on the desktop that also goes into
 * the desktop's notification history. Built in one expression:
 *
 * <pre>
 * Notifications.send(Notification.of("Computer club in 15 minutes")
 *         .body("Thursday 19:00 to 21:00, library, room 2")
 *         .icon(window.getIconImage())
 *         .action("snooze", "Snooze 5 min", () -&gt; reminders.snooze(due, 5))
 *         .defaultAction(() -&gt; window.toFront()));
 * </pre>
 *
 * <p>The body may hold {@code <b>}, {@code <i>}, {@code <u>} and {@code <a href>}; everything else is
 * shown as text, so text from outside (a mail's subject) goes through {@link #escape} first.
 * Instances are immutable; every method returns a new one.
 */
public final class Notification {
    /** How urgent: low ones go into the history quietly, critical ones stay until closed. */
    public enum Urgency { LOW, NORMAL, CRITICAL }

    /** A button on the notification; its {@code run} is called on a background thread. */
    public record Action(String id, String label, Runnable run) {
        public Action {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(run, "run");
            if (id.isBlank() || "default".equals(id)) throw new IllegalArgumentException("Bad action id: " + id);
        }
    }

    private final String summary;
    private final String body;
    private final String iconName;
    private final Path iconFile;
    private final Image iconImage;
    private final List<Action> actions;
    private final Runnable defaultAction;
    private final Urgency urgency;
    private final Duration timeout;
    private final String category;
    private final boolean transientOnly;
    private final long replaces;

    private Notification(String summary, String body, String iconName, Path iconFile, Image iconImage,
                         List<Action> actions, Runnable defaultAction, Urgency urgency, Duration timeout,
                         String category, boolean transientOnly, long replaces) {
        this.summary = summary;
        this.body = body;
        this.iconName = iconName;
        this.iconFile = iconFile;
        this.iconImage = iconImage;
        this.actions = actions;
        this.defaultAction = defaultAction;
        this.urgency = urgency;
        this.timeout = timeout;
        this.category = category;
        this.transientOnly = transientOnly;
        this.replaces = replaces;
    }

    /** A notification with this summary, the one bold line. */
    public static Notification of(String summary) {
        if (summary == null || summary.isBlank()) throw new IllegalArgumentException("summary required");
        return new Notification(summary, "", "", null, null, List.of(), null, Urgency.NORMAL, null, "", false, 0);
    }

    /** The text under the summary; may be several lines and hold simple markup. */
    public Notification body(String body) {
        return new Notification(summary, body == null ? "" : body, iconName, iconFile, iconImage, actions,
                defaultAction, urgency, timeout, category, transientOnly, replaces);
    }

    /** An icon of the icon theme ({@code "mail-unread"}) or an absolute path to a picture. */
    public Notification icon(String nameOrPath) {
        return new Notification(summary, body, nameOrPath == null ? "" : nameOrPath, null, null, actions,
                defaultAction, urgency, timeout, category, transientOnly, replaces);
    }

    /** A picture file as the icon. */
    public Notification icon(Path file) {
        return new Notification(summary, body, "", file, null, actions, defaultAction, urgency, timeout,
                category, transientOnly, replaces);
    }

    /** The application's own icon, as a picture; it is written once to a small PNG in the cache. */
    public Notification icon(Image image) {
        return new Notification(summary, body, "", null, image, actions, defaultAction, urgency, timeout,
                category, transientOnly, replaces);
    }

    /** Adds a button. */
    public Notification action(String id, String label, Runnable run) {
        List<Action> more = new ArrayList<>(actions);
        more.removeIf(a -> a.id().equals(id));
        more.add(new Action(id, label, run));
        return new Notification(summary, body, iconName, iconFile, iconImage, Collections.unmodifiableList(more),
                defaultAction, urgency, timeout, category, transientOnly, replaces);
    }

    /** What a click on the notification itself does, usually bringing the window to the front. */
    public Notification defaultAction(Runnable run) {
        return new Notification(summary, body, iconName, iconFile, iconImage, actions, run, urgency, timeout,
                category, transientOnly, replaces);
    }

    public Notification urgency(Urgency urgency) {
        return new Notification(summary, body, iconName, iconFile, iconImage, actions, defaultAction,
                Objects.requireNonNull(urgency), timeout, category, transientOnly, replaces);
    }

    /** How long the pop-up stays; without it the desktop's setting holds. {@link Duration#ZERO}: until closed. */
    public Notification timeout(Duration timeout) {
        return new Notification(summary, body, iconName, iconFile, iconImage, actions, defaultAction, urgency,
                timeout, category, transientOnly, replaces);
    }

    /** A freedesktop category such as {@code "email.arrived"} or {@code "transfer.complete"}. */
    public Notification category(String category) {
        return new Notification(summary, body, iconName, iconFile, iconImage, actions, defaultAction, urgency,
                timeout, category == null ? "" : category, transientOnly, replaces);
    }

    /** Only a pop-up, not kept in the history (a "copied" or "saved" confirmation). */
    public Notification transientOnly() {
        return new Notification(summary, body, iconName, iconFile, iconImage, actions, defaultAction, urgency,
                timeout, category, true, replaces);
    }

    /** Takes the place of an earlier notification instead of showing a second one (a progress, a count). */
    public Notification replacing(long id) {
        return new Notification(summary, body, iconName, iconFile, iconImage, actions, defaultAction, urgency,
                timeout, category, transientOnly, id);
    }

    public String summary() {
        return summary;
    }

    public String body() {
        return body;
    }

    public String iconName() {
        return iconName;
    }

    public Path iconFile() {
        return iconFile;
    }

    public Image iconImage() {
        return iconImage;
    }

    public List<Action> actions() {
        return actions;
    }

    public Runnable defaultAction() {
        return defaultAction;
    }

    public Urgency urgency() {
        return urgency;
    }

    public Duration timeout() {
        return timeout;
    }

    public String category() {
        return category;
    }

    public boolean isTransient() {
        return transientOnly;
    }

    public long replaces() {
        return replaces;
    }

    /** Escapes {@code &}, {@code <} and {@code >}, so text from outside shows as it is. */
    public static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
