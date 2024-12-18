package zombie.pathfind;

import java.util.ArrayDeque;

final class Cell {
   PolygonalMap2 map;
   public short cx;
   public short cy;
   public Chunk[][] chunks;
   static final ArrayDeque<Cell> pool = new ArrayDeque();

   Cell() {
   }

   Cell init(PolygonalMap2 var1, int var2, int var3) {
      this.map = var1;
      this.cx = (short)var2;
      this.cy = (short)var3;
      return this;
   }

   Chunk getChunkFromChunkPos(int var1, int var2) {
      if (this.chunks == null) {
         return null;
      } else {
         var1 -= this.cx * PolygonalMap2.CHUNKS_PER_CELL;
         var2 -= this.cy * PolygonalMap2.CHUNKS_PER_CELL;
         return var1 >= 0 && var1 < PolygonalMap2.CHUNKS_PER_CELL && var2 >= 0 && var2 < PolygonalMap2.CHUNKS_PER_CELL ? this.chunks[var1][var2] : null;
      }
   }

   Chunk allocChunkIfNeeded(int var1, int var2) {
      var1 -= this.cx * PolygonalMap2.CHUNKS_PER_CELL;
      var2 -= this.cy * PolygonalMap2.CHUNKS_PER_CELL;
      if (var1 >= 0 && var1 < PolygonalMap2.CHUNKS_PER_CELL && var2 >= 0 && var2 < PolygonalMap2.CHUNKS_PER_CELL) {
         if (this.chunks == null) {
            this.chunks = new Chunk[PolygonalMap2.CHUNKS_PER_CELL][PolygonalMap2.CHUNKS_PER_CELL];
         }

         if (this.chunks[var1][var2] == null) {
            this.chunks[var1][var2] = Chunk.alloc();
         }

         this.chunks[var1][var2].init(this.cx * PolygonalMap2.CHUNKS_PER_CELL + var1, this.cy * PolygonalMap2.CHUNKS_PER_CELL + var2);
         return this.chunks[var1][var2];
      } else {
         return null;
      }
   }

   void removeChunk(int var1, int var2) {
      if (this.chunks != null) {
         var1 -= this.cx * PolygonalMap2.CHUNKS_PER_CELL;
         var2 -= this.cy * PolygonalMap2.CHUNKS_PER_CELL;
         if (var1 >= 0 && var1 < PolygonalMap2.CHUNKS_PER_CELL && var2 >= 0 && var2 < PolygonalMap2.CHUNKS_PER_CELL) {
            Chunk var3 = this.chunks[var1][var2];
            if (var3 != null) {
               var3.clear();
               var3.release();
               this.chunks[var1][var2] = null;
            }

         }
      }
   }

   void clearChunks() {
      if (this.chunks != null) {
         for(int var1 = 0; var1 < PolygonalMap2.CHUNKS_PER_CELL; ++var1) {
            for(int var2 = 0; var2 < PolygonalMap2.CHUNKS_PER_CELL; ++var2) {
               Chunk var3 = this.chunks[var2][var1];
               if (var3 != null) {
                  var3.clear();
                  var3.release();
                  this.chunks[var2][var1] = null;
               }
            }
         }

      }
   }

   static Cell alloc() {
      return pool.isEmpty() ? new Cell() : (Cell)pool.pop();
   }

   void release() {
      this.clearChunks();

      assert !pool.contains(this);

      pool.push(this);
   }
}
