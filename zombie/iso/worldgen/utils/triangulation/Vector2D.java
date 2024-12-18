package zombie.iso.worldgen.utils.triangulation;

public class Vector2D {
   public double x;
   public double y;

   public Vector2D(double var1, double var3) {
      this.x = var1;
      this.y = var3;
   }

   public Vector2D sub(Vector2D var1) {
      return new Vector2D(this.x - var1.x, this.y - var1.y);
   }

   public Vector2D add(Vector2D var1) {
      return new Vector2D(this.x + var1.x, this.y + var1.y);
   }

   public Vector2D mult(double var1) {
      return new Vector2D(this.x * var1, this.y * var1);
   }

   public double mag() {
      return Math.sqrt(this.x * this.x + this.y * this.y);
   }

   public double magSqrt() {
      return this.x * this.x + this.y * this.y;
   }

   public double dot(Vector2D var1) {
      return this.x * var1.x + this.y * var1.y;
   }

   public double cross(Vector2D var1) {
      return this.y * var1.x - this.x * var1.y;
   }

   public String toString() {
      return "Vector2D[" + this.x + ", " + this.y + "]";
   }
}
