package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.vehicles.BaseVehicle;

final class VehicleRemoveTask implements IVehicleTask {
   PolygonalMap2 map;
   BaseVehicle vehicle;
   static final ArrayDeque<VehicleRemoveTask> pool = new ArrayDeque();

   VehicleRemoveTask() {
   }

   public void init(PolygonalMap2 var1, BaseVehicle var2) {
      this.map = var1;
      this.vehicle = var2;
   }

   public void execute() {
      Vehicle var1 = (Vehicle)this.map.vehicleMap.remove(this.vehicle);
      if (var1 != null) {
         this.map.vehicles.remove(var1);
         var1.release();
      }

      this.vehicle = null;
   }

   static VehicleRemoveTask alloc() {
      synchronized(pool) {
         return pool.isEmpty() ? new VehicleRemoveTask() : (VehicleRemoveTask)pool.pop();
      }
   }

   public void release() {
      synchronized(pool) {
         assert !pool.contains(this);

         pool.push(this);
      }
   }
}
