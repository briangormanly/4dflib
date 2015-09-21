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

package com.fdflib.persistence.impl;

import com.fdflib.model.state.CommonState;
import com.fdflib.model.util.WhereClause;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by brian.gormanly on 5/29/15.
 */


public interface CorePersistenceImpl {
    void createDatabase() throws SQLException;
    void createTable(Class c) throws SQLException;
    <S extends CommonState> List<S> selectQuery(Class c, List<String> select, List<WhereClause> where);
    <S> Long insert(Class<S> entityState, S state);
    <S> void update(Class<S> c, S state);
}
