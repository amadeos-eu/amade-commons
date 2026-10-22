package eu.amadeos.commons.process;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * A process that has already ended with the given output. Tests answer a {@link CommandRunner} with it
 * instead of starting programs:
 * <pre>
 * SoundServer server = new SoundServer(command -> FinishedProcess.printing("Volume: 0.25"));
 * </pre>
 */
public final class FinishedProcess extends Process {
    private final byte[] stdout;
    private final byte[] stderr;
    private final int exitCode;

    public FinishedProcess(byte[] stdout, byte[] stderr, int exitCode) {
        this.stdout = stdout.clone();
        this.stderr = stderr.clone();
        this.exitCode = exitCode;
    }

    public FinishedProcess(String stdout, String stderr, int exitCode) {
        this(stdout.getBytes(StandardCharsets.UTF_8), stderr.getBytes(StandardCharsets.UTF_8), exitCode);
    }

    /** A successful run that printed {@code stdout}. */
    public static FinishedProcess printing(String stdout) {
        return new FinishedProcess(stdout, "", 0);
    }

    /** A failed run that printed {@code stderr}. */
    public static FinishedProcess failing(int exitCode, String stderr) {
        return new FinishedProcess("", stderr, exitCode);
    }

    @Override
    public OutputStream getOutputStream() {
        return OutputStream.nullOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(stdout);
    }

    @Override
    public InputStream getErrorStream() {
        return new ByteArrayInputStream(stderr);
    }

    @Override
    public int waitFor() {
        return exitCode;
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) {
        return true;
    }

    @Override
    public int exitValue() {
        return exitCode;
    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean isAlive() {
        return false;
    }
}
