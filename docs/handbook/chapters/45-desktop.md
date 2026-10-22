# 45. Desktop Integration

A desktop program is not alone: it hands links to the web browser and files to the programs the user chose for them, tells the window manager that something is starting, and keeps passwords where the desktop keeps secrets. This chapter covers the three parts of amade-commons that talk to the desktop around the program.

## Opening links and files: DesktopLauncher

*Package `desktop` · introduced in [11. Web](11-web.md#opening-links-in-other-programs) · source: [`DesktopLauncher`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/desktop/DesktopLauncher.java)*

**What it is for.** Opening a link in the user's web browser and a file in the program the user chose for its type. Web opens links that leave the documentation, Mail and Calendar open links in messages and appointments, Files opens every file the user double-clicks.

| Method | What it does |
|---|---|
| `browse(uri)` | Opens a link in the web browser |
| `open(path)` | Opens a file in its program |
| `DesktopLauncher.isRisky(fileName)` | Whether opening the file could run code ([below](#files-that-could-run-isrisky)) |

**When to choose it.** Whenever something leaves the program. `CommonsModule` binds one, so a window asks for it in its constructor; a test binds a subclass that only records what it was asked to open. Call it on the background executor: starting another program can take a moment.

**How it works.** `xdg-open` first, the command every Linux desktop has for "open this with the user's choice". Java's own `java.awt.Desktop` is the fallback, because on Linux it needs GNOME's libraries, which amadeOS does not have, while on Windows and macOS it is the right way:

```java title="amade-commons · desktop/DesktopLauncher.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/desktop/DesktopLauncher.java:browse"
```

```java title="amade-commons · desktop/DesktopLauncher.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/desktop/DesktopLauncher.java:xdg-open"
```

The started program's output is discarded and the launcher does not wait for it. `LaunchFeedback.announce()` comes first, so the busy cursor appears while the other program starts.

!!! note "`IOException` means nothing could open it"
    `browse` and `open` throw only when neither `xdg-open` nor `Desktop` could be started. What `xdg-open` does afterwards (no program for the type, a broken file) it shows the user itself. A program shows the exception as a line in its status bar, as Web does ([chapter 11](11-web.md#opening-links-in-other-programs)).

## Files that could run: isRisky

*Package `desktop` · introduced in [11. Web](11-web.md#files-that-are-never-opened), used for every attachment in [22. Mail](22-mail.md#attachments-that-could-run) · source: [`DesktopLauncher`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/desktop/DesktopLauncher.java)*

**What it is for.** `xdg-open` on a shell script or a `.desktop` file can run it. For a link the user clicked on purpose, that is the user's decision. For an attachment from a stranger, it is how computers get infected. `isRisky` says whether a file name is of that kind:

```java title="amade-commons · desktop/DesktopLauncher.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/desktop/DesktopLauncher.java:risky"
```

**When to choose it.** For every file that came from someone else: mail attachments, downloads. A risky file is offered for saving, never opened. Files the user sees on their own disk, as in Files and Web, need no check.

**How it works.** The last extension decides, ignoring case, so `Invoice.PDF.desktop` is a desktop file. A file without an extension counts as risky: on Linux a program does not need one.

??? example "Test: risky and harmless names"
    ```java title="amade-commons · test/desktop/DesktopTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/desktop/DesktopTest.java:risky"
    ```

## The busy cursor: LaunchFeedback

*Package `desktop` · introduced in [20. Files](20-files.md#the-busy-cursor) · source: [`LaunchFeedback`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/desktop/LaunchFeedback.java)*

**What it is for.** Telling the amadeOS window manager that a program is starting, so that it shows the busy cursor until the new window appears, or for 20 seconds at most. Without a sign, the user double-clicks again and gets two editors. The Front Panel does this for the programs it starts; a program that starts others calls `LaunchFeedback.announce()`.

**When to choose it.** Before starting a program that opens a window. `DesktopLauncher` already calls it; Files calls it before it starts the Text Editor itself.

**How it works.** It sends one X11 message, a `ClientMessage` of type `_AMADEOS_LAUNCH`, to the root window, where the window manager listens. Java has no API for X11 messages, so `LaunchFeedback` calls five functions of `libX11` through the foreign function and memory API: no JNI, no C compiler, no native library of its own. The event's C structure is filled field by field in a `MemorySegment`.

```java title="amade-commons · desktop/LaunchFeedback.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/desktop/LaunchFeedback.java:announce"
```

*Never throws* is the contract. Without native access, without an X display or without `libX11`, it notes that it is unavailable and does nothing from then on.

!!! warning "Pitfall: native access"
    Java asks a program for permission before it calls C code, and prints a warning otherwise. A program allows it with `Enable-Native-Access: ALL-UNNAMED` in its jar's manifest ([chapter 20](20-files.md#the-busy-cursor)). `LaunchFeedback` checks that permission before anything else, so a program without it loses the cursor but gets no warning.

## Passwords: PasswordStore

*Package `desktop` · introduced in [22. Mail](22-mail.md#the-keyring) · source: [`PasswordStore`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/desktop/PasswordStore.java), [`SecretToolPasswordStore`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/desktop/SecretToolPasswordStore.java), [`SessionPasswordStore`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/desktop/SessionPasswordStore.java)*

**What it is for.** Keeping passwords and tokens outside settings files, in the desktop keyring: the freedesktop *Secret Service* that gnome-keyring, KDE Wallet and KeePassXC offer, encrypted and unlocked with the login password.

```java title="amade-commons · desktop/PasswordStore.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/desktop/PasswordStore.java"
```

The key is the program's own name for the secret, such as an account id. The label is what a keyring program shows next to it.

**When to choose it.** For every secret a program must remember: Mail's passwords and its sign-in tokens. `CommonsModule` binds a `SecretToolPasswordStore` under the program's `id`, so its entries carry `service=amade-mail` and `account=<key>` and can be listed with `secret-tool search service amade-mail`. Tests use a `SessionPasswordStore`, which keeps everything in memory.

**How it works.** Java has no API for the Secret Service, and a D-Bus library would be a large dependency for three calls. The small command `secret-tool` from the Debian package `libsecret-tools` makes exactly these calls. Every password goes into a session cache first, so it works for the rest of the session even when the keyring refuses it; a lookup asks the cache before the keyring:

```java title="amade-commons · desktop/SecretToolPasswordStore.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/desktop/SecretToolPasswordStore.java:lookup"
```

```java title="amade-commons · desktop/SecretToolPasswordStore.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/desktop/SecretToolPasswordStore.java:store"
```

The command gets the password on its standard input and has 30 seconds to finish. A locked keyring shows an unlock dialog, and a user who walks away must not leave the program waiting for good:

```java title="amade-commons · desktop/SecretToolPasswordStore.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/desktop/SecretToolPasswordStore.java:run"
```

`SecretToolPasswordStore` is older than [`CommandRunner`](47-programs-json-network.md#running-a-program-commandrunner) and starts its process itself; its output is a single password, far below the size of a pipe, so waiting before reading cannot dead-lock.

!!! warning "Pitfall: secrets on the command line"
    Never pass a secret as a command-line argument. The arguments of every process can be read by every user of the computer, in `/proc/<pid>/cmdline` and in `ps`. The label and the attributes are not secret and may be arguments; the password goes through standard input.

!!! note "No keyring, no pretending"
    Without `secret-tool`, `isPersistent()` is false and passwords live for the session only. A program says so honestly: Mail turns its *Remember password* box off and explains why ([chapter 22](22-mail.md#when-there-is-no-keyring)). There is no fallback to a file "just this once"; that is how passwords end up in plain text.

??? example "Test: without secret-tool, passwords live for the session"
    ```java title="amade-commons · test/desktop/DesktopTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/desktop/DesktopTest.java:session"
    ```
