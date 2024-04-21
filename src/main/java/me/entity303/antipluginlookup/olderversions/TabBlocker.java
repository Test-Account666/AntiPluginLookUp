package me.entity303.antipluginlookup.olderversions;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.entity.Player;

public abstract class TabBlocker {
    protected final AntiPluginLookUp plugin;

    public TabBlocker(AntiPluginLookUp plugin) {
        this.plugin = plugin;
    }

    public AntiPluginLookUp GetPlugin() {
        return this.plugin;
    }

    public abstract void Inject(Player var1);
}
