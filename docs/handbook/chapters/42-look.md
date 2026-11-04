# 42. Look and Themes

amadeOS looks like CDE: grey-brown panels with 3D bevels, bold buttons that sink in when pressed, and colours from a palette the user picks in Settings. Every program looks the same and changes its colours together with the desktop, without a line of code of its own. This chapter covers the colour themes, where custom themes come from, how a running program follows the desktop, the Swing look that paints it all, the flat look of the second desktop look Horizon, the text size, and what a program does for colours and fonts beyond the standard ones.

## Colour schemes: DesktopTheme

*Package `theme` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`DesktopTheme`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/theme/DesktopTheme.java)*

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

*Package `theme` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`ThemeRegistry`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/theme/ThemeRegistry.java)*

**What it is for.** The themes the user can choose: the five built-in ones and any theme found in a directory of its own, `/etc/amadeos/themes/<id>/theme.properties` for the whole system and `~/.config/amadeos/themes/<id>/theme.properties` for one user. A theme file needs a single line:

```properties title="~/.config/amadeos/themes/midnight/theme.properties"
name=Midnight
base=#2B3A55
activeTitleBg=#C08040
workspace3=#4992A7
iconTheme=NsCDE
```

**When to choose it.** A program never needs it directly: `ThemeStore.system()` builds the registry with both directories. Settings lists `registry().all()`; a theme designer writes a file, and the next registry that is built lists it.

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

*Package `theme` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`ThemeStore`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/theme/ThemeStore.java)*

**What it is for.** The theme the user chose in Settings, and news when they choose another. Settings writes the desktop look into `~/.config/amadeos/look.conf` (`look=classic` or `look=horizon`, and Horizon's scheme and accent), the classic palette's id into `theme.conf`, the icon theme into `icon_theme.conf` (classic) or the `icons` line of `look.conf` (Horizon) and the text size, 1 to 7, into `font.conf`; `ThemeStore` reads them into one `DesktopTheme` and, once `watch()` was called, follows them. The text size is part of the theme value (`fontSize()`, `withFontSize(int)`), so a new size reaches a program the same way a new palette does.

**When to choose it.** `AmadeApplication.launch` creates it and passes it to `AmadeUI.follow`, and `CommonsModule` binds it. A program asks for it only to react to a theme change beyond colours, for example to load other icons when `iconTheme()` changes.

**How it works.** Reading never fails: a missing file, an unknown id or an unreadable file mean Warm Motif (or, with `look=horizon`, Horizon Dark with the teal accent), so a program starts even with a broken setting.

```java title="amade-commons · theme/ThemeStore.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/theme/ThemeStore.java:read"
```

`watch()` starts a [`ConfigWatcher`](44-settings.md#following-a-file-configwatcher) on the four files. On a change it calls `reload()`, which reads them again and tells the listeners only if the theme really changed:

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
    `ThemeStore` has no method to write a theme. Only Settings does, and every program follows. A program with a theme setting of its own would make the desktop look different in every window.

??? example "Test: the store follows a change to the file"
    ```java title="amade-commons · test/theme/ThemeTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/theme/ThemeTest.java:follow"
    ```

## The CDE look for Swing: AmadeUI

*Package `swing` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`AmadeUI`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/AmadeUI.java)*

**What it is for.** The look of every amadeOS program: Swing's cross-platform look and feel with its colour table filled from the theme, Motif bevels around fields, scroll panes and menus, and button UIs of its own: bold labels, lighter while the mouse is over them, sunken and shifted by a pixel while pressed. Check boxes and radio buttons are the Motif ones, drawn in the theme's colours; switched off, their text is drawn in the theme's inactive text colour instead of Swing's etched text, which vanished on the black of the high contrast palette ([chapter 62](62-settings.md#accessibility-accessx-xcursor-and-a-palette)):

```java title="amade-commons · swing/AmadeUI.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/AmadeUI.java:check-box-ui"
```

Push buttons and toggle buttons do the same, and a switched-off combo box keeps the theme's background and draws its text in the inactive colour. Before, Swing drew a switched-off toolbar button's label in shades of the dark background and a switched-off combo box in its own light grey, so on dark palettes, such as Horizon Dark, the Book Editor's toolbar could not be read while no chapter was open.

Sliders and progress bars are drawn by AmadeUI too. Swing's cross-platform look paints them in its own blue and grey whatever the colour table says, so in every theme they looked like a foreign program. `AmadeSliderUI` draws the CDE slider instead: a sunken groove, filled from the minimum up to the value, and a raised thumb with a notch; progress bars get the same sunken trough and fill. The fill is the selection colour, unless that hardly differs from the groove, as in Classic Gray, whose active title bar is the base colour; then it is the dark shadow (or, on a dark groove, the highlight):

```java title="amade-commons · swing/AmadeUI.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/AmadeUI.java:fill"
```

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
| `fillColor(theme)` | The filled part of sliders and progress bars, for bars a program draws itself |
| `style(component, styler)`, `restyle(root)` | Colours of your own ([below](#colours-of-your-own-style)) |
| `sunken()`, `raised()`, `paintBevel(...)`, `colorDot(color, size)` | Helpers ([below](#bevels-and-dots)) |

## The flat look of Horizon: HorizonUI

*Package `swing` · introduced in this chapter, for the Horizon desktop look · source: [`HorizonUI`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/HorizonUI.java), [`HorizonThemes`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/theme/HorizonThemes.java)*

**What it is for.** amadeOS has two desktop looks (Settings > Desktop look). The classic one is CDE, as above. Horizon has a top bar, a dock and flat windows with rounded corners, in a dark or a light scheme with one of six accent colours. In Horizon every program is flat too: rounded buttons without bevels, the default button filled with the accent, text fields with a thin line that turns into an accent ring when focused, slim scroll bars without arrows, tabs underlined in the accent, check boxes and radio buttons filled with the accent when ticked, plain (not bold) fonts. Toolbar buttons stay flat until the mouse is over them.

**When to choose it.** Never directly. A program that starts with `AmadeApplication.launch` gets it for free: a Horizon theme in the `ThemeStore` installs it, a classic one takes it away again, while the program runs. The rules of this chapter stay the same: colours come from `AmadeUI.theme()` or a [styler](#colours-of-your-own-style), and both work in Horizon, where `base()` is the window background, `activeTitleBg()` the accent and `fieldBackground()` the background of fields and lists.

**How it works.** A Horizon theme is an ordinary `DesktopTheme` with the id `horizon-<scheme>-<accent>` and `look()` = `HORIZON`, so the desktop processes exchange it like a classic palette:

```java title="amade-commons · theme/HorizonThemes.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/theme/HorizonThemes.java:theme"
```

`AmadeUI.install` first installs the classic look as always, then calls `HorizonUI.reset()`, which removes every key Horizon may have set before, and, for a Horizon theme, `HorizonUI.install(theme)`, which replaces the borders, icons and UI classes of the controls:

```java title="amade-commons · swing/HorizonUI.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/HorizonUI.java:install"
```

Buttons keep amade-commons' own button UIs; their `paint` asks `HorizonUI.paintButton` in Horizon, which draws the rounded face, the accent for the default button and nothing at all for a toolbar button the mouse is not over:

```java title="amade-commons · swing/HorizonUI.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/HorizonUI.java:button"
```

A button is a toolbar button when it carries the client property `HorizonUI.TOOL_BUTTON`; [`ToolButtons`](46-toolbars-and-printing.md) sets it. The old helpers keep working: `AmadeUI.sunken()`, `raised()` and `paintBevel` draw a thin rounded line in Horizon instead of a bevel, so a program that framed its preview with a bevel needs no change.

| Member | What it is |
|---|---|
| `HorizonUI.active()`, `active(theme)` | True while (or if) the theme is a Horizon one |
| `HorizonUI.Palette.of(theme)` | The colours of the controls: face, hover, pressed, line, accent, text on the accent, scroll thumb |
| `HorizonUI.TOOL_BUTTON` | Client property for a flat toolbar button |
| `HorizonUI.ARC` | The corner radius of controls, 8 pixels |
| `HorizonThemes.theme(scheme, accent)`, `find(id)` | The twelve Horizon themes |

!!! warning "Pitfall: a colour from the classic palettes"
    A colour written into the code, a CDE grey for a header or a dark blue for a selection, looks wrong in Horizon's dark scheme. Take it from the theme (`base()`, `fieldBackground()`, `selectionBackground()`, `activeTitleBg()`), and the program fits both looks.

??? example "Test: the look switches both ways"
    ```java title="amade-commons · test/swing/HorizonUITest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/swing/HorizonUITest.java:switch"
    ```

### Icons of each look

Each look keeps its own icon theme. The classic look takes `icon_theme.conf`, NsCDE unless the user chose another; Horizon takes the `icons` line of `look.conf`, and without one its own theme `amadeos-horizon` (package `amadeos-icons-horizon` of amadeos-system, which falls back to NsCDE and hicolor). `ThemeStore.iconTheme(directory, theme)` makes that choice, and `DesktopTheme.iconTheme()` of the current theme is its answer, so a program that loads freedesktop icons by the theme's name gets the Horizon icons in Horizon without a line of code:

```java title="amade-commons · theme/ThemeStore.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/theme/ThemeStore.java:icons"
```

??? example "Test: each look keeps its own icon theme"
    ```java title="amade-commons · test/theme/HorizonThemesTest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/theme/HorizonThemesTest.java:icons"
    ```

## Text size: fontScale

*Package `swing` · introduced in [62. Settings](62-settings.md#font-one-number-for-every-program) · source: [`AmadeUI`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/AmadeUI.java)*

**What it is for.** The text size the user picks on the *Font* page of Settings, seven steps from *Smallest* to *Largest*. At the default size 4 nothing changes; size 7 makes every font of the look half as big again.

**When to choose it.** A program gets it for free: `install` scales the fonts of the look, and `follow` installs again when `font.conf` changes, so labels, menus, buttons, fields, lists and tables change size while the program runs. A program needs `scaled` only where it picks a font size of its own, such as a heading or the small print of a status line:

```java
AmadeUI.style(title, (l, t) -> l.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, AmadeUI.scaled(17f))));
```

Put such a line into a [styler](#colours-of-your-own-style), not into the constructor, so the size follows a change like the colours do.

**How it works.** The scale factors are a table, `0.75` to `1.5`. `install` scales every font of the look and feel's own defaults, the keys ending in `font`, and puts the result into Swing's table:

```java title="amade-commons · swing/AmadeUI.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/AmadeUI.java:fonts"
```

It always starts from `UIManager.getLookAndFeelDefaults()`, never from the fonts it installed before, so installing a theme twice at size 7 gives the same sizes as once, and going back to 4 gives the original fonts.

| Method | What it does |
|---|---|
| `fontScale()` | The factor of the installed theme's size, 1 at size 4 |
| `fontScale(size)` | The factor of any size, for a preview |
| `scaled(size)` | A size of your own times the factor, rounded to half a point |

!!! warning "Pitfall: fixed sizes in pixels"
    A layout that gives a text a fixed height in pixels, a `setPreferredSize` on a label or a table row height, cuts the text off at size 7. Let the layout manager size text components, and multiply the row heights you need with `fontScale()`.

??? example "Test: sizes scale without compounding"
    ```java title="amade-commons · test/swing/AmadeUITest.java"
    --8<-- "amade-commons/src/test/java/eu/amadeos/commons/swing/AmadeUITest.java:font-size"
    ```

## Colours of your own: style

*Package `swing` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`AmadeUI`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/AmadeUI.java)*

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

*Package `swing` · introduced in [10. Pad](10-pad.md#following-the-desktop-theme) · source: [`AmadeUI`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/AmadeUI.java)*

**What it is for.** The small pieces of the look that programs need in their own components:

- `AmadeUI.sunken()` and `raised()` are Motif bevel borders, 2 pixels, in the current theme's colours. A sunken bevel goes around content (a preview, a cover), a raised one around a group of controls.
- `paintBevel(g, x, y, width, height, raised, thickness)` paints the same bevel in a component's own `paintComponent`, as Audio's faders and cover, WritePad's ruler and Imagescan's picture view do.
- `colorDot(color, size)` is a round icon outlined in the dark shadow, for Mail's account colours in the folder tree, the message list and the options.

**How it works.** The borders paint with `theme()` each time they are drawn, so they follow a theme change without a styler. A raised bevel is light at the top left and dark at the bottom right; a sunken one the other way round.

## Picking a date: DateChooser

*Package `swing` · introduced in [21. Calendar](21-calendar.md#typing-and-picking-dates) · source: [`DateChooser`](https://github.com/amadeos-eu/amade-commons/blob/main/src/main/java/eu/amadeos/commons/swing/DateChooser.java)*

**What it is for.** A small dialog with one month as a grid of days, buttons for the previous and next month and year, *Today*, *OK* and *Cancel*. The arrow keys move the selection, Enter and a double-click accept. `button` makes the small calendar button that goes next to a date field:

```java title="amade-commons · swing/DateChooser.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/swing/DateChooser.java:button"
```

**When to choose it.** Next to every field where a date is typed. Typing stays the fast way; the dialog is for "the second Tuesday of next month".

**How it works.** The first day of the week and the names of days and months come from the locale, so a German user sees weeks from Monday and an American from Sunday. The dialog's own texts (`date.today`, `date.previous-month`, ...) are in amade-commons' texts in all five languages. `choose(parent, initial, locale)` returns the date, or an empty `Optional` if the user cancelled.
