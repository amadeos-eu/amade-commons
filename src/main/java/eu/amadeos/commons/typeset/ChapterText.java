package eu.amadeos.commons.typeset;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One chapter file as blocks for the typesetter, with its plain text for search and copying.
 *
 * @param anchors   the {@code id}s of the chapter and the offsets they mark, for links and the table of contents
 * @param footnotes texts of footnotes the chapter hides in {@code aside} elements, by id
 */
public record ChapterText(String path, List<Block> blocks, String plain, Map<String, Integer> anchors,
                          Map<String, String> footnotes) {
    public ChapterText {
        blocks = List.copyOf(blocks);
        anchors = Map.copyOf(anchors);
        footnotes = Map.copyOf(footnotes);
    }

    /** The number of characters, for the reading progress through the book. */
    public int length() {
        return plain.length();
    }

    public Optional<Integer> anchor(String id) {
        return Optional.ofNullable(anchors.get(id));
    }

    /** The paragraph that contains the offset; empty for pictures and past the end. */
    public Optional<Block.Text> paragraphAt(int offset) {
        for (Block block : blocks) {
            if (block instanceof Block.Text t && offset >= t.offset() && offset <= t.offset() + t.length()) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    /** The link at a character, if there is one. */
    public Optional<RunStyle> linkAt(int offset) {
        return paragraphAt(offset).flatMap(t -> t.runs().stream()
                .filter(r -> r.style().link() != null && offset >= t.offset() + r.start() && offset < t.offset() + r.end())
                .map(StyledRun::style).findFirst());
    }

    /** About this many characters of text from the offset on, in one line: for bookmarks and search results. */
    public String excerpt(int offset, int length) {
        int from = Math.max(0, Math.min(offset, plain.length()));
        int to = Math.min(plain.length(), from + length);
        String s = plain.substring(from, to).replace('￼', ' ').replaceAll("\\s+", " ").strip();
        return to < plain.length() ? s + "…" : s;
    }
}
