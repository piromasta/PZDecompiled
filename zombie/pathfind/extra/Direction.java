package zombie.pathfind.extra;

import zombie.iso.worldgen.utils.SquareCoord;

public enum Direction {
   NORTH(0, -1, 0),
   SOUTH(0, 1, 0),
   WEST(-1, 0, 0),
   EAST(1, 0, 0),
   NORTH_WEST(-1, -1, 0),
   NORTH_EAST(1, -1, 0),
   SOUTH_WEST(-1, 1, 0),
   SOUTH_EAST(1, 1, 0);

   private final int x;
   private final int y;
   private final int z;

   private Direction(int var3, int var4, int var5) {
      this.x = var3;
      this.y = var4;
      this.z = var5;
   }

   public static SquareCoord move(SquareCoord var0, Direction var1) {
      return var1.move(var0);
   }

   public SquareCoord move(SquareCoord var1) {
      return new SquareCoord(var1.x() + this.x, var1.y() + this.y, var1.z() + this.z);
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
