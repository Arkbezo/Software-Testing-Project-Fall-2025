package IntegrationTesting;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import model.dto.UserDTO;

import static org.junit.Assert.*;

import java.sql.*;

public class UserIntegrationTest {

    private Connection conn;
    private UserDTO user;

    @Before
    public void setUp() throws SQLException {
        // Establish connection to the test database (test_pos)
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test_pos", "root", "centralcee23");

        // Initialize a UserDTO object for testing
        user = new UserDTO("admin", "password123", "admin");
    }

    @Test
    public void testAddUser() throws SQLException {
        String sql = "INSERT INTO pos.users (username, password, role) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole());
            
            // Execute the insert query
            int result = stmt.executeUpdate();
            
            // Assert that one row has been inserted
            assertEquals(1, result); // Ensure 1 row is inserted
        }
    }

    @Test
    public void testGetUserByUsername() throws SQLException {
        // First insert the user
        testAddUser();

        // Now, retrieve the user by username
        String sql = "SELECT username, password, role FROM pos.users WHERE username = ?";
        UserDTO fetchedUser = null;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                fetchedUser = new UserDTO(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("role")
                );
            }
        }

        // Assert that the fetched user matches the inserted user
        assertNotNull(fetchedUser);
        assertEquals(user.getUsername(), fetchedUser.getUsername());
        assertEquals(user.getPassword(), fetchedUser.getPassword());
        assertEquals(user.getRole(), fetchedUser.getRole());
    }

    @After
    public void tearDown() throws SQLException {
        // Clean up the test database after each test by deleting the user
        String deleteSQL = "DELETE FROM pos.users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSQL)) {
            stmt.setString(1, user.getUsername());
            stmt.executeUpdate();
        }

        // Close the database connection
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
    
    @Test
    public void testAddUserWithInvalidData() throws SQLException {
        String sql = "INSERT INTO pos.users (username, password, role) VALUES (?, NULL, ?)";  // Use NULL directly

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "user");
            stmt.setString(2, "guest");

            // This should throw an exception because password cannot be null
            stmt.executeUpdate();
            fail("Expected SQLException due to NULL password, but no exception was thrown.");
        } catch (SQLException e) {
            // Log the exception to help understand the error
            e.printStackTrace(); // Print the full exception stack trace

            // Assert that the expected exception was thrown
            assertTrue("Expected 'column 'password' cannot be null' error message", 
                       e.getMessage().contains("column 'password' cannot be null"));

            // check for specific SQL error codes (e.g., error code 1048 for NOT NULL violation)
            assertEquals(1048, e.getErrorCode());  // Ensure error code is for 'not null' violation
        }
    }





}
