# amade-commons

Shared library of the [amadeOS](https://github.com/datazuul/amadeos) desktop and its Swing
applications ([amade-mail](https://github.com/datazuul/amade-mail),
[amade-pad](https://github.com/datazuul/amade-pad), ...): one desktop theme, one Motif look and the
same start-up code everywhere.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-21-orange)

## Contents

| Package | Content |
|---|---|
| `theme` | `DesktopTheme` with the built-in themes *Warm Motif*, *CDE Brocade*, *CDE Desert*, *Classic Gray*, *Dark Slate*; `ThemeRegistry` (custom themes from `/etc/amadeos/themes` and `~/.config/amadeos/themes`); `ThemeStore` (reads `theme.conf` / `icon_theme.conf` and follows changes) |
| `swing` | `AmadeUI` (Motif bevels, buttons, check boxes, radio buttons, menus, and theme colours for every standard component), `EdtExecutor` |
| `app` | `AmadeApplication.launch(...)`, `AppInfo`, `CommonsModule` (Guice) |
| `i18n` | `Language` (English, Deutsch, Français, Español, Italiano), `LocaleStore` (desktop language in `locale.conf`), `LocaleSettings` / `LocaleResolution` (which language an application uses), `Messages` (UTF-8 `.properties` texts), `Mnemonics` (`&Datei`), `LanguageChooser`, `TranslationCheck` |
| `config` | `XdgDirectories`, `AppDirectories` (`~/.config/amadeos/<app>`), `PropertiesFile` (atomic, owner-only), `ConfigWatcher` |
| `desktop` | `DesktopLauncher` (`xdg-open`, refuses attachments that could run code), `PasswordStore` / `SecretToolPasswordStore` (freedesktop Secret Service via `secret-tool`) |

## Themes follow the desktop

The desktop (Style Manager) writes the theme id to `~/.config/amadeos/theme.conf`. Every application
started with `AmadeApplication` watches that file and switches its colours immediately, together with
the window frames and the front panel.

Colours beyond the standard components are registered with `AmadeUI.style(...)`, so they change too:

```java
JPanel bar = AmadeUI.style(new JPanel(), (panel, theme) -> panel.setBackground(theme.softHighlight()));
```

Do not pass theme colours to `setBackground` directly; they would stay after a theme change.
Text fields, lists, tables and trees use `fieldBackground()` / `fieldText()`, which are white/black in
light themes and dark/light in dark themes (*Dark Slate*).

## Languages

amadeOS and its applications speak English, German, French, Spanish and Italian. `AmadeApplication`
decides the language before the first window opens, in this order:

1. the application's own setting, `~/.config/amadeos/<app>/language.conf` (`desktop` or `de`, `fr`, ...),
   written by its *Settings* dialog;
2. the desktop language, `~/.config/amadeos/locale.conf` (e.g. `de_DE.UTF-8`), written by amadeOS;
3. the environment as gettext reads it: `LANGUAGE`, `LC_ALL`, `LC_MESSAGES`, `LANG`, so that an
   application started outside amadeOS uses the language of that desktop;
4. English.

A setting in a language without translation is skipped. The region is kept (`de_AT` formats dates
the Austrian way). The result becomes `Locale.getDefault()` and Swing's default locale, so the JDK's own
dialog buttons and file chooser are translated too; `CommonsModule` binds `LocaleSettings` and the
`Locale`. A changed language takes effect at the next start of the application.

Texts live in `messages.properties` (English, the fallback for every missing key) and
`messages_de.properties`, `_fr`, `_es`, `_it`, all in UTF-8:

```properties
# messages.properties                      # messages_fr.properties
menu.file=&File                            menu.file=&Fichier
player.tracks={0,number} tracks            player.tracks={0,number} titres
radio.recording=Recording to {0}           radio.recording=Enregistrement dans l'album {0}
```

```java
Messages msg = Messages.load(MainWindow.class, "org.datazuul.amadeos.audio.messages");
Mnemonics.apply(new JMenu(), msg.get("menu.file"));
status.setText(msg.format("player.tracks", count));
```

`&` marks the mnemonic letter, so each language picks its own (`&&` is a literal ampersand).
Apostrophes are plain characters, unlike in raw `MessageFormat`. Add a test so that no translation
falls behind:

```java
assertEquals(Map.of(), TranslationCheck.problems(MainWindow.class, "org.datazuul.amadeos.audio.messages"));
```

The *Settings* dialog of an application offers `LanguageChooser` ("Desktop setting (Deutsch)", then
the five languages) and stores the choice with `LocaleSettings.setApplicationLanguage(...)`. When
`restartRequired()` is true, it says so with the `language.restart` text from `Messages.commons(locale)`.

## Usage

```java
public static void main(String[] args) {
    AmadeApplication.launch(new AppInfo("amade-mail", "Mailer", "mail"),
            injector -> injector.getInstance(MainPresenter.class).start(List.of(args)),
            new MailModule());
}
```

`CommonsModule` binds `AppInfo`, `AppDirectories`, an `ExecutorService` on virtual threads,
`EdtExecutor`, `ThemeStore`, `DesktopLauncher`, a `PasswordStore` whose keyring entries carry
`service=<app id>`, `LocaleSettings`, `LocaleStore` and the application's `Locale`.

### Gradle

Published through [JitPack](https://jitpack.io/#datazuul/amade-commons):

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.datazuul:amade-commons:<commit or tag>'
}
```

For development next to an application checkout, add to the application's `settings.gradle`:

```groovy
if (file('../amade-commons/settings.gradle').exists()) {
    includeBuild '../amade-commons'
}
```

Gradle then builds the local `amade-commons` instead of downloading it.

## Building

```bash
./gradlew test
./gradlew publishToMavenLocal
```

## License

[European Union Public Licence v. 1.2](LICENSE) (EUPL-1.2), like amadeOS.
Copyright © 2026 datazuul.com and contributors.
