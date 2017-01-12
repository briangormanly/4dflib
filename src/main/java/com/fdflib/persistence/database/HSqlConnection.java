package com.fdflib.persistence.database;

import com.fdflib.util.FdfSettings;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

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
public class HSqlConnection {

    private static final HSqlConnection INSTANCE = new HSqlConnection();
    private static Logger fdfLog = LoggerFactory.getLogger(HSqlConnection.class);

    private Server server;

    private HSqlConnection() {}

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

        HsqlProperties p = new HsqlProperties();
        if(FdfSettings.HSQL_DB_FILE) {
            p.setProperty("server.database.0", "file:" + FdfSettings.HQSL_DB_FILE_LOCATION + ";");
        }
        else {
            p.setProperty("server.database.0", "mem:" + FdfSettings.DB_NAME + ";");
        }
        p.setProperty("server.dbname.0", FdfSettings.DB_NAME);
        p.setProperty("server.port", FdfSettings.DB_PORT);
        server = new Server();

        try {
            server.setProperties(p);
            server.setLogWriter(null); // can use custom writer
            server.setErrWriter(null); // can use custom writer
            server.start();
        } catch (IOException | ServerAcl.AclFormatException e) {
            e.printStackTrace();
        }

        if(server.getState() > 0) {

            try {
                return DriverManager.getConnection(DatabaseUtil.returnDBConnectionString(),
                        FdfSettings.DB_USER, FdfSettings.DB_PASSWORD);
            }
            catch (SQLException e) {
                fdfLog.warn("SQL Error: {}\nDescription: ", e.getErrorCode(), e.getMessage());

                // - Unknown database 'testing_db'
                if (e.getErrorCode() == 1049) {
                    fdfLog.error("Database did not exist: {}", e.getMessage());
                } else {
                    fdfLog.error(Arrays.toString(e.getStackTrace()));
                }
            }
        }
        else {
            fdfLog.error("hsql server not running!");
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

        HsqlProperties p = new HsqlProperties();
        if(FdfSettings.HSQL_DB_FILE) {
            p.setProperty("server.database.0", "file:" + FdfSettings.HQSL_DB_FILE_LOCATION + ";");
        }
        else {
            p.setProperty("server.database.0", "mem:" + FdfSettings.DB_NAME + ";");
        }
        p.setProperty("server.dbname.0", FdfSettings.DB_NAME);
        p.setProperty("server.port", FdfSettings.DB_PORT);
        server = new Server();

        try {
            server.setProperties(p);
            server.setLogWriter(null); // can use custom writer
            server.setErrWriter(null); // can use custom writer
            server.start();
        }
        catch (IOException | ServerAcl.AclFormatException e) {
            e.printStackTrace();
        }

        if(server.getState() > 0) {

            try {
                return DriverManager.getConnection(
                        DatabaseUtil.returnDBConnectionStringWithoutDatabase(),
                        FdfSettings.DB_ROOT_USER, FdfSettings.DB_ROOT_PASSWORD);
            }
            catch (SQLException e) {
                fdfLog.warn("SQL Error: {}\nDescription: ", e.getErrorCode(), e.getMessage());

                // - Unknown database 'testing_db'
                if (e.getErrorCode() == 1049) {
                    fdfLog.error("Database did not exist: {}", e.getMessage());
                } else {
                    fdfLog.error(Arrays.toString(e.getStackTrace()));
                }
            }
        }
        else {
            fdfLog.error("hsql server not running!");
        }
        return null;
    }

    public void close(Connection connection) throws SQLException {
        fdfLog.debug("Closing hsql database connection.");
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        server.shutdown();
    }

}
