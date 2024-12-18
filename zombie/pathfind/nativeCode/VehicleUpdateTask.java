package zombie.pathfind.nativeCode;

import gnu.trove.list.array.TFloatArrayList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.system.MemoryUtil;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;

class VehicleUpdateTask implements IPathfindTask {
   ByteBuffer BB = null;
   static final ObjectPool<VehicleUpdateTask> pool = new ObjectPool(VehicleUpdateTask::new);

   VehicleUpdateTask() {
   }

   public VehicleUpdateTask init(BaseVehicle var1) {
      this.BB = MemoryUtil.memAlloc(256).order(ByteOrder.BIG_ENDIAN);
      this.BB.clear();

      assert var1.VehicleID != -1;

      this.BB.putInt(var1.VehicleID);
      var1.getPoly().toByteBuffer(this.BB);
      var1.getPolyPlusRadius().toByteBuffer(this.BB);
      this.BB.putFloat(var1.getUpVectorDot());
      TFloatArrayList var2 = var1.getScript().getCrawlOffsets();
      this.BB.put((byte)var2.size());

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         this.BB.putFloat(var2.get(var3));
      }

      return this;
   }

   public void execute() {
      PathfindNative.teleportVehicle(this.BB);
   }

   static VehicleUpdateTask alloc() {
      return (VehicleUpdateTask)pool.alloc();
   }

   public void release() {
      try {
         MemoryUtil.memFree(this.BB);
      } catch (IllegalArgumentException var3) {
         boolean var2 = true;
      }

      this.BB = null;
      pool.release((Object)this);
   }
}
