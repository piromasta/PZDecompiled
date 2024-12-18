package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.utils.ImageUtils;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;

public final class FBORenderLevels {
   public static boolean bClearCachedSquares = true;
   private final int m_playerIndex;
   private final IsoChunk m_chunk;
   private final NLevels[] m_FBOs = new NLevels[32];
   public final ArrayList<IsoGridSquare> m_treeSquares = new ArrayList();
   final ArrayList<IsoGridSquare> m_waterSquares = new ArrayList();
   final ArrayList<IsoGridSquare> m_waterShoreSquares = new ArrayList();
   final ArrayList<IsoGridSquare> m_waterAttachSquares = new ArrayList();
   public final FBORenderSnow.ChunkLevel m_snowLevelZero;
   public final FBORenderSnow.ChunkLevel m_snowLevelNotZero;
   float m_renderX;
   float m_renderY;
   float m_renderW;
   float m_renderH;
   boolean m_bInStencilRect = false;
   public int m_adjacentChunkLoadedCounter = 0;
   public IsoChunk m_seamChunkE = null;
   public IsoChunk m_seamChunkSE = null;
   public IsoChunk m_seamChunkS = null;
   public int m_prevMinZ = 2147483647;
   public int m_prevMaxZ = -2147483648;

   public FBORenderLevels(int var1, IsoChunk var2) {
      this.m_playerIndex = var1;
      this.m_chunk = var2;
      int var3 = calculateMinLevel(-32);

      for(int var4 = 0; var4 < this.m_FBOs.length; ++var4) {
         this.m_FBOs[var4] = new NLevels(calculateMinLevel(var3 + var4 * 2));
      }

      this.m_snowLevelZero = new FBORenderSnow.ChunkLevel(var2);
      this.m_snowLevelNotZero = new FBORenderSnow.ChunkLevel(var2);
   }

   public int getPlayerIndex() {
      return this.m_playerIndex;
   }

   public IsoChunk getChunk() {
      return this.m_chunk;
   }

   private int indexForLevel(int var1) {
      int var2 = calculateMinLevel(-32);
      return (calculateMinLevel(var1) - var2) / 2;
   }

   private NLevels getNLevels(int var1) {
      return this.m_FBOs[this.indexForLevel(var1)];
   }

   void calculateRenderBounds(int var1, int var2) {
      int var3 = var2 - var1 + 1;
      float var4 = (float)(64 * Core.TileScale * 8);
      float var5 = (float)(FBORenderChunk.FLOOR_HEIGHT * 8 + FBORenderChunk.PIXELS_PER_LEVEL * var3);
      float var6 = IsoUtils.XToScreen((float)(this.m_chunk.wx * 8), (float)(this.m_chunk.wy * 8), (float)var1, 0);
      float var7 = IsoUtils.YToScreen((float)(this.m_chunk.wx * 8), (float)(this.m_chunk.wy * 8), (float)var1, 0);
      var7 -= (float)(FBORenderChunk.PIXELS_PER_LEVEL * var3);
      int var8 = extraHeightForJumboTrees(var1, var2);
      var5 += (float)var8;
      var7 -= (float)var8;
      var6 -= IsoCamera.cameras[this.m_playerIndex].getOffX();
      var7 -= IsoCamera.cameras[this.m_playerIndex].getOffY();
      float var9 = Core.getInstance().getZoom(this.m_playerIndex);
      var6 /= var9;
      var7 /= var9;
      var4 /= var9;
      var5 /= var9;
      var6 -= var4 / 2.0F;
      this.m_renderX = var6;
      this.m_renderY = var7;
      this.m_renderW = var4;
      this.m_renderH = var5;
   }

   void calculateRenderBounds(int var1) {
      byte var2 = 2;
      int var3 = calculateMinLevel(var1);
      int var4 = var3 + var2 - 1;
      this.calculateRenderBounds(var3, var4);
   }

   public boolean calculateOnScreen(int var1) {
      this.calculateRenderBounds(var1);
      int var2 = IsoCamera.getScreenLeft(this.m_playerIndex) * 0;
      int var3 = IsoCamera.getScreenTop(this.m_playerIndex) * 0;
      int var4 = var2 + IsoCamera.getScreenWidth(this.m_playerIndex);
      int var5 = var3 + IsoCamera.getScreenHeight(this.m_playerIndex);
      return this.m_renderX + this.m_renderW > (float)var2 && this.m_renderX < (float)var4 && this.m_renderY + this.m_renderH > (float)var3 && this.m_renderY < (float)var5;
   }

   public boolean calculateInStencilRect(int var1) {
      this.calculateRenderBounds(var1, var1);
      float var2 = Core.getInstance().getZoom(this.m_playerIndex);
      int var3 = 512 * Core.TileScale;
      int var4 = 512 * Core.TileScale;
      float var5 = (float)IsoCamera.getScreenWidth(this.m_playerIndex) / 2.0F * var2 - IsoCamera.cameras[this.m_playerIndex].RightClickX;
      float var6 = (float)IsoCamera.getScreenHeight(this.m_playerIndex) / 2.0F * var2 - IsoCamera.cameras[this.m_playerIndex].RightClickY;
      float var7 = (var5 - (float)var3 / 2.0F) / var2;
      float var8 = (var6 - (float)var4 / 2.0F) / var2;
      float var9 = var7 + (float)var3 / var2;
      float var10 = var8 + (float)var4 / var2;
      return this.m_renderX + this.m_renderW > var7 && this.m_renderX < var9 && this.m_renderY + this.m_renderH > var8 && this.m_renderY < var10;
   }

   public void setOnScreen(int var1, boolean var2) {
      this.getNLevels(var1).m_bOnScreen = var2;
   }

   public boolean isOnScreen(int var1) {
      return this.getNLevels(var1).m_bOnScreen;
   }

   public FBORenderChunk getOrCreateFBOForLevel(int var1, float var2) {
      FBORenderChunk var3 = this.getFBOForLevel(var1, var2);
      if (var3 != null && !this.isTextureSizeValid(var3, var2)) {
         this.freeFBO(var3);
         var3 = null;
      }

      if (var3 == null) {
         var3 = this.createFBOForLevel(var1, var2);
      }

      return var3;
   }

   public FBORenderChunk getFBOForLevel(int var1, float var2) {
      return this.getNLevels(var1).getZoomInfo(var2).renderChunk;
   }

   public void clearCachedSquares(int var1) {
      NLevels var2 = this.getNLevels(var1);
      var2.m_animatedAttachments.clear();
      var2.m_corpseSquares.clear();
      var2.m_fliesSquares.clear();
      var2.m_itemSquares.clear();
      var2.m_puddleSquares.clear();
      var2.m_translucentFloor.clear();
      var2.m_translucentNonFloor.clear();
   }

   public ArrayList<IsoGridSquare> getCachedSquares_AnimatedAttachments(int var1) {
      return this.getNLevels(var1).m_animatedAttachments;
   }

   public ArrayList<IsoGridSquare> getCachedSquares_Corpses(int var1) {
      return this.getNLevels(var1).m_corpseSquares;
   }

   public ArrayList<IsoGridSquare> getCachedSquares_Flies(int var1) {
      return this.getNLevels(var1).m_fliesSquares;
   }

   public ArrayList<IsoGridSquare> getCachedSquares_Items(int var1) {
      return this.getNLevels(var1).m_itemSquares;
   }

   public ArrayList<IsoGridSquare> getCachedSquares_Puddles(int var1) {
      return this.getNLevels(var1).m_puddleSquares;
   }

   public ArrayList<IsoGridSquare> getCachedSquares_TranslucentFloor(int var1) {
      return this.getNLevels(var1).m_translucentFloor;
   }

   public ArrayList<IsoGridSquare> getCachedSquares_TranslucentNonFloor(int var1) {
      return this.getNLevels(var1).m_translucentNonFloor;
   }

   public void setRenderedSquaresCount(int var1, int var2) {
      this.getNLevels(var1).m_renderedSquareCount = var2;
   }

   public int getRenderedSquaresCount(int var1) {
      return this.getNLevels(var1).m_renderedSquareCount;
   }

   public static int getTextureScale(float var0) {
      if (DebugOptions.instance.FBORenderChunk.HighResChunkTextures.getValue()) {
         return var0 < 0.75F ? 2 : 1;
      } else {
         return 1;
      }
   }

   private boolean isTextureSizeValid(FBORenderChunk var1, float var2) {
      if (var1 == null) {
         return false;
      } else {
         int var3 = calculateTextureWidthForLevels(var1.getMinLevel(), var1.getTopLevel(), var2);
         int var4 = calculateTextureHeightForLevels(var1.getMinLevel(), var1.getTopLevel(), var2);
         return var3 == var1.w && var4 == var1.h;
      }
   }

   public static int calculateMinLevel(int var0) {
      int var1 = var0 / 2 * 2;
      if (var0 < 0) {
         var1 = Math.abs(var0 + 1) / 2;
         var1 = -(var1 + 1) * 2;
      }

      return var1;
   }

   public int getMinLevel(int var1) {
      NLevels var2 = this.getNLevels(var1);
      return Math.max(var2.m_minLevel, this.m_chunk.minLevel);
   }

   public int getMaxLevel(int var1) {
      NLevels var2 = this.getNLevels(var1);
      return Math.min(var2.m_minLevel + 2 - 1, this.m_chunk.maxLevel);
   }

   private FBORenderChunk createFBOForLevel(int var1, float var2) {
      int var3 = var1 / 2 * 2;
      int var4 = Math.min(var3 + 2 - 1, this.getChunk().maxLevel);
      if (var1 < 0) {
         var3 = Math.abs(var1 + 1) / 2;
         var3 = -(var3 + 1) * 2;
         var4 = Math.min(var3 + 2 - 1, this.m_chunk.maxLevel);
         var3 = Math.max(var3, this.getChunk().minLevel);
      }

      int var5 = calculateTextureWidthForLevels(var3, var4, var2);
      int var6 = calculateTextureHeightForLevels(var3, var4, var2);
      FBORenderChunk var7 = FBORenderChunkManager.instance.getFullRenderChunk(this.getChunk(), var5, var6);
      var7.bHighRes = getTextureScale(var2) > 1;
      var7.setRenderLevels(this);
      var7.minLevel = var3;
      NLevels var8 = this.getNLevels(var3);
      var8.setDirty(FBORenderChunk.DIRTY_CREATE);
      var8.m_zoomInfo[getTextureScale(var2) - 1].renderChunk = var7;
      return var7;
   }

   public void invalidateLevel(int var1, long var2) {
      NLevels var4 = this.getNLevels(var1);
      var4.invalidate(var2);
   }

   public void invalidateAll(long var1) {
      for(int var3 = 0; var3 < this.m_FBOs.length; ++var3) {
         this.m_FBOs[var3].setDirty(var1);
      }

   }

   public boolean isDirty(int var1, float var2) {
      NLevels var3 = this.getNLevels(var1);
      return var3.isDirty(var2);
   }

   public boolean isDirty(int var1, long var2, float var4) {
      NLevels var5 = this.getNLevels(var1);
      ZoomInfo var6 = var5.getZoomInfo(var4);
      return var6.dirty && (var6.dirtyFlags & var2) != 0L;
   }

   public void clearDirty(int var1, float var2) {
      NLevels var3 = this.getNLevels(var1);
      var3.clearDirty(var2);
   }

   public void clearDirty(int var1, long var2, float var4) {
      NLevels var5 = this.getNLevels(var1);
      var5.clearDirty(var2, var4);
   }

   public void freeChunk() {
      for(int var1 = 0; var1 < this.m_FBOs.length; ++var1) {
         NLevels var2 = this.m_FBOs[var1];

         for(int var3 = 0; var3 < var2.m_zoomInfo.length; ++var3) {
            ZoomInfo var4 = var2.m_zoomInfo[var3];
            if (var4.renderChunk != null) {
               this.freeFBO(var4.renderChunk);
            }
         }
      }

      this.m_seamChunkE = this.m_seamChunkSE = this.m_seamChunkS = null;
      this.m_prevMinZ = 2147483647;
      this.m_prevMaxZ = -2147483648;
   }

   public void freeFBO(FBORenderChunk var1) {
      FBORenderChunkManager.instance.chunks.remove(var1.index);
      FBORenderChunkManager.instance.toRenderThisFrame.remove(var1);
      NLevels var2 = this.getNLevels(var1.getMinLevel());

      for(int var3 = 0; var3 < var2.m_zoomInfo.length; ++var3) {
         ZoomInfo var4 = var2.m_zoomInfo[var3];
         if (var4.renderChunk == var1) {
            var4.renderChunk = null;
            var4.dirty = true;
            var4.dirtyFlags = FBORenderChunk.DIRTY_NONE;
            break;
         }
      }

      var2.m_bOnScreen = false;
      if (this.hasNoRenderChunks()) {
         FBORenderChunkManager.instance.chunkFullMap.remove(this.m_chunk);
      }

      if (var1.tex != null) {
         FBORenderChunkManager.instance.addToStore(var1);
      }

   }

   public void freeFBOsForLevel(int var1) {
      NLevels var2 = this.getNLevels(var1);

      for(int var3 = 0; var3 < var2.m_zoomInfo.length; ++var3) {
         FBORenderChunk var4 = var2.m_zoomInfo[var3].renderChunk;
         if (var4 != null) {
            this.freeFBO(var4);
         }
      }

   }

   boolean hasNoRenderChunks() {
      for(int var1 = 0; var1 < this.m_FBOs.length; ++var1) {
         NLevels var2 = this.m_FBOs[var1];

         for(int var3 = 0; var3 < var2.m_zoomInfo.length; ++var3) {
            ZoomInfo var4 = var2.m_zoomInfo[var3];
            if (var4.renderChunk != null) {
               return false;
            }
         }
      }

      return true;
   }

   public void handleDelayedLoading(IsoObject var1) {
      this.getNLevels(var1.getSquare().getZ()).m_bDelayedLoading = true;
   }

   public boolean isDelayedLoading(int var1) {
      return this.getNLevels(var1).m_bDelayedLoading;
   }

   public void clearDelayedLoading(int var1) {
      this.getNLevels(var1).m_bDelayedLoading = false;
   }

   public void clearCache() {
      NLevels[] var1 = this.m_FBOs;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         NLevels var4 = var1[var3];

         for(int var5 = 0; var5 < var4.m_zoomInfo.length; ++var5) {
            ZoomInfo var6 = var4.m_zoomInfo[var5];
            this.clearCache(var6.renderChunk);
            var6.renderChunk = null;
         }
      }

   }

   private void clearCache(FBORenderChunk var1) {
      if (var1 != null) {
         if (var1.tex != null && var1.tex.getTextureId() != null) {
            FBORenderChunkManager.instance.addToStore(var1);
         }

      }
   }

   public static int calculateTextureWidthForLevels(int var0, int var1, float var2) {
      byte var3 = 8;
      int var6 = var1 - var0 + 1;
      float var7 = IsoUtils.XToScreen(0.0F, 0.0F, 0.0F, 0);
      float var8 = IsoUtils.XToScreen((float)var3, 0.0F, 0.0F, 0);
      float var9 = IsoUtils.XToScreen((float)var3, (float)var3, 0.0F, 0);
      float var10 = IsoUtils.XToScreen(0.0F, (float)var3, 0.0F, 0);
      float var4 = PZMath.min(var7, var8, var9, var10);
      float var5 = PZMath.max(var7, var8, var9, var10);
      var7 = IsoUtils.XToScreen(0.0F, 0.0F, (float)var6, 0);
      var8 = IsoUtils.XToScreen((float)var3, 0.0F, (float)var6, 0);
      var9 = IsoUtils.XToScreen((float)var3, (float)var3, (float)var6, 0);
      var10 = IsoUtils.XToScreen(0.0F, (float)var3, (float)var6, 0);
      var4 = PZMath.min(var4, var7, var8, var9, var10);
      var5 = PZMath.max(var5, var7, var8, var9, var10);
      int var11 = (int)Math.ceil((double)(var5 - var4));
      return ImageUtils.getNextPowerOfTwoHW(var11 * getTextureScale(var2));
   }

   public static int calculateTextureHeightForLevels(int var0, int var1, float var2) {
      byte var3 = 8;
      int var6 = var1 - var0 + 1;
      float var7 = IsoUtils.YToScreen(0.0F, 0.0F, 0.0F, 0);
      float var8 = IsoUtils.YToScreen((float)var3, 0.0F, 0.0F, 0);
      float var9 = IsoUtils.YToScreen((float)var3, (float)var3, 0.0F, 0);
      float var10 = IsoUtils.YToScreen(0.0F, (float)var3, 0.0F, 0);
      float var4 = PZMath.min(var7, var8, var9, var10);
      float var5 = PZMath.max(var7, var8, var9, var10);
      var7 = IsoUtils.YToScreen(0.0F, 0.0F, (float)var6, 0);
      var8 = IsoUtils.YToScreen((float)var3, 0.0F, (float)var6, 0);
      var9 = IsoUtils.YToScreen((float)var3, (float)var3, (float)var6, 0);
      var10 = IsoUtils.YToScreen(0.0F, (float)var3, (float)var6, 0);
      var4 = PZMath.min(var4, var7, var8, var9, var10);
      var5 = PZMath.max(var5, var7, var8, var9, var10);
      int var11 = (int)Math.ceil((double)(var5 - var4));
      var11 += extraHeightForJumboTrees(var0, var1);
      return ImageUtils.getNextPowerOfTwoHW(var11 * getTextureScale(var2));
   }

   public static int extraHeightForJumboTrees(int var0, int var1) {
      if (var0 != 0) {
         return 0;
      } else if (var1 > 1) {
         return 0;
      } else {
         return var1 == 1 ? FBORenderChunk.JUMBO_HEIGHT - FBORenderChunk.PIXELS_PER_LEVEL * 2 - FBORenderChunk.FLOOR_HEIGHT / 2 : FBORenderChunk.JUMBO_HEIGHT - FBORenderChunk.PIXELS_PER_LEVEL - FBORenderChunk.FLOOR_HEIGHT / 2;
      }
   }

   private static final class NLevels {
      final int m_minLevel;
      final ZoomInfo[] m_zoomInfo = new ZoomInfo[2];
      boolean m_bOnScreen;
      boolean m_bDelayedLoading = false;
      int m_renderedSquareCount = 0;
      final ArrayList<IsoGridSquare> m_animatedAttachments = new ArrayList();
      final ArrayList<IsoGridSquare> m_corpseSquares = new ArrayList();
      final ArrayList<IsoGridSquare> m_fliesSquares = new ArrayList();
      final ArrayList<IsoGridSquare> m_itemSquares = new ArrayList();
      final ArrayList<IsoGridSquare> m_puddleSquares = new ArrayList();
      final ArrayList<IsoGridSquare> m_translucentFloor = new ArrayList();
      final ArrayList<IsoGridSquare> m_translucentNonFloor = new ArrayList();

      NLevels(int var1) {
         this.m_minLevel = var1;

         for(int var2 = 0; var2 < this.m_zoomInfo.length; ++var2) {
            this.m_zoomInfo[var2] = new ZoomInfo();
         }

      }

      ZoomInfo getZoomInfo(float var1) {
         return this.m_zoomInfo[FBORenderLevels.getTextureScale(var1) - 1];
      }

      public boolean isDirty(float var1) {
         return this.getZoomInfo(var1).dirty;
      }

      public void setDirty(long var1) {
         for(int var3 = 0; var3 < this.m_zoomInfo.length; ++var3) {
            this.m_zoomInfo[var3].dirty = true;
            ZoomInfo var10000 = this.m_zoomInfo[var3];
            var10000.dirtyFlags |= var1;
         }

      }

      public void clearDirty(float var1) {
         ZoomInfo var2 = this.getZoomInfo(var1);
         var2.dirty = false;
         var2.dirtyFlags = FBORenderChunk.DIRTY_NONE;
      }

      public void clearDirty(long var1, float var3) {
         ZoomInfo var4 = this.getZoomInfo(var3);
         var4.dirtyFlags &= ~var1;
         var4.dirty = var4.dirtyFlags != FBORenderChunk.DIRTY_NONE;
      }

      void invalidate(long var1) {
         this.setDirty(var1);
         if (FBORenderLevels.bClearCachedSquares) {
            if (!FBORenderChunkManager.instance.isCaching()) {
               this.m_animatedAttachments.clear();
               this.m_corpseSquares.clear();
               this.m_fliesSquares.clear();
               this.m_itemSquares.clear();
               this.m_puddleSquares.clear();
               this.m_translucentFloor.clear();
               this.m_translucentNonFloor.clear();
            }
         }
      }
   }

   private static final class ZoomInfo {
      FBORenderChunk renderChunk;
      boolean dirty = false;
      long dirtyFlags;

      private ZoomInfo() {
         this.dirtyFlags = FBORenderChunk.DIRTY_NONE;
      }
   }
}
