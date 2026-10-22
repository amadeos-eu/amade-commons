package eu.amadeos.commons.process;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * What a program printed and how it ended, from {@link CommandRunner#run}.
 *
 * @param exitCode 0 for success, by convention
 * @param stdout   its standard output, as bytes: it may be a picture or text in any encoding
 * @param stderr   its error output
 */
public record ProgramResult(int exitCode, byte[] stdout, byte[] stderr) {
    public boolean succeeded() {
        return exitCode == 0;
    }

    public String stdoutText() {
        return stdoutText(StandardCharsets.UTF_8);
    }

    public String stdoutText(Charset charset) {
        return new String(stdout, charset);
    }

    public String stderrText() {
        return stderrText(StandardCharsets.UTF_8);
    }

    public String stderrText(Charset charset) {
        return new String(stderr, charset);
    }

    // --8<-- [start:failure]
    /** "wpctl failed (exit code 1): Connection refused", from the error output, else the standard output. */
    public String failure(String program) {
        String detail = stderrText().strip();
        if (detail.isEmpty()) detail = stdoutText().strip();
        return program + " failed (exit code " + exitCode + ")" + (detail.isEmpty() ? "" : ": " + detail);
    }
    // --8<-- [end:failure]
}
