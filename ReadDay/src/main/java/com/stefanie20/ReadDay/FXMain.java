package com.stefanie20.ReadDay;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by F317 on 16/2/22.
 */
public class FXMain extends Application {
    private static Stage primaryStage;
    private static FXMLLoader loader = new FXMLLoader();

    @Override

    public void start(Stage stage) {
        try {
            primaryStage = stage;
            loader.setLocation(getClass().getClassLoader().getResource("UI.fxml"));
            Parent root = loader.load(getClass().getClassLoader().getResource("UI.fxml").openStream());
            stage.setScene(new Scene(root));
            stage.setTitle("ReadDay");
//            stage.show();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(11);
        }


    }

    public static void main(String[] args) {
        launch(args);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void setPrimaryStage(Stage primaryStage) {
        FXMain.primaryStage = primaryStage;
    }

    public static Controller getController() {
        return loader.getController();
    }
}
