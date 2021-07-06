package de.cowtipper.cowlection.chesttracker;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class ItemData {
    private final String key;
    private final ItemStack itemStack;
    private final String name;
    private int amount;
    private double bazaarInstantSellPrice = 0;
    private double bazaarSellOfferPrice = 0;
    private int lowestBin = 0;
    private PriceType priceType;

    public ItemData(String key, ItemStack itemStack) {
        this.key = key;
        this.itemStack = itemStack;
        this.itemStack.stackSize = 1;
        this.name = itemStack.getDisplayName();
        this.amount = 0;
        this.priceType = PriceType.NONE;
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

    public double getPrice(boolean useInstantSellPrices) {
        switch (priceType) {
            case BAZAAR:
                return useInstantSellPrices ? bazaarInstantSellPrice : bazaarSellOfferPrice;
            case LOWEST_BIN:
                return lowestBin;
            default:
                return 0;
        }
    }

    public double getPriceSum(boolean useInstantSellPrices) {
        switch (priceType) {
            case BAZAAR:
                return useInstantSellPrices ? getBazaarInstantSellValue() : getBazaarSellOfferValue();
            case LOWEST_BIN:
                return getLowestBinValue();
            default:
                return 0;
        }
    }

    public void setBazaarInstantSellPrice(double bazaarInstantSellPrice) {
        this.bazaarInstantSellPrice = bazaarInstantSellPrice;
        this.priceType = PriceType.BAZAAR;
    }

    public void setBazaarSellOfferPrice(double bazaarSellOfferPrice) {
        this.bazaarSellOfferPrice = bazaarSellOfferPrice;
        this.priceType = PriceType.BAZAAR;
    }

    public void setLowestBin(int lowestBin) {
        this.lowestBin = lowestBin;
        this.priceType = PriceType.LOWEST_BIN;
    }

    public ItemData addAmount(int stackSize) {
        this.amount += stackSize;
        return this;
    }

    public double getBazaarInstantSellValue() {
        return amount * bazaarInstantSellPrice;
    }

    public double getBazaarSellOfferValue() {
        return amount * bazaarSellOfferPrice;
    }

    public long getLowestBinValue() {
        return (long) amount * lowestBin;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public String toCopyableFormat() {
        return "\n" + EnumChatFormatting.getTextWithoutFormattingCodes(name) + "\t" + name + "\t" + amount + "\t" + toCopyableFormat(bazaarInstantSellPrice) + "\t" + toCopyableFormat(getBazaarInstantSellValue()) + "\t" + toCopyableFormat(bazaarSellOfferPrice) + "\t" + toCopyableFormat(getBazaarSellOfferValue()) + "\t" + toCopyableFormat(lowestBin) + "\t" + toCopyableFormat(getLowestBinValue());
    }

    private String toCopyableFormat(double value) {
        return value > 0 ? Long.toString(Math.round(value)) : "";
    }

    public enum PriceType {
        BAZAAR, LOWEST_BIN, NONE
    }
}
