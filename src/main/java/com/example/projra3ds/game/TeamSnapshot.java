package com.example.projra3ds.game;

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
        goalkeeperColor = validateColor(goalkeeperColor, "goleiro");
        batterColor = validateColor(batterColor, "batedores");
    }

    public boolean isGoalkeeper(PlayerSnapshot player) {
        return goalkeeper.equals(player);
    }

    private static String validateColor(String color, String role) {
        if (color == null || !color.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("A cor do " + role + " deve estar no formato #RRGGBB.");
        }
        return color.toUpperCase(java.util.Locale.ROOT);
    }
}
