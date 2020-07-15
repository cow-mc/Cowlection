package eu.olli.cowlection.listener;

import eu.olli.cowlection.Cowlection;
import eu.olli.cowlection.config.MooConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
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

    private String activeDungeonClass;

    public DungeonsListener(Cowlection main) {
        this.main = main;
        activeDungeonClass = "unknown";
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent e) {
        if (e.itemStack == null || e.toolTip == null) {
            return;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && isDungeonItem(e.toolTip)) {
            // simplify dungeon armor stats
            String originalItemName = e.itemStack.getDisplayName();
            NBTTagCompound extraAttributes = e.itemStack.getSubCompound("ExtraAttributes", false);
            if (extraAttributes != null && extraAttributes.hasKey("modifier")) {
                String reforge = StringUtils.capitalize(extraAttributes.getString("modifier"));
                int modifierSuffix = Math.max(reforge.indexOf("_sword"), reforge.indexOf("_bow"));
                if (modifierSuffix != -1) {
                    reforge = reforge.substring(0, modifierSuffix);
                }
                int reforgeInItemName = originalItemName.indexOf(reforge);
                if (reforgeInItemName > 0 && !originalItemName.contains(EnumChatFormatting.STRIKETHROUGH.toString())) {
                    // we have a reforged item! strike through reforge in item name and remove any essence upgrades (✪)
                    String grayedOutFormatting = "" + EnumChatFormatting.GRAY + EnumChatFormatting.STRIKETHROUGH;
                    StringBuffer modifiedItemName = new StringBuffer(originalItemName)
                            .insert(reforgeInItemName, grayedOutFormatting)
                            .insert(reforgeInItemName + reforge.length() + grayedOutFormatting.length(), originalItemName.substring(0, reforgeInItemName));
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
                    while (tooltipIterator.hasNext()) {
                        String line = tooltipIterator.next();
                        Matcher lineMatcher = TOOLTIP_LINE_PATTERN.matcher(line);
                        if (lineMatcher.matches()) {
                            if (EnumChatFormatting.getTextWithoutFormattingCodes(lineMatcher.group("prefix")).equals("Gear Score: ")) {
                                // gear score: gray + strike through because gear score doesn't really mean anything
                                String newToolTipLine = String.format("%s%s%s%s %s(%s%s%s)", lineMatcher.group("prefix"), grayedOutFormatting, lineMatcher.group("statNonDungeon"), EnumChatFormatting.RESET, // space
                                        EnumChatFormatting.GRAY, grayedOutFormatting, lineMatcher.group("statDungeon"), EnumChatFormatting.GRAY);
                                tooltipIterator.set(newToolTipLine);
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
                        }
                    }
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

                GlStateManager.translate(0, 0, 1);
                float scaleFactor = 0.8f;
                GlStateManager.scale(scaleFactor, scaleFactor, 0);
                for (Slot inventorySlot : inventorySlots.inventorySlots) {
                    if (inventorySlot.getHasStack()) {
                        int slotRow = inventorySlot.slotNumber / 9;
                        int slotColumn = inventorySlot.slotNumber % 9;
                        // check if slot is one of the middle slots with actual minions
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
        String status = "⬛"; // ok
        Color color = new Color(20, 200, 20, 255);

        List<String> itemTooltip = item.getTooltip(Minecraft.getMinecraft().thePlayer, false);
        if (itemTooltip.get(itemTooltip.size() - 1).endsWith("Complete previous floor first!")) {
            // cannot enter dungeon
            status = "✗";
            color = new Color(220, 20, 20, 255);
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
}
