package me.crazyg.everything.gui;

import me.crazyg.everything.gui.menu.MenuDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuDefinitionValidationTest {

    @Test
    void loaderHandlesDuplicateAndInvalidSlots() throws Exception {
        String yaml = """
            menus:
              test:
                title: "Test Menu"
                size: 27
                filler: "NOT_A_MATERIAL"
                items:
                  first:
                    slot: 1
                    icon: "BOOK"
                    name: "First"
                    action: "close"
                  duplicate:
                    slot: 1
                    icon: "STONE"
                    name: "Duplicate"
                    action: "close"
                  invalid:
                    slot: 99
                    icon: "DIRT"
                    name: "Invalid"
                    action: "close"
            """;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
            new java.io.StringReader(yaml));

        MenuDefinition menu = MenuDefinition.load(null, "test", config);
        assertNotNull(menu);
        assertEquals(27, menu.size());
        assertEquals(Material.BARRIER, menu.filler());
        assertEquals(1, menu.items().size());
        assertEquals(1, menu.itemsBySlot().size());
        assertEquals("first", menu.items().get(0).id());
    }

    @Test
    void loadsAdminAndPlayerSettingsMenus() throws Exception {
        try (java.io.InputStream in = getClass().getResourceAsStream("/config.yml")) {
            org.bukkit.configuration.file.YamlConfiguration config =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(in));
            MenuDefinition admin = MenuDefinition.load(null, "admin", config);
            MenuDefinition player = MenuDefinition.load(null, "player-settings", config);

            org.junit.jupiter.api.Assertions.assertNotNull(admin);
            org.junit.jupiter.api.Assertions.assertNotNull(player);
            org.junit.jupiter.api.Assertions.assertTrue(admin.size() >= 27);
            org.junit.jupiter.api.Assertions.assertTrue(player.size() >= 27);
        }
    }

    @Test
    void unauthorizedModeDefaultsToLock() throws Exception {
        String yaml = """
            menus:
              test:
                title: "Test Menu"
                size: 27
                items:
                  first:
                    slot: 1
                    icon: "BOOK"
                    name: "First"
                    action: "close"
            """;
        org.bukkit.configuration.file.YamlConfiguration config =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.StringReader(yaml));
        MenuDefinition menu = MenuDefinition.load(null, "test", config);
        assertEquals("lock", menu.unauthorizedMode());
    }
}
