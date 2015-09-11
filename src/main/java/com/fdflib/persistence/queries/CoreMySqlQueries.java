/** * 4DFLib * Copyright (c) 2015 Brian Gormanly * 4dflib.com * * 4DFLib is free software; you can redistribute it and/or modify it under * the terms of the GNU Lesser General Public License as published by the Free * Software Foundation; either version 3 of the License, or (at your option) * any later version. * * This library is distributed in the hope that it will be useful, but WITHOUT * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more * details. */package com.fdflib.persistence.queries;import com.fdflib.model.state.CommonState;import com.fdflib.model.state.SystemState;import com.fdflib.model.util.WhereClause;import com.fdflib.persistence.FdfPersistence;import com.fdflib.persistence.connection.DbConnectionManager;import com.fdflib.persistence.database.MySqlConnection;import com.fdflib.service.impl.StateServices;import com.fdflib.util.FdfSettings;import com.fdflib.util.GeneralConstants;import org.slf4j.LoggerFactory;import java.lang.reflect.Field;import java.sql.PreparedStatement;import java.sql.ResultSet;import java.sql.SQLException;import java.sql.Statement;import java.util.*;/** * Created by brian.gormanly on 5/19/15. */public class CoreMySqlQueries extends DbConnectionManager {    private static final CoreMySqlQueries INSTANCE = new CoreMySqlQueries();    static org.slf4j.Logger fdfLog = LoggerFactory.getLogger(CoreMySqlQueries.class);    private CoreMySqlQueries() {}    public static CoreMySqlQueries getInstance() {        return INSTANCE;    }    public void createDatabase() throws SQLException {        // create database:        String sqlCreate = "CREATE DATABASE " + FdfSettings.DB_NAME + " CHARACTER SET UTF8;";        String sqlUserGrant = "GRANT ALL ON " + FdfSettings.DB_NAME + ".* to '" + FdfSettings.DB_USER + "'@'"                + FdfSettings.DB_HOST + "' IDENTIFIED BY '" + FdfSettings.DB_PASSWORD + "'";        Statement ps;        try {            ps = MySqlConnection.getInstance().getNoDBSession().createStatement();            if(ps != null) {                ps.executeUpdate(sqlCreate);                ps.executeUpdate(sqlUserGrant);                fdfLog.info("(createDatabase): Database Did not exist, attempting to create.");                // get the 4df data model                List<Class> classList = FdfSettings.getInstance().modelClasses;                // create the tables for the model objects                for(Class c: classList) {                    try {                        FdfPersistence.getInstance().createTable(c);                    } catch (SQLException e) {                        e.printStackTrace();                    }                }                // create the default System entry                SystemState defaultSystemState = new SystemState();                defaultSystemState.name = "Default";                defaultSystemState.description = "Default system represents the actual application and not a " +                        "registered external system.";                StateServices.save(SystemState.class, defaultSystemState, 0, 0);            }        } catch (SQLException sqlException) {            if (sqlException.getErrorCode() == 1007) {                // Database exists! (no need to create)                fdfLog.debug("(createDatabase): Database Exists...");            } else {                // Database did not exist, go ahead and create the users, tables, etc            }        }        finally {            ps = null;            // close the connection            MySqlConnection.getInstance().close();        }    }    public void createTable(Class c) throws SQLException {        fdfLog.info("create table: {}", c.getSimpleName());        // check there there is at lease one field        if(c.getFields().length > 0) {            String sql = "CREATE TABLE IF NOT EXISTS " + FdfSettings.DB_NAME + "." + c.getSimpleName() + " ( ";            int fieldCounter = 0;            for (Field field : c.getFields()) {                if(field.getType() == String.class) {                    sql += field.getName() + " TEXT";                }                else if(field.getType() == int.class || field.getType() == Integer.class) {                    sql += field.getName() + " INT";                }                else if(field.getType() == Long.class || field.getType() == long.class) {                    sql += field.getName() + " BIGINT";                }                else if(field.getType() == Double.class || field.getType() == double.class) {                    sql += field.getName() + " DOUBLE";                }                else if(field.getType() == Float.class || field.getType() == float.class) {                    sql += field.getName() + " FLOAT";                }                else if(field.getType() == boolean.class || field.getType() == Boolean.class) {                    sql += field.getName() + " TINYINT(1)";                }                else if(field.getType() == Date.class) {                    sql += field.getName() + " TIMESTAMP";                }                fieldCounter++;                if(c.getFields().length > fieldCounter) sql += ", ";            }            sql += ");";            fdfLog.debug("Table sql {} : {}", c.getSimpleName(), sql);            Statement ps;            try {                ps = MySqlConnection.getInstance().getNoDBSession().createStatement();                if(ps != null) {                    ps.executeUpdate(sql);                }            } catch (SQLException e) {                e.printStackTrace();            } catch (Exception ex) {                ex.printStackTrace();            }            finally {                ps = null;                // close the connection                MySqlConnection.getInstance().close();            }        }        else {            fdfLog.info("No table created for model object {} class had no valid data members", c.getSimpleName());        }    }    public <S> void update(Class<S> c, S state) {        // Start the sql statement        String sql = "update " + c.getSimpleName() + " set";        int fieldCounter = 0;        for(Field field: c.getFields()) {            try {                if(field.getType() == String.class) {                    sql += " " + field.getName() + " = '" + field.get(state) + "'";                }                else if(field.getType() == int.class || field.getType() == Integer.class) {                    if(!field.getName().equals("rid")) {                        sql += " " + field.getName() + " =";                        if (field.getInt(state) == -1) {                            sql += " NULL";                        } else {                            sql += " " + field.get(state);                        }                    }                }                else if(field.getType() == Long.class || field.getType() == long.class) {                    if(!field.getName().equals("rid")) {                        sql += " " + field.getName() + " =";                        if (field.getLong(state) == -1) {                            sql += " NULL";                        } else {                            sql += " " + field.get(state);                        }                    }                }                else if(field.getType() == Double.class || field.getType() == double.class) {                    if(!field.getName().equals("rid")) {                        sql += " " + field.getName() + " =";                        if (field.getDouble(state) == -1) {                            sql += " NULL";                        } else {                            sql += " " + field.get(state);                        }                    }                }else if(field.getType() == Float.class || field.getType() == float.class) {                    if(!field.getName().equals("rid")) {                        sql += " " + field.getName() + " =";                        if (field.getFloat(state) == -1) {                            sql += " NULL";                        } else {                            sql += " " + field.get(state);                        }                    }                }                else if(field.getType() == boolean.class || field.getType() == Boolean.class) {                    sql += " " + field.getName() + " =";                    if(field.getBoolean(state) == true) {                        sql += " true";                    }                    else if (field.getBoolean(state) == false) {                        sql += " false";                    }                }                else if(field.getType() == Date.class) {                    Date insertDate = (Date) field.get(state);                    sql += " " + field.getName() + " =";                    if(insertDate != null) {                        sql += " '" + GeneralConstants.DB_DATE_FORMAT.format(insertDate) + "'";                    }                    else {                        sql += " NULL";                    }                }                else if(field.get(state) == WhereClause.NULL) {                    sql += " " + field.getName() + " = NULL";                }                else {                    sql += " " + field.getName() + " = " + field.get(state);                }            } catch (IllegalAccessException e) {                e.printStackTrace();            }            fieldCounter++;            if(c.getFields().length > fieldCounter && !field.getName().equals("rid")) sql += ",";        }        // add where clause        try {            sql += " where rid = " + c.getField("rid").get(state) + " ;";            fdfLog.debug("update sql : {}", sql);            PreparedStatement ps;            try {                ps = MySqlConnection.getInstance().getSession().prepareStatement(sql);                if(ps != null) {                    ps.executeUpdate();                }            } catch (SQLException e) {                e.printStackTrace();            } catch (Exception ex) {                ex.printStackTrace();            }            finally {                ps = null;                // close the connection                MySqlConnection.getInstance().close();            }        } catch (IllegalAccessException e) {            e.printStackTrace();        } catch (NoSuchFieldException nsfe) {            nsfe.printStackTrace();        }    }    public <S> void insert(Class<S> c, S state) {        // Start the sql statement        String sql = "insert into " + c.getSimpleName() + " (";        int fieldCounter = 0;        for(Field field: c.getFields()) {            fieldCounter++;            if(!field.getName().equals("rid")) {                sql += " " + field.getName();                if (c.getFields().length > fieldCounter) sql += ",";            }        }        sql += " ) values (";        int fieldCounter2 = 0;        for(Field field: c.getFields()) {            try {                if(field.getType() == String.class) {                    sql += " '" + field.get(state) + "'";                }                else if(field.getType() == int.class || field.getType() == Integer.class) {                    if(!field.getName().equals("rid")) {                        if (field.getInt(state) == -1) {                            sql += " NULL";                        } else {                            sql += " " + field.get(state);                        }                    }                }                else if(field.getType() == Long.class || field.getType() == long.class) {                    if(!field.getName().equals("rid")) {                        if (field.getLong(state) == -1) {                            sql += " NULL";                        } else {                            sql += " " + field.get(state);                        }                    }                }                else if(field.getType() == Double.class || field.getType() == double.class) {                    if(!field.getName().equals("rid")) {                        if (field.getDouble(state) == -1) {                            sql += " NULL";                        } else {                            sql += " " + field.get(state);                        }                    }                }else if(field.getType() == Float.class || field.getType() == float.class) {                    if(!field.getName().equals("rid")) {                        if (field.getFloat(state) == -1) {                            sql += " NULL";                        } else {                            sql += " " + field.get(state);                        }                    }                }                else if(field.getType() == boolean.class || field.getType() == Boolean.class) {                    if(field.getBoolean(state) == true) {                        sql += " true";                    }                    else if (field.getBoolean(state) == false) {                        sql += " false";                    }                }                else if(field.getType() == Date.class) {                    Date insertDate = (Date) field.get(state);                    if(insertDate != null) {                        sql += " '" + GeneralConstants.DB_DATE_FORMAT.format(insertDate) + "'";                    }                    else {                        sql += " NULL";                    }                }                else if(field.get(state) == WhereClause.NULL) {                    sql += " NULL";                }                else {                    sql += " " + field.get(state);                }            } catch (IllegalAccessException e) {                e.printStackTrace();            }            fieldCounter2++;            if(c.getFields().length > fieldCounter2 && !field.getName().equals("rid")) sql += ",";        }        sql += " );";        fdfLog.debug("insert sql : {}", sql);        PreparedStatement ps;        try {            ps = MySqlConnection.getInstance().getSession().prepareStatement(sql);            if(ps != null) {                ps.execute();            }        } catch (SQLException e) {            e.printStackTrace();        } catch (Exception ex) {            ex.printStackTrace();        }        finally {            ps = null;            // close the connection            MySqlConnection.getInstance().close();        }    }    /**     * General select Query to retrieve all information for passed entity, can be used to return specified     * data from any table.  It looks to the class for datatype information and matches each table field returned     * to the EntityState object by name.  If specific select statements are made they only the corresponding object     * members will return with data.  If the select parameter is null, all memebers will be returned.     *     * Table to query is determined by passing in the corresponding model class. (ex. MyObjectModel.class)     * Where clauses are passed as an List of Where objects which contain the key (or name), the value to check     * against, and the type of Conditional (applied between clauses if there is more then one, AND is the default).     *     * Example sql statement that would be generated for the following class: User.class     * and where: {[firstName, Larry], [lastName, Smith, AND]} would be:     *      SELECT * FROM User where firstName = 'Larry' AND lastName = 'Smith';     *     * @param c     * @param where     * @param <S>     * @return     */    public <S extends CommonState> List<S> selectQuery(Class c, List<String> select, List<WhereClause> where) {        // start the sql statement        String sql = "select";        if(select != null) {            int selectCount = 0;            for(String selectItem: select) {                sql += " " + selectItem;                selectCount++;                if (selectCount < select.size()) sql += ",";            }        }        else {            sql += " *";        }        sql += " from " + c.getSimpleName();        sql += parseWhere(where);        sql += ";";        fdfLog.debug("select sql: {}", sql);        List<S> everything = new ArrayList<S>();        PreparedStatement ps;        ResultSet rs;        try {            ps = MySqlConnection.getInstance().getSession().prepareStatement(sql);            if(ps != null) {                rs = ps.executeQuery();                while (rs.next ()) {                    // create a new object of type passed                    Object thisObject = c.newInstance();                    for(Field field: c.getFields()) {                        // check the datatype of the field to apply the correct method to                        // retrieve the data on the resultset                        if(field.getType() == String.class) {                            try {                                field.setAccessible(true);                                field.set(thisObject, rs.getString(field.getName()));                            } catch (SQLException e) {                                if(e.getSQLState().equals("S0022")) {                                    // Invalid column name, thrown if select statement does not include column                                    fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"                                            + "{}, This is usually because select statement did not include column and "                                            + "can be ignored. Message is {}", field.getName(), e.getMessage());                                } else {                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());                                }                            }                        }                        if(field.getType() == int.class || field.getType() == Integer.class) {                            try {                                field.setAccessible(true);                                field.set(thisObject, rs.getInt(field.getName()));                            } catch (SQLException e) {                                if(e.getSQLState().equals("S0022")) {                                    // Invalid column name, thrown if select statement does not include column                                    fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"                                            + "{}, This is usually because select statement did not include column and "                                            + "can be ignored. Message is {}", field.getName(), e.getMessage());                                } else {                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());                                }                            }                        }                        if(field.getType() == long.class || field.getType() == Long.class) {                            try {                                field.setAccessible(true);                                field.set(thisObject, rs.getLong(field.getName()));                            } catch (SQLException e) {                                if(e.getSQLState().equals("S0022")) {                                    // Invalid column name, thrown if select statement does not include column                                    fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"                                            + "{}, This is usually because select statement did not include column and "                                            + "can be ignored. Message is {}", field.getName(), e.getMessage());                                } else {                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());                                }                            }                        }                        if(field.getType() == double.class || field.getType() == Double.class) {                            try {                                field.setAccessible(true);                                field.set(thisObject, rs.getDouble(field.getName()));                            } catch (SQLException e) {                                if(e.getSQLState().equals("S0022")) {                                    // Invalid column name, thrown if select statement does not include column                                    fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"                                            + "{}, This is usually because select statement did not include column and "                                            + "can be ignored. Message is {}", field.getName(), e.getMessage());                                } else {                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());                                }                            }                        }                        if(field.getType() == int.class || field.getType() == Integer.class) {                            try {                                field.setAccessible(true);                                field.set(thisObject, rs.getInt(field.getName()));                            } catch (SQLException e) {                                if(e.getSQLState().equals("S0022")) {                                    // Invalid column name, thrown if select statement does not include column                                    fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"                                            + "{}, This is usually because select statement did not include column and "                                            + "can be ignored. Message is {}", field.getName(), e.getMessage());                                } else {                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());                                }                            }                        }                        if(field.getType() == Date.class) {                            try {                                field.setAccessible(true);                                field.set(thisObject, rs.getDate(field.getName()));                            } catch (SQLException e) {                                if(e.getSQLState().equals("S0022")) {                                    // Invalid column name, thrown if select statement does not include column                                    fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"                                            + "{}, This is usually because select statement did not include column and "                                            + "can be ignored. Message is {}", field.getName(), e.getMessage());                                } else {                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());                                }                            }                        }                        if(field.getType() == boolean.class) {                            try {                                field.setAccessible(true);                                field.set(thisObject, rs.getBoolean(field.getName()));                            } catch (SQLException e) {                                if(e.getSQLState().equals("S0022")) {                                    // Invalid column name, thrown if select statement does not include column                                    fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"                                            + "{}, This is usually because select statement did not include column and "                                            + "can be ignored. Message is {}", field.getName(), e.getMessage());                                } else {                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());                                }                            }                        }                    }                    S thisUserStateTest = (S) thisObject;                    everything.add(thisUserStateTest);                }            }        } catch (SQLException e) {            e.printStackTrace();        } catch (Exception ex) {            ex.printStackTrace();        }        finally {            rs = null;            ps = null;            // close the connection            MySqlConnection.getInstance().close();        }        return everything;    }    static String parseWhere(List<WhereClause> where) {        // If where clauses were passed, add them to the sql statement        String sql = "";        if(where != null && where.size() > 0) {            sql += " where";            for(WhereClause clause : where) {                // if there is more then one clause, check the conditional type.                if(where.indexOf(clause) != 0 && (where.indexOf(clause) +1) <= where.size()) {                    if(clause.conditional == WhereClause.CONDITIONALS.AND) {                        sql += " AND";                    }                    else if (clause.conditional == WhereClause.CONDITIONALS.OR) {                        sql += " OR";                    }                }                // check to see if there are any open parenthesis to apply                if(clause.groupings != null && clause.groupings.size() > 0) {                    for(WhereClause.GROUPINGS grouping: clause.groupings) {                        if(grouping == WhereClause.GROUPINGS.OPEN_PARENTHESIS) {                            sql += " (";                        }                    }                }                // add the claus formatting the sql for the correct datatype                if(clause.valueDataType == String.class) {                    sql += " " + clause.name + " " + clause.getOperatorString() + " '" + clause.value + "'";                }                else if(clause.valueDataType == int.class || clause.valueDataType == Integer.class ||                        clause.valueDataType == long.class || clause.valueDataType == Long.class ||                        clause.valueDataType == double.class || clause.valueDataType == Double.class ||                        clause.valueDataType == float.class || clause.valueDataType == Float.class){                    sql += " " + clause.name + " " + clause.getOperatorString() + " " + clause.value;                }                else if(clause.valueDataType == boolean.class || clause.valueDataType == Boolean.class) {                    if(clause.value.toLowerCase().equals("true")) {                        sql += " " + clause.name + " " + clause.getOperatorString() + " true";                    }                    else if (clause.value.toLowerCase().equals("false")) {                        sql += " " + clause.name + " " + clause.getOperatorString() + " false";                    }                }                else if(clause.valueDataType == Date.class) {                    sql += " " + clause.name + " " + clause.getOperatorString() + " '" + clause.value + "'";                }                else if(clause.value == WhereClause.NULL) {                    sql += " " + clause.name + " " + clause.getOperatorString() + " " + clause.value + "";                }                else {                    sql += " " + clause.name + " " + clause.getOperatorString() + " '" + clause.value + "'";                }                // check to see if there are any closing parenthesis to apply                if(clause.groupings != null && clause.groupings.size() > 0) {                    for(WhereClause.GROUPINGS grouping: clause.groupings) {                        if(grouping == WhereClause.GROUPINGS.CLOSE_PARENTHESIS) {                            sql += " )";                        }                    }                }            }        }        return sql;    }}