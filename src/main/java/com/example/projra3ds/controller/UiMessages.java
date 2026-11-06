package com.example.projra3ds.controller;

import javafx.scene.control.Alert;

final class UiMessages {
    private UiMessages() {
    }

    static void showError(String title, RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "Verifique os dados informados e a conexão com o banco de dados.";
        }
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
