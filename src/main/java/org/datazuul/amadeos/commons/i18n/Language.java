package org.datazuul.amadeos.commons.i18n;

import java.util.Locale;
import java.util.Optional;

/**
 * The languages amadeOS and its applications are translated into. English is the source language and the
 * fallback for every missing translation.
 */
public enum Language {
    ENGLISH("en", "US", "English"),
    GERMAN("de", "DE", "Deutsch"),
    FRENCH("fr", "FR", "Français"),
    SPANISH("es", "ES", "Español"),
    ITALIAN("it", "IT", "Italiano");

    private final String code;
    private final String defaultCountry;
    private final String nativeName;

    Language(String code, String defaultCountry, String nativeName) {
        this.code = code;
        this.defaultCountry = defaultCountry;
        this.nativeName = nativeName;
    }

    /** ISO 639-1 code, e.g. {@code de}; also the value in settings files. */
    public String code() {
        return code;
    }

    /** The name of the language in the language itself, e.g. {@code Français}; shown in language menus. */
    public String nativeName() {
        return nativeName;
    }

    /** The usual locale of the language, e.g. {@code de-DE}. */
    public Locale locale() {
        return Locale.of(code, defaultCountry);
    }

    /** The usual POSIX locale, e.g. {@code de_DE.UTF-8}, for {@code LANG} of a session. */
    public String posixLocale() {
        return code + "_" + defaultCountry + ".UTF-8";
    }

    /** The language with the code, ignoring case; empty for unsupported languages. */
    public static Optional<Language> ofCode(String code) {
        if (code == null) return Optional.empty();
        String c = code.strip().toLowerCase(Locale.ROOT);
        for (Language language : values()) {
            if (language.code.equals(c)) return Optional.of(language);
        }
        return Optional.empty();
    }

    /** The language of the locale; empty if amadeOS is not translated into it. */
    public static Optional<Language> of(Locale locale) {
        return locale == null ? Optional.empty() : ofCode(locale.getLanguage());
    }

    /**
     * Parses a locale as found in {@code LANG}, {@code locale.conf} or a language setting: {@code de},
     * {@code de_AT}, {@code de_AT.UTF-8}, {@code de_AT.UTF-8@euro} or {@code de-AT}. Keeps the region, so
     * that dates and numbers are formatted as in Austria. Empty for {@code C}, {@code POSIX}, garbage and
     * languages amadeOS is not translated into.
     */
    public static Optional<Locale> parseLocale(String value) {
        if (value == null) return Optional.empty();
        String s = value.strip();
        int cut = indexOfAny(s, '.', '@');
        if (cut >= 0) s = s.substring(0, cut);
        String[] parts = s.split("[_-]", -1);
        if (parts.length == 0 || parts.length > 2) return Optional.empty();
        Optional<Language> language = ofCode(parts[0]);
        if (language.isEmpty()) return Optional.empty();
        if (parts.length == 2) {
            String country = parts[1].toUpperCase(Locale.ROOT);
            if (!country.matches("[A-Z]{2}|[0-9]{3}")) return Optional.empty();
            return Optional.of(Locale.of(language.get().code, country));
        }
        return Optional.of(language.get().locale());
    }

    private static int indexOfAny(String s, char a, char b) {
        int i = s.indexOf(a);
        int j = s.indexOf(b);
        if (i < 0) return j;
        if (j < 0) return i;
        return Math.min(i, j);
    }
}
