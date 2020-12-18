package de.cowtipper.cowlection.listener;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.CredentialStorage;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.event.ApiErrorEvent;
import de.cowtipper.cowlection.listener.skyblock.DungeonsListener;
import de.cowtipper.cowlection.listener.skyblock.SkyBlockListener;
import de.cowtipper.cowlection.util.GsonUtils;
import de.cowtipper.cowlection.util.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
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
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Keyboard;

public class PlayerListener {
    private final Cowlection main;
    private static DungeonsListener dungeonsListener;
    private static SkyBlockListener skyBlockListener;
    private boolean isPlayerJoiningServer;
    private boolean isOnSkyBlock;

    public PlayerListener(Cowlection main) {
        this.main = main;
    }

    @SubscribeEvent
    public void onKeybindingPressed(InputEvent.KeyInputEvent e) {
        KeyBinding[] keyBindings = Cowlection.keyBindings;

        if (keyBindings[0].isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null && mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN) {
                mc.displayGuiScreen(new GuiChat("/moo "));
            }
        }
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
        if (!isPlayerJoiningServer) {
            isOnSkyBlock = false;
            isPlayerJoiningServer = true;
            main.getVersionChecker().runUpdateCheck(false);
            if (MooConfig.doBestFriendsOnlineCheck && CredentialStorage.isMooValid && main.getFriendsHandler().getBestFriends().size() > 0) {
                main.getFriendsHandler().runBestFriendsOnlineCheck(false);
            }
        }
    }

    @SubscribeEvent
    public void onApiError(ApiErrorEvent e) {
        main.getFriendsHandler().addErroredApiRequest(e.getPlayerName());
    }

    @SubscribeEvent
    public void onWorldEnter(PlayerSetSpawnEvent e) {
        isPlayerJoiningServer = false;
        // check if player is on SkyBlock or on another gamemode
        new TickDelay(() -> {
            ScoreObjective scoreboardSidebar = e.entityPlayer.worldObj.getScoreboard().getObjectiveInDisplaySlot(1);
            boolean wasOnSkyBlock = isOnSkyBlock;
            isOnSkyBlock = (scoreboardSidebar != null && EnumChatFormatting.getTextWithoutFormattingCodes(scoreboardSidebar.getDisplayName()).startsWith("SKYBLOCK"));

            if (!wasOnSkyBlock && isOnSkyBlock) {
                // player wasn't on SkyBlock before but now is on SkyBlock
                main.getLogger().info("Entered SkyBlock! Registering SkyBlock listeners");
                registerSkyBlockListeners();
            } else if (wasOnSkyBlock && !isOnSkyBlock) {
                // player was on SkyBlock before and is now in another gamemode
                unregisterSkyBlockListeners();
                main.getLogger().info("Leaving SkyBlock! Un-registering SkyBlock listeners");
            }
        }, 40); // 2 second delay, making sure scoreboard got sent
    }

    public static boolean registerSkyBlockListeners() {
        if (dungeonsListener == null && skyBlockListener == null) {
            MinecraftForge.EVENT_BUS.register(dungeonsListener = new DungeonsListener(Cowlection.getInstance()));
            MinecraftForge.EVENT_BUS.register(skyBlockListener = new SkyBlockListener());
            return true;
        }
        return false;
    }

    public static void unregisterSkyBlockListeners() {
        Cowlection.getInstance().getDungeonCache().onDungeonLeft();
        if (dungeonsListener != null && skyBlockListener != null) {
            MinecraftForge.EVENT_BUS.unregister(dungeonsListener);
            dungeonsListener = null;
            MinecraftForge.EVENT_BUS.unregister(skyBlockListener);
            skyBlockListener = null;
            Cowlection.getInstance().getLogger().info("Left SkyBlock");
        }
    }

    @SubscribeEvent
    public void onServerLeave(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        // check if player actually was on the server
        if (!isPlayerJoiningServer) {
            main.getFriendsHandler().saveBestFriends();
            main.getPlayerCache().clearAllCaches();
            unregisterSkyBlockListeners();
        }
    }
}
