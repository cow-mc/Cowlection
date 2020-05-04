package eu.olli.cowmoonication.data;

import eu.olli.cowmoonication.util.Utils;
import org.apache.commons.lang3.StringUtils;

public class HyStalkingData {
    private boolean success;
    private String cause;
    private HySession session;

    /**
     * No-args constructor for GSON
     */
    private HyStalkingData() {
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCause() {
        return cause;
    }

    public HySession getSession() {
        return session;
    }

    public static class HySession {
        private boolean online;
        private String gameType;
        private String mode;
        private String map;

        /**
         * No-args constructor for GSON
         */
        private HySession() {
        }

        public boolean isOnline() {
            return online;
        }

        public String getGameType() {
            String cleanGameType;
            try {
                cleanGameType = GameType.valueOf(gameType).getCleanName();
            } catch (IllegalArgumentException e) {
                // no matching game type found
                cleanGameType = Utils.fancyCase(gameType);
            }
            return cleanGameType;
        }

        public String getMode() {
            // modes partially taken from https://api.hypixel.net/gameCounts?key=MOO
            if (mode == null) {
                return null;
            }
            String gameType = getGameType();
            if (GameType.BEDWARS.cleanName.equals(gameType)) {
                // BedWars related
                String playerMode;
                String specialMode;
                int specialModeStart = StringUtils.ordinalIndexOf(mode, "_", 2);
                if (specialModeStart > -1) {
                    playerMode = mode.substring(0, specialModeStart);
                    specialMode = mode.substring(specialModeStart + 1) + " ";
                } else {
                    playerMode = mode;
                    specialMode = "";
                }
                String playerModeClean;
                switch (playerMode) {
                    case "EIGHT_ONE":
                        playerModeClean = "Solo";
                        break;
                    case "EIGHT_TWO":
                        playerModeClean = "Doubles";
                        break;
                    case "FOUR_THREE":
                        playerModeClean = "3v3v3v3";
                        break;
                    case "FOUR_FOUR":
                        playerModeClean = "4v4v4v4";
                        break;
                    case "TWO_FOUR":
                        playerModeClean = "4v4";
                        break;
                    default:
                        playerModeClean = playerMode;
                }
                return Utils.fancyCase(specialMode + playerModeClean);
            } else if (GameType.SKYBLOCK.cleanName.equals(gameType)) {
                // SkyBlock related
                switch (mode) {
                    case "dynamic":
                        return "Private Island";
                    case "hub":
                        return "Hub";
                    case "combat_1":
                        return "Spider's Den";
                    case "combat_2":
                        return "Blazing Fortress";
                    case "combat_3":
                        return "The End";
                    case "farming_1":
                        return "The Barn";
                    case "farming_2":
                        return "Mushroom Desert";
                    case "foraging_1":
                        return "The Park";
                    case "mining_1":
                        return "Gold Mine";
                    case "mining_2":
                        return "Deep Caverns";
                }
            }
            return Utils.fancyCase(mode);
        }

        public String getMap() {
            return map;
        }

        // TODO replace with api request: https://github.com/HypixelDev/PublicAPI/blob/master/Documentation/misc/GameType.md
        public enum GameType {
            QUAKECRAFT("Quakecraft"),
            WALLS("Walls"),
            PAINTBALL("Paintball"),
            SURVIVAL_GAMES("Blitz Survival Games"),
            TNTGAMES("The TNT Games"),
            VAMPIREZ("VampireZ"),
            WALLS3("Mega Walls"),
            ARCADE("Arcade"),
            ARENA("Arena Brawl"),
            UHC("UHC Champions"),
            MCGO("Cops and Crims"),
            BATTLEGROUND("Warlords"),
            SUPER_SMASH("Smash Heroes"),
            GINGERBREAD("Turbo Kart Racers"),
            HOUSING("Housing"),
            SKYWARS("SkyWars"),
            TRUE_COMBAT("Crazy Walls"),
            SPEED_UHC("Speed UHC"),
            SKYCLASH("SkyClash"),
            LEGACY("Classic Games"),
            PROTOTYPE("Prototype"),
            BEDWARS("Bed Wars"),
            MURDER_MYSTERY("Murder Mystery"),
            BUILD_BATTLE("Build Battle"),
            DUELS("Duels"),
            SKYBLOCK("SkyBlock"),
            PIT("Pit");

            private final String cleanName;

            GameType(String cleanName) {
                this.cleanName = cleanName;
            }

            public String getCleanName() {
                return cleanName;
            }
        }
    }
}
