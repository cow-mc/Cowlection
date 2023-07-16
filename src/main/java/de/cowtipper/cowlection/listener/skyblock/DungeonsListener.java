package de.cowtipper.cowlection.listener.skyblock;

import com.mojang.realmsclient.util.Pair;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.CredentialStorage;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.config.gui.MooConfigGui;
import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.data.DataHelper.DungeonClass;
import de.cowtipper.cowlection.handler.DungeonCache;
import de.cowtipper.cowlection.partyfinder.Rule;
import de.cowtipper.cowlection.partyfinder.RuleEditorGui;
import de.cowtipper.cowlection.util.GuiHelper;
import de.cowtipper.cowlection.util.TickDelay;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DungeonsListener {
    private static final String FORMATTING_CODE = "§[0-9a-fl-or]";
    private final Cowlection main;
    /**
     * example: (space)Robin_Hood: Archer (42)
     */
    private final Pattern DUNGEON_PARTY_FINDER_PLAYER = Pattern.compile("^ (\\w+): ([A-Za-z]+) \\((\\d+)\\)$");
    /**
     * examples: "Floor: Entrance", "Floor: Floor 4", "Floor: Floor IV"
     */
    private final Pattern DUNGEON_PARTY_FINDER_FLOOR = Pattern.compile("^Floor: (Entrance)?(?:Floor ([IVX]+)?([0-9]+)?)?$");
    private final Pattern DUNGEON_PARTY_FINDER_SELECTED_FLOOR = Pattern.compile("^Currently Selected: (Entrance)?(?:Floor ([IVX]+)?([0-9]+)?)?$");
    /**
     * example: (Adventuring|Playing|Plundering|Looting|...) The Catacombs with 5/5 players on Floor II!
     */
    private final Pattern DUNGEON_ENTERED_DUNGEON = Pattern.compile("^[A-Za-z ]+ The Catacombs( Entrance)? with [0-9]+/[0-9]+ players(?: on Floor ([IVX]+))?!$");
    /**
     * Example tooltip lines:
     * <ul>
     * <li>§7Crit Damage: §c+23% §8(Heavy -3%) §8(+28.75%)</li>
     * <li>§7Health: §a+107 HP §8(+133.75 HP)</li>
     * <li>§7Defense: §a+130 §8(Heavy +65) §8(+162.5)</li>
     * <li>§7Speed: §a-1 §8(Heavy -1)</li>
     * <li>§7Health: §a+432 HP §e(+60 HP) §9(Ancient +7 HP) §8(+1,032.48 HP)</li>
     * </ul>
     * <pre>
     * | Groups                     | Example matches   |
     * |----------------------------|-------------------|
     * | Group `prefix`             | §7Crit Damage: §c |
     * | Group `statNonDungeon`     | +23               |
     * | Group `statNonDungeonUnit` | %                 |
     * | Group `statHpb`            | +60               |
     * | Group `colorReforge`       | §8                |
     * | Group `reforge`            | Heavy             |
     * | Group `statReforge`        | -3                |
     * | Group `colorDungeon`       | §8                |
     * | Group `statDungeon`        | +42               |
     * | Group `statDungeonUnit`    | %                 |
     * </pre>
     */
    private final Pattern TOOLTIP_LINE_PATTERN = Pattern.compile("^(?<prefix>(?:" + FORMATTING_CODE + ")+[A-Za-z ]+: " + FORMATTING_CODE + ")(?<statNonDungeon>[+-]?[0-9]+)(?<statNonDungeonUnit>%| HP|)(?: §e\\(\\+(?<statHpb>[0-9]+)(?: HP)?\\))?(?: (?<colorReforge>" + FORMATTING_CODE + ")\\((?<reforge>[A-Za-z]+) (?<statReforge>[+-]?[0-9]+)(?:%| HP|)\\))?(?: (?<colorDungeon>" + FORMATTING_CODE + ")\\((?<statDungeon>[+-]?[.,0-9]+)(?<statDungeonUnit>%| HP|)\\))?$");
    /**
     * Player deaths in dungeon:
     * <ul>
     * <li> ☠ [player] disconnected from the Dungeon and became a ghost.</li>
     * <li> ☠ [player/You] died and became a ghost.</li>
     * <li> ☠ [player] fell to their death with help from [mob] and became a ghost.</li>
     * <li> ☠ [player] was killed by [mob] and became a ghost.</li>
     * <li> ☠ You were killed by [mob] and became a ghost.</li>
     * </ul>
     */
    private final Pattern DUNGEON_DEATH_PATTERN = Pattern.compile("^ ☠ (\\w+) .+ and became a ghost\\.$");
    private final Pattern DUNGEON_REVIVED_PATTERN = Pattern.compile("^ ❣ (\\w+) was revived.*?$");
    /**
     * Class milestones:
     * <ul>
     * <li>Archer Milestone ❶: You have dealt 60000 Ranged Damage so far! 00m17s</li>
     * <li>Tank Milestone ❼: You have tanked and dealt 2000000 Total Damage so far! 14m33s</li>
     * </ul>
     */
    private final Pattern DUNGEON_CLASS_MILESTONE_PATTERN = Pattern.compile("^[A-Za-z]+ Milestone (.): ");

    private DungeonClass activeDungeonClass;
    private GuiButton btnOpenRuleEditor;

    public DungeonsListener(Cowlection main) {
        this.main = main;
        activeDungeonClass = DungeonClass.UNKNOWN;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onItemTooltip(ItemTooltipEvent e) {
        if (e.itemStack == null || e.toolTip == null) {
            return;
        }
        boolean toggleKeyBindingPressed = MooConfig.isDungeonItemTooltipToggleKeyBindingPressed() && !main.getDungeonCache().isInDungeon();
        MooConfig.Setting showItemQualityAndFloorDisplay = MooConfig.getShowItemQualityAndFloorDisplay();
        boolean showItemQualityAndFloor = showItemQualityAndFloorDisplay == MooConfig.Setting.ALWAYS
                || (showItemQualityAndFloorDisplay == MooConfig.Setting.SPECIAL && toggleKeyBindingPressed);
        if ((showItemQualityAndFloor || toggleKeyBindingPressed)
                && isDungeonItem(e.toolTip)) {
            NBTTagCompound extraAttributes = e.itemStack.getSubCompound("ExtraAttributes", false);
            if (extraAttributes == null) {
                // no extra attributes? no SkyBlock item!
                return;
            }
            String reforge = "";
            if (toggleKeyBindingPressed) {
                // simplify dungeon armor stats
                String originalItemName = e.itemStack.getDisplayName();
                Pair<String, String> sbItemBaseName = Utils.extractSbItemBaseName(originalItemName, extraAttributes, true);
                e.toolTip.set(0, sbItemBaseName.first()); // replace item name
                reforge = sbItemBaseName.second();
            }
            // add item quality/floor and (if key bind is pressed: subtract stat boosts from reforge and update stats for dungeons)
            ListIterator<String> tooltipIterator = e.toolTip.listIterator();

            String itemQualityBottom = null;
            while (tooltipIterator.hasNext()) {
                String line = tooltipIterator.next();
                Matcher lineMatcher = TOOLTIP_LINE_PATTERN.matcher(line);
                String lineWithoutFormatting = EnumChatFormatting.getTextWithoutFormattingCodes(line);
                if (lineMatcher.matches()) {
                    if (EnumChatFormatting.getTextWithoutFormattingCodes(lineMatcher.group("prefix")).equals("Gear Score: ")) {
                        // replace meaningless gear score with item quality (gear score includes reforges etc)
                        StringBuilder customGearScore = new StringBuilder(EnumChatFormatting.GRAY.toString()).append("Item Quality: ");
                        boolean hasCustomGearScore = false;
                        if (extraAttributes.hasKey("baseStatBoostPercentage")) {
                            int itemQuality = extraAttributes.getInteger("baseStatBoostPercentage") * 2; // value between 0 and 50 => *2 == in %
                            customGearScore.append(EnumChatFormatting.LIGHT_PURPLE).append(itemQuality).append("%");
                            hasCustomGearScore = true;
                        }
                        if (extraAttributes.hasKey("item_tier", Constants.NBT.TAG_INT)) {
                            int obtainedFromFloor = extraAttributes.getInteger("item_tier");
                            customGearScore.append(EnumChatFormatting.GRAY).append(" (Floor ").append(EnumChatFormatting.LIGHT_PURPLE).append(obtainedFromFloor).append(EnumChatFormatting.GRAY).append(")");
                            hasCustomGearScore = true;
                        }
                        if (!hasCustomGearScore) {
                            if (MooConfig.dungItemQualityShortenNonRandomized) {
                                customGearScore.append(EnumChatFormatting.DARK_PURPLE).append("100%");
                            } else {
                                customGearScore.append(EnumChatFormatting.LIGHT_PURPLE).append("100%").append(EnumChatFormatting.DARK_GRAY).append(" (never has randomized stats)");
                            }
                        }
                        if (showItemQualityAndFloor) {
                            if (MooConfig.isDungItemQualityAtTop()) {
                                if (MooConfig.dungItemHideGearScore) {
                                    // replace gear score with item quality + obtained floor to top of tooltip
                                    tooltipIterator.set(customGearScore.toString());
                                } else {
                                    // add item quality + obtained floor
                                    tooltipIterator.add(customGearScore.toString());
                                }
                            } else {
                                // add item quality + obtained floor to bottom
                                itemQualityBottom = customGearScore.toString();
                                if (MooConfig.dungItemHideGearScore) {
                                    // remove gear score entry
                                    tooltipIterator.remove();
                                }
                            }
                        }
                        continue;
                    }
                    if (toggleKeyBindingPressed) {
                        try {
                            int statNonDungeon = Integer.parseInt(lineMatcher.group("statNonDungeon"));

                            int statBase = statNonDungeon;
                            if (reforge.equalsIgnoreCase(lineMatcher.group("reforge"))) {
                                // tooltip line has reforge stats; subtract them from base stats
                                statBase -= Integer.parseInt(lineMatcher.group("statReforge"));
                            }

                            if (lineMatcher.group("statHpb") != null) {
                                // tooltip line has Hot Potato Book stats; subtract them from base stats
                                statBase -= Integer.parseInt(lineMatcher.group("statHpb"));
                            }

                            if (statBase == 0) {
                                // don't redraw 0 stats
                                tooltipIterator.remove();
                                continue;
                            }
                            String newToolTipLine = String.format("%s%+d%s", lineMatcher.group("prefix"), statBase, lineMatcher.group("statNonDungeonUnit"));
                            if (lineMatcher.group("statDungeon") != null) {
                                // tooltip line has dungeon stats; update them!
                                double statDungeon = Double.parseDouble(lineMatcher.group("statDungeon").replace(",", ""));

                                double dungeonStatModifier = statDungeon / statNonDungeon; // modified through skill level or gear essence upgrades
                                if (extraAttributes.hasKey("dungeon_item_level")) {
                                    // with essences upgraded item => calculate base (level based) dungeon modifier
                                    dungeonStatModifier -= extraAttributes.getInteger("dungeon_item_level") / 10d;
                                }

                                double statBaseDungeon = statBase * dungeonStatModifier;
                                double statDungeonWithMaxEssenceUpgrades = statBase * (dungeonStatModifier + /*5x essence à +10% each => +50% stats */0.5d);
                                newToolTipLine += String.format(" %s(₀ₓ✪ %+.1f%s) %s(₅ₓ✪ %+.1f%s)", lineMatcher.group("colorDungeon"), statBaseDungeon, lineMatcher.group("statDungeonUnit"),
                                        lineMatcher.group("colorDungeon"), statDungeonWithMaxEssenceUpgrades, lineMatcher.group("statDungeonUnit"));
                            }

                            tooltipIterator.set(newToolTipLine);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                } else if (lineWithoutFormatting.startsWith("Item Ability: ") || lineWithoutFormatting.startsWith("Full Set Bonus: ")) {
                    // stop replacing tooltip entries once we reach item ability or full set bonus
                    break;
                }
            }
            if (itemQualityBottom != null) {
                int index = Math.max(0, e.toolTip.size() - (e.showAdvancedItemTooltips ? /* item name & nbt info */ 2 : 0));
                e.toolTip.add(index, itemQualityBottom);
            }
        }
    }

    private boolean isDungeonItem(List<String> toolTip) {
        ListIterator<String> toolTipIterator = toolTip.listIterator(toolTip.size());
        while (toolTipIterator.hasPrevious()) {
            if (toolTipIterator.previous().contains(" DUNGEON ")) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onRenderGuiBackground(GuiScreenEvent.DrawScreenEvent.Pre e) {
        if (e.gui instanceof GuiChest) {
            GuiChest guiChest = (GuiChest) e.gui;

            Container inventorySlots = guiChest.inventorySlots;
            IInventory inventory = inventorySlots.getSlot(0).inventory;
            if (inventory.getName().equals("Catacombs Gate")) {
                // update active selected class
                ItemStack dungeonClassIndicator = getStackInSlotOrByName(inventory, 47, "Dungeon Classes");
                if (dungeonClassIndicator == null) {
                    // couldn't detect dungeon class indicator
                    return;
                }
                for (String toolTipLine : Utils.getItemTooltip(dungeonClassIndicator)) {
                    String line = EnumChatFormatting.getTextWithoutFormattingCodes(toolTipLine);
                    if (line.startsWith("Currently Selected: ")) {
                        String selectedClassName = line.substring(line.lastIndexOf(' ') + 1);
                        DungeonClass selectedClass = DungeonClass.get(selectedClassName);
                        if (!selectedClass.equals(activeDungeonClass) || selectedClass == DungeonClass.UNKNOWN) {
                            activeDungeonClass = selectedClass;
                        }
                    }
                }
            } else if (inventory.getName().equals("Party Finder")) {
                // enhance party finder

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
                        int maxRow = inventoryRows - 2;
                        if (slotRow > 0 && slotRow < maxRow && slotColumn > 0 && slotColumn < 8) {
                            int slotX = guiLeft + inventorySlot.xDisplayPosition;
                            int slotY = guiTop + inventorySlot.yDisplayPosition;
                            renderPartyStatus(inventorySlot.getStack(), slotX, slotY);
                        }
                    }
                }
            }
        }
    }

    private void renderPartyStatus(ItemStack item, int x, int y) {
        if (!(item.getItem() instanceof ItemSkull && item.getMetadata() == 3 && item.hasTagCompound())) {
            // not a player skull, don't draw party status indicator
            return;
        }
        DataHelper.PartyType partyType = DataHelper.PartyType.SUITABLE;

        List<String> itemTooltip = Utils.getItemTooltip(item);
        if (itemTooltip.size() < 5) {
            // not a valid dungeon party tooltip
            return;
        }
        String lastToolTipLine = EnumChatFormatting.getTextWithoutFormattingCodes(itemTooltip.get(itemTooltip.size() - 1));
        if (lastToolTipLine.endsWith("Complete previous floor first!")
                || lastToolTipLine.startsWith("Requires a Class at Level")
                || lastToolTipLine.startsWith("Requires Catacombs Level")) {
            // cannot enter dungeon
            partyType = DataHelper.PartyType.UNJOINABLE_OR_BLOCK;
        } else if (lastToolTipLine.endsWith("You are in this party!")) {
            partyType = DataHelper.PartyType.CURRENT;
        } else {
            Map<DungeonClass, AtomicInteger> dungClassesInParty = new LinkedHashMap<>();
            if (MooConfig.dungeonPartyMarker(activeDungeonClass).ifDuped()) {
                // add our own class if we want to avoid dupes
                AtomicInteger classCounter = new AtomicInteger();
                classCounter.incrementAndGet();
                dungClassesInParty.put(activeDungeonClass, classCounter);
            }

            int partySize = 5;
            boolean memberTooLowLevel = false;
            boolean partyReqTooLowLevel = false;
            String middleText = null;

            for (String toolTipLine : itemTooltip) {
                String toolTipLineWithoutFormatting = EnumChatFormatting.getTextWithoutFormattingCodes(toolTipLine);
                Matcher playerDetailMatcher = DUNGEON_PARTY_FINDER_PLAYER.matcher(toolTipLineWithoutFormatting);
                if (playerDetailMatcher.matches()) {
                    String className = playerDetailMatcher.group(2);
                    DungeonClass clazz = DungeonClass.get(className);
                    if (MooConfig.dungeonPartyMarker(clazz) == MooConfig.Mark.BLOCK_ALWAYS) {
                        // class is blocked -> block party
                        partyType = DataHelper.PartyType.UNJOINABLE_OR_BLOCK;
                        break;
                    }
                    dungClassesInParty.putIfAbsent(clazz, new AtomicInteger(0));
                    dungClassesInParty.get(clazz).incrementAndGet();

                    int classLevel = MathHelper.parseIntWithDefault(playerDetailMatcher.group(3), 100);
                    if (classLevel < MooConfig.dungClassMin) {
                        memberTooLowLevel = true;
                    }
                } else if (" Empty".equals(toolTipLineWithoutFormatting)) {
                    --partySize;
                } else if (toolTipLineWithoutFormatting.startsWith("Note: ")) {
                    String partyNote = toolTipLineWithoutFormatting.toLowerCase();

                    StringBuilder middleTextBuilder = new StringBuilder();
                    for (Rule rule : main.getPartyFinderRules().getRules()) {
                        if (rule.isTriggered(partyNote)) {
                            DataHelper.PartyType rulePartyType = rule.getPartyType();
                            if (partyType == DataHelper.PartyType.UNJOINABLE_OR_BLOCK) {
                                // party type cannot get worse: abort
                                break;
                            } else if (rulePartyType.compareTo(partyType) > 0) {
                                // only "downgrade" party type from suitable > unideal > block
                                partyType = rulePartyType;
                            }
                            String text = rule.getMiddleText();
                            if (text != null) {
                                if (middleTextBuilder.length() > 0) {
                                    middleTextBuilder.append("§8,§r");
                                }
                                middleTextBuilder.append(text);
                            }
                        }
                    }
                    middleText = middleTextBuilder.toString();
                } else if (toolTipLineWithoutFormatting.startsWith("Dungeon Level Required: ")) {
                    int minDungLevelReq = MathHelper.parseIntWithDefault(toolTipLineWithoutFormatting.substring(toolTipLineWithoutFormatting.lastIndexOf(' ') + 1), 100);
                    if (minDungLevelReq < MooConfig.dungDungeonReqMin) {
                        partyReqTooLowLevel = true;
                    }
                }
            }
            FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
            if (partyType != DataHelper.PartyType.UNJOINABLE_OR_BLOCK && middleText != null) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, 281);
                double scaleFactor = 0.5;
                GlStateManager.scale(scaleFactor, scaleFactor, 0);
                font.drawStringWithShadow(middleText, (float) (x / scaleFactor), (float) ((y + 5) / scaleFactor), 0xFFffffff);
                GlStateManager.popMatrix();
            }
            if (partyType != DataHelper.PartyType.UNJOINABLE_OR_BLOCK) {
                if (memberTooLowLevel || partyReqTooLowLevel) {
                    // at least one party member is lower than the min class level or party min Dungeon level req is too low
                    partyType = DataHelper.PartyType.UNIDEAL;
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(0, 0, 280);
                    font.drawStringWithShadow(EnumChatFormatting.BOLD + "ᐯ", x + 1, y + 8, partyReqTooLowLevel ? new Color(170, 0, 0).getRGB() : new Color(220, 20, 20, 255).getRGB());
                    GlStateManager.popMatrix();
                }
                StringBuilder unwantedClasses = new StringBuilder();
                StringBuilder dupedClasses = new StringBuilder();
                for (Map.Entry<DungeonClass, AtomicInteger> partyClassInfo : dungClassesInParty.entrySet()) {
                    MooConfig.Mark dungeonPartyMarker = MooConfig.dungeonPartyMarker(partyClassInfo.getKey());
                    if (dungeonPartyMarker == MooConfig.Mark.UNIDEAL_DUPE && partyClassInfo.getValue().get() > 1) {
                        // 2+ class
                        dupedClasses.append(partyClassInfo.getKey().getShortName());
                    } else if (dungeonPartyMarker == MooConfig.Mark.UNIDEAL_ALWAYS) {
                        unwantedClasses.append(partyClassInfo.getKey().getShortName());
                    } else if (dungeonPartyMarker == MooConfig.Mark.BLOCK_DUPE && partyClassInfo.getValue().get() > 1) {
                        // found blocked if duplicated class
                        partyType = DataHelper.PartyType.UNJOINABLE_OR_BLOCK;
                        break;
                    }
                }
                if (partyType != DataHelper.PartyType.UNJOINABLE_OR_BLOCK) {
                    StringBuilder badClasses = new StringBuilder();
                    if (unwantedClasses.length() > 0) {
                        badClasses.append(EnumChatFormatting.WHITE).append(unwantedClasses);
                    }
                    if (dupedClasses.length() > 0) {
                        badClasses.append(EnumChatFormatting.GOLD).append("²⁺").append(EnumChatFormatting.YELLOW).append(dupedClasses); // 2+
                    }

                    if (badClasses.length() > 0) {
                        // party has unwanted classes or class duplicates
                        partyType = DataHelper.PartyType.UNIDEAL;

                        GlStateManager.pushMatrix();
                        GlStateManager.translate(0, 0, 280);
                        double scaleFactor = 0.8;
                        GlStateManager.scale(scaleFactor, scaleFactor, 0);
                        font.drawStringWithShadow(badClasses.toString(), (float) (x / scaleFactor), (float) (y / scaleFactor), new Color(255, 170, 0, 255).getRGB());
                        GlStateManager.popMatrix();
                    } else if (!memberTooLowLevel && !partyReqTooLowLevel && middleText == null) {
                        // party matches our criteria!
                        partyType = DataHelper.PartyType.SUITABLE;
                    }
                    // add party size indicator
                    if (MooConfig.dungPartiesSize && partySize > 0) {
                        GlStateManager.pushMatrix();
                        GlStateManager.translate(0, 0, 280);
                        String partySizeIndicator = String.valueOf(partySize);
                        font.drawStringWithShadow(partySizeIndicator, x + 17 - font.getStringWidth(partySizeIndicator), y + 9, 0xffFFFFFF);
                        GlStateManager.popMatrix();
                    }
                }
            }
        }
        if (MooConfig.dungPartyFinderOverlayDrawBackground &&
                (partyType != DataHelper.PartyType.CURRENT
                        || (/*partyType == PartyType.CURRENT &&*/ Minecraft.getSystemTime() % 1000 < 600))) {
            GlStateManager.pushMatrix();
            GlStateManager.enableDepth();
            GlStateManager.translate(0, 0, partyType.getZIndex());
            Gui.drawRect(x, y, x + 16, y + 16, partyType.getColor());
            GlStateManager.popMatrix();
        }
    }

    private ItemStack getStackInSlotOrByName(IInventory inventory, int slot, String itemDisplayName) {
        ItemStack item = inventory.getStackInSlot(slot);
        if (item != null && item.hasDisplayName() && itemDisplayName.equals(EnumChatFormatting.getTextWithoutFormattingCodes(item.getDisplayName()))) {
            return item;
        }
        // an update might have moved the item to another slot, search for it:
        for (int checkedSlot = 0; checkedSlot < inventory.getSizeInventory(); checkedSlot++) {
            item = inventory.getStackInSlot(checkedSlot);
            if (item != null && item.hasDisplayName() && itemDisplayName.equals(EnumChatFormatting.getTextWithoutFormattingCodes(item.getDisplayName()))) {
                return item;
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post e) {
        if (MooConfig.dungPartyFinderRuleEditorShowOpenButton && e.gui instanceof GuiChest) {
            // add "Open Rule Editor" button
            GuiChest guiChest = (GuiChest) e.gui;

            Container inventorySlots = guiChest.inventorySlots;
            IInventory inventory = inventorySlots.getSlot(0).inventory;
            if (inventory.getName().equals("Party Finder")) {
                // formulas from GuiContainer#initGui (guiTop) and GuiChest (ySize)
                int inventoryRows = inventory.getSizeInventory() / 9;
                int ySize = 222 - 108 + inventoryRows * 18;
                int guiTop = (guiChest.height - ySize) / 2;
                btnOpenRuleEditor = new GuiButtonExt(8765, 3, guiTop, 74, 18, "Rule editor ↗");
                e.buttonList.add(btnOpenRuleEditor);
            } else {
                btnOpenRuleEditor = null;
            }
        } else {
            btnOpenRuleEditor = null;
        }
    }

    @SubscribeEvent
    public void onGuiButtonClick(GuiScreenEvent.ActionPerformedEvent.Post e) {
        if (MooConfig.dungPartyFinderRuleEditorShowOpenButton && e.gui instanceof GuiChest && e.button.id == 8765 && "Rule editor ↗".equals(e.button.displayString)) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.thePlayer.closeScreen();
            mc.displayGuiScreen(new RuleEditorGui());
        }
    }

    @SubscribeEvent
    public void onGuiButtonHover(GuiScreenEvent.DrawScreenEvent.Post e) {
        if (MooConfig.dungPartyFinderRuleEditorShowOpenButton && e.gui instanceof GuiChest && btnOpenRuleEditor != null && btnOpenRuleEditor.isMouseOver()) {
            Minecraft mc = Minecraft.getMinecraft();
            List<String> btnTooltip = new ArrayList<>();
            btnTooltip.add("Open Cowlection's Party Notes Rule Editor");
            btnTooltip.add(EnumChatFormatting.GRAY + "Want to remove this button? " + EnumChatFormatting.YELLOW + "/moo config editor");
            btnTooltip.add(EnumChatFormatting.GRAY + "Want to remove the overlay inside dungeons? " + EnumChatFormatting.YELLOW + "/moo config overlay");
            GuiHelper.drawHoveringText(btnTooltip, e.mouseX, e.mouseY, mc.displayWidth, mc.displayHeight, 300);
            GlStateManager.disableLighting();
        }
    }

    // Events inside dungeons
    // priority = highest to ignore other mods modifying the chat output
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onMessageReceived(ClientChatReceivedEvent e) {
        if (e.type != 2) { // normal chat or system msg (not above action bar)
            String text = EnumChatFormatting.getTextWithoutFormattingCodes(e.message.getUnformattedText());

            if (!main.getDungeonCache().isInDungeon()) {
                if (text.startsWith("[NPC] Mort: ")) {
                    // Mort said something, probably entered dungeons
                    main.getDungeonCache().onDungeonEnterOrLeave(true);
                } else if (MooConfig.dungSendWrongFloorWarning) {
                    Matcher dungeonEnteredMatcher = DUNGEON_ENTERED_DUNGEON.matcher(text);
                    if (dungeonEnteredMatcher.matches()) {
                        String floor = dungeonEnteredMatcher.group(1) != null ? "Entrance" : dungeonEnteredMatcher.group(2);
                        if (floor == null) {
                            // this shouldn't ever happen: neither a floor nor the entrance was entered
                            return;
                        }
                        String queuedFloor = main.getDungeonCache().getQueuedFloor();
                        if (queuedFloor != null && !queuedFloor.equals(floor)) {
                            // queued and entered dungeon floors are different!
                            new TickDelay(() -> {
                                String attentionSeeker = "" + EnumChatFormatting.LIGHT_PURPLE + EnumChatFormatting.OBFUSCATED + "#";
                                main.getChatHelper().sendMessage(EnumChatFormatting.RED, attentionSeeker + EnumChatFormatting.RED + " You entered dungeon floor " + EnumChatFormatting.DARK_RED + floor + EnumChatFormatting.RED + " but originally queued for floor " + EnumChatFormatting.DARK_RED + queuedFloor + " " + attentionSeeker);
                                Minecraft.getMinecraft().thePlayer.playSound("mob.cow.hurt", Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER), 1);
                            }, 100);
                        }
                    }
                }
                return;
            }
            // player is in dungeon:
            Matcher dungeonDeathMatcher = DUNGEON_DEATH_PATTERN.matcher(text);
            Matcher dungeonRevivedMatcher = DUNGEON_REVIVED_PATTERN.matcher(text);
            if (dungeonDeathMatcher.matches()) {
                String playerName = dungeonDeathMatcher.group(1);
                if (playerName.equals("You")) {
                    playerName = Minecraft.getMinecraft().thePlayer.getName();
                }
                main.getDungeonCache().addDeath(playerName);
            } else if (dungeonRevivedMatcher.matches()) {
                main.getDungeonCache().revivedPlayer(dungeonRevivedMatcher.group(1));
            } else if (text.trim().equals("> EXTRA STATS <")) {
                // dungeon "end screen"
                if (MooConfig.dungSendPerformanceOnEndScreen) {
                    new TickDelay(() -> main.getDungeonCache().sendDungeonPerformance(), 5);
                }
            } else if (text.startsWith("PUZZLE FAIL!")) {
                // Skill: failed puzzle
                main.getDungeonCache().addFailedPuzzle(text);
            } else if (text.startsWith("Sending to server ")) {
                // changed worlds => left dungeons
                main.getDungeonCache().onDungeonEnterOrLeave(false);
            } else {
                Matcher dungeonClassMilestoneMatcher = DUNGEON_CLASS_MILESTONE_PATTERN.matcher(text);
                if (dungeonClassMilestoneMatcher.find()) {
                    // class milestone reached
                    int classMilestoneSymbol = dungeonClassMilestoneMatcher.group(1).charAt(0);
                    // 1 ❶ = 10102
                    // 9 ❾ = 10110
                    if (classMilestoneSymbol >= /* 1 */ 10102 && classMilestoneSymbol <= /* 10 */ 10111) {
                        main.getDungeonCache().setClassMilestone(classMilestoneSymbol - 10101);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouseInteractionInGui(GuiScreenEvent.MouseInputEvent.Pre e) {
        if (Mouse.getEventButton() < 0) {
            // no button press, just mouse-hover
            return;
        }
        if (e.gui instanceof GuiChest) {
            GuiChest guiChest = (GuiChest) e.gui;

            IInventory inventory = guiChest.inventorySlots.getSlot(0).inventory;
            if (inventory.getName().equals("Party Finder")) {
                // get dungeon floor nr when joining a dungeon party via party finder
                Slot hoveredSlot = GuiHelper.getSlotUnderMouse(guiChest);
                if (hoveredSlot != null && hoveredSlot.getHasStack()) {
                    // clicked on an item
                    List<String> itemToolTip = Utils.getItemTooltip(hoveredSlot.getStack());
                    if (itemToolTip.size() < 5 || hoveredSlot.getStack().getItem() != Items.skull) {
                        // not a valid dungeon party tooltip
                        return;
                    }
                    extractQueuedFloorNr(itemToolTip, DUNGEON_PARTY_FINDER_FLOOR);

                    if (CredentialStorage.isMooValid && MooConfig.dungPartyFinderPartyLookup) {
                        List<String> partyMembers = new ArrayList<>();
                        for (String toolTipLine : itemToolTip) {
                            String toolTipLineWithoutFormatting = EnumChatFormatting.getTextWithoutFormattingCodes(toolTipLine);
                            Matcher playerDetailMatcher = DUNGEON_PARTY_FINDER_PLAYER.matcher(toolTipLineWithoutFormatting);
                            if (playerDetailMatcher.matches()) {
                                partyMembers.add(playerDetailMatcher.group(1));
                            }
                        }
                        main.getDungeonCache().setPotentialPartyMembers(partyMembers);
                    }
                }
            } else if (inventory.getName().equals("Group Builder")) {
                // get dungeon floor nr when creating a dungeon party for party finder
                Slot hoveredSlot = GuiHelper.getSlotUnderMouse(guiChest);
                if (hoveredSlot != null && hoveredSlot.getHasStack() && hoveredSlot.getStack().hasDisplayName()) {
                    // clicked on an item
                    String clickedItemName = EnumChatFormatting.getTextWithoutFormattingCodes(hoveredSlot.getStack().getDisplayName());
                    if (clickedItemName.equals("Confirm Group")) {
                        // created dungeon party group
                        ItemStack selectedFloorItem = getStackInSlotOrByName(inventory, 13, "Select Floor");
                        if (selectedFloorItem != null) {
                            List<String> itemToolTip = Utils.getItemTooltip(selectedFloorItem);
                            if (itemToolTip.size() < 5) {
                                // not a valid dungeon floor tooltip
                                return;
                            }
                            extractQueuedFloorNr(itemToolTip, DUNGEON_PARTY_FINDER_SELECTED_FLOOR);
                        }
                    }
                }
            }
        }
    }

    private void extractQueuedFloorNr(List<String> itemToolTip, Pattern pattern) {
        for (String toolTipLine : itemToolTip) {
            String line = EnumChatFormatting.getTextWithoutFormattingCodes(toolTipLine);

            Matcher floorMatcher = pattern.matcher(line);
            if (floorMatcher.matches()) {
                String floorNr = floorMatcher.group(1); // floor == Entrance
                if (floorNr == null) {
                    floorNr = floorMatcher.group(2); // floor == [IVX]+
                }
                if (floorNr == null) {
                    try {
                        int floorNrArabic = Integer.parseInt(floorMatcher.group(3));
                        floorNr = Utils.convertArabicToRoman(floorNrArabic); // floor == [0-9]+
                    } catch (NumberFormatException ignored) {
                    }
                }
                main.getDungeonCache().setQueuedFloor(floorNr);
                break;
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END && e.side != Side.CLIENT && e.type != TickEvent.Type.PLAYER) {
            return;
        }
        if ("Crypt Undead".equals(e.player.getName())) {
            main.getDungeonCache().addDestroyedCrypt(e.player.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type == RenderGameOverlayEvent.ElementType.ALL) {
            DungeonCache dungeonCache = main.getDungeonCache();

            boolean isEditingDungeonOverlaySettings = MooConfigGui.showDungeonPerformanceOverlay();
            if (MooConfig.dungOverlayEnabled && dungeonCache.isInDungeon() || isEditingDungeonOverlaySettings) {
                if (dungeonCache.isInDungeon()) {
                    dungeonCache.fetchScoreboardData();
                }

                ArrayList<String> dungeonPerformanceEntries = new ArrayList<>();
                int maxSkillScore = dungeonCache.getMaxSkillScore();
                int totalDeaths = dungeonCache.getTotalDeaths();
                int failedPuzzles = dungeonCache.getFailedPuzzles();
                int classMilestone = dungeonCache.getClassMilestone();
                int destroyedCrypts = dungeonCache.getDestroyedCrypts();
                int elapsedMinutes = dungeonCache.getElapsedMinutes();

                dungeonPerformanceEntries.add("Max Skill score: " + (maxSkillScore == 100 ? EnumChatFormatting.GREEN : EnumChatFormatting.YELLOW) + maxSkillScore + " / 100");
                if (totalDeaths > 0 || isEditingDungeonOverlaySettings) {
                    dungeonPerformanceEntries.add("  Deaths: " + EnumChatFormatting.RED + totalDeaths);
                }
                if (failedPuzzles > 0 || isEditingDungeonOverlaySettings) {
                    dungeonPerformanceEntries.add("  Failed Puzzles: " + EnumChatFormatting.RED + failedPuzzles);
                }
                dungeonPerformanceEntries.add("Class Milestone: " + (classMilestone < 3 ? EnumChatFormatting.RED : EnumChatFormatting.GREEN) + classMilestone);
                dungeonPerformanceEntries.add("Destroyed Crypts: " + (destroyedCrypts >= 5 ? EnumChatFormatting.GREEN : EnumChatFormatting.YELLOW) + destroyedCrypts + " / 5");
                EnumChatFormatting color = EnumChatFormatting.GREEN;
                if (elapsedMinutes > 20) {
                    color = EnumChatFormatting.DARK_RED;
                } else if (elapsedMinutes > 18) {
                    color = EnumChatFormatting.RED;
                } else if (elapsedMinutes > 15) {
                    color = EnumChatFormatting.YELLOW;
                }
                dungeonPerformanceEntries.add("Elapsed Minutes: " + color + elapsedMinutes);
                if (!MooConfig.hasOpenedConfigGui) {
                    dungeonPerformanceEntries.add(EnumChatFormatting.RED + " ⚠ Want to move me? " + EnumChatFormatting.DARK_GRAY + "➡ " + EnumChatFormatting.LIGHT_PURPLE + "/moo config overlay");
                }

                FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
                GlStateManager.pushMatrix();

                float scaleFactor = MooConfig.dungOverlayGuiScale / 100f;
                GlStateManager.scale(scaleFactor, scaleFactor, 0);
                for (int line = 0; line < dungeonPerformanceEntries.size(); line++) {
                    String dungeonPerformanceEntry = dungeonPerformanceEntries.get(line);
                    int xPos = (int) ((e.resolution.getScaledWidth() * (MooConfig.dungOverlayPositionX / 1000d)) / scaleFactor);
                    int yPos = (int) ((e.resolution.getScaledHeight() * (MooConfig.dungOverlayPositionY / 1000d) + 2) / scaleFactor + fontRenderer.FONT_HEIGHT * line + 2);
                    MooConfig.Setting dungOverlayTextBorder = MooConfig.getDungOverlayTextBorder();
                    if (dungOverlayTextBorder == MooConfig.Setting.TEXT) {
                        // normal mc text shadow (drop shadow)
                        fontRenderer.drawStringWithShadow(dungeonPerformanceEntry, xPos, yPos, 0xffFFAA00);
                    } else if (dungOverlayTextBorder == MooConfig.Setting.ALWAYS) {
                        // full outline
                        String dungeonPerformanceEntryShadow = EnumChatFormatting.getTextWithoutFormattingCodes(dungeonPerformanceEntry);
                        fontRenderer.drawString(dungeonPerformanceEntryShadow, xPos + 1, yPos, 0xff3F2A00);
                        fontRenderer.drawString(dungeonPerformanceEntryShadow, xPos - 1, yPos, 0xff3F2A00);
                        fontRenderer.drawString(dungeonPerformanceEntryShadow, xPos, yPos + 1, 0xff3F2A00);
                        fontRenderer.drawString(dungeonPerformanceEntryShadow, xPos, yPos - 1, 0xff3F2A00);
                        fontRenderer.drawString(dungeonPerformanceEntry, xPos, yPos, 0xffFFAA00);
                    } else {
                        // no border, just plain text
                        fontRenderer.drawString(dungeonPerformanceEntry, xPos, yPos, 0xffFFAA00);
                    }
                }
                GlStateManager.popMatrix();
            }
        }
    }
}
