package zombie.pathfind;

import java.util.Arrays;
import zombie.util.list.PZArrayUtil;

final class ChunkData {
   final ChunkDataZ[] data = new ChunkDataZ[64];

   ChunkData() {
   }

   public ChunkDataZ init(Chunk var1, int var2) {
      if (this.data[var2] == null) {
         this.data[var2] = (ChunkDataZ)ChunkDataZ.pool.alloc();
         this.data[var2].init(var1, var2);
      } else if (this.data[var2].epoch != ChunkDataZ.EPOCH) {
         this.data[var2].clear();
         this.data[var2].init(var1, var2);
      }

      return this.data[var2];
   }

   public void clear() {
      PZArrayUtil.forEach((Object[])this.data, (var0) -> {
         if (var0 != null) {
            var0.clear();
            ChunkDataZ.pool.release((Object)var0);
         }

      });
      Arrays.fill(this.data, (Object)null);
   }
}
