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

## Planned

### Part II: Office

| Chapter | Application | New ideas | New in amade-commons |
|---|---|---|---|
| 23. WritePad | amade-writepad, a word processor | Swing's text kit, a page view, document formats (ODT, DOCX, RTF) | Printing moves in, its second user after Calendar |

### Part III: Graphics and Media

| Chapter | Application | New ideas | New in amade-commons |
|---|---|---|---|
| 30. Fractals | amade-fractals | Java2D, calculations on all processor cores, exporting images | Toolbars with icons drawn in Java2D (`ToolIcon`, `ToolButtons`) and the `@Ui` executor binding move in, their second user after WritePad |
| 31. Imagescan | amade-imagescan | Scanners through SANE and WIA, image operations, undo for images | `CommandRunner` for external programs moves in |
| 32. Video | amade-video | A native library (libmpv) through JNA, video in a Swing window | |
| 33. Audio | amade-audio | Sound output, decoders, web radio, CD, a mixer, custom widgets | `Json` moves in (second user after Mail), and the transport icons (second user after Video) |

### Part IV: The amade-commons Manual

The library by subsystem rather than by application, as a reference once the applications are read: 40 overview, 41 start-up and wiring, 42 look and themes, 43 languages, 44 settings and files, 45 desktop integration, 46 toolbars, icons and printing, 47 programs, JSON and the network. Each section names the chapter that introduced its part.

### Part V: Desktop Modules

The amadeOS desktop's own tools (Front Panel, Style Manager, Software, system information) use amade-commons too. A later part shows how to write one.

### Appendices

Swing and Motif pitfalls, a packaging reference (Debian packages, desktop entries and the Front Panel drawers), and a glossary.
