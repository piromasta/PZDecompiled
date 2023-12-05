package zombie.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.LogSeverity;

public class MPStatistic {
   public static MPStatistic instance;
   private static boolean doPrintStatistic = false;
   private static boolean doCSVStatistic = false;
   private static int Period = 0;
   public TasksStatistic LoaderThreadTasks = new TasksStatistic();
   public TasksStatistic RecalcThreadTasks = new TasksStatistic();
   public SaveTasksStatistic SaveTasks = new SaveTasksStatistic();
   public ServerCellStatistic ServerMapToLoad = new ServerCellStatistic();
   public ServerCellStatistic ServerMapLoadedCells = new ServerCellStatistic();
   public ServerCellStatistic ServerMapLoaded2 = new ServerCellStatistic();
   private int countServerChunkThreadSaveNow = 0;
   public MainThreadStatistic Main = new MainThreadStatistic();
   public ThreadStatistic ServerLOS = new ThreadStatistic();
   public ThreadStatistic LoaderThread = new ThreadStatistic();
   public ThreadStatistic RecalcAllThread = new ThreadStatistic();
   public ThreadStatistic SaveThread = new ThreadStatistic();
   public ThreadStatistic PolyPathThread = new ThreadStatistic();
   public ThreadStatistic WorldReuser = new ThreadStatistic();
   public ThreadStatistic PlayerDownloadServer = new ThreadStatistic();
   public ThreadStatistic MapCollisionThread = new ThreadStatistic();
   public ProbeStatistic ChunkChecksum = new ProbeStatistic();
   public ProbeStatistic Bullet = new ProbeStatistic();
   public ProbeStatistic AnimationPlayerUpdate = new ProbeStatistic();
   public ProbeStatistic ServerMapPreupdate = new ProbeStatistic();
   public ProbeStatistic ServerMapPostupdate = new ProbeStatistic();
   public ProbeStatistic IngameStateUpdate = new ProbeStatistic();
   private long packetLength = 0L;
   private int countIncomePackets = 0;
   private int countOutcomePackets = 0;
   private int countIncomeBytes = 0;
   private int countOutcomeBytes = 0;
   private int maxIncomeBytesPerSecond = 0;
   private int maxOutcomeBytesPerSecond = 0;
   private int currentIncomeBytesPerSecond = 0;
   private int currentOutcomeBytesPerSecond = 0;
   private long lastCalculateBPS = 0L;
   private long lastReport = 0L;
   private long minUpdatePeriod = 9999L;
   private long maxUpdatePeriod = 0L;
   private long avgUpdatePeriod = 0L;
   private long currentAvgUpdatePeriod = 0L;
   private long teleports = 0L;
   private long counter1 = 0L;
   private long counter2 = 0L;
   private long counter3 = 0L;
   private long updatePeriods = 0L;
   private int loadCellFromDisk = 0;
   private int saveCellToDisk = 0;
   public static boolean clientStatisticEnable = false;
   private PrintStream csvStatisticFile = null;
   private PrintStream csvIncomePacketsFile = null;
   private PrintStream csvIncomeBytesFile = null;
   private PrintStream csvOutcomePacketsFile = null;
   private PrintStream csvOutcomeBytesFile = null;
   private PrintStream csvConnectionsFile = null;
   private final ArrayList<Integer> csvConnections = new ArrayList();
   private KahluaTable table = null;

   public MPStatistic() {
   }

   public static MPStatistic getInstance() {
      if (instance == null) {
         instance = new MPStatistic();
      }

      return instance;
   }

   public void IncrementServerChunkThreadSaveNow() {
      ++this.countServerChunkThreadSaveNow;
   }

   public void teleport() {
      ++this.teleports;
   }

   public void count1(long var1) {
      this.counter1 += var1;
   }

   public void count2(long var1) {
      this.counter2 += var1;
   }

   public void count3(long var1) {
      this.counter3 += var1;
   }

   public void write(ByteBufferWriter var1) {
      var1.putLong(this.minUpdatePeriod);
      var1.putLong(this.maxUpdatePeriod);
      var1.putLong(this.currentAvgUpdatePeriod / this.updatePeriods);
      var1.putLong(this.updatePeriods / (long)Period);
      var1.putLong(this.teleports);
      var1.putLong((long)GameServer.udpEngine.connections.size());
      var1.putLong(this.counter1 / this.updatePeriods);
      var1.putLong(this.counter2 / this.updatePeriods);
      var1.putLong(this.counter3 / this.updatePeriods);
   }

   public void setPacketsLength(long var1) {
      this.packetLength = var1;
   }

   public void addIncomePacket(PacketTypes.PacketType var1, int var2) {
      if (var1 != null) {
         ++var1.incomePackets;
         ++this.countIncomePackets;
         var1.incomeBytes += var2;
         this.countIncomeBytes += var2;
         this.currentIncomeBytesPerSecond += var2;
         this.calculateMaxBPS();
      }

   }

   public void addOutcomePacket(short var1, int var2) {
      PacketTypes.PacketType var3 = (PacketTypes.PacketType)PacketTypes.packetTypes.get(var1);
      if (var3 != null) {
         ++var3.outcomePackets;
         ++this.countOutcomePackets;
         var3.outcomeBytes += var2;
         this.countOutcomeBytes += var2;
         this.currentOutcomeBytesPerSecond += var2;
         this.calculateMaxBPS();
      }

   }

   void calculateMaxBPS() {
      if (System.currentTimeMillis() - this.lastCalculateBPS > 1000L) {
         this.lastCalculateBPS = System.currentTimeMillis();
         if (this.currentIncomeBytesPerSecond > this.maxIncomeBytesPerSecond) {
            this.maxIncomeBytesPerSecond = this.currentIncomeBytesPerSecond;
         }

         if (this.currentOutcomeBytesPerSecond > this.maxOutcomeBytesPerSecond) {
            this.maxOutcomeBytesPerSecond = this.currentOutcomeBytesPerSecond;
         }

         this.currentIncomeBytesPerSecond = 0;
         this.currentOutcomeBytesPerSecond = 0;
      }

   }

   public void IncrementLoadCellFromDisk() {
      ++this.loadCellFromDisk;
   }

   public void IncrementSaveCellToDisk() {
      ++this.saveCellToDisk;
   }

   public void process(long var1) {
      if (var1 > this.maxUpdatePeriod) {
         this.maxUpdatePeriod = var1;
      }

      if (var1 < this.minUpdatePeriod) {
         this.minUpdatePeriod = var1;
      }

      this.avgUpdatePeriod = (long)((float)this.avgUpdatePeriod + (float)(var1 - this.avgUpdatePeriod) * 0.05F);
      this.currentAvgUpdatePeriod += var1;
      ++this.updatePeriods;
      if (Period != 0 && System.currentTimeMillis() - this.lastReport >= (long)Period * 1000L) {
         this.lastReport = System.currentTimeMillis();
         this.printStatistic();
         this.printCSVStatistic();
         GameServer.sendShortStatistic();
         this.table = LuaManager.platform.newTable();
         this.table.rawset("lastReport", (double)this.lastReport);
         this.table.rawset("period", (double)Period);
         this.table.rawset("minUpdatePeriod", (double)this.minUpdatePeriod);
         this.table.rawset("maxUpdatePeriod", (double)this.maxUpdatePeriod);
         this.table.rawset("avgUpdatePeriod", (double)this.avgUpdatePeriod);
         this.maxUpdatePeriod = 0L;
         this.minUpdatePeriod = 9999L;
         this.currentAvgUpdatePeriod = 0L;
         this.updatePeriods = 0L;
         this.teleports = 0L;
         this.counter1 = 0L;
         this.counter2 = 0L;
         this.counter3 = 0L;
         this.table.rawset("loadCellFromDisk", (double)this.loadCellFromDisk);
         this.table.rawset("saveCellToDisk", (double)this.saveCellToDisk);
         this.loadCellFromDisk = 0;
         this.saveCellToDisk = 0;
         this.table.rawset("usedMemory", (double)(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
         this.table.rawset("totalMemory", (double)Runtime.getRuntime().totalMemory());
         this.table.rawset("freeMemory", (double)Runtime.getRuntime().freeMemory());
         this.table.rawset("countConnections", (double)GameServer.udpEngine.connections.size());
         KahluaTable var3 = LuaManager.platform.newTable();

         KahluaTable var9;
         KahluaTable var17;
         for(int var4 = 0; var4 < GameServer.udpEngine.connections.size(); ++var4) {
            KahluaTable var5 = LuaManager.platform.newTable();
            UdpConnection var6 = (UdpConnection)GameServer.udpEngine.connections.get(var4);
            var5.rawset("ip", var6.ip);
            var5.rawset("username", var6.username);
            var5.rawset("accessLevel", var6.accessLevel);
            KahluaTable var7 = LuaManager.platform.newTable();

            for(int var8 = 0; var8 < var6.players.length; ++var8) {
               if (var6.players[var8] != null) {
                  var9 = LuaManager.platform.newTable();
                  var9.rawset("username", var6.players[var8].username);
                  var9.rawset("x", (double)var6.players[var8].x);
                  var9.rawset("y", (double)var6.players[var8].y);
                  var9.rawset("z", (double)var6.players[var8].z);
                  var7.rawset(var8, var9);
               }
            }

            var5.rawset("users", var7);
            var5.rawset("diff", (double)var6.statistic.diff);
            var5.rawset("pingAVG", (double)var6.statistic.pingAVG);
            var5.rawset("remotePlayersCount", (double)var6.statistic.remotePlayersCount);
            var5.rawset("remotePlayersDesyncAVG", (double)var6.statistic.remotePlayersDesyncAVG);
            var5.rawset("remotePlayersDesyncMax", (double)var6.statistic.remotePlayersDesyncMax);
            var5.rawset("remotePlayersTeleports", (double)var6.statistic.remotePlayersTeleports);
            var5.rawset("zombiesCount", (double)var6.statistic.zombiesCount);
            var5.rawset("zombiesLocalOwnership", (double)var6.statistic.zombiesLocalOwnership);
            var5.rawset("zombiesDesyncAVG", (double)var6.statistic.zombiesDesyncAVG);
            var5.rawset("zombiesDesyncMax", (double)var6.statistic.zombiesDesyncMax);
            var5.rawset("zombiesTeleports", (double)var6.statistic.zombiesTeleports);
            var5.rawset("FPS", (double)var6.statistic.FPS);
            var5.rawset("FPSMin", (double)var6.statistic.FPSMin);
            var5.rawset("FPSAvg", (double)var6.statistic.FPSAvg);
            var5.rawset("FPSMax", (double)var6.statistic.FPSMax);
            var17 = LuaManager.platform.newTable();
            short var18 = 0;

            for(int var10 = 0; var10 < 32; ++var10) {
               var17.rawset(var10, (double)var6.statistic.FPSHistogramm[var10]);
               if (var18 < var6.statistic.FPSHistogramm[var10]) {
                  var18 = var6.statistic.FPSHistogramm[var10];
               }
            }

            var5.rawset("FPSHistogram", var17);
            var5.rawset("FPSHistogramMax", (double)var18);
            var3.rawset(var4, var5);
         }

         this.table.rawset("connections", var3);
         this.table.rawset("packetLength", (double)this.packetLength);
         this.table.rawset("countIncomePackets", (double)this.countIncomePackets);
         this.table.rawset("countIncomeBytes", (double)this.countIncomeBytes);
         this.table.rawset("maxIncomeBytesPerSecound", (double)this.maxIncomeBytesPerSecond);
         KahluaTable var11 = LuaManager.platform.newTable();
         int var12 = -1;

         PacketTypes.PacketType var15;
         for(Iterator var13 = PacketTypes.packetTypes.values().iterator(); var13.hasNext(); var15.incomeBytes = 0) {
            var15 = (PacketTypes.PacketType)var13.next();
            if (var15.incomePackets > 0) {
               var17 = LuaManager.platform.newTable();
               var17.rawset("name", var15.name());
               var17.rawset("count", (double)var15.incomePackets);
               var17.rawset("bytes", (double)var15.incomeBytes);
               var11.rawset(var12, var17);
            }

            var15.incomePackets = 0;
         }

         this.table.rawset("incomePacketsTable", var11);
         this.countIncomePackets = 0;
         this.countIncomeBytes = 0;
         this.maxIncomeBytesPerSecond = 0;
         this.table.rawset("countOutcomePackets", (double)this.countOutcomePackets);
         this.table.rawset("countOutcomeBytes", (double)this.countOutcomeBytes);
         this.table.rawset("maxOutcomeBytesPerSecound", (double)this.maxOutcomeBytesPerSecond);
         KahluaTable var14 = LuaManager.platform.newTable();
         var12 = -1;

         PacketTypes.PacketType var19;
         for(Iterator var16 = PacketTypes.packetTypes.values().iterator(); var16.hasNext(); var19.outcomeBytes = 0) {
            var19 = (PacketTypes.PacketType)var16.next();
            if (var19.outcomePackets > 0) {
               var9 = LuaManager.platform.newTable();
               var9.rawset("name", var19.name());
               var9.rawset("count", (double)var19.outcomePackets);
               var9.rawset("bytes", (double)var19.outcomeBytes);
               var14.rawset(var12++, var9);
            }

            var19.outcomePackets = 0;
         }

         this.table.rawset("outcomePacketsTable", var14);
         this.countOutcomePackets = 0;
         this.countOutcomeBytes = 0;
         this.maxOutcomeBytesPerSecond = 0;
         this.LoaderThreadTasks.Clear();
         this.RecalcThreadTasks.Clear();
         this.SaveTasks.Clear();
         this.ServerMapToLoad.Clear();
         this.ServerMapLoadedCells.Clear();
         this.ServerMapLoaded2.Clear();
         this.countServerChunkThreadSaveNow = 0;
         this.Main.Clear();
         this.ServerLOS.Clear();
         this.LoaderThread.Clear();
         this.RecalcAllThread.Clear();
         this.SaveThread.Clear();
         this.PolyPathThread.Clear();
         this.WorldReuser.Clear();
         this.PlayerDownloadServer.Clear();
         this.MapCollisionThread.Clear();
         this.ChunkChecksum.Clear();
         this.Bullet.Clear();
         this.AnimationPlayerUpdate.Clear();
         this.ServerMapPreupdate.Clear();
         this.ServerMapPostupdate.Clear();
         this.IngameStateUpdate.Clear();
         GameServer.getStatisticFromClients();
         GameServer.sendStatistic();
      }
   }

   private void printStatistic() {
      if (doPrintStatistic) {
         DebugLog.Statistic.println("=== STATISTICS ===");
         DebugLog.Statistic.println("UpdatePeriod (mils) min:" + this.minUpdatePeriod + " max:" + this.maxUpdatePeriod + " avg:" + this.avgUpdatePeriod);
         DebugLog.Statistic.println("Server cell disk operations load:" + this.loadCellFromDisk + " save:" + this.saveCellToDisk);
         DebugLogStream var10000 = DebugLog.Statistic;
         long var10001 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
         var10000.println("Memory (bytes):" + var10001 + " of " + Runtime.getRuntime().totalMemory());
         DebugLog.Statistic.println("== Connections:" + GameServer.udpEngine.connections.size() + " ==");

         String var6;
         for(int var1 = 0; var1 < GameServer.udpEngine.connections.size(); ++var1) {
            UdpConnection var2 = (UdpConnection)GameServer.udpEngine.connections.get(var1);
            DebugLog.Statistic.println("Connection " + var1 + " " + var2.ip + " " + var2.username + " " + var2.accessLevel);

            for(int var3 = 0; var3 < var2.players.length; ++var3) {
               if (var2.players[var3] != null) {
                  var6 = var2.players[var3].username;
                  DebugLog.Statistic.println("  User " + var6 + " (" + var2.players[var3].x + ", " + var2.players[var3].y + ", " + var2.players[var3].z + ")");
               }
            }

            int var7 = var2.statistic.diff / 2;
            DebugLog.Statistic.println("  Ping:" + var7 + " AVG:" + var2.statistic.pingAVG);
            DebugLog.Statistic.println("  Players count:" + var2.statistic.remotePlayersCount + " desyncAVG:" + var2.statistic.remotePlayersDesyncAVG + " desyncMAX:" + var2.statistic.remotePlayersDesyncMax + " teleports:" + var2.statistic.remotePlayersTeleports);
            DebugLog.Statistic.println("  Zombies count:" + var2.statistic.zombiesCount + " LocalOwnership:" + var2.statistic.zombiesLocalOwnership + " desyncAVG:" + var2.statistic.zombiesDesyncAVG + " desyncMAX:" + var2.statistic.zombiesDesyncMax + " teleports:" + var2.statistic.zombiesTeleports);
            DebugLog.Statistic.println("  FPS:" + var2.statistic.FPS + " Min:" + var2.statistic.FPSMin + " Avg:" + var2.statistic.FPSAvg + " Max:" + var2.statistic.FPSMax);
         }

         DebugLog.Statistic.println("== Income Packets ==");
         DebugLog.Statistic.println("length of packet queue:" + this.packetLength);
         DebugLog.Statistic.println("count packets:" + this.countIncomePackets);
         DebugLog.Statistic.println("count bytes:" + this.countIncomeBytes);
         DebugLog.Statistic.println("max bps:" + this.maxIncomeBytesPerSecond);
         Iterator var4 = PacketTypes.packetTypes.values().iterator();

         PacketTypes.PacketType var5;
         while(var4.hasNext()) {
            var5 = (PacketTypes.PacketType)var4.next();
            if (var5.incomePackets > 0) {
               var10000 = DebugLog.Statistic;
               var6 = var5.name();
               var10000.println(var6 + "(" + var5.getId() + ") count:" + var5.incomePackets + " bytes:" + var5.incomeBytes);
            }
         }

         DebugLog.Statistic.println("== Outcome Packets ==");
         DebugLog.Statistic.println("count packets:" + this.countOutcomePackets);
         DebugLog.Statistic.println("count bytes:" + this.countOutcomeBytes);
         DebugLog.Statistic.println("max bps:" + this.maxOutcomeBytesPerSecond);
         var4 = PacketTypes.packetTypes.values().iterator();

         while(var4.hasNext()) {
            var5 = (PacketTypes.PacketType)var4.next();
            if (var5.outcomePackets > 0) {
               var10000 = DebugLog.Statistic;
               var6 = var5.name();
               var10000.println(var6 + "(" + var5.getId() + ") count:" + var5.outcomePackets + " bytes:" + var5.outcomeBytes);
            }
         }

         DebugLog.Statistic.println("=== END STATISTICS ===");
      }

   }

   public static String getStatisticDir() {
      String var0 = ZomboidFileSystem.instance.getCacheDirSub("Statistic");
      ZomboidFileSystem.ensureFolderExists(var0);
      File var1 = new File(var0);
      return var1.getAbsolutePath();
   }

   private void removeCSVStatistics() {
      String var1 = getStatisticDir();

      File var2;
      try {
         var2 = new File(var1 + File.separator + "Statistic.csv");
         var2.delete();
      } catch (Exception var8) {
         DebugLog.Statistic.printException(var8, "Delete file failed: Statistic.csv", LogSeverity.Error);
      }

      try {
         var2 = new File(var1 + File.separator + "Connections.csv");
         var2.delete();
      } catch (Exception var7) {
         DebugLog.Statistic.printException(var7, "Delete file failed: Connections.csv", LogSeverity.Error);
      }

      try {
         var2 = new File(var1 + File.separator + "IncomePackets.csv");
         var2.delete();
      } catch (Exception var6) {
         DebugLog.Statistic.printException(var6, "Delete file failed: IncomePackets.csv", LogSeverity.Error);
      }

      try {
         var2 = new File(var1 + File.separator + "IncomeBytes.csv");
         var2.delete();
      } catch (Exception var5) {
         DebugLog.Statistic.printException(var5, "Delete file failed: IncomeBytes.csv", LogSeverity.Error);
      }

      try {
         var2 = new File(var1 + File.separator + "OutcomePackets.csv");
         var2.delete();
      } catch (Exception var4) {
         DebugLog.Statistic.printException(var4, "Delete file failed: OutcomePackets.csv", LogSeverity.Error);
      }

      try {
         var2 = new File(var1 + File.separator + "OutcomeBytes.csv");
         var2.delete();
      } catch (Exception var3) {
         DebugLog.Statistic.printException(var3, "Delete file failed: OutcomeBytes.csv", LogSeverity.Error);
      }

   }

   private void closeCSVStatistics() {
      if (this.csvStatisticFile != null) {
         this.csvStatisticFile.close();
      }

      this.csvStatisticFile = null;
      if (this.csvConnectionsFile != null) {
         this.csvConnectionsFile.close();
      }

      this.csvConnectionsFile = null;
      if (this.csvIncomePacketsFile != null) {
         this.csvIncomePacketsFile.close();
      }

      this.csvIncomePacketsFile = null;
      if (this.csvIncomeBytesFile != null) {
         this.csvIncomeBytesFile.close();
      }

      this.csvIncomeBytesFile = null;
      if (this.csvOutcomePacketsFile != null) {
         this.csvOutcomePacketsFile.close();
      }

      this.csvOutcomePacketsFile = null;
      if (this.csvOutcomeBytesFile != null) {
         this.csvOutcomeBytesFile.close();
      }

      this.csvOutcomeBytesFile = null;
   }

   private void openCSVStatistic() {
      if (doCSVStatistic) {
         String var1 = getStatisticDir();

         File var2;
         PrintStream var10000;
         String var10001;
         try {
            var2 = new File(var1 + File.separator + "Statistic.csv");
            if (var2.exists()) {
               this.csvStatisticFile = new PrintStream(new FileOutputStream(var2, true));
            } else {
               this.csvStatisticFile = new PrintStream(var2);
               var10000 = this.csvStatisticFile;
               var10001 = this.Main.PrintTitle("MainThread");
               var10000.println("lastReport; minUpdatePeriod; maxUpdatePeriod; avgUpdatePeriod; loadCellFromDisk; saveCellToDisk; countLoaderThreadTasksAdded; countLoaderThreadTasksProcessed; countRecalcThreadTasksAdded; countRecalcThreadTasksProcessed; countSaveUnloadedTasksAdded; countSaveLoadedTasksAdded; countSaveGameTimeTasksAdded; countQuitThreadTasksAdded; countSaveThreadTasksProcessed; countServerMapToLoadAdded; countServerMapToLoadCanceled; countServerMapLoadedCellsAdded; countServerMapLoadedCellsCanceled; countServerMapLoaded2Added; countServerMapLoaded2Canceled; countServerChunkThreadSaveNow; " + var10001 + this.ServerLOS.PrintTitle("ServerLOS") + this.LoaderThread.PrintTitle("LoaderThread") + this.RecalcAllThread.PrintTitle("RecalcAllThread") + this.SaveThread.PrintTitle("SaveThread") + this.PolyPathThread.PrintTitle("PolyPathThread") + this.WorldReuser.PrintTitle("WorldReuser") + this.PlayerDownloadServer.PrintTitle("WorldReuser") + this.MapCollisionThread.PrintTitle("MapCollisionThread") + this.ChunkChecksum.PrintTitle("ChunkChecksum") + this.Bullet.PrintTitle("Bullet") + this.AnimationPlayerUpdate.PrintTitle("AnimationPlayerUpdate") + this.ServerMapPreupdate.PrintTitle("ServerMapPreupdate") + this.ServerMapPostupdate.PrintTitle("ServerMapPostupdate") + this.IngameStateUpdate.PrintTitle("IngameStateUpdate") + "totalMemory; freeMemory; countConnections; packetLength; countIncomePackets; countIncomeBytes; maxIncomeBytesPerSecound; countOutcomePackets; countOutcomeBytes; maxOutcomeBytesPerSecound");
            }
         } catch (FileNotFoundException var10) {
            DebugLog.Statistic.printException(var10, "Open file failed: Statistic.csv", LogSeverity.Error);
            if (this.csvStatisticFile != null) {
               this.csvStatisticFile.close();
            }

            this.csvStatisticFile = null;
         }

         try {
            var2 = new File(var1 + File.separator + "Connections.csv");
            if (var2.exists()) {
               this.csvConnectionsFile = new PrintStream(new FileOutputStream(var2, true));
            } else {
               this.csvConnectionsFile = new PrintStream(var2);
               this.csvConnectionsFile.print("ip; ");
               this.csvConnectionsFile.print("username; ");
               this.csvConnectionsFile.print("accessLevel; ");
               this.csvConnectionsFile.print("players.length; ");
               this.csvConnectionsFile.print("ping; ");
               this.csvConnectionsFile.print("pingAVG; ");
               this.csvConnectionsFile.print("remotePlayersCount; ");
               this.csvConnectionsFile.print("remotePlayersDesyncAVG; ");
               this.csvConnectionsFile.print("remotePlayersDesyncMax; ");
               this.csvConnectionsFile.print("remotePlayersTeleports; ");
               this.csvConnectionsFile.print("zombiesCount; ");
               this.csvConnectionsFile.print("zombiesLocalOwnership; ");
               this.csvConnectionsFile.print("zombiesDesyncAVG; ");
               this.csvConnectionsFile.print("zombiesDesyncMax; ");
               this.csvConnectionsFile.print("zombiesTeleports; ");
               this.csvConnectionsFile.print("FPS; ");
               this.csvConnectionsFile.print("FPSMin; ");
               this.csvConnectionsFile.print("FPSAvg; ");
               this.csvConnectionsFile.print("FPSMax; ");

               for(int var3 = 0; var3 < 32; ++var3) {
                  this.csvConnectionsFile.print("FPSHistogramm[" + var3 + "]; ");
               }

               this.csvConnectionsFile.println();
            }
         } catch (FileNotFoundException var9) {
            DebugLog.Statistic.printException(var9, "Open file failed: Connections.csv", LogSeverity.Error);
            if (this.csvConnectionsFile != null) {
               this.csvConnectionsFile.close();
            }

            this.csvConnectionsFile = null;
         }

         PacketTypes.PacketType var4;
         Iterator var11;
         try {
            var2 = new File(var1 + File.separator + "IncomePackets.csv");
            if (var2.exists()) {
               this.csvIncomePacketsFile = new PrintStream(new FileOutputStream(var2, true));
            } else {
               this.csvIncomePacketsFile = new PrintStream(var2);
               var11 = PacketTypes.packetTypes.values().iterator();

               while(var11.hasNext()) {
                  var4 = (PacketTypes.PacketType)var11.next();
                  var10000 = this.csvIncomePacketsFile;
                  var10001 = var4.name();
                  var10000.print(var10001 + "(" + var4.getId() + "); ");
               }

               this.csvIncomePacketsFile.println();
            }
         } catch (FileNotFoundException var8) {
            DebugLog.Statistic.printException(var8, "Open file failed: IncomePackets.csv", LogSeverity.Error);
            if (this.csvIncomePacketsFile != null) {
               this.csvIncomePacketsFile.close();
            }

            this.csvIncomePacketsFile = null;
         }

         try {
            var2 = new File(var1 + File.separator + "IncomeBytes.csv");
            if (var2.exists()) {
               this.csvIncomeBytesFile = new PrintStream(new FileOutputStream(var2, true));
            } else {
               this.csvIncomeBytesFile = new PrintStream(var2);
               var11 = PacketTypes.packetTypes.values().iterator();

               while(var11.hasNext()) {
                  var4 = (PacketTypes.PacketType)var11.next();
                  var10000 = this.csvIncomeBytesFile;
                  var10001 = var4.name();
                  var10000.print(var10001 + "(" + var4.getId() + "); ");
               }

               this.csvIncomeBytesFile.println();
            }
         } catch (FileNotFoundException var7) {
            DebugLog.Statistic.printException(var7, "Open file failed: IncomeBytes.csv", LogSeverity.Error);
            if (this.csvIncomeBytesFile != null) {
               this.csvIncomeBytesFile.close();
            }

            this.csvIncomeBytesFile = null;
         }

         try {
            var2 = new File(var1 + File.separator + "OutcomePackets.csv");
            if (var2.exists()) {
               this.csvOutcomePacketsFile = new PrintStream(new FileOutputStream(var2, true));
            } else {
               this.csvOutcomePacketsFile = new PrintStream(var2);
               var11 = PacketTypes.packetTypes.values().iterator();

               while(var11.hasNext()) {
                  var4 = (PacketTypes.PacketType)var11.next();
                  var10000 = this.csvOutcomePacketsFile;
                  var10001 = var4.name();
                  var10000.print(var10001 + "(" + var4.getId() + "); ");
               }

               this.csvOutcomePacketsFile.println();
            }
         } catch (FileNotFoundException var6) {
            DebugLog.Statistic.printException(var6, "Open file failed: OutcomePackets.csv", LogSeverity.Error);
            if (this.csvOutcomePacketsFile != null) {
               this.csvOutcomePacketsFile.close();
            }

            this.csvOutcomePacketsFile = null;
         }

         try {
            var2 = new File(var1 + File.separator + "OutcomeBytes.csv");
            if (var2.exists()) {
               this.csvOutcomeBytesFile = new PrintStream(new FileOutputStream(var2, true));
            } else {
               this.csvOutcomeBytesFile = new PrintStream(var2);
               var11 = PacketTypes.packetTypes.values().iterator();

               while(var11.hasNext()) {
                  var4 = (PacketTypes.PacketType)var11.next();
                  var10000 = this.csvOutcomeBytesFile;
                  var10001 = var4.name();
                  var10000.print(var10001 + "(" + var4.getId() + "); ");
               }

               this.csvOutcomeBytesFile.println();
            }
         } catch (FileNotFoundException var5) {
            DebugLog.Statistic.printException(var5, "Open file failed: OutcomeBytes.csv", LogSeverity.Error);
            if (this.csvOutcomeBytesFile != null) {
               this.csvOutcomeBytesFile.close();
            }

            this.csvOutcomeBytesFile = null;
         }
      }

   }

   private void printCSVStatistic() {
      if (doCSVStatistic) {
         try {
            try {
               this.openCSVStatistic();
               if (this.csvStatisticFile != null) {
                  this.csvStatisticFile.print(System.currentTimeMillis() + ";");
                  this.csvStatisticFile.print(this.minUpdatePeriod + ";");
                  this.csvStatisticFile.print(this.maxUpdatePeriod + ";");
                  this.csvStatisticFile.print(this.avgUpdatePeriod + ";");
                  this.csvStatisticFile.print(this.loadCellFromDisk + ";");
                  this.csvStatisticFile.print(this.saveCellToDisk + ";");
                  this.csvStatisticFile.print(this.LoaderThreadTasks.Print());
                  this.csvStatisticFile.print(this.RecalcThreadTasks.Print());
                  this.csvStatisticFile.print(this.SaveTasks.Print());
                  this.csvStatisticFile.print(this.ServerMapToLoad.Print());
                  this.csvStatisticFile.print(this.ServerMapLoadedCells.Print());
                  this.csvStatisticFile.print(this.ServerMapLoaded2.Print());
                  this.csvStatisticFile.print(this.countServerChunkThreadSaveNow + ";");
                  this.csvStatisticFile.print(this.Main.Print());
                  this.csvStatisticFile.print(this.ServerLOS.Print());
                  this.csvStatisticFile.print(this.LoaderThread.Print());
                  this.csvStatisticFile.print(this.RecalcAllThread.Print());
                  this.csvStatisticFile.print(this.SaveThread.Print());
                  this.csvStatisticFile.print(this.PolyPathThread.Print());
                  this.csvStatisticFile.print(this.WorldReuser.Print());
                  this.csvStatisticFile.print(this.PlayerDownloadServer.Print());
                  this.csvStatisticFile.print(this.MapCollisionThread.Print());
                  this.csvStatisticFile.print(this.ChunkChecksum.Print());
                  this.csvStatisticFile.print(this.Bullet.Print());
                  this.csvStatisticFile.print(this.AnimationPlayerUpdate.Print());
                  this.csvStatisticFile.print(this.ServerMapPreupdate.Print());
                  this.csvStatisticFile.print(this.ServerMapPostupdate.Print());
                  this.csvStatisticFile.print(this.IngameStateUpdate.Print());
                  this.csvStatisticFile.print(Runtime.getRuntime().totalMemory() + ";");
                  this.csvStatisticFile.print(Runtime.getRuntime().freeMemory() + ";");
                  this.csvStatisticFile.print(GameServer.udpEngine.connections.size() + ";");
                  this.csvStatisticFile.print(this.packetLength + ";");
                  this.csvStatisticFile.print(this.countIncomePackets + ";");
                  this.csvStatisticFile.print(this.countIncomeBytes + ";");
                  this.csvStatisticFile.print(this.maxIncomeBytesPerSecond + ";");
                  this.csvStatisticFile.print(this.countOutcomePackets + ";");
                  this.csvStatisticFile.print(this.countOutcomeBytes + ";");
                  this.csvStatisticFile.println(this.maxOutcomeBytesPerSecond + ";");
                  this.csvStatisticFile.flush();
               }

               if (this.csvConnectionsFile != null) {
                  int var1;
                  UdpConnection var2;
                  for(var1 = 0; var1 < GameServer.udpEngine.connections.size(); ++var1) {
                     var2 = (UdpConnection)GameServer.udpEngine.connections.get(var1);

                     try {
                        if (var2 != null && var2.username != null && !this.csvConnections.contains(var2.username.hashCode())) {
                           this.csvConnections.add(var2.username.hashCode());
                        }
                     } catch (NullPointerException var9) {
                        var9.printStackTrace();
                        return;
                     }
                  }

                  for(var1 = 0; var1 < this.csvConnections.size(); ++var1) {
                     var2 = null;

                     int var3;
                     for(var3 = 0; var3 < GameServer.udpEngine.connections.size(); ++var3) {
                        UdpConnection var4 = (UdpConnection)GameServer.udpEngine.connections.get(var3);
                        if (var4 != null && var4.username != null && var4.username.hashCode() == (Integer)this.csvConnections.get(var1)) {
                           var2 = var4;
                        }
                     }

                     if (var2 == null) {
                        for(var3 = 0; var3 < 51; ++var3) {
                           this.csvConnectionsFile.print("; ");
                        }
                     } else {
                        this.csvConnectionsFile.print(var2.ip + "; ");
                        this.csvConnectionsFile.print(var2.username + "; ");
                        this.csvConnectionsFile.print(var2.accessLevel + "; ");
                        this.csvConnectionsFile.print(var2.players.length + "; ");
                        this.csvConnectionsFile.print(var2.statistic.diff / 2 + "; ");
                        this.csvConnectionsFile.print(var2.statistic.pingAVG + "; ");
                        this.csvConnectionsFile.print(var2.statistic.remotePlayersCount + "; ");
                        this.csvConnectionsFile.print(var2.statistic.remotePlayersDesyncAVG + "; ");
                        this.csvConnectionsFile.print(var2.statistic.remotePlayersDesyncMax + "; ");
                        this.csvConnectionsFile.print(var2.statistic.remotePlayersTeleports + "; ");
                        this.csvConnectionsFile.print(var2.statistic.zombiesCount + "; ");
                        this.csvConnectionsFile.print(var2.statistic.zombiesLocalOwnership + "; ");
                        this.csvConnectionsFile.print(var2.statistic.zombiesDesyncAVG + "; ");
                        this.csvConnectionsFile.print(var2.statistic.zombiesDesyncMax + "; ");
                        this.csvConnectionsFile.print(var2.statistic.zombiesTeleports + "; ");
                        this.csvConnectionsFile.print(var2.statistic.FPS + "; ");
                        this.csvConnectionsFile.print(var2.statistic.FPSMin + "; ");
                        this.csvConnectionsFile.print(var2.statistic.FPSAvg + "; ");
                        this.csvConnectionsFile.print(var2.statistic.FPSMax + "; ");

                        for(var3 = 0; var3 < 32; ++var3) {
                           short var10001 = var2.statistic.FPSHistogramm[var3];
                           this.csvConnectionsFile.print("" + var10001 + "; ");
                        }
                     }

                     this.csvConnectionsFile.println();
                  }

                  this.csvConnectionsFile.flush();
               }

               if (this.csvIncomePacketsFile != null && this.csvOutcomePacketsFile != null && this.csvIncomeBytesFile != null && this.csvOutcomeBytesFile != null) {
                  Iterator var12 = PacketTypes.packetTypes.values().iterator();

                  while(var12.hasNext()) {
                     PacketTypes.PacketType var13 = (PacketTypes.PacketType)var12.next();
                     this.csvIncomePacketsFile.print(var13.incomePackets + ";");
                     this.csvIncomeBytesFile.print(var13.incomeBytes + ";");
                     this.csvOutcomePacketsFile.print(var13.outcomePackets + ";");
                     this.csvOutcomeBytesFile.print(var13.outcomeBytes + ";");
                  }

                  this.csvIncomePacketsFile.println();
                  this.csvIncomeBytesFile.println();
                  this.csvOutcomePacketsFile.println();
                  this.csvOutcomeBytesFile.println();
                  this.csvIncomePacketsFile.flush();
                  this.csvIncomeBytesFile.flush();
                  this.csvOutcomePacketsFile.flush();
                  this.csvOutcomeBytesFile.flush();
                  return;
               }
            } catch (NullPointerException var10) {
               var10.printStackTrace();
            }

         } finally {
            this.closeCSVStatistics();
         }
      }
   }

   public void getStatisticTable(ByteBuffer var1) throws IOException {
      if (this.table != null) {
         this.table.save(var1);
      }

   }

   public void setStatisticTable(ByteBuffer var1) throws IOException {
      if (var1.remaining() != 0) {
         this.table = LuaManager.platform.newTable();

         try {
            this.table.load(var1, 195);
            this.table.rawset("lastReportTime", (double)System.currentTimeMillis());
         } catch (Exception var3) {
            this.table = null;
            ExceptionLogger.logException(var3);
         }

      }
   }

   public KahluaTable getStatisticTableForLua() {
      return this.table;
   }

   public void printEnabled(boolean var1) {
      doPrintStatistic = var1;
   }

   public void writeEnabled(boolean var1) {
      doCSVStatistic = var1;
      if (var1) {
         this.removeCSVStatistics();
      }

   }

   public void setPeriod(int var1) {
      Period = Math.max(var1, 0);
      if (this.table != null) {
         this.table.rawset("period", (double)Period);
      }

   }

   public static class TasksStatistic {
      protected long added = 0L;
      protected long processed = 0L;

      public TasksStatistic() {
      }

      public void Clear() {
         this.added = 0L;
         this.processed = 0L;
      }

      public String PrintTitle(String var1) {
         return var1 + "Added; " + var1 + "Processed; ";
      }

      public String Print() {
         return this.added + "; " + this.processed + "; ";
      }

      public void Added() {
         ++this.added;
      }

      public void Processed() {
         ++this.processed;
      }
   }

   public static class SaveTasksStatistic extends TasksStatistic {
      private int SaveUnloadedTasksAdded = 0;
      private int SaveLoadedTasksAdded = 0;
      private int SaveGameTimeTasksAdded = 0;
      private int QuitThreadTasksAdded = 0;

      public SaveTasksStatistic() {
      }

      public void Clear() {
         super.Clear();
         this.SaveUnloadedTasksAdded = 0;
         this.SaveLoadedTasksAdded = 0;
         this.SaveGameTimeTasksAdded = 0;
         this.QuitThreadTasksAdded = 0;
      }

      public String PrintTitle(String var1) {
         return var1 + "SaveUnloadedAdded; " + var1 + "SaveLoadedAdded; " + var1 + "SaveGameTimeAdded; " + var1 + "QuitThreadAdded; " + var1 + "Processed; ";
      }

      public String Print() {
         return this.SaveUnloadedTasksAdded + "; " + this.SaveLoadedTasksAdded + "; " + this.SaveGameTimeTasksAdded + "; " + this.QuitThreadTasksAdded + "; " + this.processed + "; ";
      }

      public void SaveUnloadedTasksAdded() {
         ++this.SaveUnloadedTasksAdded;
      }

      public void SaveLoadedTasksAdded() {
         ++this.SaveLoadedTasksAdded;
      }

      public void SaveGameTimeTasksAdded() {
         ++this.SaveGameTimeTasksAdded;
      }

      public void QuitThreadTasksAdded() {
         ++this.QuitThreadTasksAdded;
      }
   }

   public static class ServerCellStatistic {
      protected long added = 0L;
      protected long canceled = 0L;

      public ServerCellStatistic() {
      }

      public void Clear() {
         this.added = 0L;
         this.canceled = 0L;
      }

      public String PrintTitle(String var1) {
         return var1 + "Added; " + var1 + "Canceled; ";
      }

      public String Print() {
         return this.added + "; " + this.canceled + "; ";
      }

      public void Added() {
         ++this.added;
      }

      public void Added(int var1) {
         this.added += (long)var1;
      }

      public void Canceled() {
         ++this.canceled;
      }
   }

   public class MainThreadStatistic extends ThreadStatistic {
      private long timeStartSleep = 0L;

      public MainThreadStatistic() {
      }

      public void Start() {
         if (this.timeStart == 0L) {
            this.timeStart = System.currentTimeMillis();
         } else {
            long var1 = System.currentTimeMillis() - this.timeStart;
            this.timeStart = System.currentTimeMillis();
            this.timeWork += var1;
            if (this.timeMax < var1) {
               this.timeMax = var1;
            }

            ++this.timeCount;
         }
      }

      public void End() {
      }

      public void StartSleep() {
         this.timeStartSleep = System.currentTimeMillis();
      }

      public void EndSleep() {
         long var1 = System.currentTimeMillis() - this.timeStartSleep;
         this.timeSleep += var1;
         this.timeStart += var1;
      }
   }

   public static class ThreadStatistic {
      protected boolean started = false;
      protected long timeStart = 0L;
      protected long timeWork = 0L;
      protected long timeMax = 0L;
      protected long timeSleep = 0L;
      protected long timeCount = 0L;

      public ThreadStatistic() {
      }

      public void Clear() {
         this.timeWork = 0L;
         this.timeMax = 0L;
         this.timeSleep = 0L;
         this.timeCount = 0L;
      }

      public String PrintTitle(String var1) {
         return var1 + "Work; " + var1 + "Max; " + var1 + "Sleep; " + var1 + "Count;";
      }

      public String Print() {
         return this.timeWork + "; " + this.timeMax + "; " + this.timeSleep + "; " + this.timeCount + "; ";
      }

      public void Start() {
         if (this.started) {
            this.End();
         }

         if (this.timeStart != 0L) {
            this.timeSleep += System.currentTimeMillis() - this.timeStart;
         }

         this.timeStart = System.currentTimeMillis();
         ++this.timeCount;
         this.started = true;
      }

      public void End() {
         if (this.timeStart != 0L && this.started) {
            long var1 = System.currentTimeMillis() - this.timeStart;
            this.timeStart = System.currentTimeMillis();
            this.timeWork += var1;
            if (this.timeMax < var1) {
               this.timeMax = var1;
            }

            this.started = false;
         }
      }
   }

   public static class ProbeStatistic {
      protected boolean started = false;
      protected long timeStart = 0L;
      protected long timeWork = 0L;
      protected long timeMax = 0L;
      protected long timeCount = 0L;

      public ProbeStatistic() {
      }

      public void Clear() {
         this.timeWork = 0L;
         this.timeMax = 0L;
         this.timeCount = 0L;
      }

      public String PrintTitle(String var1) {
         return var1 + "Work; " + var1 + "Max; " + var1 + "Count;";
      }

      public String Print() {
         long var10000 = this.timeWork / 1000000L;
         return "" + var10000 + "; " + this.timeMax / 1000000L + "; " + this.timeCount + "; ";
      }

      public void Start() {
         this.timeStart = System.nanoTime();
         ++this.timeCount;
         this.started = true;
      }

      public void End() {
         if (this.started) {
            long var1 = System.nanoTime() - this.timeStart;
            this.timeWork += var1;
            if (this.timeMax < var1) {
               this.timeMax = var1;
            }

            this.started = false;
         }
      }
   }
}
