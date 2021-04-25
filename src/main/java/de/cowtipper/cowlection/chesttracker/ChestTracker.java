package de.cowtipper.cowlection.chesttracker;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.util.ApiUtils;
import de.cowtipper.cowlection.util.MooChatComponent;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;

public class ChestTracker {
    private final Map<BlockPos, List<ItemStack>> chestCache = new HashMap<>();
    private final Map<BlockPos, EnumFacing> doubleChestCache = new HashMap<>();
    private Map<String, ItemData> analysisResult = new HashMap<>();
    private ChestInteractionListener chestInteractionListener;
    private HyBazaarData bazaarCache;
    private long lastBazaarUpdate;
    private final Cowlection main;

    public ChestTracker(Cowlection main) {
        this.main = main;
        refreshBazaarCache();
        chestInteractionListener = new ChestInteractionListener(main);
        MinecraftForge.EVENT_BUS.register(chestInteractionListener);
    }

    public void analyzeResults() {
        Map<String, ItemData> itemCounts = new HashMap<>();
        for (List<ItemStack> chestContents : chestCache.values()) {
            for (ItemStack item : chestContents) {
                String key = item.hasDisplayName() ? item.getDisplayName() : item.getUnlocalizedName();

                if (item.hasTagCompound()) {
                    key = item.getTagCompound().getCompoundTag("ExtraAttributes").getString("id");
                }

                ItemData itemData = itemCounts.get(key);
                if (itemData == null) {
                    itemData = new ItemData(key, item.copy());
                }
                itemCounts.put(key, itemData.addAmount(item.stackSize));
            }
        }
        this.analysisResult = itemCounts;
    }

    /**
     * Returns ordered analysis result with prices
     */
    public List<ItemData> getAnalysisResult(ChestOverviewGui.Column orderBy, boolean orderDesc, boolean useInstantSellPrices) {
        List<ItemData> orderedAnalysisResult = new ArrayList<>();
        // sort by bazaar value (most value first)
        for (Map.Entry<String, ItemData> itemEntry : analysisResult.entrySet()) {
            if (bazaarCache != null && bazaarCache.isSuccess()) {
                String productKey = itemEntry.getKey();
                HyBazaarData.Product product = bazaarCache.getProduct(productKey);
                if (product != null) {
                    // item is sold on bazaar!
                    itemEntry.getValue().setBazaarInstantSellPrice(product.getInstantSellPrice());
                    itemEntry.getValue().setBazaarSellOfferPrice(product.getSellOfferPrice());
                }
            }
            orderedAnalysisResult.add(itemEntry.getValue());
        }
        Comparator<ItemData> comparator;
        switch (orderBy) {
            case ITEM_NAME:
                comparator = Comparator.comparing(ItemData::getName);
                break;
            case ITEM_AMOUNT:
                comparator = Comparator.comparing(ItemData::getAmount);
                break;
            case PRICE_EACH:
                comparator = useInstantSellPrices ? Comparator.comparing(ItemData::getBazaarInstantSellPrice) : Comparator.comparing(ItemData::getBazaarSellOfferPrice);
                break;
            default: // case PRICE_SUM:
                comparator = useInstantSellPrices ? Comparator.comparing(ItemData::getBazaarInstantSellValue) : Comparator.comparing(ItemData::getBazaarSellOfferValue);
                break;
        }
        orderedAnalysisResult.sort((orderDesc ? comparator.reversed() : comparator).thenComparing(ItemData::getName));
        return orderedAnalysisResult;
    }

    public Set<BlockPos> getCachedPositions() {
        return chestCache.keySet();
    }

    public void clear() {
        MinecraftForge.EVENT_BUS.unregister(chestInteractionListener);
        chestInteractionListener = null;
        bazaarCache = null;
        chestCache.clear();
        doubleChestCache.clear();
        analysisResult.clear();
    }

    public void addChest(BlockPos chestPos, List<ItemStack> chestContents, EnumFacing otherChestFacing) {
        if (chestContents.size() > 0) { // check if the chest is a chest we want to cache/analyze
            ItemStack firstItem = chestContents.get(0);
            if (firstItem != null && firstItem.hasDisplayName() && firstItem.getDisplayName().equals(" ") && firstItem.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)) {
                // item in first slot of chest is a glass pane with the display name " ", indicating e.g. a minion chest which we don't want to track
                return;
            }
        }
        BlockPos mainChestPos = chestPos;

        if (otherChestFacing != EnumFacing.UP) { // we have a double chest!
            if (isOtherChestCached(chestPos, otherChestFacing)) { // other chest is cached already, update that one instead
                mainChestPos = chestPos.offset(otherChestFacing);
            }

            if (chestPos.equals(mainChestPos)) {
                doubleChestCache.put(chestPos, otherChestFacing);
            } else {
                doubleChestCache.put(mainChestPos, otherChestFacing.getOpposite());
            }
        }
        chestCache.put(mainChestPos, chestContents);
    }

    public void removeChest(BlockPos chestPos, EnumFacing otherChestFacing) {
        BlockPos mainChestPos = chestPos;

        if (otherChestFacing != EnumFacing.UP) { // we have a double chest!
            if (isOtherChestCached(chestPos, otherChestFacing)) { // other chest is cached already, update that one instead
                mainChestPos = chestPos.offset(otherChestFacing);
            }

            if (chestPos.equals(mainChestPos)) {
                doubleChestCache.remove(chestPos);
            } else {
                doubleChestCache.remove(mainChestPos);
            }
        }
        chestCache.remove(mainChestPos);
    }

    private boolean isOtherChestCached(BlockPos chestPos, EnumFacing otherChestFacing) {
        BlockPos otherChestPos = chestPos.offset(otherChestFacing);
        return chestCache.containsKey(otherChestPos);
    }

    public EnumFacing getOtherChestFacing(BlockPos pos) {
        return doubleChestCache.getOrDefault(pos, EnumFacing.UP);
    }

    public void refreshBazaarCache() {
        if (allowUpdateBazaar()) {
            ApiUtils.fetchBazaarData(bazaarData -> {
                if (bazaarData == null || !bazaarData.isSuccess()) {
                    main.getChatHelper().sendMessage(new MooChatComponent("Error: Couldn't get Bazaar data from Hypixel API! API might be down: check status.hypixel.net").red().setUrl("https://status.hypixel.net/"));
                }
                this.bazaarCache = bazaarData;
                this.lastBazaarUpdate = System.currentTimeMillis();
            });
        }
    }

    public boolean allowUpdateBazaar() {
        return bazaarCache == null || bazaarCache.allowRefreshData();
    }

    public long getLastBazaarUpdate() {
        return this.lastBazaarUpdate;
    }
}
