package zombie;

import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.SafeHouse;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;

public final class LootRespawn {
   private static int LastRespawnHour = -1;
   private static final ArrayList<InventoryItem> existingItems = new ArrayList();
   private static final ArrayList<InventoryItem> newItems = new ArrayList();

   public LootRespawn() {
   }

   public static void update() {
      if (!GameClient.bClient) {
         int var0 = getRespawnInterval();
         if (var0 > 0) {
            int var1 = 7 + (int)(GameTime.getInstance().getWorldAgeHours() / (double)var0) * var0;
            if (LastRespawnHour < var1) {
               LastRespawnHour = var1;
               int var2;
               int var4;
               int var5;
               IsoChunk var6;
               if (GameServer.bServer) {
                  for(var2 = 0; var2 < ServerMap.instance.LoadedCells.size(); ++var2) {
                     ServerMap.ServerCell var3 = (ServerMap.ServerCell)ServerMap.instance.LoadedCells.get(var2);
                     if (var3.bLoaded) {
                        for(var4 = 0; var4 < 8; ++var4) {
                           for(var5 = 0; var5 < 8; ++var5) {
                              var6 = var3.chunks[var5][var4];
                              checkChunk(var6);
                           }
                        }
                     }
                  }
               } else {
                  for(var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
                     IsoChunkMap var7 = IsoWorld.instance.CurrentCell.ChunkMap[var2];
                     if (!var7.ignore) {
                        for(var4 = 0; var4 < IsoChunkMap.ChunkGridWidth; ++var4) {
                           for(var5 = 0; var5 < IsoChunkMap.ChunkGridWidth; ++var5) {
                              var6 = var7.getChunk(var5, var4);
                              checkChunk(var6);
                           }
                        }
                     }
                  }
               }

            }
         }
      }
   }

   public static void Reset() {
      LastRespawnHour = -1;
   }

   public static void chunkLoaded(IsoChunk var0) {
      if (!GameClient.bClient) {
         checkChunk(var0);
      }
   }

   private static void checkChunk(IsoChunk var0) {
      if (var0 != null) {
         int var1 = getRespawnInterval();
         if (var1 > 0) {
            if (!(GameTime.getInstance().getWorldAgeHours() < (double)var1)) {
               int var2 = 7 + (int)(GameTime.getInstance().getWorldAgeHours() / (double)var1) * var1;
               if (var0.lootRespawnHour > var2) {
                  var0.lootRespawnHour = var2;
               }

               if (var0.lootRespawnHour < var2) {
                  var0.lootRespawnHour = var2;
                  respawnInChunk(var0);
               }
            }
         }
      }
   }

   private static int getRespawnInterval() {
      return SandboxOptions.instance.HoursForLootRespawn.getValue();
   }

   private static void respawnInChunk(IsoChunk var0) {
      boolean var1 = SandboxOptions.instance.ConstructionPreventsLootRespawn.getValue();
      boolean var2 = GameServer.bServer && ServerOptions.instance.SafehousePreventsLootRespawn.getValue();
      int var3 = SandboxOptions.instance.SeenHoursPreventLootRespawn.getValue();
      double var4 = GameTime.getInstance().getWorldAgeHours();

      for(int var6 = 0; var6 < 8; ++var6) {
         for(int var7 = 0; var7 < 8; ++var7) {
            IsoGridSquare var8 = var0.getGridSquare(var7, var6, 0);
            Zone var9 = var8 == null ? null : var8.getZone();
            if (var9 != null && ("TownZone".equals(var9.getType()) || "TownZones".equals(var9.getType()) || "TrailerPark".equals(var9.getType())) && (!var1 || !var9.haveConstruction) && (var3 <= 0 || !(var9.getHoursSinceLastSeen() <= (float)var3)) && (!var2 || SafeHouse.getSafeHouse(var8) == null)) {
               if (var8.getBuilding() != null) {
                  BuildingDef var10 = var8.getBuilding().getDef();
                  if (var10 != null) {
                     if ((double)var10.lootRespawnHour > var4) {
                        var10.lootRespawnHour = 0;
                     }

                     if (var10.lootRespawnHour < var0.lootRespawnHour) {
                        var10.setKeySpawned(0);
                        var10.lootRespawnHour = var0.lootRespawnHour;
                     }
                  }
               }

               for(int var17 = 0; var17 < 8; ++var17) {
                  var8 = var0.getGridSquare(var7, var6, var17);
                  if (var8 != null) {
                     int var11 = var8.getObjects().size();
                     IsoObject[] var12 = (IsoObject[])var8.getObjects().getElements();

                     for(int var13 = 0; var13 < var11; ++var13) {
                        IsoObject var14 = var12[var13];
                        if (!(var14 instanceof IsoDeadBody) && !(var14 instanceof IsoThumpable) && !(var14 instanceof IsoCompost)) {
                           for(int var15 = 0; var15 < var14.getContainerCount(); ++var15) {
                              ItemContainer var16 = var14.getContainerByIndex(var15);
                              if (var16.bExplored && var16.isHasBeenLooted()) {
                                 respawnInContainer(var14, var16);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private static void respawnInContainer(IsoObject var0, ItemContainer var1) {
      if (var1 != null && var1.getItems() != null) {
         int var2 = var1.getItems().size();
         int var3 = SandboxOptions.instance.MaxItemsForLootRespawn.getValue();
         if (var2 < var3) {
            existingItems.clear();
            existingItems.addAll(var1.getItems());
            ItemPickerJava.fillContainer(var1, (IsoPlayer)null);
            ArrayList var4 = var1.getItems();
            if (var4 != null && var2 != var4.size()) {
               var1.setHasBeenLooted(false);
               newItems.clear();

               for(int var5 = 0; var5 < var4.size(); ++var5) {
                  InventoryItem var6 = (InventoryItem)var4.get(var5);
                  if (!existingItems.contains(var6)) {
                     newItems.add(var6);
                     var6.setAge(0.0F);
                  }
               }

               ItemPickerJava.updateOverlaySprite(var0);
               if (GameServer.bServer) {
                  INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (float)var0.square.x, (float)var0.square.y, var1, newItems);
               }

            }
         }
      }
   }
}
