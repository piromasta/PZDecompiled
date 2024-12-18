package zombie.iso;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.GameTime;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.interfaces.ITexture;
import zombie.iso.weather.ClimateManager;

public final class IsoWater {
   public static float DEPTH_ADJUST = 0.001F;
   public Shader Effect;
   private float WaterTime;
   private float WaterWindAngle;
   private float WaterWindIntensity;
   private float WaterRainIntensity;
   private Vector2f WaterParamWindINT;
   private Texture texBottom = Texture.getSharedTexture("media/textures/river_bottom.png");
   private int apiId;
   private static IsoWater instance;
   private static boolean isShaderEnable = false;
   private final RenderData[][] renderData = new RenderData[3][4];
   private final RenderData[][] renderDataShore = new RenderData[3][4];
   static final int BYTES_PER_FLOAT = 4;
   static final int FLOATS_PER_VERTEX = 8;
   static final int BYTES_PER_VERTEX = 32;
   static final int VERTICES_PER_SQUARE = 4;
   private final Vector4f shaderOffset = new Vector4f();

   public static synchronized IsoWater getInstance() {
      if (instance == null) {
         instance = new IsoWater();
      }

      return instance;
   }

   public boolean getShaderEnable() {
      return isShaderEnable;
   }

   public IsoWater() {
      RenderThread.invokeOnRenderContext(() -> {
         if (GL.getCapabilities().OpenGL30) {
            this.apiId = 1;
         }

         if (GL.getCapabilities().GL_ARB_framebuffer_object) {
            this.apiId = 2;
         }

         if (GL.getCapabilities().GL_EXT_framebuffer_object) {
            this.apiId = 3;
         }

      });

      for(int var1 = 0; var1 < this.renderData.length; ++var1) {
         for(int var2 = 0; var2 < 4; ++var2) {
            this.renderData[var1][var2] = new RenderData();
            this.renderDataShore[var1][var2] = new RenderData();
         }
      }

      this.applyWaterQuality();
      this.WaterParamWindINT = new Vector2f(0.0F);
   }

   public void applyWaterQuality() {
      if (PerformanceSettings.WaterQuality == 2) {
         isShaderEnable = false;
      }

      if (PerformanceSettings.WaterQuality == 1) {
         isShaderEnable = true;
         RenderThread.invokeOnRenderContext(() -> {
            ShaderHelper.glUseProgramObjectARB(0);
            this.Effect = new WaterShader("water");
            ShaderHelper.glUseProgramObjectARB(0);
         });
      }

      if (PerformanceSettings.WaterQuality == 0) {
         isShaderEnable = true;
         RenderThread.invokeOnRenderContext(() -> {
            this.Effect = new WaterShader("water_hq");
            this.Effect.Start();
            this.Effect.End();
         });
      }

   }

   public void render(ArrayList<IsoGridSquare> var1) {
      if (this.getShaderEnable()) {
         int var2 = IsoCamera.frameState.playerIndex;
         int var3 = SpriteRenderer.instance.getMainStateIndex();
         RenderData var4 = this.renderData[var3][var2];
         var4.clear();

         for(int var5 = 0; var5 < var1.size(); ++var5) {
            IsoGridSquare var6 = (IsoGridSquare)var1.get(var5);
            if (var6.chunk == null || !var6.chunk.bLightingNeverDone[var2]) {
               IsoWaterGeometry var7 = var6.getWater();
               if (var7 != null && !var7.bShore && var7.hasWater) {
                  var4.addSquare(var7);
               }
            }
         }

         if (var4.numSquares != 0) {
            SpriteRenderer.instance.drawWater(this.Effect, var2, this.apiId, false);
         }
      }
   }

   public void renderShore(ArrayList<IsoGridSquare> var1) {
      if (this.getShaderEnable()) {
         int var2 = IsoCamera.frameState.playerIndex;
         int var3 = SpriteRenderer.instance.getMainStateIndex();
         RenderData var4 = this.renderDataShore[var3][var2];
         var4.clear();

         for(int var5 = 0; var5 < var1.size(); ++var5) {
            IsoGridSquare var6 = (IsoGridSquare)var1.get(var5);
            if (var6.chunk == null || !var6.chunk.bLightingNeverDone[var2]) {
               IsoWaterGeometry var7 = var6.getWater();
               if (var7 != null && var7.bShore) {
                  var4.addSquare(var7);
               }
            }
         }

         if (var4.numSquares > 0) {
            SpriteRenderer.instance.drawWater(this.Effect, var2, this.apiId, true);
         }

      }
   }

   public void waterProjection(Matrix4f var1) {
      int var2 = SpriteRenderer.instance.getRenderingPlayerIndex();
      PlayerCamera var3 = SpriteRenderer.instance.getRenderingPlayerCamera(var2);
      var1.setOrtho(var3.getOffX(), var3.getOffX() + (float)var3.OffscreenWidth, var3.getOffY() + (float)var3.OffscreenHeight, var3.getOffY(), -1.0F, 1.0F);
   }

   public void waterGeometry(boolean var1) {
      long var2 = System.nanoTime();
      int var4 = SpriteRenderer.instance.getRenderStateIndex();
      int var5 = SpriteRenderer.instance.getRenderingPlayerIndex();
      RenderData var6 = var1 ? this.renderDataShore[var4][var5] : this.renderData[var4][var5];
      int var7 = 0;

      int var9;
      for(int var8 = var6.numSquares; var8 > 0; var8 -= var9) {
         var9 = this.renderSome(var7, var8, var1);
         var7 += var9;
      }

      long var11 = System.nanoTime();
      SpriteRenderer.ringBuffer.restoreVBOs = true;
   }

   private int renderSome(int var1, int var2, boolean var3) {
      IsoPuddles.VBOs.next();
      FloatBuffer var4 = IsoPuddles.VBOs.vertices;
      ShortBuffer var5 = IsoPuddles.VBOs.indices;
      GL20.glEnableVertexAttribArray(4);
      GL20.glEnableVertexAttribArray(5);
      GL20.glEnableVertexAttribArray(6);
      GL20.glVertexAttribPointer(2, 1, 5126, true, 32, 0L);
      GL20.glVertexAttribPointer(3, 1, 5126, true, 32, 4L);
      GL20.glVertexAttribPointer(4, 1, 5126, true, 32, 8L);
      GL20.glVertexAttribPointer(5, 1, 5126, true, 32, 12L);
      GL20.glVertexAttribPointer(0, 2, 5126, false, 32, 16L);
      GL20.glVertexAttribPointer(1, 4, 5121, true, 32, 24L);
      GL20.glVertexAttribPointer(6, 1, 5126, true, 32, 28L);
      int var6 = SpriteRenderer.instance.getRenderStateIndex();
      int var7 = SpriteRenderer.instance.getRenderingPlayerIndex();
      RenderData var8 = var3 ? this.renderDataShore[var6][var7] : this.renderData[var6][var7];
      int var9 = Math.min(var2 * 4, IsoPuddles.VBOs.bufferSizeVertices);
      var4.put(var8.data, var1 * 4 * 8, var9 * 8);
      int var10 = 0;
      int var11 = 0;

      for(int var12 = 0; var12 < var9 / 4; ++var12) {
         if (var8.data[var12 * 4 * 8] == var8.data[var12 * 4 * 8 + 16]) {
            var5.put((short)var10);
            var5.put((short)(var10 + 1));
            var5.put((short)(var10 + 2));
            var5.put((short)var10);
            var5.put((short)(var10 + 2));
            var5.put((short)(var10 + 3));
         } else {
            var5.put((short)(var10 + 1));
            var5.put((short)(var10 + 2));
            var5.put((short)(var10 + 3));
            var5.put((short)(var10 + 1));
            var5.put((short)(var10 + 3));
            var5.put((short)(var10 + 0));
         }

         var10 += 4;
         var11 += 6;
      }

      IsoPuddles.VBOs.unmap();
      GL11.glEnable(2929);
      GL11.glDepthFunc(513);
      GL11.glDepthMask(false);
      GL11.glBlendFunc(770, 771);
      byte var16 = 0;
      byte var14 = 0;
      GL12.glDrawRangeElements(4, var16, var16 + var10, var11 - var14, 5123, (long)(var14 * 2));
      GL13.glActiveTexture(33984);
      GL20.glDisableVertexAttribArray(4);
      GL20.glDisableVertexAttribArray(5);
      GL20.glDisableVertexAttribArray(6);
      return var9 / 4;
   }

   public ITexture getTextureBottom() {
      return this.texBottom;
   }

   public float getShaderTime() {
      return this.WaterTime;
   }

   public float getRainIntensity() {
      return this.WaterRainIntensity;
   }

   public void update(ClimateManager var1) {
      this.WaterWindAngle = var1.getCorrectedWindAngleIntensity();
      this.WaterWindIntensity = var1.getWindIntensity() * 5.0F;
      this.WaterRainIntensity = var1.getRainIntensity();
      float var2 = GameTime.getInstance().getMultiplier();
      this.WaterTime += 0.0166F * var2;
      this.WaterParamWindINT.add((float)Math.sin((double)(this.WaterWindAngle * 6.0F)) * this.WaterWindIntensity * 0.05F * (var2 / 1.6F), (float)Math.cos((double)(this.WaterWindAngle * 6.0F)) * this.WaterWindIntensity * 0.15F * (var2 / 1.6F));
   }

   public float getWaterWindX() {
      return this.WaterParamWindINT.x;
   }

   public float getWaterWindY() {
      return this.WaterParamWindINT.y;
   }

   public float getWaterWindSpeed() {
      return this.WaterWindIntensity * 2.0F;
   }

   public Vector4f getShaderOffset() {
      int var1 = SpriteRenderer.instance.getRenderingPlayerIndex();
      PlayerCamera var2 = SpriteRenderer.instance.getRenderingPlayerCamera(var1);
      float var3 = -var2.fixJigglyModelsX * var2.zoom;
      float var4 = -var2.fixJigglyModelsY * var2.zoom;
      return this.shaderOffset.set(var2.getOffX() + var3 - (float)IsoCamera.getOffscreenLeft(var1) * var2.zoom, var2.getOffY() + var4 + (float)IsoCamera.getOffscreenTop(var1) * var2.zoom, (float)var2.OffscreenWidth, (float)var2.OffscreenHeight);
   }

   public void FBOStart() {
      int var1 = IsoCamera.frameState.playerIndex;
   }

   public void FBOEnd() {
      int var1 = IsoCamera.frameState.playerIndex;
   }

   private static final class RenderData {
      int numSquares;
      int capacity = 512;
      float[] data;

      private RenderData() {
      }

      void clear() {
         this.numSquares = 0;
      }

      void addSquare(IsoWaterGeometry var1) {
         int var2 = IsoCamera.frameState.playerIndex;
         byte var3 = 4;
         if (this.data == null) {
            this.data = new float[this.capacity * var3 * 8];
         }

         if (this.numSquares + 1 > this.capacity) {
            this.capacity += 128;
            this.data = Arrays.copyOf(this.data, this.capacity * var3 * 8);
         }

         PlayerCamera var4 = IsoCamera.cameras[var2];
         float var5 = var4.fixJigglyModelsX * var4.zoom;
         float var6 = var4.fixJigglyModelsY * var4.zoom;
         int var7 = this.numSquares * var3 * 8;

         for(int var8 = 0; var8 < 4; ++var8) {
            this.data[var7++] = var1.depth[var8];
            this.data[var7++] = var1.flow[var8];
            this.data[var7++] = var1.speed[var8];
            this.data[var7++] = var1.IsExternal;
            this.data[var7++] = var1.x[var8] + var5;
            this.data[var7++] = var1.y[var8] + var6;
            if (var1.square != null) {
               int var9 = var1.square.getVertLight((4 - var8) % 4, var2);
               if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
                  var9 = -1;
               }

               this.data[var7++] = Float.intBitsToFloat(var9);
               float var10 = 0.0F;
               float var11 = 0.0F;
               if (var8 == 1 || var8 == 2) {
                  var10 = 1.0F;
               }

               if (var8 == 2 || var8 == 3) {
                  var11 = 1.0F;
               }

               var10 += var4.fixJigglyModelsSquareX;
               var11 += var4.fixJigglyModelsSquareY;
               float var12 = IsoWater.DEPTH_ADJUST;
               int var13 = SpriteRenderer.instance.getMainStateIndex();
               if (this == IsoWater.instance.renderDataShore[var13][var2]) {
               }

               this.data[var7++] = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), (float)var1.square.x + var10 - 0.0F, (float)var1.square.y + var11 - 0.0F, (float)var1.square.z).depthStart + var12;
            } else {
               ++var7;
               ++var7;
            }
         }

         ++this.numSquares;
      }
   }
}
