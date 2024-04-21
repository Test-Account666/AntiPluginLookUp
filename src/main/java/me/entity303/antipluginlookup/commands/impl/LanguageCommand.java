package me.entity303.antipluginlookup.commands.impl;

import me.entity303.antipluginlookup.commands.Command;
import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LanguageCommand extends Command {

    public LanguageCommand(AntiPluginLookUp plugin) {
        super(plugin, plugin.GetMessageReader().GetMessage("LanguageDescription").replace("%break%", "\n"), "language <Language>",
              "language");
    }

    @Override
    public void Execute(CommandSender commandSender, String commandLabel, String... arguments) {
        if (!commandSender.hasPermission("antipluginlookup.language")) {
            this.SendNoPermissionsMessage(commandSender);
            return;
        }

        if (!this.plugin.GetMessageReader().LoadMessages(arguments[0])) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.plugin.GetPrefix() +
                                                                                  this.plugin.GetMessageReader().GetMessage("ErrorLanguage"))
                                               .replace("<LANGUAGE>", arguments[0]));
            return;
        }

        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.plugin.GetPrefix() +
                                                                              this.plugin.GetMessageReader().GetMessage("ChangedLanguage")));
    }

    @Override
    public List<String> OnTabComplete(CommandSender commandSender, String commandLabel, String... arguments) {
        if (arguments.length == 0)
            return List.of();

        if (arguments.length > 1)
            return List.of();

        var pluginDirectory = new File("plugins" + File.separator + "AntiPluginLookUp");

        if (!pluginDirectory.isDirectory())
            return List.of();

        var fileList = pluginDirectory.listFiles();

        if (fileList == null)
            return List.of();

        var possibleLanguageCodes = new ArrayList<String>();

        for (var file : fileList) {
            if (!file.getName().toLowerCase().startsWith("messages_"))
                continue;

            if (!file.getName().toLowerCase().endsWith(".yml"))
                continue;

            possibleLanguageCodes.add(file.getName().substring("messages_".length(), file.getName().length() - ".yml".length()));
        }

        var completedLanguageCodes = new ArrayList<>(possibleLanguageCodes);

        completedLanguageCodes.removeIf(language -> !language.startsWith(arguments[0]));

        return completedLanguageCodes.isEmpty()? possibleLanguageCodes : completedLanguageCodes;
    }
}
