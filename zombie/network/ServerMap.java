package zombie.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import zombie.GameTime;
import zombie.MapCollisionData;
import zombie.ReanimatedPlayers;
import zombie.VirtualZombieManager;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Roles;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.core.ImportantAreaManager;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.stash.StashSystem;
import zombie.core.utils.OnceEvery;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.globalObjects.SGlobalObjects;
import zombie.iso.InstanceTracker;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.MetaTracker;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.worldgen.WGParams;
import zombie.network.id.ObjectIDManager;
import zombie.network.packets.INetworkPacket;
import zombie.popman.NetworkZombiePacker;
import zombie.popman.ZombiePopulationManager;
import zombie.radio.ZomboidRadio;
import zombie.savefile.ServerPlayerDB;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclesDB2;
import zombie.world.moddata.GlobalModData;
import zombie.worldMap.network.WorldMapServer;

public class ServerMap {
   public boolean bUpdateLOSThisFrame = false;
   public static OnceEvery LOSTick = new OnceEvery(1.0F);
   public static OnceEvery TimeTick = new OnceEvery(600.0F);
   public static final int CellSize = 64;
   public static final int ChunksPerCellWidth = 8;
   public long LastSaved = 0L;
   private static boolean MapLoading;
   public final IsoObjectID<IsoZombie> ZombieMap = new IsoObjectID(IsoZombie.class);
   public boolean bQueuedSaveAll = false;
   public boolean bQueuedQuit = false;
   public static ServerMap instance = new ServerMap();
   public ServerCell[] cellMap;
   public ArrayList<ServerCell> LoadedCells = new ArrayList();
   public ArrayList<ServerCell> ReleventNow = new ArrayList();
   int width;
   int height;
   IsoMetaGrid grid;
   ArrayList<ServerCell> ToLoad = new ArrayList();
   static final DistToCellComparator distToCellComparator = new DistToCellComparator();
   private final ArrayList<ServerCell> tempCells = new ArrayList();
   long lastTick = 0L;

   public ServerMap() {
   }

   public short getUniqueZombieId() {
      return this.ZombieMap.allocateID();
   }

   public void SaveAll() {
      long var1 = System.nanoTime();

      for(int var3 = 0; var3 < this.LoadedCells.size(); ++var3) {
         ((ServerCell)this.LoadedCells.get(var3)).Save();
      }

      this.grid.save();
      double var10000 = (double)(System.nanoTime() - var1);
      DebugLog.log("SaveAll took " + var10000 / 1000000.0 + " ms");
   }

   public void QueueSaveAll() {
      this.bQueuedSaveAll = true;
   }

   public void QueueQuit() {
      this.bQueuedQuit = true;
   }

   public int toServerCellX(int var1) {
      var1 *= IsoCell.CellSizeInSquares;
      var1 /= 64;
      return var1;
   }

   public int toServerCellY(int var1) {
      var1 *= IsoCell.CellSizeInSquares;
      var1 /= 64;
      return var1;
   }

   public int toWorldCellX(int var1) {
      var1 *= 64;
      var1 /= IsoCell.CellSizeInSquares;
      return var1;
   }

   public int toWorldCellY(int var1) {
      var1 *= 64;
      var1 /= IsoCell.CellSizeInSquares;
      return var1;
   }

   public int getMaxX() {
      int var1 = this.toServerCellX(this.grid.maxX + 1);
      if ((this.grid.maxX + 1) * IsoCell.CellSizeInSquares % 64 == 0) {
         --var1;
      }

      return var1;
   }

   public int getMaxY() {
      int var1 = this.toServerCellY(this.grid.maxY + 1);
      if ((this.grid.maxY + 1) * IsoCell.CellSizeInSquares % 64 == 0) {
         --var1;
      }

      return var1;
   }

   public int getMinX() {
      return this.toServerCellX(this.grid.minX);
   }

   public int getMinY() {
      return this.toServerCellY(this.grid.minY);
   }

   public void init(IsoMetaGrid var1) {
      this.grid = var1;
      this.width = this.getMaxX() - this.getMinX() + 1;
      this.height = this.getMaxY() - this.getMinY() + 1;

      assert this.width * 64 >= var1.getWidth() * IsoCell.CellSizeInSquares;

      assert this.height * 64 >= var1.getHeight() * IsoCell.CellSizeInSquares;

      assert this.getMaxX() * 64 < (var1.getMaxX() + 1) * IsoCell.CellSizeInSquares;

      assert this.getMaxY() * 64 < (var1.getMaxY() + 1) * IsoCell.CellSizeInSquares;

      int var2 = this.width * this.height;
      this.cellMap = new ServerCell[var2];
      StashSystem.init();
   }

   public ServerCell getCell(int var1, int var2) {
      return this.isInvalidCell(var1, var2) ? null : this.cellMap[var2 * this.width + var1];
   }

   public boolean isInvalidCell(int var1, int var2) {
      return var1 < 0 || var2 < 0 || var1 >= this.width || var2 >= this.height;
   }

   public void loadOrKeepRelevent(int var1, int var2) {
      if (!this.isInvalidCell(var1, var2)) {
         ServerCell var3 = this.getCell(var1, var2);
         if (var3 == null) {
            var3 = new ServerCell();
            var3.WX = var1 + this.getMinX();
            var3.WY = var2 + this.getMinY();
            if (MapLoading) {
               int var10001 = var3.WX;
               DebugLog.MapLoading.debugln("Loading cell: " + var10001 + ", " + var3.WY + " (" + this.toWorldCellX(var3.WX) + ", " + this.toWorldCellX(var3.WY) + ")");
            }

            this.cellMap[var2 * this.width + var1] = var3;
            this.ToLoad.add(var3);
            MPStatistic.getInstance().ServerMapToLoad.Added();
            this.LoadedCells.add(var3);
            MPStatistic.getInstance().ServerMapLoadedCells.Added();
            this.ReleventNow.add(var3);
         } else if (!this.ReleventNow.contains(var3)) {
            this.ReleventNow.add(var3);
         }

      }
   }

   public void characterIn(IsoPlayer var1) {
      while(this.grid == null) {
         try {
            Thread.sleep(1000L);
         } catch (InterruptedException var9) {
            var9.printStackTrace();
         }
      }

      int var2 = var1.OnlineChunkGridWidth / 2 * 8;
      int var3 = PZMath.fastfloor((var1.getX() - (float)var2) / 64.0F) - this.getMinX();
      int var4 = PZMath.fastfloor((var1.getX() + (float)var2) / 64.0F) - this.getMinX();
      int var5 = PZMath.fastfloor((var1.getY() - (float)var2) / 64.0F) - this.getMinY();
      int var6 = PZMath.fastfloor((var1.getY() + (float)var2) / 64.0F) - this.getMinY();

      for(int var7 = var5; var7 <= var6; ++var7) {
         for(int var8 = var3; var8 <= var4; ++var8) {
            this.loadOrKeepRelevent(var8, var7);
         }
      }

   }

   public void characterIn(int var1, int var2, int var3) {
      while(this.grid == null) {
         try {
            Thread.sleep(1000L);
         } catch (InterruptedException var17) {
            var17.printStackTrace();
         }
      }

      int var4 = var1 * 8;
      int var5 = var2 * 8;
      var4 = (int)((float)var4 / 64.0F);
      var5 = (int)((float)var5 / 64.0F);
      var4 -= this.getMinX();
      var5 -= this.getMinY();
      int var6 = PZMath.fastfloor((float)var4);
      int var7 = PZMath.fastfloor((float)var5);
      int var8 = var1 * 8 % 64;
      int var9 = var2 * 8 % 64;
      int var10 = var3 / 2 * 8;
      int var11 = var6;
      int var12 = var7;
      int var13 = var6;
      int var14 = var7;
      if (var8 < var10) {
         var11 = var6 - 1;
      }

      if (var8 > 64 - var10) {
         var13 = var6 + 1;
      }

      if (var9 < var10) {
         var12 = var7 - 1;
      }

      if (var9 > 64 - var10) {
         var14 = var7 + 1;
      }

      for(int var15 = var12; var15 <= var14; ++var15) {
         for(int var16 = var11; var16 <= var13; ++var16) {
            this.loadOrKeepRelevent(var16, var15);
         }
      }

   }

   public void importantAreaIn(int var1, int var2) {
      while(this.grid == null) {
         try {
            Thread.sleep(1000L);
         } catch (InterruptedException var5) {
            var5.printStackTrace();
         }
      }

      int var3 = PZMath.fastfloor((float)var1);
      int var4 = PZMath.fastfloor((float)var2);
      var3 = (int)((float)var3 / 64.0F);
      var4 = (int)((float)var4 / 64.0F);
      var3 -= this.getMinX();
      var4 -= this.getMinY();
      this.loadOrKeepRelevent(var3, var4);
   }

   public void QueuedQuit() {
      this.QueuedSaveAll();
      ByteBufferWriter var1 = GameServer.udpEngine.startPacket();
      PacketTypes.PacketType.ServerQuit.doPacket(var1);
      GameServer.udpEngine.endPacketBroadcast(PacketTypes.PacketType.ServerQuit);

      try {
         Thread.sleep(5000L);
      } catch (InterruptedException var3) {
         var3.printStackTrace();
      }

      Roles.save();
      MapCollisionData.instance.stop();
      AnimalPopulationManager.getInstance().stop();
      ZombiePopulationManager.instance.stop();
      RCONServer.shutdown();
      ServerMap.ServerCell.chunkLoader.quit();
      ServerWorldDatabase.instance.close();
      ServerPlayersVehicles.instance.stop();
      ServerPlayerDB.getInstance().close();
      ObjectIDManager.getInstance().checkForSaveDataFile(true);
      ImportantAreaManager.getInstance().saveDataFile();
      VehiclesDB2.instance.Reset();
      GameServer.udpEngine.Shutdown();
      ServerGUI.shutdown();
      SteamUtils.shutdown();
   }

   public void QueuedSaveAll() {
      long var1 = System.nanoTime();
      this.SaveAll();
      ServerPlayerDB.getInstance().save();
      ServerMap.ServerCell.chunkLoader.saveLater(GameTime.instance);
      ReanimatedPlayers.instance.saveReanimatedPlayers();
      AnimalPopulationManager.getInstance().save();
      MapCollisionData.instance.save();
      SGlobalObjects.save();
      WGParams.instance.save();
      InstanceTracker.save();
      MetaTracker.save();

      try {
         ZomboidRadio.getInstance().Save();
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      try {
         GlobalModData.instance.save();
      } catch (Exception var4) {
         var4.printStackTrace();
      }

      WorldMapServer.instance.writeSavefile();
      INetworkPacket.sendToAll(PacketTypes.PacketType.StopPause, (UdpConnection)null);
      System.out.println("Saving finish");
      double var10000 = (double)(System.nanoTime() - var1);
      DebugLog.log("Saving took " + var10000 / 1000000.0 + " ms");
   }

   public void preupdate() {
      this.lastTick = System.nanoTime();
      MapLoading = DebugType.MapLoading.isEnabled();

      int var1;
      ServerCell var2;
      int var3;
      int var4;
      for(var1 = 0; var1 < this.ToLoad.size(); ++var1) {
         var2 = (ServerCell)this.ToLoad.get(var1);
         if (var2.bLoadingWasCancelled) {
            if (MapLoading) {
               DebugLog.MapLoading.debugln("MainThread: forgetting cancelled " + var2.WX + "," + var2.WY);
            }

            var3 = var2.WX - this.getMinX();
            var4 = var2.WY - this.getMinY();

            assert this.cellMap[var3 + var4 * this.width] == var2;

            this.cellMap[var3 + var4 * this.width] = null;
            this.LoadedCells.remove(var2);
            this.ReleventNow.remove(var2);
            ServerMap.ServerCell.loaded2.remove(var2);
            this.ToLoad.remove(var1--);
            MPStatistic.getInstance().ServerMapToLoad.Canceled();
         }
      }

      for(var1 = 0; var1 < this.LoadedCells.size(); ++var1) {
         var2 = (ServerCell)this.LoadedCells.get(var1);
         if (var2.bCancelLoading) {
            if (MapLoading) {
               DebugLog.MapLoading.debugln("MainThread: forgetting cancelled " + var2.WX + "," + var2.WY);
            }

            var3 = var2.WX - this.getMinX();
            var4 = var2.WY - this.getMinY();

            assert this.cellMap[var3 + var4 * this.width] == var2;

            this.cellMap[var3 + var4 * this.width] = null;
            this.LoadedCells.remove(var1--);
            this.ReleventNow.remove(var2);
            ServerMap.ServerCell.loaded2.remove(var2);
            this.ToLoad.remove(var2);
            MPStatistic.getInstance().ServerMapLoadedCells.Canceled();
         }
      }

      for(var1 = 0; var1 < ServerMap.ServerCell.loaded2.size(); ++var1) {
         var2 = (ServerCell)ServerMap.ServerCell.loaded2.get(var1);
         if (var2.bCancelLoading) {
            if (MapLoading) {
               DebugLog.MapLoading.debugln("MainThread: forgetting cancelled " + var2.WX + "," + var2.WY);
            }

            var3 = var2.WX - this.getMinX();
            var4 = var2.WY - this.getMinY();

            assert this.cellMap[var3 + var4 * this.width] == var2;

            this.cellMap[var3 + var4 * this.width] = null;
            this.LoadedCells.remove(var2);
            this.ReleventNow.remove(var2);
            ServerMap.ServerCell.loaded2.remove(var2);
            this.ToLoad.remove(var2);
            MPStatistic.getInstance().ServerMapLoaded2.Canceled();
         }
      }

      if (!this.ToLoad.isEmpty()) {
         this.tempCells.clear();

         for(var1 = 0; var1 < this.ToLoad.size(); ++var1) {
            var2 = (ServerCell)this.ToLoad.get(var1);
            if (!var2.bCancelLoading && !var2.startedLoading) {
               this.tempCells.add(var2);
            }
         }

         if (!this.tempCells.isEmpty()) {
            distToCellComparator.init();
            this.tempCells.sort(distToCellComparator);

            for(var1 = 0; var1 < this.tempCells.size(); ++var1) {
               var2 = (ServerCell)this.tempCells.get(var1);
               ServerMap.ServerCell.chunkLoader.addJob(var2);
               var2.startedLoading = true;
            }
         }

         ServerMap.ServerCell.chunkLoader.getLoaded(ServerMap.ServerCell.loaded);

         for(var1 = 0; var1 < ServerMap.ServerCell.loaded.size(); ++var1) {
            var2 = (ServerCell)ServerMap.ServerCell.loaded.get(var1);
            if (!var2.doingRecalc) {
               ServerMap.ServerCell.chunkLoader.addRecalcJob(var2);
               var2.doingRecalc = true;
            }
         }

         ServerMap.ServerCell.loaded.clear();
         ServerMap.ServerCell.chunkLoader.getRecalc(ServerMap.ServerCell.loaded2);
         if (!ServerMap.ServerCell.loaded2.isEmpty()) {
            try {
               ServerLOS.instance.suspend();

               for(var1 = 0; var1 < ServerMap.ServerCell.loaded2.size(); ++var1) {
                  var2 = (ServerCell)ServerMap.ServerCell.loaded2.get(var1);
                  if (var2.Load2()) {
                     --var1;
                     this.ToLoad.remove(var2);
                  }
               }
            } finally {
               ServerLOS.instance.resume();
            }
         }
      }

      var1 = ServerOptions.instance.SaveWorldEveryMinutes.getValue();
      if (var1 > 0) {
         long var8 = System.currentTimeMillis();
         if (var8 > this.LastSaved + (long)var1 * 60L * 1000L) {
            this.bQueuedSaveAll = true;
            this.LastSaved = var8;
         }
      }

      if (this.bQueuedSaveAll) {
         this.bQueuedSaveAll = false;
         this.QueuedSaveAll();
      }

      if (this.bQueuedQuit) {
         System.exit(0);
      }

      this.ReleventNow.clear();
      this.bUpdateLOSThisFrame = LOSTick.Check();
      if (TimeTick.Check()) {
         ServerMap.ServerCell.chunkLoader.saveLater(GameTime.instance);
      }

   }

   public void postupdate() {
      boolean var1 = false;

      try {
         for(int var2 = 0; var2 < this.LoadedCells.size(); ++var2) {
            ServerCell var3 = (ServerCell)this.LoadedCells.get(var2);
            boolean var4 = this.ReleventNow.contains(var3) || !this.outsidePlayerInfluence(var3);
            if (!var3.bLoaded) {
               if (!var4 && !var3.bCancelLoading) {
                  if (MapLoading) {
                     DebugLog.log(DebugType.MapLoading, "MainThread: cancelling " + var3.WX + "," + var3.WY + " cell.startedLoading=" + var3.startedLoading);
                  }

                  if (!var3.startedLoading) {
                     var3.bLoadingWasCancelled = true;
                  }

                  var3.bCancelLoading = true;
               }
            } else if (!var4) {
               int var5 = var3.WX - this.getMinX();
               int var6 = var3.WY - this.getMinY();
               if (!var1) {
                  ServerLOS.instance.suspend();
                  var1 = true;
               }

               this.cellMap[var6 * this.width + var5].Unload();
               this.cellMap[var6 * this.width + var5] = null;
               this.LoadedCells.remove(var3);
               --var2;
            } else {
               var3.update();
            }
         }
      } catch (Exception var10) {
         var10.printStackTrace();
      } finally {
         if (var1) {
            ServerLOS.instance.resume();
         }

      }

      NetworkZombiePacker.getInstance().postupdate();
      ServerMap.ServerCell.chunkLoader.updateSaved();
   }

   public void physicsCheck(int var1, int var2) {
      int var3 = var1 / 64;
      int var4 = var2 / 64;
      var3 -= this.getMinX();
      var4 -= this.getMinY();
      ServerCell var5 = this.getCell(var3, var4);
      if (var5 != null && var5.bLoaded) {
         var5.bPhysicsCheck = true;
      }

   }

   private boolean outsidePlayerInfluence(ServerCell var1) {
      int var2 = var1.WX * 64;
      int var3 = var1.WY * 64;
      int var4 = (var1.WX + 1) * 64;
      int var5 = (var1.WY + 1) * 64;

      for(int var6 = 0; var6 < GameServer.udpEngine.connections.size(); ++var6) {
         UdpConnection var7 = (UdpConnection)GameServer.udpEngine.connections.get(var6);
         if (var7.RelevantTo((float)var2, (float)var3)) {
            return false;
         }

         if (var7.RelevantTo((float)var4, (float)var3)) {
            return false;
         }

         if (var7.RelevantTo((float)var4, (float)var5)) {
            return false;
         }

         if (var7.RelevantTo((float)var2, (float)var5)) {
            return false;
         }
      }

      return true;
   }

   public static IsoGridSquare getGridSquare(Vector3 var0) {
      return instance.getGridSquare(PZMath.fastfloor(var0.x), PZMath.fastfloor(var0.y), PZMath.fastfloor(var0.z));
   }

   public IsoGridSquare getGridSquare(int var1, int var2, int var3) {
      if (!IsoWorld.instance.isValidSquare(var1, var2, var3)) {
         return null;
      } else {
         int var4 = var1 / 64;
         int var5 = var2 / 64;
         var4 -= this.getMinX();
         var5 -= this.getMinY();
         int var6 = var1 / 8;
         int var7 = var2 / 8;
         int var8 = var6 % 8;
         int var9 = var7 % 8;
         int var10 = var1 % 8;
         int var11 = var2 % 8;
         ServerCell var12 = this.getCell(var4, var5);
         if (var12 != null && var12.bLoaded) {
            IsoChunk var13 = var12.chunks[var8][var9];
            return var13 == null ? null : var13.getGridSquare(var10, var11, var3);
         } else {
            return null;
         }
      }
   }

   public void setGridSquare(int var1, int var2, int var3, IsoGridSquare var4) {
      int var5 = var1 / 64;
      int var6 = var2 / 64;
      var5 -= this.getMinX();
      var6 -= this.getMinY();
      int var7 = var1 / 8;
      int var8 = var2 / 8;
      int var9 = var7 % 8;
      int var10 = var8 % 8;
      int var11 = var1 % 8;
      int var12 = var2 % 8;
      ServerCell var13 = this.getCell(var5, var6);
      if (var13 != null) {
         IsoChunk var14 = var13.chunks[var9][var10];
         if (var14 != null) {
            var14.setSquare(var11, var12, var3, var4);
         }
      }
   }

   public IsoChunk getChunk(int var1, int var2) {
      if (var1 >= 0 && var2 >= 0) {
         int var3 = var1 / 8;
         int var4 = var2 / 8;
         var3 -= this.getMinX();
         var4 -= this.getMinY();
         int var5 = var1 % 8;
         int var6 = var2 % 8;
         ServerCell var7 = this.getCell(var3, var4);
         return var7 != null && var7.bLoaded ? var7.chunks[var5][var6] : null;
      } else {
         return null;
      }
   }

   public void setSoftResetChunk(IsoChunk var1) {
      int var2 = var1.wx / 8;
      int var3 = var1.wy / 8;
      var2 -= this.getMinX();
      var3 -= this.getMinY();
      if (!this.isInvalidCell(var2, var3)) {
         ServerCell var4 = this.getCell(var2, var3);
         if (var4 == null) {
            var4 = new ServerCell();
            var4.bLoaded = true;
            this.cellMap[var3 * this.width + var2] = var4;
         }

         int var5 = var1.wx % 8;
         int var6 = var1.wy % 8;
         var4.chunks[var5][var6] = var1;
      }
   }

   public void clearSoftResetChunk(IsoChunk var1) {
      int var2 = var1.wx / 8;
      int var3 = var1.wy / 8;
      var2 -= this.getMinX();
      var3 -= this.getMinY();
      ServerCell var4 = this.getCell(var2, var3);
      if (var4 != null) {
         int var5 = var1.wx % 8;
         int var6 = var1.wy % 8;
         var4.chunks[var5][var6] = null;
      }
   }

   public static class ServerCell {
      public int WX;
      public int WY;
      public boolean bLoaded = false;
      public boolean bPhysicsCheck = false;
      public final IsoChunk[][] chunks = new IsoChunk[8][8];
      private final HashSet<RoomDef> UnexploredRooms = new HashSet();
      private static final ServerChunkLoader chunkLoader = new ServerChunkLoader();
      private static final ArrayList<ServerCell> loaded = new ArrayList();
      private boolean startedLoading = false;
      public boolean bCancelLoading = false;
      public boolean bLoadingWasCancelled = false;
      private static final ArrayList<ServerCell> loaded2 = new ArrayList();
      private boolean doingRecalc = false;

      public ServerCell() {
      }

      public boolean Load2() {
         chunkLoader.getRecalc(loaded2);

         for(int var1 = 0; var1 < loaded2.size(); ++var1) {
            if (loaded2.get(var1) == this) {
               long var2 = System.nanoTime();
               this.RecalcAll2();
               loaded2.remove(var1);
               if (ServerMap.MapLoading) {
                  DebugLog.MapLoading.debugln("loaded2=" + loaded2);
               }

               float var4 = (float)(System.nanoTime() - var2) / 1000000.0F;
               if (ServerMap.MapLoading) {
                  DebugLog.MapLoading.debugln("finish loading cell " + this.WX + "," + this.WY + " ms=" + var4);
               }

               this.loadVehicles();
               return true;
            }
         }

         return false;
      }

      private void loadVehicles() {
         for(int var1 = 0; var1 < 8; ++var1) {
            for(int var2 = 0; var2 < 8; ++var2) {
               IsoChunk var3 = this.chunks[var1][var2];
               if (var3 != null && !var3.isNewChunk()) {
                  VehiclesDB2.instance.loadChunkMain(var3);
               }
            }
         }

      }

      public void RecalcAll2() {
         int var1 = this.WX * 8 * 8;
         int var2 = this.WY * 8 * 8;
         int var3 = var1 + 64;
         int var4 = var2 + 64;

         RoomDef var6;
         for(Iterator var5 = this.UnexploredRooms.iterator(); var5.hasNext(); --var6.IndoorZombies) {
            var6 = (RoomDef)var5.next();
         }

         this.UnexploredRooms.clear();
         this.bLoaded = true;
         int var16 = 2147483647;
         int var17 = -2147483648;

         int var7;
         int var8;
         for(var7 = 0; var7 < 8; ++var7) {
            for(var8 = 0; var8 < 8; ++var8) {
               IsoChunk var9 = this.getChunk(var8, var7);
               if (var9 != null) {
                  var16 = PZMath.min(var16, var9.getMinLevel());
                  var17 = PZMath.max(var17, var9.getMaxLevel());
               }
            }
         }

         IsoGridSquare var19;
         for(var7 = 1; var7 <= var17; ++var7) {
            for(var8 = -1; var8 < 65; ++var8) {
               var19 = ServerMap.instance.getGridSquare(var1 + var8, var2 - 1, var7);
               if (var19 != null && !var19.getObjects().isEmpty()) {
                  IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var19.x, var19.y, var7);
               } else if (var8 >= 0 && var8 < 64) {
                  var19 = ServerMap.instance.getGridSquare(var1 + var8, var2, var7);
                  if (var19 != null && !var19.getObjects().isEmpty()) {
                     IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var19.x, var19.y, var7);
                  }
               }

               var19 = ServerMap.instance.getGridSquare(var1 + var8, var2 + 64, var7);
               if (var19 != null && !var19.getObjects().isEmpty()) {
                  IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var19.x, var19.y, var7);
               } else if (var8 >= 0 && var8 < 64) {
                  ServerMap.instance.getGridSquare(var1 + var8, var2 + 64 - 1, var7);
                  if (var19 != null && !var19.getObjects().isEmpty()) {
                     IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var19.x, var19.y, var7);
                  }
               }
            }

            for(var8 = 0; var8 < 64; ++var8) {
               var19 = ServerMap.instance.getGridSquare(var1 - 1, var2 + var8, var7);
               if (var19 != null && !var19.getObjects().isEmpty()) {
                  IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var19.x, var19.y, var7);
               } else {
                  var19 = ServerMap.instance.getGridSquare(var1, var2 + var8, var7);
                  if (var19 != null && !var19.getObjects().isEmpty()) {
                     IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var19.x, var19.y, var7);
                  }
               }

               var19 = ServerMap.instance.getGridSquare(var1 + 64, var2 + var8, var7);
               if (var19 != null && !var19.getObjects().isEmpty()) {
                  IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var19.x, var19.y, var7);
               } else {
                  var19 = ServerMap.instance.getGridSquare(var1 + 64 - 1, var2 + var8, var7);
                  if (var19 != null && !var19.getObjects().isEmpty()) {
                     IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var19.x, var19.y, var7);
                  }
               }
            }
         }

         for(var7 = var16; var7 <= var17; ++var7) {
            for(var8 = 0; var8 < 64; ++var8) {
               var19 = ServerMap.instance.getGridSquare(var1 + var8, var2, var7);
               if (var19 != null) {
                  var19.RecalcAllWithNeighbours(true);
               }

               var19 = ServerMap.instance.getGridSquare(var1 + var8, var4 - 1, var7);
               if (var19 != null) {
                  var19.RecalcAllWithNeighbours(true);
               }
            }

            for(var8 = 0; var8 < 64; ++var8) {
               var19 = ServerMap.instance.getGridSquare(var1, var2 + var8, var7);
               if (var19 != null) {
                  var19.RecalcAllWithNeighbours(true);
               }

               var19 = ServerMap.instance.getGridSquare(var3 - 1, var2 + var8, var7);
               if (var19 != null) {
                  var19.RecalcAllWithNeighbours(true);
               }
            }
         }

         byte var18 = 64;

         int var21;
         for(var8 = 0; var8 < 8; ++var8) {
            for(var21 = 0; var21 < 8; ++var21) {
               IsoChunk var10 = this.chunks[var8][var21];
               if (var10 != null) {
                  var10.bLoaded = true;

                  for(int var11 = 0; var11 < var18; ++var11) {
                     for(int var12 = var10.minLevel; var12 <= var10.maxLevel; ++var12) {
                        int var13 = var10.squaresIndexOfLevel(var12);
                        IsoGridSquare var14 = var10.squares[var13][var11];
                        if (var14 != null) {
                           if (var14.getRoom() != null && !var14.getRoom().def.bExplored) {
                              this.UnexploredRooms.add(var14.getRoom().def);
                           }

                           var14.propertiesDirty = true;
                        }
                     }
                  }
               }
            }
         }

         for(var8 = 0; var8 < 8; ++var8) {
            for(var21 = 0; var21 < 8; ++var21) {
               if (this.chunks[var8][var21] != null) {
                  this.chunks[var8][var21].doLoadGridsquare();
               }
            }
         }

         Iterator var20 = this.UnexploredRooms.iterator();

         while(var20.hasNext()) {
            RoomDef var22 = (RoomDef)var20.next();
            ++var22.IndoorZombies;
            if (var22.IndoorZombies == 1) {
               try {
                  VirtualZombieManager.instance.tryAddIndoorZombies(var22, false);
               } catch (Exception var15) {
                  var15.printStackTrace();
               }
            }
         }

         this.bLoaded = true;
      }

      public void Unload() {
         if (this.bLoaded) {
            if (ServerMap.MapLoading) {
               int var10001 = this.WX;
               DebugLog.MapLoading.debugln("Unloading cell: " + var10001 + ", " + this.WY + " (" + ServerMap.instance.toWorldCellX(this.WX) + ", " + ServerMap.instance.toWorldCellX(this.WY) + ")");
            }

            for(int var1 = 0; var1 < 8; ++var1) {
               for(int var2 = 0; var2 < 8; ++var2) {
                  IsoChunk var3 = this.chunks[var1][var2];
                  if (var3 != null) {
                     var3.removeFromWorld();
                     var3.m_loadVehiclesObject = null;

                     for(int var4 = 0; var4 < var3.vehicles.size(); ++var4) {
                        BaseVehicle var5 = (BaseVehicle)var3.vehicles.get(var4);
                        VehiclesDB2.instance.updateVehicle(var5);
                     }

                     chunkLoader.addSaveUnloadedJob(var3);
                     this.chunks[var1][var2] = null;
                  }
               }
            }

            RoomDef var7;
            for(Iterator var6 = this.UnexploredRooms.iterator(); var6.hasNext(); --var7.IndoorZombies) {
               var7 = (RoomDef)var6.next();
            }

         }
      }

      public void Save() {
         if (this.bLoaded) {
            for(int var1 = 0; var1 < 8; ++var1) {
               for(int var2 = 0; var2 < 8; ++var2) {
                  IsoChunk var3 = this.chunks[var1][var2];
                  if (var3 != null) {
                     try {
                        chunkLoader.addSaveLoadedJob(var3);

                        for(int var4 = 0; var4 < var3.vehicles.size(); ++var4) {
                           BaseVehicle var5 = (BaseVehicle)var3.vehicles.get(var4);
                           VehiclesDB2.instance.updateVehicle(var5);
                        }
                     } catch (Exception var6) {
                        var6.printStackTrace();
                        LoggerManager.getLogger("map").write(var6);
                     }
                  }
               }
            }

            chunkLoader.updateSaved();
         }
      }

      public void update() {
         for(int var1 = 0; var1 < 8; ++var1) {
            for(int var2 = 0; var2 < 8; ++var2) {
               IsoChunk var3 = this.chunks[var1][var2];
               if (var3 != null) {
                  var3.update();
               }
            }
         }

         this.bPhysicsCheck = false;
      }

      public IsoChunk getChunk(int var1, int var2) {
         if (var1 >= 0 && var1 < 8 && var2 >= 0 && var2 < 8) {
            IsoChunk var3 = this.chunks[var1][var2];
            if (var3 != null) {
               return var3;
            }
         }

         return null;
      }

      public int getWX() {
         return this.WX;
      }

      public int getWY() {
         return this.WY;
      }
   }

   private static class DistToCellComparator implements Comparator<ServerCell> {
      private final Vector2[] pos = new Vector2[1024];
      private int posCount;

      public DistToCellComparator() {
         for(int var1 = 0; var1 < this.pos.length; ++var1) {
            this.pos[var1] = new Vector2();
         }

      }

      public void init() {
         this.posCount = 0;

         for(int var1 = 0; var1 < GameServer.udpEngine.connections.size(); ++var1) {
            UdpConnection var2 = (UdpConnection)GameServer.udpEngine.connections.get(var1);
            if (var2.isFullyConnected()) {
               for(int var3 = 0; var3 < 4; ++var3) {
                  if (var2.players[var3] != null) {
                     this.pos[this.posCount].set(var2.players[var3].getX(), var2.players[var3].getY());
                     ++this.posCount;
                  }
               }
            }
         }

      }

      public int compare(ServerCell var1, ServerCell var2) {
         float var3 = 3.4028235E38F;
         float var4 = 3.4028235E38F;

         for(int var5 = 0; var5 < this.posCount; ++var5) {
            float var6 = this.pos[var5].x;
            float var7 = this.pos[var5].y;
            var3 = Math.min(var3, this.distToCell(var6, var7, var1));
            var4 = Math.min(var4, this.distToCell(var6, var7, var2));
         }

         return Float.compare(var3, var4);
      }

      private float distToCell(float var1, float var2, ServerCell var3) {
         int var4 = var3.WX * 64;
         int var5 = var3.WY * 64;
         int var6 = var4 + 64;
         int var7 = var5 + 64;
         float var8 = var1;
         float var9 = var2;
         if (var1 < (float)var4) {
            var8 = (float)var4;
         } else if (var1 > (float)var6) {
            var8 = (float)var6;
         }

         if (var2 < (float)var5) {
            var9 = (float)var5;
         } else if (var2 > (float)var7) {
            var9 = (float)var7;
         }

         return IsoUtils.DistanceToSquared(var1, var2, var8, var9);
      }
   }
}
