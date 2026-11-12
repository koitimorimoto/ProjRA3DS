package com.example.projra3ds.controller;

import com.example.projra3ds.game.AimTiming;
import com.example.projra3ds.game.PenaltyAttempt;
import com.example.projra3ds.game.PenaltyMatchEngine;
import com.example.projra3ds.game.PlayerSnapshot;
import com.example.projra3ds.game.ShotPhysics;
import com.example.projra3ds.game.TeamSnapshot;
import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

final class MatchCanvasRenderer {
    private static final double GOAL_HEIGHT_METERS = 2.5;
    private static final double GOAL_LEFT_RATIO = 0.24;
    private static final double GOAL_RIGHT_RATIO = 0.76;
    private static final double GOAL_TOP_RATIO = 0.13;
    private static final double GOAL_BOTTOM_RATIO = 0.48;
    private static final double FIELD_TOP_RATIO = 0.48;
    private static final double BALL_START_RATIO = 0.88;
    static final double SHOOTER_RUN_UP_SECONDS = 0.45;

    private final Canvas canvas;

    MatchCanvasRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    void draw(PenaltyMatchEngine match, AimTiming aimTiming, PenaltyAttempt lastAttempt,
              double animationProgress) {
        if (canvas == null) {
            return;
        }

        GraphicsContext graphics = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double goalLeft = width * GOAL_LEFT_RATIO;
        double goalRight = width * GOAL_RIGHT_RATIO;
        double goalTop = height * GOAL_TOP_RATIO;
        double goalBottom = height * GOAL_BOTTOM_RATIO;
        double ballX = width / 2;
        double ballY = height * BALL_START_RATIO;
        double goalWidth = goalRight - goalLeft;
        double goalHeight = goalBottom - goalTop;

        double fieldTop = height * FIELD_TOP_RATIO;
        double boardTop = height * 0.30;

        graphics.setFill(Color.web("#a9c5d3"));
        graphics.fillRect(0, 0, width, boardTop);
        graphics.setFill(Color.web("#454b50"));
        graphics.fillRect(0, boardTop, width, fieldTop - boardTop);
        graphics.setFill(Color.web("#267b36"));
        graphics.fillRect(0, fieldTop, width, height - fieldTop);

        for (int stripe = 0; stripe < 8; stripe++) {
            double stripeTop = fieldTop + height * 0.065 * stripe;
            graphics.setFill(stripe % 2 == 0
                    ? Color.color(0.38, 0.72, 0.16, 0.18)
                    : Color.color(0.08, 0.43, 0.12, 0.14));
            graphics.fillRect(0, stripeTop, width, height * 0.065);
        }

        graphics.setStroke(Color.WHITE);
        graphics.setLineWidth(2);
        double penaltyBoxLeft = width * 0.15;
        double penaltyBoxRight = width * 0.85;
        double penaltyBoxBottom = height * 0.64;
        graphics.strokeLine(penaltyBoxLeft, goalBottom, penaltyBoxLeft, penaltyBoxBottom);
        graphics.strokeLine(penaltyBoxLeft, penaltyBoxBottom, penaltyBoxRight, penaltyBoxBottom);
        graphics.strokeLine(penaltyBoxRight, penaltyBoxBottom, penaltyBoxRight, goalBottom);

        graphics.setStroke(Color.color(1, 1, 1, 0.48));
        graphics.setLineWidth(1);
        for (int i = 1; i < 8; i++) {
            double x = goalLeft + goalWidth * i / 8.0;
            graphics.strokeLine(x, goalTop, x, goalBottom);
        }
        for (int i = 1; i < 3; i++) {
            double y = goalTop + goalHeight * i / 3.0;
            graphics.strokeLine(goalLeft, y, goalRight, y);
        }

        graphics.setStroke(Color.WHITE);
        graphics.setLineWidth(6);
        graphics.strokeRect(goalLeft, goalTop, goalWidth, goalHeight);

        double runUpProgress = lastAttempt == null ? 0
                : SHOOTER_RUN_UP_SECONDS
                / (SHOOTER_RUN_UP_SECONDS + ShotPhysics.ballFlightSeconds(
                lastAttempt.shooter().finishing(), lastAttempt.power()));
        double shotProgress = lastAttempt == null ? 0
                : clampProgress((animationProgress - runUpProgress) / (1 - runUpProgress));
        double keeperX = width / 2;
        if (lastAttempt != null) {
            double flightTime = ShotPhysics.ballFlightSeconds(lastAttempt.shooter().finishing(),
                    lastAttempt.power()) * shotProgress;
            double keeperFieldX = ShotPhysics.goalkeeperXAtImpact(lastAttempt.goalkeeperSide(),
                    lastAttempt.goalkeeper().goalkeeping(), flightTime);
            keeperX = xToCanvas(keeperFieldX);
        }
        drawPlayerMarker(graphics, keeperX, goalBottom, goalkeeperColor(match, lastAttempt),
                goalkeeperHeight(match, lastAttempt));

        if (lastAttempt != null) {
            if (animationProgress < runUpProgress) {
                TeamSnapshot shooterTeam = teamFor(match, lastAttempt.shooterControlledByCpu());
                drawShooter(graphics, interpolate(ballX - width * 0.16, ballX - 22,
                                animationProgress / runUpProgress),
                        ballY, playerColor(shooterTeam, lastAttempt.shooter()),
                        lastAttempt.shooter().heightMeters());
            }
            double shotX = interpolate(ballX, xToCanvas(lastAttempt.ballX()), shotProgress);
            double shotY = interpolate(ballY, yToCanvas(lastAttempt.ballY()), shotProgress);
            drawBall(graphics, shotX, shotY);
        } else if (match != null && !match.isFinished()) {
            boolean cpuShooter = match.getNextAttackingTeamIndex() == 1;
            TeamSnapshot shooterTeam = teamFor(match, cpuShooter);
            PlayerSnapshot shooter = match.getNextShooter();
            drawShooter(graphics, ballX - width * 0.16, ballY,
                    playerColor(shooterTeam, shooter), shooter.heightMeters());
        }

        if (lastAttempt == null && match != null && !match.isFinished()
                && match.getNextAttackingTeamIndex() == 0 && aimTiming != null) {
            double targetX = xToCanvas(aimTiming.getX() * 2 - 1);
            double targetY = yToCanvas(aimTiming.getY());
            graphics.setFill(Color.web("#ffca28"));
            graphics.fillOval(targetX - 8, targetY - 8, 16, 16);
            graphics.setStroke(Color.web("#ffca28"));
            graphics.setLineWidth(2);
            graphics.strokeOval(targetX - 14, targetY - 14, 28, 28);
        }

        if (lastAttempt == null) {
            drawBall(graphics, ballX, ballY);
        }
    }

    private Color goalkeeperColor(PenaltyMatchEngine match, PenaltyAttempt lastAttempt) {
        if (match == null) {
            return Color.web(Team.DEFAULT_GOALKEEPER_COLOR);
        }
        boolean cpuDefending = lastAttempt != null
                ? lastAttempt.goalkeeperControlledByCpu()
                : match.getNextAttackingTeamIndex() == 0;
        return Color.web(teamFor(match, cpuDefending).goalkeeperColor());
    }

    private double goalkeeperHeight(PenaltyMatchEngine match, PenaltyAttempt lastAttempt) {
        if (match == null) {
            return Player.DEFAULT_HEIGHT_METERS;
        }
        return lastAttempt == null
                ? match.getNextDefendingTeam().goalkeeper().heightMeters()
                : lastAttempt.goalkeeper().heightMeters();
    }

    private TeamSnapshot teamFor(PenaltyMatchEngine match, boolean cpuTeam) {
        return cpuTeam ? match.getCpuTeam() : match.getUserTeam();
    }

    private Color playerColor(TeamSnapshot team, PlayerSnapshot player) {
        return Color.web(team.isGoalkeeper(player) ? team.goalkeeperColor() : team.batterColor());
    }

    private double clampProgress(double progress) {
        return Math.max(0, Math.min(1, progress));
    }

    private void drawShooter(GraphicsContext graphics, double centerX, double centerY,
                             Color color, double heightMeters) {
        drawPlayerMarker(graphics, centerX, centerY, color, heightMeters);
    }

    private void drawPlayerMarker(GraphicsContext graphics, double centerX, double centerY,
                                  Color fill, double heightMeters) {
        double markerHeight = canvas.getHeight()
                * (GOAL_BOTTOM_RATIO - GOAL_TOP_RATIO)
                * heightMeters / GOAL_HEIGHT_METERS;
        double markerWidth = markerHeight * 0.42;
        double markerTop = centerY - markerHeight;
        graphics.setFill(fill);
        graphics.fillRect(centerX - markerWidth / 2, markerTop,
                markerWidth, markerHeight);
        graphics.setStroke(Color.WHITE);
        graphics.setLineWidth(2);
        graphics.strokeRect(centerX - markerWidth / 2, markerTop,
                markerWidth, markerHeight);
    }

    private void drawBall(GraphicsContext graphics, double centerX, double centerY) {
        graphics.setFill(Color.WHITE);
        graphics.fillOval(centerX - 10, centerY - 10, 20, 20);
        graphics.setStroke(Color.web("#202020"));
        graphics.setLineWidth(1.5);
        graphics.strokeOval(centerX - 10, centerY - 10, 20, 20);
    }

    private double xToCanvas(double x) {
        double goalLeft = canvas.getWidth() * GOAL_LEFT_RATIO;
        double goalWidth = canvas.getWidth() * (GOAL_RIGHT_RATIO - GOAL_LEFT_RATIO);
        return goalLeft + (x + 1) * goalWidth / 2;
    }

    private double yToCanvas(double y) {
        double goalTop = canvas.getHeight() * GOAL_TOP_RATIO;
        double goalHeight = canvas.getHeight() * (GOAL_BOTTOM_RATIO - GOAL_TOP_RATIO);
        return goalTop + (1 - y) * goalHeight;
    }

    private double interpolate(double from, double to, double progress) {
        return from + (to - from) * progress;
    }
}
