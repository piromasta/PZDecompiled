package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class RDSHenDo extends RandomizedDeadSurvivorBase {
   public RDSHenDo() {
      this.name = "Hen Do";
      this.setChance(2);
      this.setMaximumDays(60);
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      this.debugLine = "";
      if (GameClient.bClient) {
         return false;
      } else if (var1.isAllExplored() && !var2) {
         return false;
      } else if (SpawnPoints.instance.isSpawnBuilding(var1)) {
         this.debugLine = "Spawn houses are invalid";
         return false;
      } else if (StashSystem.isStashBuilding(var1)) {
         this.debugLine = "Stash buildings are invalid";
         return false;
      } else {
         if (!var2) {
            for(int var3 = 0; var3 < GameServer.Players.size(); ++var3) {
               IsoPlayer var4 = (IsoPlayer)GameServer.Players.get(var3);
               if (var4.getSquare() != null && var4.getSquare().getBuilding() != null && var4.getSquare().getBuilding().def == var1) {
                  return false;
               }
            }
         }

         if (this.getRoom(var1, "livingroom") != null) {
            return true;
         } else {
            this.debugLine = "No living room";
            return false;
         }
      }
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      RoomDef var2 = this.getRoom(var1, "livingroom");
      this.addZombies(var1, Rand.Next(5, 7), (String)null, 100, var2);
      this.addZombies(var1, 1, "Naked", 0, var2);
      this.addRandomItemsOnGround(var2, this.getHenDoSnacks(), Rand.Next(3, 7));
      this.addRandomItemsOnGround(var2, this.getHenDoDrinks(), Rand.Next(2, 6));
      var1.bAlarmed = false;
   }
}
