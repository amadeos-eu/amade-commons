package eu.amadeos.commons.chars;

import eu.amadeos.commons.swing.HorizonUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The grid of characters in the Special Characters dialog. Arrow keys, Home, End, Page Up and Page Down move the
 * selection; Enter or a double-click activates the selected character. Only the visible rows are painted, so
 * the whole of Unicode fits in it.
 */
final class CharacterGrid extends JComponent implements Scrollable {
    static final int COLUMNS = 12;

    private List<Integer> characters = List.of();
    private int selected = -1;
    private Font font = new Font(Font.DIALOG, Font.PLAIN, 20);
    private Font fallback = new Font(Font.DIALOG, Font.PLAIN, 20);
    private IntConsumer onSelect = cp -> { };
    private IntConsumer onActivate = cp -> { };

    CharacterGrid() {
        setFocusable(true);
        setOpaque(true);
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int index = indexAt(e.getPoint());
                if (index >= 0) {
                    select(index);
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) onActivate.accept(characters.get(index));
                }
            }
        });
        bind("LEFT", -1);
        bind("RIGHT", 1);
        bind("UP", -COLUMNS);
        bind("DOWN", COLUMNS);
        bind("PAGE_UP", -COLUMNS * 5);
        bind("PAGE_DOWN", COLUMNS * 5);
        bind("HOME", Integer.MIN_VALUE / 2);
        bind("END", Integer.MAX_VALUE / 2);
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate");
        getActionMap().put("activate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selected >= 0) onActivate.accept(characters.get(selected));
            }
        });
    }

    private void bind(String key, int step) {
        getInputMap().put(KeyStroke.getKeyStroke(key), "move" + key);
        getActionMap().put("move" + key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (characters.isEmpty()) return;
                long target = (long) Math.max(selected, 0) + step;
                select((int) Math.max(0, Math.min(characters.size() - 1, target)));
            }
        });
    }

    void onSelect(IntConsumer listener) {
        this.onSelect = listener;
    }

    void onActivate(IntConsumer listener) {
        this.onActivate = listener;
    }

    /** Shows the characters and selects the first one. */
    void show(List<Integer> list) {
        this.characters = List.copyOf(list);
        this.selected = -1;
        revalidate();
        repaint();
        if (!characters.isEmpty()) select(0);
        else onSelect.accept(-1);
    }

    List<Integer> characters() {
        return characters;
    }

    /** The selected character, -1 if none. */
    int selectedCharacter() {
        return selected < 0 ? -1 : characters.get(selected);
    }

    /** The font of the text the characters go into; characters it lacks are drawn in a fallback font. */
    void setTextFont(Font textFont) {
        this.font = textFont.deriveFont(Font.PLAIN, 20f * scale());
        this.fallback = new Font(Font.DIALOG, Font.PLAIN, font.getSize());
        revalidate();
        repaint();
    }

    Font textFont() {
        return font;
    }

    // --8<-- [start:emoji-font]
    /**
     * The font the character is drawn in: emoji in the black and white emoji font when it is installed (Java2D
     * cannot draw colour emoji fonts), everything else in the text's font, or the fallback if the text's font
     * lacks it.
     */
    Font fontFor(int cp) {
        if (CharacterCatalog.isEmoji(cp)) {
            Font emoji = emojiFont(font.getSize());
            if (emoji != null && emoji.canDisplay(cp)) return emoji;
        }
        return font.canDisplay(cp) ? font : fallback;
    }

    /** The family of the black and white emoji font, Debian package {@code fonts-symbola}. */
    static final String EMOJI_FAMILY = "Symbola";
    private static Boolean emojiInstalled;

    /** The emoji font in the size, null if it is not installed. */
    static Font emojiFont(int size) {
        if (emojiInstalled == null) {
            emojiInstalled = java.util.Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames(java.util.Locale.ROOT)).contains(EMOJI_FAMILY);
        }
        return emojiInstalled ? new Font(EMOJI_FAMILY, Font.PLAIN, size) : null;
    }
    // --8<-- [end:emoji-font]

    void select(int index) {
        if (index == selected) return;
        selected = index;
        Rectangle r = cell(index);
        scrollRectToVisible(r);
        repaint();
        onSelect.accept(selectedCharacter());
    }

    private float scale() {
        return Math.max(1f, UIManager.getFont("Label.font").getSize2D() / 12f);
    }

    int cellSize() {
        return Math.round(36 * scale());
    }

    private Rectangle cell(int index) {
        int s = cellSize();
        return new Rectangle((index % COLUMNS) * s, (index / COLUMNS) * s, s, s);
    }

    int indexAt(Point p) {
        int s = cellSize();
        int col = p.x / s, row = p.y / s;
        if (col < 0 || col >= COLUMNS || p.y < 0) return -1;
        int index = row * COLUMNS + col;
        return index < characters.size() ? index : -1;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int index = indexAt(e.getPoint());
        if (index < 0) return null;
        int cp = characters.get(index);
        return CharacterCatalog.name(cp) + "  " + CharacterCatalog.code(cp);
    }

    @Override
    public Dimension getPreferredSize() {
        int s = cellSize();
        int rows = Math.max(6, (characters.size() + COLUMNS - 1) / COLUMNS);
        return new Dimension(COLUMNS * s, rows * s);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = UIManager.getColor("List.background");
        Color fg = UIManager.getColor("List.foreground");
        Color selBg = UIManager.getColor("List.selectionBackground");
        Color selFg = UIManager.getColor("List.selectionForeground");
        boolean flat = HorizonUI.active();
        Color line = mix(bg, fg, 0.15);
        g.setColor(bg);
        Rectangle clip = g.getClipBounds() != null ? g.getClipBounds() : new Rectangle(getSize());
        g.fill(clip);
        int s = cellSize();
        int first = Math.max(0, clip.y / s * COLUMNS);
        int last = Math.min(characters.size(), ((clip.y + clip.height) / s + 1) * COLUMNS);
        for (int i = first; i < last; i++) {
            Rectangle r = cell(i);
            boolean sel = i == selected;
            if (sel) {
                g.setColor(selBg);
                if (flat) g.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, s - 4, s - 4, 8, 8));
                else g.fillRect(r.x + 1, r.y + 1, s - 1, s - 1);
            }
            if (!flat) {
                g.setColor(line);
                g.drawRect(r.x, r.y, s, s);
            }
            int cp = characters.get(i);
            String text = visible(cp);
            Font f = fontFor(cp);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            g.setColor(sel ? selFg : fg);
            g.drawString(text, r.x + (s - fm.stringWidth(text)) / 2, r.y + (s + fm.getAscent() - fm.getDescent()) / 2);
            if (sel && isFocusOwner() && !flat) {
                g.setColor(selFg);
                g.drawRect(r.x + 2, r.y + 2, s - 4, s - 4);
            }
        }
        g.dispose();
    }

    /** What a cell shows: spaces and invisible characters get a visible stand-in. */
    static String visible(int cp) {
        return switch (cp) {
            case 0x00A0 -> "⍽";
            case 0x202F, 0x2009 -> "·";
            case 0x00AD -> "-";
            default -> Character.getType(cp) == Character.FORMAT || Character.isSpaceChar(cp)
                    ? "⬚" : new String(Character.toChars(cp));
        };
    }

    private static Color mix(Color a, Color b, double share) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * share),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * share),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * share));
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.TABLE;
                }
            };
        }
        return accessibleContext;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        int s = cellSize();
        return new Dimension(COLUMNS * s, 6 * s);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return cellSize();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(cellSize(), visibleRect.height / cellSize() * cellSize());
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
