package de.cowtipper.cowlection.listener.skyblock;

import com.mojang.realmsclient.util.Pair;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.config.gui.MooConfigGui;
import de.cowtipper.cowlection.util.GuiHelper;
import de.cowtipper.cowlection.util.MooChatComponent;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

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
                ItemStack itemStack = hoveredSlot.getStack();
                NBTTagCompound extraAttributes = itemStack.getSubCompound("ExtraAttributes", false);
                if (extraAttributes != null && extraAttributes.hasKey("id")) {
                    // seems to be a SkyBlock item
                    String sbId = extraAttributes.getString("id");
                    if (itemLookupType == ItemLookupType.WIKI || (/* itemLookupType == ItemLookupType.PRICE && */ !blackList.contains(sbId) && !sbId.contains("_GENERATOR_"))) {
                        // open item price info or open wiki entry
                        Pair<String, String> sbItemBaseName = Utils.extractSbItemBaseName(itemStack.getDisplayName(), extraAttributes, false);
                        String itemBaseName = sbItemBaseName.first();

                        // exceptions:
                        String querySuffix = "";
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
                    } else {
                        // item is blacklisted from lookup
                        main.getChatHelper().sendMessage(EnumChatFormatting.RED, "⚠ " + EnumChatFormatting.RESET + itemStack.getDisplayName() + EnumChatFormatting.RED + " (" + Utils.fancyCase(sbId) + ") " + itemLookupType.getDescription() + " cannot be looked up.");
                        Minecraft.getMinecraft().thePlayer.playSound("mob.villager.no", Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER), 1.4f);
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
        NBTTagCompound extraAttributes = e.itemStack.getSubCompound("ExtraAttributes", false);
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
                    skyBlockDateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("d/M/yy hh:mm", Locale.US));
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

        // for auction house: show price for each item if multiple items are sold at once
        MooConfig.Setting tooltipAuctionHousePriceEachDisplay = MooConfig.getTooltipAuctionHousePriceEachDisplay();
        if ((tooltipAuctionHousePriceEachDisplay == MooConfig.Setting.ALWAYS || tooltipAuctionHousePriceEachDisplay == MooConfig.Setting.SPECIAL && MooConfig.isTooltipToggleKeyBindingPressed())
                && (e.entityPlayer.openContainer instanceof ContainerChest || Minecraft.getMinecraft().currentScreen instanceof MooConfigGui)) {
            int stackSize = e.itemStack.stackSize;
            if ((stackSize == 1 && !isSubmitBidItem(e.itemStack)) || e.toolTip.size() < 4) {
                // only 1 item or irrelevant tooltip - nothing to do here, abort!
                return;
            }

            if (isSubmitBidItem(e.itemStack)) {
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
        return ((itemStack.getItem().equals(Items.gold_nugget) || itemStack.getItem().equals(Item.getItemFromBlock(Blocks.gold_block)))
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
