package IntegrationTesting;

import java.util.Map;

public class CheckoutService {

    private final ProductDALStub productDal;
    private final SaleDALStub saleDal;

    public CheckoutService(ProductDALStub productDal, SaleDALStub saleDal) {
        this.productDal = productDal;
        this.saleDal = saleDal;
    }

    // items: productId -> quantity
    public double checkout(Map<Integer, Integer> items) {
        // 1) Validate stock
        for (Map.Entry<Integer, Integer> e : items.entrySet()) {
            int productId = e.getKey();
            int qty = e.getValue();
            if (qty <= 0) throw new IllegalArgumentException("Quantity must be > 0");
            if (productDal.getStock(productId) < qty) {
                throw new IllegalStateException("Insufficient stock for product " + productId);
            }
        }

        // 2) Calculate total + update stock
        double total = 0.0;
        for (Map.Entry<Integer, Integer> e : items.entrySet()) {
            int productId = e.getKey();
            int qty = e.getValue();
            total += productDal.getPrice(productId) * qty;
            productDal.decreaseStock(productId, qty);
        }

        // 3) Save sale (stubbed persistence)
        saleDal.saveSale(total);

        return total;
    }
}
