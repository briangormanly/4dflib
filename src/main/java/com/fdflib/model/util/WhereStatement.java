package com.fdflib.model.util;

import com.fdflib.model.state.CommonState;
import com.fdflib.persistence.FdfPersistence;
import com.fdflib.util.GeneralConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Corley.Herman1 on 4/22/2016.
 */
public class WhereStatement {
    protected final List<WhereClause> whereStatement = new ArrayList<>();

    public void add(WhereClause whereClause) {
        if(whereClause != null) {
            whereStatement.add(whereClause);
        } else {
            System.out.println("[error] ::: NullWhereClauseException - Clause was ignored.");
        }
    }

    public <S extends CommonState> List<S> run(Class<S> entityState) {
        List<WhereClause> query = new ArrayList<>(whereStatement);
        whereStatement.clear();
        return FdfPersistence.getInstance().selectQuery(entityState, null, query);
    }

    public void reset() {
        whereStatement.clear();
    }

    public String toString() {
        String where = "WHERE 1=1";
        for(WhereClause clause : whereStatement) {
            where += "\n  " + clause.conditional.name() + " " + clause.name + " " + clause.getOperatorString() + " " + clause.value;
        }
        return where;
    }

    public List<WhereClause> asList() {
        return whereStatement;
    }
}
