package me.entity303.antipluginlookup.newerversions;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class WorldChangeListener implements Listener {

    @EventHandler
    public void OnWorldChange(PlayerChangedWorldEvent e) {
        try {
            e.getPlayer().updateCommands();
        } catch (Exception ignored) {
        }
    }
}
