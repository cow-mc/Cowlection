package eu.olli.cowmoonication.config;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.search.GuiTooltip;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MooGuiConfig extends GuiConfig {
    private GuiTooltip defaultStartDateTooltip;
    private GuiTextField textFieldDefaultStartDate;
    private String defaultStartDateTooltipText;

    public MooGuiConfig(GuiScreen parent) {
        super(parent,
                getConfigElements(),
                Cowmoonication.MODID,
                false,
                false,
                EnumChatFormatting.BOLD + "Configuration for " + Cowmoonication.MODNAME);
        titleLine2 = EnumChatFormatting.GRAY + MooConfig.getConfig().getConfigFile().getAbsolutePath();
    }

    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<>(new ConfigElement(MooConfig.getConfig().getCategory(Configuration.CATEGORY_CLIENT)).getChildElements());
        list.addAll(new ConfigElement(MooConfig.getConfig().getCategory(MooConfig.CATEGORY_LOGS_SEARCH)).getChildElements());
        return list;
    }

    @Override
    public void initGui() {
        super.initGui();
        // optional: add buttons and initialize fields
        for (GuiConfigEntries.IConfigEntry configEntry : entryList.listEntries) {
            if ("defaultStartDate".equals(configEntry.getName()) && configEntry instanceof GuiConfigEntries.StringEntry) {
                GuiConfigEntries.StringEntry entry = (GuiConfigEntries.StringEntry) configEntry;
                defaultStartDateTooltipText = I18n.format(configEntry.getConfigElement().getLanguageKey() + ".tooltip");
                try {
                    textFieldDefaultStartDate = (GuiTextField) FieldUtils.readField(entry, "textFieldValue", true);
                    defaultStartDateTooltip = null;
                } catch (IllegalAccessException e) {
                    // wasn't able to access textField, abort drawing tooltip
                    return;
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        // optional: create animations, draw additional elements, etc.

        // add tooltip to defaultStartDate textField
        if (textFieldDefaultStartDate != null) {
            if (defaultStartDateTooltip == null) {
                if (textFieldDefaultStartDate.yPosition == 0) {
                    return;
                }
                // create GuiTooltip here instead in initGui because y-position of textField is 0 inside initGui
                defaultStartDateTooltip = new GuiTooltip(textFieldDefaultStartDate, Arrays.asList(defaultStartDateTooltipText.split("\\\\n")));
            } else if (defaultStartDateTooltip.checkHover(mouseX, mouseY)) {
                drawHoveringText(defaultStartDateTooltip.getText(), mouseX, mouseY, fontRendererObj);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        super.actionPerformed(button);
        // optional: process any additional buttons added in initGui
    }
}
