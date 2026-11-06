package com.example.projra3ds.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.function.Function;

public final class JpaTransactions {
    public <T> T read(Function<EntityManager, T> operation) {
        EntityManager entityManager = JpaUtil.getEntityManagerFactory().createEntityManager();
        try {
            return operation.apply(entityManager);
        } finally {
            entityManager.close();
        }
    }

    public <T> T write(Function<EntityManager, T> operation) {
        EntityManager entityManager = JpaUtil.getEntityManagerFactory().createEntityManager();
        try {
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            try {
                T result = operation.apply(entityManager);
                transaction.commit();
                return result;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        } finally {
            entityManager.close();
        }
    }
}
