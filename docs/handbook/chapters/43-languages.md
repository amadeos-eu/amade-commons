# 43. Languages

amadeOS speaks English, German, French, Spanish and Italian, and so does every program. This chapter covers the list of languages, how a program decides which one it speaks, the desktop's and the program's language settings, the texts and their formats, menu letters, and the test that keeps every translation complete.

## The five languages: Language

*Package `i18n` · introduced in [10. Pad](10-pad.md#five-languages) · source: [`Language`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/i18n/Language.java)*

**What it is for.** The languages amadeOS is translated into, as an enum: `ENGLISH`, `GERMAN`, `FRENCH`, `SPANISH`, `ITALIAN`. English is the source language and the fallback for every missing text.

| Method | Example |
|---|---|
| `code()` | `de`, the value in settings files |
| `nativeName()` | `Deutsch`, shown in language menus |
| `locale()`, `posixLocale()` | `de-DE`, `de_DE.UTF-8` |
| `ofCode(code)`, `of(locale)` | The language, or empty if amadeOS is not translated into it |
| `parseLocale(value)` | A `Locale` from `de`, `de_AT`, `de_AT.UTF-8@euro` or `de-AT` |

**How it works.** `parseLocale` reads every form a locale is written in on Linux and keeps the region, so an Austrian desktop gets German texts with Austrian dates. It returns empty for `C`, `POSIX`, garbage and languages without a translation, so the caller goes on to the next setting:

```java title="amade-commons · i18n/Language.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/i18n/Language.java:parse"
```

!!! tip "Tip: a sixth language"
    A new language is a new constant here, a file `commons_xx.properties` with amade-commons' own texts, and a file per program. `TranslationCheck` then reports every program that still lacks it ([chapter 10](10-pad.md#exercises)).

## Which language: LocaleResolution

*Package `i18n` · introduced in [10. Pad](10-pad.md#which-language) · source: [`LocaleResolution`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/i18n/LocaleResolution.java)*

**What it is for.** Deciding the language at start-up, and remembering why. Four places are asked in order, and the first that names a language amadeOS is translated into wins:

1. The program's own setting, `~/.config/amadeos/<app>/language.conf`.
2. The desktop's language, `~/.config/amadeos/locale.conf`.
3. The environment, as gettext reads it: `LANGUAGE`, then `LC_ALL`, `LC_MESSAGES`, `LANG`. This is how a program speaks the right language on another desktop.
4. English.

```java title="amade-commons · i18n/LocaleResolution.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/i18n/LocaleResolution.java:resolve"
```

**When to choose it.** `LocaleSettings` calls it; a program reads the result from `LocaleSettings.applied()`. Its `source()` (`APPLICATION`, `DESKTOP`, `ENVIRONMENT`, `DEFAULT`) is logged at start-up, which answers "why does it speak Italian?" at once.

**How it works.** A program's own language keeps the region of the desktop or the environment when the language is the same: French chosen in the program on a Swiss desktop (`fr_CH`) gives Swiss French. `LANGUAGE` is a priority list (`de:fr`) and is ignored when the locale is `C`, as gettext does.

??? example "Test: the order of the four places"
    ```java title="amade-commons · test/i18n/I18nTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/i18n/I18nTest.java:order"
    ```

## The desktop language: LocaleStore

*Package `i18n` · introduced in [10. Pad](10-pad.md#which-language) · source: [`LocaleStore`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/i18n/LocaleStore.java)*

**What it is for.** The language of the desktop, in `~/.config/amadeos/locale.conf`: one line with a POSIX locale such as `de_DE.UTF-8`. The login greeter exports it as `LANG` for the session, so programs outside amadeOS follow it too.

**When to choose it.** Programs read it through `LocaleSettings`. Only the desktop's own language switcher calls `write(language)`, the one method that changes it. `watch()` and the listeners exist for the desktop, which can relabel itself; a program's language changes only at its next start ([below](#the-language-setting-localesettings-and-languagechooser)).

**How it works.** Like [`ThemeStore`](42-look.md#following-the-desktop-themestore): it reads the file once, never fails (a missing or unknown locale is empty), writes atomically, and follows the file with a [`ConfigWatcher`](44-settings.md#following-a-file-configwatcher), calling its listeners on the watching thread.

## The language setting: LocaleSettings and LanguageChooser

*Package `i18n` · introduced in [10. Pad](10-pad.md#which-language), `LanguageChooser` in [21. Calendar](21-calendar.md#options-and-the-language) · source: [`LocaleSettings`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/i18n/LocaleSettings.java), [`LanguageChooser`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/i18n/LanguageChooser.java)*

**What it is for.** A program's own language, which the user may set to differ from the desktop's: `language.conf` holds `desktop` or a language code. `LocaleSettings` reads it, resolves the language with the desktop's and the environment's, and knows whether a changed setting needs a restart. `LanguageChooser` is the combo box for a settings dialog.

**When to choose it.** `CommonsModule` binds `LocaleSettings`. A program with an options dialog puts a `LanguageChooser` on it, as Calendar, Mail and Audio do; Pad, without a dialog, builds a menu from the same parts.

```java title="amade-commons · i18n/LanguageChooser.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/i18n/LanguageChooser.java:choices"
```

The first entry follows the desktop and says which language that is now. Every language is listed under its own name, *Français*, not *French*: a user who switched to a language they cannot read must still find their own.

**How it works.** The resolution at start-up is kept as `applied()`. After the user stored a new choice with `setApplicationLanguage`, `restartRequired()` compares it with the settings as they are now:

```java title="amade-commons · i18n/LocaleSettings.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/i18n/LocaleSettings.java:restart"
```

If it is true, the program says so with the text `language.restart` from amade-commons ("The new language is used the next time Mailer starts."). Changing the language of a running Swing program would mean rebuilding every window, menu and dialog, including the JDK's own file chooser; a restart is simpler and the user is told.

## Texts: Messages

*Package `i18n` · introduced in [10. Pad](10-pad.md#five-languages) · source: [`Messages`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/i18n/Messages.java)*

**What it is for.** A program's texts in one language, from `.properties` files in UTF-8: `messages.properties` in English, `messages_de.properties`, `_fr`, `_es` and `_it` next to it.

**When to choose it.** Inject it where the program binds it, or reach it through [`Texts`](#texts-without-injection-texts). `Messages.commons(locale)` gives amade-commons' own texts: the language menu, the start-up error and the date dialog.

**How it works.** Loading uses a `ResourceBundle.Control` without fallback to the JVM's default locale: a language without a file gets English, never the language of the developer's computer.

```java title="amade-commons · i18n/Messages.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/i18n/Messages.java:load"
```

`format` fills in `{0}`, `{1}`, ... with `MessageFormat` in the locale, so numbers, dates and `{0,choice,...}` plurals follow the language. Before formatting it doubles every apostrophe, so translators write `l'album` instead of `MessageFormat`'s `l''album`. A missing key shows as `!key!` and is logged once:

```java title="amade-commons · i18n/Messages.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/i18n/Messages.java:format"
```

| Method | What it does |
|---|---|
| `load(anchor, baseName, locale)`, `load(anchor, baseName)` | The texts next to the class `anchor`; the second in the default locale |
| `commons(locale)` | amade-commons' own texts |
| `get(key)`, `format(key, args...)` | A text as written, or with its arguments filled in |
| `has(key)`, `locale()` | Whether a text exists, in which locale they were loaded |

!!! warning "Pitfall: a literal brace"
    Because apostrophes are plain, `MessageFormat`'s quoting is gone: a text that is formatted cannot show a literal `{`. Such a text is logged as a bad pattern and shown unformatted. Pass the brace in as an argument instead, or use `get` for a text without arguments.

??? example "Test: English as the fallback, never the JVM's language"
    ```java title="amade-commons · test/i18n/I18nTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/i18n/I18nTest.java:fallback"
    ```

## Texts without injection: Texts

*Package `i18n` · introduced in [22. Mail](22-mail.md#the-texts-helper-moves-into-amade-commons) · source: [`Texts`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/i18n/Texts.java)*

**What it is for.** The program's texts for code that gets nothing injected: dialogs, table models, renderers, records with a label. A static import gives every class `tr`:

```java
import static eu.amadeos.commons.i18n.Texts.tr;

new JButton(tr("compose.send"));                 // Send
status.setText(tr("status.fetched", count));     // {0} new messages
```

**When to choose it.** In every program since Mail. It is a global, which is acceptable for texts because they are read-only and the same everywhere in a program; anything that changes stays injected.

**How it works.** The program names its text files in one resource, `META-INF/amadeos/texts.properties`:

```properties title="src/main/resources/META-INF/amadeos/texts.properties"
bundle=eu.amadeos.apps.mail.messages
```

A file instead of a call in `main` means that the program and its tests find the texts the same way, without set-up code in every test. There must be exactly one such file on the class path; none or two is an error that says so.

```java title="amade-commons · i18n/Texts.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/i18n/Texts.java:declared"
```

The texts are loaded on first use in the default locale, and loaded again when the default locale changes, which tests use to check a translation:

```java title="amade-commons · i18n/Texts.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/i18n/Texts.java:messages"
```

`Texts.use(anchor, bundle)` points it at other files, for a library's own tests; `Texts.bundle()` gives the base name to `TranslationCheck`. amade-commons itself uses `tr` only in [`TransportButtons`](46-toolbars-and-printing.md#a-players-buttons-transportbuttons), whose tooltips are the program's texts.

??? example "Test: the declared texts follow the default locale"
    ```java title="amade-commons · test/i18n/TextsTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/i18n/TextsTest.java:declared"
    ```

## Menu letters: Mnemonics

*Package `i18n` · introduced in [10. Pad](10-pad.md#mnemonics) · source: [`Mnemonics`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/i18n/Mnemonics.java)*

**What it is for.** The underlined letter of a menu, a button or a label, chosen by each language inside its text: `&File`, `&Datei`, `&Fichier`. `&&` is a literal ampersand.

```java title="amade-commons · i18n/Mnemonics.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/i18n/Mnemonics.java:apply"
```

| Method | What it does |
|---|---|
| `apply(button, marked)` | Text and mnemonic of a button, menu or menu item |
| `apply(label, marked)` | The same for a label; pair it with `setLabelFor` |
| `strip(marked)` | The text without markers, for tooltips and accessible names |
| `parse(marked)` | The text and the index of the letter, -1 if none |

**How it works.** The key code comes from `KeyEvent.getExtendedKeyCodeForChar`, so letters outside ASCII (`&Öffnen`) work as well.

## A test for every translation: TranslationCheck

*Package `i18n` · introduced in [10. Pad](10-pad.md#testing-the-translations) · source: [`TranslationCheck`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/i18n/TranslationCheck.java)*

**What it is for.** A unit test per program that compares every translation with the English file. A forgotten text is then found at build time, not by a user in a dialog that appears once a year:

```java
assertEquals(Map.of(), TranslationCheck.problems(MainWindow.class, Texts.bundle()));
```

**How it works.** For each language it reports keys that are missing, keys English does not have (usually a typo), and texts whose `{n}` arguments differ from English. A language without problems is left out, so the expected result is an empty map, and a failing test prints exactly what to fix.

```java title="amade-commons · i18n/TranslationCheck.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/i18n/TranslationCheck.java:problems"
```

!!! tip "Tip: check a few texts as a reader would"
    `TranslationCheck` knows that a text exists, not that it reads well. Each program's translation test also formats a few texts in each language and compares them with what a native speaker expects: plurals, numbers, the apostrophes of French and Italian.
