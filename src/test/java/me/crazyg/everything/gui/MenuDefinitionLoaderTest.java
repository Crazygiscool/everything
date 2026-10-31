package me.crazyg.everything.gui;

import me.crazyg.everything.gui.menu.MenuDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuDefinitionLoaderTest {

    @Test
    void loadsPrimaryMenuFromBundledConfig() throws Exception {
        try (java.io.InputStream in = getClass().getResourceAsStream(
                "/config.yml")) {
            YamlConfiguration config = YamlConfiguration
                .loadConfiguration(new java.io.InputStreamReader(in));
            MenuDefinition primary = MenuDefinition.load(
                null, "primary", config);
            org.junit.jupiter.api.Assertions.assertNotNull(primary);
            org.junit.jupiter.api.Assertions.assertEquals(
                "primary", primary.menuId());
            org.junit.jupiter.api.Assertions.assertTrue(
                !primary.items().isEmpty());
            org.junit.jupiter.api.Assertions.assertTrue(
                primary.items().stream().anyMatch(
                    item -> item.action()
                        .toString().equals("PERFORM_COMMAND")));
        }
    }
}
