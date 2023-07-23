package de.cowtipper.cowlection.chesttracker.data;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class ItemData {
    private final String key;
    private final ItemStack itemStack;
    private final String name;
    private int amount;
    private double bazaarInstantSellPrice = 0;
    private double bazaarSellOfferPrice = 0;
    private long lowestBin = 0;
    private double npcPrice = 0;
    private PriceType priceType;
    private boolean isHidden = false;

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
            case NPC_SELL:
                return npcPrice;
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
            case NPC_SELL:
                return getNpcSellValue();
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

    public void setLowestBin(long lowestBin) {
        this.lowestBin = lowestBin;
        this.priceType = PriceType.LOWEST_BIN;
    }

    public void setNpcPrice(double npcPrice) {
        this.npcPrice = npcPrice;
        this.priceType = PriceType.NPC_SELL;
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
        return ((long) amount) * lowestBin;
    }

    public long getNpcSellValue() {
        return (long) Math.floor((long) amount * npcPrice);
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public String toCopyableFormat() {
        return "\n" + EnumChatFormatting.getTextWithoutFormattingCodes(name) + "\t" + name + "\t" + amount + "\t"
                + toCopyableFormat(bazaarInstantSellPrice) + "\t" + toCopyableFormat(getBazaarInstantSellValue()) + "\t"
                + toCopyableFormat(bazaarSellOfferPrice) + "\t" + toCopyableFormat(getBazaarSellOfferValue()) + "\t"
                + toCopyableFormat(lowestBin) + "\t" + toCopyableFormat(getLowestBinValue()) + "\t"
                + toCopyableFormat(npcPrice) + "\t" + toCopyableFormat(getNpcSellValue());
    }

    private String toCopyableFormat(double value) {
        return value > 0 ? Long.toString(Math.round(value)) : "";
    }

    public enum PriceType {
        LOWEST_BIN("BIN"), BAZAAR("BZ"), NPC_SELL("NPC"), NONE("-");

        private final String indicator;

        PriceType(String indicator) {
            this.indicator = indicator;
        }

        public String getIndicator() {
            return indicator;
        }
    }
}
