package com.example.projra3ds.controller;

import com.example.projra3ds.facade.ManagementFacade;
import com.example.projra3ds.game.MatchReplay;
import com.example.projra3ds.game.PenaltyAttempt;
import com.example.projra3ds.game.PenaltyMatchEngine;
import com.example.projra3ds.game.ShotPhysics;
import com.example.projra3ds.game.ShotResult;
import com.example.projra3ds.model.PenaltyMatch;
import jakarta.persistence.PersistenceException;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Aba "Histórico": lista as partidas salvas e permite reprisar a partida inteira ou uma
 * cobrança específica usando o mesmo renderizador da partida ao vivo.
 */
public class HistoryController {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final double PAUSE_BETWEEN_ATTEMPTS_MS = 900;

    private final ManagementFacade facade = new ManagementFacade();
    private final SimpleDoubleProperty animationProgress = new SimpleDoubleProperty(1);
    private MatchCanvasRenderer renderer;
    private MatchReplay replay;
    private PenaltyMatchEngine replayEngine;
    private PenaltyAttempt currentAttempt;
    private int currentIndex = -1;
    private Timeline timeline;
    private PauseTransition pause;

    @FXML
    private TableView<PenaltyMatch> matchTable;
    @FXML
    private TableColumn<PenaltyMatch, String> dateColumn;
    @FXML
    private TableColumn<PenaltyMatch, String> titleColumn;
    @FXML
    private TableColumn<PenaltyMatch, String> userTeamColumn;
    @FXML
    private TableColumn<PenaltyMatch, String> scoreColumn;
    @FXML
    private TableColumn<PenaltyMatch, String> cpuTeamColumn;
    @FXML
    private TableColumn<PenaltyMatch, String> winnerColumn;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Canvas goalCanvas;
    @FXML
    private ListView<String> attemptList;

    @FXML
    private void initialize() {
        renderer = new MatchCanvasRenderer(goalCanvas);
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getPlayedAt().format(DATE_FORMAT)));
        titleColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getTitle() == null ? "—" : cell.getValue().getTitle()));
        userTeamColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getUserTeamName()));
        scoreColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getUserGoals() + " × " + cell.getValue().getCpuGoals()));
        cpuTeamColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCpuTeamName()));
        winnerColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getWinnerName()));

        matchTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> loadSelected(selected));
        attemptList.setOnMouseClicked(event -> {
            int index = attemptList.getSelectionModel().getSelectedIndex();
            if (replay != null && index >= 0) {
                playAttempt(index, false);
            }
        });
        animationProgress.addListener((observable, oldValue, newValue) -> draw());
        draw();
    }

    @FXML
    public void refresh() {
        try {
            PenaltyMatch selected = matchTable.getSelectionModel().getSelectedItem();
            Long selectedId = selected == null ? null : selected.getId();
            List<PenaltyMatch> matches = facade.findAllMatches();
            matchTable.setItems(FXCollections.observableArrayList(matches));
            if (selectedId != null) {
                matches.stream()
                        .filter(match -> selectedId.equals(match.getId()))
                        .findFirst()
                        .ifPresent(match -> matchTable.getSelectionModel().select(match));
            }
            if (matches.isEmpty()) {
                statusLabel.setText("Nenhuma partida salva ainda. Termine uma disputa na aba Partida.");
            }
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            showError("Não foi possível carregar o histórico", exception);
        }
    }

    public void shutdown() {
        stopPlayback();
    }

    @FXML
    private void replayAll() {
        if (!hasReplay()) {
            return;
        }
        playAttempt(0, true);
    }

    @FXML
    private void stopReplay() {
        stopPlayback();
        animationProgress.set(1);
        statusLabel.setText("Reprise interrompida.");
    }

    @FXML
    private void previousAttempt() {
        if (!hasReplay()) {
            return;
        }
        playAttempt(Math.max(currentIndex - 1, 0), false);
    }

    @FXML
    private void nextAttempt() {
        if (!hasReplay()) {
            return;
        }
        playAttempt(Math.min(currentIndex + 1, replay.attempts().size() - 1), false);
    }

    @FXML
    private void renameMatch() {
        PenaltyMatch selected = matchTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Selecione uma partida para renomear.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(
                selected.getTitle() == null ? "" : selected.getTitle());
        dialog.setTitle("Renomear partida");
        dialog.setHeaderText(null);
        dialog.setContentText("Título:");
        dialog.showAndWait().ifPresent(title -> {
            try {
                facade.renameMatch(selected.getId(), title);
                refresh();
            } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
                showError("Não foi possível renomear a partida", exception);
            }
        });
    }

    @FXML
    private void deleteMatch() {
        PenaltyMatch selected = matchTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Selecione uma partida para excluir.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir a partida " + selected.getUserTeamName() + " "
                        + selected.getUserGoals() + " × " + selected.getCpuGoals() + " "
                        + selected.getCpuTeamName() + "? Todas as cobranças salvas serão apagadas.",
                ButtonType.OK, ButtonType.CANCEL);
        confirmation.setTitle("Excluir partida");
        confirmation.setHeaderText(null);
        if (confirmation.showAndWait().filter(button -> button == ButtonType.OK).isEmpty()) {
            return;
        }
        try {
            stopPlayback();
            facade.deleteMatch(selected.getId());
            clearReplay();
            refresh();
            statusLabel.setText("Partida excluída.");
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            showError("Não foi possível excluir a partida", exception);
        }
    }

    private void loadSelected(PenaltyMatch selected) {
        stopPlayback();
        if (selected == null) {
            clearReplay();
            return;
        }
        try {
            replay = facade.loadReplay(selected.getId());
            replayEngine = new PenaltyMatchEngine(replay.userTeam(), replay.cpuTeam());
            currentAttempt = null;
            currentIndex = -1;
            animationProgress.set(1);
            attemptList.setItems(FXCollections.observableArrayList(
                    replay.attempts().stream().map(AttemptText::describe).toList()));
            scoreLabel.setText(selected.getUserTeamName() + " 0 × 0 " + selected.getCpuTeamName());
            statusLabel.setText("Partida carregada com " + replay.attempts().size()
                    + " cobranças. Use Reprisar partida ou clique em uma cobrança da lista.");
            draw();
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            clearReplay();
            showError("Não foi possível carregar a partida", exception);
        }
    }

    private void clearReplay() {
        replay = null;
        replayEngine = null;
        currentAttempt = null;
        currentIndex = -1;
        animationProgress.set(1);
        attemptList.getItems().clear();
        scoreLabel.setText("Placar: —");
        draw();
    }

    private boolean hasReplay() {
        if (replay == null || replay.attempts().isEmpty()) {
            statusLabel.setText("Selecione uma partida na tabela.");
            return false;
        }
        return true;
    }

    private void playAttempt(int index, boolean continueAfter) {
        stopPlayback();
        currentIndex = index;
        currentAttempt = replay.attempts().get(index);
        PenaltyAttempt attempt = currentAttempt;
        attemptList.getSelectionModel().select(index);
        attemptList.scrollTo(index);
        updateScore(index - 1);
        statusLabel.setText("Cobrança " + attempt.attemptNumber() + ": "
                + attempt.shooter().name() + " (" + attempt.attackingTeam() + ")");

        animationProgress.set(0);
        double durationMs = (MatchCanvasRenderer.SHOOTER_RUN_UP_SECONDS
                + ShotPhysics.ballFlightSeconds(attempt.shooter().finishing(), attempt.power()))
                * 1000;
        timeline = new Timeline(new KeyFrame(Duration.millis(durationMs),
                new KeyValue(animationProgress, 1, Interpolator.LINEAR)));
        timeline.setOnFinished(event -> {
            updateScore(index);
            statusLabel.setText(AttemptText.describe(attempt));
            if (continueAfter) {
                if (index + 1 < replay.attempts().size()) {
                    pause = new PauseTransition(Duration.millis(PAUSE_BETWEEN_ATTEMPTS_MS));
                    pause.setOnFinished(done -> playAttempt(index + 1, true));
                    pause.play();
                } else {
                    statusLabel.setText("Fim da reprise. " + scoreLabel.getText());
                }
            }
        });
        timeline.play();
    }

    private void stopPlayback() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        if (pause != null) {
            pause.stop();
            pause = null;
        }
    }

    /** Atualiza o placar considerando as cobranças de 0 até {@code lastIndex}, inclusive. */
    private void updateScore(int lastIndex) {
        if (replay == null) {
            return;
        }
        int userGoals = 0;
        int cpuGoals = 0;
        for (int i = 0; i <= lastIndex && i < replay.attempts().size(); i++) {
            PenaltyAttempt attempt = replay.attempts().get(i);
            if (attempt.result() == ShotResult.GOAL) {
                if (attempt.shooterControlledByCpu()) {
                    cpuGoals++;
                } else {
                    userGoals++;
                }
            }
        }
        scoreLabel.setText(replay.userTeam().name() + " " + userGoals
                + " × " + cpuGoals + " " + replay.cpuTeam().name());
    }

    private void draw() {
        if (renderer != null) {
            renderer.draw(replayEngine, null, currentAttempt, animationProgress.get());
        }
    }

    private void showError(String title, RuntimeException exception) {
        UiMessages.showError(title, exception);
        statusLabel.setText(title + ": " + exception.getMessage());
    }
}
