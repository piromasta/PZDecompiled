package zombie.pathfind.highLevel;

import zombie.debug.LineDrawer;
import zombie.iso.IsoDirections;
import zombie.util.Type;

public final class HLSlopedSurface extends HLLevelTransition {
   IsoDirections dir;
   int x;
   int y;
   int z;

   public HLSlopedSurface() {
   }

   public boolean equals(Object var1) {
      HLSlopedSurface var2 = (HLSlopedSurface)Type.tryCastTo(var1, this.getClass());
      return var2 != null && this.dir == var2.dir && this.x == var2.x && this.y == var2.y && this.z == var2.z;
   }

   public HLSlopedSurface set(IsoDirections var1, int var2, int var3, int var4) {
      assert var1 == IsoDirections.N || var1 == IsoDirections.S || var1 == IsoDirections.W || var1 == IsoDirections.E;

      this.dir = var1;
      this.x = var2;
      this.y = var3;
      this.z = var4;
      return this;
   }

   public HLSlopedSurface set(HLSlopedSurface var1) {
      return this.set(var1.dir, var1.x, var1.y, var1.z);
   }

   public IsoDirections getDir() {
      return this.dir;
   }

   public IsoDirections getReverseDir() {
      return this.dir.Rot180();
   }

   public boolean isDir(IsoDirections var1) {
      return this.dir == var1;
   }

   public boolean isNorth() {
      return this.isDir(IsoDirections.N);
   }

   public boolean isSouth() {
      return this.isDir(IsoDirections.S);
   }

   public boolean isWest() {
      return this.isDir(IsoDirections.W);
   }

   public boolean isEast() {
      return this.isDir(IsoDirections.E);
   }

   public int getBottomFloorX() {
      return this.x;
   }

   public int getBottomFloorY() {
      return this.y;
   }

   public int getBottomFloorZ() {
      return this.z;
   }

   public int getTopFloorX() {
      return this.getBottomFloorX() + this.dir.dx();
   }

   public int getTopFloorY() {
      return this.getBottomFloorY() + this.dir.dy();
   }

   public int getTopFloorZ() {
      return this.getBottomFloorZ() + 1;
   }

   public float getSearchNodeX(boolean var1) {
      return (float)(var1 ? this.getBottomFloorX() : this.getTopFloorX()) + 0.5F;
   }

   public float getSearchNodeY(boolean var1) {
      return (float)(var1 ? this.getBottomFloorY() : this.getTopFloorY()) + 0.5F;
   }

   public boolean isOnEdgeOfLoadedArea() {
      return false;
   }

   HLSlopedSurface asSlopedSurface() {
      return this;
   }

   public void renderDebug() {
      LineDrawer.addLine((float)this.getBottomFloorX() + 0.5F, (float)this.getBottomFloorY() + 0.5F, (float)(this.getBottomFloorZ() - 32), (float)this.getTopFloorX() + 0.5F, (float)this.getTopFloorY() + 0.5F, (float)(this.getTopFloorZ() - 32), 1.0F, 1.0F, 1.0F, 1.0F);
   }
}
