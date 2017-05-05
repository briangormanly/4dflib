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

package com.fdflib.service.impl;

import com.fdflib.model.entity.FdfEntity;
import com.fdflib.model.state.CommonState;
import com.fdflib.model.util.SqlStatement;
import com.fdflib.model.util.WhereClause;
import com.fdflib.persistence.FdfPersistence;
import com.fdflib.util.GeneralConstants;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
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
        if(state.id <= 0) {
            //Generate valid id for entity
            state.id = getNewEntityId(entityState, tenantId);
        }
        else {
            //Get the current record and archive it
            S lastCurrentEntity = auditEntityCurrentById(entityState, state.id, tenantId);
            //FdfEntity<S> thisEntity = auditEntityById(entityState, state.id, tenantId);
            if(lastCurrentEntity != null) {
                //Mark as inactive
                lastCurrentEntity.cf = false;
                //Record inactivity date
                lastCurrentEntity.ared = Calendar.getInstance().getTime();
                //Update old state
                FdfPersistence.getInstance().update(entityState, lastCurrentEntity);
            }
        }

        if(state.order < 0) {
            //this means that order was stored as a position.
            state.order = Math.abs(state.order);
            getOrderByPosition(state);
        }
        else if(state.order > 0) {
            //this means that order was already set, so check if valid.
            validateOrder(state);
        }
        else {
            //this means that order should be set to last.
            state.order = getNewOrder(state.getClass(), state.tid);
        }

        //Set generic fields for new record, save entity, then return it.
        state.arsd = Calendar.getInstance().getTime();
        state.ared = null;
        state.cf = true;
        state.euid = userId;
        state.esid = systemId;
        state.tid = tenantId;
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
                return new FdfEntity<>();
            }
        }

        if(state.order < 0) {
            //this means that order was stored as a position.
            state.order = Math.abs(state.order);
            getOrderByPosition(state);
        }
        else if(state.order > 0) {
            //this means that order was already set, so check if valid.
            validateOrder(state);
        }
        else {
            //this means that order should be set to last.
            state.order = getNewOrder(state.getClass(), state.tid);
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

    //Calculates "order" value based on the desired position.
    private static <S extends CommonState> void getOrderByPosition(S state) {
        SqlStatement statement = SqlStatement.build().select("id, order").where(setForCurrent(state.tid)).orderBy("order");
        if(state.order == 1) {
            statement.limit(1,1);
        } else {
            statement.limit(2, (int)state.order-1);
        }
        List<S> orderBetween = statement.run((Class<S>) state.getClass());
        if(orderBetween.size() == 2) {
            if(orderBetween.get(1).id != state.id && orderBetween.get(0).order >= state.order || state.order >= orderBetween.get(1).order) {
                if (orderBetween.get(1).order - orderBetween.get(0).order > 1) {
                    state.order = (orderBetween.get(0).order >= state.order
                            ? Math.floor(orderBetween.get(0).order + 1) : Math.ceil(orderBetween.get(1).order - 1));
                }
                String orderCurve = Double.toString(state.order = orderBetween.get(1).order + orderBetween.get(0).order);
                if (orderCurve.contains(".")) {
                    double curver = Math.pow(10, (1 + orderCurve.indexOf(".") - orderCurve.length()));
                    if (curver != orderBetween.get(1).order - orderBetween.get(0).order) {
                        state.order += curver;
                    }
                }
                state.order /= 2;
            }

        }
        else if(!orderBetween.isEmpty() && orderBetween.get(0).id != state.id) {
            if(state.order == 1) {
                if(orderBetween.get(0).order <= 1) {
                    String orderCurve = Double.toString(orderBetween.get(0).order);
                    if(orderCurve.contains(".")) {
                        double curver = Math.pow(10, (1 + orderCurve.indexOf(".") - orderCurve.length()));
                        if(curver != orderBetween.get(1).order - orderBetween.get(0).order) {
                            orderBetween.get(0).order += curver;
                        }
                    }
                    state.order = orderBetween.get(0).order/2;
                }
            }
            else if(orderBetween.get(0).order >= state.order) {
                state.order = Math.floor(orderBetween.get(0).order+1);
            }
        }
    }
    //Checks if "order" is already taken, and adjusts order if it is.
    private static <S extends CommonState> void validateOrder(S state) {
        WhereClause whereOrder = new WhereClause();
        whereOrder.name = "order";
        whereOrder.operator = WhereClause.Operators.LESS_THAN_OR_EQUAL;
        whereOrder.value = Double.toString(state.order);
        whereOrder.valueDataType = Double.class;
        List<S> orderBetween = SqlStatement.build().select("id, order").where(whereOrder).where(setForCurrent(state.tid))
                .orderBy("order DESC").limit(2,1).run((Class<S>) state.getClass());
        if(!orderBetween.isEmpty() && orderBetween.get(0).order == state.order && orderBetween.get(0).id != state.id) {
            if(orderBetween.size() == 2) {
                if(orderBetween.get(0).order - orderBetween.get(1).order > 1) {
                    state.order = Math.ceil(orderBetween.get(0).order-1);
                }
                else {
                    String orderCurb = Double.toString(state.order = orderBetween.get(1).order + orderBetween.get(0).order);
                    if(orderCurb.contains(".")) {
                        double curver = Math.pow(10, (1 + orderCurb.indexOf(".") - orderCurb.length()));
                        if(curver != orderBetween.get(0).order - orderBetween.get(1).order) {
                            state.order += curver;
                        }
                    }
                    state.order /= 2;
                }
            }
            else if(orderBetween.get(0).order <= 1) {
                String orderCurb = Double.toString(orderBetween.get(0).order);
                if(orderCurb.contains(".")) {
                    double curver = Math.pow(10, (1 + orderCurb.indexOf(".") - orderCurb.length()));
                    if(curver != orderBetween.get(0).order) {
                        state.order += curver;
                    }
                }
                state.order = orderBetween.get(0).order/2;
            }
            else {
                state.order = Math.ceil(orderBetween.get(0).order-1);
            }
        }
    }
    //Sets "order" to last possible order.
    private static <S extends CommonState> double getNewOrder(Class<S> entityState, long tenantId) {
        CommonState returnedState = SqlStatement.build().select("max(order) AS order").where(setForCurrent(tenantId)).run(entityState).stream().findAny().orElse(null);
        return (returnedState != null && returnedState.order > 0 ? Math.floor(returnedState.order+1) : 1);
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
        return new FdfEntity<>();
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
        return new FdfEntity<>();
    }

    /**
     * Returns a row (rid) count (equivalent of select count(rid) from ... from the class passed).
     * @param entityState The entity type to query the count for
     * @param <S> parameterized tipe of entity
     * @return long representing teh number of rows in the table
     */
    public static <S extends CommonState> long getRowCount(Class<S> entityState) {
        CommonState returnedState = SqlStatement.build().select("max(rid) AS rid").run(entityState).stream().findAny().orElse(null);
        return (returnedState != null ? returnedState.rid : 0);

        /*List<S> returnedQuery = FdfPersistence.getInstance().selectQuery(entityState, SqlStatement.build().select("rid"));
        return (returnedQuery != null ? returnedQuery.size() : 0);*/
    }

    /**
     * Returns an entity (id) count (equivalent of select count(id) from ... from the class passed).
     * @param entityState The entity type to query the count for
     * @param <S> parameterized tipe of entity
     * @return long representing teh number of rows in the table
     */
    public static <S extends CommonState> long getEntityCount(Class<S> entityState) {
        List<S> returnedQuery = FdfPersistence.getInstance().selectQuery(entityState, SqlStatement.build().select("distinct id"));
        return(returnedQuery != null ? returnedQuery.size() : 0);
    }

    /**
     * Allows SqlStatement generated queries to pass through to persistence calls.
     * TODO: Will also allow a place for rank logic to be applied
     *
     * @param entityState
     * @param sqlStatement
     * @param <S>
     * @return
     */
    public static <S extends CommonState> List<S> sqlStatementSelect(Class<S> entityState, SqlStatement sqlStatement) {
        sqlStatement = sqlStatement.orderBy("order");
        List<S> res = FdfPersistence.getInstance().selectQuery(entityState, sqlStatement);

        return res;
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
        return manageReturnedEntities(SqlStatement.build().where(addByTid(tenantId)).run(entityState));
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
        return SqlStatement.build().where(auditForCurrent(tenantId)).run(entityState);
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
        return manageReturnedEntities(SqlStatement.build().where(setWithHistory(tenantId)).run(entityState));
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
        return SqlStatement.build().where(setForCurrent(tenantId)).run(entityState);
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
        return manageReturnedEntities(SqlStatement.build().where(addNotCf()).where(setWithHistory(tenantId)).run(entityState));
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
        return SqlStatement.build().where(setAtDate(date, tenantId)).run(entityState);
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
        return SqlStatement.build().where(auditAtDate(date, tenantId)).run(entityState);
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
        return manageReturnedEntities(SqlStatement.build().where(addByAredAfter(date)).where(setWithHistory(tenantId)).run(entityState));
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
        return manageReturnedEntities(SqlStatement.build().where(addByArsdBefore(date)).where(setWithHistory(tenantId)).run(entityState));
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
        return manageReturnedEntities(SqlStatement.build().where(addByArsdBefore(endDate))
                .where(addByAredAfter(startDate)).where(setWithHistory(tenantId)).run(entityState));
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
        return SqlStatement.build().where(addByDf()).where(addByRid(rid)).run(entityState).stream().findFirst().orElse(null);
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
        return SqlStatement.build().where(addByRid(rid)).run(entityState).stream().findFirst().orElse(null);
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
        return manageReturnedEntity(SqlStatement.build().where(addById(id)).where(addByTid(tenantId)).run(entityState));
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
        return manageReturnedEntity(SqlStatement.build().where(addById(id)).where(setWithHistory(tenantId)).run(entityState));
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
    public static <S extends CommonState> S getEntityCurrentById(Class<S> entityState, long id) {
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
    public static <S extends CommonState> S getEntityCurrentById(Class<S> entityState, long id, long tenantId) {
        return SqlStatement.build().where(addById(id)).where(setForCurrent(tenantId)).run(entityState).stream().findAny().orElse(null);
    }

    public static <S extends CommonState> S auditEntityCurrentById(Class<S> entityState, long id, long tenantId) {
        return SqlStatement.build().where(addById(id)).where(auditForCurrent(tenantId)).run(entityState).stream().findAny().orElse(null);
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
        return manageReturnedEntity(SqlStatement.build().where(addNotCf()).where(addById(id)).where(setWithHistory(tenantId)).run(entityState));
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
        if(value != null && tenantId > 0) try {
            Class passedFieldType = entityState.getField(fieldName).getType();
            if(passedFieldType != null) {
                WhereClause whereField = new WhereClause();
                whereField.name = fieldName;
                whereField.operator = WhereClause.Operators.EQUAL;
                whereField.value = value;
                whereField.valueDataType = passedFieldType;
                return manageReturnedEntities(SqlStatement.build().where(whereField).where(setWithHistory(tenantId)).run(entityState));
            }
        } catch (NoSuchFieldException e) {
            fdfLog.debug("{} does not contain the field, {}",entityState.getSimpleName(), fieldName);
        }
        return new ArrayList<>();
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
        SqlStatement sqlStatement = SqlStatement.build();
        for(Map.Entry<String, String> fieldValuePair: fieldsAndValues.entrySet()) {
            try {
                if(fieldValuePair.getValue() != null && tenantId > 0) {
                    Field passedField = entityState.getField(fieldValuePair.getKey());
                    if(passedField != null) {
                        Class passedFieldType = passedField.getType();
                        if(passedFieldType != null) {
                            WhereClause whereFieldValue = new WhereClause();
                            whereFieldValue.conditional = WhereClause.CONDITIONALS.AND;
                            whereFieldValue.name = fieldValuePair.getKey();
                            whereFieldValue.operator = WhereClause.Operators.EQUAL;
                            whereFieldValue.value = fieldValuePair.getValue();
                            whereFieldValue.valueDataType = passedFieldType;
                            sqlStatement.where(whereFieldValue);
                        }
                    }
                }
            }
            catch (NoSuchFieldException e) {
                return new ArrayList<>();
            }
        }
        return manageReturnedEntities(sqlStatement.where(setWithHistory(tenantId)).run(entityState));
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
        return SqlStatement.build().where(addById(id)).where(setAtDate(date, tenantId)).run(entityState).stream().findFirst().orElse(null);
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
        return SqlStatement.build().where(addById(id)).where(auditAtDate(date, tenantId)).run(entityState).stream().findFirst().orElse(null);
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
        return manageReturnedEntity(SqlStatement.build().where(addByAredAfter(date)).where(addById(id)).where(setWithHistory(tenantId)).run(entityState));
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
        return manageReturnedEntity(SqlStatement.build().where(addByArsdBefore(date)).where(addById(id)).where(setWithHistory(tenantId)).run(entityState));
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
        return manageReturnedEntity(SqlStatement.build().where(addByArsdBefore(endDate)).where(addByArsdBefore(startDate))
                .where(addById(id)).where(setWithHistory(tenantId)).run(entityState));
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
            boolean flag = false;
            // compare this id against existing ones
            for(FdfEntity<S> thisEntity : allEntities) {
                if(thisEntity.entityId == state.id) {
                    flag = true;
                    addStateToEntity(state, thisEntity);
                }
            }
            // state id was not found in existing entities, add a new one
            if(!flag) {
                // build a entity
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
        FdfEntity<S> entity = new FdfEntity<>();
        rawStates.forEach(state -> addStateToEntity(state, entity));
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
    public static <S extends CommonState> void addStateToEntity(S state, FdfEntity<S> entity) {
        //Check to see if this is the first state being saved to the entity, if so set the entityId
        if(entity.entityId == -1) {
            entity.entityId = state.id;
        }
        //Check to see that the id of the state passed matches the existing id for this entity, otherwise it does not belong here.
        if(entity.entityId == state.id) {
            //Check to see if the passed state is the current state. If it isn't, check that it isn't already in history.
            if(state.cf) {
                entity.current = state;
            }
            else if(entity.history.stream().noneMatch(historyState -> historyState.rid == state.rid)) {
                entity.history.add(state);
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
     * @return Entities of Type passed
     */
    public static long getNewEntityId(Class<? extends CommonState> entityState, long tenantId) {
        CommonState returnedState = SqlStatement.build().select("max(id) AS id").where(auditForCurrent(tenantId)).run(entityState).stream().findAny().orElse(null);
        return (returnedState != null && returnedState.id > 0 ? returnedState.id + 1 : 1);
    }

    /* ---- Generic Service Functions ---- */
    protected static String parseIdSet(String idSet) {
        if(idSet.matches(".*\\d.*")) {
            idSet = idSet.replaceAll("-+", ",");
        }
        if(!idSet.startsWith("(")) {
            idSet = "(" + idSet;
        }
        if(!idSet.endsWith(")")) {
            idSet += ")";
        }
        return idSet;
    }

    /**
     * Creates a WhereClause that represents <i>WHERE rid = {@param rid}</i>
     * @param rid Value for WhereClause
     * @return WhereClause for rid
     */
    protected static WhereClause addByRid(long rid) {
        WhereClause whereRid = new WhereClause();
        whereRid.name = "rid";
        whereRid.operator = WhereClause.Operators.EQUAL;
        whereRid.value = Long.toString(rid);
        whereRid.valueDataType = Long.class;
        return whereRid;
    }
    /**
     * Creates a WhereClause that represents <b>WHERE id = {@param id}</b>
     * @param id Value for WhereClause
     * @return WhereClause for id
     */
    protected static WhereClause addById(long id) {
        WhereClause whereId = new WhereClause();
        whereId.name = "id";
        whereId.operator = WhereClause.Operators.EQUAL;
        whereId.value = Long.toString(id);
        whereId.valueDataType = Long.class;
        return whereId;
    }
    /**
     * Creates a WhereClause that represents <b>WHERE id IN ({@param idSet})</b>
     * @param idSet String that will be parsed into an IN statement value
     * @return WhereClause for id
     */
    protected static WhereClause addByIdSet(String idSet) {
        WhereClause whereIdSet = new WhereClause();
        whereIdSet.name = "id";
        whereIdSet.operator = WhereClause.Operators.IN;
        whereIdSet.value = parseIdSet(idSet);
        whereIdSet.valueDataType = Long.class;
        return whereIdSet;
    }
    /**
     * Creates a WhereClause that represents <b>WHERE cf = true</b>
     * @return WhereClause for cf
     */
    protected static WhereClause addByCf() {
        WhereClause whereCf = new WhereClause();
        whereCf.name = "cf";
        whereCf.operator = WhereClause.Operators.EQUAL;
        whereCf.value = "true";
        whereCf.valueDataType = Boolean.class;
        return whereCf;
    }
    /**
     * Creates a WhereClause that represents <b>WHERE cf &lt;&gt; true</b>
     * @return WhereClause for cf
     */
    protected static WhereClause addNotCf() {
        WhereClause whereCf = new WhereClause();
        whereCf.name = "cf";
        whereCf.operator = WhereClause.Operators.NOT_EQUAL;
        whereCf.value = "true";
        whereCf.valueDataType = Boolean.class;
        return whereCf;
    }
    protected static WhereClause addByDf() {
        WhereClause whereDf = new WhereClause();
        whereDf.name = "df";
        whereDf.operator = WhereClause.Operators.NOT_EQUAL;
        whereDf.value = "true";
        whereDf.valueDataType = Boolean.class;
        return whereDf;
    }
    protected static WhereClause addByArsdBefore(Date date) {
        if(date != null) {
            WhereClause whereStartBefore = new WhereClause();
            whereStartBefore.name = "arsd";
            whereStartBefore.operator = WhereClause.Operators.LESS_THAN_OR_EQUAL;
            whereStartBefore.value = GeneralConstants.DB_DATE_FORMAT.format(date);
            whereStartBefore.valueDataType = Date.class;
            return whereStartBefore;
        }
        return null;
    }
    protected static WhereClause addByArsdAfter(Date date) {
        if(date != null) {
            WhereClause whereStartAfter = new WhereClause();
            whereStartAfter.name = "arsd";
            whereStartAfter.operator = WhereClause.Operators.GREATER_THAN_OR_EQUAL;
            whereStartAfter.value = GeneralConstants.DB_DATE_FORMAT.format(date);
            whereStartAfter.valueDataType = Date.class;
            return whereStartAfter;
        }
        return null;
    }
    protected static WhereClause addByAredBefore(Date date) {
        WhereClause whereEndBefore = new WhereClause();
        whereEndBefore.name = "ared";
        if(date != null) {
            whereEndBefore.operator = WhereClause.Operators.LESS_THAN_OR_EQUAL;
            whereEndBefore.value = GeneralConstants.DB_DATE_FORMAT.format(date);
            whereEndBefore.valueDataType = Date.class;
        }
        else {
            whereEndBefore.operator = WhereClause.Operators.IS_NOT;
            whereEndBefore.value = WhereClause.NULL;
        }
        return whereEndBefore;
    }
    protected static List<WhereClause> addByAredAfter(Date date) {
        List<WhereClause> whereStatement = new ArrayList<>();
        WhereClause whereCurrent = new WhereClause();
        if(date != null) {
            WhereClause whereEndAfter = new WhereClause();
            whereEndAfter.groupings.add(WhereClause.GROUPINGS.OPEN_PARENTHESIS);
            whereEndAfter.name = "ared";
            whereEndAfter.operator = WhereClause.Operators.GREATER_THAN_OR_EQUAL;
            whereEndAfter.value = GeneralConstants.DB_DATE_FORMAT.format(date);
            whereEndAfter.valueDataType = Date.class;
            whereStatement.add(whereEndAfter);

            whereCurrent.conditional = WhereClause.CONDITIONALS.OR;
            whereCurrent.groupings.add(WhereClause.GROUPINGS.CLOSE_PARENTHESIS);
        }
        whereCurrent.name = "ared";
        whereCurrent.operator = WhereClause.Operators.IS;
        whereCurrent.value = WhereClause.NULL;
        whereStatement.add(whereCurrent);
        return whereStatement;
    }
    protected static List<WhereClause> addAtDate(Date date) {
        List<WhereClause> whereStatement = addByAredAfter(date);
        whereStatement.add(0, addByArsdBefore(date));
        return whereStatement;
    }
    protected static WhereClause addByTid(long tid) {
        WhereClause whereTid = new WhereClause();
        whereTid.name = "tid";
        whereTid.operator = WhereClause.Operators.EQUAL;
        whereTid.value = Long.toString(tid);
        whereTid.valueDataType = Long.class;
        return whereTid;
    }
    protected static WhereClause addByEuid(long euid) {
        WhereClause whereEuid = new WhereClause();
        whereEuid.name = "euid";
        whereEuid.operator = WhereClause.Operators.EQUAL;
        whereEuid.value = Long.toString(euid);
        whereEuid.valueDataType = Long.class;
        return whereEuid;
    }
    protected static WhereClause addByEsid(long esid) {
        WhereClause whereEsid = new WhereClause();
        whereEsid.name = "esid";
        whereEsid.operator = WhereClause.Operators.EQUAL;
        whereEsid.value = Long.toString(esid);
        whereEsid.valueDataType = Long.class;
        return whereEsid;
    }

    //Bundled WhereStatement Builders
    protected static List<WhereClause> setForCurrent(long tenantId) {
        List<WhereClause> whereStatement = auditForCurrent(tenantId);
        whereStatement.add(addByDf());
        return whereStatement;
    }
    protected static List<WhereClause> setAtDate(Date date, long tenantId) {
        List<WhereClause> whereStatement = auditAtDate(date, tenantId);
        whereStatement.add(addByDf());
        return whereStatement;
    }
    protected static List<WhereClause> setWithHistory(long tenantId) {
        List<WhereClause> whereStatement = new ArrayList<>();
        whereStatement.add(addByTid(tenantId));
        whereStatement.add(addByDf());
        return whereStatement;
    }
    protected static List<WhereClause> auditForCurrent(long tenantId) {
        List<WhereClause> whereStatement = new ArrayList<>();
        whereStatement.add(addByTid(tenantId));
        whereStatement.add(addByCf());
        return whereStatement;
    }
    protected static List<WhereClause> auditAtDate(Date date, long tenantId) {
        List<WhereClause> whereStatement = addAtDate(date);
        whereStatement.add(addByTid(tenantId));
        return whereStatement;
    }
}