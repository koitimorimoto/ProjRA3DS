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
        if (finishing < 0 || finishing > 100 || goalkeeping < 0 || goalkeeping > 100) {
            throw new IllegalArgumentException("As habilidades do jogador devem estar entre 0 e 100.");
        }
        if (!Double.isFinite(heightMeters) || heightMeters < Player.MIN_HEIGHT_METERS
                || heightMeters > Player.MAX_HEIGHT_METERS
                || Math.abs(heightMeters * 100 - Math.rint(heightMeters * 100)) > 1e-8) {
            throw new IllegalArgumentException(
                    "A altura deve estar entre 1,00 m e 2,50 m, com até duas casas decimais.");
        }
    }
}
