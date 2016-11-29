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

package com.fdflib.util;

import com.fdflib.persistence.database.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brian.gormanly on 5/19/15.
 */
public class FdfSettings {

    private static final FdfSettings INSTANCE = new FdfSettings();

    static Logger fdfLog = LoggerFactory.getLogger(FdfSettings.class);

    public List<Class> modelClasses = new ArrayList<>();
    public static DatabaseUtil.DatabaseType PERSISTENCE = DatabaseUtil.DatabaseType.HSQL;

    public static DatabaseUtil.DatabaseProtocol DB_PROTOCOL = DatabaseUtil.DatabaseProtocol.JDBC_HSQL;
    public static String DB_HOST = "localhost";
    public static String DB_NAME = "4dfapplicationdb";
    public static Integer DB_PORT = 9001;
    public static DatabaseUtil.DatabaseEncoding DB_ENCODING = DatabaseUtil.DatabaseEncoding.UTF8;

    // default general user information
    public static String DB_USER = "SA";
    public static String DB_PASSWORD = "";

    // Example database specific with non-root user for general db access.
    //public static String DB_USER = "fdfUser";
    //public static String DB_PASSWORD = "fdfUserPassword";

    // Root user information
    public static Boolean USE_DB_ROOT = true;
    public static String DB_ROOT_USER = "SA";
    public static String DB_ROOT_PASSWORD = "";

    // If set to true HSQL db will be written to a file to persist, if false, it will be an in-memory db only.
    public static Boolean HSQL_DB_FILE = true;
    //HyperSQL database file location
    public static String HQSL_DB_FILE_LOCATION = "hsql/";

    public static String DEFAULT_TENANT_NAME = "Default FdfTenant";
    public static String DEFAULT_TENANT_DESRIPTION = "Default FdfTenant is created by 4dflib, if you do not intent to use "
            + "built in multi-tenancy or only have one FdfTenant, all data is member of this tenant by "
            + "default";
    public static String DEFAULT_TENANT_WEBSITE = "http://www.4dflib.com";
    public static Boolean DEFAULT_TENANT_IS_PRIMARY = true;

    public static String DEFAULT_SYSTEM_NAME = "Default FdfSystem";
    public static String DEFAULT_SYSTEM_DESCRIPTION = "Default system represents the actual application and not"
            + " a registered external system.";
    public static String DEFAULT_SYSTEM_PASSWORD = "4DfPassword";

    public static String TEST_SYSTEM_NAME = "Default Test System";
    public static String TEST_SYSTEM_DESCRIPTION = "Default test system for use connecting to the system for testing";
    public static String TEST_SYSTEM_PASSWORD = "testSystemPassword";

    /**
     * HikariCP settings
     * See: https://github.com/brettwooldridge/HikariCP/wiki/Configuration
     * HikariCP uses milliseconds for all time values
     */

    /**
     * Turn on HIKARICP? If false use regular JDBC
     */
    public static Boolean USE_HIKARICP = false;

    /**
     * This is the name of the DataSource class provided by the JDBC driver Consult the documentation for your specific
     * JDBC driver to get this class name see https://github.com/brettwooldridge/HikariCP#popular-datasource-class-names
     * Note: XA data sources are not supported. XA requires a real transaction manager like bitronix. Note that you do
     * not need this property if you are using jdbcUrl for "old-school" DriverManager-based JDBC driver configuration.
     * Default: none
     */
    public static String HIKARICP_DATASOURCE_CLASSNAME = "none";

    /**
     * This property controls the default auto-commit behavior of connections returned from the pool. It is a boolean
     * value. Default: true
     */
    public static Boolean HIKARICP_AUTOCOMMIT = true;

    /**
     * This property controls the maximum number of milliseconds that a client (that's you) will wait for a connection
     * from the pool.
     */

    public static Integer HIKARICP_CONNECTION_TIMEOUT_MS = 30000;

    /**
     * This property controls the maximum amount of time that a connection is allowed to sit idle in the pool. This
     * setting only applies when minimumIdle is defined to be less than maximumPoolSize
     */
    public static Integer HIKARICP_MAX_IDLE_TIMEOUT_MS = 60000;

    /**
     * This property controls the maximum lifetime of a connection in the pool. When a connection reaches this timeout
     * it will be retired from the pool
     */
    public static Integer HIKARICP_MAX_LIFETIME_MS = 287400;

    /**
     * This is for "legacy" databases that do not support the JDBC4 Connection.isValid() API HikariCP will log an error
     * if your driver is not JDBC4 compliant to let you know. Default: none
     */
    public static String HIKARICP_CONNECTION_TEST_QUERY = "none";

    /**
     * This property controls the maximum size that the pool is allowed to reach, including both idle and in-use
     * connections. Basically this value will determine the maximum number of actual connections to the database
     * backend. A reasonable value for this is best determined by your execution environment. When the pool reaches
     * this size, and no idle connections are available, calls to getConnection() will block for up to connectionTimeout
     * milliseconds before timing out. Default: 10
     */
    public static Integer HIKARICP_MAX_POOL_SIZE = 10;

    /**
     * This property controls the minimum number of idle connections that HikariCP tries to maintain in the pool.
     * Default: same as maximumPoolSize
     */
    public static Integer HIKARICP_MIN_IDLE_MS = HIKARICP_MAX_POOL_SIZE;

    // This property is only available via programmatic configuration or IoC container. This property allows you to
    // specify an instance of a Codahale/Dropwizard MetricRegistry to be used by the pool to record various metrics.
    // See the Metrics wiki page for details. Default: none
    public static String HIKARICP_METRIC_REG = "none";

    // This property is only available via programmatic configuration or IoC container. This property allows you to
    // specify an instance of a Codahale/Dropwizard HealthCheckRegistry to be used by the pool to report current
    // health information. See the Health Checks wiki page for details. Default: none
    public static String HIKARICP_HEALTH_CHECK_REG = "none";

    /**
     * This property represents a user-defined name for the connection pool and appears mainly in logging and JMX
     * management consoles to identify pools and pool configurations. Default: auto-generated
     */

    public static String HIKARICP_POOL_NAME = "auto-generated";

    /**
     * This property controls whether the pool will "fail fast" if the pool cannot be seeded with initial connections
     * successfully. If you want your application to start even when the database is down/unavailable, set this
     * property to false. Default: true
     */
    public static Boolean HIKARICP_FAIL_FAST = true;

    public static String HIKARICP_;

    public static String HIKARICP_;

    public static String HIKARICP_



    private FdfSettings() {
    }

    public static FdfSettings getInstance() {
        return INSTANCE;
    }

    public static String returnDBConnectionString() {
        String protocolString = "";
        String encodingString = "";
        String connection = "";

        if(DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_MYSQL) {
            protocolString = "jdbc:mysql://";

            if(DB_ENCODING == DatabaseUtil.DatabaseEncoding.UTF8) {
                encodingString = "?characterEncoding=UTF-8";
            }

            connection = protocolString + DB_HOST + "/" + DB_NAME + encodingString;
        }

        if(DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_POSTGRES) {
            protocolString = "jdbc:postgresql://";

            if(DB_ENCODING == DatabaseUtil.DatabaseEncoding.UTF8) {
                encodingString = "?characterEncoding=UTF-8";
            }

            connection = protocolString + DB_HOST + "/" + DB_NAME + encodingString;
        }

        if(DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_HSQL) {

            if(HSQL_DB_FILE) {
                protocolString = "jdbc:hsqldb:file:" + FdfSettings.HQSL_DB_FILE_LOCATION + FdfSettings.DB_NAME
                        + ";sql.syntax_mys=true";
            }
            else {
                protocolString = "jdbc:hsqldb:mem:" + FdfSettings.DB_NAME + ";sql.syntax_mys=true";
            }

            connection = protocolString;
        }

        fdfLog.debug("Returning DB connection string: {}", connection);
        return connection;
    }

    public static String returnDBConnectionStringWithoutDatabase() {
        String protocolString = "";
        String encodingString = "";
        String connection = "";

        if(DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_MYSQL) {
            protocolString = "jdbc:mysql://";

            if(DB_ENCODING == DatabaseUtil.DatabaseEncoding.UTF8) {
                encodingString = "/?characterEncoding=UTF-8";
            }

            connection = protocolString + DB_HOST + encodingString;
        }

        if(DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_POSTGRES) {
            protocolString = "jdbc:postgresql://";

            if(DB_ENCODING == DatabaseUtil.DatabaseEncoding.UTF8) {
                encodingString = "/?characterEncoding=UTF-8";
            }

            connection = protocolString + DB_HOST + encodingString;
        }

        if(DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_HSQL) {
            if(HSQL_DB_FILE) {
                protocolString = "jdbc:hsqldb:file:" + FdfSettings.HQSL_DB_FILE_LOCATION + FdfSettings.DB_NAME
                        + ";sql.syntax_mys=true";
            }
            else {
                protocolString = "jdbc:hsqldb:mem:" + FdfSettings.DB_NAME + ";sql.syntax_mys=true";
            }

            connection = protocolString;
        }

        fdfLog.debug("Returning DB connection string: {}", connection);
        return connection;
    }
}
