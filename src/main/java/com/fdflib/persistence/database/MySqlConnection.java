/**
 * 4DFLib
 * Copyright (c) 2015-2016 Brian Gormanly
 * 4dflib.com
 *
 * 4DFLib is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.fdflib.persistence.database;

import com.fdflib.util.FdfSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by brian.gormanly on 5/18/15.
 */
public class MySqlConnection {

    private static final MySqlConnection INSTANCE = new MySqlConnection();
    static Logger fdfLog = LoggerFactory.getLogger(MySqlConnection.class);

    private MySqlConnection() {

    }

    public static MySqlConnection getInstance() {
        return INSTANCE;
    }

    public Connection getSession() throws SQLException  {
        fdfLog.debug("Establishing mysql connection with regular credentials");
        try {
            Class.forName("com.mysql.jdbc.Driver");
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
        fdfLog.debug("Establishing mysql connection with root credentials");
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

        fdfLog.debug("Closing mysql database connection.");
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        connection = null;

    }
}
