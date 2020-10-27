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

package com.fdflib.persistence.connection;

import com.fdflib.persistence.database.DatabaseUtil;
import com.fdflib.persistence.impl.CorePersistenceImpl;
import com.fdflib.persistence.queries.CoreHSqlQueries;
import com.fdflib.persistence.queries.CoreMariaDbQueries;
import com.fdflib.persistence.queries.CoreMySqlQueries;
import com.fdflib.persistence.queries.CorePostgreSqlQueries;
import com.fdflib.util.FdfSettings;

/**
 * Created by brian.gormanly on 5/20/15.
 */
public abstract class DbConnectionManager implements CorePersistenceImpl {

    public static CorePersistenceImpl persistence = null;

    public DbConnectionManager() {

        if(FdfSettings.getInstance().PERSISTENCE == DatabaseUtil.DatabaseType.MYSQL) {
            persistence = CoreMySqlQueries.getInstance();

        }

        if(FdfSettings.getInstance().PERSISTENCE == DatabaseUtil.DatabaseType.MARIADB) {
            persistence = CoreMariaDbQueries.getInstance();

        }

        if(FdfSettings.getInstance().PERSISTENCE == DatabaseUtil.DatabaseType.POSTGRES) {
            persistence = CorePostgreSqlQueries.getInstance();

        }

        if(FdfSettings.getInstance().PERSISTENCE == DatabaseUtil.DatabaseType.HSQL) {
            persistence = CoreHSqlQueries.getInstance();

        }
    }
}