package me.entity303.antipluginlookup.whitelistedcommands;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WhitelistedCommandManager {
    private final AntiPluginLookUp plugin;
    private final List<WhitelistedCommand> whitelistedCommands;
    private final FileConfiguration cfg;

    public WhitelistedCommandManager(FileConfiguration cfg, AntiPluginLookUp plugin) {
        this.cfg = cfg;
        this.whitelistedCommands = new ArrayList<>();
        this.plugin = plugin;
        this.FetchCommands();
    }

    private void FetchCommands() {
        this.whitelistedCommands.clear();
        this.FetchWhitelistedCommands();
        this.FetchWhitelistedCommandsGroups();
    }

    public void FetchWhitelistedCommands() {
        if (!this.whitelistedCommands.isEmpty())
            this.whitelistedCommands.clear();

        this.FetchIndividualWhitelistedCommands();
        this.FetchWhitelistedCommandsGroups();
    }

    private void FetchIndividualWhitelistedCommands() {
        this.cfg.getConfigurationSection("WhitelistedCommands").getKeys(false).forEach(this::AddWhitelistedCommand);
    }

    private void FetchWhitelistedCommandsGroups() {
        this.cfg.getConfigurationSection("WhitelistedCommandsGroups").getKeys(false).forEach(this::AddWhitelistedCommandsGroup);
    }

    private void AddWhitelistedCommand(String command) {
        var configPath = "WhitelistedCommands." + command + ".";
        var whitelistedCommand = this.CreateWhitelistedCommand(command, configPath);
        this.AddCommand(whitelistedCommand);
    }

    private void AddWhitelistedCommandsGroup(String group) {
        var configPath = "WhitelistedCommandsGroups." + group + ".";
        var commandsConfigPath = configPath + "Commands";
        var commandsString = this.cfg.getString(commandsConfigPath).replace(" ", "");
        var commands = Arrays.asList(commandsString.split(","));

        commands.forEach(command -> {
            var whitelistedCommand = this.CreateWhitelistedCommand(command, configPath);
            this.AddCommand(whitelistedCommand);
        });
    }

    private WhitelistedCommand CreateWhitelistedCommand(String command, String configPath) {
        var permission = this.cfg.getString(configPath + "Permission");
        var permissionNeeded = this.cfg.getBoolean(configPath + "PermissionNeeded");
        var blockArguments = this.cfg.getBoolean(configPath + "BlockArguments", true);
        var worldsString = this.cfg.getString(configPath + "Worlds").replace(" ", "");
        var worlds = Arrays.asList(worldsString.toLowerCase().split(","));

        if (this.cfg.isSet(configPath + "Arguments") || this.cfg.isSet(configPath + "HasArguments")) {
            Bukkit.getLogger()
                  .warning(
                          "The 'Arguments' and 'HasArguments' sections in your configuration file have been removed. Please update your '" +
                          configPath + "' configuration for the '" + command + "' command!");

            Bukkit.getLogger()
                  .info("To enable tab-completion for arguments without using the removed 'Arguments' section, set 'BlockArguments' to " +
                        "'false'. This change simplifies your configuration and ensures compatibility with future updates.");
        }


        return new WhitelistedCommand("/" + command, permission, permissionNeeded, blockArguments, worlds);
    }


    private void AddCommand(WhitelistedCommand command) {
        this.plugin.Debug("Added Whitelisted Command: " + command.command());

        this.whitelistedCommands.add(command);
    }

    public List<WhitelistedCommand> GetWhitelistedCommands() {
        return this.whitelistedCommands;
    }

    public List<WhitelistedCommand> GetWhitelistedCommands(String command) {
        var commands = this.whitelistedCommands.stream()
                                               .filter(cmd -> cmd.command().toLowerCase().startsWith(command.toLowerCase()))
                                               .collect(Collectors.toList());
        return commands.isEmpty()? this.whitelistedCommands : commands;
    }
}
