package com.banking.controller;

import com.banking.dao.AccountDAO;
import com.banking.dao.ClientDAO;
import com.banking.model.Account;
import com.banking.model.AccountType;
import com.banking.model.Client;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the account management view.
 * Provides full CRUD operations with client and type filtering.
 */
public class AccountController implements Initializable {

    @FXML private TableView<Account> tblAccounts;
    @FXML private TableColumn<Account, Integer>     colId;
    @FXML private TableColumn<Account, String>      colAccountNumber;
    @FXML private TableColumn<Account, String>      colClientName;
    @FXML private TableColumn<Account, AccountType> colType;
    @FXML private TableColumn<Account, Double>      colBalance;
    @FXML private TableColumn<Account, Double>      colOverdraft;
    @FXML private TableColumn<Account, Double>      colRate;
    @FXML private TableColumn<Account, String>      colStatus;
    @FXML private TableColumn<Account, String>      colOpenedAt;

    @FXML private ComboBox<String> cbFilterClient;
    @FXML private ComboBox<String> cbTypeFilter;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnToggleActive;

    private AccountDAO accountDAO;
    private ClientDAO clientDAO;
    private ObservableList<Account> masterList;
    private FilteredList<Account> filteredList;
    private List<Client> allClients;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            accountDAO = new AccountDAO();
            clientDAO  = new ClientDAO();
        } catch (SQLException e) {
            AlertHelper.showError("Erreur DB", "Impossible d'initialiser la base de données.");
            return;
        }

        setupTableColumns();
        setupFilters();
        setupButtonBindings();
        refreshTable();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(data -> data.getValue().idProperty().asObject());
        colAccountNumber.setCellValueFactory(new PropertyValueFactory<>("accountNumber"));
        colClientName.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        colType.setCellValueFactory(data -> data.getValue().accountTypeProperty());

        // Balance column with red/green coloring
        colBalance.setCellValueFactory(data -> data.getValue().balanceProperty().asObject());
        colBalance.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(CurrencyFormatter.format(item));
                setStyle(item < 0 ? "-fx-text-fill: #dc2626; -fx-font-weight: bold;"
                                  : "-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            }
        });

        colOverdraft.setCellValueFactory(data -> data.getValue().overdraftLimitProperty().asObject());
        colOverdraft.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : CurrencyFormatter.format(item));
            }
        });

        colRate.setCellValueFactory(data -> data.getValue().interestRateProperty().asObject());
        colRate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f%%", item));
            }
        });

        colStatus.setCellValueFactory(data ->
            data.getValue().isActiveProperty().asString());
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                boolean active = "true".equalsIgnoreCase(item);
                setText(active ? "Actif" : "Inactif");
                setStyle(active ? "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
                                : "-fx-text-fill: #dc2626;");
            }
        });

        colOpenedAt.setCellValueFactory(data ->
            data.getValue().openedAtProperty().asString());
        colOpenedAt.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                try {
                    setText(java.time.LocalDateTime.parse(item).format(DATE_FMT));
                } catch (Exception e) { setText(item); }
            }
        });
    }

    private void setupFilters() {
        masterList   = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(masterList, p -> true);
        tblAccounts.setItems(filteredList);

        // Client filter
        cbFilterClient.getItems().add("Tous les clients");
        try {
            allClients = clientDAO.findAll();
            allClients.forEach(c -> cbFilterClient.getItems().add(c.getFullName()));
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Impossible de charger les clients.");
        }
        cbFilterClient.setValue("Tous les clients");

        // Type filter
        cbTypeFilter.getItems().add("Tous les types");
        for (AccountType t : AccountType.values()) cbTypeFilter.getItems().add(t.getDisplayName());
        cbTypeFilter.setValue("Tous les types");

        cbFilterClient.setOnAction(e -> applyFilters());
        cbTypeFilter.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String clientFilter = cbFilterClient.getValue();
        String typeFilter   = cbTypeFilter.getValue();
        filteredList.setPredicate(account -> {
            boolean clientMatch = "Tous les clients".equals(clientFilter)
                || account.getClientName().equals(clientFilter);
            boolean typeMatch = "Tous les types".equals(typeFilter)
                || account.getAccountType().getDisplayName().equals(typeFilter);
            return clientMatch && typeMatch;
        });
    }

    private void setupButtonBindings() {
        btnEdit.disableProperty().bind(tblAccounts.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.disableProperty().bind(tblAccounts.getSelectionModel().selectedItemProperty().isNull());
        btnToggleActive.disableProperty().bind(tblAccounts.getSelectionModel().selectedItemProperty().isNull());
    }

    private void refreshTable() {
        try {
            masterList.setAll(accountDAO.findAll());
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Impossible de charger les comptes: " + e.getMessage());
        }
    }

    /** Opens the "new account" dialog and inserts on confirmation. */
    @FXML
    public void handleAdd() {
        showAccountDialog(new Account(), true).ifPresent(account -> {
            try {
                account.setAccountNumber(accountDAO.generateAccountNumber(account.getClientId()));
                accountDAO.insert(account);
                refreshTable();
                AlertHelper.showInfo("Succès", "Compte " + account.getAccountNumber() + " créé.");
            } catch (SQLException e) {
                AlertHelper.showError("Erreur", "Impossible de créer le compte: " + e.getMessage());
            }
        });
    }

    /** Opens the edit dialog for the selected account and updates on confirmation. */
    @FXML
    public void handleEdit() {
        Account selected = tblAccounts.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showAccountDialog(selected, false).ifPresent(account -> {
            try {
                accountDAO.update(account);
                refreshTable();
                AlertHelper.showInfo("Succès", "Compte modifié.");
            } catch (SQLException e) {
                AlertHelper.showError("Erreur", "Impossible de modifier: " + e.getMessage());
            }
        });
    }

    /**
     * Deletes the selected account.
     * Refuses if the account has any transaction history — suggest deactivation instead.
     */
    @FXML
    public void handleDelete() {
        Account selected = tblAccounts.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            if (accountDAO.countTransactions(selected.getId()) > 0) {
                AlertHelper.showError("Suppression impossible",
                    "Ce compte possède un historique de transactions.\n"
                    + "Désactivez-le plutôt que de le supprimer.");
                return;
            }
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Vérification impossible: " + e.getMessage());
            return;
        }

        Optional<ButtonType> result = AlertHelper.showConfirmation(
            "Confirmer la suppression",
            "Supprimer le compte " + selected.getAccountNumber() + " ?");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                accountDAO.delete(selected.getId());
                refreshTable();
                AlertHelper.showInfo("Succès", "Compte supprimé.");
            } catch (SQLException e) {
                AlertHelper.showError("Erreur", "Impossible de supprimer: " + e.getMessage());
            }
        }
    }

    /** Toggles the active/inactive status of the selected account. */
    @FXML
    public void handleToggleActive() {
        Account selected = tblAccounts.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        selected.setIsActive(!selected.isActive());
        try {
            accountDAO.update(selected);
            refreshTable();
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Impossible de modifier le statut: " + e.getMessage());
        }
    }

    /**
     * Shows a modal dialog for creating or editing an account.
     *
     * @param account the account to edit (modified in place)
     * @param isNew   true for "add" mode, false for "edit" mode
     * @return Optional containing the modified account, or empty if cancelled
     */
    private Optional<Account> showAccountDialog(Account account, boolean isNew) {
        Dialog<Account> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nouveau compte" : "Modifier le compte");
        dialog.setHeaderText(isNew ? "Créer un nouveau compte bancaire"
                                   : "Modifier le compte " + account.getAccountNumber());

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Client selector (only for new accounts)
        ComboBox<Client> cbClient = new ComboBox<>();
        try {
            cbClient.setItems(FXCollections.observableArrayList(clientDAO.findAll()));
            if (!isNew) {
                cbClient.setDisable(true);
                // Select the current client
                cbClient.getItems().stream()
                    .filter(c -> c.getId() == account.getClientId())
                    .findFirst()
                    .ifPresent(cbClient::setValue);
            }
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Impossible de charger les clients.");
        }

        ComboBox<AccountType> cbType = new ComboBox<>(
            FXCollections.observableArrayList(AccountType.values()));
        cbType.setValue(account.getAccountType());

        TextField txtOverdraft = new TextField(String.format("%.2f", account.getOverdraftLimit()));
        TextField txtRate      = new TextField(String.format("%.2f", account.getInterestRate()));

        grid.add(new Label("Client *"),     0, 0); grid.add(cbClient,    1, 0);
        grid.add(new Label("Type *"),       0, 1); grid.add(cbType,      1, 1);
        grid.add(new Label("Découvert (€)"),0, 2); grid.add(txtOverdraft,1, 2);
        grid.add(new Label("Taux (%)"),     0, 3); grid.add(txtRate,     1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(saveBtn).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                if (isNew && cbClient.getValue() == null) {
                    AlertHelper.showError("Formulaire invalide", "Veuillez sélectionner un client.");
                    event.consume();
                    return;
                }
                if (cbType.getValue() == null) {
                    AlertHelper.showError("Formulaire invalide", "Veuillez sélectionner un type de compte.");
                    event.consume();
                    return;
                }
                if (!FormValidator.isNonNegativeAmount(txtOverdraft.getText())) {
                    AlertHelper.showError("Formulaire invalide", "Le découvert doit être un nombre >= 0.");
                    event.consume();
                    return;
                }
                if (!FormValidator.isNonNegativeAmount(txtRate.getText())) {
                    AlertHelper.showError("Formulaire invalide", "Le taux doit être un nombre >= 0.");
                    event.consume();
                }
            });

        dialog.setResultConverter(button -> {
            if (button == saveBtn) {
                if (isNew && cbClient.getValue() != null) {
                    account.setClientId(cbClient.getValue().getId());
                    account.setClientName(cbClient.getValue().getFullName());
                }
                account.setAccountType(cbType.getValue());
                try {
                    account.setOverdraftLimit(Double.parseDouble(txtOverdraft.getText().replace(",",".")));
                    account.setInterestRate(Double.parseDouble(txtRate.getText().replace(",",".")));
                } catch (NumberFormatException ignored) {}
                return account;
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
