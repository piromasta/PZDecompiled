package zombie.iso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.physics.WorldSimulation;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.textures.ColorInfo;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.areas.IsoRoom;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.MPStatistics;
import zombie.network.PacketTypes;
import zombie.ui.TextManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleCache;
import zombie.vehicles.VehicleManager;

public final class IsoChunkMap {
   public static final int LEVELS = 64;
   public static final int GROUND_LEVEL = 32;
   public static final int TOP_LEVEL = 31;
   public static final int BOTTOM_LEVEL = -32;
   public static final int OldChunksPerWidth = 10;
   public static final int ChunksPerWidth = 8;
   public static final int ChunkSizeInSquares = 8;
   public static final HashMap<Integer, IsoChunk> SharedChunks = new HashMap();
   public static int MPWorldXA = 0;
   public static int MPWorldYA = 0;
   public static int MPWorldZA = 0;
   public static int WorldXA = 11702;
   public static int WorldYA = 6896;
   public static int WorldZA = 0;
   public static final int[] SWorldX = new int[4];
   public static final int[] SWorldY = new int[4];
   public static final ConcurrentLinkedQueue<IsoChunk> chunkStore = new ConcurrentLinkedQueue();
   public static final ReentrantLock bSettingChunk = new ReentrantLock(true);
   private static int StartChunkGridWidth = 13;
   public static int ChunkGridWidth;
   public static int ChunkWidthInTiles;
   private static final ColorInfo inf;
   private static final ArrayList<IsoChunk> saveList;
   private static final ArrayList<ArrayList<IsoFloorBloodSplat>> splatByType;
   public int PlayerID = 0;
   public boolean ignore = false;
   public int WorldX;
   public int WorldY;
   public final ArrayList<String> filenameServerRequests;
   protected IsoChunk[] chunksSwapB;
   protected IsoChunk[] chunksSwapA;
   boolean bReadBufferA;
   int XMinTiles;
   int YMinTiles;
   int XMaxTiles;
   int YMaxTiles;
   private IsoCell cell;
   private final UpdateLimit checkVehiclesFrequency;
   public int maxHeight;
   public int minHeight;
   public static final PerformanceProfileProbe ppp_update;

   public IsoChunkMap(IsoCell var1) {
      this.WorldX = chunkMapSquareToChunkMapChunkXY(WorldXA);
      this.WorldY = chunkMapSquareToChunkMapChunkXY(WorldYA);
      this.filenameServerRequests = new ArrayList();
      this.bReadBufferA = true;
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;
      this.checkVehiclesFrequency = new UpdateLimit(3000L);
      this.cell = var1;
      WorldReuserThread.instance.finished = false;
      this.chunksSwapB = new IsoChunk[ChunkGridWidth * ChunkGridWidth];
      this.chunksSwapA = new IsoChunk[ChunkGridWidth * ChunkGridWidth];
   }

   public static void CalcChunkWidth() {
      if (DebugOptions.instance.WorldChunkMap13x13.getValue()) {
         ChunkGridWidth = 13;
         ChunkWidthInTiles = ChunkGridWidth * 8;
      } else if (DebugOptions.instance.WorldChunkMap11x11.getValue()) {
         ChunkGridWidth = 11;
         ChunkWidthInTiles = ChunkGridWidth * 8;
      } else if (DebugOptions.instance.WorldChunkMap9x9.getValue()) {
         ChunkGridWidth = 9;
         ChunkWidthInTiles = ChunkGridWidth * 8;
      } else if (DebugOptions.instance.WorldChunkMap7x7.getValue()) {
         ChunkGridWidth = 7;
         ChunkWidthInTiles = ChunkGridWidth * 8;
      } else if (DebugOptions.instance.WorldChunkMap5x5.getValue()) {
         ChunkGridWidth = 5;
         ChunkWidthInTiles = ChunkGridWidth * 8;
      } else {
         float var0 = (float)Core.getInstance().getScreenWidth() / 1920.0F;
         float var1 = (float)Core.getInstance().getScreenHeight() / 1080.0F;
         float var2 = Math.max(var0, var1);
         if (var2 > 1.0F) {
            var2 = 1.0F;
         }

         ChunkGridWidth = (int)((double)((float)StartChunkGridWidth * var2) * 1.5);
         if (ChunkGridWidth / 2 * 2 == ChunkGridWidth) {
            ++ChunkGridWidth;
         }

         ChunkGridWidth = PZMath.min(ChunkGridWidth, 19);
         ChunkWidthInTiles = ChunkGridWidth * 8;
      }
   }

   public static void setWorldStartPos(int var0, int var1) {
      SWorldX[IsoPlayer.getPlayerIndex()] = chunkMapSquareToChunkMapChunkXY(var0);
      SWorldY[IsoPlayer.getPlayerIndex()] = chunkMapSquareToChunkMapChunkXY(var1);
   }

   public void Dispose() {
      WorldReuserThread.instance.finished = true;
      IsoChunk.loadGridSquare.clear();
      this.chunksSwapA = null;
      this.chunksSwapB = null;
   }

   public void setInitialPos(int var1, int var2) {
      this.WorldX = var1;
      this.WorldY = var2;
      this.XMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMinTiles = -1;
      this.YMaxTiles = -1;
   }

   public void processAllLoadGridSquare() {
      for(IsoChunk var1 = (IsoChunk)IsoChunk.loadGridSquare.poll(); var1 != null; var1 = (IsoChunk)IsoChunk.loadGridSquare.poll()) {
         bSettingChunk.lock();

         try {
            boolean var2 = false;

            for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
               IsoChunkMap var4 = IsoWorld.instance.CurrentCell.ChunkMap[var3];
               if (!var4.ignore && var4.setChunkDirect(var1, false)) {
                  var2 = true;
               }
            }

            if (!var2) {
               WorldReuserThread.instance.addReuseChunk(var1);
            } else {
               var1.doLoadGridsquare();
            }
         } finally {
            bSettingChunk.unlock();
         }
      }

   }

   public void update() {
      ppp_update.invokeAndMeasure(this, IsoChunkMap::updateInternal);
   }

   private void updateInternal() {
      int var2 = IsoChunk.loadGridSquare.size();
      if (var2 != 0) {
         var2 = 1 + var2 * 3 / ChunkGridWidth;
      }

      while(true) {
         IsoChunk var1;
         int var4;
         while(var2 > 0) {
            var1 = (IsoChunk)IsoChunk.loadGridSquare.poll();
            if (var1 != null) {
               boolean var3 = false;

               for(var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
                  IsoChunkMap var5 = IsoWorld.instance.CurrentCell.ChunkMap[var4];
                  if (!var5.ignore && var5.setChunkDirect(var1, false)) {
                     var3 = true;
                  }
               }

               if (!var3) {
                  WorldReuserThread.instance.addReuseChunk(var1);
                  --var2;
                  continue;
               }

               var1.bLoaded = true;
               bSettingChunk.lock();

               try {
                  GameProfiler var10000 = GameProfiler.getInstance();
                  Objects.requireNonNull(var1);
                  var10000.invokeAndMeasure("IsoChunk.doLoadGridsquare", var1::doLoadGridsquare);
                  if (GameClient.bClient) {
                     List var10 = VehicleCache.vehicleGet(var1.wx, var1.wy);
                     VehicleManager.instance.sendRequestGetFull(var10);
                  }
               } finally {
                  bSettingChunk.unlock();
               }

               for(var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
                  IsoPlayer var11 = IsoPlayer.players[var4];
                  if (var11 != null) {
                     var11.dirtyRecalcGridStackTime = 20.0F;
                  }
               }
            }

            --var2;
         }

         for(int var9 = 0; var9 < ChunkGridWidth; ++var9) {
            for(var4 = 0; var4 < ChunkGridWidth; ++var4) {
               var1 = this.getChunk(var4, var9);
               if (var1 != null) {
                  var1.update();
               }
            }
         }

         if (this.checkVehiclesFrequency.Check() && GameClient.bClient) {
            this.checkVehicles();
         }

         return;
      }
   }

   private void checkVehicles() {
      for(int var1 = 0; var1 < ChunkGridWidth; ++var1) {
         for(int var2 = 0; var2 < ChunkGridWidth; ++var2) {
            IsoChunk var3 = this.getChunk(var2, var1);
            if (var3 != null && var3.bLoaded) {
               List var4 = VehicleCache.vehicleGet(var3.wx, var3.wy);
               if (var4 != null && var3.vehicles.size() != var4.size()) {
                  for(int var5 = 0; var5 < var4.size(); ++var5) {
                     short var6 = ((VehicleCache)var4.get(var5)).id;
                     boolean var7 = false;

                     for(int var8 = 0; var8 < var3.vehicles.size(); ++var8) {
                        if (((BaseVehicle)var3.vehicles.get(var8)).getId() == var6) {
                           var7 = true;
                           break;
                        }
                     }

                     if (!var7 && VehicleManager.instance.getVehicleByID(var6) == null) {
                        VehicleManager.instance.sendRequestGetFull(var6, PacketTypes.PacketType.Vehicles);
                     }
                  }
               }
            }
         }
      }

   }

   public void checkIntegrity() {
      IsoWorld.instance.CurrentCell.ChunkMap[0].XMinTiles = -1;

      for(int var1 = IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMinTiles(); var1 < IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMaxTiles(); ++var1) {
         for(int var2 = IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMinTiles(); var2 < IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMaxTiles(); ++var2) {
            IsoGridSquare var3 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, 0);
            if (var3 != null && (var3.getX() != var1 || var3.getY() != var2)) {
               int var4 = var1 / 8;
               int var5 = var2 / 8;
               var4 -= IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMin();
               var5 -= IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMin();
               IsoChunk var6 = null;
               var6 = new IsoChunk(IsoWorld.instance.CurrentCell);
               var6.refs.add(IsoWorld.instance.CurrentCell.ChunkMap[0]);
               WorldStreamer.instance.addJob(var6, var1 / 8, var2 / 8, false);

               while(!var6.bLoaded) {
                  try {
                     Thread.sleep(13L);
                  } catch (InterruptedException var8) {
                     var8.printStackTrace();
                  }
               }
            }
         }
      }

   }

   public void checkIntegrityThread() {
      IsoWorld.instance.CurrentCell.ChunkMap[0].XMinTiles = -1;

      for(int var1 = IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMinTiles(); var1 < IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMaxTiles(); ++var1) {
         for(int var2 = IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMinTiles(); var2 < IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMaxTiles(); ++var2) {
            IsoGridSquare var3 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, 0);
            if (var3 != null && (var3.getX() != var1 || var3.getY() != var2)) {
               int var4 = var1 / 8;
               int var5 = var2 / 8;
               var4 -= IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMin();
               var5 -= IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMin();
               IsoChunk var6 = new IsoChunk(IsoWorld.instance.CurrentCell);
               var6.refs.add(IsoWorld.instance.CurrentCell.ChunkMap[0]);
               WorldStreamer.instance.addJobInstant(var6, var1, var2, var1 / 8, var2 / 8);
            }

            if (var3 != null) {
            }
         }
      }

   }

   public void LoadChunk(int var1, int var2, int var3, int var4) {
      IsoChunk var5 = null;
      if (SharedChunks.containsKey((var1 << 16) + var2)) {
         var5 = (IsoChunk)SharedChunks.get((var1 << 16) + var2);
         var5.setCache();
         this.setChunk(var3, var4, var5);
         var5.refs.add(this);
      } else {
         var5 = (IsoChunk)chunkStore.poll();
         if (var5 == null) {
            var5 = new IsoChunk(this.cell);
         } else {
            MPStatistics.decreaseStoredChunk();
         }

         var5.assignLoadID();
         SharedChunks.put((var1 << 16) + var2, var5);
         var5.refs.add(this);
         WorldStreamer.instance.addJob(var5, var1, var2, false);
      }

   }

   public IsoChunk LoadChunkForLater(int var1, int var2, int var3, int var4) {
      if (!IsoWorld.instance.getMetaGrid().isValidChunk(var1, var2)) {
         return null;
      } else {
         IsoChunk var5;
         if (SharedChunks.containsKey((var1 << 16) + var2)) {
            var5 = (IsoChunk)SharedChunks.get((var1 << 16) + var2);
            if (!var5.refs.contains(this)) {
               var5.refs.add(this);
               var5.checkLightingLater_OnePlayer_AllLevels(this.PlayerID);
            }

            if (!var5.bLoaded) {
               return var5;
            }

            this.setChunk(var3, var4, var5);
         } else {
            var5 = (IsoChunk)chunkStore.poll();
            if (var5 == null) {
               var5 = new IsoChunk(this.cell);
            } else {
               MPStatistics.decreaseStoredChunk();
            }

            var5.assignLoadID();
            SharedChunks.put((var1 << 16) + var2, var5);
            var5.refs.add(this);
            WorldStreamer.instance.addJob(var5, var1, var2, true);
         }

         return var5;
      }
   }

   public IsoChunk getChunkForGridSquare(int var1, int var2) {
      int var3 = this.worldSquareToChunkMapSquareX(var1);
      int var4 = this.worldSquareToChunkMapSquareY(var2);
      if (!this.isChunkMapSquareOutOfRangeXY(var3) && !this.isChunkMapSquareOutOfRangeXY(var4)) {
         int var5 = chunkMapSquareToChunkMapChunkXY(var3);
         int var6 = chunkMapSquareToChunkMapChunkXY(var4);
         return this.getChunk(var5, var6);
      } else {
         return null;
      }
   }

   public IsoChunk getChunkCurrent(int var1, int var2) {
      if (var1 >= 0 && var1 < ChunkGridWidth && var2 >= 0 && var2 < ChunkGridWidth) {
         return !this.bReadBufferA ? this.chunksSwapA[ChunkGridWidth * var2 + var1] : this.chunksSwapB[ChunkGridWidth * var2 + var1];
      } else {
         return null;
      }
   }

   public void setGridSquare(IsoGridSquare var1, int var2, int var3, int var4) {
      assert var1 == null || var1.x == var2 && var1.y == var3 && var1.z == var4;

      int var5 = this.worldSquareToChunkMapSquareX(var2);
      int var6 = this.worldSquareToChunkMapSquareY(var3);
      if (!this.isChunkMapSquareOutOfRangeXY(var5) && !this.isChunkMapSquareOutOfRangeXY(var6) && !this.isWorldSquareOutOfRangeZ(var4)) {
         int var7 = chunkMapSquareToChunkMapChunkXY(var5);
         int var8 = chunkMapSquareToChunkMapChunkXY(var6);
         IsoChunk var9 = this.getChunk(var7, var8);
         if (var9 != null) {
            var9.setSquare(this.chunkMapSquareToChunkSquareXY(var5), this.chunkMapSquareToChunkSquareXY(var6), var4, var1);
         }
      }
   }

   public IsoGridSquare getGridSquare(int var1, int var2, int var3) {
      int var4 = this.worldSquareToChunkMapSquareX(var1);
      int var5 = this.worldSquareToChunkMapSquareY(var2);
      return this.getGridSquareDirect(var4, var5, var3);
   }

   public IsoGridSquare getGridSquareDirect(int var1, int var2, int var3) {
      if (!this.isChunkMapSquareOutOfRangeXY(var1) && !this.isChunkMapSquareOutOfRangeXY(var2) && !this.isWorldSquareOutOfRangeZ(var3)) {
         int var4 = chunkMapSquareToChunkMapChunkXY(var1);
         int var5 = chunkMapSquareToChunkMapChunkXY(var2);
         IsoChunk var6 = this.getChunk(var4, var5);
         if (var6 == null) {
            return null;
         } else if (!var6.bLoaded) {
            return null;
         } else {
            int var7 = this.chunkMapSquareToChunkSquareXY(var1);
            int var8 = this.chunkMapSquareToChunkSquareXY(var2);
            return var6.getGridSquare(var7, var8, var3);
         }
      } else {
         return null;
      }
   }

   private int chunkMapSquareToChunkSquareXY(int var1) {
      return var1 % 8;
   }

   private static int chunkMapSquareToChunkMapChunkXY(int var0) {
      return var0 / 8;
   }

   private boolean isChunkMapSquareOutOfRangeXY(int var1) {
      return var1 < 0 || var1 >= this.getWidthInTiles();
   }

   private boolean isWorldSquareOutOfRangeZ(int var1) {
      return var1 < -32 || var1 > 31;
   }

   private int worldSquareToChunkMapSquareX(int var1) {
      return var1 - (this.WorldX - ChunkGridWidth / 2) * 8;
   }

   private int worldSquareToChunkMapSquareY(int var1) {
      return var1 - (this.WorldY - ChunkGridWidth / 2) * 8;
   }

   public IsoChunk getChunk(int var1, int var2) {
      if (var1 >= 0 && var1 < ChunkGridWidth && var2 >= 0 && var2 < ChunkGridWidth) {
         return this.bReadBufferA ? this.chunksSwapA[ChunkGridWidth * var2 + var1] : this.chunksSwapB[ChunkGridWidth * var2 + var1];
      } else {
         return null;
      }
   }

   public IsoChunk[] getChunks() {
      return this.bReadBufferA ? this.chunksSwapA : this.chunksSwapB;
   }

   private void setChunk(int var1, int var2, IsoChunk var3) {
      if (!this.bReadBufferA) {
         this.chunksSwapA[ChunkGridWidth * var2 + var1] = var3;
      } else {
         this.chunksSwapB[ChunkGridWidth * var2 + var1] = var3;
      }

   }

   public boolean setChunkDirect(IsoChunk var1, boolean var2) {
      long var3 = System.nanoTime();
      if (var2) {
         bSettingChunk.lock();
      }

      long var5 = System.nanoTime();
      int var7 = var1.wx - this.WorldX;
      int var8 = var1.wy - this.WorldY;
      var7 += ChunkGridWidth / 2;
      var8 += ChunkGridWidth / 2;
      if (var1.jobType == IsoChunk.JobType.Convert) {
         var7 = 0;
         var8 = 0;
      }

      if (!var1.refs.isEmpty() && var7 >= 0 && var8 >= 0 && var7 < ChunkGridWidth && var8 < ChunkGridWidth) {
         try {
            if (this.bReadBufferA) {
               this.chunksSwapA[ChunkGridWidth * var8 + var7] = var1;
            } else {
               this.chunksSwapB[ChunkGridWidth * var8 + var7] = var1;
            }

            var1.bLoaded = true;
            if (var1.jobType == IsoChunk.JobType.None) {
               var1.setCache();
               var1.updateBuildings();
            }

            double var9 = (double)(System.nanoTime() - var5) / 1000000.0;
            double var11 = (double)(System.nanoTime() - var3) / 1000000.0;
            if (LightingThread.DebugLockTime && var11 > 10.0) {
               DebugLog.log("setChunkDirect time " + var9 + "/" + var11 + " ms");
            }
         } finally {
            if (var2) {
               bSettingChunk.unlock();
            }

         }

         return true;
      } else {
         if (var1.refs.contains(this)) {
            var1.refs.remove(this);
            if (var1.refs.isEmpty()) {
               SharedChunks.remove((var1.wx << 16) + var1.wy);
            }
         }

         if (var2) {
            bSettingChunk.unlock();
         }

         return false;
      }
   }

   public void drawDebugChunkMap() {
      int var1 = 64;
      boolean var2 = false;

      for(int var3 = 0; var3 < ChunkGridWidth; ++var3) {
         int var7 = 0;

         for(int var4 = 0; var4 < ChunkGridWidth; ++var4) {
            var7 += 64;
            IsoChunk var5 = this.getChunk(var3, var4);
            if (var5 != null) {
               IsoGridSquare var6 = var5.getGridSquare(0, 0, 0);
               if (var6 == null) {
                  TextManager.instance.DrawString((double)var1, (double)var7, "wx:" + var5.wx + " wy:" + var5.wy);
               }
            }
         }

         var1 += 128;
      }

   }

   private void LoadLeft() {
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;
      this.Left();
      WorldSimulation.instance.scrollGroundLeft(this.PlayerID);
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;

      for(int var1 = -(ChunkGridWidth / 2); var1 <= ChunkGridWidth / 2; ++var1) {
         this.LoadChunkForLater(this.WorldX - ChunkGridWidth / 2, this.WorldY + var1, 0, var1 + ChunkGridWidth / 2);
      }

      this.SwapChunkBuffers();
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;
      this.UpdateCellCache();
      LightingThread.instance.scrollLeft(this.PlayerID);
   }

   public void SwapChunkBuffers() {
      for(int var1 = 0; var1 < ChunkGridWidth * ChunkGridWidth; ++var1) {
         if (this.bReadBufferA) {
            this.chunksSwapA[var1] = null;
         } else {
            this.chunksSwapB[var1] = null;
         }
      }

      this.XMinTiles = this.XMaxTiles = -1;
      this.YMinTiles = this.YMaxTiles = -1;
      this.bReadBufferA = !this.bReadBufferA;
   }

   private void setChunk(int var1, IsoChunk var2) {
      if (!this.bReadBufferA) {
         this.chunksSwapA[var1] = var2;
      } else {
         this.chunksSwapB[var1] = var2;
      }

   }

   private IsoChunk getChunk(int var1) {
      return this.bReadBufferA ? this.chunksSwapA[var1] : this.chunksSwapB[var1];
   }

   private void LoadRight() {
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;
      this.Right();
      WorldSimulation.instance.scrollGroundRight(this.PlayerID);
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;

      for(int var1 = -(ChunkGridWidth / 2); var1 <= ChunkGridWidth / 2; ++var1) {
         this.LoadChunkForLater(this.WorldX + ChunkGridWidth / 2, this.WorldY + var1, ChunkGridWidth - 1, var1 + ChunkGridWidth / 2);
      }

      this.SwapChunkBuffers();
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;
      this.UpdateCellCache();
      LightingThread.instance.scrollRight(this.PlayerID);
   }

   private void LoadUp() {
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;
      this.Up();
      WorldSimulation.instance.scrollGroundUp(this.PlayerID);
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;

      for(int var1 = -(ChunkGridWidth / 2); var1 <= ChunkGridWidth / 2; ++var1) {
         this.LoadChunkForLater(this.WorldX + var1, this.WorldY - ChunkGridWidth / 2, var1 + ChunkGridWidth / 2, 0);
      }

      this.SwapChunkBuffers();
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;
      this.UpdateCellCache();
      LightingThread.instance.scrollUp(this.PlayerID);
   }

   private void LoadDown() {
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;
      this.Down();
      WorldSimulation.instance.scrollGroundDown(this.PlayerID);
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;

      for(int var1 = -(ChunkGridWidth / 2); var1 <= ChunkGridWidth / 2; ++var1) {
         this.LoadChunkForLater(this.WorldX + var1, this.WorldY + ChunkGridWidth / 2, var1 + ChunkGridWidth / 2, ChunkGridWidth - 1);
      }

      this.SwapChunkBuffers();
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;
      this.UpdateCellCache();
      LightingThread.instance.scrollDown(this.PlayerID);
   }

   private void UpdateCellCache() {
   }

   private void Up() {
      for(int var1 = 0; var1 < ChunkGridWidth; ++var1) {
         for(int var2 = ChunkGridWidth - 1; var2 > 0; --var2) {
            IsoChunk var3 = this.getChunk(var1, var2);
            if (var3 == null && var2 == ChunkGridWidth - 1) {
               int var4 = this.WorldX - ChunkGridWidth / 2 + var1;
               int var5 = this.WorldY - ChunkGridWidth / 2 + var2;
               var3 = (IsoChunk)SharedChunks.get((var4 << 16) + var5);
               if (var3 != null) {
                  if (var3.refs.contains(this)) {
                     var3.refs.remove(this);
                     if (var3.refs.isEmpty()) {
                        SharedChunks.remove((var3.wx << 16) + var3.wy);
                     }
                  }

                  var3 = null;
               }
            }

            if (var3 != null && var2 == ChunkGridWidth - 1) {
               var3.refs.remove(this);
               if (var3.refs.isEmpty()) {
                  SharedChunks.remove((var3.wx << 16) + var3.wy);
                  var3.removeFromWorld();
                  ChunkSaveWorker.instance.Add(var3);
               }
            }

            this.setChunk(var1, var2, this.getChunk(var1, var2 - 1));
         }

         this.setChunk(var1, 0, (IsoChunk)null);
      }

      --this.WorldY;
   }

   private void Down() {
      for(int var1 = 0; var1 < ChunkGridWidth; ++var1) {
         for(int var2 = 0; var2 < ChunkGridWidth - 1; ++var2) {
            IsoChunk var3 = this.getChunk(var1, var2);
            if (var3 == null && var2 == 0) {
               int var4 = this.WorldX - ChunkGridWidth / 2 + var1;
               int var5 = this.WorldY - ChunkGridWidth / 2 + var2;
               var3 = (IsoChunk)SharedChunks.get((var4 << 16) + var5);
               if (var3 != null) {
                  if (var3.refs.contains(this)) {
                     var3.refs.remove(this);
                     if (var3.refs.isEmpty()) {
                        SharedChunks.remove((var3.wx << 16) + var3.wy);
                     }
                  }

                  var3 = null;
               }
            }

            if (var3 != null && var2 == 0) {
               var3.refs.remove(this);
               if (var3.refs.isEmpty()) {
                  SharedChunks.remove((var3.wx << 16) + var3.wy);
                  var3.removeFromWorld();
                  ChunkSaveWorker.instance.Add(var3);
               }
            }

            this.setChunk(var1, var2, this.getChunk(var1, var2 + 1));
         }

         this.setChunk(var1, ChunkGridWidth - 1, (IsoChunk)null);
      }

      ++this.WorldY;
   }

   private void Left() {
      for(int var1 = 0; var1 < ChunkGridWidth; ++var1) {
         for(int var2 = ChunkGridWidth - 1; var2 > 0; --var2) {
            IsoChunk var3 = this.getChunk(var2, var1);
            if (var3 == null && var2 == ChunkGridWidth - 1) {
               int var4 = this.WorldX - ChunkGridWidth / 2 + var2;
               int var5 = this.WorldY - ChunkGridWidth / 2 + var1;
               var3 = (IsoChunk)SharedChunks.get((var4 << 16) + var5);
               if (var3 != null) {
                  if (var3.refs.contains(this)) {
                     var3.refs.remove(this);
                     if (var3.refs.isEmpty()) {
                        SharedChunks.remove((var3.wx << 16) + var3.wy);
                     }
                  }

                  var3 = null;
               }
            }

            if (var3 != null && var2 == ChunkGridWidth - 1) {
               var3.refs.remove(this);
               if (var3.refs.isEmpty()) {
                  SharedChunks.remove((var3.wx << 16) + var3.wy);
                  var3.removeFromWorld();
                  ChunkSaveWorker.instance.Add(var3);
               }
            }

            this.setChunk(var2, var1, this.getChunk(var2 - 1, var1));
         }

         this.setChunk(0, var1, (IsoChunk)null);
      }

      --this.WorldX;
   }

   private void Right() {
      for(int var1 = 0; var1 < ChunkGridWidth; ++var1) {
         for(int var2 = 0; var2 < ChunkGridWidth - 1; ++var2) {
            IsoChunk var3 = this.getChunk(var2, var1);
            if (var3 == null && var2 == 0) {
               int var4 = this.WorldX - ChunkGridWidth / 2 + var2;
               int var5 = this.WorldY - ChunkGridWidth / 2 + var1;
               var3 = (IsoChunk)SharedChunks.get((var4 << 16) + var5);
               if (var3 != null) {
                  if (var3.refs.contains(this)) {
                     var3.refs.remove(this);
                     if (var3.refs.isEmpty()) {
                        SharedChunks.remove((var3.wx << 16) + var3.wy);
                     }
                  }

                  var3 = null;
               }
            }

            if (var3 != null && var2 == 0) {
               var3.refs.remove(this);
               if (var3.refs.isEmpty()) {
                  SharedChunks.remove((var3.wx << 16) + var3.wy);
                  var3.removeFromWorld();
                  ChunkSaveWorker.instance.Add(var3);
               }
            }

            this.setChunk(var2, var1, this.getChunk(var2 + 1, var1));
         }

         this.setChunk(ChunkGridWidth - 1, var1, (IsoChunk)null);
      }

      ++this.WorldX;
   }

   public int getWorldXMin() {
      return this.WorldX - ChunkGridWidth / 2;
   }

   public int getWorldYMin() {
      return this.WorldY - ChunkGridWidth / 2;
   }

   public void ProcessChunkPos(IsoGameCharacter var1) {
      float var2 = var1.getX();
      float var3 = var1.getY();
      int var4 = PZMath.fastfloor(var1.getZ());
      if (IsoPlayer.getInstance() != null && IsoPlayer.getInstance().getVehicle() != null) {
         IsoPlayer var5 = IsoPlayer.getInstance();
         BaseVehicle var6 = var5.getVehicle();
         float var7 = var6.getCurrentSpeedKmHour() / 5.0F;
         if (!var5.isDriving()) {
            var7 = Math.min(var7 * 2.0F, 20.0F);
         }

         var2 += (float)Math.round(var5.getForwardDirection().x * var7);
         var3 += (float)Math.round(var5.getForwardDirection().y * var7);
      }

      int var24 = PZMath.fastfloor(var2 / 8.0F);
      int var25 = PZMath.fastfloor(var3 / 8.0F);
      if (var24 != this.WorldX || var25 != this.WorldY) {
         long var26 = System.nanoTime();
         double var9 = 0.0;
         bSettingChunk.lock();
         long var11 = System.nanoTime();
         boolean var13 = false;

         try {
            if (Math.abs(var24 - this.WorldX) < ChunkGridWidth && Math.abs(var25 - this.WorldY) < ChunkGridWidth) {
               if (var24 != this.WorldX) {
                  if (var24 < this.WorldX) {
                     this.LoadLeft();
                  } else {
                     this.LoadRight();
                  }

                  var13 = true;
               } else if (var25 != this.WorldY) {
                  if (var25 < this.WorldY) {
                     this.LoadUp();
                  } else {
                     this.LoadDown();
                  }

                  var13 = true;
               }
            } else {
               if (LightingJNI.init) {
                  LightingJNI.teleport(this.PlayerID, var24 - ChunkGridWidth / 2, var25 - ChunkGridWidth / 2);
               }

               this.Unload();
               IsoPlayer var14 = IsoPlayer.players[this.PlayerID];
               var14.removeFromSquare();
               var14.square = null;
               this.WorldX = var24;
               this.WorldY = var25;
               if (!GameServer.bServer) {
                  WorldSimulation.instance.activateChunkMap(this.PlayerID);
               }

               int var15 = this.WorldX - ChunkGridWidth / 2;
               int var16 = this.WorldY - ChunkGridWidth / 2;
               int var17 = this.WorldX + ChunkGridWidth / 2;
               int var18 = this.WorldY + ChunkGridWidth / 2;

               for(int var19 = var15; var19 <= var17; ++var19) {
                  for(int var20 = var16; var20 <= var18; ++var20) {
                     this.LoadChunkForLater(var19, var20, var19 - var15, var20 - var16);
                  }
               }

               this.SwapChunkBuffers();
               this.UpdateCellCache();
               if (!IsoWorld.instance.getCell().getObjectList().contains(var14)) {
                  IsoWorld.instance.getCell().getAddList().add(var14);
               }

               var13 = true;
            }
         } finally {
            bSettingChunk.unlock();
            if (var13) {
               this.calculateZExtentsForChunkMap();
            }

         }

         var9 = (double)(System.nanoTime() - var11) / 1000000.0;
         double var27 = (double)(System.nanoTime() - var26) / 1000000.0;
         if (LightingThread.DebugLockTime && var27 > 10.0) {
            DebugLog.log("ProcessChunkPos time " + var9 + "/" + var27 + " ms");
         }

      }
   }

   public void calculateZExtentsForChunkMap() {
      int var1 = 0;
      int var2 = 0;

      for(int var3 = 0; var3 < this.chunksSwapA.length; ++var3) {
         for(int var4 = 0; var4 < this.chunksSwapA.length; ++var4) {
            IsoChunk var5 = this.getChunk(var3, var4);
            if (var5 != null) {
               var1 = Math.max(var5.maxLevel, var1);
               var2 = Math.min(var2, var5.minLevel);
            }
         }
      }

      this.maxHeight = var1;
      this.minHeight = var2;
   }

   public IsoRoom getRoom(int var1) {
      return null;
   }

   public int getWidthInTiles() {
      return ChunkWidthInTiles;
   }

   public int getWorldXMinTiles() {
      if (this.XMinTiles != -1) {
         return this.XMinTiles;
      } else {
         this.XMinTiles = this.getWorldXMin() * 8;
         return this.XMinTiles;
      }
   }

   public int getWorldYMinTiles() {
      if (this.YMinTiles != -1) {
         return this.YMinTiles;
      } else {
         this.YMinTiles = this.getWorldYMin() * 8;
         return this.YMinTiles;
      }
   }

   public int getWorldXMaxTiles() {
      if (this.XMaxTiles != -1) {
         return this.XMaxTiles;
      } else {
         this.XMaxTiles = this.getWorldXMin() * 8 + this.getWidthInTiles();
         return this.XMaxTiles;
      }
   }

   public int getWorldYMaxTiles() {
      if (this.YMaxTiles != -1) {
         return this.YMaxTiles;
      } else {
         this.YMaxTiles = this.getWorldYMin() * 8 + this.getWidthInTiles();
         return this.YMaxTiles;
      }
   }

   public void Save() {
      if (!GameServer.bServer) {
         for(int var1 = 0; var1 < ChunkGridWidth; ++var1) {
            for(int var2 = 0; var2 < ChunkGridWidth; ++var2) {
               IsoChunk var3 = this.getChunk(var1, var2);
               if (var3 != null && !saveList.contains(var3)) {
                  try {
                     var3.Save(true);
                  } catch (IOException var5) {
                     var5.printStackTrace();
                  }
               }
            }
         }

      }
   }

   public void renderBloodForChunks(int var1) {
      if (DebugOptions.instance.Terrain.RenderTiles.BloodDecals.getValue()) {
         if (!((float)var1 > IsoCamera.getCameraCharacterZ())) {
            int var2 = Core.getInstance().getOptionBloodDecals();
            if (var2 != 0) {
               float var3 = (float)GameTime.getInstance().getWorldAgeHours();
               int var4 = IsoCamera.frameState.playerIndex;

               int var5;
               for(var5 = 0; var5 < IsoFloorBloodSplat.FloorBloodTypes.length; ++var5) {
                  ((ArrayList)splatByType.get(var5)).clear();
               }

               for(var5 = 0; var5 < ChunkGridWidth; ++var5) {
                  for(int var6 = 0; var6 < ChunkGridWidth; ++var6) {
                     IsoChunk var7 = this.getChunk(var5, var6);
                     if (var7 != null) {
                        int var8;
                        IsoFloorBloodSplat var9;
                        for(var8 = 0; var8 < var7.FloorBloodSplatsFade.size(); ++var8) {
                           var9 = (IsoFloorBloodSplat)var7.FloorBloodSplatsFade.get(var8);
                           if ((var9.index < 1 || var9.index > 10 || IsoChunk.renderByIndex[var2 - 1][var9.index - 1] != 0) && PZMath.fastfloor(var9.z) == var1 && var9.Type >= 0 && var9.Type < IsoFloorBloodSplat.FloorBloodTypes.length) {
                              var9.chunk = var7;
                              ((ArrayList)splatByType.get(var9.Type)).add(var9);
                           }
                        }

                        if (!var7.FloorBloodSplats.isEmpty()) {
                           for(var8 = 0; var8 < var7.FloorBloodSplats.size(); ++var8) {
                              var9 = (IsoFloorBloodSplat)var7.FloorBloodSplats.get(var8);
                              if ((var9.index < 1 || var9.index > 10 || IsoChunk.renderByIndex[var2 - 1][var9.index - 1] != 0) && PZMath.fastfloor(var9.z) == var1 && var9.Type >= 0 && var9.Type < IsoFloorBloodSplat.FloorBloodTypes.length) {
                                 var9.chunk = var7;
                                 ((ArrayList)splatByType.get(var9.Type)).add(var9);
                              }
                           }
                        }
                     }
                  }
               }

               for(var5 = 0; var5 < splatByType.size(); ++var5) {
                  ArrayList var32 = (ArrayList)splatByType.get(var5);
                  if (!var32.isEmpty()) {
                     String var33 = IsoFloorBloodSplat.FloorBloodTypes[var5];
                     IsoSprite var34 = null;
                     if (!IsoFloorBloodSplat.SpriteMap.containsKey(var33)) {
                        IsoSprite var35 = IsoSprite.CreateSprite(IsoSpriteManager.instance);
                        var35.LoadFramesPageSimple(var33, var33, var33, var33);
                        IsoFloorBloodSplat.SpriteMap.put(var33, var35);
                        var34 = var35;
                     } else {
                        var34 = (IsoSprite)IsoFloorBloodSplat.SpriteMap.get(var33);
                     }

                     for(int var36 = 0; var36 < var32.size(); ++var36) {
                        IsoFloorBloodSplat var10 = (IsoFloorBloodSplat)var32.get(var36);
                        inf.r = 1.0F;
                        inf.g = 1.0F;
                        inf.b = 1.0F;
                        inf.a = 0.27F;
                        float var11 = (var10.x + var10.y / var10.x) * (float)(var10.Type + 1);
                        float var12 = var11 * var10.x / var10.y * (float)(var10.Type + 1) / (var11 + var10.y);
                        float var13 = var12 * var11 * var12 * var10.x / (var10.y + 2.0F);
                        var11 *= 42367.543F;
                        var12 *= 6367.123F;
                        var13 *= 23367.133F;
                        var11 %= 1000.0F;
                        var12 %= 1000.0F;
                        var13 %= 1000.0F;
                        var11 /= 1000.0F;
                        var12 /= 1000.0F;
                        var13 /= 1000.0F;
                        if (var11 > 0.25F) {
                           var11 = 0.25F;
                        }

                        ColorInfo var10000 = inf;
                        var10000.r -= var11 * 2.0F;
                        var10000 = inf;
                        var10000.g -= var11 * 2.0F;
                        var10000 = inf;
                        var10000.b -= var11 * 2.0F;
                        var10000 = inf;
                        var10000.r += var12 / 3.0F;
                        var10000 = inf;
                        var10000.g -= var13 / 3.0F;
                        var10000 = inf;
                        var10000.b -= var13 / 3.0F;
                        float var14 = var3 - var10.worldAge;
                        if (var14 >= 0.0F && var14 < 72.0F) {
                           float var15 = 1.0F - var14 / 72.0F;
                           var10000 = inf;
                           var10000.r *= 0.2F + var15 * 0.8F;
                           var10000 = inf;
                           var10000.g *= 0.2F + var15 * 0.8F;
                           var10000 = inf;
                           var10000.b *= 0.2F + var15 * 0.8F;
                           var10000 = inf;
                           var10000.a *= 0.25F + var15 * 0.75F;
                        } else {
                           var10000 = inf;
                           var10000.r *= 0.2F;
                           var10000 = inf;
                           var10000.g *= 0.2F;
                           var10000 = inf;
                           var10000.b *= 0.2F;
                           var10000 = inf;
                           var10000.a *= 0.25F;
                        }

                        if (var10.fade > 0) {
                           var10000 = inf;
                           var10000.a *= (float)var10.fade / ((float)PerformanceSettings.getLockFPS() * 5.0F);
                           if (--var10.fade == 0) {
                              var10.chunk.FloorBloodSplatsFade.remove(var10);
                           }
                        }

                        IsoGridSquare var37 = var10.chunk.getGridSquare(PZMath.fastfloor(var10.x), PZMath.fastfloor(var10.y), PZMath.fastfloor(var10.z));
                        if (var37 != null) {
                           int var16 = var37.getVertLight(0, var4);
                           int var17 = var37.getVertLight(1, var4);
                           int var18 = var37.getVertLight(2, var4);
                           int var19 = var37.getVertLight(3, var4);
                           float var20 = Color.getRedChannelFromABGR(var16);
                           float var21 = Color.getGreenChannelFromABGR(var16);
                           float var22 = Color.getBlueChannelFromABGR(var16);
                           float var23 = Color.getRedChannelFromABGR(var17);
                           float var24 = Color.getGreenChannelFromABGR(var17);
                           float var25 = Color.getBlueChannelFromABGR(var17);
                           float var26 = Color.getRedChannelFromABGR(var18);
                           float var27 = Color.getGreenChannelFromABGR(var18);
                           float var28 = Color.getBlueChannelFromABGR(var18);
                           float var29 = Color.getRedChannelFromABGR(var19);
                           float var30 = Color.getGreenChannelFromABGR(var19);
                           float var31 = Color.getBlueChannelFromABGR(var19);
                           var10000 = inf;
                           var10000.r *= (var20 + var23 + var26 + var29) / 4.0F;
                           var10000 = inf;
                           var10000.g *= (var21 + var24 + var27 + var30) / 4.0F;
                           var10000 = inf;
                           var10000.b *= (var22 + var25 + var28 + var31) / 4.0F;
                        }

                        var34.renderBloodSplat((float)(var10.chunk.wx * 8) + var10.x, (float)(var10.chunk.wy * 8) + var10.y, var10.z, inf);
                     }
                  }
               }

            }
         }
      }
   }

   public void copy(IsoChunkMap var1) {
      IsoChunkMap var2 = this;
      this.WorldX = var1.WorldX;
      this.WorldY = var1.WorldY;
      this.XMinTiles = -1;
      this.YMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMaxTiles = -1;

      for(int var3 = 0; var3 < ChunkGridWidth * ChunkGridWidth; ++var3) {
         var2.bReadBufferA = var1.bReadBufferA;
         if (var2.bReadBufferA) {
            if (var1.chunksSwapA[var3] != null) {
               var1.chunksSwapA[var3].refs.add(var2);
               var2.chunksSwapA[var3] = var1.chunksSwapA[var3];
            }
         } else if (var1.chunksSwapB[var3] != null) {
            var1.chunksSwapB[var3].refs.add(var2);
            var2.chunksSwapB[var3] = var1.chunksSwapB[var3];
         }
      }

   }

   public void Unload() {
      for(int var1 = 0; var1 < ChunkGridWidth; ++var1) {
         for(int var2 = 0; var2 < ChunkGridWidth; ++var2) {
            IsoChunk var3 = this.getChunk(var2, var1);
            if (var3 != null) {
               if (var3.refs.contains(this)) {
                  var3.refs.remove(this);
                  if (var3.refs.isEmpty()) {
                     SharedChunks.remove((var3.wx << 16) + var3.wy);
                     var3.removeFromWorld();
                     ChunkSaveWorker.instance.Add(var3);
                  }
               }

               this.chunksSwapA[var1 * ChunkGridWidth + var2] = null;
               this.chunksSwapB[var1 * ChunkGridWidth + var2] = null;
            }
         }
      }

      WorldSimulation.instance.deactivateChunkMap(this.PlayerID);
      this.XMinTiles = -1;
      this.XMaxTiles = -1;
      this.YMinTiles = -1;
      this.YMaxTiles = -1;
      if (IsoWorld.instance != null && IsoWorld.instance.CurrentCell != null) {
         IsoWorld.instance.CurrentCell.clearCacheGridSquare(this.PlayerID);
      }

   }

   static {
      ChunkGridWidth = StartChunkGridWidth;
      ChunkWidthInTiles = 8 * ChunkGridWidth;
      inf = new ColorInfo();
      saveList = new ArrayList();
      splatByType = new ArrayList();

      for(int var0 = 0; var0 < IsoFloorBloodSplat.FloorBloodTypes.length; ++var0) {
         splatByType.add(new ArrayList());
      }

      ppp_update = new PerformanceProfileProbe("IsoChunkMap.update");
   }
}
