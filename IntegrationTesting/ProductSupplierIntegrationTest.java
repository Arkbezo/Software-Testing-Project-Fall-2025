package IntegrationTesting;

import dal.DALManager;
import model.dto.CategoryDTO;
import model.dto.ProductDTO;
import model.dto.Response;
import model.dto.SupplierDTO;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ProductSupplierIntegrationTest {

    private DALManager dalManager;

    private SupplierDTO supplier;
    private ProductDTO product;

    private String uniqueSupplierName;
    private String uniqueProductName;


    private static final String DB_URL  = "jdbc:mysql://localhost:3306/pos";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "centralcee23";

    @BeforeEach
    void setUp() {
        dalManager = new DALManager();

        String uid = UUID.randomUUID().toString().substring(0, 6);
        uniqueSupplierName = "IntSupp_" + uid;
        uniqueProductName  = "IntProd_" + uid;

        // 1) INSERT SUPPLIER (DAL)
        supplier = new SupplierDTO();
        supplier.setName(uniqueSupplierName);
        supplier.setPhoneNumber("0691111111");

        Response res = new Response();
        dalManager.saveSupplier(supplier, res);
        assertTrue(res.isSuccessfull(), "Supplier insert failed: " + res.getErrorMessages());

        // 2) SYNC SUPPLIER ID (DAL)
        syncSupplierIdFromDb();
        assertTrue(supplier.getId() > 0, "Supplier ID should be > 0 after sync");
    }

    @AfterEach
    void tearDown() {
        // IMPORTANT: delete product first (product references suppliers_id)
        if (product != null && product.getProductId() > 0) {
            Response res = new Response();
            dalManager.deleteProduct(product, res);
            assertTrue(res.isSuccessfull(), "Product delete failed: " + res.getErrorMessages());
        }

        if (supplier != null && supplier.getId() > 0) {
            Response res = new Response();
            dalManager.deleteSupplier(supplier, res);
            assertTrue(res.isSuccessfull(), "Supplier delete failed: " + res.getErrorMessages());
        }
    }

    @Test
    void insertSupplier_insertProduct_search_verifySupplierLink_withDBCheck_cleanup() throws Exception {

        // 3) GET VALID CATEGORY ID (DAL)
        int categoryId = getAnyCategoryId();
        assertTrue(categoryId > 0, "categoryId must be > 0");

        // 4) INSERT PRODUCT LINKED TO SUPPLIER (DAL)
        product = new ProductDTO();
        product.setProductName(uniqueProductName);
        product.setBarcode("BC" + UUID.randomUUID().toString().substring(0, 8));
        product.setPrice(99.99);
        product.setStockQuantity(10);
        product.setCategoryId(categoryId);

        // DTO uses supplierId, DB column is suppliers_id (your DAL should map it)
        product.setSupplierId(supplier.getId());

        // must match your DB constraints; adjust if your system expects other values
        product.setQuantityType("counted");

        Response resInsert = new Response();
        dalManager.addProduct(product, resInsert);
        assertTrue(resInsert.isSuccessfull(), "Product insert failed: " + resInsert.getErrorMessages());

        // 5) SEARCH PRODUCT (DAL)
        Response resSearch = new Response();
        ArrayList<ProductDTO> results = dalManager.searchProductsByName("IntProd_", resSearch);

        assertTrue(resSearch.isSuccessfull(), "Product search failed: " + resSearch.getErrorMessages());
        assertNotNull(results, "Search results should not be null");
        assertFalse(results.isEmpty(), "Search results should not be empty");

        ProductDTO found = results.stream()
                .filter(p -> uniqueProductName.equals(p.getProductName()))
                .findFirst()
                .orElse(null);

        assertNotNull(found, "Inserted product not found in search results");

        // 6) SYNC PRODUCT ID FOR CLEANUP
        product.setProductId(found.getProductId());
        assertTrue(product.getProductId() > 0, "Product ID should be > 0 after sync");

        // 7) VERIFY SUPPLIER LINK USING REAL DB COLUMNS (source of truth)
        int supplierIdInDb = fetchSupplierIdFromDbByProductId(product.getProductId());
        assertEquals(supplier.getId(), supplierIdInDb,
                "DB suppliers_id should match inserted supplier id");
    }

    // -------------------- Helpers --------------------

    private void syncSupplierIdFromDb() {
        Response res = new Response();
        ArrayList<SupplierDTO> suppliers = dalManager.getSuppliers(res);

        assertTrue(res.isSuccessfull(), "Get suppliers failed: " + res.getErrorMessages());
        assertNotNull(suppliers, "Suppliers list is null");

        SupplierDTO found = suppliers.stream()
                .filter(s -> uniqueSupplierName.equals(s.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(found, "Could not sync supplier ID from DB");
        supplier.setId(found.getId());
    }

    private int getAnyCategoryId() {
        Response res = new Response();
        ArrayList<CategoryDTO> categories = dalManager.getCategories(res);

        assertTrue(res.isSuccessfull(), "Get categories failed: " + res.getErrorMessages());
        assertNotNull(categories, "Categories list is null");
        assertFalse(categories.isEmpty(), "No categories in DB. Insert at least one category first.");


        try {
            return (int) categories.get(0).getClass().getMethod("getId").invoke(categories.get(0));
        } catch (Exception e) {
            throw new RuntimeException("CategoryDTO does not have getId(). Adjust getAnyCategoryId().", e);
        }
    }

    /**
     * Uses your REAL schema from DESCRIBE products:
     * - PK column: id
     * - supplier FK column: suppliers_id
     */
    private int fetchSupplierIdFromDbByProductId(int productId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

            String sql = "SELECT suppliers_id FROM products WHERE id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, productId);

                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "No product row found for id=" + productId);
                    return rs.getInt("suppliers_id");
                }
            }
        }
    }
}
