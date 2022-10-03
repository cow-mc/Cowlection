package de.cowtipper.cowlection.data;

import java.util.*;

public class XpTables {
    public enum Skill {
        FARMING, MINING, COMBAT, FORAGING, FISHING, ENCHANTING, ALCHEMY, CARPENTRY, RUNECRAFTING(SkillXpTable.RC), SOCIAL(SkillXpTable.SOCIAL), TAMING;
        private final SkillXpTable skillXpTable;

        Skill() {
            this(SkillXpTable.DEFAULT);
        }

        Skill(SkillXpTable skillXpTable) {
            this.skillXpTable = skillXpTable;
        }

        public int getLevel(double exp) {
            return getLevel(exp, skillXpTable == SkillXpTable.DEFAULT ? 50 : 25);
        }

        public int getLevel(double exp, int maxLevel) {
            return Math.min(skillXpTable.getLevel(exp), maxLevel);
        }

        public static double getSkillAverage(int skillLevelsSum) {
            return skillLevelsSum / (getSkillCount() * 1d);
        }

        /**
         * Amount of skills without cosmetic skills (Runecrafting, Social)
         *
         * @return amount of existing skills
         */
        private static int getSkillCount() {
            return values().length - 2;
        }
    }

    private enum SkillXpTable {
        DEFAULT, RC, SOCIAL;
        private static final TreeMap<Integer, Integer> XP_TO_LEVEL = new TreeMap<>();
        private static final TreeMap<Integer, Integer> XP_TO_LEVEL_RC = new TreeMap<>();
        private static final TreeMap<Integer, Integer> XP_TO_LEVEL_SOCIAL = new TreeMap<>();

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
            XP_TO_LEVEL.put(59472425, 51);
            XP_TO_LEVEL.put(64072425, 52);
            XP_TO_LEVEL.put(68972425, 53);
            XP_TO_LEVEL.put(74172425, 54);
            XP_TO_LEVEL.put(79672425, 55);
            XP_TO_LEVEL.put(85472425, 56);
            XP_TO_LEVEL.put(91572425, 57);
            XP_TO_LEVEL.put(97972425, 58);
            XP_TO_LEVEL.put(104672425, 59);
            XP_TO_LEVEL.put(111672425, 60);

            XP_TO_LEVEL_RC.put(0, 0);
            XP_TO_LEVEL_RC.put(50, 1);
            XP_TO_LEVEL_RC.put(150, 2);
            XP_TO_LEVEL_RC.put(275, 3);
            XP_TO_LEVEL_RC.put(435, 4);
            XP_TO_LEVEL_RC.put(635, 5);
            XP_TO_LEVEL_RC.put(885, 6);
            XP_TO_LEVEL_RC.put(1200, 7);
            XP_TO_LEVEL_RC.put(1600, 8);
            XP_TO_LEVEL_RC.put(2100, 9);
            XP_TO_LEVEL_RC.put(2725, 10);
            XP_TO_LEVEL_RC.put(3510, 11);
            XP_TO_LEVEL_RC.put(4510, 12);
            XP_TO_LEVEL_RC.put(5760, 13);
            XP_TO_LEVEL_RC.put(7325, 14);
            XP_TO_LEVEL_RC.put(9325, 15);
            XP_TO_LEVEL_RC.put(11825, 16);
            XP_TO_LEVEL_RC.put(14950, 17);
            XP_TO_LEVEL_RC.put(18950, 18);
            XP_TO_LEVEL_RC.put(23950, 19);
            XP_TO_LEVEL_RC.put(30200, 20);
            XP_TO_LEVEL_RC.put(38050, 21);
            XP_TO_LEVEL_RC.put(47850, 22);
            XP_TO_LEVEL_RC.put(60100, 23);
            XP_TO_LEVEL_RC.put(75400, 24);
            XP_TO_LEVEL_RC.put(94450, 25);

            XP_TO_LEVEL_SOCIAL.put(0, 0);
            XP_TO_LEVEL_SOCIAL.put(50, 1);
            XP_TO_LEVEL_SOCIAL.put(150, 2);
            XP_TO_LEVEL_SOCIAL.put(300, 3);
            XP_TO_LEVEL_SOCIAL.put(550, 4);
            XP_TO_LEVEL_SOCIAL.put(1050, 5);
            XP_TO_LEVEL_SOCIAL.put(1800, 6);
            XP_TO_LEVEL_SOCIAL.put(2800, 7);
            XP_TO_LEVEL_SOCIAL.put(4050, 8);
            XP_TO_LEVEL_SOCIAL.put(5550, 9);
            XP_TO_LEVEL_SOCIAL.put(7550, 10);
            XP_TO_LEVEL_SOCIAL.put(10050, 11);
            XP_TO_LEVEL_SOCIAL.put(13050, 12);
            XP_TO_LEVEL_SOCIAL.put(16800, 13);
            XP_TO_LEVEL_SOCIAL.put(21300, 14);
            XP_TO_LEVEL_SOCIAL.put(27300, 15);
            XP_TO_LEVEL_SOCIAL.put(35300, 16);
            XP_TO_LEVEL_SOCIAL.put(45300, 17);
            XP_TO_LEVEL_SOCIAL.put(57800, 18);
            XP_TO_LEVEL_SOCIAL.put(72800, 19);
            XP_TO_LEVEL_SOCIAL.put(92800, 20);
            XP_TO_LEVEL_SOCIAL.put(117800, 21);
            XP_TO_LEVEL_SOCIAL.put(147800, 22);
            XP_TO_LEVEL_SOCIAL.put(182800, 23);
            XP_TO_LEVEL_SOCIAL.put(222800, 24);
            XP_TO_LEVEL_SOCIAL.put(272800, 25);
        }

        public int getLevel(double exp) {
            TreeMap<Integer, Integer> currentXpTable = XP_TO_LEVEL;
            if (this == RC) {
                currentXpTable = XP_TO_LEVEL_RC;
            } else if (this == SOCIAL) {
                currentXpTable = XP_TO_LEVEL_SOCIAL;
            }

            return currentXpTable.floorEntry((int) exp).getValue();
        }
    }

    public enum Slayer {
        ZOMBIE(SlayerXpTable.ZOMBIE), SPIDER(SlayerXpTable.SPIDER), WOLF, ENDERMAN, BLAZE;
        private final SlayerXpTable slayerXpTable;

        Slayer() {
            this(SlayerXpTable.DEFAULT);
        }

        Slayer(SlayerXpTable slayerXpTable) {
            this.slayerXpTable = slayerXpTable;
        }

        public int getLevel(double exp) {
            return this.slayerXpTable.getLevel(exp);
        }
    }

    private enum SlayerXpTable {
        DEFAULT, ZOMBIE, SPIDER;

        /**
         * Valid for Wolf + Enderman + Blaze
         */
        private static final TreeMap<Integer, Integer> XP_TO_LEVEL = new TreeMap<>();
        private static final TreeMap<Integer, Integer> XP_TO_LEVEL_ZOMBIE = new TreeMap<>();
        private static final TreeMap<Integer, Integer> XP_TO_LEVEL_SPIDER = new TreeMap<>();

        static {
            // exp data taken from https://wiki.hypixel.net/Slayer
            XP_TO_LEVEL.put(0, 0);
            XP_TO_LEVEL.put(10, 1);
            XP_TO_LEVEL.put(30, 2);
            XP_TO_LEVEL.put(250, 3);
            XP_TO_LEVEL.put(1500, 4);
            XP_TO_LEVEL.put(5000, 5);
            XP_TO_LEVEL.put(20000, 6);
            XP_TO_LEVEL.put(100000, 7);
            XP_TO_LEVEL.put(400000, 8);
            XP_TO_LEVEL.put(1000000, 9);

            XP_TO_LEVEL_ZOMBIE.put(0, 0);
            XP_TO_LEVEL_ZOMBIE.put(5, 1);
            XP_TO_LEVEL_ZOMBIE.put(15, 2);
            XP_TO_LEVEL_ZOMBIE.put(200, 3);
            XP_TO_LEVEL_ZOMBIE.put(1000, 4);
            XP_TO_LEVEL_ZOMBIE.put(5000, 5);
            XP_TO_LEVEL_ZOMBIE.put(20000, 6);
            XP_TO_LEVEL_ZOMBIE.put(100000, 7);
            XP_TO_LEVEL_ZOMBIE.put(400000, 8);
            XP_TO_LEVEL_ZOMBIE.put(1000000, 9);

            XP_TO_LEVEL_SPIDER.put(0, 0);
            XP_TO_LEVEL_SPIDER.put(5, 1);
            XP_TO_LEVEL_SPIDER.put(25, 2);
            XP_TO_LEVEL_SPIDER.put(200, 3);
            XP_TO_LEVEL_SPIDER.put(1000, 4);
            XP_TO_LEVEL_SPIDER.put(5000, 5);
            XP_TO_LEVEL_SPIDER.put(20000, 6);
            XP_TO_LEVEL_SPIDER.put(100000, 7);
            XP_TO_LEVEL_SPIDER.put(400000, 8);
            XP_TO_LEVEL_SPIDER.put(1000000, 9);
        }

        public int getLevel(double exp) {
            TreeMap<Integer, Integer> currentXpTable = XP_TO_LEVEL;
            if (this == ZOMBIE) {
                currentXpTable = XP_TO_LEVEL_ZOMBIE;
            } else if (this == SPIDER) {
                currentXpTable = XP_TO_LEVEL_SPIDER;
            }

            return currentXpTable.floorEntry((int) exp).getValue();
        }
    }

    public static final class Pet {
        private static final Map<DataHelper.SkyBlockRarity, TreeSet<Integer>> PET_XP = new HashMap<>();

        private Pet() {
        }

        static {
            for (DataHelper.SkyBlockRarity rarity : DataHelper.SkyBlockRarity.getPetRarities()) {
                PET_XP.put(rarity, new TreeSet<>());
            }
            Collections.addAll(PET_XP.get(DataHelper.SkyBlockRarity.COMMON),
                    0, 100, 210, 330, 460, 605, 765, 940, 1130, 1340, // 1-10
                    1570, 1820, 2095, 2395, 2725, 3085, 3485, 3925, 4415, 4955, // 11-20
                    5555, 6215, 6945, 7745, 8625, 9585, 10635, 11785, 13045, 14425, // 21-30
                    15935, 17585, 19385, 21345, 23475, 25785, 28285, 30985, 33905, 37065, // 31-40
                    40485, 44185, 48185, 52535, 57285, 62485, 68185, 74485, 81485, 89285, // 41-50
                    97985, 107685, 118485, 130485, 143785, 158485, 174685, 192485, 211985, 233285, // 51-60
                    256485, 281685, 309085, 338885, 371285, 406485, 444685, 486085, 530885, 579285, // 61-70
                    631485, 687685, 748085, 812885, 882285, 956485, 1035685, 1120385, 1211085, 1308285, // 71-80
                    1412485, 1524185, 1643885, 1772085, 1909285, 2055985, 2212685, 2380385, 2560085, 2752785, // 81-90
                    2959485, 3181185, 3418885, 3673585, 3946285, 4237985, 4549685, 4883385, 5241085, 5624785); // 91-100
            Collections.addAll(PET_XP.get(DataHelper.SkyBlockRarity.UNCOMMON),
                    0, 175, 365, 575, 805, 1055, 1330, 1630, 1960, 2320, // 1-10
                    2720, 3160, 3650, 4190, 4790, 5450, 6180, 6980, 7860, 8820, // 11-20
                    9870, 11020, 12280, 13660, 15170, 16820, 18620, 20580, 22710, 25020, // 21-30
                    27520, 30220, 33140, 36300, 39720, 43420, 47420, 51770, 56520, 61720, // 31-40
                    67420, 73720, 80720, 88520, 97220, 106920, 117720, 129720, 143020, 157720, // 41-50
                    173920, 191720, 211220, 232520, 255720, 280920, 308320, 338120, 370520, 405720, // 51-60
                    443920, 485320, 530120, 578520, 630720, 686920, 747320, 812120, 881520, 955720, // 61-70
                    1034920, 1119620, 1210320, 1307520, 1411720, 1523420, 1643120, 1771320, 1908520, 2055220, // 71-80
                    2211920, 2379620, 2559320, 2752020, 2958720, 3180420, 3418120, 3672820, 3945520, 4237220, // 81-90
                    4548920, 4882620, 5240320, 5624020, 6035720, 6477420, 6954120, 7470820, 8032520, 8644220); // 91-100
            Collections.addAll(PET_XP.get(DataHelper.SkyBlockRarity.RARE),
                    0, 275, 575, 905, 1265, 1665, 2105, 2595, 3135, 3735, // 1-10
                    4395, 5125, 5925, 6805, 7765, 8815, 9965, 11225, 12605, 14115, // 11-20
                    15765, 17565, 19525, 21655, 23965, 26465, 29165, 32085, 35245, 38665, // 21-30
                    42365, 46365, 50715, 55465, 60665, 66365, 72665, 79665, 87465, 96165, // 31-40
                    105865, 116665, 128665, 141965, 156665, 172865, 190665, 210165, 231465, 254665, // 41-50
                    279865, 307265, 337065, 369465, 404665, 442865, 484265, 529065, 577465, 629665, // 51-60
                    685865, 746265, 811065, 880465, 954665, 1033865, 1118565, 1209265, 1306465, 1410665, // 61-70
                    1522365, 1642065, 1770265, 1907465, 2054165, 2210865, 2378565, 2558265, 2750965, 2957665, // 71-80
                    3179365, 3417065, 3671765, 3944465, 4236165, 4547865, 4881565, 5239265, 5622965, 6034665, // 81-90
                    6476365, 6953065, 7469765, 8031465, 8643165, 9309865, 10036565, 10828265, 11689965, 12626665); // 91-100
            Collections.addAll(PET_XP.get(DataHelper.SkyBlockRarity.EPIC),
                    0, 440, 930, 1470, 2070, 2730, 3460, 4260, 5140, 6100, // 1-10
                    7150, 8300, 9560, 10940, 12450, 14100, 15900, 17860, 19990, 22300, // 11-20
                    24800, 27500, 30420, 33580, 37000, 40700, 44700, 49050, 53800, 59000, // 21-30
                    64700, 71000, 78000, 85800, 94500, 104200, 115000, 127000, 140300, 155000, // 31-40
                    171200, 189000, 208500, 229800, 253000, 278200, 305600, 335400, 367800, 403000, // 41-50
                    441200, 482600, 527400, 575800, 628000, 684200, 744600, 809400, 878800, 953000, // 51-60
                    1032200, 1116900, 1207600, 1304800, 1409000, 1520700, 1640400, 1768600, 1905800, 2052500, // 61-70
                    2209200, 2376900, 2556600, 2749300, 2956000, 3177700, 3415400, 3670100, 3942800, 4234500, // 71-80
                    4546200, 4879900, 5237600, 5621300, 6033000, 6474700, 6951400, 7468100, 8029800, 8641500, // 81-90
                    9308200, 10034900, 10826600, 11688300, 12625000, 13641700, 14743400, 15935100, 17221800, 18608500); // 91-100
            Collections.addAll(PET_XP.get(DataHelper.SkyBlockRarity.LEGENDARY),
                    0, 660, 1390, 2190, 3070, 4030, 5080, 6230, 7490, 8870, // 1-10
                    10380, 12030, 13830, 15790, 17920, 20230, 22730, 25430, 28350, 31510, // 11-20
                    34930, 38630, 42630, 46980, 51730, 56930, 62630, 68930, 75930, 83730, // 21-30
                    92430, 102130, 112930, 124930, 138230, 152930, 169130, 186930, 206430, 227730, // 31-40
                    250930, 276130, 303530, 333330, 365730, 400930, 439130, 480530, 525330, 573730, // 41-50
                    625930, 682130, 742530, 807330, 876730, 950930, 1030130, 1114830, 1205530, 1302730, // 51-60
                    1406930, 1518630, 1638330, 1766530, 1903730, 2050430, 2207130, 2374830, 2554530, 2747230, // 61-70
                    2953930, 3175630, 3413330, 3668030, 3940730, 4232430, 4544130, 4877830, 5235530, 5619230, // 71-80
                    6030930, 6472630, 6949330, 7466030, 8027730, 8639430, 9306130, 10032830, 10824530, 11686230, // 81-90
                    12622930, 13639630, 14741330, 15933030, 17219730, 18606430, 20103130, 21719830, 23466530, 25353230); // 91-100
        }

        public static int getLevel(String rarity, double exp, boolean hasMaxLvl200) {
            DataHelper.SkyBlockRarity petRarity = DataHelper.SkyBlockRarity.valueOf(rarity);
            if (petRarity == DataHelper.SkyBlockRarity.MYTHIC) {
                // special case: Mystic pets
                petRarity = DataHelper.SkyBlockRarity.LEGENDARY;
            }
            TreeSet<Integer> xpToLevels = PET_XP.get(petRarity);
            if (xpToLevels != null) {
                int petLevel = xpToLevels.headSet((int) exp, true).size();
                if (hasMaxLvl200 && petRarity == DataHelper.SkyBlockRarity.LEGENDARY && petLevel == 100) {
                    // after lvl 100: exp from lvl 99-100 for each level until lvl 200
                    int overflowLevels = 1; // lvl 101 = 0 additional exp over lvl 100
                    double expOverLvl100 = exp - xpToLevels.last();
                    if (expOverLvl100 >= 1) {
                        overflowLevels += 1; // lvl 102 = 1 additional exp over lvl 100
                        expOverLvl100 -= 1;
                        overflowLevels += Math.min(98, (int) (expOverLvl100 / 1886700));
                    }
                    if (overflowLevels > 0) {
                        petLevel += overflowLevels;
                    }
                }
                return petLevel;
            } else {
                return -1;
            }
        }

        public static int getTotalExp(DataHelper.SkyBlockRarity rarity, int level, int exp) {
            TreeSet<Integer> xpToLevels = PET_XP.get(rarity);
            if (xpToLevels != null) {
                if (level > 100) {
                    int expOverLvl100 = (level >= 102 ? 1 : 0) + Math.max(0, (level - 102) * 1886700);
                    return xpToLevels.last() + expOverLvl100 + exp;
                } else {
                    for (int xpToLevel : xpToLevels) {
                        if (level-- <= 1) {
                            return xpToLevel + exp;
                        }
                    }
                }
            }
            return -1;
        }
    }

    public enum Dungeoneering {
        CLASS, // classes: Archer, Berserk, Healer, Mage, Tank
        DUNGEON; // dungeon types: Catacombs, ...

        private static final TreeMap<Integer, Integer> XP_TO_LEVEL = new TreeMap<>();

        static {
            XP_TO_LEVEL.put(0, 0);
            XP_TO_LEVEL.put(50, 1);
            XP_TO_LEVEL.put(125, 2);
            XP_TO_LEVEL.put(235, 3);
            XP_TO_LEVEL.put(395, 4);
            XP_TO_LEVEL.put(625, 5);
            XP_TO_LEVEL.put(955, 6);
            XP_TO_LEVEL.put(1425, 7);
            XP_TO_LEVEL.put(2095, 8);
            XP_TO_LEVEL.put(3045, 9);
            XP_TO_LEVEL.put(4385, 10);
            XP_TO_LEVEL.put(6275, 11);
            XP_TO_LEVEL.put(8940, 12);
            XP_TO_LEVEL.put(12700, 13);
            XP_TO_LEVEL.put(17960, 14);
            XP_TO_LEVEL.put(25340, 15);
            XP_TO_LEVEL.put(35640, 16);
            XP_TO_LEVEL.put(50040, 17);
            XP_TO_LEVEL.put(70040, 18);
            XP_TO_LEVEL.put(97640, 19);
            XP_TO_LEVEL.put(135640, 20);
            XP_TO_LEVEL.put(188140, 21);
            XP_TO_LEVEL.put(259640, 22);
            XP_TO_LEVEL.put(356640, 23);
            XP_TO_LEVEL.put(488640, 24);
            XP_TO_LEVEL.put(668640, 25);
            XP_TO_LEVEL.put(911640, 26);
            XP_TO_LEVEL.put(1239640, 27);
            XP_TO_LEVEL.put(1684640, 28);
            XP_TO_LEVEL.put(2284640, 29);
            XP_TO_LEVEL.put(3084640, 30);
            XP_TO_LEVEL.put(4149640, 31);
            XP_TO_LEVEL.put(5559640, 32);
            XP_TO_LEVEL.put(7459640, 33);
            XP_TO_LEVEL.put(9959640, 34);
            XP_TO_LEVEL.put(13259640, 35);
            XP_TO_LEVEL.put(17559640, 36);
            XP_TO_LEVEL.put(23159640, 37);
            XP_TO_LEVEL.put(30359640, 38);
            XP_TO_LEVEL.put(39559640, 39);
            XP_TO_LEVEL.put(51559640, 40);
            XP_TO_LEVEL.put(66559640, 41);
            XP_TO_LEVEL.put(85559640, 42);
            XP_TO_LEVEL.put(109559640, 43);
            XP_TO_LEVEL.put(139559640, 44);
            XP_TO_LEVEL.put(177559640, 45);
            XP_TO_LEVEL.put(225559640, 46);
            XP_TO_LEVEL.put(285559640, 47);
            XP_TO_LEVEL.put(360559640, 48);
            XP_TO_LEVEL.put(453559640, 49);
            XP_TO_LEVEL.put(569809640, 50);
        }

        public int getLevel(double exp) {
            return XP_TO_LEVEL.floorEntry((int) exp).getValue();
        }
    }
}
