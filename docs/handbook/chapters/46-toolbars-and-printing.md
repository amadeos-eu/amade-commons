# 46. Toolbars, Icons and Printing

This chapter covers two kinds of output: what a program shows in its toolbars, and what it sends to the printer. The toolbar icons are drawn in Java2D instead of loaded from files, so they follow the theme and scale to any size; the printing parts decide the paper and where the pages go, while the program draws them.

## Icons drawn in code: ToolIcon

*Package `swing` · introduced in [30. Fractals](30-fractals.md#the-toolbar-moves-into-amade-commons) · source: [`ToolIcon`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/ToolIcon.java)*

**What it is for.** Toolbar icons that need no icon theme, look sharp at every scale and change colour with the desktop theme. Lines are drawn in the button's text colour (the inactive text colour when the button is disabled), paper in the field colour, highlights in the selection colour, so an icon on Dark Slate is as readable as on Warm Motif.

What is drawn is a **glyph**, a function that draws on a 16 × 16 grid:

```java title="amade-commons · swing/ToolIcon.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/ToolIcon.java:glyph"
```

**When to choose it.** For every toolbar and transport button. The commands most programs share are in [`StandardGlyph`](#the-shared-symbols-standardglyph); a program adds its own as an enum that implements `Glyph`, as Fractals (`FractalGlyph`), Imagescan (`ImagescanGlyph`) and WritePad (`WritepadGlyph`, `TextColorGlyph`) do. Icons of files and programs in a list come from the icon theme instead, because there the user expects the desktop's pictures.

**How it works.** `paintIcon` reads the theme at the moment it paints, scales the graphics from 16 units to the icon's size (22 pixels in a toolbar at 100 %) and hands the glyph a `Pen`:

```java title="amade-commons · swing/ToolIcon.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/ToolIcon.java:paint"
```

The `Pen` holds the graphics, the colours and the shapes several glyphs share:

| Method | Draws |
|---|---|
| `ink()`, `paper()`, `light()`, `enabled()` | The colours, and whether the button is enabled |
| `fill(shape, color)`, `draw(shape)`, `fillAndDraw(shape, fill)` | Any shape, outlined in ink |
| `line(x1, y1, x2, y2)`, `bold(width)`, `thin()` | Lines; `bold` with round ends until `thin` |
| `triangle(...)` | A filled arrow head |
| `page(x, y, w, h)` | A sheet with a folded corner |
| `magnifier(cx, cy, r)` | A lens with its handle |
| `letter(text, font, baseline)` | A letter, centred |
| `mirror()` | Left and right swapped, so Redo is Undo's mirror image |

!!! tip "Tip: half units"
    A 1-unit line on a whole number lies between two pixel rows and is drawn grey and two pixels wide. On half units (1.5, 2.5, ...) it covers one row exactly and stays crisp at 100 %.

??? example "Test: an application's glyph gets the pen with the theme's colours"
    ```java title="amade-commons · test/swing/ToolIconTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/swing/ToolIconTest.java:app-glyph"
    ```

## The shared symbols: StandardGlyph

*Package `swing` · introduced in [30. Fractals](30-fractals.md#the-toolbar-moves-into-amade-commons) · source: [`StandardGlyph`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/StandardGlyph.java)*

**What it is for.** The glyphs of the commands most programs share: `NEW`, `OPEN`, `SAVE`, `PRINT`, `CUT`, `COPY`, `PASTE`, `UNDO`, `REDO`, `FIND`, `ZOOM_IN`, `ZOOM_OUT`, `REFRESH`; for lists of records `EDIT`, `DELETE`, `IMPORT`, `EXPORT` and `MAIL`; for pictures `ZOOM_FIT`, `ZOOM_ACTUAL`, `ROTATE_LEFT`, `ROTATE_RIGHT`, `FULL_SCREEN` and `LEAVE_FULL_SCREEN`.

The second and third row came with the Contacts program: Imagescan, Image Viewer and Fractals each had drawn their own turn, fit and full-screen arrows, and Contacts needed *Export* like Fractals. Now all four use these.

```java title="amade-commons · swing/StandardGlyph.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/StandardGlyph.java:paint"
```

**When to choose it.** For these commands in every program, so that *Save* looks the same everywhere. A glyph only one program needs stays in that program, and moves here when a second one needs it.

**How it works.** Each glyph is a few shapes from the `Pen`. The folder of *Open* shows the style: paper for the back, ink for the front flap, every line on half units.

```java title="amade-commons · swing/StandardGlyph.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/StandardGlyph.java:open"
```

*Import* and *Export* are one drawing: a tray with an arrow that points into it or out of it.

```java title="amade-commons · swing/StandardGlyph.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/StandardGlyph.java:tray"
```

## Toolbar buttons: ToolButtons

*Package `swing` · introduced in [30. Fractals](30-fractals.md#the-toolbar-moves-into-amade-commons) · source: [`ToolButtons`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/ToolButtons.java)*

**What it is for.** Square Motif buttons for a toolbar, raised in 3D like the Front Panel's: icon only, the tooltip from the action, sunken while pressed or, for a switch, while it is on.

```java
Action save = ...;                                   // LARGE_ICON_KEY: new ToolIcon(StandardGlyph.SAVE)
toolbar.add(ToolButtons.button(save));
toolbar.add(ToolButtons.toggle(showGrid));           // an action with SELECTED_KEY
```

**When to choose it.** For toolbars whose buttons repeat menu commands. The menu and the toolbar share one `Action`, so both are enabled, disabled and switched together.

**How it works.** The buttons never take the focus, so the keyboard stays in the document after a click. Their border is set explicitly and is not a `UIResource`, because the toolbar's look would otherwise swap in its flat rollover border. The button's name is the action command, so tests find it.

```java title="amade-commons · swing/ToolButtons.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/ToolButtons.java:configure"
```

## A player's buttons: TransportButtons

*Package `swing` · introduced in [33. Audio](33-audio.md#the-transport-buttons) · source: [`TransportButtons`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/TransportButtons.java), [`TransportGlyph`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/TransportGlyph.java)*

**What it is for.** The wide buttons of a player's transport bar: Previous, Next, Rewind, Forward, Stop, Play, Pause, Record and Eject. Audio and Video use them.

```java
TransportButtons transport = TransportButtons.NORMAL;        // or new TransportButtons(0.8)
JButton play = transport.button(TransportGlyph.PLAY, presenter::play);
panel.add(TransportButtons.bar(previous, stop, play, pause, next));
```

**When to choose it.** For the main controls of a player. Unlike toolbar buttons they take the focus, because in a player they are the controls, not shortcuts for menu commands.

**How it works.** `TransportGlyph` draws filled shapes in the button's text colour; the record dot is red while the button is enabled. Next and Forward are drawn as mirror images of Previous and Rewind:

```java title="amade-commons · swing/TransportGlyph.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/TransportGlyph.java:paint"
```

The tooltip comes from the **program's** texts under `glyph.key()`, `transport.play` and so on, because the same symbol means different things: Previous is the previous track in Audio and the previous chapter in Video. A program that uses a glyph must have its text; `TranslationCheck` finds the missing ones. The sizes scale with one factor, 50 × 34 pixels and a 16-pixel symbol at 100 %:

```java title="amade-commons · swing/TransportButtons.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/TransportButtons.java:sizes"
```

`bar` puts buttons side by side without gaps, as one bar.

??? example "Test: Next is the mirror image of Previous"
    ```java title="amade-commons · test/swing/TransportButtonsTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/swing/TransportButtonsTest.java:mirror"
    ```

## Paper: PaperSize

*Package `print` · introduced in [23. WritePad](23-writepad.md#printing-moves-into-amade-commons) · source: [`PaperSize`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/print/PaperSize.java)*

**What it is for.** The paper sizes the programs offer, `A5`, `A4`, `LETTER` and `LEGAL`, in points (1/72 inch), the unit of `java.awt.print` and of the document formats. `media()` is the paper the printer should take.

**When to choose it.** As the default of every paper setting: `forLocale` gives Letter where Letter is the usual paper and A4 everywhere else.

```java title="amade-commons · print/PaperSize.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/print/PaperSize.java:for-locale"
```

**How it works.** The sizes are kept in millimetres, as the standards define them, and converted to points when asked. The country, not the language, decides: English in Germany is A4.

## Printing: PrintJobs

*Package `print` · introduced in [21. Calendar](21-calendar.md#printing), moved into amade-commons with [23. WritePad](23-writepad.md#printing-moves-into-amade-commons) · source: [`PrintJobs`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/print/PrintJobs.java)*

**What it is for.** Sending a program's pages somewhere: to a printer through the system's print dialog, or to a PostScript file. The pages are a `Pageable`, usually a `java.awt.print.Book`, and decide what is drawn; `PrintJobs` only decides where it goes. Calendar and WritePad print this way.

| Method | What it does |
|---|---|
| `pageFormat(paper, landscape, margin)` | A page format for the paper; margin 0 when the pages lay out their own |
| `attributes(paper, jobName)` | The paper and the job name for the printer |
| `printWithDialog(pages, paper, jobName)` | The print dialog, then printing; false if the user cancelled |
| `writePostScript(pages, file, jobName)` | A PostScript file, without any printer |
| `printerNames()` | The printers the system knows; empty without a print system |

**How it works.** A landscape page keeps the paper in portrait and turns the graphics. The printer is not asked for landscape as well, because some drivers then turn the page twice:

```java title="amade-commons · print/PrintJobs.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/print/PrintJobs.java:page-format"
```

```java title="amade-commons · print/PrintJobs.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/print/PrintJobs.java:attributes"
```

PostScript goes through the JDK's own stream printer, which needs no printer and no CUPS. That makes printing testable on a build server: a test writes a file and checks the pages in it.

```java title="amade-commons · print/PrintJobs.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/print/PrintJobs.java:post-script"
```

!!! warning "Pitfall: Swing components on the printing thread"
    When the pages paint Swing components, as WritePad's text pages do, `printWithDialog` must run on the EDT, like any other access to them. Pages that draw only with Java2D may print on a background thread.

??? example "Test: landscape turns the format, not the paper"
    ```java title="amade-commons · test/print/PrintJobsTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/print/PrintJobsTest.java:landscape"
    ```

??? example "Test: PostScript without a printer"
    ```java title="amade-commons · test/print/PrintJobsTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/print/PrintJobsTest.java:post-script"
    ```
