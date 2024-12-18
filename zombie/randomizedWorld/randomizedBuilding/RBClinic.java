package zombie.randomizedWorld.randomizedBuilding;

import zombie.core.random.Rand;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;

public final class RBClinic extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      IsoCell var2 = IsoWorld.instance.CurrentCell;

      for(int var3 = var1.x - 1; var3 < var1.x2 + 1; ++var3) {
         for(int var4 = var1.y - 1; var4 < var1.y2 + 1; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               IsoGridSquare var6 = var2.getGridSquare(var3, var4, var5);
               if (var6 != null && this.roomValid(var6)) {
                  for(int var7 = 0; var7 < var6.getObjects().size(); ++var7) {
                     IsoObject var8 = (IsoObject)var6.getObjects().get(var7);
                     if (Rand.NextBool(2) && var8.getSurfaceOffsetNoTable() > 0.0F && var8.getContainer() == null && var6.getProperties().Val("waterAmount") == null && !var8.hasWater()) {
                        int var9 = Rand.Next(1, 3);

                        for(int var10 = 0; var10 < var9; ++var10) {
                           ItemSpawner.spawnItem(RBBasic.getMedicallutterItem(), var6, Rand.Next(0.4F, 0.6F), Rand.Next(0.4F, 0.6F), var8.getSurfaceOffsetNoTable() / 96.0F);
                        }
                     }
                  }
               }
            }
         }
      }

   }

   public boolean roomValid(IsoGridSquare var1) {
      return var1.getRoom() != null && ("hospitalroom".equals(var1.getRoom().getName()) || "clinic".equals(var1.getRoom().getName()) || "medical".equals(var1.getRoom().getName()));
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      return var1.getRoom("medical") != null || var1.getRoom("clinic") != null || var2;
   }

   public RBClinic() {
      this.name = "Clinic (Vet, Doctor..)";
      this.setAlwaysDo(true);
   }
}
