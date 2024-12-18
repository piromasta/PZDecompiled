package zombie.iso.weather.fog;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.textures.TextureDraw;

public final class ImprovedFogDrawer extends TextureDraw.GenericDrawer {
   float screenInfo1;
   float screenInfo2;
   float screenInfo3;
   float screenInfo4;
   float textureInfo1;
   float textureInfo2;
   float textureInfo3;
   float textureInfo4;
   float worldOffset1;
   float worldOffset2;
   float worldOffset3;
   float worldOffset4;
   float scalingInfo1;
   float scalingInfo2;
   float scalingInfo3;
   float scalingInfo4;
   float colorInfo1;
   float colorInfo2;
   float colorInfo3;
   float colorInfo4;
   float paramInfo1;
   float paramInfo2;
   float paramInfo3;
   float paramInfo4;
   float cameraInfo1;
   float cameraInfo2;
   float cameraInfo3;
   float cameraInfo4;
   float alpha;
   int RECTANGLE_BYTES = 60;
   ByteBuffer rectangleBuffer;

   public ImprovedFogDrawer() {
      this.rectangleBuffer = ByteBuffer.allocate(this.RECTANGLE_BYTES * 128);
   }

   public void render() {
      FogShader var1 = FogShader.instance;
      if (var1.getProgram() == null) {
         var1.initShader();
         if (!var1.getProgram().isCompiled()) {
            return;
         }
      }

      if (this.rectangleBuffer.position() == 0) {
         int var2 = var1.getProgram().getShaderID();
         ShaderHelper.glUseProgramObjectARB(var2);
         var1.setTextureInfo3(this.textureInfo1, this.textureInfo2, this.textureInfo3, this.textureInfo4);
         var1.setWorldOffset3(this.worldOffset1, this.worldOffset2, this.worldOffset3, this.worldOffset4);
         var1.setColorInfo3(this.colorInfo1, this.colorInfo2, this.colorInfo3, this.colorInfo4);
         var1.setParamInfo3(this.paramInfo1, this.paramInfo2, this.paramInfo3, this.paramInfo4);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         if (PerformanceSettings.FBORenderChunk) {
            GL11.glDepthMask(false);
            GL11.glDepthFunc(513);
         }

         int var3 = -1;
         VBORenderer var4 = VBORenderer.getInstance();
         int var5 = this.rectangleBuffer.limit() / this.RECTANGLE_BYTES;

         for(int var6 = 0; var6 < var5; ++var6) {
            float var7 = this.rectangleBuffer.getFloat();
            float var8 = this.rectangleBuffer.getFloat();
            float var9 = this.rectangleBuffer.getFloat();
            float var10 = this.rectangleBuffer.getFloat();
            float var11 = this.rectangleBuffer.getFloat();
            float var12 = this.rectangleBuffer.getFloat();
            float var13 = this.rectangleBuffer.getFloat();
            float var14 = this.rectangleBuffer.getFloat();
            float var15 = this.rectangleBuffer.getFloat();
            float var16 = this.rectangleBuffer.getFloat();
            float var17 = this.rectangleBuffer.getFloat();
            float var18 = this.rectangleBuffer.getFloat();
            float var19 = this.rectangleBuffer.getFloat();
            float var20 = this.rectangleBuffer.getFloat();
            int var21 = this.rectangleBuffer.getInt();
            var4.startRun(VBORenderer.getInstance().FORMAT_PositionColorUVDepth);
            var4.setMode(7);
            var4.setTextureID(ImprovedFog.getNoiseTexture().getTextureId());
            var4.setDepthTest(PerformanceSettings.FBORenderChunk);
            var4.setShaderProgram(var1.getProgram());
            if (var21 != var3) {
               var1.setScalingInfo2(this.scalingInfo1, this.scalingInfo2, (float)var21, this.scalingInfo4);
               var3 = var21;
            }

            var1.setScreenInfo2(this.screenInfo1, this.screenInfo2, this.screenInfo3, var20);
            var1.setRectangleInfo2((float)((int)var7), (float)((int)var8), (float)((int)(var9 - var7)), (float)((int)(var10 - var8)));
            var1.setCameraInfo2(this.cameraInfo1, this.cameraInfo2, this.cameraInfo3, var19);
            float var22 = 0.0F;
            var4.addQuadDepth(var7, var8, var22, var11, var12, var15, var9, var8, var22, var13, var12, var16, var9, var10, var22, var13, var14, var17, var7, var10, var22, var11, var14, var18, 1.0F, 1.0F, 1.0F, this.alpha);
            var4.endRun();
         }

         var4.flush();
         ShaderHelper.glUseProgramObjectARB(0);
         GLStateRenderThread.restore();
      }
   }

   public void startFrame() {
      this.rectangleBuffer.clear();
   }

   public void endFrame() {
      if (this.rectangleBuffer.position() != 0) {
         this.rectangleBuffer.flip();
         SpriteRenderer.instance.drawGeneric(this);
      }
   }

   void addRectangle(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, int var13) {
      if (this.rectangleBuffer.capacity() < this.rectangleBuffer.position() + this.RECTANGLE_BYTES) {
         ByteBuffer var14 = ByteBuffer.allocate(this.rectangleBuffer.capacity() + this.RECTANGLE_BYTES * 128);
         this.rectangleBuffer.flip();
         var14.put(this.rectangleBuffer);
         this.rectangleBuffer = var14;
      }

      this.rectangleBuffer.putFloat(var1);
      this.rectangleBuffer.putFloat(var2);
      this.rectangleBuffer.putFloat(var3);
      this.rectangleBuffer.putFloat(var4);
      this.rectangleBuffer.putFloat(var5);
      this.rectangleBuffer.putFloat(var6);
      this.rectangleBuffer.putFloat(var7);
      this.rectangleBuffer.putFloat(var8);
      this.rectangleBuffer.putFloat(var11);
      this.rectangleBuffer.putFloat(var11);
      this.rectangleBuffer.putFloat(var10);
      this.rectangleBuffer.putFloat(var10);
      this.rectangleBuffer.putFloat(var9);
      this.rectangleBuffer.putFloat(var12);
      this.rectangleBuffer.putInt(var13);
   }
}
