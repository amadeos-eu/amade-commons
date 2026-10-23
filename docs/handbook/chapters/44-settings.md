# 44. Settings and Files

Every program remembers something: Files its folders, Mail its accounts, Audio its favourites. And every program follows settings the desktop writes: the theme and the language. This chapter covers where those files go, how a settings file is written so that a crash never destroys it, and how a program hears that a file changed.

## Where files go: XdgDirectories and AppDirectories

*Package `config` · `XdgDirectories` introduced in [10. Pad](10-pad.md#what-amade-commons-gives-pad), `AppDirectories` in [20. Files](20-files.md#settings-that-survive) · source: [`XdgDirectories`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/config/XdgDirectories.java), [`AppDirectories`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/config/AppDirectories.java)*

**What it is for.** The directories of the [XDG Base Directory specification](https://specifications.freedesktop.org/basedir-spec/latest/), which every Linux desktop follows, and the places of amadeOS inside them:

| Method | Default | Holds |
|---|---|---|
| `configHome()` | `~/.config` | Settings |
| `dataHome()` | `~/.local/share` | The user's data that is not a document: a calendar, the trash |
| `cacheHome()` | `~/.cache` | What can be rebuilt: covers, thumbnails, CD titles |
| `amadeosConfig()` | `~/.config/amadeos` | The desktop's settings: `theme.conf`, `locale.conf` |
| `appConfig(name)` | `~/.config/amadeos/<name>` | One program's settings |
| `appCache(name)` | `~/.cache/amadeos/<name>` | One program's cache |

`AppDirectories` is the pair a program needs, `config` and `cache`, built from the `configName` of its `AppInfo`; `configFile(name)` is a file in the first.

**When to choose it.** Ask Guice for `AppDirectories` and put settings into `configFile("mail.conf")` and rebuildable data into `cache()`. Data the user would miss goes below `dataHome()`, which `XdgDirectories` gives; Calendar keeps its calendar in `~/.local/share/amadeos/calendar` ([chapter 21](21-calendar.md#the-file)). Documents go where the user saves them, never into these directories.

**How it works.** The variables `XDG_CONFIG_HOME`, `XDG_DATA_HOME` and `XDG_CACHE_HOME` are used when they hold an absolute path; the specification says a relative value must be ignored:

```java title="amade-commons · config/XdgDirectories.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/config/XdgDirectories.java:base"
```

The constructor takes the environment and the home directory as parameters, and `XdgDirectories.system()` passes the real ones. A test passes a temporary directory, and nothing it does touches the developer's own settings.

??? example "Test: defaults, overrides and a relative value"
    ```java title="amade-commons · test/config/ConfigTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/config/ConfigTest.java:xdg"
    ```

## The pictures folder

*Package `config` · `XdgDirectories.pictures()` introduced in [34. Screenshot](34-screenshot.md#the-pictures-folder-and-the-clipboard-move-into-amade-commons), used by Fractals and Imagescan · source: [`XdgDirectories`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/config/XdgDirectories.java)*

**What it is for.** The folder where the user keeps pictures, under its name in the user's language: `~/Pictures`, `~/Bilder`, `~/Images`. The desktop's first login writes the names of these folders into `user-dirs.dirs` in the configuration directory.

**When to choose it.** As the place a program saves pictures without asking, as Screenshot does, and as the folder a file chooser for pictures opens first, as in Fractals and Imagescan. The folder may not exist yet: create it before saving, or fall back to `home()` in a file chooser.

**How it works.** It reads the line `XDG_PICTURES_DIR=` and accepts the two forms the file may contain, `"$HOME/Bilder"` and an absolute path. Without the file or the line, the answer is `~/Pictures`:

```java title="amade-commons · config/XdgDirectories.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/config/XdgDirectories.java:pictures"
```

??? example "Test: no file, a translated name, an absolute path"
    ```java title="amade-commons · test/config/ConfigTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/config/ConfigTest.java:pictures"
    ```

## Settings files: PropertiesFile

*Package `config` · introduced in [20. Files](20-files.md#settings-that-survive) · source: [`PropertiesFile`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/config/PropertiesFile.java)*

**What it is for.** Reading and writing a `.properties` file in UTF-8, safely. Files, Calendar, Mail, WritePad, Fractals, Imagescan and Audio keep their settings this way.

**When to choose it.** For settings: a handful of keys and values that a user could read and fix in a text editor. A missing key is no error, so a newer version can add keys and an older one ignores them. For structured data, such as Audio's list of favourite stations, write [JSON](47-programs-json-network.md#json); for documents, a format of their own.

**How it works.** Writing goes to a temporary file in the same directory, which is made readable by its owner only, and is then renamed over the old file. A rename within one directory is atomic, so a crash leaves the old file or the new one, never half a file. Where the file system cannot rename atomically, an ordinary replace is the fallback:

```java title="amade-commons · config/PropertiesFile.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/config/PropertiesFile.java:store"
```

`load` returns empty properties for a file that does not exist, and throws `UncheckedIOException` for one that cannot be read. The usual shape around it is a repository with a record of defaults:

```java
Properties p = PropertiesFile.load(directories.configFile("files.conf"));
boolean hidden = Boolean.parseBoolean(p.getProperty("view.showHidden", "false"));
```

!!! warning "Pitfall: settings that stop the program"
    A settings file can be broken: a full disk, an edit by hand. If reading it throws and nobody catches it, the program does not start. Catch the error where the settings are read, log it and go on with the defaults ([chapter 20](20-files.md#settings-that-survive)).

!!! note "Owner only"
    Settings files are `rw-------`, because some hold things that are nobody else's business: Mail's accounts name servers and user names. Passwords never go into them; they go into the [keyring](45-desktop.md#passwords-passwordstore).

??? example "Test: private and atomic, no temporary file left behind"
    ```java title="amade-commons · test/config/ConfigTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/config/ConfigTest.java:properties"
    ```

## Following a file: ConfigWatcher

*Package `config` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`ConfigWatcher`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/config/ConfigWatcher.java)*

**What it is for.** Running an action when one of some files in a directory is created, changed or deleted. `ThemeStore` and `LocaleStore` use it to follow the desktop's settings; a program can use it for a file another program writes.

```java
ConfigWatcher watcher = new ConfigWatcher(directory, Set.of("bookmarks.conf"), "bookmarks-watcher", this::reload);
watcher.start();
...
watcher.close();
```

**When to choose it.** For a file someone else writes while the program runs. A file only the program itself writes needs no watching.

**How it works.** It registers the directory, not the files, with Java's `WatchService`, because a file that is replaced by a rename is a new file and a watch on the old one would see nothing. A daemon thread waits for events, ignores those of other files, waits 150 ms for the writer to finish, then runs the action:

```java title="amade-commons · config/ConfigWatcher.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/config/ConfigWatcher.java:loop"
```

An `OVERFLOW` event, which means events were lost, counts as a change, so the action reads again rather than missing one. `start()` returns false if the file system cannot watch the directory; the program then simply does not follow changes. `close()` stops the thread.

!!! warning "Pitfall: the action runs on the watching thread"
    The action is called on the watcher's own thread. Reading the file there is right; touching Swing is not. Switch to the EDT for that, as `AmadeUI.follow` does ([chapter 42](42-look.md#following-the-desktop-themestore)).

## The desktop's files

These are the files amadeOS programs share. Programs read them; the desktop writes the first three.

| File | Written by | Read by | Holds |
|---|---|---|---|
| `~/.config/amadeos/theme.conf` | Settings | `ThemeStore` | The theme id, e.g. `dark-slate` |
| `~/.config/amadeos/icon_theme.conf` | Settings | `ThemeStore` | The icon theme, e.g. `NsCDE` |
| `~/.config/amadeos/font.conf` | Settings | `ThemeStore` | The text size, 1 to 7, e.g. `4` |
| `~/.config/amadeos/locale.conf` | the desktop's language switcher | `LocaleStore` | The desktop language, e.g. `de_DE.UTF-8` |
| `~/.config/amadeos/themes/<id>/theme.properties` | the user or a theme package | `ThemeRegistry` | A custom theme ([chapter 42](42-look.md#custom-themes-themeregistry)) |
| `~/.config/amadeos/<app>/language.conf` | the program's language setting | `LocaleSettings` | `desktop` or a language code |
| `~/.config/amadeos/<app>/*.conf` | the program | the program | Its own settings, through `PropertiesFile` |
