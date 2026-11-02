package eu.amadeos.commons.typeset;

import java.util.List;

/** A chapter set into pages of one size with one appearance. */
public record ChapterLayout(List<Page> pages) {
    public ChapterLayout {
        if (pages.isEmpty()) throw new IllegalArgumentException("a chapter has at least one page");
        pages = List.copyOf(pages);
    }

    /** The page that shows the offset: the last page that starts at or before it. */
    public int pageOf(int offset) {
        int found = 0;
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).start() <= offset) found = i;
            else break;
        }
        return found;
    }

    public int pageCount() {
        return pages.size();
    }
}
