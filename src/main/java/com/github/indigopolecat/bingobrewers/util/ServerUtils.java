package com.github.indigopolecat.bingobrewers.util;

import lombok.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerUtils {
    /**
     * Check (via the scoreboard) if the current player is in a bingo profile
     * @return true if the player is playing in a bingo profile
     */
    public static boolean isBingo() {
        if(Minecraft.getInstance().level == null) return false;
        var scoreboard = Minecraft.getInstance().level.getScoreboard();
        for(var slot : DisplaySlot.values()) {
            var objective = scoreboard.getDisplayObjective(slot);
            if(objective == null) continue;
            for(var line : scoreboard.listPlayerScores(objective)) {
                String playerName = line.owner();
                
                PlayerTeam team = scoreboard.getPlayersTeam(playerName);
                
                String text = PlayerTeam.formatNameForTeam(team, Component.nullToEmpty(playerName)).getString();
                if(text.contains("ⓑ") || text.contains("bingo") || text.contains("Ⓑ")) return true; //'ⓑ' is the bingo tag //matita: 'Ⓑ' seems the correct one
            }
        }
        return false;
    }
}
