# 72. Interfaces to the Native Environment

amadeOS is a Java desktop on a Linux system that was not written in Java. Every program of this book reaches past the JVM sooner or later: it calls a C library, sends a message to the X server, asks a system service over D-Bus or starts a command. The desktop in turn has to offer the native programs it runs, Firefox, LibreOffice or a GTK file manager, the environment they expect from a Linux desktop. This chapter puts both directions in one place. It does not repeat the chapters where each technique first came up; it says which one to choose, what they have in common, and what goes wrong.

## What you will learn

- The five ways a Java program of amadeOS talks to the system, and when to choose which.
- The rules every JNA binding of the book follows: the dependency, type mapping, memory ownership, callbacks, X errors and tests.
- What a native GUI program expects from the session: the D-Bus user bus, its activation environment, the desktop portals and `XDG_CURRENT_DESKTOP`.
- Why a session that skips `/etc/X11/Xsession` made file dialogs in Firefox wait 25 seconds, and how to find such a delay.

## Five ways to the system

| Way | Choose it when | Examples in the book |
|---|---|---|
| A command (`CommandRunner`) | A command-line program already does the job and runs for a while, or needs root through `pkexec` | `scanimage` ([31](31-imagescan.md)), LibreOffice as converter ([23](23-writepad.md)), apt ([64](64-software.md)), [chapter 47](47-programs-json-network.md) |
| D-Bus (dbus-java, `gdbus`) | A system or session service offers the job: logind, udisks2, polkit, notifications, the keyring | [67. Notifications](67-notifications.md), [68. Power](68-power.md), [71. Disks](71-disks.md) |
| JNA | A C library does the job and is called often or keeps state: a player, a CD drive, the X server | Video ([32](32-video.md)), Audio ([33](33-audio.md#audio-cds-through-jna)), Screenshot ([34](34-screenshot.md)), `Mpv` ([48](48-video-playback.md)), the panel and the window manager ([63](63-front-panel.md), [65](65-clipboard.md), [66](66-window-switcher.md)) |
| The foreign function and memory API | A handful of calls in amade-commons itself, which must not depend on JNA | `LaunchFeedback` ([45](45-desktop.md#the-busy-cursor-launchfeedback)) |
| Files and environment variables | The system already keeps the answer in a file: `/proc`, XDG folders, `/etc/os-release`, desktop entries | Task Manager ([69](69-task-manager.md)), `XdgDirectories` ([44](44-settings.md)) |

Prefer the way higher up in the list that does the job. A command fails in a way a user can read in the log; a wrong JNA mapping crashes the JVM. D-Bus calls are typed and versioned by the service; a C structure changes with the library version.

## JNA: the rules of the book

JNA (Java Native Access) turns a Java interface into calls of a shared library, with no C compiler and no native code of our own. It came up first in [chapter 32](32-video.md); the rules below are what Video, Audio, Screenshot, `Mpv` and the desktop modules have in common.

**The dependency is the program's.** amade-commons only compiles against JNA (`compileOnly`), so programs that call no C library stay small. A program that uses `Mpv` or a binding of its own adds `net.java.dev.jna:jna` to its `build.gradle`, and its Debian package depends on the C library it loads (`libmpv2`, `libcdio19`, `libx11-6`, `libopenmpt0t64`). Desktop modules get JNA through `base`.

**Map only what you call.** An interface lists the dozen functions the program needs, not the whole header. Load it by every name the library has on the systems you support: Debian's soname (`libmpv.so.2`) as well as the development name (`mpv`), and the Windows DLL if there is one. Without the library, catch `UnsatisfiedLinkError` once, log it and switch the feature off, as Screenshot does with its window list.

**Types.**

| C | Java (JNA) | Note |
|---|---|---|
| `int`, `int64_t` | `int`, `long` | Fixed sizes, map directly |
| `long`, `unsigned long` (Xlib's `Window`, `Atom`) | `NativeLong` | 64 bits on Linux, 32 on Windows |
| `size_t` | a `SizeT` of your own ([33](33-audio.md)) | 64 bits on every 64-bit system, unlike `NativeLong` |
| `const char *` | `String` | JNA converts in the platform encoding |
| `char **` with a null at the end | `String[]` | As `mpv_command` expects |
| An opaque handle | `Pointer` | Never look inside |
| A struct with many fields | `Structure` with `@FieldOrder` | Screenshot's 23-field `XWindowAttributes` ([34](34-screenshot.md)) |
| A small struct read once | `Pointer.getInt(offset)` | Video's events ([32](32-video.md#structs-without-structure)) |

**Who owns the memory.** JNA cannot know it; the header's comments say it. A string the library hands over and wants back is freed with the library's function (`mpv_free`, `XFree`); a string inside an event belongs to the library and is valid only until the next call. Write the owner next to each method of the interface.

**Callbacks live in `static final` fields.** A Java function given to C as a callback (Xlib's error handler, libopenmpt's log function) must not be collected while C still holds it, or the next call jumps into freed memory.

**X errors.** An X11 process has one error handler for all its libraries. AWT installs one that only logs; libmpv and others replace it and set Xlib's default back when they are done, and that default ends the process at the first `BadWindow`. Read the handler before the library starts and put it back afterwards, as `Mpv` does ([48](48-video-playback.md)).

**The C locale.** The JVM sets the C library's locale to the user's. A library that prints or parses decimal numbers may then read "0,5"; libmpv refuses to start. Set `LC_NUMERIC` back to `C` through libc before you start it ([32](32-video.md)). Java's own formatting does not use the C locale.

**Native access.** A program that calls C code declares `Enable-Native-Access: ALL-UNNAMED` in its jar's manifest, otherwise Java prints a warning on every start ([chapter 20](20-files.md)).

**Tests without the library.** JNA's `Memory` is native memory a test can fill with the layout of a C structure, so the code that reads events or structs runs in CI without the library. Tests that need the real library (Audio's checksum against libcdio) state the package in the CI workflow instead of being skipped there.

## Native programs in the session

The second direction: the session runs programs that were not written for amadeOS. Firefox, LibreOffice, GIMP or a GTK file manager were written for desktops that start a long list of services at login, and they quietly rely on them. A program that does not find a service usually carries on. A program that finds a service that hangs waits, and the user sees a frozen program.

### What a GTK or Qt program asks the session for

| Service | Found through | Used for |
|---|---|---|
| The D-Bus user bus | `DBUS_SESSION_BUS_ADDRESS`, set by `pam_systemd` (package `dbus-user-session`) | Everything below |
| The desktop portals (`xdg-desktop-portal` and a backend such as `xdg-desktop-portal-gtk`) | D-Bus activation | File dialogs, opening links, the colour scheme; always for Flatpak and Snap programs |
| The accessibility bus (`at-spi`) | D-Bus activation | Screen readers; every GTK program connects at start |
| gvfs | D-Bus activation | Network drives and devices in file dialogs and file managers |
| gnome-keyring | D-Bus, started by PAM at login | Passwords of browsers, Electron programs and Flatpaks |
| The desktop's name | `XDG_CURRENT_DESKTOP` | Which portal, which password store, which `OnlyShowIn` entries |

Services that are started *on demand* (D-Bus activation) are not children of the session. `systemd --user` starts them, with the environment it has, not the session's. Unless the session hands its variables over, a portal starts without `DISPLAY` and `XAUTHORITY`, cannot open a window, and fails or hangs.

### The activation environment

Every display manager runs `/etc/X11/Xsession`, and Debian's `Xsession.d/95dbus_update-activation-environment` hands the session's environment to D-Bus and systemd there. `amadeos-dm` starts `amadeos-session` directly, and so did not get this step until amadeos-system #125: the session now does it itself, before the window manager and the autostart scripts:

```sh title="amadeos-system · session/packaging/root/usr/bin/amadeos-session (excerpt)"
if [ -n "${DBUS_SESSION_BUS_ADDRESS:-}" ] || [ -S "${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/bus" ]; then
    if command -v dbus-update-activation-environment >/dev/null 2>&1; then
        dbus-update-activation-environment --systemd --all
    fi
    # A portal started before with the old environment keeps it: stop it,
    # D-Bus starts it again with this one when a program asks for it.
    systemctl --user stop xdg-desktop-portal.service xdg-desktop-portal-gtk.service \
        xdg-desktop-portal-gnome.service 2>/dev/null || true
fi
```

`--all` hands over everything, as Xsession does: the display, the language, the proxy of Settings > Network, `XDG_DATA_DIRS` with the Snap and Flatpak folders. The package depends on `dbus-bin`, which holds the command.

### Choosing the portal

`xdg-desktop-portal` picks its backend from `XDG_CURRENT_DESKTOP` and a file named after the desktop. The session names itself `amadeOS:GNOME`: GNOME second, so that Electron and Chromium programs keep their passwords in gnome-keyring. The portal reads `/usr/share/xdg-desktop-portal/amadeos-portals.conf` first, because amadeOS comes first:

```ini title="amadeos-system · session/packaging/root/usr/share/xdg-desktop-portal/amadeos-portals.conf"
[preferred]
default=gtk
org.freedesktop.impl.portal.Secret=gnome-keyring
```

Only the GTK portal, and no fallback. With `default=gtk;*`, the portal took any backend it found where the GTK one failed; on Ubuntu that is GNOME's portal, which waits for GNOME Shell. Without any portal, programs get an error from D-Bus at once and show their own dialogs, which is fast.

!!! warning "Pitfall: 25 seconds"
    A delay of about 25 seconds, every time, is D-Bus's default timeout: a program asked a service that was started but never answered. In amadeOS 1.3 it showed as a Firefox file dialog that took over 30 seconds, LibreOffice stopping at 40 % of its splash screen and a GTK file manager that took long to start, while the Java programs of the desktop, which ask none of these services, were as fast as ever.

### Finding such a delay

1. **Is it the environment?** `systemctl --user show-environment | grep -E 'DISPLAY|XAUTHORITY|XDG_CURRENT_DESKTOP'` must show all three. `grep "handed to D-Bus" ~/.cache/amadeos/logs/session.log` shows that the session did its part.
2. **Which service?** `journalctl --user -b | grep -i -E 'portal|at-spi|gvfs'` lists services that failed to start, often with *cannot open display*.
3. **Which call?** `dbus-monitor --session` in a terminal while the program hangs shows the method call that gets no answer.
4. **Confirm it.** `GTK_USE_PORTAL=0 firefox` or `NO_AT_BRIDGE=1 libreoffice` switches one service off for one start. If the delay is gone, that service is the one.

## Summary

- Choose the simplest way that does the job: a command, a D-Bus service, a file; JNA for a C library called often; the foreign function API only inside amade-commons.
- A JNA binding maps only what it calls, knows who frees each piece of memory, keeps callbacks in static fields, restores X's error handler and tests its structs with `Memory`.
- Native programs rely on services D-Bus starts for them. They get the session's environment only if the session hands it to D-Bus and systemd, which `amadeos-session` does because it does not go through Xsession.
- A delay of 25 seconds is a D-Bus timeout: look for a service without a display.
