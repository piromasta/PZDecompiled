package zombie.pathfind;

public final class Point {
   public int x;
   public int y;

   public Point() {
   }

   Point init(int var1, int var2) {
      this.x = var1;
      this.y = var2;
      return this;
   }

   public boolean equals(Object var1) {
      return var1 instanceof Point && ((Point)var1).x == this.x && ((Point)var1).y == this.y;
   }
}
