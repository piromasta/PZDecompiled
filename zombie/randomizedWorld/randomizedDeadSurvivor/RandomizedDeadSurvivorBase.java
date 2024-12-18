package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.characters.IsoPlayer;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

public class RandomizedDeadSurvivorBase extends RandomizedBuildingBase {
   public RandomizedDeadSurvivorBase() {
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      if (!var2) {
         for(int var3 = 0; var3 < GameServer.Players.size(); ++var3) {
            IsoPlayer var4 = (IsoPlayer)GameServer.Players.get(var3);
            if (var4.getSquare() != null && var4.getSquare().getBuilding() != null && var4.getSquare().getBuilding().def == var1) {
               return false;
            }
         }
      }

      if (SpawnPoints.instance.isSpawnBuilding(var1)) {
         this.debugLine = "Spawn houses are invalid";
      }

      if (StashSystem.isStashBuilding(var1)) {
         this.debugLine = "Stash buildings are invalid";
         return false;
      } else {
         return !SpawnPoints.instance.isSpawnBuilding(var1);
      }
   }
}
