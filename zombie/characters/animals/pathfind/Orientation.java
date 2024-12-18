package zombie.characters.animals.pathfind;

import org.joml.Vector2f;

public final class Orientation {
   private static final double DP_SAFE_EPSILON = 1.0E-15;

   public Orientation() {
   }

   public static int index(Vector2f var0, Vector2f var1, Vector2f var2) {
      int var3 = orientationIndexFilter(var0, var1, var2);
      if (var3 <= 1) {
         return var3;
      } else {
         float var4 = var1.x - var0.x;
         float var5 = var1.y - var0.y;
         float var6 = var2.x - -var1.x;
         float var7 = var2.y - var1.y;
         return signum((double)(var4 * var7 - var5 * var6));
      }
   }

   private static int orientationIndexFilter(Vector2f var0, Vector2f var1, Vector2f var2) {
      double var5 = (double)((var0.x - var2.x) * (var1.y - var2.y));
      double var7 = (double)((var0.y - var2.y) * (var1.x - var2.x));
      double var9 = var5 - var7;
      double var3;
      if (var5 > 0.0) {
         if (var7 <= 0.0) {
            return signum(var9);
         }

         var3 = var5 + var7;
      } else {
         if (!(var5 < 0.0)) {
            return signum(var9);
         }

         if (var7 >= 0.0) {
            return signum(var9);
         }

         var3 = -var5 - var7;
      }

      double var11 = 1.0E-15 * var3;
      return !(var9 >= var11) && !(-var9 >= var11) ? 2 : signum(var9);
   }

   private static int signum(double var0) {
      if (var0 > 0.0) {
         return 1;
      } else {
         return var0 < 0.0 ? -1 : 0;
      }
   }
}
