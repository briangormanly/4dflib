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
import org.slf4j.LoggerFactory;

/**
 * Created by brian.gormanly on 9/9/15.
 */
public class DatabaseUtil {

    private static org.slf4j.Logger fdfLog = LoggerFactory.getLogger(JdbcConnection.class);

    public enum DatabaseType {
        MYSQL, MARIADB, POSTGRES, MSSQL, HSQL
    }

    public enum DatabaseProtocol {
        JDBC_MYSQL, JDBC_MARIADB, JDBC_POSTGRES, JDBC_HSQL
    }

    public enum DatabaseEncoding {
        UTF8
    }

    public static String returnDBConnectionString() {
        String protocolString = "";
        String encodingString = "";
        String connection = "";

        if(FdfSettings.DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_MYSQL) {
            protocolString = "jdbc:mysql://";

            if(FdfSettings.DB_ENCODING == DatabaseUtil.DatabaseEncoding.UTF8) {
                if(FdfSettings.USE_SSL == true) {
                    encodingString = "?characterEncoding=UTF-8&autoReconnect=true&useSSL=true";
                }
                else {
                    encodingString = "?characterEncoding=UTF-8&autoReconnect=true&useSSL=false";
                }
            }
            else {
                if(FdfSettings.USE_SSL == true) {
                    encodingString = "?autoReconnect=true&useSSL=true";
                }
                else {
                    encodingString = "?autoReconnect=true&useSSL=false";
                }
            }

            connection = protocolString + FdfSettings.DB_HOST + "/" + FdfSettings.DB_NAME + encodingString;
        }

        if(FdfSettings.DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_MARIADB) {
            protocolString = "jdbc:mariadb://";
            connection = protocolString + FdfSettings.DB_HOST + "/" + FdfSettings.DB_NAME;
        }

        if(FdfSettings.DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_POSTGRES) {
            protocolString = "jdbc:postgresql://";

            if(FdfSettings.DB_ENCODING == DatabaseUtil.DatabaseEncoding.UTF8) {
                encodingString = "?characterEncoding=UTF-8";
            }

            connection = protocolString + FdfSettings.DB_HOST + "/" + FdfSettings.DB_NAME.toLowerCase()
                    + encodingString;
        }

        if(FdfSettings.DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_HSQL) {

            if(FdfSettings.HSQL_DB_FILE) {
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
        String questionMark = "?";
        String sslString ="";
        String connection = "";

        if(FdfSettings.DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_MYSQL) {
            protocolString = "jdbc:mysql://";

            if(FdfSettings.DB_ENCODING == DatabaseUtil.DatabaseEncoding.UTF8) {
                if(FdfSettings.USE_SSL == true) {
                    encodingString = "/?characterEncoding=UTF-8&autoReconnect=true&useSSL=true";
                }
                else {
                    encodingString = "/?characterEncoding=UTF-8&autoReconnect=true&useSSL=false";
                }
            }
            else {
                if(FdfSettings.USE_SSL == true) {
                    encodingString = "/?autoReconnect=true&useSSL=true";
                }
                else {
                    encodingString = "/?autoReconnect=true&useSSL=false";
                }
            }

            connection = protocolString + FdfSettings.DB_HOST + encodingString;
        }

        if(FdfSettings.DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_MARIADB) {
            protocolString = "jdbc:mariadb://";
            connection = protocolString + FdfSettings.DB_HOST;
        }

        if(FdfSettings.DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_POSTGRES) {
            protocolString = "jdbc:postgresql://";

            if(FdfSettings.DB_ENCODING == DatabaseUtil.DatabaseEncoding.UTF8) {
                encodingString = "/?characterEncoding=UTF-8";
            }

            connection = protocolString + FdfSettings.DB_HOST + encodingString;
        }

        if(FdfSettings.DB_PROTOCOL == DatabaseUtil.DatabaseProtocol.JDBC_HSQL) {
            if(FdfSettings.HSQL_DB_FILE) {
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
