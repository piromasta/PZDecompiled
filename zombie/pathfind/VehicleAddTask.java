package zombie.pathfind;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayDeque;
import zombie.vehicles.BaseVehicle;

final class VehicleAddTask implements IVehicleTask {
   PolygonalMap2 map;
   BaseVehicle vehicle;
   final VehiclePoly poly = new VehiclePoly();
   final VehiclePoly polyPlusRadius = new VehiclePoly();
   final TFloatArrayList crawlOffsets = new TFloatArrayList();
   float upVectorDot;
   static final ArrayDeque<VehicleAddTask> pool = new ArrayDeque();

   VehicleAddTask() {
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
      this.crawlOffsets.resetQuick();
      this.crawlOffsets.addAll(var2.getScript().getCrawlOffsets());
      this.upVectorDot = var2.getUpVectorDot();
   }

   public void execute() {
      Vehicle var1 = Vehicle.alloc();
      var1.poly.init(this.poly);
      var1.polyPlusRadius.init(this.polyPlusRadius);
      var1.crawlOffsets.resetQuick();
      var1.crawlOffsets.addAll(this.crawlOffsets);
      var1.upVectorDot = this.upVectorDot;
      this.map.vehicles.add(var1);
      this.map.vehicleMap.put(this.vehicle, var1);
      this.vehicle = null;
   }

   static VehicleAddTask alloc() {
      synchronized(pool) {
         return pool.isEmpty() ? new VehicleAddTask() : (VehicleAddTask)pool.pop();
      }
   }

   public void release() {
      synchronized(pool) {
         assert !pool.contains(this);

         pool.push(this);
      }
   }
}
