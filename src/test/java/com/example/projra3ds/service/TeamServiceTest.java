package com.example.projra3ds.service;

import com.example.projra3ds.model.Team;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void rejectsInvalidTeamColorsBeforeOpeningPersistence() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createTeam("Time", null, "red", Team.DEFAULT_BATTER_COLOR));
        assertThrows(IllegalArgumentException.class,
                () -> service.createTeam("Time", null, Team.DEFAULT_GOALKEEPER_COLOR, "#12345"));

        Team team = new Team("Time", null);
        assertEquals(Team.DEFAULT_GOALKEEPER_COLOR, team.getGoalkeeperColor());
        assertEquals(Team.DEFAULT_BATTER_COLOR, team.getBatterColor());

        Team customColorTeam = new Team("Outro time", null, "#a1B2c3", "#d4E5f6");
        assertEquals("#A1B2C3", customColorTeam.getGoalkeeperColor());
        assertEquals("#D4E5F6", customColorTeam.getBatterColor());
    }
}
