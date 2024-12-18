package zombie.randomizedWorld.randomizedBuilding.TableStories;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;

public final class RBTSDrink extends RBTableStoryBase {
   public RBTSDrink() {
      this.chance = 7;
      this.rooms.add("livingroom");
      this.rooms.add("kitchen");
   }

   public void randomizeBuilding(BuildingDef var1) {
      this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.539F, 0.742F, this.table1.getSurfaceOffsetNoTable() / 96.0F, true);
      if (Rand.Next(70) < 100) {
         this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.734F, 0.797F, this.table1.getSurfaceOffsetNoTable() / 96.0F, true);
      }

      if (Rand.Next(70) < 100) {
         this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.554F, 0.57F, this.table1.getSurfaceOffsetNoTable() / 96.0F, true);
      }

      if (Rand.Next(70) < 100) {
         this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.695F, 0.336F, this.table1.getSurfaceOffsetNoTable() / 96.0F, true);
      }

      if (Rand.Next(70) < 100) {
         this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.875F, 0.687F, this.table1.getSurfaceOffsetNoTable() / 96.0F, true);
      }

      if (Rand.Next(70) < 100) {
         this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.476F, 0.273F, this.table1.getSurfaceOffsetNoTable() / 96.0F, true);
      }

      this.addWorldItem("Base.PlasticCup", this.table1.getSquare(), 0.843F, 0.531F, this.table1.getSurfaceOffsetNoTable() / 96.0F, true);
      if (Rand.NextBool(3)) {
         String var2 = "Base.Crisps";
         int var3 = Rand.Next(0, 4);
         switch (var3) {
            case 0:
               var2 = "Base.Crisps2";
               break;
            case 1:
               var2 = "Base.Crisps3";
               break;
            case 2:
               var2 = "Base.Crisps4";
         }

         this.addWorldItem(var2, this.table1.getSquare(), 0.87F, 0.86F, this.table1.getSurfaceOffsetNoTable() / 96.0F, true);
      }

      if (Rand.Next(70) < 100) {
         this.addWorldItem("Base.CigaretteSingle", this.table1.getSquare(), 0.406F, 0.843F, this.table1.getSurfaceOffsetNoTable() / 96.0F, true);
      }

      if (Rand.Next(70) < 100) {
         this.addWorldItem("Base.CigaretteSingle", this.table1.getSquare(), 0.578F, 0.953F, this.table1.getSurfaceOffsetNoTable() / 96.0F, true);
      }

   }

   public String getDrink() {
      return "Base.PlasticCup";
   }
}
