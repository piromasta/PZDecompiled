package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import java.util.Objects;
import zombie.SandboxOptions;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Food;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class RDSRatInfested extends RandomizedDeadSurvivorBase {
   public RDSRatInfested() {
      this.name = "Rat Infested";
      this.setChance(1);
      this.isRat = true;
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      for(int var2 = 0; var2 < var1.rooms.size(); ++var2) {
         ratRoom((RoomDef)var1.rooms.get(var2));
      }

      var1.bAlarmed = false;
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
         }

         return true;
      }
   }

   public static void ratRoom(RoomDef var0) {
      IsoCell var1 = IsoWorld.instance.CurrentCell;
      int var2 = SandboxOptions.instance.getCurrentRatIndex();
      if (var2 >= 1) {
         if (var2 > 90) {
            var2 = 90;
         }

         int var3;
         int var4;
         int var5;
         IsoGridSquare var6;
         int var7;
         for(var3 = var0.x - 1; var3 < var0.x2 + 1; ++var3) {
            for(var4 = var0.y - 1; var4 < var0.y2 + 1; ++var4) {
               for(var5 = 0; var5 < 8; ++var5) {
                  var6 = var1.getGridSquare(var3, var4, var5);
                  if (var6 != null) {
                     for(var7 = 0; var7 < var6.getObjects().size(); ++var7) {
                        IsoObject var8 = (IsoObject)var6.getObjects().get(var7);
                        if (var8.getContainer() != null && var8.getContainer().getItems() != null && !Objects.equals(var8.getContainer().getType(), "fridge") && !Objects.equals(var8.getContainer().getType(), "freezer")) {
                           int var9 = 0;

                           while(true) {
                              if (var9 >= var8.getContainer().getItems().size()) {
                                 ItemPickerJava.updateOverlaySprite(var8);
                                 var8.getContainer().setExplored(true);
                                 break;
                              }

                              if (Rand.Next(100) < var2 && var8.getSquare().getRoom() != null) {
                                 IsoGridSquare var10 = var8.getSquare().getRoom().getRoomDef().getFreeSquare();
                                 InventoryItem var11 = (InventoryItem)var8.getContainer().getItems().get(var9);
                                 boolean var12 = (var11 instanceof Food || var11 instanceof Clothing) && !Objects.equals(var11.getType(), "DeadRat") && !Objects.equals(var11.getType(), "Dung_Rat");
                                 if (var10 != null && var12 && !var10.isOutside() && var10.getRoom() != null && var10.hasRoomDef()) {
                                    ItemPickerJava.trashItemRats(var11);
                                    if (Rand.NextBool(2)) {
                                       var8.getContainer().getItems().remove(var9);
                                       --var9;
                                       addItemOnGroundStatic(var10, var11);
                                    }

                                    if (Rand.NextBool(2)) {
                                       var8.getContainer().addItem(InventoryItemFactory.CreateItem("Base.Dung_Rat"));
                                    }
                                 }
                              }

                              ++var9;
                           }
                        }

                        if (var8.getContainer() != null && Rand.Next(100) < var2) {
                           var8.getContainer().addItem(InventoryItemFactory.CreateItem("Base.Dung_Rat"));
                        }

                        if (var8.getContainer() != null && Rand.Next(200) < var2) {
                           InventoryItem var19 = InventoryItemFactory.CreateItem("Base.DeadRat");
                           var19.setAutoAge();
                           var8.getContainer().addItem(var19);
                        }
                     }
                  }
               }
            }
         }

         var3 = var0.getIsoRoom().getSquares().size() / 3;
         if (var3 > var2) {
            var3 = var2;
         }

         if (var3 > 10) {
            var3 = 10;
         }

         var4 = var3 / 2;
         if (var4 < 1) {
            var4 = 1;
         }

         if (var3 < 1) {
            var3 = 1;
         }

         int var14;
         if (Rand.Next(100) < var2) {
            ArrayList var13 = new ArrayList();
            var14 = Rand.Next(var4, var3);

            for(var7 = 0; var7 < var14; ++var7) {
               IsoGridSquare var15 = var0.getFreeUnoccupiedSquare();
               String var21 = "grey";
               if (var0.getBuilding() != null && var0.getBuilding().getRoom("laboratory") != null && !Rand.NextBool(3)) {
                  var21 = "white";
               }

               if (var15 != null && var15.isFree(true) && !var13.contains(var15)) {
                  IsoAnimal var20;
                  if (Rand.NextBool(2)) {
                     var20 = new IsoAnimal(IsoWorld.instance.getCell(), var15.getX(), var15.getY(), var15.getZ(), "rat", var21);
                  } else {
                     var20 = new IsoAnimal(IsoWorld.instance.getCell(), var15.getX(), var15.getY(), var15.getZ(), "ratfemale", var21);
                  }

                  var20.addToWorld();
                  var20.randomizeAge();
                  if (Rand.NextBool(3)) {
                     var20.setStateEventDelayTimer(0.0F);
                  } else {
                     var13.add(var15);
                  }
               }
            }
         }

         var5 = Rand.Next(var4, var3);

         for(var14 = 0; var14 < var5; ++var14) {
            IsoGridSquare var16 = var0.getFreeSquare();
            if (var16 != null && !var16.isOutside() && var16.getRoom() != null && var16.hasRoomDef()) {
               addItemOnGroundStatic(var16, "Base.Dung_Rat");
            }
         }

         if (Rand.Next(200) < var2) {
            var6 = var0.getFreeUnoccupiedSquare();
            String var17 = "grey";
            if (var0.getBuilding() != null && var0.getBuilding().getRoom("laboratory") != null && !Rand.NextBool(3)) {
               var17 = "white";
            }

            if (var6 != null && var6.isFree(true)) {
               IsoAnimal var18;
               if (Rand.NextBool(2)) {
                  var18 = new IsoAnimal(IsoWorld.instance.getCell(), var6.getX(), var6.getY(), var6.getZ(), "rat", var17);
               } else {
                  var18 = new IsoAnimal(IsoWorld.instance.getCell(), var6.getX(), var6.getY(), var6.getZ(), "ratfemale", var17);
               }

               var18.randomizeAge();
               IsoDeadBody var22 = new IsoDeadBody(var18, false);
               var22.addToWorld();
            }
         }

      }
   }
}
