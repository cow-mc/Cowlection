package eu.olli.cowmoonication.listener;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.config.MooConfig;
import eu.olli.cowmoonication.util.TickDelay;
import eu.olli.cowmoonication.util.Utils;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

public class PlayerListener {
    private final Cowmoonication main;
    private final NumberFormat numberFormatter;

    public PlayerListener(Cowmoonication main) {
        this.main = main;
        numberFormatter = NumberFormat.getNumberInstance(Locale.US);
        numberFormatter.setMaximumFractionDigits(0);
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent e) {
        if (!MooConfig.showAdvancedTooltips) {
            return;
        }
        if (e.entityPlayer.openContainer instanceof ContainerChest) {
            // for auction house: show price for each item if multiple items are sold at once
            int stackSize = e.itemStack.stackSize;
            if ((stackSize == 1 && !isSubmitBidItem(e.itemStack)) || e.toolTip.size() < 4) {
                // only 1 item or irrelevant tooltip - nothing to do here, abort!
                return;
            }

            if (isSubmitBidItem(e.itemStack)) {
                // special case: "place bid on an item" interface ("Auction View")
                ItemStack auctionedItem = e.entityPlayer.openContainer.getInventory().get(13);
                stackSize = auctionedItem.stackSize;
                if (stackSize == 1) {
                    // still only 1 item, abort!
                    return;
                }
            }

            List<String> toolTip = e.toolTip;

            // starting with i=1 because first line is never the one we're looking for
            for (int i = 1; i < toolTip.size(); i++) {
                String toolTipLineUnformatted = EnumChatFormatting.getTextWithoutFormattingCodes(toolTip.get(i));
                if (toolTipLineUnformatted.startsWith("Top bid: ")
                        || toolTipLineUnformatted.startsWith("Starting bid: ")
                        || toolTipLineUnformatted.startsWith("Buy it now: ")
                        || toolTipLineUnformatted.startsWith("Sold for: ")
                        || toolTipLineUnformatted.startsWith("New bid: ") /* special case: 'Submit Bid' item */) {

                    try {
                        long price = numberFormatter.parse(StringUtils.substringBetween(toolTipLineUnformatted, ": ", " coins")).longValue();
                        double priceEach = price / (double) stackSize;
                        String formattedPriceEach = priceEach < 5000 ? numberFormatter.format(priceEach) : Utils.formatNumberWithAbbreviations(priceEach);
                        String pricePerItem = EnumChatFormatting.YELLOW + " (" + formattedPriceEach + " each)";
                        toolTip.set(i, toolTip.get(i) + pricePerItem);
                        return;
                    } catch (ParseException ex) {
                        return;
                    }
                }
            }
        }
    }

    private boolean isSubmitBidItem(ItemStack itemStack) {
        return (itemStack.getItem().equals(Items.gold_nugget) || itemStack.getItem().equals(Item.getItemFromBlock(Blocks.gold_block)))
                && (itemStack.hasDisplayName() && (itemStack.getDisplayName().endsWith("Submit Bid") || itemStack.getDisplayName().endsWith("Collect Auction")));
    }

    @SubscribeEvent
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        main.getVersionChecker().runUpdateCheck(false);
        new TickDelay(() -> main.getChatHelper().sendOfflineMessages(), 6 * 20);
    }

    @SubscribeEvent
    public void onServerLeave(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        main.getFriendsHandler().saveBestFriends();
        main.getPlayerCache().clearAllCaches();
    }
}
