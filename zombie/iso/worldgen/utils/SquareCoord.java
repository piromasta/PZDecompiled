package zombie.iso.worldgen.utils;

public record SquareCoord(int x, int y, int z) {
   public SquareCoord(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public int x() {
      return this.x;
   }

   public int y() {
      return this.y;
   }

   public int z() {
      return this.z;
   }
}
