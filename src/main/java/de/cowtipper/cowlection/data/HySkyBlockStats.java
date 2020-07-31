package de.cowtipper.cowlection.data;

import com.google.common.collect.ComparisonChain;
import com.mojang.realmsclient.util.Pair;
import com.mojang.util.UUIDTypeAdapter;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class HySkyBlockStats {
    private boolean success;
    private String cause;
    private List<Profile> profiles;

    /**
     * No-args constructor for GSON
     */
    private HySkyBlockStats() {
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCause() {
        return cause;
    }

    public Profile getActiveProfile(UUID uuid) {
        if (profiles == null) {
            return null;
        }
        Profile lastSavedProfile = null;
        long latestSave = -1;
        for (Profile profile : profiles) {
            long lastProfileSave = profile.getMember(uuid).last_save;
            if (latestSave < lastProfileSave) {
                lastSavedProfile = profile;
                latestSave = lastProfileSave;
            }
        }
        return lastSavedProfile;
    }

    public static class Profile {
        private String cute_name;
        private Map<String, Member> members;
        private Banking banking;

        /**
         * No-args constructor for GSON
         */
        private Profile() {
        }

        public String getCuteName() {
            return cute_name;
        }

        public Member getMember(UUID uuid) {
            return members.get(UUIDTypeAdapter.fromUUID(uuid));
        }

        public double getCoinBank() {
            return (banking != null) ? banking.balance : -1;
        }

        public int coopCount() {
            return members.size() - 1;
        }

        public double getCoopCoinPurses(UUID stalkedUuid) {
            double coopCoinPurses = 0;
            for (Map.Entry<String, Member> memberEntry : members.entrySet()) {
                if (memberEntry.getKey().equals(UUIDTypeAdapter.fromUUID(stalkedUuid))) {
                    // don't include stalked player's purse again, only coops' purse
                    continue;
                }
                coopCoinPurses += memberEntry.getValue().getCoinPurse();
            }
            return coopCoinPurses;
        }

        public Pair<Integer, Integer> getUniqueMinions() {
            int uniqueMinions = 0;
            int membersWithDisabledApi = 0;
            for (Member member : members.values()) {
                if (member.crafted_generators != null) {
                    if (uniqueMinions > 0) {
                        --uniqueMinions; // subtract duplicate COBBLESTONE_1 minion
                    }
                    uniqueMinions += member.crafted_generators.size();
                } else {
                    ++membersWithDisabledApi;
                }
            }
            return Pair.of(uniqueMinions, membersWithDisabledApi);
        }

        public static class Member {
            private long last_save;
            private long first_join;
            private double coin_purse;
            private NbtData inv_armor;
            private List<String> crafted_generators;
            private int fairy_souls_collected = -1;
            private double experience_skill_farming = -1;
            private double experience_skill_mining = -1;
            private double experience_skill_combat = -1;
            private double experience_skill_foraging = -1;
            private double experience_skill_fishing = -1;
            private double experience_skill_enchanting = -1;
            private double experience_skill_alchemy = -1;
            private double experience_skill_carpentry = -1;
            private double experience_skill_runecrafting = -1;
            private double experience_skill_taming = -1;
            private Map<String, SlayerBossDetails> slayer_bosses;
            private List<Pet> pets;

            /**
             * No-args constructor for GSON
             */
            private Member() {
            }

            public Pair<String, String> getFancyFirstJoined() {
                return Utils.getDurationAsWords(first_join);
            }

            public double getCoinPurse() {
                return coin_purse;
            }

            public int getFairySoulsCollected() {
                return fairy_souls_collected;
            }

            public Map<XpTables.Skill, Integer> getSkills() {
                Map<XpTables.Skill, Integer> skills = new TreeMap<>();
                if (experience_skill_farming >= 0) {
                    skills.put(XpTables.Skill.FARMING, XpTables.Skill.FARMING.getLevel(experience_skill_farming));
                }
                if (experience_skill_mining >= 0) {
                    skills.put(XpTables.Skill.MINING, XpTables.Skill.MINING.getLevel(experience_skill_mining));
                }
                if (experience_skill_combat >= 0) {
                    skills.put(XpTables.Skill.COMBAT, XpTables.Skill.COMBAT.getLevel(experience_skill_combat));
                }
                if (experience_skill_foraging >= 0) {
                    skills.put(XpTables.Skill.FORAGING, XpTables.Skill.FORAGING.getLevel(experience_skill_foraging));
                }
                if (experience_skill_fishing >= 0) {
                    skills.put(XpTables.Skill.FISHING, XpTables.Skill.FISHING.getLevel(experience_skill_fishing));
                }
                if (experience_skill_enchanting >= 0) {
                    skills.put(XpTables.Skill.ENCHANTING, XpTables.Skill.ENCHANTING.getLevel(experience_skill_enchanting));
                }
                if (experience_skill_alchemy >= 0) {
                    skills.put(XpTables.Skill.ALCHEMY, XpTables.Skill.ALCHEMY.getLevel(experience_skill_alchemy));
                }
                if (experience_skill_carpentry >= 0) {
                    skills.put(XpTables.Skill.CARPENTRY, XpTables.Skill.CARPENTRY.getLevel(experience_skill_carpentry));
                }
                if (experience_skill_runecrafting >= 0) {
                    skills.put(XpTables.Skill.RUNECRAFTING, XpTables.Skill.RUNECRAFTING.getLevel(experience_skill_runecrafting));
                }
                if (experience_skill_taming >= 0) {
                    skills.put(XpTables.Skill.TAMING, XpTables.Skill.TAMING.getLevel(experience_skill_taming));
                }
                return skills;
            }

            public Map<XpTables.Slayer, Integer> getSlayerLevels() {
                Map<XpTables.Slayer, Integer> slayerLevels = new EnumMap<>(XpTables.Slayer.class);
                for (XpTables.Slayer slayerBoss : XpTables.Slayer.values()) {
                    SlayerBossDetails bossDetails = slayer_bosses.get(slayerBoss.name().toLowerCase());
                    int slayerLevel = slayerBoss.getLevel(bossDetails.xp);
                    slayerLevels.put(slayerBoss, slayerLevel);
                }
                return slayerLevels;
            }

            public List<Pet> getPets() {
                pets.sort((p1, p2) -> ComparisonChain.start().compare(p2.active, p1.active).compare(p2.getRarity(), p1.getRarity()).compare(p2.exp, p1.exp).result());
                return pets;
            }

            public List<String> getArmor() {
                NBTTagCompound nbt = inv_armor.getDecodedData();
                List<String> armorList = new ArrayList<>();
                if (nbt.hasKey("i", Constants.NBT.TAG_LIST)) {
                    NBTTagList armor = nbt.getTagList("i", Constants.NBT.TAG_COMPOUND);
                    for (int i = 0; i < armor.tagCount(); i++) {
                        NBTTagCompound armorPiece = armor.getCompoundTagAt(i);
                        NBTTagCompound nbtDisplay = armorPiece.getCompoundTag("tag").getCompoundTag("display");
                        if (nbtDisplay != null && nbtDisplay.hasKey("Name", Constants.NBT.TAG_STRING)) {
                            String itemName = nbtDisplay.getString("Name");
                            armorList.add(0, itemName);
                        } else {
                            armorList.add(0, "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "(empty)");
                        }
                    }
                }
                return armorList;
            }

            private static class NbtData {
                private int type;
                private String data;

                private NBTTagCompound getDecodedData() {
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.decodeBase64(this.data))) {
                        return CompressedStreamTools.readCompressed(bis);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new NBTTagCompound();
                    }
                }
            }
        }

        private static class SlayerBossDetails {
            private int xp;
        }

        public static class Pet {
            private String type;
            private double exp;
            private String tier;
            private boolean active;

            public boolean isActive() {
                return active;
            }

            public DataHelper.SkyBlockRarity getRarity() {
                return DataHelper.SkyBlockRarity.valueOf(tier);
            }

            public String toFancyString() {
                return getRarity().getColor() + Utils.fancyCase(type) + " " + getLevel();
            }

            private int getLevel() {
                return XpTables.Pet.getLevel(tier, exp);
            }
        }

        public static class Banking {
            private double balance;
            // private List<Transaction> transactions;

            /**
             * No-args constructor for GSON
             */
            private Banking() {
            }

            //  private class Transaction {
            //      private int amount;
            //      private long timestamp;
            //      private Transaction.Action action;
            //      private String initiator_name;
            //
            //      /**
            //       * No-args constructor for GSON
            //       */
            //      private Transaction() {
            //      }
            //  }
        }
    }
}
