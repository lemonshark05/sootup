package org.example;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Demo3 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // Establish connection
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Database", "testuserName", "123456");
            stmt = conn.createStatement();

            // Get user input
            System.out.println("Enter username:");
            String username = scanner.nextLine();
            System.out.println("Enter password:");
            String password = scanner.nextLine();

            // Demonstration of SQL Injection vulnerability
            // Example of malicious input:
            // Username: admin' --
            // This input terminates the SQL command and makes the password check irrelevant.
            String sql = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'";
            System.out.println("Executing query: " + sql);  // Print the query to demonstrate the injection

            // Execute query
            rs = stmt.executeQuery(sql);

            // Process the result
            if (rs.next()) {
                System.out.println("Login successful!");
            } else {
                System.out.println("Login failed!");
            }
        } catch (SQLException se) {
            System.out.println("SQL Exception: " + se.getMessage());
        } finally {
            // Ensure resources are closed in the finally block
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException se) {
                System.out.println("Error closing resources: " + se.getMessage());
            }
        }

        scanner.close();
    }
}
