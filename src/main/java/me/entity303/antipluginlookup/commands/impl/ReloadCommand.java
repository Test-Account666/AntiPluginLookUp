package me.entity303.antipluginlookup.commands.impl;

import me.entity303.antipluginlookup.commands.Command;
import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Objects;

public class ReloadCommand extends Command {

    public ReloadCommand(AntiPluginLookUp plugin) {
        super(plugin, plugin.GetMessageReader().GetMessage("ReloadDescription").replace("%break%", "\n"), "reload", "reload");
    }

    @Override
    public void Execute(CommandSender commandSender, String commandLabel, String... arguments) {
        if (!commandSender.hasPermission("antipluginlookup.reload")) {
            this.SendNoPermissionsMessage(commandSender);
            return;
        }

        this.plugin.reloadConfig();
        this.plugin.Reload();

        if (!this.plugin.GetMessageReader().LoadMessages(this.plugin.getConfig().getString("Language"))) {
            var languageNotFoundMessage = this.plugin.GetPrefix() + this.plugin.GetMessageReader().GetMessage("ErrorLanguage");

            languageNotFoundMessage =
                    languageNotFoundMessage.replace("<LANGUAGE>", Objects.requireNonNull(this.plugin.getConfig().getString("Language")));

            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', languageNotFoundMessage));
            return;
        }

        var configReloadedMessage = this.plugin.GetPrefix() + this.plugin.GetMessageReader().GetMessage("ConfigReloaded");

        configReloadedMessage = ChatColor.translateAlternateColorCodes('&', configReloadedMessage);

        commandSender.sendMessage(configReloadedMessage);
    }

    @Override
    public List<String> OnTabComplete(CommandSender commandSender, String commandLabel, String... arguments) {
        return List.of();
    }
}
