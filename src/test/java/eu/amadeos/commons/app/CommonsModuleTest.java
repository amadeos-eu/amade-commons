package eu.amadeos.commons.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.commons.theme.ThemeRegistry;
import eu.amadeos.commons.theme.ThemeStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonsModuleTest {
    @TempDir
    Path home;

    @Test
    void theUiExecutorIsTheEdtExecutor() throws Exception {
        XdgDirectories xdg = new XdgDirectories(Map.of(), home.toString());
        try (ExecutorService background = Executors.newVirtualThreadPerTaskExecutor()) {
            Injector injector = Guice.createInjector(new CommonsModule(new AppInfo("amade-test", "Test", "test"),
                    background, new ThemeStore(xdg.amadeosConfig(), new ThemeRegistry()), xdg));
            Executor ui = injector.getInstance(Key.get(Executor.class, Ui.class));
            assertSame(injector.getInstance(EdtExecutor.class), ui);

            CompletableFuture<Boolean> onEdt = new CompletableFuture<>();
            background.execute(() -> ui.execute(() -> onEdt.complete(SwingUtilities.isEventDispatchThread())));
            assertTrue(onEdt.get(5, TimeUnit.SECONDS));
        }
    }
}
