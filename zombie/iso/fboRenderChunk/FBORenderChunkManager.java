package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.DefaultShader;
import zombie.core.SceneShaderStore;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;

public final class FBORenderChunkManager {
   public static FBORenderChunkManager instance = new FBORenderChunkManager();
   final HashMap<Integer, ArrayList<FBORenderChunk>> sizeChunkStore = new HashMap();
   final Stack<FBORenderChunk> toRecycle = new Stack();
   public final HashSet<IsoChunk> chunkFullMap = new HashSet();
   public final HashMap<Integer, FBORenderChunk> chunks = new HashMap();
   public final ArrayList<FBORenderChunk> toRenderThisFrame = new ArrayList();
   private final ArrayList<FBORenderChunk> tempRenderChunks = new ArrayList();
   public FBORenderChunk renderThreadCurrent;
   int rcIndex = 0;
   float yoff;
   float xoff;
   boolean caching = false;
   public FBORenderChunk renderChunk;
   public Texture combinedTexture;
   public Texture combinedDepthTexture;
   private TextureFBO combinedFBO;

   public FBORenderChunkManager() {
   }

   public float getYOffset() {
      return this.yoff;
   }

   public float getXOffset() {
      return this.xoff;
   }

   public void gameLoaded() {
      this.recycle();
      byte var1 = 0;
      IsoChunkMap var2 = IsoWorld.instance.CurrentCell.ChunkMap[var1];

      for(int var3 = 0; var3 < 8; ++var3) {
         for(int var4 = 0; var4 < 8; ++var4) {
            IsoChunk var5 = var2.getChunk(var4, var3);
            if (var5 != null) {
               FBORenderLevels var6 = var5.getRenderLevels(var1);

               for(int var7 = var5.minLevel; var7 <= var5.maxLevel; ++var7) {
                  float var8 = 1.0F;
                  var6.getOrCreateFBOForLevel(var7, var8);
                  var8 = 0.5F;
                  var6.getOrCreateFBOForLevel(var7, var8);
               }
            }
         }
      }

   }

   public void recycle() {
      for(int var1 = 0; var1 < this.toRecycle.size(); ++var1) {
         FBORenderChunk var2 = (FBORenderChunk)this.toRecycle.get(var1);
         int var3 = var2.h;
         if (!var2.submitted) {
            ArrayList var4;
            if (this.sizeChunkStore.containsKey(var3)) {
               var4 = (ArrayList)this.sizeChunkStore.get(var3);
            } else {
               var4 = new ArrayList();
               this.sizeChunkStore.put(var3, var4);
            }

            var4.add(var2);
            this.toRecycle.remove(var2);
            --var1;
         }
      }

   }

   public boolean endCaching() {
      if (this.caching) {
         this.renderChunk.endMainThread();
         SpriteRenderer.instance.glDoEndFrame();
         SpriteRenderer.instance.glDoStartFrame(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), Core.getInstance().getCurrentPlayerZoom(), IsoCamera.frameState.playerIndex);
         this.caching = false;
         return true;
      } else {
         return false;
      }
   }

   public void submitCachesForFrame() {
      int var1 = IsoCamera.frameState.playerIndex;
      ConcurrentHashMap var2 = SpriteRenderer.instance.getPopulatingState().cachedRenderChunkIndexMap;
      this.tempRenderChunks.clear();
      this.tempRenderChunks.addAll(var2.values());

      int var3;
      FBORenderChunk var4;
      for(var3 = 0; var3 < this.tempRenderChunks.size(); ++var3) {
         var4 = (FBORenderChunk)this.tempRenderChunks.get(var3);
         if (var4.getRenderLevels().getPlayerIndex() == var1) {
            var2.remove(var4.index);
         }
      }

      for(var3 = 0; var3 < this.toRenderThisFrame.size(); ++var3) {
         var4 = (FBORenderChunk)this.toRenderThisFrame.get(var3);
         var2.put(var4.index, var4);
         var4.submitted = true;
      }

   }

   public boolean beginRenderChunkLevel(IsoChunk var1, int var2, float var3, boolean var4, boolean var5) {
      int var6 = IsoCamera.frameState.playerIndex;
      FBORenderLevels var7 = var1.getRenderLevels(var6);
      this.renderChunk = var7.getOrCreateFBOForLevel(var2, var3);
      int var8 = this.renderChunk.w;
      this.xoff = (float)var8 / 2.0F;
      this.yoff = (float)((this.renderChunk.getTopLevel() - this.renderChunk.getMinLevel() + 1) * FBORenderChunk.PIXELS_PER_LEVEL);
      this.yoff += (float)(this.renderChunk.getMinLevel() * FBORenderChunk.PIXELS_PER_LEVEL);
      this.yoff += (float)FBORenderLevels.extraHeightForJumboTrees(this.renderChunk.getMinLevel(), this.renderChunk.getTopLevel());
      if (var7.isDirty(var2, var3)) {
         if (var2 == this.renderChunk.getMinLevel()) {
            this.caching = true;
            SpriteRenderer.instance.glDoEndFrame();
            SpriteRenderer.instance.glDoStartFrameFlipY(this.renderChunk.w, this.renderChunk.h, this.renderChunk.bHighRes ? 1.0F : 0.0F, var6);
            this.renderChunk.beginMainThread(true);
         }

         return true;
      } else {
         return false;
      }
   }

   public void endRenderChunkLevel(IsoChunk var1, int var2, float var3, boolean var4) {
      if (this.renderChunk.isTopLevel(var2)) {
         if (this.isCaching()) {
            int var5 = IsoCamera.frameState.playerIndex;
            FBORenderLevels var6 = var1.getRenderLevels(var5);
            if (var4) {
               var6.clearDirty(var2, var3);
            }

            this.endCaching();
         }

         this.toRenderThisFrame.add(this.renderChunk);
         this.renderChunk = null;
      }

   }

   public void clearCache() {
      Iterator var1 = this.chunkFullMap.iterator();

      while(var1.hasNext()) {
         IsoChunk var2 = (IsoChunk)var1.next();

         for(int var3 = 0; var3 < 4; ++var3) {
            FBORenderLevels var4 = var2.getRenderLevels(var3);
            var4.clearCache();
         }
      }

      this.chunkFullMap.clear();
      this.chunks.clear();
   }

   protected void addToStore(FBORenderChunk var1) {
      if (!this.toRecycle.contains(var1)) {
         this.toRecycle.add(var1);
      }
   }

   public void freeChunk(IsoChunk var1) {
      for(int var2 = 0; var2 < 4; ++var2) {
         var1.getRenderLevels(var2).freeChunk();
      }

   }

   public FBORenderChunk getFullRenderChunk(IsoChunk var1, int var2, int var3) {
      ArrayList var5 = (ArrayList)this.sizeChunkStore.get(var3);
      boolean var6 = false;
      FBORenderChunk var4;
      if (var5 != null && !var5.isEmpty()) {
         var4 = (FBORenderChunk)var5.remove(var5.size() - 1);
      } else {
         var4 = new FBORenderChunk();
         var6 = true;
      }

      var4.chunk = var1;
      var4.w = var2;
      var4.h = var3;
      if (var4.index == -1) {
         var4.index = this.rcIndex++;
      }

      this.chunkFullMap.add(var1);
      this.chunks.put(var4.index, var4);
      if (var6) {
         var4.preInit();
      }

      return var4;
   }

   public boolean isCaching() {
      return this.caching;
   }

   public void renderThreadChunkEnd() {
      if (this.renderThreadCurrent != null) {
         this.renderThreadCurrent.endRenderThread();
         this.renderThreadCurrent = null;
      }
   }

   public void renderThreadChunkStart(int var1, boolean var2) {
      this.renderThreadCurrent = (FBORenderChunk)SpriteRenderer.instance.getRenderingState().cachedRenderChunkIndexMap.get(var1);
      if (this.renderThreadCurrent != null) {
         this.renderThreadCurrent.beginRenderThread(var2);
      }
   }

   private void checkCombinedFBO() {
      int var1 = (int)Math.ceil((double)((float)Core.width * 2.5F));
      int var2 = (int)Math.ceil((double)((float)Core.height * 2.5F));
      if (this.combinedFBO == null || this.combinedFBO.getWidth() != var1 || this.combinedFBO.getHeight() != var2) {
         if (this.combinedFBO != null) {
            this.combinedFBO.destroy();
         }

         this.combinedTexture = new Texture(var1, var2, 16);
         this.combinedDepthTexture = new Texture(var1, var2, 512);
         this.combinedFBO = new TextureFBO(this.combinedTexture, this.combinedDepthTexture, false);
      }

   }

   public void startFrame() {
      this.toRenderThisFrame.clear();
   }

   public void endFrame() {
      if (!DebugOptions.instance.FBORenderChunk.RenderChunkTextures.getValue()) {
         instance.submitCachesForFrame();
         SpriteRenderer.instance.releaseFBORenderChunkLock();
      } else {
         int var1;
         int var2;
         int var3;
         int var4;
         FBORenderChunk var5;
         if (DebugOptions.instance.FBORenderChunk.CombinedFBO.getValue() && !this.toRenderThisFrame.isEmpty()) {
            var1 = IsoCamera.frameState.playerIndex;
            var2 = Core.getInstance().getOffscreenWidth(var1);
            var3 = Core.getInstance().getOffscreenHeight(var1);
            SpriteRenderer.instance.glDoEndFrame();
            this.checkCombinedFBO();
            SpriteRenderer.instance.glDoStartFrameNoZoom(this.combinedTexture.getWidth(), this.combinedTexture.getHeight(), 1.0F, var1);
            SpriteRenderer.instance.glBuffer(10, var1);

            for(var4 = 0; var4 < this.toRenderThisFrame.size(); ++var4) {
               var5 = (FBORenderChunk)this.toRenderThisFrame.get(var4);
               if (var5.getRenderLevels().getPlayerIndex() == var1) {
                  var5.renderInWorldMainThread();
               }
            }

            SpriteRenderer.instance.glBuffer(11, var1);
            SpriteRenderer.instance.glDoEndFrame();
            SpriteRenderer.instance.glDoStartFrame(Core.width, Core.height, 1.0F, IsoCamera.frameState.playerIndex);
            IndieGL.enableDepthTest();
            IndieGL.glDepthFunc(519);
            IndieGL.glDepthMask(true);
            IndieGL.glBlendFunc(770, 771);
            float var7 = Core.getInstance().getZoom(var1);
            if (SceneShaderStore.ChunkRenderShader != null) {
               IndieGL.StartShader(SceneShaderStore.ChunkRenderShader.getID());
               int var8 = SpriteRenderer.instance.m_states.getPopulatingActiveState().numSprites;
               TextureDraw var6 = SpriteRenderer.instance.m_states.getPopulatingActiveState().sprite[var8 - 1];
               var6.tex1 = this.combinedDepthTexture;
               var6.chunkDepth = 0.0F;
            }

            if (this.combinedTexture.getTextureId() != null) {
               this.combinedTexture.getTextureId().setMinFilter(9729);
               this.combinedTexture.getTextureId().setMagFilter(9729);
            }

            PlayerCamera var9 = IsoCamera.cameras[var1];
            this.combinedTexture.rendershader2(0.0F + var9.fixJigglyModelsX, 0.0F + var9.fixJigglyModelsY, (float)Core.width, (float)Core.height, 0, this.combinedTexture.getHeight() - (int)((float)Core.height * var7), (int)((float)Core.width * var7), (int)((float)Core.height * var7), 1.0F, 1.0F, 1.0F, 1.0F);
            if (SceneShaderStore.ChunkRenderShader != null) {
               IndieGL.EndShader();
            }

            SpriteRenderer.instance.glDoEndFrame();
            SpriteRenderer.instance.glDoStartFrame(var2, var3, Core.getInstance().getCurrentPlayerZoom(), var1);
         }

         if (!DebugOptions.instance.FBORenderChunk.CombinedFBO.getValue() && !this.toRenderThisFrame.isEmpty()) {
            var1 = IsoCamera.frameState.playerIndex;
            var2 = Core.getInstance().getOffscreenWidth(var1);
            var3 = Core.getInstance().getOffscreenHeight(var1);
            SpriteRenderer.instance.glDoEndFrame();
            SpriteRenderer.instance.glDoStartFrameNoZoom(var2, var3, Core.getInstance().getCurrentPlayerZoom(), var1);

            for(var4 = 0; var4 < this.toRenderThisFrame.size(); ++var4) {
               var5 = (FBORenderChunk)this.toRenderThisFrame.get(var4);
               if (var5.getRenderLevels().getPlayerIndex() == var1) {
                  var5.renderInWorldMainThread();
               }
            }

            SpriteRenderer.instance.glDoEndFrame();
            SpriteRenderer.instance.glDoStartFrame(var2, var3, Core.getInstance().getCurrentPlayerZoom(), var1);
         }

         instance.submitCachesForFrame();
         SpriteRenderer.instance.releaseFBORenderChunkLock();
      }
   }

   public void startDrawingCombined() {
      GL11.glEnable(2929);
      GL11.glDepthFunc(519);
      GL11.glDepthMask(true);
      this.combinedFBO.startDrawing(true, false);
      GL11.glClearDepth(1.0);
      GL11.glClear(256);
   }

   public void endDrawingCombined() {
      this.combinedFBO.endDrawing();
      GL20.glUseProgram(0);
      DefaultShader.isActive = false;
      ShaderHelper.forgetCurrentlyBound();
   }

   public void Reset() {
      this.clearCache();
      this.recycle();
      Iterator var1 = this.sizeChunkStore.values().iterator();

      while(var1.hasNext()) {
         ArrayList var2 = (ArrayList)var1.next();
         Iterator var3 = var2.iterator();

         while(var3.hasNext()) {
            FBORenderChunk var4 = (FBORenderChunk)var3.next();
            if (var4.fbo != null) {
               var4.fbo.destroy();
               var4.fbo = null;
            }
         }

         var2.clear();
      }

      this.sizeChunkStore.clear();
      this.renderChunk = null;
   }
}
