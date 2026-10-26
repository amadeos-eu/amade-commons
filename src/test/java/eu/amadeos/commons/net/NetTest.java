package eu.amadeos.commons.net;

import eu.amadeos.commons.desktop.SessionPasswordStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetTest {
    private static final URI STREAM = URI.create("http://stream.example/radio");
    private static final URI API = URI.create("https://api.example/json");

    private static String target(List<Proxy> proxies) {
        Proxy p = proxies.get(0);
        if (p.type() == Proxy.Type.DIRECT) return "DIRECT";
        return p.address().toString().replace("/<unresolved>", "");
    }

    @Test
    void systemModeReadsTheEnvironment() {
        NetworkProxy proxy = new NetworkProxy(Map.of(
                "http_proxy", "http://plain.example:8080",
                "HTTPS_PROXY", "http://user:p%40ss@secure.example:3128/",
                "no_proxy", "localhost,.intranet.example, music.lan"));
        proxy.configure(ProxySettings.DEFAULT, "");
        assertEquals("plain.example:8080", target(proxy.select(STREAM)));
        assertEquals("secure.example:3128", target(proxy.select(API)));
        assertEquals("DIRECT", target(proxy.select(URI.create("http://radio.intranet.example/x"))));
        assertEquals("DIRECT", target(proxy.select(URI.create("http://intranet.example/x"))));
        assertEquals("DIRECT", target(proxy.select(URI.create("http://nas.music.lan/x"))));
        assertEquals("DIRECT", target(proxy.select(URI.create("http://127.0.0.1:8000/x"))));
        assertEquals("DIRECT", target(proxy.select(URI.create("http://192.168.1.20:631/ipp/print"))));
        assertEquals("DIRECT", target(proxy.select(URI.create("http://[fe80::1]/x"))));
        assertEquals("plain.example:8080", target(proxy.select(URI.create("http://8.8.8.8/x"))));
        assertEquals("plain.example:8080", target(proxy.select(URI.create("http://172.32.0.1/x"))));
        assertEquals("p@ss", proxy.endpoint(true).orElseThrow().password());
        assertEquals("secure.example:3128", proxy.describe().orElseThrow());
    }

    @Test
    void httpsFallsBackToHttpProxyAndToTheSystem() {
        NetworkProxy proxy = new NetworkProxy(Map.of("HTTP_PROXY", "proxy.corp:3128"));
        proxy.configure(ProxySettings.DEFAULT, "");
        assertEquals("proxy.corp:3128", target(proxy.select(API)));

        NetworkProxy none = new NetworkProxy(Map.of());
        none.configure(ProxySettings.DEFAULT, "");
        assertEquals("DIRECT", target(none.select(API)));
        assertTrue(none.describe().isEmpty());

        List<URI> asked = new ArrayList<>();
        NetworkProxy windows = new NetworkProxy(Map.of(), new java.net.ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                asked.add(uri);
                return List.of(new Proxy(Proxy.Type.HTTP, java.net.InetSocketAddress.createUnresolved("win.example", 8080)));
            }

            @Override
            public void connectFailed(URI uri, java.net.SocketAddress sa, java.io.IOException ioe) {
            }
        });
        windows.configure(ProxySettings.DEFAULT, "");
        assertEquals("win.example:8080", target(windows.select(API)));
        windows.configure(ProxySettings.DEFAULT.withMode(ProxySettings.Mode.NONE), "");
        assertEquals("DIRECT", target(windows.select(API)));
        assertEquals(1, asked.size());
    }

    @Test
    void manualModeIgnoresTheEnvironment() {
        NetworkProxy proxy = new NetworkProxy(Map.of("https_proxy", "http://env.example:1"));
        proxy.configure(ProxySettings.manual("proxy.kvb.example", 8080, "ralf", "*.local, nas"), "secret");
        assertEquals("proxy.kvb.example:8080", target(proxy.select(STREAM)));
        assertEquals("proxy.kvb.example:8080", target(proxy.select(API)));
        assertEquals("DIRECT", target(proxy.select(URI.create("http://box.local/stream"))));
        assertEquals("DIRECT", target(proxy.select(URI.create("http://nas/stream"))));
        assertEquals("secret", proxy.endpoint(false).orElseThrow().password());

        proxy.configure(new ProxySettings(ProxySettings.Mode.MANUAL, "web.example", 8080, false, "tls.example", 8443,
                "", ""), "");
        assertEquals("web.example:8080", target(proxy.select(STREAM)));
        assertEquals("tls.example:8443", target(proxy.select(API)));

        proxy.configure(ProxySettings.manual("", 8080, "", ""), "");
        assertEquals("DIRECT", target(proxy.select(API)), "manual without a host means direct");
        proxy.configure(ProxySettings.manual("x", 1, "", "").withMode(ProxySettings.Mode.NONE), "");
        assertEquals("DIRECT", target(proxy.select(API)));
    }

    @Test
    void parsesProxyValues() {
        assertEquals("h:80", NetworkProxy.parse("h").orElseThrow().toString());
        assertEquals("h:3128", NetworkProxy.parse(" http://h:3128 ").orElseThrow().toString());
        assertTrue(NetworkProxy.parse("").isEmpty());
        assertTrue(NetworkProxy.parse("http://").isEmpty());
    }

    // --8<-- [start:apply]
    @Test
    void programsGetTheProxyInTheirEnvironment() {
        NetworkProxy proxy = new NetworkProxy(Map.of());
        Map<String, String> env = new HashMap<>(Map.of("HTTP_PROXY", "http://old:1", "PATH", "/usr/bin"));

        proxy.configure(ProxySettings.DEFAULT, "");
        proxy.applyTo(env);
        assertEquals("http://old:1", env.get("HTTP_PROXY"), "system mode keeps what the program has");

        proxy.configure(ProxySettings.manual("proxy.example", 3128, "anna maria", "localhost, .lan"), "p@ss:w");
        proxy.applyTo(env);
        assertEquals("http://anna%20maria:p%40ss%3Aw@proxy.example:3128", env.get("http_proxy"));
        assertEquals(env.get("http_proxy"), env.get("HTTP_PROXY"));
        assertEquals(env.get("http_proxy"), env.get("https_proxy"));
        assertEquals("localhost,.lan", env.get("NO_PROXY"));
        assertEquals("/usr/bin", env.get("PATH"));
        assertEquals("p@ss:w", NetworkProxy.parse(env.get("http_proxy")).orElseThrow().password());

        proxy.configure(ProxySettings.manual("proxy.example", 3128, "", ""), "");
        proxy.applyTo(env);
        assertEquals("http://proxy.example:3128", env.get("https_proxy"));

        proxy.configure(ProxySettings.DEFAULT.withMode(ProxySettings.Mode.NONE), "");
        proxy.applyTo(env);
        assertFalse(env.containsKey("http_proxy") || env.containsKey("HTTPS_PROXY") || env.containsKey("no_proxy"));
    }
    // --8<-- [end:apply]

    @Test
    void storeWritesAndFollowsProxyConf(@TempDir Path dir) throws Exception {
        SessionPasswordStore keyring = new SessionPasswordStore();
        ProxyStore store = new ProxyStore(dir, keyring);
        assertEquals(ProxySettings.DEFAULT, store.current());
        assertFalse(store.exists());

        List<ProxySettings> heard = new ArrayList<>();
        store.addListener(heard::add);
        ProxySettings manual = new ProxySettings(ProxySettings.Mode.MANUAL, "proxy.example", 8080, false,
                "tls.example", 8443, "ralf", "localhost, .lan");
        store.write(manual, "secret");
        assertTrue(store.exists());
        assertEquals(manual, store.current());
        assertEquals("secret", store.password());
        assertEquals(List.of(manual), heard);
        assertEquals(manual, new ProxyStore(dir, keyring).current());
        assertTrue(Files.readString(store.file()).contains("mode=manual"));

        store.write(manual.withMode(ProxySettings.Mode.NONE), "");
        assertEquals("", store.password(), "an empty password leaves the keyring");
        Files.writeString(store.file(), "mode=bogus\nhttp.port=x\n");
        store.reload();
        assertEquals(ProxySettings.Mode.SYSTEM, store.current().mode());
        assertEquals(ProxySettings.DEFAULT_PORT, store.current().port());
    }
}
