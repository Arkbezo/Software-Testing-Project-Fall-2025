package IntegrationTesting;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.*;
import java.util.UUID;

import static org.junit.Assert.*;

public class UserAuthIntegrationTest {

    private Connection conn;

    // Use unique username so tests don't clash with existing data
    private String testUsername;
    private String correctPassword;
    private String role;

    @Before
    public void setUp() throws Exception {
        conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/test_pos",
                "root",
                "centralcee23" // <-- change if needed
        );

        testUsername = "it_user_" + UUID.randomUUID().toString().substring(0, 8);
        correctPassword = "pass12345";
        role = "admin";

        // Insert a known user for authentication tests
        String insertSql = "INSERT INTO pos.users (username, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, testUsername);
            ps.setString(2, correctPassword);
            ps.setString(3, role);

            int rows = ps.executeUpdate();
            assertEquals("Setup failed: user insert did not insert exactly 1 row", 1, rows);
        }
    }

    // Helper method that simulates "login check"
    private boolean authenticate(String username, String password) throws SQLException {
        String sql = "SELECT 1 FROM pos.users WHERE username = ? AND password = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true if a match exists
            }
        }
    }

    @Test
    public void testAuthenticateUser_validCredentials_returnsTrue() throws Exception {
        boolean result = authenticate(testUsername, correctPassword);
        assertTrue("Expected authentication to succeed with correct credentials", result);
    }

    @Test
    public void testAuthenticateUser_wrongPassword_returnsFalse() throws Exception {
        boolean result = authenticate(testUsername, "wrongPassword!");
        assertFalse("Expected authentication to fail with wrong password", result);
    }

    @Test
    public void testAuthenticateUser_unknownUsername_returnsFalse() throws Exception {
        boolean result = authenticate("no_such_user_123", correctPassword);
        assertFalse("Expected authentication to fail for unknown username", result);
    }

    @After
    public void tearDown() throws Exception {
        // Clean inserted test user
        String deleteSql = "DELETE FROM pos.users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setString(1, testUsername);
            ps.executeUpdate();
        }

        if (conn != null && !conn.isClosed()) conn.close();
    }
}
