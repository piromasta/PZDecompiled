package zombie.pathfind;

import java.util.ArrayDeque;
import org.joml.Vector3f;
import zombie.core.math.PZMath;
import zombie.vehicles.BaseVehicle;

final class VehicleState {
   BaseVehicle vehicle;
   float x;
   float y;
   float z;
   final Vector3f forward = new Vector3f();
   final VehiclePoly polyPlusRadius = new VehiclePoly();
   static final ArrayDeque<VehicleState> pool = new ArrayDeque();

   VehicleState() {
   }

   VehicleState init(BaseVehicle var1) {
      this.vehicle = var1;
      this.x = var1.getX();
      this.y = var1.getY();
      this.z = var1.getZ();
      var1.getForwardVector(this.forward);
      this.polyPlusRadius.init(var1.getPolyPlusRadius());
      return this;
   }

   boolean check() {
      boolean var1 = this.x != this.vehicle.getX() || this.y != this.vehicle.getY() || PZMath.fastfloor(this.z) != PZMath.fastfloor(this.vehicle.getZ());
      if (!var1) {
         BaseVehicle.Vector3fObjectPool var2 = (BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get();
         Vector3f var3 = this.vehicle.getForwardVector((Vector3f)var2.alloc());
         var1 = this.forward.dot(var3) < 0.999F;
         if (var1) {
            this.forward.set(var3);
         }

         var2.release(var3);
      }

      if (var1) {
         this.x = this.vehicle.getX();
         this.y = this.vehicle.getY();
         this.z = this.vehicle.getZ();
      }

      return var1;
   }

   static VehicleState alloc() {
      return pool.isEmpty() ? new VehicleState() : (VehicleState)pool.pop();
   }

   void release() {
      assert !pool.contains(this);

      pool.push(this);
   }
}
