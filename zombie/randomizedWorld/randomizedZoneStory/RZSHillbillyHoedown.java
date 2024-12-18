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

public class RZSHillbillyHoedown extends RandomizedZoneStoryBase {
   public RZSHillbillyHoedown() {
      this.name = "Hillbilly Hoedown";
      this.chance = 10;
      this.minZoneHeight = 6;
      this.minZoneWidth = 6;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
   }

   public static ArrayList<String> getBagClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.Whiskey");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerBottle");
      var0.add("Base.CigaretteSingle");
      var0.add("Base.Matches");
      var0.add("Base.ChickenFried");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      ArrayList var4 = getBagClutter();
      IsoGridSquare var5 = getSq(var2, var3, var1.z);
      this.cleanAreaForStory(this, var1);
      this.cleanSquareAndNeighbors(var5);
      this.addSimpleFire(var5);
      int var6 = Rand.Next(1, 3);

      int var7;
      for(var7 = 0; var7 < var6; ++var7) {
         this.addTileObject(this.getRandomExtraFreeSquare(this, var1), "location_restaurant_bar_01_26");
      }

      var7 = Rand.Next(3, 7);

      int var8;
      for(var8 = 0; var8 < var7; ++var8) {
         this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), this.getHoedownClutterItem());
      }

      this.addZombiesOnSquare(Rand.Next(3, 5), "Farmer", (Integer)null, this.getRandomExtraFreeSquare(this, var1));
      int var9;
      if (Rand.Next(2) == 0) {
         InventoryContainer var11 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.EmptySandbag");
         var7 = Rand.Next(2, 5);

         for(var9 = 0; var9 < var7; ++var9) {
            var11.getItemContainer().AddItem((String)var4.get(Rand.Next(var4.size())));
         }

         this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), var11);
      }

      if (Rand.Next(2) == 0) {
         var8 = var1.x;
         var9 = var1.y;
         if (Rand.Next(2) == 0) {
            var8 += var1.getWidth();
         }

         if (Rand.Next(2) == 0) {
            var9 += var1.getHeight();
         }

         BaseVehicle var10 = this.addVehicle(var1, getSq(var8, var9, var1.z), (IsoChunk)null, (String)null, "Base.PickUpTruck", (Integer)null, (IsoDirections)null, (String)null);
         if (var10 != null) {
            var10.addKeyToGloveBox();
            var10.addKeyToWorld();
            var10.setAlarmed(false);
         }
      }

   }
}
