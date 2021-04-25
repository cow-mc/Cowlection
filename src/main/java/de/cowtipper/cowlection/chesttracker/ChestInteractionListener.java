package de.cowtipper.cowlection.chesttracker;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.util.AbortableRunnable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.player.inventory.ContainerLocalMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ChestInteractionListener {
    private TileEntityChest lastInteractedChest = null;
    private boolean interactedWhileSneaking;
    private boolean isOnOwnIsland;
    private AbortableRunnable checkScoreboard;
    private final Cowlection main;

    public ChestInteractionListener(Cowlection main) {
        this.main = main;
        checkIfOnPrivateIsland();
    }

    @SubscribeEvent
    public void onWorldEnter(PlayerSetSpawnEvent e) {
        checkIfOnPrivateIsland();
    }

    private void checkIfOnPrivateIsland() {
        stopScoreboardChecker();
        isOnOwnIsland = false;

        // check if player has entered or left their private island
        checkScoreboard = new AbortableRunnable() {
            private int retries = 20 * 20; // retry for up to 20 seconds

            @SubscribeEvent
            public void onTickCheckScoreboard(TickEvent.ClientTickEvent e) {
                if (!stopped && e.phase == TickEvent.Phase.END) {
                    if (Minecraft.getMinecraft().theWorld == null || retries <= 0) {
                        // already stopped; or world gone, probably disconnected; or no retries left (took too long [20 seconds not enough?] or is not on SkyBlock): stop!
                        stop();
                        return;
                    }
                    retries--;
                    Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
                    ScoreObjective scoreboardSidebar = scoreboard.getObjectiveInDisplaySlot(1);
                    if (scoreboardSidebar == null && retries >= 0) {
                        // scoreboard hasn't loaded yet, retry next tick
                        return;
                    } else if (scoreboardSidebar != null) {
                        // scoreboard loaded!
                        Collection<Score> scoreboardLines = scoreboard.getSortedScores(scoreboardSidebar);
                        for (Score line : scoreboardLines) {
                            ScorePlayerTeam scorePlayerTeam = scoreboard.getPlayersTeam(line.getPlayerName());
                            if (scorePlayerTeam != null) {
                                String lineWithoutFormatting = EnumChatFormatting.getTextWithoutFormattingCodes(scorePlayerTeam.getColorPrefix() + scorePlayerTeam.getColorSuffix());
                                if (lineWithoutFormatting.startsWith(" ⏣")) {
                                    // current location: own private island or somewhere else?
                                    isOnOwnIsland = lineWithoutFormatting.startsWith(" ⏣ Your Island");
                                    break;
                                }
                            }
                        }
                    }
                    stop();
                }
            }

            @Override
            public void stop() {
                if (!stopped) {
                    stopped = true;
                    retries = -1;
                    MinecraftForge.EVENT_BUS.unregister(this);
                    stopScoreboardChecker();
                }
            }

            @Override
            public void run() {
                MinecraftForge.EVENT_BUS.register(this);
            }
        };
        checkScoreboard.run();
    }

    private void stopScoreboardChecker() {
        if (checkScoreboard != null) {
            // there is still a scoreboard-checker running, stop it
            checkScoreboard.stop();
            checkScoreboard = null;
        }
    }

    @SubscribeEvent
    public void onRightClickChest(PlayerInteractEvent e) {
        if (isOnOwnIsland && e.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            TileEntity tileEntity = Minecraft.getMinecraft().theWorld.getTileEntity(e.pos);
            if (tileEntity instanceof TileEntityChest) {
                // Interacted with a chest at position e.pos
                lastInteractedChest = ((TileEntityChest) tileEntity);
                interactedWhileSneaking = Minecraft.getMinecraft().thePlayer.isSneaking();
            }
        }
    }

    @SubscribeEvent
    public void onGuiClose(GuiScreenEvent.KeyboardInputEvent.Pre e) {
        if (isOnOwnIsland && e.gui instanceof GuiChest && Keyboard.getEventKeyState() &&
                // closing chest via ESC or key bind to open (and close) inventory (Default: E)
                (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE || Keyboard.getEventKey() == Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode())) {
            if (lastInteractedChest == null || lastInteractedChest.isInvalid()) {
                // gui wasn't a chest gui or chest got removed
                return;
            }
            BlockPos chestPos = lastInteractedChest.getPos();
            EnumFacing otherChestFacing = getOtherChestFacing(lastInteractedChest);

            if (interactedWhileSneaking) {
                // remove chest from cache
                main.getChestTracker().removeChest(chestPos, otherChestFacing);
            } else {
                // add chest to cache
                ContainerChest chestContainer = (ContainerChest) ((GuiChest) e.gui).inventorySlots;
                ContainerLocalMenu chestInventory = (ContainerLocalMenu) chestContainer.getLowerChestInventory();

                List<ItemStack> chestContents = new ArrayList<>();
                for (int slot = 0; slot < chestInventory.getSizeInventory(); slot++) {
                    ItemStack item = chestInventory.getStackInSlot(slot);
                    if (item != null) {
                        chestContents.add(item);
                    }
                }
                main.getChestTracker().addChest(chestPos, chestContents, otherChestFacing);
            }
            lastInteractedChest = null;
        }
    }

    /**
     * Get the facing of the other chest of a double chest.
     *
     * @param chest clicked chest
     * @return facing of the other chest of a double chest, EnumFacing.UP means no double chest
     */
    private EnumFacing getOtherChestFacing(TileEntityChest chest) {
        EnumFacing otherChestFacing = EnumFacing.UP;
        if (chest.adjacentChestXNeg != null) {
            otherChestFacing = EnumFacing.WEST;
        } else if (chest.adjacentChestXPos != null) {
            otherChestFacing = EnumFacing.EAST;
        } else if (chest.adjacentChestZNeg != null) {
            otherChestFacing = EnumFacing.NORTH;
        } else if (chest.adjacentChestZPos != null) {
            otherChestFacing = EnumFacing.SOUTH;
        }
        return otherChestFacing;
    }

    /**
     * Renders a bounding box around all cached chests.
     * Partially taken from RenderManager#renderDebugBoundingBox
     */
    @SubscribeEvent
    public void highlightChests(DrawBlockHighlightEvent e) {
        if (isOnOwnIsland && Minecraft.getMinecraft().theWorld != null && Minecraft.getMinecraft().thePlayer != null) {
            Set<BlockPos> cachedChestPositions = main.getChestTracker().getCachedPositions();
            if (cachedChestPositions.isEmpty()) {
                return;
            }
            // highlight chests whose contents have already been cached
            Vec3 playerPos = e.player.getPositionEyes(e.partialTicks);
            double xMinOffset = playerPos.xCoord - 0.06;
            double xMaxOffset = playerPos.xCoord + 0.06;
            double yMinOffset = playerPos.yCoord - Minecraft.getMinecraft().thePlayer.getEyeHeight() + /* to avoid z-fighting: */ 0.009999999776482582d;
            double yMaxOffset = playerPos.yCoord + 0.12 - Minecraft.getMinecraft().thePlayer.getEyeHeight();
            double zMinOffset = playerPos.zCoord - 0.06;
            double zMaxOffset = playerPos.zCoord + 0.06;

            GlStateManager.pushMatrix();
            GlStateManager.depthMask(false);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();

            GlStateManager.color(55 / 255f, 155 / 255f, 55 / 255f, 100 / 255f);

            for (BlockPos chestPos : cachedChestPositions) {
                EnumFacing otherChestFacing = main.getChestTracker().getOtherChestFacing(chestPos);
                double chestPosXMin = chestPos.getX() - xMinOffset - (otherChestFacing == EnumFacing.WEST ? 1 : 0);
                double chestPosXMax = chestPos.getX() - xMaxOffset + 1 + (otherChestFacing == EnumFacing.EAST ? 1 : 0);
                double chestPosYMin = chestPos.getY() - yMinOffset;
                double chestPosYMax = chestPos.getY() - yMaxOffset + 1;
                double chestPosZMin = chestPos.getZ() - zMinOffset - (otherChestFacing == EnumFacing.NORTH ? 1 : 0);
                double chestPosZMax = chestPos.getZ() - zMaxOffset + 1 + (otherChestFacing == EnumFacing.SOUTH ? 1 : 0);

                // one coordinate is always either min or max; the other two coords are: min,min > min,max > max,max > max,min

                // down
                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
                worldRenderer.pos(chestPosXMin, chestPosYMin, chestPosZMin).endVertex();
                worldRenderer.pos(chestPosXMin, chestPosYMin, chestPosZMax).endVertex();
                worldRenderer.pos(chestPosXMax, chestPosYMin, chestPosZMax).endVertex();
                worldRenderer.pos(chestPosXMax, chestPosYMin, chestPosZMin).endVertex();
                tessellator.draw();
                // up
                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
                worldRenderer.pos(chestPosXMin, chestPosYMax, chestPosZMin).endVertex();
                worldRenderer.pos(chestPosXMin, chestPosYMax, chestPosZMax).endVertex();
                worldRenderer.pos(chestPosXMax, chestPosYMax, chestPosZMax).endVertex();
                worldRenderer.pos(chestPosXMax, chestPosYMax, chestPosZMin).endVertex();
                tessellator.draw();
                // north
                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
                worldRenderer.pos(chestPosXMin, chestPosYMin, chestPosZMin).endVertex();
                worldRenderer.pos(chestPosXMin, chestPosYMax, chestPosZMin).endVertex();
                worldRenderer.pos(chestPosXMax, chestPosYMax, chestPosZMin).endVertex();
                worldRenderer.pos(chestPosXMax, chestPosYMin, chestPosZMin).endVertex();
                tessellator.draw();
                // south
                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
                worldRenderer.pos(chestPosXMin, chestPosYMin, chestPosZMax).endVertex();
                worldRenderer.pos(chestPosXMin, chestPosYMax, chestPosZMax).endVertex();
                worldRenderer.pos(chestPosXMax, chestPosYMax, chestPosZMax).endVertex();
                worldRenderer.pos(chestPosXMax, chestPosYMin, chestPosZMax).endVertex();
                tessellator.draw();
                // west
                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
                worldRenderer.pos(chestPosXMin, chestPosYMin, chestPosZMin).endVertex();
                worldRenderer.pos(chestPosXMin, chestPosYMin, chestPosZMax).endVertex();
                worldRenderer.pos(chestPosXMin, chestPosYMax, chestPosZMax).endVertex();
                worldRenderer.pos(chestPosXMin, chestPosYMax, chestPosZMin).endVertex();
                tessellator.draw();
                // east
                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
                worldRenderer.pos(chestPosXMax, chestPosYMin, chestPosZMin).endVertex();
                worldRenderer.pos(chestPosXMax, chestPosYMin, chestPosZMax).endVertex();
                worldRenderer.pos(chestPosXMax, chestPosYMax, chestPosZMax).endVertex();
                worldRenderer.pos(chestPosXMax, chestPosYMax, chestPosZMin).endVertex();
                tessellator.draw();
            }
            GlStateManager.enableTexture2D();
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }
}
