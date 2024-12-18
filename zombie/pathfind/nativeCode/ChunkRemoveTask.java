package zombie.pathfind.nativeCode;

import zombie.iso.IsoChunk;
import zombie.popman.ObjectPool;

class ChunkRemoveTask implements IPathfindTask {
   int wx;
   int wy;
   static final ObjectPool<ChunkRemoveTask> pool = new ObjectPool(ChunkRemoveTask::new);

   ChunkRemoveTask() {
   }

   ChunkRemoveTask init(IsoChunk var1) {
      this.wx = var1.wx;
      this.wy = var1.wy;
      return this;
   }

   public void execute() {
      PathfindNative.removeChunk(this.wx, this.wy);
   }

   static ChunkRemoveTask alloc() {
      return (ChunkRemoveTask)pool.alloc();
   }

   public void release() {
      pool.release((Object)this);
   }
}
