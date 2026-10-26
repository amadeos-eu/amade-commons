package eu.amadeos.commons.net;

import org.slf4j.LoggerFactory;

import java.net.Authenticator;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * The network settings of the running program: the proxy of the desktop (Settings &gt; Network), followed while
 * the program runs. {@code AmadeApplication} calls {@link #install()} before the first connection; programs
 * that start otherwise (desktop modules) get it on first use of {@link #proxy()}.
 *
 * <pre>
 * HttpClient client = Network.httpClient().build();                  // proxy, password, 10 s to connect
 * ProcessBuilder pb = new ProcessBuilder("yt-dlp", url);
 * Network.proxy().applyTo(pb.environment());                          // CommandRunner.system() does this
 * </pre>
 *
 * <p>Outside amadeOS (the Windows builds) there is no {@code proxy.conf}; the proxy then comes from the
 * {@code http_proxy} variables or, when they are not set, from the operating system.
 */
public final class Network {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static volatile NetworkProxy proxy;
    private static volatile ProxyStore store;

    private Network() {
    }

    // --8<-- [start:install]
    /**
     * Installs the desktop's proxy as the JVM default and starts following {@code proxy.conf}. Allows Basic
     * authentication at proxies, which the JDK switches off for HTTPS tunnels by default. Calling it again has
     * no effect.
     */
    public static synchronized NetworkProxy install() {
        if (proxy != null) return proxy;
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        ProxySelector fallback = null;
        if (!System.getProperty("os.name", "").startsWith("Linux")) {
            // Read once, when the JDK's selector is first created: on Windows and macOS it asks the system.
            System.setProperty("java.net.useSystemProxies", "true");
            fallback = ProxySelector.getDefault();
        }
        NetworkProxy p = new NetworkProxy(System.getenv(), fallback);
        ProxyStore s = ProxyStore.system();
        p.configure(s.current(), "");
        ProxySelector.setDefault(p);
        Authenticator.setDefault(p.authenticator());
        s.addListener(settings -> p.configure(settings, s.password()));
        s.watch();
        if (!s.current().user().isEmpty()) {
            // The keyring may take a moment (or ask to be unlocked); connections without a password meanwhile.
            Thread.ofVirtual().name("amade-proxy-password").start(() -> p.configure(s.current(), s.password()));
        }
        store = s;
        proxy = p;
        LoggerFactory.getLogger(Network.class).debug("Network proxy installed from {}", s.file());
        return p;
    }
    // --8<-- [end:install]

    /** The proxy of this program, installed on first use. */
    public static NetworkProxy proxy() {
        NetworkProxy p = proxy;
        return p != null ? p : install();
    }

    /** The desktop's proxy settings; Settings &gt; Network writes through it. */
    public static ProxyStore store() {
        proxy();
        return store;
    }

    /** An HTTP client builder with the proxy, its password, a 10 second connect limit and redirects followed. */
    public static HttpClient.Builder httpClient() {
        return proxy().httpClient().connectTimeout(CONNECT_TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL);
    }
}
