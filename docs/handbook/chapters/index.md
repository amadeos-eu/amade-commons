# amadeOS Application Developer's Handbook

This book teaches you how to write desktop applications by building them: first a text editor, then a documentation viewer, a file manager, a calendar, a mail program, a word processor, and on to graphics and media programs. While the applications grow, so does the library underneath them, **amade-commons**. You will not only use a library, you will see why each part of it exists, because you write the application that first needs it.

All applications are real programs. They run on the [amadeOS](https://github.com/datazuul/amadeos) desktop, look like the rest of it, follow its colour themes and its language, and most of them run on other Linux systems and on Windows as well. Every piece of code in this book is taken from those applications. It compiles, it is tested, and you can check it out and change it.

## Who this book is for

You know **Java**: classes and interfaces, `List` and `Map`, lambdas, `record` and `enum`, exceptions. You can write a small program and run it.

You have **never written a desktop application**, or only a small one. You do not need to know Swing, threads, dependency injection, translations or Debian packages. The book introduces each of them at the point where an application needs it.

If you are still learning Java itself, work through a Java beginners' book first and come back. The applications will be waiting.

## How the book works

Each application chapter follows the same plan:

1. **What you build**: a screenshot and what the program does.
2. **What you will learn**: the new ideas of the chapter.
3. **The model**: plain Java, no window yet, with tests.
4. **Building it step by step**: files, background work, the presenter, the window.
5. **What amade-commons gives, and what moves into it**: a part that every application needs moves into the shared library, but only when a second application needs it, never "because libraries have it".
6. **Exercises** to make the program your own.

The chapters build on each other. Read them in order the first time; later the chapter titles and the search help you find things again.

Code excerpts look like this, with the file they come from above them:

```java title="amade-pad · domain/TextFormat.java"
--8<-- "amade-pad/src/main/java/eu/amadeos/apps/pad/domain/TextFormat.java"
```

Boxes point out things worth remembering:

!!! tip "Tip"
    A shortcut or a good habit.

!!! warning "Pitfall"
    A mistake that is easy to make, and how to avoid it.

!!! info "Behind the scenes"
    Background you can skip on first reading.

## The applications

| Part | Applications | You learn |
|---|---|---|
| I. First steps | Pad, Web | Start-up, Model-View-Presenter, files and encodings, background work, five languages, desktop themes, Debian packages, opening links safely |
| II. Office | Files, Calendar, Mail, WritePad | Settings files, two-pane layouts, the trash, dates and calendars, printing, passwords in the keyring, network protocols, rich text and document formats |
| III. Graphics and media | Fractals, Imagescan, Video, Audio | Drawing with Java2D, calculations on many threads, toolbars with drawn icons, external programs, native libraries, sound |

[Part IV](40-commons.md), the manual of amade-commons, comes after the applications. It describes the library by subsystem rather than by application: what each part is for, when to choose it, how it works, and which chapter introduced it. Part V shows how a program is packaged and shipped.

What is written so far and what comes next is listed in [The Road Ahead](roadmap.md).

## Conventions

- The book is about **Java 25**, built with **Gradle**. Commands are for a Linux shell; on Windows use `gradlew.bat` instead of `./gradlew`.
- Names follow the amadeOS projects: repositories `amade-<app>`, Java packages `eu.amadeos.apps.<app>`, the library in `eu.amadeos.commons`.
- All code is licensed under the [EUPL-1.2](https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12), as is this book. Use it, change it, share it.

!!! info "A sister book"
    Games are built the same way, on the same library, with an engine of their own between game and library. The [Game Developer's Handbook](https://datazuul.github.io/amade-game-engine/) tells that story. The two books share the first chapters' ideas; you can read either one first.

Ready? Start with [setting up](00-setup.md) your machine.
