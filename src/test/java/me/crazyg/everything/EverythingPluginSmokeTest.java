package me.crazyg.everything;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EverythingPluginSmokeTest {

    private static Everything plugin;

    @BeforeAll
    static void setup() {
        MockBukkit.mock();
        plugin = MockBukkit.load(Everything.class);
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void pluginLoadsAndEnables() {
        assertTrue(plugin != null);
        assertTrue(plugin.isEnabled());
    }
}
