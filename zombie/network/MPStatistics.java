package zombie.network;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.management.NotificationEmitter;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.MovingObjectUpdateScheduler;
import zombie.VirtualZombieManager;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.raknet.RakVoice;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.VoiceManagerData;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.ZNetStatistics;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.iso.WorldStreamer;
import zombie.popman.NetworkZombieManager;
import zombie.popman.NetworkZombieSimulator;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.AnimalOwnershipManager;
import zombie.util.StringUtils;

public class MPStatistics {
   private static final float MEM_USAGE_THRESHOLD = 0.95F;
   private static final long REQUEST_TIMEOUT = 10000L;
   private static final long STATISTICS_INTERVAL = 2000L;
   private static final long PING_INTERVAL = 1000L;
   private static final long PING_PERIOD = 10000L;
   private static final long PING_LIMIT_PERIOD = 60000L;
   private static final long PING_INTERVAL_COUNT = 60L;
   private static final long PING_LIMIT_COUNT = 20L;
   private static final long PING_LOG_COUNT = 120L;
   private static final long MAX_PING_TO_SUM = 1000L;
   private static final KahluaTable statsTable;
   private static final KahluaTable statusTable;
   private static final UpdateLimit ulRequestTimeout;
   private static final UpdateLimit ulStatistics;
   private static final UpdateLimit ulPing;
   private static boolean serverStatisticsEnabled;
   private static int serverPlayers;
   private static int clientPlayers;
   private static int clientAnimalObjects;
   private static int clientAnimalInstances;
   private static int clientAnimalOwned;
   private static int serverAnimalObjects;
   private static int serverAnimalInstances;
   private static int serverAnimalOwned;
   private static int clientLastPing;
   private static int clientAvgPing;
   private static int clientMinPing;
   private static String clientVOIPSource;
   private static String clientVOIPFreq;
   private static long clientVOIPRX;
   private static long clientVOIPTX;
   private static long serverVOIPRX;
   private static long serverVOIPTX;
   private static int serverWaitingRequests;
   private static int clientSentRequests;
   private static int requested1;
   private static int requested2;
   private static int pending1;
   private static int pending2;
   private static long serverCPUCores;
   private static long serverCPULoad;
   private static long serverMemMax;
   private static long serverMemFree;
   private static long serverMemTotal;
   private static long serverMemUsed;
   private static long serverRX;
   private static long serverTX;
   private static long serverResent;
   private static double serverLoss;
   private static float serverFPS;
   private static long serverNetworkingUpdates;
   private static long serverNetworkingFPS;
   private static String serverRevision;
   private static long clientCPUCores;
   private static long clientCPULoad;
   private static long clientMemMax;
   private static long clientMemFree;
   private static long clientMemTotal;
   private static long clientMemUsed;
   private static long clientRX;
   private static long clientTX;
   private static long clientResent;
   private static double clientLoss;
   private static float clientFPS;
   private static int serverStoredChunks;
   private static int serverRelevantChunks;
   private static int serverZombiesTotal;
   private static int serverZombiesLoaded;
   private static int serverZombiesSimulated;
   private static int serverZombiesCulled;
   private static int serverZombiesAuthorized;
   private static int serverZombiesUnauthorized;
   private static int serverZombiesReusable;
   private static int serverZombiesUpdated;
   private static int clientStoredChunks;
   private static int clientRelevantChunks;
   private static int clientZombiesTotal;
   private static int clientZombiesLoaded;
   private static int clientZombiesSimulated;
   private static int clientZombiesCulled;
   private static int clientZombiesAuthorized;
   private static int clientZombiesUnauthorized;
   private static int clientZombiesReusable;
   private static int clientZombiesUpdated;
   private static long zombieUpdates;
   private static long serverMinPing;
   private static long serverMaxPing;
   private static long serverAvgPing;
   private static long serverLastPing;
   private static long serverLossPing;
   private static long serverHandledPingPeriodStart;
   private static int serverHandledPingPacketIndex;
   private static final ArrayList<Long> serverHandledPingHistory;
   private static final HashSet<Long> serverHandledLossPingHistory;
   static long pingIntervalCount;
   static long pingLimitCount;
   static long maxPingToSum;
   private static int zombiesKilledByFireToday;
   private static int zombiesKilledToday;
   private static int zombifiedPlayersToday;
   private static int playersKilledByFireToday;
   private static int playersKilledByZombieToday;
   private static int playersKilledByPlayerToday;
   private static int burnedCorpsesToday;

   public MPStatistics() {
   }

   public static void onNewDay() {
      zombiesKilledByFireToday = 0;
      zombiesKilledToday = 0;
      zombifiedPlayersToday = 0;
      playersKilledByFireToday = 0;
      playersKilledByZombieToday = 0;
      playersKilledByPlayerToday = 0;
      burnedCorpsesToday = 0;
   }

   public static void onZombieWasKilled(boolean var0) {
      ++zombiesKilledToday;
      if (var0) {
         ++zombiesKilledByFireToday;
      }

   }

   public static void onPlayerWasKilled(boolean var0, boolean var1, boolean var2) {
      if (var0) {
         ++playersKilledByFireToday;
      }

      if (var2) {
         ++playersKilledByZombieToday;
      }

      if (var1) {
         ++playersKilledByPlayerToday;
      }

   }

   public static void onPlayerWasZombified() {
      ++zombifiedPlayersToday;
   }

   public static void onCorpseBurned() {
      ++burnedCorpsesToday;
   }

   public static int getZombiesKilledByFireToday() {
      return zombiesKilledByFireToday;
   }

   public static int getZombiesKilledToday() {
      return zombiesKilledToday;
   }

   public static int getZombifiedPlayersToday() {
      return zombifiedPlayersToday;
   }

   public static int getPlayersKilledByFireToday() {
      return playersKilledByFireToday;
   }

   public static int getPlayersKilledByZombieToday() {
      return playersKilledByZombieToday;
   }

   public static int getPlayersKilledByPlayerToday() {
      return playersKilledByPlayerToday;
   }

   public static int getBurnedCorpsesToday() {
      return burnedCorpsesToday;
   }

   private static boolean isClientStatisticsEnabled() {
      boolean var0 = false;
      IsoPlayer[] var1 = IsoPlayer.players;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         IsoPlayer var4 = var1[var3];
         if (var4 != null && var4.isShowMPInfos()) {
            var0 = true;
            break;
         }
      }

      return var0;
   }

   private static void getClientAnimalStatistics() {
      clientAnimalObjects = (int)IsoWorld.instance.CurrentCell.getObjectList().stream().filter((var0) -> {
         return var0 instanceof IsoAnimal;
      }).count();
      clientAnimalInstances = AnimalInstanceManager.getInstance().getAnimals().size();
      clientAnimalOwned = AnimalOwnershipManager.getInstance().getOwned();
   }

   private static void getServerAnimalStatistics() {
      serverAnimalObjects = (int)IsoWorld.instance.CurrentCell.getObjectList().stream().filter((var0) -> {
         return var0 instanceof IsoAnimal;
      }).count();
      serverAnimalInstances = AnimalInstanceManager.getInstance().getAnimals().size();
      serverAnimalOwned = AnimalOwnershipManager.getInstance().getOwned();
   }

   private static void getClientZombieStatistics() {
      int var0 = (int)Math.max(MovingObjectUpdateScheduler.instance.getFrameCounter() - zombieUpdates, 1L);
      clientZombiesTotal = GameClient.IDToZombieMap.values().length;
      clientZombiesLoaded = IsoWorld.instance.getCell().getZombieList().size();
      clientZombiesSimulated = clientZombiesUpdated / var0;
      clientZombiesAuthorized = NetworkZombieSimulator.getInstance().getAuthorizedZombieCount();
      clientZombiesUnauthorized = NetworkZombieSimulator.getInstance().getUnauthorizedZombieCount();
      clientZombiesReusable = VirtualZombieManager.instance.reusableZombiesSize();
      clientZombiesCulled = 0;
      clientZombiesUpdated = 0;
      zombieUpdates = MovingObjectUpdateScheduler.instance.getFrameCounter();
      serverZombiesCulled = 0;
   }

   private static void getServerZombieStatistics() {
      int var0 = (int)Math.max(MovingObjectUpdateScheduler.instance.getFrameCounter() - zombieUpdates, 1L);
      serverZombiesTotal = ServerMap.instance.ZombieMap.size();
      serverZombiesLoaded = IsoWorld.instance.getCell().getZombieList().size();
      serverZombiesSimulated = serverZombiesUpdated / var0;
      serverZombiesAuthorized = 0;
      serverZombiesUnauthorized = NetworkZombieManager.getInstance().getUnauthorizedZombieCount();
      serverZombiesReusable = VirtualZombieManager.instance.reusableZombiesSize();
      serverZombiesCulled = 0;
      serverZombiesUpdated = 0;
      zombieUpdates = MovingObjectUpdateScheduler.instance.getFrameCounter();
   }

   private static void getClientChunkStatistics() {
      try {
         WorldStreamer.instance.getStatistics();
      } catch (Exception var1) {
         DebugLog.Multiplayer.printException(var1, "Error getting chunk statistics", LogSeverity.Error);
      }

   }

   public static void countChunkRequests(int var0, int var1, int var2, int var3, int var4) {
      clientSentRequests = var0;
      requested1 = var1;
      requested2 = var2;
      pending1 = var3;
      pending2 = var4;
   }

   private static void resetStatistic() {
      if (GameClient.bClient) {
         GameClient.connection.netStatistics = null;
      } else {
         UdpConnection var1;
         if (GameServer.bServer) {
            for(Iterator var0 = GameServer.udpEngine.connections.iterator(); var0.hasNext(); var1.netStatistics = null) {
               var1 = (UdpConnection)var0.next();
            }
         }
      }

      serverPlayers = 0;
      clientPlayers = 0;
      clientAnimalObjects = 0;
      clientAnimalInstances = 0;
      clientAnimalOwned = 0;
      serverAnimalObjects = 0;
      serverAnimalInstances = 0;
      serverAnimalOwned = 0;
      clientVOIPSource = "";
      clientVOIPFreq = "";
      clientVOIPRX = 0L;
      clientVOIPTX = 0L;
      serverVOIPRX = 0L;
      serverVOIPTX = 0L;
      serverCPUCores = 0L;
      serverCPULoad = 0L;
      serverRX = 0L;
      serverTX = 0L;
      serverResent = 0L;
      serverLoss = 0.0;
      serverFPS = 0.0F;
      serverNetworkingFPS = 0L;
      serverMemMax = 0L;
      serverMemFree = 0L;
      serverMemTotal = 0L;
      serverMemUsed = 0L;
      clientCPUCores = 0L;
      clientCPULoad = 0L;
      clientRX = 0L;
      clientTX = 0L;
      clientResent = 0L;
      clientLoss = 0.0;
      clientFPS = 0.0F;
      clientMemMax = 0L;
      clientMemFree = 0L;
      clientMemTotal = 0L;
      clientMemUsed = 0L;
      serverZombiesTotal = 0;
      serverZombiesLoaded = 0;
      serverZombiesSimulated = 0;
      serverZombiesCulled = 0;
      serverZombiesAuthorized = 0;
      serverZombiesUnauthorized = 0;
      serverZombiesReusable = 0;
      serverZombiesUpdated = 0;
      clientZombiesTotal = 0;
      clientZombiesLoaded = 0;
      clientZombiesSimulated = 0;
      clientZombiesCulled = 0;
      clientZombiesAuthorized = 0;
      clientZombiesUnauthorized = 0;
      clientZombiesReusable = 0;
      clientZombiesUpdated = 0;
      serverWaitingRequests = 0;
      clientSentRequests = 0;
      requested1 = 0;
      requested2 = 0;
      pending1 = 0;
      pending2 = 0;
   }

   private static void getClientStatistics() {
      try {
         clientVOIPRX = 0L;
         clientVOIPTX = 0L;
         clientRX = 0L;
         clientTX = 0L;
         clientResent = 0L;
         clientLoss = 0.0;
         ZNetStatistics var0 = GameClient.connection.getStatistics();
         if (var0 != null) {
            clientRX = var0.lastActualBytesReceived / 1000L;
            clientTX = var0.lastActualBytesSent / 1000L;
            clientResent = var0.lastUserMessageBytesResent / 1000L;
            clientLoss = var0.packetlossLastSecond / 1000.0;
         }

         long[] var1 = new long[]{-1L, -1L};
         if (RakVoice.GetChannelStatistics(GameClient.connection.getConnectedGUID(), var1)) {
            clientVOIPRX = var1[0] / 2000L;
            clientVOIPTX = var1[1] / 2000L;
         }

         clientFPS = 60.0F / GameTime.instance.FPSMultiplier;
         clientCPUCores = (long)ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
         clientCPULoad = (long)(((OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean()).getProcessCpuLoad() * 100.0);
         clientMemMax = Runtime.getRuntime().maxMemory() / 1000L / 1000L;
         clientMemFree = Runtime.getRuntime().freeMemory() / 1000L / 1000L;
         clientMemTotal = Runtime.getRuntime().totalMemory() / 1000L / 1000L;
         clientMemUsed = clientMemTotal - clientMemFree;
         clientPlayers = 0;
         IsoPlayer[] var2 = IsoPlayer.players;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            IsoPlayer var5 = var2[var4];
            if (var5 != null) {
               ++clientPlayers;
            }
         }
      } catch (Exception var6) {
      }

   }

   private static void getServerStatistics() {
      try {
         serverVOIPRX = 0L;
         serverVOIPTX = 0L;
         serverRX = 0L;
         serverTX = 0L;
         serverResent = 0L;
         serverLoss = 0.0;
         long[] var0 = new long[]{-1L, -1L};
         Iterator var1 = GameServer.udpEngine.connections.iterator();

         while(var1.hasNext()) {
            UdpConnection var2 = (UdpConnection)var1.next();
            ZNetStatistics var3 = var2.getStatistics();
            if (var3 != null) {
               serverRX += var2.netStatistics.lastActualBytesReceived;
               serverTX += var2.netStatistics.lastActualBytesSent;
               serverResent += var2.netStatistics.lastUserMessageBytesResent;
               serverLoss += var2.netStatistics.packetlossLastSecond;
            }

            if (RakVoice.GetChannelStatistics(var2.getConnectedGUID(), var0)) {
               serverVOIPRX += var0[0];
               serverVOIPTX += var0[1];
            }
         }

         serverRX /= 1000L;
         serverTX /= 1000L;
         serverResent /= 1000L;
         serverLoss /= 1000.0;
         serverVOIPRX /= 2000L;
         serverVOIPTX /= 2000L;
         serverFPS = 60.0F / GameTime.instance.FPSMultiplier;
         serverCPUCores = (long)ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
         serverCPULoad = (long)(((OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean()).getProcessCpuLoad() * 100.0);
         serverNetworkingFPS = 1000L * serverNetworkingUpdates / 2000L;
         serverNetworkingUpdates = 0L;
         serverMemMax = Runtime.getRuntime().maxMemory() / 1000L / 1000L;
         serverMemFree = Runtime.getRuntime().freeMemory() / 1000L / 1000L;
         serverMemTotal = Runtime.getRuntime().totalMemory() / 1000L / 1000L;
         serverMemUsed = serverMemTotal - serverMemFree;
         serverPlayers = GameServer.IDToPlayerMap.size();
      } catch (Exception var4) {
      }

   }

   private static void resetPingCounters() {
      clientLastPing = -1;
      clientAvgPing = -1;
      clientMinPing = -1;
   }

   private static void getPing(UdpConnection var0) {
      try {
         if (var0 != null) {
            clientLastPing = var0.getLastPing();
            clientAvgPing = var0.getAveragePing();
            clientMinPing = var0.getLowestPing();
         }
      } catch (Exception var2) {
      }

   }

   static long checkLatest(UdpConnection var0, long var1) {
      if ((long)var0.pingHistory.size() >= pingIntervalCount) {
         long var3 = var0.pingHistory.stream().limit(pingIntervalCount).filter((var2) -> {
            return var2 > var1;
         }).count();
         if (var3 >= pingLimitCount) {
            return (long)Math.ceil((double)((float)var0.pingHistory.stream().limit(pingIntervalCount).mapToLong((var0x) -> {
               return Math.min(maxPingToSum, var0x);
            }).sum() / (float)pingIntervalCount));
         }
      }

      return 0L;
   }

   private static void limitPing() {
      int var0 = ServerOptions.instance.PingLimit.getValue();
      Iterator var1 = GameServer.udpEngine.connections.iterator();

      while(var1.hasNext()) {
         UdpConnection var2 = (UdpConnection)var1.next();
         serverAvgPing = (long)var2.getAveragePing();
         serverLastPing = (long)var2.getLastPing();
         var2.pingHistory.addFirst(serverLastPing);
         long var3 = checkLatest(var2, (long)var0);
         if (doKick(var2, var3)) {
            GameServer.kick(var2, "UI_Policy_Kick", "UI_OnConnectFailed_Ping");
            var2.forceDisconnect("kick-ping-limit");
            DebugLog.Multiplayer.warn("Kick: player=\"%s\" type=\"%s\"", var2.username, "UI_OnConnectFailed_Ping");
            DebugLog.Multiplayer.debugln("Ping: limit=%d/%d average-%d=%d", var0, pingLimitCount, pingIntervalCount, var3);
            DebugLog.Multiplayer.debugln("Ping: last-%d: %s", 120L, var2.pingHistory.stream().map(Object::toString).collect(Collectors.joining(", ")));
         }

         if ((long)var2.pingHistory.size() > 120L) {
            var2.pingHistory.removeLast();
         }
      }

   }

   public static boolean doKickWhileLoading(UdpConnection var0, long var1) {
      int var3 = ServerOptions.instance.PingLimit.getValue();
      return (double)var3 > ServerOptions.instance.PingLimit.getMin() && var1 > (long)var3 && !var0.role.haveCapability(Capability.CantBeKickedIfTooLaggy);
   }

   public static boolean doKick(UdpConnection var0, long var1) {
      return doKickWhileLoading(var0, var1) && var0.isFullyConnected() && var0.isConnectionGraceIntervalTimeout();
   }

   private static void resetServerHandledPingCounters() {
      serverMinPing = 0L;
      serverMaxPing = 0L;
      serverAvgPing = 0L;
      serverLastPing = 0L;
      serverLossPing = 0L;
      serverHandledPingPeriodStart = 0L;
      serverHandledPingPacketIndex = 0;
      serverHandledPingHistory.clear();
      serverHandledLossPingHistory.clear();
   }

   private static void getServerHandledPing() {
      long var0 = System.currentTimeMillis();
      if ((long)serverHandledPingPacketIndex == 10L) {
         serverMinPing = serverHandledPingHistory.stream().mapToLong((var0x) -> {
            return var0x;
         }).min().orElse(0L);
         serverMaxPing = serverHandledPingHistory.stream().mapToLong((var0x) -> {
            return var0x;
         }).max().orElse(0L);
         serverAvgPing = (long)serverHandledPingHistory.stream().mapToLong((var0x) -> {
            return var0x;
         }).average().orElse(0.0);
         serverHandledPingHistory.clear();
         serverHandledPingPacketIndex = 0;
         int var2 = serverHandledLossPingHistory.size();
         serverHandledLossPingHistory.removeIf((var2x) -> {
            return var0 > var2x + 10000L;
         });
         serverLossPing += (long)(var2 - serverHandledLossPingHistory.size());
         serverHandledPingPeriodStart = var0;
      }

      GameClient.sendServerPing(var0);
      if (serverHandledLossPingHistory.size() > 1000) {
         serverHandledLossPingHistory.clear();
      }

      serverHandledLossPingHistory.add(var0);
      ++serverHandledPingPacketIndex;
   }

   public static void setVOIPSource(VoiceManagerData.VoiceDataSource var0, int var1) {
      clientVOIPSource = VoiceManagerData.VoiceDataSource.Unknown.equals(var0) ? "" : var0.name();
      clientVOIPFreq = var1 == 0 ? "" : String.valueOf((float)var1 / 1000.0F);
   }

   public static void countServerNetworkingFPS() {
      ++serverNetworkingUpdates;
   }

   public static void increaseStoredChunk() {
      if (GameClient.bClient) {
         ++clientStoredChunks;
      } else if (GameServer.bServer) {
         ++serverStoredChunks;
      }

      decreaseRelevantChunk();
   }

   public static void decreaseStoredChunk() {
      if (GameClient.bClient) {
         --clientStoredChunks;
      } else if (GameServer.bServer) {
         --serverStoredChunks;
      }

      increaseRelevantChunk();
   }

   public static void increaseRelevantChunk() {
      if (GameClient.bClient) {
         ++clientRelevantChunks;
      } else if (GameServer.bServer) {
         ++serverRelevantChunks;
      }

   }

   public static void decreaseRelevantChunk() {
      if (GameClient.bClient) {
         --clientRelevantChunks;
      } else if (GameServer.bServer) {
         --serverRelevantChunks;
      }

   }

   public static void Init() {
      if (GameServer.bServer || GameClient.bClient) {
         try {
            Iterator var0 = ManagementFactory.getMemoryPoolMXBeans().iterator();

            while(var0.hasNext()) {
               MemoryPoolMXBean var1 = (MemoryPoolMXBean)var0.next();
               if (MemoryType.HEAP.equals(var1.getType()) && var1.isUsageThresholdSupported()) {
                  long var2 = var1.getCollectionUsageThreshold();
                  String var4 = System.getProperty("zomboid.thresholdm");
                  if (!StringUtils.isNullOrEmpty(var4)) {
                     var2 = Long.parseLong(var4) * 1000000L;
                  }

                  if (var2 == 0L) {
                     var2 = (long)((float)Runtime.getRuntime().maxMemory() * 0.95F);
                     var1.setUsageThreshold(var2);
                  }

                  if (var2 > 0L) {
                     ((NotificationEmitter)ManagementFactory.getMemoryMXBean()).addNotificationListener((var1x, var2x) -> {
                        DebugLog.Multiplayer.warn("[%s] %s (%d) free=%s", MPStatistics.class.getSimpleName(), "java.management.memory.threshold.exceeded", var1.getUsageThresholdCount(), NumberFormat.getNumberInstance().format(Runtime.getRuntime().freeMemory()));
                     }, (var0x) -> {
                        return "java.management.memory.threshold.exceeded".equals(var0x.getType());
                     }, (Object)null);
                  }

                  DebugLog.log(DebugType.Multiplayer, String.format("MEM usage notification threshold %d", var2));
                  break;
               }
            }
         } catch (Exception var5) {
            DebugLog.Multiplayer.printException(var5, String.format("[%s] init error", MPStatistics.class.getSimpleName()), LogSeverity.Error);
         }

         Reset();
      }

   }

   public static void Reset() {
      resetPingCounters();
      resetServerHandledPingCounters();
      resetStatistic();
   }

   public static void Update() {
      if (GameClient.bClient) {
         if (ulPing.Check()) {
            if (!isClientStatisticsEnabled() && !NetworkAIParams.isShowServerInfo()) {
               resetPingCounters();
               resetServerHandledPingCounters();
            } else {
               getPing(GameClient.connection);
               if (isClientStatisticsEnabled()) {
                  getServerHandledPing();
               } else {
                  resetServerHandledPingCounters();
               }
            }
         }

         if (isClientStatisticsEnabled()) {
            if (ulStatistics.Check()) {
               getClientStatistics();
               getClientZombieStatistics();
               getClientChunkStatistics();
               getClientAnimalStatistics();
            }
         } else {
            resetStatistic();
         }
      } else if (GameServer.bServer) {
         if (ulPing.Check()) {
            limitPing();
         }

         if (ulRequestTimeout.Check()) {
            serverStatisticsEnabled = false;
         }

         if (serverStatisticsEnabled) {
            if (ulStatistics.Check()) {
               getServerStatistics();
               getServerZombieStatistics();
               getServerAnimalStatistics();
            }
         } else {
            resetStatistic();
         }
      }

   }

   public static void requested() {
      serverStatisticsEnabled = true;
      ulRequestTimeout.Reset(10000L);
   }

   public static void clientZombieCulled() {
      ++clientZombiesCulled;
   }

   public static void serverZombieCulled() {
      ++serverZombiesCulled;
   }

   public static void clientZombieUpdated() {
      ++clientZombiesUpdated;
   }

   public static void serverZombieUpdated() {
      ++serverZombiesUpdated;
   }

   public static void write(UdpConnection var0, ByteBuffer var1) {
      var1.putLong(serverMemMax);
      var1.putLong(serverMemFree);
      var1.putLong(serverMemTotal);
      var1.putLong(serverMemUsed);
      var1.putLong(serverCPUCores);
      var1.putLong(serverCPULoad);
      var1.putLong(serverVOIPRX);
      var1.putLong(serverVOIPTX);
      var1.putLong(serverRX);
      var1.putLong(serverTX);
      var1.putLong(serverResent);
      var1.putDouble(serverLoss);
      var1.putFloat(serverFPS);
      var1.putLong(serverNetworkingFPS);
      var1.putInt(serverStoredChunks);
      var1.putInt(serverRelevantChunks);
      var1.putInt(serverZombiesTotal);
      var1.putInt(serverZombiesLoaded);
      var1.putInt(serverZombiesSimulated);
      var1.putInt(serverZombiesCulled);
      var1.putInt(NetworkZombieManager.getInstance().getAuthorizedZombieCount(var0));
      var1.putInt(serverZombiesUnauthorized);
      var1.putInt(serverZombiesReusable);
      var1.putInt(var0.playerDownloadServer.getWaitingRequests());
      var1.putInt(serverPlayers);
      var1.putInt(serverAnimalObjects);
      var1.putInt(serverAnimalInstances);
      var1.putInt(serverAnimalOwned);
      GameWindow.WriteString(var1, "25057");
   }

   public static void parse(ByteBuffer var0) {
      long var1 = System.currentTimeMillis();
      long var3 = var0.getLong();
      serverMemMax = var0.getLong();
      serverMemFree = var0.getLong();
      serverMemTotal = var0.getLong();
      serverMemUsed = var0.getLong();
      serverCPUCores = var0.getLong();
      serverCPULoad = var0.getLong();
      serverVOIPRX = var0.getLong();
      serverVOIPTX = var0.getLong();
      serverRX = var0.getLong();
      serverTX = var0.getLong();
      serverResent = var0.getLong();
      serverLoss = var0.getDouble();
      serverFPS = var0.getFloat();
      serverNetworkingFPS = var0.getLong();
      serverStoredChunks = var0.getInt();
      serverRelevantChunks = var0.getInt();
      serverZombiesTotal = var0.getInt();
      serverZombiesLoaded = var0.getInt();
      serverZombiesSimulated = var0.getInt();
      serverZombiesCulled += var0.getInt();
      serverZombiesAuthorized = var0.getInt();
      serverZombiesUnauthorized = var0.getInt();
      serverZombiesReusable = var0.getInt();
      serverWaitingRequests = var0.getInt();
      serverPlayers = var0.getInt();
      serverAnimalObjects = var0.getInt();
      serverAnimalInstances = var0.getInt();
      serverAnimalOwned = var0.getInt();
      serverRevision = GameWindow.ReadString(var0);
      serverHandledLossPingHistory.remove(var3);
      if (var3 >= serverHandledPingPeriodStart) {
         serverLastPing = var1 - var3;
         serverHandledPingHistory.add(serverLastPing);
      }

   }

   public static KahluaTable getLuaStatus() {
      statusTable.wipe();
      if (GameClient.bClient) {
         statusTable.rawset("serverTime", NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toSeconds(GameTime.getServerTime())));
         statusTable.rawset("svnRevision", "25057");
         statusTable.rawset("buildDate", "2024-12-17");
         statusTable.rawset("buildTime", "17:38:44");
         statusTable.rawset("position", String.format("( %.3f ; %.3f ; %.3f )", IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), IsoPlayer.getInstance().getZ()));
         statusTable.rawset("version", Core.getInstance().getVersion());
         statusTable.rawset("lastPing", String.format("%03d", clientLastPing));
         statusTable.rawset("avgPing", String.valueOf(clientAvgPing));
         statusTable.rawset("minPing", String.valueOf(clientMinPing));
      }

      return statusTable;
   }

   public static KahluaTable getLuaStatistics() {
      statsTable.wipe();
      if (GameClient.bClient) {
         statsTable.rawset("clientTime", String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
         statsTable.rawset("serverTime", NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toSeconds(GameTime.getServerTime())));
         statsTable.rawset("clientRevision", String.valueOf("25057"));
         statsTable.rawset("serverRevision", String.valueOf(serverRevision));
         statsTable.rawset("clientPlayers", String.valueOf(clientPlayers));
         statsTable.rawset("serverPlayers", String.valueOf(serverPlayers));
         statsTable.rawset("clientAnimalObjects", String.valueOf(clientAnimalObjects));
         statsTable.rawset("clientAnimalInstances", String.valueOf(clientAnimalInstances));
         statsTable.rawset("clientAnimalOwned", String.valueOf(clientAnimalOwned));
         statsTable.rawset("serverAnimalObjects", String.valueOf(serverAnimalObjects));
         statsTable.rawset("serverAnimalInstances", String.valueOf(serverAnimalInstances));
         statsTable.rawset("serverAnimalOwned", String.valueOf(serverAnimalOwned));
         statsTable.rawset("clientVOIPSource", String.valueOf(clientVOIPSource));
         statsTable.rawset("clientVOIPFreq", String.valueOf(clientVOIPFreq));
         statsTable.rawset("clientVOIPRX", String.valueOf(clientVOIPRX));
         statsTable.rawset("clientVOIPTX", String.valueOf(clientVOIPTX));
         statsTable.rawset("clientRX", String.valueOf(clientRX));
         statsTable.rawset("clientTX", String.valueOf(clientTX));
         statsTable.rawset("clientResent", String.valueOf(clientResent));
         statsTable.rawset("clientLoss", String.valueOf((int)clientLoss));
         statsTable.rawset("serverVOIPRX", String.valueOf(serverVOIPRX));
         statsTable.rawset("serverVOIPTX", String.valueOf(serverVOIPTX));
         statsTable.rawset("serverRX", String.valueOf(serverRX));
         statsTable.rawset("serverTX", String.valueOf(serverTX));
         statsTable.rawset("serverResent", String.valueOf(serverResent));
         statsTable.rawset("serverLoss", String.valueOf((int)serverLoss));
         statsTable.rawset("clientLastPing", String.valueOf(clientLastPing));
         statsTable.rawset("clientAvgPing", String.valueOf(clientAvgPing));
         statsTable.rawset("clientMinPing", String.valueOf(clientMinPing));
         statsTable.rawset("serverPingLast", String.valueOf(serverLastPing));
         statsTable.rawset("serverPingMin", String.valueOf(serverMinPing));
         statsTable.rawset("serverPingAvg", String.valueOf(serverAvgPing));
         statsTable.rawset("serverPingMax", String.valueOf(serverMaxPing));
         statsTable.rawset("serverPingLoss", String.valueOf(serverLossPing));
         statsTable.rawset("clientCPUCores", String.valueOf(clientCPUCores));
         statsTable.rawset("clientCPULoad", String.valueOf(clientCPULoad));
         statsTable.rawset("clientMemMax", String.valueOf(clientMemMax));
         statsTable.rawset("clientMemFree", String.valueOf(clientMemFree));
         statsTable.rawset("clientMemTotal", String.valueOf(clientMemTotal));
         statsTable.rawset("clientMemUsed", String.valueOf(clientMemUsed));
         statsTable.rawset("serverCPUCores", String.valueOf(serverCPUCores));
         statsTable.rawset("serverCPULoad", String.valueOf(serverCPULoad));
         statsTable.rawset("serverMemMax", String.valueOf(serverMemMax));
         statsTable.rawset("serverMemFree", String.valueOf(serverMemFree));
         statsTable.rawset("serverMemTotal", String.valueOf(serverMemTotal));
         statsTable.rawset("serverMemUsed", String.valueOf(serverMemUsed));
         statsTable.rawset("serverNetworkingFPS", String.valueOf((int)serverNetworkingFPS));
         statsTable.rawset("serverFPS", String.valueOf((int)serverFPS));
         statsTable.rawset("clientFPS", String.valueOf((int)clientFPS));
         statsTable.rawset("serverStoredChunks", String.valueOf(serverStoredChunks));
         statsTable.rawset("serverRelevantChunks", String.valueOf(serverRelevantChunks));
         statsTable.rawset("serverZombiesTotal", String.valueOf(serverZombiesTotal));
         statsTable.rawset("serverZombiesLoaded", String.valueOf(serverZombiesLoaded));
         statsTable.rawset("serverZombiesSimulated", String.valueOf(serverZombiesSimulated));
         statsTable.rawset("serverZombiesCulled", String.valueOf(serverZombiesCulled));
         statsTable.rawset("serverZombiesAuthorized", String.valueOf(serverZombiesAuthorized));
         statsTable.rawset("serverZombiesUnauthorized", String.valueOf(serverZombiesUnauthorized));
         statsTable.rawset("serverZombiesReusable", String.valueOf(serverZombiesReusable));
         statsTable.rawset("clientStoredChunks", String.valueOf(clientStoredChunks));
         statsTable.rawset("clientRelevantChunks", String.valueOf(clientRelevantChunks));
         statsTable.rawset("clientZombiesTotal", String.valueOf(clientZombiesTotal));
         statsTable.rawset("clientZombiesLoaded", String.valueOf(clientZombiesLoaded));
         statsTable.rawset("clientZombiesSimulated", String.valueOf(clientZombiesSimulated));
         statsTable.rawset("clientZombiesCulled", String.valueOf(clientZombiesCulled));
         statsTable.rawset("clientZombiesAuthorized", String.valueOf(clientZombiesAuthorized));
         statsTable.rawset("clientZombiesUnauthorized", String.valueOf(clientZombiesUnauthorized));
         statsTable.rawset("clientZombiesReusable", String.valueOf(clientZombiesReusable));
         statsTable.rawset("serverWaitingRequests", String.valueOf(serverWaitingRequests));
         statsTable.rawset("clientSentRequests", String.valueOf(clientSentRequests));
         statsTable.rawset("requested1", String.valueOf(requested1));
         statsTable.rawset("requested2", String.valueOf(requested2));
         statsTable.rawset("pending1", String.valueOf(pending1));
         statsTable.rawset("pending2", String.valueOf(pending2));
      }

      return statsTable;
   }

   static {
      statsTable = LuaManager.platform.newTable();
      statusTable = LuaManager.platform.newTable();
      ulRequestTimeout = new UpdateLimit(10000L);
      ulStatistics = new UpdateLimit(2000L);
      ulPing = new UpdateLimit(1000L);
      serverStatisticsEnabled = false;
      serverPlayers = 0;
      clientPlayers = 0;
      clientAnimalObjects = 0;
      clientAnimalInstances = 0;
      clientAnimalOwned = 0;
      serverAnimalObjects = 0;
      serverAnimalInstances = 0;
      serverAnimalOwned = 0;
      clientLastPing = -1;
      clientAvgPing = -1;
      clientMinPing = -1;
      clientVOIPSource = "";
      clientVOIPFreq = "";
      clientVOIPRX = 0L;
      clientVOIPTX = 0L;
      serverVOIPRX = 0L;
      serverVOIPTX = 0L;
      serverWaitingRequests = 0;
      clientSentRequests = 0;
      requested1 = 0;
      requested2 = 0;
      pending1 = 0;
      pending2 = 0;
      serverCPUCores = 0L;
      serverCPULoad = 0L;
      serverMemMax = 0L;
      serverMemFree = 0L;
      serverMemTotal = 0L;
      serverMemUsed = 0L;
      serverRX = 0L;
      serverTX = 0L;
      serverResent = 0L;
      serverLoss = 0.0;
      serverFPS = 0.0F;
      serverNetworkingUpdates = 0L;
      serverNetworkingFPS = 0L;
      serverRevision = "";
      clientCPUCores = 0L;
      clientCPULoad = 0L;
      clientMemMax = 0L;
      clientMemFree = 0L;
      clientMemTotal = 0L;
      clientMemUsed = 0L;
      clientRX = 0L;
      clientTX = 0L;
      clientResent = 0L;
      clientLoss = 0.0;
      clientFPS = 0.0F;
      serverStoredChunks = 0;
      serverRelevantChunks = 0;
      serverZombiesTotal = 0;
      serverZombiesLoaded = 0;
      serverZombiesSimulated = 0;
      serverZombiesCulled = 0;
      serverZombiesAuthorized = 0;
      serverZombiesUnauthorized = 0;
      serverZombiesReusable = 0;
      serverZombiesUpdated = 0;
      clientStoredChunks = 0;
      clientRelevantChunks = 0;
      clientZombiesTotal = 0;
      clientZombiesLoaded = 0;
      clientZombiesSimulated = 0;
      clientZombiesCulled = 0;
      clientZombiesAuthorized = 0;
      clientZombiesUnauthorized = 0;
      clientZombiesReusable = 0;
      clientZombiesUpdated = 0;
      zombieUpdates = 0L;
      serverMinPing = 0L;
      serverMaxPing = 0L;
      serverAvgPing = 0L;
      serverLastPing = 0L;
      serverLossPing = 0L;
      serverHandledPingPeriodStart = 0L;
      serverHandledPingPacketIndex = 0;
      serverHandledPingHistory = new ArrayList();
      serverHandledLossPingHistory = new HashSet();
      pingIntervalCount = 60L;
      pingLimitCount = 20L;
      maxPingToSum = 1000L;
      zombiesKilledByFireToday = 0;
      zombiesKilledToday = 0;
      zombifiedPlayersToday = 0;
      playersKilledByFireToday = 0;
      playersKilledByZombieToday = 0;
      playersKilledByPlayerToday = 0;
      burnedCorpsesToday = 0;
   }
}
