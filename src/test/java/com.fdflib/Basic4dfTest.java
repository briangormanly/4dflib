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


/**
 * Created by brian.gormanly on 9/25/15.
 */
public class Basic4dfTest {

    @Test
    public void basicSystemTest() {

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
        List<FdfEntity<SystemState>> systems = ss.getAllSystems();
        Assert.assertEquals("Number of systems is 0", 1, systems.size());

        // get the default system
        FdfEntity<SystemState> defaultSystem = ss.getDefaultSystem();

        // create a new system called "Unit Test Syste"
        SystemState uts = new SystemState();
        uts.name = "Unit Test Syste";
        uts.description = "This is the first description from the basicSystemTest unit test";

        // add the new system
        FdfEntity<SystemState> dbUts1 = ss.save(SystemState.class, uts, 1, defaultSystem.current.id);

        // do an update to the "Unit Test Syste" record change description
        dbUts1.current.description = "This is the changed description";

        // sleep for a couple of seconds
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // save the new description
        FdfEntity<SystemState> dbUts2 = ss.save(SystemState.class, dbUts1.current, 1, defaultSystem.current.id);

        // make a third change to the description
        dbUts2.current.description = "This is the third description";

        // sleep for a couple of seconds
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // save the new description
        FdfEntity<SystemState> dbUts3 = ss.save(SystemState.class, dbUts2.current, 1, defaultSystem.current.id);

        if(dbUts3.current == null) {
            System.out.println("~~~~ it was null!~");
            System.out.println(" there was : " + dbUts3.history.size() + " in the history!");
        }

        // do an update to the name, change to "Unit Test ERROR"
        dbUts3.current.name = "Unit Test ERRROR";

        // sleep for almost a couple of seconds
        try {
            Thread.sleep(1800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // save the new description
        FdfEntity<SystemState> dbUts4 = ss.save(SystemState.class, dbUts3.current, 1, defaultSystem.current.id);

        // sleep for almost a couple of seconds
        try {
            Thread.sleep(1800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // opps... that was bad mark this record as being in error
        ss.setDeleteFlagSingleState(SystemState.class, dbUts4.current);

        // sleep for almost a couple of seconds
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //now that we deleted the row, put it back!!!!
        ss.removeDeleteFlagSingleState(SystemState.class, dbUts4.current);

    }
}
