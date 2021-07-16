package ru.geekbrains;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

public class ClientHandler {
    private static final Logger log = LogManager.getLogger(ClientHandler.class);

    private String username;
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;


    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                while (true) {
                    String message = in.readUTF();
                    if (executeAuthenticationMessage(message)) {
                        break;
                    }
                }
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (logoutCommand (message)) {
                            break;
                        }
                        executeCommand(message);
                        continue;
                    }
                    server.broadcastMessage(username + ": " + message);
                }
            } catch (IOException e) {
                log.throwing(Level.ERROR, e);
            } finally {
                disconnect();
            }
        }).start();
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log.throwing(Level.ERROR, e);
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
        }
    }

    private boolean logoutCommand(String message) {
        if (message.equals("/exit")) {
            sendMessage(message);
            return true;
        }
        return false;
    }

    private boolean executeAuthenticationMessage(String message) {
        if (message.startsWith("/login_")) {
            String[] tokens = message.split("_");
            if (tokens.length != 3) {
                sendMessage("/login_failed Введите имя пользователя и пароль");
                return false;
            }
            String login = tokens[1];
            String password = tokens[2];

            Optional<String> userNickname = server.getAuthenticationProvider().getNicknameByLoginAndPassword(login, password);
            if (!userNickname.isPresent ()) {
                sendMessage("/login_failed Некорректный логин и пароль");
                return false;
            }
            if (server.isUserOnline(userNickname.get ())) {
                sendMessage("/login_failed Учетная запись уже используется");
                return false;
            }
            username = userNickname.get ();
            sendMessage("/login_ok " + username + " " + login);
            server.subscribe(this);
            return true;
        }
        return false;
    }

    private void executeCommand(String cmd) {
        if (cmd.startsWith("/w ")) {
            String[] tokens = cmd.split("\\s+", 3);
            if (tokens.length != 3) {
                sendMessage("Server: Введена некорректная команда");
                return;
            }
            server.sendPrivateMessage(tokens[2], this, tokens[1]);
            return;
        }
        if (cmd.equals("/who_am_i")) {
            sendMessage("Server: Ваш ник - " + username);
            return;
        }
        if (cmd.equals("/stat")) {
            sendMessage("Server: Количество сообщений чата - " + server.getCountMessage());
            return;
        }
        if (cmd.startsWith("/change_nick ")) {
            String[] tokens = cmd.split("\\s+");
            if (tokens.length != 2) {
                sendMessage("Server: Введена некорректная команда");
                return;
            }
            String newNickname = tokens[1];
            if (server.getAuthenticationProvider().isNickBusy(tokens[1])) {
                sendMessage("Server: Такой никнейм уже занят");
                return;
            }
            server.getAuthenticationProvider().changeNickname(username, newNickname);
            server.serverMessage("Server: " + username + " сменил ник на " + newNickname);
            username = newNickname;
            server.updateClientsList();
        }
    }


}
