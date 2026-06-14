package com.banking.controller;

import com.banking.dao.AccountDAO;
import com.banking.dao.ClientDAO;
import com.banking.model.Client;
import com.banking.util.AlertHelper;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the client management view.
 * Provides full CRUD operations with a search bar and modal dialogs.
 */
public class ClientController implements Initializable {

    @FXML private TableView<Client> tblClients;
    @FXML private TableColumn<Client, Integer> colId;
    @FXML private TableColumn<Client, String> colFirstName;
    @FXML private TableColumn<Client, String> colLastName;
    @FXML private TableColumn<Client, String> colEmail;
    @FXML private TableColumn<Client, String> colPhone;
    @FXML private TableColumn<Client, String> colCreatedAt;
    @FXML private TextField txtSearch;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private ClientDAO clientDAO;
    private AccountDAO accountDAO;
    private ObservableList<Client> masterList;
    private FilteredList<Client> filteredList;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            clientDAO  = new ClientDAO();
            accountDAO = new AccountDAO();
        } catch (SQLException e) {
            AlertHelper.showError("Erreur DB", "Impossible d'initialiser la base de données.");
            return;
        }

        setupTableColumns();
        setupSearch();
        setupButtonBindings();
        refreshTable();
    }

    /** Configures column cell value factories. */
    private void setupTableColumns() {
        colId.setCellValueFactory(data -> data.getValue().idProperty().asObject());
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        colCreatedAt.setCellValueFactory(data ->
            data.getValue().createdAtProperty().asString());
        colCreatedAt.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                try {
                    setText(java.time.LocalDateTime.parse(item).format(DATE_FMT));
                } catch (Exception e) { setText(item); }
            }
        });
    }

    /** Wires the search field to filter the table without re-querying the DB. */
    private void setupSearch() {
        masterList   = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(masterList, p -> true);
        tblClients.setItems(filteredList);

        txtSearch.textProperty().addListener((obs, old, text) -> {
            filteredList.setPredicate(client -> {
                if (text == null || text.isBlank()) return true;
                String lower = text.toLowerCase();
                return client.getFullName().toLowerCase().contains(lower)
                    || client.getEmail().toLowerCase().contains(lower)
                    || client.getPhone().contains(text);
            });
        });
    }

    /** Disables Edit/Delete buttons when no row is selected. */
    private void setupButtonBindings() {
        btnEdit.disableProperty().bind(tblClients.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.disableProperty().bind(tblClients.getSelectionModel().selectedItemProperty().isNull());
    }

    /** Reloads all clients from the database into the master list. */
    private void refreshTable() {
        try {
            masterList.setAll(clientDAO.findAll());
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Impossible de charger les clients: " + e.getMessage());
        }
    }

    /** Opens the "new client" dialog and inserts on confirmation. */
    @FXML
    public void handleAdd() {
        showClientDialog(new Client(), true).ifPresent(client -> {
            try {
                clientDAO.insert(client);
                refreshTable();
                AlertHelper.showInfo("Succès", "Client ajouté avec succès.");
            } catch (SQLException e) {
                AlertHelper.showError("Erreur", "Impossible d'ajouter le client: " + e.getMessage());
            }
        });
    }

    /** Opens the edit dialog for the selected client and updates on confirmation. */
    @FXML
    public void handleEdit() {
        Client selected = tblClients.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        showClientDialog(selected, false).ifPresent(client -> {
            try {
                clientDAO.update(client);
                refreshTable();
                AlertHelper.showInfo("Succès", "Client modifié avec succès.");
            } catch (SQLException e) {
                AlertHelper.showError("Erreur", "Impossible de modifier le client: " + e.getMessage());
            }
        });
    }

    /**
     * Deletes the selected client after confirmation.
     * Refuses if the client still has accounts in the database.
     */
    @FXML
    public void handleDelete() {
        Client selected = tblClients.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Guard: refuse if client has accounts
        try {
            if (!accountDAO.findByClientId(selected.getId()).isEmpty()) {
                AlertHelper.showError("Suppression impossible",
                    "Ce client possède des comptes bancaires.\n"
                    + "Supprimez ses comptes avant de supprimer le client.");
                return;
            }
        } catch (SQLException e) {
            AlertHelper.showError("Erreur", "Vérification impossible: " + e.getMessage());
            return;
        }

        Optional<ButtonType> result = AlertHelper.showConfirmation(
            "Confirmer la suppression",
            "Supprimer le client « " + selected.getFullName() + " » ?\nCette action est irréversible.");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                clientDAO.delete(selected.getId());
                refreshTable();
                AlertHelper.showInfo("Succès", "Client supprimé.");
            } catch (SQLException e) {
                AlertHelper.showError("Erreur", "Impossible de supprimer: " + e.getMessage());
            }
        }
    }

    /**
     * Opens a modal dialog for creating or editing a client.
     * Validates the form before returning the modified client.
     *
     * @param client the client to edit (modified in place)
     * @param isNew  true for "add" mode, false for "edit" mode
     * @return Optional containing the modified client, or empty if cancelled
     */
    private Optional<Client> showClientDialog(Client client, boolean isNew) {
        Dialog<Client> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nouveau client" : "Modifier le client");
        dialog.setHeaderText(isNew ? "Saisir les informations du nouveau client"
                                   : "Modifier les informations de " + client.getFullName());

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtFirstName = new TextField(client.getFirstName());
        TextField txtLastName  = new TextField(client.getLastName());
        TextField txtEmail     = new TextField(client.getEmail());
        TextField txtPhone     = new TextField(client.getPhone());
        TextField txtAddress   = new TextField(client.getAddress());
        DatePicker dpBirthDate = new DatePicker(client.getBirthDate());

        txtFirstName.setPromptText("Prénom");
        txtLastName.setPromptText("Nom");
        txtEmail.setPromptText("exemple@email.com");
        txtPhone.setPromptText("+212 6XX XXX XXX");
        txtAddress.setPromptText("Adresse postale");

        grid.add(new Label("Prénom *"),   0, 0); grid.add(txtFirstName, 1, 0);
        grid.add(new Label("Nom *"),      0, 1); grid.add(txtLastName,  1, 1);
        grid.add(new Label("Email *"),    0, 2); grid.add(txtEmail,     1, 2);
        grid.add(new Label("Téléphone"),  0, 3); grid.add(txtPhone,     1, 3);
        grid.add(new Label("Adresse"),    0, 4); grid.add(txtAddress,   1, 4);
        grid.add(new Label("Naissance"),  0, 5); grid.add(dpBirthDate,  1, 5);

        dialog.getDialogPane().setContent(grid);

        // Validate on the Save button click
        dialog.getDialogPane().lookupButton(saveBtn).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                String error = FormValidator.validateClientForm(
                    txtFirstName.getText(), txtLastName.getText(),
                    txtEmail.getText(), txtPhone.getText());
                if (error != null) {
                    AlertHelper.showError("Formulaire invalide", error);
                    event.consume(); // prevent dialog from closing
                }
            });

        dialog.setResultConverter(button -> {
            if (button == saveBtn) {
                client.setFirstName(txtFirstName.getText().trim());
                client.setLastName(txtLastName.getText().trim());
                client.setEmail(txtEmail.getText().trim());
                client.setPhone(txtPhone.getText().trim());
                client.setAddress(txtAddress.getText().trim());
                client.setBirthDate(dpBirthDate.getValue());
                return client;
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
