package zombie.pathfind.highLevel;

import java.util.ArrayList;
import zombie.debug.LineDrawer;
import zombie.iso.IsoDirections;
import zombie.pathfind.Chunk;
import zombie.pathfind.ChunkLevel;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;

public final class HLChunkLevel {
   static final int CPW = 8;
   public int m_modificationCount = -1;
   int m_modificationCount_Stairs = -1;
   final ChunkLevel m_chunkLevel;
   final ArrayList<HLChunkRegion> m_regionList = new ArrayList();
   final ArrayList<HLStaircase> m_stairs = new ArrayList();
   final ArrayList<HLSlopedSurface> m_slopedSurfaces = new ArrayList();

   public HLChunkLevel(ChunkLevel var1) {
      this.m_chunkLevel = var1;
   }

   Chunk getChunk() {
      return this.m_chunkLevel.getChunk();
   }

   int getLevel() {
      return this.m_chunkLevel.getLevel();
   }

   public void initRegions() {
      this.releaseRegions();
      Square[][] var1 = this.getChunk().getSquaresForLevel(this.getLevel());

      for(int var2 = 0; var2 < 8; ++var2) {
         for(int var3 = 0; var3 < 8; ++var3) {
            Square var4 = var1[var3][var2];
            if (this.canWalkOnSquare(var4) && !this.isSquareInRegion(var4.getX(), var4.getY()) && (PolygonalMap2.instance.getVisGraphAt((float)var4.getX() + 0.5F, (float)var4.getY() + 0.5F, var4.getZ(), 1) == null || var4.has(504) || var4.hasSlopedSurface())) {
               HLChunkRegion var5 = (HLChunkRegion)HLGlobals.chunkRegionPool.alloc();
               var5.m_levelData = this;
               var5.m_squaresMask.clear();
               this.floodFill(var5, var1, var4);
               var5.initEdges();
               this.m_regionList.add(var5);
            }
         }
      }

      if (this.m_regionList.size() > 0) {
      }

   }

   void releaseRegions() {
      HLGlobals.chunkRegionPool.releaseAll(this.m_regionList);
      this.m_regionList.clear();
   }

   void initStairsIfNeeded() {
      if (HLAStar.ModificationCount != this.m_modificationCount_Stairs) {
         this.m_modificationCount_Stairs = HLAStar.ModificationCount;
         this.initStairs();
      }
   }

   void initStairs() {
      HLAStar.PerfInitStairs.invokeAndMeasure(this, HLChunkLevel::initStairsInternal);
   }

   void initStairsInternal() {
      this.releaseStairs();
      Square[][] var1 = this.getChunk().getSquaresForLevel(this.getLevel());

      for(int var2 = 0; var2 < 8; ++var2) {
         for(int var3 = 0; var3 < 8; ++var3) {
            Square var4 = var1[var3][var2];
            if (var4 != null) {
               HLStaircase var5;
               if (var4.has(64)) {
                  var5 = (HLStaircase)HLGlobals.staircasePool.alloc();
                  var5.set(IsoDirections.N, var4.getX(), var4.getY(), var4.getZ());
                  this.m_stairs.add(var5);
               }

               if (var4.has(8)) {
                  var5 = (HLStaircase)HLGlobals.staircasePool.alloc();
                  var5.set(IsoDirections.W, var4.getX(), var4.getY(), var4.getZ());
                  this.m_stairs.add(var5);
               }
            }
         }
      }

      this.initSlopedSurfaces();
   }

   void releaseStairs() {
      HLGlobals.staircasePool.releaseAll(this.m_stairs);
      this.m_stairs.clear();
   }

   HLStaircase getStaircaseAt(int var1, int var2) {
      for(int var3 = 0; var3 < this.m_stairs.size(); ++var3) {
         HLStaircase var4 = (HLStaircase)this.m_stairs.get(var3);
         if (var4.isBottomFloorAt(var1, var2)) {
            return var4;
         }
      }

      return null;
   }

   void initSlopedSurfaces() {
      this.releaseSlopedSurfaces();
      Square[][] var1 = this.getChunk().getSquaresForLevel(this.getLevel());

      for(int var2 = 0; var2 < 8; ++var2) {
         for(int var3 = 0; var3 < 8; ++var3) {
            Square var4 = var1[var3][var2];
            if (var4 != null) {
               IsoDirections var5 = var4.getSlopedSurfaceDirection();
               if (var5 != null && !(var4.getSlopedSurfaceHeightMax() < 1.0F)) {
                  HLSlopedSurface var6 = (HLSlopedSurface)HLGlobals.slopedSurfacePool.alloc();
                  var6.set(var5, var4.getX(), var4.getY(), var4.getZ());
                  this.m_slopedSurfaces.add(var6);
               }
            }
         }
      }

   }

   void releaseSlopedSurfaces() {
      HLGlobals.slopedSurfacePool.releaseAll(this.m_slopedSurfaces);
      this.m_slopedSurfaces.clear();
   }

   HLSlopedSurface getSlopedSurfaceAt(int var1, int var2) {
      for(int var3 = 0; var3 < this.m_slopedSurfaces.size(); ++var3) {
         HLSlopedSurface var4 = (HLSlopedSurface)this.m_slopedSurfaces.get(var3);
         if (var4.isBottomFloorAt(var1, var2)) {
            return var4;
         }
      }

      return null;
   }

   HLLevelTransition getLevelTransitionAt(int var1, int var2) {
      Object var3 = this.getStaircaseAt(var1, var2);
      if (var3 == null) {
         var3 = this.getSlopedSurfaceAt(var1, var2);
      }

      return (HLLevelTransition)var3;
   }

   public void removeFromWorld() {
      this.releaseRegions();
      this.releaseStairs();
      this.releaseSlopedSurfaces();
      this.m_modificationCount = -1;
      this.m_modificationCount_Stairs = -1;
   }

   HLChunkRegion findRegionContainingSquare(int var1, int var2) {
      for(int var3 = 0; var3 < this.m_regionList.size(); ++var3) {
         HLChunkRegion var4 = (HLChunkRegion)this.m_regionList.get(var3);
         if (var4.containsSquare(var1, var2)) {
            return var4;
         }
      }

      return null;
   }

   boolean isSquareInRegion(int var1, int var2) {
      return this.findRegionContainingSquare(var1, var2) != null;
   }

   boolean canWalkOnSquare(Square var1) {
      if (var1 == null) {
         return false;
      } else if (!var1.TreatAsSolidFloor()) {
         return false;
      } else {
         return !var1.isReallySolid();
      }
   }

   boolean isCanPathTransition(Square var1, Square var2) {
      if (var1 != null && var2 != null && var1 != var2 && var1.getZ() == var2.getZ()) {
         int var3 = var2.getX() - var1.getX();
         int var4 = var2.getY() - var1.getY();
         if (var3 < 0) {
            if (var1.has(8192)) {
               return true;
            }
         } else if (var3 > 0 && var2.has(8192)) {
            return true;
         }

         if (var4 < 0) {
            if (var1.has(16384)) {
               return true;
            }
         } else if (var4 > 0 && var2.has(16384)) {
            return true;
         }

         return false;
      } else {
         return false;
      }
   }

   void floodFill(HLChunkRegion var1, Square[][] var2, Square var3) {
      HLGlobals.floodFill.reset();
      HLGlobals.floodFill.calculate(var1, var2, var3);
   }

   void renderDebug() {
      int var1;
      for(var1 = 0; var1 < this.m_regionList.size(); ++var1) {
         HLChunkRegion var2 = (HLChunkRegion)this.m_regionList.get(var1);
         var2.renderDebug();
      }

      this.initStairsIfNeeded();

      for(var1 = 0; var1 < this.m_stairs.size(); ++var1) {
         HLStaircase var3 = (HLStaircase)this.m_stairs.get(var1);
         LineDrawer.addLine((float)var3.getBottomFloorX() + 0.5F, (float)var3.getBottomFloorY() + 0.5F, (float)(var3.getBottomFloorZ() - 32), (float)var3.getTopFloorX() + 0.5F, (float)var3.getTopFloorY() + 0.5F, (float)(var3.getTopFloorZ() - 32), 1.0F, 1.0F, 1.0F, 1.0F);
      }

   }
}
