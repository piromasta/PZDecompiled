package zombie.randomizedWorld.randomizedBuilding;

import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;

public final class RBOffice extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      IsoCell var2 = IsoWorld.instance.CurrentCell;

      for(int var3 = var1.x - 1; var3 < var1.x2 + 1; ++var3) {
         for(int var4 = var1.y - 1; var4 < var1.y2 + 1; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               IsoGridSquare var6 = var2.getGridSquare(var3, var4, var5);
               if (var6 != null && this.roomValid(var6)) {
                  RBBasic.doOfficeStuff(var6);
               }
            }
         }
      }

   }

   public boolean roomValid(IsoGridSquare var1) {
      return var1.getRoom() != null && ("office".equals(var1.getRoom().getName()) || "cardealershipoffice".equals(var1.getRoom().getName()));
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      return var1.getRoom("office") != null || var1.getRoom("cardealershipoffice") != null || var2;
   }

   public RBOffice() {
      this.name = "Offices";
      this.setAlwaysDo(true);
   }
}
