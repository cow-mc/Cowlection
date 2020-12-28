package de.cowtipper.cowlection.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Slot;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GuiHelper extends GuiScreen {
    private static GuiHelper instance;
    private FontRenderer fontRendererAscii;

    private GuiHelper() {
        this.mc = Minecraft.getMinecraft();
        this.fontRendererObj = mc.fontRendererObj;

        if (this.fontRendererObj.getCharWidth('x') != this.fontRendererObj.getCharWidth('·')) {
            // we're not using default font (x and · should be the same width) - could be unicode font or a custom font
            this.fontRendererAscii = new FontRenderer(mc.gameSettings, new ResourceLocation("textures/font/ascii.png"), mc.renderEngine, false);
            this.fontRendererAscii.onResourceManagerReload(null); // load font widths

        }
        this.itemRender = mc.getRenderItem();
    }

    private static GuiHelper getInstance() {
        if (instance == null) {
            instance = new GuiHelper();
        }
        return instance;
    }

    public static Slot getSlotUnderMouse(GuiChest guiChest) {
        try {
            return ReflectionHelper.getPrivateValue(GuiContainer.class, guiChest, "theSlot", "field_147006_u");
        } catch (ReflectionHelper.UnableToAccessFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Draw a 1 pixel wide horizontal line. Args: startX, endX, y, color
     */
    public static void drawThinHorizontalLine(int startX, int endX, int y, int color) {
        if (endX < startX) {
            int i = startX;
            startX = endX;
            endX = i;
        }
        GlStateManager.pushMatrix();
        int scaleDivisor = 2;
        GlStateManager.scale(1f / scaleDivisor, 1f / scaleDivisor, 0);
        Gui.drawRect(startX * scaleDivisor, y * scaleDivisor, (endX + 1) * scaleDivisor, y * scaleDivisor + 1, color);
        GlStateManager.popMatrix();
    }

    /**
     * Draws a sprite from assets/textures/gui/container/stats_icons.png
     * <p>
     * from: GuiStats#drawSprite
     */
    public static void drawSprite(int x, int y, int width, int height, float zLevel) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(statIcons);
        float magicNumber = 0.0078125F;
        int iconSize = 18;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + iconSize, zLevel).tex((float) (width) * magicNumber, (float) (height + iconSize) * magicNumber).endVertex();
        worldrenderer.pos(x + iconSize, y + iconSize, zLevel).tex((float) (width + iconSize) * magicNumber, (float) (height + iconSize) * magicNumber).endVertex();
        worldrenderer.pos(x + iconSize, y, zLevel).tex((float) (width + iconSize) * magicNumber, (float) (height) * magicNumber).endVertex();
        worldrenderer.pos(x, y, zLevel).tex((float) (width) * magicNumber, (float) (height) * magicNumber).endVertex();
        tessellator.draw();
    }

    public static void drawHoveringText(List<String> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight, int maxTextWidth) {
        if (ForgeVersion.getBuildVersion() < 1808) {
            // we're running a forge version from before 24 March 2016 (http://files.minecraftforge.net/maven/net/minecraftforge/forge/index_1.8.9.html for reference)
            // using mc built-in method
            getInstance().width = screenWidth;
            getInstance().height = screenHeight;
            getInstance().drawHoveringText(textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, false);
        } else {
            // we're on a newer forge version, so we can use the improved tooltip rendering directly added in 1.8.9-11.15.1.1808 (released 03/24/16 09:25 PM) in this pull request: https://github.com/MinecraftForge/MinecraftForge/pull/2649
            GuiUtils.drawHoveringText(textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, getInstance().fontRendererObj);
        }
    }

    public static void drawHoveringTextWithGraph(List<String> toolTip) {
        int mouseX = Mouse.getX() * getInstance().width / getInstance().mc.displayWidth;
        int mouseY = getInstance().height - Mouse.getY() * getInstance().height / getInstance().mc.displayHeight - 1;
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

        getInstance().width = scaledResolution.getScaledWidth();
        getInstance().height = scaledResolution.getScaledHeight();

        getInstance().drawHoveringText(toolTip, mouseX, mouseY, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), -1, true);
    }

    /**
     * Fixed method for forge versions older than 1.8.9-11.15.1.1808
     *
     * @see GuiUtils#drawHoveringText
     */
    private void drawHoveringText(List<String> textLines, final int mouseX, final int mouseY, final int screenWidth, final int screenHeight, final int maxTextWidth, boolean drawGraph) {
        if (!textLines.isEmpty()) {
            FontRenderer font = fontRendererAscii != null ? fontRendererAscii : fontRendererObj;

            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            int tooltipTextWidth = 0;

            for (String textLine : textLines) {
                int textLineWidth = font.getStringWidth(textLine);

                if (textLineWidth > tooltipTextWidth) {
                    tooltipTextWidth = textLineWidth;
                }
            }

            boolean needsWrap = false;

            int titleLinesCount = 1;
            int tooltipX = mouseX + 12;
            if (tooltipX + tooltipTextWidth + 4 > screenWidth) {
                tooltipX = mouseX - 16 - tooltipTextWidth;
                if (tooltipX < 4 && !drawGraph) { // if the tooltip doesn't fit on the screen
                    if (mouseX > screenWidth / 2) {
                        tooltipTextWidth = mouseX - 12 - 8;
                    } else {
                        tooltipTextWidth = screenWidth - 16 - mouseX;
                    }
                    needsWrap = true;
                }
            }

            if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth) {
                tooltipTextWidth = maxTextWidth;
                needsWrap = true;
            }

            if (needsWrap && !drawGraph) {
                int wrappedTooltipWidth = 0;
                List<String> wrappedTextLines = new ArrayList<>();
                for (int i = 0; i < textLines.size(); i++) {
                    String textLine = textLines.get(i);
                    List<String> wrappedLine = font.listFormattedStringToWidth(textLine, tooltipTextWidth);
                    if (i == 0) {
                        titleLinesCount = wrappedLine.size();
                    }

                    for (String line : wrappedLine) {
                        int lineWidth = font.getStringWidth(line);
                        if (lineWidth > wrappedTooltipWidth) {
                            wrappedTooltipWidth = lineWidth;
                        }
                        wrappedTextLines.add(line);
                    }
                }
                tooltipTextWidth = wrappedTooltipWidth;
                textLines = wrappedTextLines;

                if (mouseX > screenWidth / 2) {
                    tooltipX = mouseX - 16 - tooltipTextWidth;
                } else {
                    tooltipX = mouseX + 12;
                }
            }

            int tooltipY = mouseY - 12;
            int tooltipHeight = 8;

            if (textLines.size() > 1) {
                tooltipHeight += (textLines.size() - 1) * 10;
                if (textLines.size() > titleLinesCount) {
                    tooltipHeight += 2; // gap between title lines and next lines
                }
            }

            if (tooltipY + tooltipHeight + 6 > screenHeight) {
                tooltipY = screenHeight - tooltipHeight - 6;
            }

            final int backgroundColor = 0xF0100010;
            Gui.drawRect(tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, backgroundColor);
            Gui.drawRect(tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, backgroundColor);
            Gui.drawRect(tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, backgroundColor);
            Gui.drawRect(tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, backgroundColor);
            Gui.drawRect(tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, backgroundColor);
            final int borderColorStart = 0x505000FF;
            final int borderColorEnd = (borderColorStart & 0xFEFEFE) >> 1 | borderColorStart & 0xFF000000;
            Gui.drawRect(tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, borderColorStart);
            Gui.drawRect(tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, borderColorStart);
            Gui.drawRect(tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 3 + 1, borderColorStart);
            Gui.drawRect(tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderColorEnd);

            List<GraphNode> graphNodes = new ArrayList<>();
            if (drawGraph) {
                graphNodes.addAll(Collections.nCopies(100, null));
            }
            int widthCharX = font.getCharWidth('x') / 2;
            for (int lineNumber = 0; lineNumber < textLines.size(); ++lineNumber) {
                String line = textLines.get(lineNumber);
                String lineWithoutFormattingCodes = EnumChatFormatting.getTextWithoutFormattingCodes(line);

                if (drawGraph && (lineWithoutFormattingCodes.startsWith("│") || lineWithoutFormattingCodes.startsWith("┌") || lineWithoutFormattingCodes.startsWith("└"))) {
                    // use default mc font
                    int substrWidth = 0;
                    int spaceOffset = 0;
                    for (int c = 0; c < Math.min(100, lineWithoutFormattingCodes.length()); c++) {
                        char xOrDot = lineWithoutFormattingCodes.charAt(c);
                        substrWidth += font.getCharWidth(xOrDot);
                        if (xOrDot == 'x') {
                            int index = c - spaceOffset;
                            int yPos = tooltipY + font.FONT_HEIGHT / 2;
                            GraphNode graphNode = graphNodes.get(index);
                            if (graphNode == null) {
                                graphNode = new GraphNode(tooltipX + substrWidth - widthCharX, yPos);
                                graphNodes.set(index, graphNode);
                            } else {
                                graphNode.addY(yPos);
                            }
                        } else if (xOrDot == ' ') {
                            spaceOffset = 1;
                        }
                    }
                    font.drawStringWithShadow(line, (float) tooltipX, (float) tooltipY, -1);
                } else {
                    // use client's font
                    fontRendererObj.drawStringWithShadow(line, (float) tooltipX, (float) tooltipY, -1);
                }

                if (lineNumber + 1 == titleLinesCount) {
                    tooltipY += 2;
                }

                tooltipY += 10;
            }

            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableRescaleNormal();

            if (drawGraph) {
                GlStateManager.pushMatrix();
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glLineWidth(6F);
                GlStateManager.disableTexture2D();
                GlStateManager.color(255 / 255F, 170 / 255F, 0 / 255F);
                WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
                wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
                for (GraphNode graphNode : graphNodes) {
                    if (graphNode == null) {
                        continue;
                    }
                    wr.pos(graphNode.getX(), graphNode.getY(), 0).endVertex();
                }
                Tessellator.getInstance().draw();
                GlStateManager.popMatrix();
            }
        }
    }

    private static class GraphNode {
        private final int x;
        private final List<Integer> y = new ArrayList<>();

        public GraphNode(int x, int y) {
            this.x = x;
            this.y.add(y);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            int sum = 0;
            for (Integer y : this.y) {
                sum += y;
            }
            return sum / this.y.size();
        }

        public void addY(int y) {
            this.y.add(y);
        }
    }
}
