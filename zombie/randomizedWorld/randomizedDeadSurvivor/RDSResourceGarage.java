package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.characters.IsoPlayer;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class RDSResourceGarage extends RandomizedDeadSurvivorBase {
   public RDSResourceGarage() {
      this.name = "Resource Garage";
      this.setChance(10);
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      RoomDef var2 = this.getRoom(var1, "garagestorage");
      if (var2 == null) {
         var2 = this.getRoom(var1, "shed");
      }

      if (var2 == null) {
         var2 = this.getRoom(var1, "garage");
      }

      if (var2 == null) {
         var2 = this.getRoom(var1, "farmstorage");
      }

      if (var2 != null) {
         var2.getIsoRoom().spawnRandomWorkstation();
      }
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

         boolean var6 = false;

         for(int var7 = 0; var7 < var1.rooms.size(); ++var7) {
            RoomDef var5 = (RoomDef)var1.rooms.get(var7);
            if (("garagestorage".equals(var5.name) || "shed".equals(var5.name) || "garage".equals(var5.name) || "farmstorage".equals(var5.name)) && var5.area >= 9) {
               var6 = true;
               break;
            }
         }

         if (!var6) {
            this.debugLine = "No shed/garage or is too small";
         }

         return var6;
      }
   }
}
