package zombie.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import zombie.GameTime;
import zombie.MapCollisionData;
import zombie.ReanimatedPlayers;
import zombie.VirtualZombieManager;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Rand;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.stash.StashSystem;
import zombie.core.utils.OnceEvery;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.globalObjects.SGlobalObjects;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.popman.NetworkZombiePacker;
import zombie.popman.ZombiePopulationManager;
import zombie.radio.ZomboidRadio;
import zombie.savefile.ServerPlayerDB;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclesDB2;
import zombie.world.moddata.GlobalModData;

public class ServerMap {
   public boolean bUpdateLOSThisFrame = false;
   public static OnceEvery LOSTick = new OnceEvery(1.0F);
   public static OnceEvery TimeTick = new OnceEvery(600.0F);
   public static final int CellSize = 50;
   public static final int ChunksPerCellWidth = 5;
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
   Vector2 start;

   public ServerMap() {
   }

   public short getUniqueZombieId() {
      return this.ZombieMap.allocateID();
   }

   public Vector3 getStartLocation(ServerWorldDatabase.LogonResult var1) {
      short var2 = 9412;
      short var3 = 10745;
      byte var4 = 0;
      return new Vector3((float)var3, (float)var2, (float)var4);
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
      DebugLog.Multiplayer.printStackTrace();
      this.bQueuedSaveAll = true;
      this.bQueuedQuit = true;
   }

   public int toServerCellX(int var1) {
      var1 *= 300;
      var1 /= 50;
      return var1;
   }

   public int toServerCellY(int var1) {
      var1 *= 300;
      var1 /= 50;
      return var1;
   }

   public int toWorldCellX(int var1) {
      var1 *= 50;
      var1 /= 300;
      return var1;
   }

   public int toWorldCellY(int var1) {
      var1 *= 50;
      var1 /= 300;
      return var1;
   }

   public int getMaxX() {
      int var1 = this.toServerCellX(this.grid.maxX + 1);
      if ((this.grid.maxX + 1) * 300 % 50 == 0) {
         --var1;
      }

      return var1;
   }

   public int getMaxY() {
      int var1 = this.toServerCellY(this.grid.maxY + 1);
      if ((this.grid.maxY + 1) * 300 % 50 == 0) {
         --var1;
      }

      return var1;
   }

   public int getMinX() {
      int var1 = this.toServerCellX(this.grid.minX);
      return var1;
   }

   public int getMinY() {
      int var1 = this.toServerCellY(this.grid.minY);
      return var1;
   }

   public void init(IsoMetaGrid var1) {
      this.grid = var1;
      this.width = this.getMaxX() - this.getMinX() + 1;
      this.height = this.getMaxY() - this.getMinY() + 1;

      assert this.width * 50 >= var1.getWidth() * 300;

      assert this.height * 50 >= var1.getHeight() * 300;

      assert this.getMaxX() * 50 < (var1.getMaxX() + 1) * 300;

      assert this.getMaxY() * 50 < (var1.getMaxY() + 1) * 300;

      int var2 = this.width * this.height;
      this.cellMap = new ServerCell[var2];
      StashSystem.init();
   }

   public ServerCell getCell(int var1, int var2) {
      return !this.isValidCell(var1, var2) ? null : this.cellMap[var2 * this.width + var1];
   }

   public boolean isValidCell(int var1, int var2) {
      return var1 >= 0 && var2 >= 0 && var1 < this.width && var2 < this.height;
   }

   public void loadOrKeepRelevent(int var1, int var2) {
      if (this.isValidCell(var1, var2)) {
         ServerCell var3 = this.getCell(var1, var2);
         if (var3 == null) {
            var3 = new ServerCell();
            var3.WX = var1 + this.getMinX();
            var3.WY = var2 + this.getMinY();
            if (MapLoading) {
               int var10001 = var3.WX;
               DebugLog.log(DebugType.MapLoading, "Loading cell: " + var10001 + ", " + var3.WY + " (" + this.toWorldCellX(var3.WX) + ", " + this.toWorldCellX(var3.WY) + ")");
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

      int var2 = var1.OnlineChunkGridWidth / 2 * 10;
      int var3 = (int)(Math.floor((double)((var1.getX() - (float)var2) / 50.0F)) - (double)this.getMinX());
      int var4 = (int)(Math.floor((double)((var1.getX() + (float)var2) / 50.0F)) - (double)this.getMinX());
      int var5 = (int)(Math.floor((double)((var1.getY() - (float)var2) / 50.0F)) - (double)this.getMinY());
      int var6 = (int)(Math.floor((double)((var1.getY() + (float)var2) / 50.0F)) - (double)this.getMinY());

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

      int var4 = var1 * 10;
      int var5 = var2 * 10;
      var4 = (int)((float)var4 / 50.0F);
      var5 = (int)((float)var5 / 50.0F);
      var4 -= this.getMinX();
      var5 -= this.getMinY();
      int var8 = var1 * 10 % 50;
      int var9 = var2 * 10 % 50;
      int var10 = var3 / 2 * 10;
      int var11 = var4;
      int var12 = var5;
      int var13 = var4;
      int var14 = var5;
      if (var8 < var10) {
         var11 = var4 - 1;
      }

      if (var8 > 50 - var10) {
         var13 = var4 + 1;
      }

      if (var9 < var10) {
         var12 = var5 - 1;
      }

      if (var9 > 50 - var10) {
         var14 = var5 + 1;
      }

      for(int var15 = var12; var15 <= var14; ++var15) {
         for(int var16 = var11; var16 <= var13; ++var16) {
            this.loadOrKeepRelevent(var16, var15);
         }
      }

   }

   public void loadMapChunk(int var1, int var2) {
      while(this.grid == null) {
         try {
            Thread.sleep(1000L);
         } catch (InterruptedException var5) {
            var5.printStackTrace();
         }
      }

      int var3 = (int)((float)var1 / 50.0F);
      int var4 = (int)((float)var2 / 50.0F);
      var3 -= this.getMinX();
      var4 -= this.getMinY();
      this.loadOrKeepRelevent(var3, var4);
   }

   public void preupdate() {
      long var1 = System.nanoTime();
      long var3 = var1 - this.lastTick;
      double var5 = (double)var3 * 1.0E-6;
      this.lastTick = var1;
      MapLoading = DebugType.Do(DebugType.MapLoading);

      int var7;
      ServerCell var8;
      int var9;
      int var10;
      for(var7 = 0; var7 < this.ToLoad.size(); ++var7) {
         var8 = (ServerCell)this.ToLoad.get(var7);
         if (var8.bLoadingWasCancelled) {
            if (MapLoading) {
               DebugLog.log(DebugType.MapLoading, "MainThread: forgetting cancelled " + var8.WX + "," + var8.WY);
            }

            var9 = var8.WX - this.getMinX();
            var10 = var8.WY - this.getMinY();

            assert this.cellMap[var9 + var10 * this.width] == var8;

            this.cellMap[var9 + var10 * this.width] = null;
            this.LoadedCells.remove(var8);
            this.ReleventNow.remove(var8);
            ServerMap.ServerCell.loaded2.remove(var8);
            this.ToLoad.remove(var7--);
            MPStatistic.getInstance().ServerMapToLoad.Canceled();
         }
      }

      for(var7 = 0; var7 < this.LoadedCells.size(); ++var7) {
         var8 = (ServerCell)this.LoadedCells.get(var7);
         if (var8.bCancelLoading) {
            if (MapLoading) {
               DebugLog.log(DebugType.MapLoading, "MainThread: forgetting cancelled " + var8.WX + "," + var8.WY);
            }

            var9 = var8.WX - this.getMinX();
            var10 = var8.WY - this.getMinY();

            assert this.cellMap[var9 + var10 * this.width] == var8;

            this.cellMap[var9 + var10 * this.width] = null;
            this.LoadedCells.remove(var7--);
            this.ReleventNow.remove(var8);
            ServerMap.ServerCell.loaded2.remove(var8);
            this.ToLoad.remove(var8);
            MPStatistic.getInstance().ServerMapLoadedCells.Canceled();
         }
      }

      for(var7 = 0; var7 < ServerMap.ServerCell.loaded2.size(); ++var7) {
         var8 = (ServerCell)ServerMap.ServerCell.loaded2.get(var7);
         if (var8.bCancelLoading) {
            if (MapLoading) {
               DebugLog.log(DebugType.MapLoading, "MainThread: forgetting cancelled " + var8.WX + "," + var8.WY);
            }

            var9 = var8.WX - this.getMinX();
            var10 = var8.WY - this.getMinY();

            assert this.cellMap[var9 + var10 * this.width] == var8;

            this.cellMap[var9 + var10 * this.width] = null;
            this.LoadedCells.remove(var8);
            this.ReleventNow.remove(var8);
            ServerMap.ServerCell.loaded2.remove(var8);
            this.ToLoad.remove(var8);
            MPStatistic.getInstance().ServerMapLoaded2.Canceled();
         }
      }

      if (!this.ToLoad.isEmpty()) {
         this.tempCells.clear();

         for(var7 = 0; var7 < this.ToLoad.size(); ++var7) {
            var8 = (ServerCell)this.ToLoad.get(var7);
            if (!var8.bCancelLoading && !var8.startedLoading) {
               this.tempCells.add(var8);
            }
         }

         if (!this.tempCells.isEmpty()) {
            distToCellComparator.init();
            Collections.sort(this.tempCells, distToCellComparator);

            for(var7 = 0; var7 < this.tempCells.size(); ++var7) {
               var8 = (ServerCell)this.tempCells.get(var7);
               ServerMap.ServerCell.chunkLoader.addJob(var8);
               var8.startedLoading = true;
            }
         }

         ServerMap.ServerCell.chunkLoader.getLoaded(ServerMap.ServerCell.loaded);

         for(var7 = 0; var7 < ServerMap.ServerCell.loaded.size(); ++var7) {
            var8 = (ServerCell)ServerMap.ServerCell.loaded.get(var7);
            if (!var8.doingRecalc) {
               ServerMap.ServerCell.chunkLoader.addRecalcJob(var8);
               var8.doingRecalc = true;
            }
         }

         ServerMap.ServerCell.loaded.clear();
         ServerMap.ServerCell.chunkLoader.getRecalc(ServerMap.ServerCell.loaded2);
         if (!ServerMap.ServerCell.loaded2.isEmpty()) {
            try {
               ServerLOS.instance.suspend();

               for(var7 = 0; var7 < ServerMap.ServerCell.loaded2.size(); ++var7) {
                  var8 = (ServerCell)ServerMap.ServerCell.loaded2.get(var7);
                  long var20 = System.nanoTime();
                  if (var8.Load2()) {
                     var20 = System.nanoTime();
                     --var7;
                     this.ToLoad.remove(var8);
                  }
               }
            } finally {
               ServerLOS.instance.resume();
            }
         }
      }

      var7 = ServerOptions.instance.SaveWorldEveryMinutes.getValue();
      long var21;
      if (var7 > 0) {
         var21 = System.currentTimeMillis();
         if (var21 > this.LastSaved + (long)(var7 * 60 * 1000)) {
            this.bQueuedSaveAll = true;
            this.LastSaved = var21;
         }
      }

      if (this.bQueuedSaveAll) {
         this.bQueuedSaveAll = false;
         var21 = System.nanoTime();
         this.SaveAll();
         ServerMap.ServerCell.chunkLoader.saveLater(GameTime.instance);
         ReanimatedPlayers.instance.saveReanimatedPlayers();
         MapCollisionData.instance.save();
         SGlobalObjects.save();

         try {
            ZomboidRadio.getInstance().Save();
         } catch (Exception var18) {
            var18.printStackTrace();
         }

         try {
            GlobalModData.instance.save();
         } catch (Exception var17) {
            var17.printStackTrace();
         }

         GameServer.UnPauseAllClients();
         System.out.println("Saving finish");
         double var10000 = (double)(System.nanoTime() - var21);
         DebugLog.log("Saving took " + var10000 / 1000000.0 + " ms");
      }

      if (this.bQueuedQuit) {
         ByteBufferWriter var22 = GameServer.udpEngine.startPacket();
         PacketTypes.PacketType.ServerQuit.doPacket(var22);
         GameServer.udpEngine.endPacketBroadcast(PacketTypes.PacketType.ServerQuit);

         try {
            Thread.sleep(5000L);
         } catch (InterruptedException var16) {
            var16.printStackTrace();
         }

         MapCollisionData.instance.stop();
         ZombiePopulationManager.instance.stop();
         RCONServer.shutdown();
         ServerMap.ServerCell.chunkLoader.quit();
         ServerWorldDatabase.instance.close();
         ServerPlayersVehicles.instance.stop();
         ServerPlayerDB.getInstance().close();
         VehiclesDB2.instance.Reset();
         GameServer.udpEngine.Shutdown();
         ServerGUI.shutdown();
         SteamUtils.shutdown();
         System.exit(0);
      }

      this.ReleventNow.clear();
      this.bUpdateLOSThisFrame = LOSTick.Check();
      if (TimeTick.Check()) {
         ServerMap.ServerCell.chunkLoader.saveLater(GameTime.instance);
      }

   }

   private IsoGridSquare getRandomSquareFromCell(int var1, int var2) {
      this.loadOrKeepRelevent(var1, var2);
      int var3 = var1;
      int var4 = var2;
      ServerCell var5 = this.getCell(var1, var2);
      if (var5 == null) {
         throw new RuntimeException("Cannot find a random square.");
      } else {
         var1 = (var1 + this.getMinX()) * 50;
         var2 = (var2 + this.getMinY()) * 50;
         IsoGridSquare var6 = null;
         int var7 = 100;

         do {
            var6 = this.getGridSquare(Rand.Next(var1, var1 + 50), Rand.Next(var2, var2 + 50), 0);
            --var7;
            if (var6 == null) {
               this.loadOrKeepRelevent(var3, var4);
            }
         } while(var6 == null && var7 > 0);

         return var6;
      }
   }

   public void postupdate() {
      int var1 = this.LoadedCells.size();
      boolean var2 = false;

      try {
         for(int var3 = 0; var3 < this.LoadedCells.size(); ++var3) {
            ServerCell var4 = (ServerCell)this.LoadedCells.get(var3);
            boolean var5 = this.ReleventNow.contains(var4) || !this.outsidePlayerInfluence(var4);
            if (!var4.bLoaded) {
               if (!var5 && !var4.bCancelLoading) {
                  if (MapLoading) {
                     DebugLog.log(DebugType.MapLoading, "MainThread: cancelling " + var4.WX + "," + var4.WY + " cell.startedLoading=" + var4.startedLoading);
                  }

                  if (!var4.startedLoading) {
                     var4.bLoadingWasCancelled = true;
                  }

                  var4.bCancelLoading = true;
               }
            } else if (!var5) {
               int var6 = var4.WX - this.getMinX();
               int var7 = var4.WY - this.getMinY();
               if (!var2) {
                  ServerLOS.instance.suspend();
                  var2 = true;
               }

               this.cellMap[var7 * this.width + var6].Unload();
               this.cellMap[var7 * this.width + var6] = null;
               this.LoadedCells.remove(var4);
               --var3;
            } else {
               var4.update();
            }
         }
      } catch (Exception var11) {
         var11.printStackTrace();
      } finally {
         if (var2) {
            ServerLOS.instance.resume();
         }

      }

      NetworkZombiePacker.getInstance().postupdate();
      ServerMap.ServerCell.chunkLoader.updateSaved();
   }

   public void physicsCheck(int var1, int var2) {
      int var3 = var1 / 50;
      int var4 = var2 / 50;
      var3 -= this.getMinX();
      var4 -= this.getMinY();
      ServerCell var5 = this.getCell(var3, var4);
      if (var5 != null && var5.bLoaded) {
         var5.bPhysicsCheck = true;
      }

   }

   private boolean outsidePlayerInfluence(ServerCell var1) {
      int var2 = var1.WX * 50;
      int var3 = var1.WY * 50;
      int var4 = (var1.WX + 1) * 50;
      int var5 = (var1.WY + 1) * 50;

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

   public void saveZoneInsidePlayerInfluence(short var1) {
      for(int var2 = 0; var2 < GameServer.udpEngine.connections.size(); ++var2) {
         UdpConnection var3 = (UdpConnection)GameServer.udpEngine.connections.get(var2);

         for(int var4 = 0; var4 < var3.players.length; ++var4) {
            if (var3.players[var4] != null && var3.players[var4].OnlineID == var1) {
               IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare((double)var3.players[var4].x, (double)var3.players[var4].y, (double)var3.players[var4].z);
               if (var5 != null) {
                  ServerMap.ServerCell.chunkLoader.addSaveLoadedJob(var5.chunk);
                  return;
               }
            }
         }
      }

      ServerMap.ServerCell.chunkLoader.updateSaved();
   }

   private boolean InsideThePlayerInfluence(ServerCell var1, short var2) {
      int var3 = var1.WX * 50;
      int var4 = var1.WY * 50;
      int var5 = (var1.WX + 1) * 50;
      int var6 = (var1.WY + 1) * 50;

      for(int var7 = 0; var7 < GameServer.udpEngine.connections.size(); ++var7) {
         UdpConnection var8 = (UdpConnection)GameServer.udpEngine.connections.get(var7);

         for(int var9 = 0; var9 < var8.players.length; ++var9) {
            if (var8.players[var9] != null && var8.players[var9].OnlineID == var2) {
               if (var8.RelevantToPlayerIndex(var9, (float)var3, (float)var4)) {
                  return true;
               }

               if (var8.RelevantToPlayerIndex(var9, (float)var5, (float)var4)) {
                  return true;
               }

               if (var8.RelevantToPlayerIndex(var9, (float)var5, (float)var6)) {
                  return true;
               }

               if (var8.RelevantToPlayerIndex(var9, (float)var3, (float)var6)) {
                  return true;
               }

               return false;
            }
         }
      }

      return false;
   }

   public IsoGridSquare getGridSquare(int var1, int var2, int var3) {
      if (!IsoWorld.instance.isValidSquare(var1, var2, var3)) {
         return null;
      } else {
         int var4 = var1 / 50;
         int var5 = var2 / 50;
         var4 -= this.getMinX();
         var5 -= this.getMinY();
         int var6 = var1 / 10;
         int var7 = var2 / 10;
         int var8 = var6 % 5;
         int var9 = var7 % 5;
         int var10 = var1 % 10;
         int var11 = var2 % 10;
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
      int var5 = var1 / 50;
      int var6 = var2 / 50;
      var5 -= this.getMinX();
      var6 -= this.getMinY();
      int var7 = var1 / 10;
      int var8 = var2 / 10;
      int var9 = var7 % 5;
      int var10 = var8 % 5;
      int var11 = var1 % 10;
      int var12 = var2 % 10;
      ServerCell var13 = this.getCell(var5, var6);
      if (var13 != null) {
         IsoChunk var14 = var13.chunks[var9][var10];
         if (var14 != null) {
            var14.setSquare(var11, var12, var3, var4);
         }
      }
   }

   public boolean isInLoaded(float var1, float var2) {
      int var3 = (int)var1;
      int var4 = (int)var2;
      var3 /= 50;
      var4 /= 50;
      var3 -= this.getMinX();
      var4 -= this.getMinY();
      if (this.ToLoad.contains(this.getCell(var3, var4))) {
         return false;
      } else {
         return this.getCell(var3, var4) != null;
      }
   }

   public IsoChunk getChunk(int var1, int var2) {
      if (var1 >= 0 && var2 >= 0) {
         int var3 = var1 / 5;
         int var4 = var2 / 5;
         var3 -= this.getMinX();
         var4 -= this.getMinY();
         int var5 = var1 % 5;
         int var6 = var2 % 5;
         ServerCell var7 = this.getCell(var3, var4);
         return var7 != null && var7.bLoaded ? var7.chunks[var5][var6] : null;
      } else {
         return null;
      }
   }

   public void setSoftResetChunk(IsoChunk var1) {
      int var2 = var1.wx / 5;
      int var3 = var1.wy / 5;
      var2 -= this.getMinX();
      var3 -= this.getMinY();
      if (this.isValidCell(var2, var3)) {
         ServerCell var4 = this.getCell(var2, var3);
         if (var4 == null) {
            var4 = new ServerCell();
            var4.bLoaded = true;
            this.cellMap[var3 * this.width + var2] = var4;
         }

         int var5 = var1.wx % 5;
         int var6 = var1.wy % 5;
         var4.chunks[var5][var6] = var1;
      }
   }

   public void clearSoftResetChunk(IsoChunk var1) {
      int var2 = var1.wx / 5;
      int var3 = var1.wy / 5;
      var2 -= this.getMinX();
      var3 -= this.getMinY();
      ServerCell var4 = this.getCell(var2, var3);
      if (var4 != null) {
         int var5 = var1.wx % 5;
         int var6 = var1.wy % 5;
         var4.chunks[var5][var6] = null;
      }
   }

   public static class ServerCell {
      public int WX;
      public int WY;
      public boolean bLoaded = false;
      public boolean bPhysicsCheck = false;
      public final IsoChunk[][] chunks = new IsoChunk[5][5];
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
                  DebugLog.log(DebugType.MapLoading, "loaded2=" + loaded2);
               }

               float var4 = (float)(System.nanoTime() - var2) / 1000000.0F;
               if (ServerMap.MapLoading) {
                  DebugLog.log(DebugType.MapLoading, "finish loading cell " + this.WX + "," + this.WY + " ms=" + var4);
               }

               this.loadVehicles();
               return true;
            }
         }

         return false;
      }

      private void loadVehicles() {
         for(int var1 = 0; var1 < 5; ++var1) {
            for(int var2 = 0; var2 < 5; ++var2) {
               IsoChunk var3 = this.chunks[var1][var2];
               if (var3 != null && !var3.isNewChunk()) {
                  VehiclesDB2.instance.loadChunkMain(var3);
               }
            }
         }

      }

      public void RecalcAll2() {
         int var1 = this.WX * 5 * 10;
         int var2 = this.WY * 5 * 10;
         int var3 = var1 + 50;
         int var4 = var2 + 50;

         RoomDef var6;
         for(Iterator var5 = this.UnexploredRooms.iterator(); var5.hasNext(); --var6.IndoorZombies) {
            var6 = (RoomDef)var5.next();
         }

         this.UnexploredRooms.clear();
         this.bLoaded = true;

         IsoGridSquare var7;
         int var13;
         int var14;
         for(var13 = 1; var13 < 8; ++var13) {
            for(var14 = -1; var14 < 51; ++var14) {
               var7 = ServerMap.instance.getGridSquare(var1 + var14, var2 - 1, var13);
               if (var7 != null && !var7.getObjects().isEmpty()) {
                  IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var7.x, var7.y, var13);
               } else if (var14 >= 0 && var14 < 50) {
                  var7 = ServerMap.instance.getGridSquare(var1 + var14, var2, var13);
                  if (var7 != null && !var7.getObjects().isEmpty()) {
                     IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var7.x, var7.y, var13);
                  }
               }

               var7 = ServerMap.instance.getGridSquare(var1 + var14, var2 + 50, var13);
               if (var7 != null && !var7.getObjects().isEmpty()) {
                  IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var7.x, var7.y, var13);
               } else if (var14 >= 0 && var14 < 50) {
                  ServerMap.instance.getGridSquare(var1 + var14, var2 + 50 - 1, var13);
                  if (var7 != null && !var7.getObjects().isEmpty()) {
                     IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var7.x, var7.y, var13);
                  }
               }
            }

            for(var14 = 0; var14 < 50; ++var14) {
               var7 = ServerMap.instance.getGridSquare(var1 - 1, var2 + var14, var13);
               if (var7 != null && !var7.getObjects().isEmpty()) {
                  IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var7.x, var7.y, var13);
               } else {
                  var7 = ServerMap.instance.getGridSquare(var1, var2 + var14, var13);
                  if (var7 != null && !var7.getObjects().isEmpty()) {
                     IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var7.x, var7.y, var13);
                  }
               }

               var7 = ServerMap.instance.getGridSquare(var1 + 50, var2 + var14, var13);
               if (var7 != null && !var7.getObjects().isEmpty()) {
                  IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var7.x, var7.y, var13);
               } else {
                  var7 = ServerMap.instance.getGridSquare(var1 + 50 - 1, var2 + var14, var13);
                  if (var7 != null && !var7.getObjects().isEmpty()) {
                     IsoWorld.instance.CurrentCell.EnsureSurroundNotNull(var7.x, var7.y, var13);
                  }
               }
            }
         }

         for(var13 = 0; var13 < 8; ++var13) {
            for(var14 = 0; var14 < 50; ++var14) {
               var7 = ServerMap.instance.getGridSquare(var1 + var14, var2 + 0, var13);
               if (var7 != null) {
                  var7.RecalcAllWithNeighbours(true);
               }

               var7 = ServerMap.instance.getGridSquare(var1 + var14, var4 - 1, var13);
               if (var7 != null) {
                  var7.RecalcAllWithNeighbours(true);
               }
            }

            for(var14 = 0; var14 < 50; ++var14) {
               var7 = ServerMap.instance.getGridSquare(var1 + 0, var2 + var14, var13);
               if (var7 != null) {
                  var7.RecalcAllWithNeighbours(true);
               }

               var7 = ServerMap.instance.getGridSquare(var3 - 1, var2 + var14, var13);
               if (var7 != null) {
                  var7.RecalcAllWithNeighbours(true);
               }
            }
         }

         byte var15 = 100;

         int var17;
         for(var14 = 0; var14 < 5; ++var14) {
            for(var17 = 0; var17 < 5; ++var17) {
               IsoChunk var8 = this.chunks[var14][var17];
               if (var8 != null) {
                  var8.bLoaded = true;

                  for(int var9 = 0; var9 < var15; ++var9) {
                     for(int var10 = 0; var10 <= var8.maxLevel; ++var10) {
                        IsoGridSquare var11 = var8.squares[var10][var9];
                        if (var11 != null) {
                           if (var11.getRoom() != null && !var11.getRoom().def.bExplored) {
                              this.UnexploredRooms.add(var11.getRoom().def);
                           }

                           var11.propertiesDirty = true;
                        }
                     }
                  }
               }
            }
         }

         for(var14 = 0; var14 < 5; ++var14) {
            for(var17 = 0; var17 < 5; ++var17) {
               if (this.chunks[var14][var17] != null) {
                  this.chunks[var14][var17].doLoadGridsquare();
               }
            }
         }

         Iterator var16 = this.UnexploredRooms.iterator();

         while(var16.hasNext()) {
            RoomDef var18 = (RoomDef)var16.next();
            ++var18.IndoorZombies;
            if (var18.IndoorZombies == 1) {
               try {
                  VirtualZombieManager.instance.tryAddIndoorZombies(var18, false);
               } catch (Exception var12) {
                  var12.printStackTrace();
               }
            }
         }

         this.bLoaded = true;
      }

      public void Unload() {
         if (this.bLoaded) {
            if (ServerMap.MapLoading) {
               int var10001 = this.WX;
               DebugLog.log(DebugType.MapLoading, "Unloading cell: " + var10001 + ", " + this.WY + " (" + ServerMap.instance.toWorldCellX(this.WX) + ", " + ServerMap.instance.toWorldCellX(this.WY) + ")");
            }

            for(int var1 = 0; var1 < 5; ++var1) {
               for(int var2 = 0; var2 < 5; ++var2) {
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
               if (var7.IndoorZombies == 1) {
               }
            }

         }
      }

      public void Save() {
         if (this.bLoaded) {
            for(int var1 = 0; var1 < 5; ++var1) {
               for(int var2 = 0; var2 < 5; ++var2) {
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
         for(int var1 = 0; var1 < 5; ++var1) {
            for(int var2 = 0; var2 < 5; ++var2) {
               IsoChunk var3 = this.chunks[var1][var2];
               if (var3 != null) {
                  var3.update();
               }
            }
         }

         this.bPhysicsCheck = false;
      }

      public IsoChunk getChunk(int var1, int var2) {
         if (var1 >= 0 && var1 < 5 && var2 >= 0 && var2 < 5) {
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
      private Vector2[] pos = new Vector2[1024];
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
                     this.pos[this.posCount].set(var2.players[var3].x, var2.players[var3].y);
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

         if (var3 < var4) {
            return -1;
         } else {
            return var3 > var4 ? 1 : 0;
         }
      }

      private float distToCell(float var1, float var2, ServerCell var3) {
         int var4 = var3.WX * 50;
         int var5 = var3.WY * 50;
         int var6 = var4 + 50;
         int var7 = var5 + 50;
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
