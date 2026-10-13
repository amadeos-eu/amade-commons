package org.datazuul.amadeos.commons.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.datazuul.amadeos.commons.config.XdgDirectories;
import org.datazuul.amadeos.commons.swing.AmadeUI;
import org.datazuul.amadeos.commons.theme.ThemeStore;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Starts an amadeOS application the same way everywhere: on the EDT, with the desktop theme installed
 * and followed, a virtual-thread executor for background work and a Guice injector with the
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
        SwingUtilities.invokeLater(() -> {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            try {
                ThemeStore themes = ThemeStore.system();
                AmadeUI.install(themes.current());
                AmadeUI.follow(themes);
                List<Module> all = new ArrayList<>();
                all.add(new CommonsModule(app, executor, themes, XdgDirectories.system()));
                all.addAll(List.of(modules));
                start.accept(Guice.createInjector(all));
            } catch (RuntimeException e) {
                LoggerFactory.getLogger(AmadeApplication.class).error("{} failed to start", app.id(), e);
                JOptionPane.showMessageDialog(null, app.title() + " could not start:\n" + e,
                        app.title(), JOptionPane.ERROR_MESSAGE);
                executor.shutdownNow();
            }
        });
    }
}
