package zombie.vehicles;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import zombie.popman.ObjectPool;

public final class ClipperPolygon {
   public final TFloatArrayList outer = new TFloatArrayList();
   public final ArrayList<TFloatArrayList> holes = new ArrayList();

   public ClipperPolygon() {
   }

   public ClipperPolygon makeCopy(ObjectPool<ClipperPolygon> var1, ObjectPool<TFloatArrayList> var2) {
      ClipperPolygon var3 = (ClipperPolygon)var1.alloc();
      var3.outer.clear();
      var3.outer.addAll(this.outer);
      var3.holes.clear();

      for(int var4 = 0; var4 < this.holes.size(); ++var4) {
         TFloatArrayList var5 = (TFloatArrayList)this.holes.get(var4);
         TFloatArrayList var6 = (TFloatArrayList)var2.alloc();
         var6.clear();
         var6.addAll(var5);
         var3.holes.add(var6);
      }

      return var3;
   }
}
