package eu.amadeos.commons.process;

import java.io.IOException;

/**
 * A program that is needed is not installed. Programs show this differently from other failures, usually
 * with the name of the package to install.
 */
public class ProgramMissingException extends IOException {
    private final String program;

    public ProgramMissingException(String program, Throwable cause) {
        super(program + " not found", cause);
        this.program = program;
    }

    /** The program, as it was called. */
    public String program() {
        return program;
    }

    // --8<-- [start:check]
    /**
     * Turns the JDK's "Cannot run program" into a {@code ProgramMissingException}; other errors (no permission,
     * no memory for the process) stay as they are.
     */
    public static IOException check(String program, IOException e) {
        if (e instanceof ProgramMissingException) return e;
        String message = String.valueOf(e.getMessage());
        // "Cannot run program "x": error=2, No such file or directory" on Linux, "CreateProcess error=2" on Windows
        if (message.contains("error=2") || message.contains("No such file")) {
            return new ProgramMissingException(program, e);
        }
        return e;
    }
    // --8<-- [end:check]
}
