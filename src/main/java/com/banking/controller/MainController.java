package com.banking.controller;

import com.banking.dao.DatabaseManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Shell controller that owns the sidebar navigation and swaps the center content pane.
 * Each navigation button loads the corresponding sub-view into {@code contentArea}.
 */
public class MainController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnClients;
    @FXML private Button btnAccounts;
    @FXML private Button btnTransactions;

    private Button activeButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize DB on application start
        try {
            DatabaseManager.getInstance();
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
        handleDashboard();
    }

    /** Loads the dashboard view. */
    @FXML
    public void handleDashboard() {
        loadView("/com/banking/fxml/dashboard.fxml", btnDashboard);
    }

    /** Loads the client management view. */
    @FXML
    public void handleClients() {
        loadView("/com/banking/fxml/clients.fxml", btnClients);
    }

    /** Loads the account management view. */
    @FXML
    public void handleAccounts() {
        loadView("/com/banking/fxml/accounts.fxml", btnAccounts);
    }

    /** Loads the transaction management view. */
    @FXML
    public void handleTransactions() {
        loadView("/com/banking/fxml/transactions.fxml", btnTransactions);
    }

    /**
     * Loads an FXML view into the content area and highlights the active nav button.
     *
     * @param fxmlPath the classpath resource path to the FXML file
     * @param button   the sidebar button that triggered this navigation
     */
    private void loadView(String fxmlPath, Button button) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);

            // Update active button styling
            if (activeButton != null) {
                activeButton.getStyleClass().remove("active");
            }
            button.getStyleClass().add("active");
            activeButton = button;
        } catch (IOException e) {
            System.err.println("Failed to load view " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
