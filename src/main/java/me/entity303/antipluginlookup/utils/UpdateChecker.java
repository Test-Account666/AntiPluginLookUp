package me.entity303.antipluginlookup.utils;

import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.Bukkit;

import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {
    private final AntiPluginLookUp plugin;
    private final String resourceId;

    public UpdateChecker(AntiPluginLookUp plugin, String resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void GetVersion(Consumer<String> consumer) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            try (var inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openStream()) {
                try (var scanner = new Scanner(inputStream)) {
                    while (scanner.hasNext())
                        consumer.accept(scanner.next());
                } finally {
                    if (inputStream != null)
                        inputStream.close();
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                this.plugin.Error("Cannot look for updates: " + throwable.getMessage());
            }

        }, 60L);
    }
}
