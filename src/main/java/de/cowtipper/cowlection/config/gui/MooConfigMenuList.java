package de.cowtipper.cowlection.config.gui;

import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.config.MooConfigCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.client.GuiScrollingList;

/**
 * Config menu displaying a list of config categories
 * <p>
 * Based on {@link net.minecraftforge.fml.client.GuiSlotModList}
 */
public class MooConfigMenuList extends GuiScrollingList {
    private final MooConfigGui parent;

    public MooConfigMenuList(MooConfigGui parent, int listWidth) {
        super(Minecraft.getMinecraft(), listWidth, parent.height, 32, parent.height - 5, 5, 15, parent.width, parent.height);
        this.parent = parent;
    }

    @Override
    protected int getSize() {
        return MooConfig.getConfigCategories().size();
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick) {
        this.parent.selectConfigCategory(index);
    }

    @Override
    protected boolean isSelected(int index) {
        return this.parent.isConfigCategorySelected(index);
    }

    @Override
    protected void drawBackground() {
        this.parent.drawDefaultBackground();
    }

    @Override
    protected int getContentHeight() {
        return (this.getSize()) * 15 + 1;
    }

    @Override
    protected void drawScreen(int mouseX, int mouseY) {
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    protected void drawSlot(int idx, int right, int top, int height, Tessellator tess) {
        MooConfigCategory configCategory = MooConfig.getConfigCategories().get(idx);
        String name = StringUtils.stripControlCodes(configCategory.getMenuDisplayName());
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;

        font.drawString(font.trimStringToWidth(name, listWidth - 10), this.left + 3, top + 2, 0xFFFFFF);
    }

    protected int getRight() {
        return this.right;
    }
}
