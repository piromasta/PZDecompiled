package zombie.iso.worldgen.utils.triangulation;

import java.util.Objects;

public class Edge2D {
   public Vector2D a;
   public Vector2D b;

   public Edge2D(Vector2D var1, Vector2D var2) {
      boolean var3 = var1.magSqrt() > var2.magSqrt();
      this.a = var3 ? var1 : var2;
      this.b = var3 ? var2 : var1;
   }

   public double mag() {
      return Math.sqrt(this.magSqrt());
   }

   public double magSqrt() {
      return (this.a.x - this.b.x) * (this.a.x - this.b.x) + (this.a.y - this.b.y) * (this.a.y - this.b.y);
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         Edge2D var2 = (Edge2D)var1;
         return Objects.equals(this.a, var2.a) && Objects.equals(this.b, var2.b);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.a, this.b});
   }
}
