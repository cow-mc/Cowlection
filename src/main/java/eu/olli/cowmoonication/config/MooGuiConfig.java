package eu.olli.cowmoonication.config;

import eu.olli.cowmoonication.Cowmoonication;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;

public class MooGuiConfig extends GuiConfig {
    public MooGuiConfig(GuiScreen parent) {
        super(parent,
                new ConfigElement(MooConfig.getConfig().getCategory(Configuration.CATEGORY_CLIENT)).getChildElements(),
                Cowmoonication.MODID,
                false,
                false,
                "Configuration for " + Cowmoonication.MODNAME);
        titleLine2 = MooConfig.getConfig().getConfigFile().getAbsolutePath();
    }

    @Override
    public void initGui() {
        super.initGui();
        // optional: add buttons and initialize fields
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        // optional: create animations, draw additional elements, etc.
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        super.actionPerformed(button);
        // optional: process any additional buttons added in initGui
    }
}
