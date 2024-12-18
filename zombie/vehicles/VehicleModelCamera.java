package zombie.vehicles;

import org.joml.Math;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.skinnedmodel.ModelCamera;

public final class VehicleModelCamera extends ModelCamera {
   public static final VehicleModelCamera instance = new VehicleModelCamera();

   public VehicleModelCamera() {
   }

   public void Begin() {
      if (this.m_bUseWorldIso) {
         Core.getInstance().DoPushIsoStuff(this.m_x, this.m_y, this.m_z, this.m_useAngle, true);
         GL11.glDepthMask(this.bDepthMask);
      } else {
         Matrix4f var1 = Core.getInstance().projectionMatrixStack.alloc();
         var1.setOrtho(-192.0F, 192.0F, -192.0F, 192.0F, -1000.0F, 1000.0F);
         float var2 = Math.sqrt(2048.0F);
         var1.scale(-var2, var2, var2);
         Core.getInstance().projectionMatrixStack.push(var1);
         Matrix4f var3 = Core.getInstance().modelViewMatrixStack.alloc();
         var3.identity();
         var3.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
         var3.rotate(0.7853982F, 0.0F, 1.0F, 0.0F);
         Core.getInstance().modelViewMatrixStack.push(var3);
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
