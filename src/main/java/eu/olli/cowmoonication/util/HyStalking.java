package eu.olli.cowmoonication.util;

import org.apache.commons.lang3.text.WordUtils;

public class HyStalking {
    private boolean success;
    private String cause;
    private HySession session;

    public HyStalking() {
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

        public HySession() {
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
                cleanGameType = WordUtils.capitalizeFully(gameType.replace('_', ' '));
            }
            return cleanGameType;
        }

        public String getMode() {
            // list partially taken from https://api.hypixel.net/gameCounts?key=MOO
            switch (mode) {
                // SkyBlock related
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
                    return WordUtils.capitalizeFully(mode.replace('_', ' '));
            }
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
