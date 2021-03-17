package de.cowtipper.cowlection.util;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import de.cowtipper.cowlection.Cowlection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ImageUtils {
    public static int getTierFromTexture(String minionSkinId) {
        String textureUrl = "http://textures.minecraft.net/texture/" + minionSkinId;
        MinecraftProfileTexture minionSkinTextureDetails = new MinecraftProfileTexture(textureUrl, null);

        ResourceLocation minionSkinLocation = Minecraft.getMinecraft().getSkinManager().loadSkin(minionSkinTextureDetails, MinecraftProfileTexture.Type.SKIN);

        ThreadDownloadImageData minionSkinTexture = (ThreadDownloadImageData) Minecraft.getMinecraft().getTextureManager().getTexture(minionSkinLocation);
        BufferedImage minionSkinImage = ReflectionHelper.getPrivateValue(ThreadDownloadImageData.class, minionSkinTexture, "bufferedImage", "field_110560_d");

        // extract relevant part of the minion tier badge (center 2x1 pixel)
        BufferedImage minionSkinTierBadge = minionSkinImage.getSubimage(43, 3, 2, 1);

        return MinionTier.getByColors(minionSkinTierBadge.getRGB(0, 0), minionSkinTierBadge.getRGB(1, 0)).getTier();
    }

    private enum MinionTier {
        UNKNOWN(-1, -1),
        I(0, 0),
        II(-2949295, -10566655),
        III(-1245259, -10566655),
        IV(-8922850, -983608),
        V(-8110849, -11790679),
        VI(-4681729, -11790679),
        VII(-9486653, -3033345),
        VIII(-907953, -7208930),
        IX(-31330, -7208930),
        X(-5046235, -20031),
        XI(-15426142, -1769477),
        XII(-1769477, -5767181);

        private final int color1;
        private final int color2;

        MinionTier(int color1, int color2) {
            this.color1 = color1;
            this.color2 = color2;
        }

        private static MinionTier getByColors(int color1, int color2) {
            MinionTier[] tiers = values();
            for (int i = 1; i < tiers.length; i++) {
                MinionTier minionTier = tiers[i];
                if (minionTier.color1 == color1 && minionTier.color2 == color2) {
                    return minionTier;
                }
            }
            return UNKNOWN;
        }

        private int getTier() {
            return ordinal();
        }
    }

    private static final Color[] MAP_COLORS;

    static {
        // base colors: https://minecraft.gamepedia.com/Map_item_format?oldid=778280#Base_colors
        MAP_COLORS = new Color[36];
        MAP_COLORS[0] = new Color(0, 0, 0, 0);
        MAP_COLORS[1] = new Color(125, 176, 55, 255);
        MAP_COLORS[2] = new Color(244, 230, 161, 255);
        MAP_COLORS[3] = new Color(197, 197, 197, 255);
        MAP_COLORS[4] = new Color(252, 0, 0, 255);
        MAP_COLORS[5] = new Color(158, 158, 252, 255);
        MAP_COLORS[6] = new Color(165, 165, 165, 255);
        MAP_COLORS[7] = new Color(0, 123, 0, 255);
        MAP_COLORS[8] = new Color(252, 252, 252, 255);
        MAP_COLORS[9] = new Color(162, 166, 182, 255);
        MAP_COLORS[10] = new Color(149, 108, 76, 255);
        MAP_COLORS[11] = new Color(111, 111, 111, 255);
        MAP_COLORS[12] = new Color(63, 63, 252, 255);
        MAP_COLORS[13] = new Color(141, 118, 71, 255);
        MAP_COLORS[14] = new Color(252, 249, 242, 255);
        MAP_COLORS[15] = new Color(213, 125, 50, 255);
        MAP_COLORS[16] = new Color(176, 75, 213, 255);
        MAP_COLORS[17] = new Color(101, 151, 213, 255);
        MAP_COLORS[18] = new Color(226, 226, 50, 255);
        MAP_COLORS[19] = new Color(125, 202, 25, 255);
        MAP_COLORS[20] = new Color(239, 125, 163, 255);
        MAP_COLORS[21] = new Color(75, 75, 75, 255);
        MAP_COLORS[22] = new Color(151, 151, 151, 255);
        MAP_COLORS[23] = new Color(75, 125, 151, 255);
        MAP_COLORS[24] = new Color(125, 62, 176, 255);
        MAP_COLORS[25] = new Color(50, 75, 176, 255);
        MAP_COLORS[26] = new Color(101, 75, 50, 255);
        MAP_COLORS[27] = new Color(101, 125, 50, 255);
        MAP_COLORS[28] = new Color(151, 50, 50, 255);
        MAP_COLORS[29] = new Color(25, 25, 25, 255);
        MAP_COLORS[30] = new Color(247, 235, 76, 255);
        MAP_COLORS[31] = new Color(91, 216, 210, 255);
        MAP_COLORS[32] = new Color(73, 129, 252, 255);
        MAP_COLORS[33] = new Color(0, 214, 57, 255);
        MAP_COLORS[34] = new Color(127, 85, 48, 255);
        MAP_COLORS[35] = new Color(111, 2, 0, 255);
    }

    public static File saveMapToFile(MapData mapData) {
        int size = 128;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        int x = 0;
        int y = 0;
        for (int i = 0; i < mapData.colors.length; i++) {
            Color pixelColor = colorIdToColor(mapData.colors[i]);
            image.setRGB(x, y, pixelColor.getRGB());
            ++x;
            if (x >= size) {
                // new line
                ++y;
                x = 0;
            }
        }
        try {
            File cowlectionImagePath = new File(Minecraft.getMinecraft().mcDataDir, "cowlection_images");
            if (!cowlectionImagePath.exists() && !cowlectionImagePath.mkdirs()) {
                // dir didn't exist and couldn't be created
                return null;
            }
            File imageFile = getTimestampedPngFileForDirectory(cowlectionImagePath, "map");
            ImageIO.write(image, "png", imageFile);
            return imageFile.getCanonicalFile();
        } catch (IOException e) {
            // couldn't save map image
            e.printStackTrace();
            return null;
        }
    }

    private static Color colorIdToColor(byte rawId) {
        int id = rawId & 255;
        int baseId = id / 4;
        int shadeId = id & 3;

        if (baseId >= MAP_COLORS.length) {
            Cowlection.getInstance().getLogger().warn("Unknown base color id " + baseId + " (id=" + id + ")");
            return new Color(0xf700d5);
        }

        Color c = MAP_COLORS[baseId];
        int shadeMul;
        switch (shadeId) {
            case 0:
                shadeMul = 180;
                break;
            case 1:
                shadeMul = 220;
                break;
            case 2:
                shadeMul = 255;
                break;
            case 3:
                shadeMul = 135;
                break;
            default:
                shadeMul = 180;
                Cowlection.getInstance().getLogger().warn("Unknown shade id " + shadeId + " (raw: " + id + ")");
                c = new Color(0xf700d5);
        }
        return new Color((shadeMul * c.getRed()) / 255, (shadeMul * c.getGreen()) / 255, (shadeMul * c.getBlue()) / 255, c.getAlpha());
    }

    /**
     * Based on ScreenShotHelper#getTimestampedPNGFileForDirectory
     */
    private static File getTimestampedPngFileForDirectory(File directory, String prefix) {
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss"));
        int i = 1;

        while (true) {
            File timestampedFile = new File(directory, prefix + "_" + currentDateTime + (i == 1 ? "" : "_" + i) + ".png");
            if (!timestampedFile.exists()) {
                return timestampedFile;
            }
            ++i;
        }
    }
}
