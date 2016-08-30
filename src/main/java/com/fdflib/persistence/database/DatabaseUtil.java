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

/**
 * Created by brian.gormanly on 9/9/15.
 */
public class DatabaseUtil {
    public enum DatabaseType {
        MYSQL, POSTGRES, ORACLE, MSSQL, HSQL
    }

    public enum DatabaseProtocol {
        JDBC_MYSQL, JDBC_POSTGRES, JDBC_HSQL
    }

    public enum DatabaseEncoding {
        UTF8
    }
}
