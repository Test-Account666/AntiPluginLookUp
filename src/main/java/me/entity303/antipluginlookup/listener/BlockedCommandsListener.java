package me.entity303.antipluginlookup.listener;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class BlockedCommandsListener implements Listener {
    private final AntiPluginLookUp plugin;

    public BlockedCommandsListener(AntiPluginLookUp plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void OnBlockedCommand(PlayerCommandPreprocessEvent event) {
        var commandString = event.getMessage().toLowerCase().split(" ")[0];
        var found = false;
        var isMinecraft = false;
        var isBukkit = false;

        if (commandString.startsWith("/minecraft:")) {
            found = true;
            isMinecraft = true;
            commandString = commandString.replaceFirst("minecraft:", "");
        }

        if (!found && commandString.startsWith("/bukkit:")) {
            isBukkit = true;
            commandString = commandString.replaceFirst("bukkit:", "");
        }

        if (!this.plugin.getBlockedCommandManager().IsBlockedCommand(commandString))
            return;

        var command = this.plugin.getBlockedCommandManager().GetBlockedCommand(commandString);
        if (command == null) {
            event.getPlayer().sendMessage("§cAn error has occurred, please contact an admin!");
            return;
        }

        var whitelistedCommands = this.plugin.GetWhitelistedCommandManager().GetWhitelistedCommands(command.command());
        if (whitelistedCommands.stream().anyMatch(whitelistedCommand -> whitelistedCommand.command().equalsIgnoreCase(command.command())))
            return;

        if (isMinecraft && !command.minecraft())
            return;

        if (isBukkit && !command.bukkit())
            return;

        if (!command.isGlobal() && !command.worlds().contains(event.getPlayer().getWorld().getName().toLowerCase()))
            return;

        if (event.getPlayer().hasPermission(command.permission()))
            return;

        event.setCancelled(true);

        if (command.sendMessage())
            this.plugin.SendMessage(event.getPlayer(), command.message());

        if (command.advertisement()) {
            this.plugin.SendMessage(event.getPlayer(), "AntiPluginLookUp by Entity303");
            this.plugin.SendMessage(event.getPlayer(), "Download: https://dev.bukkit.org/projects/antipluginlookup");
        }
    }
}
