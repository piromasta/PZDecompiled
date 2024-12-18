package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class RDSPokerNight extends RandomizedDeadSurvivorBase {
   private String money = null;
   private String card = null;

   public RDSPokerNight() {
      this.name = "Poker Night";
      this.setChance(4);
      this.setMaximumDays(60);
      this.money = "Base.Money";
      this.card = "Base.CardDeck";
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

         if (this.getRoom(var1, "kitchen") != null) {
            return true;
         } else {
            this.debugLine = "No kitchen";
            return false;
         }
      }
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      RoomDef var2 = this.getRoom(var1, "kitchen");
      this.addZombies(var1, Rand.Next(3, 5), (String)null, 10, var2);
      this.addZombies(var1, 1, "PokerDealer", 0, var2);
      this.addRandomItemsOnGround(var2, this.getPokerNightClutter(), Rand.Next(3, 7));
      this.addRandomItemsOnGround(var2, this.money, Rand.Next(8, 13));
      this.addRandomItemsOnGround(var2, this.card, 1);
      var1.bAlarmed = false;
   }
}
