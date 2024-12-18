package zombie.iso.worldgen.utils.triangulation;

import java.util.Arrays;

public class Triangle2D {
   public Vector2D a;
   public Vector2D b;
   public Vector2D c;

   public Triangle2D(Vector2D var1, Vector2D var2, Vector2D var3) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
   }

   public boolean contains(Vector2D var1) {
      double var2 = var1.sub(this.a).cross(this.b.sub(this.a));
      double var4 = var1.sub(this.b).cross(this.c.sub(this.b));
      if (!this.hasSameSign(var2, var4)) {
         return false;
      } else {
         double var6 = var1.sub(this.c).cross(this.a.sub(this.c));
         return this.hasSameSign(var2, var6);
      }
   }

   public boolean isPointInCircumcircle(Vector2D var1) {
      double var2 = this.a.x - var1.x;
      double var4 = this.b.x - var1.x;
      double var6 = this.c.x - var1.x;
      double var8 = this.a.y - var1.y;
      double var10 = this.b.y - var1.y;
      double var12 = this.c.y - var1.y;
      double var14 = (this.a.x - var1.x) * (this.a.x - var1.x) + (this.a.y - var1.y) * (this.a.y - var1.y);
      double var16 = (this.b.x - var1.x) * (this.b.x - var1.x) + (this.b.y - var1.y) * (this.b.y - var1.y);
      double var18 = (this.c.x - var1.x) * (this.c.x - var1.x) + (this.c.y - var1.y) * (this.c.y - var1.y);
      double var20 = var2 * var10 * var18 + var8 * var16 * var6 + var14 * var4 * var12 - var14 * var10 * var6 - var8 * var4 * var18 - var2 * var16 * var12;
      if (this.isOrientedCCW()) {
         return var20 > 0.0;
      } else {
         return var20 < 0.0;
      }
   }

   public boolean isOrientedCCW() {
      double var1 = this.a.x - this.c.x;
      double var3 = this.b.x - this.c.x;
      double var5 = this.a.y - this.c.y;
      double var7 = this.b.y - this.c.y;
      double var9 = var1 * var7 - var5 * var3;
      return var9 > 0.0;
   }

   public boolean isNeighbour(Edge2D var1) {
      return (this.a == var1.a || this.b == var1.a || this.c == var1.a) && (this.a == var1.b || this.b == var1.b || this.c == var1.b);
   }

   public Vector2D getNoneEdgeVertex(Edge2D var1) {
      if (this.a != var1.a && this.a != var1.b) {
         return this.a;
      } else if (this.b != var1.a && this.b != var1.b) {
         return this.b;
      } else {
         return this.c != var1.a && this.c != var1.b ? this.c : null;
      }
   }

   public boolean hasVertex(Vector2D var1) {
      return this.a == var1 || this.b == var1 || this.c == var1;
   }

   public EdgeDistancePack findNearestEdge(Vector2D var1) {
      EdgeDistancePack[] var2 = new EdgeDistancePack[]{new EdgeDistancePack(new Edge2D(this.a, this.b), this.computeClosestPoint(new Edge2D(this.a, this.b), var1).sub(var1).mag()), new EdgeDistancePack(new Edge2D(this.b, this.c), this.computeClosestPoint(new Edge2D(this.b, this.c), var1).sub(var1).mag()), new EdgeDistancePack(new Edge2D(this.c, this.a), this.computeClosestPoint(new Edge2D(this.c, this.a), var1).sub(var1).mag())};
      Arrays.sort(var2);
      return var2[0];
   }

   private Vector2D computeClosestPoint(Edge2D var1, Vector2D var2) {
      Vector2D var3 = var1.b.sub(var1.a);
      double var4 = var2.sub(var1.a).dot(var3) / var3.dot(var3);
      if (var4 < 0.0) {
         var4 = 0.0;
      } else if (var4 > 1.0) {
         var4 = 1.0;
      }

      return var1.a.add(var3.mult(var4));
   }

   private boolean hasSameSign(double var1, double var3) {
      return Math.signum(var1) == Math.signum(var3);
   }

   public String toString() {
      return "Triangle2D[" + this.a + ", " + this.b + ", " + this.c + "]";
   }
}
