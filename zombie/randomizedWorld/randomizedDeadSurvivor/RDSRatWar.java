package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.characters.IsoPlayer;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class RDSRatWar extends RandomizedDeadSurvivorBase {
   public RDSRatWar() {
      this.name = "Rat War";
      this.setChance(0);
      this.setUnique(true);
      this.isRat = true;
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      for(int var2 = 0; var2 < var1.rooms.size(); ++var2) {
         RDSRatInfested.ratRoom((RoomDef)var1.rooms.get(var2));
      }

      var1.bAlarmed = false;
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      this.debugLine = "";
      if (GameClient.bClient) {
         return false;
      } else if (SpawnPoints.instance.isSpawnBuilding(var1)) {
         this.debugLine = "Spawn houses are invalid";
         return false;
      } else if (StashSystem.isStashBuilding(var1)) {
         this.debugLine = "Stash buildings are invalid";
         return false;
      } else if (var1.isAllExplored() && !var2) {
         return false;
      } else if (this.getRoom(var1, "kitchen") == null && this.getRoom(var1, "bedroom") == null) {
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

         if (var1.getRooms().size() > 100) {
            this.debugLine = "Building is too large";
            return false;
         } else {
            return true;
         }
      }
   }
}
