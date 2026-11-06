package com.example.projra3ds.service;

import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import com.example.projra3ds.model.TeamLineup;
import com.example.projra3ds.persistence.JpaTransactions;
import com.example.projra3ds.repository.PlayerRepository;
import com.example.projra3ds.repository.TeamLineupRepository;
import com.example.projra3ds.repository.TeamRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TeamLineupService {
    private final TeamLineupRepository lineupRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final JpaTransactions transactions;

    public TeamLineupService() {
        this(new TeamLineupRepository(), new TeamRepository(),
                new PlayerRepository(), new JpaTransactions());
    }

    TeamLineupService(TeamLineupRepository lineupRepository, TeamRepository teamRepository,
                      PlayerRepository playerRepository, JpaTransactions transactions) {
        this.lineupRepository = lineupRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.transactions = transactions;
    }

    public TeamLineup findByTeamId(Long teamId) {
        return transactions.read(entityManager -> {
            teamRepository.findById(entityManager, teamId)
                    .orElseThrow(() -> new IllegalArgumentException("Time não encontrado."));
            return lineupRepository.findByTeamId(entityManager, teamId).orElse(null);
        });
    }

    public TeamLineup save(Long teamId, List<Long> batterIds, Long goalkeeperId) {
        if (batterIds == null) {
            throw new IllegalArgumentException("A lista de batedores não pode ser nula.");
        }
        if (new HashSet<>(batterIds).size() != batterIds.size()) {
            throw new IllegalArgumentException("Um jogador não pode aparecer mais de uma vez na lista.");
        }

        return transactions.write(entityManager -> {
            Team team = teamRepository.findById(entityManager, teamId)
                    .orElseThrow(() -> new IllegalArgumentException("Time não encontrado."));
            TeamLineup lineup = lineupRepository.findByTeamId(entityManager, teamId)
                    .orElseGet(() -> new TeamLineup(team));

            List<Player> batters = new ArrayList<>();
            for (Long batterId : batterIds) {
                Player player = playerRepository.findById(entityManager, batterId)
                        .orElseThrow(() -> new IllegalArgumentException("Batedor não encontrado."));
                validateTeamMembership(player, teamId);
                batters.add(player);
            }

            Player goalkeeper = null;
            if (goalkeeperId != null) {
                goalkeeper = playerRepository.findById(entityManager, goalkeeperId)
                        .orElseThrow(() -> new IllegalArgumentException("Goleiro não encontrado."));
                validateTeamMembership(goalkeeper, teamId);
            }

            lineup.setBatters(batters);
            lineup.setGoalkeeper(goalkeeper);
            lineupRepository.save(entityManager, lineup);
            return lineup;
        });
    }

    private void validateTeamMembership(Player player, Long teamId) {
        if (player.getTeam() == null || !player.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException(
                    player.getName() + " não pertence ao elenco selecionado.");
        }
    }
}
