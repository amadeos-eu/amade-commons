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
| `config` | `XdgDirectories`, `AppDirectories` (`~/.config/amadeos/<app>`), `PropertiesFile` (atomic, owner-only) |
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

## Usage

```java
public static void main(String[] args) {
    AmadeApplication.launch(new AppInfo("amade-mail", "Mailer", "mail"),
            injector -> injector.getInstance(MainPresenter.class).start(List.of(args)),
            new MailModule());
}
```

`CommonsModule` binds `AppInfo`, `AppDirectories`, an `ExecutorService` on virtual threads,
`EdtExecutor`, `ThemeStore`, `DesktopLauncher` and a `PasswordStore` whose keyring entries carry
`service=<app id>`.

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
