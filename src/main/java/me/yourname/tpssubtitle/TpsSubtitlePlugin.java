package me.yourname.tpssubtitle;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.Locale;

public final class TpsSubtitlePlugin extends JavaPlugin {

    private static final String PERM_VIEW = "tpssubtitle.view";
    private final MiniMessage mm = MiniMessage.miniMessage();

    private BukkitTask task;
    private Method getAverageTickTimeMethod;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        hookAverageTickTime();

        int interval = Math.max(1, getConfig().getInt("update-interval-ticks", 20));
        task = Bukkit.getScheduler().runTaskTimer(this, this::tick, interval, interval);
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
    }

    private void hookAverageTickTime() {
        try {
            getAverageTickTimeMethod = Bukkit.getServer().getClass().getMethod("getAverageTickTime");
        } catch (NoSuchMethodException ignored) {
            getAverageTickTimeMethod = null;
        }
    }

    private void tick() {
        boolean allowOp = getConfig().getBoolean("allow-op", true);
        String format = getConfig().getString("subtitle-format",
                "<gray>TPS: <green>{tps}</green> | <gray>MSPT: <yellow>{mspt}</yellow> | <gray>Ping: <aqua>{ping}ms</aqua>");

        double tps = getRecentTps();
        double mspt = getAverageMspt();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!canSee(p, allowOp)) continue;

            int ping = p.getPing();

            String msg = format
                    .replace("{tps}", fmt1(tps))
                    .replace("{mspt}", fmt1(mspt))
                    .replace("{ping}", Integer.toString(ping));

            Component subtitle = mm.deserialize(msg);
            p.showTitle(net.kyori.adventure.title.Title.title(Component.empty(), subtitle));
        }
    }

    private boolean canSee(Player p, boolean allowOp) {
        if (p.hasPermission(PERM_VIEW)) return true;
        return allowOp && p.isOp();
    }

    private double getRecentTps() {
        double[] tps = Bukkit.getTPS();
        return tps.length > 0 ? tps[0] : 20.0;
    }

    private double getAverageMspt() {
        if (getAverageTickTimeMethod != null) {
            try {
                Object v = getAverageTickTimeMethod.invoke(Bukkit.getServer());
                if (v instanceof Double d) return d;
                if (v instanceof Number n) return n.doubleValue();
            } catch (Throwable ignored) {}
        }
        double tps = getRecentTps();
        if (tps <= 0.0) return 50.0;
        return 1000.0 / tps;
    }

    private String fmt1(double v) {
        return String.format(Locale.US, "%.1f", v);
    }
}
