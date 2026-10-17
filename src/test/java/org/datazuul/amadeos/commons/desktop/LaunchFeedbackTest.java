package org.datazuul.amadeos.commons.desktop;

import org.junit.jupiter.api.Test;

/**
 * The test JVM runs with {@code --enable-native-access=ALL-UNNAMED} (build.gradle), so under Xvfb
 * this sends the message to the X server; without a display it must return quietly.
 */
class LaunchFeedbackTest {
    @Test
    void announcingNeverThrows() {
        LaunchFeedback.announce();
        LaunchFeedback.announce();
    }
}
