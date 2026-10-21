package eu.amadeos.commons.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * The texts of the running application, for code that does not get a {@link Messages} injected:
 * dialogs, table models, renderers. A static import gives every class {@code tr}.
 *
 * <pre>
 * import static eu.amadeos.commons.i18n.Texts.tr;
 * new JButton(tr("compose.send"));                     // Send
 * status.setText(tr("status.fetched", count));         // {0} new messages
 * </pre>
 *
 * <p>The application declares its text files in the resource {@value #DECLARATION}:
 *
 * <pre>
 * bundle=eu.amadeos.apps.mail.messages
 * </pre>
 *
 * <p>A file instead of a call in {@code main} means that the program and its tests find the texts the
 * same way, without set-up code in every test. The texts are loaded on first use, in the default
 * locale, which {@code AmadeApplication} set to the resolved language, and loaded again if the default
 * locale changes.
 */
public final class Texts {
    /** The resource that names the application's text files. */
    public static final String DECLARATION = "META-INF/amadeos/texts.properties";

    /** Where the texts come from. */
    private record Source(Class<?> anchor, String bundle) {
    }

    /** The texts loaded for a source; replaced as a whole, so readers never see a mix. */
    private record Loaded(Source source, Messages messages) {
    }

    private static volatile Source source;
    private static volatile Loaded loaded;

    private Texts() {
    }

    /**
     * Uses other text files than the declared ones, e.g. in a test of a library.
     *
     * @param anchor a class whose class loader finds the files
     * @param bundle e.g. {@code eu.amadeos.apps.mail.messages} for {@code eu/amadeos/apps/mail/messages.properties}
     */
    public static void use(Class<?> anchor, String bundle) {
        source = new Source(Objects.requireNonNull(anchor, "anchor"), Objects.requireNonNull(bundle, "bundle"));
    }

    /** The base name of the application's text files, e.g. for {@link TranslationCheck}. */
    public static String bundle() {
        return source().bundle();
    }

    /** The application's texts in the default locale. */
    public static Messages messages() {
        Source s = source();
        Loaded l = loaded;
        if (l == null || l.source() != s || !l.messages().locale().equals(Locale.getDefault())) {
            l = new Loaded(s, Messages.load(s.anchor(), s.bundle(), Locale.getDefault()));
            loaded = l;
        }
        return l.messages();
    }

    /** The text for the key. */
    public static String tr(String key) {
        return messages().get(key);
    }

    /** The text with {@code {0}}, {@code {1}}, ... filled in, formatted in the locale. */
    public static String tr(String key, Object... arguments) {
        return messages().format(key, arguments);
    }

    private static Source source() {
        Source s = source;
        if (s == null) {
            synchronized (Texts.class) {
                if (source == null) source = declared(Texts.class.getClassLoader());
                s = source;
            }
        }
        return s;
    }

    /** Reads {@value #DECLARATION}; there must be exactly one, the application's. */
    static Source declared(ClassLoader loader) {
        List<URL> found;
        try {
            found = Collections.list(loader.getResources(DECLARATION));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot look for " + DECLARATION, e);
        }
        if (found.isEmpty()) {
            throw new IllegalStateException("No " + DECLARATION + ": the application has not named its text files");
        }
        if (found.size() > 1) throw new IllegalStateException("Several " + DECLARATION + " files: " + found);
        Properties properties = new Properties();
        try (InputStream in = found.getFirst().openStream()) {
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + found.getFirst(), e);
        }
        String bundle = properties.getProperty("bundle", "").strip();
        if (bundle.isEmpty()) throw new IllegalStateException(found.getFirst() + " names no bundle");
        return new Source(Texts.class, bundle);
    }
}
