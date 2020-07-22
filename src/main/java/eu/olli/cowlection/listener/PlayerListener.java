package eu.olli.cowlection.listener;

import eu.olli.cowlection.Cowlection;
import eu.olli.cowlection.listener.skyblock.DungeonsListener;
import eu.olli.cowlection.listener.skyblock.SkyBlockListener;
import eu.olli.cowlection.util.GsonUtils;
import eu.olli.cowlection.util.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Keyboard;

public class PlayerListener {
    private final Cowlection main;
    private DungeonsListener dungeonsListener;
    private SkyBlockListener skyBlockListener;

    public PlayerListener(Cowlection main) {
        this.main = main;
    }

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre e) {
        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_C && GuiScreen.isCtrlKeyDown()) {
            // ctrl + C
            IInventory inventory;
            String inventoryName;
            if (e.gui instanceof GuiChest) {
                // some kind of chest
                ContainerChest chestContainer = (ContainerChest) ((GuiChest) e.gui).inventorySlots;
                inventory = chestContainer.getLowerChestInventory();
                inventoryName = (inventory.hasCustomName() ? EnumChatFormatting.getTextWithoutFormattingCodes(inventory.getDisplayName().getUnformattedTextForChat()) : inventory.getName());
            } else if (e.gui instanceof GuiInventory) {
                // player inventory
                inventory = Minecraft.getMinecraft().thePlayer.inventory;
                inventoryName = "Player inventory";
            } else {
                // another gui, abort!
                return;
            }
            NBTTagList items = new NBTTagList();
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack item = inventory.getStackInSlot(slot);
                if (item != null) {
                    // slot + item
                    NBTTagCompound tag = new NBTTagCompound();
                    tag.setByte("Slot", (byte) slot);
                    item.writeToNBT(tag);
                    items.appendTag(tag);
                }
            }
            GuiScreen.setClipboardString(GsonUtils.toJson(items));
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Copied " + items.tagCount() + " items from '" + inventoryName + "' to clipboard!");
        }
    }

    @SubscribeEvent
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        main.getVersionChecker().runUpdateCheck(false);
        new TickDelay(() -> main.getChatHelper().sendOfflineMessages(), 6 * 20);
        main.setIsOnSkyBlock(false);
    }

    @SubscribeEvent
    public void onWorldEnter(PlayerSetSpawnEvent e) {
        // check if player is on SkyBlock or on another gamemode
        new TickDelay(() -> {
            ScoreObjective scoreboardSidebar = e.entityPlayer.worldObj.getScoreboard().getObjectiveInDisplaySlot(1);
            boolean wasOnSkyBlock = main.isOnSkyBlock();
            main.setIsOnSkyBlock(scoreboardSidebar != null && EnumChatFormatting.getTextWithoutFormattingCodes(scoreboardSidebar.getDisplayName()).startsWith("SKYBLOCK"));

            if (!wasOnSkyBlock && main.isOnSkyBlock()) {
                // player wasn't on SkyBlock before but now is on SkyBlock
                main.getLogger().info("Entered SkyBlock! Registering SkyBlock listeners");
                registerSkyBlockListeners();
            } else if (wasOnSkyBlock && !main.isOnSkyBlock()) {
                // player was on SkyBlock before and is now in another gamemode
                unregisterSkyBlockListeners();
                main.getLogger().info("Leaving SkyBlock! Un-registering SkyBlock listeners");
            }
        }, 20); // 1 second delay, making sure scoreboard got sent
    }

    private void registerSkyBlockListeners() {
        if (dungeonsListener == null) {
            MinecraftForge.EVENT_BUS.register(dungeonsListener = new DungeonsListener(main));
        }
        if (skyBlockListener == null) {
            MinecraftForge.EVENT_BUS.register(skyBlockListener = new SkyBlockListener(main));
        }
    }

    private void unregisterSkyBlockListeners() {
        if (dungeonsListener != null) {
            MinecraftForge.EVENT_BUS.unregister(dungeonsListener);
            dungeonsListener = null;
        }
        if (skyBlockListener != null) {
            MinecraftForge.EVENT_BUS.unregister(skyBlockListener);
            skyBlockListener = null;
        }
    }

    @SubscribeEvent
    public void onServerLeave(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        main.setIsOnSkyBlock(false);
        main.getFriendsHandler().saveBestFriends();
        main.getPlayerCache().clearAllCaches();
        unregisterSkyBlockListeners();
    }
}
