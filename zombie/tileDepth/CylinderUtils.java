package zombie.tileDepth;

import java.util.Arrays;
import org.joml.Vector3f;
import zombie.vehicles.UI3DScene;

public final class CylinderUtils {
   public CylinderUtils() {
   }

   public static boolean intersect(float var0, float var1, UI3DScene.Ray var2, IntersectionRecord var3) {
      Vector3f var4 = new Vector3f(0.0F);
      Vector3f var5 = var2.origin.sub(var4, new Vector3f());
      double var6 = Math.pow((double)var2.direction.x, 2.0) + Math.pow((double)var2.direction.y, 2.0);
      double var8 = (double)(2.0F * (var2.direction.x * var5.x + var2.direction.y * var5.y));
      double var10 = Math.pow((double)var5.x, 2.0) + Math.pow((double)var5.y, 2.0) - Math.pow((double)var0, 2.0);
      double var12 = var8 * var8 - 4.0 * var6 * var10;
      if (var12 < 0.0) {
         return false;
      } else {
         double var14 = Math.min((-var8 + Math.sqrt(var12)) / (2.0 * var6), (-var8 - Math.sqrt(var12)) / (2.0 * var6));
         double var16 = ((double)var1 / 2.0 - (double)var5.z) / (double)var2.direction.z;
         double var18 = ((double)(-var1) / 2.0 - (double)var5.z) / (double)var2.direction.z;
         double[] var20 = new double[]{var14, var16, var18};
         Arrays.sort(var20);
         Double var21 = null;
         double[] var22 = var20;
         int var23 = var20.length;

         for(int var24 = 0; var24 < var23; ++var24) {
            double var25 = var22[var24];
            IntersectionRecord var27 = new IntersectionRecord();
            var2.origin.add(getScaledVector(var2.direction, (float)var25), var27.location);
            if (var25 == var14) {
               if ((double)Math.abs(var27.location.z - var4.z) < (double)var1 / 2.0) {
                  var3.normal.set(var27.location.x - var4.x, var27.location.y - var4.y, 0.0F);
                  var3.normal.normalize();
                  var21 = var25;
                  break;
               }
            } else if (Math.pow((double)(var27.location.x - var4.x), 2.0) + Math.pow((double)(var27.location.y - var4.y), 2.0) - Math.pow((double)var0, 2.0) <= 0.0) {
               if (var25 == var16) {
                  var3.normal.set(0.0F, 0.0F, 1.0F);
               } else if (var25 == var18) {
                  var3.normal.set(0.0F, 0.0F, -1.0F);
               }

               var21 = var25;
               break;
            }
         }

         if (var21 == null) {
            return false;
         } else {
            var2.t = var21.floatValue();
            var3.t = var21;
            var2.origin.add(getScaledVector(var2.direction, var21.floatValue()), var3.location);
            return true;
         }
      }
   }

   private static Vector3f getScaledVector(Vector3f var0, float var1) {
      return (new Vector3f(var0)).mul(var1);
   }

   public static final class IntersectionRecord {
      public final Vector3f location = new Vector3f();
      public final Vector3f normal = new Vector3f();
      double t;

      public IntersectionRecord() {
      }
   }
}
