package org.palata_tpsandping.palata_tpsandping;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

public class ConsoleCommandListener implements Listener {
    private String result;

    @EventHandler
    public void onConsoleCommand(ServerCommandEvent event) {
        result = event.getCommand();
    }

    public String getResult() {
        return result;
    }
}
