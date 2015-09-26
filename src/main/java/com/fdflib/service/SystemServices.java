package com.fdflib.service;

import com.fdflib.model.entity.FdfEntity;
import com.fdflib.model.state.SystemState;
import com.fdflib.model.util.WhereClause;
import com.fdflib.persistence.FdfPersistence;
import com.fdflib.service.impl.StateServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brian.gormanly on 8/22/15.
 */
public class SystemServices implements StateServices {

    public List<FdfEntity<SystemState>> getAllSystems() {
        return this.getAll(SystemState.class);
    }

    public FdfEntity<SystemState> getDefaultSystem() {
        // create the where statement for the query
        List<WhereClause> whereStatement = new ArrayList<>();

        // check that deleted records are not returned
        WhereClause whereDf = new WhereClause();
        whereDf.name = "df";
        whereDf.operator = WhereClause.Operators.NOT_EQUAL;
        whereDf.value = "1";
        whereDf.valueDataType = Integer.class;

        // add the id check
        WhereClause whereId = new WhereClause();
        whereId.conditional = WhereClause.CONDITIONALS.AND;
        whereId.name = "name";
        whereId.operator = WhereClause.Operators.EQUAL;
        whereId.value = "Default";
        whereId.valueDataType = String.class;

        whereStatement.add(whereDf);
        whereStatement.add(whereId);

        // do the query
        List<SystemState> returnedService =
                FdfPersistence.getInstance().selectQuery(SystemState.class, null, whereStatement);

        // create a List of entities
        return manageReturnedEntity(returnedService);
    }

    public List<FdfEntity<SystemState>> getSystemsByName(String name) {
        // create the where statement for the query
        List<WhereClause> whereStatement = new ArrayList<>();

        // check that deleted records are not returned
        WhereClause whereDf = new WhereClause();
        whereDf.name = "df";
        whereDf.operator = WhereClause.Operators.NOT_EQUAL;
        whereDf.value = "1";
        whereDf.valueDataType = Integer.class;

        // add the id check
        WhereClause whereId = new WhereClause();
        whereId.conditional = WhereClause.CONDITIONALS.AND;
        whereId.name = "name";
        whereId.operator = WhereClause.Operators.EQUAL;
        whereId.value = name;
        whereId.valueDataType = String.class;

        whereStatement.add(whereDf);
        whereStatement.add(whereId);

        // do the query
        List<SystemState> returnedService =
                FdfPersistence.getInstance().selectQuery(SystemState.class, null, whereStatement);

        // create a List of entities
        return manageReturnedEntities(returnedService);
    }

    public void createDefaultService(SystemState state) {

        this.save(SystemState.class, state, 1, 1);
    }

}
