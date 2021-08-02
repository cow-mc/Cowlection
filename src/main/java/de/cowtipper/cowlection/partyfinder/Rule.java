package de.cowtipper.cowlection.partyfinder;

import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Rule {
    private final transient List<String> patternException;
    private boolean isEnabled;
    private transient boolean isValid;
    private TriggerType triggerType;
    private String triggerText;
    private transient Pattern triggerRegex;
    private String middleText;
    private DataHelper.PartyType partyType;

    public Rule(boolean isEnabled, String trigger, TriggerType triggerType, String middleText, DataHelper.PartyType partyType) {
        this.patternException = new ArrayList<>();
        this.isEnabled = isEnabled;
        this.triggerText = trigger;
        this.triggerType = triggerType;
        this.middleText = middleText;
        this.partyType = partyType;
        postConstructor();
    }

    /**
     * No-args constructor for GSON
     */
    @SuppressWarnings("unused")
    public Rule() {
        patternException = new ArrayList<>();
    }

    public void postConstructor() {
        setTriggerTextAndRevalidateRule(triggerText);
        if (partyType == null) {
            partyType = DataHelper.PartyType.SUITABLE;
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isTriggered(String notes) {
        return isEnabled && isValid && (
                (triggerType == TriggerType.TEXT && notes.contains(triggerText))
                        || (triggerRegex != null && triggerRegex.matcher(notes).find()));
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    /**
     * @return human readable trigger text
     */
    public String getTriggerText() {
        return this.triggerText;
    }

    public String getTriggerRegex() {
        return triggerRegex != null ? triggerRegex.pattern() : "";
    }

    public void setTriggerTextAndRevalidateRule(String trigger) {
        if (trigger == null) {
            trigger = "";
        }
        if (trigger.isEmpty()) {
            triggerText = "";
            triggerRegex = null;
            isValid = false;
            patternException.clear();
            patternException.add(EnumChatFormatting.RED + "Pattern cannot be empty!");
        } else {
            isValid = true;
            triggerText = trigger.toLowerCase(Locale.ROOT);
        }
        boolean hasCommas = trigger.contains(",");
        boolean hasBrackets = trigger.contains("(") || trigger.contains(")");
        if (triggerType == TriggerType.REGEX) {
            try {
                triggerRegex = Pattern.compile(trigger);
            } catch (PatternSyntaxException e) {
                handleInvalidPattern(e);
            }
        } else if (hasCommas || hasBrackets) {
            if (triggerType == TriggerType.TEXT) {
                this.triggerType = TriggerType.SIMPLE_REGEX;
            }
            try {
                trigger = regexify(triggerText, hasBrackets, hasCommas);
                triggerRegex = Pattern.compile(trigger);
            } catch (PatternSyntaxException e) {
                handleInvalidPattern(e);
            }
        } else {
            triggerType = TriggerType.TEXT;
            triggerRegex = null;
        }
    }

    private String regexify(String trigger, boolean hasBrackets, boolean hasCommas) {
        trigger = trigger.replaceAll("([\\\\+*?\\[\\]{}|.^$])", "\\\\$1");
        if (hasBrackets) {
            Matcher matcher = Pattern.compile("([^ ])?\\(([^)]+)\\)([^ ])?").matcher(trigger);
            if (matcher.find()) {
                if (matcher.group(1) == null) {
                    // negative lookbehind
                    trigger = matcher.replaceFirst("(?<!$2)$3");
                } else {
                    // negative lookahead
                    trigger = matcher.replaceFirst("$1(?!$2)");
                }
                // escape additional brackets
                trigger = trigger.replaceAll("\\(([^?][^)]*)\\)", "\\\\($1\\\\)");
            }
        }
        if (hasCommas) {
            trigger = trigger.replace(",", "|");
        }
        return trigger;
    }

    private void handleInvalidPattern(PatternSyntaxException e) {
        isValid = false;
        triggerRegex = null;
        patternException.clear();
        patternException.add(EnumChatFormatting.RED + "Invalid pattern:");
        String exceptionMessage = e.getLocalizedMessage();
        if (exceptionMessage == null) {
            patternException.add(EnumChatFormatting.ITALIC + "unknown cause");
        } else if (exceptionMessage.contains("\n")) {
            patternException.addAll(Arrays.asList(exceptionMessage.split("\r?\n")));
        }
    }

    /**
     * @return true, if triggerText is a valid value; false if regex is invalid
     */
    public boolean isValid() {
        return isValid;
    }

    public List<String> getPatternException() {
        return patternException;
    }

    public void toggleTriggerType() {
        if (this.triggerType == TriggerType.REGEX) {
            this.triggerType = TriggerType.TEXT;
        } else { // TEXT or SIMPLE_REGEX:
            this.triggerType = TriggerType.REGEX;
        }
    }

    public String getMiddleText() {
        return middleText;
    }

    public void setMiddleText(String middleText) {
        this.middleText = Utils.toMcColorCodes(middleText);
    }

    public DataHelper.PartyType getPartyType() {
        return partyType;
    }

    public void setPartyType(DataHelper.PartyType partyType) {
        this.partyType = partyType;
    }

    enum TriggerType {
        TEXT("Text"), SIMPLE_REGEX("Text+"), REGEX("Regex");

        private final String buttonText;

        TriggerType(String fancyString) {
            this.buttonText = fancyString;
        }

        public String toButtonText() {
            return this.buttonText;
        }
    }
}
