/**
 * 4DFLib
 * Copyright (c) 2015-2016 Brian Gormanly
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

package com.fdflib.service;

import com.fdflib.model.entity.FdfEntity;
import com.fdflib.model.state.FdfSystem;
import com.fdflib.model.util.WhereClause;
import com.fdflib.persistence.FdfPersistence;
import com.fdflib.service.impl.FdfCommonServices;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brian.gormanly on 8/22/15.
 */
public class FdfSystemServices extends FdfCommonServices {

    public FdfEntity<FdfSystem> saveSystem(FdfSystem systemState) {
        return save(FdfSystem.class, systemState);
    }

    public FdfEntity<FdfSystem> deleteSystem(long userId, long tenantId, long callingUserId, long callingSystemId) {
        return setDeleteFlag(FdfSystem.class, userId, callingUserId, callingSystemId, tenantId);
    }

    public FdfEntity<FdfSystem> unDeleteSystem(long userId, long tenantId, long callingUserId, long callingSystemId) {
        return removeDeleteFlag(FdfSystem.class, userId, callingUserId, callingSystemId, tenantId);
    }

    public List<FdfSystem> getAllSystems() {
        return getAllSystems(1);
    }

    public List<FdfSystem> getAllSystems(long tenantId) {
        List<FdfSystem> currentSystems = new ArrayList<>();

        for(FdfEntity<FdfSystem> system: getAllSystemsWithHistory(tenantId)) {
            if(system != null && system.current != null) {
                currentSystems.add(system.current);
            }
        }

        return currentSystems;
    }

    public List<FdfEntity<FdfSystem>> getAllSystemsWithHistory() {
        return getAllSystemsWithHistory(1);
    }

    public List<FdfEntity<FdfSystem>> getAllSystemsWithHistory(long tenantId) {
        return getAll(FdfSystem.class, tenantId);
    }

    public FdfSystem getSystemById(long systemId) {
        return (systemId > 0 ? getEntityCurrentById(FdfSystem.class, systemId) : null);
    }
    public FdfEntity<FdfSystem> getSystemByIdWithHistory(long systemId) {
        return (systemId > 0 ? getEntityById(FdfSystem.class, systemId) : new FdfEntity<>());
    }

    public FdfSystem getDefaultSystem() {
        return getDefaultSystemWithHistory().current;
    }

    public FdfEntity<FdfSystem> getDefaultSystemWithHistory() {
        // create the where statement for the query
        List<WhereClause> whereStatement = new ArrayList<>();

        // check that deleted records are not returned
        WhereClause whereDf = new WhereClause();
        whereDf.name = "df";
        whereDf.operator = WhereClause.Operators.IS_NOT;
        whereDf.value = "true";
        whereDf.valueDataType = Boolean.class;

        // add the id check
        WhereClause whereId = new WhereClause();
        whereId.conditional = WhereClause.CONDITIONALS.AND;
        whereId.name = "id";
        whereId.operator = WhereClause.Operators.EQUAL;
        whereId.value = "1";
        whereId.valueDataType = Long.class;

        whereStatement.add(whereDf);
        whereStatement.add(whereId);

        // do the query
        List<FdfSystem> returnedService =
                FdfPersistence.getInstance().selectQuery(FdfSystem.class, null, whereStatement);

        // create a List of entities
        return manageReturnedEntity(returnedService);
    }

    public FdfSystem getTestSystem() {
        return getTestSystemWithHistory().current;
    }

    public FdfEntity<FdfSystem> getTestSystemWithHistory() {
        // create the where statement for the query
        List<WhereClause> whereStatement = new ArrayList<>();

        // check that deleted records are not returned
        WhereClause whereDf = new WhereClause();
        whereDf.name = "df";
        whereDf.operator = WhereClause.Operators.IS_NOT;
        whereDf.value = "true";
        whereDf.valueDataType = Boolean.class;

        // add the id check
        WhereClause whereId = new WhereClause();
        whereId.conditional = WhereClause.CONDITIONALS.AND;
        whereId.name = "id";
        whereId.operator = WhereClause.Operators.EQUAL;
        whereId.value = "2";
        whereId.valueDataType = Long.class;

        whereStatement.add(whereDf);
        whereStatement.add(whereId);

        // do the query
        List<FdfSystem> returnedService =
                FdfPersistence.getInstance().selectQuery(FdfSystem.class, null, whereStatement);

        // create a List of entities
        return manageReturnedEntity(returnedService);
    }

    public List<FdfSystem> getSystemsByName(String name) {
        List<FdfSystem> currentSystems = new ArrayList<>();

        for(FdfEntity<FdfSystem> system: getSystemsByNameWithHistory(name)) {
            if(system != null && system.current != null) {
                currentSystems.add(system.current);
            }
        }

        return currentSystems;
    }

    public List<FdfEntity<FdfSystem>> getSystemsByNameWithHistory(String name) {
        // create the where statement for the query
        List<WhereClause> whereStatement = new ArrayList<>();

        // check that deleted records are not returned
        WhereClause whereDf = new WhereClause();
        whereDf.name = "df";
        whereDf.operator = WhereClause.Operators.IS_NOT;
        whereDf.value = "true";
        whereDf.valueDataType = Boolean.class;

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

    /**
     * Returns true if the passed SHA-256 hashed password matched the one saved for the tenant.
     * @param clearTextPassword Clear text password to be hashed
     * @return String representing the hashed password
     */
    public String hashPassword(String clearTextPassword) {
        StringBuffer sb = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(clearTextPassword.getBytes("UTF-8"));
            byte[] digest = md.digest();

            for(byte b : digest) {
                sb.append(String.format("%02x", b));
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * Checks the passed system Id and SHA-256 hashed password against the system record, returns a true
     * if the credentials are correct and false otherwise.
     *
     * @param systemId System ID to check authentication for
     * @param sha256EncryptedPassword Tenant password (must be SHA-256 hashed) to check authentication for
     * @return True if authentication attempt is successful, false otherwise.
     */
    public Boolean authenticateSystem(long systemId, String sha256EncryptedPassword) {
        //compare the password hashes
        FdfEntity<FdfSystem> system = getEntityById(FdfSystem.class, systemId);
        return Boolean.valueOf(system != null && system.current != null && system.current.sha256EncodedPassword.equals(sha256EncryptedPassword));
    }

}
