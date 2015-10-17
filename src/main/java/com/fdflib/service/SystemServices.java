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
public class SystemServices implements FdfCommonServices {

    public List<FdfSystem> getAllSystems() {
        List<FdfSystem> currentSystems = new ArrayList<>();

        for(FdfEntity<FdfSystem> system: getAllSystemsWithHistory()) {
            if(system != null && system.current != null) {
                currentSystems.add(system.current);
            }
        }

        return currentSystems;
    }

    public List<FdfEntity<FdfSystem>> getAllSystemsWithHistory() {

        return this.getAll(FdfSystem.class);
    }

    public FdfSystem getSystemById(long systemId) {
        return getSystemByIdWithHistory(systemId).current;
    }

    public FdfEntity<FdfSystem> getSystemByIdWithHistory(long systemId) {

        // get the system
        if(systemId > 0) {
            return getEntityById(FdfSystem.class, systemId);
        }

        return null;
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
        whereDf.operator = WhereClause.Operators.NOT_EQUAL;
        whereDf.value = "1";
        whereDf.valueDataType = Integer.class;

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

    /**
     * Returns true if the passed SHA-256 hashed password matched the one saved for the tenant.
     * @param clearTextPassword Clear text password to be hashed
     * @return String representing the hashed password
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public String hashPassword(String clearTextPassword) {

        byte[] digest = null;
        StringBuffer sb = new StringBuffer();

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(clearTextPassword.getBytes("UTF-8"));
            digest = md.digest();

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
        FdfEntity<FdfSystem> system = getEntityById(FdfSystem.class, fdfTenantId);

        // compare the password hashes
        if(system.current.sha256EncodedPassword.equals(sha256EncryptedPassword)) {
            isValid = true;
        }

        return isValid;
    }

}
