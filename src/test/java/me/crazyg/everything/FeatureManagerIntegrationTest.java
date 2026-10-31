package me.crazyg.everything;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureManagerIntegrationTest {

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
    void togglesPersistAndChangeFeatureState() {
        String path = "features.test-value";
        plugin.getFeatureManager().setBoolean(path, true);
        assertTrue(plugin.getFeatureManager().getBoolean(path, false));

        plugin.getFeatureManager().setBoolean(path, false);
        assertFalse(plugin.getFeatureManager().getBoolean(path, false));
    }

    @Test
    void appliesParticleFeatureToggleLive() {
        String path = "particles.commands.tp";
        plugin.getFeatureManager().setBoolean(path, false);
        assertEquals(
            me.crazyg.everything.utils.FeatureManager.FeatureApplyOutcome.LIVE,
            plugin.applyFeatureToggle(path, false));
        assertFalse(plugin.getParticleManager().isEnabled("tp"));

        plugin.getFeatureManager().setBoolean(path, true);
        plugin.getParticleManager().loadConfig();
        assertTrue(plugin.getParticleManager().isEnabled("tp"));
    }

    @Test
    void cosmeticToggleAppliesImmediately() {
        me.crazyg.everything.utils.FeatureManager.FeatureApplyOutcome outcome =
            plugin.applyFeatureToggle("cosmetics.join-messages", false);
        assertEquals(
            me.crazyg.everything.utils.FeatureManager.FeatureApplyOutcome.LIVE,
            outcome);
        assertFalse(plugin.getFeatureManager().getBoolean(
            "cosmetics.join-messages", true));

        plugin.applyFeatureToggle("cosmetics.join-messages", true);
    }

    @Test
    void unknownFeaturePathIsIgnoredAndNotPersisted() {
        me.crazyg.everything.utils.FeatureManager.FeatureApplyOutcome outcome =
            plugin.applyFeatureToggle("invalid.feature.path", true);
        assertEquals(
            me.crazyg.everything.utils.FeatureManager.FeatureApplyOutcome.INVALID_PATH,
            outcome);
        assertFalse(plugin.getFeatureManager().getConfig().contains("invalid.feature.path"));
    }
}
