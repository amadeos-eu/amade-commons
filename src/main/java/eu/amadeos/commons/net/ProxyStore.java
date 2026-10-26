package eu.amadeos.commons.net;

import eu.amadeos.commons.config.ConfigWatcher;
import eu.amadeos.commons.config.PropertiesFile;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.desktop.PasswordStore;
import eu.amadeos.commons.desktop.SecretToolPasswordStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The proxy of the desktop, from {@code ~/.config/amadeos/proxy.conf}; the password is in the keyring
 * ({@code secret-tool lookup service amadeos account proxy}). Settings &gt; Network writes it, programs read
 * it and, once {@link #watch()} was called, follow changes. Without the file the mode is
 * {@link ProxySettings.Mode#SYSTEM}.
 *
 * <pre>
 * mode=manual
 * http.host=proxy.example
 * http.port=3128
 * https.same=true
 * no.proxy=localhost, 127.0.0.1, .local
 * user=ralf
 * </pre>
 */
public final class ProxyStore implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyStore.class);
    public static final String PROXY_FILE = "proxy.conf";
    /** Keyring entry of the password: service {@value #KEYRING_SERVICE}, account {@value #KEYRING_ACCOUNT}. */
    public static final String KEYRING_SERVICE = "amadeos";
    public static final String KEYRING_ACCOUNT = "proxy";

    private final Path configDirectory;
    private final PasswordStore passwords;
    private final List<Consumer<ProxySettings>> listeners = new CopyOnWriteArrayList<>();
    private volatile ProxySettings current;
    private ConfigWatcher watcher;

    /** @param configDirectory {@code ~/.config/amadeos} */
    public ProxyStore(Path configDirectory, PasswordStore passwords) {
        this.configDirectory = configDirectory;
        this.passwords = passwords;
        this.current = read();
    }

    /** The store of the running user. */
    public static ProxyStore system() {
        return new ProxyStore(XdgDirectories.system().amadeosConfig(), new SecretToolPasswordStore(KEYRING_SERVICE));
    }

    public ProxySettings current() {
        return current;
    }

    public Path file() {
        return configDirectory.resolve(PROXY_FILE);
    }

    /** True if the desktop has a proxy setting at all (the file exists). */
    public boolean exists() {
        return Files.isRegularFile(file());
    }

    /** The proxy password from the keyring; empty if none. May block while the keyring answers. */
    public String password() {
        return passwords.lookup(KEYRING_ACCOUNT).orElse("");
    }

    /** Called with the new settings on a background thread. */
    public void addListener(Consumer<ProxySettings> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<ProxySettings> listener) {
        listeners.remove(listener);
    }

    // --8<-- [start:write]
    /**
     * Saves the settings and the password (deleted from the keyring when empty or when no user is set); for
     * Settings &gt; Network, programs do not call it. The password is written first, so programs that follow
     * the file find it.
     */
    public void write(ProxySettings settings, String password) {
        if (settings.user().isEmpty() || password == null || password.isEmpty()) passwords.delete(KEYRING_ACCOUNT);
        else passwords.store(KEYRING_ACCOUNT, "amadeOS proxy " + settings.host(), password);
        Properties p = new Properties();
        p.setProperty("mode", settings.mode().name().toLowerCase(Locale.ROOT));
        p.setProperty("http.host", settings.host());
        p.setProperty("http.port", String.valueOf(settings.port()));
        p.setProperty("https.same", String.valueOf(settings.httpsSame()));
        p.setProperty("https.host", settings.httpsHost());
        p.setProperty("https.port", String.valueOf(settings.httpsPort()));
        p.setProperty("user", settings.user());
        p.setProperty("no.proxy", settings.noProxy());
        PropertiesFile.store(file(), p, "amadeOS proxy (Settings > Network)");
        reload();
    }
    // --8<-- [end:write]

    /**
     * Reads the file again and notifies the listeners, even if the settings stayed the same: the password in
     * the keyring may have changed with it.
     */
    public synchronized void reload() {
        ProxySettings settings = read();
        current = settings;
        LOG.info("Desktop proxy: {}", settings.mode());
        for (Consumer<ProxySettings> listener : listeners) {
            try {
                listener.accept(settings);
            } catch (RuntimeException e) {
                LOG.warn("Proxy listener failed", e);
            }
        }
    }

    private ProxySettings read() {
        Properties p;
        try {
            p = PropertiesFile.load(file());
        } catch (UncheckedIOException e) {
            LOG.debug("Cannot read {}", file(), e);
            return ProxySettings.DEFAULT;
        }
        if (p.isEmpty()) return ProxySettings.DEFAULT;
        ProxySettings d = ProxySettings.DEFAULT;
        ProxySettings.Mode mode;
        try {
            mode = ProxySettings.Mode.valueOf(p.getProperty("mode", "system").strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            mode = ProxySettings.Mode.SYSTEM;
        }
        return new ProxySettings(mode, p.getProperty("http.host", ""), number(p, "http.port"),
                !"false".equalsIgnoreCase(p.getProperty("https.same", "true").strip()),
                p.getProperty("https.host", ""), number(p, "https.port"), p.getProperty("user", ""),
                p.getProperty("no.proxy", d.noProxy()));
    }

    private static int number(Properties p, String key) {
        try {
            return Integer.parseInt(p.getProperty(key, "").strip());
        } catch (NumberFormatException e) {
            return ProxySettings.DEFAULT_PORT;
        }
    }

    /** Starts following {@code proxy.conf} in a daemon thread. Calling it again has no effect. */
    public synchronized void watch() {
        if (watcher == null) {
            watcher = new ConfigWatcher(configDirectory, Set.of(PROXY_FILE), "amade-proxy-watcher", this::reload);
        }
        watcher.start();
    }

    @Override
    public synchronized void close() {
        if (watcher != null) watcher.close();
        watcher = null;
    }
}
