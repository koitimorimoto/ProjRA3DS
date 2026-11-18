package com.example.projra3ds.service;

import com.example.projra3ds.game.MatchReplay;
import com.example.projra3ds.game.PenaltyAttempt;
import com.example.projra3ds.game.PenaltyMatchEngine;
import com.example.projra3ds.game.PlayerSnapshot;
import com.example.projra3ds.game.TeamSnapshot;
import com.example.projra3ds.model.MatchAttempt;
import com.example.projra3ds.model.MatchPlayer;
import com.example.projra3ds.model.MatchSide;
import com.example.projra3ds.model.MatchTeam;
import com.example.projra3ds.model.PenaltyMatch;
import com.example.projra3ds.persistence.JpaTransactions;
import com.example.projra3ds.repository.PenaltyMatchRepository;

import java.util.ArrayList;
import java.util.List;

public class PenaltyMatchService {
    private final PenaltyMatchRepository matchRepository;
    private final JpaTransactions transactions;

    public PenaltyMatchService() {
        this(new PenaltyMatchRepository(), new JpaTransactions());
    }

    PenaltyMatchService(PenaltyMatchRepository matchRepository, JpaTransactions transactions) {
        this.matchRepository = matchRepository;
        this.transactions = transactions;
    }

    public PenaltyMatch saveMatch(PenaltyMatchEngine engine) {
        PenaltyMatch match = buildMatch(engine);
        return transactions.write(entityManager -> {
            matchRepository.save(entityManager, match);
            return match;
        });
    }

    public List<PenaltyMatch> findAllMatches() {
        return transactions.read(matchRepository::findAll);
    }

    public MatchReplay loadReplay(Long id) {
        return transactions.read(entityManager -> {
            PenaltyMatch match = matchRepository.findWithAttempts(entityManager, id)
                    .orElseThrow(() -> new IllegalArgumentException("Partida não encontrada."));
            return toReplay(match);
        });
    }

    public PenaltyMatch renameMatch(Long id, String title) {
        String normalizedTitle = normalizeTitle(title);
        return transactions.write(entityManager -> {
            PenaltyMatch match = matchRepository.findById(entityManager, id)
                    .orElseThrow(() -> new IllegalArgumentException("Partida não encontrada."));
            match.setTitle(normalizedTitle);
            return match;
        });
    }

    public void deleteMatch(Long id) {
        transactions.write(entityManager -> {
            PenaltyMatch match = matchRepository.findById(entityManager, id)
                    .orElseThrow(() -> new IllegalArgumentException("Partida não encontrada."));
            matchRepository.delete(entityManager, match);
            return null;
        });
    }


    PenaltyMatch buildMatch(PenaltyMatchEngine engine) {
        if (!engine.isFinished()) {
            throw new IllegalStateException("Só é possível salvar partidas encerradas.");
        }
        MatchSide winner = MatchSide.fromCpuFlag(engine.getWinnerIndex() == 1);
        PenaltyMatch match = new PenaltyMatch(engine.getUserGoals(), engine.getCpuGoals(), winner);
        MatchTeam userTeam = buildTeam(match, MatchSide.USER, engine.getUserTeam(),
                engine.getUserGoals());
        MatchTeam cpuTeam = buildTeam(match, MatchSide.CPU, engine.getCpuTeam(),
                engine.getCpuGoals());

        int[] shotsTaken = new int[2];
        for (PenaltyAttempt attempt : engine.getAttempts()) {
            MatchSide shooterSide = MatchSide.fromCpuFlag(attempt.shooterControlledByCpu());
            MatchTeam shooterTeam = shooterSide == MatchSide.USER ? userTeam : cpuTeam;
            MatchTeam keeperTeam = shooterSide == MatchSide.USER ? cpuTeam : userTeam;

            List<MatchPlayer> slots = shooterTeam.getBatterSlots();
            MatchPlayer shooter = slots.get(shotsTaken[shooterSide.ordinal()]++ % slots.size());
            MatchPlayer goalkeeper = keeperTeam.getGoalkeeperPlayer();
            if (!toSnapshot(shooter).equals(attempt.shooter())
                    || !toSnapshot(goalkeeper).equals(attempt.goalkeeper())) {
                throw new IllegalStateException(
                        "Os jogadores da cobrança " + attempt.attemptNumber()
                                + " não correspondem à escalação registrada.");
            }
            new MatchAttempt(match, attempt.attemptNumber(), attempt.round(), shooterSide,
                    shooter, goalkeeper, attempt.aimedX(), attempt.aimedY(), attempt.ballX(),
                    attempt.ballY(), attempt.power(), attempt.goalkeeperSide(), attempt.result());
        }
        return match;
    }

    private MatchTeam buildTeam(PenaltyMatch match, MatchSide side, TeamSnapshot snapshot,
                                int goals) {
        MatchTeam team = new MatchTeam(match, side, snapshot.id(), snapshot.name(),
                snapshot.goalkeeperColor(), snapshot.batterColor(), goals);
        List<PlayerSnapshot> batters = snapshot.batters();
        for (int order = 0; order < batters.size(); order++) {
            PlayerSnapshot batter = batters.get(order);
            new MatchPlayer(team, batter.id(), batter.name(), batter.finishing(),
                    batter.goalkeeping(), batter.heightMeters(), order, false);
        }
        PlayerSnapshot keeper = snapshot.goalkeeper();
        new MatchPlayer(team, keeper.id(), keeper.name(), keeper.finishing(),
                keeper.goalkeeping(), keeper.heightMeters(), null, true);
        return team;
    }


    private MatchReplay toReplay(PenaltyMatch match) {
        MatchTeam userTeam = match.getTeam(MatchSide.USER);
        MatchTeam cpuTeam = match.getTeam(MatchSide.CPU);
        List<PenaltyAttempt> attempts = new ArrayList<>();
        for (MatchAttempt attempt : match.getAttempts()) {
            boolean cpuShooter = attempt.getShooterSide().isCpu();
            MatchTeam attacking = cpuShooter ? cpuTeam : userTeam;
            MatchTeam defending = cpuShooter ? userTeam : cpuTeam;
            attempts.add(new PenaltyAttempt(
                    attempt.getAttemptNumber(),
                    attempt.getRoundNumber(),
                    attacking.getName(),
                    defending.getName(),
                    toSnapshot(attempt.getShooter()),
                    toSnapshot(attempt.getGoalkeeper()),
                    attempt.getAimedX(),
                    attempt.getAimedY(),
                    attempt.getBallX(),
                    attempt.getBallY(),
                    attempt.getPower(),
                    attempt.getGoalkeeperSide(),
                    attempt.getResult(),
                    cpuShooter,
                    !cpuShooter));
        }
        return new MatchReplay(toSnapshot(userTeam), toSnapshot(cpuTeam), attempts);
    }

    private TeamSnapshot toSnapshot(MatchTeam team) {
        List<PlayerSnapshot> batters = team.getBatterSlots().stream()
                .map(this::toSnapshot)
                .toList();
        return new TeamSnapshot(team.getOriginalTeamId(), team.getName(), batters,
                toSnapshot(team.getGoalkeeperPlayer()), team.getGoalkeeperColor(),
                team.getBatterColor());
    }

    private PlayerSnapshot toSnapshot(MatchPlayer player) {
        return new PlayerSnapshot(player.getOriginalPlayerId(), player.getName(),
                player.getFinishing(), player.getGoalkeeping(), player.getHeightMeters());
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String normalized = title.trim();
        if (normalized.length() > PenaltyMatch.MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("O título deve ter até "
                    + PenaltyMatch.MAX_TITLE_LENGTH + " caracteres.");
        }
        return normalized;
    }
}
