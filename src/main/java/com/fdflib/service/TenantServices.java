package com.fdflib.service;

import com.fdflib.model.entity.FdfEntity;
import com.fdflib.model.state.FdfTenant;
import com.fdflib.model.util.WhereClause;
import com.fdflib.persistence.FdfPersistence;
import com.fdflib.service.impl.FdfCommonServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brian.gormanly on 9/28/15.
 */
public class TenantServices implements FdfCommonServices {

    public <S> List<FdfEntity<FdfTenant>> getTenantByName(String tenentName) {
        // create the where statement for the query
        List<WhereClause> whereStatement = new ArrayList<>();

        // check that deleted records are not returned
        WhereClause whereDf = new WhereClause();
        whereDf.name = "df";
        whereDf.operator = WhereClause.Operators.NOT_EQUAL;
        whereDf.value = "1";
        whereDf.valueDataType = Integer.class;

        // check to find results that match the name like condition
        WhereClause nameLike = new WhereClause();
        nameLike.name = "name";
        nameLike.operator = WhereClause.Operators.LIKE;
        nameLike.value = "%" + tenentName + "%";
        nameLike.valueDataType = String.class;

        whereStatement.add(nameLike);

        // do the query
        List<FdfTenant> returnedStates = FdfPersistence.getInstance().selectQuery(FdfTenant.class, null, whereStatement);
        List<FdfEntity<FdfTenant>> tenantList = this.manageReturnedEntities(returnedStates);

        return tenantList;
    }

    public FdfEntity<FdfTenant> getPrimaryTenant() {
        List<WhereClause> whereClauses = new ArrayList<>();

        // check that deleted records are not returned
        WhereClause whereDf = new WhereClause();
        whereDf.name = "df";
        whereDf.operator = WhereClause.Operators.NOT_EQUAL;
        whereDf.value = "1";
        whereDf.valueDataType = Integer.class;

        // check to find results that match the name like condition
        WhereClause primary = new WhereClause();
        primary.name = "isPrimary";
        primary.operator = WhereClause.Operators.EQUAL;
        primary.value = "1";
        primary.valueDataType = String.class;

        whereClauses.add(whereDf);
        whereClauses.add(primary);

        List<FdfTenant> returnedStates = FdfPersistence.getInstance().selectQuery(FdfTenant.class, null, whereClauses);
        return this.manageReturnedEntity(returnedStates);

    }

    /**
     * Save FdfTenant
     * @param tenantState
     * @param userId
     * @param systemId
     * @return
     */
    public FdfEntity<FdfTenant> saveTenant(FdfTenant tenantState, long userId, long systemId) {
        return this.save(FdfTenant.class, tenantState, userId, systemId);
    }

    /**
     *
     * @param tenantState
     * @param userId
     * @param systemId
     * @param tenantId
     * @return
     */
    public FdfEntity<FdfTenant> saveTenant(FdfTenant tenantState, long userId, long systemId, long tenantId) {
        return this.save(FdfTenant.class, tenantState, userId, systemId, tenantId);
    }
}
