package tn.esprit.tools;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MyConnection {

    private static MyConnection instance;
    private Connection connection;

    private MyConnection() {
        Properties properties = new Properties();

        try (InputStream inputStream = MyConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("db.properties file not found in resources");
            }

            properties.load(inputStream);

            String url = properties.getProperty("db.url");
            String user = properties.getProperty("db.user");
            String password = properties.getProperty("db.password");

            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to database skillora.");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load database configuration", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to connect to the database", e);
        }
    }

    public static synchronized MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
