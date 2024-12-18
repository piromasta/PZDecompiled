package zombie.core.particle;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.opengl.VBORenderer;
import zombie.core.textures.Texture;

public class MuzzleFlash {
   private static Texture muzzleFlashStar;
   private static Texture muzzleFlashSide;

   public MuzzleFlash() {
   }

   public static void init() {
      muzzleFlashStar = Texture.getSharedTexture("media/textures/muzzle-flash-star.png");
      muzzleFlashSide = Texture.getSharedTexture("media/textures/muzzle-flash-side.png");
   }

   public static void render(Matrix4f var0) {
      if (muzzleFlashStar != null && muzzleFlashStar.isReady()) {
         if (muzzleFlashSide != null && muzzleFlashSide.isReady()) {
            VBORenderer var1 = VBORenderer.getInstance();
            var1.cmdPushAndMultMatrix(5888, var0);
            GL11.glDisable(2884);
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            var1.startRun(var1.FORMAT_PositionColorUV);
            var1.setMode(7);
            var1.setTextureID(muzzleFlashStar.getTextureId());
            float var2 = 0.15F;
            var1.addQuad(-var2 / 2.0F, var2 / 2.0F, 0.0F, 1.0F, var2 / 2.0F, -var2 / 2.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            var1.endRun();
            var1.startRun(var1.FORMAT_PositionColorUV);
            var1.setMode(7);
            var1.setTextureID(muzzleFlashSide.getTextureId());
            var2 = 0.05F;
            var1.addQuad(0.0F, var2 / 2.0F, 0.0F, 0.0F, 1.0F, 0.0F, var2 / 2.0F, var2 * 2.0F, 1.0F, 1.0F, 0.0F, -var2 / 2.0F, var2 * 2.0F, 1.0F, 0.0F, 0.0F, -var2 / 2.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            var1.endRun();
            var1.cmdPopMatrix(5888);
            var1.flush();
            GL11.glEnable(2884);
         }
      }
   }
}
