package com.example.projra3ds.service;

import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import com.example.projra3ds.persistence.JpaTransactions;
import com.example.projra3ds.repository.PlayerRepository;
import com.example.projra3ds.repository.TeamRepository;
import com.example.projra3ds.repository.TeamLineupRepository;

import java.util.List;

public class TeamService {
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TeamLineupRepository lineupRepository;
    private final JpaTransactions transactions;

    public TeamService() {
        this(new TeamRepository(), new PlayerRepository(),
                new TeamLineupRepository(), new JpaTransactions());
    }

    TeamService(TeamRepository teamRepository, PlayerRepository playerRepository,
                TeamLineupRepository lineupRepository, JpaTransactions transactions) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.lineupRepository = lineupRepository;
        this.transactions = transactions;
    }

    public Team createTeam(String name, String country) {
        return createTeam(name, country, Team.DEFAULT_GOALKEEPER_COLOR, Team.DEFAULT_BATTER_COLOR);
    }

    public Team createTeam(String name, String country, String goalkeeperColor, String batterColor) {
        String normalizedName = validateName(name);
        String normalizedCountry = normalizeCountry(country);
        Team team = new Team(normalizedName, normalizedCountry, goalkeeperColor, batterColor);
        return transactions.write(entityManager -> {
            ensureNameIsAvailable(normalizedName, null, entityManager);
            teamRepository.save(entityManager, team);
            return team;
        });
    }

    public Team updateTeam(Long id, String name, String country) {
        String normalizedName = validateName(name);
        String normalizedCountry = normalizeCountry(country);
        return updateTeam(id, normalizedName, normalizedCountry, null);
    }

    public Team updateTeam(Long id, String name, String country,
                           String goalkeeperColor, String batterColor) {
        String normalizedName = validateName(name);
        String normalizedCountry = normalizeCountry(country);
        Team colors = new Team(normalizedName, normalizedCountry, goalkeeperColor, batterColor);
        return updateTeam(id, normalizedName, normalizedCountry, colors);
    }

    private Team updateTeam(Long id, String normalizedName, String normalizedCountry, Team colors) {
        return transactions.write(entityManager -> {
            Team team = teamRepository.findById(entityManager, id)
                    .orElseThrow(() -> new IllegalArgumentException("Time não encontrado."));
            ensureNameIsAvailable(normalizedName, id, entityManager);
            team.setName(normalizedName);
            team.setCountry(normalizedCountry);
            if (colors != null) {
                team.setGoalkeeperColor(colors.getGoalkeeperColor());
                team.setBatterColor(colors.getBatterColor());
            }
            return team;
        });
    }

    public List<Team> findAllTeams() {
        return transactions.read(teamRepository::findAll);
    }

    public void deleteTeam(Long id) {
        transactions.write(entityManager -> {
            Team team = teamRepository.findById(entityManager, id)
                    .orElseThrow(() -> new IllegalArgumentException("Time não encontrado."));
            List<Player> players = playerRepository.findByTeamId(entityManager, id);
            players.forEach(player -> player.setTeam(null));
            lineupRepository.findByTeamId(entityManager, id)
                    .ifPresent(lineup -> lineupRepository.delete(entityManager, lineup));
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
