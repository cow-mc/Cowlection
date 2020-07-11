package eu.olli.cowlection.util;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.awt.image.BufferedImage;

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
        XI(-15426142, -1769477);

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
}
