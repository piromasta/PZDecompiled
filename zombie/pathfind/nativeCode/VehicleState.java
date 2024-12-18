package zombie.pathfind.nativeCode;

import zombie.pathfind.VehiclePoly;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;

class VehicleState {
   BaseVehicle vehicle;
   final VehiclePoly poly = new VehiclePoly();
   static final ObjectPool<VehicleState> pool = new ObjectPool(VehicleState::new);

   VehicleState() {
   }

   VehicleState init(BaseVehicle var1) {
      this.vehicle = var1;
      this.poly.init(var1.getPolyPlusRadius());
      return this;
   }

   boolean check() {
      if (!this.poly.isEqual(this.vehicle.getPolyPlusRadius())) {
         this.poly.init(this.vehicle.getPolyPlusRadius());
         return true;
      } else {
         return false;
      }
   }

   static VehicleState alloc() {
      return (VehicleState)pool.alloc();
   }

   void release() {
      pool.release((Object)this);
   }
}
