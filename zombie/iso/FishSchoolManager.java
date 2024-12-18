package zombie.iso;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class FishSchoolManager {
   private static final HashMap<Long, Integer> noiseFishPointDisabler = new HashMap();
   private static final HashMap<Long, ZoneData> zoneCache = new HashMap();
   private static final HashMap<Long, ChumData> chumPoints = new HashMap();
   private static final ArrayList<Zone> tempArrayList = new ArrayList();
   private int seed = -1;
   private int trashSeed = -1;
   private static final FishSchoolManager _instance = new FishSchoolManager();
   private ArrayList<int[]> tempFishZones = new ArrayList();
   private final HashSet<IsoChunk> doneChunks = new HashSet();

   public FishSchoolManager() {
   }

   public static FishSchoolManager getInstance() {
      return _instance;
   }

   public void generateSeed() {
      if (this.seed == -1) {
         this.seed = Rand.Next(100000);
      }

      if (this.trashSeed == -1) {
         this.trashSeed = Rand.Next(100000);
      }

   }

   public void updateSeed() {
      if (!GameClient.bClient) {
         noiseFishPointDisabler.clear();
         zoneCache.clear();
         this.seed = Rand.Next(100000);
      }

      if (GameServer.bServer) {
         GameServer.transmitFishingData(this.seed, this.trashSeed, noiseFishPointDisabler, chumPoints);
      }

   }

   public void init() {
      noiseFishPointDisabler.clear();
      zoneCache.clear();
      chumPoints.clear();
      this.trashSeed = -1;
      this.load();
      if (GameClient.bClient) {
         GameClient.sendFishingDataRequest();
      } else {
         this.generateSeed();
      }

   }

   public void update() {
      if (!GameClient.bClient) {
         GameProfiler.getInstance().invokeAndMeasure("Data", this, FishSchoolManager::updateFishingData);
      }

      if (!GameServer.bServer) {
         GameProfiler.getInstance().invokeAndMeasure("Splashes", this, FishSchoolManager::generateSplashes);
      }

   }

   public void updateFishingData() {
      int var1 = this.getCurrentGameTimeInMinutes();
      boolean var2 = false;
      Iterator var3 = noiseFishPointDisabler.entrySet().iterator();

      Map.Entry var4;
      while(var3.hasNext()) {
         var4 = (Map.Entry)var3.next();
         if (var1 > (Integer)var4.getValue()) {
            var3.remove();
            var2 = true;
         }
      }

      var3 = chumPoints.entrySet().iterator();

      while(var3.hasNext()) {
         var4 = (Map.Entry)var3.next();
         if (var1 > ((ChumData)var4.getValue()).endTime) {
            var3.remove();
            var2 = true;
         }
      }

      if (GameServer.bServer && var2) {
         GameServer.transmitFishingData(this.seed, this.trashSeed, noiseFishPointDisabler, chumPoints);
      }

      var3 = zoneCache.entrySet().iterator();

      while(var3.hasNext()) {
         var4 = (Map.Entry)var3.next();
         Zone var5 = ((ZoneData)var4.getValue()).zone;
         if (var5 != null && var1 > var5.getLastActionTimestamp() + 7200) {
            var5.setName("0");
            var5.setLastActionTimestamp(this.getCurrentGameTimeInMinutes());
            if (GameServer.bServer) {
               GameServer.sendZone(var5);
            }
         }
      }

   }

   private int getCurrentGameTimeInMinutes() {
      return (int)(GameTime.instance.getCalender().getTimeInMillis() / 60000L);
   }

   private void generateSplashes() {
      IsoCell var1 = IsoCell.getInstance();
      this.doneChunks.clear();

      for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
         IsoChunkMap var3 = var1.getChunkMap(var2);
         if (!var3.ignore) {
            for(int var4 = 0; var4 < IsoChunkMap.ChunkGridWidth; ++var4) {
               for(int var5 = 0; var5 < IsoChunkMap.ChunkGridWidth; ++var5) {
                  IsoChunk var6 = var3.getChunk(var5, var4);
                  if (var6 != null && !this.doneChunks.contains(var6)) {
                     this.doneChunks.add(var6);
                     if (0 >= var6.minLevel && 0 <= var6.maxLevel && var6.getNumberOfWaterTiles() != 0 && (!PerformanceSettings.FBORenderChunk || var6.getRenderLevels(var2).isOnScreen(0)) && (PerformanceSettings.FBORenderChunk || var6.IsOnScreen(true))) {
                        IsoGridSquare[] var7 = var6.getSquaresForLevel(0);
                        IsoGridSquare[] var8 = var7;
                        int var9 = var7.length;

                        for(int var10 = 0; var10 < var9; ++var10) {
                           IsoGridSquare var11 = var8[var10];
                           if (var11 != null && var11.getProperties().Is(IsoFlagType.water)) {
                              int var12 = var11.x;
                              int var13 = var11.y;
                              if (Core.bDebug && DebugOptions.instance.DebugDraw_FishingZones.getValue()) {
                                 this.drawDebugFishingZones(var12, var13);
                              }

                              long var14 = this.coordsToHash(var12, var13);
                              if (!noiseFishPointDisabler.containsKey(var14)) {
                                 if (this.isFishPoint(var12, var13)) {
                                    int var16 = this.getNumberOfFishInPoint(var12, var13);
                                    if (var16 > 0 && Rand.Next(Math.max(12 + (30 - var16) * 2, 1)) == 0) {
                                       this.generateSplashInRadius(var12, var13, this.getFishPointRadius(var12, var13));
                                    }
                                 }

                                 ChumData var17 = (ChumData)chumPoints.get(var14);
                                 if (var17 != null && Rand.Next(Math.max(5, var17.maxForceTime - this.getCurrentGameTimeInMinutes()) * 2 + 60) == 0) {
                                    this.generateSplashInRadius(var12, var13, 0.3F);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private boolean isNoFishZone(int var1, int var2) {
      if (this.tempFishZones.size() == 0) {
         KahluaTable var3 = (KahluaTable)LuaManager.getTableObject("Fishing.NoFishZones");
         if (var3 == null) {
            return true;
         }

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            KahluaTable var5 = (KahluaTable)var3.rawget(var4);
            if (var5 != null) {
               Double var6 = (Double)var5.rawget("x1");
               Double var7 = (Double)var5.rawget("y1");
               Double var8 = (Double)var5.rawget("x2");
               Double var9 = (Double)var5.rawget("y2");
               int[] var10 = new int[]{var6.intValue(), var7.intValue(), var8.intValue(), var9.intValue()};
               this.tempFishZones.add(var10);
            }
         }
      }

      for(int var11 = 0; var11 < this.tempFishZones.size(); ++var11) {
         int[] var12 = (int[])this.tempFishZones.get(var11);
         if (var1 >= var12[0] && var1 <= var12[2] && var2 >= var12[1] && var2 <= var12[3]) {
            return true;
         }
      }

      return false;
   }

   private boolean isFishPoint(int var1, int var2) {
      if (this.isNoFishZone(var1, var2)) {
         return false;
      } else {
         return this.procedureRandomFloat((long)var1, (long)var2, (long)this.seed) > 0.995F;
      }
   }

   private float getFishPointRadius(int var1, int var2) {
      return this.procedureRandomFloat((long)var1, (long)var2, (long)(this.seed + 6599));
   }

   private boolean isTrashPoint(int var1, int var2) {
      return this.procedureRandomFloat((long)var1, (long)var2, (long)(this.trashSeed + 9281)) > 0.99F;
   }

   private float getTrashPointRadius(int var1, int var2) {
      return this.procedureRandomFloat((long)var1, (long)var2, (long)(this.trashSeed + 8573));
   }

   private long coordsToHash(int var1, int var2) {
      long var3 = var1 >= 0 ? 2L * (long)var1 : -2L * (long)var1 - 1L;
      long var5 = var2 >= 0 ? 2L * (long)var2 : -2L * (long)var2 - 1L;
      long var7 = (var3 >= var5 ? var3 * var3 + var3 + var5 : var3 + var5 * var5) / 2L;
      return (var1 >= 0 || var2 >= 0) && (var1 < 0 || var2 < 0) ? -var7 - 1L : var7;
   }

   private float procedureRandomFloat(long var1, long var3, long var5) {
      var1 ^= var1 << 13;
      var3 ^= var3 << 13;
      var5 ^= var5 << 13;
      long var7 = var1 * 790169L + var1 * var1 * var1 * 15731L + var3 * 789221L + var3 * var3 * var3 * 16057L + var5 * 788317L + var5 * var5 * var5 * 15401L + var1 * var3 * 209123L + var3 * var5 * 209581L + var1 * var5 * 208501L + var1 * var3 * var5 * 15749L + 1376312588L;
      return (float)(((double)(var7 % 1073741824L) / 5.36870912E8 + 2.0) / 4.0);
   }

   private int getNumberOfFishInPoint(int var1, int var2) {
      int var3 = (int)((double)(this.procedureRandomFloat((long)var1, (long)var2, (long)(this.seed + 7297)) * 40.0F) * ((double)SandboxOptions.instance.FishAbundance.getValue() / 5.0));
      if (zoneCache.get(this.coordsToHash(var1, var2)) == null) {
         Zone var4 = null;
         ArrayList var5 = IsoWorld.instance.MetaGrid.getZonesAt(var1, var2, 0, tempArrayList);

         for(int var6 = 0; var6 < var5.size(); ++var6) {
            if (Objects.equals(((Zone)var5.get(var6)).type, "Fishing")) {
               var4 = (Zone)var5.get(var6);
               break;
            }
         }

         zoneCache.put(this.coordsToHash(var1, var2), new ZoneData(var4));
      }

      return var3 - ((ZoneData)zoneCache.get(this.coordsToHash(var1, var2))).getCatchedFishNum();
   }

   private void generateSplashInRadius(int var1, int var2, float var3) {
      float var4 = 0.5F * var3 + 1.0F;
      float var5 = Rand.Next((float)var1 - var4, (float)var1 + var4);
      float var6 = Rand.Next((float)var2 - var4, (float)var2 + var4);
      if (!(this.dist((float)var1, (float)var2, var5, var6) > (double)var4)) {
         IsoGridSquare var7 = IsoCell.getInstance().getGridSquare((double)var5, (double)var6, 0.0);
         if (var7 != null && var7.getProperties().Is(IsoFlagType.water)) {
            var7.startWaterSplash(false);
         }

      }
   }

   private double dist(float var1, float var2, float var3, float var4) {
      float var5 = var3 - var1;
      float var6 = var4 - var2;
      return Math.sqrt((double)(var5 * var5 + var6 * var6));
   }

   public void addSoundNoise(int var1, int var2, int var3) {
      for(int var4 = var1 - var3; var4 <= var1 + var3; ++var4) {
         for(int var5 = var2 - var3; var5 <= var2 + var3; ++var5) {
            if (this.dist((float)var1, (float)var2, (float)var4, (float)var5) <= (double)var3 && (this.isFishPoint(var4, var5) || chumPoints.containsKey(this.coordsToHash(var4, var5)))) {
               noiseFishPointDisabler.put(this.coordsToHash(var4, var5), this.getCurrentGameTimeInMinutes() + 180);
            }
         }
      }

   }

   public void addChum(int var1, int var2, int var3) {
      int var4 = this.getCurrentGameTimeInMinutes();
      chumPoints.put(this.coordsToHash(var1, var2), new ChumData(var4 + 100, var4 + 100 + var3));
   }

   public void catchFish(int var1, int var2) {
      Zone var3 = this.getOrCreateFishingZone(var1, var2);
      int var4 = Integer.parseInt(var3.getName());
      var3.setName(String.valueOf(var4 + 1));
      var3.setOriginalName(String.valueOf(var4 + 1));
      var3.setLastActionTimestamp(this.getCurrentGameTimeInMinutes());
      if (GameClient.bClient) {
         var3.sendToServer();
      }

      for(int var5 = var1 - 20; var5 <= var1 + 20; ++var5) {
         for(int var6 = var2 - 20; var6 <= var2 + 20; ++var6) {
            if (this.isFishPoint(var5, var6)) {
               zoneCache.put(this.coordsToHash(var5, var6), new ZoneData(var3));
            }
         }
      }

   }

   private Zone getOrCreateFishingZone(int var1, int var2) {
      Zone var3 = null;
      ArrayList var4 = IsoWorld.instance.MetaGrid.getZonesAt(var1, var2, 0, tempArrayList);

      for(int var5 = 0; var5 < var4.size(); ++var5) {
         if (Objects.equals(((Zone)var4.get(var5)).type, "Fishing")) {
            var3 = (Zone)var4.get(var5);
            break;
         }
      }

      if (var3 == null) {
         var3 = IsoWorld.instance.registerZone("0", "Fishing", var1 - 20, var2 - 20, 0, 40, 40);
         var3.setLastActionTimestamp(this.getCurrentGameTimeInMinutes());
      }

      return var3;
   }

   public double getFishAbundance(int var1, int var2) {
      int var3 = 0;

      for(int var4 = -6; var4 <= 6; ++var4) {
         for(int var5 = -6; var5 <= 6; ++var5) {
            if (this.isFishPoint(var1 + var4, var2 + var5) && !noiseFishPointDisabler.containsKey(this.coordsToHash(var1 + var4, var2 + var5))) {
               double var6 = this.dist((float)var1, (float)var2, (float)(var1 + var4), (float)(var2 + var5));
               double var8 = (double)(this.getFishPointRadius(var1 + var4, var2 + var5) + 2.0F);
               if (var6 <= var8) {
                  var3 += this.getNumberOfFishInPoint(var1 + var4, var2 + var5);
               } else if (var6 <= var8 + 1.5) {
                  var3 = (int)((double)var3 + (double)this.getNumberOfFishInPoint(var1 + var4, var2 + var5) * 0.5);
               }
            }

            ChumData var10 = (ChumData)chumPoints.get(this.coordsToHash(var1 + var4, var2 + var5));
            if (var10 != null && !noiseFishPointDisabler.containsKey(this.coordsToHash(var1 + var4, var2 + var5))) {
               double var7 = this.dist((float)var1, (float)var2, (float)(var1 + var4), (float)(var2 + var5));
               if (this.getCurrentGameTimeInMinutes() > var10.maxForceTime) {
                  if (var7 <= 3.0) {
                     var3 += 15;
                  } else if (var7 <= 4.5) {
                     var3 += 7;
                  }
               } else if (var7 <= 3.0) {
                  var3 += 15 * (100 - (var10.maxForceTime - this.getCurrentGameTimeInMinutes())) / 100;
               } else if (var7 <= 4.5) {
                  var3 += 7 * (100 - (var10.maxForceTime - this.getCurrentGameTimeInMinutes())) / 100;
               }
            }
         }
      }

      if (var3 < 0) {
         var3 = 0;
      }

      return Math.floor((double)var3);
   }

   public double getTrashAbundance(int var1, int var2) {
      for(int var3 = -6; var3 <= 6; ++var3) {
         for(int var4 = -6; var4 <= 6; ++var4) {
            if (this.isTrashPoint(var1 + var3, var2 + var4) && this.dist((float)var1, (float)var2, (float)(var1 + var3), (float)(var2 + var4)) < (double)(4.0F * this.getTrashPointRadius(var1 + var3, var2 + var4) + 2.0F)) {
               return (double)this.procedureRandomFloat((long)(var1 + var3), (long)(var2 + var4), (long)(this.trashSeed + 9601)) / 2.0 + 0.05;
            }
         }
      }

      return 0.05;
   }

   public void setFishingData(ByteBufferWriter var1) {
      var1.putInt(this.seed);
      var1.putInt(this.trashSeed);
      var1.putInt(noiseFishPointDisabler.size());
      Iterator var2 = noiseFishPointDisabler.entrySet().iterator();

      Map.Entry var3;
      while(var2.hasNext()) {
         var3 = (Map.Entry)var2.next();
         var1.putLong((Long)var3.getKey());
      }

      var1.putInt(chumPoints.size());
      var2 = chumPoints.entrySet().iterator();

      while(var2.hasNext()) {
         var3 = (Map.Entry)var2.next();
         var1.putLong((Long)var3.getKey());
         var1.putInt(((ChumData)var3.getValue()).maxForceTime);
      }

   }

   public void receiveFishingData(ByteBuffer var1) {
      this.seed = var1.getInt();
      this.trashSeed = var1.getInt();
      noiseFishPointDisabler.clear();
      chumPoints.clear();
      int var2 = var1.getInt();

      int var3;
      for(var3 = 0; var3 < var2; ++var3) {
         noiseFishPointDisabler.put(var1.getLong(), 0);
      }

      var2 = var1.getInt();

      for(var3 = 0; var3 < var2; ++var3) {
         chumPoints.put(var1.getLong(), new ChumData(var1.getInt(), 0));
      }

   }

   public void load() {
      File var1 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("fishingData.bin"));

      try {
         FileInputStream var2 = new FileInputStream(var1);

         try {
            BufferedInputStream var3 = new BufferedInputStream(var2);

            try {
               synchronized(SliceY.SliceBufferLock) {
                  ByteBuffer var5 = SliceY.SliceBuffer;
                  var5.clear();
                  int var6 = var3.read(var5.array());
                  var5.limit(var6);
                  this.seed = var5.getInt();
                  this.trashSeed = var5.getInt();
                  int var7 = var5.getInt();

                  for(int var8 = 0; var8 < var7; ++var8) {
                     chumPoints.put(var5.getLong(), new ChumData(var5.getInt(), var5.getInt()));
                  }
               }
            } catch (Throwable var13) {
               try {
                  var3.close();
               } catch (Throwable var11) {
                  var13.addSuppressed(var11);
               }

               throw var13;
            }

            var3.close();
         } catch (Throwable var14) {
            try {
               var2.close();
            } catch (Throwable var10) {
               var14.addSuppressed(var10);
            }

            throw var14;
         }

         var2.close();
      } catch (FileNotFoundException var15) {
      } catch (Throwable var16) {
         ExceptionLogger.logException(var16);
      }

   }

   public void save() {
      if (!Core.getInstance().isNoSave()) {
         File var1 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("fishingData.bin"));

         try {
            FileOutputStream var2 = new FileOutputStream(var1);

            try {
               BufferedOutputStream var3 = new BufferedOutputStream(var2);

               try {
                  synchronized(SliceY.SliceBufferLock) {
                     ByteBuffer var5 = SliceY.SliceBuffer;
                     var5.clear();
                     var5.putInt(this.seed);
                     var5.putInt(this.trashSeed);
                     var5.putInt(chumPoints.size());
                     Iterator var6 = chumPoints.entrySet().iterator();

                     while(var6.hasNext()) {
                        Map.Entry var7 = (Map.Entry)var6.next();
                        var5.putLong((Long)var7.getKey());
                        var5.putInt(((ChumData)var7.getValue()).maxForceTime);
                        var5.putInt(((ChumData)var7.getValue()).endTime);
                     }

                     var3.write(var5.array(), 0, var5.position());
                  }
               } catch (Throwable var12) {
                  try {
                     var3.close();
                  } catch (Throwable var10) {
                     var12.addSuppressed(var10);
                  }

                  throw var12;
               }

               var3.close();
            } catch (Throwable var13) {
               try {
                  var2.close();
               } catch (Throwable var9) {
                  var13.addSuppressed(var9);
               }

               throw var13;
            }

            var2.close();
         } catch (Throwable var14) {
            ExceptionLogger.logException(var14);
         }

      }
   }

   private void drawDebugFishingZones(int var1, int var2) {
      long var3 = this.coordsToHash(var1, var2);
      if (this.isFishPoint(var1, var2)) {
         if (this.getNumberOfFishInPoint(var1, var2) > 0 && !noiseFishPointDisabler.containsKey(var3)) {
            LineDrawer.DrawIsoCircle((float)var1, (float)var2, 0.0F, this.getFishPointRadius(var1, var2) + 1.0F, 16, 1.0F, 0.0F, 0.0F, 1.0F);
         } else {
            LineDrawer.DrawIsoCircle((float)var1, (float)var2, 0.0F, this.getFishPointRadius(var1, var2) + 1.0F, 16, 1.0F, 0.6F, 0.0F, 0.6F);
         }
      }

      if (chumPoints.get(var3) != null) {
         if (noiseFishPointDisabler.containsKey(var3)) {
            LineDrawer.DrawIsoCircle((float)var1, (float)var2, 0.0F, 1.6F, 16, 1.0F, 1.0F, 0.0F, 0.6F);
         } else {
            LineDrawer.DrawIsoCircle((float)var1, (float)var2, 0.0F, 1.6F, 16, 0.4F, 0.25F, 0.15F, 1.0F);
         }
      }

   }

   public static class ChumData {
      public int maxForceTime;
      public int endTime;

      public ChumData(int var1, int var2) {
         this.maxForceTime = var1;
         this.endTime = var2;
      }
   }

   public static class ZoneData {
      public Zone zone;

      public ZoneData(Zone var1) {
         this.zone = var1;
      }

      public int getCatchedFishNum() {
         return this.zone == null ? 0 : Integer.parseInt(this.zone.getName());
      }
   }
}
