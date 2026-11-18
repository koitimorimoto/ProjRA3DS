package com.example.projra3ds.game;

import java.util.List;

public record MatchReplay(TeamSnapshot userTeam, TeamSnapshot cpuTeam,
                          List<PenaltyAttempt> attempts) {
    public MatchReplay {
        attempts = List.copyOf(attempts);
    }
}
