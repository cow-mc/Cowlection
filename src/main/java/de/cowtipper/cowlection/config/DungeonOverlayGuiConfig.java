package de.cowtipper.cowlection.config;

import de.cowtipper.cowlection.Cowlection;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class DungeonOverlayGuiConfig extends GuiScreen {
    private final Cowlection main;
    private GuiCheckBox checkBoxShowOverlay;
    private GuiSlider sliderX;
    private GuiSlider sliderY;
    private GuiSlider sliderGuiScale;
    private GuiButtonExt buttonCancel;
    private GuiButtonExt buttonSave;
    private final boolean wasDungOverlayEnabled;
    private final int previousPositionX;
    private final int previousPositionY;
    private final int previousGuiScale;

    public DungeonOverlayGuiConfig(Cowlection main) {
        this.main = main;
        wasDungOverlayEnabled = MooConfig.dungOverlayEnabled;
        previousPositionX = MooConfig.dungOverlayPositionX;
        previousPositionY = MooConfig.dungOverlayPositionY;
        previousGuiScale = MooConfig.dungOverlayGuiScale;
    }

    @Override
    public void initGui() {
        int maxX = this.width - fontRendererObj.getStringWidth(StringUtils.repeat('#', 15));
        int maxY = this.height - fontRendererObj.FONT_HEIGHT * 5; // 5 = max lines output
        int startX = Math.min(maxX, this.previousPositionX);
        int startY = Math.min(maxY, this.previousPositionY);

        this.buttonList.add(this.checkBoxShowOverlay = new GuiCheckBox(30, this.width / 2 - 50, this.height / 2 - 30, " Show overlay", MooConfig.dungOverlayEnabled));
        this.buttonList.add(sliderX = new GuiSlider(20, this.width / 2 - 150, this.height / 2 - 12, 300, 20, "x = ", "", 0, maxX, startX, false, true));
        this.buttonList.add(sliderY = new GuiSlider(21, this.width / 2 - 150, this.height / 2 + 12, 300, 20, "y = ", "", 0, maxY, startY, false, true));
        this.buttonList.add(sliderGuiScale = new GuiSlider(22, this.width / 2 - 100, this.height / 2 + 37, 200, 20, "GUI scale: ", "%", 50, 200, MooConfig.dungOverlayGuiScale, false, true));
        this.buttonList.add(this.buttonCancel = new GuiButtonExt(31, this.width / 2 - 150, this.height / 2 + 65, 80, 20, EnumChatFormatting.RED + "Cancel"));
        this.buttonList.add(this.buttonSave = new GuiButtonExt(32, this.width / 2 + 70, this.height / 2 + 65, 80, 20, EnumChatFormatting.GREEN + "Save"));
        if (!MooConfig.dungOverlayEnabled) {
            sliderX.enabled = false;
            sliderY.enabled = false;
            sliderGuiScale.enabled = false;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // draw background
        int padding = 20;
        this.drawGradientRect(this.width / 2 - 150 - padding, this.height / 2 - 40 - fontRendererObj.FONT_HEIGHT - padding,
                this.width / 2 + 150 + padding, this.height / 2 + 65 + 20 + fontRendererObj.FONT_HEIGHT + padding,
                -1072689136, -804253680);

        // draw gui elements
        String title = "Dungeon Performance Overlay Settings";
        this.drawCenteredString(this.fontRendererObj, EnumChatFormatting.BOLD + title, this.width / 2, this.height / 2 - 40 - fontRendererObj.FONT_HEIGHT, 0x00ffffff);

        GlStateManager.pushMatrix();
        float scaleFactor = 0.75f;
        GlStateManager.scale(scaleFactor, scaleFactor, 0);
        String hint = "(" + EnumChatFormatting.GOLD + EnumChatFormatting.ITALIC + "Note: " + EnumChatFormatting.RESET + "Destroyed Crypts can only be detected up to ~50 blocks away from the player)";
        this.drawCenteredString(this.fontRendererObj, hint, (int) ((this.width / 2) * (1 / scaleFactor)), (int) ((this.height / 2 + 95) * (1 / scaleFactor)), 0x00cccccc);
        GlStateManager.popMatrix();

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (checkBoxShowOverlay.enabled) {
            MooConfig.dungOverlayPositionX = sliderX.getValueInt();
            MooConfig.dungOverlayPositionY = sliderY.getValueInt();
            MooConfig.dungOverlayGuiScale = sliderGuiScale.getValueInt();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            resetDungeonOverlayPosition();
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == buttonCancel) {
            resetDungeonOverlayPosition();
            closeGui();
        } else if (button == buttonSave) {
            if (wasDungOverlayEnabled != checkBoxShowOverlay.isChecked() || previousPositionX != sliderX.getValueInt() || previousPositionY != sliderY.getValueInt() || previousGuiScale != sliderGuiScale.getValueInt()) {
                main.getConfig().syncFromFields();
                if (wasDungOverlayEnabled != checkBoxShowOverlay.isChecked()) {
                    main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "The dungeon performance overlay is now " + (checkBoxShowOverlay.isChecked() ? EnumChatFormatting.DARK_GREEN + "enabled" : EnumChatFormatting.DARK_RED + "disabled"));
                } else {
                    main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Saved new size and position of the dungeon performance overlay!");
                }
            }
            closeGui();
        } else if (button == checkBoxShowOverlay) {
            sliderX.enabled = checkBoxShowOverlay.isChecked();
            sliderY.enabled = checkBoxShowOverlay.isChecked();
            sliderGuiScale.enabled = checkBoxShowOverlay.isChecked();
            MooConfig.dungOverlayEnabled = checkBoxShowOverlay.isChecked();
        }
    }

    private void resetDungeonOverlayPosition() {
        MooConfig.dungOverlayEnabled = wasDungOverlayEnabled;
        MooConfig.dungOverlayPositionX = previousPositionX;
        MooConfig.dungOverlayPositionY = previousPositionY;
        MooConfig.dungOverlayGuiScale = previousGuiScale;
    }

    private void closeGui() {
        this.mc.displayGuiScreen(null);
        if (this.mc.currentScreen == null) {
            this.mc.setIngameFocus();
        }
    }
}
