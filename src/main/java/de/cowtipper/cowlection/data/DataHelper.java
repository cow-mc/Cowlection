package de.cowtipper.cowlection.data;

import com.google.gson.annotations.SerializedName;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class DataHelper {
    private DataHelper() {
    }

    public enum SkyBlockRarity {
        COMMON(EnumChatFormatting.WHITE),
        UNCOMMON(EnumChatFormatting.GREEN),
        RARE(EnumChatFormatting.BLUE),
        EPIC(EnumChatFormatting.DARK_PURPLE),
        LEGENDARY(EnumChatFormatting.GOLD),
        MYTHIC(EnumChatFormatting.LIGHT_PURPLE),
        SPECIAL(EnumChatFormatting.RED),
        VERY_SPECIAL(EnumChatFormatting.RED);

        public final EnumChatFormatting rarityColor;

        SkyBlockRarity(EnumChatFormatting color) {
            this.rarityColor = color;
        }

        public static SkyBlockRarity[] getPetRarities() {
            return Arrays.stream(values(), 0, 5).toArray(SkyBlockRarity[]::new);
        }

        public static SkyBlockRarity getPetRarityByColorCode(String colorCode) {
            if (MYTHIC.rarityColor.toString().equals(colorCode)) {
                // special case: Mystic Bat
                return LEGENDARY;
            }
            for (SkyBlockRarity petRarity : getPetRarities()) {
                if (petRarity.rarityColor.toString().equals(colorCode)) {
                    return petRarity;
                }
            }
            return null;
        }

        public EnumChatFormatting getColor() {
            return rarityColor;
        }
    }

    // TODO replace with api request: https://github.com/HypixelDev/PublicAPI/blob/master/Documentation/misc/GameType.md
    @SuppressWarnings("unused")
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

        public static String getFancyName(String gameName) {
            if (gameName == null) {
                return null;
            }
            String cleanGameType;
            try {
                cleanGameType = valueOf(gameName).getCleanName();
            } catch (IllegalArgumentException e) {
                // no matching game type found
                cleanGameType = Utils.fancyCase(gameName);
            }
            return cleanGameType;
        }

        public String getCleanName() {
            return cleanName;
        }
    }

    public enum PartyType {
        SUITABLE(0xff22B14C, 240),
        UNIDEAL(0xffCD8032, 240),
        UNJOINABLE(0xffEB6E6E, 279),
        CURRENT(0xff5FDE6C, 240),
        NONE(0xffFF0000, 279);

        private final float zIndex;
        private final int color;

        PartyType(int color, float zIndex) {
            this.color = color;
            this.zIndex = zIndex;
        }

        public float getZIndex() {
            return zIndex;
        }

        public int getColor() {
            return color;
        }
    }

    public enum DungeonClass {
        @SerializedName("archer") ARCHER('A'),
        @SerializedName("berserk") BERSERK('B'),
        @SerializedName("healer") HEALER('H'),
        @SerializedName("mage") MAGE('M'),
        @SerializedName("tank") TANK('T'),
        @SerializedName("") UNKNOWN('U');
        private final char shortName;

        DungeonClass(char shortName) {
            this.shortName = shortName;
        }

        public static DungeonClass get(String className) {
            try {
                return valueOf(className.toUpperCase());
            } catch (IllegalArgumentException e) {
                // invalid class name
                return UNKNOWN;
            }
        }

        public char getShortName() {
            return shortName;
        }

        public String getName() {
            return Utils.fancyCase(name());
        }
    }

    public static Map<String, String> getMinions() {
        // key = skin id, value = minion type and tier
        Map<String, String> minions = new HashMap<>();
        // TODO currently Fishing VI + VII use the same skull texture (server-side) - thus can't be distinguished
        minions.put("2f93289a82bd2a06cbbe61b733cfdc1f1bd93c4340f7a90abd9bdda774109071", "Cobblestone I");
        minions.put("3fd87486dc94cb8cd04a3d7d06f191f027f38dad7b4ed34c6681fb4d08834c06", "Cobblestone II");
        minions.put("cc088ed6bb8763af4eb7d006e00fda7dc11d7681e97c983b7011c3e872f6aab9", "Cobblestone III");
        minions.put("39514fee95d702625b974f1730fd62e567c5934997f73bae7e07ab52ddf9066e", "Cobblestone IV");
        minions.put("3e2467b8ccaf007d03a9bb7c22d6a61397ca1bb284f128d5ccd138ad09124e68", "Cobblestone V");
        minions.put("f4e01f552549037ae8887570700e74db20c6f026a650aeec5d9c8ec51ba3f515", "Cobblestone VI");
        minions.put("51616e63be0ff341f70862e0049812fa0c27b39a2e77058dd8bfc386375e1d16", "Cobblestone VII");
        minions.put("ea53e3c9f446a77e8c59df305a410a8accb751c002a41e55a1018ce1b3114690", "Cobblestone VIII");
        minions.put("ccf546584428b5385bc0c1a0031aa87e98e85875e4d6104e1be06cef8bd74fe4", "Cobblestone IX");
        minions.put("989db0a9c97f0e0b5bb9ec7b3e32f8d63c648d4608cfd5be9adbe8825d4e6a94", "Cobblestone X");
        minions.put("ebcc099f3a00ece0e5c4b31d31c828e52b06348d0a4eac11f3fcbef3c05cb407", "Cobblestone XI");

        minions.put("320c29ab966637cb9aecc34ee76d5a0130461e0c4fdb08cdaf80939fa1209102", "Obsidian I");
        minions.put("58348315724fb1409142dda1cab2e45be34ead373d4a1ecdae6cb4143cd2bd25", "Obsidian II");
        minions.put("c5c30c4800b25625ab51d4569437ad7f3e5f6465b51575512388b4c96ecbac90", "Obsidian III");
        minions.put("1f417418f6df6efc7515ef31f4db570353d36ee87d46c6f87a7f9678b1f3ac57", "Obsidian IV");
        minions.put("44d4ae42f0d6e82c7ebf9877303f9a84c96ce1978a8ac33681143f4b55a447ce", "Obsidian V");
        minions.put("7c124351bd2da2312d261574fb578594c18720ac9c9d9edfdb57754b7340bd27", "Obsidian VI");
        minions.put("db80b743fa6a8537c495ba7786ebefb3325e6013dc87d8c144ab902bbdb20f86", "Obsidian VII");
        minions.put("745c8fc5ccb0bdbc19278c7e91ad6ac33d44f11fae46e1bfbfd1737ec1e420d4", "Obsidian VIII");
        minions.put("15a45b66c8e21b515ea25abf47c9c27d995fe79b128844a0c8bf7777f3badee5", "Obsidian IX");
        minions.put("1731be266b727b49ad135b4ea7b94843f7b322f873888da9fe037edea2984324", "Obsidian X");
        minions.put("4d36910bcbb3fc0b7dedaae85ff052967ad74f3f4c2fb6f7dd2bed5bcfd0992b", "Obsidian XI");

        minions.put("20f4d7c26b0310990a7d3a3b45948b95dd4ab407a16a4b6d3b7cb4fba031aeed", "Glowstone I");
        minions.put("c0418cf84d91171f2cd67cbaf827c5b99ce4c1eeba76e77eab241e61e865a89f", "Glowstone II");
        minions.put("7b21d7757c8ae382432b606b26ce7854f6c1555e668444ed0eecc2faab55a37d", "Glowstone III");
        minions.put("3cc302e56b0474d5e428978704cd4a85de2f6c3e885a70f781e2838b551d5bfc", "Glowstone IV");
        minions.put("ba8879a5be2d2cc75fcf468054046bc1eb9c61204a66f93991c9ff840a7c57cb", "Glowstone V");
        minions.put("cd965a713f2e553c4c3ec047237b600b5ba0de9321a9c7dfe3d47b71d6afda41", "Glowstone VI");
        minions.put("7f07e68f9985db6c905fe8f4f079137a6deef493413206d4ec90756245b4765e", "Glowstone VII");
        minions.put("a8507f495bf89912dd2a317ae86faf8ce3631d62ca3d062e9fe5bf8d9d00fd70", "Glowstone VIII");
        minions.put("b30d071e8c97a9c065b307d8a845ef8be6f6db85b71a2299f1bea0be062873e7", "Glowstone IX");
        minions.put("8eeb870670e9408a78b386db6c2106e93f7c8cf03344b2cb3128ae0a4ea19674", "Glowstone X");
        minions.put("8bc66c5eb7a197d959fcc5d45a7aff938e07ddcd42e3f3993bde00f56fe58dd1", "Glowstone XI");

        minions.put("7458507ed31cf9a38986ac8795173c609637f03da653f30483a721d3fbe602d", "Gravel I");
        minions.put("fb48c89157ae36038bbd9c88054ef8797f5b6f38631c1b57e58dcb8d701fa61d", "Gravel II");
        minions.put("aae230c0ded51aa97c7964db885786f0c77f6244539b185ef4a5f2554199c785", "Gravel III");
        minions.put("ef5b6973f41305d2b41aa82b94ef3b95e05e943e4cd4f793ca59278c46cbb985", "Gravel IV");
        minions.put("c5961d126cda263759e43940c5665e9f1487ac2c7e26f903e5086affb3785714", "Gravel V");
        minions.put("69c5f0583967589650b0de2c5108811ff01c32ac9861a820bba650f0412126d6", "Gravel VI");
        minions.put("d092f7535b5d091cc3d3f0a343be5d46f16466ae9344b0cac452f3435f00996a", "Gravel VII");
        minions.put("7117a2f4cf83c41a8dfb9c7a8238ca06bbdb5540a1e91e8721df5476b70f6e74", "Gravel VIII");
        minions.put("14463534f9fbf4590d9e2dcc1067231ccb8d7f641ee56f4652a17f5027f62c63", "Gravel IX");
        minions.put("5c6e62f2366d42596c752925c7799c63edbfc226fffd9327ce7780b24c3abd11", "Gravel X");
        minions.put("3945c30d258d68576f061c162b7d50ca8a1f07e41d557e42723dbd4fcce5d594", "Gravel XI");

        minions.put("81f8e2ad021eefd1217e650e848b57622144d2bf8a39fbd50dab937a7eac10de", "Sand I");
        minions.put("2ab4e2bdb878de70505120203b4481f63611c7feac98db194f864be10b07b87e", "Sand II");
        minions.put("a53d8f2c1449bc5b89e485182633b26970538e74410ac9e6e4f5eb1195c36887", "Sand III");
        minions.put("847709a9f5bae2c5e727aee4be706a359c51acb842aafa1a4d23fb62f73e9aa6", "Sand IV");
        minions.put("52b94ddeedecce5f90f9d227015dd6071c314cf0234433329e53f5b26b8cf890", "Sand V");
        minions.put("7a756b6a9735b74031b284be6064898f649e5bb4d1300aafc3c0b280dad04b69", "Sand VI");
        minions.put("13a1a8b92d83d2200d172d4bbda8d69e37afeb676d214b83af00f246c267dcd2", "Sand VII");
        minions.put("765db90f1e3dab4df3a5a42cd80f7e71a92ea4739395df56f1750c73c27cdc4f", "Sand VIII");
        minions.put("281ccdfe00a7843bce0c109676c1b59dd156389f730f00d3987c10aef64a7f96", "Sand IX");
        minions.put("fdceae5bc34dee02b31a68b0015d0ca808844e491cf926c6763d52b26191993f", "Sand X");
        minions.put("c0e9118bcebf481394132a5111fcbcd9981b9a99504923b04794912660e22cea", "Sand XI");

        minions.put("af9b312c8f53da289060e6452855072e07971458abbf338ddec351e16c171ff8", "Clay I");
        minions.put("7411bd08421fccfea5077320a5cd2e4eecd285c86fc9d2687abb795ef119097f", "Clay II");
        minions.put("fd4ffcb5df4ef82fc07bc7585474c97fc0f2bf59022ffd6c2606b4675f8aaa42", "Clay III");
        minions.put("fb2cfdad77fb027ede854bcd14ee5c0b4133aa25bf4c444789782c89acd00593", "Clay IV");
        minions.put("393452da603462cce47dda35da160316291d8d8e6db8f377f5df71971242f3d1", "Clay V");
        minions.put("23974725dd17729fc5f751a6749e02c8fa3d9299d890c9164225b1fbb7280329", "Clay VI");
        minions.put("94a6fbf682862d7f0b68c192521e455122bb8f3a9b7ba876294027b7a35cd1a7", "Clay VII");
        minions.put("f0ec6c510e8c72627efd3011bb3dcf5ad33d6b6162fa7fcbd46d661db02b2e68", "Clay VIII");
        minions.put("c7de1140a2d1ce558dffb2f69666dc9145aa8166f1528a259013d2aa49c949a8", "Clay IX");
        minions.put("b1655ad07817ef1e71c9b82024648340f0e46a3254857e1c7fee2a1eb2eaab41", "Clay X");
        minions.put("8428bb198b27ac6656698cb3081c2ba94c8cee2f33d16e8e9e11e82a4c1763c6", "Clay XI");

        minions.put("e500064321b12972f8e5750793ec1c823da4627535e9d12feaee78394b86dabe", "Ice I");
        minions.put("de333a96dc994277adedb2c79d37605e45442bc97ff8c9138b62e90231008d08", "Ice II");
        minions.put("c2846bd72a4b9ac548f6b69f21004f4d9a0f2a1aee66044fb9388ca06ecb0b0d", "Ice III");
        minions.put("79579614fdaa24d6b2136a164c23e7ef082d3dee751c2e37e096d48bef028272", "Ice IV");
        minions.put("60bcda03d6b3b91170818dd5d91fc718e6084ca06a2fa1e841bd1db2cb0859f4", "Ice V");
        minions.put("38bdef08b0cd6378e9a7b9c4438f7324c65d2c2afdfb699ef14305b668b44700", "Ice VI");
        minions.put("93a0b0c2794dda82986934e95fb5a08e30a174ef6120b70c58f573683088e27e", "Ice VII");
        minions.put("d381912c9337a459a28f66e2a3edcdacbddc296dd69b3c820942ba1f4969d936", "Ice VIII");
        minions.put("21cb422b9e633e0700692ae573c5f63a838ebc771a209a5e0cc3cba4c56f746f", "Ice IX");
        minions.put("6406ca9dcd26cc148e05917ae1524066824a4f59f5865c47214ba8771e9b924b", "Ice X");
        minions.put("5ef40b76cca1e4bcd2cbda5bc61bc982a519a2df5170662ea889bf0d95aa2c1b", "Ice XI");

        minions.put("f6d180684c3521c9fc89478ba4405ae9ce497da8124fa0da5a0126431c4b78c3", "Snow I");
        minions.put("69921bab54af140481c016a59a819b369667a4e4fb2f2449ceebf7c897ed588e", "Snow II");
        minions.put("4e13862d1d0c52d272ece109e923af62aedebb13b56c47085f41752a5d4d59e2", "Snow III");
        minions.put("44485d90a129ff672d9287af7bf47f8ece94abeb496bda38366330893aa69464", "Snow IV");
        minions.put("9da9d3bfa431206ab33e62f8815e4092dae6e8fc9f04b9a005a205061ea895a8", "Snow V");
        minions.put("7c53e9ef4aba3a41fe8e03c43e6a310eec022d1089fd9a92f3af8ed8eed4ec03", "Snow VI");
        minions.put("e1fd2b30f2ef93785404cf4ca42e6f28755e2935cd3cae910121bfa4327345c1", "Snow VII");
        minions.put("9f53221b1b2e40a97a7a10fb47710e61bdd84e15052dd817da2f89783248375e", "Snow VIII");
        minions.put("caa370beebe77ced5ba4d106591d523640f57e7c46a4cecec60a4fe0ebac4a4c", "Snow IX");
        minions.put("f2c498b33325cce5668a3395a262046412cfd4844b8d86ddaeb9c84e940e2af", "Snow X");
        minions.put("bce70b1b8e30e90a5ad951f42ff469c19dd416cedf98d5aa4178ec953c584796", "Snow XI");

        minions.put("425b8d2ea965c780652d29c26b1572686fd74f6fe6403b5a3800959feb2ad935", "Coal I");
        minions.put("f262e1dad0b220f41581dbe272963fff60be55a85c0d587e460a447b33f797c6", "Coal II");
        minions.put("b84f042872bfc4cc1381caab90a7bbe2c053cca1dae4238a861ac3f4139d7464", "Coal III");
        minions.put("8c87968d19102ed75d95a04389f3759667cc48a2ecacee8b404f7c1681626748", "Coal IV");
        minions.put("c5ebd621512c22d013aab7f443862a2d81856ce037afe80fcd6841d0d539136b", "Coal V");
        minions.put("f4757020d157443e591b28c4661064d9a6a44dafe177c9bc133300b176fc40e", "Coal VI");
        minions.put("2d2f9afcfada866a2918335509b5401d5c56d6902658090ec4ced91fea6bf53a", "Coal VII");
        minions.put("1292ecec3b09fbbdffc07fbe7e17fa10b1ff82a6956744e3fa35c7eb75124a98", "Coal VIII");
        minions.put("29f5b3c25dd013c4b1630746b7f6ee88c73c0bacf22a970a2331818c225a0620", "Coal IX");
        minions.put("46bb54a946802ebce2f00760639b2bf484faed76cbb284eb2efaa5796d771e6", "Coal X");
        minions.put("641ffadeaa22c8d97a72036cbd5d934ca454032a81957052e85f3f95b79d3169", "Coal XI");

        minions.put("af435022cb3809a68db0fccfa8993fc1954dc697a7181494905b03fdda035e4a", "Iron I");
        minions.put("5573adc1a442b2bad0bafd4415603e821d56a20201c1e7abd259cc0790baa7bf", "Iron II");
        minions.put("b21f639707fbe87e55ca18424384b9ef5deb896fe16a6f23a7197b096f038ad9", "Iron III");
        minions.put("a7e83a03615a41d7a7c967fd40c0583ad273d37d0a19bde3e4a562a0f680c920", "Iron IV");
        minions.put("c6177919c0849aa737c07b13fd5019be5572d6c9e4be59c33645a99057f25014", "Iron V");
        minions.put("546591b819ea8a33cf993bb433fa5bbbbb200af8e6eb1103a7d1ee11da1db5f1", "Iron VI");
        minions.put("f58e6617cefe52eeacb9d3f7f424d52e4ae1b8e8f775c0bfd6045c99387851ab", "Iron VII");
        minions.put("7e5ea57f0fb1f58a68746b5a6e93b8491528d09ba07b586b73a43ae05638ad0c", "Iron VIII");
        minions.put("3679a748d40754d4e5bc418d152064c7aeee4121a2b1d8e058d0b0326f8bff3", "Iron IX");
        minions.put("ac48b4758211fd0e4713ad0b6ea9d13a29eb9f523d3911dcff945197b8bc1a56", "Iron X");
        minions.put("1b22b582b750dbdb0b7599c786d39f2efedfe64b5ae32656020295b57f2fcf7", "Iron XI");

        minions.put("f6da04ed8c810be29bba53c62e712d65cfb25238117b94d7e85a4615775bf14f", "Gold I");
        minions.put("761bcdb7251638f757cd70fd0fd21a2a05f41cbdc78ad3e50f64ecf38aa3220c", "Gold II");
        minions.put("8bd48b17a82d5e395034d01db61c76f13f88da6e5a0b1c1d198fbdb0805a7739", "Gold III");
        minions.put("2b46c4f5f574748cf82777ceba40a4fa96572f9e3cb871eb9258330df960f72e", "Gold IV");
        minions.put("f6e438a6d1ef319c42d73a00db2b2981fdbb0e437a1c02399b8daf8d9e37a05b", "Gold V");
        minions.put("1317a0b66ed2dc274b3b0d77b6859d4b9942ccc942d9e0bc26f7fa15b89842b", "Gold VI");
        minions.put("9d4411f11d6c0c0eb43a139e16e5b7db3fdca94f3c1a2443ff6e9b06d576a6d4", "Gold VII");
        minions.put("6439b889f62ff1f2cc5e00db5892655c591194b9294725a366efa3e4b2a022fa", "Gold VIII");
        minions.put("5bf721e5d5403e11d176dd33d54458332195667e89d88bbf402c6d0307ec442c", "Gold IX");
        minions.put("3d236e1e3ef0dea06e3be72f0980767c5aea41b882f8b9b58838298ace64dc9d", "Gold X");
        minions.put("936a5712543dde0a3c7dc54063178cb69179dfd5cd75cb1de4fe8771b53dd03f", "Gold XI");

        minions.put("2354bbe604dfe58bf92e7729730d0c8e37844e831ee3816d7e8427c27a1824a2", "Diamond I");
        minions.put("5138692abccf0cebd0d99b4a59a26d41f779acfd35a07b2b527593356bc8ece6", "Diamond II");
        minions.put("4e7ed1638aeaab8a87d5a1193080d46262e2d6d3aa22ad3222c6af0170b3ee17", "Diamond III");
        minions.put("51fb872f35ad1536d2d5709797f8429043dc2f4cd5a4d8e91b9422dc5c51db95", "Diamond IV");
        minions.put("32812ae070fca358216ad627859e2d69c67c8ac7b631ff4d5c57ebc394357095", "Diamond V");
        minions.put("ec01c11ca159a5cd18ab45c03b1c56ae3a228711b8369daf828f85c5c1a4bb2c", "Diamond VI");
        minions.put("a6a28d4cba6907c107421e90b90c274b5ac6ce937def9e28521b5ef81f461493", "Diamond VII");
        minions.put("6406f0ba05e99cabb166f8982ac6eff8de42b5f884b26c2ffd4ad70ec7d21151", "Diamond VIII");
        minions.put("d6cc4e3241c518aeb8e64e32a0b6f536d7684c8b5fe6bc9699db9ee6332cf70f", "Diamond IX");
        minions.put("f024c9b17c2b4a84fe503726df57b7cd7a77827f530c6b93d2e94e7cc0b515f9", "Diamond X");
        minions.put("537643ca84a56411f676a11eceab6d96fc7877ae3402c6083b69ee97ab95e700", "Diamond XI");

        minions.put("64fd97b9346c1208c1db3957530cdfc5789e3e65943786b0071cf2b2904a6b5c", "Lapis I");
        minions.put("65be0e9684b28a2531bec6186f75171c1111c3133b8ea944f32c34f247ea6923", "Lapis II");
        minions.put("2a3915a78c2397f2cef96002391f54c544889c5ced4089eb723d14b0a6f02b08", "Lapis III");
        minions.put("97df8ae6e1436504a6f08137313e5e47e17aa078827f3a636336668a4188e6fc", "Lapis IV");
        minions.put("aa5d796b9687cc358ea59b06fdd9a0a519d2c7a2928de10d37848b91fbbc648f", "Lapis V");
        minions.put("6e5db0956181c801b21e53cd7eb7335941801a0f335b535a7c9afd26022e9e70", "Lapis VI");
        minions.put("1a49908cf8c407860512997f8256f0b831bd8fc4f41d0bf21cd23dbc0bdebb0f", "Lapis VII");
        minions.put("c08a219f5cf568c9e03711518fcf18631a1866b407c1315017e3bf57f44ef563", "Lapis VIII");
        minions.put("e5a93254f20364b7117f606fd6745769994acd3b5c057d3382e5dd828f9ebfd4", "Lapis IX");
        minions.put("6fe5c4ceb6e66e7e0c357014be3d58f052a38c040be62f26af5fb9bed437541", "Lapis X");
        minions.put("736cd50c9e8cf786646960734b5e23e4d2e3112f4494d5ddb3c1e45033324a0e", "Lapis XI");

        minions.put("1edefcf1a89d687a0a4ecf1589977af1e520fc673c48a0434be426612e8faa67", "Redstone I");
        minions.put("4ebdbf0aca7d245f6d54c91c37ec7102a55dd0f3b0cfe3c2485f3a99b3e53aa0", "Redstone II");
        minions.put("c1a5175a1caf7a88a82b88b4737159132a68dc9fc99936696b1573ea5a7bb76d", "Redstone III");
        minions.put("bbf83cb38bd6861b33665c1c6f56e29cbc4a87a2f494581999d51d309d58d0aa", "Redstone IV");
        minions.put("d96fa75edd9bc6e1d89789e58a489c4594d406dd93d7c566ed4534971b52c118", "Redstone V");
        minions.put("9cfd7010be9a08edd1e91c4203fccff6ddf71e680e4dfb4d32c38dee99d4a389", "Redstone VI");
        minions.put("18db0ef0af4853603a3f663de24381159e9faaa1cdf93b026719dab050ea9954", "Redstone VII");
        minions.put("a40b85c00f824f61beefd651c9588698e49d01902e84a098f79ee09941d8e4ac", "Redstone VIII");
        minions.put("85d61b9d0b8ad786e8e1ff1dbbde1221a8691fda1daf93c8605cbc2e4fdea63", "Redstone IX");
        minions.put("6588bed4136c95dd961b54a06307b2489726bbfe4fda41cee8ab2c57fa36f291", "Redstone X");
        minions.put("6670498256b1cbae7c8463bc2d65036cf07447b146f7d3f69bfa2dc07e9fd8cf", "Redstone XI");

        minions.put("9bf57f3401b130c6b53808f2b1e119cc7b984622dac7077bbd53454e1f65bbf0", "Emerald I");
        minions.put("5e2d440d6c2300d94e7d5c44906d73a5cde521dfe516698513dd8c02ffdd5a82", "Emerald II");
        minions.put("6b94475f5c54147c27cbba0434ca0f5e4a501c7bae44c73d36ea36016fa47fec", "Emerald III");
        minions.put("65cbd71747c835a09af211e821d65c4facef7fb6824973bc3ca8c4aba4a98e30", "Emerald IV");
        minions.put("dc7c36d02b65e871cc696db42b3ae0cc98670205c8bc90cf96e5f53424cd681e", "Emerald V");
        minions.put("67136258da9fa4143c058d4d8a6758dcb6f615e6d98d019fdafe526e5f900b1f", "Emerald VI");
        minions.put("a6762402cbd0127351fd1aa37423f463b309379eff4a2ad28b5766d51407f288", "Emerald VII");
        minions.put("34e546bb3f8930b0a1a204daaec9ed76f85eab3d57a61e62c3133d11f65c15b", "Emerald VIII");
        minions.put("db81f7229ad141b925ad3ff0f121c14686478a35f4777c73d3416505343d3811", "Emerald IX");
        minions.put("c2ba3b81576024cbde5dade239ad7603e1fe5f9d8b1fe4716cfc68a9bcb2d324", "Emerald X");
        minions.put("67a5b0b9839081488ffde6d0cd74b540cf23a505c95e521f77e337775c49b431", "Emerald XI");

        minions.put("d270093be62dfd3019f908043db570b5dfd366fd5345fccf9da340e75c701a60", "Quartz I");
        minions.put("c305506b47609d71488a793c12479ad8b990f7f39fd7de53a45f4c50874d1051", "Quartz II");
        minions.put("83f023160a3289b9c21431194940c8e5f45c7e43687cf1834755151d7c2250f7", "Quartz III");
        minions.put("c2bc6c98d4cbab68af7d8434116a92a351011165f73a3f6356fb88df8af40a49", "Quartz IV");
        minions.put("5c0e10de9331da29e0a15e73475a351b8337cd4725b8b24880fb728eb9d679dd", "Quartz V");
        minions.put("300120cabf0ae77a143adca34b9d7187ca1ef6d724269b256d5e3663c7f19bd9", "Quartz VI");
        minions.put("bde647431a27149bf3f462a22515863af6c36532c1f66668688131ca11453fd1", "Quartz VII");
        minions.put("9899278d0464397dd076408812eef40758f75b1cdb82c04c08c81503453e07e6", "Quartz VIII");
        minions.put("2974bc0b9771a4af994ea571638adf1e98cd896acf95cc27b890915669bcedfd", "Quartz IX");
        minions.put("3ae41345d675f4ed4dc5145662303123cb828b6e1a3e72d8278174488562dfa9", "Quartz X");
        minions.put("7aeec9ef192e733bfcb723afd489cbf4735e7cfdd2ec45cae924009a8f093708", "Quartz XI");

        minions.put("7994be3dcfbb4ed0a5a7495b7335af1a3ced0b5888b5007286a790767c3b57e6", "End Stone I");
        minions.put("eb0f8a2752e733e8d9152b1cf0a385961fa1ba77daed8d2e4e02348691610529", "End Stone II");
        minions.put("63f211a5f8aca7607a02df177145bea0c6edc032bc82807fb1daeaa5d95b447d", "End Stone III");
        minions.put("fd40b308f1ca5b1188618c45a3de05b068d9cafba7039aa06d2bc5b9c6751cea", "End Stone IV");
        minions.put("627218013240d63354b7fe931f1cbea1321535e28e292937f0c8f6f776088723", "End Stone V");
        minions.put("70ca8dbb37647bb0b51b41799d619615af04976b8111dd22d3b23316562c6c30", "End Stone VI");
        minions.put("1260560ce465dcd39fcb0c3ce365a88ff5c8e123cdc4d0e95e682e70b9283392", "End Stone VII");
        minions.put("2b1755ccf1e0bba50ab41007aaec977286b7d7fcf3953a9c018aaf62967f3474", "End Stone VIII");
        minions.put("13f379728f8cbb88300e8de137d9b576205cde2c5e36607ad8d3bb558f533d68", "End Stone IX");
        minions.put("1fbd102a90e2a19a9d2f5fcc98da00b4addbfb02593b34367fe4c2339c37eff0", "End Stone X");
        minions.put("35fc7b11e32ead79f7d110d3efb6447a66b387fce79f70fa4ceaaa0e0fe717f5", "End Stone XI");

        minions.put("bbc571c5527336352e2fee2b40a9edfa2e809f64230779aa01253c6aa535881b", "Wheat I");
        minions.put("c62b0508b3fef406833d519da8b08ee078604c78b8ca6e9c138760041fa861bf", "Wheat II");
        minions.put("e61773628ed7555e0a63add21166ad34227d10d21e34c3c7e5a0fa8532dd3f6", "Wheat III");
        minions.put("a8751403935a5c637ff225cddb739c6a960a48256bc88e6ead0a728d70981267", "Wheat IV");
        minions.put("c32fa98a6c398ca75c9480711158a60a0f37e9f93bdc8fe4156191bd88a888b3", "Wheat V");
        minions.put("662d04c94d385f6ecddaa4bc51371baf54061ab6f21c8a030df639d87ac6be2d", "Wheat VI");
        minions.put("5c6f9f3a7b55ee7093ee8f5bf5888b16def6bfde157556df219a1c0b94b0458f", "Wheat VII");
        minions.put("efe4b6553a3d0f764a20624f4ab256792167e5fc3b75b31b59732325a316f162", "Wheat VIII");
        minions.put("2e3cb30a26293b7fc67519249bb07efd9c9a72229811e902e1627027938edcce", "Wheat IX");
        minions.put("d6ab7a3d5438c101c71b727643d08d3b3fedf321d3853ade72b1ae83feb5df70", "Wheat X");
        minions.put("2f6d62602c22e630153be8b764f45827585521bd82bb5307df1168f250f17f6b", "Wheat XI");

        minions.put("95d54539ac8d3fba9696c91f4dcc7f15c320ab86029d5c92f12359abd4df811e", "Melon I");
        minions.put("93dbb7b41ddd998842719915179a6b5a82d0c223e4c313c9eb081b52c84a764f", "Melon II");
        minions.put("9ed762a1b1bf0c811a6cc62742526840e8eb3c01fc86cc5afed89ec9beeb530e", "Melon III");
        minions.put("9e2654f305b788b9345bef6be076d8f36e7c946d6eae26d3c0803ddc4843b596", "Melon IV");
        minions.put("1234f7f7ad67acd50b30932781c21a0b1cc22530a4980f688549123e10d9c474", "Melon V");
        minions.put("8fea2e4ddf7314f21b87fc8d8634c60cab23320112002932d6c12c2e92d5549b", "Melon VI");
        minions.put("213637a2898fd04f0ced904c0293f136238057c33b16983e0262ff9ae0047dd2", "Melon VII");
        minions.put("3b7e6694866967e222664641461c0a1b08b9aa0c390944e6142e769d473987a5", "Melon VIII");
        minions.put("8ebac9c3ddbb76ea862b91a6992bae358c0eaed7b2ca73fb28b8115be019e32b", "Melon IX");
        minions.put("dca93d1d2dc3f7ee1ac66ab5280c619fbe37e1e338a6484808a5433b1a3ee911", "Melon X");
        minions.put("166a1693eb7764d0c78d683bb53787db6836518de0b5087869df692dc6be942", "Melon XI");

        minions.put("f3fb663e843a7da787e290f23c8af2f97f7b6f572fa59a0d4d02186db6eaabb7", "Pumpkin I");
        minions.put("95bcb44bbeaec7c903d4f37273ff6a20bd40f240dfbefc4aaf25cb4b0a25f3c4", "Pumpkin II");
        minions.put("6832fd793f38e20265cfef3d979289493b951e0b5fb53511984bf500b6ad64ca", "Pumpkin III");
        minions.put("6656b05400537c47b3e986697e5af027ed36adf7c80d9edaec6b48cb1af9f99b", "Pumpkin IV");
        minions.put("16685cf51ab0e08e842a822ca416225df3c583b32110bf4d778ad69f3f604b43", "Pumpkin V");
        minions.put("ee1807903ca846a732ea46e9490b752a2803f75017a3008808c48437cfd8827f", "Pumpkin VI");
        minions.put("cda682c874c482e9e659e37fe3e8399c5f3c4f6237f0656071af5ffaf418ea9a", "Pumpkin VII");
        minions.put("bb72233f28cb814aadb63f0688033e7317907cf43f015097e493a578a3f50222", "Pumpkin VIII");
        minions.put("2cc1a47302e055e561b06daede35f84a04829bc899af03d5603b78e55269c402", "Pumpkin IX");
        minions.put("7e246ead094174d265eb03222417dd4ed1d1a6a5ad33d77ed2578ab55eed3a37", "Pumpkin X");
        minions.put("4c6a48f079ef70d84df10332bb0f2bf038d8e0e82ac36734823fb4b4a50705e4", "Pumpkin XI");

        minions.put("4baea990b45d330998cb0c1f8515c27b24f93bff1df0db056e647f8200d03b9d", "Carrot I");
        minions.put("32a0a1695d50e0a9ced4b91edfd42afd41b4e737aa5d174c74b13963fb022556", "Carrot II");
        minions.put("149dbae380e85f93d4c86f5097ec2ac3dec28389fb528a0d6a719fc6139626a8", "Carrot III");
        minions.put("7399c0373eed7e12d5c212e13c51422e21d4ec7fa301f5a5c684f816a2eb2aab", "Carrot IV");
        minions.put("c56711e5002d0a7003f85cc2f59137da467648332baa630d939684580c5bbddb", "Carrot V");
        minions.put("52bc4e06ec80d34fb5c419d743e7ccf313866a98dbd15d53a83db98a7bff8ff5", "Carrot VI");
        minions.put("c8c0dbbc8cc4bdc5d8483c732a61404ad12ab0ab7c49ff81cba2b709ae547923", "Carrot VII");
        minions.put("f3e5e690f2f78fd39efb4b0bf212bf68eb02d76936891238c8db2b4940d49313", "Carrot VIII");
        minions.put("42c2ab452b92102b7ba030b81084d000e155beb616a026f95a11c654e96f4e28", "Carrot IX");
        minions.put("bdf031730f2f6bd8aaebfbc6e160723d294e03cc9545d98d9bdb84cfdf853266", "Carrot X");
        minions.put("62858c422e0963f1b1da6196e9d47936acea449bea9f90e2dbf32f921f2522e", "Carrot XI");

        minions.put("7dda35a044cb0374b516015d991a0f65bf7d0fb6566e350496642cf2059ff1d9", "Potato I");
        minions.put("6ce06fb5d857f1b821b4f6f4481464b2471650733bf7baa3e1f6b41555aab561", "Potato II");
        minions.put("29e3d309d56d37b51f4a356cba55fec4ac8e174bf2b72a03fb8361a2b41da17d", "Potato III");
        minions.put("72fd7129e7831c043447a8355e78109431e7ca19959ef79dcc7a4c8f0a4ccf77", "Potato IV");
        minions.put("2033a6e541525d523fe25da2c68a885ba1c2449362d0b35a68c95b69e8a28c87", "Potato V");
        minions.put("2aea4e3ef5782f4cb6e0d38e8d871221d29197cb186aca0e144922e7cd2e1224", "Potato VI");
        minions.put("6a1812e4f58f1ec46c608521fc5f51eb2e653bbc4e43cd9e89dff88e8c777e", "Potato VII");
        minions.put("9788f69ebdb3030054feff365e689bb5b10a867f52f9873bc952fc26b54d48ff", "Potato VIII");
        minions.put("b84792c8674b964f708b880df7d175631b9b6d9b5362353362ca997e727e1189", "Potato IX");
        minions.put("e05c2ab7f41ca1f3f221b949edc7b20b800ed3bbaeb36514eb003887338f960", "Potato X");
        minions.put("57441fa19d89d5df902c07586c084f1b00c4ca06ca4cc2ec5b6230d1a5199811", "Potato XI");

        minions.put("4a3b58341d196a9841ef1526b367209cbc9f96767c24f5f587cf413d42b74a93", "Mushroom I");
        minions.put("645c0b050d7223cce699f6cdc8649b865349ebc22001c067bf41151d6e5c1060", "Mushroom II");
        minions.put("a8e5b335b018b36c2d259711bee83da5b42fcc55ec234514ae2c23b1e98d7e77", "Mushroom III");
        minions.put("80970ebf76d0aa52a6abb7458a2e3917d967d553def9174a8b83697a10f4e339", "Mushroom IV");
        minions.put("268b2d44457a92988400687d43e1562e0cb2ed1667ef0e62ed033a2881723eb4", "Mushroom V");
        minions.put("ac8772dfd110ef66d5eb3957046834313adbf035f36352f2426be2802c1a21d8", "Mushroom VI");
        minions.put("9fd3d54f0eb2570ffd254bcbff3b3c076521ed896118d984fb66db154d4a5466", "Mushroom VII");
        minions.put("dd360de8da7f050cedad36e91f595577622a2ae9db32f622b745c47f35dc012e", "Mushroom VIII");
        minions.put("e785f2fb94555998b380d19deffe120eb8dbd1191b0927312221e1f4f762a87d", "Mushroom IX");
        minions.put("9ff2be74b7aad963d3f7bad59d2f9cda1337c3d00af0bcd6e314ba1fe348dfae", "Mushroom X");
        minions.put("74e69059cb27b7b7c65b5543db19aa153a2509a0090719e5199f1082acc1b051", "Mushroom XI");

        minions.put("ef93ec6e67a6cd272c9a9684b67df62584cb084a265eee3cde141d20e70d7d72", "Cactus I");
        minions.put("d133a6e56ac05d1cfa027a564c7392b04c2cffda3e57c70b06ed5ae1d73ca6fe", "Cactus II");
        minions.put("1b35a27732a2cb5ee36a52653b2e7d98bdd9d3d799499035c9f918344570c9e8", "Cactus III");
        minions.put("6569e2f7104a423362844fdd645c3a9d2b8f8c1b8979d208ec20487ac2a5c783", "Cactus IV");
        minions.put("c4fe9efb395689a9254fea06d929c3c408a5b314084399b386c009ca83a062a9", "Cactus V");
        minions.put("ac8a964da5cc050812171dce6e9937191e4e7b65b7eb5f27e1846f868c023f58", "Cactus VI");
        minions.put("2c1b5f3b3ffb6a8983f5110d4fd347df6086205bcedd3e065cbdf9ee47f957fe", "Cactus VII");
        minions.put("2ddea64e86688c84d9edae63cf94765b9a5fac004e4babb3bfff081b30198327", "Cactus VIII");
        minions.put("5a03ed5566128ca6d7911e2e1614450e28372e2b0513327689e183168edc5711", "Cactus IX");
        minions.put("f4272b51f991a088d3aae579b93d253efc2e6d9657f0299191e3a18ee89a22c0", "Cactus X");
        minions.put("b1cabca262d9f98ccabf5546c033614f664173c6f206e626ca8b316d7962f8c8", "Cactus XI");

        minions.put("acb680e96f6177cd8ffaf27e9625d8b544d720afc50738801818d0e745c0e5f7", "Cocoa Beans I");
        minions.put("475cb9dcc1b3c33aca8220834588d457f9f771235f37d62050544be2f2825d1b", "Cocoa Beans II");
        minions.put("1d569fdd54d61e55c84960271950ce755d60ea6dc03c427773098649e8b7136d", "Cocoa Beans III");
        minions.put("5ed37c0b33043212ad9527df957be53f0e0fb08c184648cf0d2a64775fb6b4ec", "Cocoa Beans IV");
        minions.put("4ea5d503ed03184a906ad29a8b1809f20ba95b99bb889a8e6d04c2cc586c6412", "Cocoa Beans V");
        minions.put("b1db22b8f0a12492c2c7cf2784025c6cad2afc66998c4f47c0f02e6100454851", "Cocoa Beans VI");
        minions.put("afdfa53bdd3937be5305a2ef17b3f80860d12b85000dd51a80a4f3f9b744998b", "Cocoa Beans VII");
        minions.put("fa73332b8e1e64e172f4e8ccb58f93e78d06185db298b409eccedf6d6f6ebde3", "Cocoa Beans VIII");
        minions.put("db215abd78aced038772b6f73d828dbfc33369d7e9e00a58539e989508da6911", "Cocoa Beans IX");
        minions.put("80c4434c532a0e1a41dad610989f8a01432ea47adc39d64ec81fef81284d581", "Cocoa Beans X");
        minions.put("d71be56d6fbfec9e2602737dc3df8409368e23fb854b353b2451c30daa8c425b", "Cocoa Beans XI");

        minions.put("2fced0e80f0d7a5d1f45a1a7217e6a99ea9720156c63f6efc84916d4837fabde", "Sugar Cane I");
        minions.put("30863e2c1fdce44bc35856c25c039164845456ff1525729d993f0f40ede0f257", "Sugar Cane II");
        minions.put("f77e3fe28ddc55f385733175e3cf7866a696c0e8ffca4c7de5873cd6cc9fe840", "Sugar Cane III");
        minions.put("fbeac9c599b7d794a79a7879f86b10fb7743ad42c9937954d8ffeedc3ce55122", "Sugar Cane IV");
        minions.put("802e05d0a041aaf3a7fa04d6c97e67a66987c7617ae45311ae2bb6f2005f59c1", "Sugar Cane V");
        minions.put("ca9351b61e93840264f5cc6c6b5a882111ae58a404f3bbbe455e07bf868d3975", "Sugar Cane VI");
        minions.put("f983804d6b4afdcda8050670f51bb3890945fa4fa8a9c3cfa143a3d7912036a3", "Sugar Cane VII");
        minions.put("f2212f3b630af6b32b77905e4b45fc3f11046ccc9a7dd83b15d429944c4e2102", "Sugar Cane VIII");
        minions.put("49b33dad5234dc354a84f9217daf22684b58d80058de05c785f992f0b226590a", "Sugar Cane IX");
        minions.put("f1a94db5ee94ffdf4f3e87e5c12c0f112122fa52dc7c15f3881b6190aed4db92", "Sugar Cane X");
        minions.put("237514eb4e09053002f242a04997cfb3584928185acf99fa9a1d998bd987e1d7", "Sugar Cane XI");

        minions.put("71a4620bb3459c1c2fa74b210b1c07b4a02254351f75173e643a0e009a63f558", "Nether Wart I");
        minions.put("153a8dc9cf122c9d045e540d0624ccc348a85b8829074593d9262543671dc213", "Nether Wart II");
        minions.put("d7820332f21afe31d88f42111364cb8aa33746b6f1e7581fb5f50bbfe870f0ad", "Nether Wart III");
        minions.put("4b07451870cbd1804654e5b5db62e700efad8e7bcc7bf113a54ef6f5a5ab47e6", "Nether Wart IV");
        minions.put("8ef39a8e6958dafc2b5dbc55993d63e065f3d88e62e94ac6d28c865d85b9432", "Nether Wart V");
        minions.put("3245cbc9d11455ad30f9b7860604a372cc6bdba64ebe13babf6815de9ac5ab89", "Nether Wart VI");
        minions.put("d45298dcfb39274d0eed5df91ad744d7161d75da155f52955c44767231e88584", "Nether Wart VII");
        minions.put("b5c14d391dd776ebd5d0245bb762495371f666f5772022e82aebfdffc9b9447", "Nether Wart VIII");
        minions.put("3d8780f780548eb3d7fce773c09e89307a090b175570271808953eb81b5a9d72", "Nether Wart IX");
        minions.put("3e5291d28b362a5d8c17b521a317b3a66d68f0ed9e8f322b65db0c32c42e10a2", "Nether Wart X");
        minions.put("79d99c73e1f9a5376bd697c7ccbe3844f762a2b196fa72a5831988747aaacfa", "Nether Wart XI");

        minions.put("baa7c59b2f792d8d091aecacf47a19f8ab93f3fd3c48f6930b1c2baeb09e0f9b", "Flower I");
        minions.put("ddb0a9581e7d5f989d4fb6350fc7c51d65b3e49e4a0be35c3f3523287a0ff979", "Flower II");
        minions.put("de5c24a8bcd21e4f0a37551a2ad197a798be986ef08371ab11e95c2044bb1bc0", "Flower III");
        minions.put("5d473a99697430f82786b331c9657adef370655492b6763de9ea24066168ab41", "Flower IV");
        minions.put("6367c303c2c4f6ef4e0feeb528f37bcd71c1c0765621259ff00530c5d99b584b", "Flower V");
        minions.put("b8ffa832227440cbd8f218fb20f2cca7f9778024814b4e92bd5112d0a3f4b7f9", "Flower VI");
        minions.put("3bc942565909d05cb447945726411a3da83dcaa0a5c9b04fa041c3c5ca84e955", "Flower VII");
        minions.put("8959d9c639b20b294cf6cd0726422092682e762d3991ddac39d92cdc60334103", "Flower VIII");
        minions.put("4beed0b166465261f07399fe97304b9913f522e0d42e78d86849ec72be3d7fa9", "Flower IX");
        minions.put("d719f6041aaaf6c7b55042a550d51e17af727f6b8e41af09a1aded49c9ff9e31", "Flower X");
        minions.put("1142fe535855dd6b06f4f0817dbc8bf98da31265ae918b854cd11fcacd6fab4c", "Flower XI");

        minions.put("53ea0fd89524db3d7a3544904933830b4fc8899ef60c113d948bb3c4fe7aabb1", "Fishing I");
        minions.put("8798c0d7b65bfa5f56b084c1f51767a4276ad9f2c60bcb284dc6eccb7281e2ab", "Fishing II");
        minions.put("c8cefef2d7268a5170dd86fd782d6fee06f063b0a223e86378dde3d766c19929", "Fishing III");
        minions.put("cdfb98800f7d01c56744fa116d2275090a337334f6f884230522f8ea3964c9e0", "Fishing IV");
        minions.put("5eb079ce77840f08fb170aad0a89827695d92a6ccca5977f48c43fe931fd22f7", "Fishing V");
        minions.put("db557d80642ccd12c417a9190c8d24b9df2e797eb79b9b63e55c4b0716584222", "Fishing VI");
        minions.put("db557d80642ccd12c417a9190c8d24b9df2e797eb79b9b63e55c4b0716584222", "Fishing VII");
        minions.put("a5ee01b414c8e7fb1f55d8143d63b9dfed0c0428f7de043b721424c4a84eded3", "Fishing VIII");
        minions.put("204b03b60b99d675da18c4238d3031b6139c3763dcb59ba09129e6b3367d9f59", "Fishing IX");
        minions.put("593aa3e4eaa3911456d25aab27ce63908fe7a57d880a55884498c3c6a67549b0", "Fishing X");
        minions.put("46efc2d1ebb53ed1242081f22614a7e3ac983b9f6159814e6bcbc73ce7e3132a", "Fishing XI");

        minions.put("196063a884d3901c41f35b69a8c9f401c61ac9f6330f964f80c35352c3e8bfb0", "Zombie I");
        minions.put("c01613ba2e99ee8326b5ceae77efb1e9afa6ae541f38b4ed63e79ecb01e725f0", "Zombie II");
        minions.put("d6fdd8d54bc3a109b7e06baaf1b0ac97fb22989aa93069b63cca817ff7fd7463", "Zombie III");
        minions.put("bfbec1bd0fe3b71b9da9d7666fd6bbde341b4c481e8563fddf61f4ee52f7cd1b", "Zombie IV");
        minions.put("67a1945b52761443d1a7de233a4e4aea40c9abad92ae9ac35e385478971956ae", "Zombie V");
        minions.put("a8c3ab42d327fa01271f9f19958c77e0dee9fde57415f873783737a1e83f4e86", "Zombie VI");
        minions.put("5058f08910b39c30644f33fd71f81a412f6e05fe7c703a87fd4f3d5e4b2b6509", "Zombie VII");
        minions.put("e40b20aba5b3c279dee42b39d8e03de25cbead3421655f0cf1bea43ed0b4272e", "Zombie VIII");
        minions.put("fcbf17681e579f00d65f978c0b50915aaf2d5f609da7d9ab156cb6f092b88840", "Zombie IX");
        minions.put("6b4a9dc6d0fdbd1bad3613dcb3ab5c54c5ea5e0b498ee35b2fd30951cc2e9fcd", "Zombie X");
        minions.put("6699ff5ce9a0f5032340596f6b2dd6ac7028fc7cc5b943d4c1fc2d3749fedcd6", "Zombie XI");

        minions.put("a3dce8555923558d8d74c2a2b261b2b2d630559db54ef97ed3f9c30e9a20aba", "Revenant I");
        minions.put("c5aff1b4f533bb1e1cf5ea96caea7349d5efe9e9a982ec8051ac32910e3ae68c", "Revenant II");
        minions.put("d3865482377fb54bc07dc7633a5a25bbaaecc3e9978c04bf608776da1f8a154a", "Revenant III");
        minions.put("e138071b15709fd98c89597abddeefb70bee370fdcce7da9cbfa7275f2421557", "Revenant IV");
        minions.put("73de056cedbd88c61cff93c1f97e4cc69f0dafcdac3ca62013e3c0527fb5245", "Revenant V");
        minions.put("d51616207cb10c6414aa7812e56e3f4b408eac7c5ddc9011fe794b41b7ae7c24", "Revenant VI");
        minions.put("4097b94aecc2b187fcc251b2d2273554f66853436fa8ecc8e5156b148004c804", "Revenant VII");
        minions.put("57228dedbff9e114d5e4ce7f8e39c3bfb07f2c2f121545a8dc7803dfc0484786", "Revenant VIII");
        minions.put("e7eb574b6ab8b394c6b4a112ae18d5a672ffa414ec4dff3d65c9950523c19e0a", "Revenant IX");
        minions.put("d0197c8a4eaca2e5cc1b287ac84c62ef8c9f63068218105292dd89c3f7e64596", "Revenant X");
        minions.put("9cf6f95308bedb182b434aa73058aa8d69818b48900396cebc127c1bf7df6790", "Revenant XI");

        minions.put("2fe009c5cfa44c05c88e5df070ae2533bd682a728e0b33bfc93fd92a6e5f3f64", "Skeleton I");
        minions.put("3ab6f9c3c911879181dbf2468783348abc671346d5e8c34d118b2b7ece7c47c2", "Skeleton II");
        minions.put("ccd9559dc31e4700aaf001e0e2f0bd3517f238af25decd8395f4621404ca4568", "Skeleton III");
        minions.put("5b2df127315a583e767c6116f9c6ccdb887dc71fbe36ff30e0c4533db2c8514e", "Skeleton IV");
        minions.put("1605c73264a27d5c9339b8a55c830d288450345df37329023c13cdc4e4b46ccc", "Skeleton V");
        minions.put("b51e887ab5c0966bb4622882e4417037c3eee8a2d0162e2e82bf295f0d1e1db2", "Skeleton VI");
        minions.put("40ad48abf6ae82b8bad2c8a1f1a0c40dea748c05922b7ff00f705b313329e1f1", "Skeleton VII");
        minions.put("1a81a52e837daa71fd05c9e4c37a9cad2e722f96779b574127072d30a98af582", "Skeleton VIII");
        minions.put("e0a8fae40ff866e3fb7d9131f50efb8bd870da92cdf11051af48fa394bfa19e2", "Skeleton IX");
        minions.put("ed666149a1967b13df3341690c4c9a9f409b0f3b4f9ca8725d1969102ad420e0", "Skeleton X");
        minions.put("576255c781ebfb719d28f904813f69e20541d697f88bc6d96a6d4aa05b0fbc22", "Skeleton XI");

        minions.put("54a92c2f8c1b3774e80492200d0b2218d7b019314a73c9cb5b9f04cfcacec471", "Creeper I");
        minions.put("3fcf99ab9b31c2f0b7a7378c6936b63ac4a78857831729f08cca603925a5873b", "Creeper II");
        minions.put("488b4089a835e276838bba45c79d1146f0c2341971170a6163e6493890fd1b83", "Creeper III");
        minions.put("ac2d5f8dcfc9f35897f8b0a42ff0c19e483bdc745e7e64bf0aaf1054a6e67dd", "Creeper IV");
        minions.put("654bde9a26e35094e3438540c225cffa7690c1d4456251da30cc990ff921cc36", "Creeper V");
        minions.put("b5f07bbd87cffad76aeb5337e74726e7fcbd96c2bd0dcb083f5fca5aec2e12a", "Creeper VI");
        minions.put("f6f95998dd76a3bd9ffe949e7a4fe993b4baa2e981f49bf7113417f51003b193", "Creeper VII");
        minions.put("8c0abba2be5c9a93362a7da3231aeea824c5c590bfaaaec78888f1b3d9d32adc", "Creeper VIII");
        minions.put("21abd529c1898f6ec7e01d9943419c6358de93e0d6cdd2d90c8d63e7036db60d", "Creeper IX");
        minions.put("5699c6b6bc8adfa79e22ae51cc049fab2c7a51b686ca968df222cfa98faf92a", "Creeper X");
        minions.put("70850cccb3dfb7fe4bb0f7a008d5b4c10c08f9e36998f6f44ae8c9bc1b1b8e01", "Creeper XI");

        minions.put("e77c4c284e10dea038f004d7eb43ac493de69f348d46b5c1f8ef8154ec2afdd0", "Spider I");
        minions.put("c9a88db53bdf854c29d91b09027904684a6ba638d8007b7ad142a7321b9a212", "Spider II");
        minions.put("6be5128d61371acc4eabd3590013a5b8bfc678366e61c5363bf41a8b0154efdc", "Spider III");
        minions.put("4ef774366eef0ae26c9da09f52d101fd3a6181f62c059d579900d33098968058", "Spider IV");
        minions.put("eeb537b6d278623a110b4d31784ae415789972fad78bec236aa36f3a5f43a856", "Spider V");
        minions.put("dc785f2b1cca983928b0fe8ceb700660365b757e93a42a6651a937df773c70af", "Spider VI");
        minions.put("887abd32a5aae5870821fe0883002cdad26a58f9fee052c7ab9b800ee6e9ac52", "Spider VII");
        minions.put("4781e95aeb0e31be71e64093a084602de754f3e50443d5a4f685aac00d7a662f", "Spider VIII");
        minions.put("fe3869503b7fdeaa063351fd3579dbaf6fd3592bd4c30bac1058c9367c6a3823", "Spider IX");
        minions.put("5a4209e45b623b8338bcd184f15d279558c8a9d756d538e1584911780a60697a", "Spider X");
        minions.put("62d0262788369b6734e65d0240185affc2ead224a07efbcd70e4c7125d2c5330", "Spider XI");

        minions.put("97e86007064c9ce26eb4bad8ac9aa30aac309e70a9e0b615936318dea40a721", "Tarantula I");
        minions.put("578fea239bb3881ae53d6c735b8af69d8c6b477c0f5c34bc7cbed5792869ca67", "Tarantula II");
        minions.put("fc398914acb7fce5d93c2002a258f23b795d2f20d8e5fc555acd5070662efa0b", "Tarantula III");
        minions.put("bafde429ffcd5141f42b3d754d75a0ad3528594509c09be0083bc2c98d38fdce", "Tarantula IV");
        minions.put("f78b57faf9b4935932b749e10a2fff66532fecdede5a4e58f80d6f6ace2ed7ed", "Tarantula V");
        minions.put("ba4c2f24b79f98133f9fd66760685a18d4c29e415cef0b62e67e957085b3875b", "Tarantula VI");
        minions.put("7affad96dbfb4d5bfd4dc73d4dc1295db0062cbbd0c967b7da39bcc6e051b2e", "Tarantula VII");
        minions.put("cb0bdd9de5c6d56f3f3341ed7bc07d17f372d5f21b32fbb9cdc67ec7096a7cf0", "Tarantula VIII");
        minions.put("13cb3afa7d81b71751a246278d4f8f3a406a80a1302291ac620fc42c6cf2c179", "Tarantula IX");
        minions.put("535cc5773ffb461bc491270af45aa14cda6d7d92a4cc8c12b2b188620a2a44e4", "Tarantula X");
        minions.put("9c4d0dfb09516a79b286a9e8c67c4e981f245ff6221f470f6452fdafc0a92749", "Tarantula XI");

        minions.put("5d815df973bcd01ee8dfdb3bd74f0b7cb8fef2a70559e4faa5905127bbb4a435", "Cave Spider I");
        minions.put("677fb9a717ec81d45f89b73f0acd4ee03c9e396601a2de60f2719e4458c7325b", "Cave Spider II");
        minions.put("7f4912b76e599d12e73e4b03ee51a105999ad1306709fbffcfbaed556a9d7eb0", "Cave Spider III");
        minions.put("3d90f56d6e1632c00c14d568036aa536073c6a4a7e5759e012bd46d9f3809086", "Cave Spider IV");
        minions.put("c682c74ba44a5221a70f98188e76a4e88e41f633363a54af1d26247423130636", "Cave Spider V");
        minions.put("b54735acf9c010f2d25d7af70d600d8bc2633729a4fde7b4ac248c211135f3ab", "Cave Spider VI");
        minions.put("729095202ca3cd63556e3549f71c39aae4b6718170de19067d6819be4ddecd6e", "Cave Spider VII");
        minions.put("5c4ec7d3c5084a5c91bdf3fba196a1d12d5bf71049b61b97dd1c5854617a41cf", "Cave Spider VIII");
        minions.put("42654f0248464e23cf70811a1b1665cad19aa207857f05452967f860458a4c64", "Cave Spider IX");
        minions.put("4cded81400f3ced561bed776bd44b48e784f7a810ba6cd6340d26f4c00a0c50f", "Cave Spider X");
        minions.put("36303fc7e2046822ec79a95ce5c7350e58dabd8d776e5c36669f5404581d0459", "Cave Spider XI");

        minions.put("3208fbd64e97c6e00853d36b3a201e4803cae43dcbd6936a3cece050912e1f20", "Blaze I");
        minions.put("ffcc301b04b1537f040d53fd88a5c16e9e1fde5ea32cd38758059a531b75cb46", "Blaze II");
        minions.put("da5e196586d751ba7063bcf58d3dc84121e746288cb3c364b4b6f216a6492a27", "Blaze III");
        minions.put("6ddae5fcdd5ede764f8fe9397b07893ccf3761496f8e2895625581ce54225b00", "Blaze IV");
        minions.put("f5e3a84c9d6609964b5be8f5f4c96800194677d0f8f43d53a4d2db93dbb66fad", "Blaze V");
        minions.put("e9d7db90d3118ef56c166418a2232100fb4eb0ab5403548cfa63e985d5e0152c", "Blaze VI");
        minions.put("a9bdeb530d09ee73479db19b357597318eac92ee7855740e46a1b97ae682b27", "Blaze VII");
        minions.put("d7fc92fa962d0944ce46b71bc7dcb73a5f51f9d8a7e2bcccf666f2da05a0152d", "Blaze VIII");
        minions.put("a2a246dbcc45be4a936a19b44fcb61725c0fe2372a0ce0676fb08fd54d4d899b", "Blaze IX");
        minions.put("ea357aeaf75a8cfed2b3c1c8f3ccf54f907ae2b64fa871cf201baeef53528e19", "Blaze X");
        minions.put("e791eb26b39f162f552d539a4d22c4bee8aa9c571d9acf82a012593bb945c360", "Blaze XI");

        minions.put("18c9a7a24da7e3182e4f62fa62762e21e1680962197c7424144ae1d2c42174f7", "Magma Cube I");
        minions.put("212ff47f5c8b38e96e940b9957958e37d610918df9b664b0c11bd6246799f4af", "Magma Cube II");
        minions.put("376d0b9eb9e5633d21424f6eaade8bd4124b9c91f3fa1f6be512fe0b51d6a013", "Magma Cube III");
        minions.put("69890974664089d1d08a34d5febead4bb34508f902aa624e0be02b61d0178b7f", "Magma Cube IV");
        minions.put("5a74333ed5c54aef95aead60c21e541131d797d3f0d7a647915d7a03bbe4a5fe", "Magma Cube V");
        minions.put("5de0153aa18d34939b7d297c110e7a207779908cee070e3278a3d4dc9e97b122", "Magma Cube VI");
        minions.put("bf77572393b4b420559f17a56cb55f9ec47c3e9958403184699dba27d12f3ef2", "Magma Cube VII");
        minions.put("365c702393988e0312f56c00c6e73c8cf510b89df05ad766a65b36a1f281b604", "Magma Cube VIII");
        minions.put("76101f4bb000518bbedc4b1147a920a99f141b8a679f2984fb94741a33eed69f", "Magma Cube IX");
        minions.put("e9e67c3860cc1d36cb4930e0ae0488c64abc4e910b4224dc9160d273c3af0bba", "Magma Cube X");
        minions.put("6ab2af6b08c3acedd2328e152ef7177f6bbb617dc985dfbfecdc982e04939b04", "Magma Cube XI");

        minions.put("e460d20ba1e9cd1d4cfd6d5fb0179ff41597ac6d2461bd7ccdb58b20291ec46e", "Enderman I");
        minions.put("e38b1bacbce1c6fa1928a89d443868a40a98da7b4507801993b1ab9bb9115458", "Enderman II");
        minions.put("2f2e4d0850b0d87c0b6a2d361b630960ff9165a47893c287eddf3eda2caa101b", "Enderman III");
        minions.put("2b37ae94f463c642d7c0caf3da5b95b4b7568c47daad99337ecefdeb25be5d9d", "Enderman IV");
        minions.put("9dd3f4532c428d0589bac809463b76e15e6fa31bccd2d5e350aa7d506b792904", "Enderman V");
        minions.put("89f50d3955bec550def51df0e4e143cda3d71314f9a7288dd92e0079605b5363", "Enderman VI");
        minions.put("368c2e2d9827cb25bf4add695f668180bb2b52d41342f175bdfeb142f960d712", "Enderman VII");
        minions.put("84c91f6c71b6f75b7540134cb4d36b7e3c5ff8f26b6919a7410fe3427663b7dd", "Enderman VIII");
        minions.put("c70a920c4940a1ffaebcc20b87afaaf0b17ebc4d3b1c34dfd0374a0a583de32d", "Enderman IX");
        minions.put("ecaf73a2cd819331d8096caf2f83f65db119692f0600c02d48081ceacf0c864c", "Enderman X");
        minions.put("86906d7f34af69a797ddf5b5a5b1c428f77284451c67e788caf08070e3008ad", "Enderman XI");

        minions.put("2478547d122ec83a818b46f3b13c5230429559e40c7d144d4ec225f92c1494b3", "Ghast I");
        minions.put("cd35bd7c4dd1792eeb85ee0a54645cd4e466c8b7b35d71dde4a4d51dfbbdb13f", "Ghast II");
        minions.put("e1fb348c7c14e174b19d14c8c77d282f1abe4c792519b376cd0622a777b68200", "Ghast III");
        minions.put("1b0c2e0852f7369ea7d3fe04eb17eff41bb35a1a8a034834369e1e624c79c03", "Ghast IV");
        minions.put("a3c5c52a4c945825e4c959c5cb6aa607a0e3a1bffd5cb6a0577e172d0f356a2b", "Ghast V");
        minions.put("ef97eff2721dc201b23373afc3111eda22a325c08de6a14f03dcfcb98d3c9507", "Ghast VI");
        minions.put("5836df340405415ad7d8b84bbe0e806d0cfed990796c3ade38934169a48ebd25", "Ghast VII");
        minions.put("7f2e537ca12c9d8bd0ec6bd56ac8bdae86521e960b5852b0bbb31b2cc83dfc7e", "Ghast VIII");
        minions.put("af4d8d82f4d86569c70d265f5cf62e46ee8dc0a5a6d97ef1901c793d0b127545", "Ghast IX");
        minions.put("4013d128e7116812388b789ec641d31d48bf10aa862f7d63d2b4fc0a03d147a2", "Ghast X");
        minions.put("5840896c78884ebb35103b31ffd7276c941ea862b8b6b0e0810a66b4ed66cbc2", "Ghast XI");

        minions.put("c95eced85db62c922724efca804ea0060c4a87fcdedf2fd5c4f9ac1130a6eb26", "Slime I");
        minions.put("4a3ea6b0c297c5156249353ff7fcf57b1175e1b90b56a815aa039009ff0ea04f", "Slime II");
        minions.put("b6b35286eb19278b65c61a98ed28e04ca56a58386161b1ae6a347c7181cda73b", "Slime III");
        minions.put("7afc7e081dcc29042129e1b17a10baa05c8e74432600465bf75b31c99bab3fae", "Slime IV");
        minions.put("f0d0c0365bc692b560d8e33e9ef6e232c65907957f6bec4733e3efa4ed03ef58", "Slime V");
        minions.put("a0356eda9d7227d59ad1c8616bad1bed33831670867755e1bc71a240013de867", "Slime VI");
        minions.put("7266c128064e202143402ac7caee52392e3b003274c25ad8ac5c6773bf863ca2", "Slime VII");
        minions.put("b967e05936b33c2819d32f3aecbecdd478130fccbe877275e131235968ffb6b2", "Slime VIII");
        minions.put("827b73cde1cdf73e4393f5177626c681bfaaeaf5c93f9237b6cce4f2f6a74ee8", "Slime IX");
        minions.put("7a2d1ca7dc1a6d9b3b2ee4cf5641bf4add7419f6ac97060898bd98924ab91589", "Slime X");
        minions.put("c04c9cb411cfd504c3bc7972fc74acd5045c55e1a76379d40e37f5d73c92e453", "Slime XI");

        minions.put("c2fd8976e1b64aebfd38afbe62aa1429914253df3417ace1f589e5cf45fbd717", "Cow I");
        minions.put("e4273e1870f9fc54358f7193b7fa3f27fb7bac1d68c9941f63f3c588337b70", "Cow II");
        minions.put("9c12694906b281c988312cf0575d93274c178a0449b71eff047de1eeb01e3b64", "Cow III");
        minions.put("e7b32af9f116a425c7394d23dd851f3bff53f05ec413fb2fce3839533d925a86", "Cow IV");
        minions.put("7b412e13e1eba6d84336aee778115f183b88cbbe546b83ea64c5b6295145355a", "Cow V");
        minions.put("a63ba85ccc57534108199cb2034826d9853e40df3a8edaf6452326b73748e22a", "Cow VI");
        minions.put("fd9cb1a9c54e00d1030a101c961f1f516c145b719f4ec8e7d4ab3c9759ae10f3", "Cow VII");
        minions.put("9e28cd7376398c57887bc326c14c04c9c5796f613d7de9565d5e66c5b12c4d41", "Cow VIII");
        minions.put("cfa251097580c0d8d26e93e446f28469ae7b5f1208e559626683b4a5ecf5e0e2", "Cow IX");
        minions.put("3e3f56f3924106eb91414a8859e76b0962395dffaeb91ebda538332fd9774cea", "Cow X");
        minions.put("cbe1ed84b41681fff45a60cb57b884e6bf4ecc23df2aa6cb112f74d3cb52e315", "Cow XI");

        minions.put("a9bb5f0c56408c73cfa412345c8fc51f75b6c7311ae60e7099c4781c48760562", "Pig I");
        minions.put("13d136654297e744ccb3ba71bb85bd7653267db4b9b940b621be587d52a51310", "Pig II");
        minions.put("d0215bbadccec19fc11b04d10958eedea0cb2957479d60d092fcb7339e0d3a3d", "Pig III");
        minions.put("8a591979d1f27c834b837482ff077dd6ae60603af1d42efd54fd0fe423f473b2", "Pig IV");
        minions.put("c6dcf14cfaee6c9a5aef79f7cfe7f0a05f6d1d51c0ae9f93e44945a99d7b67e9", "Pig V");
        minions.put("d3054be358caefe2b9c049159144dbd94de0bdefab4fa07472d8d8f3b22a1edc", "Pig VI");
        minions.put("73c5582b39fc6c08d4adc8c27bd7b9fc1340073ace1d5571276f57bfc852d864", "Pig VII");
        minions.put("6be861ec200f4741fb5a202c31b94c345417b7b85bc3e5dd595fdedf387a5559", "Pig VIII");
        minions.put("caa9f8b050d5f71bb638398af11fe0c6f523251b4d8ff262979248933e2ac7b1", "Pig IX");
        minions.put("4a466ca591bfe16022be2a6f8aeb2c6321913fd6ad5cb9f40f5e0058521b0d3a", "Pig X");
        minions.put("9281a6db6bec7d3d5f05f3bbec4eca94ba2073863b0ec2fa853c0c8f28c97629", "Pig XI");

        minions.put("a04b7da13b0a97839846aa5648f5ac6736ba0ca9fbf38cd366916e417153fd7f", "Chicken I");
        minions.put("7ae39f29a0cc4d8ac8277e7a4e6d56b0e42f04267a9f9033fcba109751ebfff5", "Chicken II");
        minions.put("2fdacd78fce2c6c70cd020dd0cf69481582d97796abcda0a282e1f7e1a9ab6f3", "Chicken III");
        minions.put("c968476a306df54c26053b639de69e1473b5b453a4f84cf371f675ba794314da", "Chicken IV");
        minions.put("597ca4daa25ad8a48eb0a34a23000971f87fe42319c32375c21dea940ffffd5e", "Chicken V");
        minions.put("7a6ed3e94cc354164f759c448f39cc0ac0ee50feae2e4008e26c890a8387f7e", "Chicken VI");
        minions.put("c1c9ed510850622947e215dbd9b018939a7908595c645c8415fc8e4e5ce714d", "Chicken VII");
        minions.put("c3812cb86fe22971d0ae58789f18a1d208116cb204329aff7905aa3993b0d0d8", "Chicken VIII");
        minions.put("9f24c0d1e3aa3c2999a1268fcc0f933591a9910437f082b1d5dc9bed7ee1a753", "Chicken IX");
        minions.put("4212ce883dfd2bec43e6cd9b7a7f86be1cca8ebceb33b83e3e70ad873717be18", "Chicken X");
        minions.put("d5c12fd3968d389f6d053b1a7a85dc1cfb90a39385c379e3afee6295aaafcd37", "Chicken XI");

        minions.put("fd15d4b8bce708f77f963f1b4e87b1b969fef1766a3e9b67b249c59d5e80e8c5", "Sheep I");
        minions.put("deaee0de135a24a27b8920ddc1c7b58314ffaba3ef3f4cf0d77195936d471c20", "Sheep II");
        minions.put("c33da48269f28698c4548c1dbb8773f8e49888afd93af5f5b420e0f43c39f2eb", "Sheep III");
        minions.put("3bd2c5fe2fe9be577c9034d3abfdbd3e90c697deebf9cd35107786bd4dd0555b", "Sheep IV");
        minions.put("f7b64375097693c11215acd72c429d2770e746178aa4014066285974fdacdaa6", "Sheep V");
        minions.put("aea49ac4e8f88bbf0321b55ed264df0d527952ce49c387fdebdedc5d6447376", "Sheep VI");
        minions.put("d23301e0358c2c33e55011cec3a848c6cb4f3c8a016968ffc55190ff2d813c85", "Sheep VII");
        minions.put("f04de71765e46be9bcdb6c499f954c9bc831563d52776c593c16c99113bcb2d9", "Sheep VIII");
        minions.put("eea074e9e53cb179da2ebd625de042b70cb0d8cc7280fc101c7cafb9abe07680", "Sheep IX");
        minions.put("3f92d454109855656d16061c8947760ce84a9561863481292ce8aa60b981911c", "Sheep X");
        minions.put("6abba939e3a292203108d09da6a867dcf77cef01a5e6e77bcf9cfac5360b0e88", "Sheep XI");

        minions.put("ef59c052d339bb6305cad370fd8c52f58269a957dfaf433a255597d95e68a373", "Rabbit I");
        minions.put("95beb50764cd6b3bd9cdad5c8788762dde5b8aca1cd47b9ebdeaf4ab62046022", "Rabbit II");
        minions.put("4caf38c59c689f162a1fedba239a6e44fd6c65c103038c91c0d32e5713a0694c", "Rabbit III");
        minions.put("a5253184a1665ef0e1ab9da27dcfff2bdbde45836e5b26fc860cee9c2eccf741", "Rabbit IV");
        minions.put("cd465a0e504286b0dcea425e599e8296c442138cefcea98c76cd966fe53d0639", "Rabbit V");
        minions.put("1fe6e315e28258a79ec55c5a12f2ec58fe3fa3b735517779eaa888db219f305b", "Rabbit VI");
        minions.put("c110ae6f601c71a6a779a2943a33546dc08adaac4fdfd54cfc4a98aa90ca12fb", "Rabbit VII");
        minions.put("e553b809b5164816aa61d5e39f8998d59fac4a35ff01c54d8a16b16627b06403", "Rabbit VIII");
        minions.put("26e6ecd9f7dfd5ee99a7964e0e404953a29907acca4d6b165aa2ef9807119fe0", "Rabbit IX");
        minions.put("3ccfa391def65b86e90f1938c98f1dc5874e9cc94e3eefce91ba40a202de4e69", "Rabbit X");
        minions.put("7f3fdd04826405dec5c17d0f688e874e7ba9bfbdead28b7ed5a0463335629697", "Rabbit XI");

        minions.put("57e4a30f361204ea9cded3fbff850160731a0081cc452cfe26aed48e97f6364b", "Oak I");
        minions.put("bb4eccf762baf18f2d5b5b0c8fa9ca2ce1150f8beb1ce66756a4884c68253d9a", "Oak II");
        minions.put("a306123edb86a30535267a12ba6ab13558d93abad973793dba6c82c929dfb430", "Oak III");
        minions.put("c643dd831a5d5e409b22f721bd4a6d1e1109b1b24e1fbafeeb0d2aba8c626ce9", "Oak IV");
        minions.put("553cbf53549d02cd342aafa13534617514a363ae74db94834fced3a8dd3801b8", "Oak V");
        minions.put("3497c3ff3cf509495bbf59884f8ecae2148ee391a589d4e20bbcb7872d55373f", "Oak VI");
        minions.put("c22238ee3f8a38acb4bd05a68479b9b478967eecd51547631c553733c20f6bd9", "Oak VII");
        minions.put("fcf9f335bc5c68cf1bb1590d421e8564b942ed94d3c2b4025c1b30168981214e", "Oak VIII");
        minions.put("93b2cb6e9ec862139600e83505e6b56e07838abb1b6faf4649db9a7098096d20", "Oak IX");
        minions.put("546f4040054a097956bf7e135656ea8f52c53acaebddbddbff8d123231c82e93", "Oak X");
        minions.put("e613f991f92bd0cf700cfee9a1440ff4dfe89999792e1eb9698b406549761180", "Oak XI");

        minions.put("7ba04bfe516955fd43932dcb33bd5eac20b38a231d9fa8415b3fb301f60f7363", "Spruce I");
        minions.put("3cc4e6fa46cd52a6480dc2eac053e9ac8a7d6ee0ee9c9cf74e176b289a43eb3a", "Spruce II");
        minions.put("b2d2366357a435a230fbbdd55929c23dd4985a8978020102255b7a007476fa56", "Spruce III");
        minions.put("2c188216e275281e49e64a32b787463dff849e3f6f05ae307f4b21f68be28232", "Spruce IV");
        minions.put("bdb2fcbf4be4a110b814d93fe8093ba66badabb6d65c58846a731935fa0228f0", "Spruce V");
        minions.put("5b2efe8fe599598326b4941c2ff55c284ce26b0948b520c0490de8b0d9aeff4a", "Spruce VI");
        minions.put("e1b1af499ef6a63dc5b111e955c3ad7b4647841135df7953c1d441955540a6a4", "Spruce VII");
        minions.put("ed3f7f42298490fcf71e27a7b4c5ed5f2c556c58c97fd0f2e3460488d32938c7", "Spruce VIII");
        minions.put("999cbe069cd5fc2368e41c9dd073d1aedaa8e5465276d4b8852ac5a917bbdda8", "Spruce IX");
        minions.put("74ba98e2b81e9426e5f1f44b63559633b3b2ab416a72cbc3b6cb4d527aaad8cd", "Spruce X");
        minions.put("da54f11da358d14fa11e2c32eb1b93d9444eabcd600e32cc0ab462172a1f12c", "Spruce XI");

        minions.put("eb74109dbb88178afb7a9874afc682904cedb3df75978a51f7beeb28f924251", "Birch I");
        minions.put("6dd53989833505625fa9fc5ce5d4c8a745f25201e58d56cc6f94125c78606a91", "Birch II");
        minions.put("6ed87a6d743d9e036b169b03973c5772b611db48f5c6844f1f427ffa702c12ef", "Birch III");
        minions.put("ac49f5616584ddb09b46e2d9eba91228c5c55d81dd557c8bf84f7ead7e74578a", "Birch IV");
        minions.put("1a1fb86ed5a7d5bddcee9593eed7142f68b4fb55a8b812d0bfaa765e2162138d", "Birch V");
        minions.put("7b79821acb2d8dd8bc54ac77ee6486d6bd21f5e20c828f84973325d6b3f2eb41", "Birch VI");
        minions.put("292863ce28af7319e7181be85be55c43be21d3efba789f4768cffaefd488206f", "Birch VII");
        minions.put("8f85e3656474430d5cca86f73c474aa647d78594791fcd5acb8d637f60133164", "Birch VIII");
        minions.put("5e07676b749e912c6299bdb05904aba8fc6df91eb9494376957fcf0f745be295", "Birch IX");
        minions.put("d0d6563ad8a3f57870674b7ed87069401016be21cc43625850197db8d299482d", "Birch X");
        minions.put("c7461229df076f8137a4560b38365ae48430b01070b90221aa5846284c17b876", "Birch XI");

        minions.put("5ecdc8d6b2b7e081ed9c36609052c91879b89730b9953adbc987e25bf16c5581", "Dark Oak I");
        minions.put("b25860cc1423ab010cf17697b288fdd3f5cb725ea9ab3e88a499dc1938104b02", "Dark Oak II");
        minions.put("2ecb65fceae74d76106b02eaa31bd80cc26b3f88d32372b645658d337352b42", "Dark Oak III");
        minions.put("358db48413f01eb669ac98a4cb0884021307886e29048a072d27e4f73e1ea6fe", "Dark Oak IV");
        minions.put("cf0969d586970c7ed5fef0c44d2899cfc97780488a36d725d55a6569dd02fa3c", "Dark Oak V");
        minions.put("299b2d8c62b17108023c57e2bc40873446e1b96f11674a2bb2a27f915cf9d519", "Dark Oak VI");
        minions.put("3ee074f5bb1680686d0794506c6c26e8f6acf1117b015ad3441aa938c9dcc8d", "Dark Oak VII");
        minions.put("fd20485516e15e9c7ade2529848ebee04a9242fea2e2eefa4b336e7bd9177af1", "Dark Oak VIII");
        minions.put("c0cde69130063d80dcd974d96ac02af355deeb1a5391fa14cbabecb530924ad3", "Dark Oak IX");
        minions.put("9fc5b2ee7d07de80538e77d651c9190eeafea9ef3dfe094589f70117c4d4ed07", "Dark Oak X");
        minions.put("23c650b69189a1da2a0a9e9d0a235cb89df0f32ab421ad059e012be59638057f", "Dark Oak XI");

        minions.put("42183eaf5b133b838db13d145247e389ab4b4f33c67846363792dc3d82b524c0", "Acacia I");
        minions.put("9609bcfecad73c84dd957673439a7f56426269fc569b6e8405d1a1c05ced8557", "Acacia II");
        minions.put("85c6492e5b0e3315fbdfd2314ee98073abdcdcbec36b4915b40e0943a95d726", "Acacia III");
        minions.put("4afae3d06cb1510d931c3b213854549614983e6d8e2440ce76b683960aab69f6", "Acacia IV");
        minions.put("f06b64b7743a20fc36f2aaa0908d64346540af97e38d8263acf5b53e4e4a16fe", "Acacia V");
        minions.put("836bc401455a23aed7f83b6ae46f2bcd52809a153bb5888b04a7dca3a702f531", "Acacia VI");
        minions.put("572b1b70882093a9d19c96e9dd7db8bd51aa117f5b5bbbc27e3bafb9e1c1167", "Acacia VII");
        minions.put("10a919b3efd2521fc823b2da1246568d5e83dc1f6908ac128d19cde5d326d469", "Acacia VIII");
        minions.put("2f0b33a2ab3e165a193d33e148f61384d01ed45d9edabbf1e55a3016ccd991f5", "Acacia IX");
        minions.put("9b4826120105ca75f208c3b97225245033e156a61fb53ecebc3fa6e1baaba919", "Acacia X");
        minions.put("4f6e34656f238ed0d6823fc31cb16455f79aa9756884225d6ce4ef681c8240eb", "Acacia XI");

        minions.put("2fe73d981690c1be346a16331819c4e8800859fcdc3e5153718c6ad45861924c", "Jungle I");
        minions.put("61a133a359788b12655cfb9abd3eb71532d231052f5bb213fd05930d2ee4937", "Jungle II");
        minions.put("9829fa43121066bc01344745f889c67f8e80a75ba30a38e017d2393e17cfef21", "Jungle III");
        minions.put("95ca25a3b4fc31454da307a4e98c09455efaaa9f2c074b066a98300764e2690b", "Jungle IV");
        minions.put("20d26c2e29b2205c620b8b60fbaa056942d5417b75a2acc7f4c581b0e9bc6d", "Jungle V");
        minions.put("b8619464d104822d9937344d11ee5c037169a13b2473f59b24836fca4cf214c5", "Jungle VI");
        minions.put("d7113a0d8e635447ef7b1908cab69d6fd68c010f1fc08b9db4d2612a35e65646", "Jungle VII");
        minions.put("24606b1daf8e60363fc8db71ef204262ee800fa7b6496fb2e05f57d0674ef51f", "Jungle VIII");
        minions.put("a4bbeb118757923d36871c835779aa71f8790931f64e64f2942ad3306aee59ad", "Jungle IX");
        minions.put("3ee34e1469da11fe6c44f2ca90dc9b2861a1e7b98594cb344d86824eeeabcb60", "Jungle X");
        minions.put("dbefc4e8d5c73d9a9e3fe5b1009f568c5d3cb071fa869b54d2604cadef474505", "Jungle XI");
        return minions;
    }
}