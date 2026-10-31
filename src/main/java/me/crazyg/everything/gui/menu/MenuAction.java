package me.crazyg.everything.gui.menu;

public enum MenuAction {
    NONE,
    OPEN_MENU,
    OPEN_HELP,
    OPEN_PLAYER_SETTINGS,
    OPEN_ADMIN,
    OPEN_PAGE,
    PERFORM_COMMAND,
    TOGGLE_FEATURE,
    TOGGLE_SETTING,
    CYCLE_HELP_MODE,
    OPEN_CHAT,
    CLOSE;

    public static MenuAction from(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return NONE;
        }
        String value = raw.trim().toLowerCase()
            .replace('-', '_').replace(' ', '_');
        return switch (value) {
            case "open_menu", "menu" -> OPEN_MENU;
            case "open_help", "help" -> OPEN_HELP;
            case "player_settings", "settings", "open_player_settings" -> OPEN_PLAYER_SETTINGS;
            case "admin", "open_admin", "admin_tools" -> OPEN_ADMIN;
            case "open_page", "page" -> OPEN_PAGE;
            case "perform_command", "command", "run_command" -> PERFORM_COMMAND;
            case "toggle_feature", "feature" -> TOGGLE_FEATURE;
            case "toggle_setting", "setting" -> TOGGLE_SETTING;
            case "cycle_help_mode", "cycle_help" -> CYCLE_HELP_MODE;
            case "open_chat", "chat" -> OPEN_CHAT;
            case "close" -> CLOSE;
            default -> NONE;
        };
    }
}
