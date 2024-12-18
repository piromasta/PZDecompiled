package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;

public final class VehicleCluster {
   public int z;
   public final ArrayList<VehicleRect> rects = new ArrayList();
   static final ArrayDeque<VehicleCluster> pool = new ArrayDeque();

   public VehicleCluster() {
   }

   VehicleCluster init() {
      this.rects.clear();
      return this;
   }

   void merge(VehicleCluster var1) {
      for(int var2 = 0; var2 < var1.rects.size(); ++var2) {
         VehicleRect var3 = (VehicleRect)var1.rects.get(var2);
         var3.cluster = this;
      }

      this.rects.addAll(var1.rects);
      var1.rects.clear();
   }

   public VehicleRect bounds() {
      int var1 = 2147483647;
      int var2 = 2147483647;
      int var3 = -2147483648;
      int var4 = -2147483648;

      for(int var5 = 0; var5 < this.rects.size(); ++var5) {
         VehicleRect var6 = (VehicleRect)this.rects.get(var5);
         var1 = Math.min(var1, var6.left());
         var2 = Math.min(var2, var6.top());
         var3 = Math.max(var3, var6.right());
         var4 = Math.max(var4, var6.bottom());
      }

      return VehicleRect.alloc().init(var1, var2, var3 - var1, var4 - var2, this.z);
   }

   static VehicleCluster alloc() {
      return pool.isEmpty() ? new VehicleCluster() : (VehicleCluster)pool.pop();
   }

   void release() {
      assert !pool.contains(this);

      pool.push(this);
   }
}
