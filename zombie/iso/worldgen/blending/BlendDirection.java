package zombie.iso.worldgen.blending;

public enum BlendDirection {
   NORTH(0, 1, 0, -1, 0, 1, (byte)7),
   SOUTH(1, 0, 0, 1, 0, 1, (byte)0),
   WEST(2, 3, -1, 0, 1, 0, (byte)7),
   EAST(3, 2, 1, 0, 1, 0, (byte)0);

   public final int x;
   public final int y;
   public final int planeX;
   public final int planeY;
   public final int index;
   private final int opposite;
   public final byte defaultDepth;

   private BlendDirection(int var3, int var4, int var5, int var6, int var7, int var8, byte var9) {
      this.x = var5;
      this.y = var6;
      this.planeX = var7;
      this.planeY = var8;
      this.index = var3;
      this.opposite = var4;
      this.defaultDepth = var9;
   }

   public BlendDirection opposite() {
      return values()[this.opposite];
   }
}
