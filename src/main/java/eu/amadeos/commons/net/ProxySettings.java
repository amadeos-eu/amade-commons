package eu.amadeos.commons.net;

import java.util.Arrays;
import java.util.List;

/**
 * How amadeOS programs reach the internet, as chosen in Settings &gt; Network. The proxy password is not part
 * of the settings; it lives in the keyring ({@link ProxyStore#password()}).
 *
 * @param host      manual proxy host for HTTP, and for HTTPS too if {@code httpsSame}
 * @param httpsSame true if HTTPS goes through {@code host:port} as well
 * @param httpsHost manual proxy host for HTTPS when {@code httpsSame} is false
 * @param user      user name at the proxy; empty if it needs none
 * @param noProxy   comma-separated hosts or domains reached directly, e.g. {@code localhost, .intranet.example}
 */
public record ProxySettings(Mode mode, String host, int port, boolean httpsSame, String httpsHost, int httpsPort,
                            String user, String noProxy) {
    public enum Mode {
        /** From the environment ({@code http_proxy}, {@code https_proxy}, {@code no_proxy}), else the system's. */
        SYSTEM,
        /** Always connect directly. */
        NONE,
        /** The proxy entered in the settings. */
        MANUAL
    }

    public static final int DEFAULT_PORT = 3128;
    public static final String DEFAULT_NO_PROXY = "localhost, 127.0.0.1, .local";
    public static final ProxySettings DEFAULT = new ProxySettings(Mode.SYSTEM, "", DEFAULT_PORT, true, "",
            DEFAULT_PORT, "", DEFAULT_NO_PROXY);

    public ProxySettings {
        mode = mode == null ? Mode.SYSTEM : mode;
        host = host == null ? "" : host.strip();
        port = validPort(port);
        httpsHost = httpsHost == null ? "" : httpsHost.strip();
        httpsPort = validPort(httpsPort);
        user = user == null ? "" : user.strip();
        noProxy = noProxy == null ? "" : noProxy.strip();
    }

    /** One proxy for HTTP and HTTPS. */
    public static ProxySettings manual(String host, int port, String user, String noProxy) {
        return new ProxySettings(Mode.MANUAL, host, port, true, "", port, user, noProxy);
    }

    public ProxySettings withMode(Mode value) {
        return new ProxySettings(value, host, port, httpsSame, httpsHost, httpsPort, user, noProxy);
    }

    /** The host for HTTPS connections: {@link #host()} unless a separate one is set. */
    public String effectiveHttpsHost() {
        return httpsSame || httpsHost.isEmpty() ? host : httpsHost;
    }

    public int effectiveHttpsPort() {
        return httpsSame || httpsHost.isEmpty() ? port : httpsPort;
    }

    public List<String> noProxyHosts() {
        return split(noProxy);
    }

    static List<String> split(String list) {
        return Arrays.stream(list.split("[,\\s]+")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    private static int validPort(int port) {
        return port <= 0 || port > 65535 ? DEFAULT_PORT : port;
    }
}
