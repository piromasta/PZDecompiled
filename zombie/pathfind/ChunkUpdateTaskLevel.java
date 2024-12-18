package zombie.pathfind;

import java.util.ArrayList;
import zombie.popman.ObjectPool;

final class ChunkUpdateTaskLevel {
   final int[][] data = new int[8][8];
   final short[][] cost = new short[8][8];
   final ArrayList<SlopedSurface> slopedSurfaces = new ArrayList();
   static final ObjectPool<ChunkUpdateTaskLevel> pool = new ObjectPool(ChunkUpdateTaskLevel::new);

   ChunkUpdateTaskLevel() {
   }

   static ChunkUpdateTaskLevel alloc() {
      return (ChunkUpdateTaskLevel)pool.alloc();
   }

   void release() {
      SlopedSurface.pool.releaseAll(this.slopedSurfaces);
      this.slopedSurfaces.clear();
      pool.release((Object)this);
   }
}
