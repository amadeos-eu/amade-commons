# 40. amade-commons at a Glance

The application chapters told the library's story in the order the programs needed it: start-up and languages with Pad, settings with Files, the keyring with Mail, the command runner with Imagescan. That is the right order to learn it in, and the wrong order to look something up in. Part IV is the manual: the same library, sorted by what it is for.

Each chapter of this part covers one area. Each section in it describes one part: what it is for, when to choose it and when something else fits better, how it works inside, and its methods. A line at the top of each section names the package, the chapter that introduced the part, and the source files on GitHub, so you can go from the manual to the story and to the code. The tests of amade-commons are its best examples; most sections show one.

| Chapter | Packages | For |
|---|---|---|
| [41. Start-up and Wiring](41-start-up.md) | `app`, `swing` | Starting a program, its identity, what Guice binds for every program, the two threads |
| [42. Look and Themes](42-look.md) | `theme`, `swing` | The desktop's colour themes, custom themes, following Settings, the CDE look, the text size, colours of your own, the date dialog |
| [43. Languages](43-languages.md) | `i18n` | The five languages, which one a program speaks, texts and their formats, menu letters, the language setting, the translation test |
| [44. Settings and Files](44-settings.md) | `config` | Where files go, settings files that survive a crash, following a file the desktop writes |
| [45. Desktop Integration](45-desktop.md) | `desktop` | Opening links and files in other programs, the busy cursor, passwords in the keyring |
| [46. Toolbars, Icons and Printing](46-toolbars-and-printing.md) | `swing`, `print` | Icons drawn in code, toolbar buttons, a player's transport buttons, paper sizes, printing and PostScript |
| [47. Programs, JSON and the Network](47-programs-json-network.md) | `process`, `json`, `net` | Running other programs, testing without them, reading and writing JSON, the proxy of the desktop, how the programs talk to web services |
| [48. Video Playback](48-video-playback.md) | `mpv` | Playing video and streams with libmpv in a Swing window, its events, the video output for this machine |

## What amade-commons is, and is not

amade-commons is a library, not a framework. It calls your program exactly once, in `AmadeApplication.launch`, and from then on only when you hand it something to call: a styler, a listener, an action. There is no base class for windows and no application object to extend.

It holds only what at least two programs share. Everything in it came from a program that needed it first and moved in when a second program needed the same thing: `PasswordStore` from Mail, printing from Calendar and WritePad, `Json` from Mail and Audio. Pad's text decoding, Files' trash and Audio's sound engine stay in their programs, because no second program needs them yet; Video's libmpv binding moved in when TV became its second user. The rule is in [chapter 10](10-pad.md#what-amade-commons-gives-pad), and every chapter since ends with a table of what the program takes from the library and what stays.

The desktop's own tools (the Front Panel, Settings, Software) use amade-commons too. [Part VI](roadmap.md) shows how to write one.

## Ideas that run through the library

A few rules hold for nearly every class. Knowing them makes the rest of the manual shorter.

**The desktop writes, the programs follow.** The theme (`theme.conf`), the desktop language (`locale.conf`) and the icon theme are written by Settings into `~/.config/amadeos`, and so is the text size. Programs only read these files, and a `ConfigWatcher` tells them when the files change. A program's own settings live in a directory of its own below ([chapter 44](44-settings.md#where-files-go-xdgdirectories-and-appdirectories)).

**Two threads.** Swing components are touched only on the event dispatch thread (EDT); slow work runs on the virtual-thread executor that `CommonsModule` binds, and its result comes back through the `@Ui` executor. Listeners that amade-commons calls from a background thread say so in their javadoc, and `AmadeUI.follow` is the example of switching to the EDT ([chapter 41](41-start-up.md#two-threads-edtexecutor-and-ui)).

**Colours come from the theme, at the moment they are needed.** No program stores a theme colour in a component, because the colour would stay after the user picks another theme. Standard components take their colours from Swing's table, which `AmadeUI` fills; anything else uses `AmadeUI.style` or asks `AmadeUI.theme()` while painting ([chapter 42](42-look.md#colours-of-your-own-style)).

**Texts are data.** Every text a user reads comes from a `.properties` file in five languages, menu letters included. Formatting follows the language, apostrophes are plain, and a missing text shows as `!key!` instead of crashing. A test per program checks that every translation is complete ([chapter 43](43-languages.md)).

**Never write over the only copy.** Every file amade-commons writes is written to a temporary file in the same directory and then renamed over the old one, so a crash leaves the old file or the new one, never half of one. Settings files are readable by their owner only ([chapter 44](44-settings.md#settings-files-propertiesfile)).

**Nothing waits forever.** Programs started through `CommandRunner` and `secret-tool` have time limits that hold even when the program hangs without printing anything. Watching threads are daemon threads, so they never keep a closed program alive ([chapter 47](47-programs-json-network.md#running-a-program-commandrunner)).

**Tests need no desktop.** Every class that reads the environment takes it as a parameter: `XdgDirectories` takes a map and a home directory, `LocaleResolution` the environment, `ThemeStore` a directory, `CommandRunner` is an interface a test answers with a lambda. The tests of the library and of the programs run anywhere, with `xvfb-run` for the few that draw.

**Degrade quietly.** Without `xdg-open`, `DesktopLauncher` uses AWT's `Desktop`. Without a keyring, passwords live for the session. Without X11, the busy cursor is skipped. Without a printer, PostScript can still be written. A program never fails to start because something around it is missing.

## I need … : use …

| I need | Use | Section |
|---|---|---|
| To start a program with the language, look and Guice set up | `AmadeApplication.launch`, `AppInfo` | [41](41-start-up.md#starting-a-program-amadeapplication) |
| The background executor, the EDT executor, directories, the locale | `CommonsModule` | [41](41-start-up.md#what-every-program-gets-commonsmodule) |
| To continue on the EDT after background work | `@Ui Executor`, `EdtExecutor` | [41](41-start-up.md#two-threads-edtexecutor-and-ui) |
| The colours of the current theme | `AmadeUI.theme()`, `DesktopTheme` | [42](42-look.md#colour-schemes-desktoptheme) |
| A theme of my own on the desktop | `theme.properties` in `~/.config/amadeos/themes/<id>/` | [42](42-look.md#custom-themes-themeregistry) |
| To follow the theme the user picks in Settings | `AmadeUI.follow` (done by `launch`) | [42](42-look.md#following-the-desktop-themestore) |
| A component with colours beyond the standard ones | `AmadeUI.style` | [42](42-look.md#colours-of-your-own-style) |
| A sunken or raised bevel, a coloured dot | `AmadeUI.sunken()`, `raised()`, `colorDot` | [42](42-look.md#bevels-and-dots) |
| A dialog to pick a date | `DateChooser` | [42](42-look.md#picking-a-date-datechooser) |
| Texts in five languages, formatted | `Messages`, `Texts.tr` | [43](43-languages.md#texts-messages) |
| Texts in a dialog or table model that gets nothing injected | `Texts.tr` | [43](43-languages.md#texts-without-injection-texts) |
| Menu letters that each language chooses | `Mnemonics.apply` | [43](43-languages.md#menu-letters-mnemonics) |
| Which language the program speaks, and why | `LocaleResolution`, `LocaleSettings` | [43](43-languages.md#which-language-localeresolution) |
| A language field in my settings dialog | `LanguageChooser` | [43](43-languages.md#the-language-setting-localesettings-and-languagechooser) |
| A test that every translation is complete | `TranslationCheck` | [43](43-languages.md#a-test-for-every-translation-translationcheck) |
| Where to put settings, caches and data | `AppDirectories`, `XdgDirectories` | [44](44-settings.md#where-files-go-xdgdirectories-and-appdirectories) |
| The user's pictures folder, in their language | `XdgDirectories.pictures()` | [44](44-settings.md#the-pictures-folder) |
| To read and write a settings file safely | `PropertiesFile` | [44](44-settings.md#settings-files-propertiesfile) |
| To react when a file in a directory changes | `ConfigWatcher` | [44](44-settings.md#following-a-file-configwatcher) |
| To open a link in the browser or a file in its program | `DesktopLauncher` | [45](45-desktop.md#opening-links-and-files-desktoplauncher) |
| To know whether opening a file could run code | `DesktopLauncher.isRisky` | [45](45-desktop.md#files-that-could-run-isrisky) |
| To copy a picture to the clipboard | `ImageSelection` | [45](45-desktop.md#a-picture-on-the-clipboard-imageselection) |
| The busy cursor while another program starts | `LaunchFeedback.announce` | [45](45-desktop.md#the-busy-cursor-launchfeedback) |
| A password kept in the desktop keyring | `PasswordStore` | [45](45-desktop.md#passwords-passwordstore) |
| The user's contacts: names, addresses, birthdays | `ContactDirectory` | [45](45-desktop.md#the-address-books-contactdirectory) |
| Address completion in a To or Cc field | `AddressCompletion` | [45](45-desktop.md#completing-addresses-addresscompletion) |
| A toolbar icon that follows the theme and scales | `ToolIcon`, `StandardGlyph` | [46](46-toolbars-and-printing.md#icons-drawn-in-code-toolicon) |
| Toolbar buttons in the Motif style | `ToolButtons` | [46](46-toolbars-and-printing.md#toolbar-buttons-toolbuttons) |
| Play, Pause, Stop and the rest | `TransportButtons`, `TransportGlyph` | [46](46-toolbars-and-printing.md#a-players-buttons-transportbuttons) |
| The usual paper where the user lives | `PaperSize.forLocale` | [46](46-toolbars-and-printing.md#paper-papersize) |
| To print pages with the system's dialog, or to a PostScript file | `PrintJobs` | [46](46-toolbars-and-printing.md#printing-printjobs) |
| To run another program with a time limit | `CommandRunner.run`, `output` | [47](47-programs-json-network.md#running-a-program-commandrunner) |
| A clear message when a program is not installed | `ProgramMissingException` | [47](47-programs-json-network.md#a-program-that-is-not-installed-programmissingexception) |
| Tests that do not depend on installed programs | `FinishedProcess` | [47](47-programs-json-network.md#tests-without-programs-finishedprocess) |
| To read a web service's answer or write a small JSON file | `Json` | [47](47-programs-json-network.md#json) |
| To call a web service | `Network.httpClient()` | [47](47-programs-json-network.md#the-network) |
| The proxy of the desktop, also for programs I start | `Network`, `NetworkProxy`, `ProxyStore` | [47](47-programs-json-network.md#the-proxy-of-the-desktop-network) |
| To play video or a stream in a window | `Mpv` | [48](48-video-playback.md#a-player-in-a-swing-window-mpv) |
| To read mpv's events in a `switch` | `MpvEvent` | [48](48-video-playback.md#events-mpvevent) |

## Which chapter introduced what

| Package | Parts | Introduced in |
|---|---|---|
| `app` | `AmadeApplication`, `AppInfo`, `CommonsModule` | [10. Pad](10-pad.md#starting-it) |
| `app` | `@Ui` | [30. Fractals](30-fractals.md#the-ui-executor) |
| `theme` | `DesktopTheme`, `ThemeRegistry`, `ThemeStore` | [10. Pad](10-pad.md#following-the-desktop-theme) |
| `swing` | `AmadeUI`, `EdtExecutor` | [10. Pad](10-pad.md#following-the-desktop-theme) |
| `swing` | `DateChooser` | [21. Calendar](21-calendar.md#typing-and-picking-dates) |
| `swing` | `ToolIcon`, `StandardGlyph`, `ToolButtons` | [30. Fractals](30-fractals.md#the-toolbar-moves-into-amade-commons) |
| `swing` | `TransportGlyph`, `TransportButtons` | [33. Audio](33-audio.md#the-transport-buttons) |
| `i18n` | `Language`, `LocaleResolution`, `LocaleSettings`, `LocaleStore`, `Messages`, `Mnemonics`, `TranslationCheck` | [10. Pad](10-pad.md#five-languages) |
| `i18n` | `LanguageChooser` | [21. Calendar](21-calendar.md#options-and-the-language) |
| `i18n` | `Texts` | [22. Mail](22-mail.md#the-texts-helper-moves-into-amade-commons) |
| `config` | `XdgDirectories`, `ConfigWatcher` | [10. Pad](10-pad.md#what-amade-commons-gives-pad) |
| `config` | `AppDirectories`, `PropertiesFile` | [20. Files](20-files.md#settings-that-survive) |
| `config` | `XdgDirectories.pictures()` | [34. Screenshot](34-screenshot.md#the-pictures-folder-and-the-clipboard-move-into-amade-commons) |
| `desktop` | `DesktopLauncher` | [11. Web](11-web.md#opening-links-in-other-programs) |
| `desktop` | `LaunchFeedback` | [20. Files](20-files.md#the-busy-cursor) |
| `desktop` | `PasswordStore`, `SecretToolPasswordStore`, `SessionPasswordStore` | [22. Mail](22-mail.md#the-keyring) |
| `desktop` | `ImageSelection` | [34. Screenshot](34-screenshot.md#the-pictures-folder-and-the-clipboard-move-into-amade-commons) |
| `contacts` | `ContactDirectory`, `ContactCard`, `Recipient`, `AddressCompletion` | [25. Contacts](25-contacts.md#contacts-in-other-programs) |
| `print` | `PaperSize`, `PrintJobs` | [23. WritePad](23-writepad.md#printing-moves-into-amade-commons) |
| `process` | `CommandRunner`, `ProgramResult`, `ProgramMissingException`, `FinishedProcess` | [31. Imagescan](31-imagescan.md#the-command-runner-moves-into-amade-commons) |
| `json` | `Json` | [33. Audio](33-audio.md#json) |
| `net` | `Network`, `NetworkProxy`, `ProxySettings`, `ProxyStore`, `ProxyCheck` | [33. Audio](33-audio.md), moved for [38. TV](38-tv.md#and-what-stays-in-tv) |

## Using it in a program

A program gets amade-commons from JitPack, pinned to a commit, and builds a checkout next to it instead when there is one, as [chapter 0](00-setup.md) shows. Pinning a commit instead of a version means a program never changes because the library did: it moves on when its author re-pins it, which is what the "re-pinning" sections of the application chapters do. When you change amade-commons and a program together, pin the program to the library commit that has the change, and merge the program's change no later than the library's.
