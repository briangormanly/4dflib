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
    static Logger fdfLog = LoggerFactory.getLogger(PostgreSqlConnection.class);

    private PostgreSqlConnection() {

    }

    public static PostgreSqlConnection getInstance() {
        return INSTANCE;
    }

    public Connection getSession() throws SQLException  {
        fdfLog.debug("Establishing postgresql connection with regular credentials");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Connection connection = DriverManager.getConnection(FdfSettings.returnDBConnectionString(), FdfSettings.DB_USER
                    , FdfSettings.DB_PASSWORD);

            return connection;

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
        return null;
    }

    public Connection getNoDBSession() throws SQLException  {
        fdfLog.debug("Establishing postgresql connection with root credentials");
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Connection connection = DriverManager.getConnection(FdfSettings.returnDBConnectionStringWithoutDatabase(),
                    FdfSettings.DB_ROOT_USER, FdfSettings.DB_ROOT_PASSWORD);

            return connection;

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

        return null;
    }

    public void close(Connection connection) throws SQLException {

        fdfLog.debug("Closing postgresql database connection.");
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
