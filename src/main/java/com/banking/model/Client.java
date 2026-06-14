package com.banking.model;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a bank client.
 * Uses JavaFX properties for direct {@link javafx.scene.control.TableView} binding.
 */
public class Client {

    private final IntegerProperty id;
    private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty email;
    private final StringProperty phone;
    private final StringProperty address;
    private final ObjectProperty<LocalDate> birthDate;
    private final ObjectProperty<LocalDateTime> createdAt;

    /** Creates an empty client for use in "new client" forms. */
    public Client() {
        this(0, "", "", "", "", "", null, LocalDateTime.now());
    }

    /**
     * Creates a client with all fields.
     *
     * @param id        primary key (0 for new, not-yet-persisted clients)
     * @param firstName first name
     * @param lastName  last name
     * @param email     unique email address
     * @param phone     phone number (optional)
     * @param address   postal address (optional)
     * @param birthDate date of birth (optional)
     * @param createdAt account creation timestamp
     */
    public Client(int id, String firstName, String lastName, String email,
                  String phone, String address, LocalDate birthDate,
                  LocalDateTime createdAt) {
        this.id        = new SimpleIntegerProperty(id);
        this.firstName = new SimpleStringProperty(firstName);
        this.lastName  = new SimpleStringProperty(lastName);
        this.email     = new SimpleStringProperty(email);
        this.phone     = new SimpleStringProperty(phone != null ? phone : "");
        this.address   = new SimpleStringProperty(address != null ? address : "");
        this.birthDate = new SimpleObjectProperty<>(birthDate);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
    }

    // --- Property accessors (required for TableView column binding) ---

    /** @return the id property */
    public IntegerProperty idProperty() { return id; }

    /** @return the firstName property */
    public StringProperty firstNameProperty() { return firstName; }

    /** @return the lastName property */
    public StringProperty lastNameProperty() { return lastName; }

    /** @return the email property */
    public StringProperty emailProperty() { return email; }

    /** @return the phone property */
    public StringProperty phoneProperty() { return phone; }

    /** @return the address property */
    public StringProperty addressProperty() { return address; }

    /** @return the birthDate property */
    public ObjectProperty<LocalDate> birthDateProperty() { return birthDate; }

    /** @return the createdAt property */
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }

    // --- Standard getters/setters ---

    /** @return the client's database id */
    public int getId() { return id.get(); }

    /** @return the first name */
    public String getFirstName() { return firstName.get(); }

    /** @return the last name */
    public String getLastName() { return lastName.get(); }

    /** @return the email address */
    public String getEmail() { return email.get(); }

    /** @return the phone number */
    public String getPhone() { return phone.get(); }

    /** @return the postal address */
    public String getAddress() { return address.get(); }

    /** @return the date of birth, or null if not set */
    public LocalDate getBirthDate() { return birthDate.get(); }

    /** @return the account creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt.get(); }

    /** @param v the new id (set after DB insert) */
    public void setId(int v) { id.set(v); }

    /** @param v the new first name */
    public void setFirstName(String v) { firstName.set(v); }

    /** @param v the new last name */
    public void setLastName(String v) { lastName.set(v); }

    /** @param v the new email address */
    public void setEmail(String v) { email.set(v); }

    /** @param v the new phone number */
    public void setPhone(String v) { phone.set(v != null ? v : ""); }

    /** @param v the new postal address */
    public void setAddress(String v) { address.set(v != null ? v : ""); }

    /** @param v the new date of birth */
    public void setBirthDate(LocalDate v) { birthDate.set(v); }

    /** @param v the new creation timestamp */
    public void setCreatedAt(LocalDateTime v) { createdAt.set(v); }

    /**
     * Returns the full name formatted as "First Last".
     * Used by ComboBox display and toString.
     *
     * @return formatted full name
     */
    public String getFullName() {
        return firstName.get() + " " + lastName.get();
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
