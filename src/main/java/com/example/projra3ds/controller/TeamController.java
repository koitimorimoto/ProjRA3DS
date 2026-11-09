package com.example.projra3ds.controller;

import com.example.projra3ds.facade.ManagementFacade;
import com.example.projra3ds.model.Team;
import jakarta.persistence.PersistenceException;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

import java.util.Locale;
import java.util.Optional;

public class TeamController {
    private final ManagementFacade facade = new ManagementFacade();

    @FXML
    private TableView<Team> teamTable;
    @FXML
    private TableColumn<Team, String> nameColumn;
    @FXML
    private TableColumn<Team, String> countryColumn;
    @FXML
    private TableColumn<Team, String> goalkeeperColorColumn;
    @FXML
    private TableColumn<Team, String> batterColorColumn;
    @FXML
    private TableColumn<Team, Number> playerCountColumn;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        countryColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getCountry() == null ? "" : cell.getValue().getCountry()));
        goalkeeperColorColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getGoalkeeperColor()));
        batterColorColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getBatterColor()));
        playerCountColumn.setCellValueFactory(cell ->
                new ReadOnlyIntegerWrapper(cell.getValue().getPlayers().size()));
    }

    @FXML
    public void refresh() {
        try {
            teamTable.setItems(FXCollections.observableArrayList(facade.findAllTeams()));
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            UiMessages.showError("Não foi possível carregar os times", exception);
        }
    }

    @FXML
    private void createTeam() {
        showTeamDialog(null);
    }

    @FXML
    private void editTeam() {
        Team selected = selectedTeam();
        if (selected != null) {
            showTeamDialog(selected);
        }
    }

    @FXML
    private void deleteTeam() {
        Team selected = selectedTeam();
        if (selected == null) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir o time " + selected.getName()
                        + "? Os jogadores continuarão cadastrados, sem time.");
        confirmation.setTitle("Confirmar exclusão");
        confirmation.setHeaderText(null);
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            perform("Não foi possível excluir o time", () -> facade.deleteTeam(selected.getId()));
        }
    }

    private void showTeamDialog(Team team) {
        boolean creating = team == null;
        Dialog<TeamInput> dialog = new Dialog<>();
        dialog.setTitle(creating ? "Novo time" : "Editar time");
        dialog.setHeaderText(creating ? "Informe os dados do time" : "Atualize os dados do time");

        TextField nameField = new TextField(creating ? "" : team.getName());
        TextField countryField = new TextField(
                creating || team.getCountry() == null ? "" : team.getCountry());
        ColorPicker goalkeeperColorPicker = new ColorPicker(Color.web(creating
                ? Team.DEFAULT_GOALKEEPER_COLOR : team.getGoalkeeperColor()));
        ColorPicker batterColorPicker = new ColorPicker(Color.web(creating
                ? Team.DEFAULT_BATTER_COLOR : team.getBatterColor()));
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Nome:"), nameField);
        form.addRow(1, new Label("País / região (opcional):"), countryField);
        form.addRow(2, new Label("Cor do goleiro:"), goalkeeperColorPicker);
        form.addRow(3, new Label("Cor dos batedores:"), batterColorPicker);
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);

        ButtonType saveButton = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(saveButton).addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    if (nameField.getText() == null || nameField.getText().isBlank()) {
                        UiMessages.showError("Dados inválidos",
                                new IllegalArgumentException("O nome do time é obrigatório."));
                        event.consume();
                    } else if (nameField.getText().trim().length() > 120
                            || countryField.getText().trim().length() > 120) {
                        UiMessages.showError("Dados inválidos", new IllegalArgumentException(
                                "Nome e país/região devem ter até 120 caracteres."));
                        event.consume();
                    }
                });
        dialog.setResultConverter(button -> button == saveButton
                ? new TeamInput(nameField.getText().trim(), countryField.getText().trim(),
                toHex(goalkeeperColorPicker.getValue()), toHex(batterColorPicker.getValue()))
                : null);
        Optional<TeamInput> result = dialog.showAndWait();
        result.ifPresent(input -> perform("Não foi possível salvar o time", () -> {
            if (creating) {
                facade.createTeam(input.name(), input.country(),
                        input.goalkeeperColor(), input.batterColor());
            } else {
                facade.updateTeam(team.getId(), input.name(), input.country(),
                        input.goalkeeperColor(), input.batterColor());
            }
        }));
    }

    private String toHex(Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private Team selectedTeam() {
        Team selected = teamTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiMessages.showError("Selecione um time",
                    new IllegalArgumentException("Selecione um time na lista primeiro."));
        }
        return selected;
    }

    private void perform(String title, Runnable operation) {
        try {
            operation.run();
            refresh();
        } catch (IllegalArgumentException | IllegalStateException | PersistenceException exception) {
            UiMessages.showError(title, exception);
        }
    }

    private record TeamInput(String name, String country, String goalkeeperColor, String batterColor) {
    }
}
