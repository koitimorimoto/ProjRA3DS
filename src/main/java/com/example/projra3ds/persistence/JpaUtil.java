package com.example.projra3ds.persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class JpaUtil {
    private static volatile EntityManagerFactory entityManagerFactory;
    private static final Properties LOCAL_CONFIG = loadLocalConfig();

    private JpaUtil() {
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null) {
            synchronized (JpaUtil.class) {
                if (entityManagerFactory == null) {
                    String username = requiredConfigValue("PENALTY_DB_USER");
                    String password = requiredConfigValue("PENALTY_DB_PASSWORD");
                    DatabaseInitializer.initialize(username, password);
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("jakarta.persistence.jdbc.user", username);
                    properties.put("jakarta.persistence.jdbc.password", password);
                    entityManagerFactory = Persistence.createEntityManagerFactory(
                            "penalty-persistence", properties);
                }
            }
        }
        return entityManagerFactory;
    }

    private static Properties loadLocalConfig() {
        Properties properties = new Properties();
        Path configFile = Path.of(".env");
        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                properties.load(reader);
            } catch (IOException exception) {
                throw new IllegalStateException("Não foi possível ler o arquivo .env.", exception);
            }
        }
        return properties;
    }

    private static String requiredConfigValue(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = LOCAL_CONFIG.getProperty(name);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Configure " + name
                    + " no ambiente do IntelliJ ou no arquivo .env para conectar ao MySQL.");
        }
        return value;
    }

    public static void shutdown() {
        synchronized (JpaUtil.class) {
            if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                entityManagerFactory.close();
                entityManagerFactory = null;
            }
        }
    }
}
