package zombie.popman.animal;

import java.util.ArrayList;
import zombie.iso.objects.IsoHutch;

public class HutchManager {
   private static final HutchManager instance = new HutchManager();
   private ArrayList<IsoHutch> hutchList = new ArrayList();

   public HutchManager() {
   }

   public static HutchManager getInstance() {
      return instance;
   }

   public void clear() {
      this.hutchList.clear();
   }

   public void add(IsoHutch var1) {
      this.hutchList.add(var1);
   }

   public void remove(IsoHutch var1) {
      this.hutchList.remove(var1);
   }

   public void reforceUpdate(IsoHutch var1) {
      if (!this.hutchList.contains(var1)) {
         this.hutchList.add(var1);
      }

   }

   public boolean checkHutchExistInList(IsoHutch var1) {
      for(int var2 = 0; var2 < this.hutchList.size(); ++var2) {
         IsoHutch var3 = (IsoHutch)this.hutchList.get(var2);
         if (var3.savedX == var1.getSquare().x && var3.savedY == var1.getSquare().y && var3.savedZ == var1.getSquare().z) {
            var3.square = var1.square;
            return true;
         }
      }

      return false;
   }

   public void updateAll() {
      for(int var1 = 0; var1 < this.hutchList.size(); ++var1) {
         IsoHutch var2 = (IsoHutch)this.hutchList.get(var1);
         var2.update();
      }

   }
}
