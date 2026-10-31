package me.crazyg.everything;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureToggleIntegrationTest {

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
    void particleToggleAppliesRuntimeState() {
        plugin.getFeatureManager().setBoolean("particles.master", false);
        assertEquals(
            me.crazyg.everything.utils.FeatureManager.FeatureApplyOutcome.LIVE,
            plugin.applyFeatureToggle("particles.master", false));
        assertFalse(plugin.getParticleManager().isEnabled("tp"));

        plugin.applyFeatureToggle("particles.master", true);
        plugin.getParticleManager().loadConfig();
        assertTrue(plugin.getParticleManager().isEnabled("tp"));
    }

    @Test
    void blocklogCategoryToggleAppliesAndPersists() {
        plugin.applyFeatureToggle("blocklog.log-natural", false);
        assertFalse(plugin.getFeatureManager().getBoolean(
            "blocklog.log-natural", true));
        assertFalse(me.crazyg.everything.blocklog.BlockLogConfig.logNatural(
            plugin.getConfig()));

        plugin.getFeatureManager().setBoolean("blocklog.log-natural", true);
        if (plugin.getBlockLogListener() != null) {
            plugin.getBlockLogListener().reloadConfig();
        }
        assertTrue(me.crazyg.everything.blocklog.BlockLogConfig.logNatural(
            plugin.getConfig()));
    }

    @Test
    void invalidToggleIgnored() {
        me.crazyg.everything.utils.FeatureManager.FeatureApplyOutcome outcome =
            plugin.applyFeatureToggle("features.unknown", true);
        assertEquals(
            me.crazyg.everything.utils.FeatureManager.FeatureApplyOutcome.INVALID_PATH,
            outcome);
        assertFalse(plugin.getFeatureManager().getConfig().contains("features.unknown"));
    }

    @Test
    void cosmeticTogglePersists() throws Exception {
        plugin.applyFeatureToggle("cosmetics.join-messages", false);
        assertFalse(plugin.getFeatureManager().getBoolean(
            "cosmetics.join-messages", true));

        plugin.applyFeatureToggle("cosmetics.join-messages", true);
        assertTrue(plugin.getFeatureManager().getBoolean(
            "cosmetics.join-messages", true));
    }
}
