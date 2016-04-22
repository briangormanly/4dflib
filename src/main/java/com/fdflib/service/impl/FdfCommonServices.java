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

package com.fdflib.service.impl;

import com.fdflib.model.entity.FdfEntity;
import com.fdflib.model.state.CommonState;
import com.fdflib.model.util.WhereClause;
import com.fdflib.model.util.WhereStatement;
import com.fdflib.persistence.FdfPersistence;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Universal implementation of the 4DF API, allows querying across all Entity states that extend CommonState.
 *
 * Created on 6/10/15.
 * @author brian.gormanly
 */
@SuppressWarnings("unused")
public abstract class FdfCommonServices {
    protected final static org.slf4j.Logger fdfLog = LoggerFactory.getLogger(CommonState.class);
    protected final static WhereStatement whereStatement = new WhereStatement();

    /* --- For Legacy --- */
    protected static void addByRid(long rid) {
        whereStatement.addByRid(rid);
    }
    protected static void addById(long id) {
        whereStatement.addById(id);
    }
    protected static void addByCf() {
        whereStatement.addByCf();
    }
    protected static void addNotCf() {
        whereStatement.addNotCf();
    }
    protected static void addByDf() {
        whereStatement.addByDf();
    }
    protected static void addByArsdBefore(Date date) {
        whereStatement.addByArsdBefore(date);
    }
    protected static void addByArsdAfter(Date date) {
        whereStatement.addByArsdAfter(date);
    }
    protected static void addByAredBefore(Date date) {
        whereStatement.addByAredBefore(date);
    }
    protected static void addByAredAfter(Date date) {
        whereStatement.addByAredAfter(date);
    }
    protected static void addAtDate(Date date) {
        whereStatement.addAtDate(date);
    }
    protected static void addByTid(long tid) {
        whereStatement.addByTid(tid);
    }
    protected static void addByEuid(long euid) {
        whereStatement.addByEuid(euid);
    }
    protected static void addByEsid(long esid) {
        whereStatement.addByEsid(esid);
    }

    public static void resetWhere() {
        whereStatement.reset();
    }
    public static String whereToString() {
        return whereStatement.toString();
    }
    /* --- End Legacy --- */

    /**
     * Save an Entities State to persistence internally manages all insert, update and actions associated with
     * maintaining the correct state of the data in persistence.  Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param state state to save
     * @param entityState State Type to save
     * @param userId Id of user that is saving the state
     * @param systemId Id of system that is saving the state
     * @param <S> parameterized type of entity state
     * @return S the saved entity state (without FdfEntity)
     */
    public static <S extends CommonState> S save(S state, Class<S> entityState, long userId, long systemId) {
        return save(state, entityState, userId, systemId, 1);
    }

    /**
     * Save an Entities State to persistence internally manages all insert, update and actions associated with
     * maintaining the correct state of the data in persistence.  Includes specified tenant (when using multi-tenant)
     *
     * This method signature assumes that the object contains the userId, systemId and tenantId saving the data in the
     * euid, esid, and tid respectively.
     *
     * @param state state to save
     * @param entityState State Type to save
     * @param <S> parameterized type of entity state
     * @return S the saved entity state (without FdfEntity)
     */
    public static <S extends CommonState> S save(S state, Class<S> entityState) {
        return save(state, entityState, state.euid, state.esid, state.tid);
    }

    /**
     * Save an Entities State to persistence internally manages all insert, update and actions associated with
     * maintaining the correct state of the data in persistence.  Includes specified tenant (when using multi-tenant)
     *
     * @param state state to save
     * @param entityState State Type to save
     * @param userId Id of user that is saving the state
     * @param systemId Id of system that is saving the state
     * @param tenantId Id of tenant this entity is associated with
     * @param <S> parameterized type of entity state
     * @return S the saved entity state (without FdfEntity)
     */
    public static <S extends CommonState> S save(S state, Class<S> entityState, long userId, long systemId, long tenantId) {
        // set the common meta fields for the new record
        state.arsd = Calendar.getInstance().getTime();
        state.ared = null;
        state.cf = true;
        state.euid = userId;
        state.esid = systemId;
        state.tid = tenantId;
        // check to see if this if an id is assigned (existing vs new entity)
        if(state.id <= 0) {
            // if this is a new entity, get an id for it
            state.id = getNewEntityId(entityState, tenantId);
            if(state.id <= 0) {
                return null;
            }
        }
        // get full entity for state
        FdfEntity<S> thisEntity = auditEntityById(entityState, state.id, tenantId);
        // check to see if there is an existing entity, if not, create
        if(thisEntity == null) {
            thisEntity = new FdfEntity<>();
        }
        // get the previous current record and move to history
        if(thisEntity.current != null) {
            S lastCurrentState = thisEntity.current;
            // set the end date
            lastCurrentState.ared = Calendar.getInstance().getTime();
            // set the current flag
            lastCurrentState.cf = false;
            // move the state to history
            FdfPersistence.getInstance().update(entityState, lastCurrentState);
        }
        // get id for rid
        return auditEntityByRid(entityState, FdfPersistence.getInstance().insert(entityState, state));
    }

    /**
     * Save an Entities State to persistence internally manages all insert, update and actions associated with
     * maintaining the correct state of the data in persistence.  Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState State Type to save
     * @param state state to save
     * @param userId Id of user that is saving the state
     * @param systemId Id of system that is saving the state
     * @param <S> parameterized type of entity state
     * @return FdfEntity that contains current and historical states for the saved entity
     */
    public static <S extends CommonState> FdfEntity<S> save(Class<S> entityState, S state, long userId, long systemId) {
        return save(entityState, state, userId, systemId, 1);
    }

    /**
     * Save an Entities State to persistence internally manages all insert, update and actions associated with
     * maintaining the correct state of the data in persistence.  Includes specified tenant (when using multi-tenant)
     *
     * This method signature assumes that the object contains the userId, systemId and tenantId saving the data in the
     * euid, esid, and tid respectively.
     *
     * @param entityState State Type to save
     * @param state state to save
     * @param <S> parameterized type of entity state
     * @return FdfEntity that contains current and historical states for the saved entity
     */
    public static <S extends CommonState> FdfEntity<S> save(Class<S> entityState, S state) {
        return save(entityState,state, state.euid, state.esid, state.tid);
    }

    /**
     * Save an Entities State to persistence internally manages all insert, update and actions associated with
     * maintaining the correct state of the data in persistence.  Includes specified tenant (when using multi-tenant)
     *
     * @param entityState State Type to save
     * @param state state to save
     * @param userId Id of user that is saving the state
     * @param systemId Id of system that is saving the state
     * @param tenantId Id of tenant this entity is associated with
     * @param <S> parameterized type of entity state
     * @return FdfEntity that contains current and historical states for the saved entity
     */
    public static <S extends CommonState> FdfEntity<S> save(Class<S> entityState, S state, long userId, long systemId, long tenantId) {
        // set the common meta fields for the new record
        state.arsd = Calendar.getInstance().getTime();
        state.ared = null;
        state.cf = true;
        state.euid = userId;
        state.esid = systemId;
        state.tid = tenantId;
        // check to see if this if an id is assigned (existing vs new entity)
        if(state.id <= 0) {
            // if this is a new entity, get an id for it
            state.id = getNewEntityId(entityState, tenantId);
            if(state.id < 0) {
                return null;
            }
        }
        // get full entity for state
        FdfEntity<S> thisEntity = auditEntityById(entityState, state.id, tenantId);
        // check to see if there is an existing entity, if not, create
        if(thisEntity == null) {
            thisEntity = new FdfEntity<>();
        }
        // get the previous current record and move to history
        if(thisEntity.current != null) {
            S lastCurrentState = thisEntity.current;
            // set the end date
            lastCurrentState.ared = Calendar.getInstance().getTime();
            // set the current flag
            lastCurrentState.cf = false;
            // move the state to history
            FdfPersistence.getInstance().update(entityState, lastCurrentState);
        }
        // save the new state as current
        long returnedRid = FdfPersistence.getInstance().insert(entityState, state);
        // get id for rid
        S entity = auditEntityByRid(entityState, returnedRid);
        // get the entity and return
        return auditEntityById(entityState, entity.id, tenantId);
    }

    /**
     * Sets delete flag for entity.  In order to record the date, time, user and system requesting the record be marked
     * deleted, a new current record is created to contain this information (arsd contains the date/time, ared is left
     * null, as would any current state).  The previous current state is marked as history and ends as of the present
     * date and time.  Queries on the entity for times before the delete flag was set will return the entity state for
     * the appropriate time period.
     *
     * Use this method for non multi-tenant systems.
     *
     * @param entityState The Entity Type to mark deleted
     * @param id The Id of the entity to mark deleted
     * @param userId the userId of the user making the change
     * @param systemId the systemId of the system makeing the change
     * @param <S> The parameterized type of the entity
     * @return FdfEntity that contains current and historical states for the saved entity
     */
    public static <S extends CommonState> FdfEntity<S> setDeleteFlag(Class<S> entityState, long id, long userId, long systemId) {
        return setDeleteFlag(entityState, id, userId, systemId, 1);
    }

    /**
     * Sets delete flag for entity.  In order to record the date, time, user and system requesting the record be marked
     * deleted, a new current record is created to contain this information (arsd contains the date/time, ared is left
     * null, as would any current state).  The previous current state is marked as history and ends as of the present
     * date and time.  Queries on the entity for times before the delete flag was set will return the entity state for
     * the appropriate time period.
     *
     * Use this method for multi-tenant systems.
     *
     * @param entityState The Entity Type to mark deleted
     * @param id The Id of the entity to mark deleted
     * @param userId the userId of the user making the change
     * @param systemId the systemId of the system makeing the change
     * @param tenantId the tenantId of the tenant making the change (if multi tenant)
     * @param <S> The parameterized type of the entity
     * @return FdfEntity that contains current and historical states for the saved entity
     */
    public static <S extends CommonState> FdfEntity<S> setDeleteFlag(Class<S> entityState, long id, long userId, long systemId, long tenantId) {
        if(id > -1) {
            // get full entity for state
            FdfEntity<S> thisEntity = auditEntityById(entityState, id, tenantId);
            // create the new state that will maintain the deletion records from the most recent state available
            S deletedState = thisEntity.getMostRecentState();
            // mark the state deleted
            deletedState.df = true;
            // save the state
            return save(entityState, deletedState, userId, systemId, tenantId);
        }
        return null;
    }

    /**
     * Removes delete flag for entity.  In order to record the date, time, user and system the record be marked
     * deleted, a new current record is created to contain this information (arsd contains the date/time, ared is left
     * null, as would any current state).  The previous current state is marked as history maintaining it's df flag of
     * true which tracks the time that the entity was marked deleted.  Queries on the entity for times when the delete
     * flag was set will not return a current state for the entity, only history, queries on the entity after the
     * delete flag was removed will have a current record and a gap in history during the time the entity was marked
     * as deleted.
     *
     * Use this method for non multi-tenant systems
     *
     * @param entityState The Entity Type to mark deleted
     * @param id The Id of the entity to mark deleted
     * @param userId the userId of the user making the change
     * @param systemId the systemId of the system makeing the change
     * @param <S> The parameterized type of the entity
     * @return FdfEntity that contains current and historical states for the saved entity
     */
    public static <S extends CommonState> FdfEntity<S> removeDeleteFlag(Class<S> entityState, long id, long userId, long systemId) {
        return removeDeleteFlag(entityState, id, userId, systemId, 1);
    }

    /**
     * Removes delete flag for entity.  In order to record the date, time, user and system the record be marked
     * deleted, a new current record is created to contain this information (arsd contains the date/time, ared is left
     * null, as would any current state).  The previous current state is marked as history maintaining it's df flag of
     * true which tracks the time that the entity was marked deleted.  Queries on the entity for times when the delete
     * flag was set will not return a current state for the entity, only history, queries on the entity after the
     * delete flag was removed will have a current record and a gap in history during the time the entity was marked
     * as deleted.
     *
     * Use this method for multi-tenant systems
     *
     * @param entityState The Entity Type to mark deleted
     * @param id The Id of the entity to mark deleted
     * @param userId the userId of the user making the change
     * @param systemId the systemId of the system makeing the change
     * @param tenantId the tenantId of the tenant making the change (if multi tenant)
     * @param <S> The parameterized type of the entity
     * @return FdfEntity that contains current and historical states for the saved entity
     */
    public static <S extends CommonState> FdfEntity<S> removeDeleteFlag(Class<S> entityState, long id, long userId, long systemId, long tenantId) {
        if(id > -1) {
            // get full entity for state
            FdfEntity<S> thisEntity = auditEntityById(entityState, id, tenantId);
            // create the new state that will maintain the deletion records from the most recent state available
            S deletedState = thisEntity.getMostRecentState();
            // mark the state deleted
            deletedState.df = false;
            // save the state
            return save(entityState, deletedState, userId, systemId, tenantId);
        }
        return null;
    }

    /**
     * Retrieves all entities including deleted records of type passed from persistence. Includes all current and
     * historical data for each entity returned.  Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> auditAll(Class<S> entityState) {
        return auditAll(entityState, 1);
    }

    /**
     * Retrieves all entities including deleted records of type passed from persistence. Includes all current and
     * historical data for each entity returned.  Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> auditAll(Class<S> entityState, long tenantId) {
        addByTid(tenantId);
        return manageReturnedEntities(whereStatement.run(entityState));
    }

    /**
     * Retrieves all entities including deleted records of type passed from persistence, only returns current data for
     * each entity, without any historical data.  Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<S> auditAllCurrent(Class<S> entityState) {
        return auditAllCurrent(entityState, 1);
    }

    /**
     * Retrieves all entities including deleted records of type passed from persistence, only returns current data for
     * each entity, without any historical data.  Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<S> auditAllCurrent(Class<S> entityState, long tenantId) {
        addByCf();
        addByTid(tenantId);
        return whereStatement.run(entityState);
    }

    /**
     * Retrieves all entities of type passed from persistence. Includes all current and historical data for
     * each entity returned.  Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getAll(Class<S> entityState) {
        return getAll(entityState, 1);
    }

    /**
     * Retrieves all entities of type passed from persistence. Includes all current and historical data for
     * each entity returned.  Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getAll(Class<S> entityState, long tenantId) {
        addByDf();
        addByTid(tenantId);
        return manageReturnedEntities(whereStatement.run(entityState));
    }

    /**
     * Retrieves all entities of type passed from persistence, only returns current data for each entity, without
     * any historical data.  Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<S> getAllCurrent(Class<S> entityState) {
        return getAllCurrent(entityState, 1);
    }

    /**
     * Retrieves all entities of type passed from persistence, only returns current data for each entity, without
     * any historical data.  Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<S> getAllCurrent(Class<S> entityState, long tenantId) {
        addByDf();
        addByCf();
        addByTid(tenantId);
        return whereStatement.run(entityState);
    }

    /**
     * Retrieves all entities of type passed from persistence, only returning the historical data in the entity,
     * no current data is included.  Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getAllHistory(Class<S> entityState) {
        return getAllHistory(entityState, 1);
    }

    /**
     * Retrieves all entities of type passed from persistence, only returning the historical data in the entity,
     * no current data is included.  Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getAllHistory(Class<S> entityState, long tenantId) {
        addByDf();
        addNotCf();
        addByTid(tenantId);
        return manageReturnedEntities(whereStatement.run(entityState));
    }

    /**
     * Retrieves all entities of the passed type from persistence as they existed at the date passed. Only states
     * existing at the date passed will be returned.  Usually this will only return one State per Entity in the form
     * they were in at that time, however if the time passed was the same time as a change to a entity you will get
     * back both the states as the end date of the outgoing and the startdate of the incoming will match the date
     * passed.
     *
     * One state will be returned for each entity, if the state is still the current state it will be contained
     * in the entity.current else it will be in the entity.history
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param date Date at which to get entities state
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<S> getAllAtDate(Class<S> entityState, Date date) {
        return getAllAtDate(entityState, date, 1);
    }

    /**
     * Retrieves all entities of the passed type from persistence as they existed at the date passed. Only states
     * existing at the date passed will be returned. This will return one State as they were in at that time.
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param date Date at which to get entities state
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<S> getAllAtDate(Class<S> entityState, Date date, long tenantId) {
        addByDf();
        addAtDate(date);
        addByTid(tenantId);
        return whereStatement.run(entityState);
    }

    /**
     * Retrieves all entities of the passed type from persistence as they existed at the date passed. Only states
     * existing at the date passed will be returned.
     *
     * Audit includes deleted records
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param date Date at which to get entities state
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<S> auditAllAtDate(Class<S> entityState, Date date) {
        return auditAllAtDate(entityState, date, 1);
    }

    /**
     * Retrieves all entities of the passed type from persistence as they existed at the date passed. Only states
     * existing at the date passed will be returned. This will return one State as they were in at that time.
     *
     * Audit includes deleted records
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param date Date at which to get entities state
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<S> auditAllAtDate(Class<S> entityState, Date date, long tenantId) {
        addAtDate(date);
        addByTid(tenantId);
        return whereStatement.run(entityState);
    }

    /**
     * Retrieves all entities that have states active starting at or after the date passed into the method.  Will
     * return current and historical data for the entity equal to or newer then the passed date, but no history
     * with an end date before the passed date.  If a entity does not have a record with a end date equal to or
     * past the passed date, nothing will be returned for that entity.
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param date Date to get entities states from
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getAllFromDate(Class<S> entityState, Date date) {
        return getAllFromDate(entityState, date, 1);
    }

    /**
     * Retrieves all entities that have states active starting at or after the date passed into the method.  Will
     * return current and historical data for the entity equal to or newer then the passed date, but no history
     * with an end date before the passed date.  If a entity does not have a record with a end date equal to or
     * past the passed date, nothing will be returned for that entity.
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param date Date to get entities states from
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getAllFromDate(Class<S> entityState, Date date, long tenantId) {
        addByDf();
        addByAredAfter(date);
        addByTid(tenantId);
        return manageReturnedEntities(whereStatement.run(entityState));
    }

    /**
     * Retrieves all entities that have states active starting at or before at the date passed into the method.
     * Will return current and historical data for the entity existing prior to the passed date, but no history
     * with an beginning date after the passed date.  If a entity does not have a record with a beginning date
     * equal to or before the passed date, nothing will be returned for that entity.
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param date Date to get entities states before
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getAllBeforeDate(Class<S> entityState, Date date) {
        return getAllBeforeDate(entityState, date, 1);
    }

    /**
     * Retrieves all entities that have states active starting at or before at the date passed into the method.
     * Will return current and historical data for the entity existing prior to the passed date, but no history
     * with an beginning date after the passed date.  If a entity does not have a record with a beginning date
     * equal to or before the passed date, nothing will be returned for that entity.
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param date Date to get entities states before
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getAllBeforeDate(Class<S> entityState, Date date, long tenantId) {
        addByDf();
        addByArsdBefore(date);
        addByTid(tenantId);
        return manageReturnedEntities(whereStatement.run(entityState));
    }

    /**
     * Retrieves all entities that have states active starting at or before at the startDate passed into the method
     * and ending at or after the endDate passed. Will return current and historical data for the entity existing
     * between these dates, but no history that exists completely before or after the range.  If an entity has states
     * that exist both inside and outside the range, only the ones existing within the range will be returned.  If a
     * entity has no states existing in the range then that entity will not be returned.
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param startDate Starting date in range to return entity state in
     * @param endDate Ending date in range to return entity state in
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getAllBetweenDates(Class<S> entityState, Date startDate, Date endDate) {
        return getAllBetweenDates(entityState, startDate, endDate, 1);
    }

    /**
     * Retrieves all entities that have states active starting at or before at the startDate passed into the method
     * and ending at or after the endDate passed. Will return current and historical data for the entity existing
     * between these dates, but no history that exists completely before or after the range.  If an entity has states
     * that exist both inside and outside the range, only the ones existing within the range will be returned.  If a
     * entity has no states existing in the range then that entity will not be returned.
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param startDate Starting date in range to return entity state in
     * @param endDate Ending date in range to return entity state in
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getAllBetweenDates(Class<S> entityState, Date startDate, Date endDate, long tenantId) {
        addByDf();
        addByArsdBefore(endDate);
        addByAredAfter(startDate);
        addByTid(tenantId);
        return manageReturnedEntities(whereStatement.run(entityState));
    }

    /**
     * Retrieves the entity associated with the rid passed. Returns current and historical states for the entity
     *
     * Getting an entity by Id is a "FdfTenant Safe" operation and works for multi and single tenant calls.
     *
     * @param entityState The entity type to query
     * @param rid The Id of the Entity to retrieve
     * @param <S> Parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> S getEntityByRid(Class<S> entityState, long rid) {
        addByDf();
        addByRid(rid);
        return (S) whereStatement.run(entityState).stream().findFirst().orElse(null);
    }

    /**
     * Retrieves all entities including deleted records of type passed from persistence. Returns current and historical
     *
     * states for the entity.  Getting an entity by Id is a "FdfTenant Safe" operation and works for multi and single
     * tenant calls.
     *
     * @param entityState The entity type to query
     * @param rid The Id of the Entity to retrieve
     * @param <S> Parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> S auditEntityByRid(Class<S> entityState, long rid) {
        addByRid(rid);
        return (S) whereStatement.run(entityState).stream().findFirst().orElse(null);
    }

    /**
     * Retrieves the entity associated with the id passed. Returns current and historical states for the entity
     * including states that are in a df state.
     *
     * Getting an entity by Id is a "FdfTenant Safe" operation and works for multi and single tenant calls.
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id The Id of the Entity to retrieve
     * @param <S> Parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> auditEntityById(Class<S> entityState, long id) {
        return auditEntityById(entityState, id, 1);
    }

    /**
     * Retrieves the entity associated with the id passed. Returns current and historical states for the entity
     * including states that are in a df state.
     *
     * Getting an entity by Id is a "FdfTenant Safe" operation and works for multi and single tenant calls.
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id The Id of the Entity to retrieve
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> Parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> auditEntityById(Class<S> entityState, long id, long tenantId) {
        addById(id);
        addByTid(tenantId);
        return manageReturnedEntity(whereStatement.run(entityState));
    }

    /**
     * Retrieves the entity associated with the id passed. Returns current and historical states for the entity
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id The Id of the Entity to retrieve
     * @param <S> Parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityById(Class<S> entityState, long id) {
        return getEntityById(entityState, id, 1);
    }

    /**
     * Retrieves the entity associated with the id passed. Returns current and historical states for the entity
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id The Id of the Entity to retrieve
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> Parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityById(Class<S> entityState, long id, long tenantId) {
        addByDf();
        addById(id);
        addByTid(tenantId);
        return manageReturnedEntity(whereStatement.run(entityState));
    }

    /**
     * Retrieves the entity of type passed from persistence, only returns current data, without any historical
     * data.
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityCurrentById(Class<S> entityState, long id) {
        return getEntityCurrentById(entityState, id, 1);
    }

    /**
     * Retrieves the entity of type passed from persistence, only returns current data, without any historical
     * data.
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityCurrentById(Class<S> entityState, long id, long tenantId) {
        addByDf();
        addByCf();
        addById(id);
        addByTid(tenantId);
        return manageReturnedEntity(whereStatement.run(entityState));

    }

    /**
     * Retrieves entity of type passed from persistence, only returning the historical data in the entity,
     * no current data is included.
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityHistoryById(Class<S> entityState, long id) {
        return getEntityHistoryById(entityState, id, 1);
    }

    /**
     * Retrieves entity of type passed from persistence, only returning the historical data in the entity,
     * no current data is included.
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityHistoryById(Class<S> entityState, long id, long tenantId) {
        addNotCf();
        addByDf();
        addById(id);
        addByTid(tenantId);
        return manageReturnedEntity(whereStatement.run(entityState));
    }

    /**
     * Retrieves the entity associated that contains the value passed for the field passed.  Returns current and
     * historical states for the entity
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param fieldName The Field of that is being checked for the passed value
     * @param value The value that we are checking in the field passed for.
     * @param <S> Parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getEntitiesByValueForPassedField(Class<S> entityState, String fieldName, String value) {
        return getEntitiesByValueForPassedField(entityState, fieldName, value, 1);
    }

    /**
     * Retrieves the entity associated that contains the value passed for the field passed.  Returns current and
     * historical states for the entity
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param fieldName The Field of that is being checked for the passed value
     * @param value The value that we are checking in the field passed for.
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> Parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getEntitiesByValueForPassedField(Class<S> entityState, String fieldName, String value, long tenantId) {
        try {
            if(value != null && tenantId > 0) {
                Field passedField = entityState.getField(fieldName);
                if (passedField != null) {
                    Type passedFieldType = passedField.getGenericType();
                    if (passedFieldType != null && value != null && tenantId > 0) {
                        WhereClause whereField = new WhereClause();
                        whereField.name = fieldName;
                        whereField.operator = WhereClause.Operators.EQUAL;
                        whereField.value = value;
                        whereField.valueDataType = passedFieldType;
                        whereStatement.add(whereField);
                        addByDf();
                        addByTid(tenantId);
                        return manageReturnedEntities(whereStatement.run(entityState));
                    }
                }
            }
        }
        finally {
            resetWhere();
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves the entity associated that contains the value passed for the field passed.  Returns current and
     * historical states for the entity
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param fieldsAndValues HashMap that contains Key: Field and Value: value pairs to query by
     * @param <S> Parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getEntitiesByValuesForPassedFields(Class<S> entityState, HashMap<String, String> fieldsAndValues) {
        return getEntitiesByValuesForPassedFields(entityState, fieldsAndValues, 1);
    }

    /**
     * Retrieves the entity associated that contains the value passed for the field passed.  Returns current and
     * historical states for the entity
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param fieldsAndValues HashMap that contains Key: Field and Value: value pairs to query by
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> Parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> getEntitiesByValuesForPassedFields(Class<S> entityState, HashMap<String, String> fieldsAndValues, long tenantId) {
        for(Map.Entry<String, String> fieldValuePair: fieldsAndValues.entrySet()) {
            try {
                boolean valid = false;
                if(fieldValuePair.getValue() != null && tenantId > 0) {
                    Field passedField = entityState.getField(fieldValuePair.getKey());
                    if(passedField != null) {
                        Type passedFieldType = passedField.getGenericType();
                        if(passedFieldType != null) {
                            WhereClause whereFieldValue = new WhereClause();
                            whereFieldValue.conditional = WhereClause.CONDITIONALS.AND;
                            whereFieldValue.name = fieldValuePair.getKey();
                            whereFieldValue.operator = WhereClause.Operators.EQUAL;
                            whereFieldValue.value = fieldValuePair.getValue();
                            whereFieldValue.valueDataType = passedFieldType;
                            whereStatement.add(whereFieldValue);
                            valid = true;
                        }
                    }
                }
                if(!valid) {
                    return new ArrayList<>();
                }
            }
            catch (NoSuchFieldException e) {
                return new ArrayList<>();
            }
        }
        addByDf();
        addByTid(tenantId);
        return manageReturnedEntities(whereStatement.run(entityState));
    }

    /**
     * Retrieves entity of the passed type from persistence as it existed at the date passed. Only states
     * existing at the date passed will be returned.
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param date Date to get entity state at
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> S getAtDateById(Class<S> entityState, long id, Date date) {
        return getAtDateById(entityState, id, date, 1);
    }

    /**
     * Retrieves entity of the passed type from persistence as it existed at the date passed. Only states
     * existing at the date passed will be returned.  Usually this will only return one State in the form
     * they were in at that time.
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param date Date to get entity state at
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> S getAtDateById(Class<S> entityState, long id, Date date, long tenantId) {
        addByDf();
        addById(id);
        addAtDate(date);
        addByTid(tenantId);
        return (S) whereStatement.run(entityState).stream().findFirst().orElse(null);
    }

    /**
     * Retrieves entity of the passed type from persistence as it existed at the date passed. Only states
     * existing at the date passed will be returned.
     *
     * Audit includes deleted records
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param date Date to get entity state at
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> S auditAtDateById(Class<S> entityState, long id, Date date) {
        return auditAtDateById(entityState, id, date, 1);
    }

    /**
     * Retrieves entity of the passed type from persistence as it existed at the date passed. Only states
     * existing at the date passed will be returned.  Usually this will only return one State in the form
     * they were in at that time.
     *
     * Audit includes deleted records
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param date Date to get entity state at
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> S auditAtDateById(Class<S> entityState, long id, Date date, long tenantId) {
        addById(id);
        addAtDate(date);
        addByTid(tenantId);
        return (S) whereStatement.run(entityState).stream().findFirst().orElse(null);
    }

    /**
     * Retrieves states active starting at or after the date passed into the method for the entity requested.  Will
     * return current and historical data for the entity equal to or newer then the passed date, but no history
     * with an end date before the passed date.  If a entity does not have a record with a end date equal to or
     * past the passed date, nothing will be returned for that entity.
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param date Date to get entity state(s) from
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityFromDateById(Class<S> entityState, long id, Date date) {
        return getEntityFromDateById(entityState, id, date, 1);
    }

    /**
     * Retrieves states active starting at or after the date passed into the method for the entity requested.  Will
     * return current and historical data for the entity equal to or newer then the passed date, but no history
     * with an end date before the passed date.  If a entity does not have a record with a end date equal to or
     * past the passed date, nothing will be returned for that entity.
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param date Date to get entity state(s) from
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityFromDateById(Class<S> entityState, long id, Date date, long tenantId) {
        addByAredAfter(date);
        return getEntityById(entityState, id, tenantId);
    }

    /**
     * Retrieves entity that has states active starting at or before at the date passed into the method.
     * Will return current and historical data for the entity existing prior to the passed date, but no history
     * with an beginning date after the passed date.  If a entity does not have a record with a beginning date
     * equal to or before the passed date, nothing will be returned for that entity.
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param date Date to get entity state(s) before
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityBeforeDateById(Class<S> entityState, long id, Date date) {
        return getEntityBeforeDateById(entityState, id, date, 1);
    }

    /**
     * Retrieves entity that has states active starting at or before at the date passed into the method.
     * Will return current and historical data for the entity existing prior to the passed date, but no history
     * with an beginning date after the passed date.  If a entity does not have a record with a beginning date
     * equal to or before the passed date, nothing will be returned for that entity.
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param date Date to get entity state(s) before
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> parameterized type of entity
     * @return Entity of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityBeforeDateById(Class<S> entityState, long id, Date date, long tenantId) {
        addByArsdBefore(date);
        return getEntityById(entityState, id, tenantId);
    }

    /**
     * Retrieves entity that has states active starting at or before at the startDate passed into the method
     * and ending at or after the endDate passed. Will return current and historical data for the entity existing
     * between these dates, but no history that exists completely before or after the range.  If an entity has states
     * that exist both inside and outside the range, only the ones existing within the range will be returned.  If a
     * entity has no states existing in the range then that entity will not be returned.
     *
     * Uses the Default FdfTenant (when not using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param startDate Start date in range to get entity state(s) within
     * @param endDate End date in range to get entity state(s) within
     * @param <S> Parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityBetweenDatesById(Class<S> entityState, long id, Date startDate, Date endDate) {
        return getEntityBetweenDatesById(entityState, id, startDate, endDate, 1);
    }

    /**
     * Retrieves entity that has states active starting at or before at the startDate passed into the method
     * and ending at or after the endDate passed. Will return current and historical data for the entity existing
     * between these dates, but no history that exists completely before or after the range.  If an entity has states
     * that exist both inside and outside the range, only the ones existing within the range will be returned.  If a
     * entity has no states existing in the range then that entity will not be returned.
     *
     * Includes specified tenant (when using multi-tenant)
     *
     * @param entityState The entity type to query
     * @param id Id of the Entity to retrieve
     * @param startDate Start date in range to get entity state(s) within
     * @param endDate End date in range to get entity state(s) within
     * @param tenantId Id of the tenant to retrieve for (Multi-FdfTenant mode)
     * @param <S> Parameterized type of entity
     * @return List of type passed
     */
    public static <S extends CommonState> FdfEntity<S> getEntityBetweenDatesById(Class<S> entityState, long id, Date startDate, Date endDate, long tenantId) {
        addByArsdBefore(endDate);
        addByAredAfter(startDate);
        return getEntityById(entityState, id, tenantId);
    }

    /**
     * Takes a list of raw states returned by a query and organizes them into separate entities of type passed.
     * @param rawStates : List of states to organize into entities
     * //@param entityState : Class of entity to use for returned type.
     * @param <S> Parameterized Type of entity
     * @return List of Entities of Type passed
     */
    public static <S extends CommonState> List<FdfEntity<S>> manageReturnedEntities(List<S> rawStates) {
        // create a List of entities
        List<FdfEntity<S>> allEntities = new ArrayList<>();
        for(S state: rawStates) {
            // see if this states entityId has already been seen
            int flag = 0;

            // compare this id against existing ones
            for(FdfEntity thisEntity : allEntities) {
                if (thisEntity.entityId == state.id) {
                    flag++;
                    addStateToEntity(state, thisEntity);
                }
            }
            // state id was not found in existing entities, add a new one
            if(flag == 0) {
                // create a entity
                FdfEntity<S> entity = new FdfEntity<>();
                addStateToEntity(state, entity);
                allEntities.add(entity);
            }
        }
        return allEntities;
    }

    /**
     * Takes a list of raw states returned by a query and organizes them into separate entities of type passed.
     * @param rawStates : List of states to organize into entities
     * //@param entityState : Class of entity to use for returned type.
     * @param <S> Parameterized Type of entity
     * @return Entities of Type passed
     */
    public static <S extends CommonState> FdfEntity<S> manageReturnedEntity(List<S> rawStates) {
        //create an entity
        FdfEntity<S> entity = new FdfEntity<>();
        for(S state: rawStates) {
            //Add individual state to entity
            addStateToEntity(state, entity);
        }
        return entity;
    }

    /**
     * Manages the addition of a state to an entity. Ensures that the state is correctly added to the entity within
     * the current state or List of history states, as appropriate.  Also manages the entityId being set when the
     * first state is passed to an entity.
     *
     * @param state State to be added to an Entity
     * @param entity Entity that will have a state added
     * @param <S> Parameterized type of State
     */
    @SuppressWarnings("unchecked")
    public static <S extends CommonState> void addStateToEntity(CommonState state, FdfEntity<S> entity) {
        boolean flag = false;
        //Check to see if this is the first state being saved to the entity, if so set the entityId
        if(entity.entityId == -1) {
            entity.entityId = state.id;
        }
        //Check to see that the id of the state passed matches the existing id for this entity, otherwise it does not belong here.
        if(entity.entityId == state.id) {
            //If there is history for the entity and this is not a current state, check to see if the passed state is there already.
            if(!state.cf && entity.history.size() > 0) {
                //Check to see if this record was already in history
                for(CommonState historyState: entity.history) {
                    if (historyState.rid == state.rid) {
                        flag = true;
                    }
                }
            }
            //Set the record in the entity
            if(state.cf) {
                entity.current = (S) state;
            } else if(!flag) {
                entity.history.add((S) state);
            }
        }
    }

    /**
     * Uses the Default FdfTenant (when not using multi-tenant)
     * @param entityState Class of entity to use for returned type.
     * @param <S> Parameterized type of State.
     * @return Entities of Type passed
     */
    public static <S extends CommonState> long getNewEntityId(Class<S> entityState) {
        return getNewEntityId(entityState, 1);
    }

    /**
     * Includes specified tenant (when using multi-tenant)
     * @param entityState Class of entity to use for returned type.
     * @param tenantId Id of tenant associated with the entity
     * @param <S> Parameterized type of State.
     * @return Entities of Type passed
     */
    public static <S extends CommonState> long getNewEntityId(Class<S> entityState, long tenantId) {
        whereStatement.addByTid(tenantId);
        List<S> returnedStates = FdfPersistence.getInstance().selectQuery(entityState, Arrays.asList("max(id) as id"), whereStatement.asList());
        whereStatement.reset();
        if(returnedStates != null && returnedStates.size() == 1) {
            if(returnedStates.get(0).id == -1) {
                returnedStates.get(0).id = 0;
            }
            return returnedStates.get(0).id + 1;
        }
        return -1;
    }
}