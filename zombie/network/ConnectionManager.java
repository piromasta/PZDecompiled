package zombie.network;

import java.util.ArrayDeque;
import java.util.Iterator;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.core.secure.PZcrypt;
import zombie.debug.DebugLog;

public class ConnectionManager {
   private static final ConnectionManager instance = new ConnectionManager();
   final ArrayDeque<Request> ConnectionRequests = new ArrayDeque();

   public ConnectionManager() {
   }

   public static ConnectionManager getInstance() {
      return instance;
   }

   public void ping(String var1, String var2, String var3, String var4, boolean var5) {
      synchronized(this.ConnectionRequests) {
         this.ConnectionRequests.add(new Request(ConnectionManager.RequestType.askPing, var1, var2, var3, "", var4, "", "", false, var5, 1, ""));
      }

      getInstance().process();
   }

   public void stopPing() {
      GameClient.askPing = false;
   }

   public void getCustomizationData(String var1, String var2, String var3, String var4, String var5, String var6, boolean var7) {
      synchronized(this.ConnectionRequests) {
         Iterator var9 = this.ConnectionRequests.iterator();

         while(true) {
            if (!var9.hasNext()) {
               this.ConnectionRequests.push(new Request(ConnectionManager.RequestType.askCustomizationData, var1, var2, var3, "", var4, var5, var6, false, var7, 1, ""));
               break;
            }

            Request var10 = (Request)var9.next();
            if (var10.server.equals(var3)) {
               return;
            }
         }
      }

      getInstance().process();
   }

   public void sendSecretKey(String var1, String var2, String var3, String var4, boolean var5, int var6, String var7) {
      synchronized(this.ConnectionRequests) {
         this.ConnectionRequests.removeIf((var0) -> {
            return var0.type == ConnectionManager.RequestType.askCustomizationData;
         });
         this.ConnectionRequests.push(new Request(ConnectionManager.RequestType.sendQR, var1, var2, var3, "", var4, "", "", false, var5, var6, var7));
      }

      getInstance().process();
   }

   public void serverConnect(String var1, String var2, String var3, String var4, String var5, String var6, String var7, boolean var8, boolean var9, int var10, String var11) {
      synchronized(this.ConnectionRequests) {
         this.ConnectionRequests.removeIf((var0) -> {
            return var0.type == ConnectionManager.RequestType.askCustomizationData;
         });
         this.ConnectionRequests.push(new Request(ConnectionManager.RequestType.connect, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11));
      }

      getInstance().process();
   }

   public void serverConnectCoop(String var1) {
      synchronized(this.ConnectionRequests) {
         this.ConnectionRequests.removeIf((var0) -> {
            return var0.type == ConnectionManager.RequestType.askCustomizationData;
         });
         this.ConnectionRequests.push(new Request(ConnectionManager.RequestType.connectCoop, "", "", var1, "", "", "", "", true, false, 1, ""));
      }

      getInstance().process();
   }

   public void clearQueue() {
      synchronized(this.ConnectionRequests) {
         this.ConnectionRequests.clear();
      }
   }

   public void process() {
      if (GameClient.connection == null) {
         Request var1;
         synchronized(this.ConnectionRequests) {
            if (this.ConnectionRequests.isEmpty()) {
               return;
            }

            var1 = (Request)this.ConnectionRequests.poll();
         }

         switch (var1.type) {
            case connect:
               GameClient.askPing = false;
               GameClient.askCustomizationData = false;
               GameClient.sendQR = false;
               doServerConnect(var1.user, var1.pass, var1.server, var1.localIP, var1.port, var1.serverPassword, var1.serverName, var1.useSteamRelay, var1.doHash, var1.authtype, var1.secretKey);
               break;
            case connectCoop:
               GameClient.askPing = false;
               GameClient.askCustomizationData = false;
               GameClient.sendQR = false;
               doServerConnectCoop(var1.server);
               break;
            case askPing:
               GameClient.askPing = true;
               GameClient.askCustomizationData = false;
               GameClient.sendQR = false;
               doServerConnect(var1.user, var1.pass, var1.server, var1.localIP, var1.port, var1.serverPassword, var1.serverName, var1.useSteamRelay, var1.doHash, var1.authtype, var1.secretKey);
               break;
            case askCustomizationData:
               GameClient.askPing = false;
               GameClient.askCustomizationData = true;
               GameClient.sendQR = false;
               doServerConnect(var1.user, var1.pass, var1.server, var1.localIP, var1.port, var1.serverPassword, var1.serverName, var1.useSteamRelay, var1.doHash, var1.authtype, var1.secretKey);
               break;
            case sendQR:
               GameClient.askPing = false;
               GameClient.askCustomizationData = false;
               GameClient.sendQR = true;
               doServerConnect(var1.user, var1.pass, var1.server, var1.localIP, var1.port, var1.serverPassword, var1.serverName, var1.useSteamRelay, var1.doHash, var1.authtype, var1.secretKey);
         }

      }
   }

   public static void log(String var0, String var1, UdpConnection var2) {
      DebugLog.Multiplayer.println("connection: %s [%s] \"%s\"", var2, var0, var1);
   }

   public static void doServerConnect(String var0, String var1, String var2, String var3, String var4, String var5, String var6, boolean var7, boolean var8, int var9, String var10) {
      Core.GameMode = "Multiplayer";
      Core.setDifficulty("Hardcore");
      if (GameClient.connection != null) {
         GameClient.connection.forceDisconnect("lua-connect");
      }

      if (!GameClient.askCustomizationData) {
         GameClient.instance.resetDisconnectTimer();
      }

      GameClient.bClient = true;
      GameClient.bCoopInvite = false;
      ZomboidFileSystem.instance.cleanMultiplayerSaves();
      if (var8) {
         GameClient.instance.doConnect(var0, PZcrypt.hash(ServerWorldDatabase.encrypt(var1)), var2, var3, var4, var5, var6, var7, var9, var10);
      } else {
         GameClient.instance.doConnect(var0, var1, var2, var3, var4, var5, var6, var7, var9, var10);
      }

   }

   public static void doServerConnectCoop(String var0) {
      Core.GameMode = "Multiplayer";
      Core.setDifficulty("Hardcore");
      if (GameClient.connection != null) {
         GameClient.connection.forceDisconnect("lua-connect-coop");
      }

      GameClient.bClient = true;
      GameClient.bCoopInvite = true;
      GameClient.instance.doConnectCoop(var0);
   }

   private static class Request {
      RequestType type;
      String user;
      String pass;
      String server;
      String localIP;
      String port;
      String serverPassword;
      String serverName;
      boolean useSteamRelay;
      boolean doHash;
      int authtype;
      String secretKey;

      public Request(RequestType var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, boolean var9, boolean var10, int var11, String var12) {
         this.type = var1;
         this.user = var2;
         this.pass = var3;
         this.server = var4;
         this.localIP = var5;
         this.port = var6;
         this.serverPassword = var7;
         this.serverName = var8;
         this.useSteamRelay = var9;
         this.doHash = var10;
         this.authtype = var11;
         this.secretKey = var12;
      }
   }

   private static enum RequestType {
      connect,
      connectCoop,
      askPing,
      askCustomizationData,
      sendQR;

      private RequestType() {
      }
   }
}
