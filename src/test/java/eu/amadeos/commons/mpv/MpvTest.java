package eu.amadeos.commons.mpv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MpvTest {
    @Test
    void recognisesTheSoftwareRendererWarning() {
        assertTrue(Mpv.isSoftwareRenderer(new MpvEvent.Log("warn", "vo/gpu/opengl",
                "Suspected software renderer or indirect context.")));
        assertFalse(Mpv.isSoftwareRenderer(new MpvEvent.Log("warn", "vo/gpu/vaapi", "libva: vaGetDriverNames() failed")));
    }

    @Test
    void systemPropertiesOverrideTheDefaults() {
        System.setProperty("mpv.gpu-context", "auto");
        try {
            assertEquals("auto", Mpv.overrides().get("gpu-context"));
        } finally {
            System.clearProperty("mpv.gpu-context");
        }
        assertFalse(Mpv.overrides().containsKey("gpu-context"));
    }

    @Test
    void observeIdsArePositive() {
        Mpv mpv = new Mpv();
        assertThrows(IllegalArgumentException.class, () -> mpv.observe(0, "pause", LibMpv.FORMAT_FLAG));
        assertThrows(IllegalArgumentException.class, () -> mpv.observe(-1, "pause", LibMpv.FORMAT_FLAG));
    }

    @Test
    void withoutAWindowNothingIsSent() {
        Mpv mpv = new Mpv();
        assertFalse(mpv.isAttached());
        mpv.command("stop");
        mpv.setProperty("pause", "yes");
        assertEquals("", mpv.property("pause"));
        mpv.close();
    }
}
