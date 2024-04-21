package me.entity303.antipluginlookup.whitelistedcommands;

import java.util.List;

public record WhitelistedCommand(String command, String permission, boolean permissionNeeded, boolean blockArguments, List<String> worlds) {

    public boolean IsGlobal() {
        return this.worlds.contains("global");
    }
}
