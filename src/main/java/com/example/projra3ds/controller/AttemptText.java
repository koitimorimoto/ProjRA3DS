package com.example.projra3ds.controller;

import com.example.projra3ds.game.GoalkeeperSide;
import com.example.projra3ds.game.PenaltyAttempt;
import com.example.projra3ds.game.ShotResult;

/** Textos de uma cobrança, compartilhados entre a tela da partida e a do histórico. */
final class AttemptText {
    private AttemptText() {
    }

    static String describe(PenaltyAttempt attempt) {
        return "Rodada " + attempt.round() + " · " + attempt.attackingTeam() + " · "
                + attempt.shooter().name() + " · " + result(attempt)
                + " · goleiro: " + side(attempt.goalkeeperSide());
    }

    static String result(PenaltyAttempt attempt) {
        if (attempt.result() == ShotResult.GOAL) {
            return "GOL";
        }
        if (attempt.result() == ShotResult.SAVED) {
            return "DEFENDIDO";
        }
        return "PARA FORA";
    }

    static String side(GoalkeeperSide side) {
        return switch (side) {
            case LEFT -> "esquerda";
            case CENTER -> "meio";
            case RIGHT -> "direita";
        };
    }
}
