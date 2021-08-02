package de.cowtipper.cowlection.partyfinder;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.GuiScrollingList;
import net.minecraftforge.fml.client.config.*;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Based on GuiModList
 */
public class RuleEditorGui extends GuiScreen {
    private final Rules rules;
    private RulesListGui rulesList;
    private GuiUnicodeGlyphButton btnUndoAll;
    private GuiUnicodeGlyphButton btnDefaultAll;
    private GuiButtonExt btnHelp;
    private GuiButtonExt btnClose;
    private static float lastScrollDistance;
    private final boolean expertMode;
    private final static List<String> colorCodesExplanation;

    static {
        colorCodesExplanation = new ArrayList<>();
        colorCodesExplanation.add(EnumChatFormatting.BOLD + "Minecraft color codes:");
        for (EnumChatFormatting chatFormatting : EnumChatFormatting.values()) {
            if (chatFormatting.isColor()) {
                colorCodesExplanation.add(Utils.toHumanColorCodes(chatFormatting.toString()) + " → " + chatFormatting.toString() + chatFormatting.getFriendlyName());
            }
        }
    }

    public RuleEditorGui() {
        this.rules = Cowlection.getInstance().getPartyFinderRules();
        expertMode = !MooConfig.dungPartyFinderRuleEditorSimplified;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        // undo all button
        this.buttonList.add(this.btnUndoAll = new GuiUnicodeGlyphButton(2002, this.width - 94, 4, 20, 20, "", GuiUtils.UNDO_CHAR, 2));
        // reset all button
        this.buttonList.add(this.btnDefaultAll = new GuiUnicodeGlyphButton(2001, this.width - 72, 4, 20, 20, "", GuiUtils.RESET_CHAR, 2));
        // help button
        this.buttonList.add(this.btnHelp = new GuiButtonExt(1, this.width - 47, 4, 20, 20, "?"));
        // close button
        this.buttonList.add(this.btnClose = new GuiButtonExt(1, this.width - 25, 4, 20, 20, EnumChatFormatting.RED + "X"));

        updateLastScrollDistance();
        // scrollable commands list
        rulesList = new RulesListGui(this.width - 30, this.rules.getRules(), lastScrollDistance);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.pushMatrix();
        double scaleFactor = 1.5;
        GlStateManager.scale(scaleFactor, scaleFactor, 0);
        this.drawString(this.fontRendererObj, "Dungeon Party Finder: Rules editor", 30, 6, 0xFFCC00);
        GlStateManager.popMatrix();
        this.rulesList.drawScreen(mouseX, mouseY, partialTicks);
        if (btnUndoAll.isMouseOver()) {
            drawHoveringText(Collections.singletonList("Undo all changes"), mouseX, mouseY);
        } else if (btnDefaultAll.isMouseOver()) {
            drawHoveringText(Collections.singletonList("Reset rules to default"), mouseX, mouseY);
        } else if (btnHelp.isMouseOver()) {
            List<String> helpTooltip = new ArrayList<>();
            helpTooltip.add("" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + "Dungeon Party Finder: Rules editor");
            helpTooltip.add(EnumChatFormatting.GREEN + "If you have any questions or need help with the rules editor: Join the Cowshed discord!");
            helpTooltip.add("");
            helpTooltip.add(EnumChatFormatting.YELLOW + GuiUtils.UNDO_CHAR + EnumChatFormatting.RESET + " = discard all changes");
            helpTooltip.add(EnumChatFormatting.YELLOW + GuiUtils.RESET_CHAR + EnumChatFormatting.RESET + " = remove all rules and add default rules");
            helpTooltip.add(EnumChatFormatting.GRAY + "If Party notes filter contains:");
            helpTooltip.add(EnumChatFormatting.YELLOW + " ➊ " + EnumChatFormatting.RESET + "brackets " + EnumChatFormatting.YELLOW + "()" + EnumChatFormatting.RESET + " = exclude certain terms, for example \""
                    + EnumChatFormatting.GRAY + "(free )carry" + EnumChatFormatting.RESET + "\" matches \"" + EnumChatFormatting.GRAY + "carry" + EnumChatFormatting.RESET + "\" but not \"" + EnumChatFormatting.GRAY + "free carry" + EnumChatFormatting.RESET + "\"");
            helpTooltip.add(EnumChatFormatting.YELLOW + " ➋ " + EnumChatFormatting.RESET + "comma " + EnumChatFormatting.YELLOW + "," + EnumChatFormatting.RESET + " = alternatives, for example \"" +
                    EnumChatFormatting.GRAY + "plz,pls,help" + EnumChatFormatting.RESET + "\" matches \"" + EnumChatFormatting.GRAY + "plz" + EnumChatFormatting.RESET + "\", \"" + EnumChatFormatting.GRAY + "pls" + EnumChatFormatting.RESET + "\" and \"" + EnumChatFormatting.GRAY + "help" + EnumChatFormatting.RESET + "\"");
            helpTooltip.add(EnumChatFormatting.YELLOW + " ➊+➋ " + EnumChatFormatting.RESET + "combination of both " + EnumChatFormatting.YELLOW + "(,)" + EnumChatFormatting.RESET + ": for example \""
                    + EnumChatFormatting.GRAY + "carry( pls, plz)" + EnumChatFormatting.RESET + "\" matches \"" + EnumChatFormatting.GRAY + "carry" + EnumChatFormatting.RESET + "\", but not \"" + EnumChatFormatting.GRAY + "carry pls" + EnumChatFormatting.RESET + "\" or \"" + EnumChatFormatting.GRAY + "carry plz" + EnumChatFormatting.RESET + "\"");
            drawHoveringText(helpTooltip, mouseX, mouseY);
        } else if (btnClose.isMouseOver()) {
            drawHoveringText(Arrays.asList(EnumChatFormatting.RED + "Save & Close", "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Hint:" + EnumChatFormatting.RESET + " alternatively press ESC"), mouseX, mouseY);
        }
    }

    @Override
    public void updateScreen() {
        if (this.rulesList != null) {
            this.rulesList.updateFocusedTextField();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == btnUndoAll) {
            GuiYesNo guiUndoAll = new GuiYesNo(this,
                    EnumChatFormatting.BOLD + "Discard " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "all " + EnumChatFormatting.RESET + EnumChatFormatting.BOLD + "changes?",
                    EnumChatFormatting.RED + "This action cannot be reverted!",
                    21000000);
            guiUndoAll.setButtonDelay(60);
            mc.displayGuiScreen(guiUndoAll);
        } else if (button == btnDefaultAll) {
            GuiYesNo guiDefaultAll = new GuiYesNo(this,
                    EnumChatFormatting.BOLD + "Reset to default rules?",
                    "This will remove all custom rules! " + EnumChatFormatting.RED + "This action cannot be reverted once saved!",
                    42000000);
            guiDefaultAll.setButtonDelay(60);
            mc.displayGuiScreen(guiDefaultAll);
        } else if (button == btnHelp) {
            GuiConfirmOpenLink guiHelp = new GuiConfirmOpenLink(this, "https://discord.gg/fU2tFPf", 50000000, true);
            guiHelp.disableSecurityWarning();
            mc.displayGuiScreen(guiHelp);
        } else if (button == btnClose) {
            rules.saveToFile();
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char eventChar, int eventKey) {
        if (eventKey == Keyboard.KEY_ESCAPE) {
            rules.saveToFile();
            mc.displayGuiScreen(null);
        } else {
            int focusedRuleId = this.rulesList.keyTyped(eventChar, eventKey);
            if (focusedRuleId >= 0 && (eventKey == Keyboard.KEY_UP || eventKey == Keyboard.KEY_DOWN)) {
                boolean moved = rules.move(focusedRuleId, eventKey == Keyboard.KEY_DOWN);
                if (moved) {
                    initGui();
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        updateLastScrollDistance();
    }

    private void updateLastScrollDistance() {
        if (this.rulesList != null) {
            try {
                lastScrollDistance = ReflectionHelper.getPrivateValue(GuiScrollingList.class, this.rulesList, "scrollDistance");
            } catch (ReflectionHelper.UnableToAccessFieldException ignored) {
                lastScrollDistance = 0;
            }
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result) {
            if (id == 21000000) {
                // discard all changes
                rules.loadFromFile();
            } else if (id == 42000000) {
                // reset all rules to default
                rules.resetToDefault();
            } else if (id == 50000000) {
                try {
                    Desktop.getDesktop().browse(new URI("https://discord.gg/fU2tFPf"));
                } catch (Throwable throwable) {
                    Cowlection.getInstance().getLogger().error("Couldn't open link https://discord.gg/fU2tFPf", throwable);
                }
            } else if (id < 9000000) {
                // user confirmed rule deletion
                deleteRule(id);
            }
        }
        mc.displayGuiScreen(this);
    }

    private void addNewRule(int slotIndex) {
        this.rules.add(slotIndex);
        initGui();
    }

    public void deleteRule(int id) {
        Rule removedRule = rules.remove(id);
        if (removedRule != null) {
            initGui();
        }
    }

    /**
     * Based on GuiModList.Info
     */
    private class RulesListGui extends GuiScrollingList {
        private final List<RuleEntry> ruleEntries;
        private GuiTextField focusedTextField;
        private RuleEntry hoveredEntry;

        public RulesListGui(int width, List<Rule> rules, float lastScrollDistance) {
            super(RuleEditorGui.this.mc,
                    width,
                    RuleEditorGui.this.height,
                    30, RuleEditorGui.this.height - 5, 12, 18,
                    RuleEditorGui.this.width,
                    RuleEditorGui.this.height);
            setHeaderInfo(true, 20);
            this.ruleEntries = convertToEntries(rules);
            try {
                // scroll to previous location
                ReflectionHelper.setPrivateValue(GuiScrollingList.class, this, lastScrollDistance, "scrollDistance");
            } catch (ReflectionHelper.UnableToAccessFieldException ignored) {
            }
        }

        private List<RuleEntry> convertToEntries(List<Rule> rules) {
            List<RuleEntry> ruleEntries = new ArrayList<>();
            for (int i = 0, rulesSize = rules.size(); i < rulesSize; i++) {
                Rule rule = rules.get(i);
                ruleEntries.add(new RuleEntry(rule, i));
            }
            return ruleEntries;
        }

        @Override
        protected void drawHeader(int entryRight, int relativeY, Tessellator tess) {
            if (relativeY >= 0) {
                int currentX = this.left + 15;
                GlStateManager.enableBlend();
                RuleEditorGui.this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "If Party notes contain…", currentX, relativeY + 5, 0xFFFFFF);
                currentX += 180;
                RuleEditorGui.this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "…mark party:", currentX, relativeY + 5, 0xFFFFFF);
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
            }
        }

        @Override
        protected int getSize() {
            return rules.getCount();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            this.ruleEntries.get(index).mousePressed(mouseX, mouseY, index);
        }

        @Override
        protected boolean isSelected(int index) {
            return false;
        }

        @Override
        protected void drawBackground() {
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            hoveredEntry = null;
            super.drawScreen(mouseX, mouseY, partialTicks);
            if (hoveredEntry != null) {
                hoveredEntry.drawToolTip(mouseX, mouseY);
            }
        }

        protected void updateFocusedTextField() {
            if (focusedTextField != null) {
                if (focusedTextField.isFocused()) {
                    focusedTextField.updateCursorCounter();
                } else {
                    focusedTextField = null;
                }
            }
        }

        @Override
        protected void drawSlot(int slotIdx, int entryRight, int slotTop, int slotBuffer, Tessellator tess) {
            RuleEntry rule = ruleEntries.get(slotIdx);
            if (rule != null) {
                rule.drawEntry(this.left + 4, slotTop);
                if (mouseY >= slotTop && mouseY < slotTop + this.slotHeight) {
                    // mouse is over this slot
                    hoveredEntry = rule;
                }
            }
        }

        public int keyTyped(char eventChar, int eventKey) {
            int focusedRuleId = -1;
            for (int i = 0, ruleEntriesSize = ruleEntries.size(); i < ruleEntriesSize; i++) {
                RuleEntry ruleEntry = ruleEntries.get(i);
                boolean isFieldFocused = ruleEntry.keyTyped(eventChar, eventKey);
                if (isFieldFocused) {
                    focusedRuleId = i;
                    break;
                }
            }
            return focusedRuleId;
        }

        /**
         * Based on:
         *
         * @see GuiConfigEntries.StringEntry
         * @see GuiEditArrayEntries.BaseEntry
         */
        private class RuleEntry {
            private final Rule rule;
            private final GuiCheckBox chkEnabled;
            private final HoverChecker hoverCheckerEnabled;
            private final GuiTextField fieldTrigger;
            private final List<String> fieldTriggerTooltip;
            private final GuiButton btnTriggerType;
            private final List<String> triggerTypeExplanation;
            private final GuiButton btnPartyType;
            private final HoverChecker hoverCheckerPartyType;
            private final GuiTextField fieldMiddleText;
            private final ItemStack demoItem;
            protected final GuiButton btnAddNewEntryAbove;
            private final HoverChecker hoverCheckerAddNewEntryAbove;
            protected final GuiButton btnRemoveEntry;
            private final HoverChecker hoverCheckerRemoveEntry;
            private final List<String> enabledToolTip;
            private final List<String> partyTypeToolTip;
            private final List<String> addNewToolTip;
            private final List<String> removeToolTip;

            public RuleEntry(Rule rule, int index) {
                this.rule = rule;
                // enabled
                this.chkEnabled = new GuiCheckBox(32, 0, 0, "", rule.isEnabled());

                // trigger:
                this.fieldTrigger = new GuiTextField(52, fontRendererObj, 0, 0, 120, 12);
                this.fieldTrigger.setText(rule.getTriggerText());
                this.fieldTriggerTooltip = new ArrayList<>();
                this.btnTriggerType = new GuiButtonExt(50, 0, 0, 36, 14, this.rule.getTriggerType().toButtonText());
                this.triggerTypeExplanation = new ArrayList<>();
                this.triggerTypeExplanation.add(EnumChatFormatting.BOLD + "Trigger type:");
                this.triggerTypeExplanation.add("Text " + EnumChatFormatting.LIGHT_PURPLE + "= " + EnumChatFormatting.RESET + "basic Text filter");
                this.triggerTypeExplanation.add("Text+ " + EnumChatFormatting.LIGHT_PURPLE + "= " + EnumChatFormatting.RESET + "Advanced text filter with optional excluding \"" + EnumChatFormatting.YELLOW + "(don't match this)" + EnumChatFormatting.RESET
                        + "\" and alternative trigger words \"" + EnumChatFormatting.YELLOW + "one,two" + EnumChatFormatting.RESET + "\"");
                this.triggerTypeExplanation.add("Regex " + EnumChatFormatting.LIGHT_PURPLE + "= " + EnumChatFormatting.RESET + "Java Regular Expressions (experts only)");

                // action:
                btnPartyType = new GuiButtonExt(60, 0, 0, 60, 16, this.rule.getPartyType().toFancyString());

                this.fieldMiddleText = new GuiTextField(54, fontRendererObj, 0, 0, 60, 12);
                String middleText = rule.getMiddleText();
                fieldMiddleText.setText(middleText == null ? "" : Utils.toHumanColorCodes(middleText));

                // preview:
                NBTTagCompound partyNbt = new NBTTagCompound();
                NBTTagCompound skinDetails = new NBTTagCompound();
                String skinUuid;
                String skinTexture;
                switch (index % 4) {
                    case 1: // cake
                        skinUuid = "afb489c4-9fc8-48a4-98f2-dd7bea414c9a";
                        skinTexture = "ZWMyNDFhNTk3YzI4NWUxMDRjMjcxMTk2ZDc4NWRiNGNkMDExMGE0MGM4ZjhlNWQzNTRjNTY0NDE1OTU2N2M5ZCJ9fX0=";
                        break;
                    case 2: // villager
                        skinUuid = "bd482739-767c-45dc-a1f8-c33c40530952";
                        skinTexture = "YjRiZDgzMjgxM2FjMzhlNjg2NDg5MzhkN2EzMmY2YmEyOTgwMWFhZjMxNzQwNDM2N2YyMTRiNzhiNGQ0NzU0YyJ9fX0=";
                        break;
                    case 3: // squid
                        skinUuid = "72e64683-e313-4c36-a408-c66b64e94af5";
                        skinTexture = "NWU4OTEwMWQ1Y2M3NGFhNDU4MDIxYTA2MGY2Mjg5YTUxYTM1YTdkMzRkOGNhZGRmYzNjZGYzYjJjOWEwNzFhIn19fQ==";
                        break;
                    default: // cow
                        skinUuid = "f159b274-c22e-4340-b7c1-52abde147713";
                        skinTexture = "ZDBlNGU2ZmJmNWYzZGNmOTQ0MjJhMWYzMTk0NDhmMTUyMzY5ZDE3OWRiZmJjZGYwMGU1YmZlODQ5NWZhOTc3In19fQ==";
                        break;
                }
                GameProfile gameProfile = new GameProfile(UUID.fromString(skinUuid), null);
                gameProfile.getProperties().put("textures", new Property("url", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv" + skinTexture));
                NBTUtil.writeGameProfile(skinDetails, gameProfile);
                partyNbt.setTag("SkullOwner", skinDetails);
                demoItem = new ItemStack(Items.skull, 1, 3);
                demoItem.setTagCompound(partyNbt);

                // add new & remove entry:
                this.btnAddNewEntryAbove = new GuiButtonExt(0, 0, 0, 18, 18, "+");
                this.btnAddNewEntryAbove.packedFGColour = GuiUtils.getColorCode('2', true);

                this.btnRemoveEntry = new GuiButtonExt(0, 0, 0, 18, 18, "x");
                this.btnRemoveEntry.packedFGColour = GuiUtils.getColorCode('c', true);

                // hover checkers
                this.hoverCheckerEnabled = new HoverChecker(this.chkEnabled, 300);
                this.hoverCheckerPartyType = new HoverChecker(this.btnPartyType, 300);
                this.hoverCheckerAddNewEntryAbove = new HoverChecker(this.btnAddNewEntryAbove, 300);
                this.hoverCheckerRemoveEntry = new HoverChecker(this.btnRemoveEntry, 300);

                // tooltips
                this.enabledToolTip = Collections.singletonList("Enable/disable this rule");
                this.partyTypeToolTip = new ArrayList<>();
                partyTypeToolTip.add("Priority: " + EnumChatFormatting.RED + "⬛" + EnumChatFormatting.WHITE + " > " + EnumChatFormatting.GOLD + "⬛" + EnumChatFormatting.WHITE + " > " + EnumChatFormatting.GREEN + "⬛");
                partyTypeToolTip.add(EnumChatFormatting.GRAY + "= once a party is marked with " + EnumChatFormatting.RED + "⬛" + EnumChatFormatting.GRAY + ", another rule does/can not change it back to e.g. " + EnumChatFormatting.GREEN + "⬛");
                partyTypeToolTip.add("  ‣ suitable " + EnumChatFormatting.GREEN + "⬛");
                partyTypeToolTip.add("  ‣ unideal " + EnumChatFormatting.GOLD + "⬛");
                partyTypeToolTip.add("  ‣ block " + EnumChatFormatting.RED + "⬛");
                this.addNewToolTip = Collections.singletonList(I18n.format("fml.configgui.tooltip.addNewEntryAbove"));
                this.removeToolTip = Collections.singletonList(I18n.format("fml.configgui.tooltip.removeEntry"));
                refreshInputElements();
            }

            private void refreshInputElements() {
                // trigger:
                btnTriggerType.displayString = rule.getTriggerType().toButtonText();
                fieldTriggerTooltip.clear();
                if (rule.isValid()) {
                    String triggerText = this.rule.getTriggerText();
                    if (rule.getTriggerType() == Rule.TriggerType.SIMPLE_REGEX) {
                        String triggerRegex = rule.getTriggerRegex();
                        // matches:
                        String triggerMatch = triggerRegex.replaceAll("\\(\\?<?![^)]+\\)", "").replace("\\(", "(").replace("\\)", ")");
                        if (triggerMatch.contains("|")) {
                            triggerMatch = addFancySeparators(triggerMatch);
                        }
                        fieldTriggerTooltip.add("Matches: " + EnumChatFormatting.YELLOW + triggerMatch);

                        // if not preceded/followed by:
                        Pattern exclusionPattern = Pattern.compile("\\(\\?(<?)!([^)]+)\\)");
                        Matcher exclusionMatcher = exclusionPattern.matcher(triggerRegex);
                        while (exclusionMatcher.find()) {
                            boolean isFollowing = exclusionMatcher.group(1).isEmpty();
                            String notMatching = exclusionMatcher.group(2);
                            if (notMatching.contains("|")) {
                                notMatching = addFancySeparators(notMatching);
                            }
                            fieldTriggerTooltip.add("If not " + (isFollowing ? "followed" : "preceded") + " by: " + EnumChatFormatting.YELLOW + notMatching);
                        }
                    } else if (rule.getTriggerType() == Rule.TriggerType.REGEX) {
                        fieldTriggerTooltip.add("Matches regex: " + EnumChatFormatting.YELLOW + triggerText);
                    } else {
                        // TEXT
                        fieldTriggerTooltip.add("Matches: " + EnumChatFormatting.YELLOW + triggerText);
                    }
                } else {
                    fieldTriggerTooltip.addAll(rule.getPatternException());
                }
            }

            /**
             * Change | separated string into enumeration
             *
             * @see CommandBase#joinNiceString
             */
            public String addFancySeparators(String string) {
                StringBuilder sb = new StringBuilder();
                String[] segments = string.split("\\|");
                for (int i = 0; i < segments.length; ++i) {
                    if (i > 0) {
                        if (i == segments.length - 1) {
                            sb.append(EnumChatFormatting.WHITE).append(", or ").append(EnumChatFormatting.YELLOW);
                        } else {
                            sb.append(EnumChatFormatting.WHITE).append(", ").append(EnumChatFormatting.YELLOW);
                        }
                    }
                    sb.append(segments[i]);
                }
                return sb.toString();
            }

            public void drawEntry(int x, int y) {
                int currentX = x;

                // enabled checkbox:
                chkEnabled.xPosition = currentX;
                chkEnabled.yPosition = y + 2;
                chkEnabled.drawButton(mc, mouseX, mouseY);
                currentX += 20; // button width

                // trigger:
                fieldTrigger.xPosition = currentX;
                fieldTrigger.yPosition = y + 1;
                fieldTrigger.drawTextBox();
                if (rule.isValid()) {
                    fieldTrigger.setTextColor(14737632);
                } else {
                    fieldTrigger.setTextColor(0xFFdd2222);
                }
                currentX += 130; // field width

                if (expertMode) {
                    btnTriggerType.xPosition = currentX - 9;
                    btnTriggerType.yPosition = y;
                    btnTriggerType.drawButton(mc, mouseX, mouseY);
                    currentX += 40; // button width + space
                } else {
                    currentX += 10; // space
                }

                // action:
                btnPartyType.xPosition = currentX;
                btnPartyType.yPosition = y - 1;
                btnPartyType.drawButton(mc, mouseX, mouseY);
                currentX += 65; // button width

                fieldMiddleText.xPosition = currentX;
                fieldMiddleText.yPosition = y + 1;
                fieldMiddleText.drawTextBox();
                currentX += 65; // field width


                // party preview:
                drawItemsPreview(demoItem, currentX, y - 2);

                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, 281);
                double scaleFactor = 0.5;
                GlStateManager.scale(scaleFactor, scaleFactor, 0);
                RuleEditorGui.this.fontRendererObj.drawStringWithShadow(rule.getMiddleText(), (float) ((currentX + 1) / scaleFactor), (float) ((y + 5) / scaleFactor), 0xFFffffff);
                GlStateManager.popMatrix();

                if (MooConfig.dungPartyFinderOverlayDrawBackground) {
                    DataHelper.PartyType partyType = rule.getPartyType();
                    GlStateManager.pushMatrix();
                    if (partyType == DataHelper.PartyType.UNJOINABLE_OR_BLOCK) {
                        GlStateManager.translate(0, 0, partyType.getZIndex());
                    }
                    Gui.drawRect(currentX + 1, y - 1, currentX + 16 + 1, y + 16 - 1, partyType.getColor());
                    GlStateManager.popMatrix();
                }
                currentX += 35;

                // draw add & remove entry buttons:
                this.btnAddNewEntryAbove.visible = true;
                this.btnAddNewEntryAbove.xPosition = currentX;
                this.btnAddNewEntryAbove.yPosition = y - 2;
                this.btnAddNewEntryAbove.drawButton(mc, mouseX, mouseY);
                currentX += 20;

                if (getSize() > 1) {
                    this.btnRemoveEntry.xPosition = currentX;
                    this.btnRemoveEntry.yPosition = y - 2;
                    this.btnRemoveEntry.drawButton(mc, mouseX, mouseY);
                } else {
                    this.btnRemoveEntry.visible = false;
                }

                if (!rule.isEnabled()) {
                    drawRect(x + 18, y - 1, currentX - 36, y + 15, 0x99666666);
                }
            }

            private void drawItemsPreview(ItemStack item, int xFakeHotbar, int yFakeHotbar) {
                GlStateManager.pushMatrix();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/inventory.png"));

                GuiUtils.drawTexturedModalRect(xFakeHotbar, yFakeHotbar, 87, 25, 18, 18, 0);

                GlStateManager.enableRescaleNormal();
                GlStateManager.enableBlend();

                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
                RenderHelper.enableGUIStandardItemLighting();

                int xItem = xFakeHotbar + 1;
                int yItem = yFakeHotbar + 1;
                Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(item, xItem, yItem);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }

            public void drawToolTip(int mouseX, int mouseY) {
                GlStateManager.pushMatrix();
                if (mouseX >= fieldTrigger.xPosition && mouseX <= fieldTrigger.xPosition + fieldTrigger.width
                        && mouseY >= fieldTrigger.yPosition && mouseY <= fieldTrigger.yPosition + fieldTrigger.height) {
                    drawHoveringText(this.fieldTriggerTooltip, mouseX, mouseY);
                } else if (this.hoverCheckerEnabled.checkHover(mouseX, mouseY, true)) {
                    drawHoveringText(this.enabledToolTip, mouseX, mouseY);
                } else if (btnTriggerType.isMouseOver()) {
                    drawHoveringText(this.triggerTypeExplanation, mouseX, mouseY);
                } else if (mouseX >= fieldMiddleText.xPosition && mouseX <= fieldMiddleText.xPosition + fieldMiddleText.width
                        && mouseY >= fieldMiddleText.yPosition && mouseY <= fieldMiddleText.yPosition + fieldMiddleText.height) {
                    drawHoveringText(colorCodesExplanation, mouseX, mouseY);
                } else if (this.hoverCheckerPartyType.checkHover(mouseX, mouseY, true)) {
                    drawHoveringText(partyTypeToolTip, mouseX, mouseY);
                } else if (this.hoverCheckerAddNewEntryAbove.checkHover(mouseX, mouseY, true)) {
                    drawHoveringText(this.addNewToolTip, mouseX, mouseY);
                } else if (this.hoverCheckerRemoveEntry.checkHover(mouseX, mouseY, true)) {
                    drawHoveringText(this.removeToolTip, mouseX, mouseY);
                }
                RenderHelper.disableStandardItemLighting();
                GlStateManager.popMatrix();
            }

            public void mousePressed(int mouseX, int mouseY, int slotIndex) {
                if (focusedTextField != null && focusedTextField.isFocused()) {
                    // un-focus previous text field
                    focusedTextField.setFocused(false);
                }
                fieldTrigger.mouseClicked(mouseX, mouseY, /* left click: */ 0);
                fieldMiddleText.mouseClicked(mouseX, mouseY, /* left click: */ 0);
                if (fieldTrigger.isFocused()) {
                    focusedTextField = fieldTrigger;
                } else if (fieldMiddleText.isFocused()) {
                    focusedTextField = fieldMiddleText;
                } else if (chkEnabled.mousePressed(mc, mouseX, mouseY)) {
                    this.rule.setEnabled(chkEnabled.isChecked());
                } else if (btnTriggerType.mousePressed(mc, mouseX, mouseY)) {
                    boolean isRegex = rule.getTriggerType() == Rule.TriggerType.REGEX;
                    GuiYesNo guiCycleTriggerType = new GuiYesNo((result, id) -> {
                        if (result) {
                            this.rule.toggleTriggerType();
                            refreshInputElements();
                        }
                        mc.displayGuiScreen(RuleEditorGui.this);
                    }, EnumChatFormatting.BOLD + "Switch to " + (isRegex ? "text matcher?" : "Regex?"),
                            isRegex ? "Do you want to change this trigger text back to a text matcher?\n" + EnumChatFormatting.YELLOW + "This will probably break this Party notes rule."
                                    : "Do you want to change this trigger text to Regular Expressions?\nIf you never heard this term, it's not a good idea to do so, because " + EnumChatFormatting.YELLOW + "you can easily break your Party notes rules.",
                            40000000);
                    guiCycleTriggerType.setButtonDelay(60);
                    mc.displayGuiScreen(guiCycleTriggerType);
                } else if (btnPartyType.mousePressed(mc, mouseX, mouseY)) {
                    switch (rule.getPartyType()) {
                        case SUITABLE:
                            rule.setPartyType(DataHelper.PartyType.UNIDEAL);
                            break;
                        case UNIDEAL:
                            rule.setPartyType(DataHelper.PartyType.UNJOINABLE_OR_BLOCK);
                            break;
                        default: // UNJOINABLE_OR_BLOCK
                            rule.setPartyType(DataHelper.PartyType.SUITABLE);
                            break;
                    }
                    btnPartyType.displayString = rule.getPartyType().toFancyString();
                } else if (btnAddNewEntryAbove.mousePressed(mc, mouseX, mouseY)) {
                    addNewRule(slotIndex);
                } else if (btnRemoveEntry.mousePressed(mc, mouseX, mouseY)) {
                    if (isShiftKeyDown()) {
                        deleteRule(slotIndex);
                    } else {
                        // show rule deletion confirmation screen
                        GuiYesNo guiDeleteRule = new GuiYesNo(RuleEditorGui.this,
                                EnumChatFormatting.BOLD + "Delete the rule " + EnumChatFormatting.RESET + EnumChatFormatting.YELLOW + fieldTrigger.getText() + EnumChatFormatting.RESET + EnumChatFormatting.BOLD + "?",
                                EnumChatFormatting.RED + "This action cannot be reverted!\n\n" + EnumChatFormatting.GRAY + "Hint: You can hold SHIFT while clicking the " + EnumChatFormatting.RED + "x " + EnumChatFormatting.GRAY + "button to skip this confirmation screen.",
                                slotIndex);
                        mc.displayGuiScreen(guiDeleteRule);
                    }
                }
            }

            public boolean keyTyped(char eventChar, int eventKey) {
                boolean isFieldFocused = false;
                if (fieldTrigger.isFocused()) {
                    isFieldFocused = true;
                    boolean changedText = fieldTrigger.textboxKeyTyped(eventChar, eventKey);
                    if (changedText) {
                        rule.setTriggerTextAndRevalidateRule(fieldTrigger.getText());
                        refreshInputElements();
                    }
                } else if (fieldMiddleText.isFocused()) {
                    isFieldFocused = true;
                    boolean changedText = fieldMiddleText.textboxKeyTyped(eventChar, eventKey);
                    if (changedText) {
                        rule.setMiddleText(fieldMiddleText.getText());
                        refreshInputElements();
                    }
                }
                return isFieldFocused;
            }
        }
    }
}
