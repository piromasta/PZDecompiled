package zombie.pathfind;

public final class PathNode {
   public float x;
   public float y;
   public float z;
   int flags;

   public PathNode() {
   }

   PathNode init(float var1, float var2, float var3, int var4) {
      this.x = var1;
      this.y = var2;
      this.z = var3;
      this.flags = var4;
      return this;
   }

   PathNode init(PathNode var1) {
      this.x = var1.x;
      this.y = var1.y;
      this.z = var1.z;
      this.flags = var1.flags;
      return this;
   }

   boolean hasFlag(int var1) {
      return (this.flags & var1) != 0;
   }

   boolean isApproximatelyEqual(float var1, float var2, float var3) {
      return Math.abs(this.x - var1) < 0.01F && Math.abs(this.y - var2) < 0.01F && Math.abs(this.z - var3) < 0.01F;
   }

   boolean isApproximatelyEqual(PathNode var1) {
      return this.isApproximatelyEqual(var1.x, var1.y, var1.z);
   }
}
