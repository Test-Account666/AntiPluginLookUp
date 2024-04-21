package me.entity303.antipluginlookup.commands;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface ICommand {

    AntiPluginLookUp GetPlugin();

    String GetDescription();

    String GetName();

    String GetSyntax();

    String[] GetAliases();

    void Execute(CommandSender commandSender, String commandLabel, String... arguments);

    List<String> OnTabComplete(CommandSender commandSender, String commandLabel, String... arguments);
}
