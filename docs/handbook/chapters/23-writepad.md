# 23. WritePad

Pad edits text. WritePad edits documents: fonts and sizes, bold and italic, indents and tab stops, lists, and above all *pages*. A letter has to fit on a sheet of A4 and look on paper the way it looked on the screen. And a document has to open in other people's word processors, so WritePad reads and writes their file formats.

amade-writepad is modelled on the small word processors that came with the desktops of the 1990s: a toolbar, a format bar, a ruler, and the pages on a dark desk. It is the biggest program so far, about 18,400 lines of Java and 5,600 lines of tests. This chapter shows the parts that make it a word processor, the page view, pictures with text flowing around them, tables, paragraph styles, the file formats and printing, and it moves printing into amade-commons, where Calendar has been waiting for it since chapter 21.

## What you build

![amade-writepad with the sample text](images/writepad/window.png)

- **Formatted text**: font, size, bold, italic, underline, strike-through, colour, alignment.
- **Real pages** in A5, A4, Letter or Legal, portrait or landscape, with margins; zoom from 50 % to 300 %.
- **A ruler** with indent markers and tab stops (left, centre, right, decimal) to drag.
- **Bullet and numbered lists** in several levels.
- **Pictures** in the line, on a line of their own, or at the left or right margin with the text flowing beside them; resized with handles, set up in a dialog.
- **Paragraph styles**: Normal, Title, Subtitle, Heading 1 to 6, Quote and Code, chosen in a style box or with ++ctrl+0++ to ++ctrl+6++, and changed for the whole document in a dialog.
- **Tables** with a heading row that repeats on every page, lines, cell colours, columns to drag, and rows that break between pages.
- **ODT, DOCX, RTF and plain text**, opened and saved; old DOC files through LibreOffice when it is installed.
- **Printing** that puts the lines and page breaks on paper exactly where they are on the screen.

## What you will learn

- How Swing's text components are built: document, elements, views and the editor kit.
- How to replace Swing's views to lay text out on pages.
- How to keep a layout in points and show it in pixels, at any zoom.
- How to put an object into styled text as one character, and how to let text flow around it.
- How to build tables out of plain paragraphs, and keep the user from breaking them.
- How Swing's named styles carry paragraph styles, and how a file's complete formatting turns back into styles.
- How to read and write document formats with the JDK alone, and why that is possible.
- How to print through `java.awt.print`, and what moves into amade-commons for it.

## Swing's text model in five minutes

Pad used a `JTextArea`, which shows one font and has no idea of a page. WritePad uses a `JTextPane`, the styled text component of Swing. Like every Swing text component it has four parts, and a word processor has to know all of them:

| Part | Class | What it is |
|---|---|---|
| **Document** | `StyledDocument` (`DefaultStyledDocument`) | The text and its attributes. The model: no pixels, no fonts on screen. |
| **Elements** | `Element` | The structure of the document: a root, one element per paragraph, and inside each paragraph one element per run of equally formatted characters. Attributes (`AttributeSet`) hang on the elements. |
| **Views** | `View` (`BoxView`, `ParagraphView`, `LabelView`, ...) | The layout: one view per element, which knows its size, where its lines break, and how it paints. |
| **Editor kit** | `EditorKit` (`StyledEditorKit`) | The glue: it makes the document, and its *view factory* decides which view each element gets. |

The document is shared, the views are not. That is the opening: if you give a `JTextPane` an editor kit with a different view factory, the same document is laid out differently. Everything else (typing, the caret, selection, undo, copy and paste) keeps working, because it talks to the document and asks the views only where things are.

## Text on pages

Swing's `StyledEditorKit` lays out a document as one endless sheet: the section view stacks the paragraphs, and each paragraph view breaks its text into lines as wide as the component. WritePad's kit keeps most of the views and replaces two:

```java title="amade-writepad · service/layout/PagedEditorKit.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PagedEditorKit.java:factory"
```

The section becomes a `PagedSectionView`, which stacks the paragraphs on pages, and every paragraph a `PagedParagraphView`, which never lets a line cross the bottom of a page. One small class knows where the pages are, `PageLayout`, and it answers one question for both: where does a line go?

```java title="amade-writepad · service/layout/PageLayout.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PageLayout.java:place"
```

`place` is pure arithmetic over the page size and the margins, so it is tested without Swing:

```java title="amade-writepad · PageLayoutTest.java"
--8<-- "amade-writepad/src/test/java/eu/amadeos/apps/writepad/service/layout/PageLayoutTest.java:crossing"
```

Note the last rule in `place`: a line that starts at the top of a page stays there, even if it is taller than the page. Without that rule, a picture larger than a page would move to the next page, and the next, and the layout would never end. Every layout loop needs a case where it stops moving things.

A paragraph uses `place` for every line it has broken:

```java title="amade-writepad · service/layout/PagedParagraphView.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PagedParagraphView.java:layout"
```

`layoutMajorAxis` is where a `BoxView` decides the vertical position of its children, here the lines. WritePad lets Swing break the lines as usual, then walks over them and moves each one that would cross a page boundary. The space a moved line leaves behind (`pushed`) is added to the paragraph's height, so the paragraphs below move down too. For that, a paragraph has to know where on the page it starts, and the section tells it before it lays it out:

```java title="amade-writepad · service/layout/PagedSectionView.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PagedSectionView.java:stack"
```

When the number of pages changes, the section asks its parent for a new size. That is how the scroll bar grows while you type.

![Pages at 50 %: the second section starts on page 1, its lines continue on page 2](images/writepad/pages.png)

!!! warning "Pitfall: Swing's view classes are not an API"
    `ParagraphView`, `BoxView` and their protected methods are documented, but they were written for Swing's own use, and their behaviour is defined mostly by their source code. WritePad overrides a handful of them (`layoutMajorAxis`, `calculateMajorAxisRequirements`, `getFlowStart`, `nextTabStop`, `getViewAtPoint`), and each override has a comment that says what Swing does there and why it is not enough. When you extend a framework through its protected methods, write those comments: the next reader has to understand two pieces of code, not one.

## Points inside, pixels outside

A page of A4 is 595 × 842 points (1/72 inch). On a screen at 100 % it should be as big as the real sheet, which on a 96 dpi screen is 794 pixels wide; at 50 % half of that. Where should the zoom live?

If the views laid out in pixels, every zoom would break the lines differently: fonts do not scale exactly linearly in pixels, so a line that just fits at 100 % may not fit at 125 %. The page breaks would move with the zoom, and the printout would match none of them. So WritePad lays out in points, always, and only the section view translates:

```java title="amade-writepad · service/layout/PagedSectionView.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PagedSectionView.java:paint"
```

`toScreen` is a scale (and the offset that centres the page in a wide window). Everything inside the paragraphs and lines is in points; everything outside, the component size, the caret, the mouse, in pixels. The same translation runs the other way in `modelToView` and `viewToModel`, so a click lands on the right character at every zoom.

Two rendering hints matter. *Fractional metrics* lets glyphs sit at fractions of a pixel; without it, every glyph of a scaled font snaps to whole pixels and the spaces in a line become uneven. *Text antialiasing* makes small text readable at 50 %.

### Tabs measured from the margin

Tab stops are a good example of how much a word processor depends on getting the coordinates right. A tab stop at 4 cm is 4 cm from the left margin, in RTF, ODT and DOCX alike. Swing measures tabs from a position it only learns while painting, so before the first paint, lines could break with different tab widths than after it. `PagedParagraphView` overrides `getFlowStart` and `nextTabStop` to measure from the margin, in the same coordinates in which it paints, and leaves the rules (which stop comes next, how a decimal tab aligns) to `TabStops`, a small class without Swing that is tested on its own.

The ruler follows the same idea. `RulerModel` knows the markers, the stops and what a drag does to them; `Ruler` is the component that draws the model and passes on the mouse. The model has its own tests, which never open a window.

## Lists that are drawn, not typed

A numbered list item in WritePad is a paragraph with three attributes: the id of its list, its level, and its format (`1.`, `a.`, `i.`, `•`). The number itself is not in the text. It is computed when the paragraph is painted:

```java title="amade-writepad · service/list/ListNumbering.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/list/ListNumbering.java:compute"
```

One pass over the paragraphs counts, per list and level, and a higher level starts the deeper ones again. Paragraphs between two items of a list do not interrupt it, which is how word processors number a list with a note in the middle. The labels are kept in a cache until the document changes, so painting a page does not count the whole document again.

Why not type the numbers? Because then deleting item 2 would leave "1., 3., 4.", and every file format stores lists as structure, not as text. A label that the program draws is always right. `PagedParagraphView.paintLabel` draws it in front of the first line, at the first-line indent, in the font of the paragraph's first character.

## Pictures

![A picture floating at the right, selected, in the Horizon look](images/writepad/pictures.png)

### A picture is one character

Swing's styled text has a place for things that are not text: a character whose attributes hold a component or an icon. WritePad uses the same idea with its own class. A picture is the character U+FFFC, OBJECT REPLACEMENT CHARACTER, and its attributes carry an immutable `Picture`: the image's bytes, its size in points, where it sits, its border and its description.

```java title="amade-writepad · service/picture/PictureAttributes.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/picture/PictureAttributes.java:character"
```

Because the picture is a character, everything the editor already does works for it without a line of new code: the caret steps over it, a selection includes it, ++delete++ removes it, cut and paste move it, and undo brings it back. Changing a picture (a new size, another position) is `setCharacterAttributes` on its one character, which is one undoable edit too.

Two details make this safe. `Picture` uses *identity* for `equals`: two equal pictures side by side would otherwise end up in one element, because `DefaultStyledDocument` joins neighbouring runs with equal attributes, and one element can only be one view. And `WritepadDocument.insertString` drops the picture attribute from any text except a single U+FFFC, so typing right after a picture does not create more pictures with the attributes of the character before.

The view factory of the editor kit gives a character with a picture a `PictureView` instead of a `LabelView`. It is as wide and high as the picture and stands on the baseline, so the line it is in grows to fit it, as a large letter would. A picture "on a line of its own" is nothing new either: it is a picture alone in its paragraph, and the paragraph's alignment puts it left, centred or right. LibreOffice and Word store such pictures the same way.

### Text that flows around a picture

A floating picture is harder, because its text is not in its line. The text of the paragraph runs down beside it, and the lines beside it are shorter. Swing's paragraph view breaks lines with a *flow strategy*, which asks the view two questions for every row: where does the row start (`getFlowStart`), and how wide may it be (`getFlowSpan`). Both take the row's index. A plain `ParagraphView` gives the same answer for every row. `PagedParagraphView` answers per row:

```java title="amade-writepad · service/layout/PagedParagraphView.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PagedParagraphView.java:floats"
```

To know where row 5 goes on the page, the view adds up the heights of rows 0 to 4, which are already filled when the strategy asks for row 5, and places each with the same `layout.place` as the paragraphs, so a row that would cross the bottom margin moves to the next page here too. Row 5 itself is not filled yet, so its height is a guess: as high as the row before. A row that would be narrower than 54 points (three short words) does not squeeze in next to the picture; it moves below it.

Who decides where the pictures are? The section view, which stacks the paragraphs, already knows the y of each paragraph before it lays it out. Now it also places the paragraph's floating pictures at that y, and hands each paragraph the room that the pictures of this and earlier paragraphs still take below its top:

```java title="amade-writepad · service/layout/PagedSectionView.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PagedSectionView.java:place-floats"
```

So a picture reaches into the paragraphs after its own, the way it does in every word processor, and two pictures on the same side stack. The section paints the floating pictures itself, after the text, because their character view has no size. And a click on a floating picture cannot find it through `viewToModel`, so the section answers "which picture is at this point" from its list.

!!! note "Note: why a centred picture does not float"
    Text on both sides of a picture is possible (Word calls it "square, both sides"), and it is hard to read: the eye has to jump over the picture in every line. WritePad offers left and right only. A centred picture stands on its own line.

### Handles on top of the caret

A selected picture shows a frame with eight handles. Dragging a handle must not reach the caret, or the press would start a text selection. So the editor looks at mouse events before Swing does:

```java title="amade-writepad · ui/editor/PageEditor.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/ui/editor/PageEditor.java:mouse"
```

While dragging, only the frame follows the mouse; the picture gets its new size when the button is let go, as one undo step. Corner handles keep the proportions unless ++shift++ is held, and a dragged picture never gets wider than the text.

The clipboard learned pictures too. Inside WritePad a copy carries the text with its pictures; other programs get plain text without the U+FFFC characters, or an image if exactly one picture is selected. A screenshot on the clipboard and image files dropped from the File Manager come in as pictures.

## Tables

![The right-click menu of a table cell, in the Horizon look](images/writepad/tables.png)

### A table is paragraphs that know their cell

Swing's styled document has no tables. `HTMLDocument` builds them as nested elements (table, row, cell, paragraph), and everything that walks the paragraphs of a document then has to learn about the nesting: the lists, the ruler, the four formats, printing. WritePad keeps its document flat. A table is a run of ordinary paragraphs, and each one carries three more paragraph attributes: the table's format, its row and its column.

```java title="amade-writepad · service/table/TableAttributes.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/table/TableAttributes.java:attributes"
```

A cell is simply the paragraphs in a row that have the same table, row and column. ++enter++ in a cell makes a new paragraph, which inherits the attributes, so it lands in the same cell without a line of table code. Bold, alignment and lists work in a cell because a cell's text is ordinary text. The row is an object that equals only itself, for the same reason as `Picture` above: two rows with equal attributes would otherwise melt into one. The `TableFormat` is immutable (column widths, lines, padding, alignment, the heading's repeat); changing a table means giving all its paragraphs a new one.

Inserting a table is inserting line ends and giving them attributes:

```java title="amade-writepad · service/table/TableEditing.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/table/TableEditing.java:insert"
```

The method splits the paragraph when the caret is in the middle of it, so there is always a paragraph after a table, where the text goes on.

### Line ends you cannot delete

A flat model has one danger: the line end at the end of a cell is a character like any other. ++backspace++ at the start of a cell would join it with the cell before, and selecting half a table and pressing ++delete++ would pour cells into each other. So the document refuses:

```java title="amade-writepad · service/format/WritepadDocument.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/format/WritepadDocument.java:protect"
```

`remove` cuts the range into the pieces between protected line ends and removes only those: the text of the cells goes, the cells stay. The pieces are removed in one `CompoundEdit`, so one undo brings all of them back. Table operations themselves (a new row, a deleted column) must remove line ends, so they run inside `structurally(...)`, which switches the protection off, and in `grouped(...)`, which makes their many small edits one undo step.

### Rows on pages

`PagedSectionView` stacks the paragraphs one below the other. In a table, the paragraphs of a row stand side by side instead. A `BoxView` lays out the minor axis (x) before the major axis (y), and that helps: in `layoutMinorAxis` every paragraph of a cell gets its column's x and width, so it breaks its lines at the column's width. Then the major axis places the rows:

```java title="amade-writepad · service/layout/PagedSectionView.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PagedSectionView.java:table"
```

A row is as high as its highest cell. A row that does not fit on the rest of the page moves to the next one as a whole, and when the table has a heading row that repeats, the section lays out a *copy* of the heading above it. The copy is no text in the document; the section paints the heading's views a second time, shifted to the new page. Cells centred or at the bottom move down within their row after the row's height is known.

The cells' backgrounds are painted before the text, the lines after it:

```java title="amade-writepad · service/layout/PagedSectionView.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PagedSectionView.java:paint-tables"
```

### Clicks and arrow keys

Two things Swing does with the views assume that paragraphs are stacked. A click asks the section which child is under the point, and `BoxView` answers with the last child that starts above it: in a row of cells, always the last cell. So the section answers itself, by row and then by column:

```java title="amade-writepad · service/layout/PagedSectionView.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PagedSectionView.java:hit"
```

The column edges are in the section's own coordinates, which already include the page's left edge. The first version subtracted that edge from the click once more, and every click landed one cell to the left; the tests that clicked into the first column passed. The test that found it clicks into every cell, in a table narrower and in one wider than the text:

```java title="amade-writepad · TableLayoutTest.java"
--8<-- "amade-writepad/src/test/java/eu/amadeos/apps/writepad/service/layout/TableLayoutTest.java:clicks"
```

++up++ and ++down++ are the other one. Swing moves the caret to the paragraph before or after, which in a table is the cell to the left or right. The section overrides the step from one child to the next and stays in the column:

```java title="amade-writepad · service/layout/PagedSectionView.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PagedSectionView.java:north-south"
```

++tab++ is the editor's business, not the view's: in a table it goes to the next cell and selects its text, in the last cell it adds a row, and ++ctrl+tab++ types a real tab. The column lines are dragged with the same trick as the picture handles: the editor looks at the mouse events before Swing does, and a press near a line starts a drag that ends with one `setFormat` when the button is let go.

!!! note "Note: merged cells later"
    A merged cell spans columns or rows, and then "the paragraphs with the same row and column" is no longer a cell's definition. WritePad leaves them out for now. Files with merged cells open with the cells split again, the text in the first part, which every format can express. Exercise 7 asks how the model would carry them.

## Paragraph styles

![Format → Paragraph Style, in the CDE look](images/writepad/styles.png)

A heading in a word processor is not big bold text. It is a paragraph in the style *Heading 1*, and the style says what Heading 1 looks like. Change the style and every heading changes; save the file and LibreOffice shows the headings in its navigator. WritePad has eleven built-in styles, each with its names in the three file formats:

```java title="amade-writepad · service/style/ParagraphStyle.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/style/ParagraphStyle.java:styles"
```

The names look arbitrary and are not: `Heading_20_1` is how ODT writes "Heading 1" (a space is `_20_`), Word's ids have no spaces and its names are lower case, and Code borrows *Preformatted Text* and *HTML Preformatted*, the styles both programs give `<pre>`. With these names, LibreOffice and Word take WritePad's headings for their own.

### Styles are Swing's named styles

Swing has had styles from the start, and they fit. A `StyledDocument` keeps named `Style`s, and every paragraph element has a *resolve parent*: an attribute that is not set on the paragraph itself is looked up in it. Make the resolve parent the style *Heading 1*, and the paragraph has the heading's font, size and spacing without a single attribute of its own. The runs of text inside resolve to their paragraph, so they get the heading's font too. Change the style, and Swing tells the views, which lay the paragraphs out again.

Two details make it work. Every style defines *every* key (font, size, bold, italic, alignment, indents, spacing), so a paragraph never falls through to Swing's defaults; only black text leaves the colour out, because "no colour" means the automatic colour that a document keeps in every format. And Normal is a style of its own, not Swing's `default` style, because `JTextPane` copies the component's font into `default` whenever the look changes.

```java title="amade-writepad · service/style/ParagraphStyle.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/style/ParagraphStyle.java:built-in"
```

### Applying a style

What happens to formatting set by hand when a paragraph gets a style? Leave it, and a heading pasted from somewhere keeps its 16 pt Arial and never takes the heading's look. Remove it, and a word in italics loses its italics. WritePad does what Word does: formatting that covers the whole paragraph goes, formatting of a part stays.

```java title="amade-writepad · service/style/ParagraphStyles.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/style/ParagraphStyles.java:apply"
```

The test says it in one example:

```java title="amade-writepad · ParagraphStylesTest.java"
--8<-- "amade-writepad/src/test/java/eu/amadeos/apps/writepad/service/style/ParagraphStylesTest.java:apply"
```

The format bar's style box, the *Format → Paragraph Style* menu and the keys ++ctrl+0++ to ++ctrl+6++ all call the same action. ++enter++ at the end of a heading asks the style for its `next()` style and gives it to the new paragraph, in the same undo step as the line end.

### Changing a style, undoably

*Format → Styles…* (++f11++) edits the definitions. Its result is a map of the changed styles, and the change is one `StyleEdit`: it remembers the old and the new definitions, and undo and redo set one or the other. Changing a style is no edit of the text, so Swing records nothing; the edit is posted to the undo manager by hand:

```java title="amade-writepad · ui/main/EditorActions.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/ui/main/EditorActions.java:styles-dialog"
```

### Reading files back into styles

The readers of the file formats below already worked hard to give every paragraph and every run its complete formatting, resolved through the file's style chain. They keep doing that, and then hand the result to one method that turns it into styles: the styles take the file's definitions, each paragraph its style, and only what differs from the style stays on the paragraph and its runs.

```java title="amade-writepad · service/style/ParagraphStyles.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/style/ParagraphStyles.java:adopt"
```

The method first takes a snapshot of how everything looks, because installing the new definitions changes what the attributes resolve to. Without the snapshot, a paragraph that was 12 pt before would compare itself with the new style and look the same as it, when it was not. That is the test the method has to pass: after `adopt`, every character looks exactly as before.

The readers record which paragraph has which style as they go, as a `Position` in the document. The first version took the position before inserting the paragraph's text, at the end of the document, and a `Position` at the end moves with everything inserted there: every style ended up on the last paragraph. The position is taken after the paragraph's line end now, inside the paragraph.

## File formats

Every format is a `DocumentFormat`:

```java title="amade-writepad · service/format/DocumentFormat.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/format/DocumentFormat.java"
```

Read the comments about threads: `read` builds a new document that nothing shows yet, so it may run in the background; `write` reads the document in the editor, so it runs on the EDT and only produces bytes. `finish` runs in the background again, for formats that hand the bytes to another program. Guice collects the formats with a *multibinder*, a set that every binding adds to:

```java title="amade-writepad · bootstrap/WritepadModule.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/bootstrap/WritepadModule.java:formats"
```

A new format is one class and one line here. The registry decides which format opens a file, and it trusts the content more than the name:

```java title="amade-writepad · service/format/FormatRegistry.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/format/FormatRegistry.java:for-opening"
```

### ZIP files with XML inside

ODT (OpenDocument) and DOCX (Office Open XML) look frightening and are not: both are ZIP files with a few XML files inside. WritePad reads and writes them with `java.util.zip` and StAX, the streaming XML reader and writer that are part of the JDK. An ODT file has one rule that is easy to miss:

```java title="amade-writepad · service/format/odt/OdtWriter.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/format/odt/OdtWriter.java:zip"
```

The first entry must be `mimetype`, uncompressed. Then its name and its content stand at fixed offsets in the file, and a program can recognise an ODT file from its first hundred bytes, without unpacking anything. WritePad's own recognition does exactly that:

```java title="amade-writepad · service/format/odt/OdtFormat.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/format/odt/OdtFormat.java:recognizes"
```

A DOCX file is a ZIP too, but without the `mimetype` entry, which is why the check reads the name and the type instead of stopping at `PK`. And the test checks the bytes, not only that reading works:

```java title="amade-writepad · OdtFormatTest.java"
--8<-- "amade-writepad/src/test/java/eu/amadeos/apps/writepad/service/format/odt/OdtFormatTest.java:package"
```

!!! note "Note: why no document library"
    Java libraries exist for these formats, and they are big: the usual one for DOCX brings about 19 MB of jars, four times the size of the whole WritePad package, and a DOM of the entire document in memory. WritePad needs three XML files of a DOCX and four of an ODT, and StAX reads them as a stream. The decision and its alternative are written down in the repository's `CONTRIBUTING.md`, with the size, the date and what would change it. Write such notes for every dependency you decide against: in a year someone will ask again.

RTF is different: a text format of nested groups in curly braces with control words such as `\b` and `\par`. WritePad reads it with its own small tokenizer. The old binary DOC format is the one case where WritePad does not write its own code: it runs LibreOffice (`soffice --headless --convert-to docx`) in the background, with a profile of its own, and reads the DOCX. Running another program well, with a timeout, its output and its exit code, is a part that moves into amade-commons in chapter 31.

### Pictures in three formats

Each format stores a picture as the image file plus a frame that says how big it is and where it goes, and each has its own words for it:

| | ODT | DOCX | RTF |
|---|---|---|---|
| Image | `Pictures/1.jpg` in the ZIP | `word/media/image1.jpg` and a relationship | hex digits in `\pict` (`\pngblip`, `\jpegblip`) |
| In the line | `draw:frame` anchored `as-char` | `wp:inline` | `{\pict ...}` in the text |
| Floating | anchored to the paragraph, `style:wrap` right or left | `wp:anchor` with `wrapSquare` | a picture shape `{\shp ...}` with `\shpwr2` |
| Description | `svg:desc` | `descr` of `wp:docPr` | shape property `wzDescription` |

```java title="amade-writepad · service/format/docx/DocxWriter.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/format/docx/DocxWriter.java:drawing"
```

The size in a file is the picture *with* its border, because LibreOffice writes it so. Write it without, and a picture grows by the width of its border every time the file goes through LibreOffice and back. The test that found this reads a file that LibreOffice saved from one of WritePad's, in each of the three formats; the round trip of WritePad alone would never have shown it. LibreOffice's RTF loses the border, so for RTF the test accepts the frame's size as the picture's.

The tests for every format do the same three things: write a document and read it back (a *round trip*), read files that other programs wrote, and check the bytes against the rules of the format. The round trip alone is not enough: a writer and a reader that share a mistake pass it.

### Tables in four formats

A table is easier to move between formats than a picture, because all three document formats think in rows and cells. They disagree on the details:

| | ODT | DOCX | RTF |
|---|---|---|---|
| Table | `table:table` with `table:table-column`s | `w:tbl` with `w:tblGrid` | rows of `\intbl` paragraphs |
| Row | `table:table-row` | `w:tr` | `\trowd` … `\row` |
| Cell | `table:table-cell` | `w:tc` | its right edge `\cellx`, its text ending in `\cell` |
| Repeated heading | inside `table:table-header-rows` | `w:tblHeader` in the row | `\trhdr` |
| Lines | per cell, `fo:border-left` … | per table, `w:tblBorders`, or per cell | per cell, `\clbrdrt` … |
| Background | `fo:background-color` of the cell style | `w:shd` | `\clcbpat`, a colour of the colour table |

RTF is the odd one: a table is not a group with rows inside but paragraphs marked `\intbl`, and each row first describes its cells (`\trowd` and one `\cellx` per cell, with the cell's lines and colour before it) and then gives their text:

```java title="amade-writepad · service/format/rtf/RtfWriter.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/format/rtf/RtfWriter.java:table"
```

Readers collect the paragraphs of a table at the end of the document as they come and only make them a table at its end, with `TableBuilder`, because the format of a table (DOCX's grid, RTF's `\cellx` of the first row) is not always known before its cells. Lines are stored per cell in two of the three formats, so `TableLines` turns the table's choice (around every cell, around the table, between the rows, none) into the edges of each cell when writing, and guesses it back from the first cells when reading. Merged cells come in split (`table:covered-table-cell`, `w:gridSpan` and `w:vMerge`, `\clmrg`), a table inside a cell comes in as its paragraphs. Plain text writes each row as one line with tabs between the cells.

The tests are the three of the pictures: a round trip, files that LibreOffice saved, and the bytes. The LibreOffice files found two things the round trip could not: LibreOffice writes DOCX's lines per cell with the words `start` and `end` instead of `left` and `right`, and it drops `\trhdr` and writes the padding per cell (`\clpadl`) when it saves RTF.

### Styles in three formats

| | ODT | DOCX | RTF |
|---|---|---|---|
| Definitions | `style:style` in `styles.xml` | `w:style` in `word/styles.xml` | `{\stylesheet}` with `{\s1 … heading 1;}` |
| A paragraph's style | `text:style-name`, or an automatic style with it as parent | `w:pStyle` | `\s1` after `\pard` |
| Headings | `text:h` with `text:outline-level` | `w:outlineLvl` in the style | `\outlinelevel` |
| Formatting by hand | an automatic style with only the differences | `w:pPr` and `w:rPr` with only the differences | everything, always |

ODT and DOCX write the styles once and then only what a paragraph or run does differently, with explicit negatives where a word is not bold in a bold heading (`fo:font-weight="normal"`, `w:b w:val="0"`). RTF is the exception: a reader that ignores the style sheet must still see the right formatting, so RTF repeats the whole formatting after every `\s`. Word and LibreOffice write it that way too.

```java title="amade-writepad · service/format/odt/OdtWriter.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/format/odt/OdtWriter.java:named-style"
```

Reading needs a decision for styles WritePad does not know. A document from LibreOffice may have *Text Body* for the text, or a style of its own based on Heading 1. The readers walk the chain of parents to the first style with a known name, `ParagraphStyle.forName` maps the names every program uses to the eleven built-in ones, and the paragraph keeps its look through the formatting set on it by hand:

```java title="amade-writepad · service/format/docx/DocxReader.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/format/docx/DocxReader.java:styles"
```

The tests are the usual three: a round trip of a document with every style and a redefined Heading 1, the bytes, and a file of each format that LibreOffice wrote from HTML with `<h1>` to `<h4>`, `<blockquote>` and `<pre>`. LibreOffice calls a quote *Block Quotation* in DOCX and RTF, which the round trip would never have told.

## Saving, and a second Ctrl+S

Opening and saving follow Pad (chapter 10): read and parse in the background, write the bytes on the EDT from the document in the editor, put them on disk in the background, and remember which revision was saved:

```java title="amade-writepad · ui/main/MainPresenter.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/ui/main/MainPresenter.java:save"
```

The first line of `save` was missing when this chapter was written. *Save As* and the "save your changes?" question checked `busy`, but plain *Save* of a document that already had a file did not. Press Ctrl+S twice quickly, and two writes of the same file ran on two virtual threads. They could finish in either order, and the older text could be the one left on disk: the same race as in Calendar's saving (chapter 21). Calendar had to save every change, so it chains its saves. A word processor saves when the user says so, and the user sees whether the document is saved: the title keeps its `*`. So WritePad simply starts no second save while one is running. The test drives both saves through an executor that only queues its tasks:

```java title="amade-writepad · MainPresenterTest.java"
--8<-- "amade-writepad/src/test/java/eu/amadeos/apps/writepad/ui/main/MainPresenterTest.java:second-save"
```

## Printing

Because the layout is in points, printing is the easy part. `java.awt.print` hands a program a `Graphics2D` per page, in points, with (0, 0) at the corner of the paper. WritePad paints each page with the same views as the screen, at zoom 1:

```java title="amade-writepad · service/layout/PagedSectionView.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/layout/PagedSectionView.java:paint-page"
```

```java title="amade-writepad · service/print/DocumentPageable.java"
--8<-- "amade-writepad/src/main/java/eu/amadeos/apps/writepad/service/print/DocumentPageable.java:print"
```

The lines on paper break where they broke on the screen, because they are the same lines. This is the payoff of the decision in "Points inside, pixels outside".

### Printing moves into amade-commons

Calendar prints its reports (chapter 21), and WritePad prints documents. Their pages have nothing in common. Calendar paints grids of days, WritePad paints text views. But around the pages they had the same code twice:

- **Paper sizes**: Calendar's `paperFor(Locale)` with A4 and Letter in points, WritePad's `PaperSize` with A5, A4, Letter and Legal.
- **A page format** for a paper, portrait or landscape, with or without margins.
- **What the printer is told**: the paper and the job name, and *not* the orientation.
- **Where the pages go**: a PostScript file (Calendar), the system's print dialog (WritePad).

That is the part that moves, into the package `eu.amadeos.commons.print`. `PaperSize` comes from WritePad, with one addition from Calendar, the usual paper of the user's country:

```java title="amade-commons · print/PaperSize.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/print/PaperSize.java:for-locale"
```

`PrintJobs` has the rest, as static methods, because none of them keeps state:

```java title="amade-commons · print/PrintJobs.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/print/PrintJobs.java:page-format"
```

```java title="amade-commons · print/PrintJobs.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/print/PrintJobs.java:attributes"
```

The comment on `attributes` is a lesson WritePad learned the hard way: a `PageFormat` in landscape already turns the graphics, and some printer drivers turn the page again when they are also asked for landscape. A rule like that belongs in one place, next to a test.

### PostScript without a printer

Calendar wrote its PostScript files with a `PrinterJob` and a `Destination` attribute. Moving the code into amade-commons meant writing a test for it there, and the test failed on the build machine with "No print service found": a `PrinterJob` needs a printer, even when it only writes a file. Calendar's own tests had skipped on such machines all along, so nobody had noticed that *Print to File* only worked where CUPS was installed.

The JDK has a second way to print: *stream print services*, which write a format into any `OutputStream`. One of them writes PostScript and needs no printer at all:

```java title="amade-commons · print/PrintJobs.java"
--8<-- "amade-commons/src/main/java/eu/amadeos/commons/print/PrintJobs.java:post-script"
```

Now Calendar prints to a file on any computer, and its tests run everywhere instead of skipping. A test that is skipped is a test that does not exist; when you write `assumeTrue`, check now and then where it is false.

### Re-pinning the two programs

The move is the same as for `Texts` in chapter 22:

1. amade-commons gets `print/PaperSize.java`, `print/PrintJobs.java` and their tests.
2. WritePad deletes its `PaperSize` and imports the one from amade-commons (the format readers and writers, the page setup dialog and the menus use it). `DocumentPageable.pageFormat` and `PrintService` shrink to one line each.
3. Calendar deletes `paperFor`, `render` and `printerNames`, and asks `PaperSize.forLocale` and `PrintJobs`. Its test helper that probed for a print service is gone.
4. Both programs pin the new amade-commons commit, and all tests run.

What stays in the programs is what is really theirs: Calendar's report pages and its `lp` command with dtcm's options, WritePad's pages and its use of the system print dialog.

## What amade-commons gives WritePad

| Part | Since | WritePad uses it for |
|---|---|---|
| `AmadeApplication`, `CommonsModule`, `EdtExecutor` | chapter 10 | start-up, wiring, the two threads |
| `Messages`, `Mnemonics`, `LocaleSettings` | chapter 10 | five languages, menu letters, the language setting |
| `AmadeUI`, `DesktopTheme` | chapter 10 | the theme's colours for bars, ruler and icons |
| `AppDirectories`, `PropertiesFile` | chapter 20 | the preferences: which bars are shown |
| `PaperSize`, `PrintJobs` | **this chapter** | paper sizes in the formats and the page setup; printing |

### And what stays in WritePad

- **The toolbar icons** (`ToolIcon`, `ToolButtons`) are drawn in Java2D in the theme's colours, so WritePad needs no icon theme. Fractals draws its toolbar the same way, and they move into amade-commons with it in chapter 30.
- **`LibreOffice`**, the runner for `soffice`, moves with Imagescan's command runner in chapter 31.
- **`DocumentFiles`** writes files atomically and keeps their permissions, like Pad. That makes four atomic writes (Pad, `PropertiesFile`, Calendar, WritePad), and the first exercise of chapter 21 is still open.

## Exercises

1. **Keep together.** Headings should never be the last line of a page. Add a paragraph attribute "keep with next" and make `PagedSectionView` move a heading to the next page when the paragraph after it does not start on the same page. Where does the loop stop if every paragraph has the attribute?
2. **Page numbers in the footer.** The page view already paints a footer ("1 / 6"). Make it a setting of the document, saved in ODT as a footer with a page-number field. Which part of the code reads it, and which writes it?
3. **The paper where you live.** New documents always start on A4. Use `PaperSize.forLocale` for new documents. What should happen when a document from a Letter country is opened in an A4 country?
4. **A second Ctrl+S that counts.** Instead of ignoring a save while one is running, remember it and save again when the first one is done, with the newest text. Write the test first, with the queueing executor.
5. **Pictures in the middle.** A floating picture always starts at the top of its paragraph. Let the user drag it down: store its distance from the paragraph's top (ODT `svg:y`, DOCX `wp:posOffset`) and use it in `placeFloats`. What happens to the rows above the picture?
6. **Your own styles.** Let the Styles dialog add a style based on another one. What becomes of `ParagraphStyle`, an enum? Which names do the three writers use, and what does `forName` do with them when the file comes back?
7. **Merged cells.** Add a column span to the paragraphs of a cell (one more attribute) and teach `layoutMinorAxis`, the hit test and the three formats about it. Where does the definition "a cell is the paragraphs with the same row and column" still hold, and what replaces it for a cell that spans rows?
8. **Print preview for Calendar.** `ReportPages` paints into any `Graphics2D`. Show the pages in a window before printing. Could WritePad's page view show Calendar's pages? What would a `PreviewPanel` in amade-commons need to know about the pages?

## Summary

- A Swing text component is a document, its elements, the views and an editor kit. Replace the views through the kit's view factory, and typing, caret, selection and undo keep working.
- Lay out in one unit (points) and translate only at the edge; then zoom does not move line breaks, and printing is the same painting at scale 1.
- A layout that moves things needs a case in which it stops moving them.
- Labels of list items are computed from structure, not stored as text.
- A picture is one character with the picture in its attributes, so caret, selection, clipboard and undo work for it unchanged. Text flows around a floating picture because the paragraph answers the flow strategy's two questions per row.
- Paragraph styles are Swing's named styles as resolve parents. Define every key in every style, apply a style the way Word does, and let the readers fill complete formatting and turn it into styles in one place, comparing against a snapshot.
- A table can be plain paragraphs that know their row and column. Protect the line ends between cells in `remove`, and lay the paragraphs of a row side by side in the minor axis.
- ODT and DOCX are ZIP files with XML inside; the JDK's `java.util.zip` and StAX read and write them. Recognise files by content first, by name second.
- Two writes of the same file must not run at the same time: chain them, or start no second one.
- Paper sizes, page formats, printer attributes, the print dialog and PostScript files move into amade-commons as `PaperSize` and `PrintJobs`, their second user after Calendar. The JDK's stream print service writes PostScript without a printer.
