package me.entity303.antipluginlookup.newerversions;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import me.entity303.antipluginlookup.whitelistedcommands.WhitelistedCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.List;

public class TabCompleteListener implements Listener {
    private final AntiPluginLookUp plugin;

    public TabCompleteListener(AntiPluginLookUp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void OnTabComplete(TabCompleteEvent e) {
        if (e.getSender().hasPermission("antipluginlookup.allow.tabcomplete"))
            return;

        if (!(e.getSender() instanceof Player player))
            return;

        var currentCommand = e.getBuffer();

        var command = currentCommand.split(" ")[0];
        var count = currentCommand.split(" ").length;

        if (!currentCommand.endsWith(" "))
            --count;

        var argument = "";
        if (currentCommand.split(" ").length > count)
            argument = currentCommand.split(" ")[count];

        if (!AntiPluginLookUp.IsTabWhitelistActive()) {
            e.setCompletions(this.PlayerNames(argument.equalsIgnoreCase("")? null : argument, player));
            return;
        }

        var whitelistedCommand = this.GetWhitelistedCommand(command);
        if (whitelistedCommand == null) {
            e.setCompletions(this.PlayerNames(argument.equalsIgnoreCase("")? null : argument, player));
            return;
        }

        if (!whitelistedCommand.blockArguments())
            return;

        e.setCompletions(this.PlayerNames(argument.equalsIgnoreCase("")? null : argument, player));
    }

    public List<String> PlayerNames(String name, Player commandSender) {
        return name == null?
               Bukkit.getOnlinePlayers().stream().filter(commandSender::canSee).map(HumanEntity::getName).toList() :
               Bukkit.getOnlinePlayers()
                     .stream()
                     .filter(commandSender::canSee)
                     .map(HumanEntity::getName)
                     .filter(playerName -> playerName.toLowerCase().startsWith(name.toLowerCase()))
                     .toList();
    }

    public WhitelistedCommand GetWhitelistedCommand(String command) {
        return this.plugin.GetWhitelistedCommandManager()
                          .GetWhitelistedCommands()
                          .stream()
                          .filter((whitelistedCommand1) -> whitelistedCommand1.command().equalsIgnoreCase(command))
                          .findFirst()
                          .orElse(null);
    }
}
