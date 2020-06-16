package eu.olli.cowmoonication.search;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

class GuiDateField extends GuiTextField {
    GuiDateField(int componentId, FontRenderer fontrendererObj, int x, int y, int width, int height) {
        super(componentId, fontrendererObj, x, y, width, height);
    }

    LocalDate getDate() {
        try {
            return LocalDate.parse(this.getText());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    boolean validateDate() {
        try {
            LocalDate localDate = LocalDate.parse(this.getText());
            if (localDate.isAfter(LocalDate.now()) || localDate.isBefore(LocalDate.ofYearDay(2009, 1))) {
                // searching for things written in the future isn't possible (yet). It is also not possible to perform a search before the existence of mc.
                setTextColor(0xFFFF3333);
                return false;
            }
        } catch (DateTimeParseException e) {
            setTextColor(0xFFFF3333);
            return false;
        }
        setTextColor(0xFFFFFF);
        return true;
    }
}
