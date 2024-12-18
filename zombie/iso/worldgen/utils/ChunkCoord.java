package zombie.iso.worldgen.utils;

public record ChunkCoord(int x, int y) {
   public ChunkCoord(int x, int y) {
      this.x = x;
      this.y = y;
   }

   public int x() {
      return this.x;
   }

   public int y() {
      return this.y;
   }
}
