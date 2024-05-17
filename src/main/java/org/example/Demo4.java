package org.example;

import java.sql.*;
import java.util.Scanner;

public class Demo4 {
    private static Connection conn;
    private static Scanner scanner;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        conn = establishConnection();

        // Introducing an aliased scanner
        Scanner aliasedScanner = scanner;

        try {
            // Passing aliased scanner to emphasize on aliasing effect
            loginUsers(aliasedScanner);
        } finally {
            closeResources();
        }
    }

    private static Connection establishConnection() {
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/Database", "testuserName", "123456");
        } catch (SQLException e) {
            System.out.println("Failed to connect to the database: " + e.getMessage());
            return null;
        }
    }

    private static void loginUsers(Scanner aliasedScanner) {
        try {
            Statement stmt = conn.createStatement();
            System.out.println("Enter username:");
            // Using aliased scanner to get username
            String username = getUserInput("username", aliasedScanner);
            System.out.println("Enter password:");
            // Using original scanner to get password
            String password = getUserInput("password", scanner);

            // Construct SQL query with user inputs directly
            String sql = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'";
            System.out.println("Executing query: " + sql);

            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.println("Login successful!");
            } else {
                System.out.println("Login failed!");
            }

            rs.close();
            stmt.close();
        } catch (SQLException se) {
            System.out.println("SQL Exception: " + se.getMessage());
        }
    }

    private static String getUserInput(String field, Scanner scannerInstance) {
        System.out.println("Please enter your " + field + ":");
        return scannerInstance.nextLine();
    }

    private static void closeResources() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.out.println("Error closing the database connection: " + e.getMessage());
        }
        scanner.close();
    }
}
