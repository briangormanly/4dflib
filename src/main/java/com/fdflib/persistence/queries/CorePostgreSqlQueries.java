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
import com.fdflib.persistence.database.PostgreSqlConnection;
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

/**
 * Created by brian.gormanly on 1/14/16.
 */
public class CorePostgreSqlQueries implements CorePersistenceImpl {

    private static final CorePostgreSqlQueries INSTANCE = new CorePostgreSqlQueries();
    static org.slf4j.Logger fdfLog = LoggerFactory.getLogger(CorePostgreSqlQueries.class);

    private CorePostgreSqlQueries() {}

    public static CorePostgreSqlQueries getInstance() {
        return INSTANCE;
    }

    public void checkDatabase() throws SQLException {
        // build database
        String dbexists = "SELECT * FROM pg_database WHERE datname= '" + FdfSettings.DB_NAME.toLowerCase() + "';";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = PostgreSqlConnection.getInstance().get4dfDbRootConnection();
            stmt = conn.createStatement();

            if (stmt != null) {
                rs = stmt.executeQuery(dbexists);
            }

            if(rs != null) {
                if(!rs.next()) {
                    // Database does not exist, build
                    String sqlCreate = "CREATE DATABASE " +  "\"" + FdfSettings.DB_NAME.toLowerCase() + "\""
                            +  " ENCODING '" + FdfSettings.DB_ENCODING.UTF8 + "';";

                    String sqlCreateUser = "CREATE USER " +  "\"" +  FdfSettings.DB_USER.toLowerCase() + "\""
                            +  " WITH PASSWORD '" + FdfSettings.DB_PASSWORD.toLowerCase() + "';";
                    String sqlUserGrant = "GRANT ALL PRIVILEGES ON DATABASE " + "\"" + FdfSettings.DB_NAME.toLowerCase()
                            + "\"" + " to " + "\"" +  FdfSettings.DB_USER.toLowerCase() + "\"" +  ";";

                    if(stmt != null) {
                        stmt.executeUpdate(sqlCreate);
                        stmt.executeUpdate(sqlCreateUser);
                        stmt.executeUpdate(sqlUserGrant);
                        fdfLog.info("******************************************************************");
                        fdfLog.info("4DFLib Database did not exist, attempting to build.");
                        fdfLog.info("******************************************************************");

                        fdfLog.info("Database created.");
                        fdfLog.info("******************************************************************");
                    }
                }
            }

        }
        catch (SQLException sqlException) {

            // some other error
            fdfLog.warn("Error occurred checking or creating database:::");
            fdfLog.warn("SQL error \nCode: {},\nState: {}\nMessage" +
                    ": {}\n", sqlException.getErrorCode(), sqlException.getSQLState(), sqlException.getMessage());

        }
        finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if(conn != null) {
                PostgreSqlConnection.getInstance().close4dfDbSession(conn);
            }
        }

    }

    public void checkTables() throws SQLException {
        // get the 4df data model
        List<Class> classList = FdfSettings.getInstance().modelClasses;

        // build the tables for the model objects
        for(Class c: classList) {

            // check to see if the class has an @fdfIgonre
            if(!c.isAnnotationPresent(FdfIgnore.class)) {

                // determine the number of fields
                int numberOfFields = 0;
                for (Field field : c.getFields()) {

                    // check to see if the class has an @fdfIgonre
                    if (!field.isAnnotationPresent(FdfIgnore.class)) {
                        numberOfFields++;
                    }
                }

                // check to see if the table already exists
                String tableTest = "select * from information_schema.TABLES where table_catalog = '"
                        + FdfSettings.DB_NAME.toLowerCase() + "' and table_name = '"
                        + c.getSimpleName().toLowerCase() + "';";

                Connection conn = null;
                Statement stmt = null;
                ResultSet rs = null;



                try {
                    conn = PostgreSqlConnection.getInstance().get4dfDbConnection();
                    stmt = conn.createStatement();

                    if (stmt != null) {
                        rs = stmt.executeQuery(tableTest);
                    }

                    if (rs != null) {
                        if (!rs.next()) {
                            // Table does not exist, build
                            fdfLog.info("creating table: {}", c.getSimpleName().toLowerCase());
                            // check there there is at lease one field
                            if (c.getFields().length > 0) {
                                String sql = "CREATE TABLE " + "\"" + c.getSimpleName().toLowerCase()
                                        + "\"" + " ( ";
                                int fieldCounter = 0;

                                for (Field field : c.getFields()) {

                                    // check to see if the class has an @fdfIgonre
                                    if(!field.isAnnotationPresent(FdfIgnore.class)) {

                                        sql += getFieldNameAndDataType(field);

                                        fieldCounter++;

                                        if (numberOfFields > fieldCounter) sql += ", ";
                                    }

                                }
                                sql += ");";

                                fdfLog.debug("Table sql {} : {}", c.getSimpleName().toLowerCase(), sql);

                                if (stmt != null) {
                                    stmt.executeUpdate(sql);
                                }
                            } else {
                                fdfLog.info("No table created for model object {} class had no valid data members", c.getSimpleName().toLowerCase());
                            }
                        }
                    }
                } catch (SQLException sqlException) {

                    // some other error
                    fdfLog.warn("Error occurred checking or creating a table:::");
                    fdfLog.warn("SQL error \nCode: {},\nState: {}\nMessage" +
                                    ": {}\n", sqlException.getErrorCode(), sqlException.getSQLState(),
                            sqlException.getMessage());

                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (rs != null) {
                        rs.close();
                    }
                    if (stmt != null) {
                        stmt.close();
                    }
                    if(conn != null) {
                        PostgreSqlConnection.getInstance().close4dfDbSession(conn);
                    }
                }
            }

        }
    }

    public void checkFields() throws SQLException {
        // get the 4df data model
        List<Class> classList = FdfSettings.getInstance().modelClasses;

        // build the tables for the model objects
        for(Class c: classList) {

            // determine the number of fields
            int numberOfFields = 0;
            for (Field field : c.getFields()) {

                // check to see if the class has an @fdfIgonre
                if (!field.isAnnotationPresent(FdfIgnore.class)) {
                    numberOfFields++;
                }
            }

            if(numberOfFields > 0) {

                // check to see if the class has an @fdfIgonre
                if(!c.isAnnotationPresent(FdfIgnore.class)) {

                    Connection conn = null;
                    Statement stmt = null;
                    ResultSet rs = null;

                    try {
                        conn = PostgreSqlConnection.getInstance().get4dfDbConnection();
                        stmt = conn.createStatement();

                        for (Field field : c.getFields()) {

                            // check to see if the class has an @fdfIgonre
                            if(!field.isAnnotationPresent(FdfIgnore.class)) {

                                // query for the field in the database
                                // check to see if the table already exists
                                String fieldTest = "select * from information_schema.columns where table_catalog= '"
                                        + FdfSettings.DB_NAME.toLowerCase() + "' and table_name= '"
                                        + c.getSimpleName().toLowerCase() + "' and column_name= '"
                                        + field.getName().toLowerCase() + "';";
                                fdfLog.debug("Checking existence of field {} with SQL: {}", c.getSimpleName().toLowerCase(), fieldTest);

                                if (stmt != null) {
                                    rs = stmt.executeQuery(fieldTest);
                                }

                                if (rs != null) {
                                    if (!rs.next()) {
                                        // the field did not exist,
                                        String alterSql = "alter table " + "\"" + c.getSimpleName().toLowerCase() + "\"" + " add column "
                                                + this.getFieldNameAndDataType(field) + ";";

                                        fdfLog.info("Add field sql {} : {}", c.getSimpleName().toLowerCase(), alterSql);

                                        if (stmt != null) {
                                            stmt.executeUpdate(alterSql);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (SQLException sqlException) {

                        // some other error
                        fdfLog.warn("Error occurred checking or creating a field:::");
                        fdfLog.warn("SQL error \nCode: {},\nState: {}\nMessage" +
                                        ": {}\n", sqlException.getErrorCode(), sqlException.getSQLState(),
                                sqlException.getMessage());

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        if (rs != null) {
                            rs.close();
                        }
                        if (stmt != null) {
                            stmt.close();
                        }
                        if(conn != null) {
                            PostgreSqlConnection.getInstance().close4dfDbSession(conn);
                        }
                    }
                }
            }
        }
    }

    public void checkDefaultEntries() throws SQLException {
        // check to see if the default entry exists for FdfSystem
        FdfSystemServices ss = new FdfSystemServices();
        FdfSystem defaultSystem = ss.getDefaultSystem();
        if(defaultSystem == null) {
            // build the default FdfSystem entry
            FdfSystem newDefaultSystem = new FdfSystem();
            newDefaultSystem.name = FdfSettings.DEFAULT_SYSTEM_NAME;
            newDefaultSystem.description = FdfSettings.DEFAULT_SYSTEM_DESCRIPTION;

            newDefaultSystem.sha256EncodedPassword = ss.hashPassword(FdfSettings.DEFAULT_SYSTEM_PASSWORD);
            newDefaultSystem.euid = 0;
            newDefaultSystem.esid = 0;
            ss.save(FdfSystem.class, newDefaultSystem);
            fdfLog.info("Created default system.");
        }

        // check to see if the test system entry exists
        FdfSystem testSystem = ss.getTestSystem();
        if(testSystem == null) {
            // build the default FdfSystem entry
            FdfSystem newTestSystem = new FdfSystem();
            newTestSystem.name = FdfSettings.TEST_SYSTEM_NAME;
            newTestSystem.description = FdfSettings.TEST_SYSTEM_DESCRIPTION;

            newTestSystem.sha256EncodedPassword = ss.hashPassword(FdfSettings.TEST_SYSTEM_PASSWORD);
            newTestSystem.euid = 0;
            newTestSystem.esid = 0;
            ss.save(FdfSystem.class, newTestSystem);
            fdfLog.info("Created test system.");
        }

        // check to see if the default Tenant entry exists
        FdfTenantServices ts = new FdfTenantServices();
        FdfTenant defaultTenant = ts.getDefaultTenant();

        if(defaultTenant == null) {
            // build the default FdfTenant
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

        // check to see if the class has an @fdfIgonre
        if(!c.isAnnotationPresent(FdfIgnore.class)) {

            // determine the number of fields
            int numberOfFields = 0;
            for (Field field : c.getFields()) {

                // check to see if the class has an @fdfIgonre
                if (!field.isAnnotationPresent(FdfIgnore.class)) {
                    numberOfFields++;
                }
            }

            // Start the sql statement
            String sql = "update " + "\"" + c.getSimpleName().toLowerCase() + "\"" + " set";

            int fieldCounter = 0;

            for (Field field : c.getFields()) {

                // check to see if the class has an @fdfIgonre
                if(!field.isAnnotationPresent(FdfIgnore.class)) {

                    fieldCounter++;
                    if (!field.getName().equals("rid")) {
                        sql += " " + field.getName() + " = ?";
                        if (numberOfFields > fieldCounter) sql += ",";
                    }
                }
            }

            Connection conn = null;
            PreparedStatement preparedStmt = null;
            ResultSet rs = null;

            try {

                sql += " where rid = " + c.getField("rid").get(state) + " ;";

                conn = PostgreSqlConnection.getInstance().get4dfDbConnection();
                preparedStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

                int fieldCounter3 = 1;
                for (Field field : c.getFields()) {

                    // check to see if the class has an @fdfIgonre
                    if(!field.isAnnotationPresent(FdfIgnore.class)) {

                        try {
                            if (field.getType() == String.class) {
                                if (field.get(state) != null) {
                                    preparedStmt.setString(fieldCounter3, field.get(state).toString());
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                                }
                            } else if (field.getType() == int.class || field.getType() == Integer.class) {
                                if (!field.getName().equals("rid") && field.get(state) != null) {
                                    if (field.get(state) != null) {
                                        preparedStmt.setInt(fieldCounter3, (int) field.get(state));
                                    } else {
                                        preparedStmt.setNull(fieldCounter3, Types.INTEGER);
                                    }
                                }
                            } else if (field.getType() == Long.class || field.getType() == long.class) {
                                if (!field.getName().equals("rid")) {
                                    if (field.get(state) != null) {
                                        preparedStmt.setLong(fieldCounter3, (long) field.get(state));
                                    } else {
                                        preparedStmt.setNull(fieldCounter3, Types.BIGINT);
                                    }
                                }
                            } else if (field.getType() == Double.class || field.getType() == double.class) {
                                if (!field.getName().equals("rid")) {
                                    if (field.get(state) != null) {
                                        preparedStmt.setDouble(fieldCounter3, (double) field.get(state));
                                    } else {
                                        preparedStmt.setNull(fieldCounter3, Types.DOUBLE);
                                    }
                                }
                            } else if (field.getType() == Float.class || field.getType() == float.class) {
                                if (!field.getName().equals("rid")) {
                                    if (field.get(state) != null) {
                                        preparedStmt.setFloat(fieldCounter3, (float) field.get(state));
                                    } else {
                                        preparedStmt.setNull(fieldCounter3, Types.FLOAT);
                                    }
                                }
                            }
                            else if (field.getType() == BigDecimal.class) {
                                if (!field.getName().equals("rid")) {
                                    if (field.get(state) != null) {
                                        preparedStmt.setBigDecimal(fieldCounter3, (BigDecimal) field.get(state));
                                    } else {
                                        preparedStmt.setNull(fieldCounter3, Types.NUMERIC);
                                    }
                                }
                            }
                            else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                                if (field.get(state) != null) {
                                    preparedStmt.setBoolean(fieldCounter3, (boolean) field.get(state));
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.BOOLEAN);
                                }

                            } else if (field.getType() == char.class || field.getType() == Character.class) {
                                if (field.get(state) != null && field.get(state).toString() != null
                                        && field.get(state).toString().substring(0, 1) != null) {
                                    Character convert = field.get(state).toString().charAt(0);

                                    // check to see if the character is a null character -- screw you postgres this better work
                                    if (Character.isLetterOrDigit(field.get(state).toString().charAt(0))) {
                                        preparedStmt.setString(fieldCounter3, convert.toString());
                                    } else {
                                        preparedStmt.setNull(fieldCounter3, Types.CHAR);
                                    }
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.CHAR);
                                }
                            } else if (field.getType() == Date.class) {
                                if (field.get(state) != null) {
                                    Date insertDate = (Date) field.get(state);
                                    if (insertDate == null) {
                                        preparedStmt.setTimestamp(fieldCounter3, null);
                                    } else {
                                        preparedStmt.setTimestamp(fieldCounter3, new Timestamp(insertDate.getTime()));
                                    }
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.TIMESTAMP);
                                }
                            } else if (field.getType() == UUID.class) {
                                if (field.get(state) != null) {
                                    preparedStmt.setString(fieldCounter3, field.get(state).toString());
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                                }
                            }
                            else if (field.getType() instanceof Class && ((Class<?>) field.getType()).isEnum()) {
                                if (field.get(state) != null) {
                                    preparedStmt.setString(fieldCounter3, field.get(state).toString());
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                                }
                            } else if (Class.class.isAssignableFrom(field.getType())) {
                                if (field.get(state) != null) {
                                    String className = field.get(state).toString();
                                    preparedStmt.setString(fieldCounter3, FdfUtil.getClassName(className));
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                                }
                            } else if (field.getGenericType() instanceof ParameterizedType && field.get(state) != null) {
                                ParameterizedType pt = (ParameterizedType) field.getGenericType();
                                if (pt.getActualTypeArguments().length == 1
                                        && (pt.getActualTypeArguments()[0].toString().contains("Long")
                                        || pt.getActualTypeArguments()[0].toString().contains("long")
                                        || pt.getActualTypeArguments()[0].toString().contains("Integer")
                                        || pt.getActualTypeArguments()[0].toString().contains("int")
                                        || pt.getActualTypeArguments()[0].toString().contains("Double")
                                        || pt.getActualTypeArguments()[0].toString().contains("double")
                                        || pt.getActualTypeArguments()[0].toString().contains("Float")
                                        || pt.getActualTypeArguments()[0].toString().contains("float")
                                        || pt.getActualTypeArguments()[0].toString().contains("boolean")
                                        || pt.getActualTypeArguments()[0].toString().contains("Boolean")
                                        || pt.getActualTypeArguments()[0].toString().contains("String"))) {

                                    preparedStmt.setString(fieldCounter3, field.get(state).toString());

                                } else {
                                    // try to serialize the object
                                    if (field.get(state) != null) {

                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        ObjectOutputStream oos = null;
                                        try {
                                            oos = new ObjectOutputStream(baos);
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
                                }

                            } else {
                                // try to serialize the object
                                if (field.get(state) != null) {

                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    ObjectOutputStream oos = null;
                                    try {
                                        oos = new ObjectOutputStream(baos);
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
                            }

                            if (!field.getName().equals("rid")) fieldCounter3++;

                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }

                fdfLog.debug("update sql : {}", preparedStmt);

                preparedStmt.execute();


            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (preparedStmt != null) {
                    try {
                        preparedStmt.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (conn != null) {
                        PostgreSqlConnection.getInstance().close4dfDbSession(conn);
                    }
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public <S> Long insert(Class<S> c, S state) {

        // spot to hold returned id for new record
        long newId = -1L;

        // determine the number of fields
        int numberOfFields = 0;
        for (Field field : c.getFields()) {

            // check to see if the class has an @fdfIgonre
            if (!field.isAnnotationPresent(FdfIgnore.class)) {
                numberOfFields++;
            }
        }

        // check to see if the class has an @fdfIgonre
        if(!c.isAnnotationPresent(FdfIgnore.class)) {

            // Start the sql statement
            String sql = "insert into " + "\"" + c.getSimpleName().toLowerCase() + "\"" + " (";

            int fieldCounter = 0;
            for (Field field : c.getFields()) {

                // check to see if the class has an @fdfIgonre
                if(!field.isAnnotationPresent(FdfIgnore.class)) {

                    fieldCounter++;
                    if (!field.getName().equals("rid")) {
                        sql += " " + field.getName();
                        if (numberOfFields > fieldCounter) sql += ",";
                    }
                }
            }

            sql += " ) values (";

            //insert the correct number of question marks for the prepared statement
            int fieldCounter2 = 0;
            for (Field field : c.getFields()) {

                // check to see if the class has an @fdfIgonre
                if(!field.isAnnotationPresent(FdfIgnore.class)) {

                    fieldCounter2++;
                    if (!field.getName().equals("rid")) {
                        sql += " ?";
                        if (numberOfFields > fieldCounter2) sql += ",";
                    }
                }
            }
            sql += ");";

            Connection conn = null;
            PreparedStatement preparedStmt = null;
            ResultSet rs = null;

            try {
                conn = PostgreSqlConnection.getInstance().get4dfDbConnection();
                preparedStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

                int fieldCounter3 = 1;
                for (Field field : c.getFields()) {

                    // check to see if the class has an @fdfIgonre
                    if(!field.isAnnotationPresent(FdfIgnore.class)) {

                        try {
                            if (field.getType() == String.class) {
                                if (field.get(state) != null) {
                                    preparedStmt.setString(fieldCounter3, field.get(state).toString());
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                                }
                            } else if (field.getType() == int.class || field.getType() == Integer.class) {
                                if (field.get(state) != null) {
                                    if (!field.getName().equals("rid")) {
                                        preparedStmt.setInt(fieldCounter3, (int) field.get(state));
                                    }
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.INTEGER);
                                }
                            } else if (field.getType() == Long.class || field.getType() == long.class) {
                                if (field.get(state) != null) {
                                    if (!field.getName().equals("rid")) {
                                        preparedStmt.setLong(fieldCounter3, (long) field.get(state));
                                    }
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.BIGINT);
                                }
                            } else if (field.getType() == Double.class || field.getType() == double.class) {
                                if (field.get(state) != null) {
                                    if (!field.getName().equals("rid")) {
                                        preparedStmt.setDouble(fieldCounter3, (double) field.get(state));
                                    }
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.BIGINT);
                                }
                            } else if (field.getType() == Float.class || field.getType() == float.class) {
                                if (field.get(state) != null) {
                                    if (!field.getName().equals("rid")) {
                                        preparedStmt.setFloat(fieldCounter3, (float) field.get(state));
                                    }
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.BIGINT);
                                }
                            }
                            else if (field.getType() == BigDecimal.class) {
                                if (field.get(state) != null) {
                                    if (!field.getName().equals("rid")) {
                                        preparedStmt.setBigDecimal(fieldCounter3, (BigDecimal) field.get(state));
                                    }
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.NUMERIC);
                                }
                            }
                            else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                                if (field.get(state) != null) {
                                    preparedStmt.setBoolean(fieldCounter3, (boolean) field.get(state));
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.TINYINT);
                                }

                            } else if (field.getType() == char.class || field.getType() == Character.class) {
                                if (field.get(state) != null && field.get(state).toString() != null
                                        && field.get(state).toString().substring(0, 1) != null) {

                                    Character convert = field.get(state).toString().charAt(0);

                                    // check to see if the character is a null character -- screw you postgres this better work
                                    if (Character.isLetterOrDigit(field.get(state).toString().charAt(0))) {
                                        preparedStmt.setString(fieldCounter3, convert.toString());
                                    } else {
                                        preparedStmt.setNull(fieldCounter3, Types.CHAR);
                                    }
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.CHAR);
                                }
                            } else if (field.getType() == Date.class) {
                                Date insertDate = (Date) field.get(state);
                                if (insertDate == null) {
                                    preparedStmt.setTimestamp(fieldCounter3, null);
                                } else {
                                    preparedStmt.setTimestamp(fieldCounter3, new Timestamp(insertDate.getTime()));
                                }

                            } else if (field.getType() == UUID.class) {
                                if (field.get(state) != null) {
                                    preparedStmt.setString(fieldCounter3, field.get(state).toString());
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                                }
                            } else if (field.getType() instanceof Class && ((Class<?>) field.getType()).isEnum()) {
                                if (field.get(state) != null) {
                                    preparedStmt.setString(fieldCounter3, field.get(state).toString());
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                                }
                            } else if (Class.class.isAssignableFrom(field.getType())) {
                                if (field.get(state) != null) {
                                    String className = field.get(state).toString();
                                    preparedStmt.setString(fieldCounter3, FdfUtil.getClassName(className));
                                } else {
                                    preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                                }
                            } else if (field.getGenericType() instanceof ParameterizedType && field.get(state) != null) {
                                ParameterizedType pt = (ParameterizedType) field.getGenericType();
                                if (pt.getActualTypeArguments().length == 1
                                        && (pt.getActualTypeArguments()[0].toString().contains("Long")
                                        || pt.getActualTypeArguments()[0].toString().contains("long")
                                        || pt.getActualTypeArguments()[0].toString().contains("Integer")
                                        || pt.getActualTypeArguments()[0].toString().contains("int")
                                        || pt.getActualTypeArguments()[0].toString().contains("Double")
                                        || pt.getActualTypeArguments()[0].toString().contains("double")
                                        || pt.getActualTypeArguments()[0].toString().contains("Float")
                                        || pt.getActualTypeArguments()[0].toString().contains("float")
                                        || pt.getActualTypeArguments()[0].toString().contains("boolean")
                                        || pt.getActualTypeArguments()[0].toString().contains("Boolean")
                                        || pt.getActualTypeArguments()[0].toString().contains("String"))) {

                                    preparedStmt.setString(fieldCounter3, field.get(state).toString());

                                } else {
                                    // try to serialize the object
                                    if (field.get(state) != null) {

                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        ObjectOutputStream oos = null;
                                        try {
                                            oos = new ObjectOutputStream(baos);
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
                                }
                            } else {
                                // try to serialize the object
                                if (field.get(state) != null) {

                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    ObjectOutputStream oos = null;
                                    try {
                                        oos = new ObjectOutputStream(baos);
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
                            }

                            if (!field.getName().equals("rid")) fieldCounter3++;

                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }

                fdfLog.debug("insert sql : {}", preparedStmt);

                preparedStmt.execute();
                rs = preparedStmt.getGeneratedKeys();
                rs.next();
                newId = rs.getLong("rid");

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (preparedStmt != null) {
                    try {
                        preparedStmt.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (conn != null) {
                        PostgreSqlConnection.getInstance().close4dfDbSession(conn);
                    }
                }
                catch (SQLException e) {
                    e.printStackTrace();
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

        // check to see if the class has an @fdfIgonre
        if(!c.isAnnotationPresent(FdfIgnore.class)) {

            // if no order by was passed use id
            if(sqlStatement.getOrderBy().length() == 0) {
                sqlStatement.orderBy("id");
            }
            // start the sql statement
            String sql = sqlStatement.getSelect() + " FROM \"" + c.getSimpleName().toLowerCase() + "\""
                    + sqlStatement.getWhere() + sqlStatement.getGroupBy() + sqlStatement.getOrderBy()
                    + sqlStatement.getLimit() + ";";

            fdfLog.debug("select sql: {}", sql);

            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                conn = PostgreSqlConnection.getInstance().get4dfDbConnection();
                ps = conn.prepareStatement(sql);

                if (ps != null) {
                    rs = ps.executeQuery();
                    while (rs.next()) {

                        // build a new object of type passed
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
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                                                + "{}, This is usually because select statement did not include column and "
                                                                + "can be ignored. Message is {}", field.getName(),
                                                        e.getMessage());
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
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getType() == BigDecimal.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, rs.getBigDecimal(field.getName()));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    } else if (field.getType() == char.class || field.getType() == Character.class) {
                                        try {
                                            field.setAccessible(true);
                                            if (field.getName() != null && rs.getString(field.getName()) != null && rs.getString(field.getName()).length() > 0) {
                                                field.set(thisObject, rs.getString(field.getName()).charAt(0));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        } catch (NullPointerException npe) {
                                            // Nullpointer in timestap
                                            fdfLog.debug("NullPointer on timestamp column {}, This is usually because select"
                                                    + "statement did not include column", field.getName(), npe.getMessage());
                                        }
                                    } else if (field.getType() == UUID.class) {
                                        try {
                                            if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                field.setAccessible(true);
                                                field.set(thisObject, UUID.fromString(rs.getString(field.getName())));
                                            }
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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
                                                    FdfUtil.getClassByFullyQualifiedName(
                                                            rs.getString(field.getName())));
                                        } catch (SQLException e) {
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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
                                                if (e.getSQLState().equals("42703")) {
                                                    // Invalid column name, thrown if select statement does not include column
                                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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
                                            if (e.getSQLState().equals("42703")) {
                                                // Invalid column name, thrown if select statement does not include column
                                                fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                                        + "{}, This is usually because select statement did not include column and "
                                                        + "can be ignored. Message is {}", field.getName(), e.getMessage());
                                            } else {
                                                fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                                        ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                            }
                                        }
                                    }
                                } catch (SQLException e) {
                                    if (e.getSQLState().equals("42703")) {
                                        // Invalid column name, thrown if select statement does not include column
                                        fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
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

            } catch (SQLException e) {
                e.printStackTrace();
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
                try {
                    if (conn != null) {
                        PostgreSqlConnection.getInstance().close4dfDbSession(conn);
                    }
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return everything;
    }

    static String getFieldNameAndDataType(Field field) {
        String sql = "";

        fdfLog.debug("checking field: {} of type: {} ", field.getName(), field.getType());

        if (field.getType() == String.class) {
            sql += field.getName()+ " TEXT";
        } else if (field.getType() == int.class || field.getType() == Integer.class) {
            sql += field.getName()+ " INT";
        } else if (field.getType() == Long.class || field.getType() == long.class) {

            if (field.getName().equals("rid")) {
                sql += field.getName()+ " BIGSERIAL PRIMARY KEY";
            }
            else {
                sql += field.getName()+ " BIGINT";
            }
        } else if (field.getType() == Double.class || field.getType() == double.class) {
            sql += field.getName()+ " double precision";
        } else if (field.getType() == Float.class || field.getType() == float.class) {
            sql += field.getName()+ " real";

        }
        else if (field.getType() == BigDecimal.class) {
            sql += field.getName() + " NUMERIC(10,4)";
        } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
            sql += field.getName()+ " BOOLEAN";
        } else if (field.getType() == Date.class) {
            sql += field.getName()+ " TIMESTAMP";
            if (field.getName().equals("arsd")) {
                sql += " DEFAULT CURRENT_TIMESTAMP";
            } else {
                sql += " NULL";
            }
        } else if (field.getType() == UUID.class) {
            sql += field.getName() + " VARCHAR(132)";
        } else if (field.getType() == Character.class || field.getType() == char.class) {
            sql += field.getName()+ " CHAR";
        } else if (field.getType() instanceof Class && ((Class<?>) field.getType()).isEnum()) {
            sql += field.getName()+ " VARCHAR(200)";
        } else if (Class.class.isAssignableFrom(field.getType())) {
            sql += field.getName()+ " VARCHAR(200)";
        }
        else if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            if(pt.getActualTypeArguments().length == 1
                    && (pt.getActualTypeArguments()[0].toString().contains("Long")
                    || pt.getActualTypeArguments()[0].toString().contains("long")
                    || pt.getActualTypeArguments()[0].toString().contains("Integer")
                    || pt.getActualTypeArguments()[0].toString().contains("int")
                    || pt.getActualTypeArguments()[0].toString().contains("Double")
                    || pt.getActualTypeArguments()[0].toString().contains("double")
                    || pt.getActualTypeArguments()[0].toString().contains("Float")
                    || pt.getActualTypeArguments()[0].toString().contains("float")
                    || pt.getActualTypeArguments()[0].toString().contains("boolean")
                    || pt.getActualTypeArguments()[0].toString().contains("Boolean")
                    || pt.getActualTypeArguments()[0].toString().contains("String"))) {

                sql += field.getName() + " TEXT";
            }
            else {
                // unknown build text fields to serialize
                fdfLog.debug("Was not able to identify field: {} of type: {} ", field.getName(), field.getType());
                sql += field.getName()+ " bytea";

            }

        }
        else {
            // unknown build text fields to serialize
            fdfLog.debug("Was not able to identify field: {} of type: {} ", field.getName(), field.getType());
            sql += field.getName()+ " bytea";

        }

        return sql;
    }

    static String parseWhere(List<WhereClause> where) {
        // If where clauses were passed, add them to the sql statement
        String sql = "";
        if(where != null && where.size() > 0) {
            sql += " where";
            for(WhereClause clause : where) {
                // if there is more then one clause, check the conditional type.
                if(where.indexOf(clause) != 0 && (where.indexOf(clause) +1) <= where.size()) {
                    if(clause.conditional == WhereClause.CONDITIONALS.AND) {
                        sql += " AND";
                    }
                    else if (clause.conditional == WhereClause.CONDITIONALS.OR) {
                        sql += " OR";
                    }
                    else if (clause.conditional == WhereClause.CONDITIONALS.NOT) {
                        sql += " NOT";
                    }
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
                    if (clause.valueDataType == String.class) {
                        sql += " " + clause.name + " " + clause.getOperatorString() + " '" + clause.value + "'";
                    } else if (clause.valueDataType == int.class || clause.valueDataType == Integer.class ||
                            clause.valueDataType == long.class || clause.valueDataType == Long.class ||
                            clause.valueDataType == double.class || clause.valueDataType == Double.class ||
                            clause.valueDataType == float.class || clause.valueDataType == Float.class ||
                            clause.value2DataType == BigDecimal.class) {
                        sql += " " + clause.name + " " + clause.getOperatorString() + " " + clause.value;
                    } else if (clause.valueDataType == boolean.class || clause.valueDataType == Boolean.class) {
                        if (clause.value.toLowerCase().equals("true")) {
                            sql += " " + clause.name + " " + clause.getOperatorString() + " true";
                        } else if (clause.value.toLowerCase().equals("false")) {
                            sql += " " + clause.name + " " + clause.getOperatorString() + " false";
                        }
                    } else if (clause.valueDataType == Date.class) {
                        sql += " " + clause.name + " " + clause.getOperatorString() + " '" + clause.value + "'";
                    } else if (clause.valueDataType == UUID.class) {
                        sql += " " + clause.name + " " + clause.getOperatorString() + " '" + clause.value + "'";
                    } else if (clause.value == WhereClause.NULL) {
                        sql += " " + clause.name + " " + clause.getOperatorString() + " " + clause.value + "";
                    } else {
                        sql += " " + clause.name + " " + clause.getOperatorString() + " '" + clause.value + "'";
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