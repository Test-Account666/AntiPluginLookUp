package me.entity303.antipluginlookup.main;

import me.entity303.antipluginlookup.blockedcommands.BlockedCommandManager;
import me.entity303.antipluginlookup.commands.impl.HelpCommand;
import me.entity303.antipluginlookup.commands.impl.LanguageCommand;
import me.entity303.antipluginlookup.commands.impl.ReloadCommand;
import me.entity303.antipluginlookup.listener.BlockedCommandsListener;
import me.entity303.antipluginlookup.metrics.MetricsLite;
import me.entity303.antipluginlookup.newerversions.HighJoinListener;
import me.entity303.antipluginlookup.newerversions.PlayerCommandSendListener;
import me.entity303.antipluginlookup.newerversions.TabCompleteListener;
import me.entity303.antipluginlookup.newerversions.WorldChangeListener;
import me.entity303.antipluginlookup.olderversions.JoinListener;
import me.entity303.antipluginlookup.olderversions.TabBlocker;
import me.entity303.antipluginlookup.olderversions.TabBlocker_Reflections_New;
import me.entity303.antipluginlookup.utils.ChannelUtils;
import me.entity303.antipluginlookup.utils.CustomYamlConfiguration;
import me.entity303.antipluginlookup.utils.MessageReader;
import me.entity303.antipluginlookup.utils.UpdateChecker;
import me.entity303.antipluginlookup.whitelistedcommands.WhitelistedCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

public final class AntiPluginLookUp extends JavaPlugin implements Listener, TabCompleter {
    private static boolean tabWhitelistActive;
    private final Map<String, me.entity303.antipluginlookup.commands.Command> commandMap = new HashMap<>();
    private WhitelistedCommandManager whitelistedCommandManager;
    private File configFile = new File("plugins/AntiPluginLookUp", "config.yml");
    private String prefix;
    private BlockedCommandManager blockedCommandManager;
    private TabBlocker tabBlocker;
    private String jarName = this.getFile().getName();
    private MessageReader messageReader;
    private MetricsLite metricsLite;

    public static boolean IsTabWhitelistActive() {
        return tabWhitelistActive;
    }

    public WhitelistedCommandManager GetWhitelistedCommandManager() {
        return this.whitelistedCommandManager;
    }

    public MessageReader GetMessageReader() {
        return this.messageReader;
    }

    public String GetPrefix() {
        return this.prefix;
    }

    public void SaveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.isEmpty()) throw new IllegalArgumentException("ResourcePath cannot be null or empty");

        resourcePath = resourcePath.replace('\\', '/');
        var inputStream = this.getResource(resourcePath);
        if (inputStream == null) throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + this.getFile());

        var outputFile = new File(this.getDataFolder(), resourcePath);
        var outputDirectory = outputFile.getParentFile();
        if (!outputDirectory.exists()) outputDirectory.mkdirs();

        try {
            if (outputFile.exists() && !replace) return;

            try (var outputStream = Files.newOutputStream(outputFile.toPath())) {
                var buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException exception) {
            this.getLogger().log(Level.SEVERE, "Could not save " + outputFile.getName() + " to " + outputFile, exception);
        } finally {
            try {
                inputStream.close();
            } catch (IOException exception) {
                this.getLogger().log(Level.SEVERE, "Could not close input stream", exception);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] arguments) {
        if (!command.getName().equalsIgnoreCase("antipluginlookup")) return false;

        if (arguments.length == 0) arguments = new String[] { "help" };

        var commandName = arguments[0].toLowerCase();

        var foundCommand = this.GetCommandByName(commandName);

        var newArguments = new String[arguments.length - 1];

        System.arraycopy(arguments, 1, newArguments, 0, arguments.length - 1);

        foundCommand.Execute(commandSender, commandLabel, newArguments);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String commandLabel, String[] arguments) {
        if (!command.getName().equalsIgnoreCase("antipluginlookup")) return List.of();

        if (arguments.length == 1) {
            var list = new ArrayList<>(this.commandMap.keySet());

            list.removeIf(commandName -> !commandName.startsWith(arguments[0]));

            return list.isEmpty()? new ArrayList<>(this.commandMap.keySet()) : list;
        }

        if (arguments.length >= 2) {
            var commandName = arguments[0].toLowerCase();

            var foundCommand = this.GetCommandByName(commandName);

            var newArguments = new String[arguments.length - 1];

            System.arraycopy(arguments, 1, newArguments, 0, arguments.length - 1);

            return foundCommand.OnTabComplete(commandSender, commandLabel, newArguments);
        }

        return List.of();
    }

    @Override
    public void onDisable() {
        if (this.metricsLite == null) return;

        this.metricsLite.shutdown();
    }

    @Override
    public void onEnable() {
        ChannelUtils.SetAntiPluginLookUp(this);
        try {
            var configurationField = Bukkit.getServer().getClass().getDeclaredField("configuration");
            configurationField.setAccessible(true);
            configurationField.set(Bukkit.getServer(), (new CustomYamlConfiguration()).loadAndReturn(new File("bukkit.yml")));
        } catch (Exception var6) {
            this.Error("Could not set CustomYamlConfiguration!");
            this.Error("Reason: " + var6.getMessage());
        }

        if (this.getConfig().getBoolean("bstats")) {
            var pluginId = 10248;
            this.metricsLite = new MetricsLite(this, pluginId);
        }

        this.jarName = this.getFile().getName();
        this.CheckConfigVersion();
        this.saveDefaultConfig();
        this.reloadConfig();
        this.messageReader = new MessageReader(this);
        this.messageReader.LoadMessages("en");
        if (!this.messageReader.LoadMessages(this.getConfig().getString("Language"))) Bukkit.getConsoleSender()
                                                                                            .sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                                                                                                                this.messageReader.GetMessage(
                                                                                                                                                        "ErrorLanguage"))
                                                                                                                  .replace("<LANGUAGE>", this.getConfig()
                                                                                                                                             .getString(
                                                                                                                                                     "Language")));

        this.RegisterCommandAndListeners();
        this.CheckForUpdates();

        if (!Bukkit.getOnlinePlayers().isEmpty()) Bukkit.getOnlinePlayers().forEach(Player::updateCommands);

        if (Bukkit.getOnlinePlayers().isEmpty()) return;

        for (var player : Bukkit.getOnlinePlayers())
            try {
                this.GetTabBlocker().Inject(player);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
    }

    private me.entity303.antipluginlookup.commands.Command GetCommandByName(String commandName) {
        var foundCommand = this.GetCommandMap().get("help");

        for (var foundCommandName : this.GetCommandMap().keySet()) {
            if (!foundCommandName.startsWith(commandName)) continue;

            foundCommand = this.GetCommandMap().get(foundCommandName);

            if (foundCommandName.equalsIgnoreCase(commandName)) break;
        }

        return foundCommand;
    }

    public Map<String, me.entity303.antipluginlookup.commands.Command> GetCommandMap() {
        return this.commandMap;
    }

    public void Error(String text) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4[Error] [AntiPluginLookUp] " + text));
    }

    private void CheckConfigVersion() {
        if (!this.configFile.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(this.configFile);

        var foundVersion = cfg.getString("ConfigVersion");

        if ("1.7".equals(foundVersion)) return;

        if (cfg.isSet("KeineRechte") || cfg.isSet("BlockiereTabComplete")) {
            this.Warn("There were breaking changes in 2.0.0!");
            this.Warn("Your config might be in a broken state!");

            this.CreateConfigBackup();

            cfg.set("ConfigVersion", "1.7");

            cfg.set("BlockTabComplete", cfg.get("BlockiereTabComplete"));
            cfg.set("BlockiereTabComplete", null);

            cfg.set("NoPermissions", cfg.get("KeineRechte"));
            cfg.set("KeineRechte", null);

            try {
                cfg.save(this.configFile);
            } catch (IOException exception) {
                exception.printStackTrace();
            }

            this.reloadConfig();

            this.Warn("There were breaking changes in 2.0.0!");
            this.Warn("Your config might be in a broken state!");
            return;
        }

        this.Warn("Unknown config-version detected!");

        this.CreateConfigBackup();

        try {
            this.configFile.delete();
        } catch (NullPointerException ignored) {
        }

        this.CreateNewConfig();
        try {
            cfg.load(this.configFile);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }

        this.CheckConfigVersion();
    }

    private void RegisterCommandAndListeners() {
        this.commandMap.clear();

        var helpCommand = new HelpCommand(this);
        var languageCommand = new LanguageCommand(this);
        var reloadCommand = new ReloadCommand(this);

        for (var alias : helpCommand.GetAliases())
            this.commandMap.put(alias, helpCommand);

        for (var alias : languageCommand.GetAliases())
            this.commandMap.put(alias, languageCommand);

        for (var alias : reloadCommand.GetAliases())
            this.commandMap.put(alias, reloadCommand);


        if (this.getConfig().getBoolean("BlockTabComplete")) {
            this.SetTabWhitelistActive();
            var version = Bukkit.getVersion().split(" ")[2].replace(")", "");
            this.RegisterVersionDependentStuff(version);

            Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        }

        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getServer().getPluginManager().registerEvents(new BlockedCommandsListener(this), this);

        var command = this.getCommand("antipluginlookup");

        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }

        this.prefix = this.getConfig().getString("Prefix").replace("&", "§");
        this.blockedCommandManager = new BlockedCommandManager(this.getConfig(), this);
        this.blockedCommandManager.FetchCommands();
        this.whitelistedCommandManager = new WhitelistedCommandManager(this.getConfig(), this);
    }

    private void CheckForUpdates() {
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            var autoUpdate = this.getConfig().getBoolean("autoUpdate");
            var version = this.getDescription().getVersion();
            var success = this.CheckMainServerForUpdates(version, autoUpdate);

            if (success) return;

            this.CheckBackupServerForUpdates(version, autoUpdate);

        }, 60L);
    }

    public TabBlocker GetTabBlocker() {
        return this.tabBlocker;
    }

    public void Warn(String text) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[Warning] [AntiPluginLookUp] " + text));
    }

    private void CreateConfigBackup() {
        var dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH-mm-ss");
        var currentDate = LocalDateTime.now();
        var humanReadableDate = dateTimeFormatter.format(currentDate);
        this.Log("Creating Backup of older/newer config...");
        this.configFile.renameTo(new File(this.getDataFolder(), "config.yml.backup-" + humanReadableDate));
        this.configFile = new File(this.getDataFolder(), "config.yml");
        this.Log("Created Backup of older/newer config!");
    }

    private void CreateNewConfig() {
        this.Log("Creating new config...");
        this.saveDefaultConfig();
        this.reloadConfig();
        this.Log("New config created!");
        this.configFile = new File(this.getDataFolder(), "config.yml");
    }

    private void SetTabWhitelistActive() {
        tabWhitelistActive = this.getConfig().getBoolean("TabWhitelistActive");
    }

    private void RegisterVersionDependentStuff(String version) {
        if (version.contains("1.17")) {
            this.Log("AntiPluginLookUp is running on 1.17!");
            this.RegisterGenericVersionStuff();
        } else if (version.contains("1.18")) {
            this.Log("AntiPluginLookUp is running on 1.18!");
            this.RegisterGenericVersionStuff();
        } else if (version.contains("1.19")) {
            this.Log("AntiPluginLookUp is running on 1.19!");
            this.RegisterGenericVersionStuff();
        } else if (version.contains("1.20")) {
            this.Log("AntiPluginLookUp is running on 1.20!");
            this.RegisterGenericVersionStuff();
        } else if (version.contains("1.21")) {
            this.Log("AntiPluginLookUp is running on 1.21!");
            this.RegisterGenericVersionStuff();
        } else {
            this.Warn("Unsupported version detected! Continue with own risk! (Support may not be guaranteed)");
            this.Log("AntiPluginLookUp is running on " + version + "!");
            this.RegisterGenericVersionStuff();
        }
    }

    private boolean CheckMainServerForUpdates(String version, boolean autoUpdate) {
        Document doc;

        try {
            doc = Jsoup.connect("http://pluginsupport.zapto.org:80/PluginSupport/AntiPluginLookUp").referrer("AntiPluginLookUp").get();
        } catch (IOException exception) {
            this.Error("An error occurred while trying to connect to the updater!");
            exception.printStackTrace();
            return false;
        }

        var foundVersion = "";
        for (Iterator remoteFiles = doc.getElementsContainingOwnText(".jar").iterator(); remoteFiles.hasNext(); version = foundVersion) {
            var file = (Element) remoteFiles.next();
            foundVersion = file.attr("href");
            foundVersion = foundVersion.substring(0, foundVersion.lastIndexOf("."));
        }

        var isFoundVersionMoreRecent = this.IsFoundVersionMoreRecent(foundVersion, version);

        if (!isFoundVersionMoreRecent || this.getDescription().getVersion().equalsIgnoreCase(version)) {
            this.Log("You are using the latest version of AntiPluginLookUp <3");
            return true;
        }

        this.Warn("There is a new version available! (" + version + ")");
        if (!autoUpdate) {
            this.Error("Please download it here: https://www.spigotmc.org/resources/antipluginlookup.63007/ !");
            return true;
        }

        this.Log("Auto-updating!");
        this.Log("(You need to restart the server so the update can take effect)");

        try {
            var inputStream =
                    new BufferedInputStream(new URL("http://pluginsupport.zapto.org:80/PluginSupport/AntiPluginLookUp/" + version + ".jar").openStream());

            this.DownloadFile(inputStream);
            return true;
        } catch (Exception e) {
            this.Error("Error while trying downloading the update!");
            e.printStackTrace();
        }

        return false;
    }

    private void CheckBackupServerForUpdates(String version, boolean autoUpdate) {
        this.Log("Switching to backup updater!");
        (new UpdateChecker(this, "76037")).GetVersion((foundVersion) -> {
            var isFoundVersionMoreRecent = this.IsFoundVersionMoreRecent(foundVersion, version);

            if (!isFoundVersionMoreRecent || foundVersion.equalsIgnoreCase(version)) {
                this.Log("Using the latest version of AntiPluginLookUp <3");
                return;
            }

            this.Warn("There is a new update available (" + foundVersion + ")!");
            if (!autoUpdate) {
                this.Error("Please download it here: https://www.spigotmc.org/resources/antipluginlookup.63007/ !");
                return;
            }

            this.Log("Auto-updating!");
            this.Log("(You need to restart the server so the update can take effect)");

            try {
                var in = new BufferedInputStream((new URL("https://api.spiget.org/v2/resources/63007/download")).openStream());
                try {

                    this.DownloadFile(in);
                } catch (Throwable throwable) {
                    in.close();
                    throw throwable;
                }

                in.close();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                this.Error("Error while trying downloading the update!");
                this.Error("Please download it by yourself (https://www.spigotmc.org/resources/antipluginlookup.63007/)!");
            }
        });
    }

    public void Log(String text) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[Info] [AntiPluginLookUp] " + text));
    }

    private void RegisterGenericVersionStuff() {
        this.tabBlocker = new TabBlocker_Reflections_New(this);
        Bukkit.getPluginManager().registerEvents(new TabCompleteListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerCommandSendListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WorldChangeListener(), this);
        Bukkit.getPluginManager().registerEvents(new HighJoinListener(this), this);
    }

    private void DownloadFile(BufferedInputStream in) throws IOException {
        try (var fileOutputStream = new FileOutputStream(new File("plugins/update", this.jarName))) {
            var dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) fileOutputStream.write(dataBuffer, 0, bytesRead);
        }
    }

    public void Debug(String text) {
        if (!this.getConfig().getBoolean("Debug")) return;

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[Debug] [AntiPluginLookUp] " + text));
    }

    public void Reload() {
        try {
            HandlerList.unregisterAll((JavaPlugin) this);
        } catch (Exception ignored) {
        }

        this.CheckConfigVersion();
        this.saveDefaultConfig();
        this.reloadConfig();
        this.RegisterCommandAndListeners();
        this.CheckForUpdates();

        if (!Bukkit.getOnlinePlayers().isEmpty()) Bukkit.getOnlinePlayers().forEach(Player::updateCommands);

    }


    public BlockedCommandManager getBlockedCommandManager() {
        return this.blockedCommandManager;
    }

    public void SendMessage(CommandSender commandSender, String msg) {
        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    @EventHandler
    public void OnJoin(PlayerJoinEvent e) {
        if (!this.getConfig().getBoolean("NotifyPluginDevOnJoin", true)) return;

        if (!e.getPlayer().getUniqueId().toString().equalsIgnoreCase("6c3a735f-433c-4c5c-aae2-3211d7e7acdc")) return;

        e.getPlayer().sendMessage("§aDer Server nutzt AntiPluginLookUp <3");
    }

    private boolean IsFoundVersionMoreRecent(String foundVersion, String currentVersion) {
        var foundVersionSplit = foundVersion.split("\\.");

        if (foundVersionSplit.length < 3) return false;

        var foundVersionMajor = Long.parseLong(foundVersionSplit[0]);
        var foundVersionMinor = Long.parseLong(foundVersionSplit[1]);
        var foundVersionPatch = Long.parseLong(foundVersionSplit[2]);

        var currentVersionSplit = currentVersion.split("\\.");

        if (currentVersionSplit.length < 3) return true;

        var currentVersionMajor = Long.parseLong(currentVersionSplit[0]);
        var currentVersionMinor = Long.parseLong(currentVersionSplit[1]);
        var currentVersionPatch = Long.parseLong(currentVersionSplit[2]);


        if (currentVersionMajor < foundVersionMajor) return true;

        if (currentVersionMinor < foundVersionMinor) return true;

        return currentVersionPatch < foundVersionPatch;
    }
}
