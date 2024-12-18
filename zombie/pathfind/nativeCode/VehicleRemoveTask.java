package zombie.pathfind.nativeCode;

import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;

class VehicleRemoveTask implements IPathfindTask {
   short VehicleID;
   static final ObjectPool<VehicleRemoveTask> pool = new ObjectPool(VehicleRemoveTask::new);

   VehicleRemoveTask() {
   }

   public VehicleRemoveTask init(BaseVehicle var1) {
      this.VehicleID = var1.VehicleID;

      assert this.VehicleID != -1;

      return this;
   }

   public void execute() {
      PathfindNative.removeVehicle(this.VehicleID);
   }

   static VehicleRemoveTask alloc() {
      return (VehicleRemoveTask)pool.alloc();
   }

   public void release() {
      pool.release((Object)this);
   }
}
