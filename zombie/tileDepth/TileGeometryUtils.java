package zombie.tileDepth;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.Core;
import zombie.iso.Vector2;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.UI3DScene;

public final class TileGeometryUtils {
   private static final UI3DScene.Plane m_plane = new UI3DScene.Plane();
   private static final Vector3f m_viewRotation = new Vector3f(30.0F, 315.0F, 0.0F);
   private static final int[] m_viewport = new int[]{0, 0, 0, 0};
   private static int m_viewWidth = 1;
   private static int m_viewHeight = 2;

   public TileGeometryUtils() {
   }

   static void calcMatricesForSquare(Matrix4f var0, Matrix4f var1) {
      float var2 = (float)Math.sqrt(2.0);
      var0.setOrtho((float)(-m_viewWidth) * var2 / 2.0F, (float)m_viewWidth * var2 / 2.0F, (float)(-m_viewHeight) * var2 / 2.0F, (float)m_viewHeight * var2 / 2.0F, -2.0F, 2.0F);
      var0.translate(0.0F, (float)(-m_viewHeight) * var2 * 0.375F, 0.0F);
      var1.rotationXYZ(m_viewRotation.x * 0.017453292F, m_viewRotation.y * 0.017453292F, m_viewRotation.z * 0.017453292F);
   }

   public static float getDepthOnBoxAt(float var0, float var1, Vector3f var2, Vector3f var3, Vector3f var4, Vector3f var5) {
      Matrix4f var6 = allocMatrix4f();
      Matrix4f var7 = allocMatrix4f();
      calcMatricesForSquare(var6, var7);
      var0 *= (float)m_viewWidth / (64.0F * (float)Core.TileScale);
      var1 *= (float)m_viewHeight / (128.0F * (float)Core.TileScale);
      Vector3f var8 = allocVector3f();
      UI3DScene.Ray var9 = getCameraRay(var0, (float)m_viewHeight - var1, var6, var7, m_viewWidth, m_viewHeight, UI3DScene.allocRay());
      Matrix4f var10 = allocMatrix4f();
      var10.translation(var2);
      var10.rotateXYZ(var3.x * 0.017453292F, var3.y * 0.017453292F, var3.z * 0.017453292F);
      var10.invert();
      var10.transformPosition(var9.origin);
      var10.transformDirection(var9.direction);
      releaseMatrix4f(var10);
      Vector2f var14 = allocVector2f();
      boolean var11 = intersectRayAab(var9.origin.x, var9.origin.y, var9.origin.z, var9.direction.x, var9.direction.y, var9.direction.z, var4.x, var4.y, var4.z, var5.x, var5.y, var5.z, var14);
      Matrix4f var12;
      if (var11) {
         var8.set(var9.origin).add(var9.direction.mul(var14.x));
         var12 = allocMatrix4f();
         var12.translation(var2);
         var12.rotateXYZ(var3.x * 0.017453292F, var3.y * 0.017453292F, var3.z * 0.017453292F);
         var12.transformPosition(var8);
         releaseMatrix4f(var12);
      }

      releaseVector2f(var14);
      UI3DScene.releaseRay(var9);
      var12 = allocMatrix4f();
      var12.set(var6);
      var12.mul(var7);
      var12.transformPosition(var8);
      float var13 = var8.z;
      releaseMatrix4f(var12);
      releaseVector3f(var8);
      releaseMatrix4f(var6);
      releaseMatrix4f(var7);
      return var11 ? var13 : 666.0F;
   }

   public static float getDepthOnCylinderAt(float var0, float var1, Vector3f var2, Vector3f var3, float var4, float var5) {
      Matrix4f var6 = allocMatrix4f();
      Matrix4f var7 = allocMatrix4f();
      calcMatricesForSquare(var6, var7);
      var0 *= (float)m_viewWidth / (64.0F * (float)Core.TileScale);
      var1 *= (float)m_viewHeight / (128.0F * (float)Core.TileScale);
      Vector3f var8 = allocVector3f();
      UI3DScene.Ray var9 = getCameraRay(var0, (float)m_viewHeight - var1, var6, var7, m_viewWidth, m_viewHeight, UI3DScene.allocRay());
      Matrix4f var10 = allocMatrix4f();
      var10.translation(var2);
      var10.rotateXYZ(var3.x * 0.017453292F, var3.y * 0.017453292F, var3.z * 0.017453292F);
      var10.invert();
      var10.transformPosition(var9.origin);
      var10.transformDirection(var9.direction);
      releaseMatrix4f(var10);
      CylinderUtils.IntersectionRecord var14 = new CylinderUtils.IntersectionRecord();
      boolean var11 = CylinderUtils.intersect(var4, var5, var9, var14);
      Matrix4f var12;
      if (var11) {
         var12 = allocMatrix4f();
         var12.translation(var2);
         var12.rotateXYZ(var3.x * 0.017453292F, var3.y * 0.017453292F, var3.z * 0.017453292F);
         var12.transformPosition(var14.location);
         var12.transformDirection(var14.normal);
         releaseMatrix4f(var12);
         var8.set(var14.location);
      }

      UI3DScene.releaseRay(var9);
      var12 = allocMatrix4f();
      var12.set(var6);
      var12.mul(var7);
      var12.transformPosition(var8);
      float var13 = var8.z;
      releaseMatrix4f(var12);
      releaseVector3f(var8);
      releaseMatrix4f(var6);
      releaseMatrix4f(var7);
      return var11 ? var13 : 666.0F;
   }

   public static float getDepthOnPlaneAt(float var0, float var1, UI3DScene.GridPlane var2, Vector3f var3) {
      Vector3f var4 = allocVector3f();
      switch (var2) {
         case XY:
            var4.set(0.0F, 0.0F, 1.0F);
            break;
         case XZ:
            var4.set(0.0F, 1.0F, 0.0F);
            break;
         case YZ:
            var4.set(1.0F, 0.0F, 0.0F);
      }

      float var5 = getDepthOnPlaneAt(var0, var1, var3, var4);
      releaseVector3f(var4);
      return var5;
   }

   public static float getDepthOnPlaneAt(float var0, float var1, Vector3f var2, Vector3f var3) {
      return getDepthOnPlaneAt(var0, var1, var2, var3, (Vector3f)null);
   }

   public static float getDepthOnPlaneAt(float var0, float var1, Vector3f var2, Vector3f var3, Vector3f var4) {
      Matrix4f var5 = allocMatrix4f();
      Matrix4f var6 = allocMatrix4f();
      calcMatricesForSquare(var5, var6);
      m_plane.point.set(var2);
      m_plane.normal.set(var3);
      var0 *= (float)m_viewWidth / (64.0F * (float)Core.TileScale);
      var1 *= (float)m_viewHeight / (128.0F * (float)Core.TileScale);
      Vector3f var7 = allocVector3f();
      UI3DScene.Ray var8 = getCameraRay(var0, (float)m_viewHeight - var1, var5, var6, m_viewWidth, m_viewHeight, UI3DScene.allocRay());
      boolean var9 = UI3DScene.intersect_ray_plane(m_plane, var8, var7) == 1;
      UI3DScene.releaseRay(var8);
      if (var4 != null) {
         var4.set(var7);
      }

      Matrix4f var10 = allocMatrix4f();
      var10.set(var5);
      var10.mul(var6);
      var10.transformPosition(var7);
      float var11 = var7.z;
      releaseMatrix4f(var10);
      releaseVector3f(var7);
      releaseMatrix4f(var5);
      releaseMatrix4f(var6);
      return var9 ? var11 : 666.0F;
   }

   static float getDepthOfScenePoint(float var0, float var1, float var2) {
      Matrix4f var3 = allocMatrix4f();
      Matrix4f var4 = allocMatrix4f();
      calcMatricesForSquare(var3, var4);
      Matrix4f var5 = allocMatrix4f();
      var5.set(var3);
      var5.mul(var4);
      Vector3f var6 = allocVector3f().set(var0, var1, var2);
      var5.transformPosition(var6);
      float var7 = var6.z;
      releaseMatrix4f(var5);
      releaseVector3f(var6);
      releaseMatrix4f(var3);
      releaseMatrix4f(var4);
      return var7;
   }

   static float getNormalizedDepth(float var0) {
      float var1 = Math.abs(getDepthOfScenePoint(-0.5F, 0.0F, -0.5F));
      float var2 = 1.0F / var1;
      var2 *= 0.25F;
      float var3 = 0.75F;
      return var0 * var2 + var3;
   }

   public static float getNormalizedDepthOnBoxAt(float var0, float var1, Vector3f var2, Vector3f var3, Vector3f var4, Vector3f var5) {
      float var6 = getDepthOnBoxAt(var0, var1, var2, var3, var4, var5);
      return var6 == 666.0F ? -1.0F : getNormalizedDepth(var6);
   }

   public static float getNormalizedDepthOnCylinderAt(float var0, float var1, Vector3f var2, Vector3f var3, float var4, float var5) {
      float var6 = getDepthOnCylinderAt(var0, var1, var2, var3, var4, var5);
      return var6 == 666.0F ? -1.0F : getNormalizedDepth(var6);
   }

   public static float getNormalizedDepthOnPlaneAt(float var0, float var1, Vector3f var2, Vector3f var3) {
      float var4 = getDepthOnPlaneAt(var0, var1, var2, var3);
      return var4 == 666.0F ? -1.0F : getNormalizedDepth(var4);
   }

   public static float getNormalizedDepthOnPlaneAt(float var0, float var1, UI3DScene.GridPlane var2, Vector3f var3) {
      float var4 = getDepthOnPlaneAt(var0, var1, var2, var3);
      return var4 == 666.0F ? -1.0F : getNormalizedDepth(var4);
   }

   public static UI3DScene.Ray getCameraRay(float var0, float var1, Matrix4f var2, Matrix4f var3, int var4, int var5, UI3DScene.Ray var6) {
      Matrix4f var7 = allocMatrix4f();
      var7.set(var2);
      var7.mul(var3);
      var7.invert();
      m_viewport[2] = var4;
      m_viewport[3] = var5;
      Vector3f var8 = var7.unprojectInv(var0, var1, 0.0F, m_viewport, allocVector3f());
      Vector3f var9 = var7.unprojectInv(var0, var1, 1.0F, m_viewport, allocVector3f());
      var6.origin.set(var8);
      var6.direction.set(var9.sub(var8).normalize());
      releaseVector3f(var9);
      releaseVector3f(var8);
      releaseMatrix4f(var7);
      return var6;
   }

   public static boolean intersectRayAab(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, Vector2f var12) {
      float var13 = 1.0F / var3;
      float var14 = 1.0F / var4;
      float var15 = 1.0F / var5;
      float var16;
      float var17;
      if (var13 >= 0.0F) {
         var16 = (var6 - var0) * var13;
         var17 = (var9 - var0) * var13;
      } else {
         var16 = (var9 - var0) * var13;
         var17 = (var6 - var0) * var13;
      }

      float var18;
      float var19;
      if (var14 >= 0.0F) {
         var18 = (var7 - var1) * var14;
         var19 = (var10 - var1) * var14;
      } else {
         var18 = (var10 - var1) * var14;
         var19 = (var7 - var1) * var14;
      }

      if (!(var16 > var19) && !(var18 > var17)) {
         float var20;
         float var21;
         if (var15 >= 0.0F) {
            var20 = (var8 - var2) * var15;
            var21 = (var11 - var2) * var15;
         } else {
            var20 = (var11 - var2) * var15;
            var21 = (var8 - var2) * var15;
         }

         if (!(var16 > var21) && !(var20 > var17)) {
            var16 = !(var18 > var16) && !Float.isNaN(var16) ? var16 : var18;
            var17 = !(var19 < var17) && !Float.isNaN(var17) ? var17 : var19;
            var16 = var20 > var16 ? var20 : var16;
            var17 = var21 < var17 ? var21 : var17;
            if (var16 < var17 && var17 >= 0.0F) {
               var12.x = var16;
               var12.y = var17;
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Matrix4f allocMatrix4f() {
      return (Matrix4f)((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).alloc();
   }

   private static void releaseMatrix4f(Matrix4f var0) {
      ((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).release(var0);
   }

   private static Quaternionf allocQuaternionf() {
      return (Quaternionf)((BaseVehicle.QuaternionfObjectPool)BaseVehicle.TL_quaternionf_pool.get()).alloc();
   }

   private static void releaseQuaternionf(Quaternionf var0) {
      ((BaseVehicle.QuaternionfObjectPool)BaseVehicle.TL_quaternionf_pool.get()).release(var0);
   }

   private static Vector2 allocVector2() {
      return (Vector2)((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).alloc();
   }

   private static void releaseVector2(Vector2 var0) {
      ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var0);
   }

   private static Vector2f allocVector2f() {
      return BaseVehicle.allocVector2f();
   }

   private static void releaseVector2f(Vector2f var0) {
      BaseVehicle.releaseVector2f(var0);
   }

   private static Vector3f allocVector3f() {
      return (Vector3f)((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).alloc();
   }

   private static void releaseVector3f(Vector3f var0) {
      ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(var0);
   }
}
