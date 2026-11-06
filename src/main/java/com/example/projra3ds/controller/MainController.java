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
    private Tab lineupTab;
    @FXML
    private Tab matchTab;
    @FXML
    private PlayerController playersViewController;
    @FXML
    private TeamController teamsViewController;
    @FXML
    private LineupController lineupViewController;
    @FXML
    private MatchController matchViewController;

    @FXML
    private void initialize() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (selected == teamsTab) {
                teamsViewController.refresh();
            } else if (selected == playersTab) {
                playersViewController.refresh();
            } else if (selected == lineupTab) {
                lineupViewController.refresh();
            } else if (selected == matchTab) {
                matchViewController.refresh();
            }
        });
        playersViewController.refresh();
    }

    public void shutdown() {
        matchViewController.shutdown();
    }
}
