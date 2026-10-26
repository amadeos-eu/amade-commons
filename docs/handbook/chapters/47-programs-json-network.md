# 47. Programs, JSON and the Network

Desktop programs on Linux lean on other programs: Imagescan asks `scanimage` for a picture, Audio asks `wpctl` for the volume, WritePad hands documents to a converter. And some of them talk to web services: Mail signs in through the browser, Audio looks up CDs and radio stations. This chapter covers running other programs safely, testing without them, reading and writing JSON, the proxy of the desktop, which every program uses without doing anything, and the rules the programs follow on the network.

## Running a program: CommandRunner

*Package `process` · introduced in [31. Imagescan](31-imagescan.md#commandrunner-in-amade-commons) · source: [`CommandRunner`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/process/CommandRunner.java)*

**What it is for.** Starting another program, and for short commands, running it to its end and collecting what it printed:

```java
ProgramResult result = runner.run(List.of("scanimage", "-L"), Duration.ofSeconds(20));
String volume = runner.output(List.of("wpctl", "get-volume", "@DEFAULT_AUDIO_SINK@"), Duration.ofSeconds(3));
```

**When to choose it.** Whenever a program runs another one. Code takes a `CommandRunner` instead of calling `ProcessBuilder` itself, so that tests answer with canned output ([below](#tests-without-programs-finishedprocess)) and never depend on what is installed. `CommandRunner.system()` is the real one:

```java title="amade-commons · process/CommandRunner.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/process/CommandRunner.java:system"
```

| Method | For |
|---|---|
| `start(command)` | A long-running program whose output is a stream, such as a scanned picture; read the `Process` yourself |
| `run(command, timeout)` | A short command: exit code and both outputs as bytes, in a [`ProgramResult`](#what-a-program-printed-programresult) |
| `output(command, timeout)` | A short command whose standard output is text; any failure is an `IOException` with the program's own message |

**How it works.** `run` handles the parts that are easy to get wrong. Standard input is closed at once, so a program that asks a question gets an end of file instead of waiting. Both outputs are read at the same time on virtual threads, because a program that fills one pipe while nobody reads it blocks for good. And the time limit holds even for a program that hangs without printing anything; when it runs out, the program is stopped:

```java title="amade-commons · process/CommandRunner.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/process/CommandRunner.java:run"
```

`output` turns every failure into one `IOException` whose message says what happened, `wpctl failed (exit code 1): Connection refused`, ready for a status bar:

```java title="amade-commons · process/CommandRunner.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/process/CommandRunner.java:output"
```

!!! warning "Pitfall: reading the output after `waitFor`"
    The obvious code, `waitFor()` and then `readAllBytes()`, works until a program prints more than the pipe holds (64 KB on Linux). The program then waits for a reader, the caller waits for the program, and both wait forever. Read while waiting, as `run` does ([chapter 31](31-imagescan.md#commandrunner-in-amade-commons)).

!!! warning "Pitfall: a shell"
    `List.of("sh", "-c", "scanimage " + device)` lets a device name with a `;` in it run anything. Pass each argument as its own list element; no shell is involved, and nothing in an argument is interpreted.

??? example "Test: the time limit holds for a program that prints nothing"
    ```java title="amade-commons · test/process/CommandRunnerTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/process/CommandRunnerTest.java:hang"
    ```

## What a program printed: ProgramResult

*Package `process` · introduced in [31. Imagescan](31-imagescan.md#commandrunner-in-amade-commons) · source: [`ProgramResult`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/process/ProgramResult.java)*

**What it is for.** The exit code and both outputs of a finished program. The outputs are bytes, because standard output may be a picture or text in any encoding; `stdoutText()` and `stderrText()` decode them as UTF-8, or in a charset you pass. `succeeded()` is true for exit code 0.

**How it works.** `failure(program)` builds the message for a failed run from the error output, or the standard output when a program prints its errors there:

```java title="amade-commons · process/ProgramResult.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/process/ProgramResult.java:failure"
```

## A program that is not installed: ProgramMissingException

*Package `process` · introduced in [31. Imagescan](31-imagescan.md#a-missing-program-is-a-message-not-an-error) · source: [`ProgramMissingException`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/process/ProgramMissingException.java)*

**What it is for.** Telling "the program is not installed" apart from every other failure. A missing program is a message for the user, usually with the package to install (Imagescan: "The scanning program scanimage is not installed", followed by `sudo apt install sane-utils`), not an error report.

**When to choose it.** Catch it where a program offers a feature that needs another program: Imagescan without `scanimage`, Audio without `wpctl`. `CommandRunner.system()` throws it from `start`, `run` and `output`; a test runner can throw it to check the message.

**How it works.** The JDK reports a missing program as an `IOException` with "error=2" in its message. `check` turns that one into a `ProgramMissingException` and leaves other errors, such as a missing permission, as they are:

```java title="amade-commons · process/ProgramMissingException.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/process/ProgramMissingException.java:check"
```

## Tests without programs: FinishedProcess

*Package `process` · introduced in [31. Imagescan](31-imagescan.md#fakes-for-the-tests) · source: [`FinishedProcess`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/process/FinishedProcess.java)*

**What it is for.** A `Process` that has already ended with a given output. Because `CommandRunner` has one abstract method, a test answers with a lambda that returns one:

```java
SoundServer server = new SoundServer(command -> FinishedProcess.printing("Volume: 0.25"));
```

**When to choose it.** In every test of code that runs programs. `printing(stdout)` is a successful run, `failing(exitCode, stderr)` a failed one, and the constructor takes both outputs as text or bytes. The lambda can also record the commands, to check what would have been run.

??? example "Test: a fake runner that records the commands"
    ```java title="amade-commons · test/process/CommandRunnerTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/process/CommandRunnerTest.java:fake"
    ```

## JSON

*Package `json` · introduced in [33. Audio](33-audio.md#json) · source: [`Json`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/json/Json.java)*

**What it is for.** Reading the answers of web services and token endpoints, and reading and writing small JSON files. Mail reads its sign-in tokens with it; Audio reads MusicBrainz, the radio station directory and its favourites file.

**When to choose it.** For JSON you read a few members from, and for small files you write. Objects become `LinkedHashMap`s, so members keep their order, arrays `List`s, numbers `Double`s. A program that maps large JSON documents onto many classes would want a real JSON library; none of the programs does.

**How it works.** `parse` is a small recursive parser and throws `IllegalArgumentException` on text that is not JSON:

```java title="amade-commons · json/Json.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/json/Json.java:parse"
```

The reading helpers are the reason the class exists. Web services leave members out, and send numbers as strings or strings as numbers. The helpers never throw on a missing or mistyped member, they return `""`, `0` or an empty list, so reading a release date is one line instead of five checks:

```java title="amade-commons · json/Json.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/json/Json.java:helpers"
```

```java
Map<String, Object> token = Json.parseObject(response.body());
String access = Json.str(token, "access_token");               // "" if missing
int seconds = Json.integer(token, "expires_in");               // 0 if missing
String name = Json.str(Json.member(answer, "artist"), "name");  // nested: "" if anything on the way is missing
```

`write(value)` writes maps, lists, strings, numbers, booleans and `null`, indented two spaces, for files people may read. Whole numbers are written without `.0`.

??? example "Test: it writes what it reads"
    ```java title="amade-commons · test/json/JsonTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/json/JsonTest.java:round-trip"
    ```

## The proxy of the desktop: Network

*Package `net` · introduced in [33. Audio](33-audio.md) and moved into amade-commons for TV · source: [`Network`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/net/Network.java), [`NetworkProxy`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/net/NetworkProxy.java), [`ProxyStore`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/net/ProxyStore.java)*

**What it is for.** In an office or a school the way out to the internet often leads through a proxy. amadeOS has one place for it, Settings > Network ([chapter 62](62-settings.md)), and every program follows it: its own connections, the programs it starts (mpv, yt-dlp, restic, the browser) and the video it plays.

**When to choose it.** Nothing to do for connections with `HttpURLConnection`: `AmadeApplication.launch` installs the proxy as the JVM's default. For `HttpClient`, take the builder from `Network`, which adds the proxy password and the usual limits:

```java
HttpClient client = Network.httpClient().build();     // proxy and its password, 10 s to connect, redirects followed
```

Programs started through [`CommandRunner.system()`](#running-a-program-commandrunner) and links opened with `DesktopLauncher` get the proxy in their environment; [`Mpv`](48-video-playback.md) hands it to libmpv. Code that builds a `ProcessBuilder` itself calls `Network.proxy().applyTo(builder.environment())`.

**How it works.** The settings are a record with three modes:

| Mode | Connections go | Programs started get |
|---|---|---|
| `SYSTEM` (no `proxy.conf`) | through `http_proxy`/`https_proxy` of this program's environment; else, on Windows and macOS, through the system's proxy | the variables this program has |
| `NONE` | directly | no proxy variables |
| `MANUAL` | through the host and port of the settings, HTTPS through the same or its own | `http_proxy`, `https_proxy`, `no_proxy` in lower and upper case, with user and password in the URL |

`ProxyStore` reads `~/.config/amadeos/proxy.conf` and the password from the keyring (service `amadeos`, account `proxy`), and follows the file with a [`ConfigWatcher`](44-settings.md#following-a-file-configwatcher). `Network.install()` ties the two together:

```java title="amade-commons · net/Network.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/net/Network.java:install"
```

`NetworkProxy` is a `ProxySelector`. For every connection it first checks the no-proxy list (loopback is always direct), then picks the proxy for HTTP or HTTPS:

```java title="amade-commons · net/NetworkProxy.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/net/NetworkProxy.java:select"
```

A program the user starts now should use the proxy as it is now, not as it was when the calling program started. So the environment is filled in at every start:

```java title="amade-commons · net/NetworkProxy.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/net/NetworkProxy.java:apply-to"
```

!!! warning "Pitfall: `HttpClient` and the proxy password"
    `HttpClient.newBuilder()` uses the JVM's default `ProxySelector`, so it finds the proxy. But it never asks the default `Authenticator`; a proxy that wants a password answers *407* to every request. Take the builder from `Network.httpClient()`, or hand in `Network.proxy().authenticator()` yourself.

!!! warning "Pitfall: Basic authentication through HTTPS tunnels"
    Since Java 8u111 the JDK refuses Basic authentication for HTTPS tunnels (`jdk.http.auth.tunneling.disabledSchemes=Basic`), which is what most office proxies use. `Network.install()` allows it again before the first connection; set later, the property has no effect.

??? example "Test: programs get the proxy in their environment"
    ```java title="amade-commons · test/net/NetTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/net/NetTest.java:apply"
    ```

## The network

*Rules, not code · see [22. Mail](22-mail.md#signing-in-through-the-browser) and [33. Audio](33-audio.md#being-polite-to-a-web-service)*

Apart from the proxy, amade-commons has no network code, and that is deliberate. Mail, Audio and TV all use the JDK's `java.net.http.HttpClient`, but what they need around it differs: Mail signs in with OAuth through the browser and loads remote images only when the user asks; Audio keeps a cache of CD titles and a rate limit for MusicBrainz; TV reads programme guides. What they share is a handful of rules:

```java title="amade-audio · service/metadata/HttpFetcher.java"
--8<-- "amade-audio/src/main/java/eu/amadeos/apps/audio/service/metadata/HttpFetcher.java"
```

- **An interface in front of the client.** The service takes an `HttpFetcher`, not an `HttpClient`, so its tests answer with canned bytes and never go online, just as `CommandRunner` keeps tests away from installed programs.
- **Two time limits.** A connect timeout for a server that does not answer at all, and a request timeout for one that answers and then stalls. Without them a request can wait for minutes.
- **A `User-Agent` that names the program** and where to reach its authors. Services run by volunteers, such as MusicBrainz, ask for it, and block clients that hide.
- **Off the EDT.** Every request runs on the background executor; the result comes back through the `@Ui` executor ([chapter 41](41-start-up.md#two-threads-edtexecutor-and-ui)).
- **Never online by itself.** A program goes online when the user asked for something that needs it: a CD lookup, a station search, images in a message. Web, the documentation viewer, never goes online at all ([chapter 11](11-web.md#never-online-by-itself)).
- **Answers are read with `Json`'s forgiving helpers**, because a member that a service leaves out must not break the program.

- **The client from `Network.httpClient()`**, so the proxy of the desktop and its password apply ([above](#the-proxy-of-the-desktop-network)).

The proxy was the first shared part to move into amade-commons, when TV became the third program on the network ([chapter 38](38-tv.md#and-what-stays-in-tv)).
