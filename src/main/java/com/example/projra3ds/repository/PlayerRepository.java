package com.example.projra3ds.repository;

import com.example.projra3ds.model.Player;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class PlayerRepository {
    public Optional<Player> findById(EntityManager entityManager, Long id) {
        return Optional.ofNullable(entityManager.find(Player.class, id));
    }

    public List<Player> findAll(EntityManager entityManager) {
        return entityManager.createQuery(
                "SELECT p FROM Player p LEFT JOIN FETCH p.team ORDER BY p.name", Player.class)
                .getResultList();
    }

    public List<Player> findWithoutTeam(EntityManager entityManager) {
        return entityManager.createQuery(
                "SELECT p FROM Player p WHERE p.team IS NULL ORDER BY p.name", Player.class)
                .getResultList();
    }

    public List<Player> findByTeamId(EntityManager entityManager, Long teamId) {
        return entityManager.createQuery(
                "SELECT p FROM Player p LEFT JOIN FETCH p.team "
                        + "WHERE p.team.id = :teamId ORDER BY p.name", Player.class)
                .setParameter("teamId", teamId)
                .getResultList();
    }

    public void save(EntityManager entityManager, Player player) {
        if (player.getId() == null) {
            entityManager.persist(player);
        } else {
            entityManager.merge(player);
        }
    }

    public void delete(EntityManager entityManager, Player player) {
        entityManager.remove(player);
    }
}
