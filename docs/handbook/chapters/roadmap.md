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
| [33. Audio](33-audio.md) | amade-audio, the Audio Station | `Json` moves in from Audio, its second user after Mail; the transport symbols become `TransportGlyph` and `TransportButtons`, their second user after Video |
| [40. amade-commons at a Glance](40-commons.md) | Part IV, the manual | The library by subsystem: what each part is for, when to choose it, how it works, with the chapter that introduced it |
| [41](41-start-up.md) to [47](47-programs-json-network.md) | | Start-up and wiring, look and themes, languages, settings and files, desktop integration, toolbars and printing, programs, JSON and the network |
| [50. Packaging and Installers](50-packaging.md) | amade-fractals, packaged | The Debian package, desktop entry and Front Panel drawers, icons, AppStream metadata, the amadeOS image, the Windows installer with jpackage, and CI checks for all of them |
| [60. The Desktop and Its Modules](60-desktop.md) | Part VI, amadeos-system | The map of the desktop: desktop modules and tool programs, what they take from amade-commons, who writes and who follows the theme and the language |
| [61. System Information](61-system-information.md) | amadeos-sysinfo | A tool program from `main` to the package; it becomes `CommandRunner`'s next user |
| [62. Style Manager](62-style-manager.md) | amadeos-style | The writing side of themes: `ThemeManager`, atomic files, the root window property, the X server's own tools, five languages without Guice |
| [63. The Front Panel](63-front-panel.md) | amadeos-panel-cde | A Swing window as an EWMH dock, drawers from desktop entries that follow installations, starting programs, status cells, the panel's language |
| [64. Software](64-software.md) | amadeos-software, amadeos-packages | A root helper behind pkexec and polkit, apt's progress, tests with a fake apt, watching for changes |
| [65. The Clipboard Manager](65-clipboard.md) | amadeos-session clipboard | A program without a window: XFixes instead of polling, an X thread, a worker and the EDT, taking the clipboard over, `SAVE_TARGETS`, passwords left alone |
| [66. A Module of Your Own](66-own-module.md) | | The checklist for a new module of amadeos-system |

## Planned

### Appendices

Swing and Motif pitfalls and a glossary. Packaging, first planned as an appendix, became [chapter 50](50-packaging.md).
