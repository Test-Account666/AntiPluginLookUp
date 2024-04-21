package me.entity303.antipluginlookup.commands;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public abstract class Command implements ICommand {
    protected final AntiPluginLookUp plugin;
    private final String description;
    private final String name;
    private final String syntax;
    private final String[] aliases;

    protected Command(AntiPluginLookUp plugin, String description, String syntax, String... aliases) {
        this.plugin = plugin;
        this.description = description;
        this.syntax = syntax;
        this.name = aliases[0];
        this.aliases = aliases;
    }

    @Override
    public AntiPluginLookUp GetPlugin() {
        return this.plugin;
    }

    @Override
    public String GetDescription() {
        return this.description;
    }

    @Override
    public String GetName() {
        return this.name;
    }

    @Override
    public String GetSyntax() {
        return this.syntax;
    }

    @Override
    public String[] GetAliases() {
        return this.aliases;
    }

    protected void SendNoPermissionsMessage(CommandSender sender) {
        if ("null".equals(this.plugin.getConfig().getString("NoPermissions"))) {
            sender.sendMessage("Unknown command. Type \"/help\" for help.");
            return;
        }

        var noPermissionsMessage = this.plugin.getConfig().getString("NoPermissions");

        assert noPermissionsMessage != null;
        noPermissionsMessage = noPermissionsMessage.replace("%player%", sender.getName()).replace("%break%", "\n");

        noPermissionsMessage = ChatColor.translateAlternateColorCodes('&', noPermissionsMessage);

        sender.sendMessage(noPermissionsMessage);
    }
}
