package me.entity303.antipluginlookup.blockedcommands;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Field;
import java.util.*;

public class BlockedCommandManager {

    private final AntiPluginLookUp plugin;
    private final List<BlockedCommand> blockedCommandList = new ArrayList<>();
    private final FileConfiguration cfg;
    private Field commandMapField = null;
    private Field knownCommandsField = null;

    public BlockedCommandManager(FileConfiguration cfg, AntiPluginLookUp plugin) {
        this.cfg = cfg;
        this.plugin = plugin;
    }

    public void FetchCommands() {
        if (!this.blockedCommandList.isEmpty())
            this.blockedCommandList.clear();

        this.FetchPluginCommands();

        this.cfg.getConfigurationSection("BlockedCommands").getKeys(false).forEach((command) -> {
            var configPath = "BlockedCommands." + command + ".";

            var blockedCommand = this.CreateBlockedCommand(command, configPath);

            this.AddCommand(blockedCommand);
        });

        this.cfg.getConfigurationSection("BlockedCommandsGroups").getKeys(false).forEach((group) -> {
            var configPath = "BlockedCommandsGroups." + group + ".";
            var commandsConfigPath = configPath + "Commands";
            var commandsString = this.cfg.getString(commandsConfigPath).replace(" ", "");
            var commands = Arrays.asList(commandsString.split(","));

            commands.forEach(command -> {
                var blockedCommand = this.CreateBlockedCommand(command, configPath);
                this.AddCommand(blockedCommand);
            });
        });
    }

    private void FetchPluginCommands() {
        for (var pluginName : this.cfg.getConfigurationSection("BlacklistPlugins").getKeys(false)) {
            var plugin = Arrays.stream(Bukkit.getPluginManager().getPlugins())
                               .filter(pl -> pl.getName().equalsIgnoreCase(pluginName))
                               .findFirst()
                               .orElse(null);

            if (plugin == null) {
                AntiPluginLookUp.getPlugin(AntiPluginLookUp.class).Error("Couldn't find any plugin named '" + pluginName + "'!");
                continue;
            }

            var configPath = "BlacklistPlugins." + pluginName + ".";
            var message = this.cfg.getString(configPath + "Message");
            var permission = this.cfg.getString(configPath + "Permission");
            var worldsString = this.cfg.getString(configPath + "Worlds");
            List worlds = Arrays.asList(worldsString.toLowerCase().split(","));

            for (var command : Objects.requireNonNull(this.GetKnownCommands()).entrySet()) {
                var cmd = command.getValue();

                if (!command.getKey().contains(":"))
                    continue;

                if (!command.getKey().split(":")[0].equalsIgnoreCase(pluginName))
                    continue;

                var blockedCommand = new BlockedCommand(cmd.getName(), permission, message, true, false, false, false, worlds);
                var blockedCommand2 =
                        new BlockedCommand(pluginName.toLowerCase(Locale.ROOT) + ":" + cmd.getName(), permission, message, true, false,
                                           false, false, worlds);
                this.AddCommand(blockedCommand);
                this.AddCommand(blockedCommand2);
            }
        }
    }

    private BlockedCommand CreateBlockedCommand(String command, String configPath) {
        var sendMessage = this.cfg.getBoolean(configPath + "SendMessage");
        var message = this.cfg.getString(configPath + "Message");
        var permission = this.cfg.getString(configPath + "Permission");
        var sendAdvertisement = this.cfg.getBoolean(configPath + "SendAdvertisement");
        var bukkit = this.cfg.getBoolean(configPath + "Bukkit");
        var minecraft = this.cfg.getBoolean(configPath + "Minecraft");
        var worldsString = this.cfg.getString(configPath + "Worlds");
        worldsString = worldsString.replace(" ", "");
        var worlds = Arrays.asList(worldsString.toLowerCase().split(","));

        return new BlockedCommand(command, permission, message, sendMessage, bukkit, minecraft, sendAdvertisement, worlds);
    }

    public void AddCommand(BlockedCommand command) {
        this.plugin.Debug("Added Blocked Command: " + command.command());
        this.blockedCommandList.add(command);
    }

    private Map<String, Command> GetKnownCommands() {
        if (this.commandMapField == null)
            try {
                this.commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                this.commandMapField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                return null;
            }
        SimpleCommandMap commandMap;
        try {
            commandMap = (SimpleCommandMap) this.commandMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (this.knownCommandsField == null)
            try {
                this.knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                this.knownCommandsField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                return null;
            }

        Map<String, Command> knownCommands;

        try {
            knownCommands = (Map<String, Command>) this.knownCommandsField.get(commandMap);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }

        return knownCommands;
    }

    public BlockedCommand GetBlockedCommand(String command) {
        if (this.blockedCommandList.isEmpty())
            return null;
        else {
            command = command.replaceFirst("/", "");
            var finalCommand = command;
            return this.blockedCommandList.stream().filter((cmd) -> cmd.command().equalsIgnoreCase(finalCommand)).findFirst().orElse(null);
        }
    }

    public boolean IsBlockedCommand(String command) {
        if (this.blockedCommandList.isEmpty())
            return false;

        command = command.replaceFirst("/", "");
        var finalCommand = command;

        return this.blockedCommandList.stream().anyMatch((blockedCommand) -> blockedCommand.command().equalsIgnoreCase(finalCommand));
    }
}
