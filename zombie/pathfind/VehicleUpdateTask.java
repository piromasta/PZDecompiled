package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.vehicles.BaseVehicle;

final class VehicleUpdateTask implements IVehicleTask {
   PolygonalMap2 map;
   BaseVehicle vehicle;
   final VehiclePoly poly = new VehiclePoly();
   final VehiclePoly polyPlusRadius = new VehiclePoly();
   float upVectorDot;
   static final ArrayDeque<VehicleUpdateTask> pool = new ArrayDeque();

   VehicleUpdateTask() {
   }

   public void init(PolygonalMap2 var1, BaseVehicle var2) {
      this.map = var1;
      this.vehicle = var2;
      this.poly.init(var2.getPoly());
      VehiclePoly var10000 = this.poly;
      var10000.z += 32.0F;
      this.polyPlusRadius.init(var2.getPolyPlusRadius());
      var10000 = this.polyPlusRadius;
      var10000.z += 32.0F;
      this.upVectorDot = var2.getUpVectorDot();
   }

   public void execute() {
      Vehicle var1 = (Vehicle)this.map.vehicleMap.get(this.vehicle);
      var1.poly.init(this.poly);
      var1.polyPlusRadius.init(this.polyPlusRadius);
      var1.upVectorDot = this.upVectorDot;
      this.vehicle = null;
   }

   static VehicleUpdateTask alloc() {
      synchronized(pool) {
         return pool.isEmpty() ? new VehicleUpdateTask() : (VehicleUpdateTask)pool.pop();
      }
   }

   public void release() {
      synchronized(pool) {
         assert !pool.contains(this);

         pool.push(this);
      }
   }
}
