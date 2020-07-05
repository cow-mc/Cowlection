package eu.olli.cowlection.data;

import com.mojang.realmsclient.util.Pair;
import com.mojang.util.UUIDTypeAdapter;
import eu.olli.cowlection.util.Utils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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

            public Map<SkillLevel, Double> getSkills() {
                Map<SkillLevel, Double> skills = new TreeMap<>();
                if (experience_skill_farming >= 0) {
                    skills.put(SkillLevel.FARMING, experience_skill_farming);
                }
                if (experience_skill_mining >= 0) {
                    skills.put(SkillLevel.MINING, experience_skill_mining);
                }
                if (experience_skill_combat >= 0) {
                    skills.put(SkillLevel.COMBAT, experience_skill_combat);
                }
                if (experience_skill_foraging >= 0) {
                    skills.put(SkillLevel.FORAGING, experience_skill_foraging);
                }
                if (experience_skill_fishing >= 0) {
                    skills.put(SkillLevel.FISHING, experience_skill_fishing);
                }
                if (experience_skill_enchanting >= 0) {
                    skills.put(SkillLevel.ENCHANTING, experience_skill_enchanting);
                }
                if (experience_skill_alchemy >= 0) {
                    skills.put(SkillLevel.ALCHEMY, experience_skill_alchemy);
                }
                if (experience_skill_carpentry >= 0) {
                    skills.put(SkillLevel.CARPENTRY, experience_skill_carpentry);
                }
                if (experience_skill_runecrafting >= 0) {
                    skills.put(SkillLevel.RUNECRAFTING, experience_skill_runecrafting);
                }
                if (experience_skill_taming >= 0) {
                    skills.put(SkillLevel.TAMING, experience_skill_taming);
                }
                return skills;
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

    public enum SkillLevel {
        FARMING, MINING, COMBAT, FORAGING, FISHING, ENCHANTING, ALCHEMY, CARPENTRY, RUNECRAFTING(true), TAMING;
        private final boolean alternativeXpFormula;
        private static final TreeMap<Integer, Integer> XP_TO_LEVEL = new TreeMap<>();
        private static final TreeMap<Integer, Integer> XP_TO_LEVEL_ALTERNATIVE = new TreeMap<>();

        static {
            // exp data taken from https://api.hypixel.net/resources/skyblock/skills
            XP_TO_LEVEL.put(0, 0);
            XP_TO_LEVEL.put(50, 1);
            XP_TO_LEVEL.put(175, 2);
            XP_TO_LEVEL.put(375, 3);
            XP_TO_LEVEL.put(675, 4);
            XP_TO_LEVEL.put(1175, 5);
            XP_TO_LEVEL.put(1925, 6);
            XP_TO_LEVEL.put(2925, 7);
            XP_TO_LEVEL.put(4425, 8);
            XP_TO_LEVEL.put(6425, 9);
            XP_TO_LEVEL.put(9925, 10);
            XP_TO_LEVEL.put(14925, 11);
            XP_TO_LEVEL.put(22425, 12);
            XP_TO_LEVEL.put(32425, 13);
            XP_TO_LEVEL.put(47425, 14);
            XP_TO_LEVEL.put(67425, 15);
            XP_TO_LEVEL.put(97425, 16);
            XP_TO_LEVEL.put(147425, 17);
            XP_TO_LEVEL.put(222425, 18);
            XP_TO_LEVEL.put(322425, 19);
            XP_TO_LEVEL.put(522425, 20);
            XP_TO_LEVEL.put(822425, 21);
            XP_TO_LEVEL.put(1222425, 22);
            XP_TO_LEVEL.put(1722425, 23);
            XP_TO_LEVEL.put(2322425, 24);
            XP_TO_LEVEL.put(3022425, 25);
            XP_TO_LEVEL.put(3822425, 26);
            XP_TO_LEVEL.put(4722425, 27);
            XP_TO_LEVEL.put(5722425, 28);
            XP_TO_LEVEL.put(6822425, 29);
            XP_TO_LEVEL.put(8022425, 30);
            XP_TO_LEVEL.put(9322425, 31);
            XP_TO_LEVEL.put(10722425, 32);
            XP_TO_LEVEL.put(12222425, 33);
            XP_TO_LEVEL.put(13822425, 34);
            XP_TO_LEVEL.put(15522425, 35);
            XP_TO_LEVEL.put(17322425, 36);
            XP_TO_LEVEL.put(19222425, 37);
            XP_TO_LEVEL.put(21222425, 38);
            XP_TO_LEVEL.put(23322425, 39);
            XP_TO_LEVEL.put(25522425, 40);
            XP_TO_LEVEL.put(27822425, 41);
            XP_TO_LEVEL.put(30222425, 42);
            XP_TO_LEVEL.put(32722425, 43);
            XP_TO_LEVEL.put(35322425, 44);
            XP_TO_LEVEL.put(38072425, 45);
            XP_TO_LEVEL.put(40972425, 46);
            XP_TO_LEVEL.put(44072425, 47);
            XP_TO_LEVEL.put(47472425, 48);
            XP_TO_LEVEL.put(51172425, 49);
            XP_TO_LEVEL.put(55172425, 50);

            XP_TO_LEVEL_ALTERNATIVE.put(0, 0);
            XP_TO_LEVEL_ALTERNATIVE.put(50, 1);
            XP_TO_LEVEL_ALTERNATIVE.put(150, 2);
            XP_TO_LEVEL_ALTERNATIVE.put(275, 3);
            XP_TO_LEVEL_ALTERNATIVE.put(435, 4);
            XP_TO_LEVEL_ALTERNATIVE.put(635, 5);
            XP_TO_LEVEL_ALTERNATIVE.put(885, 6);
            XP_TO_LEVEL_ALTERNATIVE.put(1200, 7);
            XP_TO_LEVEL_ALTERNATIVE.put(1600, 8);
            XP_TO_LEVEL_ALTERNATIVE.put(2100, 9);
            XP_TO_LEVEL_ALTERNATIVE.put(2725, 10);
            XP_TO_LEVEL_ALTERNATIVE.put(3510, 11);
            XP_TO_LEVEL_ALTERNATIVE.put(4510, 12);
            XP_TO_LEVEL_ALTERNATIVE.put(5760, 13);
            XP_TO_LEVEL_ALTERNATIVE.put(7325, 14);
            XP_TO_LEVEL_ALTERNATIVE.put(9325, 15);
            XP_TO_LEVEL_ALTERNATIVE.put(11825, 16);
            XP_TO_LEVEL_ALTERNATIVE.put(14950, 17);
            XP_TO_LEVEL_ALTERNATIVE.put(18950, 18);
            XP_TO_LEVEL_ALTERNATIVE.put(23950, 19);
            XP_TO_LEVEL_ALTERNATIVE.put(30200, 20);
            XP_TO_LEVEL_ALTERNATIVE.put(38050, 21);
            XP_TO_LEVEL_ALTERNATIVE.put(47850, 22);
            XP_TO_LEVEL_ALTERNATIVE.put(60100, 23);
            XP_TO_LEVEL_ALTERNATIVE.put(75400, 24);

        }

        SkillLevel() {
            this(false);
        }

        SkillLevel(boolean alternativeXpFormula) {
            this.alternativeXpFormula = alternativeXpFormula;
        }

        public int getLevel(double exp) {
            if (alternativeXpFormula) {
                return XP_TO_LEVEL_ALTERNATIVE.floorEntry((int) exp).getValue();
            } else {
                return XP_TO_LEVEL.floorEntry((int) exp).getValue();
            }
        }
    }
}
