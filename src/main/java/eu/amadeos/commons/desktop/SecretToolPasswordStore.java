package eu.amadeos.commons.desktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Stores passwords in the freedesktop Secret Service (gnome-keyring, KeePassXC, ...) through the
 * {@code secret-tool} command from the Debian package {@code libsecret-tools}. Falls back to a
 * {@link SessionPasswordStore} when the tool or the service is not available.
 *
 * <p>Entries carry the attributes {@code service=<application>} and {@code account=<key>}, so they can be
 * found with {@code secret-tool search service amade-mail}.
 */
public final class SecretToolPasswordStore implements PasswordStore {
    private static final Logger LOG = LoggerFactory.getLogger(SecretToolPasswordStore.class);

    private final String service;
    private final String executable;
    private final SessionPasswordStore cache = new SessionPasswordStore();

    /** @param service the application, e.g. {@code amade-mail} */
    public SecretToolPasswordStore(String service) {
        this(service, findOnPath("secret-tool"));
    }

    SecretToolPasswordStore(String service, String executable) {
        if (service == null || service.isBlank()) throw new IllegalArgumentException("service required");
        this.service = service;
        this.executable = executable;
    }

    // --8<-- [start:lookup]
    @Override
    public Optional<String> lookup(String key) {
        Optional<String> cached = cache.lookup(key);
        if (cached.isPresent() || executable == null) return cached;
        Result result = run(null, executable, "lookup", "service", service, "account", key);
        if (result.exitCode() != 0 || result.output().isEmpty()) return Optional.empty();
        cache.store(key, "", result.output());
        return Optional.of(result.output());
    }
    // --8<-- [end:lookup]

    // --8<-- [start:store]
    @Override
    public void store(String key, String label, String password) {
        cache.store(key, label, password);
        if (executable == null) return;
        Result result = run(password, executable, "store", "--label=" + label,
                "service", service, "account", key);
        if (result.exitCode() != 0) {
            LOG.warn("secret-tool could not store the password for {} (exit {}); keeping it for this session only",
                    key, result.exitCode());
        }
    }
    // --8<-- [end:store]

    @Override
    public void delete(String key) {
        cache.delete(key);
        if (executable != null) run(null, executable, "clear", "service", service, "account", key);
    }

    @Override
    public boolean isPersistent() {
        return executable != null;
    }

    // --8<-- [start:run]
    private static Result run(String input, String... command) {
        try {
            Process process = new ProcessBuilder(List.of(command)).redirectErrorStream(false).start();
            try (OutputStream stdin = process.getOutputStream()) {
                if (input != null) stdin.write(input.getBytes(StandardCharsets.UTF_8));
            }
            // The output is a single password, far below the pipe buffer, so waiting first cannot dead-lock
            // and a hanging keyring (e.g. waiting for an unlock prompt) cannot block the caller forever.
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new Result(-1, "");
            }
            String output;
            try (InputStream stdout = process.getInputStream(); InputStream stderr = process.getErrorStream()) {
                output = new String(stdout.readAllBytes(), StandardCharsets.UTF_8);
                stderr.readAllBytes();
            }
            return new Result(process.exitValue(), output);
        } catch (IOException e) {
            LOG.warn("Cannot run secret-tool", e);
            return new Result(-1, "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(-1, "");
        }
    }
    // --8<-- [end:run]

    static String findOnPath(String name) {
        String path = System.getenv("PATH");
        if (path == null) return null;
        for (String dir : path.split(File.pathSeparator)) {
            File candidate = new File(dir, name);
            if (candidate.isFile() && candidate.canExecute()) return candidate.getAbsolutePath();
        }
        return null;
    }

    private record Result(int exitCode, String output) {
    }
}
