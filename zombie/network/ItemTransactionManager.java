package zombie.network;

import java.nio.ByteBuffer;
import java.util.HashSet;
import zombie.GameTime;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

public class ItemTransactionManager {
   private static final HashSet<ItemRequest> requests = new HashSet();

   public ItemTransactionManager() {
   }

   public static void update() {
      requests.removeIf(ItemRequest::isTimeout);
   }

   public static boolean isConsistent(int var0, int var1, int var2) {
      boolean var3 = requests.stream().filter((var3x) -> {
         return var0 == var3x.itemID || var1 == var3x.itemID || var2 == var3x.itemID || var0 == var3x.srcID || var0 == var3x.dstID;
      }).noneMatch((var0x) -> {
         return var0x.state == 1;
      });
      return var3;
   }

   public static void receiveOnClient(ByteBuffer var0, short var1) {
      try {
         byte var2 = var0.get();
         int var3 = var0.getInt();
         int var4 = var0.getInt();
         int var5 = var0.getInt();
         DebugLog.Multiplayer.debugln("%d [ %d : %d => %d ]", var2, var3, var4, var5);
         requests.stream().filter((var3x) -> {
            return var0 == var3x.itemID && var1 == var3x.srcID && var2 == var3x.dstID;
         }).forEach((var1x) -> {
            var1x.setState(var2);
         });
      } catch (Exception var6) {
         DebugLog.Multiplayer.printException(var6, "ReceiveOnClient: failed", LogSeverity.Error);
      }

   }

   public static void receiveOnServer(ByteBuffer var0, UdpConnection var1, short var2) {
      try {
         byte var3 = var0.get();
         int var4 = var0.getInt();
         int var5 = var0.getInt();
         int var6 = var0.getInt();
         if (0 == var3) {
            if (isConsistent(var4, var5, var6)) {
               requests.add(new ItemRequest(var4, var5, var6));
               sendItemTransaction(var1, (byte)2, var4, var5, var6);
               DebugLog.Multiplayer.trace("set accepted [ %d : %d => %d ]", var4, var5, var6);
            } else {
               sendItemTransaction(var1, (byte)1, var4, var5, var6);
               DebugLog.Multiplayer.trace("set rejected [ %d : %d => %d ]", var4, var5, var6);
            }
         } else {
            requests.removeIf((var3x) -> {
               return var0 == var3x.itemID && var1 == var3x.srcID && var2 == var3x.dstID;
            });
            DebugLog.Multiplayer.trace("remove processed [ %d : %d => %d ]", var4, var5, var6);
         }
      } catch (Exception var7) {
         DebugLog.Multiplayer.printException(var7, "ReceiveOnClient: failed", LogSeverity.Error);
      }

   }

   public static void createItemTransaction(int var0, int var1, int var2) {
      if (isConsistent(var0, var1, var2)) {
         requests.add(new ItemRequest(var0, var1, var2));
         sendItemTransaction(GameClient.connection, (byte)0, var0, var1, var2);
      }

   }

   public static void removeItemTransaction(int var0, int var1, int var2) {
      if (requests.removeIf((var3x) -> {
         return var0 == var3x.itemID && var1 == var3x.srcID && var2 == var3x.dstID;
      })) {
         sendItemTransaction(GameClient.connection, (byte)2, var0, var1, var2);
      }

   }

   private static void sendItemTransaction(UdpConnection var0, byte var1, int var2, int var3, int var4) {
      if (var0 != null) {
         ByteBufferWriter var5 = var0.startPacket();

         try {
            PacketTypes.PacketType.ItemTransaction.doPacket(var5);
            var5.putByte(var1);
            var5.putInt(var2);
            var5.putInt(var3);
            var5.putInt(var4);
            PacketTypes.PacketType.ItemTransaction.send(var0);
         } catch (Exception var7) {
            var0.cancelPacket();
            DebugLog.Multiplayer.printException(var7, "SendItemTransaction: failed", LogSeverity.Error);
         }
      }

   }

   private static class ItemRequest {
      private static final byte StateUnknown = 0;
      private static final byte StateRejected = 1;
      private static final byte StateAccepted = 2;
      private final int itemID;
      private final int srcID;
      private final int dstID;
      private final long timestamp;
      private byte state;

      private ItemRequest(int var1, int var2, int var3) {
         this.itemID = var1;
         this.srcID = var2;
         this.dstID = var3;
         this.timestamp = GameTime.getServerTimeMills() + 5000L;
         this.state = (byte)(GameServer.bServer ? 1 : 0);
      }

      private void setState(byte var1) {
         this.state = var1;
      }

      private boolean isTimeout() {
         return GameTime.getServerTimeMills() > this.timestamp;
      }
   }
}
