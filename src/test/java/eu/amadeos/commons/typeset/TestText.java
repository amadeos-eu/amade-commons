package eu.amadeos.commons.typeset;

/** Chapters and words for the typesetting tests. */
final class TestText {
    static final String CSS = """
            p { text-indent: 1.5em; margin: 0; text-align: justify }
            p.first { text-indent: 0 }
            h1 { text-align: center; font-size: 1.4em; margin: 2em 0 0.3em }
            .right { float: right; width: 40% }
            """;

    private TestText() {
    }

    static String xhtml(String title, String body) {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en" lang="en">
                <head><title>%s</title><link rel="stylesheet" type="text/css" href="style.css"/></head>
                <body>
                %s
                </body>
                </html>
                """.formatted(title, body);
    }

    static String words(int count) {
        String[] pool = ("Alice was beginning to get very tired of sitting by her sister on the bank and of having "
                + "nothing to do once or twice she had peeped into the book her sister was reading but it had no "
                + "pictures or conversations in it and what is the use of a book thought Alice without pictures").split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(' ');
            sb.append(pool[i % pool.length]);
        }
        return sb.toString();
    }
}
