package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSBeachParty extends RandomizedZoneStoryBase {
   public RZSBeachParty() {
      this.name = "Beach Party";
      this.chance = 10;
      this.minZoneHeight = 13;
      this.minZoneWidth = 13;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Beach.toString());
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Lake.toString());
   }

   public static ArrayList<String> getBeachClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.Crisps");
      var0.add("Base.Crisps3");
      var0.add("Base.Pop");
      var0.add("Base.Whiskey");
      var0.add("Base.CigaretteSingle");
      var0.add("Base.BeerBottle");
      var0.add("Base.BeerBottle");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerCan");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      ArrayList var4 = getBeachClutter();
      ArrayList var5 = RZSForestCamp.getCoolerClutter();
      IsoGridSquare var6 = getSq(var2, var3, var1.z);
      int var7;
      if (Rand.NextBool(2)) {
         this.cleanSquareAndNeighbors(var6);
         var7 = Rand.Next(2);
         switch (var7) {
            case 0:
               this.addTileObject(var6, "camping_01_6");
               break;
            case 1:
               this.addCookingPit(var6);
         }
      }

      var7 = Rand.Next(1, 4);

      int var8;
      int var9;
      for(var8 = 0; var8 < var7; ++var8) {
         var9 = Rand.Next(4) + 1;
         switch (var9) {
            case 1:
               var9 = 25;
               break;
            case 2:
               var9 = 26;
               break;
            case 3:
               var9 = 28;
               break;
            case 4:
               var9 = 31;
         }

         IsoGridSquare var10 = this.getRandomExtraFreeSquare(this, var1);
         this.addTileObject(var10, "furniture_seating_outdoor_01_" + var9);
         if (var9 == 25) {
            var10 = getSq(var10.x, var10.y + 1, var10.z);
            this.addTileObject(var10, "furniture_seating_outdoor_01_24");
         } else if (var9 == 26) {
            var10 = getSq(var10.x + 1, var10.y, var10.z);
            this.addTileObject(var10, "furniture_seating_outdoor_01_27");
         } else if (var9 == 28) {
            var10 = getSq(var10.x, var10.y - 1, var10.z);
            this.addTileObject(var10, "furniture_seating_outdoor_01_29");
         } else {
            var10 = getSq(var10.x - 1, var10.y, var10.z);
            this.addTileObject(var10, "furniture_seating_outdoor_01_30");
         }
      }

      var7 = Rand.Next(1, 3);

      for(var8 = 0; var8 < var7; ++var8) {
         this.addTileObject(this.getRandomExtraFreeSquare(this, var1), "furniture_seating_outdoor_01_" + Rand.Next(16, 20));
      }

      InventoryContainer var12 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Cooler");
      var9 = Rand.Next(4, 8);

      int var13;
      for(var13 = 0; var13 < var9; ++var13) {
         var12.getItemContainer().AddItem((String)var5.get(Rand.Next(var5.size())));
      }

      this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), var12);
      var9 = Rand.Next(3, 7);

      for(var13 = 0; var13 < var9; ++var13) {
         this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), (String)var4.get(Rand.Next(var4.size())));
      }

      var13 = Rand.Next(3, 8);

      int var11;
      for(var11 = 0; var11 < var13; ++var11) {
         this.addZombiesOnSquare(1, "Swimmer", (Integer)null, this.getRandomExtraFreeSquare(this, var1));
      }

      var13 = Rand.Next(1, 3);

      for(var11 = 0; var11 < var13; ++var11) {
         this.addZombiesOnSquare(1, "Tourist", (Integer)null, this.getRandomExtraFreeSquare(this, var1));
      }

   }
}
