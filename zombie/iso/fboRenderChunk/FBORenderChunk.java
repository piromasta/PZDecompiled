package zombie.iso.fboRenderChunk;

import java.util.function.Consumer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoUtils;
import zombie.iso.PlayerCamera;

public final class FBORenderChunk {
   public static final int PIXELS_PER_LEVEL;
   public static final int FLOOR_HEIGHT;
   public static final int JUMBO_HEIGHT;
   public static final int TEXTURE_HEIGHT = 1024;
   public static final int LEVELS_PER_TEXTURE = 2;
   public static long DIRTY_NONE;
   public static long DIRTY_BLOOD;
   public static long DIRTY_CORPSE;
   public static long DIRTY_ITEM_ADD;
   public static long DIRTY_ITEM_REMOVE;
   public static long DIRTY_ITEM_MODIFY;
   public static long DIRTY_LIGHTING;
   public static long DIRTY_OBJECT_ADD;
   public static long DIRTY_OBJECT_REMOVE;
   public static long DIRTY_OBJECT_MODIFY;
   public static long DIRTY_CREATE;
   public static long DIRTY_REDRAW;
   public static long DIRTY_CUTAWAYS;
   public static long DIRTY_TREES;
   public static long DIRTY_OBSCURING;
   public static long DIRTY_REDO_CUTAWAYS;
   private FBORenderLevels m_renderLevels = null;
   public int index = -1;
   public TextureFBO fbo;
   public boolean submitted;
   public boolean isInit;
   public Texture tex;
   public Texture depth;
   public int w;
   public int h;
   public IsoChunk chunk;
   public boolean bHighRes = false;
   public int minLevel;
   public float m_renderX;
   public float m_renderY;
   public float m_renderW;
   public float m_renderH;

   public FBORenderChunk() {
   }

   public void setRenderLevels(FBORenderLevels var1) {
      this.m_renderLevels = var1;
   }

   public FBORenderLevels getRenderLevels() {
      return this.m_renderLevels;
   }

   public int getTextureWidth(float var1) {
      return FBORenderLevels.calculateTextureWidthForLevels(this.getMinLevel(), this.getTopLevel(), var1);
   }

   public int getTextureHeight(float var1) {
      return FBORenderLevels.calculateTextureHeightForLevels(this.getMinLevel(), this.getTopLevel(), var1);
   }

   public int getMinLevel() {
      return this.minLevel;
   }

   public int getTopLevel() {
      if (this.minLevel < 0) {
         int var1 = Math.abs(this.getMinLevel() + 1) / 2;
         return Math.min(-(var1 + 1) * 2 + 2 - 1, this.chunk.maxLevel);
      } else {
         return Math.min(this.minLevel + 2 - 1, this.chunk.maxLevel);
      }
   }

   public boolean isTopLevel(int var1) {
      return var1 == this.getTopLevel();
   }

   public void preInit() {
      this.tex = new Texture(this.w, this.h, 16, true);
      this.tex.setNameOnly("FBORenderChunk.tex");
      this.depth = new Texture(this.w, this.h, 512, true);
      this.depth.setNameOnly("FBORenderChunk.depth");
   }

   public void init() {
      if (!this.isInit) {
         this.depth.TexDeferedCreation(this.w, this.h, 512);
         this.tex.TexDeferedCreation(this.w, this.h, 16);
         this.fbo = new TextureFBO(this.tex, this.depth, false);
         this.isInit = true;
      }
   }

   public void beginMainThread(boolean var1) {
      SpriteRenderer.instance.FBORenderChunkStart(this.index, var1);
   }

   public void endMainThread() {
      SpriteRenderer.instance.FBORenderChunkEnd();
   }

   public void beginRenderThread(boolean var1) {
      if (!this.isInit) {
         this.init();
      }

      if (var1) {
         this.fbo.startDrawing(true, true);
         GL11.glEnable(2929);
         GL11.glDepthFunc(519);
         GL11.glDepthMask(true);
         GL11.glClearDepth(1.0);
         GL11.glClear(256);
         GLStateRenderThread.DepthFunc.restore();
         GLStateRenderThread.DepthMask.restore();
         GLStateRenderThread.DepthTest.restore();
      } else {
         this.fbo.startDrawing(false, false);
      }

   }

   public void endRenderThread() {
      this.fbo.endDrawing();
      if (DebugOptions.instance.FBORenderChunk.MipMaps.getValue() && !this.bHighRes) {
         this.tex.bind();
         GL30.glGenerateMipmap(3553);
      }

   }

   public Texture getTexture() {
      return this.tex;
   }

   public void renderInWorldMainThread() {
      int var1 = IsoCamera.frameState.playerIndex;
      if (SceneShaderStore.ChunkRenderShader != null) {
         IndieGL.StartShader(SceneShaderStore.ChunkRenderShader.getID());
         int var2 = SpriteRenderer.instance.m_states.getPopulatingActiveState().numSprites;
         TextureDraw var3 = SpriteRenderer.instance.m_states.getPopulatingActiveState().sprite[var2 - 1];
         var3.tex1 = this.depth;
         byte var4 = 8;
         IsoDepthHelper.Results var5 = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / (float)var4), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / (float)var4), this.chunk.wx, this.chunk.wy, this.getMinLevel());
         var3.chunkDepth = var5.depthStart;
         if (!DebugOptions.instance.FBORenderChunk.CombinedFBO.getValue()) {
            PlayerCamera var6 = IsoCamera.cameras[var1];
            float var7 = var6.fixJigglyModelsSquareX;
            float var8 = var6.fixJigglyModelsSquareY;
            float var9 = ((float)(var5.indexX + var5.indexY) - var7 - var8) / (float)var4 / 40.0F;
            var9 *= IsoDepthHelper.ChunkDepth * 20.0F;
            var9 -= (float)FBORenderLevels.calculateMinLevel(this.getMinLevel()) * IsoDepthHelper.LevelDepth;
            var3.chunkDepth = var9;
         }
      }

      IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
      float var10 = IsoUtils.XToScreen((float)(this.chunk.wx * 8), (float)(this.chunk.wy * 8), (float)this.getMinLevel(), 0);
      float var11 = IsoUtils.YToScreen((float)(this.chunk.wx * 8), (float)(this.chunk.wy * 8), (float)this.getMinLevel(), 0);
      float var12 = (float)this.w;
      float var13 = (float)this.h;
      if (this.bHighRes) {
         var12 /= 2.0F;
         var13 /= 2.0F;
      }

      var11 -= (float)(PIXELS_PER_LEVEL * (this.getTopLevel() - this.getMinLevel() + 1));
      var11 -= (float)FBORenderLevels.extraHeightForJumboTrees(this.getMinLevel(), this.getTopLevel());
      var10 -= IsoCamera.getOffX();
      var11 -= IsoCamera.getOffY();
      if (!DebugOptions.instance.FBORenderChunk.CombinedFBO.getValue()) {
         var10 /= IsoCamera.frameState.zoom;
         var11 /= IsoCamera.frameState.zoom;
         var12 /= IsoCamera.frameState.zoom;
         var13 /= IsoCamera.frameState.zoom;
      }

      var10 -= var12 / 2.0F;
      if (!DebugOptions.instance.FBORenderChunk.CombinedFBO.getValue()) {
         var10 += IsoCamera.cameras[var1].fixJigglyModelsX;
         var11 += IsoCamera.cameras[var1].fixJigglyModelsY;
      }

      if (this.tex.getTextureId() != null) {
         boolean var14 = DebugOptions.instance.FBORenderChunk.MipMaps.getValue() && !this.bHighRes;
         this.tex.getTextureId().setMinFilter(var14 ? 9987 : 9728);
         this.tex.getTextureId().setMagFilter(IsoCamera.frameState.zoom == 0.75F ? 9729 : 9728);
      }

      IndieGL.glDepthFunc(515);
      IndieGL.glDepthMask(true);
      IndieGL.enableDepthTest();
      SpriteRenderer.instance.render(this.getTexture(), var10, var11, var12, var13, 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
      IndieGL.enableDepthTest();
      IndieGL.glDepthMask(true);
      if (SceneShaderStore.ChunkRenderShader != null) {
         IndieGL.EndShader();
      }

      this.m_renderX = var10;
      this.m_renderY = var11;
      this.m_renderW = var12;
      this.m_renderH = var13;
   }

   static {
      PIXELS_PER_LEVEL = 96 * Core.TileScale;
      FLOOR_HEIGHT = 32 * Core.TileScale;
      JUMBO_HEIGHT = 256 * Core.TileScale;
      DIRTY_NONE = 0L;
      DIRTY_BLOOD = 1L;
      DIRTY_CORPSE = 2L;
      DIRTY_ITEM_ADD = 4L;
      DIRTY_ITEM_REMOVE = 8L;
      DIRTY_ITEM_MODIFY = 16L;
      DIRTY_LIGHTING = 32L;
      DIRTY_OBJECT_ADD = 64L;
      DIRTY_OBJECT_REMOVE = 128L;
      DIRTY_OBJECT_MODIFY = 256L;
      DIRTY_CREATE = 512L;
      DIRTY_REDRAW = 1024L;
      DIRTY_CUTAWAYS = 2048L;
      DIRTY_TREES = 4096L;
      DIRTY_OBSCURING = 8192L;
      DIRTY_REDO_CUTAWAYS = 16384L;
   }
}
