package zombie.randomizedWorld.randomizedBuilding;

import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.characters.IsoGameCharacter;
import zombie.characters.ZombiesStageDefinitions;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;

public final class RBSafehouse extends RandomizedBuildingBase {
   public void randomizeBuilding(BuildingDef var1) {
      var1.bAlarmed = false;
      var1.setHasBeenVisited(true);
      String var2 = "SafehouseLoot";
      int var3 = (int)(GameTime.getInstance().getWorldAgeHours() / 24.0) + (SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30;
      if (var3 >= ZombiesStageDefinitions.daysLate) {
         var2 = "SafehouseLoot_Late";
      } else if (var3 >= ZombiesStageDefinitions.daysMid) {
         var2 = "SafehouseLoot_Mid";
      }

      ItemPickerJava.ItemPickerRoom var4 = (ItemPickerJava.ItemPickerRoom)ItemPickerJava.rooms.get(var2);
      IsoCell var5 = IsoWorld.instance.CurrentCell;
      int var6 = 40 + SandboxOptions.instance.getCurrentLootedChance();
      if (var6 > 90) {
         var6 = 90;
      }

      boolean var7 = Rand.NextBool(8);
      boolean var8 = var7 || Rand.NextBool(2);

      for(int var9 = var1.x - 1; var9 < var1.x2 + 1; ++var9) {
         for(int var10 = var1.y - 1; var10 < var1.y2 + 1; ++var10) {
            for(int var11 = 0; var11 < 8; ++var11) {
               IsoGridSquare var12 = var5.getGridSquare(var9, var10, var11);
               if (var12 != null) {
                  boolean var13 = var12.getRoom() != null && var12.getRoom().getRoomDef() != null && var12.getRoom().getRoomDef().isKidsRoom();
                  boolean var14 = var8 && !var13 && var12.getObjects().size() < 2 && var12.hasFloor() && !var12.isOutside();

                  for(int var15 = 0; var15 < var12.getObjects().size(); ++var15) {
                     IsoObject var16 = (IsoObject)var12.getObjects().get(var15);
                     IsoGridSquare var17;
                     boolean var18;
                     IsoBarricade var19;
                     int var20;
                     int var21;
                     if (var16 instanceof IsoDoor && ((IsoDoor)var16).isBarricadeAllowed() && !SpawnPoints.instance.isSpawnBuilding(var1)) {
                        var17 = var12.getRoom() == null ? var12 : ((IsoDoor)var16).getOppositeSquare();
                        if (var17 != null && var17.getRoom() == null) {
                           var18 = var17 != var12;
                           var19 = IsoBarricade.AddBarricadeToObject((IsoDoor)var16, var18);
                           if (var19 != null) {
                              var20 = Rand.Next(1, 4);

                              for(var21 = 0; var21 < var20; ++var21) {
                                 var19.addPlank((IsoGameCharacter)null, (InventoryItem)null);
                              }

                              if (GameServer.bServer) {
                                 var19.transmitCompleteItemToClients();
                              }
                           }
                        }
                     }

                     if (var16 instanceof IsoWindow) {
                        var17 = var12.getRoom() == null ? var12 : ((IsoWindow)var16).getOppositeSquare();
                        if (((IsoWindow)var16).isBarricadeAllowed() && var11 == 0 && var17 != null && var17.getRoom() == null) {
                           var18 = var17 != var12;
                           var19 = IsoBarricade.AddBarricadeToObject((IsoWindow)var16, var18);
                           if (var19 != null) {
                              var20 = Rand.Next(1, 4);

                              for(var21 = 0; var21 < var20; ++var21) {
                                 var19.addPlank((IsoGameCharacter)null, (InventoryItem)null);
                              }

                              if (GameServer.bServer) {
                                 var19.transmitCompleteItemToClients();
                              }
                           }
                        } else {
                           ((IsoWindow)var16).addSheet((IsoGameCharacter)null);
                           ((IsoWindow)var16).HasCurtains().ToggleDoor((IsoGameCharacter)null);
                        }
                     }

                     if (!var13 && var16.getContainer() != null && var12.getRoom() != null && var12.getRoom().getBuilding().getDef() == var1 && Rand.Next(100) <= 70 && var12.getRoom().getName() != null && var4.Containers.containsKey(var16.getContainer().getType())) {
                        ItemPickerJava.fillContainerType(var4, var16.getContainer(), "", (IsoGameCharacter)null);
                        ItemPickerJava.updateOverlaySprite(var16);
                        var16.getContainer().setExplored(true);
                     }

                     if (!var13 && var7 && this.isValidGraffSquare(var12, true, false) && Rand.Next(500) <= var6) {
                        this.graffSquare(var12, true);
                     }

                     if (!var13 && var7 && this.isValidGraffSquare(var12, false, false) && Rand.Next(500) <= var6) {
                        this.graffSquare(var12, false);
                     }

                     if (var14 && Rand.Next(1000) <= var6) {
                        this.trashSquare(var12);
                     }
                  }
               }
            }
         }
      }

      var1.setAllExplored(true);
      var1.bAlarmed = false;
      this.addZombies(var1, var7);
   }

   private void addZombies(BuildingDef var1, boolean var2) {
      // $FF: Couldn't be decompiled
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      if (!super.isValid(var1, var2)) {
         return false;
      } else if (var1.getRooms().size() > 20) {
         return false;
      } else if (SpawnPoints.instance.isSpawnBuilding(var1)) {
         this.debugLine = "Spawn houses are invalid";
         return false;
      } else if (StashSystem.isStashBuilding(var1)) {
         this.debugLine = "Stash buildings are invalid";
         return false;
      } else {
         return true;
      }
   }

   public RBSafehouse() {
      this.name = "Safehouse";
      this.setChance(10);
   }
}
