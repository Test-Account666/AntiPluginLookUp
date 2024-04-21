package me.entity303.antipluginlookup.utils;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class CustomYamlConfiguration extends YamlConfiguration {
    public CustomYamlConfiguration loadAndReturn(File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        return this;
    }

    @Override
    public boolean getBoolean(String path) {
        return !path.equalsIgnoreCase("settings.query-plugins") && !path.equalsIgnoreCase("query-plugins") && super.getBoolean(path);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return !path.equalsIgnoreCase("settings.query-plugins") && !path.equalsIgnoreCase("query-plugins") && super.getBoolean(path, def);
    }
}
