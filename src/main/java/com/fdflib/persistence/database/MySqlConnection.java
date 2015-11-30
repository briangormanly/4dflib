/**
 * 4DFLib
 * Copyright (c) 2015 Brian Gormanly
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

    private Connection connection;
    //private Session session;

    private void connect() throws SQLException {

        fdfLog.debug("Establishing mysql connection with regular credentials");
        try {
            Class.forName("com.mysql.jdbc.Driver");
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
