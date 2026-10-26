package eu.amadeos.commons.net;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Tries proxy settings before they are saved, for the Test button of Settings &gt; Network: asks a known HTTPS
 * address through them. Blocks; call it from a background thread.
 */
public final class ProxyCheck {
    /** The Debian mirror service, which amadeOS needs for its updates anyway. */
    public static final URI DEFAULT_TARGET = URI.create("https://deb.debian.org/debian/");

    public enum Outcome {
        /** The address answered. */
        OK,
        /** The proxy wants a user name and password (HTTP 407) and none was given. */
        CREDENTIALS_NEEDED,
        /** The proxy refused the user name or password. */
        CREDENTIALS_REFUSED,
        /** The address answered with an HTTP error; {@link Result#detail()} is the status code. */
        HTTP_ERROR,
        /** No connection; {@link Result#detail()} is the error message. */
        FAILED
    }

    /** @param via the proxy that was used, e.g. {@code proxy.example:3128}; empty for a direct connection */
    public record Result(Outcome outcome, String via, String detail) {
    }

    private final Map<String, String> environment;
    private final URI target;

    public ProxyCheck(Map<String, String> environment, URI target) {
        this.environment = Map.copyOf(environment);
        this.target = target;
    }

    public ProxyCheck() {
        this(System.getenv(), DEFAULT_TARGET);
    }

    public Result check(ProxySettings candidate, String password) {
        NetworkProxy probe = new NetworkProxy(environment);
        probe.configure(candidate, password);
        String via = probe.describe().orElse("");
        HttpClient client = probe.httpClient().connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder(target).header("User-Agent", "amadeos-settings")
                .timeout(Duration.ofSeconds(10)).GET().build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 407) {
                return new Result(candidate.user().isEmpty() ? Outcome.CREDENTIALS_NEEDED : Outcome.CREDENTIALS_REFUSED, via, "407");
            }
            if (response.statusCode() >= 400) return new Result(Outcome.HTTP_ERROR, via, String.valueOf(response.statusCode()));
            return new Result(Outcome.OK, via, String.valueOf(response.statusCode()));
        } catch (IOException e) {
            String m = e.getMessage() == null ? e.toString() : e.getMessage();
            if (m.contains("407")) {
                return new Result(candidate.user().isEmpty() ? Outcome.CREDENTIALS_NEEDED : Outcome.CREDENTIALS_REFUSED, via, m);
            }
            return new Result(Outcome.FAILED, via, m);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(Outcome.FAILED, via, "interrupted");
        } finally {
            client.close();
        }
    }
}
