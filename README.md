<h1>4DFLib</h1>
<h2>4DF (4th Dimensional Form) Library and ORM Tool</h2>
Author: Brian Gormanly
bgormanly@gmail.com
4dflib.com
Copyright &copy; 2015-2020

<h2>0. Introduction</h2>
What is 4DF????

4DF is a Library that manages your applications interaction with the database, providing ORM, database abstraction and
4DF or 4th Dimensional Form data which means that all data is saved in every state in every table over time.  Nothing
is ever deleted or updated, and you do not need to anything to implement this behavior, it happens automatically.  It 
also provides a basic service layer for accessing your data through time, allowing you to retrieve current and 
historical data and filtering by time ranges.

4DFLib currently works with HyperSQL (HSQLDB), PostgreSQL, MariaDB (new in 4DFLib 1.4) and MySQL (Only MySQL version 5.7), but many more are planned soon and you can easily 
implement your own database see:
    com.fdflib.persistence.impl.CorePersistenceImpl;
    and example connections in: com.fdflib.persistence.database 
    and dynamic queries builders in: com.fdflib.persistence.queries
    
    We welcome pull requests with new database implementations.  4DFLib can support connecting to both relational and 
    NoSQL persistence.

For more information see the follow blog posts:
    http://www.dailydevshot.com/4df-4th-dimensional-form-normalize-your-data-with-time/
    http://www.dailydevshot.com/4df-4th-dimensional-form-building-a-relational-database-with-a-memory-longer-then-a-goldfish/

Website: http://4dflib.com


<h2>1. License</h2>

Distributed under the LGPL License (http://www.gnu.org/licenses/lgpl-3.0.en.html)
Please see license.txt for details.

    4DFLib is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as
    published by the Free Software Foundation, either version 3 of
    the License or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

<h2>2. Logging</h2>

4DFLib using SLF4J for logging for Logging Facade.  You can plug in your
desired logging framework at deployment time.

<h2>2.1 Connection Pool</h2>

As of version 1.3 4DFLib uses HikariCP (https://github.com/brettwooldridge/HikariCP) for connection pooling.  To use it simplely add the following FdfSetting when initializing: 

    fdfSettings.USE_HIKARICP = true;

That is it, for additional connection pool tuning see [FdfSettings](https://github.com/briangormanly/4dflib/blob/master/src/main/java/com/fdflib/util/FdfSettings.java)

<h2>3. Maven Central Repository Information</h2>
```
    <dependencies>
        ...
        <dependency>
            <groupId>com.fdflib</groupId>
            <artifactId>4dflib</artifactId>
            <version>1.4</version>
        </dependency>
        ...
    </dependencies>
```
<h3>Gradle</h3>
```
dependencies {
    ...
    compile "com.fdflib:4dflib:1.4"
    ...
}
```
<h3>sbt</h3>
```
libraryDependencies ++= Seq(
  ...
  "com.fdflib" % "4dflib" % "1.4"
  ...
)
```

<h2>Examples of Usage</h2>
You can initialize the Library like so (the only assumptions here are that you have a model class Foo that you would like to persist with a <strong>default contstructor</strong> (for reflection), and that Foo extends com.fdflib.model.state.CommonState. There will be an example of an 4DFLib model object below this section):
```
// Create a array that will hold the classes that make up our 4df data model
List<Class> myModel = new ArrayList<>();

// add model objects
myModel.add(Foo.class);

// call the initialization of library!
FdfServices.initializeFdfDataModel(myModel);
```
That is it!  Just add any classes you want to persist to myModel. If all was configured correctly, you will now have a HSQLDB database named 4dfapplicationdb with a table called Foo.  The database is written to disk by default in the hsql/ folder within your project.  HSQLDB is provided to test by default, but you can easly change 4DFLib to use MariaDB, PostgreSQL or MySQL (version 5.7) as well. 

Here is an example of a model class that is ready to be persisted by 4DFLib:
```
public class Foo extends CommonState {

    public String name;
    public List<String> coolThings;

    @FdfIgnore
    public Integer notPersisted;

    /**
     * Default constructor required!! (Required for Reflection calls)
     */
    public Lab1User() {
        super();
        coolThings = new ArrayList<>();
    }
}
```
To create and save Foo with 4DFLib you can do the following:
```
Foo foo = new Foo();
foo.name = "fooboo";
foo.coolThings.add("Ice cream");
foo.coolThings.add("V8 Engines");
foo.coolThings.add("First Dates");
foo.notPersisted = 10;

// save our object to the database
FdfCommonServices.save(Foo.class, foo);
```
If you check your fdfapplication database you should now have a saved record for fooboo.

<h3>Working with your data</h3>
4DFLib provides built in GenericServices that you can use to retrieve historical and current data, and also provides many useful retrieval methods see: <strong>com.fdflib.service.impl.FdfCommonServices</strong> for details and <strong>com.fdflib.service.FdfSystemServices</strong> and <strong>com.fdflib.service.FdfTenantServices</strong> for examples of how simple and powerful your services can be leveraging this tool.

Here is an example asking 4DFLib to retrieve all instances of Foo:
```
GenericService genericService = new GenericService();
        
// get all Foo objects
List<Foo> allFoo = genericService.getAllCurrent(Foo.class);
        
// get all Foo objects including audit information (all changes though time)
List<FdfEntity<Foo>> allFooWithHistory = genericService.getAll(Foo.class);
```

There are more super-powerful methods in there like: getEntitiesByValuesForPassedFields, getAtDateById(), getAtDateById(), getEntityBetweenDatesById() and many more!  And they are all yours to use, in all your services, free of charge!

<h3>FdfSettings</h3>
So far, we have used all the default settings, you can can override any by investigating the following settings contained here: [FdfSettings](https://github.com/briangormanly/4dflib/blob/master/src/main/java/com/fdflib/util/FdfSettings.java)
```
// get the 4dflib settings singleton
FdfSettings fdfSettings = FdfSettings.getInstance();
```
Example PostgreSQL configuration:
```
fdfSettings.PERSISTENCE = DatabaseUtil.DatabaseType.POSTGRES;
fdfSettings.DB_PROTOCOL = DatabaseUtil.DatabaseProtocol.JDBC_POSTGRES;
fdfSettings.DB_ENCODING = DatabaseUtil.DatabaseEncoding.UTF8;
fdfSettings.DB_HOST = "localhost";
fdfSettings.DB_NAME = "myDB";
fdfSettings.DB_ROOT_USER = "postgres"; 
fdfSettings.DB_ROOT_PASSWORD = "";
fdfSettings.DB_USER = "myUser";
fdfSettings.DB_PASSWORD = "myUserPassword";       
```
Example MariaDB configuration:
```
fdfSettings.PERSISTENCE = DatabaseUtil.DatabaseType.MARIADB;
fdfSettings.DB_PROTOCOL = DatabaseUtil.DatabaseProtocol.JDBC_MARIADB;
fdfSettings.DB_ENCODING = DatabaseUtil.DatabaseEncoding.UTF8;
fdfSettings.DB_HOST = "localhost";
fdfSettings.DB_NAME = "myDB";
fdfSettings.DB_ROOT_USER = "root"; 
fdfSettings.DB_ROOT_PASSWORD = "";
fdfSettings.DB_USER = "myUser";
fdfSettings.DB_PASSWORD = "myUserPassword";       
```
Example MySQL configuration:
```
fdfSettings.PERSISTENCE = DatabaseUtil.DatabaseType.MYSQL;
fdfSettings.DB_PROTOCOL = DatabaseUtil.DatabaseProtocol.JDBC_MYSQL;
fdfSettings.DB_ENCODING = DatabaseUtil.DatabaseEncoding.UTF8;
fdfSettings.DB_HOST = "localhost";
fdfSettings.DB_NAME = "myDB";
fdfSettings.DB_ROOT_USER = "root"; 
fdfSettings.DB_ROOT_PASSWORD = "";
fdfSettings.DB_USER = "myUser";
fdfSettings.DB_PASSWORD = "myUserPassword";      
```
Other configuration options:
```
// If true, HSQLDB will create a file based db which will persist, false will be in memory database.
public static Boolean HSQL_DB_FILE = true;
//HyperSQL database file location
public static String HQSL_DB_FILE_LOCATION = "hsql/";

// 4DF gives you multitenant software for free! You can modify your default tenant if you like.
public static String DEFAULT_TENANT_NAME = "Default FdfTenant";
public static String DEFAULT_TENANT_DESRIPTION = "Default FdfTenant is created by 4dflib, if you do not intent to use "
        + "built in multi-tenancy or only have one FdfTenant, all data is member of this tenant by "
        + "default";
public static String DEFAULT_TENANT_WEBSITE = "http://www.4dflib.com";
public static Boolean DEFAULT_TENANT_IS_PRIMARY = true;

// 4DF also lets you keep track of and manage what external systems connect to your software
public static String DEFAULT_SYSTEM_NAME = "Default FdfSystem";
public static String DEFAULT_SYSTEM_DESCRIPTION = "Default system represents the actual application and not"
        + " a registered external system.";
public static String DEFAULT_SYSTEM_PASSWORD = "4DfPassword";

public static String TEST_SYSTEM_NAME = "Default Test System";
public static String TEST_SYSTEM_DESCRIPTION = "Default test system for use connecting to the system for testing";
public static String TEST_SYSTEM_PASSWORD = "testSystemPassword";
```
<h2>Full Examples:</h2>

We are working on updating the full examples to use version 1.4 of 4DFLib


