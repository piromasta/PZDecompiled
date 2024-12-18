package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;

final class ChunkUpdateTask implements IChunkTask {
   PolygonalMap2 map;
   int wx;
   int wy;
   ChunkUpdateTaskLevel[] levels = new ChunkUpdateTaskLevel[1];
   int minLevel = 32;
   int maxLevel = 32;
   static final ArrayDeque<ChunkUpdateTask> pool = new ArrayDeque();

   ChunkUpdateTask() {
      this.levels[0] = new ChunkUpdateTaskLevel();
   }

   void setMinMaxLevel(int var1, int var2) {
      if (var1 != this.minLevel || var2 != this.maxLevel) {
         for(int var3 = this.minLevel; var3 <= this.maxLevel; ++var3) {
            if (var3 < var1 || var3 > var2) {
               ChunkUpdateTaskLevel var4 = this.levels[var3 - this.minLevel];
               if (var4 != null) {
                  var4.release();
                  this.levels[var3 - this.minLevel] = null;
               }
            }
         }

         ChunkUpdateTaskLevel[] var5 = new ChunkUpdateTaskLevel[var2 - var1 + 1];

         for(int var6 = var1; var6 <= var2; ++var6) {
            if (var6 >= this.minLevel && var6 <= this.maxLevel) {
               var5[var6 - var1] = this.levels[var6 - this.minLevel];
            } else {
               var5[var6 - var1] = ChunkUpdateTaskLevel.alloc();
            }
         }

         this.minLevel = var1;
         this.maxLevel = var2;
         this.levels = var5;
      }
   }

   ChunkUpdateTask init(PolygonalMap2 var1, IsoChunk var2) {
      this.map = var1;
      this.wx = var2.wx;
      this.wy = var2.wy;
      this.setMinMaxLevel(var2.minLevel + 32, var2.maxLevel + 32);

      for(int var3 = this.minLevel; var3 <= this.maxLevel; ++var3) {
         ChunkUpdateTaskLevel var4 = this.levels[var3 - this.minLevel];
         SlopedSurface.pool.releaseAll(var4.slopedSurfaces);
         var4.slopedSurfaces.clear();

         for(int var5 = 0; var5 < 8; ++var5) {
            for(int var6 = 0; var6 < 8; ++var6) {
               IsoGridSquare var7 = var2.getGridSquare(var6, var5, var3 - 32);
               if (var7 == null) {
                  var4.data[var6][var5] = 0;
                  var4.cost[var6][var5] = 0;
               } else {
                  var4.data[var6][var5] = SquareUpdateTask.getBits(var7);
                  var4.cost[var6][var5] = SquareUpdateTask.getCost(var7);
                  if (var7.hasSlopedSurface()) {
                     SlopedSurface var8 = SlopedSurface.alloc();
                     var8.x = (byte)var6;
                     var8.y = (byte)var5;
                     var8.direction = var7.getSlopedSurfaceDirection();
                     var8.heightMin = var7.getSlopedSurfaceHeightMin();
                     var8.heightMax = var7.getSlopedSurfaceHeightMax();
                     var4.slopedSurfaces.add(var8);
                  }
               }
            }
         }
      }

      return this;
   }

   public void execute() {
      Chunk var1 = this.map.allocChunkIfNeeded(this.wx, this.wy);
      var1.setData(this);
      ++ChunkDataZ.EPOCH;
   }

   static ChunkUpdateTask alloc() {
      synchronized(pool) {
         return pool.isEmpty() ? new ChunkUpdateTask() : (ChunkUpdateTask)pool.pop();
      }
   }

   public void release() {
      synchronized(pool) {
         assert !pool.contains(this);

         pool.push(this);
      }
   }
}
