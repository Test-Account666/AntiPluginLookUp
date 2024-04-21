package me.entity303.antipluginlookup.newerversions;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.ArrayList;
import java.util.Collection;

public class PlayerCommandSendListener implements Listener {
    private final AntiPluginLookUp plugin;

    public PlayerCommandSendListener(AntiPluginLookUp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void OnPlayerCommandSend(PlayerCommandSendEvent e) {
        if (e.getPlayer().hasPermission("antipluginlookup.allow.tabcomplete"))
            return;

        e.getCommands().clear();

        if (!AntiPluginLookUp.IsTabWhitelistActive())
            return;

        e.getCommands().addAll(this.GetCommands(e.getPlayer()));
    }

    private Collection<String> GetCommands(Player player) {
        Collection<String> commands = new ArrayList<>();
        for (var command : this.plugin.GetWhitelistedCommandManager().GetWhitelistedCommands()) {
            if (command.IsGlobal()) {
                if (!command.permissionNeeded()) {
                    commands.add(command.command().replaceFirst("/", ""));
                    continue;
                }

                if (!player.hasPermission(command.permission()))
                    continue;

                commands.add(command.command().replaceFirst("/", ""));
                continue;
            }

            if (!command.worlds().contains(player.getWorld().getName().toLowerCase()))
                continue;

            if (!command.permissionNeeded()) {
                commands.add(command.command().replaceFirst("/", ""));
                continue;
            }

            if (!player.hasPermission(command.permission()))
                continue;

            commands.add(command.command().replaceFirst("/", ""));
        }
        return commands;
    }
}
