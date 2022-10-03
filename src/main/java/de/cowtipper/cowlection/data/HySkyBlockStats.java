package de.cowtipper.cowlection.data;

import com.google.common.collect.ComparisonChain;
import com.mojang.realmsclient.util.Pair;
import com.mojang.util.UUIDTypeAdapter;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal", "unused", "MismatchedQueryAndUpdateOfCollection"})
public class HySkyBlockStats {
    private boolean success;
    private String cause;
    private List<Profile> profiles;

    /**
     * No-args constructor for GSON
     */
    private HySkyBlockStats() {
    }

    /**
     * ConcurrentHashMaps don't allow null values, so this acts as a null object
     */
    public HySkyBlockStats(boolean fakeNullElement) {
        success = false;
        cause = "Hypixel API down? Check status.hypixel.net";
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
        for (Profile profile : profiles) {
            if (profile.selected) {
                return profile;
            }
        }
        // no selected profile
        return null;
    }

    public static class Profile {
        public boolean selected;
        private long last_save;
        private String cute_name;
        private Map<String, Member> members;
        private String game_mode;
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

        public String getGameModeIcon() {
            if (StringUtils.isNullOrEmpty(game_mode)) {
                return "";
            } else if ("ironman".equals(game_mode)) {
                return EnumChatFormatting.GRAY + "♲";
            }
        }

        public Pair<String, String> getFancyLastSave() {
            return (last_save > 0) ? Utils.getDurationAsWords(last_save) : null;
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
            private JacobData jacob2;
            private Dungeons dungeons;

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
                    skills.put(XpTables.Skill.FARMING, XpTables.Skill.FARMING.getLevel(experience_skill_farming, getMaxFarmingLevel()));
                }
                if (experience_skill_mining >= 0) {
                    skills.put(XpTables.Skill.MINING, XpTables.Skill.MINING.getLevel(experience_skill_mining, 60));
                }
                if (experience_skill_combat >= 0) {
                    skills.put(XpTables.Skill.COMBAT, XpTables.Skill.COMBAT.getLevel(experience_skill_combat, 60));
                }
                if (experience_skill_foraging >= 0) {
                    skills.put(XpTables.Skill.FORAGING, XpTables.Skill.FORAGING.getLevel(experience_skill_foraging));
                }
                if (experience_skill_fishing >= 0) {
                    skills.put(XpTables.Skill.FISHING, XpTables.Skill.FISHING.getLevel(experience_skill_fishing));
                }
                if (experience_skill_enchanting >= 0) {
                    skills.put(XpTables.Skill.ENCHANTING, XpTables.Skill.ENCHANTING.getLevel(experience_skill_enchanting, 60));
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
                    int slayerLevel = bossDetails != null ? slayerBoss.getLevel(bossDetails.xp) : 0;
                    slayerLevels.put(slayerBoss, slayerLevel);
                }
                return slayerLevels;
            }

            public Dungeons getDungeons() {
                return dungeons;
            }

            public List<Pet> getPets() {
                if (pets == null) {
                    pets = Collections.emptyList();
                } else {
                    pets.sort((p1, p2) -> ComparisonChain.start().compare(p2.active, p1.active).compare(p2.getRarity(), p1.getRarity()).compare(p2.exp, p1.exp).result());
                }
                return pets;
            }

            public Pet getActivePet() {
                for (Pet pet : getPets()) {
                    if (pet.isActive()) {
                        return pet;
                    }
                }
                return null;
            }

            public Pet getPet(String type) {
                for (Pet pet : getPets()) {
                    if (type.equals(pet.type)) {
                        return pet;
                    }
                }
                return null;
            }

            public int getMaxFarmingLevel() {
                int farmingLevelCap = 50;
                if (jacob2 != null && jacob2.perks != null) {
                    farmingLevelCap += jacob2.perks.getOrDefault("farming_level_cap", 0);
                }
                return farmingLevelCap;
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

            public String getType() {
                return type;
            }

            public double getExp() {
                return exp;
            }

            public DataHelper.SkyBlockRarity getRarity() {
                return DataHelper.SkyBlockRarity.valueOf(tier);
            }

            public String toFancyString() {
                return getRarity().getColor() + Utils.fancyCase(type) + " " + getLevel();
            }

            private int getLevel() {
                return XpTables.Pet.getLevel(tier, exp, "GOLDEN_DRAGON".equals(type));
            }
        }

        private static class JacobData {
            private Map<String, Integer> perks;
        }

        public static class Dungeons {
            private Map<String, Type> dungeon_types;
            private Map<DataHelper.DungeonClass, ClassDetails> player_classes;
            private DataHelper.DungeonClass selected_dungeon_class;

            public Map<String, Type> getDungeonTypes() {
                return dungeon_types;
            }

            public Map<DataHelper.DungeonClass, Integer> getClassLevels() {
                Map<DataHelper.DungeonClass, Integer> classLevels = new TreeMap<>();
                for (Map.Entry<DataHelper.DungeonClass, ClassDetails> classEntry : player_classes.entrySet()) {
                    classLevels.put(classEntry.getKey(), classEntry.getValue().getLevel());
                }
                return classLevels;
            }

            public DataHelper.DungeonClass getSelectedClass() {
                return selected_dungeon_class;
            }

            public boolean hasPlayed() {
                if (dungeon_types != null) {
                    for (Type dungeonType : dungeon_types.values()) {
                        if (dungeonType != null && dungeonType.hasPlayed()) {
                            return true;
                        }
                    }
                }
                return false;
            }

            public int getSelectedClassLevel() {
                return player_classes.get(selected_dungeon_class).getLevel();
            }

            public StringBuilder getHighestFloorCompletions(int nHighestFloors, boolean indent) {
                StringBuilder output = new StringBuilder();
                String spacer = indent ? "\n  " : "\n";

                // print detail of 4 latest dungeon types (cata + master cata + xx + master xx)
                Map<String, Type> latestDungeonType = Utils.getLastNMapEntries(dungeon_types, 4);
                for (Map.Entry<String, Type> dungeonTypeEntry : latestDungeonType.entrySet()) {
                    if (!indent) {
                        output.append(spacer);
                    }
                    if (dungeonTypeEntry != null) {
                        if (!dungeonTypeEntry.getValue().hasPlayed()) {
                            // never played this dungeon type
                            continue;
                        }
                        Map<String, Integer> highestFloorCompletions = Utils.getLastNMapEntries(dungeonTypeEntry.getValue().getTierCompletions(), nHighestFloors);
                        String latestDungeonTypeName = Utils.fancyCase(dungeonTypeEntry.getKey());
                        if (highestFloorCompletions != null) {
                            // top n highest floor completions:
                            String highest = highestFloorCompletions.size() > 1 ? highestFloorCompletions.size() + " highest " : "Highest ";
                            String pluralS = highestFloorCompletions.size() > 1 ? "s" : "";
                            output.append(spacer).append(EnumChatFormatting.BOLD).append(highest).append(latestDungeonTypeName).append(" floor").append(pluralS).append(":");

                            for (Map.Entry<String, Integer> highestFloorEntry : highestFloorCompletions.entrySet()) {
                                int highestFloorHighestScore = dungeonTypeEntry.getValue().getBestScore().get(highestFloorEntry.getKey());
                                output.append(spacer).append(EnumChatFormatting.GRAY).append("  Floor ").append(EnumChatFormatting.YELLOW).append(highestFloorEntry.getKey()).append(EnumChatFormatting.GRAY).append(": ")
                                        .append(EnumChatFormatting.GOLD).append(highestFloorEntry.getValue()).append(EnumChatFormatting.GRAY).append(" completions (best score: ").append(EnumChatFormatting.LIGHT_PURPLE).append(highestFloorHighestScore).append(EnumChatFormatting.GRAY).append(")");
                            }
                        } else {
                            // no floor completions yet
                            output.append(spacer).append(EnumChatFormatting.ITALIC).append("No ").append(latestDungeonTypeName).append(" floor completions yet");
                        }
                    }
                }
                return output;
            }

            public String getDungeonTypesLevels() {
                StringBuilder dungeonTypesLevels = new StringBuilder();
                if (dungeon_types != null && !dungeon_types.isEmpty()) {
                    for (Map.Entry<String, HySkyBlockStats.Profile.Dungeons.Type> dungeonType : dungeon_types.entrySet()) {
                        if (dungeonType.getKey().startsWith("master_")) {
                            // master floors don't have their own experience/levels, skip!
                            continue;
                        }
                        if (dungeonTypesLevels.length() == 0) {
                            dungeonTypesLevels.append(EnumChatFormatting.DARK_GRAY).append("  [").append(EnumChatFormatting.GRAY);
                        } else {
                            dungeonTypesLevels.append(EnumChatFormatting.DARK_GRAY).append(", ").append(EnumChatFormatting.GRAY);
                        }
                        int dungeonTypeLevel = dungeonType.getValue().getLevel();
                        dungeonTypesLevels.append(Utils.fancyCase(dungeonType.getKey().substring(0, 4))).append(" ").append(MooConfig.useRomanNumerals() ? Utils.convertArabicToRoman(dungeonTypeLevel) : dungeonTypeLevel);
                    }
                    dungeonTypesLevels.append(EnumChatFormatting.DARK_GRAY).append("]");
                }
                return dungeonTypesLevels.toString();
            }

            public int getTotalDungeonCompletions() {
                int totalDungeonCompletions = 0;
                if (dungeon_types != null) {
                    for (Type dungeonType : dungeon_types.values()) {
                        if (dungeonType != null && dungeonType.hasPlayed()) {
                            totalDungeonCompletions += dungeonType.getTotalTierCompletions();
                        }
                    }
                }
                return totalDungeonCompletions;
            }

            public static class Type {
                private Map<String, Integer> times_played;
                private Map<String, Integer> tier_completions;
                private Map<String, Integer> best_score;
                private int highest_tier_completed;
                private double experience;

                public Map<String, Integer> getTimesPlayed() {
                    return times_played;
                }

                public Map<String, Integer> getTierCompletions() {
                    return tier_completions;
                }

                public Map<String, Integer> getBestScore() {
                    return best_score;
                }

                public int getLevel() {
                    return XpTables.Dungeoneering.DUNGEON.getLevel(experience);
                }

                public boolean hasPlayed() {
                    return experience > 0 || best_score != null;
                }

                public int getTotalTierCompletions() {
                    int totalTierCompletions = 0;
                    if (tier_completions != null) {
                        for (Integer completions : tier_completions.values()) {
                            totalTierCompletions += completions;
                        }
                    }
                    return totalTierCompletions;
                }

                /**
                 * Level [lvl] ([amount]x Floor [highest])
                 *
                 * @return summary text
                 */
                public String getSummary(boolean isMasterFloor) {
                    String floorCompletion;
                    if (tier_completions != null && tier_completions.size() > 0) {
                        int highestTierCompletions = tier_completions.get(String.valueOf(highest_tier_completed));
                        floorCompletion = "" + highestTierCompletions + EnumChatFormatting.GRAY + "x " + EnumChatFormatting.YELLOW + "Floor " + (MooConfig.useRomanNumerals() ? Utils.convertArabicToRoman(highest_tier_completed) : highest_tier_completed);
                    } else {
                        // played dungeons but never completed a floor
                        floorCompletion = "not a single floor...";
                    }
                    return (isMasterFloor ? "" : "Level " + getLevel()) + EnumChatFormatting.GRAY + " (beaten " + EnumChatFormatting.YELLOW + floorCompletion + EnumChatFormatting.GRAY + ")";
                }
            }

            private static class ClassDetails {
                private double experience;

                public int getLevel() {
                    return XpTables.Dungeoneering.CLASS.getLevel(experience);
                }
            }
        }

        public static class Banking {
            private double balance;

            /**
             * No-args constructor for GSON
             */
            private Banking() {
            }
        }
    }
}
