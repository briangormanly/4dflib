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

package com.fdflib.persistence.queries;

import com.fdflib.annotation.FdfIgnore;
import com.fdflib.model.state.CommonState;
import com.fdflib.model.state.FdfSystem;
import com.fdflib.model.state.FdfTenant;
import com.fdflib.model.util.SqlStatement;
import com.fdflib.model.util.WhereClause;
import com.fdflib.persistence.database.MySqlConnection;
import com.fdflib.persistence.impl.CorePersistenceImpl;
import com.fdflib.service.FdfSystemServices;
import com.fdflib.service.FdfTenantServices;
import com.fdflib.util.FdfSettings;
import com.fdflib.util.FdfUtil;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Created by brian.gormanly on 5/19/15.
 * Updated by corley.herman over time.
 */
public class CoreMySqlQueries implements CorePersistenceImpl {
    private static final CoreMySqlQueries INSTANCE = new CoreMySqlQueries();
    private static org.slf4j.Logger fdfLog = LoggerFactory.getLogger(CoreMySqlQueries.class);

    private CoreMySqlQueries() {}

    public static CoreMySqlQueries getInstance() {
        return INSTANCE;
    }

    public void checkDatabase() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = MySqlConnection.getInstance().get4dfDbRootConnection();
            stmt = conn.createStatement();
            if(stmt != null) {
                rs = stmt.executeQuery("SELECT * FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + FdfSettings.DB_NAME + "';");
                if(rs != null && !rs.next()) {
                    //Database does not exist, create
                    String sqlCreate = "CREATE DATABASE IF NOT EXISTS " + FdfSettings.DB_NAME + " CHARACTER SET "
                            + FdfSettings.DB_ENCODING + ";";
                    String sqlUserGrant = "GRANT ALL ON " + FdfSettings.DB_NAME + ".* to '" + FdfSettings.DB_USER
                            + "'@'" + FdfSettings.DB_HOST + "' IDENTIFIED BY '" + FdfSettings.DB_PASSWORD + "'";


                    if(stmt != null) {
                        stmt.executeUpdate(sqlCreate);
                        stmt.executeUpdate(sqlUserGrant);
                        fdfLog.info("******************************************************************");
                        fdfLog.info("4DFLib Database did not exist, attempting to create.");
                        fdfLog.info("******************************************************************");

                        fdfLog.info("Database created.");
                        fdfLog.info("******************************************************************");
                    }
                }
            }
        }
        catch (SQLException sqlException) {
            fdfLog.warn("Error occurred checking or creating database:::");
            fdfLog.warn("SQL error \nCode: {},\nState: {}\nMessage" + ": {}\n",
                    sqlException.getErrorCode(), sqlException.getSQLState(), sqlException.getMessage());
        }
        finally {
            if (rs != null) {
                rs.close();
            }
            if(stmt != null) {
                stmt.close();
            }
            if(conn != null) {
                MySqlConnection.getInstance().close4dfDbSession(conn);
            }
        }
    }

    public void checkTables() throws SQLException {
        //Create tables for model
        for(Class c: FdfSettings.getInstance().modelClasses) {
            //Check if @FdfIgonre class
            if(!c.isAnnotationPresent(FdfIgnore.class)) {
                Connection conn = null;
                Statement stmt = null;
                ResultSet rs = null;
                try {
                    conn = MySqlConnection.getInstance().get4dfDbConnection();
                    stmt = conn.createStatement();
                    if (stmt != null) {
                        rs = stmt.executeQuery("SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA = '"
                                + FdfSettings.DB_NAME + "' AND TABLE_NAME = '" + c.getSimpleName().toLowerCase() + "';");
                        if (rs != null && !rs.next()) {
                            fdfLog.info("creating table: {}", c.getSimpleName().toLowerCase());
                            //Check that there is at lease one field
                            if(c.getFields().length > 0) {
                                //Table does not exist, create
                                StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                                        .append(FdfSettings.DB_NAME).append(".")
                                        .append(c.getSimpleName().toLowerCase()).append(" (");
                                Arrays.stream(c.getFields()).filter(field -> !field.isAnnotationPresent(FdfIgnore.class))
                                        .forEach(field -> sql.append(" ").append(getFieldNameAndDataType(field)).append(","));

                                String create = sql.deleteCharAt(sql.length()-1).append(");").toString();
                                fdfLog.debug("Table sql {} : {}", c.getSimpleName().toLowerCase(), create);
                                stmt.executeUpdate(create);
                            } else {
                                fdfLog.info("No table created for model object {} class had no valid data members",
                                        c.getSimpleName().toLowerCase());
                            }
                        }
                    }
                }
                catch (SQLException sqlException) {
                    fdfLog.warn("Error occurred checking or creating a table:::");
                    fdfLog.warn("SQL error \nCode: {},\nState: {}\nMessage" + ": {}\n",
                            sqlException.getErrorCode(), sqlException.getSQLState(), sqlException.getMessage());
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                finally {
                    if (rs != null) {
                        rs.close();
                    }
                    if (stmt != null) {
                        stmt.close();
                    }
                    if(conn != null) {
                        MySqlConnection.getInstance().close4dfDbSession(conn);
                    }
                }
            }
        }
    }

    public void checkFields() throws SQLException {
        //Create tables for model
        for(Class c: FdfSettings.getInstance().modelClasses) {
            //Check if @FdfIgonre class
            if(!c.isAnnotationPresent(FdfIgnore.class)) {
                //Remove @FdfIgnore fields
                List<Field> fields = Arrays.stream(c.getFields()).filter(field -> !field.isAnnotationPresent(FdfIgnore.class)).collect(Collectors.toList());
                //If any fields are left
                if(fields.size() > 0) {

                    Connection conn = null;
                    Statement stmt = null;
                    ResultSet rs = null;
                    try {
                        conn = MySqlConnection.getInstance().get4dfDbConnection();
                        stmt = conn.createStatement();
                        for(Field field : fields) {
                            //Check if field exists in table
                            String fieldTest = "SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '"
                                    + FdfSettings.DB_NAME + "' AND TABLE_NAME = '" + c.getSimpleName().toLowerCase()
                                    + "' AND COLUMN_NAME= '" + field.getName() + "';";
                            if(stmt != null && (rs = stmt.executeQuery(fieldTest)) != null && !rs.next()) {
                                //Field did not exist
                                fdfLog.info("creating field: {} in table: {}", field.getName(),
                                        c.getSimpleName().toLowerCase());
                                String alterSql = "ALTER TABLE " + FdfSettings.DB_NAME + "." + c.getSimpleName().toLowerCase()
                                        + " ADD COLUMN " + getFieldNameAndDataType(field) + ";";
                                fdfLog.debug("Add field sql {} : {}", c.getSimpleName().toLowerCase(), alterSql);
                                stmt.executeUpdate(alterSql);
                            }
                        }
                    }
                    catch(SQLException sqlException) {
                        fdfLog.warn("Error occurred checking or creating a field:::");
                        fdfLog.warn("SQL error \nCode: {},\nState: {}\nMessage" + ": {}\n",
                                sqlException.getErrorCode(), sqlException.getSQLState(), sqlException.getMessage());
                    }
                    catch(Exception ex) {
                        ex.printStackTrace();
                    }
                    finally {
                        if(rs != null) {
                            rs.close();
                        }
                        if(stmt != null) {
                            stmt.close();
                        }
                        if(conn != null) {
                            MySqlConnection.getInstance().close4dfDbSession(conn);
                        }
                    }
                }
            }
        }
    }

    public void checkDefaultEntries() throws SQLException {
        /*FdfSystem Checks*/
        FdfSystemServices ss = new FdfSystemServices();

        //Check if the default system exists
        FdfSystem defaultSystem = ss.getDefaultSystem();
        if(defaultSystem == null) {
            //Create the Default FdfSystem
            FdfSystem newDefaultSystem = new FdfSystem();
            newDefaultSystem.name = FdfSettings.DEFAULT_SYSTEM_NAME;
            newDefaultSystem.description = FdfSettings.DEFAULT_SYSTEM_DESCRIPTION;
            newDefaultSystem.sha256EncodedPassword = ss.hashPassword(FdfSettings.DEFAULT_SYSTEM_PASSWORD);
            newDefaultSystem.euid = 0;
            newDefaultSystem.esid = 0;
            ss.saveSystem(newDefaultSystem);
            fdfLog.info("Created default system.");
        }

        //Check if the test system exists
        FdfSystem testSystem = ss.getTestSystem();
        if(testSystem == null) {
            //Create the Test FdfSystem
            FdfSystem newTestSystem = new FdfSystem();
            newTestSystem.name = FdfSettings.TEST_SYSTEM_NAME;
            newTestSystem.description = FdfSettings.TEST_SYSTEM_DESCRIPTION;
            newTestSystem.sha256EncodedPassword = ss.hashPassword(FdfSettings.TEST_SYSTEM_PASSWORD);
            newTestSystem.euid = 0;
            newTestSystem.esid = 0;
            ss.saveSystem(newTestSystem);
            fdfLog.info("Created test system.");
        }

        /*FdfTenant Checks*/
        FdfTenantServices ts = new FdfTenantServices();

        //Check if default tenant exists
        FdfTenant defaultTenant = ts.getDefaultTenant();
        if(defaultTenant == null) {
            //Create the Default FdfTenant
            FdfTenant defaultTenantState = new FdfTenant();
            defaultSystem = ss.getDefaultSystem();
            defaultTenantState.name = FdfSettings.DEFAULT_TENANT_NAME;
            defaultTenantState.description = FdfSettings.DEFAULT_TENANT_DESRIPTION;
            defaultTenantState.isPrimary = FdfSettings.DEFAULT_TENANT_IS_PRIMARY;
            defaultTenantState.webURL = FdfSettings.DEFAULT_TENANT_WEBSITE;
            defaultTenantState.euid = 1;
            defaultTenantState.esid = defaultSystem.id;
            ts.saveTenant(defaultTenantState);
            fdfLog.info("Created default tenant.");
        }
    }

    public <S> void update(Class<S> c, S state) {
        //Check if @FdfIgonre class
        if(!c.isAnnotationPresent(FdfIgnore.class)) {
            Connection conn = null;
            PreparedStatement preparedStmt = null;
            try {
                //Remove @FdfIgnore Fields
                List<Field> fields = Arrays.stream(c.getFields()).filter(field -> !field.isAnnotationPresent(FdfIgnore.class)
                        && !field.getName().equals("rid")).collect(Collectors.toList());
                //Start Sql Statement
                StringBuilder sql = new StringBuilder("UPDATE ").append(FdfSettings.DB_NAME).append(".")
                        .append(c.getSimpleName().toLowerCase()).append(" SET");
                fields.forEach(field -> sql.append(" ").append(field.getName()).append(" = ?,"));
                sql.deleteCharAt(sql.length() - 1).append(" WHERE rid = ").append(c.getField("rid").get(state)).append(";");
                //Create Connection
                conn = MySqlConnection.getInstance().get4dfDbConnection();
                preparedStmt = conn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);

                int fieldCounter3 = 1;
                for(Field field : fields) {
                    try {
                        if(field.getType() == String.class) {
                            if(field.get(state) != null) {
                                preparedStmt.setString(fieldCounter3, field.get(state).toString());
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                            }
                        } else if(field.getType() == int.class || field.getType() == Integer.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setInt(fieldCounter3, (int) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.INTEGER);
                            }
                        } else if (field.getType() == Long.class || field.getType() == long.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setLong(fieldCounter3, (long) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.BIGINT);
                            }
                        } else if (field.getType() == Double.class || field.getType() == double.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setDouble(fieldCounter3, (double) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.DOUBLE);
                            }
                        } else if (field.getType() == Float.class || field.getType() == float.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setFloat(fieldCounter3, (float) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.FLOAT);
                            }
                        } else if (field.getType() == BigDecimal.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setBigDecimal(fieldCounter3, (BigDecimal) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.NUMERIC);
                            }
                        } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setBoolean(fieldCounter3, (boolean) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.BOOLEAN);
                            }
                        } else if (field.getType() == char.class || field.getType() == Character.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setString(fieldCounter3, field.get(state).toString().substring(0, 1));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.CHAR);
                            }
                        } else if (field.getType() == Date.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setTimestamp(fieldCounter3, new java.sql.Timestamp(((Date) field.get(state)).getTime()));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.DATE);
                            }
                        } else if (field.getType() == UUID.class) {
                            if(field.get(state) != null) {
                                preparedStmt.setString(fieldCounter3, field.get(state).toString());
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                            }
                        } else if (field.getType() != null && field.getType().isEnum()) {
                            if(field.get(state) != null) {
                                preparedStmt.setString(fieldCounter3, field.get(state).toString());
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                            }
                        } else if (Class.class.isAssignableFrom(field.getType())) {
                            if(field.get(state) != null) {
                                String className = field.get(state).toString();
                                preparedStmt.setString(fieldCounter3, FdfUtil.getClassName(className));
                            }
                            else {
                                preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                            }
                        } else if (field.getGenericType() instanceof ParameterizedType && field.get(state) != null) {
                            ParameterizedType pt = (ParameterizedType) field.getGenericType();
                            if(pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0].toString()
                                    .matches(".*?((L|l)ong|Integer|int|(D|d)ouble|(F|f)loat|(B|b)oolean|String).*")) {
                                preparedStmt.setString(fieldCounter3, field.get(state).toString());
                            } else try {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ObjectOutputStream oos = new ObjectOutputStream(baos);
                                oos.writeObject(field.get(state));
                                oos.close();
                                preparedStmt.setString(fieldCounter3,
                                        Base64.getEncoder().encodeToString(baos.toByteArray()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (field.get(state) != null) {
                            try {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ObjectOutputStream oos = new ObjectOutputStream(baos);
                                oos.writeObject(field.get(state));
                                oos.close();
                                preparedStmt.setString(fieldCounter3,
                                        Base64.getEncoder().encodeToString(baos.toByteArray()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            preparedStmt.setNull(fieldCounter3, Types.BLOB);
                        }
                        fieldCounter3++;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                fdfLog.debug("update sql : {}", preparedStmt);
                preparedStmt.execute();
            } catch (SQLException | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            } finally {
                if (preparedStmt != null) try {
                    preparedStmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if(conn != null) {
                    try {
                        MySqlConnection.getInstance().close4dfDbSession(conn);
                    }
                    catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public <S> Long insert(Class<S> c, S state) {
        long newId = -1L;
        //Check if @FdfIgonre class
        if(!c.isAnnotationPresent(FdfIgnore.class)) {
            Connection conn = null;
            PreparedStatement preparedStmt = null;
            ResultSet rs = null;
            try {
                //Remove @FdfIgnore Fields
                List<Field> fields = Arrays.stream(c.getFields()).filter(field -> !field.isAnnotationPresent(FdfIgnore.class)
                        && !field.getName().equals("rid")).collect(Collectors.toList());
                //Start Sql Statement
                StringBuilder sql = new StringBuilder("INSERT INTO ").append(FdfSettings.DB_NAME).append(".")
                        .append(c.getSimpleName().toLowerCase()).append(" ("),
                        val = new StringBuilder();
                fields.forEach(field -> {
                    sql.append(" ").append(field.getName()).append(",");
                    val.append(" ?,");
                });
                sql.deleteCharAt(sql.length()-1).append(") VALUES (")
                        .append(val.deleteCharAt(val.length()-1).toString()).append(");");
                //Create Connection
                conn = MySqlConnection.getInstance().get4dfDbConnection();
                preparedStmt = conn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
                int fieldCounter3 = 1;
                for (Field field : fields) {
                    try {
                        if (field.getType() == String.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setString(fieldCounter3, field.get(state).toString());
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                            }
                        } else if (field.getType() == int.class || field.getType() == Integer.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setInt(fieldCounter3, (int) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.INTEGER);
                            }
                        } else if (field.getType() == Long.class || field.getType() == long.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setLong(fieldCounter3, (long) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.BIGINT);
                            }
                        } else if (field.getType() == Double.class || field.getType() == double.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setDouble(fieldCounter3, (double) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.DOUBLE);
                            }
                        } else if (field.getType() == Float.class || field.getType() == float.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setFloat(fieldCounter3, (float) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.FLOAT);
                            }
                        } else if (field.getType() == BigDecimal.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setBigDecimal(fieldCounter3, (BigDecimal) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.NUMERIC);
                            }
                        } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setBoolean(fieldCounter3, (boolean) field.get(state));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.TINYINT);
                            }
                        } else if (field.getType() == char.class || field.getType() == Character.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setString(fieldCounter3, field.get(state).toString().substring(0, 1));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.CHAR);
                            }
                        } else if (field.getType() == UUID.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setString(fieldCounter3, field.get(state).toString());
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                            }
                        } else if (field.getType() == Date.class) {
                            if (field.get(state) != null) {
                                preparedStmt.setTimestamp(fieldCounter3, new java.sql.Timestamp(((Date) field.get(state)).getTime()));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.DATE);
                            }
                        } else if (field.getType() != null && field.getType().isEnum()) {
                            if (field.get(state) != null) {
                                preparedStmt.setString(fieldCounter3, field.get(state).toString());
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                            }
                        } else if (Class.class.isAssignableFrom(field.getType())) {
                            if (field.get(state) != null) {
                                preparedStmt.setString(fieldCounter3, FdfUtil.getClassName(field.get(state).toString()));
                            } else {
                                preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                            }
                        } else if (field.getGenericType() instanceof ParameterizedType && field.get(state) != null) {
                            ParameterizedType pt = (ParameterizedType) field.getGenericType();
                            if(pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0].toString()
                                    .matches(".*?((L|l)ong|Integer|int|(D|d)ouble|(F|f)loat|(B|b)oolean|String).*")) {
                                preparedStmt.setString(fieldCounter3, field.get(state).toString());
                            } else try {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ObjectOutputStream oos = new ObjectOutputStream(baos);
                                oos.writeObject(field.get(state));
                                oos.close();
                                preparedStmt.setString(fieldCounter3,
                                        Base64.getEncoder().encodeToString(baos.toByteArray()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if(field.get(state) != null) {
                            try {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ObjectOutputStream oos = new ObjectOutputStream(baos);
                                oos.writeObject(field.get(state));
                                oos.close();
                                preparedStmt.setString(fieldCounter3,
                                        Base64.getEncoder().encodeToString(baos.toByteArray()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            preparedStmt.setNull(fieldCounter3, Types.BLOB);
                        }
                        fieldCounter3++;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                fdfLog.debug("insert sql : {}", preparedStmt);
                preparedStmt.execute();
                rs = preparedStmt.getGeneratedKeys();
                if(rs != null && rs.next()) {
                    newId = rs.getLong(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (rs != null) try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (preparedStmt != null) try {
                    preparedStmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if(conn != null) {
                    try {
                        MySqlConnection.getInstance().close4dfDbSession(conn);
                    }
                    catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return newId;
    }


    /**
     * General select Query to retrieve all information for passed entity, can be used to return specified
     * data from any table.  It looks to the class for datatype information and matches each table field returned
     * to the EntityState object by name.  If specific select statements are made they only the corresponding object
     * members will return with data.  If the select parameter is null, all memebers will be returned.
     *
     * Table to query is determined by passing in the corresponding model class. (ex. MyObjectModel.class)
     * Where clauses are passed as an List of Where objects which contain the key (or name), the value to check
     * against, and the type of Conditional (applied between clauses if there is more then one, AND is the default).
     *
     * Example sql statement that would be generated for the following class: User.class
     * and where: {[firstName, Larry], [lastName, Smith, AND]} would be:
     *      SELECT * FROM User where firstName = 'Larry' AND lastName = 'Smith';
     *
     * @param c Class of entity to select from
     * @param sqlStatement Class that contains all the necessary fields to build the sql statement
     * @param <S> Type extending CommonState to query and return
     * @return data queried
     */
    public <S extends CommonState> List<S> selectQuery(Class c, SqlStatement sqlStatement) {
        List<S> everything = new ArrayList<>();
        //Check if class is @FdfIgonre
        if(!c.isAnnotationPresent(FdfIgnore.class)) {
            //Start the sql statement
            String sql = sqlStatement.getSelect() + " FROM " + FdfSettings.DB_NAME + "." + c.getSimpleName().toLowerCase()
                    + sqlStatement.getWhere() + sqlStatement.getGroupBy()
                    + sqlStatement.getOrderBy() + sqlStatement.getLimit() + ";";

            fdfLog.debug("select sql: {}", sql);

            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                conn = MySqlConnection.getInstance().get4dfDbConnection();
                ps = conn.prepareStatement(sql);
                if(ps != null) {
                    rs = ps.executeQuery();
                    while(rs.next()) {
                        // create a new object of type passed
                        Object thisObject = c.newInstance();

                        for (Field field : c.getFields()) {
                            // check the datatype of the field to apply the correct method to
                            // retrieve the data on the resultset

                            // check to see if the class has an @fdfIgonre
                            if(!field.isAnnotationPresent(FdfIgnore.class)) {
                                try {
                                    if (field.getType() == String.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, rs.getString(field.getName()));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getType() == int.class || field.getType() == Integer.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, rs.getInt(field.getName()));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getType() == long.class || field.getType() == Long.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, rs.getLong(field.getName()));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getType() == double.class || field.getType() == Double.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, rs.getDouble(field.getName()));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getType() == float.class || field.getType() == Float.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, rs.getFloat(field.getName()));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getType() == int.class || field.getType() == Integer.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, rs.getInt(field.getName()));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    }
                                    else if (field.getType() == BigDecimal.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, rs.getBigDecimal(field.getName()));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    }
                                    else if (field.getType() == char.class || field.getType() == Character.class) {
                                        try {
                                            field.setAccessible(true);
                                            if (field.getName() != null && rs.getString(field.getName()) != null && rs.getString(field.getName()).length() > 0) {
                                                field.set(thisObject, rs.getString(field.getName()).charAt(0));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getType() == Date.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, rs.getTimestamp(field.getName()));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        } catch (NullPointerException npe) {
                                            // Nullpointer in timestap
                                            fdfLog.debug("NullPointer on Date column {}, This is usually because select"
                                                    + "statement did not include column", field.getName(), npe.getMessage());
                                        }
                                    } else if (field.getType() == UUID.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, UUID.fromString(rs.getString(field.getName())));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getType() == boolean.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, rs.getBoolean(field.getName()));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getType() == Boolean.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, rs.getBoolean(field.getName()));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getType() instanceof Class && ((Class<?>) field.getType()).isEnum()
                                            && field.getName() != null && rs.getString(field.getName()) != null) {
                                        try {
                                            field.setAccessible(true);
                                            field.set(thisObject, Enum.valueOf((Class<Enum>) field.getType(),
                                                    rs.getString(field.getName())));
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (Class.class.isAssignableFrom(field.getType())
                                            && field.getName() != null && rs.getString(field.getName()) != null) {
                                        try {
                                            field.setAccessible(true);
                                            field.set(thisObject,
                                                    FdfUtil.getClassByFullyQualifiedName(rs.getString(field.getName())));
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getGenericType() instanceof ParameterizedType
                                            && field.getName() != null && rs.getString(field.getName()) != null) {
                                        ParameterizedType pt = (ParameterizedType) field.getGenericType();

                                        if (pt.getActualTypeArguments().length == 1 &&
                                                (pt.getActualTypeArguments()[0].toString().contains("Long")
                                                        || pt.getActualTypeArguments()[0].toString().contains("long"))) {

                                            List<Long> list = new ArrayList<>();

                                            String[] strArr = rs.getString(field.getName()).substring(1,
                                                    rs.getString(field.getName()).length() - 1).split(",");

                                            for (String str : strArr) {
                                                list.add(Long.parseLong(str.replaceAll("\\s", "")));
                                            }

                                            field.set(thisObject, list);
                                        } else if (pt.getActualTypeArguments().length == 1 &&
                                                (pt.getActualTypeArguments()[0].toString().contains("Integer")
                                                        || pt.getActualTypeArguments()[0].toString().contains("int"))) {

                                            List<Integer> list = new ArrayList<>();

                                            String[] strArr = rs.getString(field.getName()).substring(1,
                                                    rs.getString(field.getName()).length() - 1).split(",");

                                            for (String str : strArr) {
                                                list.add(Integer.parseInt(str.replaceAll("\\s", "")));
                                            }

                                            field.set(thisObject, list);
                                        } else if (pt.getActualTypeArguments().length == 1 &&
                                                (pt.getActualTypeArguments()[0].toString().contains("Float")
                                                        || pt.getActualTypeArguments()[0].toString().contains("float"))) {

                                            List<Float> list = new ArrayList<>();

                                            String[] strArr = rs.getString(field.getName()).substring(1,
                                                    rs.getString(field.getName()).length() - 1).split(",");

                                            for (String str : strArr) {
                                                list.add(Float.parseFloat(str.replaceAll("\\s", "")));
                                            }

                                            field.set(thisObject, list);
                                        } else if (pt.getActualTypeArguments().length == 1 &&
                                                (pt.getActualTypeArguments()[0].toString().contains("Double")
                                                        || pt.getActualTypeArguments()[0].toString().contains("double"))) {

                                            List<Double> list = new ArrayList<>();

                                            String[] strArr = rs.getString(field.getName()).substring(1,
                                                    rs.getString(field.getName()).length() - 1).split(",");

                                            for (String str : strArr) {
                                                list.add(Double.parseDouble(str.replaceAll("\\s", "")));
                                            }

                                            field.set(thisObject, list);
                                        }
                                        else if (pt.getActualTypeArguments().length == 1 &&
                                                (pt.getActualTypeArguments()[0].toString().contains("BigDecimal"))) {

                                            List<Double> list = new ArrayList<>();

                                            String[] strArr = rs.getString(field.getName()).substring(1,
                                                    rs.getString(field.getName()).length() - 1).split(",");

                                            for (String str : strArr) {
                                                list.add(Double.parseDouble(str.replaceAll("\\s", "")));
                                            }

                                            field.set(thisObject, list);
                                        }
                                        else if (pt.getActualTypeArguments().length == 1 &&
                                                (pt.getActualTypeArguments()[0].toString().contains("Boolean")
                                                        || pt.getActualTypeArguments()[0].toString().contains("boolean"))) {

                                            List<Boolean> list = new ArrayList<>();

                                            String[] strArr = rs.getString(field.getName()).substring(1,
                                                    rs.getString(field.getName()).length() - 1).split(",");

                                            for (String str : strArr) {
                                                list.add(Boolean.parseBoolean(str.replaceAll("\\s", "")));
                                            }

                                            field.set(thisObject, list);
                                        } else if (pt.getActualTypeArguments().length == 1 &&
                                                pt.getActualTypeArguments()[0].toString().contains("String")) {

                                            List<String> list = new ArrayList<>();

                                            String[] strArr = rs.getString(field.getName()).substring(1,
                                                    rs.getString(field.getName()).length() - 1).split(",");

                                            for (String str : strArr) {
                                                list.add(str.replaceAll("\\s", ""));
                                            }

                                            field.set(thisObject, list);
                                        } else {
                                            // serialized object, deserialize
                                            try {
                                                field.setAccessible(true);

                                                byte[] data = Base64.getDecoder().decode(rs.getString(field.getName()));
                                                ObjectInputStream ois = new ObjectInputStream(
                                                        new ByteArrayInputStream(data));
                                                Object o = ois.readObject();
                                                ois.close();

                                                field.set(thisObject, o);
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("S0022")) {
                                                    // Invalid column name, thrown if select statement does not include column
                                                    fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                            + "{}, This is usually because select statement did not include column and "
                                                            + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                                } else {
                                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                                }
                                            }
                                        }
                                    } else if (field.getName() != null && rs.getString(field.getName()) != null) {
                                        // serialized object, deserialize
                                        try {
                                            field.setAccessible(true);

                                            byte[] data = Base64.getDecoder().decode(rs.getString(field.getName()));
                                            ObjectInputStream ois = new ObjectInputStream(
                                                    new ByteArrayInputStream(data));
                                            Object o = ois.readObject();
                                            ois.close();

                                            field.set(thisObject, o);
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("S0022")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    }
                                } catch (SQLException e) {
                                    if (e.getSQLState().equals("S0022")) {
                                        // Invalid column name, thrown if select statement does not include column
                                        fdfLog.debug("Select statement had sql state S0022 (Invalid column name) on column"
                                                + "{}, This is usually because select statement did not include column and "
                                                + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                    } else {
                                        fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                    }
                                }
                            }
                        }
                        S thisUserStateTest = (S) thisObject;
                        everything.add(thisUserStateTest);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if(conn != null) {
                    try {
                        MySqlConnection.getInstance().close4dfDbSession(conn);
                    }
                    catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return everything;
    }

    static String getFieldNameAndDataType(Field field) {
        String sql = "";

        fdfLog.debug("checking field: {} of type: {} ", "`" + field.getName() + "`", field.getType());

        if (field.getType() == String.class) {
            sql += "`" + field.getName() + "`" + " TEXT";
        } else if (field.getType() == int.class || field.getType() == Integer.class) {
            sql += "`" + field.getName() + "`" + " INT";
        } else if (field.getType() == Long.class || field.getType() == long.class) {
            sql += "`" + field.getName() + "`" + " BIGINT";
            if (field.getName().equals("rid")) {
                sql += " PRIMARY KEY AUTO_INCREMENT";
            }
        } else if (field.getType() == Double.class || field.getType() == double.class) {
            sql += "`" + field.getName() + "`" + " DOUBLE";
        } else if (field.getType() == Float.class || field.getType() == float.class) {
            sql += "`" + field.getName() + "`" + " FLOAT";
        }
        else if (field.getType() == BigDecimal.class) {
            sql += "`" + field.getName() + "`" + " NUMERIC(10,4)";
        } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
            sql += "`" + field.getName() + "`" + " TINYINT(1)";
        } else if (field.getType() == Date.class) {
            sql += "`" + field.getName() + "`" + " DATETIME";
            if (field.getName().equals("arsd")) {
                sql += " DEFAULT CURRENT_TIMESTAMP";
            } else {
                sql += " NULL";
            }
        } else if (field.getType() == UUID.class) {
            sql += "`" + field.getName() + "`" + " VARCHAR(132)";
        } else if (field.getType() == Character.class || field.getType() == char.class) {
            sql += "`" + field.getName() + "`" + " CHAR";
        } else if (field.getType() != null && field.getType().isEnum()) {
            sql += "`" + field.getName() + "`" + " VARCHAR(200)";
        } else if (Class.class.isAssignableFrom(field.getType())) {
            sql += "`" + field.getName() + "`" + " VARCHAR(200)";
        }
        else if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            if(pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0].toString()
                    .matches(".*?((L|l)ong|Integer|int|(D|d)ouble|(F|f)loat|(B|b)oolean|String).*")) {
                sql += "`" + field.getName() + "`" + " TEXT";
            }
            else {
                // unknown create text fields to serialize
                fdfLog.debug("Was not able to identify field: {} of type: {} ", field.getName(), field.getType());
                sql += "`" + field.getName() + "`" + " BLOB";
            }
        }
        else {
            // unknown create text fields to serialize
            fdfLog.debug("Was not able to identify field: {} of type: {} ", field.getName(), field.getType());
            sql += "`" + field.getName() + "`" + " BLOB";
        }
        return sql;
    }

    static String parseWhere(List<WhereClause> where) {
        String sql = "";
        //If where clauses were passed, add them to the sql statement
        if(where != null && where.size() > 0) {
            sql += " WHERE";
            for(WhereClause clause : where) {
                // if there is more then one clause, check the conditional type.
                if(where.indexOf(clause) != 0 && (where.indexOf(clause) +1) <= where.size()) {
                    sql += " " + clause.conditional.name();
                    /*if(clause.conditional == WhereClause.CONDITIONALS.AND) {
                        sql += " AND";
                    }
                    else if (clause.conditional == WhereClause.CONDITIONALS.OR) {
                        sql += " OR";
                    }
                    else if (clause.conditional == WhereClause.CONDITIONALS.NOT) {
                        sql += " NOT";
                    }*/
                }

                // check to see if there are any open parenthesis to apply
                if(clause.groupings != null && clause.groupings.size() > 0) {
                    for(WhereClause.GROUPINGS grouping: clause.groupings) {
                        if(grouping == WhereClause.GROUPINGS.OPEN_PARENTHESIS) {
                            sql += " (";
                        }
                    }
                }

                // add the claus formatting the sql for the correct datatype
                if(clause.operator != WhereClause.Operators.UNARY) {
                    sql += " " + clause.name + " " + clause.getOperatorString() + " ";
                    if(clause.value.equals(WhereClause.NULL)) {
                        sql += clause.value;
                    }
                    else if(clause.valueDataType == int.class || clause.valueDataType == Integer.class ||
                            clause.valueDataType == long.class || clause.valueDataType == Long.class ||
                            clause.valueDataType == double.class || clause.valueDataType == Double.class ||
                            clause.valueDataType == float.class || clause.valueDataType == Float.class ||
                            clause.valueDataType == BigDecimal.class) {
                        sql += clause.value;
                    }
                    else if (clause.valueDataType == boolean.class || clause.valueDataType == Boolean.class) {
                        sql += clause.value.toLowerCase();
                    }
                    else {
                        sql += "'" + clause.value + "'";
                    }
                }

                // check to see if there are any closing parenthesis to apply
                if(clause.groupings != null && clause.groupings.size() > 0) {
                    for(WhereClause.GROUPINGS grouping: clause.groupings) {
                        if(grouping == WhereClause.GROUPINGS.CLOSE_PARENTHESIS) {
                            sql += " )";
                        }
                    }
                }
            }
        }
        return sql;
    }
}