package eu.amadeos.commons.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class I18nTest {
    @TempDir
    Path dir;

    @Test
    void parsesPosixAndBcp47Locales() {
        assertEquals(Optional.of(Locale.of("de", "AT")), Language.parseLocale("de_AT.UTF-8"));
        assertEquals(Optional.of(Locale.of("de", "AT")), Language.parseLocale("de_AT.UTF-8@euro"));
        assertEquals(Optional.of(Locale.of("fr", "CH")), Language.parseLocale("fr-CH"));
        assertEquals(Optional.of(Locale.of("it", "IT")), Language.parseLocale(" it \n"));
        assertEquals(Optional.of(Locale.of("es", "419")), Language.parseLocale("es_419"));
        assertEquals(Optional.empty(), Language.parseLocale("C.UTF-8"));
        assertEquals(Optional.empty(), Language.parseLocale("POSIX"));
        assertEquals(Optional.empty(), Language.parseLocale("pt_BR.UTF-8"), "not translated");
        assertEquals(Optional.empty(), Language.parseLocale("de_Deutschland"));
        assertEquals(Optional.empty(), Language.parseLocale(""));
        assertEquals(Optional.empty(), Language.parseLocale(null));
        assertEquals("fr_FR.UTF-8", Language.FRENCH.posixLocale());
        assertEquals(Optional.of(Language.SPANISH), Language.ofCode("ES"));
    }

    @Test
    void applicationSettingWinsThenDesktopThenEnvironmentThenEnglish() {
        Map<String, String> env = Map.of("LANG", "it_IT.UTF-8");
        Optional<Locale> desktop = Optional.of(Locale.of("de", "DE"));

        LocaleResolution app = LocaleResolution.resolve(Optional.of(Language.FRENCH), desktop, env);
        assertEquals(Language.FRENCH, app.language());
        assertEquals(LocaleResolution.Source.APPLICATION, app.source());
        assertEquals(Locale.of("fr", "FR"), app.locale());

        LocaleResolution fromDesktop = LocaleResolution.resolve(Optional.empty(), desktop, env);
        assertEquals(Language.GERMAN, fromDesktop.language());
        assertEquals(LocaleResolution.Source.DESKTOP, fromDesktop.source());

        LocaleResolution fromEnv = LocaleResolution.resolve(Optional.empty(), Optional.empty(), env);
        assertEquals(Locale.of("it", "IT"), fromEnv.locale());
        assertEquals(LocaleResolution.Source.ENVIRONMENT, fromEnv.source());

        LocaleResolution fallback = LocaleResolution.resolve(Optional.empty(), Optional.empty(),
                Map.of("LANG", "pt_BR.UTF-8"));
        assertEquals(Language.ENGLISH, fallback.language());
        assertEquals(LocaleResolution.Source.DEFAULT, fallback.source());
    }

    @Test
    void applicationSettingKeepsTheRegionOfTheSameLanguage() {
        LocaleResolution r = LocaleResolution.resolve(Optional.of(Language.GERMAN), Optional.empty(),
                Map.of("LANG", "de_CH.UTF-8"));
        assertEquals(Locale.of("de", "CH"), r.locale());
    }

    @Test
    void environmentFollowsGettextRules() {
        assertEquals(Optional.of(Locale.of("es", "ES")), LocaleResolution.fromEnvironment(
                Map.of("LANGUAGE", "pt:es", "LANG", "de_DE.UTF-8")), "first translated entry of LANGUAGE");
        assertEquals(Optional.empty(), LocaleResolution.fromEnvironment(
                Map.of("LANGUAGE", "fr", "LANG", "C.UTF-8")), "LANGUAGE is ignored for the C locale");
        assertEquals(Optional.of(Locale.of("fr", "FR")), LocaleResolution.fromEnvironment(
                Map.of("LC_ALL", "fr_FR.UTF-8", "LC_MESSAGES", "it_IT.UTF-8", "LANG", "de_DE.UTF-8")));
        assertEquals(Optional.of(Locale.of("it", "IT")), LocaleResolution.fromEnvironment(
                Map.of("LC_MESSAGES", "it_IT.UTF-8", "LANG", "de_DE.UTF-8")));
        assertEquals(Optional.empty(), LocaleResolution.fromEnvironment(Map.of()));
    }

    @Test
    void localeStoreReadsWritesAndNotifies() throws Exception {
        LocaleStore store = new LocaleStore(dir);
        assertEquals(Optional.empty(), store.current());
        List<Optional<Locale>> heard = new ArrayList<>();
        store.addListener(heard::add);

        Files.writeString(dir.resolve(LocaleStore.LOCALE_FILE), "fr_CA.UTF-8\n", StandardCharsets.UTF_8);
        assertTrue(store.reload());
        assertEquals(Optional.of(Locale.of("fr", "CA")), store.current());
        assertFalse(store.reload(), "unchanged");

        store.write(Language.ITALIAN);
        assertEquals("it_IT.UTF-8", Files.readString(store.file()).strip());
        assertEquals(Optional.of(Language.ITALIAN), store.language());
        assertEquals(List.of(Optional.of(Locale.of("fr", "CA")), Optional.of(Locale.of("it", "IT"))), heard);
        try (var files = Files.list(dir)) {
            assertEquals(1, files.count(), "no temporary files left");
        }
    }

    @Test
    void localeStoreFollowsTheFile() throws Exception {
        try (LocaleStore store = new LocaleStore(dir)) {
            List<Optional<Locale>> heard = new java.util.concurrent.CopyOnWriteArrayList<>();
            store.addListener(heard::add);
            store.watch();
            Files.writeString(dir.resolve(LocaleStore.LOCALE_FILE), "es", StandardCharsets.UTF_8);
            long until = System.currentTimeMillis() + 10_000;
            while (heard.isEmpty() && System.currentTimeMillis() < until) Thread.sleep(50);
            assertEquals(List.of(Optional.of(Locale.of("es", "ES"))), heard);
        }
    }

    @Test
    void localeSettingsStoreTheApplicationLanguage() throws Exception {
        Path config = dir.resolve("amadeos");
        Files.createDirectories(config);
        Files.writeString(config.resolve(LocaleStore.LOCALE_FILE), "de_DE.UTF-8");
        LocaleSettings settings = new LocaleSettings(new LocaleStore(config), config.resolve("audio"),
                Map.of("LANG", "en_GB.UTF-8"));
        assertEquals(Language.GERMAN, settings.applied().language());
        assertEquals(Optional.empty(), settings.applicationLanguage());
        assertFalse(settings.restartRequired());

        settings.setApplicationLanguage(Optional.of(Language.SPANISH));
        assertEquals("es", Files.readString(config.resolve("audio").resolve(LocaleSettings.LANGUAGE_FILE)).strip());
        assertEquals(Optional.of(Language.SPANISH), settings.applicationLanguage());
        assertTrue(settings.restartRequired());
        assertEquals(Language.GERMAN, settings.desktopResolution().language());

        settings.setApplicationLanguage(Optional.empty());
        assertEquals("desktop", Files.readString(config.resolve("audio").resolve(LocaleSettings.LANGUAGE_FILE)).strip());
        assertFalse(settings.restartRequired());
    }

    @Test
    void messagesFallBackToEnglishNotToTheDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMAN);
            Messages portuguese = Messages.commons(Locale.of("pt", "BR"));
            assertEquals("Desktop setting (Deutsch)", portuguese.format("language.follow-desktop", "Deutsch"));
        } finally {
            Locale.setDefault(previous);
        }
        Messages german = Messages.commons(Locale.of("de", "AT"));
        assertEquals("Desktop-Einstellung (Français)", german.format("language.follow-desktop", "Français"));
        assertEquals("!no.such.key!", german.get("no.such.key"));
        assertFalse(german.has("no.such.key"));
    }

    @Test
    void apostrophesArePlainAndArgumentsAreLocalised() {
        Messages french = Messages.commons(Locale.FRANCE);
        assertEquals("Impossible de démarrer Lecteur :\nboom", french.format("app.start-failed", "Lecteur", "boom"));
        Messages italian = Messages.commons(Locale.ITALY);
        assertEquals("La nuova lingua sarà usata al prossimo avvio di Audio.",
                italian.format("language.restart", "Audio"));
    }

    @Test
    void everyCommonsTranslationIsComplete() {
        assertEquals(Map.of(), TranslationCheck.problems(Messages.class, "eu.amadeos.commons.i18n.commons"));
    }

    @Test
    void translationCheckFindsArguments() {
        assertEquals(Set.of(0, 1), TranslationCheck.arguments("{0} could not start: {1,number}"));
        assertEquals(Set.of(), TranslationCheck.arguments("no {arguments} here"));
    }

    @Test
    void mnemonicsComeFromTheText() {
        Mnemonics.Text t = Mnemonics.parse("&Datei");
        assertEquals("Datei", t.text());
        assertEquals(0, t.index());
        assertEquals(KeyEvent.VK_D, t.keyCode());
        assertEquals(new Mnemonics.Text("Rock && Roll", -1).text(), Mnemonics.parse("Rock &&&& Roll").text());
        assertEquals("Save &As", Mnemonics.parse("Save &&As").text());
        assertEquals(-1, Mnemonics.parse("Save &&As").index());
        assertEquals(5, Mnemonics.parse("Save &As").index());
        assertEquals("Fichier", Mnemonics.strip("&Fichier"));
        assertEquals(KeyEvent.VK_UNDEFINED, Mnemonics.parse("None").keyCode());
    }

    @Test
    void chooserOffersTheDesktopLanguageFirst() throws Exception {
        Path config = dir.resolve("amadeos");
        Files.createDirectories(config);
        Files.writeString(config.resolve(LocaleStore.LOCALE_FILE), "fr_FR.UTF-8");
        LocaleSettings settings = new LocaleSettings(new LocaleStore(config), config.resolve("pad"), Map.of());
        settings.setApplicationLanguage(Optional.of(Language.ITALIAN));
        LanguageChooser chooser = new LanguageChooser(settings, Messages.commons(Locale.GERMANY));
        assertEquals(1 + Language.values().length, chooser.getItemCount());
        assertEquals("Desktop-Einstellung (Français)", chooser.getItemAt(0).label());
        assertEquals(Optional.of(Language.ITALIAN), chooser.selectedLanguage());
        chooser.setSelectedLanguage(Optional.empty());
        assertEquals(Optional.empty(), chooser.selectedLanguage());
    }
}
