package org.datazuul.amadeos.commons.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Set;

/**
 * Follows some files of one directory, such as {@code theme.conf} and {@code locale.conf} in
 * {@code ~/.config/amadeos}, and runs an action on a daemon thread when one of them is created, changed
 * or deleted. Used by the stores of desktop settings; the desktop writes the files, applications follow.
 */
public final class ConfigWatcher implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigWatcher.class);
    /** The desktop writes the files non-atomically; wait a moment before reading them. */
    private static final long SETTLE_MILLIS = 150;

    private final Path directory;
    private final Set<String> fileNames;
    private final String threadName;
    private final Runnable onChange;
    private WatchService watchService;
    private Thread thread;

    public ConfigWatcher(Path directory, Set<String> fileNames, String threadName, Runnable onChange) {
        this.directory = directory;
        this.fileNames = Set.copyOf(fileNames);
        this.threadName = threadName;
        this.onChange = onChange;
    }

    /** Starts watching; false if the file system cannot watch the directory. Calling it again has no effect. */
    public synchronized boolean start() {
        if (thread != null) return true;
        try {
            Files.createDirectories(directory);
            watchService = directory.getFileSystem().newWatchService();
            directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        } catch (IOException | UnsupportedOperationException e) {
            LOG.warn("Changes to {} in {} will not be followed: {}", fileNames, directory, e.toString());
            return false;
        }
        WatchService service = watchService;
        thread = Thread.ofPlatform().daemon().name(threadName).start(() -> loop(service));
        return true;
    }

    private void loop(WatchService service) {
        try {
            while (true) {
                WatchKey key = service.take();
                boolean relevant = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    Object context = event.context();
                    relevant |= event.kind() == StandardWatchEventKinds.OVERFLOW
                            || (context instanceof Path p && fileNames.contains(p.getFileName().toString()));
                }
                key.reset();
                if (relevant) {
                    Thread.sleep(SETTLE_MILLIS);
                    try {
                        onChange.run();
                    } catch (RuntimeException e) {
                        LOG.warn("Reacting to a change in {} failed", directory, e);
                    }
                }
            }
        } catch (InterruptedException | ClosedWatchServiceException e) {
            // stopped
        }
    }

    @Override
    public synchronized void close() {
        if (thread != null) thread.interrupt();
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                LOG.debug("Closing watch service failed", e);
            }
        }
        thread = null;
        watchService = null;
    }
}
