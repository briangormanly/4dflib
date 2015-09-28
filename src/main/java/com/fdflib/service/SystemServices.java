package com.fdflib.service;

import com.fdflib.model.entity.FdfEntity;
import com.fdflib.model.state.FdfSystem;
import com.fdflib.model.util.WhereClause;
import com.fdflib.persistence.FdfPersistence;
import com.fdflib.service.impl.FdfCommonServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brian.gormanly on 8/22/15.
 */
public class SystemServices implements FdfCommonServices {

    public List<FdfEntity<FdfSystem>> getAllSystems() {
        return this.getAll(FdfSystem.class);
    }

    public FdfEntity<FdfSystem> getDefaultSystem() {
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
        List<FdfSystem> returnedService =
                FdfPersistence.getInstance().selectQuery(FdfSystem.class, null, whereStatement);

        // create a List of entities
        return manageReturnedEntity(returnedService);
    }

    public List<FdfEntity<FdfSystem>> getSystemsByName(String name) {
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
        List<FdfSystem> returnedService =
                FdfPersistence.getInstance().selectQuery(FdfSystem.class, null, whereStatement);

        // create a List of entities
        return manageReturnedEntities(returnedService);
    }

    public FdfEntity<FdfSystem> saveSystem(FdfSystem systemState, long userId, long systemId) {
        return save(FdfSystem.class, systemState, userId, systemId);
    }

    public void createDefaultService(FdfSystem state) {

        this.save(FdfSystem.class, state, 1, 1);
    }

}
