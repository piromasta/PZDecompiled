package zombie.network.packets;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.fields.INetworkPacketField;

public interface INetworkPacket extends INetworkPacketField {
   default void setData(Object... var1) {
   }

   void parse(ByteBuffer var1, UdpConnection var2);

   default void parseClientLoading(ByteBuffer var1, UdpConnection var2) {
      this.parse(var1, var2);
   }

   default void parseClient(ByteBuffer var1, UdpConnection var2) {
      this.parse(var1, var2);
   }

   default void parseServer(ByteBuffer var1, UdpConnection var2) {
      this.parse(var1, var2);
   }

   default void processClientLoading(UdpConnection var1) {
   }

   default void processClient(UdpConnection var1) {
   }

   default void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
   }

   default void sync(PacketTypes.PacketType var1, UdpConnection var2) {
   }

   default void sendToConnection(PacketTypes.PacketType var1, UdpConnection var2) {
      ByteBufferWriter var3 = var2.startPacket();

      try {
         var1.doPacket(var3);
         this.write(var3);
         var1.send(var2);
      } catch (Exception var5) {
         var2.cancelPacket();
         DebugLog.Multiplayer.printException(var5, "Packet " + var1.name() + " send error", LogSeverity.Error);
      }

   }

   default void sendToClient(PacketTypes.PacketType var1, UdpConnection var2) {
      if (GameServer.bServer) {
         this.sendToConnection(var1, var2);
      }

   }

   default void sendToServer(PacketTypes.PacketType var1) {
      if (GameClient.bClient) {
         this.sendToConnection(var1, GameClient.connection);
      }

   }

   default void sendToClients(PacketTypes.PacketType var1, UdpConnection var2) {
      if (GameServer.bServer) {
         Iterator var3 = GameServer.udpEngine.connections.iterator();

         while(true) {
            UdpConnection var4;
            do {
               if (!var3.hasNext()) {
                  return;
               }

               var4 = (UdpConnection)var3.next();
            } while(var2 != null && var4.getConnectedGUID() == var2.getConnectedGUID());

            if (var4.isFullyConnected()) {
               this.sendToConnection(var1, var4);
            }
         }
      }
   }

   default void sendToRelativeClients(PacketTypes.PacketType var1, UdpConnection var2, float var3, float var4) {
      if (GameServer.bServer) {
         Iterator var5 = GameServer.udpEngine.connections.iterator();

         while(true) {
            UdpConnection var6;
            do {
               if (!var5.hasNext()) {
                  return;
               }

               var6 = (UdpConnection)var5.next();
            } while(var2 != null && var6.getConnectedGUID() == var2.getConnectedGUID());

            if (var6.isFullyConnected() && var6.RelevantTo(var3, var4)) {
               this.sendToConnection(var1, var6);
            }
         }
      }
   }

   static INetworkPacket createPacket(PacketTypes.PacketType var0, Object... var1) {
      INetworkPacket var2;
      try {
         var2 = (INetworkPacket)var0.handler.getDeclaredConstructor().newInstance();
         var2.setData(var1);
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException var4) {
         DebugLog.Multiplayer.printException(var4, "Packet " + var0.name() + " creation error", LogSeverity.Error);
         var2 = null;
      }

      return var2;
   }

   static void send(UdpConnection var0, PacketTypes.PacketType var1, Object... var2) {
      INetworkPacket var3 = createPacket(var1, var2);
      if (var3 != null) {
         var3.sendToConnection(var1, var0);
      }

   }

   static void send(PacketTypes.PacketType var0, Object... var1) {
      if (GameClient.bClient) {
         send(GameClient.connection, var0, var1);
      }

   }

   static void send(IsoPlayer var0, PacketTypes.PacketType var1, Object... var2) {
      if (GameServer.bServer) {
         long var3 = (Long)GameServer.PlayerToAddressMap.get(var0);
         UdpConnection var5 = GameServer.udpEngine.getActiveConnection(var3);
         if (var5 != null) {
            send(var5, var1, var2);
         }
      }

   }

   static void send(zombie.spnetwork.UdpConnection var0, PacketTypes.PacketType var1, Object... var2) {
      INetworkPacket var3 = createPacket(var1, var2);
      if (var3 != null) {
         ByteBufferWriter var4 = var0.startPacket();

         try {
            var1.doPacket(var4);
            var3.write(var4);
            var0.endPacketImmediate();
         } catch (Exception var6) {
            var0.cancelPacket();
            DebugLog.Multiplayer.printException(var6, "Packet " + var1.name() + " sp send error", LogSeverity.Error);
         }
      }

   }

   static void sendToAll(PacketTypes.PacketType var0, UdpConnection var1, Object... var2) {
      if (GameServer.bServer) {
         INetworkPacket var3 = createPacket(var0, var2);
         if (var3 != null) {
            Iterator var4 = GameServer.udpEngine.connections.iterator();

            while(true) {
               UdpConnection var5;
               do {
                  if (!var4.hasNext()) {
                     return;
                  }

                  var5 = (UdpConnection)var4.next();
               } while(var1 != null && var5.getConnectedGUID() == var1.getConnectedGUID());

               if (var5.isFullyConnected()) {
                  var3.sendToConnection(var0, var5);
               }
            }
         }
      }

   }

   static void sendToRelative(PacketTypes.PacketType var0, float var1, float var2, Object... var3) {
      if (GameServer.bServer) {
         sendToRelative(var0, (UdpConnection)null, var1, var2, var3);
      }

   }

   static void sendToRelative(PacketTypes.PacketType var0, UdpConnection var1, float var2, float var3, Object... var4) {
      if (GameServer.bServer) {
         INetworkPacket var5 = createPacket(var0, var4);
         if (var5 != null) {
            Iterator var6 = GameServer.udpEngine.connections.iterator();

            while(true) {
               UdpConnection var7;
               do {
                  if (!var6.hasNext()) {
                     return;
                  }

                  var7 = (UdpConnection)var6.next();
               } while(var1 != null && var7.getConnectedGUID() == var1.getConnectedGUID());

               if (var7.isFullyConnected() && var7.RelevantTo(var2, var3)) {
                  var5.sendToConnection(var0, var7);
               }
            }
         }
      }

   }

   static void processPacketOnServer(PacketTypes.PacketType var0, UdpConnection var1, Object... var2) {
      if (GameServer.bServer) {
         INetworkPacket var3 = createPacket(var0, var2);
         if (var3 != null) {
            var3.processServer(var0, var1);
         }
      }

   }

   static void sendToRelativeAndProcess(PacketTypes.PacketType var0, int var1, int var2, Object... var3) {
      if (GameServer.bServer) {
         INetworkPacket var4 = createPacket(var0, var3);
         if (var4 != null) {
            var4.processServer(var0, (UdpConnection)null);
            Iterator var5 = GameServer.udpEngine.connections.iterator();

            while(var5.hasNext()) {
               UdpConnection var6 = (UdpConnection)var5.next();
               if (var6.RelevantTo((float)var1, (float)var2)) {
                  var4.sendToConnection(var0, var6);
               }
            }
         }
      }

   }
}
