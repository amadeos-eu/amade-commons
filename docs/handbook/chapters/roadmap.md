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
| [30. Fractals](30-fractals.md) | amade-fractals, the fractal explorer | Toolbars with icons drawn in Java2D (`ToolIcon`, `StandardGlyph`, `ToolButtons`) and the `@Ui` executor binding move in, their second user after WritePad |
| [31. Imagescan](31-imagescan.md) | amade-imagescan, the scanning program | `CommandRunner` for other programs moves in from Imagescan, Audio and WritePad, with a time limit that holds; Imagescan becomes the toolbar's third user |
| [32. Video](32-video.md) | amade-video, the video player | Nothing: libmpv through JNA and the player's state stay in Video; its transport symbols wait for their second user, Audio |
| [50. Packaging and Installers](50-packaging.md) | amade-fractals, packaged | The Debian package, desktop entry and Front Panel drawers, icons, AppStream metadata, the amadeOS image, the Windows installer with jpackage, and CI checks for all of them |

## Planned

### Part III: Graphics and Media

| Chapter | Application | New ideas | New in amade-commons |
|---|---|---|---|
| 33. Audio | amade-audio | Sound output, decoders, web radio, CD, a mixer, custom widgets | `Json` moves in (second user after Mail), and the transport icons (second user after Video) |

### Part IV: The amade-commons Manual

The library by subsystem rather than by application, as a reference once the applications are read: 40 overview, 41 start-up and wiring, 42 look and themes, 43 languages, 44 settings and files, 45 desktop integration, 46 toolbars, icons and printing, 47 programs, JSON and the network. Each section names the chapter that introduced its part.

### Part VI: Desktop Modules

The amadeOS desktop's own tools (Front Panel, Style Manager, Software, system information) use amade-commons too. A later part shows how to write one.

### Appendices

Swing and Motif pitfalls and a glossary. Packaging, first planned as an appendix, became [chapter 50](50-packaging.md).
