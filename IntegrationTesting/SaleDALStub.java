package IntegrationTesting;

public class SaleDALStub {

    private boolean saved = false;
    private double savedTotal = 0.0;

    public void saveSale(double total) {
        saved = true;
        savedTotal = total;
    }

    public boolean isSaved() {
        return saved;
    }

    public double getSavedTotal() {
        return savedTotal;
    }
}
