package de.cowtipper.cowlection.listener.skyblock;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.DungeonOverlayGuiConfig;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.handler.DungeonCache;
import de.cowtipper.cowlection.util.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
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
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;
import java.util.*;
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
    private final Pattern DUNGEON_DEATH_PATTERN = Pattern.compile("^ ☠ (\\w+) (?:.*?) and became a ghost\\.$");
    /**
     * Class milestones:
     * <ul>
     * <li>Archer Milestone ❶: You have dealt 60000 Ranged Damage so far! 00m17s</li>
     * <li>Tank Milestone ❼: You have tanked and dealt 2000000 Total Damage so far! 14m33s</li>
     * </ul>
     */
    private final Pattern DUNGEON_CLASS_MILESTONE_PATTERN = Pattern.compile("^[A-Za-z]+ Milestone (.): ");

    private String activeDungeonClass;

    public DungeonsListener(Cowlection main) {
        this.main = main;
        activeDungeonClass = "unknown";
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onItemTooltip(ItemTooltipEvent e) {
        if (e.itemStack == null || e.toolTip == null) {
            return;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && isDungeonItem(e.toolTip)) {
            // simplify dungeon armor stats
            String originalItemName = e.itemStack.getDisplayName();
            NBTTagCompound extraAttributes = e.itemStack.getSubCompound("ExtraAttributes", false);
            if (extraAttributes != null) {
                StringBuilder modifiedItemName = new StringBuilder(originalItemName);
                String reforge = "";
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

                // subtract stat boosts from reforge and update stats for dungeons
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
                            if (MooConfig.isDungItemQualityAtTop()) {
                                // replace 'Gear Score' line
                                tooltipIterator.set(customGearScore.toString());
                            } else {
                                // delete 'Gear Score' line and add item quality to bottom
                                tooltipIterator.remove();
                                itemQualityBottom = customGearScore.toString();
                            }
                            continue;
                        }
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
                ItemStack dungeonClassIndicator = inventory.getStackInSlot(47);
                if (dungeonClassIndicator == null) {
                    // couldn't detect dungeon class indicator
                    return;
                }
                for (String toolTipLine : dungeonClassIndicator.getTooltip(Minecraft.getMinecraft().thePlayer, false)) {
                    String line = EnumChatFormatting.getTextWithoutFormattingCodes(toolTipLine);
                    if (line.startsWith("Currently Selected: ")) {
                        String selectedClass = line.substring(line.lastIndexOf(' ') + 1);
                        if (!selectedClass.equals(activeDungeonClass)) {
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
                GlStateManager.pushMatrix();

                GlStateManager.translate(0, 0, 280);
                float scaleFactor = 0.8f;
                GlStateManager.scale(scaleFactor, scaleFactor, 0);
                for (Slot inventorySlot : inventorySlots.inventorySlots) {
                    if (inventorySlot.getHasStack()) {
                        int slotRow = inventorySlot.slotNumber / 9;
                        int slotColumn = inventorySlot.slotNumber % 9;
                        // check if slot is one of the middle slots with parties
                        int maxRow = inventoryRows - 2;
                        if (slotRow > 0 && slotRow < maxRow && slotColumn > 0 && slotColumn < 8) {
                            int slotX = (int) ((guiLeft + inventorySlot.xDisplayPosition) / scaleFactor);
                            int slotY = (int) ((guiTop + inventorySlot.yDisplayPosition) / scaleFactor);
                            renderPartyStatus(inventorySlot.getStack(), slotX, slotY);
                        }
                    }
                }
                GlStateManager.popMatrix();
            }
        }
    }

    private void renderPartyStatus(ItemStack item, int x, int y) {
        if (!(item.getItem() instanceof ItemSkull && item.getMetadata() == 3 && item.hasTagCompound())) {
            // not a player skull, don't draw party status indicator
            return;
        }
        String status = "⬛"; // ok
        Color color = new Color(20, 200, 20, 255);

        List<String> itemTooltip = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
        if (itemTooltip.size() < 5) {
            // not a valid dungeon party tooltip
            return;
        }
        if (itemTooltip.get(itemTooltip.size() - 1).endsWith("Complete previous floor first!")) {
            // cannot enter dungeon
            status = "✗";
            color = new Color(220, 20, 20, 255);
        } else if (itemTooltip.get(itemTooltip.size() - 1).endsWith("You are in this party!")) {
            status = EnumChatFormatting.OBFUSCATED + "#";
        } else {
            int dungClassMin = MooConfig.dungClassRange[0];
            int dungClassMax = MooConfig.dungClassRange[1];
            Set<String> dungClassesInParty = new HashSet<>();
            dungClassesInParty.add(activeDungeonClass); // add our own class

            for (String toolTipLine : itemTooltip) {
                Matcher playerDetailMatcher = DUNGEON_PARTY_FINDER_PLAYER.matcher(EnumChatFormatting.getTextWithoutFormattingCodes(toolTipLine));
                if (playerDetailMatcher.matches()) {
                    String clazz = playerDetailMatcher.group(1);
                    int classLevel = MathHelper.parseIntWithDefault(playerDetailMatcher.group(2), -1);
                    if (MooConfig.dungFilterPartiesWithDupes && !dungClassesInParty.add(clazz)) {
                        // duped class!
                        status = "²⁺"; // 2+
                        color = new Color(220, 120, 20, 255);
                        break;
                    } else if (dungClassMin > -1 && classLevel < dungClassMin) {
                        // party member too low level
                        status = EnumChatFormatting.BOLD + "ᐯ";
                        color = new Color(200, 20, 20, 255);
                        break;
                    } else if (dungClassMax > -1 && classLevel > dungClassMax) {
                        // party member too high level
                        status = EnumChatFormatting.BOLD + "ᐱ";
                        color = new Color(20, 120, 230, 255);
                        break;
                    }
                }
            }
        }
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(status, x, y, color.getRGB());
    }

    // Events inside dungeons
    @SubscribeEvent
    public void onDungeonsEnterOrLeave(PlayerSetSpawnEvent e) {
        // check if player has entered or left a SkyBlock dungeon
        new TickDelay(() -> {
            Scoreboard scoreboard = e.entityPlayer.worldObj.getScoreboard();
            ScoreObjective scoreboardSidebar = scoreboard.getObjectiveInDisplaySlot(1);
            if (scoreboardSidebar == null) {
                return;
            }

            Collection<Score> scoreboardLines = scoreboard.getSortedScores(scoreboardSidebar);
            for (Score line : scoreboardLines) {
                ScorePlayerTeam scorePlayerTeam = scoreboard.getPlayersTeam(line.getPlayerName());
                if (scorePlayerTeam != null) {
                    String lineWithoutFormatting = EnumChatFormatting.getTextWithoutFormattingCodes(scorePlayerTeam.getColorPrefix() + scorePlayerTeam.getColorSuffix());

                    if (lineWithoutFormatting.startsWith(" ⏣")) {
                        // current location: dungeon or outside?
                        boolean isInDungeonNow = lineWithoutFormatting.startsWith(" ⏣ The Catacombs");
                        main.getDungeonCache().onDungeonEnterOrLeave(isInDungeonNow);
                        return;
                    }
                }
            }
        }, 40); // 2 second delay, making sure scoreboard got sent
    }

    @SubscribeEvent
    public void onMessageReceived(ClientChatReceivedEvent e) {
        if (e.type != 2) { // normal chat or system msg (not above action bar)
            String text = EnumChatFormatting.getTextWithoutFormattingCodes(e.message.getUnformattedText());

            if (!main.getDungeonCache().isInDungeon()) {
                if (text.startsWith("[NPC] Mort: ")) {
                    // Mort said something, probably entered dungeons
                    main.getDungeonCache().onDungeonEnterOrLeave(true);
                }
                return;
            }
            // player is in dungeon:
            Matcher dungeonDeathMatcher = DUNGEON_DEATH_PATTERN.matcher(text);
            if (dungeonDeathMatcher.matches()) {
                String playerName = dungeonDeathMatcher.group(1);
                if (playerName.equals("You")) {
                    playerName = Minecraft.getMinecraft().thePlayer.getName();
                }
                main.getDungeonCache().addDeath(playerName);
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

            boolean isConfigGui = Minecraft.getMinecraft().currentScreen instanceof DungeonOverlayGuiConfig;
            if (MooConfig.dungOverlayEnabled && dungeonCache.isInDungeon() || isConfigGui) {
                ArrayList<String> dungeonPerformanceEntries = new ArrayList<>();
                int maxSkillScore = dungeonCache.getMaxSkillScore();
                int totalDeaths = dungeonCache.getTotalDeaths();
                int failedPuzzles = dungeonCache.getFailedPuzzles();
                int classMilestone = dungeonCache.getClassMilestone();
                int destroyedCrypts = dungeonCache.getDestroyedCrypts();
                int elapsedMinutes = dungeonCache.getElapsedMinutes();

                dungeonPerformanceEntries.add("Max Skill score: " + (maxSkillScore == 100 ? EnumChatFormatting.GREEN : EnumChatFormatting.YELLOW) + maxSkillScore + " / 100");
                if (totalDeaths > 0 || isConfigGui) {
                    dungeonPerformanceEntries.add("  Deaths: " + EnumChatFormatting.RED + totalDeaths);
                }
                if (failedPuzzles > 0 || isConfigGui) {
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
                    int yPos = MooConfig.dungOverlayPositionY + fontRenderer.FONT_HEIGHT * line + 2;
                    fontRenderer.drawString(dungeonPerformanceEntry, MooConfig.dungOverlayPositionX, yPos, 0xffFFAA00);
                }
                GlStateManager.popMatrix();
            }
        }
    }
}
