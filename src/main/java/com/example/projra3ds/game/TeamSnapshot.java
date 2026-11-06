package com.example.projra3ds.game;

import java.util.List;

public record TeamSnapshot(Long id, String name, List<PlayerSnapshot> batters,
                           PlayerSnapshot goalkeeper) {
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
    }
}
