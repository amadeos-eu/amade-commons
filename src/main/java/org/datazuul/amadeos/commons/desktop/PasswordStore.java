package org.datazuul.amadeos.commons.desktop;

import java.util.Optional;

/**
 * Keeps secrets (passwords, tokens) outside configuration files, keyed by an application-defined key
 * such as an account id.
 */
public interface PasswordStore {
    Optional<String> lookup(String key);

    void store(String key, String label, String password);

    void delete(String key);

    /** False if passwords only live for the running session. */
    boolean isPersistent();
}
