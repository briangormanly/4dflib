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

import com.fdflib.persistence.queries.JdbcConnection;
import com.fdflib.util.FdfSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by brian.gormanly on 5/18/15.
 */
public class MySqlConnection extends JdbcConnection {

    private static final MySqlConnection INSTANCE = new MySqlConnection();
    static Logger fdfLog = LoggerFactory.getLogger(MySqlConnection.class);

    private MySqlConnection() {

    }

    public static MySqlConnection getInstance() {
        return INSTANCE;
    }

    @Override
    public Connection get4dfDbConnection() throws SQLException {
        return super.get4dfDbConnection();
    }

    @Override
    public Connection get4dfDbRootConnection() throws SQLException {
        return super.get4dfDbRootConnection();
    }

    @Override
    public void close4dfDbSession(Connection connection) throws SQLException {
        super.close4dfDbSession(connection);
    }

}
