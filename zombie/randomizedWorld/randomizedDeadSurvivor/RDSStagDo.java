package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import java.util.List;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.list.PZArrayUtil;

public final class RDSStagDo extends RandomizedDeadSurvivorBase {
   private final ArrayList<String> items = new ArrayList();
   private final ArrayList<String> otherItems = new ArrayList();
   private final ArrayList<String> outfits = new ArrayList();

   public RDSStagDo() {
      this.name = "Stag Do";
      this.setChance(2);
      this.setMaximumDays(60);
      this.otherItems.add("Base.CigaretteSingle");
      this.otherItems.add("Base.Whiskey");
      this.otherItems.add("Base.Wine");
      this.otherItems.add("Base.Wine2");
      this.otherItems.add("Base.WineBox");
      this.otherItems.add("Base.Cigar");
      this.otherItems.add("Base.Cigar");
      this.items.add("Base.Crisps");
      this.items.add("Base.Crisps2");
      this.items.add("Base.Crisps3");
      this.items.add("Base.Pop");
      this.items.add("Base.Pop2");
      this.items.add("Base.Pop3");
      this.outfits.add("NakedVeil");
      this.outfits.add("StripperBlack");
      this.outfits.add("StripperNaked");
      this.outfits.add("StripperPink");
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
      this.addZombies(var1, Rand.Next(5, 7), (String)null, 0, var2);
      this.addZombies(var1, 1, (String)PZArrayUtil.pickRandom((List)this.outfits), 100, var2);
      this.addRandomItemsOnGround(var2, this.items, Rand.Next(3, 7));
      this.addRandomItemsOnGround(var2, this.otherItems, Rand.Next(2, 6));
      var1.bAlarmed = false;
   }
}
