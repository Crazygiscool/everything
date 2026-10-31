package me.crazyg.everything;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EverythingCommandIntegrationTest {

    private static Everything plugin;
    private static be.seeseemelk.mockbukkit.ServerMock server;

    @BeforeAll
    static void setup() {
        server = (be.seeseemelk.mockbukkit.ServerMock) MockBukkit.mock();
        plugin = MockBukkit.load(Everything.class);
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void commandsOpenTheirScreens() {
        PlayerMock player = server.addPlayer("IntegrationPlayer");
        player.setOp(true);

        assertTrue(plugin.isEnabled());
        assertTrue(server.dispatchCommand(player, "everything"));
        assertTrue(server.dispatchCommand(player, "everything menu"));
        assertTrue(server.dispatchCommand(player, "everything chat"));
        assertTrue(server.dispatchCommand(player, "everything help"));
        assertTrue(server.dispatchCommand(player, "everything settings"));
        assertTrue(server.dispatchCommand(player, "everything admin"));
        assertTrue(server.dispatchCommand(player, "help"));
        player.closeInventory();
    }

    @Test
    void featureManagerPersiststoggles() {
        assertFalse(plugin.getFeatureManager().getBoolean("integration.feature-test", false));
        plugin.getFeatureManager().setBoolean("integration.feature-test", true);
        assertTrue(plugin.getFeatureManager().getBoolean(
            "integration.feature-test", false));
    }

    @Test
    void playerSettingsPersist() {
        PlayerMock player = server.addPlayer("SettingsPlayer");
        UUID uuid = player.getUniqueId();
        plugin.getPlayerSettingsManager().setBoolean(uuid, "particles", false);
        assertTrue(!plugin.getPlayerSettingsManager().getBoolean(
            uuid, "particles", true));
        plugin.getPlayerSettingsManager().setString(uuid, "help-mode", "disabled");
        assertTrue("disabled".equals(
            plugin.getPlayerSettingsManager().getHelpMode(uuid)));
    }

    @Test
    void reloadCommandSucceeds() {
        PlayerMock player = server.addPlayer("ReloadPlayer");
        player.setOp(true);
        assertTrue(server.dispatchCommand(player, "everything reload"));
    }
}
