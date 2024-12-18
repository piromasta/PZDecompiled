package zombie.core.opengl;

import org.joml.Math;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.skinnedmodel.ModelCamera;

public final class CharacterModelCamera extends ModelCamera {
   public static final CharacterModelCamera instance = new CharacterModelCamera();

   public CharacterModelCamera() {
   }

   public void Begin() {
      if (this.m_bUseWorldIso) {
         Core.getInstance().DoPushIsoStuff(this.m_x, this.m_y, this.m_z, this.m_useAngle, this.m_bInVehicle);
         GL11.glDepthMask(this.bDepthMask);
      } else {
         short var1 = 1024;
         short var2 = 1024;
         float var3 = 42.75F;
         float var4 = 0.0F;
         float var5 = -0.45F;
         float var6 = 0.0F;
         Matrix4f var7 = Core.getInstance().projectionMatrixStack.alloc();
         float var8 = (float)var1 / (float)var2;
         boolean var9 = false;
         if (var9) {
            var7.setOrtho(-var3 * var8, var3 * var8, var3, -var3, -100.0F, 100.0F);
         } else {
            var7.setOrtho(-var3 * var8, var3 * var8, -var3, var3, -100.0F, 100.0F);
         }

         float var10 = Math.sqrt(2048.0F);
         var7.scale(-var10, var10, var10);
         Core.getInstance().projectionMatrixStack.push(var7);
         Matrix4f var11 = Core.getInstance().modelViewMatrixStack.alloc();
         var11.translation(var4, var5, var6);
         var11.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
         var11.rotate(this.m_useAngle + 0.7853982F, 0.0F, 1.0F, 0.0F);
         Core.getInstance().modelViewMatrixStack.push(var11);
         GL11.glDepthRange(0.0, 1.0);
         GL11.glDepthMask(this.bDepthMask);
      }
   }

   public void End() {
      if (this.m_bUseWorldIso) {
         Core.getInstance().DoPopIsoStuff();
      } else {
         GL11.glDepthFunc(519);
         Core.getInstance().projectionMatrixStack.pop();
         Core.getInstance().modelViewMatrixStack.pop();
      }
   }
}
