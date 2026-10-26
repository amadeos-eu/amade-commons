package eu.amadeos.commons.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Decides for every connection whether it goes through a proxy, from the {@link ProxySettings} of the desktop
 * and, in mode {@link ProxySettings.Mode#SYSTEM}, the {@code http_proxy} variables. {@link Network} installs one
 * as the JVM's default {@link ProxySelector} and {@link Authenticator}, so {@code HttpURLConnection} and every
 * {@link HttpClient} without a proxy of its own use it; {@link #applyTo(Map)} hands the same proxy to programs
 * started from Java.
 */
public final class NetworkProxy extends ProxySelector {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkProxy.class);
    /** The variables {@link #applyTo(Map)} sets or removes, lower case; upper case is handled too. */
    static final List<String> VARIABLES = List.of("http_proxy", "https_proxy", "all_proxy", "no_proxy");

    /** A proxy from the settings or the environment. */
    public record Endpoint(String host, int port, String user, String password) {
        /** {@code http://user:password@host:port}, percent-encoded, as programs expect in {@code http_proxy}. */
        public String url() {
            String credentials = user.isEmpty() ? "" : encode(user) + (password.isEmpty() ? "" : ":" + encode(password)) + "@";
            String h = host.contains(":") && !host.startsWith("[") ? "[" + host + "]" : host;
            return "http://" + credentials + h + ":" + port;
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }

        private static String encode(String s) {
            return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
        }
    }

    private final Map<String, String> environment;
    private final ProxySelector fallback;
    private final Authenticator authenticator = new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            if (getRequestorType() != RequestorType.PROXY) return null;
            Optional<Endpoint> e = endpoint(getRequestingURL() != null && "https".equals(getRequestingURL().getProtocol()));
            if (e.isEmpty() || e.get().user().isEmpty()) return null;
            return new PasswordAuthentication(e.get().user(), e.get().password().toCharArray());
        }
    };
    private volatile ProxySettings settings = ProxySettings.DEFAULT;
    private volatile String password = "";

    /**
     * @param environment the variables of this program, read in mode {@link ProxySettings.Mode#SYSTEM}
     * @param fallback    asked in mode SYSTEM when the environment names no proxy (the operating system's own
     *                    setting on Windows); null for none
     */
    public NetworkProxy(Map<String, String> environment, ProxySelector fallback) {
        this.environment = Map.copyOf(environment);
        this.fallback = fallback;
    }

    public NetworkProxy(Map<String, String> environment) {
        this(environment, null);
    }

    public void configure(ProxySettings newSettings, String newPassword) {
        settings = newSettings == null ? ProxySettings.DEFAULT : newSettings;
        password = newPassword == null ? "" : newPassword;
        LOG.info("Network proxy ({}): {}", settings.mode(), describe().orElse("direct"));
    }

    public ProxySettings settings() {
        return settings;
    }

    public Authenticator authenticator() {
        return authenticator;
    }

    // --8<-- [start:http-client]
    /**
     * An HTTP client builder with this proxy and its password. A plain {@code HttpClient.newBuilder()} finds the
     * proxy too (it is the JVM default) but not the password, which {@code HttpClient} only takes from its own
     * {@link Authenticator}.
     */
    public HttpClient.Builder httpClient() {
        return HttpClient.newBuilder().proxy(this).authenticator(authenticator);
    }
    // --8<-- [end:http-client]

    /** E.g. {@code proxy.example:3128}, or empty for a direct connection; for status lines. */
    public Optional<String> describe() {
        return endpoint(true).map(Endpoint::toString);
    }

    // --8<-- [start:select]
    @Override
    public List<Proxy> select(URI uri) {
        if (uri == null || uri.getHost() == null) return List.of(Proxy.NO_PROXY);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.startsWith("http") && !scheme.equals("socket")) return List.of(Proxy.NO_PROXY);
        if (bypass(uri.getHost())) return List.of(Proxy.NO_PROXY);
        Optional<Endpoint> e = endpoint(scheme.equals("https") || scheme.equals("socket"));
        if (e.isEmpty() && settings.mode() == ProxySettings.Mode.SYSTEM && fallback != null) return fallback.select(uri);
        return e.<List<Proxy>>map(p -> List.of(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(p.host(), p.port()))))
                .orElse(List.of(Proxy.NO_PROXY));
    }
    // --8<-- [end:select]

    @Override
    public void connectFailed(URI uri, SocketAddress address, IOException e) {
        LOG.warn("Cannot connect to the proxy {} for {}: {}", address, uri.getHost(), e.toString());
    }

    /** The proxy for HTTPS (or plain HTTP) connections, if any. */
    public Optional<Endpoint> endpoint(boolean https) {
        ProxySettings s = settings;
        return switch (s.mode()) {
            case NONE -> Optional.empty();
            case MANUAL -> {
                String host = https ? s.effectiveHttpsHost() : s.host();
                int port = https ? s.effectiveHttpsPort() : s.port();
                yield host.isEmpty() ? Optional.empty() : Optional.of(new Endpoint(host, port, s.user(), password));
            }
            case SYSTEM -> fromEnvironment(https);
        };
    }

    /**
     * True if the host is reached directly: loopback, an address of the local network (a printer, a NAS), or
     * listed in the no-proxy list.
     */
    public boolean bypass(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        if (h.equals("localhost") || h.startsWith("127.") || h.equals("::1") || h.equals("[::1]")) return true;
        if (isLocalAddress(h)) return true;
        List<String> rules = settings.mode() == ProxySettings.Mode.SYSTEM
                ? ProxySettings.split(env("no_proxy")) : settings.noProxyHosts();
        for (String rule : rules) {
            String r = rule.toLowerCase(Locale.ROOT);
            if (r.equals("*")) return true;
            if (r.startsWith("*.")) r = r.substring(1);
            if (r.startsWith(".") ? h.endsWith(r) || h.equals(r.substring(1)) : h.equals(r) || h.endsWith("." + r)) return true;
        }
        return false;
    }

    /** An IP address of a private or link-local network, written as such (no name lookup). */
    static boolean isLocalAddress(String host) {
        String h = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        if (h.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            String[] parts = h.split("\\.");
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1]);
            return a == 10 || (a == 172 && b >= 16 && b <= 31) || (a == 192 && b == 168) || (a == 169 && b == 254);
        }
        return h.contains(":") && (h.startsWith("fc") || h.startsWith("fd") || h.startsWith("fe8") || h.startsWith("fe9")
                || h.startsWith("fea") || h.startsWith("feb"));
    }

    // --8<-- [start:apply-to]
    /**
     * Puts this proxy into the environment of a program about to start ({@code ProcessBuilder.environment()}):
     * in mode MANUAL {@code http_proxy}, {@code https_proxy} and {@code no_proxy} in lower and upper case, with
     * user and password in the URL; in mode NONE it removes them; in mode SYSTEM the program keeps the variables
     * this one has.
     */
    public void applyTo(Map<String, String> target) {
        ProxySettings s = settings;
        if (s.mode() == ProxySettings.Mode.SYSTEM) return;
        for (String name : VARIABLES) {
            target.remove(name);
            target.remove(name.toUpperCase(Locale.ROOT));
        }
        if (s.mode() == ProxySettings.Mode.NONE) return;
        Optional<Endpoint> http = endpoint(false);
        Optional<Endpoint> https = endpoint(true);
        if (http.isEmpty()) return;
        put(target, "http_proxy", http.get().url());
        https.ifPresent(e -> put(target, "https_proxy", e.url()));
        put(target, "no_proxy", String.join(",", s.noProxyHosts()));
    }
    // --8<-- [end:apply-to]

    private static void put(Map<String, String> target, String name, String value) {
        target.put(name, value);
        target.put(name.toUpperCase(Locale.ROOT), value);
    }

    private Optional<Endpoint> fromEnvironment(boolean https) {
        String value = https ? env("https_proxy") : env("http_proxy");
        if (value.isEmpty()) value = env("all_proxy");
        if (value.isEmpty() && https) value = env("http_proxy");
        return parse(value);
    }

    /** {@code http://user:pass@host:port/}, {@code host:port} or {@code host}. */
    public static Optional<Endpoint> parse(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        String v = value.strip();
        if (!v.contains("://")) v = "http://" + v;
        try {
            URI uri = URI.create(v);
            if (uri.getHost() == null) return Optional.empty();
            String user = "";
            String pass = "";
            if (uri.getRawUserInfo() != null) {
                String[] up = uri.getRawUserInfo().split(":", 2);
                user = URLDecoder.decode(up[0], StandardCharsets.UTF_8);
                pass = up.length > 1 ? URLDecoder.decode(up[1], StandardCharsets.UTF_8) : "";
            }
            int port = uri.getPort() > 0 ? uri.getPort() : "https".equals(uri.getScheme()) ? 443 : 80;
            return Optional.of(new Endpoint(uri.getHost(), port, user, pass));
        } catch (IllegalArgumentException e) {
            LOG.warn("Ignoring invalid proxy setting {}", value);
            return Optional.empty();
        }
    }

    private String env(String name) {
        String v = environment.get(name);
        if (v == null || v.isBlank()) v = environment.get(name.toUpperCase(Locale.ROOT));
        return v == null ? "" : v.strip();
    }
}
