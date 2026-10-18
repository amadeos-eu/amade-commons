package eu.amadeos.commons.desktop;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps passwords in memory for the running session only. Used when no keyring is available.
 */
public final class SessionPasswordStore implements PasswordStore {
    private final Map<String, String> passwords = new ConcurrentHashMap<>();

    @Override
    public Optional<String> lookup(String key) {
        return Optional.ofNullable(passwords.get(key));
    }

    @Override
    public void store(String key, String label, String password) {
        passwords.put(key, password);
    }

    @Override
    public void delete(String key) {
        passwords.remove(key);
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}
