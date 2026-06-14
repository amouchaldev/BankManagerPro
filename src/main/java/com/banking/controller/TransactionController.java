package com.banking.controller;

import com.banking.dao.AccountDAO;
import com.banking.dao.TransactionDAO;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.TransactionType;
import com.banking.util.AlertHelper;
import com.banking.util.CurrencyFormatter;
import com.banking.util.FormValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the transaction management view.
 * Exposes deposit, withdrawal, and transfer actions via modal dialogs,
 * and provides filtering by type, account, and date range.
 */
public class TransactionController implements Initializable {

    @FXML private TableView<Transaction> tblTransactions;
    @FXML private TableColumn<Transaction, Integer>         colId;
    @FXML private TableColumn<Transaction, String>          colDate;
    @FXML private TableColumn<Transaction, TransactionType> colType;
    @FXML private TableColumn<Transaction, String>          colSource;
    @FXML private TableColumn<Transaction, String>          colTarget;
    @FXML private TableColumn<Transaction, Double>          colAmount;
    @FXML private TableColumn<Transaction, String>          colDescription;

    @FXML private ComboBox<String>  cbTypeFilter;
    @FXML private ComboBox<String>  cbAccountFilter;
    @FXML private DatePicker        dpFrom;
    @FXML private DatePicker        dpTo;
    @FXML private Button btnDeposit;
    @FXML private Button btnWithdraw;
    @FXML private Button btnTransfer;

    private TransactionDAO transactionDAO;
    private AccountDAO accountDAO;
    private ObservableList<Transaction> masterList;
    private FilteredList<Transaction>   filteredList;
    private List<Account> allAccounts;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            transactionDAO = new TransactionDAO();
            accountDAO     = new AccountDAO();
        } catch (SQLException e) {
            AlertHelper.showError("Erreur DB", "Impossible d'initialiser la base de données.");
            return;
        }

        setupTableColumns();
        setupFilters();
        refreshTable();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(data -> data.getValue().idProperty().asObject());

        colDate.setCellValueFactory(data -> data.getValue().transactionDateProperty().asString());
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                try { setText(LocalDateTime.parse(item).format(DATE_FMT)); }
                catch (Exception e) { setText(item); }
            }
        });

        colType.setCellValueFactory(data -> data.getValue().transactionTypeProperty());
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(TransactionType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.getDisplayName());
                setStyle(switch (item) {
                    case DEPOSIT    -> "-fx-text-fill: #16a34a; -fx-font-weight: bold;";
                    case WITHDRAWAL -> "-fx-text-fill: #dc2626; -fx-font-weight: bold;";
                    case TRANSFER   -> "-fx-text-fill: #0891b2; -fx-font-weight: bold;";
                });
            }
        });

        colSource.setCellValueFactory(data -> data.getValue().sourceAccountNumberProperty());
        colTarget.setCellValueFactory(data -> data.getValue().targetAccountNumberProperty());

        colAmount.setCellValueFactory(data -> data.getValue().amountProperty().asObject());
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : CurrencyFormatter.format(item));
            }
        });

        colDescription.setCellValueFactory(data -> data.getValue().descriptionProperty());
    }

    private void setupFilters() {
        masterList   = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(masterList, p -> true);
        tblTransactions.setItems(filteredList);

        cbTypeFilter.getItems().add("Tous les types");
        for (TransactionType t : TransactionType.values()) cbTypeFilter.getItems().add(t.getDisplayName());
        cbTypeFilter.setValue("Tous les types");

        cbAccountFilter.getItems().add("Tous les comptes");
        try {
            allAccounts = accountDAO.findAll();
            allAccounts.forEach(a -> cbAccountFilter.getItems().add(a.getAccountNumber()));
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Impossible de charger les comptes.");
        }
        cbAccountFilter.setValue("Tous les comptes");

        cbTypeFilter.setOnAction(e -> applyFilters());
        cbAccountFilter.setOnAction(e -> applyFilters());
        dpFrom.setOnAction(e -> applyFilters());
        dpTo.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String typeFilter    = cbTypeFilter.getValue();
        String accountFilter = cbAccountFilter.getValue();
        LocalDate from = dpFrom.getValue();
        LocalDate to   = dpTo.getValue();

        filteredList.setPredicate(tx -> {
            if (!"Tous les types".equals(typeFilter)
                    && !tx.getTransactionType().getDisplayName().equals(typeFilter)) return false;
            if (!"Tous les comptes".equals(accountFilter)
                    && !tx.getSourceAccountNumber().equals(accountFilter)
                    && !tx.getTargetAccountNumber().equals(accountFilter)) return false;
            LocalDate txDate = tx.getTransactionDate().toLocalDate();
            if (from != null && txDate.isBefore(from)) return false;
            if (to   != null && txDate.isAfter(to))    return false;
            return true;
        });
    }

    /** Clears all active filters. */
    @FXML
    public void handleClearFilters() {
        cbTypeFilter.setValue("Tous les types");
        cbAccountFilter.setValue("Tous les comptes");
        dpFrom.setValue(null);
        dpTo.setValue(null);
        filteredList.setPredicate(p -> true);
    }

    private void refreshTable() {
        try {
            masterList.setAll(transactionDAO.findAll());
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Impossible de charger les transactions: " + e.getMessage());
        }
    }

    /** Opens the deposit dialog and executes the deposit on confirmation. */
    @FXML
    public void handleDeposit() {
        showDepositDialog().ifPresent(data -> {
            try {
                transactionDAO.executeDeposit(
                    (int) data[0], (double) data[1], (String) data[2]);
                refreshTable();
                AlertHelper.showInfo("Dépôt effectué",
                    "Dépôt de " + CurrencyFormatter.format((double) data[1]) + " enregistré.");
            } catch (SQLException e) {
                AlertHelper.showError("Erreur", "Dépôt impossible: " + e.getMessage());
            }
        });
    }

    /** Opens the withdrawal dialog and executes the withdrawal on confirmation. */
    @FXML
    public void handleWithdraw() {
        showWithdrawalDialog().ifPresent(data -> {
            try {
                transactionDAO.executeWithdrawal(
                    (int) data[0], (double) data[1], (String) data[2]);
                refreshTable();
                AlertHelper.showInfo("Retrait effectué",
                    "Retrait de " + CurrencyFormatter.format((double) data[1]) + " enregistré.");
            } catch (SQLException e) {
                AlertHelper.showError("Solde insuffisant", e.getMessage());
            }
        });
    }

    /** Opens the transfer dialog and executes the transfer on confirmation. */
    @FXML
    public void handleTransfer() {
        showTransferDialog().ifPresent(data -> {
            try {
                transactionDAO.executeTransfer(
                    (int) data[0], (int) data[1], (double) data[2], (String) data[3]);
                refreshTable();
                AlertHelper.showInfo("Virement effectué",
                    "Virement de " + CurrencyFormatter.format((double) data[2]) + " enregistré.");
            } catch (SQLException e) {
                AlertHelper.showError("Erreur", "Virement impossible: " + e.getMessage());
            }
        });
    }

    /**
     * Shows the deposit dialog.
     *
     * @return Optional of Object[]{targetAccountId, amount, description} or empty if cancelled
     */
    private Optional<Object[]> showDepositDialog() {
        Dialog<Object[]> dialog = new Dialog<>();
        dialog.setTitle("Dépôt");
        dialog.setHeaderText("Effectuer un dépôt sur un compte");

        ButtonType okBtn = new ButtonType("Déposer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        GridPane grid = buildTransactionGrid();
        ComboBox<Account> cbTarget = new ComboBox<>(FXCollections.observableArrayList(allAccounts));
        TextField txtAmount = new TextField();
        TextField txtDesc   = new TextField();
        txtAmount.setPromptText("Montant en €");
        txtDesc.setPromptText("Description (optionnel)");

        grid.add(new Label("Compte cible *"), 0, 0); grid.add(cbTarget,   1, 0);
        grid.add(new Label("Montant (€) *"),  0, 1); grid.add(txtAmount,  1, 1);
        grid.add(new Label("Description"),    0, 2); grid.add(txtDesc,    1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(okBtn).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                if (cbTarget.getValue() == null) {
                    AlertHelper.showError("Erreur", "Sélectionnez un compte cible.");
                    event.consume(); return;
                }
                String err = FormValidator.validateAmount(txtAmount.getText());
                if (err != null) { AlertHelper.showError("Erreur", err); event.consume(); }
            });

        dialog.setResultConverter(btn -> btn == okBtn ? new Object[]{
            cbTarget.getValue().getId(),
            Double.parseDouble(txtAmount.getText().replace(",", ".")),
            txtDesc.getText()
        } : null);

        return dialog.showAndWait();
    }

    /**
     * Shows the withdrawal dialog.
     *
     * @return Optional of Object[]{sourceAccountId, amount, description} or empty if cancelled
     */
    private Optional<Object[]> showWithdrawalDialog() {
        Dialog<Object[]> dialog = new Dialog<>();
        dialog.setTitle("Retrait");
        dialog.setHeaderText("Effectuer un retrait depuis un compte");

        ButtonType okBtn = new ButtonType("Retirer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        GridPane grid = buildTransactionGrid();
        ComboBox<Account> cbSource = new ComboBox<>(FXCollections.observableArrayList(allAccounts));
        Label lblBalance = new Label("—");
        TextField txtAmount = new TextField();
        TextField txtDesc   = new TextField();
        txtAmount.setPromptText("Montant en €");
        txtDesc.setPromptText("Description (optionnel)");

        // Show available balance when account is selected
        cbSource.setOnAction(e -> {
            Account a = cbSource.getValue();
            if (a != null) {
                double avail = a.getBalance() + a.getOverdraftLimit();
                lblBalance.setText("Disponible: " + CurrencyFormatter.format(avail));
            }
        });

        grid.add(new Label("Compte source *"), 0, 0); grid.add(cbSource,  1, 0);
        grid.add(new Label(""),                0, 1); grid.add(lblBalance, 1, 1);
        grid.add(new Label("Montant (€) *"),   0, 2); grid.add(txtAmount, 1, 2);
        grid.add(new Label("Description"),     0, 3); grid.add(txtDesc,   1, 3);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(okBtn).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                if (cbSource.getValue() == null) {
                    AlertHelper.showError("Erreur", "Sélectionnez un compte source.");
                    event.consume(); return;
                }
                String err = FormValidator.validateAmount(txtAmount.getText());
                if (err != null) { AlertHelper.showError("Erreur", err); event.consume(); }
            });

        dialog.setResultConverter(btn -> btn == okBtn ? new Object[]{
            cbSource.getValue().getId(),
            Double.parseDouble(txtAmount.getText().replace(",", ".")),
            txtDesc.getText()
        } : null);

        return dialog.showAndWait();
    }

    /**
     * Shows the transfer dialog.
     *
     * @return Optional of Object[]{sourceAccountId, targetAccountId, amount, description} or empty if cancelled
     */
    private Optional<Object[]> showTransferDialog() {
        Dialog<Object[]> dialog = new Dialog<>();
        dialog.setTitle("Virement");
        dialog.setHeaderText("Effectuer un virement entre comptes");

        ButtonType okBtn = new ButtonType("Virer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        GridPane grid = buildTransactionGrid();
        ComboBox<Account> cbSource = new ComboBox<>(FXCollections.observableArrayList(allAccounts));
        ComboBox<Account> cbTarget = new ComboBox<>();
        TextField txtAmount = new TextField();
        TextField txtDesc   = new TextField();
        txtAmount.setPromptText("Montant en €");
        txtDesc.setPromptText("Description (optionnel)");

        // Filter target to exclude the selected source account
        cbSource.setOnAction(e -> {
            Account src = cbSource.getValue();
            if (src != null) {
                cbTarget.setItems(FXCollections.observableArrayList(
                    allAccounts.stream()
                        .filter(a -> a.getId() != src.getId())
                        .toList()));
            }
        });

        grid.add(new Label("Compte source *"), 0, 0); grid.add(cbSource, 1, 0);
        grid.add(new Label("Compte cible *"),  0, 1); grid.add(cbTarget, 1, 1);
        grid.add(new Label("Montant (€) *"),   0, 2); grid.add(txtAmount,1, 2);
        grid.add(new Label("Description"),     0, 3); grid.add(txtDesc,  1, 3);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(okBtn).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                if (cbSource.getValue() == null || cbTarget.getValue() == null) {
                    AlertHelper.showError("Erreur", "Sélectionnez les comptes source et cible.");
                    event.consume(); return;
                }
                String err = FormValidator.validateAmount(txtAmount.getText());
                if (err != null) { AlertHelper.showError("Erreur", err); event.consume(); }
            });

        dialog.setResultConverter(btn -> btn == okBtn ? new Object[]{
            cbSource.getValue().getId(),
            cbTarget.getValue().getId(),
            Double.parseDouble(txtAmount.getText().replace(",", ".")),
            txtDesc.getText()
        } : null);

        return dialog.showAndWait();
    }

    private GridPane buildTransactionGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        return grid;
    }
}
