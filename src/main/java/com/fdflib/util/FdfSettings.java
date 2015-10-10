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
    public static DatabaseUtil.DatabaseType PERSISTENCE = null;

    public static DatabaseUtil.DatabaseProtocol DB_PROTOCOL = null;
    public static String DB_HOST = null;
    public static String DB_NAME = null;
    public static DatabaseUtil.DatabaseEncoding DB_ENCODING = null;

    public static String DB_USER = null;
    public static String DB_PASSWORD = null;
    public static String DB_ROOT_USER = null;
    public static String DB_ROOT_PASSWORD = null;

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

    private FdfSettings() {
    }

    public static FdfSettings getInstance() {
        return INSTANCE;
    }

    public static String returnDBConnectionString() {
        String protocolString = "";
        if(DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_MYSQL) {
            protocolString = "jdbc:mysql://";
        }

        String encodingString = "";
        if(DB_ENCODING == DatabaseUtil.DatabaseEncoding.UTF8) {
            encodingString = "?characterEncoding=UTF-8";
        }
        String connection = protocolString + DB_HOST + "/" + DB_NAME + encodingString;
        fdfLog.debug("Returning DB connection string: {}", connection);
        return connection;
    }

    public static String returnDBConnectionStringWithoutDatabase() {
        String protocolString = "";
        if(DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_MYSQL) {
            protocolString = "jdbc:mysql://";
        }

        String encodingString = "";
        if(DB_ENCODING == DatabaseUtil.DatabaseEncoding.UTF8) {
            encodingString = "?characterEncoding=UTF-8";
        }
        String connection = protocolString + DB_HOST + "/" + encodingString;
        fdfLog.debug("Returning DB connection string: {}", connection);
        return connection;
    }
}
