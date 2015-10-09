package com.fdflib.service;

import com.fdflib.model.entity.FdfEntity;
import com.fdflib.model.state.FdfTenant;
import com.fdflib.model.util.WhereClause;
import com.fdflib.persistence.FdfPersistence;
import com.fdflib.service.impl.FdfCommonServices;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brian.gormanly on 9/28/15.
 */
public class TenantServices implements FdfCommonServices {

    public <S> List<FdfEntity<FdfTenant>> getTenantByName(String tenantName) {
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
        nameLike.value = "%" + tenantName + "%";
        nameLike.valueDataType = String.class;

        whereStatement.add(nameLike);

        // do the query
        List<FdfTenant> returnedStates = FdfPersistence.getInstance().selectQuery(FdfTenant.class, null, whereStatement);
        List<FdfEntity<FdfTenant>> tenantList = this.manageReturnedEntities(returnedStates);

        return tenantList;
    }

    public FdfEntity<FdfTenant> getDefaultTenant() {
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
    public FdfEntity<FdfTenant> saveTenant(FdfTenant tenantState, long userId, long systemId,
                                           long tenantId) {
        return this.save(FdfTenant.class, tenantState, userId, systemId, tenantId);
    }


    /**
     * Returns true if the passed SHA-256 hashed password matched the one saved for the tenant.
     * @param clearTextPassword Clear text password to be hashed
     * @return String representing the hashed password
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public String hashPassword(String clearTextPassword) {

        byte[] digest = null;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(clearTextPassword.getBytes("UTF-8"));
            digest = md.digest();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return digest.toString();
    }

    /**
     * Checks the passed FfdTenant Id and SHA-256 hashed password against the FdfTenant record, returns a true
     * if the credentials are correct and false otherwise.
     *
     * @param fdfTenantId Tenant ID to check authentication for
     * @param sha256EncryptedPassword Tenant password (must be SHA-256 hashed) to check authentication for
     * @return True if authentication attempt is successful, false otherwise.
     */
    public Boolean authenticateTenant(long fdfTenantId, String sha256EncryptedPassword) {
        Boolean isValid = false;

        // get the tenant
        FdfEntity<FdfTenant> tenant = getEntityById(FdfTenant.class, fdfTenantId);

        // compare the password hashes
        if(tenant.current.sha256EncodedPassword.equals(sha256EncryptedPassword)) {
            isValid = true;
        }

        return isValid;
    }
}
