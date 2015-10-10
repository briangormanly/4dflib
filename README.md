<h1>4DFLib</h1>
<h2>4DF (4th Dimensional Form) Library and ORM Tool</h2>
Author: Brian Gormanly
bgormanly@gmail.com
4dflib.com
Copyright &copy; 2015

<h2>0. Introduction</h2>
What is 4DF????

4DF is a Library that manages your applications interaction with the database, providing ORM, database abstraction and
4DF or 4th Dimensional Form data which means that all data is saved in every state in every table over time.  Nothing
is ever deleted or updated.  It also provides a basic service layer for accessing your data through time, allowing you
to retrieve current and historical data and filtering by time ranges.

For more information see the follow blog posts:
    http://www.dailydevshot.com/4df-4th-dimensional-form-normalize-your-data-with-time/
    http://www.dailydevshot.com/4df-4th-dimensional-form-building-a-relational-database-with-a-memory-longer-then-a-goldfish/

And coming soon: http://4dflib.com


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


<h2>3. Maven Repository Information</h2>
Currently only the development SNAPSHOT is available, once stable versions are released this will be updated.
```
    <dependencies>
        ...
        <dependency>
            <groupId>com.fdflib</groupId>
            <artifactId>4dflib</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        ...
    </dependencies>
```


<h2>Examples of Usage</h2>
The easiest way to see how 4DFLib works is to build a very simple application that uses it.  For this example we have
created an application called "<a href="https://github.com/briangormanly/4dflib-bcs-example">Black Car Service</a>", or BCS for short.  BCS is a simple java application that uses maven
and run as a jar file on the command line to demonstrate how to setup 4dflib, and give you a sense of the power it can
yield.

Here is the BCS git repository for reference, you can get the source for the project there and follow along with this
quick and dirty tutorial <a href="https://github.com/briangormanly/4dflib-bcs-example">https://github.com/briangormanly/4dflib-bcs-example</a>

Taking a basic maven project to add 4DFLib as a dependency just add 4DFLib as shown in section 3 "Maven Repository 
Information" above.  

Our application is going to be very simple consisting of only a main method in our applications


Lets say that we have a small application that only had 2 model objects which are User and Car.

In order to use 4dflib in our application we only have to do the following in code that is run everytime the application
starts up.  The database will only be created if it does not already exist, the same goes for the default data entries
in the FdfSystem and FdfTenant 



The following will initialize 4DFLib to work with your application.
```
/**
 * Initialization and configuration of 4DF DB connection.
 */

// get the 4dflib settings singleton
FdfSettings fdfSettings = FdfSettings.getInstance();

// set the database type and name and connection information
fdfSettings.PERSISTENCE = DatabaseUtil.DatabaseType.MYSQL;
fdfSettings.DB_PROTOCOL = DatabaseUtil.DatabaseProtocol.JDBC_MYSQL;
fdfSettings.DB_ENCODING = DatabaseUtil.DatabaseEncoding.UTF8;
fdfSettings.DB_NAME = "mydbname";
fdfSettings.DB_HOST = "localhost";
fdfSettings.DB_USER = "myuser";
fdfSettings.DB_PASSWORD = "mysecurepassword";

// root user settings are only required for initial database creation.  Once the database is created you
// should remove this information
fdfSettings.DB_ROOT_USER = "root";
fdfSettings.DB_ROOT_PASSWORD = "myverysecurepassword";

/**
 * Next you just need to create an ArrayList<Class> containing your model objects extending the 4DFLib CommonState.
 */

// Create a array that will hold the classes that make up our 4df data model
List<Class> myModel = new ArrayList<>();

    Option 1 (manually add):
        myModel.add(MyFirstClass.class);
        myModel.add(MySecondClass.class);

    Option 2 (dynamically grab all classes in a package that are subclasses of CommonState):
        // setup a class scan
        ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(true);

        // filter to subclasses of CommonState
        provider.addIncludeFilter(new AssignableTypeFilter(CommonState.class));

        // look in the "model" package
        Set<BeanDefinition> components = provider.findCandidateComponents("model");

        // do the scan
        for (BeanDefinition component : components)
        {
            try {
                // add the class to our array
                Class cls = Class.forName(component.getBeanClassName());
                myModel.add(cls);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }


/**
 * Last step call the 4DFLib initialization passing in your data model List.
 */

// call the initialization of library!
CommonServices.initializeFdfDataModel(myModel);
```


<h3>Full Example using manual class selection (Using option 2):</h3>

```
// get the 4dflib settings singleton
FdfSettings fdfSettings = FdfSettings.getInstance();

// Create a array that will hold the classes that make up our 4df data model
List<Class> myModel = new ArrayList<>();

// set the database type and name and connection information
fdfSettings.PERSISTENCE = DatabaseUtil.DatabaseType.MYSQL;
fdfSettings.DB_PROTOCOL = DatabaseUtil.DatabaseProtocol.JDBC_MYSQL;
fdfSettings.DB_ENCODING = DatabaseUtil.DatabaseEncoding.UTF8;
fdfSettings.DB_NAME = "mydbname";
fdfSettings.DB_HOST = "localhost";
fdfSettings.DB_USER = "myuser";
fdfSettings.DB_PASSWORD = "mysecurepassword";

// root user settings are only required for initial database creation.  Once the database is created you
// should remove this information
fdfSettings.DB_ROOT_USER = "root";
fdfSettings.DB_ROOT_PASSWORD = "myverysecurepassword";

// Add your model to List
myModel.add(MyFirstClass.class);
myModel.add(MySecondClass.class);

// call the initialization of library!
CommonServices.initializeFdfDataModel(myModel);
```

<h3>Simple  Example</h3>
Let's Pretend that you have a 4DF object in your application called User.

Here is what the class looks like
```

```

