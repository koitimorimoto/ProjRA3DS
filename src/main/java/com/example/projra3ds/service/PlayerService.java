package com.example.projra3ds.service;

import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import com.example.projra3ds.model.TeamLineup;
import com.example.projra3ds.persistence.JpaTransactions;
import com.example.projra3ds.repository.PlayerRepository;
import com.example.projra3ds.repository.TeamRepository;
import com.example.projra3ds.repository.TeamLineupRepository;

import java.util.List;

public class PlayerService {
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final TeamLineupRepository lineupRepository;
    private final JpaTransactions transactions;

    public PlayerService() {
        this(new PlayerRepository(), new TeamRepository(),
                new TeamLineupRepository(), new JpaTransactions());
    }

    PlayerService(PlayerRepository playerRepository, TeamRepository teamRepository,
                  TeamLineupRepository lineupRepository, JpaTransactions transactions) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.lineupRepository = lineupRepository;
        this.transactions = transactions;
    }

    public Player createPlayer(String name, int finishing, int goalkeeping, Long teamId) {
        return createPlayer(name, finishing, goalkeeping, Player.DEFAULT_HEIGHT_METERS, teamId);
    }

    public Player createPlayer(String name, int finishing, int goalkeeping,
                               double heightMeters, Long teamId) {
        String normalizedName = validateName(name);
        validateSkill(finishing, "Finalização");
        validateSkill(goalkeeping, "Goleiro");
        validateHeight(heightMeters);
        return transactions.write(entityManager -> {
            Team team = findTeamOrNull(teamId, entityManager);
            Player player = new Player(normalizedName, finishing, goalkeeping, heightMeters);
            player.setTeam(team);
            playerRepository.save(entityManager, player);
            return player;
        });
    }

    public Player updatePlayer(Long id, String name, int finishing, int goalkeeping, Long teamId) {
        return updatePlayer(id, name, finishing, goalkeeping, null, teamId);
    }

    public Player updatePlayer(Long id, String name, int finishing, int goalkeeping,
                               Double heightMeters, Long teamId) {
        String normalizedName = validateName(name);
        validateSkill(finishing, "Finalização");
        validateSkill(goalkeeping, "Goleiro");
        if (heightMeters != null) {
            validateHeight(heightMeters);
        }
        return transactions.write(entityManager -> {
            Player player = playerRepository.findById(entityManager, id)
                    .orElseThrow(() -> new IllegalArgumentException("Jogador não encontrado."));
            Team team = findTeamOrNull(teamId, entityManager);
            if (!sameTeam(player.getTeam(), team)) {
                removePlayerFromLineups(player, entityManager);
            }
            player.setName(normalizedName);
            player.setFinishing(finishing);
            player.setGoalkeeping(goalkeeping);
            if (heightMeters != null) {
                player.setHeightMeters(heightMeters);
            }
            player.setTeam(team);
            return player;
        });
    }

    public List<Player> findAllPlayers() {
        return transactions.read(playerRepository::findAll);
    }

    public List<Player> findPlayersWithoutTeam() {
        return transactions.read(playerRepository::findWithoutTeam);
    }

    public List<Player> findPlayersByTeam(Long teamId) {
        return transactions.read(entityManager -> {
            teamRepository.findById(entityManager, teamId)
                    .orElseThrow(() -> new IllegalArgumentException("Time não encontrado."));
            return playerRepository.findByTeamId(entityManager, teamId);
        });
    }

    public void deletePlayer(Long id) {
        transactions.write(entityManager -> {
            Player player = playerRepository.findById(entityManager, id)
                    .orElseThrow(() -> new IllegalArgumentException("Jogador não encontrado."));
            removePlayerFromLineups(player, entityManager);
            entityManager.flush();
            playerRepository.delete(entityManager, player);
            return null;
        });
    }

    public void transferPlayer(Long playerId, Long newTeamId) {
        if (newTeamId == null) {
            throw new IllegalArgumentException("Selecione um time para associar ao jogador.");
        }
        transactions.write(entityManager -> {
            Player player = playerRepository.findById(entityManager, playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Jogador não encontrado."));
            Team newTeam = teamRepository.findById(entityManager, newTeamId)
                    .orElseThrow(() -> new IllegalArgumentException("Time não encontrado."));
            if (player.getTeam() == null || !player.getTeam().getId().equals(newTeamId)) {
                removePlayerFromLineups(player, entityManager);
                player.setTeam(newTeam);
            }
            return null;
        });
    }

    public void removePlayerFromTeam(Long playerId) {
        transactions.write(entityManager -> {
            Player player = playerRepository.findById(entityManager, playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Jogador não encontrado."));
            if (player.getTeam() != null) {
                removePlayerFromLineups(player, entityManager);
                player.setTeam(null);
            }
            return null;
        });
    }

    private Team findTeamOrNull(Long teamId, jakarta.persistence.EntityManager entityManager) {
        if (teamId == null) {
            return null;
        }
        return teamRepository.findById(entityManager, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Time não encontrado."));
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do jogador é obrigatório.");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > 120) {
            throw new IllegalArgumentException("O nome do jogador deve ter até 120 caracteres.");
        }
        return normalizedName;
    }

    private void validateSkill(int value, String field) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(field + " deve estar entre 0 e 100.");
        }
    }

    private void validateHeight(double heightMeters) {
        if (!Double.isFinite(heightMeters) || heightMeters < Player.MIN_HEIGHT_METERS
                || heightMeters > Player.MAX_HEIGHT_METERS
                || Math.abs(heightMeters * 100 - Math.rint(heightMeters * 100)) > 1e-8) {
            throw new IllegalArgumentException(
                    "A altura deve estar entre 1,00 m e 2,50 m, com até duas casas decimais.");
        }
    }

    private boolean sameTeam(Team first, Team second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.getId().equals(second.getId());
    }

    private void removePlayerFromLineups(Player player,
                                         jakarta.persistence.EntityManager entityManager) {
        for (TeamLineup lineup : lineupRepository.findContainingPlayer(entityManager, player)) {
            lineup.getBatters().removeIf(batter -> batter.getId().equals(player.getId()));
            if (lineup.getGoalkeeper() != null
                    && lineup.getGoalkeeper().getId().equals(player.getId())) {
                lineup.setGoalkeeper(null);
            }
        }
    }
}
