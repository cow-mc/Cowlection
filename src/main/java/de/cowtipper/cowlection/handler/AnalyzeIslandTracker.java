package de.cowtipper.cowlection.handler;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.util.ImageUtils;
import de.cowtipper.cowlection.util.MooChatComponent;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AnalyzeIslandTracker {
    private static final String MINION_WITH_SKIN = EnumChatFormatting.RED + "Unknown minion " + EnumChatFormatting.YELLOW + "(new or with minion skin)";
    private final Map<BlockPos, DataHelper.Minion> minions;
    private int minionsWithSkinCount;
    private final Set<BlockPos> chests;
    private final Set<BlockPos> hoppers;
    private Map<String, DataHelper.Minion> minionsDatabase;
    private final Cowlection main;

    public AnalyzeIslandTracker(Cowlection main) {
        this.main = main;
        minions = new HashMap<>();
        chests = new HashSet<>();
        hoppers = new HashSet<>();
        minionsWithSkinCount = 0;
    }

    @SubscribeEvent
    public void onWorldEnter(PlayerSetSpawnEvent e) {
        MinecraftForge.EVENT_BUS.unregister(this);
        minions.clear();
        chests.clear();
        hoppers.clear();
        minionsWithSkinCount = 0;
        minionsDatabase = null;
    }

    public void analyzeIsland(World world) {
        MinecraftForge.EVENT_BUS.register(this);

        boolean isInitialSearch = false;
        if (minionsDatabase == null) {
            minionsDatabase = DataHelper.getMinions();
            isInitialSearch = true;
        }

        int previousMinionCount = minions.size();
        int previousMinionWithSkinCount = minionsWithSkinCount;
        int previousChestCount = chests.size();
        int previousHopperCount = hoppers.size();

        entityLoop:
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof EntityArmorStand)) {
                continue;
            }
            EntityArmorStand minion = (EntityArmorStand) entity;

            if (minion.isInvisible() || !minion.isSmall() || minion.getHeldItem() == null) {
                // not a minion: invisible, or not small armor stand, or no item in hand (= minion in a minion chair)
                continue;
            }
            for (int slot = 0; slot < 4; slot++) {
                if (minion.getCurrentArmor(slot) == null) {
                    // not a minion: missing equipment
                    continue entityLoop;
                }
            }
            ItemStack skullItem = minion.getCurrentArmor(3); // head slot
            if (skullItem.getItem() instanceof ItemSkull && skullItem.getMetadata() == 3 && skullItem.hasTagCompound()) {
                // is a player head!
                if (skullItem.getTagCompound().hasKey("SkullOwner", Constants.NBT.TAG_COMPOUND)) {
                    NBTTagCompound skullOwner = skullItem.getTagCompound().getCompoundTag("SkullOwner");
                    String skullDataBase64 = skullOwner.getCompoundTag("Properties").getTagList("textures", Constants.NBT.TAG_COMPOUND).getCompoundTagAt(0).getString("Value");
                    String skullData = new String(Base64.decodeBase64(skullDataBase64));
                    String minionSkinId = StringUtils.substringBetween(skullData, "http://textures.minecraft.net/texture/", "\"");
                    DataHelper.Minion detectedMinion = minionsDatabase.get(minionSkinId);
                    if (detectedMinion != null) {
                        // minion head matches one know minion tier
                        this.minions.put(minion.getPosition(), detectedMinion);
                    } else {
                        int minionTier = ImageUtils.getTierFromTexture(minionSkinId);
                        if (minionTier > 0) {
                            DataHelper.Minion overwrittenMinion = this.minions.put(minion.getPosition(), new DataHelper.Minion(MINION_WITH_SKIN, minionTier));
                            if (overwrittenMinion == null || !MINION_WITH_SKIN.equals(overwrittenMinion.getType())) {
                                ++minionsWithSkinCount;
                            }
                        } else {
                            // looked like a minion but has no matching tier badge
                            main.getLogger().info("[/moo analyzeIsland] Found an armor stand that could be a minion but it is missing a tier badge: " + minionSkinId + "\t\t\t" + minion.serializeNBT());
                        }
                    }
                }
            }
        }

        // Tile entities (chests/hoppers)
        for (TileEntity tileEntity : world.loadedTileEntityList) {
            if (tileEntity instanceof TileEntityChest) {
                this.chests.add(tileEntity.getPos());
            } else if (tileEntity instanceof TileEntityHopper) {
                this.hoppers.add(tileEntity.getPos());
            }
        }
        sendAnalysisReport();

        if (isInitialSearch) {
            main.getChatHelper().sendMessage(new MooChatComponent(" (repeat the command in other areas to scan them as well)").lightPurple().setSuggestCommand("/moo analyzeIsland"));
        } else {
            StringBuilder newlyFound = new StringBuilder(" (");
            int minionDiff = minions.size() - previousMinionCount;
            int chestDiff = chests.size() - previousChestCount;
            int hopperDiff = hoppers.size() - previousHopperCount;
            if (minionDiff > 0) {
                newlyFound.append("+").append(minionDiff).append(" Minions ");

                int minionWithSkinDiff = minionsWithSkinCount - previousMinionWithSkinCount;
                if (minionWithSkinDiff > 0) {
                    newlyFound.append("[+").append(minionWithSkinDiff).append(" with skins] ");
                }
            }
            if (chestDiff > 0) {
                newlyFound.append("+").append(chestDiff).append(" Chests ");
            }
            if (hopperDiff > 0) {
                newlyFound.append("+").append(hopperDiff).append(" Hoppers ");
            }

            if (newlyFound.length() > 5) {
                main.getChatHelper().sendMessage(EnumChatFormatting.GRAY, newlyFound.append("compared to previous scan)").toString());
            } else {
                main.getChatHelper().sendMessage(EnumChatFormatting.GRAY, " (= same as previous scan)");
            }
        }
    }

    private void sendAnalysisReport() {
        StringBuilder analysisResults = new StringBuilder("Found ").append(EnumChatFormatting.GOLD).append(this.minions.size()).append(EnumChatFormatting.YELLOW).append(" minions");
        if (minionsWithSkinCount > 0) {
            analysisResults.append(" (").append(EnumChatFormatting.GOLD).append(minionsWithSkinCount).append(EnumChatFormatting.YELLOW).append(" unknown minions)");
        }
        analysisResults.append(" nearby");
        this.minions.values().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // sort alphabetically by minion type and tier
                .forEach(minionEntry -> {
                    DataHelper.Minion minion = minionEntry.getKey();
                    long occurrences = minionEntry.getValue();

                    analysisResults.append("\n  ").append(EnumChatFormatting.GOLD).append(occurrences).append(occurrences > 1 ? "✕ " : "⨉ ")
                            .append(EnumChatFormatting.YELLOW).append(minion.getType())
                            .append(" ")
                            .append(Utils.getMinionTierColor(minion.getTier())).append(MooConfig.useRomanNumerals() ? Utils.convertArabicToRoman(minion.getTier()) : minion.getTier());
                });

        analysisResults.append("\n").append(EnumChatFormatting.YELLOW).append("Found ")
                .append(EnumChatFormatting.GOLD).append(chests.size()).append(EnumChatFormatting.YELLOW).append(" chests and ")
                .append(EnumChatFormatting.GOLD).append(hoppers.size()).append(EnumChatFormatting.YELLOW).append(" hoppers nearby.");

        main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, analysisResults.toString());
    }
}
