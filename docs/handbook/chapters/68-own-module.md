# 68. A Module of Your Own

The chapters of this part went through five modules of the desktop and the window manager's switcher. This one turns them into a checklist: what a new module of amadeos-system needs, from the first decision to the package in the image. As the running example, take a small tool that does not exist yet, *Disks*, which shows the disks and how full they are, `amadeos-disks`.

## What you will learn

- How to decide between a desktop module and a tool program.
- Every file a new module needs, and what the shared build does with it.
- How the module gets into the panel, the CI and the amadeOS image.

## First decision: which kind

| Question | Desktop module | Tool program |
|---|---|---|
| Does it run before the user can start anything, or must it be on the screen within a second? | yes: window manager, panel, login, lock | no |
| Does it talk to the X server directly (window types, root window properties, grabs)? | yes | rarely; then through `base` |
| Does the user open it from a drawer and wait for a window? | sometimes (Settings, logs) | yes |
| Start-up, wiring, languages | `main`, by hand | `AmadeApplication`, Guice, `Messages` ([chapter 41](41-start-up.md)) |
| Logging | `java.util.logging`, into the session log | SLF4J with Logback, like the applications |

When in doubt, build a tool program. It gets the whole start-up of amade-commons, it can be written and tested like the applications of this book, and nothing of the desktop waits for it. Disks is a tool program.

## Checklist

### 1. The module

Add it to `settings.gradle`:

```groovy
include 'disks'
```

and give it a `build.gradle` with the main class and the `debian` block. The shared build in the root `build.gradle` does the rest: Java 25, amade-commons, JUnit, the fat jar, the launcher in `/usr/bin`, the package ([chapter 61](61-system-information.md#the-package) shows sysinfo's):

```groovy title="disks/build.gradle"
// Disks: the disks and partitions with their free space. `amadeos-disks`; the Tools drawer lists it.
application {
    mainClass = 'eu.amadeos.system.disks.bootstrap.AmadeDisksApplication'
}

debian = [
    section    : 'utils',
    summary    : 'Disks and free space of amadeOS',
    description: '''Shows the disks and partitions of the computer with their size,
file system and free space. Part of the amadeOS desktop.'''
]
```

A desktop module goes into the list `desktopModules` of the root `build.gradle` as well. It then gets `base`, the `java.util.logging` binding and native access, and loses Guice ([chapter 60](60-desktop.md#two-kinds-of-module)). Add `javaOptions: '--enable-native-access=ALL-UNNAMED'` to its `debian` block, as `style` and `logviewer` do.

### 2. The packages

The package is `amadeos-<module>`, here `amadeos-disks`. Use the Java package `eu.amadeos.system.disks` with the parts of every tool program: `bootstrap` (the main class and the Guice module), `domain` (records), `service` (reading the system, behind interfaces), `ui` (the window). The main class follows sysinfo's ([chapter 61](61-system-information.md#start-up-and-wiring)):

```java
public static final AppInfo APP = new AppInfo("amadeos-disks", "Disks", "disks");

public static void main(String[] args) {
    AmadeApplication.launch(APP, injector -> injector.getInstance(MainWindow.class).setVisible(true),
            new DisksModule());
}
```

The third name of `AppInfo` is the program's directory below `~/.config/amadeos`.

### 3. Reading the system

Read Linux's files through a root directory that tests replace, and run programs through `CommandRunner` from amade-commons with a time limit ([chapter 61](61-system-information.md#a-time-limit-that-holds)). For Disks that is `/proc/mounts` and `/sys/block` as files, and `lsblk --json` as a program, read with `Json` ([chapter 47](47-programs-json-network.md#json)). A program the user may not have installed becomes a missing line or a hint, not an error, and goes into `recommends` of the `debian` block.

If the module has to change the system as root, do not run it as root. Write a helper as in [chapter 64](64-software.md#changing-needs-root-the-helper), install a polkit policy for it, and give it its own tests with a fake of the program it runs.

### 4. Texts in five languages

Put the texts into `src/main/resources/eu/amadeos/system/disks/messages.properties` and its translations `_de`, `_fr`, `_es`, `_it`, mark the menu letters with `&`, and add a test with `TranslationCheck` ([chapter 43](43-languages.md#a-test-for-every-translation-translationcheck)). A desktop module without Guice loads its texts with `Messages.load` and finds its language with `LocaleResolution`, as `SettingsTexts` of Settings does ([chapter 62](62-settings.md#five-languages-without-guice)).

### 5. The desktop entry

`packaging/amadeos-disks.desktop` puts the program into the Front Panel and the Application Manager. The shared build copies it into the package when the file has the package's name:

```ini title="disks/packaging/amadeos-disks.desktop"
[Desktop Entry]
Type=Application
Version=1.0
Name=amadeOS Disks
Name[de]=amadeOS Datenträger
GenericName=Disk Usage
GenericName[de]=Speicherbelegung
Comment=Show the disks and how much space is free
Comment[de]=Datenträger und freien Speicherplatz anzeigen
Icon=drive-harddisk
Exec=/usr/bin/amadeos-disks
Terminal=false
Categories=System;Monitor;
StartupNotify=true
```

The names and comments go into all five languages. `System;Monitor;` puts it into the Tools drawer, after the panel's fixed entries ([chapter 63](63-front-panel.md#drawers-from-desktop-entries)); nothing in the panel changes. Check the file with `desktop-file-validate`.

### 6. System files and maintainer scripts

Files the package installs besides the jar go into `packaging/root`, which mirrors the file system: `packaging/root/etc/amadeos/...`, `packaging/root/usr/share/...`. The shared build copies the tree into the package and makes every file under `etc/` a conffile, so dpkg keeps the administrator's changes on upgrades. Maintainer scripts (`postinst`, `prerm`, ...) go into `packaging/debian`. Disks needs neither.

### 7. Tests

Every part that decides something gets a test without the real system: the parsers with real output pasted in, the services with a temporary root directory and a fake `CommandRunner`, the texts with `TranslationCheck`. Tests that open a window run under `xvfb-run` in CI. The repository's CI checks that every module ran at least five tests.

### 8. The CI

`.github/workflows/build.yml` names the modules in two loops: the one that checks the tests ran, and the one that checks each package (the jar's main class, the launcher, the copyright file, the desktop entry, the files of `packaging/root` and the conffiles). Add the new module to both. A module that is not in the lists is built, but nobody checks its package.

### 9. Running and installing

```sh
./gradlew :disks:run                # try it on your own desktop
./gradlew :disks:test
./gradlew :disks:deb                # disks/build/distributions/amadeos-disks_0.1.0_all.deb
./gradlew :disks:installToAmadeOs   # into ../amadeos/config/packages.chroot
```

The image installs every package in `config/packages.chroot` ([chapter 50](50-packaging.md#into-the-amadeos-image)). For the desktop to depend on it, add it to the `Depends` or `Recommends` of the package that should bring it, or to the image's package list in the amadeos repository.

### 10. The handbooks

A module that users see gets a page in the amadeOS User's Handbook, in the amadeos repository. One that shows something new about building desktop software gets a chapter here.

## Exercises

1. **Disks.** Build it: the records, a parser for `/proc/mounts` that skips the pseudo file systems (`proc`, `sysfs`, `tmpfs`, ...), free space with `java.nio.file.FileStore`, a window like System Information's. What should the page show for a mount point the user may not read?
2. **A desktop module.** Turn the Beep page of Settings into a small module of its own, `amadeos-bell`, that only plays the bell with the current settings, as a desktop module without Guice. Which lines of the root `build.gradle` and of the CI have to change?

## Summary

- Choose a tool program unless the module is part of the desktop's start or talks to X11 itself.
- A module is an entry in `settings.gradle`, a `build.gradle` with the main class and the `debian` block, and the code; the shared build makes the jar, the launcher and the package.
- The desktop entry's `Categories` put the program into a drawer; `packaging/root` and `packaging/debian` hold system files and maintainer scripts.
- Texts in five languages with `TranslationCheck`, tests without the real system, and both CI lists: then the module is ready for the image.
