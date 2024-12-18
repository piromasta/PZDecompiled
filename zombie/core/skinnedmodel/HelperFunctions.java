package zombie.core.skinnedmodel;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.vecmath.Matrix3d;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.model.VertexPositionNormalTangentTextureSkin;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayUtil;

public final class HelperFunctions {
   private static final Vector3f s_zero3 = new Vector3f(0.0F, 0.0F, 0.0F);
   private static final Quaternion s_identityQ = new Quaternion();
   private static final Stack<Matrix4f> MatrixStack = new Stack();
   private static final AtomicBoolean MatrixLock = new AtomicBoolean(false);
   private static final ObjectPool<Vector3f> VectorPool = new ObjectPool(Vector3f::new);
   private static final ObjectPool<Quaternion> QuaternionPool = new ObjectPool(Quaternion::new);

   public HelperFunctions() {
   }

   public static int ToRgba(Color var0) {
      return (int)var0.a << 24 | (int)var0.b << 16 | (int)var0.g << 8 | (int)var0.r;
   }

   public static void returnMatrix(Matrix4f var0) {
      while(!MatrixLock.compareAndSet(false, true)) {
         Thread.onSpinWait();
      }

      assert !MatrixStack.contains(var0);

      MatrixStack.push(var0);
      MatrixLock.set(false);
   }

   public static Matrix4f getMatrix() {
      Matrix4f var0 = null;

      while(!MatrixLock.compareAndSet(false, true)) {
         Thread.onSpinWait();
      }

      if (MatrixStack.isEmpty()) {
         var0 = new Matrix4f();
      } else {
         var0 = (Matrix4f)MatrixStack.pop();
      }

      MatrixLock.set(false);
      return var0;
   }

   public static Matrix4f getMatrix(Matrix4f var0) {
      Matrix4f var1 = getMatrix();
      var1.load(var0);
      return var1;
   }

   public static Vector3f allocVector3f(float var0, float var1, float var2) {
      Vector3f var3 = allocVector3f();
      var3.set(var0, var1, var2);
      return var3;
   }

   public static Vector3f allocVector3f() {
      while(!MatrixLock.compareAndSet(false, true)) {
         Thread.onSpinWait();
      }

      Vector3f var0 = (Vector3f)VectorPool.alloc();
      MatrixLock.set(false);
      return var0;
   }

   public static void releaseVector3f(Vector3f var0) {
      while(!MatrixLock.compareAndSet(false, true)) {
         Thread.onSpinWait();
      }

      VectorPool.release((Object)var0);
      MatrixLock.set(false);
   }

   public static Quaternion allocQuaternion(float var0, float var1, float var2, float var3) {
      Quaternion var4 = allocQuaternion();
      var4.set(var0, var1, var2, var3);
      return var4;
   }

   public static Quaternion allocQuaternion() {
      while(!MatrixLock.compareAndSet(false, true)) {
         Thread.onSpinWait();
      }

      Quaternion var0 = (Quaternion)QuaternionPool.alloc();
      MatrixLock.set(false);
      return var0;
   }

   public static void releaseQuaternion(Quaternion var0) {
      while(!MatrixLock.compareAndSet(false, true)) {
         Thread.onSpinWait();
      }

      QuaternionPool.release((Object)var0);
      MatrixLock.set(false);
   }

   public static Matrix4f CreateFromQuaternion(Quaternion var0) {
      Matrix4f var1 = getMatrix();
      CreateFromQuaternion(var0, var1);
      return var1;
   }

   public static Matrix4f CreateFromQuaternion(Quaternion var0, Matrix4f var1) {
      var1.setIdentity();
      float var2 = var0.lengthSquared();
      float var3;
      float var4;
      if (var2 > 0.0F && var2 < 0.99999F || var2 > 1.00001F) {
         var3 = (float)Math.sqrt((double)var2);
         var4 = 1.0F / var3;
         var0.scale(var4);
      }

      var3 = var0.x * var0.x;
      var4 = var0.x * var0.y;
      float var5 = var0.x * var0.z;
      float var6 = var0.x * var0.w;
      float var7 = var0.y * var0.y;
      float var8 = var0.y * var0.z;
      float var9 = var0.y * var0.w;
      float var10 = var0.z * var0.z;
      float var11 = var0.z * var0.w;
      var1.m00 = 1.0F - 2.0F * (var7 + var10);
      var1.m10 = 2.0F * (var4 - var11);
      var1.m20 = 2.0F * (var5 + var9);
      var1.m30 = 0.0F;
      var1.m01 = 2.0F * (var4 + var11);
      var1.m11 = 1.0F - 2.0F * (var3 + var10);
      var1.m21 = 2.0F * (var8 - var6) * 1.0F;
      var1.m31 = 0.0F;
      var1.m02 = 2.0F * (var5 - var9);
      var1.m12 = 2.0F * (var8 + var6);
      var1.m22 = 1.0F - 2.0F * (var3 + var7);
      var1.m32 = 0.0F;
      var1.m03 = 0.0F;
      var1.m13 = 0.0F;
      var1.m23 = 0.0F;
      var1.m33 = 1.0F;
      var1.m30 = 0.0F;
      var1.m31 = 0.0F;
      var1.m32 = 0.0F;
      var1.transpose();
      return var1;
   }

   public static Matrix4f CreateFromQuaternionPositionScale(Vector3f var0, Quaternion var1, Vector3f var2, Matrix4f var3) {
      Matrix4f var4 = getMatrix();
      Matrix4f var5 = getMatrix();
      CreateFromQuaternionPositionScale(var0, var1, var2, var3, var5, var4);
      returnMatrix(var4);
      returnMatrix(var5);
      return var3;
   }

   public static float getAngle(float var0, float var1, float var2, float var3) {
      float var4 = var0 * var3 - var1 * var2;
      float var5 = var0 * var2 + var1 * var3;
      float var6 = PZMath.sqrt(var0 * var0 + var1 * var1);
      float var7 = PZMath.sqrt(var2 * var2 + var3 * var3);
      float var8 = var5 / (var6 * var7);
      if (var8 < -1.0F) {
         var8 = -1.0F;
      } else if (var8 > 1.0F) {
         var8 = 1.0F;
      }

      return (float)Math.acos((double)var8) * (float)PZMath.sign(var4);
   }

   public static void CreateFromQuaternionPositionScale(Vector3f var0, Quaternion var1, Vector3f var2, TransformResult_QPS var3) {
      CreateFromQuaternionPositionScale(var0, var1, var2, var3.result, var3.rot, var3.scl);
   }

   private static void CreateFromQuaternionPositionScale(Vector3f var0, Quaternion var1, Vector3f var2, Matrix4f var3, Matrix4f var4, Matrix4f var5) {
      var5.setIdentity();
      var5.scale(var2);
      CreateFromQuaternion(var1, var4);
      Matrix4f.mul(var5, var4, var3);
      setPosition(var3, var0);
   }

   public static void TransformVertices(VertexPositionNormalTangentTextureSkin[] var0, List<Matrix4f> var1) {
      Vector3 var2 = new Vector3();
      Vector3 var3 = new Vector3();
      VertexPositionNormalTangentTextureSkin[] var4 = var0;
      int var5 = var0.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         VertexPositionNormalTangentTextureSkin var7 = var4[var6];
         var2.reset();
         var3.reset();
         Vector3 var8 = var7.Position;
         Vector3 var9 = var7.Normal;
         ApplyBlendBone(var7.BlendWeights.x, (Matrix4f)var1.get(var7.BlendIndices.X), var8, var9, var2, var3);
         ApplyBlendBone(var7.BlendWeights.y, (Matrix4f)var1.get(var7.BlendIndices.Y), var8, var9, var2, var3);
         ApplyBlendBone(var7.BlendWeights.z, (Matrix4f)var1.get(var7.BlendIndices.Z), var8, var9, var2, var3);
         ApplyBlendBone(var7.BlendWeights.w, (Matrix4f)var1.get(var7.BlendIndices.W), var8, var9, var2, var3);
         var8.set(var2);
         var9.set(var3);
      }

   }

   public static void ApplyBlendBone(float var0, Matrix4f var1, Vector3 var2, Vector3 var3, Vector3 var4, Vector3 var5) {
      if (var0 > 0.0F) {
         float var9 = var2.x();
         float var10 = var2.y();
         float var11 = var2.z();
         float var6 = var1.m00 * var9 + var1.m01 * var10 + var1.m02 * var11 + var1.m03;
         float var7 = var1.m10 * var9 + var1.m11 * var10 + var1.m12 * var11 + var1.m13;
         float var8 = var1.m20 * var9 + var1.m21 * var10 + var1.m22 * var11 + var1.m23;
         var4.add(var6 * var0, var7 * var0, var8 * var0);
         var9 = var3.x();
         var10 = var3.y();
         var11 = var3.z();
         var6 = var1.m00 * var9 + var1.m01 * var10 + var1.m02 * var11;
         var7 = var1.m10 * var9 + var1.m11 * var10 + var1.m12 * var11;
         var8 = var1.m20 * var9 + var1.m21 * var10 + var1.m22 * var11;
         var5.add(var6 * var0, var7 * var0, var8 * var0);
      }

   }

   public static Vector3f getXAxis(Matrix4f var0, Vector3f var1) {
      var1.set(var0.m00, var0.m10, var0.m20);
      return var1;
   }

   public static void setXAxis(Matrix4f var0, Vector3f var1) {
      var0.m00 = var1.x;
      var0.m10 = var1.y;
      var0.m20 = var1.z;
   }

   public static Vector3f getYAxis(Matrix4f var0, Vector3f var1) {
      var1.set(var0.m01, var0.m11, var0.m21);
      return var1;
   }

   public static void setYAxis(Matrix4f var0, Vector3f var1) {
      var0.m01 = var1.x;
      var0.m11 = var1.y;
      var0.m21 = var1.z;
   }

   public static Vector3f getZAxis(Matrix4f var0, Vector3f var1) {
      var1.set(var0.m02, var0.m12, var0.m22);
      return var1;
   }

   public static void setZAxis(Matrix4f var0, Vector3f var1) {
      var0.m02 = var1.x;
      var0.m12 = var1.y;
      var0.m22 = var1.z;
   }

   public static Vector3f getPosition(Matrix4f var0, Vector3f var1) {
      var1.set(var0.m03, var0.m13, var0.m23);
      return var1;
   }

   public static void setPosition(Matrix4f var0, Vector3f var1) {
      var0.m03 = var1.x;
      var0.m13 = var1.y;
      var0.m23 = var1.z;
   }

   public static void setPosition(Matrix4f var0, float var1, float var2, float var3) {
      var0.m03 = var1;
      var0.m13 = var2;
      var0.m23 = var3;
   }

   public static Quaternion getRotation(Matrix4f var0, Quaternion var1) {
      return Quaternion.setFromMatrix(var0, var1);
   }

   public static Vector3f transform(Quaternion var0, Vector3f var1, Vector3f var2) {
      var0.normalise();
      float var3 = var0.w;
      float var4 = var0.x;
      float var5 = var0.y;
      float var6 = var0.z;
      float var7 = var3 * var3;
      float var8 = var4 * var4 + var5 * var5 + var6 * var6;
      float var9 = var1.x;
      float var10 = var1.y;
      float var11 = var1.z;
      float var12 = var5 * var11 - var6 * var10;
      float var13 = var6 * var9 - var4 * var11;
      float var14 = var4 * var10 - var5 * var9;
      float var15 = var9 * var4 + var10 * var5 + var11 * var6;
      float var16 = (var7 - var8) * var9 + 2.0F * var3 * var12 + 2.0F * var4 * var15;
      float var17 = (var7 - var8) * var10 + 2.0F * var3 * var13 + 2.0F * var5 * var15;
      float var18 = (var7 - var8) * var11 + 2.0F * var3 * var14 + 2.0F * var6 * var15;
      var2.set(var16, var17, var18);
      return var2;
   }

   public static Vector4f transform(Matrix4f var0, Vector4f var1, Vector4f var2) {
      float var3 = var0.m00 * var1.x + var0.m01 * var1.y + var0.m02 * var1.z + var0.m03 * var1.w;
      float var4 = var0.m10 * var1.x + var0.m11 * var1.y + var0.m12 * var1.z + var0.m13 * var1.w;
      float var5 = var0.m20 * var1.x + var0.m21 * var1.y + var0.m22 * var1.z + var0.m23 * var1.w;
      float var6 = var0.m30 * var1.x + var0.m31 * var1.y + var0.m32 * var1.z + var0.m33 * var1.w;
      var2.x = var3;
      var2.y = var4;
      var2.z = var5;
      var2.w = var6;
      return var2;
   }

   public static Vector3f transformVector(Matrix4f var0, Vector3f var1, Vector3f var2) {
      float var3 = var0.m00 * var1.x + var0.m01 * var1.y + var0.m02 * var1.z;
      float var4 = var0.m10 * var1.x + var0.m11 * var1.y + var0.m12 * var1.z;
      float var5 = var0.m20 * var1.x + var0.m21 * var1.y + var0.m22 * var1.z;
      var2.x = var3;
      var2.y = var4;
      var2.z = var5;
      return var2;
   }

   public static float getRotationY(Matrix4f var0) {
      float var1 = var0.m00 * 1.0F + var0.m01 * 0.0F + var0.m02 * 0.0F;
      float var2 = var0.m20 * 1.0F + var0.m21 * 0.0F + var0.m22 * 0.0F;
      float var3 = var0.m00 * 0.0F + var0.m01 * 0.0F + var0.m02 * 1.0F;
      float var4 = var0.m20 * 0.0F + var0.m21 * 0.0F + var0.m22 * 1.0F;
      float var5 = (var1 + var4) / 2.0F;
      float var6 = (var2 - var3) / 2.0F;
      float var7 = PZMath.sqrt(var5 * var5 + var6 * var6);
      float var8 = var5 / var7;
      float var9 = var6 / var7;
      return (float)Math.atan2((double)(-var9), (double)var8);
   }

   public static float getRotationY(Quaternion var0) {
      var0.normalise();
      float var1 = var0.w;
      float var2 = var0.x;
      float var3 = var0.y;
      float var4 = var0.z;
      float var5 = var1 * var1;
      float var6 = var2 * var2 + var3 * var3 + var4 * var4;
      float var9 = var3 * 0.0F - var4 * 0.0F;
      float var10 = var2 * 0.0F - var3 * 1.0F;
      float var11 = 1.0F * var2 + 0.0F * var3 + 0.0F * var4;
      float var7 = (var5 - var6) * 1.0F + 2.0F * var1 * var9 + 2.0F * var2 * var11;
      float var8 = (var5 - var6) * 0.0F + 2.0F * var1 * var10 + 2.0F * var4 * var11;
      var11 = var3 * 1.0F - var4 * 0.0F;
      float var12 = var2 * 0.0F - var3 * 0.0F;
      float var13 = 0.0F * var2 + 0.0F * var3 + 1.0F * var4;
      var9 = (var5 - var6) * 0.0F + 2.0F * var1 * var11 + 2.0F * var2 * var13;
      var10 = (var5 - var6) * 1.0F + 2.0F * var1 * var12 + 2.0F * var4 * var13;
      var11 = (var7 + var10) / 2.0F;
      var12 = (var8 - var9) / 2.0F;
      var13 = PZMath.sqrt(var11 * var11 + var12 * var12);
      float var14 = var11 / var13;
      float var15 = var12 / var13;
      return (float)Math.atan2((double)(-var15), (double)var14);
   }

   public static float getRotationZ(Quaternion var0) {
      float var1 = var0.w;
      float var2 = var0.x;
      float var3 = var0.y;
      float var4 = var0.z;
      float var5 = var1 * var1;
      float var6 = var2 * var2 + var3 * var3 + var4 * var4;
      float var7 = var4 * 1.0F;
      float var8 = 1.0F * var2;
      float var9 = (var5 - var6) * 1.0F + 2.0F * var2 * var8;
      float var10 = 2.0F * var1 * var7 + 2.0F * var3 * var8;
      float var11 = (float)Math.atan2((double)var10, (double)var9);
      return var11;
   }

   public static Vector3f ToEulerAngles(Quaternion var0, Vector3f var1) {
      double var2 = 2.0 * (double)(var0.w * var0.x + var0.y * var0.z);
      double var4 = 1.0 - 2.0 * (double)(var0.x * var0.x + var0.y * var0.y);
      var1.x = (float)Math.atan2(var2, var4);
      double var6 = 2.0 * (double)(var0.w * var0.y - var0.z * var0.x);
      if (Math.abs(var6) >= 1.0) {
         var1.y = (float)Math.copySign(1.5707963705062866, var6);
      } else {
         var1.y = (float)Math.asin(var6);
      }

      double var8 = 2.0 * (double)(var0.w * var0.z + var0.x * var0.y);
      double var10 = 1.0 - 2.0 * (double)(var0.y * var0.y + var0.z * var0.z);
      var1.z = (float)Math.atan2(var8, var10);
      return var1;
   }

   public static Quaternion ToQuaternion(double var0, double var2, double var4, Quaternion var6) {
      double var7 = Math.cos(var4 * 0.5);
      double var9 = Math.sin(var4 * 0.5);
      double var11 = Math.cos(var2 * 0.5);
      double var13 = Math.sin(var2 * 0.5);
      double var15 = Math.cos(var0 * 0.5);
      double var17 = Math.sin(var0 * 0.5);
      var6.w = (float)(var7 * var11 * var15 + var9 * var13 * var17);
      var6.x = (float)(var7 * var11 * var17 - var9 * var13 * var15);
      var6.y = (float)(var9 * var11 * var17 + var7 * var13 * var15);
      var6.z = (float)(var9 * var11 * var15 - var7 * var13 * var17);
      return var6;
   }

   public static void setRotation(Matrix4f var0, Quaternion var1) {
      double[] var2 = new double[9];
      double[] var3 = new double[3];
      getScaleRotate(var0, var3, var2);
      var0.m00 = (float)((double)(1.0F - 2.0F * var1.y * var1.y - 2.0F * var1.z * var1.z) * var3[0]);
      var0.m10 = (float)((double)(2.0F * (var1.x * var1.y + var1.w * var1.z)) * var3[0]);
      var0.m20 = (float)((double)(2.0F * (var1.x * var1.z - var1.w * var1.y)) * var3[0]);
      var0.m01 = (float)((double)(2.0F * (var1.x * var1.y - var1.w * var1.z)) * var3[1]);
      var0.m11 = (float)((double)(1.0F - 2.0F * var1.x * var1.x - 2.0F * var1.z * var1.z) * var3[1]);
      var0.m21 = (float)((double)(2.0F * (var1.y * var1.z + var1.w * var1.x)) * var3[1]);
      var0.m02 = (float)((double)(2.0F * (var1.x * var1.z + var1.w * var1.y)) * var3[2]);
      var0.m12 = (float)((double)(2.0F * (var1.y * var1.z - var1.w * var1.x)) * var3[2]);
      var0.m22 = (float)((double)(1.0F - 2.0F * var1.x * var1.x - 2.0F * var1.y * var1.y) * var3[2]);
   }

   private static void getScaleRotate(Matrix4f var0, double[] var1, double[] var2) {
      double[] var3 = new double[]{(double)var0.m00, (double)var0.m01, (double)var0.m02, (double)var0.m10, (double)var0.m11, (double)var0.m12, (double)var0.m20, (double)var0.m21, (double)var0.m22};
      Matrix3d.compute_svd(var3, var1, var2);
   }

   public static Vector3f getZero3() {
      s_zero3.set(0.0F, 0.0F, 0.0F);
      return s_zero3;
   }

   public static Quaternion getIdentityQ() {
      s_identityQ.setIdentity();
      return s_identityQ;
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

   static {
      HelperFunctions.UnitTests.runAll();
      HelperFunctions.UnitTests.runAll();
   }

   public static class TransformResult_QPS {
      public final Matrix4f result;
      final Matrix4f rot;
      final Matrix4f scl;

      public TransformResult_QPS() {
         this.result = new Matrix4f();
         this.rot = new Matrix4f();
         this.scl = new Matrix4f();
      }

      public TransformResult_QPS(Matrix4f var1) {
         this.result = var1;
         this.rot = new Matrix4f();
         this.scl = new Matrix4f();
      }
   }

   private static final class UnitTests {
      private static final Runnable[] s_unitTests = new Runnable[]{getRotationY::run};

      private UnitTests() {
      }

      private static void runAll() {
         if (DebugLog.UnitTests.isEnabled()) {
            DebugLog.enableLog(DebugType.UnitTests, LogSeverity.Debug);
            PZArrayUtil.forEach((Object[])s_unitTests, Runnable::run);
         }

      }

      private static final class transformQuaternion {
         private transformQuaternion() {
         }

         public static void run() {
            DebugLog.UnitTests.println("UnitTest_transformQuaternion");
            DebugLog.UnitTests.println("roll, pitch, yaw, out.x, out.y, out.z, cout.x, cout.y, cout.z, result");
            Quaternion var0 = new Quaternion();
            Vector3f var1 = new Vector3f();
            Vector3f var2 = new Vector3f();
            Matrix4f var3 = new Matrix4f();
            Vector4f var4 = new Vector4f();
            Vector4f var5 = new Vector4f();
            runTest(0.0F, 0.0F, 90.0F, var0, var1, var2, var3, var4, var5);
            runTest(0.0F, 0.0F, 5.0F, var0, var1, var2, var3, var4, var5);

            for(int var6 = 0; var6 < 10; ++var6) {
               float var7 = PZMath.wrap((float)var6 / 10.0F * 360.0F, -180.0F, 180.0F);

               for(int var8 = 0; var8 < 10; ++var8) {
                  float var9 = PZMath.wrap((float)var8 / 10.0F * 360.0F, -180.0F, 180.0F);

                  for(int var10 = 0; var10 < 10; ++var10) {
                     float var11 = PZMath.wrap((float)var10 / 10.0F * 360.0F, -180.0F, 180.0F);
                     runTest(var7, var9, var11, var0, var1, var2, var3, var4, var5);
                  }
               }
            }

            DebugLog.UnitTests.println("UnitTest_transformQuaternion. Complete");
         }

         public static void runTest(float var0, float var1, float var2, Quaternion var3, Vector3f var4, Vector3f var5, Matrix4f var6, Vector4f var7, Vector4f var8) {
            Vector3f var9 = new Vector3f(15.0F, 5.0F, -15.0F);
            HelperFunctions.CreateFromQuaternionPositionScale(var9, HelperFunctions.ToQuaternion((double)var0, (double)var1, (double)var2, var3), new Vector3f(1.0F, 1.0F, 1.0F), var6);
            var4.set(1.0F, 0.0F, 0.0F);
            var7.set(var4.x, var4.y, var4.z, 1.0F);
            HelperFunctions.transform(var6, var7, var8);
            HelperFunctions.transform(var3, var4, var5);
            var5.x += var9.x;
            var5.y += var9.y;
            var5.z += var9.z;
            Vector3f var10 = HelperFunctions.getPosition(var6, new Vector3f());
            boolean var11 = PZMath.equal(var5.x, var8.x, 0.01F) && PZMath.equal(var5.y, var8.y, 0.01F) && PZMath.equal(var5.z, var8.z, 0.01F) && PZMath.equal(var9.x, var10.x, 0.01F) && PZMath.equal(var9.y, var10.y, 0.01F) && PZMath.equal(var9.z, var10.z, 0.01F);
            DebugLog.UnitTests.printUnitTest("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", var11, var0, var1, var2, var5.x, var5.y, var5.z, var8.x, var8.y, var8.z, var10.x, var10.y, var10.z);
         }
      }

      private static final class getRotationMatrix {
         private getRotationMatrix() {
         }

         public static void run() {
            DebugLog.UnitTests.println("UnitTest_getRotationMatrix");
            DebugLog.UnitTests.println("q.x, q.y, q.z, q.w, q_out.x, q_out.y, q_out.z, q_out.w");
            Quaternion var0 = new Quaternion();
            Vector4f var1 = new Vector4f();
            Matrix4f var2 = new Matrix4f();
            Quaternion var3 = new Quaternion();
            Quaternion var4 = new Quaternion();

            for(int var5 = 0; var5 < 360; var5 += 10) {
               float var6 = PZMath.wrap((float)var5, -180.0F, 180.0F);
               var1.set(1.0F, 0.0F, 0.0F, var6 * 0.017453292F);
               var0.setFromAxisAngle(var1);
               HelperFunctions.CreateFromQuaternion(var0, var2);
               HelperFunctions.getRotation(var2, var3);
               var4.set(-var3.x, -var3.y, -var3.z, -var3.w);
               boolean var7 = PZMath.equal(var0.x, var3.x, 0.01F) && PZMath.equal(var0.y, var3.y, 0.01F) && PZMath.equal(var0.z, var3.z, 0.01F) && PZMath.equal(var0.w, var3.w, 0.01F) || PZMath.equal(var0.x, var4.x, 0.01F) && PZMath.equal(var0.y, var4.y, 0.01F) && PZMath.equal(var0.z, var4.z, 0.01F) && PZMath.equal(var0.w, var4.w, 0.01F);
               DebugLog.UnitTests.printUnitTest("%f,%f,%f,%f, %f,%f,%f,%f", var7, var0.x, var0.y, var0.z, var0.w, var3.x, var3.y, var3.z, var3.w);
            }

            DebugLog.UnitTests.println("UnitTest_getRotationMatrix. Complete");
         }
      }

      private static final class getRotationY {
         private getRotationY() {
         }

         public static void run() {
            DebugLog.UnitTests.println("UnitTest_getRotationY");
            DebugLog.UnitTests.println("in, out, result");
            Quaternion var0 = new Quaternion();

            for(int var1 = 0; var1 < 360; ++var1) {
               float var2 = PZMath.wrap((float)var1, -180.0F, 180.0F);
               var0.setFromAxisAngle(new Vector4f(0.0F, 1.0F, 0.0F, var2 * 0.017453292F));
               float var3 = HelperFunctions.getRotationY(var0) * 57.295776F;
               boolean var4 = PZMath.equal(var2, var3, 0.001F);
               DebugLog.UnitTests.printUnitTest("%f,%f", var4, var2, var3);
            }

            DebugLog.UnitTests.println("UnitTest_getRotationY. Complete");
         }
      }

      private static final class getRotationZ {
         private getRotationZ() {
         }

         public static void run() {
            DebugLog.UnitTests.println("UnitTest_getRotationZ");
            DebugLog.UnitTests.println("in, out, result");
            Quaternion var0 = new Quaternion();

            for(int var1 = 0; var1 < 360; ++var1) {
               float var2 = PZMath.wrap((float)var1, -180.0F, 180.0F);
               var0.setFromAxisAngle(new Vector4f(0.0F, 0.0F, 1.0F, var2 * 0.017453292F));
               float var3 = HelperFunctions.getRotationZ(var0) * 57.295776F;
               boolean var4 = PZMath.equal(var2, var3, 0.001F);
               DebugLog.UnitTests.printUnitTest("%f,%f", var4, var2, var3);
            }

            DebugLog.UnitTests.println("UnitTest_getRotationZ. Complete");
         }
      }
   }
}
