package com.example.projra3ds.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamServiceTest {
    private final TeamService service = new TeamService();

    @Test
    void rejectsBlankTeamNameBeforeOpeningPersistence() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createTeam(" ", null));
    }

    @Test
    void rejectsTeamNameLongerThanDatabaseLimit() {
        String name = "T".repeat(121);
        assertThrows(IllegalArgumentException.class,
                () -> service.createTeam(name, null));
    }
}
