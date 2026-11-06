package com.example.projra3ds.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class PenaltyMatchEngine {
    private static final int REGULAR_KICKS = 5;
    private static final double GOAL_MIN_X = -1.0;
    private static final double GOAL_MAX_X = 1.0;
    private static final double GOAL_MIN_Y = 0.0;
    private static final double GOAL_MAX_Y = 1.0;

    private final TeamSnapshot userTeam;
    private final TeamSnapshot cpuTeam;
    private final Random random;
    private final List<PenaltyAttempt> attempts = new ArrayList<>();
    private int userGoals;
    private int cpuGoals;
    private int winnerIndex = -1;
    private boolean finished;

    public PenaltyMatchEngine(TeamSnapshot userTeam, TeamSnapshot cpuTeam) {
        this(userTeam, cpuTeam, new Random());
    }

    public PenaltyMatchEngine(TeamSnapshot userTeam, TeamSnapshot cpuTeam, Random random) {
        this.userTeam = Objects.requireNonNull(userTeam);
        this.cpuTeam = Objects.requireNonNull(cpuTeam);
        this.random = Objects.requireNonNull(random);
        if (userTeam.id() != null && userTeam.id().equals(cpuTeam.id())) {
            throw new IllegalArgumentException("Escolha dois times diferentes.");
        }
    }

    public TeamSnapshot getUserTeam() {
        return userTeam;
    }

    public TeamSnapshot getCpuTeam() {
        return cpuTeam;
    }

    public List<PenaltyAttempt> getAttempts() {
        return List.copyOf(attempts);
    }

    public int getUserGoals() {
        return userGoals;
    }

    public int getCpuGoals() {
        return cpuGoals;
    }

    public int getUserAttempts() {
        return countAttempts(0);
    }

    public int getCpuAttempts() {
        return countAttempts(1);
    }

    public boolean isFinished() {
        return finished;
    }

    public int getWinnerIndex() {
        return winnerIndex;
    }

    public int getNextAttackingTeamIndex() {
        if (finished) {
            throw new IllegalStateException("A disputa já terminou.");
        }
        return getUserAttempts() == getCpuAttempts() ? 0 : 1;
    }

    public TeamSnapshot getNextAttackingTeam() {
        return teamAt(getNextAttackingTeamIndex());
    }

    public TeamSnapshot getNextDefendingTeam() {
        return teamAt(1 - getNextAttackingTeamIndex());
    }

    public PlayerSnapshot getNextShooter() {
        int teamIndex = getNextAttackingTeamIndex();
        int shotsTaken = countAttempts(teamIndex);
        return teamAt(teamIndex).batters().get(shotsTaken % teamAt(teamIndex).batters().size());
    }

    public int getNextRound() {
        return Math.min(getUserAttempts(), getCpuAttempts()) + 1;
    }

    public PenaltyAttempt resolveNextAttempt(Double aimedX, Double aimedY, Double power,
                                             GoalkeeperSide goalkeeperSide) {
        if (finished) {
            throw new IllegalStateException("A disputa já terminou.");
        }
        boolean cpuShooter = getNextAttackingTeamIndex() == 1;
        if ((aimedX == null) != (aimedY == null)) {
            throw new IllegalArgumentException("Informe as duas coordenadas da mira.");
        }
        double targetX = aimedX == null ? random.nextDouble(-0.88, 0.89) : aimedX;
        double targetY = aimedY == null ? random.nextDouble(0.08, 0.93) : aimedY;
        validateAim(targetX, targetY);

        double shotPower = power == null ? random.nextDouble(0.55, 1.0) : power;
        if (!Double.isFinite(shotPower) || shotPower < 0.0 || shotPower > 1.0) {
            throw new IllegalArgumentException("A força deve estar entre 0 e 1.");
        }
        GoalkeeperSide dive = goalkeeperSide == null
                ? GoalkeeperSide.values()[random.nextInt(GoalkeeperSide.values().length)]
                : goalkeeperSide;

        TeamSnapshot attackingTeam = teamAt(cpuShooter ? 1 : 0);
        TeamSnapshot defendingTeam = teamAt(cpuShooter ? 0 : 1);
        PlayerSnapshot shooter = getNextShooter();
        PlayerSnapshot goalkeeper = defendingTeam.goalkeeper();
        double precisionRange = 0.035 + (100 - shooter.finishing()) * 0.002;
        double ballX = targetX + random.nextDouble(-precisionRange, precisionRange);
        double ballY = targetY + random.nextDouble(-precisionRange, precisionRange);
        ShotResult result = resolveShot(ballX, ballY, dive, goalkeeper.goalkeeping());

        int round = getNextRound();
        PenaltyAttempt attempt = new PenaltyAttempt(
                attempts.size() + 1,
                round,
                attackingTeam.name(),
                defendingTeam.name(),
                shooter,
                goalkeeper,
                targetX,
                targetY,
                ballX,
                ballY,
                shotPower,
                dive,
                result,
                cpuShooter,
                !cpuShooter);
        attempts.add(attempt);

        if (result == ShotResult.GOAL) {
            if (cpuShooter) {
                cpuGoals++;
            } else {
                userGoals++;
            }
        }
        updateMatchStatus();
        return attempt;
    }

    public String getWinnerName() {
        if (!finished) {
            return null;
        }
        return winnerIndex == 0 ? userTeam.name() : cpuTeam.name();
    }

    private ShotResult resolveShot(double x, double y, GoalkeeperSide side, int goalkeeperSkill) {
        if (x < GOAL_MIN_X || x > GOAL_MAX_X || y < GOAL_MIN_Y || y > GOAL_MAX_Y) {
            return ShotResult.OUT;
        }
        double keeperX = switch (side) {
            case LEFT -> -0.5;
            case CENTER -> 0.0;
            case RIGHT -> 0.5;
        };
        double horizontalReach = 0.14 + goalkeeperSkill * 0.0025;
        double verticalReach = 0.18 + goalkeeperSkill * 0.004;
        boolean caught = Math.abs(x - keeperX) <= horizontalReach
                && Math.abs(y - 0.5) <= verticalReach;
        return caught ? ShotResult.SAVED : ShotResult.GOAL;
    }

    private void updateMatchStatus() {
        int userShots = getUserAttempts();
        int cpuShots = getCpuAttempts();

        if (userShots <= REGULAR_KICKS && cpuShots <= REGULAR_KICKS) {
            int userRemaining = REGULAR_KICKS - userShots;
            int cpuRemaining = REGULAR_KICKS - cpuShots;
            if (userGoals > cpuGoals + cpuRemaining) {
                finish(0);
            } else if (cpuGoals > userGoals + userRemaining) {
                finish(1);
            } else if (userShots == REGULAR_KICKS && cpuShots == REGULAR_KICKS
                    && userGoals != cpuGoals) {
                finish(userGoals > cpuGoals ? 0 : 1);
            }
            return;
        }

        if (userShots == cpuShots && userGoals != cpuGoals) {
            finish(userGoals > cpuGoals ? 0 : 1);
        }
    }

    private void finish(int index) {
        winnerIndex = index;
        finished = true;
    }

    private int countAttempts(int teamIndex) {
        int count = 0;
        for (PenaltyAttempt attempt : attempts) {
            if ((teamIndex == 0) != attempt.shooterControlledByCpu()) {
                count++;
            }
        }
        return count;
    }

    private TeamSnapshot teamAt(int index) {
        return index == 0 ? userTeam : cpuTeam;
    }

    private void validateAim(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)
                || x < GOAL_MIN_X || x > GOAL_MAX_X || y < GOAL_MIN_Y || y > GOAL_MAX_Y) {
            throw new IllegalArgumentException("A mira deve permanecer dentro dos limites do gol.");
        }
    }
}
