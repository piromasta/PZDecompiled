package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.iso.BuildingDef;
import zombie.iso.SpawnPoints;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

public class RandomizedDeadSurvivorBase extends RandomizedBuildingBase {
   public RandomizedDeadSurvivorBase() {
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      return !SpawnPoints.instance.isSpawnBuilding(var1);
   }
}
