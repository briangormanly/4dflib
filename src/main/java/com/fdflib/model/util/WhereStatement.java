package com.fdflib.model.util;

import com.fdflib.model.state.CommonState;
import com.fdflib.persistence.FdfPersistence;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Corley.Herman1 on 4/22/2016.
 */
public class WhereStatement {
    private final List<WhereClause> whereStatement = new ArrayList<>();

    public void add(WhereClause whereClause) {
        if(whereClause != null) {
            whereStatement.add(whereClause);
        } else {
            System.out.println("[error] ::: NullWhereClauseException - Clause was ignored.");
        }
    }

    public List<WhereClause> asList() {
        return whereStatement;
    }
}
