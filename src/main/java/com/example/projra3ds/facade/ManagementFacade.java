package com.example.projra3ds.facade;

import com.example.projra3ds.game.MatchReplay;
import com.example.projra3ds.game.PenaltyMatchEngine;
import com.example.projra3ds.model.PenaltyMatch;
import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import com.example.projra3ds.model.TeamLineup;
import com.example.projra3ds.service.PenaltyMatchService;
import com.example.projra3ds.service.PlayerService;
import com.example.projra3ds.service.TeamLineupService;
import com.example.projra3ds.service.TeamService;

import java.util.List;

public class ManagementFacade {
    private final PlayerService playerService;
    private final TeamService teamService;
    private final TeamLineupService teamLineupService;
    private final PenaltyMatchService matchService;

    public ManagementFacade() {
        this(new PlayerService(), new TeamService(), new TeamLineupService(),
                new PenaltyMatchService());
    }

    ManagementFacade(PlayerService playerService, TeamService teamService,
                     TeamLineupService teamLineupService, PenaltyMatchService matchService) {
        this.playerService = playerService;
        this.teamService = teamService;
        this.teamLineupService = teamLineupService;
        this.matchService = matchService;
    }

    public Player createPlayer(String name, int finishing, int goalkeeping,
                               double heightMeters, Long teamId) {
        return playerService.createPlayer(name, finishing, goalkeeping, heightMeters, teamId);
    }

    public Player updatePlayer(Long id, String name, int finishing, int goalkeeping,
                               Double heightMeters, Long teamId) {
        return playerService.updatePlayer(id, name, finishing, goalkeeping, heightMeters, teamId);
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

    public Team createTeam(String name, String country, String goalkeeperColor, String batterColor) {
        return teamService.createTeam(name, country, goalkeeperColor, batterColor);
    }

    public Team updateTeam(Long id, String name, String country,
                           String goalkeeperColor, String batterColor) {
        return teamService.updateTeam(id, name, country, goalkeeperColor, batterColor);
    }

    public List<Team> findAllTeams() {
        return teamService.findAllTeams();
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

    public PenaltyMatch saveMatch(PenaltyMatchEngine engine) {
        return matchService.saveMatch(engine);
    }

    public List<PenaltyMatch> findAllMatches() {
        return matchService.findAllMatches();
    }

    public MatchReplay loadReplay(Long matchId) {
        return matchService.loadReplay(matchId);
    }

    public PenaltyMatch renameMatch(Long matchId, String title) {
        return matchService.renameMatch(matchId, title);
    }

    public void deleteMatch(Long matchId) {
        matchService.deleteMatch(matchId);
    }
}
