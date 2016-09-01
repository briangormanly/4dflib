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

package com.fdflib.service;

import com.fdflib.model.state.FdfSystem;
import com.fdflib.model.state.FdfTenant;
import com.fdflib.persistence.FdfPersistence;
import com.fdflib.persistence.database.DatabaseUtil;
import com.fdflib.util.FdfSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.sql.SQLException;
import java.util.List;

/**
 * Universal implementation of the 4DF API, allows querying across all Entity states that extend CommonState.
 * These services allow application using the services to initialize and manage database connections
 *
 * This service must be called to establish database connection settings before any queries can be run.
 *
 * Created on 8/21/15.
 * @author brian.gormanly
 */
public class FdfServices {

    static Logger fdfLog = LoggerFactory.getLogger(FdfServices.class);
    static FdfSettings settings = FdfSettings.getInstance();

    public static void initializeFdfDataModel(List<Class> passedClasses) {

        // check to see if the necessary settings are in place
        int flag = 0;
        if(settings.DB_HOST == null) {
            flag++;
            fdfLog.error("(initializeFdfDataModel): No DB_HOST set!");
        }
        if(settings.DB_NAME == null) {
            flag++;
            fdfLog.error("(initializeFdfDataModel): No DB_NAME set!");
        }
        if(settings.PERSISTENCE == null) {
            flag++;
            fdfLog.error("(initializeFdfDataModel): No PERSISTENCE (Database) set!");
        }
        if(settings.DB_USER == null) {
            flag++;
            fdfLog.error("(initializeFdfDataModel): No DB_USER set!");
        }
        if(settings.DB_PASSWORD == null) {
            fdfLog.info("(initializeFdfDataModel): No DB_PASSWORD set, optional");
        }
        if(settings.DB_ROOT_USER == null) {
            fdfLog.info("(initializeFdfDataModel): No DB_ROOT_USER set, optional");
        }
        if(settings.DB_ROOT_PASSWORD == null) {
            fdfLog.info("(initializeFdfDataModel): No DB_ROOT_PASSWORD set, optional");
        }
        if(settings.DB_ENCODING == null) {
            settings.DB_ENCODING = DatabaseUtil.DatabaseEncoding.UTF8;
            fdfLog.info("(initializeFdfDataModel): No DB_ENCODING set, setting default {}", settings.DB_ENCODING.toString());

        }
        if(FdfSettings.DB_PROTOCOL == null) {
            if(settings.PERSISTENCE == DatabaseUtil.DatabaseType.MYSQL) {
                settings.DB_PROTOCOL = DatabaseUtil.DatabaseProtocol.JDBC_MYSQL;
            }
            fdfLog.info("(initializeFdfDataModel): No DB_PROTOCOL set, setting default for persistence " +
                    "type {}", settings.DB_PROTOCOL.toString());
        }

        // register the passed in 4df data model (represented by passed list of classes)
        FdfSettings.getInstance().modelClasses.addAll(passedClasses);

        // add the library FdfSystem model
        FdfSettings.getInstance().modelClasses.add(FdfSystem.class);

        // add the library FdfTenant model
        FdfSettings.getInstance().modelClasses.add(FdfTenant.class);

        if(flag == 0) {
            // We have enough information to see if there exists a database
            // do necessary logging
            fdfLog.info("------------------------------------------------------------------");
            fdfLog.info("4DFLib initializing database connection");
            fdfLog.info("------------------------------------------------------------------");
            fdfLog.info("Database Type: {}", settings.PERSISTENCE);
            fdfLog.info("Database Protocol: {}", settings.DB_PROTOCOL);
            fdfLog.info("Database Encoding: {}", settings.DB_ENCODING);
            fdfLog.info("Database Host: {}", settings.DB_HOST);
            fdfLog.info("Database schema name: {}", settings.DB_NAME);
            fdfLog.info("--------------------------------------------------------------");
            try {
                FdfPersistence.getInstance().checkDatabase();
                FdfPersistence.getInstance().checkTables();
                FdfPersistence.getInstance().checkFields();
                FdfPersistence.getInstance().checkDefaultEntries();
            } catch (SQLException e) {
                e.printStackTrace();
            }


        } else {
            fdfLog.error("4DFLIB (initializeFdfDataModel): Unable to connect to or create database, "
                    + " See errors above");
        }

    }


}
