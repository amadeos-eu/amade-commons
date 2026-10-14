package org.datazuul.amadeos.commons.app;

import com.google.inject.AbstractModule;
import org.datazuul.amadeos.commons.config.AppDirectories;
import org.datazuul.amadeos.commons.config.XdgDirectories;
import org.datazuul.amadeos.commons.desktop.DesktopLauncher;
import org.datazuul.amadeos.commons.desktop.PasswordStore;
import org.datazuul.amadeos.commons.desktop.SecretToolPasswordStore;
import org.datazuul.amadeos.commons.i18n.LocaleSettings;
import org.datazuul.amadeos.commons.i18n.LocaleStore;
import org.datazuul.amadeos.commons.swing.EdtExecutor;
import org.datazuul.amadeos.commons.theme.ThemeStore;

import java.util.Locale;
import java.util.concurrent.ExecutorService;

/**
 * Bindings every amadeOS application gets: {@link AppInfo}, {@link AppDirectories}, the background
 * {@link ExecutorService} (virtual threads), {@link EdtExecutor}, {@link ThemeStore}, {@link DesktopLauncher}
 * a {@link PasswordStore} in the keyring under the application id, and the language: {@link LocaleSettings}
 * and the {@link Locale} the application runs in.
 */
public final class CommonsModule extends AbstractModule {
    private final AppInfo app;
    private final ExecutorService executor;
    private final ThemeStore themes;
    private final XdgDirectories xdg;
    private final LocaleSettings locales;

    public CommonsModule(AppInfo app, ExecutorService executor, ThemeStore themes, XdgDirectories xdg,
                         LocaleSettings locales) {
        this.app = app;
        this.executor = executor;
        this.themes = themes;
        this.xdg = xdg;
        this.locales = locales;
    }

    /** With the language settings of the running user. */
    public CommonsModule(AppInfo app, ExecutorService executor, ThemeStore themes, XdgDirectories xdg) {
        this(app, executor, themes, xdg, new LocaleSettings(new LocaleStore(xdg.amadeosConfig()),
                xdg.appConfig(app.configName()), System.getenv()));
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
        bind(LocaleSettings.class).toInstance(locales);
        bind(LocaleStore.class).toInstance(locales.desktop());
        bind(Locale.class).toInstance(locales.locale());
    }
}
