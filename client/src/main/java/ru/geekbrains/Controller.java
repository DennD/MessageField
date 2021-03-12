package ru.geekbrains;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller {

    @FXML
    TextArea chatArea;

    @FXML
    TextField messageField, usernameField;

    @FXML
    HBox loginPanel, messagePanel;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public void setUsername(String username) {
        this.username = username;
        if (username != null) {
            loginPanel.setVisible(false);
            loginPanel.setManaged(false);
            messagePanel.setVisible(true);
            messagePanel.setManaged(true);
        } else {
            loginPanel.setVisible(true);
            loginPanel.setManaged(true);
            messagePanel.setVisible(false);
            messagePanel.setManaged(false);
        }
    }

    public void clickSendButton() {
//        if (!messageField.getText().isEmpty()){
//            chatArea.appendText(messageField.getText()+"\n"+"\n");
//        }
        try {
            out.writeUTF(messageField.getText());
            messageField.clear();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно отправить сообщение", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String message = in.readUTF();
                        if (message.startsWith("/login_ok ")) {
                            setUsername(message.split("\\s")[1]);
                            chatArea.clear();
                            break;
                        }
                        if (message.startsWith("/login_failed ")) {
                            String cause = message.split("\\s", 2)[1];
                            chatArea.appendText(cause + "\n");
                        }
//                        if (messageField.equals("/end")){
//                            return;
//                        }
                    }
                    while (true) {
                        String message = in.readUTF();
                        chatArea.appendText(message + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            });
            t.start();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно поключиться к серверу", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void login() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        if (usernameField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Имя пользователя не может быть пустым", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        try {
            out.writeUTF("/login " + usernameField.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        setUsername(null);
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}
