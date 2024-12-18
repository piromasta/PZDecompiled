package zombie.pathfind;

import zombie.pathfind.highLevel.HLAStar;
import zombie.pathfind.highLevel.HLChunkLevel;
import zombie.popman.ObjectPool;

public final class ChunkLevel {
   Chunk chunk;
   int z;
   final Square[][] squares = new Square[8][8];
   final HLChunkLevel hlChunkLevel = new HLChunkLevel(this);
   static final ObjectPool<ChunkLevel> pool = new ObjectPool(ChunkLevel::new);

   ChunkLevel() {
   }

   public Chunk getChunk() {
      return this.chunk;
   }

   public int getLevel() {
      return this.z;
   }

   public Square getSquare(int var1, int var2) {
      var1 -= this.chunk.getMinX();
      var2 -= this.chunk.getMinY();
      return var1 >= 0 && var1 < 8 && var2 >= 0 && var2 < 8 ? this.squares[var1][var2] : null;
   }

   public void setBits(int var1, int var2, int var3, short var4) {
      Square var5;
      if (var3 == 0) {
         var5 = this.squares[var1][var2];
         if (var5 != null) {
            var5.release();
            this.squares[var1][var2] = null;
         }
      } else {
         var5 = this.squares[var1][var2];
         if (var5 == null) {
            var5 = Square.alloc();
            this.squares[var1][var2] = var5;
         }

         var5.x = this.chunk.getMinX() + var1;
         var5.y = this.chunk.getMinY() + var2;
         var5.z = this.z;
         var5.bits = var3;
         var5.cost = var4;
      }

   }

   public ChunkLevel init(Chunk var1, int var2) {
      this.chunk = var1;
      this.z = var2;
      return this;
   }

   public void clear() {
      for(int var1 = 0; var1 < 64; ++var1) {
         Square var2 = this.squares[var1 % 8][var1 / 8];
         if (var2 != null) {
            var2.release();
            this.squares[var1 % 8][var1 / 8] = null;
         }
      }

      this.hlChunkLevel.removeFromWorld();
   }

   public HLChunkLevel getHighLevelData() {
      if (this.hlChunkLevel.m_modificationCount != HLAStar.ModificationCount) {
         this.hlChunkLevel.m_modificationCount = HLAStar.ModificationCount;
         this.hlChunkLevel.initRegions();
      }

      return this.hlChunkLevel;
   }

   static ChunkLevel alloc() {
      return (ChunkLevel)pool.alloc();
   }

   void release() {
      pool.release((Object)this);
   }
}
