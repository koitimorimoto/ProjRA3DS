package com.example.projra3ds.game;

public record PenaltyAttempt(
        int attemptNumber,
        int round,
        String attackingTeam,
        String defendingTeam,
        PlayerSnapshot shooter,
        PlayerSnapshot goalkeeper,
        double aimedX,
        double aimedY,
        double ballX,
        double ballY,
        double power,
        GoalkeeperSide goalkeeperSide,
        ShotResult result,
        boolean shooterControlledByCpu,
        boolean goalkeeperControlledByCpu) {
}
