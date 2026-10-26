package eu.amadeos.commons.mpv;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * libmpv playing into a native window of a Swing window: loads the library, creates the player with the
 * video output options for this machine ({@link VideoOutput}), runs the event thread and closes it all in
 * the right order. What the events mean for a program (its player state) stays in the program; Video and
 * TV each have a player class on top of this one.
 *
 * <p>Commands can be sent from any thread; the listener gets the events on the event thread. mpv options
 * can be overridden with system properties {@code mpv.<option>}, e.g. {@code -Dmpv.ao=null}.
 *
 * <p>Needs JNA ({@code net.java.dev.jna:jna}) on the program's class path; amade-commons does not bring it.
 */
public final class Mpv implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Mpv.class);
    private static final String OVERRIDE_PREFIX = "mpv.";
    /** reply_userdata of the property Mpv observes itself; programs use positive ids. */
    private static final long IDLE_ACTIVE = -1;

    private final LibMpv mpv;
    private final String unavailableReason;
    private volatile Pointer handle;
    private volatile boolean running;
    private volatile boolean fastProfile;
    private Consumer<MpvEvent> listener = e -> {
    };
    private Thread events;

    // --8<-- [start:load]
    public Mpv() {
        LibMpv lib = null;
        StringBuilder why = new StringBuilder();
        for (String name : LibMpv.NAMES) {
            try {
                lib = Native.load(name, LibMpv.class);
                break;
            } catch (UnsatisfiedLinkError e) {
                why.append(name).append(": ").append(e.getMessage()).append('\n');
            }
        }
        this.mpv = lib;
        this.unavailableReason = lib == null ? why.toString().strip() : "";
        if (lib == null) LOG.warn("libmpv is not installed:\n{}", unavailableReason);
    }
    // --8<-- [end:load]

    /** False if libmpv is not installed; nothing can be played then. */
    public boolean isAvailable() {
        return mpv != null;
    }

    public String unavailableReason() {
        return unavailableReason;
    }

    /** libmpv's client API version, e.g. "2.5". */
    public String apiVersion() {
        long v = mpv.mpv_client_api_version();
        return (v >> 16) + "." + (v & 0xFFFF);
    }

    // --8<-- [start:attach]
    /**
     * Creates the player inside the given native window (X11 window id, HWND) and starts the event thread.
     * The window must exist, i.e. its component must be displayable.
     *
     * @param options  the program's mpv options, set after the video output ones and before the
     *                 {@code -Dmpv.*} overrides
     * @param listener gets every event but the log messages, which go to the log
     */
    public synchronized void attach(long windowId, Map<String, String> options, Consumer<MpvEvent> listener) {
        if (mpv == null) throw new IllegalStateException("libmpv is not installed");
        if (handle != null) throw new IllegalStateException("already attached");
        // libmpv refuses to start unless LC_NUMERIC is "C"; the JVM sets the user's locale.
        if (!System.getProperty("os.name", "").startsWith("Windows")) {
            Native.load("c", LibMpv.LibC.class).setlocale(LibMpv.LibC.LC_NUMERIC, "C");
        }
        XErrors.remember();
        Pointer h = mpv.mpv_create();
        if (h == null) throw new IllegalStateException("mpv_create failed");
        handle = h;
        this.listener = listener;
        option("wid", Long.toString(windowId));
        Map<String, String> output = new LinkedHashMap<>(VideoOutput.forThisMachine());
        Map<String, String> overrides = overrides();
        output.keySet().removeAll(overrides.keySet());
        fastProfile = "fast".equals(output.get("profile"));
        output.forEach(this::outputOption);
        options.forEach(this::option);
        overrides.forEach(this::option);
        check(mpv.mpv_initialize(h), "mpv_initialize");
        mpv.mpv_request_log_messages(h, LOG.isDebugEnabled() ? "info" : "warn");
        mpv.mpv_observe_property(h, IDLE_ACTIVE, "idle-active", LibMpv.FORMAT_FLAG);
        running = true;
        events = Thread.ofPlatform().daemon().name("mpv-events").start(this::eventLoop);
        LOG.info("libmpv {} attached to window 0x{}", apiVersion(), Long.toHexString(windowId));
    }
    // --8<-- [end:attach]

    public boolean isAttached() {
        return handle != null;
    }

    /**
     * Asks for {@link MpvEvent.PropertyChange} events of a property: one with its value now, then one with
     * every change. The id (positive) comes back in the events.
     */
    public void observe(long id, String property, int format) {
        if (id <= 0) throw new IllegalArgumentException("observe ids are positive: " + id);
        Pointer h = handle;
        if (h != null) mpv.mpv_observe_property(h, id, property, format);
    }

    /** Runs an mpv command such as {@code loadfile <url> replace}; a failure is logged. */
    public void command(String... args) {
        Pointer h = handle;
        if (h == null) return;
        int r = mpv.mpv_command(h, args);
        if (r < 0) LOG.warn("mpv {}: {}", String.join(" ", args), mpv.mpv_error_string(r));
    }

    public void setProperty(String name, String value) {
        Pointer h = handle;
        if (h == null) return;
        int r = mpv.mpv_set_property_string(h, name, value);
        if (r < 0) LOG.warn("mpv {}={}: {}", name, value, mpv.mpv_error_string(r));
    }

    /** A property as text, e.g. {@code current-tracks/audio/lang}; empty if it is not available. */
    public String property(String name) {
        Pointer h = handle;
        if (h == null) return "";
        Pointer p = mpv.mpv_get_property_string(h, name);
        if (p == null) return "";
        try {
            return p.getString(0, "UTF-8");
        } finally {
            mpv.mpv_free(p);
        }
    }

    /** mpv's text for a (negative) error code, e.g. of {@link MpvEvent.EndFile#error()}. */
    public String errorString(int error) {
        return mpv == null ? "error " + error : mpv.mpv_error_string(error);
    }

    public String eventName(int id) {
        return mpv == null ? "event " + id : mpv.mpv_event_name(id);
    }

    // --8<-- [start:close]
    @Override
    public synchronized void close() {
        Pointer h = handle;
        if (h == null) return;
        running = false;
        // The event thread must leave mpv_wait_event before the handle is destroyed.
        mpv.mpv_wakeup(h);
        try {
            if (events != null) events.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        handle = null;
        mpv.mpv_terminate_destroy(h);
        // Before the windows are disposed: they get BadWindow errors, which Xlib's default handler ends
        // the program on.
        XErrors.restore();
    }
    // --8<-- [end:close]

    // ---- events ----------------------------------------------------------------------------------

    // --8<-- [start:event-loop]
    private void eventLoop() {
        while (running) {
            Pointer h = handle;
            if (h == null) break;
            handle(MpvEvent.read(mpv.mpv_wait_event(h, 1.0)));
        }
    }
    // --8<-- [end:event-loop]

    private void handle(MpvEvent event) {
        switch (event) {
            case MpvEvent.None n -> {
                return;
            }
            case MpvEvent.Shutdown s -> running = false;
            case MpvEvent.Log l -> {
                log(l);
                return;
            }
            case MpvEvent.PropertyChange p when p.id() == IDLE_ACTIVE -> {
                // mpv goes idle after closing the video output, which reset the X error handler.
                if (Boolean.TRUE.equals(p.value())) XErrors.restore();
                return;
            }
            default -> {
            }
        }
        try {
            listener.accept(event);
        } catch (RuntimeException e) {
            LOG.warn("mpv event listener failed on {}", event, e);
        }
    }

    private void log(MpvEvent.Log l) {
        if (isSoftwareRenderer(l) && !fastProfile) {
            // The renderer found no GPU (QEMU, Xvfb) although /dev/dri suggested one: the high-quality
            // scalers would leave the picture blank, so switch to the simple ones while running.
            fastProfile = true;
            LOG.info("Software rendering: switching mpv to the fast profile");
            command("apply-profile", "fast");
        }
        // Probing hardware decoders fails normally on machines without them (no VAAPI driver, ...).
        boolean probing = l.prefix().contains("vaapi") || l.prefix().contains("vdpau");
        switch (probing ? "debug" : l.level()) {
            case "fatal", "error" -> LOG.error("mpv {}: {}", l.prefix(), l.text());
            case "warn" -> LOG.warn("mpv {}: {}", l.prefix(), l.text());
            default -> LOG.debug("mpv {}: {}", l.prefix(), l.text());
        }
    }

    /** mpv's warning when its OpenGL renderer runs without a GPU (llvmpipe). */
    static boolean isSoftwareRenderer(MpvEvent.Log l) {
        return l.prefix().startsWith("vo/gpu") && l.text().contains("software renderer");
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private void option(String name, String value) {
        check(mpv.mpv_set_option_string(handle, name, value), "option " + name + "=" + value);
    }

    /** The {@code -Dmpv.<option>=<value>} system properties, which replace the defaults of the same name. */
    static Map<String, String> overrides() {
        Map<String, String> overrides = new TreeMap<>();
        System.getProperties().stringPropertyNames().stream().filter(k -> k.startsWith(OVERRIDE_PREFIX))
                .forEach(k -> overrides.put(k.substring(OVERRIDE_PREFIX.length()), System.getProperty(k)));
        return overrides;
    }

    // --8<-- [start:output-option]
    /**
     * Sets a video output option. These depend on the mpv version (older ones know fewer GPU contexts), so
     * a value this libmpv rejects is logged and its own default stays, instead of no player at all.
     */
    private void outputOption(String name, String value) {
        int result = mpv.mpv_set_option_string(handle, name, value);
        if (result < 0) {
            LOG.warn("libmpv {} rejects {}={} ({}); using its default", apiVersion(), name, value,
                    mpv.mpv_error_string(result));
        }
    }
    // --8<-- [end:output-option]

    private void check(int result, String what) {
        if (result < 0) throw new IllegalStateException(what + ": " + mpv.mpv_error_string(result));
    }
}
