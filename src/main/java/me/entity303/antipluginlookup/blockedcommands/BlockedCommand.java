package me.entity303.antipluginlookup.blockedcommands;

import java.util.List;

public record BlockedCommand(String command, String permission, String message, boolean sendMessage, boolean bukkit, boolean minecraft,
                             boolean advertisement, List<String> worlds) {

    public boolean isGlobal() {
        return this.worlds.contains("global");
    }
}
