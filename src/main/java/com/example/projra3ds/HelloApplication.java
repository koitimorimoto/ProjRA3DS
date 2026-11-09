package com.example.projra3ds;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.example.projra3ds.controller.MainController;

import java.io.IOException;

public class HelloApplication extends Application {
    private MainController mainController;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 960, 760);
        mainController = fxmlLoader.getController();
        stage.setTitle("Simulador de Pênaltis - Gerenciamento");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (mainController != null) {
            mainController.shutdown();
        }
        com.example.projra3ds.persistence.JpaUtil.shutdown();
    }
}
