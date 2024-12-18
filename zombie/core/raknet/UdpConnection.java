package zombie.core.raknet;

import gnu.trove.list.array.TShortArrayList;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import zombie.SystemDisabler;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.core.network.ByteBufferWriter;
import zombie.core.utils.UpdateTimer;
import zombie.core.znet.ZNetStatistics;
import zombie.iso.IsoUtils;
import zombie.iso.Vector3;
import zombie.network.ClientServerMap;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.MPStatistic;
import zombie.network.PlayerDownloadServer;
import zombie.network.anticheats.PacketValidator;
import zombie.util.StringUtils;

public class UdpConnection {
   Lock bufferLock = new ReentrantLock();
   private ByteBuffer bb = ByteBuffer.allocate(1000000);
   private ByteBufferWriter bbw;
   Lock bufferLockPing;
   private ByteBuffer bbPing;
   private ByteBufferWriter bbwPing;
   long connectedGUID;
   UdpEngine engine;
   public int index;
   public boolean allChatMuted;
   public String username;
   public String[] usernames;
   public byte ReleventRange;
   public Role role;
   public static HashMap<String, Long> lastConnections = new HashMap();
   public long lastConnection;
   public long lastUnauthorizedPacket;
   public String ip;
   public boolean wasInLoadingQueue;
   public String password;
   public boolean ping;
   public Vector3[] ReleventPos;
   public short[] playerIDs;
   public IsoPlayer[] players;
   public Vector3[] connectArea;
   public int ChunkGridWidth;
   public ClientServerMap[] loadedCells;
   public PlayerDownloadServer playerDownloadServer;
   public ChecksumState checksumState;
   public long checksumTime;
   public boolean awaitingCoopApprove;
   public long steamID;
   public long ownerID;
   public String idStr;
   public boolean isCoopHost;
   public int maxPlayers;
   public final TShortArrayList chunkObjectState;
   public MPClientStatistic statistic;
   public ZNetStatistics netStatistics;
   public final Deque<Long> pingHistory;
   public final PacketValidator validator;
   private static final long CONNECTION_ATTEMPT_TIMEOUT = 5000L;
   private static final long CONNECTION_GOOGLE_AUTH_TIMEOUT = 60000L;
   public static final long CONNECTION_GRACE_INTERVAL = 60000L;
   public static final long CONNECTION_READY_INTERVAL = 5000L;
   public long connectionTimestamp;
   public boolean googleAuth;
   private boolean ready;
   public UpdateTimer timerSendZombie;
   public UpdateTimer timerUpdateAnimal;
   public UpdateTimer timerOwnershipAnimal;
   private boolean bFullyConnected;
   public boolean isNeighborPlayer;

   public UdpConnection(UdpEngine var1, long var2, int var4) {
      this.bbw = new ByteBufferWriter(this.bb);
      this.bufferLockPing = new ReentrantLock();
      this.bbPing = ByteBuffer.allocate(50);
      this.bbwPing = new ByteBufferWriter(this.bbPing);
      this.connectedGUID = 0L;
      this.allChatMuted = false;
      this.usernames = new String[4];
      this.role = null;
      this.lastConnection = 0L;
      this.lastUnauthorizedPacket = 0L;
      this.ping = false;
      this.ReleventPos = new Vector3[4];
      this.playerIDs = new short[4];
      this.players = new IsoPlayer[4];
      this.connectArea = new Vector3[4];
      this.loadedCells = new ClientServerMap[4];
      this.checksumState = UdpConnection.ChecksumState.Init;
      this.awaitingCoopApprove = false;
      this.chunkObjectState = new TShortArrayList();
      this.statistic = new MPClientStatistic();
      this.pingHistory = new ArrayDeque();
      this.validator = new PacketValidator(this);
      this.googleAuth = false;
      this.timerSendZombie = new UpdateTimer();
      this.timerUpdateAnimal = new UpdateTimer();
      this.timerOwnershipAnimal = new UpdateTimer();
      this.bFullyConnected = false;
      this.isNeighborPlayer = false;
      this.engine = var1;
      this.connectedGUID = var2;
      this.index = var4;
      this.ReleventPos[0] = new Vector3();

      for(int var5 = 0; var5 < 4; ++var5) {
         this.playerIDs[var5] = -1;
      }

      this.setConnectionTimestamp(5000L);
      this.wasInLoadingQueue = false;
   }

   public RakNetPeerInterface getPeer() {
      return this.engine.peer;
   }

   public long getConnectedGUID() {
      return this.connectedGUID;
   }

   public String getServerIP() {
      return this.engine.getServerIP();
   }

   public ByteBufferWriter startPacket() {
      this.bufferLock.lock();
      this.bb.clear();
      return this.bbw;
   }

   public ByteBufferWriter startPingPacket() {
      this.bufferLockPing.lock();
      this.bbPing.clear();
      return this.bbwPing;
   }

   public boolean RelevantTo(float var1, float var2) {
      for(int var3 = 0; var3 < 4; ++var3) {
         if (this.connectArea[var3] != null) {
            int var4 = (int)this.connectArea[var3].z;
            int var5 = (int)(this.connectArea[var3].x - (float)(var4 / 2)) * 8;
            int var6 = (int)(this.connectArea[var3].y - (float)(var4 / 2)) * 8;
            int var7 = var5 + var4 * 8;
            int var8 = var6 + var4 * 8;
            if (var1 >= (float)var5 && var1 < (float)var7 && var2 >= (float)var6 && var2 < (float)var8) {
               return true;
            }
         }

         if (this.ReleventPos[var3] != null && Math.abs(this.ReleventPos[var3].x - var1) <= (float)(this.ReleventRange * 8) && Math.abs(this.ReleventPos[var3].y - var2) <= (float)(this.ReleventRange * 8)) {
            return true;
         }
      }

      return false;
   }

   public float getRelevantAndDistance(float var1, float var2, float var3) {
      for(int var4 = 0; var4 < 4; ++var4) {
         if (this.ReleventPos[var4] != null && Math.abs(this.ReleventPos[var4].x - var1) <= (float)(this.ReleventRange * 8) && Math.abs(this.ReleventPos[var4].y - var2) <= (float)(this.ReleventRange * 8)) {
            return IsoUtils.DistanceTo(this.ReleventPos[var4].x, this.ReleventPos[var4].y, var1, var2);
         }
      }

      return 1.0F / 0.0F;
   }

   public boolean RelevantToPlayerIndex(int var1, float var2, float var3) {
      if (this.connectArea[var1] != null) {
         int var4 = (int)this.connectArea[var1].z;
         int var5 = (int)(this.connectArea[var1].x - (float)(var4 / 2)) * 8;
         int var6 = (int)(this.connectArea[var1].y - (float)(var4 / 2)) * 8;
         int var7 = var5 + var4 * 8;
         int var8 = var6 + var4 * 8;
         if (var2 >= (float)var5 && var2 < (float)var7 && var3 >= (float)var6 && var3 < (float)var8) {
            return true;
         }
      }

      return this.ReleventPos[var1] != null && Math.abs(this.ReleventPos[var1].x - var2) <= (float)(this.ReleventRange * 8) && Math.abs(this.ReleventPos[var1].y - var3) <= (float)(this.ReleventRange * 8);
   }

   public boolean RelevantTo(float var1, float var2, float var3) {
      for(int var4 = 0; var4 < 4; ++var4) {
         if (this.connectArea[var4] != null) {
            int var5 = (int)this.connectArea[var4].z;
            int var6 = (int)(this.connectArea[var4].x - (float)(var5 / 2)) * 8;
            int var7 = (int)(this.connectArea[var4].y - (float)(var5 / 2)) * 8;
            int var8 = var6 + var5 * 8;
            int var9 = var7 + var5 * 8;
            if (var1 >= (float)var6 && var1 < (float)var8 && var2 >= (float)var7 && var2 < (float)var9) {
               return true;
            }
         }

         if (this.ReleventPos[var4] != null && Math.abs(this.ReleventPos[var4].x - var1) <= var3 && Math.abs(this.ReleventPos[var4].y - var2) <= var3) {
            return true;
         }
      }

      return false;
   }

   public void cancelPacket() {
      this.bufferLock.unlock();
   }

   public int getBufferPosition() {
      return this.bb.position();
   }

   public void endPacket(int var1, int var2, byte var3) {
      if (GameServer.bServer) {
         int var4 = this.bb.position();
         this.bb.position(1);
         MPStatistic.getInstance().addOutcomePacket(this.bb.getShort(), var4);
         this.bb.position(var4);
      }

      this.bb.flip();
      this.engine.peer.Send(this.bb, var1, var2, var3, this.connectedGUID, false);
      this.bufferLock.unlock();
   }

   public void endPacket() {
      int var1;
      if (GameServer.bServer) {
         var1 = this.bb.position();
         this.bb.position(1);
         MPStatistic.getInstance().addOutcomePacket(this.bb.getShort(), var1);
         this.bb.position(var1);
      }

      this.bb.flip();
      var1 = this.engine.peer.Send(this.bb, 1, 3, (byte)0, this.connectedGUID, false);
      this.bufferLock.unlock();
   }

   public void endPacketImmediate() {
      int var1;
      if (GameServer.bServer) {
         var1 = this.bb.position();
         this.bb.position(1);
         MPStatistic.getInstance().addOutcomePacket(this.bb.getShort(), var1);
         this.bb.position(var1);
      }

      this.bb.flip();
      var1 = this.engine.peer.Send(this.bb, 0, 3, (byte)0, this.connectedGUID, false);
      this.bufferLock.unlock();
   }

   public void endPacketUnordered() {
      int var1;
      if (GameServer.bServer) {
         var1 = this.bb.position();
         this.bb.position(1);
         MPStatistic.getInstance().addOutcomePacket(this.bb.getShort(), var1);
         this.bb.position(var1);
      }

      this.bb.flip();
      var1 = this.engine.peer.Send(this.bb, 2, 2, (byte)0, this.connectedGUID, false);
      this.bufferLock.unlock();
   }

   public void endPacketUnreliable() {
      this.bb.flip();
      int var1 = this.engine.peer.Send(this.bb, 2, 1, (byte)0, this.connectedGUID, false);
      this.bufferLock.unlock();
   }

   public void endPacketSuperHighUnreliable() {
      int var1;
      if (GameServer.bServer) {
         var1 = this.bb.position();
         this.bb.position(1);
         MPStatistic.getInstance().addOutcomePacket(this.bb.getShort(), var1);
         this.bb.position(var1);
      }

      this.bb.flip();
      var1 = this.engine.peer.Send(this.bb, 0, 1, (byte)0, this.connectedGUID, false);
      this.bufferLock.unlock();
   }

   public void endPingPacket() {
      if (GameServer.bServer) {
         int var1 = this.bb.position();
         this.bb.position(1);
         MPStatistic.getInstance().addOutcomePacket(this.bb.getShort(), var1);
         this.bb.position(var1);
      }

      this.bbPing.flip();
      this.engine.peer.Send(this.bbPing, 0, 1, (byte)0, this.connectedGUID, false);
      this.bufferLockPing.unlock();
   }

   public InetSocketAddress getInetSocketAddress() {
      String var1 = this.engine.peer.getIPFromGUID(this.connectedGUID);
      if ("UNASSIGNED_SYSTEM_ADDRESS".equals(var1)) {
         return null;
      } else {
         var1 = var1.replace("|", "Â£");
         String[] var2 = var1.split("Â£");
         InetSocketAddress var3 = new InetSocketAddress(var2[0], Integer.parseInt(var2[1]));
         return var3;
      }
   }

   public void forceDisconnect(String var1) {
      if (!GameServer.bServer) {
         GameClient.instance.disconnect(!"receive-CustomizationData".equals(var1) && !"lua-connect".equals(var1));
      }

      this.engine.forceDisconnect(this.getConnectedGUID(), var1);
      ConnectionManager.log("force-disconnect", var1, this);
   }

   public void setFullyConnected() {
      this.validator.reset();
      this.bFullyConnected = true;
      this.setConnectionTimestamp(5000L);
      ConnectionManager.log("fully-connected", "", this);
   }

   public void setConnectionTimestamp(long var1) {
      this.connectionTimestamp = System.currentTimeMillis() + var1;
      this.setReady(false);
   }

   public void checkReady() {
      if (!this.ready) {
         boolean var1 = System.currentTimeMillis() > this.connectionTimestamp;
         if (this.ready != var1) {
            this.setReady(var1);
         }

         if (!var1) {
            IsoPlayer.getInstance().setAlpha(0, 0.6F);
         }
      }

   }

   public boolean isReady() {
      return this.ready;
   }

   public void setReady(boolean var1) {
      this.ready = var1;
   }

   public boolean isGoogleAuthTimeout() {
      return System.currentTimeMillis() > this.connectionTimestamp + 60000L;
   }

   public boolean isConnectionAttemptTimeout() {
      return System.currentTimeMillis() > this.connectionTimestamp + 5000L;
   }

   public boolean isConnectionGraceIntervalTimeout() {
      return System.currentTimeMillis() > this.connectionTimestamp + 60000L;
   }

   public boolean isFullyConnected() {
      return this.bFullyConnected;
   }

   public void calcCountPlayersInRelevantPosition() {
      if (this.isFullyConnected()) {
         boolean var1 = false;

         for(int var2 = 0; var2 < GameServer.udpEngine.connections.size(); ++var2) {
            UdpConnection var3 = (UdpConnection)GameServer.udpEngine.connections.get(var2);
            if (var3.isFullyConnected() && var3 != this) {
               for(int var4 = 0; var4 < var3.players.length; ++var4) {
                  IsoPlayer var5 = var3.players[var4];
                  if (var5 != null && this.RelevantTo(var5.getX(), var5.getY(), 120.0F)) {
                     var1 = true;
                  }
               }

               if (var1) {
                  break;
               }
            }
         }

         this.isNeighborPlayer = var1;
      }
   }

   public ZNetStatistics getStatistics() {
      try {
         this.netStatistics = this.engine.peer.GetNetStatistics(this.connectedGUID);
      } catch (Exception var5) {
         this.netStatistics = null;
      } finally {
         return this.netStatistics;
      }
   }

   public int getAveragePing() {
      return this.engine.peer.GetAveragePing(this.connectedGUID);
   }

   public int getLastPing() {
      return this.engine.peer.GetLastPing(this.connectedGUID);
   }

   public int getLowestPing() {
      return this.engine.peer.GetLowestPing(this.connectedGUID);
   }

   public int getMTUSize() {
      return this.engine.peer.GetMTUSize(this.connectedGUID);
   }

   public ConnectionType getConnectionType() {
      return UdpConnection.ConnectionType.values()[this.engine.peer.GetConnectionType(this.connectedGUID)];
   }

   public String toString() {
      if (GameClient.bClient) {
         return SystemDisabler.printDetailedInfo() ? String.format("guid=%s ip=%s steam-id=%s role=\"%s\" username=\"%s\" connection-type=\"%s\"", this.connectedGUID, this.ip == null ? GameClient.ip : this.ip, this.steamID == 0L ? GameClient.steamID : this.steamID, this.role.getName(), this.username == null ? GameClient.username : this.username, this.getConnectionType().name()) : String.format("guid=%s", this.connectedGUID);
      } else {
         return SystemDisabler.printDetailedInfo() ? String.format("guid=%s ip=%s steam-id=%s role=%s username=\"%s\" connection-type=\"%s\"", this.connectedGUID, this.ip, this.steamID, this.role.getName(), this.username, this.getConnectionType().name()) : String.format("guid=%s", this.connectedGUID);
      }
   }

   public boolean havePlayer(String var1) {
      if (StringUtils.isNullOrEmpty(var1)) {
         return false;
      } else {
         for(int var2 = 0; var2 < this.players.length; ++var2) {
            if (this.players[var2] != null && var1.equals(this.players[var2].getUsername())) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean havePlayer(IsoPlayer var1) {
      if (var1 == null) {
         return false;
      } else {
         for(int var2 = 0; var2 < this.players.length; ++var2) {
            if (this.players[var2] == var1) {
               return true;
            }
         }

         return false;
      }
   }

   public byte getPlayerIndex(IsoPlayer var1) {
      for(byte var2 = 0; var2 < this.playerIDs.length; ++var2) {
         if (this.playerIDs[var2] == var1.OnlineID) {
            return var2;
         }
      }

      return -1;
   }

   public static enum ChecksumState {
      Init,
      Different,
      Done;

      private ChecksumState() {
      }
   }

   public class MPClientStatistic {
      public byte enable = 0;
      public int diff = 0;
      public float pingAVG = 0.0F;
      public int zombiesCount = 0;
      public int zombiesLocalOwnership = 0;
      public float zombiesDesyncAVG = 0.0F;
      public float zombiesDesyncMax = 0.0F;
      public int zombiesTeleports = 0;
      public int remotePlayersCount = 0;
      public float remotePlayersDesyncAVG = 0.0F;
      public float remotePlayersDesyncMax = 0.0F;
      public int remotePlayersTeleports = 0;
      public float FPS = 0.0F;
      public float FPSMin = 0.0F;
      public float FPSAvg = 0.0F;
      public float FPSMax = 0.0F;
      public short[] FPSHistogramm = new short[32];

      public MPClientStatistic() {
      }

      public void parse(ByteBuffer var1) {
         long var2 = var1.getLong();
         long var4 = System.currentTimeMillis();
         this.diff = (int)(var4 - var2);
         this.pingAVG += ((float)this.diff * 0.5F - this.pingAVG) * 0.1F;
         this.zombiesCount = var1.getInt();
         this.zombiesLocalOwnership = var1.getInt();
         this.zombiesDesyncAVG = var1.getFloat();
         this.zombiesDesyncMax = var1.getFloat();
         this.zombiesTeleports = var1.getInt();
         this.remotePlayersCount = var1.getInt();
         this.remotePlayersDesyncAVG = var1.getFloat();
         this.remotePlayersDesyncMax = var1.getFloat();
         this.remotePlayersTeleports = var1.getInt();
         this.FPS = var1.getFloat();
         this.FPSMin = var1.getFloat();
         this.FPSAvg = var1.getFloat();
         this.FPSMax = var1.getFloat();

         for(int var6 = 0; var6 < 32; ++var6) {
            this.FPSHistogramm[var6] = var1.getShort();
         }

      }
   }

   public static enum ConnectionType {
      Disconnected,
      UDPRakNet,
      Steam;

      private ConnectionType() {
      }
   }
}
