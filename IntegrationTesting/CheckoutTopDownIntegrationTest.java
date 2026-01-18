package IntegrationTesting;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CheckoutTopDownIntegrationTest {

    @Test
    public void checkout_validItems_updatesStock_andSavesSale() {
        // Arrange (stubs)
        ProductDALStub productDal = new ProductDALStub();
        SaleDALStub saleDal = new SaleDALStub();

        // Seed products: id, price, stock
        productDal.seedProduct(101, 50.0, 10); // product 101: price 50, stock 10
        productDal.seedProduct(202, 20.0, 5);  // product 202: price 20, stock 5

        CheckoutService service = new CheckoutService(productDal, saleDal);

        Map<Integer, Integer> cart = new HashMap<>();
        cart.put(101, 2); // 2 * 50
        cart.put(202, 1); // 1 * 20

        // Act
        double total = service.checkout(cart);

        // Assert total
        assertEquals(120.0, total, 0.0001);

        // Assert stock updated
        assertEquals(8, productDal.getStock(101)); // 10 - 2
        assertEquals(4, productDal.getStock(202)); // 5 - 1

        // Assert sale saved
        assertTrue(saleDal.isSaved());
        assertEquals(120.0, saleDal.getSavedTotal(), 0.0001);
    }

    @Test
    public void checkout_insufficientStock_throwsException_andDoesNotSaveSale() {
        ProductDALStub productDal = new ProductDALStub();
        SaleDALStub saleDal = new SaleDALStub();

        productDal.seedProduct(101, 50.0, 1); // only 1 in stock
        CheckoutService service = new CheckoutService(productDal, saleDal);

        Map<Integer, Integer> cart = new HashMap<>();
        cart.put(101, 2); // request 2 -> should fail

        try {
            service.checkout(cart);
            fail("Expected exception due to insufficient stock");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("Insufficient stock"));
        }

        assertFalse("Sale should not be saved when checkout fails", saleDal.isSaved());
        assertEquals(1, productDal.getStock(101)); // stock unchanged
    }
}
