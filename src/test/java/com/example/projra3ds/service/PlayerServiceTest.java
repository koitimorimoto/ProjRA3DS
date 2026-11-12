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

    @Test
    void rejectsHeightOutsideSupportedRangeOrPrecision() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createPlayer("Jogador", 50, 50, 0.99, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.createPlayer("Jogador", 50, 50, 2.51, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.createPlayer("Jogador", 50, 50, 1.755, null));
    }
}
