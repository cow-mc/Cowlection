package de.cowtipper.cowlection.data;

import de.cowtipper.cowlection.util.Utils;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
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
            return DataHelper.GameType.getFancyName(gameType);
        }

        public String getMode() {
            // modes partially taken from https://api.hypixel.net/gameCounts?key=MOO
            if (mode == null) {
                return null;
            }
            String gameType = getGameType();
            if (DataHelper.GameType.BEDWARS.getCleanName().equals(gameType)) {
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
            } else if (DataHelper.GameType.SKYBLOCK.getCleanName().equals(gameType)) {
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
                    default:
                        // fall-through
                        break;
                }
            }
            return Utils.fancyCase(mode);
        }

        public String getMap() {
            return map;
        }
    }
}
