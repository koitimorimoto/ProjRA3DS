package com.example.projra3ds.service;

import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import com.example.projra3ds.persistence.JpaTransactions;
import com.example.projra3ds.repository.PlayerRepository;
import com.example.projra3ds.repository.TeamRepository;

import java.util.List;

public class PlayerService {
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final JpaTransactions transactions;

    public PlayerService() {
        this(new PlayerRepository(), new TeamRepository(), new JpaTransactions());
    }

    PlayerService(PlayerRepository playerRepository, TeamRepository teamRepository,
                  JpaTransactions transactions) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.transactions = transactions;
    }

    public Player createPlayer(String name, int finishing, int goalkeeping, Long teamId) {
        String normalizedName = validateName(name);
        validateSkill(finishing, "Finalização");
        validateSkill(goalkeeping, "Goleiro");
        return transactions.write(entityManager -> {
            Team team = findTeamOrNull(teamId, entityManager);
            Player player = new Player(normalizedName, finishing, goalkeeping);
            player.setTeam(team);
            playerRepository.save(entityManager, player);
            return player;
        });
    }

    public Player updatePlayer(Long id, String name, int finishing, int goalkeeping, Long teamId) {
        String normalizedName = validateName(name);
        validateSkill(finishing, "Finalização");
        validateSkill(goalkeeping, "Goleiro");
        return transactions.write(entityManager -> {
            Player player = playerRepository.findById(entityManager, id)
                    .orElseThrow(() -> new IllegalArgumentException("Jogador não encontrado."));
            Team team = findTeamOrNull(teamId, entityManager);
            player.setName(normalizedName);
            player.setFinishing(finishing);
            player.setGoalkeeping(goalkeeping);
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
}
