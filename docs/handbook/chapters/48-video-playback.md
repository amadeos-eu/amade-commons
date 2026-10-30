# 48. Video Playback

Two programs of the book play video: Video plays files, streams and DVDs ([chapter 32](32-video.md)), TV plays the live streams of the broadcasters ([chapter 38](38-tv.md)). Both leave the decoding and the picture to *libmpv*, the library form of the mpv player, and both drive it the same way. When TV came, that shared part moved into amade-commons as the package `mpv`. What the events mean for a program, its player state, stays in the program.

## A player in a Swing window: Mpv

*Package `mpv` · introduced in [32. Video](32-video.md#a-c-library-as-a-java-interface), moved to amade-commons for [38. TV](38-tv.md#what-moves-in-with-tv) · source: [`Mpv`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/mpv/Mpv.java)*

**What it is for.** Playing video and sound with libmpv into the native window of an AWT `Canvas`: loading the library, creating the player with the right options for this machine, the event thread, commands and properties, and closing it all in the right order.

```java
Mpv mpv = new Mpv();
if (!mpv.isAvailable()) { /* tell the user to install libmpv2 */ }
mpv.attach(Native.getComponentID(canvas), Map.of("idle", "yes"), this::handle);
mpv.observe(1, "pause", LibMpv.FORMAT_FLAG);
mpv.command("loadfile", "https://example.org/live.m3u8", "replace");
```

**When to choose it.** For anything libmpv can play: files, HLS and DASH streams, DVDs, internet video through yt-dlp. A player that only needs sound, such as the Audio Station, is better off with Java Sound ([chapter 33](33-audio.md)): it needs no native library and plays gaplessly. And `Mpv` is not a player class for the program to show: it hands over mpv's events, and the program makes its own state of them, because Video's state (chapters, duration, DVD titles) and TV's (how far behind live, audio tracks) have little in common.

**It needs JNA.** amade-commons does not bring JNA along, so that the programs that play nothing stay small. A program that uses `Mpv` adds it itself:

```groovy
implementation 'net.java.dev.jna:jna:5.14.0'
```

and its launcher starts Java with `--enable-native-access=ALL-UNNAMED`, as Video's and TV's do. The Debian package depends on `libmpv2`.

**Linux only, for now.** `LibMpv.NAMES` also has the Windows names (`libmpv-2.dll`, `mpv-2.dll`), so `Mpv` would load the library there, and `wid` takes a window handle (HWND) as well as an X11 window id. But Windows has no libmpv to depend on: a Windows installer would have to bring `libmpv-2.dll` and its FFmpeg libraries along, in the jpackage image of [chapter 50](50-packaging.md). Video and TV do not do that yet, so their CI builds only the Debian package, and their READMEs say so. Without the library, `isAvailable()` is false and the program shows its message instead of a picture.

| Method | For |
|---|---|
| `new Mpv()` | Loads libmpv; never fails. Without the library, `isAvailable()` is false and `unavailableReason()` says why, for a message to the user |
| `apiVersion()` | libmpv's client API version, e.g. `2.2`, for the About box and the log |
| `attach(windowId, options, listener)` | Creates the player in the window, sets the options, starts the event thread. The window must exist: call it after `setVisible(true)` |
| `observe(id, property, format)` | Asks for an event with the property's value now and at every change; the id (positive) comes back in the event |
| `command(args...)` | An mpv command such as `loadfile`, `seek`, `cycle pause`; a failure is logged |
| `setProperty(name, value)`, `property(name)` | A property as text, e.g. `property("current-tracks/audio/lang")`; empty when it is not available |
| `errorString(code)`, `eventName(id)` | mpv's words for an error code and an event, for messages and the log |
| `close()` | Stops the event thread and destroys the player; before the windows are disposed |

**How it works.** `attach` sets things up in an order that libmpv insists on:

```java title="amade-commons · mpv/Mpv.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/mpv/Mpv.java:attach"
```

- libmpv refuses to start unless the C library's `LC_NUMERIC` is `C`, and the JVM sets it to the user's locale. `attach` sets it back.
- The video output options come from [`VideoOutput`](#the-video-output-videooutput). A value this libmpv does not know (older versions know fewer GPU contexts) is logged, and mpv's own default stays.
- The program's options come next, and last the system properties `-Dmpv.<option>=<value>`, so a user can try `-Dmpv.hwdec=no` without a new build.
- Log messages at `warn` and above go to the program's log, with the hardware decoder probing that fails on every machine without one turned down to debug. When mpv's GPU renderer finds only a software renderer, `Mpv` switches to mpv's `fast` profile, because the better scalers leave the picture blank without a GPU.

The events are read on a thread of their own. `mpv_wait_event` blocks for up to a second, then the loop checks whether it should still run:

```java title="amade-commons · mpv/Mpv.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/mpv/Mpv.java:event-loop"
```

The listener is called on that thread, so a program updates its state there and hands it to the event dispatch thread through its `@Ui` executor ([chapter 41](41-start-up.md#two-threads-edtexecutor-and-ui)).

**Closing, and the X error handler.** `close` wakes the event thread, waits for it to leave `mpv_wait_event` and only then destroys the player, because the thread would otherwise read freed memory:

```java title="amade-commons · mpv/Mpv.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/mpv/Mpv.java:close"
```

The last line is the fix for a crash TV found ([chapter 38](38-tv.md#quitting)). An X11 program has one error handler for the whole process. AWT installs one that only logs: X errors such as `BadWindow` happen in normal work, for example when a window is disposed while the window manager still asks about it. mpv's video output replaces it with its own while it runs, and when the video output closes, mpv sets the handler back to Xlib's default. That one ends the process at the first error. So a program that disposed its windows after `mpv.close()` ended with an X error instead of quietly. `Mpv` reads AWT's handler before mpv starts, and puts it back after the player is destroyed and whenever mpv goes idle (after **Stop** or at the end of a stream), because mpv has closed the video output by then. This is done by the package-private `XErrors`, with JNA and libX11, and only under the X11 toolkit.

So the order in a program's `quit` is always: `mpv.close()`, then dispose the windows.

## Events: MpvEvent

*Package `mpv` · introduced in [32. Video](32-video.md#structs-without-structure) · source: [`MpvEvent`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/mpv/MpvEvent.java)*

`mpv_wait_event` returns a pointer into memory that mpv reuses for the next event. `MpvEvent.read` copies what it needs into a record, so the event can be handed on:

| Record | When |
|---|---|
| `StartFile`, `FileLoaded` | A `loadfile` started; the file or stream is open and plays |
| `EndFile(reason, error)` | It ended: `END_EOF`, `END_STOP`, `END_QUIT`, or `END_ERROR` with mpv's error code |
| `PropertyChange(id, name, value)` | An observed property: `Double`, `Boolean`, `Long` or `String` by the format asked for, `null` when it is not available (the duration of a live stream) |
| `Shutdown` | The player is going away |
| `Other(id)` | Any other event; `eventName(id)` names it |

The listener of `Mpv` never sees `None` (the one-second timeout) or `Log` (the log messages, which `Mpv` writes to the log itself). A sealed interface lets the program's `switch` cover all of them:

```java
void handle(MpvEvent event) {
    switch (event) {
        case MpvEvent.FileLoaded f -> update(st -> st.withStatus(PLAYING));
        case MpvEvent.EndFile e when e.reason() == MpvEvent.END_ERROR -> update(st -> st.failed(mpv.errorString(e.error())));
        case MpvEvent.PropertyChange p -> update(st -> propertyChanged(st, p));
        default -> { }
    }
}
```

The layouts of the C structs are offsets in `LibMpv`. The test of `MpvEvent` builds events in native memory with these layouts, so the parsing is tested without libmpv.

## The C interface: LibMpv

*Package `mpv` · introduced in [32. Video](32-video.md#a-c-library-as-a-java-interface) · source: [`LibMpv`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/mpv/LibMpv.java)*

The functions of `mpv/client.h` that the programs use, mapped with JNA, with the constants of the formats and events and the offsets in the structs. `LibMpv.NAMES` lists the names to load it by on Debian and Ubuntu (`mpv`, `libmpv.so.2`), Windows and macOS. A program rarely needs `LibMpv` itself: `Mpv` covers the functions, and the program uses only the format constants for `observe`.

## The video output: VideoOutput

*Package `mpv` · introduced in [32. Video](32-video.md#machines-without-a-gpu-and-older-libraries) · source: [`VideoOutput`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/mpv/VideoOutput.java)*

`VideoOutput.forThisMachine()` gives the options `Mpv` starts with. On Linux mpv's GPU output may only use X11 contexts (otherwise it tries the console and DRM), with `xv` and plain `x11` as fallbacks, and hardware decoding where it is safe. Without a GPU render node in `/dev/dri` (QEMU, Xvfb) it adds mpv's `fast` profile. A program can override each of them with its own options or with `-Dmpv.<option>`.

## Summary

- `Mpv` plays with libmpv in a native window: `attach` after the window is visible, `observe` the properties the program shows, `command` for everything else, `close` before the windows are disposed.
- The program keeps its own state and makes it from `MpvEvent`s, on the event thread.
- JNA and `libmpv2` are the program's dependencies, not the library's.
- `Mpv` restores AWT's X error handler after mpv's video output closes, so a program ends quietly.
