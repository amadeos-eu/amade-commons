package org.datazuul.amadeos.commons.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares the translations of a bundle with the English base file, for a unit test per application:
 *
 * <pre>
 * assertEquals(Map.of(), TranslationCheck.problems(MainWindow.class, "org.datazuul.amadeos.audio.messages"));
 * </pre>
 */
public final class TranslationCheck {
    private TranslationCheck() {
    }

    /**
     * Per language the keys that are missing in its file (prefixed {@code missing: }), are not in the base
     * file ({@code unknown: }) or use other {@code {n}} arguments than English ({@code arguments: }).
     * Languages without problems are left out; a missing file counts every key as missing.
     */
    public static Map<Language, Set<String>> problems(Class<?> anchor, String baseName) {
        Properties base = read(anchor, baseName, null);
        if (base == null) throw new IllegalArgumentException("No base file for " + baseName);
        Map<Language, Set<String>> result = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            if (language == Language.ENGLISH) continue;
            Properties translation = read(anchor, baseName, language);
            if (translation == null) translation = new Properties();
            Set<String> problems = new TreeSet<>();
            for (String key : base.stringPropertyNames()) {
                String text = translation.getProperty(key);
                if (text == null) {
                    problems.add("missing: " + key);
                } else if (!arguments(text).equals(arguments(base.getProperty(key)))) {
                    problems.add("arguments: " + key);
                }
            }
            for (String key : translation.stringPropertyNames()) {
                if (!base.containsKey(key)) problems.add("unknown: " + key);
            }
            if (!problems.isEmpty()) result.put(language, problems);
        }
        return result;
    }

    /** The argument numbers used in a text, e.g. {@code [0, 1]}. */
    static Set<Integer> arguments(String text) {
        Set<Integer> numbers = new TreeSet<>();
        for (int i = text.indexOf('{'); i >= 0; i = text.indexOf('{', i + 1)) {
            int end = i + 1;
            while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
            if (end > i + 1 && end < text.length() && (text.charAt(end) == '}' || text.charAt(end) == ',')) {
                numbers.add(Integer.parseInt(text.substring(i + 1, end)));
            }
        }
        return numbers;
    }

    private static Properties read(Class<?> anchor, String baseName, Language language) {
        String path = baseName.replace('.', '/') + (language == null ? "" : "_" + language.code()) + ".properties";
        ClassLoader loader = anchor.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(path)) {
            if (in == null) return null;
            Properties properties = new Properties();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
