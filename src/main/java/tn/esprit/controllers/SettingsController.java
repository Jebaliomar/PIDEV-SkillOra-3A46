package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.tools.AvatarService;
import tn.esprit.tools.ThemeManager;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private HBox successBox;
    @FXML private Label successLabel;
    @FXML private HBox errorBox;
    @FXML private Label errorLabel;

    @FXML private Label currentAvatarLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private DatePicker dobPicker;
    @FXML private TextArea bioArea;

    @FXML private ComboBox<String> countryCodeCombo;
    @FXML private TextField phoneField;
    @FXML private TextField countryField;

    @FXML private TextField fieldOfStudyField;
    @FXML private TextField universityField;

    @FXML private TextField emailField;
    @FXML private Label accountTypeBadge;
    @FXML private Label memberSinceLabel;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        genderCombo.setItems(FXCollections.observableArrayList("Male", "Female", "Other", "Prefer not to say"));
        countryCodeCombo.setItems(FXCollections.observableArrayList(
                "+1 US", "+44 UK", "+33 FR", "+49 DE", "+39 IT", "+34 ES",
                "+216 TN", "+213 DZ", "+212 MA", "+20 EG", "+971 AE", "+966 SA",
                "+91 IN", "+86 CN", "+81 JP", "+82 KR", "+61 AU", "+55 BR", "+52 MX", "+7 RU"
        ));
        countryCodeCombo.setValue("+216 TN");

        User u = StudentLayoutController.getCurrentUser();
        if (u == null) return;

        updateAvatarLabel(u.getAvatarType());
        firstNameField.setText(safe(u.getFirstName()));
        lastNameField.setText(safe(u.getLastName()));
        genderCombo.setValue(u.getGender());
        if (u.getDateOfBirth() != null) dobPicker.setValue(u.getDateOfBirth());
        bioArea.setText(safe(u.getBio()));

        String phone = safe(u.getPhone());
        if (phone.startsWith("+")) {
            int space = phone.indexOf(' ');
            if (space > 0) {
                String code = phone.substring(0, space);
                for (String item : countryCodeCombo.getItems()) {
                    if (item.startsWith(code + " ")) {
                        countryCodeCombo.setValue(item);
                        break;
                    }
                }
                phoneField.setText(phone.substring(space + 1));
            } else {
                phoneField.setText(phone);
            }
        } else {
            phoneField.setText(phone);
        }

        countryField.setText(safe(u.getCountry()));
        fieldOfStudyField.setText(safe(u.getFieldOfStudy()));
        universityField.setText(safe(u.getUniversity()));
        emailField.setText(safe(u.getEmail()));

        if (u.getCreatedAt() != null) {
            memberSinceLabel.setText(u.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        }
    }

    @FXML
    public void handleSave() {
        hideMessages();
        User u = StudentLayoutController.getCurrentUser();
        if (u == null) return;

        String bio = bioArea.getText();
        if (bio != null && bio.length() > 500) {
            showError("Bio must be 500 characters or fewer.");
            return;
        }

        u.setFirstName(firstNameField.getText().trim());
        u.setLastName(lastNameField.getText().trim());
        u.setGender(genderCombo.getValue());
        u.setDateOfBirth(dobPicker.getValue());
        u.setBio(bio);

        String code = countryCodeCombo.getValue();
        String codeOnly = code == null ? "" : code.split(" ")[0];
        String number = phoneField.getText() == null ? "" : phoneField.getText().trim();
        u.setPhone(number.isEmpty() ? null : (codeOnly + " " + number).trim());

        u.setCountry(countryField.getText().trim());
        u.setFieldOfStudy(fieldOfStudyField.getText().trim());
        u.setUniversity(universityField.getText().trim());

        try {
            userService.update(u);
            showSuccess("Your settings have been saved.");
        } catch (Exception e) {
            showError("Could not save: " + e.getMessage());
        }
    }

    @FXML
    public void handleChangeAvatar() {
        hideMessages();
        User u = StudentLayoutController.getCurrentUser();
        if (u == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AvatarPicker.fxml"));
            Parent root = loader.load();
            AvatarPickerController ctrl = loader.getController();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Select Your Avatar");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            dialog.setScene(scene);
            dialog.showAndWait();

            String picked = ctrl.getConfirmedFilename();
            if (picked != null) {
                userService.updateAvatar(u.getId(), picked);
                u.setAvatarType(picked);
                updateAvatarLabel(picked);
                showSuccess("Avatar updated. It will appear on your profile.");
            }
        } catch (Exception e) {
            showError("Could not open avatar picker: " + e.getMessage());
        }
    }

    private void updateAvatarLabel(String avatarType) {
        if (avatarType == null || avatarType.isEmpty()) {
            currentAvatarLabel.setText("No avatar selected");
        } else {
            currentAvatarLabel.setText("Current: " + AvatarService.displayName(avatarType));
        }
    }

    @FXML
    public void handleCancel() {
        StudentLayoutController.getInstance().showProfile();
    }

    @FXML
    public void handleChangePassword() {
        hideMessages();
        User u = StudentLayoutController.getCurrentUser();
        if (u == null) return;

        String current = currentPasswordField.getText();
        String newPass = newPasswordField.getText();

        if (current == null || current.isEmpty() || newPass == null || newPass.isEmpty()) {
            showError("Please fill both password fields.");
            return;
        }
        if (newPass.length() < 8) {
            showError("New password must be at least 8 characters.");
            return;
        }

        try {
            User verified = userService.authenticate(u.getEmail(), current);
            if (verified == null) {
                showError("Current password is incorrect.");
                return;
            }
            userService.updatePassword(u.getId(), newPass);
            currentPasswordField.clear();
            newPasswordField.clear();
            showSuccess("Password updated successfully.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successBox.setVisible(true);
        successBox.setManaged(true);
        errorBox.setVisible(false);
        errorBox.setManaged(false);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorBox.setVisible(true);
        errorBox.setManaged(true);
        successBox.setVisible(false);
        successBox.setManaged(false);
    }

    private void hideMessages() {
        successBox.setVisible(false);
        successBox.setManaged(false);
        errorBox.setVisible(false);
        errorBox.setManaged(false);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
