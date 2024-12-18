package zombie.core;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.ItemTransactionPacket;
import zombie.util.StringUtils;

public class TransactionManager {
   public static final byte success = 0;
   public static final byte reject = 1;
   public static final byte invalid = -1;
   public static final TransactionManager instance = new TransactionManager();
   private static final ConcurrentLinkedQueue<Transaction> transactions = new ConcurrentLinkedQueue();
   public static String lastItemFullType = "";
   public static int lightweightItemsCount = 0;
   public static long lightweightItemsLastTransactionTime = 0L;

   public TransactionManager() {
   }

   public static void add(Transaction var0) {
      transactions.add(var0);
   }

   public static void update() {
      if (GameServer.bServer) {
         Iterator var0 = transactions.iterator();

         while(var0.hasNext()) {
            Transaction var1 = (Transaction)var0.next();
            if (var1.state == Transaction.TransactionState.Accept && var1.endTime <= GameTime.getServerTimeMills()) {
               if (var1.update()) {
                  var1.setState(Transaction.TransactionState.Done);
                  if (var1 instanceof ItemTransactionPacket) {
                     UdpConnection var2 = GameServer.getConnectionFromPlayer(var1.playerID.getPlayer());
                     if (var2 != null && var2.isFullyConnected()) {
                        ByteBufferWriter var3 = var2.startPacket();
                        PacketTypes.PacketType.ItemTransaction.doPacket(var3);
                        ((ItemTransactionPacket)var1).write(var3);
                        PacketTypes.PacketType.ItemTransaction.send(var2);
                     }
                  }
               } else {
                  var1.setState(Transaction.TransactionState.Reject);
               }
            }
         }

         transactions.removeIf((var0x) -> {
            return var0x.state == Transaction.TransactionState.Done || var0x.state == Transaction.TransactionState.Reject;
         });
      } else if (GameClient.bClient) {
         Stream var4 = transactions.stream().filter((var0x) -> {
            return GameTime.getServerTimeMills() > var0x.startTime + 10000L;
         });
         var4.forEach((var0x) -> {
            transactions.remove(var0x);
            DebugLog.Objects.noise("Timeout: %s", var0x);
         });
      }

   }

   public static byte isConsistent(int var0, ItemContainer var1, ItemContainer var2, String var3, ItemTransactionPacket var4) {
      String var5 = String.format("item=%d source=%s destination=%s %s", var0, var1 == null ? "null" : var1, var2 == null ? "null" : var2, var4 == null ? "" : var4.getDescription());
      if (var2 == null && (var4 == null || var4.sourceId.containerType != ContainerID.ContainerType.IsoObject || var4.destinationId.containerType != ContainerID.ContainerType.IsoObject)) {
         DebugLog.Objects.noise("Inconsistent: destination container can't be found (%s)", var5);
         return 1;
      } else {
         if (var0 != -1 && var1 != null) {
            if (!var1.containsID(var0)) {
               DebugLog.Objects.noise("Inconsistent: source container is not contain the item (%s)", var5);
               return -1;
            }

            if (var2 != null && !var2.isItemAllowed(var1.getItemWithID(var0))) {
               DebugLog.Objects.noise("Inconsistent: destination container can't contain the item (%s)", var5);
               return 1;
            }
         }

         float var6 = 0.0F;
         Iterator var7 = transactions.iterator();

         while(var7.hasNext()) {
            Transaction var8 = (Transaction)var7.next();
            if (var0 != -1) {
               if (var2 != null && var2.getCharacter() != null) {
                  if (var0 == var8.itemId && var3 == null || var1 != null && var1.ID == var8.itemId || var0 == var8.sourceId.getContainer().ID || var0 == var8.destinationId.getContainer().ID) {
                     DebugLog.Objects.noise("Inconsistent: item to inventory (%s) t=(%s)", var5, var8);
                     return 1;
                  }
               } else if (var2 != null && (var0 == var8.itemId || var2.ID == var8.itemId || var8.sourceId.isContainerTheSame(var0, var2) && var8.itemId == -1)) {
                  DebugLog.Objects.noise("Inconsistent: item from inventory (%s) t=(%s)", var5, var8);
                  return 1;
               }
            } else if (var2.getCharacter() != null && var1 != null && (var8.destinationId.isContainerTheSame(var0, var1) || var8.sourceId.isContainerTheSame(var0, var1))) {
               DebugLog.Objects.noise("Inconsistent: object to inventory (%s) t=(%s)", var5, var8);
               return 1;
            }

            if (var8.itemId != -1 && var8.sourceId.getContainer() != null && var8.destinationId.getContainer() != null && var8.destinationId.isContainerTheSame(var0, var2) && var8.sourceId.getContainer().containsID(var8.itemId)) {
               InventoryItem var9 = var8.sourceId.getContainer().getItemWithID(var8.itemId);
               if (var9 != null) {
                  var6 += var9.getActualWeight();
               }
            }
         }

         if (var0 != -1 && var1 != null) {
            InventoryItem var10 = var1.getItemWithID(var0);
            if (var2 != null && "floor".equals(var2.getType()) && var1.getCharacter() != null) {
               IsoGridSquare var11 = getNotFullFloorSquare(var1.getCharacter(), var10, var2);
               if (var11 == null) {
                  DebugLog.Objects.warn("Inconsistent: destination square does not contain enough space for the item (%s)", var5);
                  return 1;
               }

               if (var4 != null) {
                  var4.square = var11;
               }

               return 0;
            }

            if (var1 == var2 && var1.getParent() instanceof IsoPlayer && var10 != null && var10.getClothingItemExtraOption() != null && !var10.getClothingItemExtraOption().isEmpty() && !StringUtils.isNullOrEmpty(var3)) {
               return 0;
            }

            if (var2 != null && (float)var2.getCapacity() < var2.getCapacityWeight() + var10.getActualWeight() + var6) {
               DebugLog.Objects.noise("Inconsistent: destination container is not contain enough space for the item (%s)", var5);
               return 1;
            }
         }

         return 0;
      }
   }

   private static boolean floorHasRoomFor(IsoGridSquare var0, IsoGameCharacter var1, InventoryItem var2, ItemContainer var3) {
      float var4 = (float)var3.getEffectiveCapacity(var1);
      float var5 = var0.getTotalWeightOfItemsOnFloor();
      if (var5 >= var4) {
         return false;
      } else if (transactions.stream().anyMatch((var1x) -> {
         return var1x.square == var0;
      })) {
         return false;
      } else if (ItemContainer.floatingPointCorrection(var5) + var2.getUnequippedWeight() <= var4) {
         return true;
      } else {
         return var2.getUnequippedWeight() >= var4;
      }
   }

   private static boolean canDropOnFloor(IsoGridSquare var0, IsoGameCharacter var1) {
      if (var0 == null) {
         return false;
      } else if (!var0.TreatAsSolidFloor()) {
         return false;
      } else if (!var0.isSolid() && !var0.isSolidTrans()) {
         IsoGridSquare var2 = var1.getCurrentSquare();
         if (var2 != null && var0 != var2) {
            if (var2.isBlockedTo(var0) || var2.isWindowTo(var0)) {
               return false;
            }

            if (var2.HasStairs() != var0.HasStairs()) {
               return false;
            }

            if (var2.HasStairs() && !var2.isSameStaircase(var0.getX(), var0.getY(), var0.getZ())) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static IsoGridSquare getNotFullFloorSquare(IsoGameCharacter var0, InventoryItem var1, ItemContainer var2) {
      IsoGridSquare var3 = var0.getCurrentSquare();
      if (canDropOnFloor(var3, var0) && floorHasRoomFor(var3, var0, var1, var2)) {
         return var3;
      } else {
         for(int var4 = -1; var4 <= 1; ++var4) {
            for(int var5 = -1; var5 <= 1; ++var5) {
               if (var5 != 0 || var4 != 0) {
                  var3 = IsoWorld.instance.getCell().getGridSquare((double)(var0.getX() + (float)var5), (double)(var0.getY() + (float)var4), (double)var0.getZ());
                  if (canDropOnFloor(var3, var0) && floorHasRoomFor(var3, var0, var1, var2)) {
                     return var3;
                  }
               }
            }
         }

         return null;
      }
   }

   public static boolean isRejected(byte var0) {
      return transactions.stream().filter((var1x) -> {
         return var0 == var1x.id;
      }).allMatch((var0x) -> {
         return var0x.state == Transaction.TransactionState.Reject;
      });
   }

   public static boolean isDone(byte var0) {
      return transactions.stream().filter((var1x) -> {
         return var0 == var1x.id;
      }).allMatch((var0x) -> {
         return var0x.state == Transaction.TransactionState.Done;
      });
   }

   public static void cancelAllRelevantToUser(IsoPlayer var0) {
      Iterator var1 = transactions.iterator();

      while(true) {
         Transaction var2;
         do {
            if (!var1.hasNext()) {
               return;
            }

            var2 = (Transaction)var1.next();
         } while((var2.sourceId.containerType != ContainerID.ContainerType.PlayerInventory || var2.sourceId.playerID.getID() != var0.OnlineID) && (var2.destinationId.containerType != ContainerID.ContainerType.PlayerInventory || var2.destinationId.playerID.getID() != var0.OnlineID));

         var2.setState(Transaction.TransactionState.Reject);
      }
   }

   public static int getDuration(byte var0) {
      Optional var1 = transactions.stream().filter((var1x) -> {
         return var0 == var1x.id;
      }).findFirst();
      if (var1.isEmpty()) {
         return -1;
      } else {
         Transaction var2 = (Transaction)var1.get();
         return (int)(var2.endTime - var2.startTime);
      }
   }

   public static byte createItemTransaction(IsoPlayer var0, InventoryItem var1, ItemContainer var2, ItemContainer var3) {
      return createItemTransaction(var0, var1, var2, var3, IsoDirections.N, 0.0F, 0.0F, 0.0F);
   }

   public static byte createItemTransaction(IsoPlayer var0, InventoryItem var1, ItemContainer var2, ItemContainer var3, IsoDirections var4, float var5, float var6, float var7) {
      int var8 = -1;
      if (var1 != null) {
         var8 = var1.id;
      }

      if (var2.getType().equals("floor") && var1.getWorldItem() != null) {
         var8 = -1;
      }

      if (isConsistent(var8, var2, var3, (String)null, (ItemTransactionPacket)null) == 0) {
         INetworkPacket.send(PacketTypes.PacketType.SyncInventory, var0);
         ItemTransactionPacket var9 = new ItemTransactionPacket();
         var9.set(var0, var1, var2, var3, (String)null, var4, var5, var6, var7);
         ByteBufferWriter var10 = GameClient.connection.startPacket();
         PacketTypes.PacketType.ItemTransaction.doPacket(var10);
         var9.write(var10);
         PacketTypes.PacketType.ItemTransaction.send(GameClient.connection);
         add(var9);
         return var9.id;
      } else {
         return 0;
      }
   }

   public static byte changeItemTypeTransaction(IsoPlayer var0, InventoryItem var1, ItemContainer var2, ItemContainer var3, String var4) {
      if (isConsistent(var1.id, var2, var3, var4, (ItemTransactionPacket)null) == 0) {
         INetworkPacket.send(PacketTypes.PacketType.SyncInventory, var0);
         ItemTransactionPacket var5 = new ItemTransactionPacket();
         var5.set(var0, var1, var2, var3, var4, (IsoDirections)null, 0.0F, 0.0F, 0.0F);
         ByteBufferWriter var6 = GameClient.connection.startPacket();
         PacketTypes.PacketType.ItemTransaction.doPacket(var6);
         var5.write(var6);
         PacketTypes.PacketType.ItemTransaction.send(GameClient.connection);
         add(var5);
         return var5.id;
      } else {
         return 0;
      }
   }

   public static void removeItemTransaction(byte var0, boolean var1) {
      if (GameClient.bClient) {
         if (var0 != 0) {
            if (var1) {
               ItemTransactionPacket var2 = new ItemTransactionPacket();
               var2.id = var0;
               var2.setState(Transaction.TransactionState.Reject);
               ByteBufferWriter var3 = GameClient.connection.startPacket();
               PacketTypes.PacketType.ItemTransaction.doPacket(var3);
               var2.write(var3);
               PacketTypes.PacketType.ItemTransaction.send(GameClient.connection);
            }

            transactions.removeIf((var1x) -> {
               return var1x.id == var0;
            });
         }
      } else if (GameServer.bServer) {
         transactions.removeIf((var1x) -> {
            return var1x.id == var0;
         });
      }

   }

   public void setStateFromPacket(ItemTransactionPacket var1) {
      Iterator var2 = transactions.iterator();

      while(var2.hasNext()) {
         Transaction var3 = (Transaction)var2.next();
         if (var1.id == var3.id) {
            var3.setState(var1.state);
            var3.setDuration((long)var1.duration);
            break;
         }
      }

   }
}
