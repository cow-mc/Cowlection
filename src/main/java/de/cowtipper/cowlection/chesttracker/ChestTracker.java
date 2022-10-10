package de.cowtipper.cowlection.chesttracker;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.chesttracker.data.HyBazaarData;
import de.cowtipper.cowlection.chesttracker.data.HyItemsData;
import de.cowtipper.cowlection.chesttracker.data.ItemData;
import de.cowtipper.cowlection.chesttracker.data.LowestBinsCache;
import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.data.HySkyBlockStats;
import de.cowtipper.cowlection.util.ApiUtils;
import de.cowtipper.cowlection.util.GsonUtils;
import de.cowtipper.cowlection.util.MooChatComponent;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class ChestTracker {
    public static long lastBazaarUpdate;
    public static long lastLowestBinsUpdate;
    public static long lastNpcSellUpdate;
    private final Map<BlockPos, List<ItemStack>> chestCache = new HashMap<>();
    private final Map<BlockPos, EnumFacing> doubleChestCache = new HashMap<>();
    private final Set<BlockPos> chestsWithWantedItem = new HashSet<>();
    private final Set<String> hiddenItems = new HashSet<>();
    private Map<String, ItemData> analysisResult = new HashMap<>();
    private ChestInteractionListener chestInteractionListener;
    private HyBazaarData bazaarCache;
    private LowestBinsCache lowestBinsCache;
    private Map<String, Double> npcSellCache;
    private final Cowlection main;

    public ChestTracker(Cowlection main) {
        this.main = main;
        refreshPriceCache();
        chestInteractionListener = new ChestInteractionListener(main);
        MinecraftForge.EVENT_BUS.register(chestInteractionListener);
    }

    public void analyzeResults() {
        Map<String, ItemData> itemCounts = new HashMap<>();
        for (List<ItemStack> chestContents : chestCache.values()) {
            for (ItemStack item : chestContents) {
                String key = item.hasDisplayName() ? item.getDisplayName() : item.getUnlocalizedName();

                boolean isAmbiguousItem = false;
                if (item.hasTagCompound()) {
                    key = item.getTagCompound().getCompoundTag("ExtraAttributes").getString("id");
                }
                if ("PET".equals(key)) {
                    HySkyBlockStats.Profile.Pet petInfo = GsonUtils.fromJson(item.getTagCompound().getCompoundTag("ExtraAttributes").getString("petInfo"), HySkyBlockStats.Profile.Pet.class);
                    key = petInfo.getType() + ";" + petInfo.getRarity().ordinal();
                    // remove pet lvl from name, as lowest BINs also disregard it
                    String petName = item.getDisplayName();
                    int endOfPetLevel = petName.indexOf(']');
                    if (petName.startsWith(EnumChatFormatting.GRAY + "[Lvl ") && endOfPetLevel > 0) {
                        item.setStackDisplayName(EnumChatFormatting.GRAY + "[Lvl " + EnumChatFormatting.DARK_GRAY + "?" + EnumChatFormatting.GRAY + petName.substring(endOfPetLevel));
                    }
                } else if (DataHelper.AMBIGUOUS_ITEM_IDS.contains(key)) {
                    isAmbiguousItem = true;
                    key += "_ambiguous";
                }

                ItemData itemData = itemCounts.get(key);
                if (itemData == null) {
                    // item hasn't been cached yet
                    if (isAmbiguousItem) {
                        convertToDummyItem(item, key);
                    }
                    itemData = new ItemData(key, item.copy());
                }
                itemCounts.put(key, itemData.addAmount(item.stackSize));
            }
        }
        this.analysisResult = itemCounts;
    }

    private void convertToDummyItem(ItemStack item, String key) {
        NBTTagCompound itemNbtDisplay = item.getSubCompound("display", true);
        NBTTagList loreList = new NBTTagList();
        loreList.appendTag(new NBTTagString("" + EnumChatFormatting.RED + EnumChatFormatting.ITALIC + "This ambiguous item type"));
        loreList.appendTag(new NBTTagString("" + EnumChatFormatting.RED + EnumChatFormatting.ITALIC + "is not listed separately."));
        itemNbtDisplay.setTag("Lore", loreList);
        String itemName = null;
        switch (key) {
            case "ENCHANTED_BOOK_ambiguous":
                itemName = "Enchanted Book";
                break;
            case "POTION_ambiguous":
                itemName = "Potion";
                break;
            case "RUNE_ambiguous":
                itemName = "Rune";
                NBTTagCompound skullNbtTextureData = item.getSubCompound("SkullOwner", false);
                if (skullNbtTextureData != null) {
                    skullNbtTextureData.setString("Id", UUID.randomUUID().toString());
                    NBTTagCompound nbtSkin = new NBTTagCompound();
                    // set texture to Empty Rune
                    nbtSkin.setString("Value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODJiODIwN2E1ZmUxOTJjZDQ3N2U5MjE0NjYxOTdjOGFmNzQ5YWYxOGRkMWVmMzg5ZTI3MzNhMmY3NGQwOTI4YiJ9fX0=");
                    skullNbtTextureData.getCompoundTag("Properties").getTagList("textures", Constants.NBT.TAG_COMPOUND).set(0, nbtSkin);
                }
                break;
            case "NEW_YEAR_CAKE_ambiguous":
                itemName = "New Year Cake";
                break;
            case "SPOOKY_PIE_ambiguous":
                itemName = "Spooky Pie";
                break;
            case "CAKE_SOUL_ambiguous":
                itemName = "Cake Soul";
                break;
        }
        if (itemName != null) {
            item.setStackDisplayName(EnumChatFormatting.GRAY + itemName);
        }
    }

    /**
     * Returns ordered analysis result with prices
     */
    public List<ItemData> getAnalysisResult(String searchQuery, ChestOverviewGui.Column orderBy, boolean orderDesc, EnumSet<ItemData.PriceType> visiblePriceTypes, boolean useInstantSellPrices) {
        List<ItemData> orderedAnalysisResult = new ArrayList<>();

        boolean checkBazaarPrices = bazaarCache != null && bazaarCache.isSuccess() && visiblePriceTypes.contains(ItemData.PriceType.BAZAAR);
        boolean checkLowestBinPrices = lowestBinsCache != null && lowestBinsCache.size() > 0 && visiblePriceTypes.contains(ItemData.PriceType.LOWEST_BIN);
        boolean checkNpcSellPrices = npcSellCache != null && npcSellCache.size() > 0 && visiblePriceTypes.contains(ItemData.PriceType.NPC_SELL);

        boolean hasSearchQuery = StringUtils.isNotEmpty(searchQuery);
        for (Map.Entry<String, ItemData> itemEntry : analysisResult.entrySet()) {
            ItemData itemData = itemEntry.getValue();

            if (hasSearchQuery
                    && !StringUtils.containsIgnoreCase(itemData.getKey(), searchQuery)
                    && !StringUtils.containsIgnoreCase(EnumChatFormatting.getTextWithoutFormattingCodes(itemData.getName()), searchQuery)) {
                // item doesn't match search query
                continue;
            }
            boolean foundPriceForItem = false;

            if (checkBazaarPrices) {
                String productKey = itemEntry.getKey();
                HyBazaarData.Product product = bazaarCache.getProduct(productKey);
                if (product != null) {
                    // item is sold on bazaar!
                    itemData.setBazaarInstantSellPrice(product.getInstantSellPrice());
                    itemData.setBazaarSellOfferPrice(product.getSellOfferPrice());
                    foundPriceForItem = true;
                }
            }
            if (!foundPriceForItem && checkLowestBinPrices) {
                String productKey = itemEntry.getKey().replace(':', '-');
                Integer lowestBin = lowestBinsCache.get(productKey);
                if (lowestBin != null) {
                    // item is sold via BIN
                    itemData.setLowestBin(lowestBin);
                    foundPriceForItem = true;
                }
            }
            if (!foundPriceForItem && checkNpcSellPrices) {
                String productKey = itemEntry.getKey();
                Double npcSellPrice = npcSellCache.get(productKey);
                if (npcSellPrice != null) {
                    // item can be sold to NPC
                    itemData.setNpcPrice(npcSellPrice);
                    // foundPriceForItem = true;
                }
            }
            itemData.setHidden(hiddenItems.contains(itemData.getKey()));
            orderedAnalysisResult.add(itemData);
        }
        Comparator<ItemData> comparator;
        switch (orderBy) {
            case PRICE_TYPE:
                comparator = Comparator.comparing(ItemData::getPriceType).reversed();
                break;
            case ITEM_NAME:
                comparator = Comparator.comparing(ItemData::getName);
                break;
            case ITEM_AMOUNT:
                comparator = Comparator.comparing(ItemData::getAmount);
                break;
            case PRICE_EACH:
                comparator = Comparator.comparing(itemData -> itemData.getPrice(useInstantSellPrices));
                break;
            default: // case PRICE_SUM:
                comparator = Comparator.comparing(itemData -> itemData.getPriceSum(useInstantSellPrices));
                break;
        }
        orderedAnalysisResult.sort((orderDesc ? comparator.reversed() : comparator).thenComparing(ItemData::getName));
        return orderedAnalysisResult;
    }

    public Set<BlockPos> getCachedPositions() {
        return chestCache.keySet();
    }

    public Set<BlockPos> getChestsWithWantedItem() {
        return chestsWithWantedItem;
    }

    public void clear() {
        MinecraftForge.EVENT_BUS.unregister(chestInteractionListener);
        chestInteractionListener = null;
        bazaarCache = null;
        lowestBinsCache = null;
        chestCache.clear();
        doubleChestCache.clear();
        chestsWithWantedItem.clear();
        hiddenItems.clear();
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
        chestsWithWantedItem.remove(mainChestPos);
    }

    private boolean isOtherChestCached(BlockPos chestPos, EnumFacing otherChestFacing) {
        BlockPos otherChestPos = chestPos.offset(otherChestFacing);
        return chestCache.containsKey(otherChestPos);
    }

    public EnumFacing getOtherChestFacing(BlockPos pos) {
        return doubleChestCache.getOrDefault(pos, EnumFacing.UP);
    }

    public EnumSet<ItemData.PriceType> refreshPriceCache() {
        EnumSet<ItemData.PriceType> updating = EnumSet.noneOf(ItemData.PriceType.class);
        if (allowUpdateBazaar()) {
            updating.add(ItemData.PriceType.BAZAAR);
            ApiUtils.fetchBazaarData(bazaarData -> {
                if (bazaarData == null || !bazaarData.isSuccess()) {
                    main.getChatHelper().sendMessage(new MooChatComponent("Error: Couldn't get Bazaar data from Hypixel API! API might be down: check status.hypixel.net").red().setUrl("https://status.hypixel.net/"));
                }
                this.bazaarCache = bazaarData;
                lastBazaarUpdate = System.currentTimeMillis();
            });
        }
        if (allowUpdateLowestBins()) {
            updating.add(ItemData.PriceType.LOWEST_BIN);
            ApiUtils.fetchLowestBins(lowestBins -> {
                if (!lowestBins.hasData()) {
                    main.getChatHelper().sendMessage(new MooChatComponent("Error: Couldn't get lowest BINs from Moulberry's API! API might be down: check if " + ApiUtils.LOWEST_BINS + " is reachable.").red().setUrl(ApiUtils.LOWEST_BINS));
                }
                this.lowestBinsCache = lowestBins;
                lastLowestBinsUpdate = System.currentTimeMillis();
            });
        }
        if (allowUpdateNpcSell()) {
            updating.add(ItemData.PriceType.NPC_SELL);
            ApiUtils.fetchItemsData(itemsData -> {
                this.npcSellCache = new HashMap<>();
                if (itemsData == null || !itemsData.isSuccess()) {
                    main.getChatHelper().sendMessage(new MooChatComponent("Error: Couldn't get Items data from Hypixel API! API might be down: check status.hypixel.net").red().setUrl("https://status.hypixel.net/"));
                } else {
                    for (HyItemsData.Item item : itemsData.getItems()) {
                        if (item.getNpcSellPrice() > 0) {
                            // item has a NPC sell price
                            this.npcSellCache.put(item.getId(), item.getNpcSellPrice());
                        }
                    }
                }
                lastNpcSellUpdate = System.currentTimeMillis();
            });
        }
        return updating;
    }

    /**
     * Allow bazaar update once per minute
     */
    public boolean allowUpdateBazaar() {
        return bazaarCache == null || bazaarCache.allowRefreshData();
    }

    /**
     * Allow lowest bins update once every 5 minutes
     */
    public boolean allowUpdateLowestBins() {
        return lowestBinsCache == null || (System.currentTimeMillis() - lastLowestBinsUpdate) > 300000;
    }

    /**
     * Allow NPC sell prices update once every 15 minutes
     */
    public boolean allowUpdateNpcSell() {
        return npcSellCache == null || (System.currentTimeMillis() - lastNpcSellUpdate) > 900000;
    }

    public boolean allowAnyPriceUpdate() {
        return allowUpdateBazaar() || allowUpdateLowestBins() || allowUpdateNpcSell();
    }

    public void markChestsWithWantedItem(String sbKey, int amount, String itemName) {
        // clear old search results
        chestsWithWantedItem.clear();

        Map<BlockPos, Integer> chestsWithWantedItemsAndCount = new TreeMap<>();

        if (sbKey.endsWith("_ambiguous")) {
            sbKey = sbKey.substring(0, sbKey.length() - 10);
        }
        int mostWantedItemsInOneChest = 0;
        for (Map.Entry<BlockPos, List<ItemStack>> chestCache : chestCache.entrySet()) {
            int foundWantedItemsInChest = 0;
            for (ItemStack item : chestCache.getValue()) {
                String key = item.hasDisplayName() ? item.getDisplayName() : item.getUnlocalizedName();
                if (item.hasTagCompound()) {
                    key = item.getTagCompound().getCompoundTag("ExtraAttributes").getString("id");
                }
                if (sbKey.equals(key)) {
                    foundWantedItemsInChest += item.stackSize;
                }
            }
            if (foundWantedItemsInChest > 0) {
                // chest was a match!
                chestsWithWantedItemsAndCount.put(chestCache.getKey(), foundWantedItemsInChest);
                if (foundWantedItemsInChest > mostWantedItemsInOneChest) {
                    mostWantedItemsInOneChest = foundWantedItemsInChest;
                }
            }
            amount -= foundWantedItemsInChest;
            if (amount <= 0) {
                // already found all relevant chests
                break;
            }
        }

        int relevantChestCount = chestsWithWantedItemsAndCount.size();
        int relevantChestsCoordsLimit = 30;
        int maxItemCountLength = Utils.formatNumber(mostWantedItemsInOneChest).length();
        final MooChatComponent relevantChestCoordsHover = new MooChatComponent("Chests with ").gold().bold().appendSibling(new MooChatComponent(itemName).reset())
                .appendFreshSibling(new MooChatComponent(StringUtils.repeat(' ', maxItemCountLength) + "        (x | y | z)").gray());

        chestsWithWantedItemsAndCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(relevantChestsCoordsLimit)
                .forEach(wantedChest -> {
                    BlockPos chestPos = wantedChest.getKey();
                    String itemCountInChest = StringUtils.leftPad(Utils.formatNumber(wantedChest.getValue()), maxItemCountLength, ' ');

                    relevantChestCoordsHover.appendFreshSibling(new MooChatComponent(" " + EnumChatFormatting.YELLOW + itemCountInChest + EnumChatFormatting.DARK_GRAY + "x ➜ "
                            + EnumChatFormatting.WHITE + chestPos.getX() + EnumChatFormatting.DARK_GRAY
                            + " | " + EnumChatFormatting.WHITE + chestPos.getY() + EnumChatFormatting.DARK_GRAY
                            + " | " + EnumChatFormatting.WHITE + chestPos.getZ()).white());
                });
        if (relevantChestCount > relevantChestsCoordsLimit) {
            relevantChestCoordsHover.appendFreshSibling(new MooChatComponent("      + " + (relevantChestCount - relevantChestsCoordsLimit) + " more chests").gray());
        }
        main.getChatHelper().sendMessage(new MooChatComponent("Chest Tracker & Analyzer is now highlighting " + EnumChatFormatting.LIGHT_PURPLE + relevantChestCount + EnumChatFormatting.GREEN + " chest" + (relevantChestCount > 1 ? "s" : "") + " with " + itemName
                + EnumChatFormatting.DARK_GREEN + " [hover for coords, click to re-open GUI]").green()
                .setHover(relevantChestCoordsHover)
                .setSuggestCommand("/moo analyzeChests", false));
        chestsWithWantedItem.addAll(chestsWithWantedItemsAndCount.keySet());
    }

    public void toggleHiddenStateForItem(String key) {
        boolean removed = hiddenItems.remove(key);
        if (!removed) {
            hiddenItems.add(key);
        }
    }
}
