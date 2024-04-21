package me.entity303.antipluginlookup.utils;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageReader {
    private FileConfiguration cfg;

    public MessageReader(AntiPluginLookUp plugin) {
        plugin.SaveResource("messages_en.yml", false);
        plugin.SaveResource("messages_de.yml", false);
        plugin.SaveResource("messages_fa.yml", false);
        plugin.SaveResource("messages_it.yml", false);
    }

    public boolean LoadMessages(String langCode) {
        var file = new File("plugins//AntiPluginLookUp", "messages_" + langCode + ".yml");
        if (!file.exists())
            return false;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        if (!cfg.isSet("ChangedLanguage"))
            return false;

        if (!cfg.isSet("HelpDescription"))
            return false;

        if (!cfg.isSet("LanguageDescription"))
            return false;

        if (!cfg.isSet("ReloadDescription"))
            return false;

        if (!cfg.isSet("ConfigReloaded"))
            return false;

        if (!cfg.isSet("ErrorLanguage"))
            return false;

        this.cfg = cfg;
        return true;
    }

    public String GetMessage(String path) {
        if (!this.cfg.isSet(path))
            return "Error in file: " + path;

        var msg = this.cfg.getString(path);
        return msg == null? "Error in file: " + path : msg;
    }
}
