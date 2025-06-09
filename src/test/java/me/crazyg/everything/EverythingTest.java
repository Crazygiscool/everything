package me.crazyg.everything;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EverythingTest {
    private ServerMock server;
    private Everything plugin;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Everything.class);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testPluginEnable() {
        assertTrue(plugin.isEnabled());
        assertNotNull(server.getPluginManager().getPlugin("everything"));
    }

    @Test
    void testConfigDefaults() {
        assertNotNull(plugin.getConfig());
        assertTrue(plugin.getConfig().contains("homes"));
        assertTrue(plugin.getConfig().contains("warps"));
    }
}