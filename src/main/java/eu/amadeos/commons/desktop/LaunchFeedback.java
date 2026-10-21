package eu.amadeos.commons.desktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Tells the amadeOS window manager that an application is starting, so it shows the busy cursor
 * until the new window appears (or 20 seconds have passed). The front panel does the same for the
 * programs it starts; this is for applications that start others, such as the File Manager opening a
 * document.
 *
 * <p>The message is a ClientMessage of type {@code _AMADEOS_LAUNCH} sent to the root window, like
 * the EWMH requests, over an X connection of its own, through Xlib with the foreign function API.
 * Nothing happens without an X display, without libX11, or when the application has not been
 * granted native access ({@code Enable-Native-Access: ALL-UNNAMED} in the jar manifest or
 * {@code --enable-native-access=ALL-UNNAMED}): the JDK would otherwise print a warning for every
 * application that does not know about this.
 */
public final class LaunchFeedback {
    private static final Logger LOG = LoggerFactory.getLogger(LaunchFeedback.class);
    public static final String MESSAGE_ATOM = "_AMADEOS_LAUNCH";

    private static final int CLIENT_MESSAGE = 33;
    private static final long SUBSTRUCTURE_NOTIFY_MASK = 1L << 19;
    private static final long SUBSTRUCTURE_REDIRECT_MASK = 1L << 20;
    /** sizeof(XEvent): a union padded to 24 longs. */
    private static final long XEVENT_SIZE = 24 * 8;
    // XClientMessageEvent on LP64: int type; unsigned long serial; Bool send_event; Display *display;
    // Window window; Atom message_type; int format; union { ... long l[5]; } data;
    private static final long TYPE = 0;
    private static final long SEND_EVENT = 16;
    private static final long DISPLAY = 24;
    private static final long WINDOW = 32;
    private static final long MESSAGE_TYPE = 40;
    private static final long FORMAT = 48;

    private static volatile Xlib xlib;
    private static volatile boolean unavailable;

    private LaunchFeedback() {
    }

    // --8<-- [start:announce]
    /** Asks the window manager to show the busy cursor until the next new window. Never throws. */
    public static void announce() {
        if (unavailable) return;
        if (!LaunchFeedback.class.getModule().isNativeAccessEnabled()) {
            unavailable = true;
            LOG.debug("No launch feedback: native access is not enabled for this application");
            return;
        }
        try {
            send(xlib());
        } catch (Throwable e) {
            unavailable = true;
            LOG.debug("No launch feedback: X11 not available", e);
        }
    }
    // --8<-- [end:announce]

    private static Xlib xlib() {
        Xlib x = xlib;
        if (x == null) {
            synchronized (LaunchFeedback.class) {
                x = xlib;
                if (x == null) xlib = x = new Xlib();
            }
        }
        return x;
    }

    /** One message over a connection of its own; Xlib is not thread safe, so one at a time. */
    private static synchronized void send(Xlib x) throws Throwable {
        MemorySegment display = (MemorySegment) x.openDisplay.invokeExact(MemorySegment.NULL);
        if (display.equals(MemorySegment.NULL)) {
            unavailable = true;
            LOG.debug("No launch feedback: no X display");
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            long root = (long) x.defaultRootWindow.invokeExact(display);
            long atom = (long) x.internAtom.invokeExact(display, arena.allocateFrom(MESSAGE_ATOM), 0);
            MemorySegment event = arena.allocate(XEVENT_SIZE);
            event.set(ValueLayout.JAVA_INT, TYPE, CLIENT_MESSAGE);
            event.set(ValueLayout.JAVA_INT, SEND_EVENT, 1);
            event.set(ValueLayout.ADDRESS, DISPLAY, display);
            event.set(ValueLayout.JAVA_LONG, WINDOW, root);
            event.set(ValueLayout.JAVA_LONG, MESSAGE_TYPE, atom);
            event.set(ValueLayout.JAVA_INT, FORMAT, 32);
            int unused = (int) x.sendEvent.invokeExact(display, root, 0,
                    SUBSTRUCTURE_REDIRECT_MASK | SUBSTRUCTURE_NOTIFY_MASK, event);
            unused = (int) x.flush.invokeExact(display);
        } finally {
            int unused = (int) x.closeDisplay.invokeExact(display);
        }
    }

    /** The five Xlib functions, looked up once. */
    private static final class Xlib {
        final MethodHandle openDisplay;
        final MethodHandle closeDisplay;
        final MethodHandle defaultRootWindow;
        final MethodHandle internAtom;
        final MethodHandle sendEvent;
        final MethodHandle flush;

        Xlib() {
            Linker linker = Linker.nativeLinker();
            SymbolLookup lib = SymbolLookup.libraryLookup("libX11.so.6", Arena.global());
            openDisplay = handle(linker, lib, "XOpenDisplay",
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            closeDisplay = handle(linker, lib, "XCloseDisplay",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            defaultRootWindow = handle(linker, lib, "XDefaultRootWindow",
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
            internAtom = handle(linker, lib, "XInternAtom",
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            sendEvent = handle(linker, lib, "XSendEvent", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
            flush = handle(linker, lib, "XFlush", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        }

        private static MethodHandle handle(Linker linker, SymbolLookup lib, String name, FunctionDescriptor descriptor) {
            return linker.downcallHandle(lib.find(name).orElseThrow(() -> new IllegalStateException(name)), descriptor);
        }
    }
}
