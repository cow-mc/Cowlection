package de.cowtipper.cowlection.listener.skyblock;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.data.Friend;
import de.cowtipper.cowlection.data.HySkyBlockStats;
import de.cowtipper.cowlection.util.ApiUtils;
import de.cowtipper.cowlection.util.MooChatComponent;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DungeonsPartyListener {
    private static final Pattern PARTY_START_PATTERN = Pattern.compile("^Party Members \\((\\d+)\\)$");
    private static final Pattern PARTY_LEADER_PATTERN = Pattern.compile("^Party Leader: (?:\\[.+?] )?(\\w+) ●$");
    private static final Pattern PARTY_MEMBERS_OR_MODERATORS_PATTERN = Pattern.compile(" (?:\\[.+?] )?(\\w+) ●");
    private Cowlection main;

    private static Step nextStep = Step.STOP;
    private int msgCounter = 0;
    private boolean listenForChatMsgs = true;
    private final AtomicInteger pendingApiRequests = new AtomicInteger();
    private final ConcurrentHashMap<String, Optional<UUID>> partyMembers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HySkyBlockStats> partyMemberStats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> partyMemberFoundDungeonSecrets = new ConcurrentHashMap<>();

    public DungeonsPartyListener(Cowlection main) {
        if (nextStep == Step.STOP) { // prevent double-registration of this listener
            nextStep = Step.START;
            this.main = main;
            // get party members by parsing output of /party list:
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    public DungeonsPartyListener(Cowlection main, List<String> partyMembers) {
        if (nextStep == Step.STOP) { // prevent double-registration of this listener
            this.main = main;
            nextStep = Step.AWAITING_API_RESPONSE;
            for (String partyMember : partyMembers) {
                this.partyMembers.put(partyMember, Optional.empty());
            }
            getDungeonPartyStats();
        }
    }

    // priority = highest to ignore other mods modifying the chat output
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onMessageReceived(ClientChatReceivedEvent e) {
        if (e.type != 2 && listenForChatMsgs) { // normal chat or system msg (not above action bar), and not stopped
            if (msgCounter == 15) {
                // received too many messages without detecting any party-related lines, abort!
                listenForChatMsgs = false;
                shutdown();
                main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Wasn't able to detect the party member list. Maybe the chat formatting was changed?");
                return;
            }
            ++msgCounter;

            String text = EnumChatFormatting.getTextWithoutFormattingCodes(e.message.getUnformattedText().trim());

            if (text.isEmpty()) {
                // spacer message, nothing to see here
                return;
            }

            switch (nextStep) {
                case START:
                    if (text.equals("You are not currently in a party.")) {
                        shutdown();
                        return;
                    }
                    Matcher matcher = PARTY_START_PATTERN.matcher(text);
                    if (matcher.matches()) {
                        int partySize = Integer.parseInt(matcher.group(1));
                        if (partySize < 2 || partySize > 6) {
                            // party too small or too large, abort
                            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Party is too " + (partySize < 2 ? "small" : "large") + " for a dungeons party.");
                            shutdown();
                        } else {
                            nextStep = Step.LEADER;
                        }
                    }
                    break;
                case LEADER:
                    matcher = PARTY_LEADER_PATTERN.matcher(text);
                    if (matcher.matches()) {
                        // found leader
                        partyMembers.put(matcher.group(1), Optional.empty());
                        nextStep = Step.MODERATORS_OR_MEMBERS;
                    }
                    break;
                case MEMBERS_OR_END:
                    if (!text.startsWith("Party Members: ")) {
                        // seems to have reached the end
                        nextStep = Step.API_REQUESTS;
                        break;
                    }
                    // fall-through:
                case MODERATORS_OR_MEMBERS:
                    matcher = PARTY_MEMBERS_OR_MODERATORS_PATTERN.matcher(text);
                    boolean isPartyMods = text.startsWith("Party Moderators: ");
                    if (isPartyMods || text.startsWith("Party Members: ")) {
                        while (matcher.find()) {
                            // found moderators/members
                            partyMembers.put(matcher.group(1), Optional.empty());
                        }
                        nextStep = isPartyMods ? Step.MEMBERS_OR_END : Step.API_REQUESTS;
                    }
                    break;
                default:
                    // do nothing
                    break;
            }

            if (nextStep == Step.API_REQUESTS) {
                listenForChatMsgs = false;
                nextStep = Step.AWAITING_API_RESPONSE;
                if (partyMembers.size() > 0) {
                    getDungeonPartyStats();
                } else {
                    main.getChatHelper().sendMessage(EnumChatFormatting.RED, "How did you end up in a party with zero members?");
                    nextStep = Step.STOP;
                }
            }
            if (nextStep == Step.STOP) {
                listenForChatMsgs = false;
                shutdown();
            }
        }
    }

    private void getDungeonPartyStats() {
        for (String partyMemberName : partyMembers.keySet()) {
            pendingApiRequests.incrementAndGet();
            pendingApiRequests.incrementAndGet();
            ApiUtils.fetchFriendData(partyMemberName, partyMember -> {
                // (1) send Mojang API request to get uuid
                if (partyMember != null && !partyMember.equals(Friend.FRIEND_NOT_FOUND)) {
                    partyMembers.put(partyMemberName, Optional.ofNullable(partyMember.getUuid()));

                    ApiUtils.fetchSkyBlockStats(partyMember, hySkyBlockStats -> {
                        // (2) once completed, request SkyBlock stats
                        partyMemberStats.put(partyMemberName, hySkyBlockStats);

                        if (pendingApiRequests.decrementAndGet() == 0) {
                            // (3) wait for all requests to finish
                            // (4) once completed extract relevant data
                            nextStep = Step.SEND_PARTY_STATS;
                            sendDungeonPartyStats();
                        }
                    });
                    ApiUtils.fetchHyPlayerDetails(partyMember, hyPlayerData -> {
                        // (2) once completed, request player stats
                        partyMemberFoundDungeonSecrets.put(partyMemberName, (hyPlayerData != null ? hyPlayerData.getAchievement("skyblock_treasure_hunter") : 0));

                        if (pendingApiRequests.decrementAndGet() == 0) {
                            // (3) wait for all requests to finish
                            // (4) once completed extract relevant data
                            nextStep = Step.SEND_PARTY_STATS;
                            sendDungeonPartyStats();
                        }
                    });
                } else {
                    // player not found (nicked?)
                    pendingApiRequests.decrementAndGet();
                    pendingApiRequests.decrementAndGet();
                }
            });
        }
    }

    private void sendDungeonPartyStats() {
        String thePlayerName = Minecraft.getMinecraft().thePlayer.getName();
        MooChatComponent dungeonsParty = new MooChatComponent("Dungeons party").gold().bold();

        StringBuilder playerEntry = new StringBuilder();
        StringBuilder playerTooltip = new StringBuilder();
        String partyMemberName = "";
        for (Map.Entry<String, Optional<UUID>> partyMember : partyMembers.entrySet()) {
            if (playerEntry.length() > 0) {
                // append previous data
                MooChatComponent dungeonPartyEntry = new MooChatComponent(playerEntry.toString())
                        .setSuggestCommand((partyMemberName.equals(thePlayerName) ? "/boop " : "/p kick ") + partyMemberName, false);
                if (playerTooltip.length() > 0) {
                    dungeonPartyEntry.setHover(new MooChatComponent(playerTooltip.toString()));
                }
                dungeonsParty.appendFreshSibling(dungeonPartyEntry);
                // reset 'caches'
                playerEntry.setLength(0);
                playerTooltip.setLength(0);
            }
            partyMemberName = partyMember.getKey();
            String errorNamePrefix = "  " + EnumChatFormatting.RED + partyMemberName + EnumChatFormatting.LIGHT_PURPLE + " ➜ " + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC;

            if (!partyMember.getValue().isPresent()) {
                playerEntry.setLength(0);
                playerEntry.append(errorNamePrefix).append("player not found, might be nicked?");
                continue;
            }
            UUID uuid = partyMember.getValue().get();
            // player name:
            playerEntry.append("  ").append(EnumChatFormatting.DARK_GREEN).append(partyMemberName).append(EnumChatFormatting.LIGHT_PURPLE).append(" ➜ ").append(EnumChatFormatting.GRAY);

            HySkyBlockStats sbStats = partyMemberStats.get(partyMemberName);
            if (sbStats != null && sbStats.isSuccess()) {
                HySkyBlockStats.Profile activeProfile = sbStats.getActiveProfile(uuid);

                if (activeProfile == null) {
                    // player hasn't played SkyBlock
                    playerEntry.setLength(0);
                    playerEntry.append(errorNamePrefix).append("hasn't played SkyBlock yet");
                    continue;
                }
                // ^ abort if any of the above failed, otherwise visualize API data:
                HySkyBlockStats.Profile.Member member = activeProfile.getMember(uuid);

                // active pet:
                HySkyBlockStats.Profile.Pet activePet = member.getActivePet();

                // append player name + class and class level + armor + active pet
                playerTooltip.append(EnumChatFormatting.WHITE).append(EnumChatFormatting.BOLD).append(partyMemberName).append(EnumChatFormatting.LIGHT_PURPLE).append(" ➜ ").append(EnumChatFormatting.GRAY).append(EnumChatFormatting.ITALIC).append("no class selected")
                        .append("\n").append(String.join("\n", member.getArmor()))
                        .append("\n\n").append(EnumChatFormatting.GRAY).append("Active pet: ").append(activePet != null ? activePet.toFancyString() : "" + EnumChatFormatting.DARK_GRAY + EnumChatFormatting.ITALIC + "none");

                // spirit pet:
                HySkyBlockStats.Profile.Pet spiritPet = member.getPet("SPIRIT");
                if (spiritPet != null) {
                    playerTooltip.append(EnumChatFormatting.GRAY).append(" (").append(spiritPet.toFancyString()).append(EnumChatFormatting.GRAY).append(")");
                }

                HySkyBlockStats.Profile.Dungeons dungeons = member.getDungeons();
                boolean hasNotPlayedDungeons = dungeons == null || !dungeons.hasPlayed();
                if (hasNotPlayedDungeons) {
                    playerEntry.append(EnumChatFormatting.ITALIC).append("hasn't played Dungeons yet");
                    continue;
                }
                DataHelper.DungeonClass selectedClass = dungeons.getSelectedClass();

                String noClassSelected = EnumChatFormatting.ITALIC + "no class selected";
                String classAndDungeonTypeInfo = noClassSelected;
                if (selectedClass != null) {
                    int selectedClassLevel = dungeons.getSelectedClassLevel();
                    classAndDungeonTypeInfo = selectedClass.getName() + " " + (MooConfig.useRomanNumerals() ? Utils.convertArabicToRoman(selectedClassLevel) : selectedClassLevel);
                }
                classAndDungeonTypeInfo += dungeons.getDungeonTypesLevels();
                playerEntry.append(classAndDungeonTypeInfo);
                // insert class + dungeon type data into str:
                int start = playerTooltip.indexOf(noClassSelected);
                playerTooltip.replace(start, start + noClassSelected.length(), classAndDungeonTypeInfo);

                // highest floor completions:
                playerTooltip.append(dungeons.getHighestFloorCompletions(3, false));

                // found dungeon secrets:
                playerTooltip.append("\n").append(EnumChatFormatting.GRAY).append("Found secrets: ").append(EnumChatFormatting.GOLD).append(partyMemberFoundDungeonSecrets.getOrDefault(partyMemberName, 0));
            } else {
                playerEntry.setLength(0);
                playerEntry.append(errorNamePrefix).append("API error").append(sbStats != null && sbStats.getCause() != null ? ": " + sbStats.getCause() : "");
            }
        }
        MooChatComponent dungeonPartyEntry = new MooChatComponent(playerEntry.toString())
                .setSuggestCommand((partyMemberName.equals(thePlayerName) ? "/boop " : "/p kick ") + partyMemberName, false);
        if (playerTooltip.length() > 0) {
            dungeonPartyEntry.setHover(new MooChatComponent(playerTooltip.toString()));
        }
        dungeonsParty.appendFreshSibling(dungeonPartyEntry);
        main.getChatHelper().sendMessage(dungeonsParty);
        shutdown();
    }

    public boolean isStillRunning() {
        return nextStep != Step.STOP;
    }

    public void shutdown() {
        nextStep = Step.STOP;
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    private enum Step {
        START, LEADER, MODERATORS_OR_MEMBERS, MEMBERS_OR_END, // party detection
        API_REQUESTS, AWAITING_API_RESPONSE, SEND_PARTY_STATS, // dungeon stats
        STOP
    }
}
