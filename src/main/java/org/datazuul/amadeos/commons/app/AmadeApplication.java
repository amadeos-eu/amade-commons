package org.datazuul.amadeos.commons.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.datazuul.amadeos.commons.config.XdgDirectories;
import org.datazuul.amadeos.commons.i18n.LocaleSettings;
import org.datazuul.amadeos.commons.i18n.LocaleStore;
import org.datazuul.amadeos.commons.i18n.Messages;
import org.datazuul.amadeos.commons.swing.AmadeUI;
import org.datazuul.amadeos.commons.theme.ThemeStore;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Starts an amadeOS application the same way everywhere: in the language of the desktop (or the
 * application's own language setting), on the EDT, with the desktop theme installed and followed, a
 * virtual-thread executor for background work and a Guice injector with the
 * {@link CommonsModule} plus the application's own modules.
 *
 * <pre>
 * public static void main(String[] args) {
 *     AmadeApplication.launch(new AppInfo("amade-mail", "Mailer", "mail"),
 *             injector -> injector.getInstance(MainPresenter.class).start(List.of(args)),
 *             new MailModule());
 * }
 * </pre>
 */
public final class AmadeApplication {
    private AmadeApplication() {
    }

    public static void launch(AppInfo app, Consumer<Injector> start, Module... modules) {
        // Before any Swing class is used, so that the JDK's own texts (OK, Cancel, file chooser) and every
        // date and number format follow the language as well.
        XdgDirectories xdg = XdgDirectories.system();
        LocaleSettings locales = new LocaleSettings(new LocaleStore(xdg.amadeosConfig()),
                xdg.appConfig(app.configName()), System.getenv());
        Locale locale = locales.locale();
        Locale.setDefault(locale);
        LoggerFactory.getLogger(AmadeApplication.class).info("{} runs in {} (from {})", app.id(),
                locale.toLanguageTag(), locales.applied().source());
        SwingUtilities.invokeLater(() -> {
            JComponent.setDefaultLocale(locale);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            try {
                ThemeStore themes = ThemeStore.system();
                AmadeUI.install(themes.current());
                AmadeUI.follow(themes);
                List<Module> all = new ArrayList<>();
                all.add(new CommonsModule(app, executor, themes, xdg, locales));
                all.addAll(List.of(modules));
                start.accept(Guice.createInjector(all));
            } catch (RuntimeException e) {
                LoggerFactory.getLogger(AmadeApplication.class).error("{} failed to start", app.id(), e);
                JOptionPane.showMessageDialog(null, Messages.commons(locale).format("app.start-failed", app.title(), e),
                        app.title(), JOptionPane.ERROR_MESSAGE);
                executor.shutdownNow();
            }
        });
    }
}
