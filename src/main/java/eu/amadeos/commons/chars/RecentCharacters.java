package eu.amadeos.commons.chars;

import eu.amadeos.commons.config.PropertiesFile;
import eu.amadeos.commons.config.XdgDirectories;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The characters the user inserted last, newest first, shared by all programs:
 * {@code ~/.config/amadeos/special-characters.properties}. The file is read again each time the list is asked
 * for, so a character inserted in WritePad shows up in Mail's dialog too.
 */
public final class RecentCharacters {
    /** The list keeps this many characters. */
    public static final int SIZE = 24;
    /** Shown while nothing was inserted yet. */
    static final String START = "äöüß€„“–—…°×";

    private final Path file;

    public RecentCharacters(Path file) {
        this.file = file;
    }

    /** The list of the user's desktop. */
    public static RecentCharacters of(XdgDirectories xdg) {
        return new RecentCharacters(xdg.amadeosConfig().resolve("special-characters.properties"));
    }

    /** Newest first; a few common characters while the list is still empty. */
    public List<Integer> list() {
        String stored = PropertiesFile.load(file).getProperty("recent", "");
        List<Integer> list = new ArrayList<>();
        for (String hex : stored.split(",")) {
            try {
                int cp = Integer.parseInt(hex.strip(), 16);
                if (CharacterCatalog.offered(cp) && !list.contains(cp)) list.add(cp);
            } catch (NumberFormatException ignored) {
                // a hand-edited or broken entry is skipped
            }
        }
        if (list.isEmpty()) START.codePoints().forEach(list::add);
        return list.size() > SIZE ? list.subList(0, SIZE) : list;
    }

    /** Puts the characters of the text at the front of the list. */
    public void add(String inserted) {
        List<Integer> stored = new ArrayList<>();
        String old = PropertiesFile.load(file).getProperty("recent", "");
        if (!old.isBlank()) stored.addAll(list());
        int[] cps = inserted.codePoints().toArray();
        for (int i = cps.length - 1; i >= 0; i--) {
            stored.remove(Integer.valueOf(cps[i]));
            stored.addFirst(cps[i]);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(SIZE, stored.size()); i++) {
            if (i > 0) sb.append(',');
            sb.append(Integer.toHexString(stored.get(i)).toUpperCase(java.util.Locale.ROOT));
        }
        Properties p = new Properties();
        p.setProperty("recent", sb.toString());
        PropertiesFile.store(file, p, "Characters inserted last with the Special Characters dialog, newest first");
    }
}
