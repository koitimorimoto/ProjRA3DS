package com.example.projra3ds.service;

import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import com.example.projra3ds.persistence.JpaTransactions;
import com.example.projra3ds.repository.PlayerRepository;
import com.example.projra3ds.repository.TeamRepository;

import java.util.List;

public class TeamService {
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final JpaTransactions transactions;

    public TeamService() {
        this(new TeamRepository(), new PlayerRepository(), new JpaTransactions());
    }

    TeamService(TeamRepository teamRepository, PlayerRepository playerRepository,
                JpaTransactions transactions) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.transactions = transactions;
    }

    public Team createTeam(String name, String country) {
        String normalizedName = validateName(name);
        String normalizedCountry = normalizeCountry(country);
        return transactions.write(entityManager -> {
            ensureNameIsAvailable(normalizedName, null, entityManager);
            Team team = new Team(normalizedName, normalizedCountry);
            teamRepository.save(entityManager, team);
            return team;
        });
    }

    public Team updateTeam(Long id, String name, String country) {
        String normalizedName = validateName(name);
        String normalizedCountry = normalizeCountry(country);
        return transactions.write(entityManager -> {
            Team team = teamRepository.findById(entityManager, id)
                    .orElseThrow(() -> new IllegalArgumentException("Time não encontrado."));
            ensureNameIsAvailable(normalizedName, id, entityManager);
            team.setName(normalizedName);
            team.setCountry(normalizedCountry);
            return team;
        });
    }

    public List<Team> findAllTeams() {
        return transactions.read(teamRepository::findAll);
    }

    public long countPlayers(Long teamId) {
        return transactions.read(entityManager -> teamRepository.countPlayers(entityManager, teamId));
    }

    public void deleteTeam(Long id) {
        transactions.write(entityManager -> {
            Team team = teamRepository.findById(entityManager, id)
                    .orElseThrow(() -> new IllegalArgumentException("Time não encontrado."));
            List<Player> players = playerRepository.findByTeamId(entityManager, id);
            players.forEach(player -> player.setTeam(null));
            entityManager.flush();
            teamRepository.delete(entityManager, team);
            return null;
        });
    }

    private void ensureNameIsAvailable(String name, Long excludedId,
                                       jakarta.persistence.EntityManager entityManager) {
        if (teamRepository.existsByName(entityManager, name, excludedId)) {
            throw new IllegalArgumentException("Já existe um time com esse nome.");
        }
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do time é obrigatório.");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > 120) {
            throw new IllegalArgumentException("O nome do time deve ter até 120 caracteres.");
        }
        return normalizedName;
    }

    private String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        String normalizedCountry = country.trim();
        if (normalizedCountry.length() > 120) {
            throw new IllegalArgumentException("País ou região deve ter até 120 caracteres.");
        }
        return normalizedCountry;
    }
}
