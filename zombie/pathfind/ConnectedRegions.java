package zombie.pathfind;

import java.util.HashSet;
import zombie.core.utils.BooleanGrid;

final class ConnectedRegions {
   PolygonalMap2 map;
   HashSet<Chunk> doneChunks = new HashSet();
   int minX;
   int minY;
   int maxX;
   int maxY;
   int MINX;
   int MINY;
   int WIDTH;
   int HEIGHT;
   BooleanGrid visited;
   int[] stack;
   int stackLen;
   int[] choices;
   int choicesLen;

   ConnectedRegions() {
      this.visited = new BooleanGrid(this.WIDTH, this.WIDTH);
   }

   void findAdjacentChunks(int var1, int var2) {
      this.doneChunks.clear();
      this.minX = this.minY = 2147483647;
      this.maxX = this.maxY = -2147483648;
      Chunk var3 = this.map.getChunkFromSquarePos(var1, var2);
      this.findAdjacentChunks(var3);
   }

   void findAdjacentChunks(Chunk var1) {
      if (var1 != null && !this.doneChunks.contains(var1)) {
         this.minX = Math.min(this.minX, var1.wx);
         this.minY = Math.min(this.minY, var1.wy);
         this.maxX = Math.max(this.maxX, var1.wx);
         this.maxY = Math.max(this.maxY, var1.wy);
         this.doneChunks.add(var1);
         Chunk var2 = this.map.getChunkFromChunkPos(var1.wx - 1, var1.wy);
         Chunk var3 = this.map.getChunkFromChunkPos(var1.wx, var1.wy - 1);
         Chunk var4 = this.map.getChunkFromChunkPos(var1.wx + 1, var1.wy);
         Chunk var5 = this.map.getChunkFromChunkPos(var1.wx, var1.wy + 1);
         this.findAdjacentChunks(var2);
         this.findAdjacentChunks(var3);
         this.findAdjacentChunks(var4);
         this.findAdjacentChunks(var5);
      }
   }

   void floodFill(int var1, int var2) {
      this.findAdjacentChunks(var1, var2);
      this.MINX = this.minX * 8;
      this.MINY = this.minY * 8;
      this.WIDTH = (this.maxX - this.minX + 1) * 8;
      this.HEIGHT = (this.maxY - this.minY + 1) * 8;
      this.visited = new BooleanGrid(this.WIDTH, this.WIDTH);
      this.stack = new int[this.WIDTH * this.WIDTH];
      this.choices = new int[this.WIDTH * this.HEIGHT];
      this.stackLen = 0;
      this.choicesLen = 0;
      if (this.push(var1, var2)) {
         int var3;
         label81:
         while((var3 = this.pop()) != -1) {
            int var4 = this.MINX + (var3 & '\uffff');

            int var5;
            for(var5 = this.MINY + (var3 >> 16) & '\uffff'; this.shouldVisit(var4, var5, var4, var5 - 1); --var5) {
            }

            boolean var6 = false;
            boolean var7 = false;

            while(this.visit(var4, var5)) {
               if (!var6 && this.shouldVisit(var4, var5, var4 - 1, var5)) {
                  if (!this.push(var4 - 1, var5)) {
                     return;
                  }

                  var6 = true;
               } else if (var6 && !this.shouldVisit(var4, var5, var4 - 1, var5)) {
                  var6 = false;
               } else if (var6 && !this.shouldVisit(var4 - 1, var5, var4 - 1, var5 - 1) && !this.push(var4 - 1, var5)) {
                  return;
               }

               if (!var7 && this.shouldVisit(var4, var5, var4 + 1, var5)) {
                  if (!this.push(var4 + 1, var5)) {
                     return;
                  }

                  var7 = true;
               } else if (var7 && !this.shouldVisit(var4, var5, var4 + 1, var5)) {
                  var7 = false;
               } else if (var7 && !this.shouldVisit(var4 + 1, var5, var4 + 1, var5 - 1) && !this.push(var4 + 1, var5)) {
                  return;
               }

               ++var5;
               if (!this.shouldVisit(var4, var5 - 1, var4, var5)) {
                  continue label81;
               }
            }

            return;
         }

         System.out.println("#choices=" + this.choicesLen);
      }
   }

   boolean shouldVisit(int var1, int var2, int var3, int var4) {
      if (var3 < this.MINX + this.WIDTH && var3 >= this.MINX) {
         if (var4 < this.MINY + this.WIDTH && var4 >= this.MINY) {
            if (this.visited.getValue(this.gridX(var3), this.gridY(var4))) {
               return false;
            } else {
               Square var5 = PolygonalMap2.instance.getSquare(var1, var2, 0);
               Square var6 = PolygonalMap2.instance.getSquare(var3, var4, 0);
               if (var5 != null && var6 != null) {
                  return !this.isBlocked(var5, var6, false);
               } else {
                  return false;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   boolean visit(int var1, int var2) {
      if (this.choicesLen >= this.WIDTH * this.WIDTH) {
         return false;
      } else {
         this.choices[this.choicesLen++] = this.gridY(var2) << 16 | (short)this.gridX(var1);
         this.visited.setValue(this.gridX(var1), this.gridY(var2), true);
         return true;
      }
   }

   boolean push(int var1, int var2) {
      if (this.stackLen >= this.WIDTH * this.WIDTH) {
         return false;
      } else {
         this.stack[this.stackLen++] = this.gridY(var2) << 16 | (short)this.gridX(var1);
         return true;
      }
   }

   int pop() {
      return this.stackLen == 0 ? -1 : this.stack[--this.stackLen];
   }

   int gridX(int var1) {
      return var1 - this.MINX;
   }

   int gridY(int var1) {
      return var1 - this.MINY;
   }

   boolean isBlocked(Square var1, Square var2, boolean var3) {
      assert Math.abs(var1.x - var2.x) <= 1;

      assert Math.abs(var1.y - var2.y) <= 1;

      assert var1.z == var2.z;

      assert var1 != var2;

      boolean var4 = var2.x < var1.x;
      boolean var5 = var2.x > var1.x;
      boolean var6 = var2.y < var1.y;
      boolean var7 = var2.y > var1.y;
      if (var2.isReallySolid()) {
         return true;
      } else if (var2.y < var1.y && var1.has(64)) {
         return true;
      } else if (var2.x < var1.x && var1.has(8)) {
         return true;
      } else if (var2.y > var1.y && var2.x == var1.x && var2.has(64)) {
         return true;
      } else if (var2.x > var1.x && var2.y == var1.y && var2.has(8)) {
         return true;
      } else if (var2.x != var1.x && var2.has(448)) {
         return true;
      } else if (var2.y != var1.y && var2.has(56)) {
         return true;
      } else if (var2.x != var1.x && var1.has(448)) {
         return true;
      } else if (var2.y != var1.y && var1.has(56)) {
         return true;
      } else if (!var2.has(512) && !var2.has(504)) {
         return true;
      } else {
         boolean var8 = var6 && var1.has(4) && (var1.x != var2.x || var3 || !var1.has(16384));
         boolean var9 = var4 && var1.has(2) && (var1.y != var2.y || var3 || !var1.has(8192));
         boolean var10 = var7 && var2.has(4) && (var1.x != var2.x || var3 || !var2.has(16384));
         boolean var11 = var5 && var2.has(2) && (var1.y != var2.y || var3 || !var2.has(8192));
         if (!var8 && !var9 && !var10 && !var11) {
            boolean var12 = var2.x != var1.x && var2.y != var1.y;
            if (var12) {
               Square var13 = PolygonalMap2.instance.getSquare(var1.x, var2.y, var1.z);
               Square var14 = PolygonalMap2.instance.getSquare(var2.x, var1.y, var1.z);

               assert var13 != var1 && var13 != var2;

               assert var14 != var1 && var14 != var2;

               if (var2.x == var1.x + 1 && var2.y == var1.y + 1 && var13 != null && var14 != null && var13.has(4096) && var14.has(2048)) {
                  return true;
               } else if (var2.x == var1.x - 1 && var2.y == var1.y - 1 && var13 != null && var14 != null && var13.has(2048) && var14.has(4096)) {
                  return true;
               } else if (var13 != null && this.isBlocked(var1, var13, true)) {
                  return true;
               } else if (var14 != null && this.isBlocked(var1, var14, true)) {
                  return true;
               } else if (var13 != null && this.isBlocked(var2, var13, true)) {
                  return true;
               } else if (var14 != null && this.isBlocked(var2, var14, true)) {
                  return true;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return true;
         }
      }
   }
}
