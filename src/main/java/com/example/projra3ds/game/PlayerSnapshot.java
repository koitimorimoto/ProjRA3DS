package com.example.projra3ds.game;

public record PlayerSnapshot(Long id, String name, int finishing, int goalkeeping) {
    public PlayerSnapshot {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O jogador precisa ter nome.");
        }
        if (finishing < 0 || finishing > 100 || goalkeeping < 0 || goalkeeping > 100) {
            throw new IllegalArgumentException("As habilidades do jogador devem estar entre 0 e 100.");
        }
    }
}
