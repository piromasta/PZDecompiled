package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.iso.IsoChunk;

final class ChunkRemoveTask implements IChunkTask {
   PolygonalMap2 map;
   int wx;
   int wy;
   static final ArrayDeque<ChunkRemoveTask> pool = new ArrayDeque();

   ChunkRemoveTask() {
   }

   ChunkRemoveTask init(PolygonalMap2 var1, IsoChunk var2) {
      this.map = var1;
      this.wx = var2.wx;
      this.wy = var2.wy;
      return this;
   }

   public void execute() {
      Cell var1 = this.map.getCellFromChunkPos(this.wx, this.wy);
      var1.removeChunk(this.wx, this.wy);
   }

   static ChunkRemoveTask alloc() {
      synchronized(pool) {
         return pool.isEmpty() ? new ChunkRemoveTask() : (ChunkRemoveTask)pool.pop();
      }
   }

   public void release() {
      synchronized(pool) {
         assert !pool.contains(this);

         pool.push(this);
      }
   }
}
