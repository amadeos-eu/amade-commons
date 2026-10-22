package eu.amadeos.commons.process;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRunnerTest {
    private static final Duration LIMIT = Duration.ofSeconds(10);
    private final CommandRunner system = CommandRunner.system();

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void outputOfAShortCommand() throws Exception {
        assertEquals("hello\n", system.output(List.of("echo", "hello"), LIMIT));
    }

    @Test
    void aMissingProgramIsReportedAsSuch() {
        ProgramMissingException e = assertThrows(ProgramMissingException.class,
                () -> system.output(List.of("amade-no-such-program"), LIMIT));
        assertEquals("amade-no-such-program", e.program());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void failuresCarryTheErrorOutput() {
        IOException e = assertThrows(IOException.class,
                () -> system.output(List.of("sh", "-c", "echo 'device busy' >&2; exit 3"), LIMIT));
        assertEquals("sh failed (exit code 3): device busy", e.getMessage());
    }

    // --8<-- [start:hang]
    /** A program that hangs without printing anything must not hang its caller: the limit holds. */
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void theTimeLimitHoldsForAProgramThatPrintsNothing() {
        long start = System.nanoTime();
        IOException e = assertThrows(IOException.class,
                () -> system.run(List.of("sleep", "30"), Duration.ofMillis(300)));
        assertTrue(e.getMessage().contains("did not finish"), e.getMessage());
        assertTrue(System.nanoTime() - start < Duration.ofSeconds(10).toNanos());
    }
    // --8<-- [end:hang]

    /** 300 KB on each stream: far more than a pipe holds, so reading one after the other would block. */
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void bothStreamsAreReadAtTheSameTime() throws Exception {
        ProgramResult result = system.run(List.of("sh", "-c",
                "head -c 300000 /dev/zero >&2; head -c 300000 /dev/zero"), LIMIT);
        assertTrue(result.succeeded());
        assertEquals(300_000, result.stdout().length);
        assertEquals(300_000, result.stderr().length);
    }

    @Test
    void testsAnswerWithFinishedProcesses() throws Exception {
        List<List<String>> commands = new ArrayList<>();
        CommandRunner fake = command -> {
            commands.add(command);
            return command.getFirst().equals("wpctl") ? FinishedProcess.printing("Volume: 0.25")
                    : FinishedProcess.failing(1, "no such sink");
        };
        assertEquals("Volume: 0.25", fake.output(List.of("wpctl", "get-volume"), LIMIT));
        IOException e = assertThrows(IOException.class, () -> fake.output(List.of("pactl", "info"), LIMIT));
        assertEquals("pactl failed (exit code 1): no such sink", e.getMessage());
        assertEquals(List.of(List.of("wpctl", "get-volume"), List.of("pactl", "info")), commands);
    }

    @Test
    void onlyAMissingProgramBecomesProgramMissing() {
        IOException missing = new IOException("Cannot run program \"x\": error=2, No such file or directory");
        IOException denied = new IOException("Cannot run program \"x\": error=13, Permission denied");
        assertTrue(ProgramMissingException.check("x", missing) instanceof ProgramMissingException);
        assertSame(denied, ProgramMissingException.check("x", denied));
        assertFalse(ProgramMissingException.check("x", denied) instanceof ProgramMissingException);
    }
}
