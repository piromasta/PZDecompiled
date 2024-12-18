package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Moveable;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class RBTrashed extends RandomizedBuildingBase {
   public RBTrashed() {
      this.name = "Trashed Building";
      this.setChance(5);
      this.setAlwaysDo(true);
   }

   public void randomizeBuilding(BuildingDef var1) {
      this.trashHouse(var1);
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      this.debugLine = "";
      if (GameClient.bClient) {
         return false;
      } else if (StashSystem.isStashBuilding(var1)) {
         this.debugLine = "Stash buildings are invalid";
         return false;
      } else if (SpawnPoints.instance.isSpawnBuilding(var1)) {
         this.debugLine = "Spawn houses are invalid";
         return false;
      } else if (var1.isAllExplored() && !var2) {
         return false;
      } else {
         if (!var2) {
            IsoGridSquare var3 = IsoCell.getInstance().getGridSquare(var1.x, var1.y, 0);
            int var4 = this.getChance(var3);
            if (Rand.Next(100) > var4) {
               return false;
            }

            for(int var5 = 0; var5 < GameServer.Players.size(); ++var5) {
               IsoPlayer var6 = (IsoPlayer)GameServer.Players.get(var5);
               if (var6.getSquare() != null && var6.getSquare().getBuilding() != null && var6.getSquare().getBuilding().def == var1) {
                  return false;
               }
            }
         }

         if (var1.getRooms().size() > 100) {
            this.debugLine = "Building is too large";
            return false;
         } else {
            return true;
         }
      }
   }

   public IsoGridSquare getFloorSquare(ArrayList<IsoGridSquare> var1, IsoGridSquare var2, RoomDef var3, IsoBuilding var4) {
      IsoGridSquare var5 = null;
      if (!Rand.NextBool(3)) {
         var5 = var2.getRandomAdjacentFreeSameRoom();
      } else if (!Rand.NextBool(5)) {
         var5 = ((IsoRoom)Objects.requireNonNull(((IsoBuilding)Objects.requireNonNull(var4)).getRandomRoom())).getRoomDef().getExtraFreeSquare();
      } else {
         var5 = var3.getExtraFreeSquare();
      }

      return var5;
   }

   public void trashHouse(BuildingDef var1) {
      IsoCell var2 = IsoWorld.instance.CurrentCell;
      int var3 = 40 + SandboxOptions.instance.getCurrentLootedChance();
      if (var3 > 90) {
         var3 = 90;
      }

      boolean var4 = Rand.NextBool(2);
      boolean var5 = false;

      int var6;
      int var8;
      for(var6 = var1.x - 1; var6 < var1.x2 + 1; ++var6) {
         for(int var7 = var1.y - 1; var7 < var1.y2 + 1; ++var7) {
            for(var8 = 0; var8 < 8; ++var8) {
               IsoGridSquare var9 = var2.getGridSquare(var6, var7, var8);
               if (var9 != null && var9.getRoom() != null && !var9.getRoom().getRoomDef().isKidsRoom()) {
                  IsoBuilding var10 = var9.getBuilding();
                  RoomDef var11 = var9.getRoom().getRoomDef();
                  boolean var12 = var11 != null && var11.isKidsRoom();
                  boolean var13 = !var12 && var9.getObjects().size() < 2 && var9.hasFloor() && !var9.isOutside();
                  ArrayList var14 = new ArrayList();

                  int var15;
                  for(var15 = 0; var15 < 7; ++var15) {
                     IsoGridSquare var16 = var9.getAdjacentSquare(IsoDirections.fromIndex(var15));
                     if (var16 != null && var16.isExtraFreeSquare() && var16.getRoom() != null && var16.getRoom() == var9.getRoom()) {
                        var14.add(var16);
                     }
                  }

                  if (var4 && this.isValidGraffSquare(var9, true, false) && Rand.Next(500) <= var3) {
                     this.graffSquare(var9, true);
                  }

                  if (var4 && this.isValidGraffSquare(var9, false, false) && Rand.Next(500) <= var3) {
                     this.graffSquare(var9, false);
                  }

                  for(var15 = 0; var15 < var9.getObjects().size(); ++var15) {
                     IsoObject var24 = (IsoObject)var9.getObjects().get(var15);
                     if (var24 instanceof IsoDoor && !var24.getSprite().getProperties().Is("GarageDoor")) {
                        if (Rand.Next(300) <= var3) {
                           ((IsoDoor)var24).destroy();
                        } else if (Rand.Next(150) <= var3) {
                           ((IsoDoor)var24).ToggleDoorSilent();
                           if (((IsoDoor)var24).isLocked()) {
                              ((IsoDoor)var24).setLocked(false);
                           }
                        } else if (Rand.Next(100) <= var3) {
                           ((IsoDoor)var24).setLocked(false);
                        }
                     }

                     if (var24 instanceof IsoWindow && Rand.Next(150) <= var3) {
                        ((IsoWindow)var24).smashWindow(true, false);
                        ((IsoWindow)var24).addBrokenGlass(Rand.NextBool(2));
                     }

                     if (var24.getContainer() != null && var24.getContainer().getItems() != null && !var24.getSprite().getProperties().Is("IsTrashCan")) {
                        int var17 = 0;

                        while(true) {
                           if (var17 >= var24.getContainer().getItems().size()) {
                              ItemPickerJava.updateOverlaySprite(var24);
                              var24.getContainer().setExplored(true);
                              break;
                           }

                           InventoryItem var18 = (InventoryItem)var24.getContainer().getItems().get(var17);
                           IsoGridSquare var19;
                           if (Rand.Next(200) < var3 && !Objects.equals(var18.getType(), "VHS_Home")) {
                              if (var18.getReplaceOnUseFullType() != null && var24.getSquare().getRoom() != null) {
                                 var19 = var24.getSquare().getRandomAdjacentFreeSameRoom();
                                 if (var19 == null || Rand.NextBool(3)) {
                                    var19 = var24.getSquare().getRoom().getRoomDef().getExtraFreeSquare();
                                 }

                                 if (var19 == null || Rand.NextBool(5)) {
                                    var19 = ((IsoRoom)Objects.requireNonNull(((IsoBuilding)Objects.requireNonNull(var24.getSquare().getBuilding())).getRandomRoom())).getRoomDef().getExtraFreeSquare();
                                 }

                                 if (var19 != null && !var19.isOutside() && var19.getRoom() != null && var19.hasRoomDef()) {
                                    this.addItemOnGround(var19, var18.getReplaceOnUseFullType());
                                 }
                              } else if (var18 instanceof DrainableComboItem && ((DrainableComboItem)var18).getReplaceOnDepleteFullType() != null && var24.getSquare().getRoom() != null) {
                                 var19 = this.getFloorSquare(var14, var9, var11, var10);
                                 if (var19 != null && !var19.isOutside() && var19.getRoom() != null && var19.hasRoomDef()) {
                                    this.addItemOnGround(var19, ((DrainableComboItem)var18).getReplaceOnDepleteFullType());
                                 }
                              } else if (var18 instanceof DrainableComboItem && var18.isKeepOnDeplete() && var24.getSquare().getRoom() != null) {
                                 var19 = this.getFloorSquare(var14, var9, var11, var10);
                                 if (var19 != null && !var19.isOutside() && var19.getRoom() != null && var19.hasRoomDef()) {
                                    InventoryItem var20 = InventoryItemFactory.CreateItem(var18.getFullType());
                                    var20.setCurrentUses(0);
                                    addItemOnGroundStatic(var19, var20);
                                 }
                              }

                              var24.getContainer().getItems().remove(var17);
                              --var17;
                           } else if (Rand.Next(100) < var3 && !(var18 instanceof Moveable)) {
                              var19 = this.getFloorSquare(var14, var9, var11, var10);
                              if (var19 != null && !var19.isOutside() && var19.getRoom() != null && var19.hasRoomDef()) {
                                 ItemPickerJava.trashItemLooted(var18);
                                 var24.getContainer().getItems().remove(var17);
                                 --var17;
                                 this.addItemOnGround(var19, var18);
                              }
                           }

                           ++var17;
                        }
                     }

                     if (!var5 && (Objects.equals(var24.getSprite().getName(), "location_shop_mall_01_18") || Objects.equals(var24.getSprite().getName(), "location_shop_mall_01_19"))) {
                        var9.RemoveTileObject(var24);
                        var9.RecalcProperties();
                        var9.RecalcAllWithNeighbours(true);
                        if (var9.getWindow() != null) {
                           var9.getWindow().smashWindow(true, false);
                        }

                        var5 = true;
                     }
                  }

                  if (var13 && Rand.Next(500) <= var3) {
                     this.trashSquare(var9);
                  }
               }
            }
         }
      }

      for(var6 = 0; var6 < var1.rooms.size(); ++var6) {
         RoomDef var21 = (RoomDef)var1.rooms.get(var6);
         var8 = (int)((float)GameTime.getInstance().getWorldAgeHours() / 24.0F + (float)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30)) / 10;
         if (var8 < 10) {
            var8 = 10;
         }

         int var22 = Math.min(var8 / 10, var21.getIsoRoom().getSquares().size());
         if (var8 >= 60 && var21 != null && Rand.Next(100) <= var22) {
            IsoDeadBody var23 = super.createSkeletonCorpse(var21);
            if (var23 != null) {
               var23.getHumanVisual().setSkinTextureIndex(1);
               super.addBloodSplat(var23.getCurrentSquare(), Rand.Next(7, 12));
            }
         }
      }

      var1.setAllExplored(true);
      var1.bAlarmed = false;
   }
}
