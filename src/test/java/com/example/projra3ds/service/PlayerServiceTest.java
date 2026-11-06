package com.example.projra3ds.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerServiceTest {
    private final PlayerService service = new PlayerService();

    @Test
    void rejectsBlankPlayerNameBeforeOpeningPersistence() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createPlayer("  ", 50, 50, null));
    }

    @Test
    void rejectsFinishingOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createPlayer("Jogador", 101, 50, null));
    }

    @Test
    void rejectsGoalkeepingOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createPlayer("Jogador", 50, -1, null));
    }
}
