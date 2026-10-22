# 42. Look and Themes

amadeOS looks like CDE: grey-brown panels with 3D bevels, bold buttons that sink in when pressed, and colours from a palette the user picks in the Style Manager. Every program looks the same and changes its colours together with the desktop, without a line of code of its own. This chapter covers the colour themes, where custom themes come from, how a running program follows the desktop, the Swing look that paints it all, and what a program does for colours beyond the standard ones.

## Colour schemes: DesktopTheme

*Package `theme` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`DesktopTheme`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/theme/DesktopTheme.java)*

**What it is for.** A `DesktopTheme` is one palette: a base colour, the shades of the bevels, the title bar colours, text colours, the colours of the Front Panel's frame and of the four workspaces, and the name of the icon theme. The window manager, the Front Panel and every program use the same five built-in themes:

| Constant | Id | Look |
|---|---|---|
| `WARM_MOTIF` | `warm-motif` | The default: warm grey with a slate-blue title bar |
| `CDE_BROCADE` | `cde-brocade` | Teal and blue |
| `CDE_DESERT` | `cde-desert` | Sand |
| `CLASSIC_GRAY` | `classic-gray` | Neutral grey |
| `DARK_SLATE` | `dark-slate` | Light text on a dark base |

**When to choose it.** Read the current theme with `AmadeUI.theme()` while painting, or receive it in a [styler](#colours-of-your-own-style). Never keep a theme in a field: the user may pick another one at any moment.

**How it works.** Only the base colour is required. Every missing colour is derived from it, as CDE derived its palettes: the highlight is white, the shadow 68 % of the base, the dark shadow 35 %, a sunken area 88 %.

```java title="amade-commons · theme/DesktopTheme.java (derived shades)"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/theme/DesktopTheme.java:derive"
```

Four colours are not in CDE's palettes but are needed by every program with a document, a list or a selection. They are computed from the others, so a custom theme gets them too:

```java title="amade-commons · theme/DesktopTheme.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/theme/DesktopTheme.java:fields"
```

| Method | Colour |
|---|---|
| `base()`, `highlight()`, `softHighlight()`, `shadow()`, `darkShadow()` | The panel and its bevels |
| `sunkenBg()` | A pressed button, a scroll bar's track, a check box |
| `activeTitleBg()`, `inactiveTitleBg()` | Window title bars; the active one is also the selection |
| `textActive()`, `textInactive()` | Text, and text of disabled components |
| `fieldBackground()`, `fieldText()` | Text fields, lists, tables, documents |
| `selectionBackground()`, `selectionText()` | Selected text, rows and cells |
| `workspaceColors()`, `workspaceColor(i)` | The four workspaces; the fifth repeats the first |
| `panelFrameBg()`, `panelFrameHighlight()`, `panelFrameShadow()` | The Front Panel's frame |
| `iconTheme()`, `withIconTheme(name)` | The freedesktop icon theme, `NsCDE` unless set |
| `isDark()`, `isDark(color)` | True for light text on a dark base, by luminance |

Two themes are equal when their id and icon theme are equal, so a store can tell whether anything changed.

## Custom themes: ThemeRegistry

*Package `theme` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`ThemeRegistry`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/theme/ThemeRegistry.java)*

**What it is for.** The themes the user can choose: the five built-in ones and any theme found in a directory of its own, `/etc/amadeos/themes/<id>/theme.properties` for the whole system and `~/.config/amadeos/themes/<id>/theme.properties` for one user. A theme file needs a single line:

```properties title="~/.config/amadeos/themes/midnight/theme.properties"
name=Midnight
base=#2B3A55
activeTitleBg=#C08040
workspace3=#4992A7
iconTheme=NsCDE
```

**When to choose it.** A program never needs it directly: `ThemeStore.system()` builds the registry with both directories. The Style Manager lists `registry().all()`; a theme designer writes a file, and the next registry that is built lists it.

**How it works.** `discover` registers every sub-directory with a `theme.properties`, in alphabetical order, and later directories win, so a user's theme can replace the system's of the same id. A broken file is logged and skipped: one bad theme never takes the others down.

```java title="amade-commons · theme/ThemeRegistry.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/theme/ThemeRegistry.java:discover"
```

The keys are the method names of `DesktopTheme`: `highlight`, `shadow`, `activeTitleBg`, `workspace1` to `workspace4`, `panelFrameBg` and so on, each `#RRGGBB`.

??? example "Test: a custom theme with derived colours, a broken one skipped"
    ```java title="amade-commons · test/theme/ThemeTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/theme/ThemeTest.java:custom-theme"
    ```

## Following the desktop: ThemeStore

*Package `theme` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`ThemeStore`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/theme/ThemeStore.java)*

**What it is for.** The theme the user chose in the Style Manager, and news when they choose another. The Style Manager writes the theme's id into `~/.config/amadeos/theme.conf` and the icon theme into `icon_theme.conf`; `ThemeStore` reads them and, once `watch()` was called, follows them.

**When to choose it.** `AmadeApplication.launch` creates it and passes it to `AmadeUI.follow`, and `CommonsModule` binds it. A program asks for it only to react to a theme change beyond colours, for example to load other icons when `iconTheme()` changes.

**How it works.** Reading never fails: a missing file, an unknown id or an unreadable file mean Warm Motif, so a program starts even with a broken setting.

```java title="amade-commons · theme/ThemeStore.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/theme/ThemeStore.java:read"
```

`watch()` starts a [`ConfigWatcher`](44-settings.md#following-a-file-configwatcher) on the two files. On a change it calls `reload()`, which reads them again and tells the listeners only if the theme really changed:

```java title="amade-commons · theme/ThemeStore.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/theme/ThemeStore.java:reload"
```

| Method | What it does |
|---|---|
| `ThemeStore.system()` | The store of the running user, with custom themes from both directories |
| `new ThemeStore(dir, registry)` | A store on any directory, for tests |
| `current()`, `registry()` | The theme now, the themes to choose from |
| `addListener`, `removeListener` | Called with the new theme, **on the watching thread** |
| `watch()`, `close()` | Start and stop following the files |

!!! note "Programs read, the desktop writes"
    `ThemeStore` has no method to write a theme. Only the Style Manager does, and every program follows. A program with a theme setting of its own would make the desktop look different in every window.

??? example "Test: the store follows a change to the file"
    ```java title="amade-commons · test/theme/ThemeTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/theme/ThemeTest.java:follow"
    ```

## The CDE look for Swing: AmadeUI

*Package `swing` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`AmadeUI`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/AmadeUI.java)*

**What it is for.** The look of every amadeOS program: Swing's cross-platform look and feel with its colour table filled from the theme, Motif bevels around fields, scroll panes and menus, and button UIs of its own: bold labels, lighter while the mouse is over them, sunken and shifted by a pixel while pressed. Check boxes and radio buttons are the Motif ones, drawn in the theme's colours.

**When to choose it.** `launch` installs it. A program calls it directly only for the helpers below, or in a test that draws a component in a given theme.

**How it works.** `install(theme)` sets Swing's colour table: `Panel.background`, `TextField.background`, `Table.selectionBackground` and about a hundred others, plus the borders and UI classes. Every colour is a `ColorUIResource`, Swing's mark for "comes from the look". `apply(theme)` installs a new theme and updates every open window:

```java title="amade-commons · swing/AmadeUI.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/AmadeUI.java:apply"
```

`updateComponentTreeUI` replaces only colours that are still `UIResource`s; a colour a program set itself stays. That is the reason for the next section. `follow` connects the store to `apply`, switching to the EDT on the way:

```java title="amade-commons · swing/AmadeUI.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/AmadeUI.java:follow"
```

| Method | What it does |
|---|---|
| `install(theme)` | Fills Swing's colour table; before the first component, on the EDT |
| `apply(theme)` | `install`, then updates and restyles every window |
| `follow(store)` | Applies every change of the store on the EDT |
| `theme()` | The theme installed last; read it while painting |
| `style(component, styler)`, `restyle(root)` | Colours of your own ([below](#colours-of-your-own-style)) |
| `sunken()`, `raised()`, `paintBevel(...)`, `colorDot(color, size)` | Helpers ([below](#bevels-and-dots)) |

## Colours of your own: style

*Package `swing` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`AmadeUI`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/AmadeUI.java)*

**What it is for.** Components that need a colour Swing's table does not give them: a calendar's day grid in the field colour, a path bar in the selection colour, a highlighted status line. A *styler* is a function from theme to colours; `style` runs it now and again after every theme change.

```java title="amade-commons · swing/AmadeUI.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/AmadeUI.java:style"
```

```java
AmadeUI.style(scroll.getViewport(), (viewport, theme) -> viewport.setBackground(theme.fieldBackground()));
```

**When to choose it.** For a component whose colours you set with `setBackground`, `setForeground` or a border. A component that paints its own pixels needs no styler: it reads `AmadeUI.theme()` in `paintComponent`, and the repaint after a theme change does the rest. Pad's line number margin and Audio's racks and faders work that way.

**How it works.** The styler is kept in a client property of the component. `apply` walks every window and runs the stylers it finds, menus' pop-ups included.

!!! warning "Pitfall: `setBackground(AmadeUI.theme().softHighlight())`"
    A colour set once stays forever. After switching to Dark Slate, one bright bar remains in an otherwise dark window. If the colour comes from the theme, it belongs in a styler.

??? example "Test: stylers run again after a theme change"
    ```java title="amade-commons · test/swing/AmadeUITest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/swing/AmadeUITest.java:stylers"
    ```

## Bevels and dots

*Package `swing` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`AmadeUI`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/AmadeUI.java)*

**What it is for.** The small pieces of the look that programs need in their own components:

- `AmadeUI.sunken()` and `raised()` are Motif bevel borders, 2 pixels, in the current theme's colours. A sunken bevel goes around content (a preview, a cover), a raised one around a group of controls.
- `paintBevel(g, x, y, width, height, raised, thickness)` paints the same bevel in a component's own `paintComponent`, as Audio's faders and cover, WritePad's ruler and Imagescan's picture view do.
- `colorDot(color, size)` is a round icon outlined in the dark shadow, for Mail's account colours in the folder tree, the message list and the options.

**How it works.** The borders paint with `theme()` each time they are drawn, so they follow a theme change without a styler. A raised bevel is light at the top left and dark at the bottom right; a sunken one the other way round.

## Picking a date: DateChooser

*Package `swing` · introduced in [21. Calendar](21-calendar.md#typing-and-picking-dates) · source: [`DateChooser`](https://github.com/datazuul/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/DateChooser.java)*

**What it is for.** A small dialog with one month as a grid of days, buttons for the previous and next month and year, *Today*, *OK* and *Cancel*. The arrow keys move the selection, Enter and a double-click accept. `button` makes the small calendar button that goes next to a date field:

```java title="amade-commons · swing/DateChooser.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/DateChooser.java:button"
```

**When to choose it.** Next to every field where a date is typed. Typing stays the fast way; the dialog is for "the second Tuesday of next month".

**How it works.** The first day of the week and the names of days and months come from the locale, so a German user sees weeks from Monday and an American from Sunday. The dialog's own texts (`date.today`, `date.previous-month`, ...) are in amade-commons' texts in all five languages. `choose(parent, initial, locale)` returns the date, or an empty `Optional` if the user cancelled.
