package eu.amadeos.commons.i18n;

import java.util.Locale;
import java.util.Objects;

/**
 * The texts of the running application, for code that does not get a {@link Messages} injected:
 * dialogs, table models, renderers. The application names its text files once, in {@code main}
 * before {@code AmadeApplication.launch}; after that a static import gives every class {@code tr}.
 *
 * <pre>
 * Texts.use(AmadeMailApplication.class, "eu.amadeos.apps.mail.messages");   // in main
 *
 * import static eu.amadeos.commons.i18n.Texts.tr;
 * new JButton(tr("compose.send"));                     // Send
 * status.setText(tr("status.fetched", count));         // {0} new messages
 * </pre>
 *
 * <p>The texts are loaded on first use, in the default locale, which {@code AmadeApplication} set to
 * the resolved language, and loaded again if the default locale changes. Tests that reach code using
 * {@code tr} call {@link #use} too, typically in a {@code @BeforeAll} method.
 */
public final class Texts {
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
     * Names the application's text files.
     *
     * @param anchor a class of the application; its class loader finds the files
     * @param bundle e.g. {@code eu.amadeos.apps.mail.messages} for {@code eu/amadeos/apps/mail/messages.properties}
     */
    public static void use(Class<?> anchor, String bundle) {
        source = new Source(Objects.requireNonNull(anchor, "anchor"), Objects.requireNonNull(bundle, "bundle"));
    }

    /** The application's texts in the default locale. */
    public static Messages messages() {
        Source s = source;
        if (s == null) {
            throw new IllegalStateException("Texts.use(...) was not called: the application has not named its text files");
        }
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
}
