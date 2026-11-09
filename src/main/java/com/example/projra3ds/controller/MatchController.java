package com.example.projra3ds.controller;

import com.example.projra3ds.facade.ManagementFacade;
import com.example.projra3ds.game.AimTiming;
import com.example.projra3ds.game.GoalkeeperSide;
import com.example.projra3ds.game.PenaltyAttempt;
import com.example.projra3ds.game.PenaltyMatchEngine;
import com.example.projra3ds.game.PlayerSnapshot;
import com.example.projra3ds.game.ShotResult;
import com.example.projra3ds.game.TeamSnapshot;
import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import com.example.projra3ds.model.TeamLineup;
import jakarta.persistence.PersistenceException;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;

public class MatchController {
    private static final double GOAL_LEFT_RATIO = 0.24;
    private static final double GOAL_RIGHT_RATIO = 0.76;
    private static final double GOAL_TOP_RATIO = 0.08;
    private static final double GOAL_BOTTOM_RATIO = 0.43;
    private static final double BALL_START_RATIO = 0.88;
    private static final double SHOOTER_RUN_UP_PROGRESS = 0.28;

    private final ManagementFacade facade = new ManagementFacade();
    private final SimpleDoubleProperty animationProgress = new SimpleDoubleProperty(1);
    private final AnimationTimer aimTimer = new AnimationTimer() {
        private long previousFrame;

        @Override
        public void handle(long now) {
            if (previousFrame != 0 && aimTiming != null) {
                double elapsed = Math.min((now - previousFrame) / 1_000_000_000.0, 0.1);
                aimTiming.advance(elapsed);
                updateAimMeters();
                drawGoal();
            }
            previousFrame = now;
        }

        @Override
        public void stop() {
            previousFrame = 0;
            super.stop();
        }
    };
    private PenaltyMatchEngine match;
    private AimTiming aimTiming;
    private PenaltyAttempt lastAttempt;
    private GoalkeeperSide selectedDive;
    private boolean animating;

    @FXML
    private ComboBox<Team> userTeamCombo;
    @FXML
    private ComboBox<Team> cpuTeamCombo;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label roundLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Canvas goalCanvas;
    @FXML
    private ProgressBar aimXBar;
    @FXML
    private ProgressBar aimYBar;
    @FXML
    private Button lockAimButton;
    @FXML
    private Slider powerSlider;
    @FXML
    private Button shootButton;
    @FXML
    private Button diveLeftButton;
    @FXML
    private Button diveCenterButton;
    @FXML
    private Button diveRightButton;
    @FXML
    private ListView<String> attemptList;

    @FXML
    private void initialize() {
        userTeamCombo.setCellFactory(list -> teamCell());
        userTeamCombo.setButtonCell(teamCell());
        cpuTeamCombo.setCellFactory(list -> teamCell());
        cpuTeamCombo.setButtonCell(teamCell());
        animationProgress.addListener((observable, oldValue, newValue) -> drawGoal());
        drawGoal();
        updateControls();
    }

    @FXML
    public void refresh() {
        try {
            Long userId = userTeamCombo.getValue() == null ? null : userTeamCombo.getValue().getId();
            Long cpuId = cpuTeamCombo.getValue() == null ? null : cpuTeamCombo.getValue().getId();
            List<Team> teams = facade.findAllTeams();
            userTeamCombo.setValue(null);
            cpuTeamCombo.setValue(null);
            userTeamCombo.setItems(FXCollections.observableArrayList(teams));
            cpuTeamCombo.setItems(FXCollections.observableArrayList(teams));
            findTeam(teams, userId).ifPresent(userTeamCombo::setValue);
            findTeam(teams, cpuId).ifPresent(cpuTeamCombo::setValue);
            if (userTeamCombo.getValue() == null && !teams.isEmpty()) {
                userTeamCombo.setValue(teams.get(0));
            }
            if (cpuTeamCombo.getValue() == null && teams.size() > 1) {
                cpuTeamCombo.setValue(teams.get(1));
            }
            if (teams.size() < 2) {
                statusLabel.setText("Cadastre pelo menos dois times para iniciar uma partida.");
            }
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            showError("Não foi possível carregar os times", exception);
        }
    }

    public void shutdown() {
        aimTimer.stop();
    }

    @FXML
    private void startMatch() {
        Team userTeam = userTeamCombo.getValue();
        Team cpuTeam = cpuTeamCombo.getValue();
        if (userTeam == null || cpuTeam == null) {
            showError("Selecione os times", new IllegalArgumentException(
                    "Selecione seu time e o time controlado pela CPU."));
            return;
        }
        if (userTeam.getId().equals(cpuTeam.getId())) {
            showError("Times iguais", new IllegalArgumentException(
                    "Escolha dois times diferentes para a partida."));
            return;
        }
        try {
            TeamSnapshot userSnapshot = createSnapshot(userTeam);
            TeamSnapshot cpuSnapshot = createSnapshot(cpuTeam);
            match = new PenaltyMatchEngine(userSnapshot, cpuSnapshot);
            lastAttempt = null;
            selectedDive = null;
            animating = false;
            aimTimer.stop();
            attemptList.getItems().clear();
            animationProgress.set(1);
            updateScore();
            statusLabel.setText("Sua vez: " + match.getNextShooter().name()
                    + " cobrará por " + match.getNextAttackingTeam().name() + ".");
            startAimTiming();
            updateControls();
            drawGoal();
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            showError("Não foi possível iniciar a partida", exception);
        }
    }

    @FXML
    private void resetMatch() {
        aimTimer.stop();
        match = null;
        aimTiming = null;
        lastAttempt = null;
        selectedDive = null;
        animating = false;
        attemptList.getItems().clear();
        scoreLabel.setText("Placar: —");
        roundLabel.setText("Configure os times e as escalações");
        statusLabel.setText("Cada time precisa de uma escalação com batedores em ordem e um goleiro.");
        updateControls();
        drawGoal();
    }

    @FXML
    private void shoot() {
        if (match == null || animating || match.isFinished()) {
            return;
        }
        boolean userAttacking = match.getNextAttackingTeamIndex() == 0;
        if (!userAttacking && selectedDive == null) {
            statusLabel.setText("Escolha defender à esquerda, no meio ou à direita.");
            return;
        }
        if (userAttacking && (aimTiming == null || !aimTiming.isComplete())) {
            statusLabel.setText("Fixe as coordenadas horizontal e vertical antes de cobrar.");
            return;
        }

        try {
            aimTimer.stop();
            PenaltyAttempt attempt = userAttacking
                    ? match.resolveNextAttempt(aimTiming.getX() * 2 - 1, aimTiming.getY(),
                    powerSlider.getValue(), null)
                    : match.resolveNextAttempt(null, null, null, selectedDive);
            selectedDive = null;
            lastAttempt = attempt;
            animationProgress.set(0);
            attemptList.getItems().add(formatAttempt(attempt));
            updateScore();
            updateControls();
            animateAttempt();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError("Não foi possível executar a cobrança", exception);
        }
    }

    @FXML
    private void diveLeft() {
        selectDive(GoalkeeperSide.LEFT);
    }

    @FXML
    private void diveRight() {
        selectDive(GoalkeeperSide.RIGHT);
    }

    @FXML
    private void diveCenter() {
        selectDive(GoalkeeperSide.CENTER);
    }

    @FXML
    private void lockAim() {
        if (aimTiming == null || aimTiming.isComplete()) {
            return;
        }
        boolean lockingX = aimTiming.isXActive();
        aimTiming.lockCurrentAxis();
        updateAimMeters();
        if (aimTiming.isComplete()) {
            statusLabel.setText("Mira definida. Ajuste a força e clique em Cobrar.");
        } else {
            statusLabel.setText(lockingX
                    ? "Eixo X fixado. Agora a mira vertical está se movendo."
                    : "Eixo Y fixado. Mira completa.");
        }
        updateControls();
    }

    private TeamSnapshot createSnapshot(Team team) {
        TeamLineup lineup = facade.findLineup(team.getId());
        if (lineup == null) {
            throw new IllegalArgumentException("Configure a escalação de " + team.getName()
                    + " na aba Escalações.");
        }
        if (lineup.getBatters().isEmpty()) {
            throw new IllegalArgumentException("Adicione ao menos um batedor à escalação de "
                    + team.getName() + ".");
        }
        if (lineup.getGoalkeeper() == null) {
            throw new IllegalArgumentException("Escolha o goleiro da escalação de "
                    + team.getName() + ".");
        }
        List<PlayerSnapshot> batters = lineup.getBatters().stream()
                .map(this::snapshot)
                .toList();
        return new TeamSnapshot(team.getId(), team.getName(), batters, snapshot(lineup.getGoalkeeper()));
    }

    private PlayerSnapshot snapshot(Player player) {
        return new PlayerSnapshot(player.getId(), player.getName(),
                player.getFinishing(), player.getGoalkeeping());
    }

    private void selectDive(GoalkeeperSide side) {
        if (match == null || animating || match.isFinished()
                || match.getNextAttackingTeamIndex() != 1) {
            return;
        }
        selectedDive = side;
        statusLabel.setText("Você escolheu defender " + divePhrase(side) + ".");
        shoot();
    }

    private void animateAttempt() {
        animating = true;
        updateControls();
        double animationDuration = 850 - lastAttempt.power() * 550;
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(animationDuration),
                new KeyValue(animationProgress, 1, Interpolator.EASE_BOTH)));
        timeline.setOnFinished(event -> {
            animating = false;
            if (match.isFinished()) {
                statusLabel.setText("Fim de jogo! " + match.getWinnerName() + " venceu.");
            } else {
                statusLabel.setText(resultMessage(lastAttempt) + " Próxima cobrança: "
                        + match.getNextShooter().name() + " (" + match.getNextAttackingTeam().name() + "). "
                        + (match.getNextAttackingTeamIndex() == 0
                        ? "Sua vez de cobrar." : "Escolha o lado para defender."));
                lastAttempt = null;
                if (match.getNextAttackingTeamIndex() == 0) {
                    startAimTiming();
                }
            }
            updateControls();
            drawGoal();
        });
        timeline.play();
    }

    private void updateControls() {
        boolean running = match != null && !match.isFinished() && !animating;
        boolean userAttacking = running && match.getNextAttackingTeamIndex() == 0;
        boolean userDefending = running && match.getNextAttackingTeamIndex() == 1;
        aimXBar.setDisable(!userAttacking);
        aimYBar.setDisable(!userAttacking);
        powerSlider.setDisable(!userAttacking);
        lockAimButton.setDisable(!userAttacking || aimTiming == null || aimTiming.isComplete());
        if (aimTiming == null || aimTiming.isComplete()) {
            lockAimButton.setText("Fixar mira");
        } else {
            lockAimButton.setText(aimTiming.isXActive() ? "Fixar X" : "Fixar Y");
        }
        aimXBar.setStyle(aimTiming != null && aimTiming.isXActive()
                ? "-fx-accent: #ffca28;" : "-fx-accent: #78909c;");
        aimYBar.setStyle(aimTiming != null && aimTiming.isYActive()
                ? "-fx-accent: #ffca28;" : "-fx-accent: #78909c;");
        shootButton.setDisable(!userAttacking || aimTiming == null || !aimTiming.isComplete());
        diveLeftButton.setDisable(!userDefending || animating);
        diveCenterButton.setDisable(!userDefending || animating);
        diveRightButton.setDisable(!userDefending || animating);
        drawGoal();
    }

    private void startAimTiming() {
        if (match == null || match.isFinished() || match.getNextAttackingTeamIndex() != 0) {
            aimTiming = null;
            aimTimer.stop();
            updateControls();
            return;
        }
        aimTiming = new AimTiming(match.getNextShooter().finishing());
        updateAimMeters();
        aimTimer.start();
    }

    private void updateAimMeters() {
        if (aimTiming == null) {
            return;
        }
        aimXBar.setProgress(aimTiming.getX());
        aimYBar.setProgress(aimTiming.getY());
    }

    private void updateScore() {
        if (match == null) {
            return;
        }
        scoreLabel.setText(match.getUserTeam().name() + " " + match.getUserGoals()
                + " × " + match.getCpuGoals() + " " + match.getCpuTeam().name());
        roundLabel.setText((match.isFinished() ? "Disputa encerrada" : "Rodada " + match.getNextRound())
                + " · cobranças " + match.getUserAttempts() + "–" + match.getCpuAttempts()
                + (match.getNextRound() > 5 ? " · morte súbita" : ""));
    }

    private void drawGoal() {
        if (goalCanvas == null) {
            return;
        }
        GraphicsContext graphics = goalCanvas.getGraphicsContext2D();
        double width = goalCanvas.getWidth();
        double height = goalCanvas.getHeight();
        double goalLeft = width * GOAL_LEFT_RATIO;
        double goalRight = width * GOAL_RIGHT_RATIO;
        double goalTop = height * GOAL_TOP_RATIO;
        double goalBottom = height * GOAL_BOTTOM_RATIO;
        double ballX = width / 2;
        double ballY = height * BALL_START_RATIO;
        double goalWidth = goalRight - goalLeft;
        double goalHeight = goalBottom - goalTop;

        graphics.setFill(Color.web("#267b36"));
        graphics.fillRect(0, 0, width, height);

        for (int stripe = 0; stripe < 8; stripe++) {
            double stripeTop = height * 0.48 + height * 0.065 * stripe;
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
        graphics.strokeLine(width * 0.39, goalBottom, width * 0.39, height * 0.53);
        graphics.strokeLine(width * 0.39, height * 0.53, width * 0.61, height * 0.53);
        graphics.strokeLine(width * 0.61, height * 0.53, width * 0.61, goalBottom);
        graphics.setLineWidth(1.5);
        graphics.strokeLine(width * 0.08, height * 0.55, width * 0.92, height * 0.55);

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

        double keeperX;
        if (lastAttempt != null && animationProgress.get() < 1) {
            double target = xToCanvas(keeperPosition(lastAttempt.goalkeeperSide()));
            keeperX = interpolate(width / 2, target, animationProgress.get());
        } else if (lastAttempt != null && match != null && match.isFinished()) {
            keeperX = xToCanvas(keeperPosition(lastAttempt.goalkeeperSide()));
        } else {
            keeperX = width / 2;
        }
        double keeperY = goalBottom - goalHeight * 0.18;
        graphics.setFill(Color.web("#1976d2"));
        graphics.fillRoundRect(keeperX - 18, keeperY - 16, 36, 32, 10, 10);
        graphics.setStroke(Color.WHITE);
        graphics.setLineWidth(2);
        graphics.strokeRoundRect(keeperX - 18, keeperY - 16, 36, 32, 10, 10);

        if (lastAttempt != null) {
            double progress = animationProgress.get();
            if (progress < SHOOTER_RUN_UP_PROGRESS) {
                drawShooter(graphics, interpolate(ballX - width * 0.16, ballX - 22,
                        progress / SHOOTER_RUN_UP_PROGRESS), ballY);
            }
            double shotX = interpolate(ballX, xToCanvas(lastAttempt.ballX()), progress);
            double shotY = interpolate(ballY, yToCanvas(lastAttempt.ballY()), progress);
            graphics.setFill(Color.WHITE);
            graphics.fillOval(shotX - 10, shotY - 10, 20, 20);
            graphics.setStroke(Color.web("#202020"));
            graphics.setLineWidth(1.5);
            graphics.strokeOval(shotX - 10, shotY - 10, 20, 20);
        } else {
            if (match != null && !match.isFinished()) {
                drawShooter(graphics, ballX - width * 0.16, ballY);
            }
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
            graphics.setFill(Color.WHITE);
            graphics.fillOval(ballX - 10, ballY - 10, 20, 20);
            graphics.setStroke(Color.web("#202020"));
            graphics.setLineWidth(1.5);
            graphics.strokeOval(ballX - 10, ballY - 10, 20, 20);
        }
    }

    private void drawShooter(GraphicsContext graphics, double centerX, double centerY) {
        graphics.setFill(Color.web("#f5f5f5"));
        graphics.fillRect(centerX - 15, centerY - 19, 30, 38);
        graphics.setFill(Color.web("#1565c0"));
        graphics.fillRect(centerX - 11, centerY - 15, 22, 30);
        graphics.setStroke(Color.WHITE);
        graphics.setLineWidth(2);
        graphics.strokeRect(centerX - 15, centerY - 19, 30, 38);
    }

    private double xToCanvas(double x) {
        double goalLeft = goalCanvas.getWidth() * GOAL_LEFT_RATIO;
        double goalWidth = goalCanvas.getWidth() * (GOAL_RIGHT_RATIO - GOAL_LEFT_RATIO);
        return goalLeft + (x + 1) * goalWidth / 2;
    }

    private double yToCanvas(double y) {
        double goalTop = goalCanvas.getHeight() * GOAL_TOP_RATIO;
        double goalHeight = goalCanvas.getHeight() * (GOAL_BOTTOM_RATIO - GOAL_TOP_RATIO);
        return goalTop + (1 - y) * goalHeight;
    }

    private double interpolate(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    private String formatAttempt(PenaltyAttempt attempt) {
        return "Rodada " + attempt.round() + " · " + attempt.attackingTeam() + " · "
                + attempt.shooter().name() + " · " + resultMessage(attempt)
                + " · goleiro: " + sideName(attempt.goalkeeperSide());
    }

    private String resultMessage(PenaltyAttempt attempt) {
        if (attempt.result() == ShotResult.GOAL) {
            return "GOL";
        }
        if (attempt.result() == ShotResult.SAVED) {
            return "DEFENDIDO";
        }
        return "PARA FORA";
    }

    private String sideName(GoalkeeperSide side) {
        return switch (side) {
            case LEFT -> "esquerda";
            case CENTER -> "meio";
            case RIGHT -> "direita";
        };
    }

    private String divePhrase(GoalkeeperSide side) {
        return switch (side) {
            case LEFT -> "à esquerda";
            case CENTER -> "no meio";
            case RIGHT -> "à direita";
        };
    }

    private double keeperPosition(GoalkeeperSide side) {
        return switch (side) {
            case LEFT -> -0.5;
            case CENTER -> 0;
            case RIGHT -> 0.5;
        };
    }

    private Optional<Team> findTeam(List<Team> teams, Long teamId) {
        if (teamId == null) {
            return Optional.empty();
        }
        return teams.stream().filter(team -> team.getId().equals(teamId)).findFirst();
    }

    private ListCell<Team> teamCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Team team, boolean empty) {
                super.updateItem(team, empty);
                setText(empty || team == null ? null : team.getName());
            }
        };
    }

    private void showError(String title, RuntimeException exception) {
        UiMessages.showError(title, exception);
        statusLabel.setText(title + ": " + exception.getMessage());
    }
}
