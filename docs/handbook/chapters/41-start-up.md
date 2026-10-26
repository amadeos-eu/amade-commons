# 41. Start-up and Wiring

Every amadeOS program starts the same way: one call in `main` decides the language, installs the desktop's look, builds the Guice injector and hands it to the program. This chapter covers that call, what the injector holds for every program, and the two executors that keep slow work off the event dispatch thread.

## Starting a program: AmadeApplication

*Package `app` · introduced in [10. Pad](10-pad.md#starting-it) · source: [`AmadeApplication`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/app/AmadeApplication.java)*

**What it is for.** `AmadeApplication.launch` is the whole start-up of a program. You pass who the program is, what to do once the injector exists, and the program's own Guice modules:

```java
public static void main(String[] args) {
    AmadeApplication.launch(new AppInfo("amade-mail", "Mailer", "mail"),
            injector -> injector.getInstance(MainPresenter.class).start(List.of(args)),
            new MailModule());
}
```

**When to choose it.** In every desktop program. A command-line tool or a test does not need it: tests build the injector themselves with a `CommonsModule` that points into a temporary directory ([below](#what-every-program-gets-commonsmodule)).

**How it works.** The order of the steps matters:

```java title="amade-commons · app/AmadeApplication.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/app/AmadeApplication.java:launch"
```

1. **The language first**, on the main thread and before any Swing class is used. `Locale.setDefault` then reaches the JDK's own texts (*OK*, *Cancel*, the file chooser) and every date and number format. Which language it is, is decided by [`LocaleSettings`](43-languages.md#the-language-setting-localesettings-and-languagechooser).
2. **The network proxy**, still before any connection: [`Network.install()`](47-programs-json-network.md#the-proxy-of-the-desktop-network) makes the proxy of Settings > Network the JVM's default and follows it.
3. **Everything else on the EDT**: the components' default locale, the look in the current theme's colours, and the order to follow the theme ([chapter 42](42-look.md#following-the-desktop-themestore)).
4. **One executor** of virtual threads for the program's background work, created here so that the program and the library share it.
5. **The injector** from `CommonsModule` and the program's modules, handed to the start function, still on the EDT, where the program may create its first window.
6. **A failed start** is logged and shown in a message box in the user's language. Without it, a program clicked in the Front Panel would silently not appear.

!!! warning "Pitfall: work in `main` before `launch`"
    Anything that touches Swing before `launch`, even a `JOptionPane` for a quick check, loads Swing's classes in the JVM's own locale and the old look. Put such code into the start function, where the language and the look are already set.

## The identity of a program: AppInfo

*Package `app` · introduced in [10. Pad](10-pad.md#starting-it) · source: [`AppInfo`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/app/AppInfo.java)*

**What it is for.** `AppInfo` names the program three times, for three audiences:

| Field | Example | Used for |
|---|---|---|
| `id` | `amade-mail` | The system: the Debian package, the keyring service of its passwords, log lines |
| `title` | `Mailer` | People: window titles and the start-up error |
| `configName` | `mail` | The directory `~/.config/amadeos/mail` and the cache `~/.cache/amadeos/mail` |

**How it works.** The record checks that `configName` is a plain lower-case name (`[a-z0-9][a-z0-9._-]*`), so a typo such as `../mail` can never put a program's files outside its directory.

## What every program gets: CommonsModule

*Package `app` · introduced in [10. Pad](10-pad.md#what-amade-commons-gives-pad) · source: [`CommonsModule`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/app/CommonsModule.java)*

**What it is for.** The Guice bindings every program needs, so that a presenter or a service can ask for them in its constructor:

```java title="amade-commons · app/CommonsModule.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/app/CommonsModule.java:configure"
```

| Type | What it is | Details |
|---|---|---|
| `AppInfo` | The program's identity | [above](#the-identity-of-a-program-appinfo) |
| `XdgDirectories`, `AppDirectories` | Where files go | [44](44-settings.md#where-files-go-xdgdirectories-and-appdirectories) |
| `ExecutorService` | Background work on virtual threads | [below](#two-threads-edtexecutor-and-ui) |
| `EdtExecutor`, `@Ui Executor` | Continuing on the EDT | [below](#two-threads-edtexecutor-and-ui) |
| `ThemeStore` | The desktop theme | [42](42-look.md#following-the-desktop-themestore) |
| `DesktopLauncher` | Opening links and files | [45](45-desktop.md#opening-links-and-files-desktoplauncher) |
| `PasswordStore` | The keyring, under the program's `id` | [45](45-desktop.md#passwords-passwordstore) |
| `LocaleSettings`, `LocaleStore`, `Locale` | The language the program runs in, and its setting | [43](43-languages.md#the-language-setting-localesettings-and-languagechooser) |
| `ContactDirectory` | The user's address books, read-only | [45](45-desktop.md#the-address-books-contactdirectory) |

**When to choose it.** `launch` installs it for you. Build it yourself in tests that need the real wiring, with an `XdgDirectories` pointing into a temporary directory so that nothing touches the developer's own settings; the test below does that. Most tests need none of it and pass their collaborators by hand.

**How it works.** Every binding is an instance made before the injector, not a class Guice constructs. That keeps the library's parts free of Guice annotations, so a program that does not use Guice could still use them. A program's own module adds its presenters and services and may use everything above.

!!! tip "Tip: the texts are the program's"
    `CommonsModule` binds the `Locale`, not the texts, because only the program knows where its texts are. Pad provides its `Messages` in its module with a `@Provides` method ([chapter 2](02-structure.md#wiring-it-together-guice)); Mail and the later programs use `Texts`, which finds them through a declaration file ([chapter 43](43-languages.md#texts-without-injection-texts)).

??? example "Test: the `@Ui` executor is the EDT executor"
    ```java title="amade-commons · test/app/CommonsModuleTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/app/CommonsModuleTest.java:ui-executor"
    ```

## Two threads: EdtExecutor and @Ui

*Package `swing` and `app` · introduced in [10. Pad](10-pad.md#work-in-the-background), `@Ui` in [30. Fractals](30-fractals.md#the-ui-executor) · source: [`EdtExecutor`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/EdtExecutor.java), [`Ui`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/app/Ui.java)*

**What it is for.** Swing allows its components to be touched only on the event dispatch thread, and anything slow on that thread freezes the window. So a program reads files, asks servers and computes on the background `ExecutorService`, and continues on the EDT with the result. `EdtExecutor` is the executor for that second half:

```java
CompletableFuture.supplyAsync(() -> repository.load(file), background)
        .whenCompleteAsync((document, error) -> view.show(document, error), ui);
```

**When to choose it.** A presenter takes the `@Ui Executor` as a plain `Executor`, not the `EdtExecutor` class. In tests it then gets a direct executor (`Runnable::run`) and runs everything on the test thread, or a queueing one and decides when the "EDT" runs. Code that is Swing anyway, such as a component's own timer, takes the `EdtExecutor` or calls `SwingUtilities.invokeLater` itself.

**How it works.** It is two lines: a task submitted on the EDT runs at once, any other is queued with `invokeLater`. `requireEdt()` is a guard for methods that must only be called on the EDT; it throws instead of corrupting a component that is touched from two threads.

```java title="amade-commons · swing/EdtExecutor.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/EdtExecutor.java"
```

The background executor makes a new virtual thread per task. Virtual threads are cheap enough that a program never pools them, and waiting for a file, a server or another program costs no platform thread ([chapter 2](02-structure.md#two-threads)).

!!! warning "Pitfall: running on the EDT at once"
    Because a task submitted on the EDT runs at once, `ui.execute(...)` called from the EDT is not "later". Code that relies on a component being laid out first still needs `SwingUtilities.invokeLater`.

!!! warning "Pitfall: listeners from the library"
    `ThemeStore` and `LocaleStore` call their listeners on their watching thread, not on the EDT. A listener that touches Swing must switch first, as `AmadeUI.follow` does ([chapter 42](42-look.md#following-the-desktop-themestore)).
