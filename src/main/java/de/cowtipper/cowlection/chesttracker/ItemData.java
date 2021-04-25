package de.cowtipper.cowlection.chesttracker;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class ItemData {
    private final String key;
    private final ItemStack itemStack;
    private final String name;
    private int amount;
    private double bazaarInstantSellPrice = -1;
    private double bazaarSellOfferPrice = -1;

    public ItemData(String key, ItemStack itemStack) {
        this.key = key;
        this.itemStack = itemStack;
        this.itemStack.stackSize = 1;
        this.name = itemStack.getDisplayName();
        this.amount = 0;
    }

    public String getKey() {
        return key;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getName() {
        return name;
    }

    public int getAmount() {
        return amount;
    }

    public double getBazaarInstantSellPrice() {
        return bazaarInstantSellPrice;
    }

    public void setBazaarInstantSellPrice(double bazaarInstantSellPrice) {
        this.bazaarInstantSellPrice = bazaarInstantSellPrice;
    }

    public double getBazaarSellOfferPrice() {
        return bazaarSellOfferPrice;
    }

    public void setBazaarSellOfferPrice(double bazaarSellOfferPrice) {
        this.bazaarSellOfferPrice = bazaarSellOfferPrice;
    }

    public ItemData addAmount(int stackSize) {
        this.amount += stackSize;
        return this;
    }

    public double getBazaarInstantSellValue() {
        return bazaarInstantSellPrice >= 0 ? amount * bazaarInstantSellPrice : -1;
    }

    public double getBazaarSellOfferValue() {
        return bazaarSellOfferPrice >= 0 ? amount * bazaarSellOfferPrice : -1;
    }

    public String toCopyableFormat() {
        return "\n" + EnumChatFormatting.getTextWithoutFormattingCodes(name) + "\t" + name + "\t" + amount + "\t" + Math.round(getBazaarInstantSellPrice()) + "\t" + Math.round(getBazaarInstantSellValue()) + "\t" + Math.round(getBazaarSellOfferPrice()) + "\t" + Math.round(getBazaarSellOfferValue());
    }
}
