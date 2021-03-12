package ru.geekbrains;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private String username;
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private static int countMessage = 0;

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
                    if (message.startsWith("/login ")) {
                        String usernameFromLogin = message.split("\\s")[1];
                        if(server.isNickBusy(usernameFromLogin)) {
                            sendMessage("/login_failed Curent nickname is already used");
                            continue;
                        }
                        username = usernameFromLogin;
                        sendMessage("/login_ok " + username);
                        server.subscribe(this);
                        break;
                    }
                }
                while (true) {
                    String message = in.readUTF();
                    server.broadcastMessage(username + ": " + message);
//                    if (message.startsWith("/")) {
//                        if (message.equals("/stat")) {
//                            out.writeUTF("Количество сообщений - " + countMessage);
//                            continue;
//                        }
//                    }
//            if(message.equals("/end")){
//                out.writeUTF(message);
//                break;
//            }
//                    System.out.println(message);
//                    sendMessage("ECHO"+message);
//                    countMessage++;
                }
            } catch (IOException e) {
                e.printStackTrace();
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
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }
}
