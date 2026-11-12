package com.example.projra3ds.game;

import com.example.projra3ds.model.Team;

import java.util.List;

public record TeamSnapshot(Long id, String name, List<PlayerSnapshot> batters,
                           PlayerSnapshot goalkeeper, String goalkeeperColor, String batterColor) {
    public TeamSnapshot(Long id, String name, List<PlayerSnapshot> batters, PlayerSnapshot goalkeeper) {
        this(id, name, batters, goalkeeper, "#FFD54F", "#1565C0");
    }

    public TeamSnapshot {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O time precisa ter nome.");
        }
        batters = List.copyOf(batters);
        if (batters.isEmpty()) {
            throw new IllegalArgumentException("O time precisa ter ao menos um batedor.");
        }
        if (goalkeeper == null) {
            throw new IllegalArgumentException("O time precisa ter um goleiro definido.");
        }
        goalkeeperColor = Team.normalizeColor(goalkeeperColor, "goleiro");
        batterColor = Team.normalizeColor(batterColor, "batedores");
    }

    public boolean isGoalkeeper(PlayerSnapshot player) {
        return goalkeeper.equals(player);
    }

}
