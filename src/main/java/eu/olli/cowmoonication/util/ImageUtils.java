package eu.olli.cowmoonication.util;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import eu.olli.cowmoonication.Cowmoonication;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    public static int getTierFromTexture(String minionSkinId) {
        String textureUrl = "http://textures.minecraft.net/texture/" + minionSkinId;
        MinecraftProfileTexture minionSkinTextureDetails = new MinecraftProfileTexture(textureUrl, null);

        ResourceLocation minionSkinLocation = Minecraft.getMinecraft().getSkinManager().loadSkin(minionSkinTextureDetails, MinecraftProfileTexture.Type.SKIN);

        ThreadDownloadImageData minionSkinTexture = (ThreadDownloadImageData) Minecraft.getMinecraft().getTextureManager().getTexture(minionSkinLocation);
        BufferedImage minionSkinImage = ReflectionHelper.getPrivateValue(ThreadDownloadImageData.class, minionSkinTexture, "bufferedImage", "field_110560_d");

        // extract part of the minion tier badge (center 2x2 pixel)
        BufferedImage minionSkinTierBadge = minionSkinImage.getSubimage(43, 3, 2, 2);

        // reference image for tier badges: each tier is 2x2 pixel
        ResourceLocation tierBadgesLocation = new ResourceLocation(Cowmoonication.MODID, "minion-tier-badges.png");

        try (InputStream tierBadgesStream = Minecraft.getMinecraft().getResourceManager().getResource(tierBadgesLocation).getInputStream()) {
            BufferedImage tierBadges = ImageIO.read(tierBadgesStream);

            final int maxTier = 11;
            for (int tier = 0; tier < maxTier; tier++) {
                BufferedImage tierBadgeRaw = tierBadges.getSubimage(tier * 2, 0, 2, 2);
                if (ImageUtils.areEquals(minionSkinTierBadge, tierBadgeRaw)) {
                    return tier + 1;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return -5;
    }

    /**
     * Compares two images pixel by pixel
     *
     * @param imageA the first image
     * @param imageB the second image
     * @return whether the images are both the same or not
     * @see <a href="https://stackoverflow.com/a/29886786">Source</a>
     */
    private static boolean areEquals(BufferedImage imageA, BufferedImage imageB) {
        // images must be the same size
        if (imageA.getWidth() != imageB.getWidth() || imageA.getHeight() != imageB.getHeight()) {
            return false;
        }

        int width = imageA.getWidth();
        int height = imageB.getHeight();

        // loop over every pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // compare the pixels for equality
                if (imageA.getRGB(x, y) != imageB.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}
