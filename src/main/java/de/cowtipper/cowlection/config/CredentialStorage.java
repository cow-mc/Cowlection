package de.cowtipper.cowlection.config;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.util.ApiUtils;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Key and secret holder in its own file to avoid people leaking their keys accidentally.
 */
public class CredentialStorage {
    public static String moo;
    public static boolean isMooValid;
    private Property propMoo;
    private Property propIsMooValid;
    private final Configuration cfg;

    public CredentialStorage(Configuration configuration) {
        cfg = configuration;
        initConfig();
    }

    private void initConfig() {
        cfg.load();
        propMoo = cfg.get(Configuration.CATEGORY_CLIENT,
                "moo", "", "Don't share this with anybody! Do not edit this entry manually either!", Utils.VALID_UUID_PATTERN)
                .setShowInGui(false);
        propMoo.setLanguageKey(Cowlection.MODID + ".config." + propMoo.getName());

        propIsMooValid = cfg.get(Configuration.CATEGORY_CLIENT,
                "isMooValid", false, "Is the value valid?")
                .setShowInGui(false);
        moo = propMoo.getString();
        isMooValid = propIsMooValid.getBoolean();
        if (cfg.hasChanged()) {
            cfg.save();
        }
    }

    public void setMooIfValid(String moo, boolean commandTriggered) {
        ApiUtils.fetchApiKeyInfo(moo, hyApiKey -> {
            if (hyApiKey != null && hyApiKey.isSuccess()) {
                // api key is valid!
                Cowlection.getInstance().getMoo().setMoo(moo);
                if (commandTriggered) {
                    Cowlection.getInstance().getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Successfully verified API key ✔");
                }
            } else if (commandTriggered) {
                // api key is invalid
                String cause = hyApiKey != null ? hyApiKey.getCause() : null;
                Cowlection.getInstance().getChatHelper().sendMessage(EnumChatFormatting.RED, "Failed to verify API key: " + (cause != null ? cause : "unknown cause :c"));
            }
        });
    }

    private void setMoo(String moo) {
        CredentialStorage.moo = moo;
        propMoo.set(moo);
        setMooValidity(true);
    }

    public void setMooValidity(boolean isMooValid) {
        CredentialStorage.isMooValid = isMooValid;
        propIsMooValid.set(isMooValid);
        cfg.save();
    }

    public Property getPropIsMooValid() {
        return propIsMooValid;
    }
}
