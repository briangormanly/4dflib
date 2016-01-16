package com.fdflib.persistence.queries;

import com.fdflib.model.state.CommonState;
import com.fdflib.model.state.FdfSystem;
import com.fdflib.model.state.FdfTenant;
import com.fdflib.model.util.WhereClause;
import com.fdflib.persistence.connection.DbConnectionManager;
import com.fdflib.persistence.database.DatabaseUtil;
import com.fdflib.persistence.database.PostgreSqlConnection;
import com.fdflib.persistence.impl.CorePersistenceImpl;
import com.fdflib.service.FdfSystemServices;
import com.fdflib.service.FdfTenantServices;
import com.fdflib.util.FdfSettings;
import com.fdflib.util.FdfUtil;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Created by brian.gormanly on 1/14/16.
 */
public class CorePostgreSqlQueries extends DbConnectionManager implements CorePersistenceImpl {

    private static final CorePostgreSqlQueries INSTANCE = new CorePostgreSqlQueries();
    static org.slf4j.Logger fdfLog = LoggerFactory.getLogger(CorePostgreSqlQueries.class);

    private CorePostgreSqlQueries() {}

    public static CorePostgreSqlQueries getInstance() {
        return INSTANCE;
    }

    public void checkDatabase() throws SQLException {
        // create database
        String dbexists = "SELECT * FROM pg_database WHERE datname= '" + FdfSettings.DB_NAME.toLowerCase() + "';";

        Statement ps;
        ResultSet rs = null;
        try {
            ps = PostgreSqlConnection.getInstance().getNoDBSession().createStatement();

            if (ps != null) {
                rs = ps.executeQuery(dbexists);
            }

            if(rs != null) {
                if(!rs.next()) {
                    // Database does not exist, create
                    String sqlCreate = "CREATE DATABASE " +  "\"" + FdfSettings.DB_NAME.toLowerCase() + "\""
                            +  " ENCODING '" + FdfSettings.DB_ENCODING.UTF8 + "';";

                    String sqlCreateUser = "CREATE USER " +  "\"" +  FdfSettings.DB_USER.toLowerCase() + "\""
                            +  " WITH PASSWORD '" + FdfSettings.DB_PASSWORD + "';";
                    String sqlUserGrant = "GRANT ALL PRIVILEGES ON DATABASE " + "\"" + FdfSettings.DB_NAME + "\""
                            + " to " + "\"" +  FdfSettings.DB_USER.toLowerCase() + "\"" +  ";";

                    ps = PostgreSqlConnection.getInstance().getNoDBSession().createStatement();

                    if(ps != null) {
                        ps.executeUpdate(sqlCreate);
                        ps.executeUpdate(sqlCreateUser);
                        ps.executeUpdate(sqlUserGrant);
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

            // some other error
            fdfLog.warn("Error occurred checking or creating database:::");
            fdfLog.warn("SQL error \nCode: {},\nState: {}\nMessage" +
                    ": {}\n", sqlException.getErrorCode(), sqlException.getSQLState(), sqlException.getMessage());

        }
        finally {
            ps = null;
            // close the connection
            PostgreSqlConnection.getInstance().close();
        }

    }

    public void checkTables() throws SQLException {
        // get the 4df data model
        List<Class> classList = FdfSettings.getInstance().modelClasses;

        // create the tables for the model objects
        for(Class c: classList) {

            // check to see if the table already exists
            String tableTest = "select * from information_schema.TABLES where table_catalog = '"
                    + FdfSettings.DB_NAME.toLowerCase() +  "' and table_name = '"
                    +  c.getSimpleName().toLowerCase() +  "';";

            Statement ps = null;
            ResultSet rs = null;

            try {
                ps = PostgreSqlConnection.getInstance().getSession().createStatement();

                if(ps != null) {
                    rs = ps.executeQuery(tableTest);
                }

                if(rs != null) {
                    if (!rs.next()) {
                        // Table does not exist, create
                        fdfLog.debug("creating table: {}", c.getSimpleName().toLowerCase());
                        // check there there is at lease one field
                        if(c.getFields().length > 0) {
                            String sql = "CREATE TABLE " + "\"" +  c.getSimpleName().toLowerCase()
                                    + "\"" +  " ( ";
                            int fieldCounter = 0;

                            for (Field field : c.getFields()) {

                                sql += getFieldNameAndDataType(field);

                                fieldCounter++;

                                if (c.getFields().length > fieldCounter) sql += ", ";

                            }
                            sql += ");";

                            fdfLog.debug("Table sql {} : {}", c.getSimpleName().toLowerCase(), sql);

                            if(ps != null) {
                                ps.executeUpdate(sql);
                            }
                        }
                        else {
                            fdfLog.info("No table created for model object {} class had no valid data members", c.getSimpleName().toLowerCase());
                        }
                    }
                }
            }
            catch (SQLException sqlException) {

                // some other error
                fdfLog.warn("Error occurred checking or creating a table:::");
                fdfLog.warn("SQL error \nCode: {},\nState: {}\nMessage" +
                                ": {}\n", sqlException.getErrorCode(), sqlException.getSQLState(),
                        sqlException.getMessage());

            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            finally {
                ps = null;
                // close the connection
                PostgreSqlConnection.getInstance().close();
            }

        }
    }

    public void checkFields() throws SQLException {
        // get the 4df data model
        List<Class> classList = FdfSettings.getInstance().modelClasses;

        // create the tables for the model objects
        for(Class c: classList) {
            if(c.getFields().length > 0) {

                Statement ps = null;
                ResultSet rs = null;

                try {
                    ps = PostgreSqlConnection.getInstance().getSession().createStatement();

                    for (Field field : c.getFields()) {

                        // query for the field in the database
                        // check to see if the table already exists
                        String fieldTest = "select * from information_schema.columns where table_catalog= '"
                                +  FdfSettings.DB_NAME.toLowerCase() +  "' and table_name= '"
                                +  c.getSimpleName().toLowerCase() +  "' and column_name= '"
                                +  field.getName().toLowerCase() + "';";

                        //System.out.println("checking sql---------------> " + fieldTest);
                        if (ps != null) {
                            rs = ps.executeQuery(fieldTest);
                        }

                        if (rs != null) {
                            if (!rs.next()) {
                                // the field did not exist,
                                String alterSql = "alter table " + "\"" +  c.getSimpleName() + "\"" +  " add column "
                                        + "\"" +  this.getFieldNameAndDataType(field) + "\"" +  ";";

                                fdfLog.info("Add field sql {} : {}", c.getSimpleName().toLowerCase(), alterSql);

                                if(ps != null) {
                                    ps.executeUpdate(alterSql);
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
                    ps = null;
                    // close the connection
                    PostgreSqlConnection.getInstance().close();
                }
            }
        }
    }

    public void checkDefaultEntries() throws SQLException {
        // check to see if the default entry exists for FdfSystem
        FdfSystemServices ss = new FdfSystemServices();
        FdfSystem defaultSystem = ss.getDefaultSystem();
        if(defaultSystem == null) {
            // create the default FdfSystem entry
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
            // create the default FdfSystem entry
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
            // create the default FdfTenant
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

        // Start the sql statement
        String sql = "update " + "\"" +  c.getSimpleName().toLowerCase() + "\"" +  " set";

        int fieldCounter = 0;


        for(Field field: c.getFields()) {
            fieldCounter++;
            if(!field.getName().toLowerCase().equals("rid")) {
                sql += " " + field.getName().toLowerCase() + " = ?";
                if (c.getFields().length > fieldCounter) sql += ",";
            }
        }

        PreparedStatement preparedStmt;

        try {


            sql += " where rid = " + c.getField("rid").get(state) + " ;";


            preparedStmt = PostgreSqlConnection.getInstance().getSession().prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS);

            int fieldCounter3 = 1;
            for(Field field: c.getFields()) {
                try {
                    if(field.getType() == String.class) {
                        preparedStmt.setString(fieldCounter3, field.get(state).toString());
                    }

                    else if(field.getType() == int.class || field.getType() == Integer.class) {
                        if(!field.getName().toLowerCase().equals("rid")) {
                            preparedStmt.setInt(fieldCounter3, (int) field.get(state));

                        }
                    }
                    else if(field.getType() == Long.class || field.getType() == long.class) {
                        if(!field.getName().toLowerCase().equals("rid")) {
                            preparedStmt.setLong(fieldCounter3, (long) field.get(state));

                        }
                    }
                    else if(field.getType() == Double.class || field.getType() == double.class) {
                        if(!field.getName().toLowerCase().equals("rid")) {
                            preparedStmt.setDouble(fieldCounter3, (double) field.get(state));

                        }
                    }else if(field.getType() == Float.class || field.getType() == float.class) {
                        if(!field.getName().toLowerCase().equals("rid")) {
                            preparedStmt.setFloat(fieldCounter3, (float) field.get(state));

                        }
                    }

                    else if(field.getType() == boolean.class || field.getType() == Boolean.class) {
                        preparedStmt.setBoolean(fieldCounter3, (boolean) field.get(state));

                    }

                    else if(field.getType() == char.class || field.getType() == Character.class) {
                        preparedStmt.setString(fieldCounter3, field.get(state).toString().substring(0, 1));

                    }

                    else if(field.getType() == Date.class) {
                        Date insertDate = (Date) field.get(state);
                        if(insertDate == null) {
                            preparedStmt.setTimestamp(fieldCounter3, null);
                        }
                        else {
                            preparedStmt.setTimestamp(fieldCounter3, new Timestamp(insertDate.getTime()));
                        }

                    }

                    else if(field.getType() instanceof Class && ((Class<?>)field.getType()).isEnum()) {
                        preparedStmt.setString(fieldCounter3, field.get(state).toString());
                    }

                    else if(Class.class.isAssignableFrom(field.getType())) {
                        String className = field.get(state).toString();
                        preparedStmt.setString(fieldCounter3, FdfUtil.getClassName(className));
                    }

                    else {
                        // try to serialize the object
                        if(field.get(state) != null) {

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = null;
                            try {
                                oos = new ObjectOutputStream( baos );
                                oos.writeObject(field.get(state));
                                oos.close();

                                preparedStmt.setString(fieldCounter3,
                                        Base64.getEncoder().encodeToString(baos.toByteArray()));

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            preparedStmt.setNull(fieldCounter3, Types.BLOB);
                        }
                    }

                    if(!field.getName().toLowerCase().equals("rid")) fieldCounter3++;

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
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
        }
        finally {
            preparedStmt = null;
            // close the connection
            PostgreSqlConnection.getInstance().close();
        }

    }

    public <S> Long insert(Class<S> c, S state) {

        // spot to hold returned id for new record
        long newId = -1L;

        // Start the sql statement
        String sql = "insert into " + "\"" +  c.getSimpleName().toLowerCase() + "\"" +  " (";

        int fieldCounter = 0;
        for(Field field: c.getFields()) {
            fieldCounter++;
            if(!field.getName().toLowerCase().equals("rid")) {
                sql += " " + field.getName().toLowerCase();
                if (c.getFields().length > fieldCounter) sql += ",";
            }
        }

        sql += " ) values (";

        //insert the correct number of question marks for the prepared statement
        int fieldCounter2 = 0;
        for(Field field: c.getFields()) {
            fieldCounter2++;
            if(!field.getName().toLowerCase().equals("rid")) {
                sql += " ?";
                if (c.getFields().length > fieldCounter2) sql += ",";
            }
        }
        sql += ");";

        PreparedStatement preparedStmt;

        try {
            preparedStmt = PostgreSqlConnection.getInstance().getSession().prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS);

            int fieldCounter3 = 1;
            for(Field field: c.getFields()) {
                try {
                    if(field.getType() == String.class) {
                        if(field.get(state) != null) {
                            preparedStmt.setString(fieldCounter3, field.get(state).toString());
                        }
                        else {
                            preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                        }
                    }

                    else if(field.getType() == int.class || field.getType() == Integer.class) {
                        if(field.get(state) != null) {
                            if(!field.getName().toLowerCase().equals("rid")) {
                                preparedStmt.setInt(fieldCounter3, (int) field.get(state));
                            }
                        }
                        else {
                            preparedStmt.setNull(fieldCounter3, Types.INTEGER);
                        }
                    }
                    else if(field.getType() == Long.class || field.getType() == long.class) {
                        if(field.get(state) != null) {
                            if(!field.getName().toLowerCase().equals("rid")) {
                                preparedStmt.setLong(fieldCounter3, (long) field.get(state));
                            }
                        }
                        else {
                            preparedStmt.setNull(fieldCounter3, Types.BIGINT);
                        }
                    }
                    else if(field.getType() == Double.class || field.getType() == double.class) {
                        if(field.get(state) != null) {
                            if(!field.getName().toLowerCase().equals("rid")) {
                                preparedStmt.setDouble(fieldCounter3, (double) field.get(state));
                            }
                        }
                        else {
                            preparedStmt.setNull(fieldCounter3, Types.BIGINT);
                        }
                    }else if(field.getType() == Float.class || field.getType() == float.class) {
                        if(field.get(state) != null) {
                            if (!field.getName().toLowerCase().equals("rid")) {
                                preparedStmt.setFloat(fieldCounter3, (float) field.get(state));
                            }
                        }
                        else {
                            preparedStmt.setNull(fieldCounter3, Types.BIGINT);
                        }
                    }

                    else if(field.getType() == boolean.class || field.getType() == Boolean.class) {
                        if(field.get(state) != null) {
                            preparedStmt.setBoolean(fieldCounter3, (boolean) field.get(state));
                        }
                        else {
                            preparedStmt.setNull(fieldCounter3, Types.TINYINT);
                        }

                    }

                    else if(field.getType() == char.class || field.getType() == Character.class) {
                        if(field.get(state) != null) {
                            preparedStmt.setString(fieldCounter3, field.get(state).toString().substring(0, 1));
                        }
                        else {
                            preparedStmt.setString(fieldCounter3, "");
                        }
                    }

                    else if(field.getType() == Date.class) {
                        Date insertDate = (Date) field.get(state);
                        if(insertDate == null) {
                            preparedStmt.setTimestamp(fieldCounter3, null);
                        }
                        else {
                            preparedStmt.setTimestamp(fieldCounter3, new Timestamp(insertDate.getTime()));
                        }

                    }

                    else if(field.getType() instanceof Class && ((Class<?>)field.getType()).isEnum()) {
                        if(field.get(state) != null) {
                            preparedStmt.setString(fieldCounter3, field.get(state).toString());
                        }
                        else {
                            preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                        }
                    }

                    else if(Class.class.isAssignableFrom(field.getType())) {
                        if(field.get(state) != null) {
                            String className = field.get(state).toString();
                            preparedStmt.setString(fieldCounter3, FdfUtil.getClassName(className));
                        }
                        else {
                            preparedStmt.setNull(fieldCounter3, Types.VARCHAR);
                        }
                    }

                    else {
                        // try to serialize the object
                        if(field.get(state) != null) {

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = null;
                            try {
                                oos = new ObjectOutputStream( baos );
                                oos.writeObject(field.get(state));
                                oos.close();

                                preparedStmt.setString(fieldCounter3,
                                        Base64.getEncoder().encodeToString(baos.toByteArray()));

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            preparedStmt.setNull(fieldCounter3, Types.BLOB);
                        }
                    }

                    if(!field.getName().toLowerCase().equals("rid")) fieldCounter3++;

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }

            fdfLog.info("insert sql : {}", preparedStmt);

            preparedStmt.execute();
            ResultSet rs = preparedStmt.getGeneratedKeys();
            rs.next();
            newId = rs.getLong("id");


        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            preparedStmt = null;
            // close the connection
            PostgreSqlConnection.getInstance().close();
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
     * @param c
     * @param where
     * @param <S>
     * @return
     */
    public <S extends CommonState> List<S> selectQuery(Class c, List<String> select, List<WhereClause> where) {

        // start the sql statement
        String sql = "select";
        if(select != null) {
            int selectCount = 0;
            for(String selectItem: select) {
                sql += " " + selectItem;
                selectCount++;
                if (selectCount < select.size()) sql += ",";
            }
        }
        else {
            sql += " *";
        }

        sql += " from " + "\"" +  c.getSimpleName().toLowerCase() +  "\"";

        sql += parseWhere(where);

        sql += ";";

        fdfLog.debug("select sql: {}", sql);

        List<S> everything = new ArrayList<>();

        PreparedStatement ps;
        ResultSet rs;

        try {
            ps = PostgreSqlConnection.getInstance().getSession().prepareStatement(sql);

            if(ps != null) {
                rs = ps.executeQuery();
                while (rs.next ()) {

                    // create a new object of type passed
                    Object thisObject = c.newInstance();

                    for(Field field: c.getFields()) {

                        // check the datatype of the field to apply the correct method to
                        // retrieve the data on the resultset
                        if(field.getType() == String.class) {
                            try {
                                field.setAccessible(true);
                                field.set(thisObject, rs.getString(field.getName().toLowerCase()));
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                        }
                        else if(field.getType() == int.class || field.getType() == Integer.class) {
                            try {
                                field.setAccessible(true);
                                field.set(thisObject, rs.getInt(field.getName().toLowerCase()));
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                        }
                        else if(field.getType() == long.class || field.getType() == Long.class) {
                            try {
                                field.setAccessible(true);
                                field.set(thisObject, rs.getLong(field.getName().toLowerCase()));
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(),
                                            e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                        }
                        else if(field.getType() == double.class || field.getType() == Double.class) {
                            try {
                                field.setAccessible(true);
                                field.set(thisObject, rs.getDouble(field.getName().toLowerCase()));
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                        }
                        else if(field.getType() == float.class || field.getType() == Float.class) {
                            try {
                                field.setAccessible(true);
                                field.set(thisObject, rs.getFloat(field.getName().toLowerCase()));
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                        }
                        else if(field.getType() == int.class || field.getType() == Integer.class) {
                            try {
                                field.setAccessible(true);
                                field.set(thisObject, rs.getInt(field.getName().toLowerCase()));
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                        }
                        else if(field.getType() == char.class || field.getType() == Character.class) {
                            try {
                                field.setAccessible(true);
                                if(rs.getString(field.getName()) != null && rs.getString(field.getName()).length() > 0) {
                                    field.set(thisObject, rs.getString(field.getName().toLowerCase()).charAt(0));
                                }
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                        }
                        else if(field.getType() == Date.class) {
                            try {
                                field.setAccessible(true);
                                field.set(thisObject, rs.getTimestamp(field.getName().toLowerCase()));
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                            catch (NullPointerException npe) {
                                // Nullpointer in timestap
                                fdfLog.debug("NullPointer on timestamp column {}, This is usually because select"
                                        + "statement did not include column", field.getName().toLowerCase(), npe.getMessage());
                            }
                        }
                        else if(field.getType() == boolean.class) {
                            try {
                                field.setAccessible(true);
                                field.set(thisObject, rs.getBoolean(field.getName().toLowerCase()));
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                        }
                        else if(field.getType() == Boolean.class) {
                            try {
                                field.setAccessible(true);
                                field.set(thisObject, rs.getBoolean(field.getName().toLowerCase()));
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                        }
                        else if(field.getType() instanceof Class && ((Class<?>)field.getType()).isEnum()) {
                            try {
                                field.setAccessible(true);
                                field.set(thisObject, Enum.valueOf((Class<Enum>) field.getType(),
                                        rs.getString(field.getName().toLowerCase())));
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                        }
                        else if(Class.class.isAssignableFrom(field.getType())) {
                            try {
                                field.setAccessible(true);
                                field.set(thisObject,
                                        FdfUtil.getClassByFullyQualifiedName(
                                                rs.getString(field.getName().toLowerCase())));
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
                                } else {
                                    fdfLog.warn("SQL error in Select\nCode: {},\nState: {}\nMessage" +
                                            ": {}\n", e.getErrorCode(), e.getSQLState(), e.getMessage());
                                }
                            }
                        }
                        else {
                            // serialized object, deserialize
                            try {
                                field.setAccessible(true);

                                byte [] data = Base64.getDecoder().decode(rs.getString(field.getName().toLowerCase()));
                                ObjectInputStream ois = new ObjectInputStream(
                                        new ByteArrayInputStream(  data ) );
                                Object o  = ois.readObject();
                                ois.close();

                                field.set(thisObject, o);
                            } catch (SQLException e) {
                                if(e.getSQLState().equals("42703")) {
                                    // Invalid column name, thrown if select statement does not include column
                                    fdfLog.debug("Select statement had sql state 42703 (Invalid column name) on column"
                                            + "{}, This is usually because select statement did not include column and "
                                            + "can be ignored. Message is {}", field.getName().toLowerCase(), e.getMessage());
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
        }
        finally {
            rs = null;
            ps = null;
            // close the connection
            PostgreSqlConnection.getInstance().close();
        }

        return everything;
    }

    static String getFieldNameAndDataType(Field field) {
        String sql = "";

        fdfLog.debug("checking field: {} of type: {} ", field.getName().toLowerCase(), field.getType());

        if (field.getType() == String.class) {
            sql += field.getName().toLowerCase()+ " TEXT";
        } else if (field.getType() == int.class || field.getType() == Integer.class) {
            sql += field.getName().toLowerCase()+ " INT";
        } else if (field.getType() == Long.class || field.getType() == long.class) {

            if (field.getName().equals("rid")) {
                sql += field.getName().toLowerCase()+ " BIGSERIAL PRIMARY KEY";
            }
            else {
                sql += field.getName().toLowerCase()+ " BIGINT";
            }
        } else if (field.getType() == Double.class || field.getType() == double.class) {
            sql += field.getName().toLowerCase()+ " double precision";
        } else if (field.getType() == Float.class || field.getType() == float.class) {
            sql += field.getName().toLowerCase()+ " real";
        } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
            sql += field.getName().toLowerCase()+ " BOOLEAN";
        } else if (field.getType() == Date.class) {
            sql += field.getName().toLowerCase()+ " TIMESTAMP";
            if (field.getName().equals("arsd")) {
                sql += " DEFAULT CURRENT_TIMESTAMP";
            } else {
                sql += " NULL";
            }
        } else if (field.getType() == Character.class || field.getType() == char.class) {
            sql += field.getName().toLowerCase()+ " CHAR";
        } else if (field.getType() instanceof Class && ((Class<?>) field.getType()).isEnum()) {
            sql += field.getName().toLowerCase()+ " VARCHAR(200)";
        } else if (Class.class.isAssignableFrom(field.getType())) {
            sql += field.getName().toLowerCase()+ " VARCHAR(200)";
        }
        else {
            // unknown create text fields to serialize
            fdfLog.debug("Was not able to identify field: {} of type: {} ", field.getName(), field.getType());
            sql += field.getName().toLowerCase()+ " BLOB";

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
                if(clause.valueDataType == String.class) {
                    sql += " " + clause.name + " " + clause.getOperatorString() + " '" + clause.value + "'";
                }
                else if(clause.valueDataType == int.class || clause.valueDataType == Integer.class ||
                        clause.valueDataType == long.class || clause.valueDataType == Long.class ||
                        clause.valueDataType == double.class || clause.valueDataType == Double.class ||
                        clause.valueDataType == float.class || clause.valueDataType == Float.class){
                    sql += " " + clause.name + " " + clause.getOperatorString() + " " + clause.value;
                }
                else if(clause.valueDataType == boolean.class || clause.valueDataType == Boolean.class) {
                    if(clause.value.toLowerCase().equals("true")) {
                        sql += " " + clause.name + " " + clause.getOperatorString() + " true";
                    }
                    else if (clause.value.toLowerCase().equals("false")) {
                        sql += " " + clause.name + " " + clause.getOperatorString() + " false";
                    }
                }
                else if(clause.valueDataType == Date.class) {
                    sql += " " + clause.name + " " + clause.getOperatorString() + " '" + clause.value + "'";
                }
                else if(clause.value == WhereClause.NULL) {
                    sql += " " + clause.name + " " + clause.getOperatorString() + " " + clause.value + "";
                }
                else {
                    sql += " " + clause.name + " " + clause.getOperatorString() + " '" + clause.value + "'";
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