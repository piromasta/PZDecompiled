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

public final class RBPoliceSiege extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      var1.bAlarmed = false;
      IsoCell var2 = IsoWorld.instance.CurrentCell;

      int var5;
      for(int var3 = var1.x - 1; var3 < var1.x2 + 1; ++var3) {
         for(int var4 = var1.y - 1; var4 < var1.y2 + 1; ++var4) {
            for(var5 = 0; var5 < 8; ++var5) {
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

                     if (var8 instanceof IsoWindow && Rand.Next(100) <= 85) {
                        ((IsoWindow)var8).smashWindow(true, false);
                        var9 = var6.getRoom() == null ? var6 : ((IsoWindow)var8).getOppositeSquare();
                        if (((IsoWindow)var8).isBarricadeAllowed() && var5 == 0 && var9 != null && var9.getRoom() == null) {
                           var10 = var9 == var6;
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
                        }
                     }
                  }
               }
            }
         }
      }

      var1.bAlarmed = false;
      ArrayList var14 = this.addZombies(var1, Rand.Next(4) + 1, "Police", 50, (RoomDef)null);
      InventoryContainer var15 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_WeaponBag");
      ItemPickerJava.rollContainerItem(var15, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var15.getType()));
      this.addItemOnGround(var1.getFreeSquareInRoom(), var15);
      InventoryContainer var16;
      if (Rand.Next(2) == 0) {
         var16 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_SurvivorBag");
         ItemPickerJava.rollContainerItem(var16, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var16.getType()));
         this.addItemOnGround(var1.getFreeSquareInRoom(), var16);
      }

      if (Rand.Next(2) == 0) {
         var16 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_FoodCanned");
         ItemPickerJava.rollContainerItem(var16, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var16.getType()));
         this.addItemOnGround(var1.getFreeSquareInRoom(), var16);
      }

      if (Rand.Next(2) == 0) {
         var16 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_MedicalBag");
         ItemPickerJava.rollContainerItem(var16, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var16.getType()));
         this.addItemOnGround(var1.getFreeSquareInRoom(), var16);
      }

      if (Rand.Next(2) == 0) {
         var16 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_ProtectiveCaseBulkyAmmo");
         ItemPickerJava.rollContainerItem(var16, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var16.getType()));
         this.addItemOnGround(var1.getFreeSquareInRoom(), var16);
      }

      var5 = Rand.Next(2);
      String var17 = "Base.CarLightsPolice";
      switch (var5) {
         case 1:
            var17 = "Base.PickUpVanLightsPolice";
         default:
            BaseVehicle var18 = this.spawnCarOnNearestNav(var17, var1);
            if (var18 != null) {
               var18.setAlarmed(false);
               ((IsoZombie)var14.get(Rand.Next(var14.size()))).addItemToSpawnAtDeath(var18.createVehicleKey());
            }

      }
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      if (SpawnPoints.instance.isSpawnBuilding(var1)) {
         this.debugLine = "Spawn houses are invalid";
         return false;
      } else if (var1.getRooms().size() > 30) {
         return false;
      } else {
         return var1.getRoom("policestorage") != null;
      }
   }

   public RBPoliceSiege() {
      this.name = "Barricaded Police Station";
      this.setChance(5);
      this.setUnique(true);
   }
}
