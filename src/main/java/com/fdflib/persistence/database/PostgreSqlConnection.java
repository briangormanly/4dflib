package com.fdflib.persistence.database;

import com.fdflib.util.FdfSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by brian.gormanly on 1/14/16.
 */
public class PostgreSqlConnection {
    private static final PostgreSqlConnection INSTANCE = new PostgreSqlConnection();
    static Logger fdfLog = LoggerFactory.getLogger(MySqlConnection.class);

    private PostgreSqlConnection() {

    }

    public static PostgreSqlConnection getInstance() {
        return INSTANCE;
    }

    private Connection connection;
    //private Session session;

    private void connect() throws SQLException {

        fdfLog.debug("Establishing mysql connection with regular credentials");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            connection = DriverManager.getConnection(FdfSettings.returnDBConnectionString(), FdfSettings.DB_USER
                    , FdfSettings.DB_PASSWORD);
        } catch (SQLException e) {
            fdfLog.warn("SQL Error: {}\nDescription: ", e.getErrorCode(), e.getMessage());

            // - Unknown database 'testing_db'
            if(e.getErrorCode() == 1049) {
                fdfLog.error("Database did not exist: {}", e.getMessage());
            }
            else {
                fdfLog.error(e.getStackTrace().toString());
            }
        }
    }

    private void connectWithoutDatabase() throws SQLException {

        fdfLog.debug("Establishing mysql connection with root credentials");
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            connection = DriverManager.getConnection(FdfSettings.returnDBConnectionStringWithoutDatabase(),
                    FdfSettings.DB_ROOT_USER, FdfSettings.DB_ROOT_PASSWORD);
        } catch (SQLException e) {
            fdfLog.warn("SQL Error: {}\nDescription: ", e.getErrorCode(), e.getMessage());

            // - Unknown database 'testing_db'
            if(e.getErrorCode() == 1049) {
                fdfLog.error("Database did not exist: {}", e.getMessage());
            }
            else {
                fdfLog.error(e.getStackTrace().toString());
            }
        }
    }

    public Connection getSession() throws SQLException  {
        if(this.connection == null) {
            this.connect();
        }
        return this.connection;
    }

    public Connection getNoDBSession() throws SQLException  {
        if(this.connection == null) {
            this.connectWithoutDatabase();
        }
        return this.connection;
    }

    public void close() {

        fdfLog.debug("Closing mysql database connection.");
        if (this.connection != null) {
            try {
                this.connection.close();
            }
            catch (SQLException ignore) {
            }
            this.connection = null;
        }

    }
}
