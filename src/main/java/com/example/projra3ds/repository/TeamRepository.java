package com.example.projra3ds.repository;

import com.example.projra3ds.model.Team;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class TeamRepository {
    public Optional<Team> findById(EntityManager entityManager, Long id) {
        return Optional.ofNullable(entityManager.find(Team.class, id));
    }

    public List<Team> findAll(EntityManager entityManager) {
        return entityManager.createQuery(
                "SELECT DISTINCT t FROM Team t LEFT JOIN FETCH t.players ORDER BY t.name", Team.class)
                .getResultList();
    }

    public boolean existsByName(EntityManager entityManager, String name, Long excludedId) {
        if (excludedId == null) {
            return entityManager.createQuery(
                            "SELECT COUNT(t) FROM Team t WHERE LOWER(t.name) = LOWER(:name)", Long.class)
                    .setParameter("name", name)
                    .getSingleResult() > 0;
        }
        return entityManager.createQuery(
                        "SELECT COUNT(t) FROM Team t WHERE LOWER(t.name) = LOWER(:name) "
                                + "AND t.id <> :excludedId", Long.class)
                .setParameter("name", name)
                .setParameter("excludedId", excludedId)
                .getSingleResult() > 0;
    }

    public long countPlayers(EntityManager entityManager, Long teamId) {
        return entityManager.createQuery(
                "SELECT COUNT(p) FROM Player p WHERE p.team.id = :teamId", Long.class)
                .setParameter("teamId", teamId)
                .getSingleResult();
    }

    public void save(EntityManager entityManager, Team team) {
        if (team.getId() == null) {
            entityManager.persist(team);
        } else {
            entityManager.merge(team);
        }
    }

    public void delete(EntityManager entityManager, Team team) {
        entityManager.remove(team);
    }
}
