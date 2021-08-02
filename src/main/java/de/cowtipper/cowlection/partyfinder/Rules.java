package de.cowtipper.cowlection.partyfinder;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.util.GsonUtils;
import net.minecraft.util.MathHelper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Rules {
    private final List<Rule> partyFinderRules;
    private final File file;
    private final Cowlection main;

    public Rules(Cowlection main, File file) {
        this.partyFinderRules = new ArrayList<>();
        this.file = file;
        this.main = main;
        if (file.exists()) {
            loadFromFile();
        } else {
            try {
                file.createNewFile();
                addDefaultRules();
                saveToFile();
            } catch (IOException e) {
                main.getLogger().error("Couldn't create dungeons party finder rules" + this.file, e);
            }
        }
    }

    public List<Rule> getRules() {
        return partyFinderRules;
    }

    public int getCount() {
        return partyFinderRules.size();
    }

    /**
     * Add new empty rule
     */
    public void add(int slotIndex) {
        this.partyFinderRules.add(slotIndex, new Rule(true, "", Rule.TriggerType.TEXT, "", DataHelper.PartyType.SUITABLE));
    }

    public Rule remove(int ruleId) {
        Rule removedRule = partyFinderRules.remove(ruleId);
        if (removedRule != null) {
            removedRule.setEnabled(false);
        }
        return removedRule;
    }

    public boolean move(int ruleId, boolean moveDown) {
        int lastIndex = partyFinderRules.size() - 1;
        if (partyFinderRules.size() >= 2 && (moveDown && ruleId != lastIndex || !moveDown && ruleId > 0)) {
            int otherRuleId = MathHelper.clamp_int(ruleId + (moveDown ? 1 : -1), 0, lastIndex);
            if (ruleId != otherRuleId) {
                Collections.swap(partyFinderRules, ruleId, otherRuleId);
                return true;
            }
        }
        return false;
    }

    public void saveToFile() {
        partyFinderRules.stream().filter(rule -> rule.getTriggerText().isEmpty()).forEach(rule -> rule.setEnabled(false));
        try {
            String partyFinderRules = GsonUtils.toJson(this.partyFinderRules);
            FileUtils.writeStringToFile(file, partyFinderRules, StandardCharsets.UTF_8);
        } catch (IOException e) {
            main.getLogger().error("Couldn't save dungeons party finder rules" + this.file, e);
        }
    }

    public void loadFromFile() {
        this.partyFinderRules.clear();
        try {
            String ruleData = FileUtils.readFileToString(this.file, StandardCharsets.UTF_8);
            if (ruleData.length() > 0) {
                this.partyFinderRules.addAll(parseJson(ruleData));
            }
        } catch (IOException e) {
            main.getLogger().error("Couldn't read dungeons party finder rules file " + this.file, e);
        } catch (JsonParseException e) {
            main.getLogger().error("Couldn't parse dungeons party finder rules file " + this.file, e);
            try {
                Files.copy(file.toPath(), file.toPath().resolveSibling("partyfinder-rules_corrupted.json"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ioException) {
                main.getLogger().error("Couldn't copy the corrupted dungeons party finder rules file " + this.file, e);
            }
        }
    }

    public void resetToDefault() {
        partyFinderRules.clear();
        addDefaultRules();
    }

    private void addDefaultRules() {
        partyFinderRules.add(new Rule(false,
                "(free )carr", Rule.TriggerType.SIMPLE_REGEX,
                "§bcarry", DataHelper.PartyType.SUITABLE));
        partyFinderRules.add(new Rule(false,
                "free carr", Rule.TriggerType.TEXT,
                "§acarry", DataHelper.PartyType.SUITABLE));
        partyFinderRules.add(new Rule(true,
                "hyp", Rule.TriggerType.TEXT,
                "§ahyp", DataHelper.PartyType.SUITABLE));
        partyFinderRules.add(new Rule(false,
                "pls,plz,help", Rule.TriggerType.SIMPLE_REGEX,
                "", DataHelper.PartyType.UNIDEAL));
        partyFinderRules.add(new Rule(true,
                "speed", Rule.TriggerType.TEXT,
                "§f✦", DataHelper.PartyType.SUITABLE));
        partyFinderRules.add(new Rule(true,
                "frag", Rule.TriggerType.TEXT,
                "§ffrag", DataHelper.PartyType.SUITABLE));
        partyFinderRules.add(new Rule(true,
                "exp", Rule.TriggerType.TEXT,
                "§fxp", DataHelper.PartyType.SUITABLE));
        partyFinderRules.add(new Rule(true,
                "s+", Rule.TriggerType.TEXT,
                "§fS+", DataHelper.PartyType.SUITABLE));
    }

    private List<Rule> parseJson(String rulesData) {
        Type collectionType = new TypeToken<List<Rule>>() {
        }.getType();
        List<Rule> rules = GsonUtils.fromJson(rulesData, collectionType);
        for (Rule rule : rules) {
            rule.postConstructor();
        }
        return rules;
    }
}
