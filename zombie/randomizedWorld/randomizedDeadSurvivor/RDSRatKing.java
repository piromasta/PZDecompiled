package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class RDSRatKing extends RandomizedDeadSurvivorBase {
   public RDSRatKing() {
      this.name = "Rat King";
      this.setChance(1);
      this.setUnique(true);
      this.isRat = true;
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      String var2 = "bedroom";
      int var3 = Rand.Next(3);
      if (var3 == 0) {
         var2 = "kitchen";
      }

      if (var3 == 1) {
         var2 = "livingroom";
      }

      RoomDef var4 = this.getRoom(var1, var2);
      if (var4 == null) {
         var4 = this.getRoom(var1, "kitchen");
      }

      if (var4 == null) {
         var4 = this.getRoom(var1, "livingroom");
      }

      if (var4 == null) {
         var4 = this.getRoom(var1, "bedroom");
      }

      if (var4 != null) {
         this.addItemOnGround(var4.getFreeSquare(), "Base.RatKing");
         int var5 = var4.getIsoRoom().getSquares().size();
         if (var5 > 21) {
            var5 = 21;
         }

         int var6 = var5 / 2;
         if (var6 < 1) {
            var6 = 1;
         }

         if (var6 > 9) {
            var6 = 9;
         }

         int var7 = Rand.Next(var6, var5);

         int var8;
         for(var8 = 0; var8 < var7; ++var8) {
            IsoGridSquare var9 = var4.getFreeUnoccupiedSquare();
            if (var9 != null && var9.isFree(true)) {
               IsoAnimal var10;
               if (Rand.NextBool(2)) {
                  var10 = new IsoAnimal(IsoWorld.instance.getCell(), var9.getX(), var9.getY(), var9.getZ(), "rat", "grey");
               } else {
                  var10 = new IsoAnimal(IsoWorld.instance.getCell(), var9.getX(), var9.getY(), var9.getZ(), "ratfemale", "grey");
               }

               var10.randomizeAge();
               IsoDeadBody var11 = new IsoDeadBody(var10, false);
               var11.addToWorld();
            }
         }

         var8 = Rand.Next(var6, var5);

         int var12;
         for(var12 = 0; var12 < var8; ++var12) {
            IsoGridSquare var13 = var4.getFreeSquare();
            if (var13 != null && !var13.isOutside() && var13.getRoom() != null && var13.hasRoomDef()) {
               this.addItemOnGround(var13, "Base.Dung_Rat");
            }
         }

         RDSRatInfested.ratRoom(var4);

         for(var12 = 0; var12 < var1.rooms.size(); ++var12) {
            RDSRatInfested.ratRoom((RoomDef)var1.rooms.get(var12));
         }

      }
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
