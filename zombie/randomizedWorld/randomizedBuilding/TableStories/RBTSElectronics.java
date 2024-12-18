package zombie.randomizedWorld.randomizedBuilding.TableStories;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;

public final class RBTSElectronics extends RBTableStoryBase {
   public RBTSElectronics() {
      this.chance = 5;
      this.rooms.add("livingroom");
      this.rooms.add("kitchen");
      this.rooms.add("bedroom");
   }

   public void randomizeBuilding(BuildingDef var1) {
      String var2 = "Base.ElectronicsMag1";
      int var3 = Rand.Next(4);
      switch (var3) {
         case 0:
            var2 = "Base.ElectronicsMag2";
            break;
         case 1:
            var2 = "Base.ElectronicsMag3";
            break;
         case 2:
            var2 = "Base.ElectronicsMag5";
      }

      this.addWorldItem(var2, this.table1.getSquare(), 0.36F, 0.789F, this.table1.getSurfaceOffsetNoTable() / 96.0F);
      this.addWorldItem("Base.ElectronicsScrap", this.table1.getSquare(), 0.71F, 0.82F, this.table1.getSurfaceOffsetNoTable() / 96.0F);
      this.addWorldItem("Base.Screwdriver", this.table1.getSquare(), 0.36F, 0.421F, this.table1.getSurfaceOffsetNoTable() / 96.0F);
      var2 = "Base.CDPlayer";
      var3 = Rand.Next(7);
      switch (var3) {
         case 0:
            var2 = "Base.Torch";
            break;
         case 1:
            var2 = "Base.Remote";
            break;
         case 2:
            var2 = "Base.VideoGame";
            break;
         case 3:
            var2 = "Base.CordlessPhone";
            break;
         case 4:
            var2 = "Base.Headphones";
            break;
         case 5:
            var2 = "Base.HairDryer";
      }

      this.addWorldItem(var2, this.table1.getSquare(), 0.695F, 0.43F, this.table1.getSurfaceOffsetNoTable() / 96.0F);
   }
}
