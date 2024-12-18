package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import java.util.Objects;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoRadio;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class RZSRockerParty extends RandomizedZoneStoryBase {
   public RZSRockerParty() {
      this.name = "Rocker Kids Partying";
      this.chance = 5;
      this.minZoneHeight = 6;
      this.minZoneWidth = 6;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Beach.toString());
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Lake.toString());
      this.setUnique(true);
   }

   public static ArrayList<String> getForestClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.WhiskeyFull");
      var0.add("Base.BeerBottle");
      var0.add("Base.BeerCan");
      var0.add("Base.HottieZ");
      var0.add("Base.WhiskeyEmpty");
      var0.add("Base.BeerCanEmpty");
      var0.add("Base.BeerEmpty");
      var0.add("Base.PlasticCup");
      var0.add("Base.SmashedBottle");
      var0.add("Base.CigaretteSingle");
      var0.add("Base.GuitarAcoustic");
      var0.add("Base.Cooler_Beer");
      return var0;
   }

   public static ArrayList<String> getBagClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.WhiskeyFull");
      var0.add("Base.BeerCan");
      var0.add("Base.BeerBottle");
      var0.add("Base.CigaretteSingle");
      var0.add("Base.LighterDisposable");
      var0.add("Base.CigaretteRollingPapers");
      return var0;
   }

   public static ArrayList<String> getFireClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.BeerCanEmpty");
      var0.add("Base.BeerEmpty");
      var0.add("Base.SmashedBottle");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      ArrayList var4 = getForestClutter();
      ArrayList var5 = getBagClutter();
      ArrayList var6 = getFireClutter();
      IsoGridSquare var7 = getSq(var2, var3, var1.z);
      this.cleanAreaForStory(this, var1);
      int var8 = Rand.Next(4);
      this.cleanSquareAndNeighbors(var7);
      this.addCampfireOrPit(var7);
      this.addItemOnGround(getSq(var2, var3, var1.z), (String)var6.get(Rand.Next(var6.size())));
      IsoRadio var9 = new IsoRadio(IsoWorld.instance.getCell(), getSq(var2 + 2, var3, var1.z), IsoSpriteManager.instance.getSprite("appliances_radio_01_8"));
      if (GameServer.bServer) {
         getSq(var2 + 2, var3, var1.z).transmitAddObjectToSquare(var9, -1);
      } else {
         getSq(var2 + 2, var3, var1.z).AddTileObject(var9);
      }

      var9.setRenderYOffset(0.0F);
      int var10;
      int var11;
      if (!Rand.NextBool(4)) {
         var10 = Rand.Next(-1, 2);
         var11 = Rand.Next(-1, 2);
         this.addSleepingBagOrTentWestEast(var2 + var10 - 3, var3 + var11, var1.z);
         if (Rand.Next(100) < 70) {
            this.addSleepingBagOrTentNorthSouth(var2 + var10, var3 + var11 - 3, var1.z);
         }

         if (Rand.Next(100) < 70) {
            this.addSleepingBagOrTentNorthSouth(var2 + var10 + 3, var3 + var11 - 2, var1.z);
         }
      }

      var10 = var1.x;
      var11 = var1.y;
      int var10000;
      if (Rand.Next(2) == 0) {
         var10000 = var10 + var1.getWidth();
      }

      if (Rand.Next(2) == 0) {
         var10000 = var11 + var1.getHeight();
      }

      String var12 = "Rocker";
      String var13 = "Base.Bag_Schoolbag";
      String var14 = "Base.PickUpTruck";
      if (var8 == 1) {
         var12 = "Punk";
         var13 = "Base.Bag_Schoolbag_Patches";
         var14 = "Base.VanSeats";
      } else if (var8 == 2) {
         var12 = "Redneck";
         var13 = "Base.Cooler";
         var14 = "Base.OffRoad";
      } else if (var8 == 3) {
         var12 = "Backpacker";
         var13 = "Base.Bag_Schoolbag_Travel";
         var14 = "Base.VanSeats";
      }

      this.addZombiesOnSquare(Rand.Next(3, 8), var12, 50, this.getRandomExtraFreeSquare(this, var1));
      int var15;
      int var16;
      if (Rand.Next(3) != 0) {
         var15 = var1.x;
         var16 = var1.y;
         if (Rand.Next(2) == 0) {
            var15 += var1.getWidth();
         }

         if (Rand.Next(2) == 0) {
            var16 += var1.getHeight();
         }

         BaseVehicle var17 = this.addVehicle(var1, getSq(var15, var16, var1.z), (IsoChunk)null, (String)null, var14, (Integer)null, (IsoDirections)null, (String)null);
         if (var17 != null) {
            var17.addKeyToWorld();
            if (var17.getPassengerDoor(0) != null && ((VehiclePart)Objects.requireNonNull(var17.getPassengerDoor(0))).getDoor() != null) {
               ((VehiclePart)Objects.requireNonNull(var17.getPassengerDoor(0))).getDoor().setLocked(false);
            }

            var17.setAlarmed(false);
            if (var8 != 3 && Rand.NextBool(2)) {
               int var18 = Rand.Next(1, 3);

               for(int var19 = 0; var19 < var18; ++var19) {
                  IsoGridSquare var20 = this.getRandomExtraFreeSquare(this, var1);
                  if (var20.getX() != var2 || var20.getY() != var3) {
                     this.addTileObject(var20, "construction_01_5");
                  }
               }
            }
         }
      }

      var15 = Rand.Next(3, 7);

      for(var16 = 0; var16 < var15; ++var16) {
         this.addItemOnGround(getRandomExtraFreeUnoccupiedSquare(this, var1), (String)var4.get(Rand.Next(var4.size())));
      }

      if (Rand.Next(2) == 0) {
         InventoryContainer var22 = (InventoryContainer)InventoryItemFactory.CreateItem(var13);
         var15 = Rand.Next(2, 5);

         for(int var21 = 0; var21 < var15; ++var21) {
            var22.getItemContainer().AddItem((String)var5.get(Rand.Next(var5.size())));
         }

         this.addItemOnGround(getRandomExtraFreeUnoccupiedSquare(this, var1), var22);
      }

   }
}
