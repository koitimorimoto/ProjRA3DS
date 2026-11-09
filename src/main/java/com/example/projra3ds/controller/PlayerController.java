package com.example.projra3ds.controller;

import com.example.projra3ds.facade.ManagementFacade;
import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import jakarta.persistence.PersistenceException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.event.ActionEvent;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PlayerController {
    private final ManagementFacade facade = new ManagementFacade();

    @FXML
    private ComboBox<Team> teamFilterCombo;
    @FXML
    private CheckBox withoutTeamCheckBox;
    @FXML
    private TableView<Player> playerTable;
    @FXML
    private TableColumn<Player, String> nameColumn;
    @FXML
    private TableColumn<Player, Number> finishingColumn;
    @FXML
    private TableColumn<Player, Number> goalkeepingColumn;
    @FXML
    private TableColumn<Player, String> heightColumn;
    @FXML
    private TableColumn<Player, String> playerTeamColumn;
    @FXML
    private ComboBox<Team> assignTeamCombo;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        finishingColumn.setCellValueFactory(cell ->
                new ReadOnlyIntegerWrapper(cell.getValue().getFinishing()));
        goalkeepingColumn.setCellValueFactory(cell ->
                new ReadOnlyIntegerWrapper(cell.getValue().getGoalkeeping()));
        heightColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                String.format(Locale.forLanguageTag("pt-BR"), "%.2f m",
                        cell.getValue().getHeightMeters())));
        playerTeamColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getTeam() == null ? "Sem time" : cell.getValue().getTeam().getName()));

        configureTeamCombo(teamFilterCombo, false);
        configureTeamCombo(assignTeamCombo, true);
        teamFilterCombo.valueProperty().addListener((observable, previous, selected) -> refreshPlayers());
        withoutTeamCheckBox.selectedProperty().addListener(
                (observable, previous, selected) -> refreshPlayers());
        teamFilterCombo.setOnShowing(event -> reloadTeamChoices());
        assignTeamCombo.setOnShowing(event -> reloadTeamChoices());
    }

    @FXML
    public void refresh() {
        try {
            loadTeams();
            refreshPlayers();
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            UiMessages.showError("Não foi possível carregar os jogadores", exception);
        }
    }

    @FXML
    private void createPlayer() {
        try {
            showPlayerDialog(null);
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            UiMessages.showError("Não foi possível abrir o cadastro", exception);
        }
    }

    @FXML
    private void editPlayer() {
        Player selected = selectedPlayer();
        if (selected != null) {
            try {
                showPlayerDialog(selected);
            } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
                UiMessages.showError("Não foi possível abrir a edição", exception);
            }
        }
    }

    @FXML
    private void deletePlayer() {
        Player selected = selectedPlayer();
        if (selected == null) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir o jogador " + selected.getName() + "?");
        confirmation.setTitle("Confirmar exclusão");
        confirmation.setHeaderText(null);
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            perform("Não foi possível excluir o jogador",
                    () -> facade.deletePlayer(selected.getId()));
        }
    }

    @FXML
    private void assignPlayer() {
        Player selected = selectedPlayer();
        if (selected == null) {
            return;
        }
        Team team = assignTeamCombo.getValue();
        if (team == null) {
            UiMessages.showError("Selecione um time",
                    new IllegalArgumentException("Escolha o time ao qual deseja associar o jogador."));
            return;
        }
        perform("Não foi possível associar o jogador",
                () -> facade.transferPlayer(selected.getId(), team.getId()));
    }

    @FXML
    private void removePlayerFromTeam() {
        Player selected = selectedPlayer();
        if (selected != null) {
            perform("Não foi possível desvincular o jogador",
                    () -> facade.removePlayerFromTeam(selected.getId()));
        }
    }

    private void showPlayerDialog(Player player) {
        loadTeams();
        Dialog<PlayerInput> dialog = new Dialog<>();
        boolean creating = player == null;
        dialog.setTitle(creating ? "Novo jogador" : "Editar jogador");
        dialog.setHeaderText(creating ? "Informe os dados do jogador" : "Atualize os dados do jogador");

        TextField nameField = new TextField(creating ? "" : player.getName());
        TextField finishingField = new TextField(creating ? "50" : Integer.toString(player.getFinishing()));
        TextField goalkeepingField = new TextField(creating ? "50" : Integer.toString(player.getGoalkeeping()));
        TextField heightField = new TextField(creating
                ? Double.toString(Player.DEFAULT_HEIGHT_METERS)
                : Double.toString(player.getHeightMeters()));
        ComboBox<Team> teamCombo = new ComboBox<>(FXCollections.observableArrayList(facade.findAllTeams()));
        configureTeamCombo(teamCombo, true);
        if (!creating && player.getTeam() != null) {
            teamCombo.getItems().stream()
                    .filter(team -> team.getId().equals(player.getTeam().getId()))
                    .findFirst()
                    .ifPresent(teamCombo::setValue);
        }

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Nome:"), nameField);
        form.addRow(1, new Label("Finalização (0-100):"), finishingField);
        form.addRow(2, new Label("Habilidade de goleiro (0-100):"), goalkeepingField);
        form.addRow(3, new Label("Altura (1,00-2,50 m):"), heightField);
        form.addRow(4, new Label("Time (opcional):"), teamCombo);
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);

        ButtonType saveButton = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(saveButton).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                validatePlayerInput(nameField.getText(), finishingField.getText(),
                        goalkeepingField.getText(), heightField.getText());
            } catch (IllegalArgumentException exception) {
                UiMessages.showError("Dados inválidos", exception);
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == saveButton
                ? new PlayerInput(nameField.getText().trim(), Integer.parseInt(finishingField.getText().trim()),
                Integer.parseInt(goalkeepingField.getText().trim()), parseHeight(heightField.getText()),
                teamCombo.getValue() == null ? null : teamCombo.getValue().getId())
                : null);

        Optional<PlayerInput> result = dialog.showAndWait();
        result.ifPresent(input -> perform("Não foi possível salvar o jogador", () -> {
            if (creating) {
                facade.createPlayer(input.name(), input.finishing(), input.goalkeeping(),
                        input.heightMeters(), input.teamId());
            } else {
                facade.updatePlayer(player.getId(), input.name(), input.finishing(),
                        input.goalkeeping(), input.heightMeters(), input.teamId());
            }
        }));
    }

    private void validatePlayerInput(String name, String finishing, String goalkeeping, String height) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do jogador é obrigatório.");
        }
        validateSkillInput(finishing, "Finalização");
        validateSkillInput(goalkeeping, "Habilidade de goleiro");
        parseHeight(height);
    }

    private double parseHeight(String text) {
        try {
            double height = Double.parseDouble(text.trim().replace(',', '.'));
            if (!Double.isFinite(height) || height < Player.MIN_HEIGHT_METERS
                    || height > Player.MAX_HEIGHT_METERS
                    || Math.abs(height * 100 - Math.rint(height * 100)) > 1e-8) {
                throw new IllegalArgumentException(
                        "A altura deve estar entre 1,00 m e 2,50 m, com até duas casas decimais.");
            }
            return height;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Altura deve ser informada em metros, por exemplo 1,75.");
        }
    }

    private void validateSkillInput(String text, String field) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 0 || value > 100) {
                throw new IllegalArgumentException(field + " deve estar entre 0 e 100.");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " deve ser um número inteiro entre 0 e 100.");
        }
    }

    private void configureTeamCombo(ComboBox<Team> combo, boolean nullable) {
        combo.setCellFactory(list -> teamCell(nullable));
        combo.setButtonCell(teamCell(nullable));
    }

    private ListCell<Team> teamCell(boolean nullable) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Team team, boolean empty) {
                super.updateItem(team, empty);
                if (empty) {
                    setText(null);
                } else if (team == null) {
                    setText(nullable ? "Sem time" : null);
                } else {
                    setText(team.getName());
                }
            }
        };
    }

    private void loadTeams() {
        Long selectedFilterId = teamFilterCombo.getValue() == null ? null : teamFilterCombo.getValue().getId();
        Long selectedAssignId = assignTeamCombo.getValue() == null ? null : assignTeamCombo.getValue().getId();
        List<Team> teams = facade.findAllTeams();
        teamFilterCombo.setValue(null);
        assignTeamCombo.setValue(null);
        teamFilterCombo.setItems(FXCollections.observableArrayList(teams));
        assignTeamCombo.setItems(FXCollections.observableArrayList(teams));
        findTeam(teams, selectedFilterId).ifPresent(teamFilterCombo::setValue);
        findTeam(teams, selectedAssignId).ifPresent(assignTeamCombo::setValue);
    }

    private void reloadTeamChoices() {
        try {
            loadTeams();
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            UiMessages.showError("Não foi possível carregar os times", exception);
        }
    }

    private Optional<Team> findTeam(List<Team> teams, Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return teams.stream().filter(team -> team.getId().equals(id)).findFirst();
    }

    private void refreshPlayers() {
        try {
            if (withoutTeamCheckBox.isSelected()) {
                playerTable.setItems(FXCollections.observableArrayList(facade.findPlayersWithoutTeam()));
            } else if (teamFilterCombo.getValue() != null) {
                playerTable.setItems(FXCollections.observableArrayList(
                        facade.findPlayersByTeam(teamFilterCombo.getValue().getId())));
            } else {
                playerTable.setItems(FXCollections.observableArrayList(facade.findAllPlayers()));
            }
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            UiMessages.showError("Não foi possível carregar os jogadores", exception);
        }
    }

    private Player selectedPlayer() {
        Player selected = playerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiMessages.showError("Selecione um jogador",
                    new IllegalArgumentException("Selecione um jogador na lista primeiro."));
        }
        return selected;
    }

    private void perform(String title, Runnable operation) {
        try {
            operation.run();
            refresh();
        } catch (IllegalArgumentException | PersistenceException exception) {
            UiMessages.showError(title, exception);
        }
    }

    private record PlayerInput(String name, int finishing, int goalkeeping,
                               double heightMeters, Long teamId) {
    }
}
