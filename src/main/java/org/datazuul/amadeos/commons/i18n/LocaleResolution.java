package org.datazuul.amadeos.commons.i18n;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The locale an application runs in, and where it came from.
 *
 * @param locale   language and region; its language is always a {@link Language}
 * @param language the language of {@code locale}
 * @param source   which setting decided
 */
public record LocaleResolution(Locale locale, Language language, Source source) {

    /** The settings in the order they are asked. */
    public enum Source {
        /** The application's own setting ("Settings" → Language). */
        APPLICATION,
        /** The amadeOS desktop, {@code ~/.config/amadeos/locale.conf}. */
        DESKTOP,
        /** {@code LANGUAGE}, {@code LC_ALL}, {@code LC_MESSAGES} or {@code LANG}, e.g. outside amadeOS. */
        ENVIRONMENT,
        /** Nothing usable was set: English. */
        DEFAULT
    }

    public LocaleResolution {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(source, "source");
    }

    /**
     * Decides the locale: the application's own setting, else the desktop's, else the environment, else
     * English. A setting whose language amadeOS is not translated into is skipped.
     *
     * @param application the application's own setting; empty means "follow the desktop"
     * @param desktop     the locale of {@code locale.conf}, see {@link LocaleStore#current()}
     * @param environment the process environment
     */
    public static LocaleResolution resolve(Optional<Language> application, Optional<Locale> desktop,
                                           Map<String, String> environment) {
        Optional<Locale> fromEnvironment = fromEnvironment(environment);
        if (application.isPresent()) {
            Language language = application.get();
            // Keep the region of the desktop or the environment if it is the same language (de-AT, fr-CH).
            Locale locale = desktop.filter(l -> Language.of(l).equals(application))
                    .or(() -> fromEnvironment.filter(l -> Language.of(l).equals(application)))
                    .orElse(language.locale());
            return new LocaleResolution(locale, language, Source.APPLICATION);
        }
        if (desktop.isPresent()) return of(desktop.get(), Source.DESKTOP);
        if (fromEnvironment.isPresent()) return of(fromEnvironment.get(), Source.ENVIRONMENT);
        return new LocaleResolution(Language.ENGLISH.locale(), Language.ENGLISH, Source.DEFAULT);
    }

    private static LocaleResolution of(Locale locale, Source source) {
        return new LocaleResolution(locale, Language.of(locale).orElseThrow(), source);
    }

    /**
     * The locale for messages as gettext finds it: {@code LANGUAGE} (a colon-separated priority list, ignored
     * when the locale is {@code C}), then the first set of {@code LC_ALL}, {@code LC_MESSAGES}, {@code LANG}.
     */
    static Optional<Locale> fromEnvironment(Map<String, String> environment) {
        String messages = firstSet(environment, "LC_ALL", "LC_MESSAGES", "LANG");
        boolean posix = messages == null || messages.equals("C") || messages.startsWith("C.")
                || messages.equals("POSIX");
        String priority = environment.get("LANGUAGE");
        if (!posix && priority != null) {
            for (String entry : priority.split(":")) {
                Optional<Locale> locale = Language.parseLocale(entry);
                if (locale.isPresent()) return locale;
            }
        }
        return messages == null ? Optional.empty() : Language.parseLocale(messages);
    }

    private static String firstSet(Map<String, String> environment, String... names) {
        for (String name : names) {
            String value = environment.get(name);
            if (value != null && !value.isBlank()) return value.strip();
        }
        return null;
    }
}
