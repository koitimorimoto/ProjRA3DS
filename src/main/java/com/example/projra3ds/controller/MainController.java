package com.example.projra3ds.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainController {
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab playersTab;
    @FXML
    private Tab teamsTab;
    @FXML
    private PlayerController playersViewController;
    @FXML
    private TeamController teamsViewController;

    @FXML
    private void initialize() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (selected == teamsTab) {
                teamsViewController.refresh();
            } else if (selected == playersTab) {
                playersViewController.refresh();
            }
        });
        playersViewController.refresh();
    }
}
