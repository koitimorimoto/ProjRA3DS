package com.example.projra3ds.facade;

import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import com.example.projra3ds.model.TeamLineup;
import com.example.projra3ds.service.PlayerService;
import com.example.projra3ds.service.TeamLineupService;
import com.example.projra3ds.service.TeamService;

import java.util.List;

public class ManagementFacade {
    private final PlayerService playerService;
    private final TeamService teamService;
    private final TeamLineupService teamLineupService;

    public ManagementFacade() {
        this(new PlayerService(), new TeamService(), new TeamLineupService());
    }

    ManagementFacade(PlayerService playerService, TeamService teamService,
                     TeamLineupService teamLineupService) {
        this.playerService = playerService;
        this.teamService = teamService;
        this.teamLineupService = teamLineupService;
    }

    public Player createPlayer(String name, int finishing, int goalkeeping, Long teamId) {
        return playerService.createPlayer(name, finishing, goalkeeping, teamId);
    }

    public Player updatePlayer(Long id, String name, int finishing, int goalkeeping, Long teamId) {
        return playerService.updatePlayer(id, name, finishing, goalkeeping, teamId);
    }

    public List<Player> findAllPlayers() {
        return playerService.findAllPlayers();
    }

    public List<Player> findPlayersWithoutTeam() {
        return playerService.findPlayersWithoutTeam();
    }

    public List<Player> findPlayersByTeam(Long teamId) {
        return playerService.findPlayersByTeam(teamId);
    }

    public void deletePlayer(Long id) {
        playerService.deletePlayer(id);
    }

    public void transferPlayer(Long playerId, Long newTeamId) {
        playerService.transferPlayer(playerId, newTeamId);
    }

    public void removePlayerFromTeam(Long playerId) {
        playerService.removePlayerFromTeam(playerId);
    }

    public Team createTeam(String name, String country) {
        return teamService.createTeam(name, country);
    }

    public Team updateTeam(Long id, String name, String country) {
        return teamService.updateTeam(id, name, country);
    }

    public List<Team> findAllTeams() {
        return teamService.findAllTeams();
    }

    public long countPlayers(Long teamId) {
        return teamService.countPlayers(teamId);
    }

    public void deleteTeam(Long id) {
        teamService.deleteTeam(id);
    }

    public TeamLineup findLineup(Long teamId) {
        return teamLineupService.findByTeamId(teamId);
    }

    public TeamLineup saveLineup(Long teamId, List<Long> batterIds, Long goalkeeperId) {
        return teamLineupService.save(teamId, batterIds, goalkeeperId);
    }
}
