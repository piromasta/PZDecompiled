package zombie.iso.fboRenderChunk;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.IModelCamera;

public class FBORenderChunkCamera implements IModelCamera {
   FBORenderChunk renderChunk;
   float m_originX;
   float m_originY;
   float m_originZ;
   float m_x;
   float m_y;
   float m_z;
   float m_useAngle;
   int m_width;
   int m_height;
   int m_bottom;

   public FBORenderChunkCamera() {
   }

   public void set(FBORenderChunk var1, float var2, float var3, float var4, float var5) {
      this.renderChunk = var1;
      this.m_originX = (float)(var1.chunk.wx * 8) + 4.0F;
      this.m_originY = (float)(var1.chunk.wy * 8) + 4.0F;
      this.m_originZ = (float)var1.getMinLevel();
      int var6 = FBORenderChunk.FLOOR_HEIGHT * 8;
      int var7 = var1.getTopLevel() - var1.getMinLevel() + 1;
      this.m_bottom = var6 + var7 * FBORenderChunk.PIXELS_PER_LEVEL;
      this.m_bottom += FBORenderLevels.extraHeightForJumboTrees(var1.getMinLevel(), var1.getTopLevel());
      this.m_x = var2;
      this.m_y = var3;
      this.m_z = var4;
      this.m_useAngle = var5;
      this.m_width = var1.w;
      this.m_height = var1.h;
   }

   public void DoPushIsoStuff(float var1, float var2, float var3, float var4, boolean var5) {
      float var6 = this.m_originX;
      float var7 = this.m_originY;
      float var8 = this.m_originZ;
      double var9 = (double)var6;
      double var11 = (double)var7;
      double var13 = (double)var8;
      double var15 = (double)((float)this.m_width / 1920.0F);
      Matrix4f var17 = Core.getInstance().projectionMatrixStack.alloc();
      int var18 = FBORenderChunk.FLOOR_HEIGHT * 8;
      float var19 = (float)this.m_bottom - (float)var18 / 2.0F;
      if (this.renderChunk.bHighRes) {
         var19 *= 2.0F;
      }

      float var20 = (float)this.m_height - var19;
      var17.setOrtho(-((float)var15) / 2.0F, (float)var15 / 2.0F, var19 / 1920.0F, -var20 / 1920.0F, -10.0F, 10.0F);
      Core.getInstance().projectionMatrixStack.push(var17);
      Matrix4f var21 = Core.getInstance().modelViewMatrixStack.alloc();
      var21.scaling(Core.scale);
      var21.scale((float)Core.TileScale / 2.0F);
      if (this.renderChunk.bHighRes) {
         var21.scale(2.0F);
      }

      var21.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
      var21.rotate(2.3561945F, 0.0F, 1.0F, 0.0F);
      double var22 = (double)var1 - var9;
      double var24 = (double)var2 - var11;
      var21.translate(-((float)var22), (float)((double)var3 - var13) * 2.44949F, -((float)var24));
      if (var5) {
         var21.scale(-1.0F, 1.0F, 1.0F);
      } else {
         var21.scale(-1.5F, 1.5F, 1.5F);
      }

      var21.rotate(var4 + 3.1415927F, 0.0F, 1.0F, 0.0F);
      if (!var5) {
      }

      Core.getInstance().modelViewMatrixStack.push(var21);
      GL11.glDepthRange(0.0, 1.0);
   }

   public void DoPopIsoStuff() {
      GL11.glDepthRange(0.0, 1.0);
      GL11.glEnable(3008);
      GL11.glDepthFunc(519);
      GL11.glDepthMask(false);
      GLStateRenderThread.AlphaTest.restore();
      GLStateRenderThread.DepthFunc.restore();
      GLStateRenderThread.DepthMask.restore();
      Core.getInstance().projectionMatrixStack.pop();
      Core.getInstance().modelViewMatrixStack.pop();
   }

   public void Begin() {
      this.DoPushIsoStuff(this.m_x, this.m_y, this.m_z, this.m_useAngle, false);
   }

   public void End() {
      this.DoPopIsoStuff();
   }
}
