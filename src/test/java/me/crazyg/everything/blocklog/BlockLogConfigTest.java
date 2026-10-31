package me.crazyg.everything.blocklog;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BlockLogConfigTest {

    @Test
    void defaultWorldIsLogged() {
        YamlConfiguration cfg = new YamlConfiguration();
        assertTrue(BlockLogConfig.isWorldLogged(cfg, "world"));
    }

    @Test
    void blacklistSkipsListedWorld() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("blocklog.worlds.mode", "blacklist");
        cfg.set("blocklog.worlds.list", java.util.List.of("nether"));
        assertFalse(BlockLogConfig.isWorldLogged(cfg, "nether"));
        assertTrue(BlockLogConfig.isWorldLogged(cfg, "world"));
    }

    @Test
    void whitelistOnlyListedWorld() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("blocklog.worlds.mode", "whitelist");
        cfg.set("blocklog.worlds.list", java.util.List.of("world"));
        assertTrue(BlockLogConfig.isWorldLogged(cfg, "world"));
        assertFalse(BlockLogConfig.isWorldLogged(cfg, "nether"));
    }
}
