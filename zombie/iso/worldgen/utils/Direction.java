package zombie.iso.worldgen.utils;

public enum Direction {
   NORTH(0, 1, 0, -1, "n"),
   SOUTH(1, 0, 0, 1, "s"),
   WEST(2, 3, -1, 0, "w"),
   EAST(3, 2, 1, 0, "e");

   public final int index;
   private final int opposite;
   public final int x;
   public final int y;
   public final String config;

   private Direction(int var3, int var4, int var5, int var6, String var7) {
      this.index = var3;
      this.opposite = var4;
      this.x = var5;
      this.y = var6;
      this.config = var7;
   }

   public Direction opposite() {
      return values()[this.opposite];
   }
}
