package com.example.projra3ds.controller;

import com.example.projra3ds.facade.ManagementFacade;
import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import com.example.projra3ds.model.TeamLineup;
import jakarta.persistence.PersistenceException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LineupController {
    private final ManagementFacade facade = new ManagementFacade();
    private List<Player> roster = List.of();

    @FXML
    private ComboBox<Team> teamCombo;
    @FXML
    private ListView<Player> rosterList;
    @FXML
    private ListView<Player> batterList;
    @FXML
    private ComboBox<Player> goalkeeperCombo;
    @FXML
    private Label statusLabel;
    @FXML
    private Button saveButton;

    @FXML
    private void initialize() {
        teamCombo.setCellFactory(list -> teamCell());
        teamCombo.setButtonCell(teamCell());
        rosterList.setCellFactory(list -> playerCell());
        batterList.setCellFactory(list -> playerCell());
        goalkeeperCombo.setCellFactory(list -> playerCell());
        goalkeeperCombo.setButtonCell(playerCell());
        teamCombo.valueProperty().addListener((observable, oldTeam, newTeam) -> loadSelectedTeam());
    }

    @FXML
    public void refresh() {
        try {
            Long selectedTeamId = teamCombo.getValue() == null ? null : teamCombo.getValue().getId();
            List<Team> teams = facade.findAllTeams();
            teamCombo.setValue(null);
            teamCombo.setItems(FXCollections.observableArrayList(teams));
            findTeam(teams, selectedTeamId).ifPresent(teamCombo::setValue);
            if (teamCombo.getValue() == null && !teams.isEmpty()) {
                teamCombo.setValue(teams.get(0));
            }
            if (teams.isEmpty()) {
                setStatus("Cadastre um time antes de montar uma escalação.");
            }
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            showError("Não foi possível carregar as escalações", exception);
        }
    }

    @FXML
    private void addBatter() {
        Player selected = rosterList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Selecione um jogador do elenco para adicionar.");
            return;
        }
        if (batterList.getItems().stream().anyMatch(player -> player.getId().equals(selected.getId()))) {
            setStatus(selected.getName() + " já está na lista de batedores.");
            return;
        }
        batterList.getItems().add(selected);
        setStatus(selected.getName() + " foi adicionado à lista.");
    }

    @FXML
    private void removeBatter() {
        Player selected = batterList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            batterList.getItems().remove(selected);
            setStatus(selected.getName() + " foi removido da lista de batedores.");
        }
    }

    @FXML
    private void moveBatterUp() {
        moveBatter(-1);
    }

    @FXML
    private void moveBatterDown() {
        moveBatter(1);
    }

    @FXML
    private void saveLineup() {
        Team team = teamCombo.getValue();
        if (team == null) {
            setStatus("Selecione um time.");
            return;
        }
        List<Long> batterIds = batterList.getItems().stream().map(Player::getId).toList();
        Long goalkeeperId = goalkeeperCombo.getValue() == null
                ? null : goalkeeperCombo.getValue().getId();
        try {
            facade.saveLineup(team.getId(), batterIds, goalkeeperId);
            setStatus("Escalação de " + team.getName() + " salva. A ordem da lista será a ordem das cobranças.");
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            showError("Não foi possível salvar a escalação", exception);
        }
    }

    private void loadSelectedTeam() {
        Team team = teamCombo.getValue();
        batterList.getItems().clear();
        rosterList.getItems().clear();
        goalkeeperCombo.getItems().clear();
        if (team == null) {
            saveButton.setDisable(true);
            return;
        }
        try {
            roster = facade.findPlayersByTeam(team.getId());
            rosterList.setItems(FXCollections.observableArrayList(roster));
            goalkeeperCombo.setItems(FXCollections.observableArrayList(roster));
            TeamLineup lineup = facade.findLineup(team.getId());
            if (lineup != null) {
                batterList.setItems(FXCollections.observableArrayList(lineup.getBatters()));
                if (lineup.getGoalkeeper() != null) {
                    findPlayer(roster, lineup.getGoalkeeper().getId())
                            .ifPresent(goalkeeperCombo::setValue);
                }
                setStatus("Escalação carregada para " + team.getName() + ".");
            } else {
                setStatus("Monte a lista de batedores e escolha o goleiro de " + team.getName() + ".");
            }
            saveButton.setDisable(false);
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            showError("Não foi possível carregar o elenco", exception);
            saveButton.setDisable(true);
        }
    }

    private void moveBatter(int direction) {
        int index = batterList.getSelectionModel().getSelectedIndex();
        int newIndex = index + direction;
        if (index < 0 || newIndex < 0 || newIndex >= batterList.getItems().size()) {
            return;
        }
        List<Player> reordered = new ArrayList<>(batterList.getItems());
        Player player = reordered.remove(index);
        reordered.add(newIndex, player);
        batterList.setItems(FXCollections.observableArrayList(reordered));
        batterList.getSelectionModel().select(newIndex);
    }

    private Optional<Team> findTeam(List<Team> teams, Long teamId) {
        if (teamId == null) {
            return Optional.empty();
        }
        return teams.stream().filter(team -> team.getId().equals(teamId)).findFirst();
    }

    private Optional<Player> findPlayer(List<Player> players, Long playerId) {
        return players.stream().filter(player -> player.getId().equals(playerId)).findFirst();
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

    private ListCell<Player> playerCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);
                setText(empty || player == null ? null : player.getName());
            }
        };
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String title, RuntimeException exception) {
        UiMessages.showError(title, exception);
        setStatus(title + ".");
    }
}
