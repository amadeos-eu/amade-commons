package eu.amadeos.commons.desktop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationsTest {
    @TempDir
    Path cache;

    /** Answers Notify with ids 1, 2, ...; records the calls; the monitor waits for lines from the test. */
    static final class FakeBus implements Notifications.Bus {
        final List<String> methods = new ArrayList<>();
        final List<List<String>> arguments = new ArrayList<>();
        final CountDownLatch monitorStarted = new CountDownLatch(1);
        boolean down;
        int next = 1;

        @Override
        public synchronized String call(String method, List<String> args) throws IOException {
            if (down) throw new IOException("The name org.freedesktop.Notifications was not provided");
            methods.add(method);
            arguments.add(args);
            return "Notify".equals(method) ? "(uint32 " + next++ + ",)\n" : "()\n";
        }

        @Override
        public void monitor(Consumer<String> line) {
            monitorStarted.countDown();
        }
    }

    private final FakeBus bus = new FakeBus();
    private final List<String> tray = new ArrayList<>();
    private final AtomicLong now = new AtomicLong(1_000_000);

    private Notifications notifications() {
        Notifications n = new Notifications(bus, (app, note) -> tray.add(app + ": " + note.summary()), cache, now::get);
        n.identify("Calendar", "amade-calendar");
        return n;
    }

    @Test
    void notifyGetsTheEightArgumentsInGVariantText() {
        Notifications n = notifications();
        long id = n.deliver(Notification.of("Computer club in 15 minutes")
                .body("Thursday 19:00\nlibrary, room 'two'")
                .icon("appointment-soon")
                .category("x-amade.reminder")
                .timeout(Duration.ofSeconds(10)));
        assertEquals(1, id);
        assertEquals(List.of("'Calendar'", "uint32 0", "'appointment-soon'", "'Computer club in 15 minutes'",
                "'Thursday 19:00\\nlibrary, room \\'two\\''", "@as []",
                "{'urgency': <byte 1>, 'desktop-entry': <'amade-calendar'>, 'category': <'x-amade.reminder'>, "
                        + "'sender-pid': <int64 " + ProcessHandle.current().pid() + ">}",
                "int32 10000"), bus.arguments.getFirst());
    }

    @Test
    void actionsAndTheDefaultActionBecomePairsAndStartTheMonitor() throws InterruptedException {
        Notifications n = notifications();
        n.deliver(Notification.of("New mail").action("read", "Mark as read", () -> { })
                .defaultAction(() -> { }).urgency(Notification.Urgency.LOW).transientOnly().replacing(4));
        List<String> args = bus.arguments.getFirst();
        assertEquals("uint32 4", args.get(1));
        assertEquals("['default', '', 'read', 'Mark as read']", args.get(5));
        assertTrue(args.get(6).contains("'urgency': <byte 0>"));
        assertTrue(args.get(6).contains("'transient': <true>"));
        assertEquals("int32 -1", args.get(7));
        assertTrue(bus.monitorStarted.await(5, TimeUnit.SECONDS), "the signal monitor runs");
    }

    // --8<-- [start:signals-test]
    @Test
    void anActionInvokedSignalRunsTheButtonsCode() {
        Notifications n = notifications();
        AtomicInteger snoozed = new AtomicInteger();
        AtomicInteger opened = new AtomicInteger();
        long id = n.deliver(Notification.of("Reminder").action("snooze", "Snooze 5 min", snoozed::incrementAndGet)
                .defaultAction(opened::incrementAndGet));
        n.onSignal("/org/freedesktop/Notifications: org.freedesktop.Notifications.ActionInvoked (uint32 " + id
                + ", 'snooze')");
        n.onSignal("/org/freedesktop/Notifications: org.freedesktop.Notifications.ActionInvoked (uint32 " + id
                + ", 'default')");
        n.onSignal("/org/freedesktop/Notifications: org.freedesktop.Notifications.ActionInvoked (uint32 99, 'snooze')");
        assertEquals(1, snoozed.get());
        assertEquals(1, opened.get());

        n.onSignal("/org/freedesktop/Notifications: org.freedesktop.Notifications.NotificationClosed (uint32 " + id
                + ", uint32 2)");
        n.onSignal("/org/freedesktop/Notifications: org.freedesktop.Notifications.ActionInvoked (uint32 " + id
                + ", 'snooze')");
        assertEquals(1, snoozed.get(), "a closed notification's buttons are forgotten");
    }
    // --8<-- [end:signals-test]

    @Test
    void closeCallsCloseNotification() {
        Notifications n = notifications();
        n.withdraw(7);
        assertEquals(List.of("CloseNotification"), bus.methods);
        assertEquals(List.of("uint32 7"), bus.arguments.getFirst());
    }

    @Test
    void withoutAServiceTheTrayShowsItAndTheServiceIsAskedAgainAMinuteLater() {
        Notifications n = notifications();
        bus.down = true;
        assertEquals(0, n.deliver(Notification.of("Backup finished")));
        bus.down = false;
        assertEquals(0, n.deliver(Notification.of("Second")));
        assertEquals(List.of("Calendar: Backup finished", "Calendar: Second"), tray);
        assertTrue(bus.methods.isEmpty(), "not asked again at once");

        now.addAndGet(Notifications.RETRY_MILLIS);
        assertEquals(1, n.deliver(Notification.of("Third")));
        assertEquals(2, tray.size());
    }

    @Test
    void anImageIconIsWrittenOnceToTheCache() {
        Notifications n = notifications();
        var image = new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        n.deliver(Notification.of("A").icon(image));
        n.deliver(Notification.of("B").icon(image));
        String expected = "'" + cache.resolve("amade-calendar.png") + "'";
        assertEquals(expected, bus.arguments.get(0).get(2));
        assertEquals(expected, bus.arguments.get(1).get(2));
        assertTrue(cache.resolve("amade-calendar.png").toFile().isFile());
    }

    @Test
    void quotingEscapesWhatGVariantTextNeeds() {
        assertEquals("'a\\\\b\\'c\\td'", Notifications.quote("a\\b'c\td"));
        assertEquals("it's", Notifications.unquote("it\\'s"));
        assertEquals("&lt;b&gt; &amp;", Notification.escape("<b> &"));
    }
}
