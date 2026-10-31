package me.crazyg.everything.gui.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuActionTest {

    @Test
    void parsesAliases() {
        assertEquals(MenuAction.OPEN_MENU, MenuAction.from("open-menu"));
        assertEquals(MenuAction.OPEN_MENU, MenuAction.from("menu"));
        assertEquals(MenuAction.OPEN_PAGE, MenuAction.from("page"));
        assertEquals(MenuAction.PERFORM_COMMAND, MenuAction.from("run_command"));
        assertEquals(MenuAction.TOGGLE_FEATURE, MenuAction.from("feature"));
        assertEquals(MenuAction.TOGGLE_SETTING, MenuAction.from("setting"));
        assertEquals(MenuAction.OPEN_CHAT, MenuAction.from("open-chat"));
        assertEquals(MenuAction.CLOSE, MenuAction.from("close"));
    }

    @Test
    void handlesBlankAndUnknown() {
        assertEquals(MenuAction.NONE, MenuAction.from(null));
        assertEquals(MenuAction.NONE, MenuAction.from("   "));
        assertEquals(MenuAction.NONE, MenuAction.from("not-a-valid-action"));
    }
}
