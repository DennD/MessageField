package ru.geekbrains;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    TextArea chatArea;

    @FXML
    TextField messageField, loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    HBox loginPanel, messagePanel;

    @FXML
    ListView clientsList;

    @FXML
    VBox rootElement;


    private Network network;
    private HistoryManager historyManager;
    private String username;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setUsername (null);
        historyManager = new HistoryManager ();
        network = new Network ();


        network.setOnAuthFailedCallback(args -> chatArea.appendText((String)args[0] + "\n"));

        network.setOnAuthOkCallback(args -> {
            String message = (String)args[0];
            setUsername(message.split("\\s")[2]);
            historyManager.init(message.split("\\s")[1]);
            chatArea.clear();
            chatArea.appendText(historyManager.load());
        });

        network.setOnMessageReceivedCallback(args -> {
            String message = (String)args[0];
            if (message.startsWith("/")) {
                if (message.startsWith("/clients_list ")) {
                    String[] tokens = message.split("\\s");
                    Platform.runLater(() -> {
                        clientsList.getItems().clear();
                        for (int i = 1; i < tokens.length; i++) {
                            clientsList.getItems().add(tokens[i]);
                        }
                    });
                }
                if (message.equals ("/exit")) {
                    chatArea.clear ();
                    loginField.clear ();
                    passwordField.clear ();
                    setUsername (null);
                }
                return;
            }
            historyManager.write(message + "\n");
            chatArea.appendText(message + "\n");
        });

        network.setOnDisconnectCallback(args -> {
            setUsername(null);
            historyManager.close();
        });

        historyManager = new HistoryManager();
    }



    public void setUsername(String username) {
        this.username = username;
        boolean usernameIsNull = username == null;
        loginPanel.setVisible (usernameIsNull);
        loginPanel.setManaged (usernameIsNull);
        messagePanel.setVisible (!usernameIsNull);
        messagePanel.setManaged (!usernameIsNull);
        clientsList.setVisible (!usernameIsNull);
        clientsList.setManaged (!usernameIsNull);
    }

    public void clickSendButton() {
        try {
            if (!messageField.getText ().isEmpty ()) {
                network.sendMessage (messageField.getText ());
                messageField.clear ();
            }
        } catch (IOException e) {
            showErrorAlert ("Невозможно отправить сообщение");
        }
    }

    public void login() {
        if (loginField.getText ().isEmpty () && passwordField.getText ().isEmpty ()) {
            showErrorAlert ("Логин и пароль не могут быть пустыми");
            return;
        }

        if (!network.isConnected()) {
            try {
                network.connect(8189);
            } catch (IOException e) {
                showErrorAlert("Невозможно подключиться к серверу на порт: " + 8189);
                return;
            }
        }

        try {
            network.tryToLogin(loginField.getText(), passwordField.getText());
        } catch (IOException e) {
            showErrorAlert("Невозможно отправить данные пользователя");
        }
    }

    public void logout() {
        try {
            network.sendMessage ("/exit");
        } catch (IOException e) {
            e.printStackTrace ();
        }
    }

    public void exit() {
        network.disconnect();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert (Alert.AlertType.ERROR);
        alert.setContentText (message);
        alert.setTitle ("Message List FX");
        alert.setHeaderText (null);
        alert.showAndWait ();
    }

}
