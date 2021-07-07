package de.cowtipper.cowlection.config.gui;

import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.util.MooChatComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class MooConfigPreview {
    private final Type type;
    private ItemStack[] items;
    private IChatComponent chatComponent;
    private Runnable previewRunnable;
    private long nextPreviewAt;
    public static ItemStack hoveredItem;
    public static IChatComponent hoveredChatComponent;
    public static MooConfigGui parent;

    public MooConfigPreview(IChatComponent chatComponent) {
        this.type = Type.CHAT;
        this.chatComponent = chatComponent;
    }

    public MooConfigPreview(ItemStack... items) {
        this.type = Type.ITEM;
        this.items = items;
    }

    public MooConfigPreview(PreviewChatRunnable previewRunnable) {
        this.type = Type.RUNNABLE;
        this.previewRunnable = previewRunnable;
    }

    public MooConfigPreview(String sound, float volume, float pitch) {
        this.type = Type.RUNNABLE_ON_HOVER;
        previewRunnable = () -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.playSound(sound, volume, pitch);
            }
        };
    }

    public MooConfigPreview(DataHelper.DungeonClass dungeonClass) {
        this(new MooConfigPreview.PreviewChatRunnable() {
            @Override
            public IChatComponent getText() {
                String markWith;
                switch (MooConfig.dungeonPartyMarker(dungeonClass)) {
                    case UNIDEAL_DUPE:
                        markWith = EnumChatFormatting.GOLD + "²⁺" + EnumChatFormatting.YELLOW + dungeonClass.getShortName() + EnumChatFormatting.GRAY + " + " + EnumChatFormatting.GOLD + "⬛ " + EnumChatFormatting.GRAY + "as background";
                        break;
                    case UNIDEAL_ALWAYS:
                        markWith = "" + EnumChatFormatting.WHITE + dungeonClass.getShortName() + EnumChatFormatting.GRAY + " + " + EnumChatFormatting.GOLD + "⬛ " + EnumChatFormatting.GRAY + "as background";
                        break;
                    case BLOCK_DUPE:
                        markWith = EnumChatFormatting.RED + "⬛ " + EnumChatFormatting.GRAY + "as background if duplicated";
                        break;
                    case BLOCK_ALWAYS:
                        markWith = EnumChatFormatting.RED + "⬛ " + EnumChatFormatting.GRAY + "as background";
                        break;
                    default: // do not mark
                        markWith = "" + EnumChatFormatting.DARK_GRAY + EnumChatFormatting.ITALIC + "not marked";
                }
                return new MooChatComponent("Marked with: " + markWith).gray();
            }
        });
    }

    public void drawPreview(int x, int y, int mouseX, int mouseY, boolean enablePreview) {
        switch (type) {
            case ITEM:
                drawItemsPreview(x, y, mouseX, mouseY, enablePreview);
                break;
            case CHAT:
                drawChatPreview(x, y, mouseX, mouseY, enablePreview);
                break;
            case RUNNABLE:
                drawButtonWithRunnablePreview(x, y, mouseX, mouseY, false);
                break;
            case RUNNABLE_ON_HOVER:
                drawButtonWithRunnablePreview(x, y, mouseX, mouseY, true);
                break;
            default:
                // do nothing
                break;
        }
    }

    public static void drawPreviewHover(int mouseX, int mouseY) {
        if (hoveredItem != null) {
            // draw preview item tool tip
            parent.renderToolTip(hoveredItem, mouseX, mouseY);
            hoveredItem = null;
        } else if (hoveredChatComponent != null) {
            // draw hover event of hovered chat component
            parent.handleComponentHover(hoveredChatComponent, mouseX, mouseY);
            hoveredChatComponent = null;
        }
    }

    private void drawItemsPreview(int x, int yFakeHotbar, int mouseX, int mouseY, boolean enablePreview) {
        int xFakeHotbar = x + 15;
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/inventory.png"));

        GuiUtils.drawTexturedModalRect(xFakeHotbar, yFakeHotbar, 87, 25, 18 * items.length, 18, 0);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();

        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();

        int xItem = xFakeHotbar + 1;
        int yItem = yFakeHotbar + 1;
        for (ItemStack item : items) {
            Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(item, xItem, yItem);

            // check mouse hover to draw item tooltip
            if (enablePreview && mouseX >= xItem - 1 && mouseX < xItem + 18 - 1 && mouseY >= yItem - 1 && mouseY < yItem + 18 - 1) {
                hoveredItem = item;
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.colorMask(true, true, true, false);
                // draw white slot hover
                Gui.drawRect(xItem, yItem, xItem + 16, yItem + 16, 0x80FFFFFF);
                GlStateManager.colorMask(true, true, true, true);
                GlStateManager.enableDepth();
                GlStateManager.enableLighting();
            }
            xItem += 18;
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static ItemStack createDemoItem(String itemId, String itemName, String[] lore, Map<String, NBTBase> extraAttributes) {
        return createDemoItem(itemId, itemName, lore, extraAttributes, -1);
    }

    public static ItemStack createDemoItem(String itemId, String itemName, String[] lore, Map<String, NBTBase> extraAttributes, int color) {
        ItemStack demoItem = new ItemStack(Item.getByNameOrId(itemId));

        // lore
        NBTTagCompound display = new NBTTagCompound();
        demoItem.setTagInfo("display", display);
        NBTTagList loreTag = new NBTTagList();
        display.setTag("Lore", loreTag);
        for (String loreEntry : lore) {
            loreTag.appendTag(new NBTTagString(loreEntry));
        }
        // color
        if (color >= 0) {
            display.setInteger("color", color);
        }
        // SkyBlock extra attributes
        NBTTagCompound extraAttributesTag = new NBTTagCompound();
        demoItem.setTagInfo("ExtraAttributes", extraAttributesTag);
        for (Map.Entry<String, NBTBase> extraAttribute : extraAttributes.entrySet()) {
            extraAttributesTag.setTag(extraAttribute.getKey(), extraAttribute.getValue());
        }
        demoItem.setStackDisplayName(itemName);

        return demoItem;
    }

    public static ItemStack createDungeonItem(String modifier, String timestamp, String... lore) {
        Map<String, NBTBase> extraAttributes = new HashMap<>();
        extraAttributes.put("modifier", new NBTTagString(modifier));
        extraAttributes.put("baseStatBoostPercentage", new NBTTagInt(48));
        extraAttributes.put("item_tier", new NBTTagInt(1));
        extraAttributes.put("id", new NBTTagString("SKELETON_SOLDIER_LEGGINGS"));
        extraAttributes.put("timestamp", new NBTTagString(timestamp));


        return createDemoItem("leather_leggings", EnumChatFormatting.DARK_PURPLE + WordUtils.capitalize(modifier) + " Skeleton Soldier Leggings",
                lore, extraAttributes, 16759819);
    }

    private void drawChatPreview(int x, int y, int mouseX, int mouseY, boolean enablePreview) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        int currentX = x + 15;
        int chatY = y + 20 / 2 - fontRenderer.FONT_HEIGHT / 2;

        fontRenderer.drawStringWithShadow(this.chatComponent.getFormattedText(), currentX, chatY, 0xffffffff);
        if (!enablePreview) {
            Gui.drawRect(currentX - 2, chatY - 2, currentX + fontRenderer.getStringWidth(chatComponent.getUnformattedText()) + 1, chatY + fontRenderer.FONT_HEIGHT + 1, 0xdd444444);
        }

        // hover checker for online best friends (partially taken from GuiNewChat#getChatComponent)
        if (enablePreview && mouseY >= chatY && mouseY <= chatY + fontRenderer.FONT_HEIGHT) {
            for (IChatComponent chatComponent : chatComponent) {
                if (chatComponent instanceof ChatComponentText) {
                    currentX += fontRenderer.getStringWidth(GuiUtilRenderComponents.func_178909_a(chatComponent.getUnformattedTextForChat(), false));
                    if (currentX > mouseX) {
                        hoveredChatComponent = chatComponent;
                        break;
                    }
                }
            }
        }
    }

    public static IChatComponent createDemoOnline(String name, String gameMode, String onlineTime) {
        return new MooChatComponent(name).darkGreen().setHover(new MooChatComponent(gameMode).yellow().appendFreshSibling(new MooChatComponent("Online for " + onlineTime).white()));
    }

    private void drawButtonWithRunnablePreview(int x, int y, int mouseX, int mouseY, boolean checkHover) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        int currentX = x + 15;
        int chatY = y + 20 / 2 - fontRenderer.FONT_HEIGHT / 2;

        if (previewRunnable instanceof PreviewChatRunnable) {
            ((PreviewChatRunnable) previewRunnable).setPos(x, y);
            previewRunnable.run();
        } else {
            String previewText = "(hover me to preview)";
            fontRenderer.drawStringWithShadow(previewText, currentX, chatY, 0xffffffff);
            if (checkHover && nextPreviewAt < Minecraft.getSystemTime()
                    && mouseY >= chatY && mouseY <= chatY + fontRenderer.FONT_HEIGHT
                    && mouseX > currentX && mouseX < currentX + fontRenderer.getStringWidth(previewText)) {
                nextPreviewAt = Minecraft.getSystemTime() + 3000;
                previewRunnable.run();
            }
        }
    }

    public abstract static class PreviewChatRunnable implements Runnable {
        private int x;
        private int y;

        public void setPos(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public abstract IChatComponent getText();

        @Override
        public void run() {
            FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
            int currentX = x + 15;
            int chatY = y + 20 / 2 - fontRenderer.FONT_HEIGHT / 2;

            fontRenderer.drawStringWithShadow(getText().getFormattedText(), currentX, chatY, 0xffffffff);
        }
    }

    private enum Type {
        CHAT, ITEM, RUNNABLE, RUNNABLE_ON_HOVER
    }
}
