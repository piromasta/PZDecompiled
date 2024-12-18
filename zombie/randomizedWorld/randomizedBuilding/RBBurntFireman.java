package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.vehicles.BaseVehicle;

public final class RBBurntFireman extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      var1.bAlarmed = false;
      int var2 = Rand.Next(1, 4);
      var1.setHasBeenVisited(true);
      IsoCell var3 = IsoWorld.instance.CurrentCell;

      for(int var4 = var1.x - 1; var4 < var1.x2 + 1; ++var4) {
         for(int var5 = var1.y - 1; var5 < var1.y2 + 1; ++var5) {
            for(int var6 = 0; var6 < 8; ++var6) {
               IsoGridSquare var7 = var3.getGridSquare(var4, var5, var6);
               if (var7 != null && Rand.Next(100) < 70) {
                  var7.Burn(false);
               }
            }
         }
      }

      var1.setAllExplored(true);
      BaseVehicle var8;
      if (Rand.NextBool(2)) {
         var8 = this.spawnCarOnNearestNav("Base.PickUpVanLightsFire", var1);
      } else {
         var8 = this.spawnCarOnNearestNav("Base.PickUpTruckLightsFire", var1);
      }

      if (var8 != null) {
         var8.setAlarmed(false);
      }

      String var9 = "FiremanFullSuit";
      if (var8 != null && var8.getZombieType() != null) {
         var9 = var8.getFirstZombieType();
      }

      ArrayList var10 = this.addZombies(var1, var2, var9, 35, this.getLivingRoomOrKitchen(var1));

      for(int var11 = 0; var11 < var10.size(); ++var11) {
         ((IsoZombie)var10.get(var11)).getInventory().setExplored(true);
      }

      if (var8 != null && !var10.isEmpty()) {
         ((IsoZombie)var10.get(Rand.Next(var10.size()))).addItemToSpawnAtDeath(var8.createVehicleKey());
      }

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

   public RBBurntFireman() {
      this.name = "Burnt Fireman";
      this.setChance(2);
   }
}
