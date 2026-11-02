package eu.amadeos.commons.typeset;

/** Characters {@code start} (inclusive) to {@code end} (exclusive) of a paragraph's text in one style. */
public record StyledRun(int start, int end, RunStyle style) {
}
