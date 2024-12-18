package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

public class RZSVanCamp extends RandomizedZoneStoryBase {
   public RZSVanCamp() {
      this.name = "Van Camp";
      this.chance = 2;
      this.minZoneHeight = 6;
      this.minZoneWidth = 6;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Beach.toString());
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Lake.toString());
      this.setUnique(true);
   }

   public static ArrayList<String> getForestClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.HottieZ");
      var0.add("Base.Socks_Ankle");
      var0.add("Base.Boxers_White");
      var0.add("Base.Suit_Jacket");
      var0.add("Base.Suit_JacketTINT");
      var0.add("Base.Trousers_Suit");
      var0.add("Base.Tie_Full");
      var0.add("Base.Pillow");
      var0.add("Base.ToiletPaper");
      var0.add("Base.Shirt_FormalWhite_ShortSleeve");
      var0.add("Base.Shirt_FormalWhite");
      var0.add("Base.Hotdog");
      return var0;
   }

   public static ArrayList<String> getBriefcaseClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.HottieZ");
      var0.add("Base.Socks_Ankle");
      var0.add("Base.Boxers_White");
      var0.add("Base.Whiskey");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerBottle");
      var0.add("Base.BeefJerky");
      var0.add("Base.TVDinner");
      var0.add("Base.BeefJerky");
      var0.add("Base.Hotdog");
      var0.add("Base.Burger");
      var0.add("Base.BaloneySlice");
      var0.add("Base.Mustard");
      var0.add("Base.Coffee2");
      var0.add("Base.Suit_Jacket");
      var0.add("Base.Suit_JacketTINT");
      var0.add("Base.Trousers_Suit");
      var0.add("Base.Tie_Full");
      var0.add("Base.ToiletPaper");
      var0.add("Base.Shirt_FormalWhite_ShortSleeve");
      var0.add("Base.Shirt_FormalWhite");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      ArrayList var4 = getForestClutter();
      ArrayList var5 = getBriefcaseClutter();
      IsoGridSquare var6 = getSq(var2, var3, var1.z);
      this.cleanAreaForStory(this, var1);
      this.addCampfire(var6);
      this.addTileObject(var2, var3 - 2, var1.z, "furniture_seating_indoor_01_60");
      this.addTileObject(var2 - 1, var3 + 2, var1.z, "carpentry_02_76");
      this.addTileObject(var2, var3 + 2, var1.z, "carpentry_02_77");
      InventoryContainer var7 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Briefcase");
      int var8 = Rand.Next(2, 5);

      int var9;
      for(var9 = 0; var9 < var8; ++var9) {
         var7.getItemContainer().AddItem((String)var5.get(Rand.Next(var5.size())));
      }

      this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), var7);
      var8 = Rand.Next(2, 5);

      for(var9 = 0; var9 < var8; ++var9) {
         this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), (String)var4.get(Rand.Next(var4.size())));
      }

      this.addZombiesOnSquare(1, "OfficeWorker", 0, this.getRandomExtraFreeSquare(this, var1));
      var9 = var1.x;
      int var10 = var1.y;
      if (Rand.Next(2) == 0) {
         var9 += var1.getWidth();
      }

      if (Rand.Next(2) == 0) {
         var10 += var1.getHeight();
      }

      BaseVehicle var11 = this.addVehicle(var1, getSq(var9, var10, var1.z), (IsoChunk)null, (String)null, "Base.Van", (Integer)null, (IsoDirections)null, (String)null);
      if (var11 != null) {
         if (var11.getPassengerDoor(0) != null && var11.getPassengerDoor(0).getDoor() != null) {
            var11.getPassengerDoor(0).getDoor().setLocked(false);
         }

         var11.addKeyToWorld();
         var11.setAlarmed(false);
      }

   }
}
