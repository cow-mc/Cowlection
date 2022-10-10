package de.cowtipper.cowlection.chesttracker.data;

import java.util.List;

@SuppressWarnings("unused")
public class HyItemsData {
    private boolean success;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private List<Item> items;

    public boolean isSuccess() {
        return success;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class Item {
        private String id;
        private double npc_sell_price;

        public String getId() {
            return id;
        }

        public double getNpcSellPrice() {
            return npc_sell_price;
        }
    }
}
