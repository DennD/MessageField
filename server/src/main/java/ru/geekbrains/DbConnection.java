package ru.geekbrains;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DbConnection {
    private static final Logger log = LogManager.getLogger(DbConnection.class);

    private Connection connection;
    private Statement stmt;

    public DbConnection(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/message_list", "root", "123456");
            this.stmt = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            log.throwing (Level.ERROR,e);
            throw new RuntimeException ("Невозможно подключиться к БД");
        }
    }

    public Statement getStmt() {
        return stmt;
    }

    public void close() {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            log.throwing (Level.ERROR,e);
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.throwing (Level.ERROR,e);
            }
        }
    }


}
