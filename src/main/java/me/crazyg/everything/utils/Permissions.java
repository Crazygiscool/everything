package me.crazyg.everything.utils;

/**
 * Central registry of all permission nodes used by the plugin.
 * Referencing these constants instead of inline strings catches typos at
 * compile time and keeps plugin.yml in sync.
 */
public final class Permissions {

    private Permissions() {}

    // General
    public static final String EVERYTHING = "everything.everything";
    public static final String RELOAD = "everything.reload";
    public static final String UPDATE = "everything.update";
    public static final String MAINTENANCE = "everything.maintenance";
    public static final String MAINTENANCE_BYPASS = "everything.maintenance.bypass";

    // Teleport
    public static final String TELEPORT = "everything.teleport";
    public static final String TELEPORT_OTHER = "everything.teleport.other";
    public static final String TPA = "everything.tpa";
    public static final String TPACCEPT = "everything.tpaccept";
    public static final String TPENY = "everything.tpdeny";
    public static final String RTP = "everything.rtp";
    public static final String RTP_BYPASS_COOLDOWN = "everything.rtp.bypasscooldown";

    // Combat / utility
    public static final String KILL = "everything.kill";
    public static final String GOD = "everything.god";
    public static final String GOD_OTHER = "everything.god.others";
    public static final String GAMEMODE_CREATIVE = "everything.gamemode.creative";
    public static final String GAMEMODE_SURVIVAL = "everything.gamemode.survival";
    public static final String GAMEMODE_SPECTATOR = "everything.gamemode.spectator";
    public static final String GAMEMODE_ADVENTURE = "everything.gamemode.adventure";
    public static final String GAMEMODE_OTHER = "everything.gamemode.others";

    // Homes / spawn / warps
    public static final String SPAWN = "everything.spawn";
    public static final String SPAWN_SET = "everything.spawn.set";
    public static final String HOME = "everything.home";
    public static final String HOME_SET = "everything.home.set";
    public static final String WARP = "everything.warp";
    public static final String WARP_SET = "everything.warp.set";
    public static final String WARP_DELETE = "everything.warp.delete";

    // Chat / messaging
    public static final String MSG = "everything.msg";
    public static final String REPLY = "everything.reply";
    public static final String NAMECOLOR = "everything.namecolor";

    // Economy
    public static final String ECO = "everything.eco";
    public static final String ECO_OTHERS = "everything.eco.others";
    public static final String ECO_SET = "everything.eco.set";
    public static final String ECO_SET_OTHERS = "everything.eco.set.others";
    public static final String BALANCE = "everything.balance";
    public static final String PAY = "everything.pay";

    // Stats / help / report
    public static final String STATS = "everything.stats";
    public static final String STATS_OTHERS = "everything.stats.others";
    public static final String HELP = "everything.help";
    public static final String REPORT = "everything.report";
    public static final String REPORT_VIEW = "everything.report.view";

    // Block log
    public static final String BLOCKLOG_INSPECT = "everything.blocklog.inspect";
    public static final String BLOCKLOG_LOOKUP = "everything.blocklog.lookup";
    public static final String BLOCKLOG_ROLLBACK = "everything.blocklog.rollback";
    public static final String BLOCKLOG_PRUNE = "everything.blocklog.prune";
}
