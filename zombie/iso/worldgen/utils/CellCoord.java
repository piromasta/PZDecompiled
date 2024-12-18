package zombie.iso.worldgen.utils;

public record CellCoord(int x, int y) {
   public CellCoord(int x, int y) {
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
