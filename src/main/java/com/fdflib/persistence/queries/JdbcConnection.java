package com.fdflib.persistence.queries;

import com.fdflib.util.FdfSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by brian on 1/11/17.
 */
public class JdbcConnection {

    private static HikariDataSource hds = null;

    private static org.slf4j.Logger fdfLog = LoggerFactory.getLogger(JdbcConnection.class);


    public Connection get4dfDbConnection() throws SQLException {
        return (FdfSettings.USE_HIKARICP) ? getHikariConnection() : getPlainConnection();
    }

    public Connection get4dfDbRootConnection() throws SQLException {
        return (FdfSettings.USE_HIKARICP) ? getNoDbHikariConnection() : getNoDbPlainConnection();
    }

    public void close4dfDbSession(Connection connection) throws SQLException {
        this.closeConnection(connection);
    }

    /**
     * This must be called when you application shuts down or you risk connection being left open the database with
     * connection pools.
     */
    public void shutdownDb() {
        if (FdfSettings.USE_HIKARICP) {
            hds.close();
        }
    }


    protected Connection getHikariConnection() throws SQLException {
        // check to see if the HakariDataSource has be created
        if(hds == null) initializeHakariDatasource();
        return hds.getConnection(FdfSettings.DB_USER, FdfSettings.DB_PASSWORD);
    }

    protected Connection getNoDbHikariConnection() throws SQLException {
        if(hds == null) initializeHakariDatasource();
        return hds.getConnection(FdfSettings.DB_ROOT_USER, FdfSettings.DB_ROOT_PASSWORD);
    }

    protected Connection getPlainConnection() throws SQLException {
        fdfLog.debug("Establishing mysql connection with regular credentials");
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            fdfLog.error("Database driver error!");
            e.printStackTrace();
        }

        System.out.println("----------- 2::: " + FdfSettings.returnDBConnectionString() + " " + FdfSettings.DB_USER + " " + FdfSettings.DB_PASSWORD );
        return DriverManager.getConnection(FdfSettings.returnDBConnectionString(),
                FdfSettings.DB_USER, FdfSettings.DB_PASSWORD);
    }

    protected Connection getNoDbPlainConnection() throws SQLException {
        fdfLog.debug("Establishing mysql connection with root credentials");
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            fdfLog.error("Database driver error!");
            fdfLog.error(e.getStackTrace().toString());
        }

        System.out.println("----------- 1::: " + FdfSettings.returnDBConnectionStringWithoutDatabase() + " " + FdfSettings.DB_ROOT_USER + " " + FdfSettings.DB_ROOT_PASSWORD );

        return DriverManager.getConnection(FdfSettings.returnDBConnectionStringWithoutDatabase(),
                FdfSettings.DB_ROOT_USER, FdfSettings.DB_ROOT_PASSWORD);
    }

    private void closeConnection(Connection connection) throws SQLException {
        fdfLog.debug("Closing mysql database connection.");
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private HikariDataSource initializeHakariDatasource() {
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName(FdfSettings.HIKARICP_DATASOURCE_CLASSNAME);
        config.setAutoCommit(FdfSettings.HIKARICP_AUTOCOMMIT);
        config.setConnectionTimeout(FdfSettings.HIKARICP_CONNECTION_TIMEOUT_MS);
        config.setIdleTimeout(FdfSettings.HIKARICP_IDLE_TIMEOUT_MS);
        config.setMaxLifetime(FdfSettings.HIKARICP_MAX_LIFETIME_MS);
        config.setConnectionTestQuery(FdfSettings.HIKARICP_CONNECTION_TEST_QUERY);
        config.setMaximumPoolSize(FdfSettings.HIKARICP_MAX_POOL_SIZE);
        config.setMinimumIdle(FdfSettings.HIKARICP_MIN_IDLE_MS);
        /* TODO: Caused by: java.lang.ClassNotFoundException: com.codahale.metrics.MetricRegistry
        if(FdfSettings.HIKARICP_METRIC_REG != null) {
            config.setMetricRegistry(FdfSettings.HIKARICP_METRIC_REG);
        }
        */
        /* TODO: Caused by: java.lang.ClassNotFoundException: com.codahale.metrics.MetricRegistry
        config.setHealthCheckRegistry(FdfSettings.HIKARICP_HEALTH_CHECK_REG);
        */
        config.setPoolName(FdfSettings.HIKARICP_POOL_NAME);
        config.setInitializationFailFast(FdfSettings.HIKARICP_FAIL_FAST);
        config.setIsolateInternalQueries(FdfSettings.HIKARICP_ISOLATE_INTERNAL_QUERIES);
        config.setAllowPoolSuspension(FdfSettings.HIKARICP_ALLOW_POOL_SUSPENSION);
        config.setReadOnly(FdfSettings.HIKARICP_READ_ONLY);
        config.setRegisterMbeans(FdfSettings.HIKARICP_REGISTER_MBEANS);
        /* TODO: IllegalArgumentException: Invalid transaction isolation value: driver default
        config.setCatalog(FdfSettings.HIKARICP_CATALOG);
         */
        config.setConnectionInitSql(FdfSettings.HIKARICP_CONNECTION_INIT_SQL);
        /* TODO: Caused by: java.lang.ClassNotFoundException: com.codahale.metrics.MetricRegistry
        config.setDriverClassName(FdfSettings.HIKARICP_DRIVER_CLASS_NAME);
        */
        /* TODO: IllegalArgumentException: Invalid transaction isolation value: driver default
        config.setTransactionIsolation(FdfSettings.HIKARICP_TRANSATION_ISOLATION);
         */
        config.setValidationTimeout(FdfSettings.HIKARICP_VALIDATION_TIMEOUT);
        config.setLeakDetectionThreshold(FdfSettings.HIKARICP_LEAK_DETECTION_THRESHOLD);
        config.setDataSourceClassName(FdfSettings.HIKARICP_DATA_SOURCE);
        config.setThreadFactory(FdfSettings.HIKARICP_THREAD_FACTORY);

        config.setJdbcUrl(FdfSettings.returnDBConnectionStringWithoutDatabase());
        //config.setUsername(FdfSettings.DB_ROOT_USER);
        //config.setPassword(FdfSettings.DB_ROOT_PASSWORD);

        return new HikariDataSource(config);
    }
}
