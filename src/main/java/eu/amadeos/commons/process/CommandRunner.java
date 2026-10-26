package eu.amadeos.commons.process;

import eu.amadeos.commons.net.Network;
import eu.amadeos.commons.net.NetworkProxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Starts external programs. Code that runs programs takes a {@code CommandRunner} instead of calling
 * {@link ProcessBuilder} itself, so that tests can answer with canned output ({@link FinishedProcess}) and
 * never depend on what is installed.
 *
 * <p>Long-running programs whose output is a stream (a scanner's picture) use {@link #start} and read the
 * process themselves. Short commands use {@link #run}, which handles the parts that are easy to get wrong:
 * <pre>
 * ProgramResult result = runner.run(List.of("scanimage", "-L"), Duration.ofSeconds(20));
 * String volume = runner.output(List.of("wpctl", "get-volume", "@DEFAULT_AUDIO_SINK@"), Duration.ofSeconds(3));
 * </pre>
 */
@FunctionalInterface
public interface CommandRunner {
    /**
     * Starts the program; the first element of the command is its name or path.
     *
     * @throws ProgramMissingException if the program does not exist
     */
    Process start(List<String> command) throws IOException;

    // --8<-- [start:system]
    /**
     * Starts real processes, with the environment of this program and the current proxy of the desktop
     * ({@code http_proxy} and friends, see {@link NetworkProxy#applyTo}).
     */
    static CommandRunner system() {
        return command -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                Network.proxy().applyTo(builder.environment());
                return builder.start();
            } catch (IOException e) {
                throw ProgramMissingException.check(command.getFirst(), e);
            }
        };
    }
    // --8<-- [end:system]

    // --8<-- [start:run]
    /**
     * Runs a command to its end and collects what it printed. Standard input is closed at once, both output
     * streams are read at the same time on virtual threads, so that a program that fills one of them does
     * not block, and the time limit holds even when the program hangs without printing anything.
     *
     * @throws ProgramMissingException if the program does not exist
     * @throws IOException             if it does not end within {@code timeout}; it is stopped then
     * @throws InterruptedException    if the calling thread is interrupted; the program is stopped then
     */
    default ProgramResult run(List<String> command, Duration timeout) throws IOException, InterruptedException {
        Process process = start(command);
        process.getOutputStream().close();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Thread outReader = Thread.ofVirtual().start(() -> copy(process.getInputStream(), out));
        Thread errReader = Thread.ofVirtual().start(() -> copy(process.getErrorStream(), err));
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IOException(command.getFirst() + " did not finish within " + timeout.toSeconds() + " s");
            }
            // The streams end with the process; a child that inherited them could keep them open, so the
            // readers get a moment, not for ever.
            outReader.join(Duration.ofSeconds(5));
            errReader.join(Duration.ofSeconds(5));
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw e;
        }
        synchronized (out) {
            synchronized (err) {
                return new ProgramResult(process.exitValue(), out.toByteArray(), err.toByteArray());
            }
        }
    }
    // --8<-- [end:run]

    // --8<-- [start:output]
    /**
     * Runs a short command and returns its standard output as UTF-8 text.
     *
     * @throws IOException if the program is missing, fails (exit code other than 0, with its error message),
     *                     takes longer than {@code timeout} or is interrupted
     */
    default String output(List<String> command, Duration timeout) throws IOException {
        ProgramResult result;
        try {
            result = run(command, timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while " + command.getFirst() + " ran", e);
        }
        if (!result.succeeded()) throw new IOException(result.failure(command.getFirst()));
        return result.stdoutText();
    }
    // --8<-- [end:output]

    private static void copy(InputStream in, ByteArrayOutputStream out) {
        byte[] buffer = new byte[8192];
        try (in) {
            int n;
            while ((n = in.read(buffer)) >= 0) {
                synchronized (out) {
                    out.write(buffer, 0, n);
                }
            }
        } catch (IOException e) {
            // The process was stopped; what it printed so far is kept.
        }
    }
}
