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
