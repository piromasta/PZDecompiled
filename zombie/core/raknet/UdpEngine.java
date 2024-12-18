package zombie.core.raknet;

import java.io.File;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.characters.SafetySystemManager;
import zombie.core.ThreadGroups;
import zombie.core.Translator;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.core.secure.PZcrypt;
import zombie.core.znet.SteamUser;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.RequestDataManager;
import zombie.network.anticheats.AntiCheat;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.connection.ServerCustomizationPacket;
import zombie.popman.NetworkZombieManager;

public class UdpEngine {
   private int maxConnections = 0;
   private final Map<Long, UdpConnection> connectionMap = new HashMap();
   public final List<UdpConnection> connections = new ArrayList();
   protected final RakNetPeerInterface peer;
   final boolean bServer;
   Lock bufferLock = new ReentrantLock();
   private ByteBuffer bb = ByteBuffer.allocate(500000);
   private ByteBufferWriter bbw;
   public int port;
   private final Thread thread;
   private boolean bQuit;
   UdpConnection[] connectionArray;
   ByteBuffer buf;

   public UdpEngine(int var1, int var2, int var3, String var4, boolean var5) throws ConnectException {
      this.bbw = new ByteBufferWriter(this.bb);
      this.port = 0;
      this.connectionArray = new UdpConnection[256];
      this.buf = ByteBuffer.allocate(1000000);
      this.port = var1;
      this.peer = new RakNetPeerInterface();
      DebugLog.Network.println("Initialising RakNet...");
      this.peer.Init(SteamUtils.isSteamModeEnabled());
      this.peer.SetMaximumIncomingConnections(var3);
      this.bServer = var5;
      if (this.bServer) {
         if (GameServer.IPCommandline != null) {
            this.peer.SetServerIP(GameServer.IPCommandline);
         }

         this.peer.SetServerPort(var1, var2);
         this.peer.SetIncomingPassword(this.hashServerPassword(var4));
      } else {
         this.peer.SetClientPort(GameServer.DEFAULT_PORT + Rand.Next(10000) + 1234);
      }

      this.peer.SetOccasionalPing(true);
      this.maxConnections = var3;
      int var6 = this.peer.Startup(var3);
      DebugLog.Network.println("RakNet.Startup() return code: %s (0 means success)", var6);
      if (var6 != 0) {
         throw new ConnectException("Connection Startup Failed. Code: " + var6);
      } else {
         if (var5) {
            VoiceManager.instance.InitVMServer();
         }

         this.thread = new Thread(ThreadGroups.Network, this::threadRun, "UdpEngine");
         this.thread.setDaemon(true);
         this.thread.start();
      }
   }

   private void threadRun() {
      while(true) {
         if (!this.bQuit) {
            ByteBuffer var1 = this.Receive();
            if (!this.bQuit) {
               try {
                  this.decode(var1);
               } catch (Exception var3) {
                  DebugLog.Network.printException(var3, "Exception thrown during decode.", LogSeverity.Error);
               }
               continue;
            }
         }

         return;
      }
   }

   public void Shutdown() {
      DebugLog.log("waiting for UdpEngine thread termination");
      this.bQuit = true;

      while(this.thread.isAlive()) {
         try {
            Thread.sleep(10L);
         } catch (InterruptedException var2) {
         }
      }

      this.peer.Shutdown();
   }

   public void SetServerPassword(String var1) {
      if (this.peer != null) {
         this.peer.SetIncomingPassword(var1);
      }

   }

   public String hashServerPassword(String var1) {
      return PZcrypt.hash(var1, true);
   }

   public String getServerIP() {
      return this.peer.GetServerIP();
   }

   public long getClientSteamID(long var1) {
      return this.peer.GetClientSteamID(var1);
   }

   public long getClientOwnerSteamID(long var1) {
      return this.peer.GetClientOwnerSteamID(var1);
   }

   public ByteBufferWriter startPacket() {
      this.bufferLock.lock();
      this.bb.clear();
      return this.bbw;
   }

   public void endPacketBroadcast(PacketTypes.PacketType var1) {
      this.bb.flip();
      this.peer.Send(this.bb, var1.PacketPriority, var1.PacketPriority, (byte)0, -1L, true);
      this.bufferLock.unlock();
   }

   public void endPacketBroadcastExcept(int var1, int var2, UdpConnection var3) {
      this.bb.flip();
      this.peer.Send(this.bb, var1, var2, (byte)0, var3.connectedGUID, true);
      this.bufferLock.unlock();
   }

   public void connected() {
      VoiceManager.instance.VoiceConnectReq(GameClient.connection.getConnectedGUID());
      if (GameClient.bClient) {
         if (!GameClient.askPing && !GameClient.sendQR && !GameClient.askCustomizationData) {
            GameClient.startAuth = Calendar.getInstance();
            INetworkPacket.send(PacketTypes.PacketType.Login);
            RequestDataManager.getInstance().clear();
            ConnectionManager.log("send-packet", "login", GameClient.connection);
         } else if (GameClient.askPing) {
            ByteBufferWriter var1 = GameClient.connection.startPacket();
            PacketTypes.PacketType.Ping.doPacket(var1);
            var1.putUTF(GameClient.ip);
            PacketTypes.PacketType.Ping.send(GameClient.connection);
            RequestDataManager.getInstance().clear();
         } else if (GameClient.sendQR) {
            INetworkPacket.send(PacketTypes.PacketType.GoogleAuthKey, "");
         } else if (GameClient.askCustomizationData && !GameClient.ServerName.contains(File.separator)) {
            INetworkPacket.send(PacketTypes.PacketType.ServerCustomization, GameClient.ServerName, ServerCustomizationPacket.Data.ServerImageIcon);
            INetworkPacket.send(PacketTypes.PacketType.ServerCustomization, GameClient.ServerName, ServerCustomizationPacket.Data.ServerImageLoginScreen);
            INetworkPacket.send(PacketTypes.PacketType.ServerCustomization, GameClient.ServerName, ServerCustomizationPacket.Data.ServerImageLoadingScreen);
            INetworkPacket.send(PacketTypes.PacketType.ServerCustomization, GameClient.ServerName, ServerCustomizationPacket.Data.Done);
         }
      }

   }

   private void decode(ByteBuffer var1) {
      int var2 = var1.get() & 255;
      int var3;
      long var4;
      UdpConnection var5;
      long var7;
      switch (var2) {
         case 0:
         case 1:
            break;
         case 16:
            var3 = var1.get() & 255;
            var4 = this.peer.getGuidOfPacket();
            if (GameClient.bClient) {
               GameClient.connection = this.addConnection(var3, var4);
               ConnectionManager.log("RakNet", "connection-request-accepted", this.connectionArray[var3]);
               if (!SteamUtils.isSteamModeEnabled()) {
                  this.connected();
               } else {
                  GameClient.steamID = SteamUser.GetSteamID();
               }
            } else {
               ConnectionManager.log("RakNet", "connection-request-accepted", this.connectionArray[var3]);
            }
            break;
         case 17:
            ConnectionManager.log("RakNet", "connection-attempt-failed", (UdpConnection)null);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 18:
            ConnectionManager.log("RakNet", "already-connected", (UdpConnection)null);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 19:
            var3 = var1.get() & 255;
            var4 = this.peer.getGuidOfPacket();
            this.addConnection(var3, var4);
            ConnectionManager.log("RakNet", "new-incoming-connection", this.connectionArray[var3]);
            break;
         case 20:
            ConnectionManager.log("RakNet", "no-free-incoming-connections", (UdpConnection)null);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 21:
            var3 = var1.get() & 255;
            var4 = this.peer.getGuidOfPacket();
            ConnectionManager.log("RakNet", "disconnection-notification", this.connectionArray[var3]);
            this.removeConnection(var3);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 22:
            var3 = var1.get() & 255;
            ConnectionManager.log("RakNet", "connection-lost", this.connectionArray[var3]);
            this.removeConnection(var3);
            break;
         case 23:
            var3 = var1.get() & 255;
            ConnectionManager.log("RakNet", "connection-banned", this.connectionArray[var3]);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 24:
            var3 = var1.get() & 255;
            ConnectionManager.log("RakNet", "invalid-password", this.connectionArray[var3]);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 25:
            ConnectionManager.log("RakNet", "incompatible-protocol-version", (UdpConnection)null);
            String var8 = GameWindow.ReadString(var1);
            LuaEventManager.triggerEvent("OnConnectionStateChanged", "ClientVersionMismatch", var8);
            break;
         case 31:
            var3 = var1.get() & 255;
            ConnectionManager.log("RakNet", "remote-disconnection-notification", this.connectionArray[var3]);
            break;
         case 32:
            var3 = var1.get() & 255;
            ConnectionManager.log("RakNet", "remote-connection-lost", this.connectionArray[var3]);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 33:
            var3 = var1.get() & 255;
            ConnectionManager.log("RakNet", "remote-new-incoming-connection", this.connectionArray[var3]);
            break;
         case 44:
            var7 = this.peer.getGuidOfPacket();
            VoiceManager.instance.VoiceConnectAccept(var7);
            break;
         case 45:
            var7 = this.peer.getGuidOfPacket();
            VoiceManager.instance.VoiceOpenChannelReply(var7, var1);
            break;
         case 46:
            var7 = this.peer.getGuidOfPacket();
            var5 = (UdpConnection)this.connectionMap.get(var7);
            DebugLog.Voice.println("RakVoice channel is closed \"%s\" guid=%d", var5.username, var5.getConnectedGUID());
            break;
         case 134:
            var3 = var1.getShort();
            if (GameServer.bServer) {
               var4 = this.peer.getGuidOfPacket();
               UdpConnection var6 = (UdpConnection)this.connectionMap.get(var4);
               if (var6 == null) {
                  DebugLog.Network.warn("GOT PACKET FROM UNKNOWN CONNECTION guid=%d packetId=%d", var4, Short.valueOf((short)var3));
                  return;
               }

               GameServer.addIncoming((short)var3, var1, var6);
            } else {
               GameClient.instance.addIncoming((short)var3, var1);
            }
            break;
         default:
            DebugLog.Network.warn("Received unknown packet: %s", var2);
            if (GameServer.bServer) {
               var7 = this.peer.getGuidOfPacket();
               var5 = (UdpConnection)this.connectionMap.get(var7);
               AntiCheat.PacketRakNet.act(var5, String.valueOf(var2));
            }
      }

   }

   public void removeConnection(int var1) {
      UdpConnection var2 = this.connectionArray[var1];
      if (var2 != null) {
         this.connectionArray[var1] = null;
         this.connectionMap.remove(var2.getConnectedGUID());
         if (GameClient.bClient) {
            GameClient.instance.connectionLost();
         } else if (GameServer.bServer) {
            float var3 = SafetySystemManager.getCooldown(var2);
            if (var3 > 0.0F) {
               GameServer.addDelayedDisconnect(var2);
               LoggerManager.getLogger("user").write(String.format("Connection delayed disconnect index=%d guid=%d id=%s", var2.index, var2.getConnectedGUID(), var2.idStr));
               IsoPlayer[] var4 = var2.players;
               int var5 = var4.length;

               for(int var6 = 0; var6 < var5; ++var6) {
                  IsoPlayer var7 = var4[var6];
                  if (var7 != null) {
                     var7.networkAI.setDisconnected(true);
                     NetworkZombieManager.getInstance().clearTargetAuth(var2, var7);
                     INetworkPacket.sendToRelative(PacketTypes.PacketType.PlayerUpdateReliable, var2, var7.getX(), var7.getY(), var7);
                  }
               }
            } else {
               GameServer.addDisconnect(var2);
               LoggerManager.getLogger("user").write(String.format("Connection disconnect index=%d guid=%d id=%s", var2.index, var2.getConnectedGUID(), var2.idStr));
            }
         }
      }

   }

   private UdpConnection addConnection(int var1, long var2) {
      UdpConnection var4 = new UdpConnection(this, var2, var1);
      this.connectionMap.put(var2, var4);
      this.connectionArray[var1] = var4;
      if (GameServer.bServer) {
         GameServer.addConnection(var4);
      }

      return var4;
   }

   public ByteBuffer Receive() {
      boolean var1 = false;

      do {
         var1 = this.peer.Receive(this.buf);
         if (var1) {
            return this.buf;
         }

         try {
            Thread.sleep(1L);
         } catch (InterruptedException var3) {
            var3.printStackTrace();
         }
      } while(!this.bQuit && !var1);

      return this.buf;
   }

   public UdpConnection getActiveConnection(long var1) {
      return (UdpConnection)this.connectionMap.get(var1);
   }

   public void Connect(String var1, int var2, String var3, boolean var4) {
      if (var2 == 0 && SteamUtils.isSteamModeEnabled()) {
         long var10 = 0L;

         try {
            var10 = SteamUtils.convertStringToSteamID(var1);
         } catch (NumberFormatException var9) {
            var9.printStackTrace();
            LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_UnknownHost"));
            return;
         }

         this.peer.ConnectToSteamServer(var10, this.hashServerPassword(var3), var4);
      } else {
         String var5;
         try {
            InetAddress var6 = InetAddress.getByName(var1);
            var5 = var6.getHostAddress();
         } catch (UnknownHostException var8) {
            var8.printStackTrace();
            LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_UnknownHost"));
            return;
         }

         this.peer.Connect(var5, var2, this.hashServerPassword(var3), var4);
      }

   }

   public void forceDisconnect(long var1, String var3) {
      this.peer.disconnect(var1, var3);
      if (this.connectionMap.containsKey(var1)) {
         this.removeConnection(((UdpConnection)this.connectionMap.get(var1)).index);
      }

   }

   public RakNetPeerInterface getPeer() {
      return this.peer;
   }

   public int getMaxConnections() {
      return this.maxConnections;
   }
}
