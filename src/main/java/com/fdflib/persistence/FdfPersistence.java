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

package com.fdflib.persistence;

import com.fdflib.model.state.CommonState;
import com.fdflib.model.util.WhereClause;
import com.fdflib.persistence.connection.DbConnectionManager;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by brian.gormanly on 6/10/15.
 */
public class FdfPersistence extends DbConnectionManager {

    private static final FdfPersistence INSTANCE = new FdfPersistence();

    private FdfPersistence() {}

    public static FdfPersistence getInstance() {
        return INSTANCE;
    }

    @Override
    public void createDatabase() throws SQLException {
        persistence.createDatabase();
    }

    @Override
    public void createTable(Class c) throws SQLException {
        persistence.createTable(c);
    }

    @Override
    public <S> void update(Class<S> entityState, S state) {

        persistence.update(entityState, state);
    }

    @Override
    public <S> Long insert(Class<S> entityState, S state) {
        return persistence.insert(entityState, state);
    }

    @Override
    public <S extends CommonState> List<S> selectQuery(Class c, List<String> select, List<WhereClause> where) {
        return persistence.selectQuery(c, select, where);
    }

}
