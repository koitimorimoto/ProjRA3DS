package com.example.projra3ds.repository;

import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.TeamLineup;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class TeamLineupRepository {
    public Optional<TeamLineup> findByTeamId(EntityManager entityManager, Long teamId) {
        List<TeamLineup> lineups = entityManager.createQuery(
                        "SELECT DISTINCT l FROM TeamLineup l "
                                + "LEFT JOIN FETCH l.batters "
                                + "LEFT JOIN FETCH l.goalkeeper "
                                + "WHERE l.team.id = :teamId", TeamLineup.class)
                .setParameter("teamId", teamId)
                .getResultList();
        return lineups.stream().findFirst();
    }

    public List<TeamLineup> findContainingPlayer(EntityManager entityManager, Player player) {
        return entityManager.createQuery(
                        "SELECT DISTINCT l FROM TeamLineup l LEFT JOIN FETCH l.batters "
                                + "WHERE l.goalkeeper = :player OR :player MEMBER OF l.batters",
                        TeamLineup.class)
                .setParameter("player", player)
                .getResultList();
    }

    public void save(EntityManager entityManager, TeamLineup lineup) {
        if (lineup.getId() == null) {
            entityManager.persist(lineup);
        }
    }

    public void delete(EntityManager entityManager, TeamLineup lineup) {
        entityManager.remove(lineup);
    }
}
