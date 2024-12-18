package zombie.pathfind;

import java.nio.ByteBuffer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import zombie.core.math.PZMath;
import zombie.core.physics.Transform;
import zombie.iso.Vector2;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.QuadranglesIntersection;

public final class VehiclePoly {
   private static final Vector3f tempVec3f_1 = new Vector3f();
   public Transform t = new Transform();
   public float x1;
   public float y1;
   public float x2;
   public float y2;
   public float x3;
   public float y3;
   public float x4;
   public float y4;
   public float z;
   public final Vector2[] borders = new Vector2[4];
   private static final Quaternionf tempQuat = new Quaternionf();

   public VehiclePoly() {
      for(int var1 = 0; var1 < this.borders.length; ++var1) {
         this.borders[var1] = new Vector2();
      }

   }

   public VehiclePoly init(VehiclePoly var1) {
      this.x1 = var1.x1;
      this.y1 = var1.y1;
      this.x2 = var1.x2;
      this.y2 = var1.y2;
      this.x3 = var1.x3;
      this.y3 = var1.y3;
      this.x4 = var1.x4;
      this.y4 = var1.y4;
      this.z = var1.z;
      return this;
   }

   public VehiclePoly init(BaseVehicle var1, float var2) {
      VehicleScript var3 = var1.getScript();
      Vector3f var4 = var3.getExtents();
      Vector3f var5 = var3.getCenterOfMassOffset();
      float var6 = 1.0F;
      Vector2[] var7 = this.borders;
      Quaternionf var8 = tempQuat;
      var1.getWorldTransform(this.t);
      this.t.getRotation(var8);
      float var9 = var4.x * var6 + var2 * 2.0F;
      float var10 = var4.z * var6 + var2 * 2.0F;
      float var11 = var4.y * var6 + var2 * 2.0F;
      var9 /= 2.0F;
      var10 /= 2.0F;
      var11 /= 2.0F;
      Vector3f var12 = tempVec3f_1;
      if (var8.x < 0.0F) {
         var1.getWorldPos(var5.x - var9, 0.0F, var5.z + var10, var12);
         var7[0].set(var12.x, var12.y);
         var1.getWorldPos(var5.x + var9, var11, var5.z + var10, var12);
         var7[1].set(var12.x, var12.y);
         var1.getWorldPos(var5.x + var9, var11, var5.z - var10, var12);
         var7[2].set(var12.x, var12.y);
         var1.getWorldPos(var5.x - var9, 0.0F, var5.z - var10, var12);
         var7[3].set(var12.x, var12.y);
         this.z = var1.getZ();
      } else {
         var1.getWorldPos(var5.x - var9, var11, var5.z + var10, var12);
         var7[0].set(var12.x, var12.y);
         var1.getWorldPos(var5.x + var9, 0.0F, var5.z + var10, var12);
         var7[1].set(var12.x, var12.y);
         var1.getWorldPos(var5.x + var9, 0.0F, var5.z - var10, var12);
         var7[2].set(var12.x, var12.y);
         var1.getWorldPos(var5.x - var9, var11, var5.z - var10, var12);
         var7[3].set(var12.x, var12.y);
         this.z = var1.getZ();
      }

      int var13 = 0;

      Vector2 var15;
      Vector2 var16;
      for(int var14 = 0; var14 < var7.length; ++var14) {
         var15 = var7[var14];
         var16 = var7[(var14 + 1) % var7.length];
         var13 = (int)((float)var13 + (var16.x - var15.x) * (var16.y + var15.y));
      }

      if (var13 < 0) {
         Vector2 var17 = var7[1];
         var15 = var7[2];
         var16 = var7[3];
         var7[1] = var16;
         var7[2] = var15;
         var7[3] = var17;
      }

      this.x1 = var7[0].x;
      this.y1 = var7[0].y;
      this.x2 = var7[1].x;
      this.y2 = var7[1].y;
      this.x3 = var7[2].x;
      this.y3 = var7[2].y;
      this.x4 = var7[3].x;
      this.y4 = var7[3].y;
      return this;
   }

   public static Vector2 lineIntersection(Vector2 var0, Vector2 var1, Vector2 var2, Vector2 var3) {
      Vector2 var4 = new Vector2();
      float var5 = var0.y - var1.y;
      float var6 = var1.x - var0.x;
      float var7 = -var5 * var0.x - var6 * var0.y;
      float var8 = var2.y - var3.y;
      float var9 = var3.x - var2.x;
      float var10 = -var8 * var2.x - var9 * var2.y;
      float var11 = QuadranglesIntersection.det(var5, var6, var8, var9);
      if (var11 != 0.0F) {
         var4.x = -QuadranglesIntersection.det(var7, var6, var10, var9) * 1.0F / var11;
         var4.y = -QuadranglesIntersection.det(var5, var7, var8, var10) * 1.0F / var11;
         return var4;
      } else {
         return null;
      }
   }

   VehicleRect getAABB(VehicleRect var1) {
      float var2 = Math.min(this.x1, Math.min(this.x2, Math.min(this.x3, this.x4)));
      float var3 = Math.min(this.y1, Math.min(this.y2, Math.min(this.y3, this.y4)));
      float var4 = Math.max(this.x1, Math.max(this.x2, Math.max(this.x3, this.x4)));
      float var5 = Math.max(this.y1, Math.max(this.y2, Math.max(this.y3, this.y4)));
      return var1.init((Vehicle)null, PZMath.fastfloor(var2), PZMath.fastfloor(var3), (int)Math.ceil((double)var4) - PZMath.fastfloor(var2), (int)Math.ceil((double)var5) - PZMath.fastfloor(var3), PZMath.fastfloor(this.z));
   }

   float isLeft(float var1, float var2, float var3, float var4, float var5, float var6) {
      return (var3 - var1) * (var6 - var2) - (var5 - var1) * (var4 - var2);
   }

   public boolean containsPoint(float var1, float var2) {
      int var3 = 0;

      for(int var4 = 0; var4 < 4; ++var4) {
         Vector2 var5 = this.borders[var4];
         Vector2 var6 = var4 == 3 ? this.borders[0] : this.borders[var4 + 1];
         if (var5.y <= var2) {
            if (var6.y > var2 && this.isLeft(var5.x, var5.y, var6.x, var6.y, var1, var2) > 0.0F) {
               ++var3;
            }
         } else if (var6.y <= var2 && this.isLeft(var5.x, var5.y, var6.x, var6.y, var1, var2) < 0.0F) {
            --var3;
         }
      }

      return var3 != 0;
   }

   public boolean isEqual(VehiclePoly var1) {
      return PZMath.equal(this.x1, var1.x1, 0.001F) && PZMath.equal(this.y1, var1.y1, 0.001F) && PZMath.equal(this.x2, var1.x2, 0.001F) && PZMath.equal(this.y2, var1.y2, 0.001F) && PZMath.equal(this.x3, var1.x3, 0.001F) && PZMath.equal(this.y3, var1.y3, 0.001F) && PZMath.equal(this.x4, var1.x4, 0.001F) && PZMath.equal(this.y4, var1.y4, 0.001F);
   }

   public void toByteBuffer(ByteBuffer var1) {
      var1.putFloat(this.x1);
      var1.putFloat(this.y1);
      var1.putFloat(this.x2);
      var1.putFloat(this.y2);
      var1.putFloat(this.x3);
      var1.putFloat(this.y3);
      var1.putFloat(this.x4);
      var1.putFloat(this.y4);
      var1.putFloat(this.z + 32.0F);
   }
}
