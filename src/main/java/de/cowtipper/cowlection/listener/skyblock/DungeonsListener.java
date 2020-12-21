package de.cowtipper.cowlection.listener.skyblock;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.config.gui.MooConfigGui;
import de.cowtipper.cowlection.data.DataHelper.DungeonClass;
import de.cowtipper.cowlection.handler.DungeonCache;
import de.cowtipper.cowlection.util.TickDelay;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.StringUtils;
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
    private final Pattern DUNGEON_PARTY_FINDER_PLAYER = Pattern.compile("^ (?:\\w+): ([A-Za-z]+) \\((\\d+)\\)$");
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
     * </ul>
     * <pre>
     * | Groups                     | Example matches   |
     * |----------------------------|-------------------|
     * | Group `prefix`             | §7Crit Damage: §c |
     * | Group `statNonDungeon`     | +23               |
     * | Group `statNonDungeonUnit` | %                 |
     * | Group `colorReforge`       | §8                |
     * | Group `reforge`            | Heavy             |
     * | Group `statReforge`        | -3                |
     * | Group `statReforgeUnit`    | %                 |
     * | Group `colorDungeon`       | §8                |
     * </pre>
     */
    private final Pattern TOOLTIP_LINE_PATTERN = Pattern.compile("^(?<prefix>(?:" + FORMATTING_CODE + ")+[A-Za-z ]+: " + FORMATTING_CODE + ")(?<statNonDungeon>[+-]?[0-9]+)(?<statNonDungeonUnit>%| HP|)(?: (?<colorReforge>" + FORMATTING_CODE + ")\\((?<reforge>[A-Za-z]+) (?<statReforge>[+-]?[0-9]+)(?<statReforgeUnit>%| HP|)\\))?(?: (?<colorDungeon>" + FORMATTING_CODE + ")\\((?<statDungeon>[+-]?[.0-9]+)(?<statDungeonUnit>%| HP|)\\))?$");
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
    private final Pattern DUNGEON_DEATH_PATTERN = Pattern.compile("^ ☠ (\\w+) (.+) and became a ghost\\.$");
    private final Pattern DUNGEON_REVIVED_PATTERN = Pattern.compile("^ ❣ (\\w+) was revived(?:.*?)$");
    /**
     * Class milestones:
     * <ul>
     * <li>Archer Milestone ❶: You have dealt 60000 Ranged Damage so far! 00m17s</li>
     * <li>Tank Milestone ❼: You have tanked and dealt 2000000 Total Damage so far! 14m33s</li>
     * </ul>
     */
    private final Pattern DUNGEON_CLASS_MILESTONE_PATTERN = Pattern.compile("^[A-Za-z]+ Milestone (.): ");

    private DungeonClass activeDungeonClass;

    public DungeonsListener(Cowlection main) {
        this.main = main;
        activeDungeonClass = DungeonClass.UNKNOWN;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onItemTooltip(ItemTooltipEvent e) {
        if (e.itemStack == null || e.toolTip == null) {
            return;
        }
        boolean toggleKeyBindingPressed = MooConfig.isDungeonItemTooltipToggleKeyBindingPressed();
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
                StringBuilder modifiedItemName = new StringBuilder(originalItemName);
                String grayedOutFormatting = "" + EnumChatFormatting.GRAY + EnumChatFormatting.STRIKETHROUGH;

                if (extraAttributes.hasKey("modifier")) {
                    // item has been reforged; re-format item name to exclude reforges
                    reforge = StringUtils.capitalize(extraAttributes.getString("modifier"));
                    int modifierSuffix = Math.max(reforge.indexOf("_sword"), reforge.indexOf("_bow"));
                    if (modifierSuffix != -1) {
                        reforge = reforge.substring(0, modifierSuffix);
                    }
                    int reforgeInItemName = originalItemName.indexOf(reforge);
                    if (reforgeInItemName == -1 && reforge.equals("Light") && extraAttributes.getString("id").startsWith("HEAVY_")) {
                        // special case: heavy armor with light reforge
                        reforgeInItemName = originalItemName.indexOf("Heavy");
                    }

                    if (reforgeInItemName > 0 && !originalItemName.contains(EnumChatFormatting.STRIKETHROUGH.toString())) {
                        // we have a reforged item! strike through reforge in item name and remove any essence upgrades (✪)

                        int reforgeLength = reforge.length();
                        String reforgePrefix = null;
                        // special cases for reforge + item name
                        if (reforge.equals("Heavy") && extraAttributes.getString("id").startsWith("HEAVY_")) {
                            reforgePrefix = "Extremely ";
                        } else if (reforge.equals("Light") && extraAttributes.getString("id").startsWith("HEAVY_")) {
                            reforgePrefix = "Not So ";
                        } else if ((reforge.equals("Wise") && extraAttributes.getString("id").startsWith("WISE_DRAGON_"))
                                || (reforge.equals("Strong") && extraAttributes.getString("id").startsWith("STRONG_DRAGON_"))) {
                            reforgePrefix = "Very ";
                        } else if (reforge.equals("Superior") && extraAttributes.getString("id").startsWith("SUPERIOR_DRAGON_")) {
                            reforgePrefix = "Highly ";
                        } else if (reforge.equals("Perfect") && extraAttributes.getString("id").startsWith("PERFECT_")) {
                            reforgePrefix = "Absolutely ";
                        }
                        if (reforgePrefix != null) {
                            reforgeInItemName -= reforgePrefix.length();
                            reforgeLength = reforgePrefix.length() - 1;
                        }

                        modifiedItemName.insert(reforgeInItemName, grayedOutFormatting)
                                .insert(reforgeInItemName + reforgeLength + grayedOutFormatting.length(), originalItemName.substring(0, reforgeInItemName));
                    }
                }
                // remove essence upgrade indicators (✪)
                String essenceUpgradeIndicator = EnumChatFormatting.GOLD + "✪";
                int essenceModifier = modifiedItemName.indexOf(essenceUpgradeIndicator);
                while (essenceModifier > 0) {
                    modifiedItemName.replace(essenceModifier, essenceModifier + essenceUpgradeIndicator.length(), grayedOutFormatting + "✪");
                    essenceModifier = modifiedItemName.indexOf(essenceUpgradeIndicator);
                }
                e.toolTip.set(0, modifiedItemName.toString()); // replace item name
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
                            customGearScore.append("―");
                        }
                        if (showItemQualityAndFloor) {
                            if (MooConfig.isDungItemQualityAtTop()) {
                                // replace gear score with item quality + obtained floor to top of tooltip
                                tooltipIterator.set(customGearScore.toString());
                            } else {
                                // add item quality + obtained floor to bottom
                                itemQualityBottom = customGearScore.toString();
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

                            if (statBase == 0) {
                                // don't redraw 0 stats
                                tooltipIterator.remove();
                                continue;
                            }
                            String newToolTipLine = String.format("%s%+d%s", lineMatcher.group("prefix"), statBase, lineMatcher.group("statNonDungeonUnit"));
                            if (lineMatcher.group("statDungeon") != null) {
                                // tooltip line has dungeon stats; update them!
                                double statDungeon = Double.parseDouble(lineMatcher.group("statDungeon"));

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
                for (String toolTipLine : dungeonClassIndicator.getTooltip(Minecraft.getMinecraft().thePlayer, false)) {
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
        ItemStack indicatorItem = null;

        List<String> itemTooltip = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
        if (itemTooltip.size() < 5) {
            // not a valid dungeon party tooltip
            return;
        }
        String lastToolTipLine = EnumChatFormatting.getTextWithoutFormattingCodes(itemTooltip.get(itemTooltip.size() - 1));
        if (lastToolTipLine.endsWith("Complete previous floor first!")
                || lastToolTipLine.startsWith("Requires a Class at Level")
                || lastToolTipLine.startsWith("Requires Catacombs Level")) {
            // cannot enter dungeon
            indicatorItem = new ItemStack(Blocks.carpet, 1, EnumDyeColor.RED.getMetadata());
        } else if (lastToolTipLine.endsWith("You are in this party!")) {
            indicatorItem = new ItemStack(Items.leather, 1);
        } else {
            Map<DungeonClass, AtomicInteger> dungClassesInParty = new LinkedHashMap<>();
            AtomicInteger classCounter = new AtomicInteger();
            classCounter.incrementAndGet();
            dungClassesInParty.put(activeDungeonClass, classCounter); // add our own class

            boolean memberTooLowLevel = false;

            for (String toolTipLine : itemTooltip) {
                Matcher playerDetailMatcher = DUNGEON_PARTY_FINDER_PLAYER.matcher(EnumChatFormatting.getTextWithoutFormattingCodes(toolTipLine));
                if (playerDetailMatcher.matches()) {
                    String className = playerDetailMatcher.group(1);
                    DungeonClass clazz = DungeonClass.get(className);
                    dungClassesInParty.putIfAbsent(clazz, new AtomicInteger(0));
                    dungClassesInParty.get(clazz).incrementAndGet();

                    int classLevel = MathHelper.parseIntWithDefault(playerDetailMatcher.group(2), 100);
                    if (classLevel < MooConfig.dungClassMin) {
                        memberTooLowLevel = true;
                    }
                }
            }
            if (memberTooLowLevel) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, 280);
                Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "ᐯ", x + 1, y + 8, new Color(220, 20, 20, 255).getRGB());
                GlStateManager.popMatrix();
            }
            StringBuilder dupedClasses = new StringBuilder();
            for (Map.Entry<DungeonClass, AtomicInteger> partyClassInfo : dungClassesInParty.entrySet()) {
                if (partyClassInfo.getValue().get() > 1 && MooConfig.filterDungPartiesWithDupes(partyClassInfo.getKey())) {
                    dupedClasses.append(partyClassInfo.getKey().getShortName());
                }
            }
            if (dupedClasses.length() > 0) {
                dupedClasses.insert(0, EnumChatFormatting.RED).insert(0, "²⁺");

                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, 280);
                double scaleFactor = 0.8;
                GlStateManager.scale(scaleFactor, scaleFactor, 0);
                Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(dupedClasses.toString(), (float) (x / scaleFactor), (float) (y / scaleFactor), new Color(255, 170, 0, 255).getRGB());
                GlStateManager.popMatrix();
            } else if (!memberTooLowLevel) {
                // party matches our criteria!
                indicatorItem = new ItemStack(Blocks.carpet, 1, EnumDyeColor.LIME.getMetadata());
            }
        }
        if (indicatorItem != null) {
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(indicatorItem, x, y);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
        }
    }

    private ItemStack getStackInSlotOrByName(IInventory inventory, int slot, String itemDisplayName) {
        ItemStack item = inventory.getStackInSlot(slot);
        if (item != null && item.hasDisplayName() && itemDisplayName.equals(EnumChatFormatting.getTextWithoutFormattingCodes(item.getDisplayName()))) {
            return item;
        }
        // an update might have moved the item to another slot, search for it:
        for (int checkedSlot = 0; checkedSlot < inventory.getSizeInventory(); checkedSlot++) {
            item = inventory.getStackInSlot(slot);
            if (item != null && item.hasDisplayName() && itemDisplayName.equals(EnumChatFormatting.getTextWithoutFormattingCodes(item.getDisplayName()))) {
                return item;
            }
        }
        return null;
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
                    return;
                }
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
                main.getDungeonCache().addDeath(playerName, dungeonDeathMatcher.group(2).contains("disconnected"));
            } else if (dungeonRevivedMatcher.matches()) {
                main.getDungeonCache().revivedPlayer(dungeonRevivedMatcher.group(1));
            } else if (text.trim().equals("> EXTRA STATS <")) {
                // dungeon "end screen"
                new TickDelay(() -> main.getDungeonCache().sendDungeonPerformance(), 5);
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
                Slot hoveredSlot = getSlotUnderMouse(guiChest);
                if (hoveredSlot != null && hoveredSlot.getHasStack()) {
                    // clicked on an item
                    List<String> itemToolTip = hoveredSlot.getStack().getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    if (itemToolTip.size() < 5) {
                        // not a valid dungeon party tooltip
                        return;
                    }
                    extractQueuedFloorNr(itemToolTip, DUNGEON_PARTY_FINDER_FLOOR);
                }
            } else if (inventory.getName().equals("Group Builder")) {
                // get dungeon floor nr when creating a dungeon party for party finder
                Slot hoveredSlot = getSlotUnderMouse(guiChest);
                if (hoveredSlot != null && hoveredSlot.getHasStack() && hoveredSlot.getStack().hasDisplayName()) {
                    // clicked on an item
                    String clickedItemName = EnumChatFormatting.getTextWithoutFormattingCodes(hoveredSlot.getStack().getDisplayName());
                    if (clickedItemName.equals("Confirm Group")) {
                        // created dungeon party group
                        ItemStack selectedFloorItem = getStackInSlotOrByName(inventory, 12, "Select Floor");
                        if (selectedFloorItem != null) {
                            List<String> itemToolTip = selectedFloorItem.getTooltip(Minecraft.getMinecraft().thePlayer, false);
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
                    } catch (NumberFormatException ex) {
                        floorNr = null;
                    }
                }
                main.getDungeonCache().setQueuedFloor(floorNr);
                break;
            }
        }
    }

    private Slot getSlotUnderMouse(GuiChest guiChest) {
        try {
            return ReflectionHelper.getPrivateValue(GuiContainer.class, guiChest, "theSlot", "field_147006_u");
        } catch (ReflectionHelper.UnableToAccessFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END && e.side != Side.CLIENT && e.type != TickEvent.Type.PLAYER) {
            return;
        }
        if ("Crypt Undead".equals(e.player.getName())) {
            boolean isNewDestroyedCrypt = main.getDungeonCache().addDestroyedCrypt(e.player.getUniqueID());
            if (isNewDestroyedCrypt) {
                main.getLogger().info("[Dungeon Bonus Score] Crypt Undead spawned @ " + e.player.getPosition() + " - distance to player: " + Math.sqrt(e.player.getPosition().distanceSq(Minecraft.getMinecraft().thePlayer.getPosition())));
            }
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type == RenderGameOverlayEvent.ElementType.ALL) {
            DungeonCache dungeonCache = main.getDungeonCache();

            if (dungeonCache.isInDungeon()) {
                dungeonCache.updateElapsedMinutesFromScoreboard();
            }

            boolean isEditingDungeonOverlaySettings = MooConfigGui.showDungeonPerformanceOverlay();
            if (MooConfig.dungOverlayEnabled && dungeonCache.isInDungeon() || isEditingDungeonOverlaySettings) {
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
                if (elapsedMinutes > 15 && elapsedMinutes <= 20) {
                    dungeonPerformanceEntries.add(EnumChatFormatting.RED + "  ⚠ slower than 20 mins = point penalty");
                } else if (elapsedMinutes > 20) {
                    dungeonPerformanceEntries.add(EnumChatFormatting.GOLD + "  Time penalty: " + EnumChatFormatting.RED + ((int) (2.2 * (elapsedMinutes - 20))) + " points");
                }

                FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
                GlStateManager.pushMatrix();

                float scaleFactor = MooConfig.dungOverlayGuiScale / 100f;
                GlStateManager.scale(scaleFactor, scaleFactor, 0);
                for (int line = 0; line < dungeonPerformanceEntries.size(); line++) {
                    String dungeonPerformanceEntry = dungeonPerformanceEntries.get(line);
                    int xPos = (int) ((e.resolution.getScaledWidth() * (MooConfig.dungOverlayPositionX / 1000d)) / scaleFactor);
                    int yPos = (int) ((e.resolution.getScaledHeight() * (MooConfig.dungOverlayPositionY / 1000d) + 2) / scaleFactor + fontRenderer.FONT_HEIGHT * line + 2);
                    if (MooConfig.dungOverlayTextShadow) {
                        fontRenderer.drawStringWithShadow(dungeonPerformanceEntry, xPos, yPos, 0xffFFAA00);
                    } else {
                        fontRenderer.drawString(dungeonPerformanceEntry, xPos, yPos, 0xffFFAA00);
                    }
                }
                GlStateManager.popMatrix();
            }
        }
    }
}
