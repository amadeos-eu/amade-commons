package org.datazuul.amadeos.commons.app;

import com.google.inject.AbstractModule;
import org.datazuul.amadeos.commons.config.AppDirectories;
import org.datazuul.amadeos.commons.config.XdgDirectories;
import org.datazuul.amadeos.commons.desktop.DesktopLauncher;
import org.datazuul.amadeos.commons.desktop.PasswordStore;
import org.datazuul.amadeos.commons.desktop.SecretToolPasswordStore;
import org.datazuul.amadeos.commons.swing.EdtExecutor;
import org.datazuul.amadeos.commons.theme.ThemeStore;

import java.util.concurrent.ExecutorService;

/**
 * Bindings every amadeOS application gets: {@link AppInfo}, {@link AppDirectories}, the background
 * {@link ExecutorService} (virtual threads), {@link EdtExecutor}, {@link ThemeStore}, {@link DesktopLauncher}
 * and a {@link PasswordStore} in the keyring under the application id.
 */
public final class CommonsModule extends AbstractModule {
    private final AppInfo app;
    private final ExecutorService executor;
    private final ThemeStore themes;
    private final XdgDirectories xdg;

    public CommonsModule(AppInfo app, ExecutorService executor, ThemeStore themes, XdgDirectories xdg) {
        this.app = app;
        this.executor = executor;
        this.themes = themes;
        this.xdg = xdg;
    }

    @Override
    protected void configure() {
        bind(AppInfo.class).toInstance(app);
        bind(XdgDirectories.class).toInstance(xdg);
        bind(AppDirectories.class).toInstance(AppDirectories.of(xdg, app.configName()));
        bind(ExecutorService.class).toInstance(executor);
        bind(EdtExecutor.class).toInstance(new EdtExecutor());
        bind(ThemeStore.class).toInstance(themes);
        bind(DesktopLauncher.class).toInstance(new DesktopLauncher());
        bind(PasswordStore.class).toInstance(new SecretToolPasswordStore(app.id()));
    }
}
