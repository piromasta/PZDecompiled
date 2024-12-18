package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class RDSBanditRaid extends RandomizedDeadSurvivorBase {
   public RDSBanditRaid() {
      this.name = "Bandit Raid";
      this.setChance(1);
      this.setMinimumDays(30);
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      int var2 = var1.getRooms().size();
      if (Rand.Next(2) == 0) {
         this.spawnCarOnNearestNav("Base.VanSeats", var1);
      }

      var1.bAlarmed = false;
      IsoCell var3 = IsoWorld.instance.CurrentCell;

      int var6;
      for(int var4 = var1.x - 1; var4 < var1.x2 + 1; ++var4) {
         for(int var5 = var1.y - 1; var5 < var1.y2 + 1; ++var5) {
            for(var6 = 0; var6 < 8; ++var6) {
               IsoGridSquare var7 = var3.getGridSquare(var4, var5, var6);
               if (var7 != null) {
                  for(int var8 = 0; var8 < var7.getObjects().size(); ++var8) {
                     IsoObject var9 = (IsoObject)var7.getObjects().get(var8);
                     if (Rand.Next(100) <= 85 && var9 instanceof IsoDoor) {
                        ((IsoDoor)var9).destroy();
                     }

                     if (Rand.Next(100) <= 85 && var9 instanceof IsoWindow) {
                        ((IsoWindow)var9).smashWindow(true, false);
                        IsoGridSquare var10 = var7.getRoom() == null ? var7 : ((IsoWindow)var9).getOppositeSquare();
                        if (((IsoWindow)var9).isBarricadeAllowed() && var6 == 0 && var10 != null && var10.getRoom() == null) {
                           boolean var11 = var10 == var7;
                           IsoBarricade var12 = IsoBarricade.AddBarricadeToObject((IsoWindow)var9, var11);
                           if (var12 != null) {
                              int var13 = Rand.Next(0, 4);

                              for(int var14 = 0; var14 < var13; ++var14) {
                                 var12.addPlank((IsoGameCharacter)null, (InventoryItem)null);
                              }

                              if (GameServer.bServer) {
                                 var12.transmitCompleteItemToClients();
                              }
                           }
                        }
                     }

                     if (var9.getContainer() != null && var9.getContainer().getItems() != null) {
                        for(int var19 = 0; var19 < var9.getContainer().getItems().size(); ++var19) {
                           if (Rand.Next(100) < 80) {
                              var9.getContainer().getItems().remove(var19);
                              --var19;
                           }
                        }

                        ItemPickerJava.updateOverlaySprite(var9);
                        var9.getContainer().setExplored(true);
                     }
                  }
               }
            }
         }
      }

      var1.setAllExplored(true);
      var1.bAlarmed = false;
      RoomDef var15 = this.getLivingRoomOrKitchen(var1);
      String var16 = "Bandit";
      if (Rand.NextBool(3)) {
         var16 = "PrivateMilitia";
      }

      this.addZombies(var1, Rand.Next(2, 4), var16, (Integer)null, var15);
      var15 = this.getLivingRoomOrKitchen(var1);
      var6 = Rand.Next(2, 4);

      int var17;
      IsoGridSquare var18;
      for(var17 = 0; var17 < var6; ++var17) {
         var18 = getRandomSquareForCorpse(var15);
         createRandomDeadBody(var18, (IsoDirections)null, Rand.Next(5, 10), 5, (String)null);
      }

      if (var2 > 5) {
         var15 = this.getRandomRoomNoKids(var1, 6);
         this.addZombies(var1, Rand.Next(2, 4), var16, (Integer)null, var15);
         var15 = this.getRandomRoom(var1, 6);
         var6 = Rand.Next(2, 4);

         for(var17 = 0; var17 < var6; ++var17) {
            var18 = getRandomSquareForCorpse(var15);
            createRandomDeadBody(var18, (IsoDirections)null, Rand.Next(5, 10), 5, (String)null);
         }

         var2 = (int)Math.floor((double)(var2 / 10));
         if (var2 > 10) {
            var2 = 10;
         }

         if (var2 > 0) {
            for(var17 = 0; var17 < var2; ++var17) {
               var15 = this.getRandomRoomNoKids(var1, 6);
               this.addZombies(var1, Rand.Next(2, 4), var16, (Integer)null, var15);
               var15 = this.getRandomRoomNoKids(var1, 6);
               if (var15 != null) {
                  var6 = Rand.Next(2, 4);

                  for(var17 = 0; var17 < var6; ++var17) {
                     var18 = getRandomSquareForCorpse(var15);
                     createRandomDeadBody(var18, (IsoDirections)null, Rand.Next(5, 10), 5, (String)null);
                  }
               }
            }
         }
      }

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
      } else if (this.getRoom(var1, "kitchen") == null && this.getRoom(var1, "bedroom") == null) {
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
            return false;
         } else {
            return true;
         }
      }
   }
}
