package com.banking.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

/**
 * Static factory methods for showing consistent JavaFX alert dialogs.
 */
public class AlertHelper {

    private AlertHelper() {}

    /**
     * Shows a modal error dialog.
     *
     * @param title   the dialog title
     * @param content the error message
     */
    public static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Shows a modal information dialog.
     *
     * @param title   the dialog title
     * @param content the informational message
     */
    public static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Shows a modal confirmation dialog with OK and Cancel buttons.
     *
     * @param title   the dialog title
     * @param content the confirmation question
     * @return the button the user clicked
     */
    public static Optional<ButtonType> showConfirmation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait();
    }

    /**
     * Shows a modal warning dialog.
     *
     * @param title   the dialog title
     * @param content the warning message
     */
    public static void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
