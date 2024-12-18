package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;

public final class Square {
   static int nextID = 1;
   Integer ID;
   int x;
   int y;
   int z;
   int bits;
   short cost;
   IsoDirections slopedSurfaceDirection;
   float slopedSurfaceHeightMin;
   float slopedSurfaceHeightMax;
   static final ArrayDeque<Square> pool = new ArrayDeque();

   Square() {
      this.ID = nextID++;
   }

   Square init(int var1, int var2, int var3) {
      this.x = var1;
      this.y = var2;
      this.z = var3;
      this.bits = 0;
      this.cost = 0;
      this.slopedSurfaceDirection = null;
      this.slopedSurfaceHeightMin = 0.0F;
      this.slopedSurfaceHeightMax = 0.0F;
      return this;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getZ() {
      return this.z;
   }

   public boolean has(int var1) {
      return (this.bits & var1) != 0;
   }

   public boolean TreatAsSolidFloor() {
      return this.has(512) || this.has(504);
   }

   public boolean isReallySolid() {
      return this.has(1) || this.has(1024) && !this.isAdjacentToWindow() && !this.isAdjacentToHoppable();
   }

   boolean isNonThumpableSolid() {
      return this.isReallySolid() && !this.has(131072);
   }

   boolean isCanPathW() {
      if (this.has(8192)) {
         return true;
      } else {
         Square var1 = PolygonalMap2.instance.getSquareRawZ(this.x - 1, this.y, this.z);
         return var1 != null && (var1.has(131072) || var1.has(262144));
      }
   }

   boolean isCanPathN() {
      if (this.has(16384)) {
         return true;
      } else {
         Square var1 = PolygonalMap2.instance.getSquareRawZ(this.x, this.y - 1, this.z);
         return var1 != null && (var1.has(131072) || var1.has(524288));
      }
   }

   boolean isCollideW() {
      if (this.has(2)) {
         return true;
      } else {
         Square var1 = PolygonalMap2.instance.getSquareRawZ(this.x - 1, this.y, this.z);
         return var1 != null && (var1.has(262144) || var1.has(448) || var1.isReallySolid());
      }
   }

   boolean isCollideN() {
      if (this.has(4)) {
         return true;
      } else {
         Square var1 = PolygonalMap2.instance.getSquareRawZ(this.x, this.y - 1, this.z);
         return var1 != null && (var1.has(524288) || var1.has(56) || var1.isReallySolid());
      }
   }

   boolean isThumpW() {
      if (this.has(32768)) {
         return true;
      } else {
         Square var1 = PolygonalMap2.instance.getSquareRawZ(this.x - 1, this.y, this.z);
         return var1 != null && var1.has(131072);
      }
   }

   boolean isThumpN() {
      if (this.has(65536)) {
         return true;
      } else {
         Square var1 = PolygonalMap2.instance.getSquareRawZ(this.x, this.y - 1, this.z);
         return var1 != null && var1.has(131072);
      }
   }

   boolean isAdjacentToWindow() {
      if (!this.has(2048) && !this.has(4096)) {
         Square var1 = PolygonalMap2.instance.getSquareRawZ(this.x, this.y + 1, this.z);
         if (var1 != null && var1.has(4096)) {
            return true;
         } else {
            Square var2 = PolygonalMap2.instance.getSquareRawZ(this.x + 1, this.y, this.z);
            return var2 != null && var2.has(2048);
         }
      } else {
         return true;
      }
   }

   boolean isAdjacentToHoppable() {
      if (!this.has(16777216) && !this.has(33554432)) {
         Square var1 = PolygonalMap2.instance.getSquareRawZ(this.x, this.y + 1, this.z);
         if (var1 != null && var1.has(16777216)) {
            return true;
         } else {
            Square var2 = PolygonalMap2.instance.getSquareRawZ(this.x + 1, this.y, this.z);
            return var2 != null && var2.has(33554432);
         }
      } else {
         return true;
      }
   }

   public boolean isUnblockedWindowN() {
      if (!this.has(2097152)) {
         return false;
      } else if (this.isReallySolid()) {
         return false;
      } else {
         Square var1 = PolygonalMap2.instance.getSquare(this.x, this.y - 1, this.z);
         return var1 != null && !var1.isReallySolid();
      }
   }

   public boolean isUnblockedWindowW() {
      if (!this.has(1048576)) {
         return false;
      } else if (this.isReallySolid()) {
         return false;
      } else {
         Square var1 = PolygonalMap2.instance.getSquare(this.x - 1, this.y, this.z);
         return var1 != null && !var1.isReallySolid();
      }
   }

   boolean isUnblockedDoorN() {
      if (!this.has(8388608)) {
         return false;
      } else if (this.has(1025)) {
         return false;
      } else {
         Square var1 = PolygonalMap2.instance.getSquare(this.x, this.y - 1, this.z);
         return var1 != null && !var1.has(1025);
      }
   }

   boolean isUnblockedDoorW() {
      if (!this.has(4194304)) {
         return false;
      } else if (this.has(1025)) {
         return false;
      } else {
         Square var1 = PolygonalMap2.instance.getSquare(this.x - 1, this.y, this.z);
         return var1 != null && !var1.has(1025);
      }
   }

   public Square getAdjacentSquare(IsoDirections var1) {
      return PolygonalMap2.instance.getSquare(this.getX() + var1.dx(), this.getY() + var1.dy(), this.getZ());
   }

   public boolean isInside(int var1, int var2, int var3, int var4) {
      return this.getX() >= var1 && this.getX() < var3 && this.getY() >= var2 && this.getY() < var4;
   }

   public boolean testPathFindAdjacent(PMMover var1, int var2, int var3, int var4) {
      if (var2 >= -1 && var2 <= 1 && var3 >= -1 && var3 <= 1 && var4 >= -1 && var4 <= 1) {
         return var2 == 0 && var3 == 0 && var4 == 0 ? false : PolygonalMap2.instance.canNotMoveBetween(var1, this.getX(), this.getY(), this.getZ(), this.getX() + var2, this.getY() + var3, this.getZ() + var4);
      } else {
         return true;
      }
   }

   public boolean hasTransitionToLevelAbove(IsoDirections var1) {
      if (var1 == IsoDirections.N && this.has(64)) {
         return true;
      } else if (var1 == IsoDirections.W && this.has(8)) {
         return true;
      } else {
         return this.hasSlopedSurfaceToLevelAbove(var1);
      }
   }

   public boolean hasSlopedSurface() {
      return this.slopedSurfaceDirection != null;
   }

   public IsoDirections getSlopedSurfaceDirection() {
      return this.slopedSurfaceDirection;
   }

   public float getSlopedSurfaceHeightMin() {
      return this.slopedSurfaceHeightMin;
   }

   public float getSlopedSurfaceHeightMax() {
      return this.slopedSurfaceHeightMax;
   }

   public boolean hasIdenticalSlopedSurface(Square var1) {
      return this.getSlopedSurfaceDirection() == var1.getSlopedSurfaceDirection() && this.getSlopedSurfaceHeightMin() == var1.getSlopedSurfaceHeightMin() && this.getSlopedSurfaceHeightMax() == var1.getSlopedSurfaceHeightMax();
   }

   public boolean isSlopedSurfaceDirectionVertical() {
      IsoDirections var1 = this.getSlopedSurfaceDirection();
      return var1 == IsoDirections.N || var1 == IsoDirections.S;
   }

   public boolean isSlopedSurfaceDirectionHorizontal() {
      IsoDirections var1 = this.getSlopedSurfaceDirection();
      return var1 == IsoDirections.W || var1 == IsoDirections.E;
   }

   public float getSlopedSurfaceHeight(float var1, float var2) {
      IsoDirections var3 = this.getSlopedSurfaceDirection();
      if (var3 == null) {
         return 0.0F;
      } else {
         var1 = PZMath.clamp(var1, 0.0F, 1.0F);
         var2 = PZMath.clamp(var2, 0.0F, 1.0F);
         float var4 = this.getSlopedSurfaceHeightMin();
         float var5 = this.getSlopedSurfaceHeightMax();
         float var10000;
         switch (var3) {
            case N:
               var10000 = PZMath.lerp(var4, var5, 1.0F - var2);
               break;
            case S:
               var10000 = PZMath.lerp(var4, var5, var2);
               break;
            case W:
               var10000 = PZMath.lerp(var4, var5, 1.0F - var1);
               break;
            case E:
               var10000 = PZMath.lerp(var4, var5, var1);
               break;
            default:
               var10000 = -1.0F;
         }

         float var6 = var10000;
         return var6 < 0.0F ? 0.0F : var6;
      }
   }

   public float getSlopedSurfaceHeight(IsoDirections var1) {
      IsoDirections var2 = this.getSlopedSurfaceDirection();
      if (var2 == null) {
         return 0.0F;
      } else if (var2 == var1) {
         return this.getSlopedSurfaceHeightMax();
      } else {
         return var2.Rot180() == var1 ? this.getSlopedSurfaceHeightMin() : -1.0F;
      }
   }

   public boolean isSlopedSurfaceEdgeBlocked(IsoDirections var1) {
      IsoDirections var2 = this.getSlopedSurfaceDirection();
      if (var2 == null) {
         return false;
      } else {
         Square var3 = this.getAdjacentSquare(var1);
         if (var3 == null) {
            return true;
         } else {
            return this.getSlopedSurfaceHeight(var1) != var3.getSlopedSurfaceHeight(var1.Rot180());
         }
      }
   }

   public boolean hasSlopedSurfaceToLevelAbove(IsoDirections var1) {
      IsoDirections var2 = this.getSlopedSurfaceDirection();
      if (var2 == null) {
         return false;
      } else {
         return this.getSlopedSurfaceHeight(var1) == 1.0F;
      }
   }

   public boolean hasSlopedSurfaceBottom(IsoDirections var1) {
      if (!this.hasSlopedSurface()) {
         return false;
      } else {
         return this.getSlopedSurfaceHeight(var1.Rot180()) == 0.0F;
      }
   }

   static Square alloc() {
      return pool.isEmpty() ? new Square() : (Square)pool.pop();
   }

   void release() {
      assert !pool.contains(this);

      pool.push(this);
   }
}
