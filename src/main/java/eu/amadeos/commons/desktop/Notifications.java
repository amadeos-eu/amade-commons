package eu.amadeos.commons.desktop;

import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.process.CommandRunner;
import eu.amadeos.commons.process.ProgramResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends {@link Notification}s to the desktop. On amadeOS, and on every Linux desktop, that is the
 * freedesktop.org notification service ({@code org.freedesktop.Notifications} on the session bus), reached
 * through the {@code gdbus} command of GLib (package {@code libglib2.0-bin}), so commons needs no D-Bus
 * library. Buttons and clicks come back through {@code gdbus monitor}, started the first time a notification
 * has an action. Without a notification service (Windows, macOS, a bare X session), the message goes to the
 * system tray, if there is one, and otherwise only to the log.
 *
 * <pre>
 * long id = Notifications.send(Notification.of("Backup finished").body("3 412 files, 1.2 GB"));
 * Notifications.close(id);
 * </pre>
 *
 * <p>{@link eu.amadeos.commons.app.AmadeApplication#launch} calls {@link #install} with the application's
 * name, so the desktop shows the right program and applies its per-program setting. Sending never throws
 * and never blocks the event dispatch thread for long: the call to the service has a time limit of 3 s.
 */
public final class Notifications {
    private static final Logger LOG = LoggerFactory.getLogger(Notifications.class);

    static final String SERVICE = "org.freedesktop.Notifications";
    static final String PATH = "/org/freedesktop/Notifications";
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(3);
    /** Callbacks are kept for this many notifications; older ones are forgotten. */
    static final int KEEP_CALLBACKS = 64;

    private static final Pattern ID = Pattern.compile("\\(uint32 (\\d+),?\\)");
    private static final Pattern ACTION_INVOKED =
            Pattern.compile(".*\\.Notifications\\.ActionInvoked \\(uint32 (\\d+), '((?:[^'\\\\]|\\\\.)*)'\\)");
    private static final Pattern CLOSED =
            Pattern.compile(".*\\.Notifications\\.NotificationClosed \\(uint32 (\\d+), uint32 (\\d+)\\)");

    /** The session bus, as far as notifications need it; a fake in the tests. */
    interface Bus {
        /** Calls a method; returns gdbus's answer, such as {@code (uint32 7,)}. */
        String call(String method, List<String> arguments) throws IOException;

        /** Delivers every signal of the notification service, one line each, until the program ends. */
        void monitor(Consumer<String> line) throws IOException;
    }

    /** Where a notification goes when there is no notification service. */
    interface Fallback {
        void show(String appName, Notification notification);
    }

    private static volatile Notifications instance;

    private final Bus bus;
    private final Fallback fallback;
    private final Path iconCache;
    private final Map<Long, Notification> callbacks = new LinkedHashMap<>(16, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Notification> eldest) {
            return size() > KEEP_CALLBACKS;
        }
    };
    private volatile String appName = "";
    private volatile String desktopEntry = "";
    private boolean monitoring;
    /** When the service last failed to answer; it is asked again a minute later (it may restart). */
    private volatile long missingSince = Long.MIN_VALUE;
    static final long RETRY_MILLIS = 60_000;
    private final java.util.function.LongSupplier clock;

    Notifications(Bus bus, Fallback fallback, Path iconCache) {
        this(bus, fallback, iconCache, System::currentTimeMillis);
    }

    Notifications(Bus bus, Fallback fallback, Path iconCache, java.util.function.LongSupplier clock) {
        this.clock = clock;
        this.bus = bus;
        this.fallback = fallback;
        this.iconCache = iconCache;
    }

    private static Notifications instance() {
        Notifications n = instance;
        if (n == null) {
            synchronized (Notifications.class) {
                n = instance;
                if (n == null) {
                    instance = n = new Notifications(new GdbusBus(CommandRunner.system()), new TrayFallback(),
                            XdgDirectories.system().cacheHome().resolve("amadeos").resolve("notification-icons"));
                }
            }
        }
        return n;
    }

    // --8<-- [start:api]
    /** Names the program for every notification it sends; {@code AmadeApplication.launch} does this. */
    public static void install(AppInfo app) {
        instance().identify(app.title(), app.id());
    }

    /** Sends a notification; returns its id, for {@link #close} and {@link Notification#replacing}, or 0. */
    public static long send(Notification notification) {
        return instance().deliver(notification);
    }

    /** Takes a notification off the screen and out of the history (a reminder that was dealt with). */
    public static void close(long id) {
        instance().withdraw(id);
    }
    // --8<-- [end:api]

    void identify(String name, String entry) {
        appName = name == null ? "" : name;
        desktopEntry = entry == null ? "" : entry;
    }

    long deliver(Notification n) {
        if (!serviceMissing()) {
            try {
                if (hasCallbacks(n)) startMonitor();
                String answer = bus.call("Notify", notifyArguments(n));
                Matcher m = ID.matcher(answer.strip());
                if (m.matches()) {
                    long id = Long.parseLong(m.group(1));
                    if (hasCallbacks(n)) {
                        synchronized (callbacks) {
                            callbacks.put(id, n);
                        }
                    }
                    return id;
                }
                LOG.debug("Unexpected answer from the notification service: {}", answer);
            } catch (IOException e) {
                // No session bus, no service, no gdbus: the tray from now on.
                if (missingSince == Long.MIN_VALUE) {
                    LOG.info("No desktop notification service ({}); using the system tray", e.getMessage());
                }
                missingSince = clock.getAsLong();
            }
        }
        fallback.show(appName, n);
        return 0;
    }

    void withdraw(long id) {
        if (id <= 0 || serviceMissing()) return;
        synchronized (callbacks) {
            callbacks.remove(id);
        }
        try {
            bus.call("CloseNotification", List.of("uint32 " + id));
        } catch (IOException e) {
            LOG.debug("Could not close notification {}", id, e);
        }
    }

    private boolean serviceMissing() {
        long since = missingSince;
        return since != Long.MIN_VALUE && clock.getAsLong() - since < RETRY_MILLIS;
    }

    private static boolean hasCallbacks(Notification n) {
        return n.defaultAction() != null || !n.actions().isEmpty();
    }

    // --8<-- [start:arguments]
    /** The eight arguments of Notify, in GVariant's text form, which gdbus parses. */
    List<String> notifyArguments(Notification n) {
        List<String> actions = new ArrayList<>();
        if (n.defaultAction() != null) {
            actions.add(quote("default"));
            actions.add(quote(""));
        }
        for (Notification.Action a : n.actions()) {
            actions.add(quote(a.id()));
            actions.add(quote(a.label()));
        }
        List<String> hints = new ArrayList<>();
        hints.add("'urgency': <byte " + n.urgency().ordinal() + ">");
        if (!desktopEntry.isEmpty()) hints.add("'desktop-entry': <" + quote(desktopEntry) + ">");
        if (!n.category().isEmpty()) hints.add("'category': <" + quote(n.category()) + ">");
        if (n.isTransient()) hints.add("'transient': <true>");
        // gdbus call ends at once; the process id tells the desktop that the buttons work while this program runs.
        hints.add("'sender-pid': <int64 " + ProcessHandle.current().pid() + ">");
        int timeout = n.timeout() == null ? -1 : (int) Math.min(Integer.MAX_VALUE, n.timeout().toMillis());
        return List.of(
                quote(appName),
                "uint32 " + Math.max(0, n.replaces()),
                quote(icon(n)),
                quote(n.summary()),
                quote(n.body()),
                actions.isEmpty() ? "@as []" : "[" + String.join(", ", actions) + "]",
                "{" + String.join(", ", hints) + "}",
                "int32 " + timeout);
    }
    // --8<-- [end:arguments]

    private String icon(Notification n) {
        if (n.iconFile() != null) return n.iconFile().toAbsolutePath().toString();
        if (n.iconImage() != null) return cachedIcon(n.iconImage());
        return n.iconName();
    }

    /** The application's icon as a PNG of 64 pixels in the cache, written once per program. */
    private String cachedIcon(Image image) {
        String name = (desktopEntry.isEmpty() ? "application" : desktopEntry).replaceAll("[^A-Za-z0-9._-]", "_");
        Path file = iconCache.resolve(name + ".png");
        try {
            if (!Files.isRegularFile(file)) {
                BufferedImage scaled = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                var g = scaled.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image, 0, 0, 64, 64, null);
                g.dispose();
                Files.createDirectories(iconCache);
                Path part = Files.createTempFile(iconCache, name, ".part");
                ImageIO.write(scaled, "png", part.toFile());
                Files.move(part, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            }
            return file.toString();
        } catch (IOException | RuntimeException e) {
            LOG.debug("Could not write the notification icon", e);
            return "";
        }
    }

    /** A GVariant string: single quotes, with backslash, quote and control characters escaped. */
    static String quote(String text) {
        StringBuilder sb = new StringBuilder(text.length() + 2).append('\'');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\'' -> sb.append("\\'");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('\'').toString();
    }

    static String unquote(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) c = text.charAt(++i);
            sb.append(c);
        }
        return sb.toString();
    }

    private synchronized void startMonitor() {
        if (monitoring) return;
        monitoring = true;
        Thread.ofVirtual().name("notification-signals").start(() -> {
            try {
                bus.monitor(this::onSignal);
            } catch (IOException e) {
                LOG.debug("No notification signals; buttons will do nothing", e);
            }
            synchronized (this) {
                monitoring = false;
            }
        });
    }

    // --8<-- [start:signals]
    /** One line of {@code gdbus monitor}: runs the button's code, or forgets a closed notification. */
    void onSignal(String line) {
        Matcher invoked = ACTION_INVOKED.matcher(line);
        if (invoked.matches()) {
            long id = Long.parseLong(invoked.group(1));
            String action = unquote(invoked.group(2));
            Notification n;
            synchronized (callbacks) {
                n = callbacks.get(id);
            }
            if (n == null) return;
            Runnable run = "default".equals(action) ? n.defaultAction() : n.actions().stream()
                    .filter(a -> a.id().equals(action)).map(Notification.Action::run).findFirst().orElse(null);
            if (run != null) run(run);
            return;
        }
        Matcher closed = CLOSED.matcher(line);
        if (closed.matches()) {
            synchronized (callbacks) {
                callbacks.remove(Long.parseLong(closed.group(1)));
            }
        }
    }
    // --8<-- [end:signals]

    private static void run(Runnable run) {
        try {
            run.run();
        } catch (RuntimeException e) {
            LOG.warn("A notification's action failed", e);
        }
    }

    /** gdbus of GLib: {@code gdbus call} for the methods, {@code gdbus monitor} for the signals. */
    static final class GdbusBus implements Bus {
        private final CommandRunner runner;

        GdbusBus(CommandRunner runner) {
            this.runner = runner;
        }

        @Override
        public String call(String method, List<String> arguments) throws IOException {
            List<String> command = new ArrayList<>(List.of("gdbus", "call", "--session", "--dest", SERVICE,
                    "--object-path", PATH, "--method", SERVICE + "." + method, "--"));
            command.addAll(arguments);
            ProgramResult result;
            try {
                result = runner.run(command, CALL_TIMEOUT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
            }
            if (!result.succeeded()) throw new IOException(result.failure("gdbus"));
            return result.stdoutText();
        }

        @Override
        public void monitor(Consumer<String> line) throws IOException {
            Process process = runner.start(List.of("gdbus", "monitor", "--session", "--dest", SERVICE,
                    "--object-path", PATH));
            process.getOutputStream().close();
            Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String text;
                while ((text = in.readLine()) != null) line.accept(text);
            }
        }
    }

    /** The system tray's message, for desktops without a notification service. */
    static final class TrayFallback implements Fallback {
        private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notification-tray");
            t.setDaemon(true);
            return t;
        });

        @Override
        public void show(String appName, Notification n) {
            if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
                LOG.info("Notification: {} {}", n.summary(), n.body());
                return;
            }
            Image image = n.iconImage() != null ? n.iconImage() : new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            TrayIcon icon = new TrayIcon(image, appName.isEmpty() ? n.summary() : appName);
            icon.setImageAutoSize(true);
            if (n.defaultAction() != null) icon.addActionListener(e -> run(n.defaultAction()));
            try {
                SystemTray.getSystemTray().add(icon);
            } catch (AWTException | RuntimeException e) {
                LOG.info("Notification: {} {}", n.summary(), n.body());
                return;
            }
            TrayIcon.MessageType type = switch (n.urgency()) {
                case LOW -> TrayIcon.MessageType.NONE;
                case NORMAL -> TrayIcon.MessageType.INFO;
                case CRITICAL -> TrayIcon.MessageType.WARNING;
            };
            icon.displayMessage(n.summary(), n.body().replaceAll("<[^>]*>", ""), type);
            timer.schedule(() -> SystemTray.getSystemTray().remove(icon), 12, TimeUnit.SECONDS);
        }
    }
}
