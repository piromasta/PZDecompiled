package zombie.characters.animals.pathfind;

import org.joml.Vector2f;

public final class Envelope {
   public Envelope() {
   }

   public static boolean intersects(Vector2f var0, Vector2f var1, Vector2f var2) {
      return var2.x >= (var0.x < var1.x ? var0.x : var1.x) && var2.x <= (var0.x > var1.x ? var0.x : var1.x) && var2.y >= (var0.y < var1.y ? var0.y : var1.y) && var2.y <= (var0.y > var1.y ? var0.y : var1.y);
   }

   public static boolean intersects(Vector2f var0, Vector2f var1, Vector2f var2, Vector2f var3) {
      double var4 = (double)Math.min(var2.x, var3.x);
      double var6 = (double)Math.max(var2.x, var3.x);
      double var8 = (double)Math.min(var0.x, var1.x);
      double var10 = (double)Math.max(var0.x, var1.x);
      if (var8 > var6) {
         return false;
      } else if (var10 < var4) {
         return false;
      } else {
         var4 = (double)Math.min(var2.y, var3.y);
         var6 = (double)Math.max(var2.y, var3.y);
         var8 = (double)Math.min(var0.y, var1.y);
         var10 = (double)Math.max(var0.y, var1.y);
         if (var8 > var6) {
            return false;
         } else {
            return !(var10 < var4);
         }
      }
   }
}
