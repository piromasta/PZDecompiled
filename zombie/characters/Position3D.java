package zombie.characters;

public class Position3D {
   public float x;
   public float y;
   public float z;

   public float x() {
      return this.x;
   }

   public float y() {
      return this.y;
   }

   public float z() {
      return this.z;
   }

   public Position3D() {
   }

   public Position3D(int var1, int var2, int var3) {
      this.x = (float)var1;
      this.y = (float)var2;
      this.z = (float)var3;
   }
}
