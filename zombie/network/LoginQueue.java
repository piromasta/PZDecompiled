package zombie.network;

import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.network.packets.connection.QueuePacket;

public class LoginQueue {
   private static final ArrayList<UdpConnection> LoginQueue = new ArrayList();
   private static final ArrayList<UdpConnection> PreferredLoginQueue = new ArrayList();
   private static final UpdateLimit UpdateLimit = new UpdateLimit(3050L);
   private static final UpdateLimit UpdateServerInformationLimit = new UpdateLimit(20000L);
   private static final UpdateLimit LoginQueueTimeout = new UpdateLimit(15000L);
   private static UdpConnection currentLoginQueue;

   public LoginQueue() {
   }

   public static void receiveLoginQueueDone(long var0, UdpConnection var2) {
      LoggerManager.getLogger("user").write("player " + var2.username + " loading time was: " + var0 + " ms");
      synchronized(LoginQueue) {
         if (currentLoginQueue == var2) {
            currentLoginQueue = null;
         }

         loadNextPlayer();
      }

      ConnectionManager.log("receive-packet", "login-queue-done", var2);
      var2.validator.checksumSend(true, false);
   }

   public static void receiveServerLoginQueueRequest(UdpConnection var0) {
      ZLogger var10000 = LoggerManager.getLogger("user");
      String var10001 = var0.idStr;
      var10000.write(var10001 + " \"" + var0.username + "\" attempting to join used " + (var0.role.haveCapability(Capability.PriorityLogin) ? "preferred " : "") + "queue");
      synchronized(LoginQueue) {
         if (!ServerOptions.getInstance().LoginQueueEnabled.getValue() || !var0.role.haveCapability(Capability.PriorityLogin) && currentLoginQueue == null && PreferredLoginQueue.isEmpty() && LoginQueue.isEmpty() && getCountPlayers() < ServerOptions.getInstance().getMaxPlayers() || var0.role.haveCapability(Capability.PriorityLogin) && currentLoginQueue == null && PreferredLoginQueue.isEmpty()) {
            DebugLog.DetailedInfo.trace("ConnectionImmediate ip=%s", var0.ip);
            currentLoginQueue = var0;
            currentLoginQueue.wasInLoadingQueue = true;
            LoginQueueTimeout.Reset((long)ServerOptions.getInstance().LoginQueueConnectTimeout.getValue() * 1000L);
            QueuePacket var2 = new QueuePacket();
            var2.setConnectionImmediate();
            ByteBufferWriter var3 = var0.startPacket();
            PacketTypes.PacketType.LoginQueueRequest.doPacket(var3);
            var2.write(var3);
            PacketTypes.PacketType.LoginQueueRequest.send(var0);
         } else {
            DebugLog.DetailedInfo.trace("PlaceInQueue ip=%s preferredInQueue=%b", var0.ip, var0.role.haveCapability(Capability.PriorityLogin));
            if (var0.role.haveCapability(Capability.PriorityLogin)) {
               if (!PreferredLoginQueue.contains(var0)) {
                  PreferredLoginQueue.add(var0);
               }
            } else if (!LoginQueue.contains(var0)) {
               LoginQueue.add(var0);
            }

            sendPlaceInTheQueue();
         }
      }

      ConnectionManager.log("receive-packet", "login-queue-request", var0);
   }

   private static void sendPlaceInTheQueue() {
   }

   private static void sendConnectRequest(UdpConnection var0) {
      DebugLog.DetailedInfo.trace("SendApplyRequest ip=%s", var0.ip);
      QueuePacket var1 = new QueuePacket();
      var1.setConnectionImmediate();
      ByteBufferWriter var2 = var0.startPacket();
      PacketTypes.PacketType.LoginQueueRequest.doPacket(var2);
      var1.write(var2);
      PacketTypes.PacketType.LoginQueueRequest.send(var0);
      ConnectionManager.log("send-packet", "login-queue-request", var0);
   }

   public static void disconnect(UdpConnection var0) {
      DebugLog.DetailedInfo.trace("ip=%s", var0.ip);
      synchronized(LoginQueue) {
         if (var0 == currentLoginQueue) {
            currentLoginQueue = null;
         } else {
            LoginQueue.remove(var0);
            PreferredLoginQueue.remove(var0);
         }

         sendPlaceInTheQueue();
      }
   }

   public static boolean isInTheQueue(UdpConnection var0) {
      if (!ServerOptions.getInstance().LoginQueueEnabled.getValue()) {
         return false;
      } else {
         synchronized(LoginQueue) {
            return var0 == currentLoginQueue || LoginQueue.contains(var0) || PreferredLoginQueue.contains(var0);
         }
      }
   }

   public static void update() {
      if (ServerOptions.getInstance().LoginQueueEnabled.getValue() && UpdateLimit.Check()) {
         synchronized(LoginQueue) {
            if (currentLoginQueue != null) {
               if (currentLoginQueue.isFullyConnected()) {
                  DebugLog.DetailedInfo.trace("Connection isFullyConnected ip=%s", currentLoginQueue.ip);
                  currentLoginQueue = null;
               } else if (LoginQueueTimeout.Check()) {
                  DebugLog.DetailedInfo.trace("Connection timeout ip=%s", currentLoginQueue.ip);
                  currentLoginQueue = null;
               }
            }

            loadNextPlayer();
         }
      }

      if (UpdateServerInformationLimit.Check()) {
         sendPlaceInTheQueue();
      }

   }

   private static void loadNextPlayer() {
      if (!PreferredLoginQueue.isEmpty() && currentLoginQueue == null) {
         currentLoginQueue = (UdpConnection)PreferredLoginQueue.remove(0);
         currentLoginQueue.wasInLoadingQueue = true;
         DebugLog.DetailedInfo.trace("Next player from the preferred queue to connect ip=%s", currentLoginQueue.ip);
         LoginQueueTimeout.Reset((long)ServerOptions.getInstance().LoginQueueConnectTimeout.getValue() * 1000L);
         sendConnectRequest(currentLoginQueue);
         sendPlaceInTheQueue();
      }

      if (!LoginQueue.isEmpty() && currentLoginQueue == null && getCountPlayers() < ServerOptions.getInstance().getMaxPlayers()) {
         currentLoginQueue = (UdpConnection)LoginQueue.remove(0);
         currentLoginQueue.wasInLoadingQueue = true;
         DebugLog.DetailedInfo.trace("Next player from queue to connect ip=%s", currentLoginQueue.ip);
         LoginQueueTimeout.Reset((long)ServerOptions.getInstance().LoginQueueConnectTimeout.getValue() * 1000L);
         sendConnectRequest(currentLoginQueue);
         sendPlaceInTheQueue();
      }

   }

   public static int getCountPlayers() {
      int var0 = 0;

      for(int var1 = 0; var1 < GameServer.udpEngine.connections.size(); ++var1) {
         UdpConnection var2 = (UdpConnection)GameServer.udpEngine.connections.get(var1);
         if ((var2.role == null || !var2.role.haveCapability(Capability.HideFromSteamUserList)) && var2.wasInLoadingQueue && !LoginQueue.contains(var2) && !PreferredLoginQueue.contains(var2)) {
            ++var0;
         }
      }

      return var0;
   }

   public static String getDescription() {
      int var10000 = LoginQueue.size();
      return "queue=[" + var10000 + "/" + PreferredLoginQueue.size() + "/\"" + (currentLoginQueue == null ? "" : currentLoginQueue.getConnectedGUID()) + "\"]";
   }
}
