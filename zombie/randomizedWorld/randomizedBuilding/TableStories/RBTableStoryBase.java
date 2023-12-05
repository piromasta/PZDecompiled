package zombie.randomizedWorld.randomizedBuilding.TableStories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.core.Rand;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

public class RBTableStoryBase extends RandomizedBuildingBase {
   public static ArrayList<RBTableStoryBase> allStories = new ArrayList();
   public static int totalChance = 0;
   protected int chance = 0;
   protected ArrayList<String> rooms = new ArrayList();
   protected boolean need2Tables = false;
   protected boolean ignoreAgainstWall = false;
   protected IsoObject table2 = null;
   protected IsoObject table1 = null;
   protected boolean westTable = false;
   private static final HashMap<RBTableStoryBase, Integer> rbtsmap = new HashMap();
   private static final ArrayList<IsoObject> tableObjects = new ArrayList();
   public ArrayList<HashMap<String, Integer>> fullTableMap = new ArrayList();

   public RBTableStoryBase() {
   }

   public static void initStories(IsoGridSquare var0, IsoObject var1) {
      if (allStories.isEmpty()) {
         allStories.add(new RBTSBreakfast());
         allStories.add(new RBTSDinner());
         allStories.add(new RBTSSoup());
         allStories.add(new RBTSSewing());
         allStories.add(new RBTSElectronics());
         allStories.add(new RBTSFoodPreparation());
         allStories.add(new RBTSButcher());
         allStories.add(new RBTSSandwich());
         allStories.add(new RBTSDrink());
      }

      totalChance = 0;
      rbtsmap.clear();

      for(int var2 = 0; var2 < allStories.size(); ++var2) {
         RBTableStoryBase var3 = (RBTableStoryBase)allStories.get(var2);
         if (var3.isValid(var0, var1, false) && var3.isTimeValid(false)) {
            totalChance += var3.chance;
            rbtsmap.put(var3, var3.chance);
         }
      }

   }

   public static RBTableStoryBase getRandomStory(IsoGridSquare var0, IsoObject var1) {
      initStories(var0, var1);
      int var2 = Rand.Next(totalChance);
      Iterator var3 = rbtsmap.keySet().iterator();
      int var4 = 0;

      RBTableStoryBase var5;
      do {
         if (!var3.hasNext()) {
            return null;
         }

         var5 = (RBTableStoryBase)var3.next();
         var4 += (Integer)rbtsmap.get(var5);
      } while(var2 >= var4);

      var5.table1 = var1;
      return var5;
   }

   public boolean isValid(IsoGridSquare var1, IsoObject var2, boolean var3) {
      if (var3) {
         return true;
      } else if (this.rooms != null && var1.getRoom() != null && !this.rooms.contains(var1.getRoom().getName())) {
         return false;
      } else {
         if (this.need2Tables) {
            this.table2 = this.getSecondTable(var2);
            if (this.table2 == null) {
               return false;
            }
         }

         return !this.ignoreAgainstWall || !var1.getWallFull();
      }
   }

   public IsoObject getSecondTable(IsoObject var1) {
      this.westTable = true;
      IsoGridSquare var2 = var1.getSquare();
      if (this.ignoreAgainstWall && var2.getWallFull()) {
         return null;
      } else {
         var1.getSpriteGridObjects(tableObjects);
         IsoGridSquare var3 = var2.getAdjacentSquare(IsoDirections.W);
         IsoObject var4 = this.checkForTable(var3, var1, tableObjects);
         if (var4 == null) {
            var3 = var2.getAdjacentSquare(IsoDirections.E);
            var4 = this.checkForTable(var3, var1, tableObjects);
         }

         if (var4 == null) {
            this.westTable = false;
         }

         if (var4 == null) {
            var3 = var2.getAdjacentSquare(IsoDirections.N);
            var4 = this.checkForTable(var3, var1, tableObjects);
         }

         if (var4 == null) {
            var3 = var2.getAdjacentSquare(IsoDirections.S);
            var4 = this.checkForTable(var3, var1, tableObjects);
         }

         return var4 != null && this.ignoreAgainstWall && var3.getWallFull() ? null : var4;
      }
   }

   private IsoObject checkForTable(IsoGridSquare var1, IsoObject var2, ArrayList<IsoObject> var3) {
      if (var1 == null) {
         return null;
      } else if (var1.isSomethingTo(var2.getSquare())) {
         return null;
      } else {
         for(int var4 = 0; var4 < var1.getObjects().size(); ++var4) {
            IsoObject var5 = (IsoObject)var1.getObjects().get(var4);
            if ((var3.isEmpty() || var3.contains(var5)) && var5.getProperties().isTable() && var5.getContainer() == null && var5 != var2) {
               return var5;
            }
         }

         return null;
      }
   }
}
