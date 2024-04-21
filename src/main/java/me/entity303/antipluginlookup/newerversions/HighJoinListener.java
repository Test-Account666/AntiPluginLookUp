package me.entity303.antipluginlookup.newerversions;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class HighJoinListener implements Listener {
    private final AntiPluginLookUp plugin;

    public HighJoinListener(AntiPluginLookUp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void OnJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            try {
                e.getPlayer().updateCommands();
            } catch (Exception var2) {
                HandlerList.unregisterAll(this);
            }

        }, 20L);
    }
}
