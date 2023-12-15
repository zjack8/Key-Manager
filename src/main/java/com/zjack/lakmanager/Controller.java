package com.zjack.lakmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Controller {


    @FXML
    private ListView<String> listview;

    private static List<CustomKey> keyList;

    private static EncryptionManager ec;

    private static final String FILE_PATH = ""; // TODO Configure FILE_PATH
    private static final String KEYS_FILE_NAME = "keys.yml";
    private static final String PUBLIC_FILE_NAME = "public.yml";

    @FXML
    private void initialize() {
        keyList = new ArrayList<>();
        ec = new EncryptionManager();
        load();

    }

    public static void save() {
        // Save keys to yaml
        try (FileWriter fileWriter = new FileWriter(FILE_PATH + KEYS_FILE_NAME)) {
            Map<String, Object> newData = getStringListMap();

            // Dump the data to YAML and write to the file
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml newYaml = new Yaml(options);

            String yamlString = newYaml.dump(newData);
            fileWriter.write(yamlString);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save keys to yaml
        try (FileWriter fileWriter = new FileWriter(FILE_PATH + PUBLIC_FILE_NAME)) {
            Map<String, Object> data = new HashMap<>();
            data.put("public", encodePublicToString(ec.getPublicKey()));
            data.put("private", encodePrivateToString(ec.getPrivateKey()));

            // Dump the data to YAML and write to the file
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml newYaml = new Yaml(options);

            String yamlString = newYaml.dump(data);
            fileWriter.write(yamlString);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void load() {
        Yaml yaml = new Yaml();

        try (FileReader fileReader = new FileReader(FILE_PATH + KEYS_FILE_NAME)) {
            // Parse the YAML file
            Map<String, ?> data = yaml.load(fileReader);

            if (data == null || !data.containsKey("keys")) {
                System.out.println("No valid 'keys' data found in the YAML file.");
                // If the data is empty or not in the expected format, proceed with an empty keyList
                keyList.clear();
            } else {

                List<Map<String, String>> mapList = (List<Map<String, String>>) data.get("keys");

                // Transform the list of maps into a list of keys and decrypt
                for (Map<String, String> keyInfo : mapList) {
                    CustomKey key = new CustomKey(
                            ec.decrypt(keyInfo.get("user")),
                            ec.decrypt(keyInfo.get("key")),
                            ec.decrypt(keyInfo.get("expiryDate"))
                    );
                    keyList.add(key);
                }
            }

            // Set the transformed list to the ListView
            ObservableList<String> items = formatList(keyList);
            listview.setItems(items);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static Map<String, Object> getStringListMap() {
        Map<String, Object> newData = new HashMap<>();
        List<Map<String, String>> newMapList = new ArrayList<>();

        // Transform the list of keys into a list of maps
        for (CustomKey key : keyList) {
            Map<String, String> keyInfo = new HashMap<>();
            keyInfo.put("user", ec.encrypt(key.getUser()));
            keyInfo.put("key", ec.encrypt(key.getKey()));
            keyInfo.put("expiryDate", ec.encrypt(key.getExpiryDate()));
            newMapList.add(keyInfo);
        }

        newData.put("keys", newMapList);
        return newData;
    }


    private ObservableList<String> formatList(List<CustomKey> keyList) {
        ObservableList<String> stringList = FXCollections.observableArrayList();

        for (CustomKey key: keyList) {
            stringList.add("User Name: " + key.getUser() +
                    ", Key Name: " + key.getKey() +
                    ", Expiry Date: " + key.getExpiryDate());
        }

        return stringList;
    }

    @FXML
    private void create() {
        // Create new instance of key
        Optional<CustomKey> optional = showCreateDialog();

        if (optional.isPresent()) {
            CustomKey key = optional.get();
            // Add key to the listview
            listview.getItems().add(("User Name: " + key.getUser() +
                    ", Key Name: " + key.getKey() +
                    ", Expiry Date: " + key.getExpiryDate()));

            // Add key to the keyList
            keyList.add(key);
        }
        save();
    }

    private Optional<CustomKey> showCreateDialog() {
        // Create a custom dialog
        Dialog<CustomKey> dialog = new Dialog<>();
        dialog.setTitle("Custom Dialog");
        dialog.initModality(Modality.WINDOW_MODAL);

        // Set up the GridPane for layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Add labels and text fields to the GridPane
        TextField field1 = new TextField();
        Label field2 = new Label(generateKey());
        DatePicker field3 = new DatePicker();

        // Set the initial date to three months from the current date
        LocalDate currentDate = LocalDate.now();
        LocalDate futureDate = currentDate.plusMonths(3);
        field3.setValue(futureDate);

        // Refresh Button
        // Load a refresh icon image (you can replace this with your own image)
        Image refreshImage = new Image(Objects.requireNonNull(KeysApplication.class.getResource("refresh_icon.png")).toString());

        // Create an ImageView with the refresh icon
        ImageView refreshIcon = new ImageView(refreshImage);
        refreshIcon.setFitWidth(16);  // Set the width of the icon
        refreshIcon.setFitHeight(16); // Set the height of the icon

        Button refresh = new Button("", refreshIcon);
        refresh.setOnAction(event -> field2.setText(generateKey()));

        grid.add(new Label("User Name:"), 0, 0);
        grid.add(field1, 1, 0);
        grid.add(new Label("Generated Key:"), 0, 1);
        grid.add(field2, 1, 1);
        grid.add(refresh,2, 1);
        grid.add(new Label("Expiry Date:"), 0, 2);
        grid.add(field3, 1, 2);

        // Set the content of the dialog to the GridPane
        dialog.getDialogPane().setContent(grid);

        // Set up the buttons (Accept and Close)
        ButtonType acceptButtonType = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(acceptButtonType, ButtonType.CLOSE);

        // Enable/disable Accept button based on text field input
        dialog.getDialogPane().lookupButton(acceptButtonType).setDisable(true);

        field1.textProperty().addListener((observable, oldValue, newValue) ->
                dialog.getDialogPane().lookupButton(acceptButtonType).setDisable(newValue.trim().isEmpty()));

        // Set the result converter for the dialog
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == acceptButtonType) {
                // Return the result object with the entered values
                return new CustomKey(field1.getText(), field2.getText(), field3.getValue().format(DateTimeFormatter.ofPattern("dd MMM y")));
            }
            return null;
        });

        // show and return a key when completed
        return dialog.showAndWait();
    }

    private String generateKey() {
        int length = 12;
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^*+-=?~";
        StringBuilder randomStringBuilder = new StringBuilder(length);
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            randomStringBuilder.append(randomChar);
        }

        return randomStringBuilder.toString();
    }

    private static String encodePublicToString(PublicKey publicKey) {
        byte[] encodedKey = publicKey.getEncoded();
        return Base64.getEncoder().encodeToString(encodedKey);
    }

    private static String encodePrivateToString(PrivateKey privateKey) {
        byte[] encodedKey = privateKey.getEncoded();
        return Base64.getEncoder().encodeToString(encodedKey);
    }

    @FXML
    private void remove() {

        int index = listview.getSelectionModel().getSelectedIndex();

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Delete Request");
        confirmationAlert.setHeaderText("User Name: " + keyList.get(index).getUser() + " Key: " + keyList.get(index).getUser());
        confirmationAlert.setContentText("Are you sure you want to delete this key?");

        // Customize the button types (in this case, Yes and No)
        confirmationAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        // Show the confirmation alert and wait for the user's response
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        // Check the user's response
        if (result.isPresent() && result.get() == ButtonType.YES) {

            keyList.remove(index);
            listview.setItems(formatList(keyList));

        }
        listview.getSelectionModel().selectFirst();
        save();
    }

    @FXML
    private void copy() {

        int index = listview.getSelectionModel().getSelectedIndex();
        CustomKey key = keyList.get(index);
        String textToCopy = key.getKey();

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(textToCopy);
        clipboard.setContent(content);

    }
}