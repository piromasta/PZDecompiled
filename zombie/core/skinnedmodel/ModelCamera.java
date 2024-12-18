package zombie.core.skinnedmodel;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.opengl.IModelCamera;
import zombie.core.rendering.RenderTarget;

public abstract class ModelCamera implements IModelCamera {
   public static ModelCamera instance = null;
   public float m_useAngle;
   public boolean m_bUseWorldIso;
   public float m_x;
   public float m_y;
   public float m_z;
   public boolean m_bInVehicle;
   public boolean bDepthMask = true;
   private static final Vector3f ImposterScale = new Vector3f();

   public ModelCamera() {
   }

   public void BeginImposter(RenderTarget var1) {
      int var2 = var1.GetWidth();
      int var3 = var1.GetHeight();
      float var4 = 42.75F;
      Matrix4f var5 = Core.getInstance().projectionMatrixStack.alloc();
      float var6 = (float)var2 / (float)var3;
      float var7 = 0.5F;
      float var8 = 0.5F;
      var5.setOrtho(-var7, var7, -var8, var8, -10.0F, 10.0F);
      Core.getInstance().projectionMatrixStack.push(var5);
      float var9 = Core.scale * (float)Core.TileScale / 2.0F * 1.5F;
      Matrix4f var10 = Core.getInstance().modelViewMatrixStack.alloc();
      var10.scaling(Core.scale * (float)Core.TileScale / 2.0F);
      var10.rotateX(0.5235988F);
      var10.rotateY(2.3561945F);
      var10.scale(-1.5F, 1.5F, 1.5F);
      var10.rotateY(this.m_useAngle + 3.1415927F);
      var10.translate(0.0F, -0.58F, 0.0F);
      var10.getScale(ImposterScale);
      var10.scaleLocal(1.0F / ImposterScale.x, 1.0F / ImposterScale.y, 1.0F);
      Core.getInstance().modelViewMatrixStack.push(var10);
      GL11.glDepthRange(-10.0, 10.0);
      GL11.glDepthMask(this.bDepthMask);
   }

   public void EndImposter() {
      GL11.glDepthFunc(519);
      Core.getInstance().projectionMatrixStack.pop();
      Core.getInstance().modelViewMatrixStack.pop();
   }
}
