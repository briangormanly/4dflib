package com.fdflib;

import com.fdflib.model.entity.FdfEntity;
import com.fdflib.model.state.FdfSystem;
import com.fdflib.persistence.database.DatabaseUtil;
import com.fdflib.service.CommonServices;
import com.fdflib.service.SystemServices;
import com.fdflib.util.FdfSettings;
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
        List<FdfEntity<FdfSystem>> systems = ss.getAllSystems();
        //Assert.assertEquals("Number of systems is 0", 1, systems.size());
        if(systems.size() < 1) {
            // get the default system
            FdfEntity<FdfSystem> defaultSystem = ss.getDefaultSystem();

            // create a new system called "Unit Test Syste"
            FdfSystem uts = new FdfSystem();
            uts.name = "Unit Test Syste";
            uts.description = "This is the first description from the basicSystemTest unit test";

            // add the new system
            FdfEntity<FdfSystem> dbUts1 = ss.save(FdfSystem.class, uts, 1, defaultSystem.current.id);

            // do an update to the "Unit Test Syste" record change description
            dbUts1.current.description = "This is the changed description";

            // sleep for a couple of seconds
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // save the new description
            FdfEntity<FdfSystem> dbUts2 = ss.save(FdfSystem.class, dbUts1.current, 1, defaultSystem.current.id);

            // make a third change to the description
            dbUts2.current.description = "This is the third description";

            // sleep for a couple of seconds
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // save the new description
            FdfEntity<FdfSystem> dbUts3 = ss.save(FdfSystem.class, dbUts2.current, 1, defaultSystem.current.id);

            if(dbUts3.current == null) {
                java.lang.System.out.println("~~~~ it was null!~");
                java.lang.System.out.println(" there was : " + dbUts3.history.size() + " in the history!");
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
            FdfEntity<FdfSystem> dbUts4 = ss.save(FdfSystem.class, dbUts3.current, 1, defaultSystem.current.id);

            // sleep for almost a couple of seconds
            try {
                Thread.sleep(1800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // opps... that was bad mark this record as being in error
            ss.setDeleteFlagSingleState(FdfSystem.class, dbUts4.current);

            // sleep for almost a couple of seconds
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //now that we deleted the row, put it back!!!!
            ss.removeDeleteFlagSingleState(FdfSystem.class, dbUts4.current);
        }



    }
}
