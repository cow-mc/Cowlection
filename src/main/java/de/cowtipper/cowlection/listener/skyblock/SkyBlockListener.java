package de.cowtipper.cowlection.listener.skyblock;

import com.mojang.realmsclient.util.Pair;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.config.gui.MooConfigGui;
import de.cowtipper.cowlection.data.BestiaryEntry;
import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.data.XpTables;
import de.cowtipper.cowlection.util.GuiHelper;
import de.cowtipper.cowlection.util.MooChatComponent;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
    private static final Set<String> blackList = new HashSet<>(Arrays.asList("ENCHANTED_BOOK", "RUNE", "PET", "POTION")); // + minions (_GENERATOR_)
    private static final Pattern ITEM_COUNT_PREFIXED_PATTERN = Pattern.compile("^(?:§[0-9a-fl-or])*[\\d]+x ");
    private static final Pattern ITEM_COUNT_SUFFIXED_PATTERN = Pattern.compile(" (?:§[0-9a-fl-or])*x[\\d]+$");
    private static final Pattern PET_NAME_PATTERN = Pattern.compile("^§7\\[Lvl (\\d+)] (§[0-9a-f])");
    private static final Pattern TIER_SUFFIX_PATTERN = Pattern.compile(" [IVX0-9]+$");
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
                if (extraAttributes != null && extraAttributes.hasKey("id")) {
                    // seems to be a SkyBlock item
                    String sbId = extraAttributes.getString("id");
                    if (itemLookupType == ItemLookupType.WIKI || (/* itemLookupType == ItemLookupType.PRICE && */ !blackList.contains(sbId) && !sbId.contains("_GENERATOR_"))) {
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
                    List<String> lore = itemStack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    if (lore.size() > 5 && lore.get(1).endsWith(" commodity")) {
                        // item is a Bazaar commodity
                        itemBaseName = itemStack.getDisplayName();
                    }
                }
                if (itemBaseName != null) {
                    String link = buildLink(EnumChatFormatting.getTextWithoutFormattingCodes(itemBaseName).trim() + querySuffix, itemLookupType);
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

                if (inventoryName.endsWith("➜ Graphs")) {
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
                String petInfo = extraAttributes.getString("petInfo");
                String expSubstr = "\"exp\":";
                int beginPetExp = petInfo.indexOf(expSubstr);
                int endPetExp = petInfo.indexOf(',', beginPetExp);
                if (beginPetExp > 0 && endPetExp > 0) {
                    try {
                        long petExp = (long) Double.parseDouble(petInfo.substring(beginPetExp + expSubstr.length(), endPetExp));
                        int index = Math.max(0, e.toolTip.size() - (e.showAdvancedItemTooltips ? /* item name & nbt info */ 2 : 0));
                        e.toolTip.add(index, EnumChatFormatting.GRAY + "Pet exp: " + EnumChatFormatting.GOLD + numberFormatter.format(petExp));
                    } catch (NumberFormatException ignored) {
                        // do nothing
                    }
                }
            } else if (e.itemStack.getDisplayName().contains("[Lvl ")) {
                // pet in pets menu
                for (int i = e.toolTip.size() - 1; i >= 0; i--) {
                    String loreLine = EnumChatFormatting.getTextWithoutFormattingCodes(e.toolTip.get(i));
                    if (loreLine.startsWith("--------------------")) { // exp bar to next level
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
                    case ALWAYS:
                        e.toolTip.add(index, "Item age: " + EnumChatFormatting.DARK_GRAY + ((MooConfig.tooltipItemAgeShortened) ? Utils.getDurationAsWord(dateTime.toEpochSecond() * 1000) : Utils.getDurationAsWords(dateTime.toEpochSecond() * 1000).first()));
                        break;
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
        if (currentScreen instanceof GuiChest && isBestiaryOverviewVisible && isBestiaryGui((GuiChest) currentScreen, e.itemStack)) {
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
                                if (loreLine.startsWith("-------------------- ")) {
                                    try {
                                        String progress = loreLine.substring("-------------------- ".length());
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

        // for auction house: show price for each item if multiple items are sold at once
        MooConfig.Setting tooltipAuctionHousePriceEachDisplay = MooConfig.getTooltipAuctionHousePriceEachDisplay();
        if ((tooltipAuctionHousePriceEachDisplay == MooConfig.Setting.ALWAYS || tooltipAuctionHousePriceEachDisplay == MooConfig.Setting.SPECIAL && MooConfig.isTooltipToggleKeyBindingPressed())
                && (e.entityPlayer.openContainer instanceof ContainerChest || Minecraft.getMinecraft().currentScreen instanceof MooConfigGui)) {
            int stackSize = e.itemStack.stackSize;
            boolean isSubmitBidItem = isSubmitBidItem(e.itemStack);
            if ((stackSize == 1 && !isSubmitBidItem) || e.toolTip.size() < 4) {
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
                stackSize = auctionedItem.stackSize;
            }

            List<String> toolTip = e.toolTip;

            // starting with i=1 because first line is never the one we're looking for
            for (int i = 1; i < toolTip.size(); i++) {
                String toolTipLineUnformatted = EnumChatFormatting.getTextWithoutFormattingCodes(toolTip.get(i));
                if (toolTipLineUnformatted.startsWith("Top bid: ")
                        || toolTipLineUnformatted.startsWith("Starting bid: ")
                        || toolTipLineUnformatted.startsWith("Item price: ")
                        || toolTipLineUnformatted.startsWith("Buy it now: ")
                        || toolTipLineUnformatted.startsWith("Sold for: ")
                        || toolTipLineUnformatted.startsWith("New bid: ") /* special case: 'Submit Bid' item */) {

                    try {
                        long price = numberFormatter.parse(StringUtils.substringBetween(toolTipLineUnformatted, ": ", " coins")).longValue();
                        double priceEach = price / (double) stackSize;
                        String formattedPriceEach = priceEach < 5000 ? numberFormatter.format(priceEach) : Utils.formatNumberWithAbbreviations(priceEach);
                        String pricePerItem = EnumChatFormatting.YELLOW + " (" + formattedPriceEach + " each)";
                        toolTip.set(i, toolTip.get(i) + pricePerItem);
                        return;
                    } catch (ParseException ex) {
                        return;
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

    private String buildLink(String itemName, ItemLookupType itemLookupType) {
        try {
            return itemLookupType.getBaseUrl() + URLEncoder.encode(itemName, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return null;
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
        WIKI("wiki", "https://hypixel-skyblock.fandom.com/wiki/Special:Search?search="),
        PRICE("price info", "https://stonks.gg/search?input="),
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

        public String getBaseUrl() {
            return baseUrl;
        }
    }
}
