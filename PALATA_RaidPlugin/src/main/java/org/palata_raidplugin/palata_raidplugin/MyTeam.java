package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

public class MyTeam {
    private String teamName;
    private Player captain;
    private List<String> members;

    public MyTeam(String teamName, Player captain) {
        this.teamName = teamName;
        this.captain = captain;
        this.members = new ArrayList<>();
    }

    public String getTeamName() {
        return teamName;
    }

    public Player getCaptain() {
        return captain;
    }

    public List<String> getMembers() {
        return members;
    }

    public void addMember(String playerName) {
        members.add(playerName);
    }
}

