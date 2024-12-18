package zombie.randomizedWorld.randomizedBuilding;

import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;

public final class RBLooted extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      IsoCell var2 = IsoWorld.instance.CurrentCell;

      for(int var3 = var1.x - 1; var3 < var1.x2 + 1; ++var3) {
         for(int var4 = var1.y - 1; var4 < var1.y2 + 1; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               IsoGridSquare var6 = var2.getGridSquare(var3, var4, var5);
               if (var6 != null) {
                  for(int var7 = 0; var7 < var6.getObjects().size(); ++var7) {
                     IsoObject var8 = (IsoObject)var6.getObjects().get(var7);
                     if (Rand.Next(100) >= 85 && var8 instanceof IsoDoor && ((IsoDoor)var8).isExterior()) {
                        ((IsoDoor)var8).destroy();
                     }

                     if (Rand.Next(100) >= 85 && var8 instanceof IsoWindow) {
                        ((IsoWindow)var8).smashWindow(true, false);
                     }

                     if (var8.getContainer() != null && var8.getContainer().getItems() != null) {
                        for(int var9 = 0; var9 < var8.getContainer().getItems().size(); ++var9) {
                           if (Rand.Next(100) < 80) {
                              var8.getContainer().getItems().remove(var9);
                              --var9;
                           }
                        }

                        ItemPickerJava.updateOverlaySprite(var8);
                        var8.getContainer().setExplored(true);
                     }
                  }
               }
            }
         }
      }

      var1.setAllExplored(true);
      var1.bAlarmed = false;
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      if (!super.isValid(var1, var2)) {
         return false;
      } else if (var1.getRooms().size() > 20) {
         return false;
      } else if (SpawnPoints.instance.isSpawnBuilding(var1)) {
         this.debugLine = "Spawn houses are invalid";
         return false;
      } else if (StashSystem.isStashBuilding(var1)) {
         this.debugLine = "Stash buildings are invalid";
         return false;
      } else {
         return true;
      }
   }

   public RBLooted() {
      this.name = "Looted";
      this.setChance(10);
   }
}
