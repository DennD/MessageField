package ru.geekbrains;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Optional;

public class DbAuthenticationProvider implements AuthenticationProvider {
    private static final Logger log = LogManager.getLogger(DbAuthenticationProvider.class);

    private DbConnection dbConnection;

    @Override
    public void init() {
        dbConnection = new DbConnection();
    }

    @Override
    public Optional<String> getNicknameByLoginAndPassword(String login, String password) {
        String query = String.format("select username from users where login = '%s' and password = '%s';", login, password);
        try (ResultSet resultSet = dbConnection.getStmt().executeQuery(query)) {
            if (resultSet.next()) {
                return Optional.of (resultSet.getString("username"));
            }
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
            return Optional.empty ();
        }
        return Optional.empty ();
    }

    @Override
    public void changeNickname(String oldNickname, String newNickname) {
        String query = String.format("update users set username = '%s' where username = '%s';", newNickname, oldNickname);
        try {
            dbConnection.getStmt().executeUpdate(query);
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
        }
    }

    @Override
    public boolean isNickBusy(String nickname) {
        String query = String.format("select id from users where username = '%s';", nickname);
        try (ResultSet resultSet = dbConnection.getStmt().executeQuery(query)) {
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
        }
        return false;
    }

    @Override
    public void shutdown() {
        dbConnection.close();

    }
}
