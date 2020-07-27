package de.cowtipper.cowlection.search;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.client.config.HoverChecker;

import java.util.List;

public class GuiTooltip {
    private final HoverChecker hoverChecker;
    private final List<String> tooltip;

    public <T extends Gui> GuiTooltip(T field, List<String> tooltip) {
        if (field instanceof GuiCheckBox) {
            // checkbox
            GuiCheckBox guiCheckBox = (GuiCheckBox) field;
            int top = guiCheckBox.yPosition;
            int bottom = guiCheckBox.yPosition + guiCheckBox.height;
            int left = guiCheckBox.xPosition;
            int right = guiCheckBox.xPosition + guiCheckBox.width;

            this.hoverChecker = new HoverChecker(top, bottom, left, right, 300);
        } else if (field instanceof GuiTextField) {
            // text field
            GuiTextField guiTextField = (GuiTextField) field;
            int top = guiTextField.yPosition;
            int bottom = guiTextField.yPosition + guiTextField.height;
            int left = guiTextField.xPosition;
            int right = guiTextField.xPosition + guiTextField.width;

            this.hoverChecker = new HoverChecker(top, bottom, left, right, 300);
        } else if (field instanceof GuiButton) {
            // button
            this.hoverChecker = new HoverChecker((GuiButton) field, 300);
        } else {
            throw new IllegalArgumentException("Tried to add a tooltip to an illegal field type: " + field.getClass());
        }
        this.tooltip = tooltip;
    }

    public List<String> getText() {
        return tooltip;
    }

    public boolean checkHover(int mouseX, int mouseY) {
        return hoverChecker.checkHover(mouseX, mouseY);
    }
}
