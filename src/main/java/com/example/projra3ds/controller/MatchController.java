package com.example.projra3ds.controller;

import com.example.projra3ds.facade.ManagementFacade;
import com.example.projra3ds.game.AimTiming;
import com.example.projra3ds.game.GoalkeeperSide;
import com.example.projra3ds.game.PenaltyAttempt;
import com.example.projra3ds.game.PenaltyMatchEngine;
import com.example.projra3ds.game.PlayerSnapshot;
import com.example.projra3ds.game.ShotPhysics;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.util.Duration;

import java.util.List;

public class MatchController {
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
    private MatchCanvasRenderer matchRenderer;

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
        matchRenderer = new MatchCanvasRenderer(goalCanvas);
        userTeamCombo.setCellFactory(list -> ControllerUi.teamCell());
        userTeamCombo.setButtonCell(ControllerUi.teamCell());
        cpuTeamCombo.setCellFactory(list -> ControllerUi.teamCell());
        cpuTeamCombo.setButtonCell(ControllerUi.teamCell());
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
            ControllerUi.findTeam(teams, userId).ifPresent(userTeamCombo::setValue);
            ControllerUi.findTeam(teams, cpuId).ifPresent(cpuTeamCombo::setValue);
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
        return new TeamSnapshot(team.getId(), team.getName(), batters,
                snapshot(lineup.getGoalkeeper()), team.getGoalkeeperColor(), team.getBatterColor());
    }

    private PlayerSnapshot snapshot(Player player) {
        return new PlayerSnapshot(player.getId(), player.getName(),
                player.getFinishing(), player.getGoalkeeping(), player.getHeightMeters());
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
        double animationDuration = (MatchCanvasRenderer.SHOOTER_RUN_UP_SECONDS
                + ShotPhysics.ballFlightSeconds(lastAttempt.shooter().finishing(),
                lastAttempt.power())) * 1000;
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(animationDuration),
                new KeyValue(animationProgress, 1, Interpolator.LINEAR)));
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
        if (matchRenderer != null) {
            matchRenderer.draw(match, aimTiming, lastAttempt, animationProgress.get());
        }
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

    private void showError(String title, RuntimeException exception) {
        UiMessages.showError(title, exception);
        statusLabel.setText(title + ": " + exception.getMessage());
    }
}
