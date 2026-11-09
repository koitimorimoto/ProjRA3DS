package com.example.projra3ds.game;

import com.example.projra3ds.model.Player;

public record PlayerSnapshot(Long id, String name, int finishing, int goalkeeping,
                             double heightMeters) {
    public PlayerSnapshot(Long id, String name, int finishing, int goalkeeping) {
        this(id, name, finishing, goalkeeping, Player.DEFAULT_HEIGHT_METERS);
    }

    public PlayerSnapshot {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O jogador precisa ter nome.");
        }
        if (!Player.isSkillInRange(finishing) || !Player.isSkillInRange(goalkeeping)) {
            throw new IllegalArgumentException("As habilidades do jogador devem estar entre 0 e 100.");
        }
        Player.validateHeightMeters(heightMeters);
    }
}
