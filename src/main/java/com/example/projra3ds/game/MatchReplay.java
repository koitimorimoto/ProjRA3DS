package com.example.projra3ds.game;

import java.util.List;

/**
 * Dados de uma partida salva, já convertidos para os tipos do motor de jogo, prontos para
 * serem reproduzidos pela tela de histórico.
 */
public record MatchReplay(TeamSnapshot userTeam, TeamSnapshot cpuTeam,
                          List<PenaltyAttempt> attempts) {
    public MatchReplay {
        attempts = List.copyOf(attempts);
    }
}
