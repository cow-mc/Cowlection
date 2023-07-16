package de.cowtipper.cowlection.listener.skyblock;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.mojang.realmsclient.util.Pair;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.config.gui.MooConfigGui;
import de.cowtipper.cowlection.data.BestiaryEntry;
import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.data.HySkyBlockStats;
import de.cowtipper.cowlection.data.XpTables;
import de.cowtipper.cowlection.util.GsonUtils;
import de.cowtipper.cowlection.util.GuiHelper;
import de.cowtipper.cowlection.util.MooChatComponent;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkyBlockListener {
    private static final Pattern ITEM_COUNT_PREFIXED_PATTERN = Pattern.compile("^(?:§[0-9a-fl-or])*\\d+x ");
    private static final Pattern ITEM_COUNT_SUFFIXED_PATTERN = Pattern.compile(" (?:§[0-9a-fl-or])*x\\d+$");
    private static final Pattern PET_NAME_PATTERN = Pattern.compile("^§7\\[Lvl (\\d+)] (§[0-9a-f])");
    private static final Pattern TIER_SUFFIX_PATTERN = Pattern.compile(" [IVX0-9]+$");
    // example: " §a42§7x §fLeather §7for §6436.8 coins"
    private static final Pattern BAZAAR_SELL_ALL_PATTERN = Pattern.compile("^(?:§[0-9a-fl-or])* (?:§[0-9a-fl-or])+([0-9,]+)(?:§[0-9a-fl-or])+x (?:§[0-9a-fl-or])+.+ (?:§[0-9a-fl-or])+for (?:§[0-9a-fl-or])+([0-9,.]+) coins$");
    private static final Pattern BAZAAR_TARGET_AMOUNT_PATTERN = Pattern.compile("^O(?:ff|rd)er amount: ([\\d,]+)x$");
    private static final Pattern BAZAAR_FILLED_PATTERN = Pattern.compile("^Filled: ([\\d,.k]+)/[\\d,.k]+ \\(?([\\d.]+)%[)!]$");
    List<BestiaryEntry> bestiaryOverview = null;
    private final NumberFormat numberFormatter;
    private final Cowlection main;

    public SkyBlockListener(Cowlection main) {
        this.main = main;
        numberFormatter = NumberFormat.getNumberInstance(Locale.US);
        numberFormatter.setMaximumFractionDigits(0);
    }

    @SubscribeEvent
    public void onItemLookupInGui(GuiScreenEvent.KeyboardInputEvent.Pre e) {
        // open item info (wiki, price: bazaar or auction)
        ItemLookupType itemLookupType;
        if (MooConfig.isLookupWikiKeyBindingPressed()) {
            itemLookupType = ItemLookupType.WIKI;
        } else if (MooConfig.isLookupOfficialWikiKeyBindingPressed()) {
            itemLookupType = ItemLookupType.OFFICIAL_WIKI;
        } else if (MooConfig.isLookupPriceKeyBindingPressed()) {
            itemLookupType = ItemLookupType.PRICE;
        } else {
            itemLookupType = ItemLookupType.INVALID;
        }
        if (itemLookupType != ItemLookupType.INVALID && Keyboard.getEventKeyState() && e.gui instanceof GuiContainer) {
            GuiContainer guiContainer = (GuiContainer) e.gui;
            Slot hoveredSlot = GuiHelper.getSlotUnderMouse(guiContainer);
            if (hoveredSlot != null && hoveredSlot.getHasStack()) {
                String itemBaseName = null;
                String querySuffix = "";

                ItemStack itemStack = hoveredSlot.getStack();
                NBTTagCompound extraAttributes = itemStack.getSubCompound("ExtraAttributes", false);
                String sbId = null;
                if (extraAttributes != null && extraAttributes.hasKey("id")) {
                    // seems to be a SkyBlock item
                    sbId = extraAttributes.getString("id");
                    if (itemLookupType == ItemLookupType.WIKI || itemLookupType == ItemLookupType.OFFICIAL_WIKI || (/* itemLookupType == ItemLookupType.PRICE && */ !DataHelper.AMBIGUOUS_ITEM_IDS.contains(sbId) && !sbId.contains("_GENERATOR_"))) {
                        // open item price info or open wiki entry
                        Pair<String, String> sbItemBaseName = Utils.extractSbItemBaseName(itemStack.getDisplayName(), extraAttributes, false);
                        itemBaseName = sbItemBaseName.first();

                        // exceptions:
                        // remove item count (prefixed or suffixed)
                        Matcher itemCountPrefixMatcher = ITEM_COUNT_PREFIXED_PATTERN.matcher(itemBaseName);
                        Matcher itemCountSuffixMatcher = ITEM_COUNT_SUFFIXED_PATTERN.matcher(itemBaseName);
                        if (itemCountPrefixMatcher.find()) {
                            itemBaseName = itemBaseName.substring(itemCountPrefixMatcher.end());
                        } else if (itemCountSuffixMatcher.find()) {
                            itemBaseName = itemBaseName.substring(0, itemCountSuffixMatcher.start());
                        }
                        // special sb item ids
                        if ("PET".equals(sbId)) {
                            int petLevelPrefix = itemBaseName.indexOf("] ");
                            if (petLevelPrefix > 0) {
                                itemBaseName = itemBaseName.substring(petLevelPrefix + 2);
                            }
                            querySuffix = " Pet";
                        } else if ("CAKE_SOUL".equals(sbId)) {
                            itemBaseName = EnumChatFormatting.LIGHT_PURPLE + "Cake Soul";
                        }
                    } else {
                        // item is blacklisted from lookup
                        main.getChatHelper().sendMessage(EnumChatFormatting.RED, "⚠ " + EnumChatFormatting.RESET + itemStack.getDisplayName() + EnumChatFormatting.RED + " (" + Utils.fancyCase(sbId) + ") " + itemLookupType.getDescription() + " cannot be looked up.");
                        Minecraft.getMinecraft().thePlayer.playSound("mob.villager.no", Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER), 1.4f);
                    }
                } else {
                    // check if item is inside Bazaar
                    List<String> lore = Utils.getItemTooltip(itemStack);
                    if (lore.size() > 5 && lore.get(1).endsWith(" commodity")) {
                        // item is a Bazaar commodity
                        itemBaseName = itemStack.getDisplayName();
                    }
                }
                if (itemBaseName != null) {
                    String link = itemLookupType.buildLink(sbId, EnumChatFormatting.getTextWithoutFormattingCodes(itemBaseName).trim() + querySuffix);
                    if (link == null) {
                        main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Error: Your operating system doesn't support UTF-8? Huh?");
                        return;
                    }
                    main.getChatHelper().sendMessage(new MooChatComponent(EnumChatFormatting.DARK_GREEN + " ➡ "
                            + EnumChatFormatting.GREEN + "Open" + (MooConfig.lookupItemDirectly ? "ing " : " ") + itemLookupType.getDescription() + " for " + itemBaseName).green()
                            .setUrl(link, itemLookupType.description + ": " + EnumChatFormatting.WHITE + link));

                    if (MooConfig.lookupItemDirectly) {
                        boolean success = openLink(link);
                        if (!success) {
                            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Error: couldn't open your browser");
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent e) {
        if (e.itemStack == null || e.toolTip == null || e.entityPlayer == null) {
            return;
        }

        // bazaar graphs enhancements
        if ((MooConfig.getBazaarConnectGraphsNodes() == MooConfig.Setting.ALWAYS
                || MooConfig.getBazaarConnectGraphsNodes() == MooConfig.Setting.SPECIAL && MooConfig.isTooltipToggleKeyBindingPressed())
                && e.itemStack.getItem() == Items.paper) {
            boolean drawGraph = false;
            GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
            if (currentScreen instanceof GuiChest) {
                // some kind of chest
                ContainerChest chestContainer = (ContainerChest) ((GuiChest) currentScreen).inventorySlots;
                IInventory inventory = chestContainer.getLowerChestInventory();
                String inventoryName = (inventory.hasCustomName() ? EnumChatFormatting.getTextWithoutFormattingCodes(inventory.getDisplayName().getUnformattedTextForChat()) : inventory.getName());

                if (inventoryName.contains("➜ G")) {
                    // bazaar interface with graphs
                    drawGraph = true;
                }
            } else if (currentScreen instanceof MooConfigGui) {
                // preview in config gui
                drawGraph = true;
            }
            if (drawGraph) {
                GuiHelper.drawHoveringTextWithGraph(new ArrayList<>(e.toolTip));
                e.toolTip.clear();
                return;
            }
        }

        // show total pet exp
        NBTTagCompound extraAttributes = e.itemStack.getSubCompound("ExtraAttributes", false);
        if ((MooConfig.getTooltipPetExpDisplay() == MooConfig.Setting.ALWAYS
                || MooConfig.getTooltipPetExpDisplay() == MooConfig.Setting.SPECIAL && MooConfig.isTooltipToggleKeyBindingPressed())
                && e.itemStack.getItem() == Items.skull) {
            if (extraAttributes != null && extraAttributes.hasKey("petInfo")) {
                // pet in inventory, auction house or similar
                HySkyBlockStats.Profile.Pet petInfo = GsonUtils.fromJson(extraAttributes.getString("petInfo"), HySkyBlockStats.Profile.Pet.class);
                int index = Math.max(0, e.toolTip.size() - (e.showAdvancedItemTooltips ? /* item name & nbt info */ 2 : 0));
                e.toolTip.add(index, EnumChatFormatting.GRAY + "Pet exp: " + EnumChatFormatting.GOLD + numberFormatter.format(petInfo.getExp()));
            } else if (e.itemStack.getDisplayName().contains("[Lvl ")) {
                // pet in pets menu
                for (int i = e.toolTip.size() - 1; i >= 0; i--) {
                    String loreLine = EnumChatFormatting.getTextWithoutFormattingCodes(e.toolTip.get(i));
                    if (loreLine.startsWith("                    ")) { // exp bar to next level
                        int beginPetExp = loreLine.indexOf(' ');
                        int endPetExp = loreLine.indexOf('/');
                        if (beginPetExp < 0 || endPetExp < 0) {
                            // didn't find pet exp, abort
                            break;
                        }
                        try {
                            int petExp = numberFormatter.parse(loreLine.substring(beginPetExp + 1, endPetExp)).intValue();

                            Matcher petNameMatcher = PET_NAME_PATTERN.matcher(e.itemStack.getDisplayName());
                            if (petNameMatcher.find()) {
                                int petLevel = Integer.parseInt(petNameMatcher.group(1));
                                DataHelper.SkyBlockRarity petRarity = DataHelper.SkyBlockRarity.getPetRarityByColorCode(petNameMatcher.group(2));
                                if (petRarity == null) {
                                    break;
                                }
                                int totalPetExp = XpTables.Pet.getTotalExp(petRarity, petLevel, petExp);
                                e.toolTip.add(i + 1, EnumChatFormatting.GRAY + "Total pet exp: " + EnumChatFormatting.GOLD + numberFormatter.format(totalPetExp));
                            }
                        } catch (ParseException | NumberFormatException ignored) {
                            // do nothing
                        }
                        break;
                    }
                }
            }
        }

        // remove unnecessary tooltip entries: dyed leather armor
        NBTTagCompound nbtDisplay = e.itemStack.getSubCompound("display", false);
        if (!Minecraft.getMinecraft().gameSettings.advancedItemTooltips
                && nbtDisplay != null && nbtDisplay.hasKey("color", Constants.NBT.TAG_INT)) {
            e.toolTip.removeIf(line -> line.equals(EnumChatFormatting.ITALIC + StatCollector.translateToLocal("item.dyed")));
        }

        // remove unnecessary tooltip entries: enchantments (already added via lore)
        NBTTagList enchantments = e.itemStack.getEnchantmentTagList();
        if (enchantments != null) {
            for (int enchantmentNr = 0; enchantmentNr < enchantments.tagCount(); ++enchantmentNr) {
                int enchantmentId = enchantments.getCompoundTagAt(enchantmentNr).getShort("id");
                int enchantmentLevel = enchantments.getCompoundTagAt(enchantmentNr).getShort("lvl");

                if (Enchantment.getEnchantmentById(enchantmentId) != null) {
                    e.toolTip.remove(Enchantment.getEnchantmentById(enchantmentId).getTranslatedName(enchantmentLevel));
                }
            }
        }

        MooConfig.Setting tooltipItemAgeDisplay = MooConfig.getTooltipItemAgeDisplay();
        MooConfig.Setting tooltipItemTimestampDisplay = MooConfig.getTooltipItemTimestampDisplay();

        // add item age to tooltip
        if (extraAttributes != null && extraAttributes.hasKey("timestamp")
                && (tooltipItemAgeDisplay != MooConfig.Setting.DISABLED || tooltipItemTimestampDisplay != MooConfig.Setting.DISABLED)) {
            LocalDateTime skyBlockDateTime;
            try {
                String timestamp = extraAttributes.getString("timestamp");
                if (timestamp.endsWith("M")) {
                    // format: month > day > year + 12 hour clock (AM or PM)
                    skyBlockDateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("M/d/yy h:mm a", Locale.US));
                } else {
                    // format: day > month > year + 24 hour clock (very, very rare)
                    skyBlockDateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("d/M/yy HH:mm", Locale.US));
                }
            } catch (DateTimeParseException ignored) {
                // unknown/invalid timestamp format
                skyBlockDateTime = null;
            }

            if (skyBlockDateTime != null) {
                // Timezone = America/Toronto! headquarter is in Val-des-Monts, Quebec, Canada; timezone can also be confirmed by looking at the timestamps of New Year Cakes
                ZonedDateTime dateTime = ZonedDateTime.of(skyBlockDateTime, ZoneId.of("America/Toronto")); // EDT/EST

                int index = Math.max(0, e.toolTip.size() - (e.showAdvancedItemTooltips ? /* item name & nbt info */ 2 : 0));

                switch (tooltipItemTimestampDisplay) {
                    case SPECIAL:
                        if (!MooConfig.isTooltipToggleKeyBindingPressed()) {
                            break;
                        }
                    case ALWAYS:
                        e.toolTip.add(index, "Timestamp: " + EnumChatFormatting.DARK_GRAY + dateTime.withZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm zzz")));
                        break;
                    default:
                        // do nothing
                        break;
                }
                switch (tooltipItemAgeDisplay) {
                    case SPECIAL:
                        if (!MooConfig.isTooltipToggleKeyBindingPressed()) {
                            break;
                        }
                    case ALWAYS: {
                        long itemCreationTimestamp = dateTime.toEpochSecond() * 1000;
                        long itemAgeInMs = System.currentTimeMillis() - itemCreationTimestamp;

                        String itemAge = itemAgeInMs >= 60_000
                                ? (MooConfig.tooltipItemAgeShortened ? Utils.getDurationAsWord(itemCreationTimestamp) : Utils.getDurationAsWords(itemCreationTimestamp).first())
                                : "<1 minute";
                        e.toolTip.add(index, "Item age: " + EnumChatFormatting.DARK_GRAY + itemAge);
                        break;
                    }
                    default:
                        // do nothing
                        break;
                }
            }
        }

        // bestiary overview:
        String bestiaryOverviewOrder = MooConfig.bestiaryOverviewOrder;
        boolean isBestiaryOverviewVisible = !"hidden".equals(bestiaryOverviewOrder);
        // either use cached bestiary overview or generate overview if trigger item is different
        boolean isDifferentTriggerItem = BestiaryEntry.isDifferentTriggerItem(e.itemStack);
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        if (currentScreen instanceof GuiChest && isBestiaryOverviewVisible && isBestiaryMainGui((GuiChest) currentScreen, e.itemStack)) {
            e.toolTip.add("");
            e.toolTip.add("" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + EnumChatFormatting.UNDERLINE + "Cowlection: Bestiary overview");
            e.toolTip.add(EnumChatFormatting.GRAY + "Select an area below, then hover");
            e.toolTip.add(EnumChatFormatting.GRAY + "over the area icon at the top to see");
            e.toolTip.add(EnumChatFormatting.GRAY + "the " + bestiaryOverviewOrder + " left to the next bestiary tier.");
            e.toolTip.add(EnumChatFormatting.DARK_GRAY + "(Hypixel's bestiary shows " + EnumChatFormatting.ITALIC + "progress");
            e.toolTip.add(EnumChatFormatting.DARK_GRAY + " & Cowlection tooltip shows kills " + EnumChatFormatting.ITALIC + "left" + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "!)");
        } else if (currentScreen instanceof GuiChest && isBestiaryOverviewVisible && isBestiaryGui((GuiChest) currentScreen, e.itemStack)) {
            if (isDifferentTriggerItem) {
                BestiaryEntry.reinitialize(e.itemStack);
                bestiaryOverview = new ArrayList<>();

                ContainerChest chestContainer = (ContainerChest) ((GuiChest) currentScreen).inventorySlots;
                IInventory inventory = chestContainer.getLowerChestInventory();
                for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                    ItemStack item = inventory.getStackInSlot(slot);
                    if (item != null) {
                        // slot + item
                        NBTTagCompound itemNbtDisplay = item.getSubCompound("display", false);
                        if (itemNbtDisplay != null && itemNbtDisplay.hasKey("Lore", Constants.NBT.TAG_LIST)) {
                            NBTTagList loreList = itemNbtDisplay.getTagList("Lore", Constants.NBT.TAG_STRING);

                            if (loreList.tagCount() < 10 || !"§eClick to view!".equals(loreList.getStringTagAt(loreList.tagCount() - 1))) {
                                if (item.getItem() == Items.dye && item.getMetadata() == EnumDyeColor.GRAY.getDyeDamage()) {
                                    // mob hasn't been killed yet
                                    bestiaryOverview.add(new BestiaryEntry(item.getDisplayName()));
                                }
                                // not a bestiary icon with additional data
                                continue;
                            }

                            for (int loreLineNr = 0; loreLineNr < loreList.tagCount(); ++loreLineNr) {
                                String loreLineFormatted = loreList.getStringTagAt(loreLineNr);
                                String loreLine = EnumChatFormatting.getTextWithoutFormattingCodes(loreLineFormatted);
                                if (loreLine.startsWith("                    ")) { // bar to next level
                                    try {
                                        String progress = loreLine.substring(loreLine.lastIndexOf(' ') + 1);
                                        int divider = progress.indexOf('/');
                                        if (divider > 0) {
                                            bestiaryOverview.add(new BestiaryEntry(TIER_SUFFIX_PATTERN.matcher(item.getDisplayName()).replaceFirst(""),
                                                    abbreviatedToLongNumber(progress.substring(0, divider)),
                                                    abbreviatedToLongNumber(progress.substring(divider + 1))));
                                            break;
                                        }
                                    } catch (NumberInvalidException ignored) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (currentScreen instanceof MooConfigGui && isBestiaryOverviewVisible && e.itemStack.getItem() == Items.wheat) {
            if (isDifferentTriggerItem) {
                // bestiary overview preview in config gui
                BestiaryEntry.reinitialize(e.itemStack);
                bestiaryOverview = new ArrayList<>();
                bestiaryOverview.add(new BestiaryEntry(EnumChatFormatting.GREEN + "Cow", 1, 2));
                bestiaryOverview.add(new BestiaryEntry(EnumChatFormatting.GREEN + "Pig", 1163, 2500));
                bestiaryOverview.add(new BestiaryEntry(EnumChatFormatting.GREEN + "Chicken", 10800, 15000));
                bestiaryOverview.add(new BestiaryEntry(EnumChatFormatting.RED + "Farmhand"));
            }
        } else {
            isBestiaryOverviewVisible = false;
        }
        if (bestiaryOverview != null && isBestiaryOverviewVisible) {
            e.toolTip.add("");
            e.toolTip.add("" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + EnumChatFormatting.UNDERLINE + "Bestiary Overview:" + EnumChatFormatting.RESET + EnumChatFormatting.YELLOW + " (ordered by: " + bestiaryOverviewOrder + " to next tier)");

            if (bestiaryOverview.size() == 0) {
                e.toolTip.add(EnumChatFormatting.GREEN + "All mobs max tier " + EnumChatFormatting.DARK_GREEN + "✔");
            } else {
                boolean sortBestiaryOverviewByKills = "fewest kills".equals(bestiaryOverviewOrder);
                // sort by <kills|percentage> left, then alphabetically by mob name
                bestiaryOverview.sort(Comparator.<BestiaryEntry>comparingInt(entry -> (sortBestiaryOverviewByKills) ? entry.getKillsToGo() : entry.getPercentageToGo()).thenComparing(BestiaryEntry::getMobName));
                BestiaryEntry.calculateWidestEntries();

                for (BestiaryEntry bestiaryEntry : bestiaryOverview) {
                    e.toolTip.add(bestiaryEntry.getFormattedOutput(sortBestiaryOverviewByKills));
                }
            }
        }

        // bazaar: sort "Sell <Inventory|Sacks> Now"
        if ((e.itemStack.getItem() == Items.cauldron || e.itemStack.getItem() == Item.getItemFromBlock(Blocks.chest)) && !"unordered".equals(MooConfig.bazaarSellAllOrder)) {
            String hoveredItemName = EnumChatFormatting.getTextWithoutFormattingCodes(e.itemStack.getDisplayName());
            if ("Sell Inventory Now".equals(hoveredItemName) || "Sell Sacks Now".equals(hoveredItemName)) {
                NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.US);
                numberFormatter.setMaximumFractionDigits(1);
                List<String> toolTip = e.toolTip;
                int startIndex = 1337;
                Ordering<Double> tooltipOrdering = Ordering.natural();
                if ("high → low".equals(MooConfig.bazaarSellAllOrderAscDesc)) {
                    tooltipOrdering = tooltipOrdering.reverse();
                }
                TreeMultimap<Double, String> sellEntries = TreeMultimap.create(tooltipOrdering, Ordering.natural());
                for (int i = 0; i < toolTip.size(); i++) {
                    Matcher bazaarSellMatcher = BAZAAR_SELL_ALL_PATTERN.matcher(toolTip.get(i));
                    if (bazaarSellMatcher.matches()) {
                        if (i < startIndex) {
                            startIndex = i;
                        }
                        String amountStr = bazaarSellMatcher.group(1);
                        String priceStr = bazaarSellMatcher.group(2);
                        try {
                            double key;
                            String suffix = "";
                            switch (MooConfig.bazaarSellAllOrder) {
                                case "price (sum)":
                                    key = numberFormatter.parse(priceStr).doubleValue();
                                    break;
                                case "item amount":
                                    key = numberFormatter.parse(amountStr).intValue();
                                    break;
                                case "price (each)":
                                    key = numberFormatter.parse(priceStr).doubleValue() / numberFormatter.parse(amountStr).doubleValue();
                                    suffix = EnumChatFormatting.DARK_GRAY + " (" + numberFormatter.format(key) + " each)";
                                    break;
                                default:
                                    // invalid value, abort!
                                    return;
                            }
                            sellEntries.put(key, toolTip.get(i) + suffix);
                        } catch (ParseException ex) {
                            // abort
                            return;
                        }
                    } else if (startIndex < 1337) {
                        // startIndex has been set; lore line no longer matches regex: reached end of tooltip's 'sell all'-section
                        break;
                    }
                }
                if (sellEntries.size() > 1) {
                    int sellEntryIndex = 0;
                    for (String sellEntry : sellEntries.values()) {
                        e.toolTip.set(startIndex + sellEntryIndex, sellEntry);
                        ++sellEntryIndex;
                    }
                    e.toolTip.add(startIndex + sellEntryIndex, EnumChatFormatting.DARK_GRAY + "  » ordered by " + MooConfig.bazaarSellAllOrder);
                }
            }
        }

        // bazaar: show how many items left on offer/order
        if (MooConfig.bazaarShowItemsLeft) {
            String displayName = e.itemStack.getDisplayName();
            if (displayName.startsWith("" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + "SELL")
                    || displayName.startsWith("" + EnumChatFormatting.GREEN + EnumChatFormatting.BOLD + "BUY")) {
                int targetAmount = -1; // order/offer amount
                List<String> toolTip = e.toolTip;
                for (int lineNr = 2; lineNr < Math.min(5, toolTip.size()); lineNr++) {
                    String line = EnumChatFormatting.getTextWithoutFormattingCodes(toolTip.get(lineNr));
                    Matcher targetAmountMatcher = BAZAAR_TARGET_AMOUNT_PATTERN.matcher(line);
                    Matcher filledMatcher = BAZAAR_FILLED_PATTERN.matcher(line);
                    try {
                        if (targetAmount == -1 && targetAmountMatcher.matches()) {
                            targetAmount = Integer.parseInt(targetAmountMatcher.group(1).replace(",", ""));
                        } else if (targetAmount > 0 && filledMatcher.matches()) {
                            double percentageFilled = Double.parseDouble(filledMatcher.group(2)) / 100;
                            int itemsLeft;
                            if (percentageFilled == 1) {
                                // order already filled 100%
                                break;
                            } else if (filledMatcher.group(1).contains("k")) {
                                // filled amount is abbreviated, use filled % to calculate remaining items
                                itemsLeft = (int) (targetAmount - targetAmount * percentageFilled);
                            } else {
                                int amountFilled = Integer.parseInt(filledMatcher.group(1).replace(",", ""));
                                itemsLeft = targetAmount - amountFilled;
                            }

                            if (itemsLeft > 0) {
                                toolTip.set(lineNr, toolTip.get(lineNr) + EnumChatFormatting.YELLOW + " " + Utils.formatNumber(itemsLeft) + " left");
                                break;
                            }
                        } else if (targetAmount > 0) {
                            // order/offer amount was found, but next line wasn't the 'filled' line, abort!
                            break;
                        }
                    } catch (NumberFormatException ignored) {
                        break;
                    }
                }
            }
        }

        // for auction house: show price for each item if multiple items are sold at once or if higher tier ultimate enchantment books are sold
        MooConfig.Setting tooltipAuctionHousePriceEachDisplay = MooConfig.getTooltipAuctionHousePriceEachDisplay();
        if ((tooltipAuctionHousePriceEachDisplay == MooConfig.Setting.ALWAYS || tooltipAuctionHousePriceEachDisplay == MooConfig.Setting.SPECIAL && MooConfig.isTooltipToggleKeyBindingPressed())
                && (e.entityPlayer.openContainer instanceof ContainerChest || Minecraft.getMinecraft().currentScreen instanceof MooConfigGui)) {

            int itemAmount = -1;
            String superEnchant = null;
            if (e.itemStack.getItem() == Items.enchanted_book && extraAttributes != null && extraAttributes.hasKey("enchantments", Constants.NBT.TAG_COMPOUND)) {
                NBTTagCompound enchants = extraAttributes.getCompoundTag("enchantments");
                try {
                    Map<String, NBTBase> enchantmentsMap = ReflectionHelper.getPrivateValue(NBTTagCompound.class, enchants, "tagMap", "field_74784_a");

                    if (enchantmentsMap.size() == 1) {
                        for (Map.Entry<String, NBTBase> enchant : enchantmentsMap.entrySet()) {
                            String enchantKey = enchant.getKey();
                            if (enchantKey.startsWith("ultimate_") && enchant.getValue() instanceof NBTTagInt) {
                                // enchanted book with 1 enchantment (which is an ultimate enchant)
                                // enchantment tier => amount of books needed
                                //    I = 2^0 = 1
                                //   II = 2^1 = 2
                                //  III = 2^2 = 4
                                //   IV = 2^3 = 8
                                //    V = 2^4 = 16
                                itemAmount = (int) Math.pow(2, ((NBTTagInt) enchant.getValue()).getInt() - 1);
                                superEnchant = Utils.fancyCase(enchantKey.substring("ultimate_".length()));
                                break;
                            } else if (enchantKey.startsWith("turbo_") && enchant.getValue() instanceof NBTTagInt) {
                                itemAmount = (int) Math.pow(2, ((NBTTagInt) enchant.getValue()).getInt() - 1);
                                superEnchant = "Turbo-" + Utils.fancyCase(enchantKey.substring("turbo_".length()));
                                if (superEnchant.equals("Turbo-Cactus")) {
                                    // (╯°□°）╯︵ ┻━┻
                                    superEnchant = "Turbo-Cacti";
                                }
                                break;
                            }
                            for (String priceEachEnchantment : MooConfig.tooltipAuctionHousePriceEachEnchantments) {
                                String priceEachEnchantKey = priceEachEnchantment;
                                int dashInEnchantName = priceEachEnchantKey.indexOf('-');
                                if (dashInEnchantName > 0) {
                                    priceEachEnchantKey = priceEachEnchantKey.replace('-', '_');
                                }
                                if (enchantKey.equals(priceEachEnchantKey) && enchant.getValue() instanceof NBTTagInt) {
                                    itemAmount = (int) Math.pow(2, ((NBTTagInt) enchant.getValue()).getInt() - 1);
                                    superEnchant = Utils.fancyCase(priceEachEnchantment);
                                }
                            }
                        }
                    }
                } catch (ReflectionHelper.UnableToAccessFieldException ignored) {
                    return;
                }
            } else {
                itemAmount = e.itemStack.stackSize;
                boolean isSubmitBidItem = isSubmitBidItem(e.itemStack);
                if ((itemAmount == 1 && !isSubmitBidItem) || e.toolTip.size() < 4) {
                    // only 1 item or irrelevant tooltip - nothing to do here, abort!
                    return;
                }

                if (isSubmitBidItem) {
                    // special case: "place bid on an item" interface ("Auction View")
                    ItemStack auctionedItem = e.entityPlayer.openContainer.getInventory().get(13);
                    if (auctionedItem == null || auctionedItem.stackSize == 1) {
                        // still only 1 item, abort!
                        return;
                    }
                    itemAmount = auctionedItem.stackSize;
                }
            }
            if (itemAmount > 1) {
                List<String> toolTip = e.toolTip;
                String superEnchantName = null;

                // starting with i=1 because first line is never the one we're looking for
                for (int i = 1; i < toolTip.size(); i++) {
                    String toolTipLine = toolTip.get(i);
                    String toolTipLineUnformatted = EnumChatFormatting.getTextWithoutFormattingCodes(toolTipLine);
                    if (superEnchant != null && superEnchantName == null && (toolTipLineUnformatted.startsWith(superEnchant) || toolTipLineUnformatted.startsWith("Ultimate " + superEnchant))) {
                        int lastSpace = toolTipLine.lastIndexOf(' ');
                        if (lastSpace > 0) {
                            superEnchantName = toolTipLine.substring(0, lastSpace);
                        }
                    }
                    if (toolTipLineUnformatted.startsWith("Top bid: ")
                            || toolTipLineUnformatted.startsWith("Starting bid: ")
                            || toolTipLineUnformatted.startsWith("Item price: ")
                            || toolTipLineUnformatted.startsWith("Buy it now: ")
                            || toolTipLineUnformatted.startsWith("Sold for: ")
                            || toolTipLineUnformatted.startsWith("New bid: ") /* special case: 'Submit Bid' item */) {

                        try {
                            String hopefullyAPrice = StringUtils.substringBetween(toolTipLineUnformatted, ": ", " coins");
                            if (hopefullyAPrice == null) {
                                return;
                            }
                            long price = numberFormatter.parse(hopefullyAPrice).longValue();
                            double priceEach = price / (double) itemAmount;
                            String formattedPriceEach = priceEach < 5000 ? numberFormatter.format(priceEach) : Utils.formatNumberWithAbbreviations(priceEach);
                            if (superEnchantName != null) {
                                toolTip.add(i + 1, EnumChatFormatting.YELLOW + "  (≙ " + itemAmount + "x " + superEnchantName + " " + (MooConfig.useRomanNumerals() ? "I" : "1") + EnumChatFormatting.RESET + EnumChatFormatting.YELLOW + " for " + formattedPriceEach + " each)");
                            } else {
                                String pricePerItem = EnumChatFormatting.YELLOW + " (" + formattedPriceEach + " each)";
                                toolTip.set(i, toolTipLine + pricePerItem);
                            }
                            return;
                        } catch (ParseException ex) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderGuiBackground(GuiScreenEvent.DrawScreenEvent.Pre e) {
        if (e.gui instanceof GuiChest) {
            MooConfig.Setting markAuctionHouseEndedAuctions = MooConfig.getMarkAuctionHouseEndedAuctions();
            if (markAuctionHouseEndedAuctions == MooConfig.Setting.DISABLED) {
                return;
            }
            GuiChest guiChest = (GuiChest) e.gui;

            Container inventorySlots = guiChest.inventorySlots;
            IInventory inventory = inventorySlots.getSlot(0).inventory;
            if (inventory.getName().contains("Auction")) {
                // Auctions Browser, Auction: "<search term>", <player>'s Auctions, Auction View
                FontRenderer fontRenderer = e.gui.mc.fontRendererObj;
                // formulas from GuiContainer#initGui (guiLeft, guiTop) and GuiChest (ySize)
                int guiLeft = (guiChest.width - 176) / 2;
                int inventoryRows = inventory.getSizeInventory() / 9;
                int ySize = 222 - 108 + inventoryRows * 18;
                int guiTop = (guiChest.height - ySize) / 2;

                for (Slot inventorySlot : inventorySlots.inventorySlots) {
                    if (inventorySlot.getHasStack()) {
                        int slotRow = inventorySlot.slotNumber / 9;
                        int slotColumn = inventorySlot.slotNumber % 9;
                        // check if slot is one of the middle slots with parties
                        if (slotRow == 0 || slotRow == (inventoryRows - 1) || slotColumn == 0 || slotColumn == 8) {
                            // one of the glass pane borders
                            continue;
                        }
                        NBTTagCompound itemNbtDisplay = inventorySlot.getStack().getSubCompound("display", false);
                        if (itemNbtDisplay != null && itemNbtDisplay.hasKey("Lore", Constants.NBT.TAG_LIST)) {
                            NBTTagList loreList = itemNbtDisplay.getTagList("Lore", Constants.NBT.TAG_STRING);
                            if (loreList.tagCount() < 5) {
                                continue;
                            }
                            for (int loreLineNr = loreList.tagCount() - 1; loreLineNr >= 0; --loreLineNr) {
                                String loreLineFormatted = loreList.getStringTagAt(loreLineNr);
                                String loreLine = EnumChatFormatting.getTextWithoutFormattingCodes(loreLineFormatted);

                                String auctionStatus;
                                if (loreLine.startsWith("Ends in: ")) {
                                    // auction is still going
                                    break;
                                } else if (loreLine.equals("Status: Expired!")) {
                                    auctionStatus = EnumChatFormatting.RED + (markAuctionHouseEndedAuctions == MooConfig.Setting.TEXT ? "Expired" : "E");
                                } else if (loreLine.equals("Status: Ended!")) {
                                    auctionStatus = EnumChatFormatting.GREEN + (markAuctionHouseEndedAuctions == MooConfig.Setting.TEXT ? "Ended" : "E");
                                } else if (loreLine.equals("Status: Sold!")) {
                                    auctionStatus = EnumChatFormatting.GREEN + (markAuctionHouseEndedAuctions == MooConfig.Setting.TEXT ? "Sold" : "S");
                                } else {
                                    continue;
                                }
                                GlStateManager.pushMatrix();
                                GlStateManager.translate(0, 0, 281);
                                double scaleFactor = markAuctionHouseEndedAuctions == MooConfig.Setting.TEXT ? 0.4 : 0.5;
                                GlStateManager.scale(scaleFactor, scaleFactor, 0);
                                int slotX = guiLeft + inventorySlot.xDisplayPosition;
                                int slotY = guiTop + inventorySlot.yDisplayPosition;
                                fontRenderer.drawStringWithShadow(auctionStatus, (float) (slotX / scaleFactor), (float) (slotY / scaleFactor), 0xffcc00);
                                GlStateManager.popMatrix();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiClose(GuiOpenEvent e) {
        if (e.gui == null && this.bestiaryOverview != null) {
            this.bestiaryOverview = null;
            BestiaryEntry.reinitialize(null);
        }
    }

    @SubscribeEvent
    public void onMouseInteractionInGui(GuiScreenEvent.MouseInputEvent.Pre e) {
        int clickedMouseButton = Mouse.getEventButton();
        if (clickedMouseButton < 0) {
            // no button press, just mouse-hover
            return;
        }
        if (Mouse.getEventButtonState() && e.gui instanceof GuiChest) {
            GuiChest guiChest = (GuiChest) e.gui;
            Slot hoveredSlot = GuiHelper.getSlotUnderMouse(guiChest);
            if (hoveredSlot != null && hoveredSlot.getHasStack() && hoveredSlot.getStack().hasDisplayName()) {
                if (clickedMouseButton == 0 && isBestiaryGui(guiChest, hoveredSlot.getStack())) {
                    // cycle bestiary order on left click
                    main.getConfig().cycleBestiaryOverviewOrder();
                    Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                } else if (this.bestiaryOverview != null && clickedMouseButton <= 3 && hoveredSlot.getStack().getItem() == Items.arrow) {
                    // reset bestiary overview cache on page switch via left, middle or right click on arrow
                    this.bestiaryOverview = null;
                    BestiaryEntry.reinitialize(null);
                }
            }
        }
    }

    private boolean isBestiaryMainGui(GuiChest guiChest, ItemStack hoveredItem) {
        IInventory inventory = guiChest.inventorySlots.getSlot(0).inventory;
        String inventoryName = (inventory.hasCustomName() ? EnumChatFormatting.getTextWithoutFormattingCodes(inventory.getDisplayName().getUnformattedTextForChat()) : inventory.getName());
        String hoveredItemName = EnumChatFormatting.getTextWithoutFormattingCodes(hoveredItem.getDisplayName());
        return ("Bestiary".equals(inventoryName) && "Bestiary".equals(hoveredItemName) && hoveredItem.getItem() == Items.writable_book);
    }

    private boolean isBestiaryGui(GuiChest guiChest, ItemStack hoveredItem) {
        IInventory inventory = guiChest.inventorySlots.getSlot(0).inventory;
        String inventoryName = (inventory.hasCustomName() ? EnumChatFormatting.getTextWithoutFormattingCodes(inventory.getDisplayName().getUnformattedTextForChat()) : inventory.getName());
        String hoveredItemName = EnumChatFormatting.getTextWithoutFormattingCodes(hoveredItem.getDisplayName());
        if (inventoryName.startsWith("Bestiary ➜ ") && inventoryName.endsWith(hoveredItemName)) {
            // bestiary overview is enabled and mouse is hovering over bestiary category item (with same name as category)
            BestiaryEntry.triggerItem = hoveredItem;
            return true;
        } else if (inventoryName.equals("Search Results") && hoveredItem.getItem() == Items.sign && "Search Results".equals(hoveredItemName)) {
            NBTTagCompound itemNbtDisplay = hoveredItem.getSubCompound("display", false);
            if (itemNbtDisplay != null && itemNbtDisplay.hasKey("Lore", Constants.NBT.TAG_LIST)) {
                NBTTagList loreList = itemNbtDisplay.getTagList("Lore", Constants.NBT.TAG_STRING);
                if (loreList.tagCount() >= 2
                        && loreList.getStringTagAt(0).startsWith("§7Query: §a")
                        && loreList.getStringTagAt(1).startsWith("§7Results: §a")) {
                    // hovering over bestiary search result item
                    BestiaryEntry.triggerItem = hoveredItem;
                    return true;
                }
            }
        }
        return false;
    }

    private int abbreviatedToLongNumber(String number) throws NumberInvalidException {
        try {
            number = number.replace(",", "");
            if (number.endsWith("k")) {
                return (int) (Double.parseDouble(number.substring(0, number.length() - 1)) * 1000);
            } else {
                return Integer.parseInt(number);
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new NumberInvalidException("commands.generic.num.invalid", number);
        }
    }

    private boolean openLink(String link) {
        try {
            Desktop.getDesktop().browse(new URI(link));
            return true;
        } catch (Throwable throwable) {
            main.getLogger().error("Couldn't open link: " + link, throwable);
            return false;
        }
    }

    private boolean isSubmitBidItem(ItemStack itemStack) {
        return ((itemStack.getItem() == Items.gold_nugget || itemStack.getItem() == Item.getItemFromBlock(Blocks.gold_block))
                && (itemStack.hasDisplayName() && (itemStack.getDisplayName().endsWith("Submit Bid") || itemStack.getDisplayName().endsWith("Collect Auction"))))
                || (/* green hardened clay + */ itemStack.hasDisplayName() && (itemStack.getDisplayName().endsWith("Create BIN Auction") || itemStack.getDisplayName().endsWith("Create Auction")));
    }

    private enum ItemLookupType {
        OFFICIAL_WIKI("official wiki", "https://wiki.hypixel.net/?search="),
        WIKI("unofficial wiki", "https://hypixel-skyblock.fandom.com/wiki/Special:Search?search="),
        PRICE("price info", "https://sky.coflnet.com/item/"),
        INVALID("nothing", "https://google.com/search?q=");

        private final String description;
        private final String baseUrl;

        ItemLookupType(String description, String baseUrl) {
            this.description = description;
            this.baseUrl = baseUrl;
        }

        public String getDescription() {
            return description;
        }

        public String buildLink(String itemId, String itemName) {
            try {
                return this.baseUrl + (this == PRICE ? itemId : URLEncoder.encode(itemName, "UTF-8"));
            } catch (UnsupportedEncodingException ignored) {
            }
            return null;
        }
    }
}
