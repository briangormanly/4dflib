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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by brian.gormanly on 1/14/16.
 */
public class PostgreSqlConnection extends JdbcConnection {

    private static final PostgreSqlConnection INSTANCE = new PostgreSqlConnection();
    static Logger fdfLog = LoggerFactory.getLogger(PostgreSqlConnection.class);

    private PostgreSqlConnection() {

    }

    public static PostgreSqlConnection getInstance() {
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

    /**
     * Force lower case for postres
     * @return
     * @throws SQLException
     */
    @Override
    protected Connection getPlainConnection() throws SQLException {
        fdfLog.debug("Establishing mysql connection with regular PostgreSQL credentials");
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            fdfLog.error("Database driver error!");
            fdfLog.error(e.getStackTrace().toString());
        }

        return DriverManager.getConnection(DatabaseUtil.returnDBConnectionString(),
                FdfSettings.DB_USER.toLowerCase(), FdfSettings.DB_PASSWORD);
    }

    /**
     * Force lower case for postres
     * @return
     * @throws SQLException
     */
    @Override
    protected Connection getNoDbPlainConnection() throws SQLException {
        fdfLog.debug("Establishing mysql connection with root PostgreSQL credentials");
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            fdfLog.error("Database driver error!");
            fdfLog.error(e.getStackTrace().toString());
        }

        return DriverManager.getConnection(DatabaseUtil.returnDBConnectionStringWithoutDatabase(),
                FdfSettings.DB_ROOT_USER.toLowerCase(), FdfSettings.DB_ROOT_PASSWORD);
    }

    @Override
    public void close4dfDbSession(Connection connection) throws SQLException {
        super.close4dfDbSession(connection);
    }

    @Override
    protected void initializeHakariDatasource() {
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName(FdfSettings.HIKARICP_DATASOURCE_CLASSNAME);
        config.setAutoCommit(FdfSettings.HIKARICP_AUTOCOMMIT);
        config.setConnectionTimeout(FdfSettings.HIKARICP_CONNECTION_TIMEOUT_MS);
        config.setIdleTimeout(FdfSettings.HIKARICP_IDLE_TIMEOUT_MS);
        config.setMaxLifetime(FdfSettings.HIKARICP_MAX_LIFETIME_MS);
        config.setConnectionTestQuery(FdfSettings.HIKARICP_CONNECTION_TEST_QUERY);
        config.setMaximumPoolSize(FdfSettings.HIKARICP_MAX_POOL_SIZE);
        config.setMinimumIdle(FdfSettings.HIKARICP_MIN_IDLE_MS);
        /* TODO: Caused by: java.lang.ClassNotFoundException: com.codahale.metrics.MetricRegistry
        if(FdfSettings.HIKARICP_METRIC_REG != null) {
            config.setMetricRegistry(FdfSettings.HIKARICP_METRIC_REG);
        }
        */
        /* TODO: Caused by: java.lang.ClassNotFoundException: com.codahale.metrics.MetricRegistry
        config.setHealthCheckRegistry(FdfSettings.HIKARICP_HEALTH_CHECK_REG);
        */
        config.setPoolName(FdfSettings.HIKARICP_POOL_NAME);
        config.setInitializationFailFast(FdfSettings.HIKARICP_FAIL_FAST);
        config.setIsolateInternalQueries(FdfSettings.HIKARICP_ISOLATE_INTERNAL_QUERIES);
        config.setAllowPoolSuspension(FdfSettings.HIKARICP_ALLOW_POOL_SUSPENSION);
        config.setReadOnly(FdfSettings.HIKARICP_READ_ONLY);
        config.setRegisterMbeans(FdfSettings.HIKARICP_REGISTER_MBEANS);
        /* TODO: IllegalArgumentException: Invalid transaction isolation value: driver default
        config.setCatalog(FdfSettings.HIKARICP_CATALOG);
         */
        config.setConnectionInitSql(FdfSettings.HIKARICP_CONNECTION_INIT_SQL);
        /* TODO: Caused by: java.lang.ClassNotFoundException: com.codahale.metrics.MetricRegistry
        config.setDriverClassName(FdfSettings.HIKARICP_DRIVER_CLASS_NAME);
        */
        /* TODO: IllegalArgumentException: Invalid transaction isolation value: driver default
        config.setTransactionIsolation(FdfSettings.HIKARICP_TRANSATION_ISOLATION);
         */
        config.setValidationTimeout(FdfSettings.HIKARICP_VALIDATION_TIMEOUT);
        config.setLeakDetectionThreshold(FdfSettings.HIKARICP_LEAK_DETECTION_THRESHOLD);
        config.setDataSourceClassName(FdfSettings.HIKARICP_DATA_SOURCE);
        config.setThreadFactory(FdfSettings.HIKARICP_THREAD_FACTORY);

        config.setJdbcUrl(DatabaseUtil.returnDBConnectionString());
        config.setUsername(FdfSettings.DB_USER.toLowerCase());
        config.setPassword(FdfSettings.DB_PASSWORD.toLowerCase());

        hds = new HikariDataSource(config);
    }
}
