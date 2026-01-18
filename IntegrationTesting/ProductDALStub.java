package IntegrationTesting;

import java.util.HashMap;
import java.util.Map;

public class ProductDALStub {

    private final Map<Integer, Integer> stockByProductId = new HashMap<>();
    private final Map<Integer, Double> priceByProductId = new HashMap<>();

    public void seedProduct(int productId, double price, int stock) {
        priceByProductId.put(productId, price);
        stockByProductId.put(productId, stock);
    }

    public int getStock(int productId) {
        return stockByProductId.getOrDefault(productId, 0);
    }

    public double getPrice(int productId) {
        return priceByProductId.getOrDefault(productId, 0.0);
    }

    public void decreaseStock(int productId, int qty) {
        int current = getStock(productId);
        stockByProductId.put(productId, current - qty);
    }
}
