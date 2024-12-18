package zombie.characters.animals.pathfind;

import org.joml.Vector2f;

public final class RobustLineIntersector {
   public static final int NO_INTERSECTION = 0;
   public static final int POINT_INTERSECTION = 1;
   public static final int COLLINEAR_INTERSECTION = 2;

   public RobustLineIntersector() {
   }

   public static int computeIntersection(Vector2f var0, Vector2f var1, Vector2f var2, Vector2f var3, Vector2f var4, Vector2f var5) {
      if (!Envelope.intersects(var0, var1, var2, var3)) {
         return 0;
      } else {
         int var6 = Orientation.index(var0, var1, var2);
         int var7 = Orientation.index(var0, var1, var3);
         if ((var6 <= 0 || var7 <= 0) && (var6 >= 0 || var7 >= 0)) {
            int var8 = Orientation.index(var2, var3, var0);
            int var9 = Orientation.index(var2, var3, var1);
            if (var8 > 0 && var9 > 0 || var8 < 0 && var9 < 0) {
               return 0;
            } else {
               boolean var10 = var6 == 0 && var7 == 0 && var8 == 0 && var9 == 0;
               return var10 ? computeCollinearIntersection(var0, var1, var2, var3, var4, var5) : 0;
            }
         } else {
            return 0;
         }
      }
   }

   static int computeCollinearIntersection(Vector2f var0, Vector2f var1, Vector2f var2, Vector2f var3, Vector2f var4, Vector2f var5) {
      boolean var6 = Envelope.intersects(var0, var1, var2);
      boolean var7 = Envelope.intersects(var0, var1, var3);
      boolean var8 = Envelope.intersects(var2, var3, var0);
      boolean var9 = Envelope.intersects(var2, var3, var1);
      if (var6 && var7) {
         if (var4 != null && var5 != null) {
            var4.set(var2);
            var5.set(var3);
         }

         return 2;
      } else if (var8 && var9) {
         if (var4 != null && var5 != null) {
            var4.set(var0);
            var5.set(var1);
         }

         return 2;
      } else if (var6 && var8) {
         if (var4 != null && var5 != null) {
            var4.set(var2);
            var5.set(var0);
         }

         return var2.equals(var0) && !var7 && !var9 ? 1 : 2;
      } else if (var6 && var9) {
         if (var4 != null && var5 != null) {
            var4.set(var2);
            var5.set(var1);
         }

         return var2.equals(var1) && !var7 && !var8 ? 1 : 2;
      } else if (var7 && var8) {
         if (var4 != null && var5 != null) {
            var4.set(var3);
            var5.set(var0);
         }

         return var3.equals(var0) && !var6 && !var9 ? 1 : 2;
      } else if (var7 && var9) {
         if (var4 != null && var5 != null) {
            var4.set(var3);
            var5.set(var1);
         }

         return var3.equals(var1) && !var6 && !var8 ? 1 : 2;
      } else {
         return 0;
      }
   }
}
