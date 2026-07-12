package servlets;



import java.sql.Connection;

import java.sql.DriverManager;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.sql.SQLException;

import java.sql.Statement;



public class DatabaseUtil {

    private static final String DB_TYPE = "mysql";

    private static final String DB_HOST = "localhost";

    private static final int DB_PORT = 3306;

    private static final String DB_USERNAME = "ar274790";

    private static final String DB_PASSWORD = "nraozZzZ";

    private static final String DATABASE_NAME = "rcp274790_dis"; // Change this to your actual database name

    

    static {

        try {

            // Load the MySQL JDBC driver class

            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (ClassNotFoundException e) {

            e.printStackTrace();

            throw new RuntimeException("Error loading MySQL JDBC driver", e);

        }

    }

    



    public static Connection getConnection() throws SQLException {

        String jdbcUrl = "jdbc:" + DB_TYPE + "://" + DB_HOST + ":" + DB_PORT + "/" + DATABASE_NAME;

        return DriverManager.getConnection(jdbcUrl, DB_USERNAME, DB_PASSWORD);

    }



    public static Connection getConnection(String databaseName) throws SQLException {

        String jdbcUrl = "jdbc:" + DB_TYPE + "://" + DB_HOST + ":" + DB_PORT + "/" + databaseName;

        return DriverManager.getConnection(jdbcUrl, DB_USERNAME, DB_PASSWORD);

    }



    public static ResultSet executeQuery(String sql) throws SQLException {

        Connection conn = null;

        Statement stmt = null;

        ResultSet rs = null;

        try {

            conn = getConnection();

            stmt = conn.createStatement();

            rs = stmt.executeQuery(sql);

            return rs;

        } finally {

            closeResources(conn, stmt, rs);

        }

    }



    public static ResultSet executeQueryWithParams(String sql, String... params) throws SQLException {

        Connection conn = null;

        PreparedStatement stmt = null;

        ResultSet rs = null;

        try {

            conn = getConnection();

            stmt = conn.prepareStatement(sql);



            // Set parameters

            for (int i = 0; i < params.length; i++) {

                stmt.setString(i + 1, params[i]);

            }



            rs = stmt.executeQuery();

            return rs;

        } finally {

            closeResources(conn, stmt, rs);

        }

    }



    public static int executeUpdate(String sql) throws SQLException {

        Connection conn = null;

        Statement stmt = null;

        try {

            conn = getConnection();

            stmt = conn.createStatement();

            return stmt.executeUpdate(sql);

        } finally {

            closeResources(conn, stmt, null);

        }

    }



    public static void closeResources(Connection conn, Statement stmt, ResultSet rs) {

        try {

            if (rs != null) {

                rs.close();

            }

            if (stmt != null) {

                stmt.close();

            }

            if (conn != null) {

                conn.close();

            }

        } catch (SQLException e) {

            e.printStackTrace();

        }

    }

    

    public static void closeConnection(Connection conn) {

        try {

            if (conn != null) {

                conn.close();

            }

        } catch (SQLException e) {

            e.printStackTrace();

        }

    }



}



