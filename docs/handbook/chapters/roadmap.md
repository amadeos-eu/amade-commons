# The Road Ahead

The handbook grows with the applications: a chapter is written for an application that is finished enough to teach from. Here is where it stands and where it goes. Each application adds a few new ideas, and amade-commons grows with them: a part moves into the library in the chapter of the second application that needs it.

## Written

| Chapter | Application | New in amade-commons |
|---|---|---|
| [0. Setting Up](00-setup.md) | | |
| [1. Why Swing and Our Own Library](01-why-swing.md) | | |
| [2. How an Application Is Built](02-structure.md) | | |
| [10. Pad](10-pad.md) | amade-pad, the text editor | Uses what every program needs: start-up, Guice wiring, the CDE look and themes, five languages, the EDT executor |
| [11. Web](11-web.md) | amade-web, the documentation viewer | `DesktopLauncher`: opening links and files in other programs, refusing files that could run code |
| [20. Files](20-files.md) | amade-files, the two-panel file manager | `AppDirectories` and `PropertiesFile` for settings, `LaunchFeedback` for the busy cursor; the trash and file operations stay in Files |
| [21. Calendar](21-calendar.md) | amade-calendar, dtcm's successor | `DateChooser` and `LanguageChooser` for dialogs; printing waits in Calendar for its second user |
| [22. Mail](22-mail.md) | amade-mail, the mailer | `PasswordStore` for the desktop keyring; the `Texts` helper that four applications copied moves in |
| [23. WritePad](23-writepad.md) | amade-writepad, the word processor | Printing moves in (`PaperSize`, `PrintJobs`), its second user after Calendar |
| [24. Calculator](24-calculator.md) | amade-calculator, the calculator | Nothing: exact numbers, the expression parser and the keyboard stay in Calculator |
| [25. Contacts](25-contacts.md) | amade-contacts, the address book | `StandardGlyph` grows by *Edit*, *Delete*, *Import*, *Export*, *Mail* and the picture glyphs that Imagescan, Image Viewer and Fractals had each drawn; `ContactDirectory` and `AddressCompletion` give the address books to Mail (completion) and Calendar (birthdays); vCard writing, CSV and LDIF stay in Contacts |
| [26. Backup & Sync](26-backup.md) | amade-backup, backups and folder syncs | Nothing: the engine behind `BackupEngine` (restic today), the disks by UUID and the sync planner stay in Backup & Sync; it is the trash's third writer after Files and Image Viewer, so the writing half of the trash is the next candidate |
| [27. News](27-news.md) | amade-news, the feed reader | Nothing: the feed parser, the catalog and the picture cache stay in News; `ImageCache` is the third cache of pictures from the network after Audio and Image Viewer, so it is the next candidate |
| [30. Fractals](30-fractals.md) | amade-fractals, the fractal explorer | Toolbars with icons drawn in Java2D (`ToolIcon`, `StandardGlyph`, `ToolButtons`) and the `@Ui` executor binding move in, their second user after WritePad |
| [31. Imagescan](31-imagescan.md) | amade-imagescan, the scanning program | `CommandRunner` for other programs moves in from Imagescan, Audio and WritePad, with a time limit that holds; Imagescan becomes the toolbar's third user |
| [32. Video](32-video.md) | amade-video, the video player | Nothing: libmpv through JNA and the player's state stay in Video; its transport symbols wait for their second user, Audio |
| [33. Audio](33-audio.md) | amade-audio, the Audio Station | `Json` moves in from Audio, its second user after Mail; the transport symbols become `TransportGlyph` and `TransportButtons`, their second user after Video |
| [34. Screenshot](34-screenshot.md) | amade-screenshot, the screenshot tool | The pictures folder (`XdgDirectories.pictures()`) and a picture on the clipboard (`ImageSelection`) move in, their third user after Fractals and Imagescan |
| [35. Image Viewer](35-imageviewer.md) | amade-imageviewer, the image viewer | Nothing: orientation, metadata and map tiles stay in Image Viewer; it is the trash's second writer after Files, and the writing half of the trash is the next candidate |
| [36. PDF Viewer](36-pdfviewer.md) | amade-pdfviewer, the PDF viewer | Nothing: PDFBox, the document thread with priorities and the printing on it stay in PDF Viewer; `PrintJobs` split into dialog and printing, and a state folder in `XdgDirectories`, are candidates |
| [37. Books](37-books.md) | amade-books, the e-book and comic reader | Nothing: the typesetter, the CSS subset and the comic reader stay in Books; the hyphenator moves in when WritePad divides words too |
| [38. TV](38-tv.md) | amade-tv, live TV and the program guide | The libmpv binding moves in from Video as the package `mpv` (`Mpv`), its second user; the guide sources, the channel list and the logos stay in TV, and its HTTP client is the third, so the network is the next candidate |
| [39. Image Editor](39-imageeditor.md) | amade-imageeditor, the image editor | Nothing: the history, the crop geometry and the first EXIF writer stay in Image Editor; `Orientation` now exists twice, in Image Viewer and here, so it is the next candidate |
| [40. amade-commons at a Glance](40-commons.md) | Part IV, the manual | The library by subsystem: what each part is for, when to choose it, how it works, with the chapter that introduced it |
| [41](41-start-up.md) to [47](47-programs-json-network.md) | | Start-up and wiring, look and themes, languages, settings and files, desktop integration, toolbars and printing, programs, JSON and the network |
| [50. Packaging and Installers](50-packaging.md) | amade-fractals, packaged | The Debian package, desktop entry and Front Panel drawers, icons, AppStream metadata, the amadeOS image, the Windows installer with jpackage, and CI checks for all of them |
| [51. Versions and Releases](51-releases.md) | amade-calculator, released | Semantic Versioning from Conventional Commits, the branches develop, release and main, the Promote workflow, Debian versions with ~, release notes and AppStream releases, library pins |
| [60. The Desktop and Its Modules](60-desktop.md) | Part VI, amadeos-system | The map of the desktop: desktop modules and tool programs, what they take from amade-commons, who writes and who follows the theme and the language |
| [61. System Information](61-system-information.md) | amadeos-sysinfo | A tool program from `main` to the package; it becomes `CommandRunner`'s next user |
| [62. Settings](62-settings.md) | amadeos-settings | A sidebar with a search and CDE's compact view from one set of pages; the writing side of themes and the text size: `ThemeManager`, atomic files, the root window property; window settings the window manager follows; the X server's own tools; five languages without Guice |
| [63. The Front Panel](63-front-panel.md) | amadeos-panel-cde | A Swing window as an EWMH dock, drawers from desktop entries that follow installations, starting programs, status cells, the panel's language |
| [64. Software](64-software.md) | amadeos-software, amadeos-packages | A root helper behind pkexec and polkit, apt's progress, tests with a fake apt, watching for changes |
| [65. The Clipboard Manager](65-clipboard.md) | amadeos-session clipboard | A program without a window: XFixes instead of polling, an X thread, a worker and the EDT, taking the clipboard over, `SAVE_TARGETS`, passwords left alone |
| [66. The Window Switcher](66-window-switcher.md) | amadeos-wm, Alt+Tab | Global keys and an active keyboard grab, previews read from the screen without a compositor, `_NET_WM_ICON`, a Swing popup the window manager does not manage |
| [67. A Module of Your Own](67-own-module.md) | | The checklist for a new module of amadeos-system |

## Planned

### Appendices

Swing and Motif pitfalls and a glossary. Packaging, first planned as an appendix, became [chapter 50](50-packaging.md).
