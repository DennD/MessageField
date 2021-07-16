package ru.geekbrains;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final Logger log = LogManager.getLogger (Server.class);

    private int port;
    private List<ClientHandler> clients;
    private List<String> login_password;
    private int countMessage = 0;
    private AuthenticationProvider authentificationProvider;

    public int getCountMessage() {
        return countMessage;
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authentificationProvider;
    }

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<> ();
        this.authentificationProvider = new DbAuthenticationProvider ();
        this.authentificationProvider.init ();
        try (ServerSocket serverSocket = new ServerSocket (port)) {
            log.info ("Сервер запущен на порту " + port);
            while (true) {
                    log.info ("Ожидание клиентов");
                    Socket socket = serverSocket.accept ();
                    log.info ("Клиент подключился");
                    new ClientHandler (this, socket);
            }
        } catch (IOException e) {
            log.throwing (Level.ERROR,e);
        } finally {
            authentificationProvider.shutdown ();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add (clientHandler);
        serverMessage ("Server: " + clientHandler.getUsername () + " вошел в чат");
        updateClientsList ();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove (clientHandler);
        serverMessage ("Server: " + clientHandler.getUsername () + " вышел из чата");
        updateClientsList ();
    }

    public synchronized void broadcastMessage(String message) {
        countMessage++;
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage (message);
        }

    }

    public synchronized void serverMessage(String cmdMessage) {
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage (cmdMessage);
        }
    }

    public synchronized void sendPrivateMessage(String message, ClientHandler clientHandlerSender, String recepientUsername) {
        for (ClientHandler clientHandler : clients) {
            if (recepientUsername.equals (clientHandler.getUsername ())) {
                clientHandler.sendMessage (clientHandlerSender.getUsername () + " -> " + recepientUsername + ": " + message);
                clientHandlerSender.sendMessage (clientHandlerSender.getUsername () + " -> " + recepientUsername + ": " + message);
                return;
            }
        }
        clientHandlerSender.sendMessage ("Server: Невозможно отправить сообщение пользователю " + recepientUsername + ". Такого пользователя нет в сети.");
    }

    public synchronized boolean isUserOnline(String username) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getUsername ().equals (username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void updateClientsList() {
        StringBuilder stringBuilder = new StringBuilder ("/clients_list ");
        for (ClientHandler clientHandler : clients) {
            stringBuilder.append (clientHandler.getUsername ()).append (" ");
        }
        stringBuilder.setLength (stringBuilder.length () - 1);
        String clientsList = stringBuilder.toString ();
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage (clientsList);
        }
    }
}

