package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;

public final class RBGunstoreSiege extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      var1.bAlarmed = false;
      IsoCell var2 = IsoWorld.instance.CurrentCell;

      int var3;
      for(var3 = var1.x - 1; var3 < var1.x2 + 1; ++var3) {
         for(int var4 = var1.y - 1; var4 < var1.y2 + 1; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               IsoGridSquare var6 = var2.getGridSquare(var3, var4, var5);
               if (var6 != null) {
                  for(int var7 = 0; var7 < var6.getObjects().size(); ++var7) {
                     IsoObject var8 = (IsoObject)var6.getObjects().get(var7);
                     IsoGridSquare var9;
                     boolean var10;
                     IsoBarricade var11;
                     int var12;
                     int var13;
                     if (var8 instanceof IsoDoor && ((IsoDoor)var8).isBarricadeAllowed() && !SpawnPoints.instance.isSpawnBuilding(var1)) {
                        var9 = var6.getRoom() == null ? var6 : ((IsoDoor)var8).getOppositeSquare();
                        if (var9 != null && var9.getRoom() == null) {
                           var10 = var9 == var6;
                           var11 = IsoBarricade.AddBarricadeToObject((IsoDoor)var8, var10);
                           if (var11 != null) {
                              if (Rand.Next(2) == 0) {
                                 var12 = Rand.Next(1, 4);

                                 for(var13 = 0; var13 < var12; ++var13) {
                                    var11.addPlank((IsoGameCharacter)null, (InventoryItem)null);
                                 }
                              } else {
                                 var11.addMetal((IsoGameCharacter)null, (InventoryItem)null);
                              }

                              if (GameServer.bServer) {
                                 var11.transmitCompleteItemToClients();
                              }
                           }
                        }
                     }

                     if (var8 instanceof IsoWindow) {
                        var9 = var6.getRoom() == null ? var6 : ((IsoWindow)var8).getOppositeSquare();
                        if (((IsoWindow)var8).isBarricadeAllowed() && var5 == 0 && var9 != null && var9.getRoom() == null) {
                           var10 = var9 != var6;
                           var11 = IsoBarricade.AddBarricadeToObject((IsoWindow)var8, var10);
                           if (var11 != null) {
                              if (Rand.Next(2) == 0) {
                                 var12 = Rand.Next(1, 4);

                                 for(var13 = 0; var13 < var12; ++var13) {
                                    var11.addPlank((IsoGameCharacter)null, (InventoryItem)null);
                                 }
                              } else {
                                 var11.addMetal((IsoGameCharacter)null, (InventoryItem)null);
                              }

                              if (GameServer.bServer) {
                                 var11.transmitCompleteItemToClients();
                              }
                           }
                        } else {
                           ((IsoWindow)var8).addSheet((IsoGameCharacter)null);
                           ((IsoWindow)var8).HasCurtains().ToggleDoor((IsoGameCharacter)null);
                        }
                     }
                  }
               }
            }
         }
      }

      var1.bAlarmed = false;
      var3 = Rand.Next(3);
      String var14 = "Hunter";
      switch (var3) {
         case 1:
            var14 = "Survivalist03";
            break;
         case 2:
            var14 = "Veteran";
      }

      ArrayList var15 = this.addZombies(var1, 1, var14, 0, (RoomDef)null);
      InventoryContainer var16 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_WeaponBag");
      ItemPickerJava.rollContainerItem(var16, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var16.getType()));
      this.addItemOnGround(var1.getFreeSquareInRoom(), var16);
      InventoryContainer var17;
      if (Rand.Next(2) == 0) {
         var17 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_SurvivorBag");
         ItemPickerJava.rollContainerItem(var17, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var17.getType()));
         this.addItemOnGround(var1.getFreeSquareInRoom(), var17);
      }

      if (Rand.Next(2) == 0) {
         var17 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_FoodCanned");
         ItemPickerJava.rollContainerItem(var17, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var17.getType()));
         this.addItemOnGround(var1.getFreeSquareInRoom(), var17);
      }

      if (Rand.Next(2) == 0) {
         var17 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_MedicalBag");
         ItemPickerJava.rollContainerItem(var17, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var17.getType()));
         this.addItemOnGround(var1.getFreeSquareInRoom(), var17);
      }

      if (Rand.Next(2) == 0) {
         var17 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_ProtectiveCaseBulkyAmmo");
         ItemPickerJava.rollContainerItem(var17, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var17.getType()));
         this.addItemOnGround(var1.getFreeSquareInRoom(), var17);
      }

      if (Rand.Next(2) == 0) {
         String var19 = "Base.OffRoad";
         int var18 = Rand.Next(9);
         if (var18 == 0) {
            var19 = "Base.OffRoad";
         } else if (var18 == 1) {
            var19 = "Base.PickUpVan";
         } else if (var18 == 2) {
            var19 = "Base.PickUpVanLightsFire";
         } else if (var18 == 3) {
            var19 = "Base.PickUpTruck";
         } else if (var18 == 4) {
            var19 = "Base.PickUpTruckLightsRanger";
         } else if (var18 == 5) {
            var19 = "Base.StepVan";
         } else if (var18 == 6) {
            var19 = "Base.SUV";
         } else if (var18 == 7) {
            var19 = "Base.Van";
         } else if (var18 == 8) {
            var19 = "Base.VanSeats";
         }

         BaseVehicle var20 = this.spawnCarOnNearestNav(var19, var1, "Survivalist");
         if (var20 != null) {
            var20.setAlarmed(false);
            ((IsoZombie)var15.get(Rand.Next(var15.size()))).addItemToSpawnAtDeath(var20.createVehicleKey());
         }
      }

   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      if (SpawnPoints.instance.isSpawnBuilding(var1)) {
         this.debugLine = "Spawn houses are invalid";
         return false;
      } else if (var1.getRooms().size() > 20) {
         return false;
      } else {
         return var1.getRoom("gunstore") != null;
      }
   }

   public RBGunstoreSiege() {
      this.name = "Barricaded Gunstore";
      this.setChance(10);
      this.setUnique(true);
   }
}
