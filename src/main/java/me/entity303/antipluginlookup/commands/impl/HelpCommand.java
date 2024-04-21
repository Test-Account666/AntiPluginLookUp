package me.entity303.antipluginlookup.commands.impl;

import me.entity303.antipluginlookup.commands.Command;
import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.command.CommandSender;

import java.util.List;

public class HelpCommand extends Command {

    public HelpCommand(AntiPluginLookUp plugin) {
        super(plugin, plugin.GetMessageReader().GetMessage("HelpDescription").replace("%break%", "\n"), "help", "help");
    }

    @Override
    public void Execute(CommandSender commandSender, String commandLabel, String... arguments) {
        if (!commandSender.hasPermission("antipluginlookup.help")) {
            this.SendNoPermissionsMessage(commandSender);
            return;
        }

        for (var command : this.plugin.GetCommandMap().values())
            this.plugin.SendMessage(commandSender, this.plugin.GetPrefix() + "&2/" + commandLabel + " " + command.GetSyntax() + " &8-> &2" +
                                                   command.GetDescription().replace("%sender%", commandSender.getName()));
    }

    @Override
    public List<String> OnTabComplete(CommandSender commandSender, String commandLabel, String... arguments) {
        return List.of();
    }
}
