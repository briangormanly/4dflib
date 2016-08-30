package com.fdflib.persistence.database;

import com.fdflib.util.FdfSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by brian.gormanly on 8/30/16.
 */
public class HSqlConnection {

    private static final HSqlConnection INSTANCE = new HSqlConnection();
    static Logger fdfLog = LoggerFactory.getLogger(HSqlConnection.class);

    private HSqlConnection() {

    }

    public static HSqlConnection getInstance() {
        return INSTANCE;
    }

    public Connection getSession() throws SQLException {
        fdfLog.debug("Establishing hsql connection with regular credentials");
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR: failed to load HSQLDB JDBC driver.");
            e.printStackTrace();
        }

        try {
            Connection connection = DriverManager.getConnection(FdfSettings.returnDBConnectionString(),
                    FdfSettings.DB_USER, FdfSettings.DB_PASSWORD);
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
        fdfLog.debug("Establishing hsql connection with root credentials");
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR: failed to load HSQLDB JDBC driver.");
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

        fdfLog.debug("Closing hsql database connection.");
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        connection = null;

    }

}
