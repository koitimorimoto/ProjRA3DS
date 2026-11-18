package com.example.projra3ds.repository;

import com.example.projra3ds.model.PenaltyMatch;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PenaltyMatchRepository {
    public Optional<PenaltyMatch> findById(EntityManager entityManager, Long id) {
        return Optional.ofNullable(entityManager.find(PenaltyMatch.class, id));
    }

    /** Busca a partida já com todas as cobranças carregadas. */
    public Optional<PenaltyMatch> findWithAttempts(EntityManager entityManager, Long id) {
        List<PenaltyMatch> matches = entityManager.createQuery(
                        "SELECT DISTINCT m FROM PenaltyMatch m "
                                + "LEFT JOIN FETCH m.attempts WHERE m.id = :id", PenaltyMatch.class)
                .setParameter("id", id)
                .getResultList();
        return matches.stream().findFirst();
    }

    public List<PenaltyMatch> findAll(EntityManager entityManager) {
        return entityManager.createQuery(
                        "SELECT m FROM PenaltyMatch m ORDER BY m.playedAt DESC, m.id DESC",
                        PenaltyMatch.class)
                .getResultList();
    }

    public void save(EntityManager entityManager, PenaltyMatch match) {
        if (match.getId() == null) {
            entityManager.persist(match);
        }
    }

    public void delete(EntityManager entityManager, PenaltyMatch match) {
        // As cobranças referenciam os jogadores da partida; removê-las primeiro mantém a
        // ordem correta de exclusão no banco.
        new ArrayList<>(match.getAttempts()).forEach(entityManager::remove);
        match.getAttempts().clear();
        entityManager.flush();
        entityManager.remove(match);
    }
}
