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
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjglx.BufferUtils;
import zombie.GameTime;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.SharedVertexBufferObjects;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.interfaces.ITexture;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.tileDepth.TileSeamManager;

public final class IsoPuddles {
   public Shader Effect;
   private float PuddlesWindAngle;
   private float PuddlesWindIntensity;
   private float PuddlesTime;
   private final Vector2f PuddlesParamWindINT;
   public static boolean leakingPuddlesInTheRoom = false;
   private Texture texHM;
   private int apiId;
   private static IsoPuddles instance;
   private static boolean isShaderEnable = false;
   static final int BYTES_PER_FLOAT = 4;
   static final int FLOATS_PER_VERTEX = 8;
   static final int BYTES_PER_VERTEX = 32;
   static final int VERTICES_PER_SQUARE = 4;
   public static final SharedVertexBufferObjects VBOs = new SharedVertexBufferObjects(32);
   private final RenderData[][] renderData = new RenderData[3][4];
   private final Vector4f shaderOffset = new Vector4f();
   private final Vector4f shaderOffsetMain = new Vector4f();
   private FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);
   public static final int BOOL_MAX = 0;
   public static final int FLOAT_RAIN = 0;
   public static final int FLOAT_WETGROUND = 1;
   public static final int FLOAT_MUDDYPUDDLES = 2;
   public static final int FLOAT_PUDDLESSIZE = 3;
   public static final int FLOAT_RAININTENSITY = 4;
   public static final int FLOAT_MAX = 5;
   private PuddlesFloat rain;
   private PuddlesFloat wetGround;
   private PuddlesFloat muddyPuddles;
   private PuddlesFloat puddlesSize;
   private PuddlesFloat rainIntensity;
   private final PuddlesFloat[] climateFloats = new PuddlesFloat[5];
   private final ObjectPool<RenderToChunkTexture> renderToChunkTexturePool = new ObjectPool(RenderToChunkTexture::new);

   public static synchronized IsoPuddles getInstance() {
      if (instance == null) {
         instance = new IsoPuddles();
      }

      return instance;
   }

   public boolean getShaderEnable() {
      return isShaderEnable;
   }

   public IsoPuddles() {
      if (GameServer.bServer) {
         Core.getInstance().setPerfPuddles(3);
         this.applyPuddlesQuality();
         this.PuddlesParamWindINT = new Vector2f(0.0F);
         this.setup();
      } else {
         this.texHM = Texture.getSharedTexture("media/textures/puddles_hm.png");
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
         this.applyPuddlesQuality();
         this.PuddlesParamWindINT = new Vector2f(0.0F);

         for(int var1 = 0; var1 < this.renderData.length; ++var1) {
            for(int var2 = 0; var2 < 4; ++var2) {
               this.renderData[var1][var2] = new RenderData();
            }
         }

         this.setup();
      }
   }

   public void applyPuddlesQuality() {
      leakingPuddlesInTheRoom = Core.getInstance().getPerfPuddles() == 0;
      if (Core.getInstance().getPerfPuddles() == 3) {
         isShaderEnable = false;
      } else {
         isShaderEnable = true;
         if (PerformanceSettings.PuddlesQuality == 2) {
            RenderThread.invokeOnRenderContext(() -> {
               this.Effect = new PuddlesShader("puddles_lq");
               this.Effect.Start();
               this.Effect.End();
            });
         }

         if (PerformanceSettings.PuddlesQuality == 1) {
            RenderThread.invokeOnRenderContext(() -> {
               this.Effect = new PuddlesShader("puddles_mq");
               this.Effect.Start();
               this.Effect.End();
            });
         }

         if (PerformanceSettings.PuddlesQuality == 0) {
            RenderThread.invokeOnRenderContext(() -> {
               this.Effect = new PuddlesShader("puddles_hq");
               this.Effect.Start();
               this.Effect.End();
            });
         }
      }

   }

   public Vector4f getShaderOffset() {
      int var1 = SpriteRenderer.instance.getRenderingPlayerIndex();
      PlayerCamera var2 = SpriteRenderer.instance.getRenderingPlayerCamera(var1);
      float var3 = -var2.fixJigglyModelsX * var2.zoom;
      float var4 = -var2.fixJigglyModelsY * var2.zoom;
      return this.shaderOffset.set(var2.getOffX() + var3, var2.getOffY() + var4, (float)var2.OffscreenWidth, (float)var2.OffscreenHeight);
   }

   public Vector4f getShaderOffsetMain() {
      int var1 = IsoCamera.frameState.playerIndex;
      PlayerCamera var2 = IsoCamera.cameras[var1];
      float var3 = -var2.fixJigglyModelsX * var2.zoom;
      float var4 = -var2.fixJigglyModelsY * var2.zoom;
      return this.shaderOffsetMain.set(var2.getOffX() + var3, var2.getOffY() + var4, (float)IsoCamera.getOffscreenWidth(var1), (float)IsoCamera.getOffscreenHeight(var1));
   }

   public boolean shouldRenderPuddles() {
      if (!DebugOptions.instance.Weather.WaterPuddles.getValue()) {
         return false;
      } else if (!this.getShaderEnable()) {
         return false;
      } else if (!Core.getInstance().getUseShaders()) {
         return false;
      } else if (Core.getInstance().getPerfPuddles() == 3) {
         return false;
      } else {
         return (double)this.wetGround.getFinalValue() != 0.0 || (double)this.puddlesSize.getFinalValue() != 0.0;
      }
   }

   public void render(ArrayList<IsoGridSquare> var1, int var2) {
      if (DebugOptions.instance.Weather.WaterPuddles.getValue()) {
         int var3 = SpriteRenderer.instance.getMainStateIndex();
         int var4 = IsoCamera.frameState.playerIndex;
         RenderData var5 = this.renderData[var3][var4];
         if (!var1.isEmpty()) {
            if (this.getShaderEnable()) {
               if (Core.getInstance().getUseShaders()) {
                  if (Core.getInstance().getPerfPuddles() != 3) {
                     if (var2 <= 0 || Core.getInstance().getPerfPuddles() <= 0) {
                        if ((double)this.wetGround.getFinalValue() != 0.0 || (double)this.puddlesSize.getFinalValue() != 0.0) {
                           int var6 = var5.numSquares;

                           int var7;
                           for(var7 = 0; var7 < var1.size(); ++var7) {
                              IsoPuddlesGeometry var8 = ((IsoGridSquare)var1.get(var7)).getPuddles();
                              if (var8 != null && var8.shouldRender()) {
                                 var8.updateLighting(var4);
                                 var5.addSquare(var2, var8, (TileSeamManager.Tiles)null);
                              }
                           }

                           var7 = var5.numSquares - var6;
                           if (var7 > 0) {
                              SpriteRenderer.instance.drawPuddles(var4, var2, var6, var7);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void puddlesProjection(Matrix4f var1) {
      int var2 = SpriteRenderer.instance.getRenderingPlayerIndex();
      PlayerCamera var3 = SpriteRenderer.instance.getRenderingPlayerCamera(var2);
      var1.setOrtho(var3.getOffX(), var3.getOffX() + (float)var3.OffscreenWidth, var3.getOffY() + (float)var3.OffscreenHeight, var3.getOffY(), -1.0F, 1.0F);
   }

   public void puddlesGeometry(int var1, int var2) {
      while(var2 > 0) {
         int var3 = this.renderSome(var1, var2, false);
         var1 += var3;
         var2 -= var3;
      }

      SpriteRenderer.ringBuffer.restoreVBOs = true;
   }

   private int renderSome(int var1, int var2, boolean var3) {
      VBOs.next();
      FloatBuffer var4 = VBOs.vertices;
      ShortBuffer var5 = VBOs.indices;
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
      RenderData var8 = this.renderData[var6][var7];
      int var9 = Math.min(var2 * 4, VBOs.bufferSizeVertices);
      var4.put(var8.data, var1 * 4 * 8, var9 * 8);
      int var10 = 0;
      int var11 = 0;

      for(int var12 = 0; var12 < var9 / 4; ++var12) {
         var5.put((short)var10);
         var5.put((short)(var10 + 1));
         var5.put((short)(var10 + 2));
         var5.put((short)var10);
         var5.put((short)(var10 + 2));
         var5.put((short)(var10 + 3));
         var10 += 4;
         var11 += 6;
      }

      VBOs.unmap();
      if (var3) {
         GL11.glDepthMask(false);
      } else {
         GL11.glDepthMask(false);
         GL11.glBlendFunc(770, 771);
      }

      GL11.glEnable(2929);
      GL11.glDepthFunc(515);
      byte var16 = 0;
      byte var14 = 0;
      GL12.glDrawRangeElements(4, var16, var16 + var10, var11 - var14, 5123, (long)(var14 * 2));
      GL20.glDisableVertexAttribArray(4);
      GL20.glDisableVertexAttribArray(5);
      GL20.glDisableVertexAttribArray(6);
      return var9 / 4;
   }

   public void update(ClimateManager var1) {
      this.PuddlesWindAngle = var1.getCorrectedWindAngleIntensity();
      this.PuddlesWindIntensity = var1.getWindIntensity();
      this.rain.setFinalValue(var1.getRainIntensity());
      float var2 = GameTime.getInstance().getThirtyFPSMultiplier();
      float var3 = 2.0E-5F * var2 * var1.getTemperature();
      float var4 = 2.0E-5F * var2;
      float var5 = 2.0E-4F * var2;
      float var6 = this.rain.getFinalValue();
      var6 = var6 * var6 * 0.05F * var2;
      this.rainIntensity.setFinalValue(this.rain.getFinalValue() * 2.0F);
      this.wetGround.addFinalValue(var6);
      this.muddyPuddles.addFinalValue(var6 * 2.0F);
      this.puddlesSize.addFinalValueForMax(var6 * 0.01F, 0.7F);
      if ((double)var6 == 0.0) {
         this.wetGround.addFinalValue(-var3);
         this.muddyPuddles.addFinalValue(-var5);
      }

      if ((double)this.wetGround.getFinalValue() == 0.0) {
         this.puddlesSize.addFinalValue(-var4);
      }

      this.PuddlesTime += 0.0166F * GameTime.getInstance().getMultiplier();
      this.PuddlesParamWindINT.add((float)Math.sin((double)(this.PuddlesWindAngle * 6.0F)) * this.PuddlesWindIntensity * 0.05F, (float)Math.cos((double)(this.PuddlesWindAngle * 6.0F)) * this.PuddlesWindIntensity * 0.05F);
   }

   public float getShaderTime() {
      return this.PuddlesTime;
   }

   public float getPuddlesSize() {
      return this.puddlesSize.getFinalValue();
   }

   public ITexture getHMTexture() {
      return this.texHM;
   }

   public FloatBuffer getPuddlesParams(int var1) {
      this.floatBuffer.clear();
      this.floatBuffer.put(this.PuddlesParamWindINT.x);
      this.floatBuffer.put(this.muddyPuddles.getFinalValue());
      this.floatBuffer.put(0.0F);
      this.floatBuffer.put(0.0F);
      this.floatBuffer.put(this.PuddlesParamWindINT.y);
      this.floatBuffer.put(this.wetGround.getFinalValue());
      this.floatBuffer.put(0.0F);
      this.floatBuffer.put(0.0F);
      this.floatBuffer.put(this.PuddlesWindIntensity * 1.0F);
      this.floatBuffer.put(this.puddlesSize.getFinalValue());
      this.floatBuffer.put(0.0F);
      this.floatBuffer.put(0.0F);
      this.floatBuffer.put((float)var1);
      this.floatBuffer.put(this.rainIntensity.getFinalValue());
      this.floatBuffer.put(0.0F);
      this.floatBuffer.put(0.0F);
      this.floatBuffer.flip();
      return this.floatBuffer;
   }

   public float getRainIntensity() {
      return this.rainIntensity.getFinalValue();
   }

   public int getFloatMax() {
      return 5;
   }

   public int getBoolMax() {
      return 0;
   }

   public PuddlesFloat getPuddlesFloat(int var1) {
      if (var1 >= 0 && var1 < 5) {
         return this.climateFloats[var1];
      } else {
         DebugLog.log("ERROR: Climate: cannot get float override id.");
         return null;
      }
   }

   private PuddlesFloat initClimateFloat(int var1, String var2) {
      if (var1 >= 0 && var1 < 5) {
         return this.climateFloats[var1].init(var1, var2);
      } else {
         DebugLog.log("ERROR: Climate: cannot get float override id.");
         return null;
      }
   }

   private void setup() {
      for(int var1 = 0; var1 < this.climateFloats.length; ++var1) {
         this.climateFloats[var1] = new PuddlesFloat();
      }

      this.rain = this.initClimateFloat(0, Translator.getText("IGUI_PuddlesControl_Rain"));
      this.wetGround = this.initClimateFloat(1, Translator.getText("IGUI_PuddlesControl_WetGround"));
      this.muddyPuddles = this.initClimateFloat(2, Translator.getText("IGUI_PuddlesControl_MudPuddle"));
      this.puddlesSize = this.initClimateFloat(3, Translator.getText("IGUI_PuddlesControl_PuddleSize"));
      this.rainIntensity = this.initClimateFloat(4, Translator.getText("IGUI_PuddlesControl_RainIntensity"));
   }

   public void clearThreadData() {
      int var1 = SpriteRenderer.instance.getMainStateIndex();
      int var2 = IsoCamera.frameState.playerIndex;
      RenderData var3 = this.renderData[var1][var2];
      var3.clear();
   }

   public void renderToChunkTexture(ArrayList<IsoGridSquare> var1, int var2) {
      if (!var1.isEmpty()) {
         if (var2 <= 0 || Core.getInstance().getPerfPuddles() <= 0) {
            int var3 = SpriteRenderer.instance.getMainStateIndex();
            int var4 = IsoCamera.frameState.playerIndex;
            RenderData var5 = this.renderData[var3][var4];
            int var6 = var5.numSquares;

            int var10;
            for(int var7 = 0; var7 < var1.size(); ++var7) {
               IsoGridSquare var8 = (IsoGridSquare)var1.get(var7);
               IsoPuddlesGeometry var9 = var8.getPuddles();
               if (var9 != null && var9.shouldRender()) {
                  var9.updateLighting(var4);
                  var5.addSquare(var2, var9, (TileSeamManager.Tiles)null);
                  if (DebugOptions.instance.FBORenderChunk.SeamFix2.getValue()) {
                     var10 = 8;
                     if (PZMath.coordmodulo(var8.x, var10) == var10 - 1) {
                        var5.addSquare(var2, var9, TileSeamManager.Tiles.FloorEast);
                     }

                     if (PZMath.coordmodulo(var8.y, var10) == var10 - 1) {
                        var5.addSquare(var2, var9, TileSeamManager.Tiles.FloorSouth);
                     }
                  }
               }
            }

            if (var5.numSquares != var6) {
               RenderToChunkTexture var11 = (RenderToChunkTexture)this.renderToChunkTexturePool.alloc();
               FBORenderChunk var12 = FBORenderChunkManager.instance.renderChunk;
               var11.renderChunkX = IsoUtils.XToScreen((float)(var12.chunk.wx * 8), (float)(var12.chunk.wy * 8), (float)var2, 0);
               var11.renderChunkY = IsoUtils.YToScreen((float)(var12.chunk.wx * 8), (float)(var12.chunk.wy * 8), (float)var2, 0);
               var11.renderChunkWidth = var12.w;
               var11.renderChunkHeight = var12.h;
               int var13 = FBORenderChunk.FLOOR_HEIGHT * 8;
               var10 = var12.getTopLevel() - var12.getMinLevel() + 1;
               var11.renderChunkBottom = var13 + var10 * FBORenderChunk.PIXELS_PER_LEVEL;
               var11.renderChunkBottom += FBORenderLevels.extraHeightForJumboTrees(var12.getMinLevel(), var12.getTopLevel());
               var11.renderChunkMinZ = var12.getMinLevel();
               var11.bHighRes = var12.bHighRes;
               var11.playerIndex = var4;
               var11.z = var2;
               var11.firstSquare = var6;
               var11.numSquares = var5.numSquares - var6;
               SpriteRenderer.instance.drawGeneric(var11);
            }
         }
      }
   }

   public float getWetGroundFinalValue() {
      return this.wetGround.getFinalValue();
   }

   public float getPuddlesSizeFinalValue() {
      return this.puddlesSize.getFinalValue();
   }

   private static final class RenderData {
      final int[] squaresPerLevel = new int[64];
      int numSquares;
      int capacity = 512;
      float[] data;

      RenderData() {
      }

      void clear() {
         this.numSquares = 0;
         Arrays.fill(this.squaresPerLevel, 0);
      }

      void addSquare(int var1, IsoPuddlesGeometry var2, TileSeamManager.Tiles var3) {
         byte var4 = 4;
         if (this.data == null) {
            this.data = new float[this.capacity * var4 * 8];
         }

         if (this.numSquares + 1 > this.capacity) {
            this.capacity += 128;
            this.data = Arrays.copyOf(this.data, this.capacity * var4 * 8);
         }

         int var5 = IsoCamera.frameState.playerIndex;
         PlayerCamera var6 = IsoCamera.cameras[var5];
         float var7 = var6.fixJigglyModelsX * var6.zoom;
         float var8 = var6.fixJigglyModelsY * var6.zoom;
         int var9 = this.numSquares * var4 * 8;

         int var10002;
         for(int var10 = 0; var10 < 4; ++var10) {
            this.data[var9++] = var2.pdne[var10];
            this.data[var9++] = var2.pdnw[var10];
            this.data[var9++] = var2.pda[var10];
            this.data[var9++] = var2.pnon[var10];
            this.data[var9++] = var2.x[var10] + var7;
            this.data[var9++] = var2.y[var10] + var8;
            this.data[var9++] = Float.intBitsToFloat(var2.color[var10]);
            if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
               this.data[var9 - 1] = Float.intBitsToFloat(-1);
            }

            float var11 = 0.0F;
            float var12 = 0.0F;
            if (var10 == 2 || var10 == 3) {
               var11 = 1.0F;
            }

            if (var10 == 1 || var10 == 2) {
               var12 = 1.0F;
            }

            var11 += var6.fixJigglyModelsSquareX;
            var12 += var6.fixJigglyModelsSquareY;
            this.data[var9++] = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), (float)var2.square.x + var11, (float)var2.square.y + var12, (float)var2.square.z).depthStart - 1.0E-4F;
            if (FBORenderChunkManager.instance.isCaching()) {
               float[] var10000 = this.data;
               var10000[var9 - 1] -= IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / 8.0F), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / 8.0F), var2.square.chunk.wx, var2.square.chunk.wy, var1).depthStart;
               if (PZMath.coordmodulo(var2.square.x, 8) == 0 && var11 == 0.0F) {
                  var10000 = this.data;
                  var10000[var9 - 4] -= 2.0F;
                  var10002 = this.data[var9 - 3]--;
               }

               if (var3 == TileSeamManager.Tiles.FloorSouth) {
                  var10000 = this.data;
                  var10000[var9 - 1] -= IsoDepthHelper.SquareDepth;
                  var10000 = this.data;
                  var10000[var9 - 4] -= var12 == 1.0F ? 6.0F : 64.0F;
                  var10000 = this.data;
                  var10000[var9 - 3] += var12 == 1.0F ? 3.0F : 32.0F;
               }

               if (var3 == TileSeamManager.Tiles.FloorEast) {
                  var10000 = this.data;
                  var10000[var9 - 1] -= IsoDepthHelper.SquareDepth;
                  var10000 = this.data;
                  var10000[var9 - 4] += var11 == 1.0F ? 6.0F : 64.0F;
                  var10000 = this.data;
                  var10000[var9 - 3] += var11 == 1.0F ? 3.0F : 32.0F;
               }
            }
         }

         ++this.numSquares;
         var10002 = this.squaresPerLevel[var1 + 32]++;
      }
   }

   public static class PuddlesFloat {
      protected float finalValue;
      private boolean isAdminOverride = false;
      private float adminValue;
      private float min = 0.0F;
      private float max = 1.0F;
      private float delta = 0.01F;
      private int ID;
      private String name;

      public PuddlesFloat() {
      }

      public PuddlesFloat init(int var1, String var2) {
         this.ID = var1;
         this.name = var2;
         return this;
      }

      public int getID() {
         return this.ID;
      }

      public String getName() {
         return this.name;
      }

      public float getMin() {
         return this.min;
      }

      public float getMax() {
         return this.max;
      }

      public void setEnableAdmin(boolean var1) {
         this.isAdminOverride = var1;
      }

      public boolean isEnableAdmin() {
         return this.isAdminOverride;
      }

      public void setAdminValue(float var1) {
         this.adminValue = Math.max(this.min, Math.min(this.max, var1));
      }

      public float getAdminValue() {
         return this.adminValue;
      }

      public void setFinalValue(float var1) {
         this.finalValue = Math.max(this.min, Math.min(this.max, var1));
      }

      public void addFinalValue(float var1) {
         this.finalValue = Math.max(this.min, Math.min(this.max, this.finalValue + var1));
      }

      public void addFinalValueForMax(float var1, float var2) {
         this.finalValue = Math.max(this.min, Math.min(var2, this.finalValue + var1));
      }

      public float getFinalValue() {
         return this.isAdminOverride ? this.adminValue : this.finalValue;
      }

      public void interpolateFinalValue(float var1) {
         if (Math.abs(this.finalValue - var1) < this.delta) {
            this.finalValue = var1;
         } else if (var1 > this.finalValue) {
            this.finalValue += this.delta;
         } else {
            this.finalValue -= this.delta;
         }

      }

      private void calculate() {
         if (this.isAdminOverride) {
            this.finalValue = this.adminValue;
         }
      }
   }

   private static final class RenderToChunkTexture extends TextureDraw.GenericDrawer {
      float renderChunkX;
      float renderChunkY;
      int renderChunkWidth;
      int renderChunkHeight;
      int renderChunkBottom;
      int renderChunkMinZ;
      boolean bHighRes = false;
      int playerIndex;
      int z;
      int firstSquare;
      int numSquares;

      private RenderToChunkTexture() {
      }

      public void render() {
         GL11.glPushClientAttrib(-1);
         GL11.glPushAttrib(1048575);
         Matrix4f var1 = Core.getInstance().projectionMatrixStack.alloc();
         int var2 = FBORenderChunk.FLOOR_HEIGHT * 8;
         if (this.bHighRes) {
            var1.setOrtho((float)(-this.renderChunkWidth) / 4.0F, (float)this.renderChunkWidth / 4.0F, (float)(-var2) / 2.0F + 256.0F - (float)(this.z * FBORenderChunk.PIXELS_PER_LEVEL), (float)var2 / 2.0F + 256.0F - (float)(this.z * FBORenderChunk.PIXELS_PER_LEVEL), -1.0F, 1.0F);
         } else {
            var1.setOrtho((float)(-this.renderChunkWidth) / 2.0F, (float)this.renderChunkWidth / 2.0F, (float)(-var2) / 2.0F + 256.0F - (float)(this.z * FBORenderChunk.PIXELS_PER_LEVEL), (float)var2 / 2.0F + 256.0F - (float)(this.z * FBORenderChunk.PIXELS_PER_LEVEL), -1.0F, 1.0F);
         }

         Core.getInstance().projectionMatrixStack.push(var1);
         Matrix4f var3 = Core.getInstance().modelViewMatrixStack.alloc();
         var3.identity();
         Core.getInstance().modelViewMatrixStack.push(var3);
         if (this.bHighRes) {
            GL11.glViewport(0, (this.renderChunkBottom - var2 - (this.z - this.renderChunkMinZ) * FBORenderChunk.PIXELS_PER_LEVEL) * 2, this.renderChunkWidth, var2 * 2);
         } else {
            GL11.glViewport(0, this.renderChunkBottom - var2 - (this.z - this.renderChunkMinZ) * FBORenderChunk.PIXELS_PER_LEVEL, this.renderChunkWidth, var2);
         }

         int var4 = IsoPuddles.getInstance().Effect.getID();
         ShaderHelper.glUseProgramObjectARB(var4);
         Shader var5 = (Shader)Shader.ShaderMap.get(var4);
         int var6;
         if (var5 instanceof PuddlesShader) {
            ((PuddlesShader)var5).updatePuddlesParams(this.playerIndex, this.z);
            var6 = GL20.glGetUniformLocation(var4, "WOffset");
            float var7 = this.renderChunkX;
            float var8 = -this.renderChunkY;
            int var9;
            if (this.bHighRes) {
               GL20.glUniform4f(var6, var7 - 90000.0F, var8 - 640000.0F, (float)(this.renderChunkWidth / 2), (float)var2);
               var9 = GL20.glGetUniformLocation(var4, "WViewport");
               GL20.glUniform4f(var9, 0.0F, (float)((this.renderChunkBottom - var2 - (this.z - this.renderChunkMinZ) * FBORenderChunk.PIXELS_PER_LEVEL) * 2), (float)this.renderChunkWidth, (float)(var2 * 2));
            } else {
               GL20.glUniform4f(var6, var7 - 90000.0F, var8 - 640000.0F, (float)this.renderChunkWidth, (float)var2);
               var9 = GL20.glGetUniformLocation(var4, "WViewport");
               GL20.glUniform4f(var9, 0.0F, (float)(this.renderChunkBottom - var2 - (this.z - this.renderChunkMinZ) * FBORenderChunk.PIXELS_PER_LEVEL), (float)this.renderChunkWidth, (float)var2);
            }
         }

         VertexBufferObject.setModelViewProjection(var5.getProgram());
         GL14.glBlendFuncSeparate(770, 771, 1, 1);

         while(this.numSquares > 0) {
            var6 = IsoPuddles.instance.renderSome(this.firstSquare, this.numSquares, true);
            this.firstSquare += var6;
            this.numSquares -= var6;
         }

         SpriteRenderer.ringBuffer.restoreVBOs = true;
         Core.getInstance().projectionMatrixStack.pop();
         Core.getInstance().modelViewMatrixStack.pop();
         ShaderHelper.glUseProgramObjectARB(0);
         Texture.lastTextureID = -1;
         GL11.glPopAttrib();
         GL11.glPopClientAttrib();
         ShaderHelper.glUseProgramObjectARB(0);
         Texture.lastTextureID = -1;
         GLStateRenderThread.restore();
      }

      public void postRender() {
         IsoPuddles.instance.renderToChunkTexturePool.release((Object)this);
      }
   }
}
