package me.crazyg.everything;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import me.crazyg.everything.listeners.SlashPrefixTabCompleteListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class SlashPrefixTabCompletionTest {

    private static Everything plugin;
    private static be.seeseemelk.mockbukkit.ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Everything.class);
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void slashPrefixSuggestion() {
        PlayerMock player = server.addPlayer("TabPlayer");
        player.setOp(true);

        assertTrue(plugin.getCommand("wand") != null);
        List<String> doubleSlash = SlashPrefixTabCompleteListener.completionsFor(
            plugin, null, player, "//w");
        assertTrue(doubleSlash != null && doubleSlash.contains("//wand"),
            "double slash suffix should suggest //wand");

        List<String> singleSlash = SlashPrefixTabCompleteListener.completionsFor(
            plugin, null, player, "/wand");
        assertTrue(singleSlash != null && singleSlash.contains("//wand"),
            "single-slash form should be guided to //wand");

        assertNull(SlashPrefixTabCompleteListener.completionsFor(
            plugin, null, player, "///wand"),
            "three-slash buffers should not receive plugin suggestions");
    }
}
