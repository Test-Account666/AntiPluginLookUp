package me.entity303.antipluginlookup.olderversions;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final AntiPluginLookUp plugin;

    public JoinListener(AntiPluginLookUp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void OnLogin(PlayerJoinEvent e) {
        if (this.GetPlugin().GetTabBlocker() == null) {
            HandlerList.unregisterAll(this);
            return;
        }

        this.GetPlugin().GetTabBlocker().Inject(e.getPlayer());
    }

    public AntiPluginLookUp GetPlugin() {
        return this.plugin;
    }
}
