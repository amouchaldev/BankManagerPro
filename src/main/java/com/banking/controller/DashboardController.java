package com.banking.controller;

import com.banking.dao.AccountDAO;
import com.banking.dao.ClientDAO;
import com.banking.dao.TransactionDAO;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.util.AlertHelper;
import com.banking.util.CurrencyFormatter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the dashboard view.
 * Displays key banking statistics and an interactive balance history chart.
 */
public class DashboardController implements Initializable {

    @FXML private Label lblTotalClients;
    @FXML private Label lblTotalAccounts;
    @FXML private Label lblTotalAssets;
    @FXML private Label lblMonthlyVolume;

    @FXML private LineChart<String, Number> balanceChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private ComboBox<Account> cbChartAccount;

    @FXML private TableView<Transaction> tblRecentTransactions;
    @FXML private TableColumn<Transaction, String> colDate;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, Double> colAmount;
    @FXML private TableColumn<Transaction, String> colDescription;

    private ClientDAO clientDAO;
    private AccountDAO accountDAO;
    private TransactionDAO transactionDAO;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter DATE_FMT_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            clientDAO     = new ClientDAO();
            accountDAO    = new AccountDAO();
            transactionDAO = new TransactionDAO();
        } catch (SQLException e) {
            AlertHelper.showError("Erreur DB", "Impossible d'initialiser la base de données.");
            return;
        }

        setupRecentTransactionsTable();
        loadStats();
        loadAccountsComboBox();
        loadRecentTransactions();

        cbChartAccount.setOnAction(e -> loadChart(cbChartAccount.getValue()));
    }

    /** Loads the four summary stat labels from the database. */
    private void loadStats() {
        try {
            lblTotalClients.setText(String.valueOf(clientDAO.countAll()));
            lblTotalAccounts.setText(String.valueOf(accountDAO.countAll()));
            lblTotalAssets.setText(CurrencyFormatter.format(accountDAO.totalAssetsUnderManagement()));
            lblMonthlyVolume.setText(CurrencyFormatter.format(transactionDAO.totalTransactionsThisMonth()));
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Impossible de charger les statistiques: " + e.getMessage());
        }
    }

    /** Populates the chart account selector ComboBox. */
    private void loadAccountsComboBox() {
        try {
            List<Account> accounts = accountDAO.findAll();
            cbChartAccount.setItems(FXCollections.observableArrayList(accounts));
            if (!accounts.isEmpty()) {
                cbChartAccount.setValue(accounts.get(0));
                loadChart(accounts.get(0));
            }
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Impossible de charger les comptes: " + e.getMessage());
        }
    }

    /**
     * Builds the LineChart series for the selected account's balance history.
     * Data points show the running balance after each transaction, with tooltips.
     *
     * @param account the account to display history for
     */
    private void loadChart(Account account) {
        balanceChart.getData().clear();
        if (account == null) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(account.getAccountNumber());

        try {
            List<Object[]> history = transactionDAO.getBalanceHistory(account.getId());
            if (history.isEmpty()) {
                series.getData().add(new XYChart.Data<>("Aujourd'hui", account.getBalance()));
            } else {
                for (Object[] point : history) {
                    LocalDateTime date = (LocalDateTime) point[0];
                    double balance     = (double) point[1];
                    series.getData().add(new XYChart.Data<>(date.format(DATE_FMT), balance));
                }
            }
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Impossible de charger l'historique: " + e.getMessage());
            return;
        }

        balanceChart.getData().add(series);

        // Style data points and add tooltips after the scene is live
        Platform.runLater(() -> {
            if (series.getNode() != null) {
                series.getNode().setStyle("-fx-stroke: #16a34a; -fx-stroke-width: 2.5px;");
            }
            for (XYChart.Data<String, Number> point : series.getData()) {
                if (point.getNode() != null) {
                    Tooltip.install(point.getNode(),
                        new Tooltip(CurrencyFormatter.format(point.getYValue().doubleValue())));
                    point.getNode().setStyle(
                        "-fx-background-color: #16a34a, white; -fx-background-radius: 4px;");
                }
            }
        });
    }

    /** Configures the recent transactions table columns. */
    private void setupRecentTransactionsTable() {
        colDate.setCellValueFactory(data ->
            data.getValue().transactionDateProperty().asString());
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                try {
                    setText(LocalDateTime.parse(item).format(DATE_FMT_FULL));
                } catch (Exception e) { setText(item); }
            }
        });

        colType.setCellValueFactory(data ->
            data.getValue().transactionTypeProperty().asString());

        colAmount.setCellValueFactory(data ->
            data.getValue().amountProperty().asObject());
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(CurrencyFormatter.format(item));
            }
        });

        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    /** Loads the 10 most recent transactions into the table. */
    private void loadRecentTransactions() {
        try {
            List<Transaction> all = transactionDAO.findAll();
            List<Transaction> recent = all.subList(0, Math.min(10, all.size()));
            tblRecentTransactions.setItems(FXCollections.observableArrayList(recent));
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Impossible de charger les transactions: " + e.getMessage());
        }
    }
}
