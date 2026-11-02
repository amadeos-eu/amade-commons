package eu.amadeos.commons.chars;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.swing.HorizonUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Predicate;

/**
 * The Special Characters window. It stays open next to the document; {@link SpecialCharacters} opens it and
 * says where an inserted character goes.
 */
final class SpecialCharactersDialog extends JDialog {
    private final Messages texts;
    private final CharacterCatalog catalog;
    private final RecentCharacters recent;
    /** Puts the text into the program; false if there is nowhere to put it. */
    private Predicate<String> insert;

    private final JTextField search = new JTextField(22) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // The hint while the field is empty
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(UIManager.getColor("Label.disabledForeground"));
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(texts.get("chars.search.hint"), in.left + 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    };
    private final JList<CharacterCatalog.Category> categories = new JList<>(CharacterCatalog.Category.values());
    private final CharacterGrid grid = new CharacterGrid();
    private final JLabel big = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel name = new JLabel(" ");
    private final JLabel words = new JLabel(" ");
    private final JLabel code = new JLabel(" ");
    private final JLabel shownIn = new JLabel(" ");
    private final JLabel status = new JLabel(" ");
    private final JLabel fontLabel = new JLabel();
    private final JButton insertButton = new JButton();
    private final JButton copyButton = new JButton();
    private boolean searching;

    SpecialCharactersDialog(Window owner, CharacterCatalog catalog, RecentCharacters recent, Predicate<String> insert) {
        super(owner, ModalityType.MODELESS);
        this.catalog = catalog;
        this.recent = recent;
        this.insert = insert;
        this.texts = Messages.commons(catalog.locale());
        setTitle(texts.get("chars.title"));
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        build();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                if (!searching && categories.getSelectedValue() == CharacterCatalog.Category.RECENT) showCategory();
            }
        });
    }

    void setTarget(Predicate<String> insert) {
        this.insert = insert;
    }

    /** The font of the text the characters go into. */
    void setTextFont(Font font) {
        grid.setTextFont(font);
        fontLabel.setText(texts.format("chars.font", font.getFamily()));
        showPreview(grid.selectedCharacter());
    }

    /** Shows the window with the cursor in the search field. */
    void open() {
        if (!isVisible()) {
            search.setText("");
            if (categories.getSelectedIndex() < 0) categories.setSelectedIndex(0);
            showCategory();
        }
        setVisible(true);
        toFront();
        search.requestFocusInWindow();
        search.selectAll();
    }

    private void build() {
        JPanel all = new JPanel(new BorderLayout(0, 6));
        all.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel searchLabel = Mnemonics.apply(new JLabel(), texts.get("chars.search"));
        searchLabel.setLabelFor(search);
        search.setToolTipText(texts.get("chars.search.hint"));
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchChanged();
            }
        });
        // Enter in the search field inserts the selected result, Down goes into the grid
        search.addActionListener(e -> insertSelected());
        search.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "toGrid");
        search.getActionMap().put("toGrid", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                grid.requestFocusInWindow();
            }
        });
        fontLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.add(searchLabel, BorderLayout.WEST);
        top.add(search, BorderLayout.CENTER);
        top.add(fontLabel, BorderLayout.EAST);
        all.add(top, BorderLayout.NORTH);

        categories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categories.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                String text = texts.get(((CharacterCatalog.Category) value).textKey());
                return super.getListCellRendererComponent(list, text, index, sel, focus);
            }
        });
        categories.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || categories.getSelectedIndex() < 0) return;
            if (!search.getText().isEmpty()) {
                searching = false;
                search.setText("");
            }
            showCategory();
        });
        categories.getAccessibleContext().setAccessibleName(texts.get("chars.categories"));
        JScrollPane categoryScroll = new JScrollPane(categories);
        categoryScroll.setPreferredSize(new Dimension(Math.max(160, categories.getPreferredSize().width + 24), 10));

        grid.onSelect(this::showPreview);
        grid.onActivate(cp -> insert(new String(Character.toChars(cp))));
        grid.getAccessibleContext().setAccessibleName(texts.get("chars.title"));
        JScrollPane gridScroll = new JScrollPane(grid);
        gridScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        gridScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        gridScroll.getVerticalScrollBar().setUnitIncrement(grid.cellSize());

        JPanel centre = new JPanel(new BorderLayout(6, 0));
        centre.add(categoryScroll, BorderLayout.WEST);
        centre.add(gridScroll, BorderLayout.CENTER);
        centre.add(preview(), BorderLayout.EAST);
        all.add(centre, BorderLayout.CENTER);

        status.setForeground(UIManager.getColor("Label.disabledForeground"));
        Mnemonics.apply(insertButton, texts.get("chars.insert"));
        insertButton.addActionListener(e -> insertSelected());
        Mnemonics.apply(copyButton, texts.get("chars.copy"));
        copyButton.addActionListener(e -> copySelected());
        JButton close = new JButton(texts.get("chars.close"));
        close.addActionListener(e -> setVisible(false));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(copyButton);
        buttons.add(insertButton);
        buttons.add(close);
        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);
        all.add(bottom, BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(e -> setVisible(false), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setContentPane(all);
        status.setText(texts.get("chars.hint"));
        setTextFont(UIManager.getFont("TextArea.font"));
    }

    private JComponent preview() {
        int width = Math.round(170 * Math.max(1f, UIManager.getFont("Label.font").getSize2D() / 12f));
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        big.setOpaque(true);
        big.setBackground(UIManager.getColor("List.background"));
        big.setForeground(UIManager.getColor("List.foreground"));
        Border frame = HorizonUI.active() ? new HorizonUI.LineBorder(true) : AmadeUI.sunken();
        big.setBorder(frame);
        Dimension box = new Dimension(width, width * 3 / 5);
        big.setPreferredSize(box);
        big.setMaximumSize(box);
        big.setMinimumSize(box);
        for (JComponent c : new JComponent[]{big, name, words, code, shownIn}) c.setAlignmentX(0);
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        code.setFont(new Font(Font.MONOSPACED, Font.PLAIN, code.getFont().getSize()));
        shownIn.setForeground(UIManager.getColor("Label.disabledForeground"));
        p.add(big);
        p.add(Box.createVerticalStrut(6));
        p.add(name);
        p.add(Box.createVerticalStrut(2));
        p.add(words);
        p.add(Box.createVerticalStrut(4));
        p.add(code);
        p.add(Box.createVerticalStrut(4));
        p.add(shownIn);
        p.add(Box.createVerticalGlue());
        p.setPreferredSize(new Dimension(width, box.height));
        name.putClientProperty("width", width);
        return p;
    }

    private void showCategory() {
        CharacterCatalog.Category c = categories.getSelectedValue();
        if (c == null) return;
        grid.show(c == CharacterCatalog.Category.RECENT ? recent.list() : catalog.characters(c));
        status.setText(texts.get("chars.hint"));
    }

    private void searchChanged() {
        String q = search.getText();
        if (q.isBlank()) {
            if (searching) {
                searching = false;
                if (categories.getSelectedIndex() < 0) categories.setSelectedIndex(0);
                showCategory();
            }
            return;
        }
        searching = true;
        categories.clearSelection();
        List<Integer> found = catalog.search(q);
        grid.show(found);
        status.setText(found.isEmpty() ? texts.get("chars.none") : texts.format("chars.found", found.size()));
    }

    private void showPreview(int cp) {
        boolean has = cp >= 0;
        insertButton.setEnabled(has);
        copyButton.setEnabled(has);
        if (!has) {
            for (JLabel l : new JLabel[]{big, name, words, code, shownIn}) l.setText(" ");
            return;
        }
        Font f = grid.fontFor(cp);
        big.setFont(f.deriveFont(Font.PLAIN, 56f * Math.max(1f, UIManager.getFont("Label.font").getSize2D() / 12f)));
        big.setText(CharacterGrid.visible(cp));
        int width = (Integer) name.getClientProperty("width") - 10;
        name.setText(html(CharacterCatalog.name(cp), width));
        String own = catalog.localWords(cp);
        words.setText(own.isEmpty() ? " " : html(own, width));
        code.setText(CharacterCatalog.code(cp));
        shownIn.setText(f == grid.textFont() ? " " : html(texts.get("chars.other-font"), width));
    }

    private static String html(String text, int width) {
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div style='width:" + width + "px'>" + escaped + "</div></html>";
    }

    private void insertSelected() {
        int cp = grid.selectedCharacter();
        if (cp >= 0) insert(new String(Character.toChars(cp)));
    }

    // --8<-- [start:insert]
    void insert(String text) {
        if (insert.test(text)) {
            remember(text);
            status.setText(texts.format("chars.inserted", text));
        } else {
            // Nowhere to put it (no text field had the focus, or it is read-only): the clipboard still helps
            copy(text);
            status.setText(texts.get("chars.copied-instead"));
        }
    }
    // --8<-- [end:insert]

    private void copySelected() {
        int cp = grid.selectedCharacter();
        if (cp < 0) return;
        String text = new String(Character.toChars(cp));
        copy(text);
        remember(text);
        status.setText(texts.format("chars.copied", text));
    }

    private void copy(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (IllegalStateException e) {
            UIManager.getLookAndFeel().provideErrorFeedback(this);
        }
    }

    private void remember(String text) {
        try {
            recent.add(text);
        } catch (RuntimeException e) {
            // A read-only home must not stop inserting; the list just does not grow
        }
    }

    // For tests
    CharacterGrid grid() {
        return grid;
    }

    JTextField searchField() {
        return search;
    }

    JList<CharacterCatalog.Category> categoryList() {
        return categories;
    }
}
