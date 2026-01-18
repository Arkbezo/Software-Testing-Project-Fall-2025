package IntegrationTesting;

import org.junit.*;

import java.sql.*;
import java.util.UUID;

import static org.junit.Assert.*;

public class ProductCategoryIntegrationTest {

    private Connection conn;

    private int categoryId;
    private int productId;

    private String categoryName;
    private String productName;

    @Before
    public void setUp() throws Exception {
        conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/test_pos",
                "root",
                "centralcee23"
        );

        categoryName = "CAT_" + UUID.randomUUID().toString().substring(0, 6);
        productName  = "PROD_" + UUID.randomUUID().toString().substring(0, 6);

        // Insert Category (parent)
        String insertCategory = "INSERT INTO categories (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insertCategory, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categoryName);
            int rows = ps.executeUpdate();
            assertEquals(1, rows);

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                categoryId = keys.getInt(1);
            }
        }
    }

    @Test
    public void testInsertAndRetrieveProduct_withValidCategory_shouldPass() throws Exception {
        // Insert Product referencing valid category
        String insertProduct = "INSERT INTO products (name, price, category_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertProduct, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, productName);
            ps.setDouble(2, 99.99);
            ps.setInt(3, categoryId);

            int rows = ps.executeUpdate();
            assertEquals(1, rows);

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                productId = keys.getInt(1);
            }
        }

        // Retrieve and validate
        String select = "SELECT name, price, category_id FROM products WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(productName, rs.getString("name"));
                assertEquals(99.99, rs.getDouble("price"), 0.0001);
                assertEquals(categoryId, rs.getInt("category_id"));
            }
        }
    }

    @Test
    public void testInsertProduct_withInvalidCategory_shouldFail() throws Exception {
        int invalidCategoryId = 999999;

        String insertProduct = "INSERT INTO products (name, price, category_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertProduct)) {
            ps.setString(1, "BAD_" + productName);
            ps.setDouble(2, 10.0);
            ps.setInt(3, invalidCategoryId);

            ps.executeUpdate();
            fail("Expected SQLException due to foreign key violation, but insert succeeded.");
        } catch (SQLException ex) {
            String msg = ex.getMessage().toLowerCase();
            assertTrue(msg.contains("foreign key") || msg.contains("constraint"));
        }
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null) {
            // delete child first
            if (productId != 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
                    ps.setInt(1, productId);
                    ps.executeUpdate();
                }
            }

            // delete parent
            if (categoryId != 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
                    ps.setInt(1, categoryId);
                    ps.executeUpdate();
                }
            }

            conn.close();
        }
    }
}