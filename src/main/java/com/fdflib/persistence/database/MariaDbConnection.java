package com.fdflib.persistence.database;

import com.fdflib.persistence.queries.JdbcConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class MariaDbConnection extends JdbcConnection {

    private static final MariaDbConnection INSTANCE = new MariaDbConnection();
    static Logger fdfLog = LoggerFactory.getLogger(MySqlConnection.class);

    private MariaDbConnection() {

    }

    public static MariaDbConnection getInstance() {
        return INSTANCE;
    }

    @Override
    public Connection get4dfDbConnection() throws SQLException {
        return super.get4dfDbConnection();
    }

    @Override
    public Connection get4dfDbRootConnection() throws SQLException {
        return super.get4dfDbRootConnection();
    }

    @Override
    public void close4dfDbSession(Connection connection) throws SQLException {
        super.close4dfDbSession(connection);
    }

}