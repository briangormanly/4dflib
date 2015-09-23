package com.fdflib;

import com.fdflib.model.entity.FdfEntity;
import com.fdflib.model.state.SystemState;
import com.fdflib.persistence.database.DatabaseUtil;
import com.fdflib.service.CommonServices;
import com.fdflib.service.SystemServices;
import com.fdflib.util.FdfSettings;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class GenericTest {

    @Test
    public void CreateDBTest() {
        // get the 4dflib settings singleton
        FdfSettings fdfSettings = FdfSettings.getInstance();

        // Create a array that will hold the classes that make up our 4df data model
        List<Class> myModel = new ArrayList<>();

        // set the database type and name and connection information
        fdfSettings.PERSISTENCE = DatabaseUtil.DatabaseType.MYSQL;
        fdfSettings.DB_PROTOCOL = DatabaseUtil.DatabaseProtocol.JDBC_MYSQL;
        fdfSettings.DB_ENCODING = DatabaseUtil.DatabaseEncoding.UTF8;
        fdfSettings.DB_NAME = "test4dflib";
        fdfSettings.DB_HOST = "localhost";
        fdfSettings.DB_USER = "test4dflib";
        fdfSettings.DB_PASSWORD = "test4dflibpassword";

        // root user settings are only required for initial database creation.  Once the database is created you
        // should remove this information
        fdfSettings.DB_ROOT_USER = "root";
        fdfSettings.DB_ROOT_PASSWORD = "";

        // call the initialization of library!
        CommonServices.initializeFdfDataModel(myModel);

        // check that the database exists and that we can query the default system.
        SystemServices ss = new SystemServices();
        List<FdfEntity<SystemState>> systems = ss.getAllServices();
        Assert.assertEquals("Number of systems is 0", 1, systems.size());
    }
}