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
public class FdfTenantServices implements FdfCommonServices {

    public List<FdfTenant> getAllTenants() {
        List<FdfTenant> currentTenants = new ArrayList<>();

        for(FdfEntity<FdfTenant> tenant: getAllTenantsWithHistory()) {
            if(tenant != null && tenant.current != null) {
                currentTenants.add(tenant.current);
            }
        }

        return currentTenants;
    }

    public FdfEntity<FdfTenant> deleteTenant(long userId, long tenantId, long callingUserId, long callingSystemId) {
        // get all with history
        GenericService gs = new GenericService();
        return gs.setDeleteFlag(FdfTenant.class, userId, callingUserId, callingSystemId, tenantId);

    }

    public FdfEntity<FdfTenant> unDeleteTenant(long userId, long tenantId, long callingUserId, long callingSystemId) {
        // get all with history
        GenericService gs = new GenericService();
        return gs.removeDeleteFlag(FdfTenant.class, userId, callingUserId, callingSystemId, tenantId);

    }

    public List<FdfEntity<FdfTenant>> getAllTenantsWithHistory() {

        return this.getAll(FdfTenant.class);
    }

    public FdfTenant getTenantById(long tenantId) {
        return getTenantByIdWithHistory(tenantId).current;
    }

    public FdfEntity<FdfTenant> getTenantByIdWithHistory(long tenantId) {

        // get the tenant
        if(tenantId > 0) {
            return getEntityById(FdfTenant.class, tenantId);
        }

        return null;
    }

    public FdfTenant getDefaultTenant() {
        return getDefaultTenantWithHistory().current;
    }

    public FdfEntity<FdfTenant> getDefaultTenantWithHistory() {
        List<WhereClause> whereClauses = new ArrayList<>();

        // check that deleted records are not returned
        WhereClause whereDf = new WhereClause();
        whereDf.name = "df";
        whereDf.operator = WhereClause.Operators.IS_NOT;
        whereDf.value = "true";
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
    
    public FdfTenant getTenantByName(String tenantName) {

        return getTenantByNameWithHistory(tenantName).current;
    }

    public FdfEntity<FdfTenant> getTenantByNameWithHistory(String tenantName) {
        // create the where statement for the query
        List<WhereClause> whereStatement = new ArrayList<>();

        // check that deleted records are not returned
        WhereClause whereDf = new WhereClause();
        whereDf.name = "df";
        whereDf.operator = WhereClause.Operators.IS_NOT;
        whereDf.value = "true";
        whereDf.valueDataType = Integer.class;

        // check to find results that match the name like condition
        WhereClause nameLike = new WhereClause();
        nameLike.name = "name";
        nameLike.operator = WhereClause.Operators.LIKE;
        nameLike.value = "%" + tenantName + "%";
        nameLike.valueDataType = String.class;

        whereStatement.add(nameLike);

        // do the query
        return this.manageReturnedEntity(FdfPersistence.getInstance().selectQuery(FdfTenant.class, null, whereStatement));

        //return tenantList;
    }

    /**
     * Save FdfTenant
     * Save FdfTenant, checks to see if there is already a tenant with the same name as names are unique. If the name
     * already exists, it is treated as an update, not a insert.
     * @param tenant to save
     * @return tenant saved with id
     */
    public FdfEntity<FdfTenant> saveTenant(FdfTenant tenant) {


        FdfEntity<FdfTenant> returnEntity = null;
        if(tenant != null) {
            // check to see if there is a user with the passed users id and or username
            FdfTenant existingTenant = this.getTenantByName(tenant.name);
            if(existingTenant != null) {
                tenant.id = existingTenant.id;
            }
            returnEntity = this.save(FdfTenant.class, tenant);
        }

        return returnEntity;
    }

}
