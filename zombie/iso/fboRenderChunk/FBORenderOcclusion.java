package zombie.iso.fboRenderChunk;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.vehicles.Clipper;

public final class FBORenderOcclusion {
   private static FBORenderOcclusion instance = null;
   boolean bEnabled = true;
   int occludedGridX1;
   int occludedGridY1;
   int occludedGridX2;
   int occludedGridY2;
   int[] occludedGrid;
   final int[][] occludingChunkLevels = new int[64][];
   final TFloatArrayList floatArrayList1 = new TFloatArrayList();
   final TFloatArrayList floatArrayList2 = new TFloatArrayList();
   Clipper s_clipper = null;
   ByteBuffer s_clipperBuffer = null;
   int testValue;

   public static FBORenderOcclusion getInstance() {
      if (instance == null) {
         instance = new FBORenderOcclusion();
      }

      return instance;
   }

   private FBORenderOcclusion() {
   }

   public void init() {
      if (this.occludingChunkLevels[0] == null) {
         if (this.s_clipper == null) {
            this.s_clipper = new Clipper();
         }

         byte var1 = 0;
         byte var2 = 0;
         byte var3 = 31;
         this.createChunkPolygon(var1, var2, var3, this.floatArrayList1);
         byte var4 = 8;
         float var5 = IsoUtils.XToScreen((float)(var1 * var4 - 3), (float)(var2 * var4 - 3), (float)var3, 0);
         float var6 = IsoUtils.YToScreen((float)(var1 * var4 - 3), (float)(var2 * var4 - 3), (float)var3, 0);
         float var7 = IsoUtils.XToScreen((float)((var1 + 1) * var4 + 3), (float)((var2 + 1) * var4 + 3), (float)var3, 0);
         float var8 = IsoUtils.YToScreen((float)((var1 + 1) * var4 + 3), (float)((var2 + 1) * var4 + 3), (float)var3, 0);
         TIntArrayList var9 = new TIntArrayList();

         for(int var10 = var3 - 1; var10 >= -32; --var10) {
            var9.clear();
            int var11 = PZMath.fastfloor(this.XToIso(var5, var6, (float)var10) / (float)var4);
            int var12 = PZMath.fastfloor(this.YToIso(var5, var6, (float)var10) / (float)var4);
            int var13 = PZMath.fastfloor(this.XToIso(var7, var8, (float)var10) / (float)var4);
            int var14 = PZMath.fastfloor(this.YToIso(var7, var8, (float)var10) / (float)var4);

            for(int var15 = var12; var15 <= var14; ++var15) {
               for(int var16 = var11; var16 <= var13; ++var16) {
                  this.createChunkPolygon(var16, var15, var10, this.floatArrayList2);
                  if (this.polygonsOverlap(this.floatArrayList1, this.floatArrayList2)) {
                     var9.add(var16);
                     var9.add(var15);
                  }
               }
            }

            this.occludingChunkLevels[var10 + 32] = var9.toArray();
         }

      }
   }

   float XToIso(float var1, float var2, float var3) {
      float var4 = (var1 + 2.0F * var2) / (64.0F * (float)Core.TileScale);
      var4 += 3.0F * var3;
      return var4;
   }

   float YToIso(float var1, float var2, float var3) {
      float var4 = (var1 - 2.0F * var2) / (-64.0F * (float)Core.TileScale);
      var4 += 3.0F * var3;
      return var4;
   }

   public void invalidateOverlappedChunkLevels(int var1, IsoChunk var2, int var3) {
      IsoChunkMap var4 = IsoWorld.instance.getCell().getChunkMap(var1);
      int var5 = 62;

      for(int var6 = var3 - 1; var6 >= var4.minHeight; --var6) {
         int[] var7 = this.occludingChunkLevels[var5];
         --var5;

         for(int var8 = 0; var8 < var7.length; var8 += 2) {
            int var9 = var7[var8];
            int var10 = var7[var8 + 1];
            IsoChunk var11 = var4.getChunk(var2.wx - var4.getWorldXMin() + var9, var2.wy - var4.getWorldYMin() + var10);
            if (var11 != null && var6 >= var11.getMinLevel() && var6 <= var11.getMaxLevel()) {
               FBORenderLevels var12 = var11.getRenderLevels(var1);
               var12.invalidateLevel(var6, FBORenderChunk.DIRTY_REDRAW);
            }
         }
      }

   }

   boolean polygonsOverlap(TFloatArrayList var1, TFloatArrayList var2) {
      this.s_clipper.clear();
      this.addPolygon(var1, false);
      this.addPolygon(var2, true);
      int var3 = this.s_clipper.generatePolygons(0, 0.0, 0);
      if (var3 == 0) {
         return false;
      } else {
         for(int var4 = 0; var4 < var3; ++var4) {
            this.s_clipperBuffer.clear();
            this.s_clipper.getPolygon(var4, this.s_clipperBuffer);
            short var5 = this.s_clipperBuffer.getShort();
            if (var5 >= 3) {
               return true;
            }
         }

         return false;
      }
   }

   void addPolygon(TFloatArrayList var1, boolean var2) {
      if (this.s_clipperBuffer == null || this.s_clipperBuffer.capacity() < var1.size() * 8 * 4) {
         this.s_clipperBuffer = ByteBuffer.allocateDirect(var1.size() * 8 * 4);
      }

      this.s_clipperBuffer.clear();
      int var3;
      if (this.isClockwise(var1)) {
         for(var3 = this.numPoints(var1) - 1; var3 >= 0; --var3) {
            this.s_clipperBuffer.putFloat(this.getX(var1, var3));
            this.s_clipperBuffer.putFloat(this.getY(var1, var3));
         }
      } else {
         for(var3 = 0; var3 < this.numPoints(var1); ++var3) {
            this.s_clipperBuffer.putFloat(this.getX(var1, var3));
            this.s_clipperBuffer.putFloat(this.getY(var1, var3));
         }
      }

      this.s_clipper.addPath(this.numPoints(var1), this.s_clipperBuffer, var2);
   }

   void createChunkPolygon(int var1, int var2, int var3, TFloatArrayList var4) {
      this.createPolygon(var1 * 8, var2 * 8, var3, 8, var4);
   }

   void createPolygon(int var1, int var2, int var3, int var4, TFloatArrayList var5) {
      var5.clear();
      var5.add(IsoUtils.XToScreen((float)var1, (float)(var2 + var4), (float)var3, 0));
      var5.add(IsoUtils.YToScreen((float)var1, (float)(var2 + var4), (float)var3, 0));
      var5.add(IsoUtils.XToScreen((float)var1, (float)(var2 + var4), (float)(var3 + 1), 0));
      var5.add(IsoUtils.YToScreen((float)var1, (float)(var2 + var4), (float)(var3 + 1), 0));
      var5.add(IsoUtils.XToScreen((float)var1, (float)var2, (float)(var3 + 1), 0));
      var5.add(IsoUtils.YToScreen((float)var1, (float)var2, (float)(var3 + 1), 0));
      var5.add(IsoUtils.XToScreen((float)(var1 + var4), (float)var2, (float)(var3 + 1), 0));
      var5.add(IsoUtils.YToScreen((float)(var1 + var4), (float)var2, (float)(var3 + 1), 0));
      var5.add(IsoUtils.XToScreen((float)(var1 + var4), (float)var2, (float)var3, 0));
      var5.add(IsoUtils.YToScreen((float)(var1 + var4), (float)var2, (float)var3, 0));
      var5.add(IsoUtils.XToScreen((float)(var1 + var4), (float)(var2 + var4), (float)var3, 0));
      var5.add(IsoUtils.YToScreen((float)(var1 + var4), (float)(var2 + var4), (float)var3, 0));
   }

   boolean isClockwise(TFloatArrayList var1) {
      float var2 = 0.0F;

      for(int var3 = 0; var3 < this.numPoints(var1); ++var3) {
         float var4 = this.getX(var1, var3);
         float var5 = this.getY(var1, var3);
         float var6 = this.getX(var1, (var3 + 1) % this.numPoints(var1));
         float var7 = this.getY(var1, (var3 + 1) % this.numPoints(var1));
         var2 += (var6 - var4) * (var7 + var5);
      }

      return (double)var2 > 0.0;
   }

   int numPoints(TFloatArrayList var1) {
      return var1.size() / 2;
   }

   float getX(TFloatArrayList var1, int var2) {
      return var1.get(var2 * 2);
   }

   float getY(TFloatArrayList var1, int var2) {
      return var1.get(var2 * 2 + 1);
   }

   public void setFloorOccluded(int var1, int var2, int var3) {
   }

   public void setNorthWallOccluded(int var1, int var2, int var3) {
   }

   public void setWestWallOccluded(int var1, int var2, int var3) {
   }

   public boolean isOccluded(int var1, int var2, int var3) {
      if (!this.bEnabled) {
         return false;
      } else {
         this.testValue = var3;
         if (!this.isOccludedAux(var1, var2, var3)) {
            return false;
         } else if (!this.isOccludedAux(var1 - 1, var2 - 1, var3)) {
            return false;
         } else if (!this.isOccludedAux(var1 - 2, var2 - 2, var3)) {
            return false;
         } else if (!this.isOccludedAux(var1 - 3, var2 - 3, var3)) {
            return false;
         } else if (!this.isOccludedAux(var1 - 1, var2 - 0, var3)) {
            return false;
         } else if (!this.isOccludedAux(var1 - 0, var2 - 1, var3)) {
            return false;
         } else if (!this.isOccludedAux(var1 - 2, var2 - 1, var3)) {
            return false;
         } else if (!this.isOccludedAux(var1 - 1, var2 - 2, var3)) {
            return false;
         } else if (!this.isOccludedAux(var1 - 3, var2 - 2, var3)) {
            return false;
         } else {
            return this.isOccludedAux(var1 - 2, var2 - 3, var3);
         }
      }
   }

   boolean isOccludedAux(int var1, int var2, int var3) {
      int var4 = var1 - var3 * 3 - this.occludedGridX1;
      int var5 = var2 - var3 * 3 - this.occludedGridY1;
      if (var4 >= 0 && var5 >= 0 && var4 <= this.occludedGridX2 - this.occludedGridX1 && var5 <= this.occludedGridY2 - this.occludedGridY1) {
         int var6 = var4 + var5 * (this.occludedGridX2 - this.occludedGridX1 + 1);
         int var7 = this.occludedGrid[var6];
         return var7 > this.testValue;
      } else {
         return false;
      }
   }

   public void removeChunkFromWorld(IsoChunk var1) {
      if (this.bEnabled) {
         for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
            for(int var3 = var1.minLevel; var3 <= var1.maxLevel; ++var3) {
               this.invalidateOverlappedChunkLevels(var2, var1, var3);
            }
         }

      }
   }
}
