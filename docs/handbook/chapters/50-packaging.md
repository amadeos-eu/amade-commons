# 50. Packaging and Installers

A program is finished for its users only when they can install it. Every chapter so far ended with a running program on the developer's machine; this one is about the way from there to a user's desktop. On amadeOS and other Debian systems that is a **Debian package**: the program, a start script, a desktop entry that puts it into the Front Panel, an icon, and AppStream metadata that *Software* shows. On Windows it is an **MSI installer** that brings its own Java runtime.

All amade applications and games are packaged the same way, by a few Gradle tasks in their `build.gradle` and a `packaging/` folder next to the sources. This chapter follows amade-fractals (chapter 30) through every step. [Chapter 10](10-pad.md#shipping-it) showed the short version for Pad; here is the whole of it.

## What you build

| Command | Result | For |
|---|---|---|
| `./gradlew jar` | `build/libs/amade-fractals-0.1.0.jar`, one jar with all libraries | every platform with Java 25 |
| `./gradlew deb` | `build/distributions/amade-fractals_0.1.0_all.deb` | amadeOS, Debian, Ubuntu |
| `./gradlew installToAmadeOs` | the package in `../amadeos/config/packages.chroot/` | the amadeOS image |
| `./gradlew windowsInstaller` | `build/windows/msi/amade-fractals-0.1.0.msi`, with a Java runtime | Windows |
| `./gradlew windowsInstaller -PinstallerType=app-image` | `build/windows/app-image/amade-fractals/`, a folder to unzip anywhere | Windows, without installing |

And in the repository:

```text
packaging/
    amade-fractals.desktop                    the desktop entry
    eu.amadeos.apps.fractals.metainfo.xml     the AppStream metadata
    windows/amade-fractals.ico                the Windows icon, 16 to 256 pixels
src/main/resources/icons/amade-fractals.png   the icon, 48 × 48 pixels
docs/screenshots/amade-fractals.png           the screenshot Software shows
```

## What you will learn

- How to build one jar that runs with `java -jar`, and why signed libraries need care.
- What a Debian package contains, where each file goes, and what the control file says.
- How a desktop entry's categories decide the Front Panel drawer.
- Which icon sizes a program needs and how to make them.
- How AppStream metadata gives Software a name, a description and a screenshot in five languages.
- How a package gets into the amadeOS image.
- How jpackage and WiX build a Windows installer that needs no Java on the user's machine, and which programs get one.
- How CI checks every package, so that a broken one never reaches a user.

## One jar with everything

Both packages start from the same file: a "fat" jar that holds the program and all its libraries, so that `java -jar` needs nothing else.

```groovy title="amade-fractals · build.gradle"
--8<-- "amade-fractals/build.gradle:jar"
```

- `Main-Class` makes the jar runnable. `Implementation-Version` lets the program read its own version (`getClass().getPackage().getImplementationVersion()`), for an About dialog for example.
- `from { configurations.runtimeClasspath ... }` unpacks every library jar into this one. Two libraries can have a file of the same name (licence texts, `module-info.class`); `DuplicatesStrategy.EXCLUDE` keeps the first.
- Some libraries are **signed**: their `META-INF` has a signature (`*.SF`, `*.DSA`, `*.RSA`) over their own classes. Copied into a jar with other classes, the signature no longer fits, and Java refuses to load the jar with a `SecurityException`. Leave the signatures out.

!!! tip "Tip: try the jar, not the IDE"
    Before you package, run `java -jar build/libs/amade-fractals-0.1.0.jar` once. A resource that only the IDE finds (a file under `src/` instead of `src/main/resources/`) or a library that is only `compileOnly` shows up here, not in the IDE.

## The Debian package

A Debian package (`.deb`) is an archive of files in the places where they will be installed, plus a folder `DEBIAN/` with a description for the package manager. The `deb` task builds this tree in `build/deb-staging` and lets `dpkg-deb` pack it. No Debian tools beyond `dpkg-deb` are needed, which every Debian and Ubuntu has.

| Path in the package | What it is |
|---|---|
| `/opt/amade-fractals/amade-fractals.jar` | the fat jar |
| `/usr/bin/amade-fractals` | the start script, so the program can be started by name |
| `/usr/share/applications/amade-fractals.desktop` | the desktop entry: name, icon, categories |
| `/usr/share/icons/hicolor/48x48/apps/amade-fractals.png` | the icon, in the standard icon theme |
| `/usr/share/metainfo/eu.amadeos.apps.fractals.metainfo.xml` | the AppStream metadata for Software |
| `/usr/share/doc/amade-fractals/copyright` | the licence, where Debian expects it |
| `DEBIAN/control` | name, version, dependencies, description |

`/opt/<package>` is the place for a program that brings its own files and does not split them over `/usr/lib` and `/usr/share`. A jar is such a program.

### The files

```groovy title="amade-fractals · build.gradle (excerpt of the deb task)"
--8<-- "amade-fractals/build.gradle:deb-files"
```

The start script is two lines. `exec` replaces the shell with Java, so the process the desktop started is the program itself, and `"$@"` hands over the arguments unchanged: `amade-fractals ~/Pictures/seahorse.fractal` opens that file. It is made executable for everyone (`setExecutable(true, false)`); `dpkg-deb` keeps the file modes.

The desktop entry and the metainfo are files in `packaging/`, copied unchanged. Pad generates its desktop entry inside `build.gradle` (chapter 10), which is fine for a few lines; with names in five languages a file of its own is easier to read and to check.

### The control file

```groovy title="amade-fractals · build.gradle (end of the deb task)"
--8<-- "amade-fractals/build.gradle:deb-control"
```

- **Package** is the name for `apt install` and `dpkg -l`. The amade programs use their repository name.
- **Version** comes from `version` in `build.gradle`. apt installs a package over an installed one only when the version is higher, so it must grow with every release. `0.1.0-SNAPSHOT` sorts *after* `0.1.0` for dpkg, not before; for a pre-release write `0.1.0~beta1`, which sorts before `0.1.0`. CI now works the version out from the commits and writes it the Debian way ([chapter 51](51-releases.md)).
- **Section** sorts the package in package managers: `graphics`, `editors`, `mail`, `sound`, `video`, `web`, `utils`, `games`.
- **Architecture: all** says that the package runs on every processor. A jar does; a package with a native library would need one package per architecture.
- **Depends** names what must be installed first. `openjdk-25-jre | java-runtime (>= 25)` takes Debian's OpenJDK or any package that provides a Java 25 runtime. Name every other program you start: Imagescan depends on `sane-utils` for `scanimage`, Video on `libmpv2`.
- **Maintainer** is the amadeOS project for all amade packages.
- **Description** has a short first line (shown in lists) and a longer text, every line of it indented by one space.

`--root-owner-group` makes every file belong to root in the package, whoever built it. Without it the files would belong to the developer's user id on every machine that installs the package.

Try it:

```sh
./gradlew deb
dpkg-deb --info build/distributions/amade-fractals_0.1.0_all.deb       # the control file
dpkg-deb --contents build/distributions/amade-fractals_0.1.0_all.deb   # the files
sudo apt install ./build/distributions/amade-fractals_0.1.0_all.deb    # installs it with its dependencies
```

`apt install ./file.deb` (with the `./`) installs a local file and fetches the dependencies; `dpkg -i` would install the file alone and complain about missing ones.

## The desktop entry

The desktop entry is what makes a program appear on the desktop. It follows the [freedesktop Desktop Entry Specification](https://specifications.freedesktop.org/desktop-entry-spec/latest/), which every Linux desktop reads:

```ini title="amade-fractals · packaging/amade-fractals.desktop"
--8<-- "amade-fractals/packaging/amade-fractals.desktop"
```

- **Name**, **GenericName** and **Comment** in the five languages of the program (chapter 10). The desktop picks the one for the user's language. `Comment` is the tooltip; keep it one short sentence, the same as the AppStream summary.
- **Icon** is a name, not a file: the desktop looks for `amade-fractals.png` in the icon theme. That is why the package installs the icon under `hicolor`.
- **Exec** is the command. `%F` stands for the files the user opens with the program (from the file manager's *Open With*, for example); a program that takes one file at a time writes `%f`. A program that opens files also lists their types in `MimeType=`, as Pad does.
- **Keywords** help search: in Software, and in the menus of other desktops.
- **StartupNotify=true** shows the busy cursor until the first window appears.

Check a desktop entry with `desktop-file-validate packaging/amade-fractals.desktop` (in the package `desktop-file-utils`).

### Categories and the Front Panel drawers

`Categories` decides where the program appears: in the drawers of the amadeOS Front Panel, in the Application Manager, and in the menus of other desktops. The Front Panel reads the desktop entries of all installed programs and fills its drawers by these categories:

| Categories (any of them) | Front Panel drawer |
|---|---|
| `FileManager`, `FileTools` | File Manager |
| `TextEditor`, `WordProcessor`, then `Office`, `Spreadsheet`, `Presentation`, `Calendar` | Text & Office |
| `Email`, `ContactManagement`, `News`, then `Network`, `WebBrowser`, `Chat` | Mail & Internet |
| `AudioVideo`, `Audio`, `Video`, `Player`, `Recorder` | Sound & Video |
| `Graphics`, `Viewer`, `RasterGraphics`, `2DGraphics`, `VectorGraphics`, `Photography`, `Scanning` | Graphics |
| `Game` and its kinds: `ArcadeGame`, `BoardGame`, `CardGame`, `LogicGame`, … | Games |
| `Printing`, `HardwarePrinting` | Print Manager |
| `Settings`, `DesktopSettings`, `PackageManager`, `Monitor`, `System` | Tools |
| `Documentation`, `Help` | Help |
| anything else (`Utility`, `Development`, …) | only the Application Manager |

A program appears in every drawer whose category it has, so give it the one main category the specification asks for (`Graphics`, `Office`, `Game`, …) and the more specific ones that fit. Fractals says `Graphics;Education;Math;`: the Graphics drawer, and the education menus of other desktops. `desktop-file-validate` gives a hint that `Graphics` and `Education` are both main categories and the program may appear twice in a menu; for Fractals that is wanted. The Application Developer's Handbook itself, as a package, says `Documentation;Development;` and is in the Help drawer.

## Icons

A program needs its icon in three places, in two formats:

| Where | Format | Size |
|---|---|---|
| the window's title bar and the task list (`setIconImage`) | PNG in the jar, `src/main/resources/icons/<name>.png` | 48 × 48 |
| the Front Panel, the Application Manager, Software | the same PNG, installed as `/usr/share/icons/hicolor/48x48/apps/<name>.png` | 48 × 48 |
| the Windows Start menu, desktop shortcut and `.exe` | ICO, `packaging/windows/<name>.ico` | 16, 24, 32, 48, 64, 128, 256 |

The window loads its icon from the jar:

```java
java.net.URL icon = getClass().getResource("/icons/amade-fractals.png");
if (icon != null) setIconImage(new javax.swing.ImageIcon(icon).getImage());
```

On Linux the icon goes into the `hicolor` theme, the one every icon theme falls back to. The folder name is the size: an icon in `48x48/apps/` must be 48 × 48 pixels, or desktops scale it wrongly or ignore it. Software looks for the sizes 64, 48, 96, 128, 256 and 32 in this order, then in `/usr/share/pixmaps`; a program may install several sizes, and the amade programs install 48.

Windows wants one `.ico` file with several sizes inside, from 16 for lists up to 256 for large icons in Explorer. Draw the icon once, large, and let a tool make the sizes. The games draw their icons in Java2D with a small program in `tools/MakeIcon.java`, which writes a PNG of any size:

```sh
java tools/MakeIcon.java 48 src/main/resources/icons/amade-snake-game.png
java tools/MakeIcon.java 256 build/icon-256.png
python3 -c "from PIL import Image; Image.open('build/icon-256.png').save('packaging/windows/amade-snake-game.ico',
    sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])"
```

With ImageMagick instead of Python: `convert build/icon-256.png -define icon:auto-resize=256,128,64,48,32,24,16 packaging/windows/<name>.ico`. Check the result with `identify packaging/windows/<name>.ico`, which lists one line per size. The icon files are part of the repository; the tool runs again only when the icon changes.

!!! tip "Tip: small sizes need their own care"
    An icon scaled down from 256 to 16 pixels often becomes a blur. If the small sizes look wrong, draw them separately (the same picture with thicker lines and fewer details) and put them together into the ICO.

## AppStream metadata

The desktop entry says how to start a program. **AppStream** says what it is: a name, a summary, a description, a screenshot, the home page, the licence. *Software*, the amadeOS program for installing and updating, shows this for every installed amade program, and GNOME Software and KDE Discover read the same files. One file per program, `/usr/share/metainfo/<id>.metainfo.xml`, as described in the [AppStream specification](https://www.freedesktop.org/software/appstream/docs/).

The beginning says who the program is:

```xml title="amade-fractals · packaging/eu.amadeos.apps.fractals.metainfo.xml (beginning)"
--8<-- "amade-fractals/packaging/eu.amadeos.apps.fractals.metainfo.xml:4:13"
```

- The **id** is a reverse domain name that never changes: `eu.amadeos.apps.<name>` for the applications, `eu.amadeos.games.<name>` for the games. The file is named after it.
- **metadata_license** is the licence of this file. `CC0-1.0` lets every catalogue copy it; **project_license** is the program's, `EUPL-1.2` for all amade programs.
- **name**, and after it **summary** and **description**, come once without `xml:lang` (English) and once for each language. In the description every paragraph and every list item is repeated with its `xml:lang`. Keep the summary short and without a full stop at the end; it is the line under the name in lists.

The rest links the metadata to the package and to the web:

```xml title="amade-fractals · packaging/eu.amadeos.apps.fractals.metainfo.xml (end)"
--8<-- "amade-fractals/packaging/eu.amadeos.apps.fractals.metainfo.xml:51:80"
```

- **launchable** names the desktop entry, so that Software can start the program. **icon type="stock"** is the icon name from the desktop entry.
- **categories** repeat those of the desktop entry.
- **url** and **developer** point to the repository and its issues, and say that the amadeOS project makes the program.
- **screenshots**: one picture, with a caption in each language. The image is a web address, `https://raw.githubusercontent.com/datazuul/<repo>/main/docs/screenshots/<repo>.png`, so the picture lives in the repository and not in the package. Take it on amadeOS with the program doing what it is for: a document open, a fractal zoomed, a game *in play*, never a title screen or an empty window.
- **provides** names the command, so that a search for `amade-fractals` finds the package.
- **content_rating** follows OARS, the Open Age Rating Service. An empty `oars-1.1` rating says "nothing to warn about"; a game with fighting says so, for example `<content_attribute id="violence-fantasy">mild</content_attribute>`.

Check the file with `appstreamcli validate --no-net packaging/eu.amadeos.apps.fractals.metainfo.xml` (in the package `appstream`). `--no-net` skips the check of the web addresses, which the screenshot only passes once it is on `main`. Warnings about a summary that is too long or ends with a full stop are worth fixing; Software cuts long summaries.

!!! note "Later: a package repository of our own"
    Today the amade packages come with the amadeOS image, and Software reads the metadata of installed packages. When amadeOS gets a package repository of its own, its catalogue (DEP-11 files, next to the package lists) is made from these same files by `appstream-generator`, which also downloads the screenshots and turns the icons into the sizes catalogues need. A repository tool such as aptly publishes the packages, not this catalogue; that is an extra step after publishing. Nothing changes in the programs.

## Into the amadeOS image

The amadeOS image installs every package in its folder `config/packages.chroot/`. Every amade program has the same task to put its package there:

```groovy title="amade-fractals · build.gradle"
--8<-- "amade-fractals/build.gradle:install"
```

It expects a checkout of the amadeos repository next to the program's (`../amadeos`). You rarely call it yourself: `buildDistribution.sh` in the amadeos repository builds the whole distribution:

1. It clones every repository of `repositories.list` that is not checked out yet, and pulls `main` of all of them.
2. It builds amade-commons, then every repository whose `build.gradle` has an `installToAmadeOs` task, against that checkout of amade-commons (the composite build of chapter 2), and removes the packages of an earlier build first.
3. It builds the handbooks' packages.
4. It runs `build.sh`, which makes the ISO image with live-build.

So a new program needs two things to be in the image: the `installToAmadeOs` task, and its repository name in `amadeos/repositories.list`. `./buildDistribution.sh --apps amade-fractals --quick` builds only one program and repacks the previous image, which is much faster while you try a package.

## The Windows installer

The amade programs are Java, so they run on Windows too. A Windows user does not have Java 25, though, and should not need it. **jpackage**, which comes with the JDK, builds a Windows installer that contains the program *and* a Java runtime. Installed, it is an ordinary Windows program with a Start menu entry and an `.exe`; nobody sees Java.

```groovy title="amade-fractals · build.gradle"
--8<-- "amade-fractals/build.gradle:windows"
```

- jpackage builds packages only for the system it runs on: a Windows installer only on Windows. The task stops with a clear message elsewhere. CI builds the Windows packages on a Windows machine (below).
- It takes the `jpackage.exe` of the Java toolchain (Java 25), not of whatever Java runs Gradle.
- `--input` is a folder that jpackage puts into the installer completely, so the task gives it a folder with just the fat jar.
- **Three types**: `msi`, the Windows Installer package that administrators can also roll out; `exe`, a setup program; `app-image`, a folder with `amade-fractals.exe`, the jar and the runtime, which runs from anywhere (a USB stick) without installing. `msi` and `exe` need the **WiX Toolset** on the `PATH`: version 3, or with JDK 25 also 4 or 5. `app-image` needs nothing.
- `--app-version`: Windows installers know only numbers and dots, so `0.1.0-SNAPSHOT` becomes `0.1.0`.
- `--win-menu --win-menu-group amadeOS` puts the program into an *amadeOS* folder of the Start menu, `--win-shortcut` onto the desktop. `--win-dir-chooser` lets the user choose the folder, `--license-file` shows the licence before installing.
- `--win-upgrade-uuid` is the program's identity for Windows. A newer installer with the same UUID and a higher version replaces the installed one instead of installing a second copy. Make a new UUID once for each program (`uuidgen`) and **never change it**.

The MSI is more than 30 MB, most of it the Java runtime. jpackage takes the whole JDK runtime; `--jlink-options` or `--runtime-image` with a runtime made by `jlink` from just the modules the program uses (`java.desktop`, `java.prefs`, `java.net.http`, …) makes it smaller, at the price of an error at run time when a module is missing.

To build it yourself on Windows, install a JDK 25 and WiX 3 (from the WiX Toolset web site, or `choco install wixtoolset`), then:

```bat
gradlew windowsInstaller
gradlew windowsInstaller -PinstallerType=app-image
```

!!! tip "Tip: test the installed program, not the build"
    Install the MSI on a Windows that has never seen Java, start the program from the Start menu, open a file, and uninstall it again in *Settings → Apps*. The installed program runs with a different working directory than Gradle and without your user's Java settings, and that is where paths and resources that worked during development break.

## Which programs run on Windows

Not every amade program is built for Windows. A program gets the `windowsInstaller` task and the Windows CI job only when it runs there, and the release job then attaches the MSI and the ZIP next to the `.deb` ([chapter 51](51-releases.md)). The others ship only the Debian package, and their README should say so in a short *Platforms* section, so that nobody looks for an installer that does not exist.

| Program | Windows | What it depends on |
|---|---|---|
| Web, WritePad, Fractals | MSI and ZIP | Java only |
| Imagescan | MSI and ZIP | SANE on Linux, Windows Image Acquisition on Windows ([chapter 31](31-imagescan.md)) |
| Audio | MSI and ZIP | libcdio on Linux, the Windows CD-ROM driver on Windows ([chapter 33](33-audio.md)) |
| the games | MSI and ZIP | Java only, gamepads through XInput on Windows |
| Pad, Calculator, Contacts, Books, PDF Viewer | not built yet | Java only: a Windows build needs only the task and the CI job |
| Calendar, Mail | not built yet | Java only; handing an invitation or a contact to the other program starts it by its Linux command name |
| Image Viewer | not built yet | Java; lossless JPEG rotation calls `jpegtran` |
| Files | no | `/proc/self/mounts` for the disks, the freedesktop.org trash, `xdg-open` |
| Screenshot | no | X11 for the window list and the clipboard hand-off |
| Video, TV | no | libmpv, embedded in an X11 window ([chapter 48](48-video-playback.md)); libmpv exists for Windows as `libmpv-2.dll` |
| Backup & Sync | no | restic, `/proc/mounts` and `/dev/disk/by-uuid` for the disks, the freedesktop.org trash, the Secret Service keyring; restic itself exists for Windows |

A program moves from *no* to *MSI and ZIP* when each Linux part has a Windows counterpart behind the same interface, as Imagescan's scanner backends and Audio's CD drives have. A native program it needs, such as `restic.exe` or `libmpv-2.dll`, goes into the installer with jpackage's `--input` folder, because Windows has no package manager that would bring it along.

## Checking it in CI

A package nobody tried is a package that may be broken. Each repository's CI builds the packages on every push and pull request and checks what is in them. On Linux, after the tests:

```yaml title="amade-fractals · .github/workflows/build.yml (job test)"
--8<-- "amade-fractals/.github/workflows/build.yml:check-deb"
```

The jar must contain the icon and the right `Main-Class`, and the package every file of the table above. Then the package is uploaded as an artifact of the run, so it can be downloaded from the pull request and installed.

A second job runs on Windows. It installs WiX when the machine does not have it:

```yaml title="amade-fractals · .github/workflows/build.yml (job windows)"
--8<-- "amade-fractals/.github/workflows/build.yml:wix"
```

and after the tests builds the MSI and the portable folder, checks them and packs the folder into a zip:

```yaml title="amade-fractals · .github/workflows/build.yml (job windows)"
--8<-- "amade-fractals/.github/workflows/build.yml:windows-build"
```

An MSI under 30 MB has no Java runtime inside and would not run on a machine without Java; the check catches that. Both files are uploaded too, so every pull request has a Windows installer to try.

## Checklist for a new program

1. `version` from `appVersion` and `debVersion` ([chapter 51](51-releases.md)), `jar` with `Main-Class` and the signature excludes, as above.
2. A 48 × 48 icon in `src/main/resources/icons/<name>.png`, set as the window icon, and `packaging/windows/<name>.ico` with the sizes 16 to 256.
3. `packaging/<name>.desktop` with names in five languages, `Icon=<name>`, `Exec=/usr/bin/<name> %F` and the categories of the drawer it belongs to. `desktop-file-validate` reports no errors.
4. `packaging/eu.amadeos.apps.<name>.metainfo.xml` (or `eu.amadeos.games.<name>`) with names, summaries and descriptions in five languages, the screenshot address and an OARS rating. `appstreamcli validate --no-net` passes.
5. `docs/screenshots/<name>.png`, taken on amadeOS with the program in use.
6. The tasks `deb` (with the right `Section` and `Depends`), `installToAmadeOs` and, if the program runs on Windows, `windowsInstaller` with a new upgrade UUID. Otherwise a *Platforms* section in the README says that it is Linux only, and why.
7. The CI jobs for both packages, with the file list of the package check, and the version, release and commit jobs of [chapter 51](51-releases.md).
8. The repository name in `amadeos/repositories.list`.
9. Install the package on amadeOS: the program is in its drawer with its icon, starts, and Software shows its description and screenshot.

## Exercises

1. **More icon sizes.** Install the icon in 32, 64 and 128 pixels as well, made by the same tool. What changes in the `deb` task, and which size does Software take now?
2. **A smaller installer.** Build the Windows installer with a runtime from `jlink` that has only the modules Fractals needs. `jdeps --print-module-deps` on the fat jar tells you which. How much smaller is the MSI, and what happens when a module is missing?
3. **A man page.** Debian programs that can be started from a terminal have a manual page. Write a short one for `amade-fractals` (in the `man` format or with `pandoc` from Markdown), install it as `/usr/share/man/man1/amade-fractals.1.gz`, and check it with `man amade-fractals`.
4. **A desktop entry check in CI.** Install `desktop-file-utils` in the build job and let `desktop-file-validate` check `packaging/amade-fractals.desktop`. Which mistakes does it catch that the Front Panel would silently ignore?
5. **Package checks with lintian.** Run `lintian` on the package. Which of its complaints matter for a program that is part of amadeOS, and which are about rules for packages in Debian itself?

## Summary

- One fat jar holds the program and its libraries; leave the signatures of signed libraries out.
- A Debian package is the installed files plus `DEBIAN/control`. The amade programs put the jar into `/opt/<name>`, a two-line start script into `/usr/bin`, and the desktop entry, the 48-pixel icon, the AppStream metadata and the licence into `/usr/share`. `dpkg-deb --root-owner-group --build` packs it. CI sets the version ([chapter 51](51-releases.md)).
- The desktop entry's `Categories` decide the Front Panel drawer; its `Icon` is a name looked up in the `hicolor` icon theme.
- AppStream metadata gives Software the name, summary, description and screenshot in five languages. The same files will feed the catalogue of an amadeOS package repository.
- `installToAmadeOs` and a line in `repositories.list` put a program into the amadeOS image; `buildDistribution.sh` builds them all.
- jpackage with WiX builds a Windows MSI with its own Java runtime; the upgrade UUID never changes. Only programs that run on Windows get one; the others say *Linux only* in their README.
- CI builds both packages on every change, checks their contents, and keeps them for download.
