package org.example;

import java.sql.*;
import java.util.Scanner;

public class Demo2 {
    private static Connection conn;
    private static Scanner scanner;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        conn = establishConnection();
        User user = authenticateUser();

        if (user != null) {
            performUserOperations(user);
        } else {
            System.out.println("Authentication failed!");
        }

        closeResources();
    }

    private static Connection establishConnection() {
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/Database", "testuserName", "123456");
        } catch (SQLException e) {
            System.out.println("Failed to connect to the database: " + e.getMessage());
            return null;
        }
    }

    private static User authenticateUser() {
        System.out.println("Enter username:");
        String username = scanner.nextLine();
        System.out.println("Enter password:");
        String password = scanner.nextLine();

        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT role FROM users WHERE username = '" + username + "' AND password = '" + password + "'";
            System.out.println("Executing query: " + sql);
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                String role = rs.getString("role");
                rs.close();
                stmt.close();
                return new User(username, role);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL Exception: " + e.getMessage());
        }
        return null;
    }

    private static void performUserOperations(User user) {
        // Simulate operations based on user role
        System.out.println("Logged in as: " + user.getUsername() + " with role: " + user.getRole());
        if ("admin".equals(user.getRole())) {
            System.out.println("Performing administrative tasks.");
        } else {
            System.out.println("Performing general user tasks.");
        }
    }

    private static void closeResources() {
        try {
            if (conn != null) conn.close();
            scanner.close();
        } catch (SQLException e) {
            System.out.println("Error closing the database connection: " + e.getMessage());
        }
    }

    static class User {
        private String username;
        private String role;

        public User(String username, String role) {
            this.username = username;
            this.role = role;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }
    }
}
