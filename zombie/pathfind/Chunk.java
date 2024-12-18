package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;

public final class Chunk {
   public short wx;
   public short wy;
   ChunkLevel[] levels = new ChunkLevel[1];
   final ChunkData collision = new ChunkData();
   int minLevel = 32;
   int maxLevel = 32;
   final ArrayList<VisibilityGraph> visibilityGraphs = new ArrayList();
   static final ArrayDeque<Chunk> pool = new ArrayDeque();

   Chunk() {
      this.levels[0] = ChunkLevel.alloc().init(this, this.minLevel);
   }

   Chunk init(int var1, int var2) {
      this.wx = (short)var1;
      this.wy = (short)var2;
      return this;
   }

   void clear() {
      for(int var1 = this.minLevel; var1 <= this.maxLevel; ++var1) {
         ChunkLevel var2 = this.levels[var1 - this.minLevel];
         if (var2 != null) {
            var2.clear();
            var2.release();
            this.levels[var1 - this.minLevel] = null;
         }
      }

      this.wx = this.wy = -1;
      this.levels = new ChunkLevel[1];
      this.minLevel = this.maxLevel = 32;
      this.levels[0] = ChunkLevel.alloc().init(this, this.minLevel);
   }

   public int getMinX() {
      return this.wx * 8;
   }

   public int getMinY() {
      return this.wy * 8;
   }

   public int getMaxX() {
      return (this.wx + 1) * 8 - 1;
   }

   public int getMaxY() {
      return (this.wy + 1) * 8 - 1;
   }

   public int getMinLevel() {
      return this.minLevel;
   }

   public int getMaxLevel() {
      return this.maxLevel;
   }

   public boolean isValidLevel(int var1) {
      return var1 >= this.getMinLevel() && var1 <= this.getMaxLevel();
   }

   public boolean contains(int var1, int var2) {
      return var1 >= this.getMinX() && var2 >= this.getMinY() && var1 <= this.getMaxX() && var2 <= this.getMaxY();
   }

   void setMinMaxLevel(int var1, int var2) {
      if (var1 != this.minLevel || var2 != this.maxLevel) {
         for(int var3 = this.minLevel; var3 <= this.maxLevel; ++var3) {
            if (var3 < var1 || var3 > var2) {
               ChunkLevel var4 = this.levels[var3 - this.minLevel];
               if (var4 != null) {
                  var4.clear();
                  var4.release();
                  this.levels[var3 - this.minLevel] = null;
               }
            }
         }

         ChunkLevel[] var5 = new ChunkLevel[var2 - var1 + 1];

         for(int var6 = var1; var6 <= var2; ++var6) {
            if (this.isValidLevel(var6)) {
               var5[var6 - var1] = this.levels[var6 - this.minLevel];
            } else {
               var5[var6 - var1] = ChunkLevel.alloc().init(this, var6);
            }
         }

         this.minLevel = var1;
         this.maxLevel = var2;
         this.levels = var5;
      }
   }

   public ChunkLevel getLevelData(int var1) {
      return this.isValidLevel(var1) ? this.levels[var1 - this.minLevel] : null;
   }

   Square getSquare(int var1, int var2, int var3) {
      ChunkLevel var4 = this.getLevelData(var3);
      return var4 == null ? null : var4.getSquare(var1, var2);
   }

   public Square[][] getSquaresForLevel(int var1) {
      ChunkLevel var2 = this.getLevelData(var1);
      return var2 == null ? null : var2.squares;
   }

   void setData(ChunkUpdateTask var1) {
      this.setMinMaxLevel(var1.minLevel, var1.maxLevel);

      for(int var2 = this.minLevel; var2 <= this.maxLevel; ++var2) {
         ChunkUpdateTaskLevel var3 = var1.levels[var2 - this.minLevel];
         ChunkLevel var4 = this.getLevelData(var2);

         int var5;
         for(var5 = 0; var5 < 8; ++var5) {
            for(int var6 = 0; var6 < 8; ++var6) {
               int var7 = var3.data[var6][var5];
               short var8 = var3.cost[var6][var5];
               var4.setBits(var6, var5, var7, var8);
               Square var9 = var4.squares[var6][var5];
               if (var9 != null) {
                  var9.slopedSurfaceDirection = null;
                  var9.slopedSurfaceHeightMin = 0.0F;
                  var9.slopedSurfaceHeightMax = 0.0F;
               }
            }
         }

         for(var5 = 0; var5 < var3.slopedSurfaces.size(); ++var5) {
            SlopedSurface var10 = (SlopedSurface)var3.slopedSurfaces.get(var5);
            Square var11 = var4.squares[var10.x][var10.y];
            var11.slopedSurfaceDirection = var10.direction;
            var11.slopedSurfaceHeightMin = var10.heightMin;
            var11.slopedSurfaceHeightMax = var10.heightMax;
         }
      }

   }

   boolean setData(SquareUpdateTask var1) {
      int var2 = var1.x - this.wx * 8;
      int var3 = var1.y - this.wy * 8;
      if (var2 >= 0 && var2 < 8) {
         if (var3 >= 0 && var3 < 8) {
            Square[][] var4 = this.getSquaresForLevel(var1.z);
            Square var5 = var4[var2][var3];
            if (var1.bits == 0) {
               if (var5 != null) {
                  var5.release();
                  var4[var2][var3] = null;
                  return true;
               }
            } else {
               if (var5 == null) {
                  var5 = Square.alloc().init(var1.x, var1.y, var1.z);
                  var4[var2][var3] = var5;
               }

               boolean var6 = var5.bits != var1.bits || var5.cost != var1.cost;
               if (var1.slopedSurface == null) {
                  var6 |= var5.slopedSurfaceDirection != null || var5.slopedSurfaceHeightMin != 0.0F || var5.slopedSurfaceHeightMax != 0.0F;
                  var5.slopedSurfaceDirection = null;
                  var5.slopedSurfaceHeightMin = 0.0F;
                  var5.slopedSurfaceHeightMax = 0.0F;
               } else {
                  var6 |= var5.slopedSurfaceDirection != var1.slopedSurface.direction || var5.slopedSurfaceHeightMin != var1.slopedSurface.heightMin || var5.slopedSurfaceHeightMax != var1.slopedSurface.heightMax;
                  var5.slopedSurfaceDirection = var1.slopedSurface.direction;
                  var5.slopedSurfaceHeightMin = var1.slopedSurface.heightMin;
                  var5.slopedSurfaceHeightMax = var1.slopedSurface.heightMax;
               }

               if (var6) {
                  var5.bits = var1.bits;
                  var5.cost = var1.cost;
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   void addVisibilityGraph(VisibilityGraph var1) {
      if (!this.visibilityGraphs.contains(var1)) {
         this.visibilityGraphs.add(var1);
      }
   }

   void removeVisibilityGraph(VisibilityGraph var1) {
      this.visibilityGraphs.remove(var1);
   }

   static Chunk alloc() {
      return pool.isEmpty() ? new Chunk() : (Chunk)pool.pop();
   }

   void release() {
      assert !pool.contains(this);

      pool.push(this);
   }
}
