package eu.amadeos.commons.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The texts of an application in one language, from {@code .properties} files in UTF-8:
 * {@code messages.properties} holds the English texts (the fallback for every missing key),
 * {@code messages_de.properties}, {@code messages_fr.properties}, ... the translations.
 *
 * <pre>
 * Messages msg = Messages.load(MainWindow.class, "eu.amadeos.audio.messages", locale);
 * new JButton(msg.get("radio.stations"));                // Stations...
 * status.setText(msg.format("player.tracks", count));     // {0} tracks
 * </pre>
 *
 * <p>Texts are formatted with {@link MessageFormat} in the locale ({@code {0,number}}, {@code {0,date}},
 * {@code {0,choice,...}}), but apostrophes are plain characters, so translators write {@code l'album}
 * and never {@code l''album}. A missing key shows as {@code !key!} and is logged once.
 */
public final class Messages {
    private static final Logger LOG = LoggerFactory.getLogger(Messages.class);
    private static final ResourceBundle.Control CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    private final ResourceBundle bundle;
    private final Locale locale;
    private final String baseName;
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    private Messages(ResourceBundle bundle, Locale locale, String baseName) {
        this.bundle = bundle;
        this.locale = locale;
        this.baseName = baseName;
    }

    /**
     * Loads the texts for the locale. A language without a file falls back to the base file (English), never
     * to the JVM's default locale.
     *
     * @param anchor   a class of the application; its class loader finds the files
     * @param baseName e.g. {@code eu.amadeos.audio.messages} for
     *                 {@code eu/amadeos/audio/messages.properties}
     */
    public static Messages load(Class<?> anchor, String baseName, Locale locale) {
        Objects.requireNonNull(locale, "locale");
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, anchor.getClassLoader(), CONTROL);
        return new Messages(bundle, locale, baseName);
    }

    /** Loads the texts in the default locale, which {@code AmadeApplication} set to the resolved language. */
    public static Messages load(Class<?> anchor, String baseName) {
        return load(anchor, baseName, Locale.getDefault());
    }

    /** The texts of amade-commons itself (language names, start-up errors, ...). */
    public static Messages commons(Locale locale) {
        return load(Messages.class, "eu.amadeos.commons.i18n.commons", locale);
    }

    public Locale locale() {
        return locale;
    }

    /** True if the key exists, in the language or in English. */
    public boolean has(String key) {
        return bundle.containsKey(key);
    }

    /** The text as written in the file. */
    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            if (reported.add(key)) LOG.warn("Missing text {} in {}", key, baseName);
            return "!" + key + "!";
        }
    }

    /** The text with {@code {0}}, {@code {1}}, ... replaced by the arguments, formatted in the locale. */
    public String format(String key, Object... arguments) {
        String pattern = get(key);
        if (arguments.length == 0) return pattern;
        try {
            return new MessageFormat(pattern.replace("'", "''"), locale).format(arguments);
        } catch (IllegalArgumentException e) {
            if (reported.add(key + "#format")) LOG.warn("Bad pattern {} in {}: {}", key, baseName, e.getMessage());
            return pattern;
        }
    }
}
