package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSForestCamp extends RandomizedZoneStoryBase {
   public RZSForestCamp() {
      this.name = "Basic Forest Camp";
      this.chance = 10;
      this.minZoneHeight = 6;
      this.minZoneWidth = 6;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
   }

   public static ArrayList<String> getForestClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.Crisps");
      var0.add("Base.Crisps2");
      var0.add("Base.Crisps3");
      var0.add("Base.Crisps4");
      var0.add("Base.Pop");
      var0.add("Base.Pop2");
      var0.add("Base.WaterBottle");
      var0.add("Base.CannedSardines");
      var0.add("Base.CannedChili");
      var0.add("Base.CannedBolognese");
      var0.add("Base.CannedCornedBeef");
      var0.add("Base.TinnedSoup");
      var0.add("Base.TinnedBeans");
      var0.add("Base.TunaTin");
      var0.add("Base.Whiskey");
      var0.add("Base.BeerBottle");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerCan");
      var0.add("Base.Lantern_Propane");
      var0.add("Base.Bag_PicnicBasket");
      var0.add("Base.GuitarAcoustic");
      return var0;
   }

   public static ArrayList<String> getCoolerClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.Pop");
      var0.add("Base.Pop2");
      var0.add("Base.BeefJerky");
      var0.add("Base.Ham");
      var0.add("Base.WaterBottle");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerCan");
      var0.add("Base.Smore");
      var0.add("Base.WineBox");
      var0.add("Base.Marshmallows");
      return var0;
   }

   public static ArrayList<String> getFireClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.WaterPotRice");
      var0.add("Base.Pot");
      var0.add("Base.WaterSaucepanRice");
      var0.add("Base.WaterSaucepanPasta");
      var0.add("Base.PotOfStew");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      ArrayList var4 = getForestClutter();
      ArrayList var5 = getCoolerClutter();
      ArrayList var6 = getFireClutter();
      IsoGridSquare var7 = getSq(var2, var3, var1.z);
      this.cleanAreaForStory(this, var1);
      this.cleanSquareAndNeighbors(var7);
      this.addCampfireOrPit(var7);
      this.addItemOnGround(getSq(var2, var3, var1.z), (String)var6.get(Rand.Next(var6.size())));
      int var8 = Rand.Next(-1, 2);
      int var9 = Rand.Next(-1, 2);
      this.addRandomTentWestEast(var2 + var8 - 2, var3 + var9, var1.z);
      if (Rand.Next(100) < 70) {
         this.addRandomTentNorthSouth(var2 + var8, var3 + var9 - 2, var1.z);
      }

      if (Rand.Next(100) < 30) {
         this.addRandomTentNorthSouth(var2 + var8 + 3, var3 + var9 - 2, var1.z);
      }

      this.addTileObject(var2 + 2, var3, var1.z, "furniture_seating_outdoor_01_19");
      InventoryContainer var10 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Cooler");
      int var11 = Rand.Next(2, 5);

      int var12;
      for(var12 = 0; var12 < var11; ++var12) {
         var10.getItemContainer().AddItem((String)var5.get(Rand.Next(var5.size())));
      }

      this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), var10);
      var11 = Rand.Next(3, 7);

      for(var12 = 0; var12 < var11; ++var12) {
         this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), (String)var4.get(Rand.Next(var4.size())));
      }

      String var13 = "Camper";
      if (Rand.NextBool(2)) {
         var13 = "Backpacker";
      }

      this.addZombiesOnSquare(Rand.Next(1, 3), var13, (Integer)null, this.getRandomExtraFreeSquare(this, var1));
   }
}
