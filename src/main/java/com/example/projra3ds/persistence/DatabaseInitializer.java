package com.example.projra3ds.persistence;

import jakarta.persistence.PersistenceException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

final class DatabaseInitializer {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/simulador_penaltis"
            + "?createDatabaseIfNotExist=true&serverTimezone=UTC";

    private static final List<String> CREATE_TABLES = List.of(
            """
            CREATE TABLE IF NOT EXISTS teams (
                ID BIGINT NOT NULL AUTO_INCREMENT,
                name VARCHAR(120) NOT NULL,
                country VARCHAR(120),
                created_at DATETIME NOT NULL,
                PRIMARY KEY (ID),
                UNIQUE KEY uk_penalty_teams_name (name)
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS players (
                ID BIGINT NOT NULL AUTO_INCREMENT,
                name VARCHAR(120) NOT NULL,
                finishing INTEGER NOT NULL,
                goalkeeping INTEGER NOT NULL,
                created_at DATETIME NOT NULL,
                team_id BIGINT,
                PRIMARY KEY (ID),
                KEY idx_penalty_players_team (team_id),
                CONSTRAINT fk_penalty_players_team
                    FOREIGN KEY (team_id) REFERENCES teams (ID) ON DELETE SET NULL
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS team_lineups (
                ID BIGINT NOT NULL AUTO_INCREMENT,
                team_id BIGINT NOT NULL,
                goalkeeper_id BIGINT,
                PRIMARY KEY (ID),
                UNIQUE KEY uk_penalty_lineups_team (team_id),
                KEY idx_penalty_lineups_goalkeeper (goalkeeper_id),
                CONSTRAINT fk_penalty_lineups_team
                    FOREIGN KEY (team_id) REFERENCES teams (ID) ON DELETE CASCADE,
                CONSTRAINT fk_penalty_lineups_goalkeeper
                    FOREIGN KEY (goalkeeper_id) REFERENCES players (ID) ON DELETE SET NULL
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS lineup_batters (
                lineup_id BIGINT NOT NULL,
                player_id BIGINT NOT NULL,
                batting_order INTEGER NOT NULL,
                PRIMARY KEY (lineup_id, batting_order),
                UNIQUE KEY uk_penalty_lineup_player (lineup_id, player_id),
                KEY idx_penalty_lineup_batter (player_id),
                CONSTRAINT fk_penalty_batters_lineup
                    FOREIGN KEY (lineup_id) REFERENCES team_lineups (ID) ON DELETE CASCADE,
                CONSTRAINT fk_penalty_batters_player
                    FOREIGN KEY (player_id) REFERENCES players (ID) ON DELETE CASCADE
            ) ENGINE=InnoDB
            """
    );

    private DatabaseInitializer() {
    }

    static void initialize(String username, String password) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, username, password);
             Statement statement = connection.createStatement()) {
            for (String createTable : CREATE_TABLES) {
                statement.executeUpdate(createTable);
            }
        } catch (SQLException exception) {
            throw new PersistenceException(
                    "Não foi possível preparar o banco simulador_penaltis. "
                            + "Verifique a conexão e se o usuário tem permissão para criar tabelas.",
                    exception);
        }
    }
}
