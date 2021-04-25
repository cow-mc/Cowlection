package de.cowtipper.cowlection.chesttracker;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

@SuppressWarnings("unused")
public class HyBazaarData {
    private boolean success;
    private long lastUpdated;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<String, Product> products;

    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns {@link Product} from bazaar reply.
     * Returns null if product does not exist
     *
     * @param productId product in bazaar
     * @return instance of Product
     */
    public Product getProduct(String productId) {
        return products.get(productId);
    }

    /**
     * Refresh only allowed once per minute
     */
    public boolean allowRefreshData() {
        return (System.currentTimeMillis() - lastUpdated) > 60000;
    }

    public static class Product {
        @SerializedName("quick_status")
        private Status quickStatus;

        public double getInstantSellPrice() {
            return quickStatus.getSellPrice();
        }

        public double getSellOfferPrice() {
            return quickStatus.getBuyPrice();
        }

        public static class Status {
            private double sellPrice;
            private double buyPrice;

            public double getSellPrice() {
                return sellPrice;
            }

            public double getBuyPrice() {
                return buyPrice;
            }
        }
    }
}
