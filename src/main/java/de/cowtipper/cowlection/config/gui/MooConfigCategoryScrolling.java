package de.cowtipper.cowlection.config.gui;

import com.google.common.collect.Lists;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.config.MooConfigCategory;
import de.cowtipper.cowlection.search.GuiSearch;
import de.cowtipper.cowlection.util.GuiHelper;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiSlider;
import net.minecraftforge.fml.client.config.HoverChecker;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import java.util.*;

import static net.minecraftforge.fml.client.config.GuiUtils.RESET_CHAR;
import static net.minecraftforge.fml.client.config.GuiUtils.UNDO_CHAR;

/**
 * Based on {@link net.minecraft.client.gui.GuiKeyBindingList}
 */
public class MooConfigCategoryScrolling extends GuiListExtended {
    private final Minecraft mc;
    private final List<IGuiListEntry> listEntries;
    private final Map<Integer, List<String>> explanations;
    private final MooConfigGui parent;
    private int maxListLabelWidth = 0;
    private KeyBindingConfigEntry currentKeyBindingConfigEntry = null;
    /**
     * listEntryIndex => (sub-category) preview
     */
    private final NavigableMap<Integer, MooConfigPreview> listEntriesPreviews;

    public MooConfigCategoryScrolling(MooConfigGui parent, Minecraft mc, MooConfigCategory currentConfigCategory, int marginLeft) {
        super(mc, parent.width - marginLeft, parent.height, 32, parent.height - 5, 20);
        this.parent = parent;
        setSlotXBoundsFromLeft(marginLeft);
        this.mc = mc;
        listEntriesPreviews = new TreeMap<>();
        explanations = new HashMap<>();

        this.listEntries = new ArrayList<>();
        for (MooConfigCategory.SubCategory subCategory : currentConfigCategory.getSubCategories()) {
            int subCategoryStartIndex = this.listEntries.size();
            this.listEntries.add(new MooConfigCategoryScrolling.CategoryEntry(subCategory.getDisplayName(), !subCategory.getExplanations().isEmpty()));

            explanations.put(subCategoryStartIndex, subCategory.getExplanations());
            int explanationsLineCount = 0;
            if (subCategory.getExplanations().size() > 0 && MooConfig.getConfigGuiExplanationsDisplay() == MooConfig.Setting.TEXT) {
                Iterator<String> explanationsIterator = subCategory.getExplanations().iterator();
                explanationsIterator.next(); // skip first entry (= sub category name)
                while (explanationsIterator.hasNext()) {
                    String msgLine = explanationsIterator.next();
                    if (explanationsIterator.hasNext()) {
                        msgLine += "\n" + explanationsIterator.next();
                    }
                    this.listEntries.add(new ExplanationsEntry(msgLine));
                    ++explanationsLineCount;
                }
            }

            // add control buttons to navigate to other guis
            if ("Other settings".equals(subCategory.getDisplayName())) {
                this.listEntries.add(new GuiSwitchEntry("gotoKeyBindings", "Controls", () -> mc.displayGuiScreen(new GuiControls(MooConfigCategoryScrolling.this.parent, mc.gameSettings))));
                this.listEntries.add(new GuiSwitchEntry("gotoLogSearchConfig", "Log Search", () -> mc.displayGuiScreen(new GuiSearch(Cowlection.getInstance().getConfigDirectory(), ""))));
                continue; // don't add properties to main config gui
            }

            // add previews
            Map<Integer, MooConfigPreview> previews = subCategory.getPreviews();
            this.listEntriesPreviews.put(subCategoryStartIndex, null);
            // previews for specific properties
            for (Map.Entry<Integer, MooConfigPreview> previewEntry : previews.entrySet()) {
                this.listEntriesPreviews.put(subCategoryStartIndex + explanationsLineCount + previewEntry.getKey(), previewEntry.getValue());
            }

            // add config elements
            for (Property configEntry : subCategory.getConfigEntries()) {
                int labelWidth = mc.fontRendererObj.getStringWidth(I18n.format(configEntry.getLanguageKey()));

                if (labelWidth > this.maxListLabelWidth) {
                    this.maxListLabelWidth = labelWidth;
                }

                Property.Type type = configEntry.getType();
                if (configEntry.isList() && type == Property.Type.STRING && configEntry.equals(Cowlection.getInstance().getConfig().getTabCompletableNamesCommandsProperty())) {
                    this.listEntries.add(new GuiSwitchEntry("tabCompletableNamesCommands", "➡ modify", () ->
                            mc.displayGuiScreen(new GuiConfig(MooConfigCategoryScrolling.this.parent,
                                    Lists.newArrayList(new ConfigElement(Cowlection.getInstance().getConfig().getTabCompletableNamesCommandsProperty())),
                                    Cowlection.MODID, "cowlectionTabCompletableCommands", false, false,
                                    EnumChatFormatting.GOLD + "Press 2x Done to save changes. " + EnumChatFormatting.RED + "Requires a game restart to take effect!"))));
                    continue;
                } else if (type == Property.Type.BOOLEAN) {
                    this.listEntries.add(new BooleanConfigEntry(configEntry));
                    continue;
                } else if (type == Property.Type.INTEGER) {
                    if (configEntry.getLanguageKey() != null && configEntry.getLanguageKey().endsWith("KeyBinding")) {
                        // special case: key binding
                        this.listEntries.add(new KeyBindingConfigEntry(configEntry));
                        continue;
                    } else if (configEntry.isIntValue() && configEntry.getMinValue() != null && configEntry.getMaxValue() != null) {
                        // generic special case: int value with min & max value
                        this.listEntries.add(new NumberSliderConfigEntry(configEntry,
                                subCategory.getGuiSliderExtra(configEntry.getLanguageKey())));
                        continue;
                    }
                } else if (type == Property.Type.STRING) {
                    if (configEntry.getLanguageKey().equals(Cowlection.MODID + ".config.isMooValid")) {
                        // special case: moo!
                        this.listEntries.add(new BooleanConfigEntry(configEntry));
                        continue;
                    } else if (configEntry.equals(Cowlection.getInstance().getConfig().getMooCmdAliasProperty())) {
                        this.listEntries.add(new GuiSwitchEntry("mooCmdAlias", "➡ modify", () ->
                                mc.displayGuiScreen(new GuiConfig(MooConfigCategoryScrolling.this.parent,
                                        Lists.newArrayList(new ConfigElement(Cowlection.getInstance().getConfig().getMooCmdAliasProperty())),
                                        Cowlection.MODID, "cowlectionMooCmdAlias", false, false,
                                        EnumChatFormatting.GOLD + "Press Done to save changes. " + EnumChatFormatting.RED + "Requires a game restart to take effect!"))));
                        continue;
                    } else if (configEntry.getValidValues() != null && configEntry.getValidValues().length > 0) {
                        if ("dungOverlayTextBorder".equals(configEntry.getName())) {
                            // special case: Dung Performance Overlay: show preview on button click
                            this.listEntries.add(new DungCycleConfigEntry(configEntry));
                        } else {
                            this.listEntries.add(new CycleConfigEntry(configEntry));
                        }
                        continue;
                    }
                }
                // type == Property.Type.DOUBLE
                // type == Property.Type.COLOR // => ChatColorEntry#drawEntry
                // type == Property.Type.MOD_ID
                // + some other cases
                throw new NotImplementedException("Unsupported config entry of type " + configEntry.getType() + " (" + configEntry.getName() + ")");
            }
        }
    }

    protected int getSize() {
        return this.listEntries.size();
    }

    /**
     * Gets the IGuiListEntry object for the given index
     */
    public GuiListExtended.IGuiListEntry getListEntry(int index) {
        return this.listEntries.get(index);
    }

    @Override
    protected void drawSlot(int slotIndex, int x, int y, int slotHeight, int mouseXIn, int mouseYIn) {
        if (y >= this.top - 5 && y <= this.bottom) {
            // entry is visible
            IGuiListEntry listEntry = this.getListEntry(slotIndex);

            MooConfigPreview preview = listEntriesPreviews.get(slotIndex);
            boolean enablePreview = true;
            if (preview != null) {
                // draw preview
                if (listEntry instanceof BaseConfigEntry) {
                    Property property = ((BaseConfigEntry) listEntry).property;
                    if (property.isBooleanValue()) {
                        enablePreview = property.getBoolean();
                    }
                }
                preview.drawPreview(x + getListWidth(), y, mouseXIn, mouseYIn, enablePreview);
            }

            listEntry.drawEntry(slotIndex, x, y, this.getListWidth(), slotHeight, mouseXIn, mouseYIn, this.getSlotIndexFromScreenCoords(mouseXIn, mouseYIn) == slotIndex);
        }
    }

    protected int getScrollBarX() {
        return this.left + 5;
    }

    /**
     * Gets the width of the list (label + property element [button/textbox] + undo and reset buttons; without preview)
     */
    public int getListWidth() {
        return maxListLabelWidth + 135;
    }

    private int hoveredSlotIndex = -2;

    @Override
    public int getSlotIndexFromScreenCoords(int mouseX, int mouseY) {
        int k = mouseY - this.top - this.headerPadding + (int) this.amountScrolled - 4;
        int slotIndex = k / this.slotHeight;
        int result = isMouseYWithinSlotBounds(mouseY) && slotIndex < this.getSize() ? slotIndex : -1;
        if (result != hoveredSlotIndex) {
            hoveredSlotIndex = result;
        }
        return result;
    }

    @Override
    protected void overlayBackground(int startY, int endY, int startAlpha, int endAlpha) {
        // no overlay needed; entries are not drawn when they're out of the draw area
    }

    @Override
    protected void drawContainerBackground(Tessellator tessellator) {
        // default behavior: draw dirt background
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float p_148128_3_) {
        if (this.field_178041_q) {
            this.bindAmountScrolled();
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.drawBackground();
            GlStateManager.disableLighting();
            GlStateManager.disableFog();
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            // Forge: background rendering moved into separate method.
            this.drawContainerBackground(tessellator);
            int x = this.left + 20; // menu + 20 margin
            int y = this.top + 4 - (int) this.amountScrolled;

            if (this.hasListHeader) {
                this.drawListHeader(x, y, tessellator);
            }

            this.drawSelectionBox(x, y, mouseX, mouseY);
            GlStateManager.disableDepth();
            this.overlayBackground(0, this.top, 255, 255);
            this.overlayBackground(this.bottom, this.height, 255, 255);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
            GlStateManager.disableAlpha();
            GlStateManager.shadeModel(7425);
            GlStateManager.disableTexture2D();

            int maxScroll = this.func_148135_f();
            if (maxScroll > 0) {
                // draw scrollbars
                int scrollBarX = this.getScrollBarX();
                int scrollBarXEnd = scrollBarX + 6;

                int k1 = (this.bottom - this.top) * (this.bottom - this.top) / this.getContentHeight();
                k1 = MathHelper.clamp_int(k1, 32, this.bottom - this.top - 8);
                int l1 = (int) this.amountScrolled * (this.bottom - this.top - k1) / maxScroll + this.top;

                if (l1 < this.top) {
                    l1 = this.top;
                }

                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                worldrenderer.pos(scrollBarX, this.bottom, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                worldrenderer.pos(scrollBarXEnd, this.bottom, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                worldrenderer.pos(scrollBarXEnd, this.top, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 255).endVertex();
                worldrenderer.pos(scrollBarX, this.top, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 255).endVertex();
                tessellator.draw();
                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                worldrenderer.pos(scrollBarX, l1 + k1, 0.0D).tex(0.0D, 1.0D).color(128, 128, 128, 255).endVertex();
                worldrenderer.pos(scrollBarXEnd, l1 + k1, 0.0D).tex(1.0D, 1.0D).color(128, 128, 128, 255).endVertex();
                worldrenderer.pos(scrollBarXEnd, l1, 0.0D).tex(1.0D, 0.0D).color(128, 128, 128, 255).endVertex();
                worldrenderer.pos(scrollBarX, l1, 0.0D).tex(0.0D, 0.0D).color(128, 128, 128, 255).endVertex();
                tessellator.draw();
                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                worldrenderer.pos(scrollBarX, l1 + k1 - 1, 0.0D).tex(0.0D, 1.0D).color(192, 192, 192, 255).endVertex();
                worldrenderer.pos(scrollBarXEnd - 1, l1 + k1 - 1, 0.0D).tex(1.0D, 1.0D).color(192, 192, 192, 255).endVertex();
                worldrenderer.pos(scrollBarXEnd - 1, l1, 0.0D).tex(1.0D, 0.0D).color(192, 192, 192, 255).endVertex();
                worldrenderer.pos(scrollBarX, l1, 0.0D).tex(0.0D, 0.0D).color(192, 192, 192, 255).endVertex();
                tessellator.draw();
            }

            this.func_148142_b(mouseX, mouseY); // GuiSlot#renderDecorations
            GlStateManager.enableTexture2D();
            GlStateManager.shadeModel(7424);
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();

            // draw tooltips:
            if (hoveredSlotIndex >= 0) {
                // draw tooltip for category heading
                List<String> explanations = this.explanations.get(hoveredSlotIndex);
                MooConfig.Setting configGuiExplanationsDisplay = MooConfig.getConfigGuiExplanationsDisplay();
                if (explanations != null && !explanations.isEmpty() && (configGuiExplanationsDisplay == MooConfig.Setting.TOOLTIP || configGuiExplanationsDisplay == MooConfig.Setting.SPECIAL)) {
                    if (configGuiExplanationsDisplay == MooConfig.Setting.SPECIAL) {
                        Gui.drawRect(0, 0, this.right, this.bottom + 20, 0x99111111);
                    }
                    GuiHelper.drawHoveringText(explanations, this.getScrollBarX(), mouseY, parent.width, parent.height, parent.width - this.getScrollBarX() - 30);
                    GlStateManager.disableLighting();
                }
                IGuiListEntry hoveredEntry = this.listEntries.get(hoveredSlotIndex);
                if (hoveredEntry instanceof BaseConfigEntry) {
                    // draw tooltip
                    ((BaseConfigEntry) hoveredEntry).checkHover(mouseX, mouseY);
                    ((BaseConfigEntry) hoveredEntry).drawTooltip();

                    MooConfigPreview.drawPreviewHover(mouseX, mouseY);
                }
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (currentKeyBindingConfigEntry != null) {
            // change key binding
            if (keyCode == Keyboard.KEY_ESCAPE) {
                currentKeyBindingConfigEntry.setKeyBinding(-1);
            } else if (keyCode != 0) {
                currentKeyBindingConfigEntry.setKeyBinding(keyCode);
            }
        } else if (this.hoveredSlotIndex >= 0) {
            // move GuiSlider by +/- 1 with left/right arrow key:
            int direction;
            if (keyCode == Keyboard.KEY_LEFT) {
                direction = -1;
            } else if (keyCode == Keyboard.KEY_RIGHT) {
                direction = 1;
            } else {
                // abort!
                return;
            }
            IGuiListEntry listEntry = this.getListEntry(this.hoveredSlotIndex);
            if (listEntry instanceof NumberSliderConfigEntry) {
                NumberSliderConfigEntry configEntry = (NumberSliderConfigEntry) listEntry;
                if (configEntry.btnChangeConfigEntry.enabled && configEntry.btnChangeConfigEntry instanceof GuiSlider) {
                    GuiSlider slider = (GuiSlider) configEntry.btnChangeConfigEntry;
                    slider.setValue(slider.getValue() + direction);
                    configEntry.updateConfigEntryButtonText();
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseEvent) {
        if (currentKeyBindingConfigEntry != null) {
            currentKeyBindingConfigEntry.undoChanges();
        }
        return super.mouseClicked(mouseX, mouseY, mouseEvent);
    }

    public boolean isModifyingKeyBind() {
        return currentKeyBindingConfigEntry != null;
    }

    @SideOnly(Side.CLIENT)
    public class CategoryEntry implements GuiListExtended.IGuiListEntry {
        private final String labelText;
        private final int labelWidth;
        private final boolean hasExplanations;

        public CategoryEntry(String labelText, boolean hasExplanations) {
            this.labelText = "" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + I18n.format(labelText);
            this.labelWidth = MooConfigCategoryScrolling.this.mc.fontRendererObj.getStringWidth(this.labelText);
            this.hasExplanations = hasExplanations;
        }

        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
            GuiHelper.drawThinHorizontalLine(x, MooConfigCategoryScrolling.this.right, y + (slotHeight - MooConfigCategoryScrolling.this.mc.fontRendererObj.FONT_HEIGHT) / 2, 0x99ffffff);

            int yTextPos = y + slotHeight - MooConfigCategoryScrolling.this.mc.fontRendererObj.FONT_HEIGHT + 3;
            if (hasExplanations && MooConfig.getConfigGuiExplanationsDisplay() != MooConfig.Setting.DISABLED) {
                // draw "!" to indicate a sub-category has additional info
                MooConfigCategoryScrolling.this.mc.fontRendererObj.drawString(EnumChatFormatting.DARK_GREEN + "❢", x + 2, yTextPos, 0xffffff);
            }
            // draw sub category label
            int labelX = (int) (x + MooConfigCategoryScrolling.this.maxListLabelWidth * 0.75 - this.labelWidth / 2);
            MooConfigCategoryScrolling.this.mc.fontRendererObj.drawString(this.labelText, labelX, yTextPos, 0xffffff);
        }

        /**
         * Returns true if the mouse has been pressed on this control.
         */
        public boolean mousePressed(int slotIndex, int p_148278_2_, int p_148278_3_, int p_148278_4_, int p_148278_5_, int p_148278_6_) {
            return false;
        }

        /**
         * Fired when the mouse button is released. Arguments: index, x, y, mouseEvent, relativeX, relativeY
         */
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
        }

        public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {
        }
    }

    @SideOnly(Side.CLIENT)
    public class ExplanationsEntry implements IGuiListEntry {
        private final String msgLine;
        private final String msgLine2;

        public ExplanationsEntry(String msgLine) {
            EnumChatFormatting prefix = EnumChatFormatting.YELLOW;
            msgLine = StringUtils.replaceEach(msgLine,
                    new String[]{EnumChatFormatting.YELLOW.toString(), EnumChatFormatting.RESET.toString()},
                    new String[]{EnumChatFormatting.WHITE.toString(), EnumChatFormatting.YELLOW.toString()});
            if (msgLine.contains("\n")) {
                String[] msgLines = msgLine.split("\n", 2);
                this.msgLine = prefix + msgLines[0];
                this.msgLine2 = prefix + msgLines[1];
            } else {
                this.msgLine = prefix + msgLine;
                this.msgLine2 = null;
            }
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
            FontRenderer fontRendererObj = MooConfigCategoryScrolling.this.mc.fontRendererObj;
            fontRendererObj.drawString(msgLine, x, y, 16777215);
            if (msgLine2 != null) {
                fontRendererObj.drawString(msgLine2, x, y + 1 + fontRendererObj.FONT_HEIGHT, 16777215);
            }
        }

        @Override
        public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {
        }


        @Override
        public boolean mousePressed(int slotIndex, int p_148278_2_, int p_148278_3_, int p_148278_4_, int p_148278_5_, int p_148278_6_) {
            return false;
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
        }
    }

    /**
     * Based on {@link net.minecraftforge.fml.client.config.GuiConfigEntries.ListEntryBase} and its subclasses
     */
    @SideOnly(Side.CLIENT)
    public abstract class BaseConfigEntry implements GuiListExtended.IGuiListEntry {
        /**
         * The property specified for this ConfigEntry
         */
        protected final Property property;
        protected final GuiButtonExt btnUndoChanges;
        protected final GuiButtonExt btnDefault;
        protected HoverChecker undoHoverChecker;
        protected HoverChecker defaultHoverChecker;
        protected final String name;
        /**
         * The localized key description for this ConfigEntry
         */
        private final List<String> tooltip;
        private int x;
        private int y;

        private BaseConfigEntry(Property property) {
            this.property = property;
            this.name = I18n.format(property.getLanguageKey());
            this.tooltip = Arrays.asList(I18n.format(property.getLanguageKey() + ".tooltip").split("\\\\n"));

            this.btnUndoChanges = new GuiButtonExt(0, 0, 0, 18, 18, UNDO_CHAR);
            this.btnDefault = new GuiButtonExt(0, 0, 0, 18, 18, RESET_CHAR);

            this.undoHoverChecker = new HoverChecker(this.btnUndoChanges, 250);
            this.defaultHoverChecker = new HoverChecker(this.btnDefault, 500);
        }

        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
            this.x = x;
            this.y = y;
            MooConfigCategoryScrolling.this.mc.fontRendererObj.drawString(this.name, x, y + slotHeight / 2 - MooConfigCategoryScrolling.this.mc.fontRendererObj.FONT_HEIGHT / 2 + 2, 16777215);

            this.btnUndoChanges.xPosition = x + MooConfigCategoryScrolling.this.maxListLabelWidth + 88;
            this.btnUndoChanges.yPosition = y + 1;
            this.btnUndoChanges.enabled = this.isChanged();
            this.btnUndoChanges.drawButton(MooConfigCategoryScrolling.this.mc, mouseX, mouseY);

            this.btnDefault.xPosition = x + MooConfigCategoryScrolling.this.maxListLabelWidth + 88 + 20;
            this.btnDefault.yPosition = y + 1;
            this.btnDefault.enabled = !isDefault();
            this.btnDefault.drawButton(MooConfigCategoryScrolling.this.mc, mouseX, mouseY);
        }

        public void checkHover(int mouseX, int mouseY) {
            if (undoHoverChecker != null && undoHoverChecker.checkHover(mouseX, mouseY)) {
                GuiHelper.drawHoveringText(Collections.singletonList(I18n.format("fml.configgui.tooltip.undoChanges")), mouseX, mouseY, parent.width, parent.height, 300);
            } else if (defaultHoverChecker != null && defaultHoverChecker.checkHover(mouseX, mouseY)) {
                GuiHelper.drawHoveringText(Collections.singletonList(I18n.format("fml.configgui.tooltip.resetToDefault")), mouseX, mouseY, parent.width, parent.height, 300);
            }
        }

        /**
         * Returns true if the mouse has been pressed on this control.
         */
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            if (currentKeyBindingConfigEntry != null && currentKeyBindingConfigEntry != this) {
                // clicked on another undo/reset button while editing a key binding => abort changing key bind
                currentKeyBindingConfigEntry.undoChanges();
            }
            if (this.btnDefault.mousePressed(mc, mouseX, mouseY)) {
                btnDefault.playPressSound(mc.getSoundHandler());
                setToDefault();
                return true;
            } else if (this.btnUndoChanges.mousePressed(mc, mouseX, mouseY)) {
                btnUndoChanges.playPressSound(mc.getSoundHandler());
                undoChanges();
                return true;
            }
            return false;
        }


        /**
         * Fired when the mouse button is released. Arguments: index, x, y, mouseEvent, relativeX, relativeY
         */
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            this.btnUndoChanges.mouseReleased(x, y);
            this.btnDefault.mouseReleased(x, y);

            Cowlection.getInstance().getConfig().syncFromGuiWithoutSaving();
        }

        public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {
        }

        public boolean isDefault() {
            return property.isDefault();
        }

        public abstract boolean isChanged();

        public void setToDefault() {
            property.setToDefault();
        }

        public abstract void undoChanges();

        public void drawTooltip() {
            int mouseX = MooConfigCategoryScrolling.this.mouseX;
            int mouseY = MooConfigCategoryScrolling.this.mouseY;
            if (mouseX >= x && MooConfigCategoryScrolling.this.mouseX < x + maxListLabelWidth && mouseY >= y && mouseY < y + slotHeight) {
                // mouse is over entry
                GuiHelper.drawHoveringText(Lists.newArrayList(tooltip), mouseX, mouseY, width, height, 300);
                GlStateManager.disableLighting();
            }
        }
    }

    public abstract class ButtonConfigEntry extends BaseConfigEntry {
        protected GuiButton btnChangeConfigEntry;

        private ButtonConfigEntry(Property property) {
            super(property);
            this.btnChangeConfigEntry = new GuiButton(0, 0, 0, 78, 20, "Loading...");

            if (property.getLanguageKey().equals(Cowlection.MODID + ".config.isMooValid")) {
                btnChangeConfigEntry.enabled = false;
                btnUndoChanges.enabled = false;
                btnUndoChanges.visible = false;
                btnDefault.enabled = false;
                btnDefault.visible = false;
                defaultHoverChecker = null;
                undoHoverChecker = null;
            }
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
            this.btnChangeConfigEntry.xPosition = x + MooConfigCategoryScrolling.this.maxListLabelWidth + 5;
            this.btnChangeConfigEntry.yPosition = y;
            this.btnChangeConfigEntry.drawButton(MooConfigCategoryScrolling.this.mc, mouseX, mouseY);
            super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected);
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            if (this.btnChangeConfigEntry.mousePressed(mc, mouseX, mouseY)) {
                btnChangeConfigEntry.playPressSound(mc.getSoundHandler());
                changeConfigEntryButtonPressed();
                return true;
            } else {
                return super.mousePressed(slotIndex, mouseX, mouseY, mouseEvent, relativeX, relativeY);
            }
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            super.mouseReleased(slotIndex, x, y, mouseEvent, relativeX, relativeY);
            this.btnChangeConfigEntry.mouseReleased(x, y);
        }

        protected abstract void changeConfigEntryButtonPressed();

        @SuppressWarnings("unused")
        protected abstract void updateConfigEntryButtonText();
    }

    private class BooleanConfigEntry extends ButtonConfigEntry {
        protected final boolean beforeValue;

        public BooleanConfigEntry(Property property) {
            super(property);
            beforeValue = property.getBoolean();

            updateConfigEntryButtonText();
        }

        @Override
        protected void changeConfigEntryButtonPressed() {
            property.set(!property.getBoolean());
            updateConfigEntryButtonText();
        }

        @Override
        public boolean isChanged() {
            return property.getBoolean() != beforeValue;
        }

        @Override
        public void setToDefault() {
            super.setToDefault();
            updateConfigEntryButtonText();
        }

        @Override
        public void undoChanges() {
            property.set(beforeValue);
            updateConfigEntryButtonText();
        }

        @Override
        protected void updateConfigEntryButtonText() {
            btnChangeConfigEntry.displayString = Utils.booleanToSymbol(property.getBoolean());
        }
    }

    private class GuiSwitchEntry extends ButtonConfigEntry {
        private final Runnable runnable;

        private GuiSwitchEntry(String langKey, String buttonText, Runnable runnable) {
            super(new Property("fakeProperty", "fakeValue", Property.Type.STRING, Cowlection.MODID + ".config." + langKey));
            btnChangeConfigEntry.displayString = buttonText;
            this.runnable = runnable;

            this.btnDefault.visible = false;
            this.btnUndoChanges.visible = false;
        }

        @Override
        public boolean isChanged() {
            return false;
        }

        @Override
        public void undoChanges() {
        }

        @Override
        protected void changeConfigEntryButtonPressed() {
            runnable.run();
        }

        @Override
        protected void updateConfigEntryButtonText() {
        }
    }

    private class DungCycleConfigEntry extends CycleConfigEntry {
        private DungCycleConfigEntry(Property property) {
            super(property);
        }

        @Override
        protected void changeConfigEntryButtonPressed() {
            super.changeConfigEntryButtonPressed();
            MooConfigGui.showDungeonPerformanceOverlayUntil = System.currentTimeMillis() + 3000;
        }
    }

    private class CycleConfigEntry extends ButtonConfigEntry {
        protected final int beforeIndex;
        protected final int defaultIndex;
        protected int currentIndex;
        /**
         * Work-around to avoid infinite loop: updateConfigEntryButtonText <> switchDisplayedConfigCategory
         */
        private boolean hasBeenModified = false;

        private CycleConfigEntry(Property property) {
            super(property);
            beforeIndex = getIndex(property.getString());
            defaultIndex = getIndex(property.getDefault());
            currentIndex = beforeIndex;
            updateConfigEntryButtonText();
        }

        private int getIndex(String s) {
            for (int i = 0; i < property.getValidValues().length; i++) {
                if (property.getValidValues()[i].equals(s)) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public boolean isDefault() {
            return currentIndex == defaultIndex;
        }

        @Override
        public boolean isChanged() {
            return currentIndex != beforeIndex;
        }

        @Override
        public void setToDefault() {
            currentIndex = defaultIndex;
            updateConfigEntryButtonText();
        }

        @Override
        public void undoChanges() {
            currentIndex = beforeIndex;
            updateConfigEntryButtonText();
        }

        @Override
        protected void changeConfigEntryButtonPressed() {
            if (++this.currentIndex >= property.getValidValues().length) {
                this.currentIndex = 0;
            }

            updateConfigEntryButtonText();
        }

        @Override
        protected void updateConfigEntryButtonText() {
            String newValue = property.getValidValues()[currentIndex];
            property.setValue(newValue);
            this.btnChangeConfigEntry.displayString = newValue;

            if (hasBeenModified && (Cowlection.MODID + ".config.configGuiExplanations").equals(property.getLanguageKey())) {
                // save properties and re-draw category
                Cowlection.getInstance().getConfig().syncFromGuiWithoutSaving();
                parent.switchDisplayedConfigCategory();
            }
            hasBeenModified = true;
        }
    }

    private class NumberSliderConfigEntry extends ButtonConfigEntry {
        protected final int beforeValue;

        private NumberSliderConfigEntry(Property property, MooConfigCategory.SubCategory.GuiSliderExtra guiSliderExtra) {
            super(property);

            beforeValue = property.getInt();

            int minVal = Integer.parseInt(property.getMinValue());
            int maxVal = Integer.parseInt(property.getMaxValue());

            String prefix = "";
            String suffix = "";
            GuiSlider.ISlider onChangeSliderValue = null;
            if (guiSliderExtra != null) {
                onChangeSliderValue = guiSliderExtra.getOnChangeSliderValue();
                prefix = guiSliderExtra.getPrefix();
                suffix = guiSliderExtra.getSuffix();
            }

            this.btnChangeConfigEntry = new GuiSlider(0, 0, 0, 78, 20, prefix, suffix, minVal, maxVal, beforeValue, false, true, onChangeSliderValue);
            updateConfigEntryButtonText();
            MooConfigGui.showDungeonPerformanceOverlayUntil = 0;
        }

        @Override
        public boolean isDefault() {
            return ((GuiSlider) this.btnChangeConfigEntry).getValueInt() == Integer.parseInt(property.getDefault());
        }

        @Override
        public boolean isChanged() {
            return ((GuiSlider) this.btnChangeConfigEntry).getValueInt() != beforeValue;
        }

        @Override
        public void setToDefault() {
            ((GuiSlider) this.btnChangeConfigEntry).setValue(Double.parseDouble(property.getDefault()));
            updateConfigEntryButtonText();
        }

        @Override
        public void undoChanges() {
            ((GuiSlider) this.btnChangeConfigEntry).setValue(beforeValue);
            updateConfigEntryButtonText();
        }

        @Override
        protected void changeConfigEntryButtonPressed() {
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            if (((GuiSlider) btnChangeConfigEntry).dragging) {
                updateConfigEntryButtonText();
            }
            super.mouseReleased(slotIndex, x, y, mouseEvent, relativeX, relativeY);
        }

        @Override
        protected void updateConfigEntryButtonText() {
            property.setValue(((GuiSlider) this.btnChangeConfigEntry).getValueInt());

            ((GuiSlider) this.btnChangeConfigEntry).updateSlider();
        }
    }

    private class KeyBindingConfigEntry extends ButtonConfigEntry {
        private final int beforeValue;

        public KeyBindingConfigEntry(Property configEntry) {
            super(configEntry);
            beforeValue = property.getInt();

            updateConfigEntryButtonText();
        }

        @Override
        protected void changeConfigEntryButtonPressed() {
            currentKeyBindingConfigEntry = this;
            updateConfigEntryButtonText();
        }

        @Override
        protected void updateConfigEntryButtonText() {
            int keyCode = property.getInt();
            String keyName = keyCode > 1 && keyCode < 256 ? Keyboard.getKeyName(keyCode) : EnumChatFormatting.ITALIC + "none";
            if (currentKeyBindingConfigEntry == this) {
                // key is  is currently being modified
                keyName = EnumChatFormatting.WHITE + "> " + EnumChatFormatting.YELLOW + (keyName != null ? keyName : "Key #" + keyCode) + EnumChatFormatting.WHITE + " <";
            }
            btnChangeConfigEntry.displayString = keyName;
        }

        @Override
        public boolean isChanged() {
            return property.getInt() != beforeValue;
        }

        @Override
        public void undoChanges() {
            property.set(beforeValue);
            currentKeyBindingConfigEntry = null;
            Cowlection.getInstance().getConfig().syncFromGuiWithoutSaving();
            updateConfigEntryButtonText();
        }

        @Override
        public void setToDefault() {
            super.setToDefault();
            currentKeyBindingConfigEntry = null;
            Cowlection.getInstance().getConfig().syncFromGuiWithoutSaving();
            updateConfigEntryButtonText();
        }

        public void setKeyBinding(int keyCode) {
            property.set(Math.min(keyCode, 255));
            currentKeyBindingConfigEntry = null;
            Cowlection.getInstance().getConfig().syncFromGuiWithoutSaving();
            updateConfigEntryButtonText();
        }
    }
}
