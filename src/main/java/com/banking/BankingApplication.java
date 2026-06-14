package com.banking;

import com.banking.dao.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * JavaFX application entry point for BankManager Pro.
 * Initializes the database on startup and tears it down on exit.
 */
public class BankingApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/banking/fxml/main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(
            getClass().getResource("/com/banking/css/banking.css").toExternalForm());

        primaryStage.setTitle("BankManager Pro — Système de Gestion Bancaire");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        try {
            DatabaseManager.getInstance().closeConnection();
        } catch (SQLException ignored) {}
    }

    /**
     * Application entry point. Delegates to JavaFX launch mechanism.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
