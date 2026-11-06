package com.example.projra3ds;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 960, 640);
        stage.setTitle("Simulador de Pênaltis - Gerenciamento");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        com.example.projra3ds.persistence.JpaUtil.shutdown();
    }
}
