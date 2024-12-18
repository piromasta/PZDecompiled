package zombie.core.math;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.debug.DebugLog;
import zombie.iso.Vector2;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class PZMath {
   public static final float PI = 3.1415927F;
   public static final float PI2 = 6.2831855F;
   public static final float degToRads = 0.017453292F;
   public static final float radToDegs = 57.295776F;
   public static final long microsToNanos = 1000L;
   public static final long millisToMicros = 1000L;
   public static final long secondsToMillis = 1000L;
   public static long secondsToNanos = 1000000000L;

   public PZMath() {
   }

   public static float almostUnitIdentity(float var0) {
      return var0 * var0 * (2.0F - var0);
   }

   public static float almostIdentity(float var0, float var1, float var2) {
      if (var0 > var1) {
         return var0;
      } else {
         float var3 = 2.0F * var2 - var1;
         float var4 = 2.0F * var1 - 3.0F * var2;
         float var5 = var0 / var1;
         return (var3 * var5 + var4) * var5 * var5 + var2;
      }
   }

   public static float gain(float var0, float var1) {
      float var2 = (float)(0.5 * Math.pow((double)(2.0F * (var0 < 0.5F ? var0 : 1.0F - var0)), (double)var1));
      return var0 < 0.5F ? var2 : 1.0F - var2;
   }

   public static float clamp(float var0, float var1, float var2) {
      if (var0 < var1) {
         return var1;
      } else {
         return var0 > var2 ? var2 : var0;
      }
   }

   public static long clamp(long var0, long var2, long var4) {
      if (var0 < var2) {
         return var2;
      } else {
         return var0 > var4 ? var4 : var0;
      }
   }

   public static int clamp(int var0, int var1, int var2) {
      if (var0 < var1) {
         return var1;
      } else {
         return var0 > var2 ? var2 : var0;
      }
   }

   public static double clamp(double var0, double var2, double var4) {
      if (var0 < var2) {
         return var2;
      } else {
         return var0 > var4 ? var4 : var0;
      }
   }

   public static float clampFloat(float var0, float var1, float var2) {
      return clamp(var0, var1, var2);
   }

   public static float clamp_01(float var0) {
      return clamp(var0, 0.0F, 1.0F);
   }

   public static double clampDouble_01(double var0) {
      return clamp(var0, 0.0, 1.0);
   }

   public static Quaternion setFromAxisAngle(float var0, float var1, float var2, float var3, Quaternion var4) {
      var4.x = var0;
      var4.y = var1;
      var4.z = var2;
      float var5 = (float)Math.sqrt((double)(var4.x * var4.x + var4.y * var4.y + var4.z * var4.z));
      float var6 = (float)(Math.sin(0.5 * (double)var3) / (double)var5);
      var4.x *= var6;
      var4.y *= var6;
      var4.z *= var6;
      var4.w = (float)Math.cos(0.5 * (double)var3);
      return var4;
   }

   public static float lerp(float var0, float var1, float var2) {
      return var0 + (var1 - var0) * var2;
   }

   public static float lerpAngle(float var0, float var1, float var2) {
      float var3 = getClosestAngle(var0, var1);
      float var4 = var0 + var2 * var3;
      return wrap(var4, -3.1415927F, 3.1415927F);
   }

   public static Vector3f lerp(Vector3f var0, Vector3f var1, Vector3f var2, float var3) {
      var0.set(var1.x + (var2.x - var1.x) * var3, var1.y + (var2.y - var1.y) * var3, var1.z + (var2.z - var1.z) * var3);
      return var0;
   }

   public static zombie.iso.Vector3 lerp(zombie.iso.Vector3 var0, zombie.iso.Vector3 var1, zombie.iso.Vector3 var2, float var3) {
      var0.set(var1.x + (var2.x - var1.x) * var3, var1.y + (var2.y - var1.y) * var3, var1.z + (var2.z - var1.z) * var3);
      return var0;
   }

   public static Vector2 lerp(Vector2 var0, Vector2 var1, Vector2 var2, float var3) {
      var0.set(var1.x + (var2.x - var1.x) * var3, var1.y + (var2.y - var1.y) * var3);
      return var0;
   }

   public static float c_lerp(float var0, float var1, float var2) {
      float var3 = (float)(1.0 - Math.cos((double)(var2 * 3.1415927F))) / 2.0F;
      return var0 * (1.0F - var3) + var1 * var3;
   }

   public static Quaternion slerp(Quaternion var0, Quaternion var1, Quaternion var2, float var3) {
      double var4 = (double)(var1.x * var2.x + var1.y * var2.y + var1.z * var2.z + var1.w * var2.w);
      double var6 = var4 < 0.0 ? -var4 : var4;
      double var8 = (double)(1.0F - var3);
      double var10 = (double)var3;
      if (1.0 - var6 > 0.1) {
         double var12 = org.joml.Math.acos(var6);
         double var14 = org.joml.Math.sin(var12);
         double var16 = 1.0 / var14;
         var8 = org.joml.Math.sin(var12 * (1.0 - (double)var3)) * var16;
         var10 = org.joml.Math.sin(var12 * (double)var3) * var16;
      }

      if (var4 < 0.0) {
         var10 = -var10;
      }

      var0.set((float)(var8 * (double)var1.x + var10 * (double)var2.x), (float)(var8 * (double)var1.y + var10 * (double)var2.y), (float)(var8 * (double)var1.z + var10 * (double)var2.z), (float)(var8 * (double)var1.w + var10 * (double)var2.w));
      return var0;
   }

   public static float sqrt(float var0) {
      return org.joml.Math.sqrt(var0);
   }

   public static float lerpFunc_EaseOutQuad(float var0) {
      return var0 * var0;
   }

   public static float lerpFunc_EaseInQuad(float var0) {
      float var1 = 1.0F - var0;
      return 1.0F - var1 * var1;
   }

   public static float lerpFunc_EaseOutInQuad(float var0) {
      return var0 < 0.5F ? lerpFunc_EaseOutQuad(var0) * 2.0F : 0.5F + lerpFunc_EaseInQuad(2.0F * var0 - 1.0F) / 2.0F;
   }

   public static double tryParseDouble(String var0, double var1) {
      if (StringUtils.isNullOrWhitespace(var0)) {
         return var1;
      } else {
         try {
            return Double.parseDouble(var0.trim());
         } catch (NumberFormatException var4) {
            return var1;
         }
      }
   }

   public static float tryParseFloat(String var0, float var1) {
      if (StringUtils.isNullOrWhitespace(var0)) {
         return var1;
      } else {
         try {
            return Float.parseFloat(var0.trim());
         } catch (NumberFormatException var3) {
            return var1;
         }
      }
   }

   public static boolean canParseFloat(String var0) {
      if (StringUtils.isNullOrWhitespace(var0)) {
         return false;
      } else {
         try {
            Float.parseFloat(var0.trim());
            return true;
         } catch (NumberFormatException var2) {
            return false;
         }
      }
   }

   public static int tryParseInt(String var0, int var1) {
      if (StringUtils.isNullOrWhitespace(var0)) {
         return var1;
      } else {
         try {
            return Integer.parseInt(var0.trim());
         } catch (NumberFormatException var3) {
            return var1;
         }
      }
   }

   public static float pow(float var0, float var1) {
      return (float)Math.pow((double)var0, (double)var1);
   }

   public static float degToRad(float var0) {
      return 0.017453292F * var0;
   }

   public static float radToDeg(float var0) {
      return 57.295776F * var0;
   }

   public static float getClosestAngle(float var0, float var1) {
      float var2 = wrap(var0, 6.2831855F);
      float var3 = wrap(var1, 6.2831855F);
      float var4 = var3 - var2;
      float var5 = wrap(var4, -3.1415927F, 3.1415927F);
      return var5;
   }

   public static float getClosestAngleDegrees(float var0, float var1) {
      float var2 = degToRad(var0);
      float var3 = degToRad(var1);
      float var4 = getClosestAngle(var2, var3);
      return radToDeg(var4);
   }

   public static int sign(float var0) {
      return var0 > 0.0F ? 1 : (var0 < 0.0F ? -1 : 0);
   }

   public static int fastfloor(double var0) {
      int var2 = (int)var0;
      return var0 < (double)var2 ? var2 - 1 : var2;
   }

   public static int fastfloor(float var0) {
      int var1 = (int)var0;
      return var0 < (float)var1 ? var1 - 1 : var1;
   }

   public static int coorddivision(int var0, int var1) {
      return fastfloor((float)var0 / (float)var1);
   }

   public static int coordmodulo(int var0, int var1) {
      return var0 - fastfloor((float)var0 / (float)var1) * var1;
   }

   public static float coordmodulof(float var0, int var1) {
      return var0 - (float)(fastfloor(var0 / (float)var1) * var1);
   }

   public static float floor(float var0) {
      return (float)fastfloor(var0);
   }

   public static double floor(double var0) {
      return (double)fastfloor(var0);
   }

   public static float ceil(float var0) {
      return var0 >= 0.0F ? (float)((int)(var0 + 0.9999999F)) : (float)((int)(var0 - 1.0E-7F));
   }

   public static float frac(float var0) {
      float var1 = floor(var0);
      float var2 = var0 - var1;
      return var2;
   }

   public static float wrap(float var0, float var1) {
      if (var1 == 0.0F) {
         return 0.0F;
      } else if (var1 < 0.0F) {
         return 0.0F;
      } else {
         float var2;
         float var3;
         float var4;
         if (var0 < 0.0F) {
            var2 = -var0 / var1;
            var3 = 1.0F - frac(var2);
            var4 = var3 * var1;
            return var4;
         } else {
            var2 = var0 / var1;
            var3 = frac(var2);
            var4 = var3 * var1;
            return var4;
         }
      }
   }

   public static float wrap(float var0, float var1, float var2) {
      float var3 = max(var2, var1);
      float var4 = min(var2, var1);
      float var5 = var3 - var4;
      float var6 = var0 - var4;
      float var7 = wrap(var6, var5);
      float var8 = var4 + var7;
      return var8;
   }

   public static float max(float var0, float var1) {
      return var0 > var1 ? var0 : var1;
   }

   public static float max(float var0, float var1, float var2) {
      return max(var0, max(var1, var2));
   }

   public static float max(float var0, float var1, float var2, float var3) {
      return max(var0, max(var1, max(var2, var3)));
   }

   public static float max(float var0, float var1, float var2, float var3, float var4) {
      return max(var0, max(var1, max(var2, max(var3, var4))));
   }

   public static int max(int var0, int var1) {
      return var0 > var1 ? var0 : var1;
   }

   public static float min(float var0, float var1) {
      return var0 > var1 ? var1 : var0;
   }

   public static float min(float var0, float var1, float var2) {
      return min(var0, min(var1, var2));
   }

   public static float min(float var0, float var1, float var2, float var3) {
      return min(var0, min(var1, min(var2, var3)));
   }

   public static float min(float var0, float var1, float var2, float var3, float var4) {
      return min(var0, min(var1, min(var2, min(var3, var4))));
   }

   public static int min(int var0, int var1) {
      return var0 > var1 ? var1 : var0;
   }

   public static float abs(float var0) {
      return var0 * (float)sign(var0);
   }

   public static boolean equal(float var0, float var1) {
      return equal(var0, var1, 1.0E-7F);
   }

   public static boolean equal(float var0, float var1, float var2) {
      float var3 = var1 - var0;
      float var4 = abs(var3);
      return var4 < var2;
   }

   public static Matrix4f convertMatrix(org.joml.Matrix4f var0, Matrix4f var1) {
      if (var1 == null) {
         var1 = new Matrix4f();
      }

      var1.m00 = var0.m00();
      var1.m01 = var0.m01();
      var1.m02 = var0.m02();
      var1.m03 = var0.m03();
      var1.m10 = var0.m10();
      var1.m11 = var0.m11();
      var1.m12 = var0.m12();
      var1.m13 = var0.m13();
      var1.m20 = var0.m20();
      var1.m21 = var0.m21();
      var1.m22 = var0.m22();
      var1.m23 = var0.m23();
      var1.m30 = var0.m30();
      var1.m31 = var0.m31();
      var1.m32 = var0.m32();
      var1.m33 = var0.m33();
      return var1;
   }

   public static org.joml.Matrix4f convertMatrix(Matrix4f var0, org.joml.Matrix4f var1) {
      if (var1 == null) {
         var1 = new org.joml.Matrix4f();
      }

      return var1.set(var0.m00, var0.m01, var0.m02, var0.m03, var0.m10, var0.m11, var0.m12, var0.m13, var0.m20, var0.m21, var0.m22, var0.m23, var0.m30, var0.m31, var0.m32, var0.m33);
   }

   public static float step(float var0, float var1, float var2) {
      if (var0 > var1) {
         return max(var0 + var2, var1);
      } else {
         return var0 < var1 ? min(var0 + var2, var1) : var0;
      }
   }

   public static float angleBetween(Vector2 var0, Vector2 var1) {
      float var2 = var0.x;
      float var3 = var0.y;
      float var4 = var1.x;
      float var5 = var1.y;
      return angleBetween(var2, var3, var4, var5);
   }

   public static float angleBetween(float var0, float var1, float var2, float var3) {
      float var4 = sqrt(var0 * var0 + var1 * var1);
      if (var4 == 0.0F) {
         return 0.0F;
      } else {
         float var5 = sqrt(var2 * var2 + var3 * var3);
         if (var5 == 0.0F) {
            return 0.0F;
         } else {
            float var6 = var0 / var4;
            float var7 = var1 / var4;
            float var8 = var2 / var5;
            float var9 = var3 / var5;
            return angleBetweenNormalized(var6, var8, var7, var9);
         }
      }
   }

   public static float angleBetweenNormalized(float var0, float var1, float var2, float var3) {
      float var4 = var0 * var1 + var2 * var3;
      float var5 = acosf(var4);
      float var6 = var0 * var3 - var2 * var1;
      int var7 = sign(var6);
      float var8 = var5 * (float)var7;
      return var8;
   }

   public static float acosf(float var0) {
      return (float)Math.acos((double)var0);
   }

   public static float calculateBearing(zombie.iso.Vector3 var0, Vector2 var1, zombie.iso.Vector3 var2) {
      float var3 = var2.x - var0.x;
      float var4 = var2.y - var0.y;
      return angleBetween(var1.x, var1.y, var3, var4) * 57.295776F;
   }

   public static SideOfLine testSideOfLine(float var0, float var1, float var2, float var3, float var4, float var5) {
      float var6 = (var4 - var0) * (var3 - var1) - (var5 - var1) * (var2 - var0);
      return var6 > 0.0F ? PZMath.SideOfLine.Left : (var6 < 0.0F ? PZMath.SideOfLine.Right : PZMath.SideOfLine.OnLine);
   }

   public static <E> void normalize(List<E> var0, FloatGet<E> var1, FloatSet<E> var2) {
      float[] var3 = new float[var0.size()];

      int var4;
      for(var4 = 0; var4 < var0.size(); ++var4) {
         var3[var4] = var1.get(var0.get(var4));
      }

      normalize(var3);

      for(var4 = 0; var4 < var0.size(); ++var4) {
         var2.set(var0.get(var4), var3[var4]);
      }

   }

   public static <E> void normalize(E[] var0, FloatGet<E> var1, FloatSet<E> var2) {
      float[] var3 = new float[var0.length];

      int var4;
      for(var4 = 0; var4 < var0.length; ++var4) {
         var3[var4] = var1.get(var0[var4]);
      }

      normalize(var3);

      for(var4 = 0; var4 < var0.length; ++var4) {
         var2.set(var0[var4], var3[var4]);
      }

   }

   public static float[] normalize(float[] var0) {
      int var1 = var0.length;
      float var2 = 0.0F;
      int var3 = var1;

      while(true) {
         --var3;
         if (var3 < 0) {
            if (var2 != 0.0F) {
               var3 = var1;

               while(true) {
                  --var3;
                  if (var3 < 0) {
                     break;
                  }

                  var0[var3] /= var2;
               }
            }

            return var0;
         }

         var2 += var0[var3];
      }
   }

   public static ArrayList<Double> normalize(ArrayList<Double> var0) {
      float[] var1 = new float[var0.size()];

      int var2;
      for(var2 = 0; var2 < var0.size(); ++var2) {
         var1[var2] = ((Double)var0.get(var2)).floatValue();
      }

      normalize(var1);
      var0.clear();

      for(var2 = 0; var2 < var1.length; ++var2) {
         var0.add((double)var1[var2]);
      }

      return var0;
   }

   public static float roundFloatPos(float var0, int var1) {
      int var2 = 10;

      for(int var3 = 1; var3 < var1; ++var3) {
         var2 *= 10;
      }

      float var4 = var0 * (float)var2;
      return (float)((int)(var4 - (float)((int)var4) >= 0.5F ? var4 + 1.0F : var4)) / (float)var2;
   }

   public static float roundFloat(float var0, int var1) {
      int var2 = 10;

      for(int var3 = 1; var3 < var1; ++var3) {
         var2 *= 10;
      }

      float var5 = var0 * (float)var2;
      float var4 = var5 - (float)((int)var5);
      return (float)((int)(var0 >= 0.0F ? (var4 >= 0.5F ? var5 + 1.0F : var5) : (var4 >= -0.5F ? var5 : var5 - 1.0F))) / (float)var2;
   }

   public static int nextPowerOfTwo(int var0) {
      if (var0 == 0) {
         return 1;
      } else {
         --var0;
         var0 |= var0 >> 1;
         var0 |= var0 >> 2;
         var0 |= var0 >> 4;
         var0 |= var0 >> 8;
         var0 |= var0 >> 16;
         return var0 + 1;
      }
   }

   public static float roundToNearest(float var0) {
      int var1 = sign(var0);
      return floor(var0 + 0.5F * (float)var1);
   }

   public static int roundToInt(float var0) {
      return (int)(roundToNearest(var0) + 1.0E-4F);
   }

   public static float roundToIntPlus05(float var0) {
      return floor(var0) + 0.5F;
   }

   public static float roundFromEdges(float var0) {
      float var1 = (float)((int)var0);
      float var2 = var0 - var1;
      if (var2 < 0.2F) {
         return var1 + 0.2F;
      } else {
         return var2 > 0.8F ? var1 + 1.0F - 0.2F : var0;
      }
   }

   public static zombie.iso.Vector3 closestVector3(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      zombie.iso.Vector3 var9 = new zombie.iso.Vector3(var0, var1, var2);
      zombie.iso.Vector3 var10 = new zombie.iso.Vector3(var3, var4, var5);
      zombie.iso.Vector3 var11 = new zombie.iso.Vector3(var6, var7, var8);
      float var12 = var10.x - var9.x;
      float var13 = var10.y - var9.y;
      float var14 = var10.z - var9.z;
      float var15 = var11.x - var9.x;
      float var16 = var11.y - var9.y;
      float var17 = var11.z - var9.z;
      float var18 = var15 * var12 + var16 * var13 + var17 * var14;
      float var19 = var12 * var12 + var13 * var13 + var14 * var14;
      float var20 = var18 / var19;
      var20 = org.joml.Math.max(0.0F, org.joml.Math.min(1.0F, var20));
      float var21 = var9.x + var20 * var12;
      float var22 = var9.y + var20 * var13;
      float var23 = var9.z + var20 * var14;
      return new zombie.iso.Vector3(var21, var22, var23);
   }

   static {
      PZMath.UnitTests.runAll();
   }

   public static enum SideOfLine {
      Left,
      OnLine,
      Right;

      private SideOfLine() {
      }
   }

   public interface FloatGet<E> {
      float get(E var1);
   }

   public interface FloatSet<E> {
      void set(E var1, float var2);
   }

   private static final class UnitTests {
      private static final Runnable[] s_unitTests = new Runnable[0];

      private UnitTests() {
      }

      private static void runAll() {
         PZArrayUtil.forEach((Object[])s_unitTests, Runnable::run);
      }

      public static final class vector2 {
         public vector2() {
         }

         public static void run() {
            runUnitTest_direction();
         }

         private static void runUnitTest_direction() {
            DebugLog.UnitTests.println("runUnitTest_direction");
            DebugLog.UnitTests.println("x, y, angle, length, rdir.x, rdir.y, rangle, rlength, pass");
            checkDirection(1.0F, 0.0F);
            checkDirection(1.0F, 1.0F);
            checkDirection(0.0F, 1.0F);
            checkDirection(-1.0F, 1.0F);
            checkDirection(-1.0F, 0.0F);
            checkDirection(-1.0F, -1.0F);
            checkDirection(0.0F, -1.0F);
            checkDirection(1.0F, -1.0F);
            DebugLog.UnitTests.println("runUnitTest_direction. Complete");
         }

         private static void checkDirection(float var0, float var1) {
            Vector2 var2 = new Vector2(var0, var1);
            float var3 = var2.getDirection();
            float var4 = var2.getLength();
            Vector2 var5 = Vector2.fromLengthDirection(var4, var3);
            float var6 = var5.getDirection();
            float var7 = var5.getLength();
            boolean var8 = PZMath.equal(var2.x, var5.x, 1.0E-4F) && PZMath.equal(var2.y, var5.y, 1.0E-4F) && PZMath.equal(var3, var6, 1.0E-4F) && PZMath.equal(var4, var7, 1.0E-4F);
            DebugLog.UnitTests.println("%f, %f, %f, %f, %f, %f, %f, %f, %s", var0, var1, var3, var4, var5.x, var5.y, var6, var7, var8 ? "true" : "false");
         }
      }

      private static final class getClosestAngle {
         private getClosestAngle() {
         }

         public static void run() {
            DebugLog.UnitTests.println("runUnitTests_getClosestAngle");
            DebugLog.UnitTests.println("a, b, result, expected, pass");
            runUnitTest(0.0F, 0.0F, 0.0F);
            runUnitTest(0.0F, 15.0F, 15.0F);
            runUnitTest(15.0F, 0.0F, -15.0F);
            runUnitTest(0.0F, 179.0F, 179.0F);
            runUnitTest(180.0F, 180.0F, 0.0F);
            runUnitTest(180.0F, 359.0F, 179.0F);
            runUnitTest(90.0F, 180.0F, 90.0F);
            runUnitTest(180.0F, 90.0F, -90.0F);

            for(int var0 = -360; var0 < 360; var0 += 10) {
               for(int var1 = -360; var1 < 360; var1 += 10) {
                  float var2 = (float)var0;
                  float var3 = (float)var1;
                  runUnitTest_noexp(var2, var3);
               }
            }

            DebugLog.UnitTests.println("runUnitTests_getClosestAngle. Complete");
         }

         private static void runUnitTest_noexp(float var0, float var1) {
            float var2 = PZMath.getClosestAngleDegrees(var0, var1);
            logResult(var0, var1, var2, "N/A", "N/A");
         }

         private static void runUnitTest(float var0, float var1, float var2) {
            float var3 = PZMath.getClosestAngleDegrees(var0, var1);
            boolean var4 = PZMath.equal(var2, var3, 1.0E-4F);
            String var5 = var4 ? "pass" : "fail";
            logResult(var0, var1, var3, String.valueOf(var2), var5);
         }

         private static void logResult(float var0, float var1, float var2, String var3, String var4) {
            DebugLog.UnitTests.println("%f, %f, %f, %s, %s", var0, var1, var2, var3, var4);
         }
      }

      private static final class lerpFunctions {
         private lerpFunctions() {
         }

         public static void run() {
            DebugLog.UnitTests.println("UnitTest_lerpFunctions");
            DebugLog.UnitTests.println("x,Sqrt,EaseOutQuad,EaseInQuad,EaseOutInQuad");

            for(int var0 = 0; var0 < 100; ++var0) {
               float var1 = (float)var0 / 100.0F;
               DebugLog.UnitTests.println("%f,%f,%f,%f", var1, PZMath.lerpFunc_EaseOutQuad(var1), PZMath.lerpFunc_EaseInQuad(var1), PZMath.lerpFunc_EaseOutInQuad(var1));
            }

            DebugLog.UnitTests.println("UnitTest_lerpFunctions. Complete");
         }
      }
   }
}
