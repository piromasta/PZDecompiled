package zombie.iso;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.erosion.ErosionRegions;
import zombie.erosion.season.ErosionIceQueen;
import zombie.gameStates.GameLoadingState;
import zombie.gameStates.IngameState;
import zombie.globalObjects.GlobalObjectLookup;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.CoopSlave;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.scripting.ScriptManager;
import zombie.vehicles.VehicleManager;
import zombie.world.WorldDictionary;
import zombie.world.WorldDictionaryException;

public final class WorldConverter {
   public static final WorldConverter instance = new WorldConverter();
   public static int MIN_VERSION = 1;
   public static int convertingVersion = 0;
   public static boolean converting;
   public HashMap<Integer, Integer> TilesetConversions = null;
   int oldID = 0;

   public WorldConverter() {
   }

   public void convert(String var1, IsoSpriteManager var2) throws IOException {
      String var10002 = ZomboidFileSystem.instance.getGameModeCacheDir();
      File var3 = new File(var10002 + File.separator + var1 + File.separator + "map_ver.bin");
      if (var3.exists()) {
         converting = true;
         FileInputStream var4 = new FileInputStream(var3);
         DataInputStream var5 = new DataInputStream(var4);
         convertingVersion = var5.readInt();
         var5.close();
         if (convertingVersion < 219) {
            if (convertingVersion < MIN_VERSION) {
               GameLoadingState.worldVersionError = true;
               return;
            }

            try {
               this.convert(var1, convertingVersion, 219);
            } catch (Exception var7) {
               IngameState.createWorld(var1);
               IngameState.copyWorld(var1 + "_backup", var1);
               var7.printStackTrace();
            }
         }

         converting = false;
      }

   }

   private void convert(String var1, int var2, int var3) {
      if (!GameClient.bClient) {
         GameLoadingState.convertingWorld = true;
         String var4 = Core.GameSaveWorld;
         IngameState.createWorld(var1 + "_backup");
         IngameState.copyWorld(var1, Core.GameSaveWorld);
         Core.GameSaveWorld = var4;
         if (var3 >= 14 && var2 < 14) {
            try {
               this.convertchunks(var1, 25, 25);
            } catch (IOException var8) {
               var8.printStackTrace();
            }
         } else if (var2 == 7) {
            try {
               this.convertchunks(var1);
            } catch (IOException var7) {
               var7.printStackTrace();
            }
         }

         if (var2 <= 4) {
            this.loadconversionmap(var2, "tiledefinitions");
            this.loadconversionmap(var2, "newtiledefinitions");

            try {
               this.convertchunks(var1);
            } catch (IOException var6) {
               var6.printStackTrace();
            }
         }

         GameLoadingState.convertingWorld = false;
      }
   }

   private void convertchunks(String var1) throws IOException {
      IsoCell var2 = new IsoCell(IsoCell.CellSizeInSquares, IsoCell.CellSizeInSquares);
      IsoChunkMap var3 = new IsoChunkMap(var2);
      String var10002 = ZomboidFileSystem.instance.getGameModeCacheDir();
      File var4 = new File(var10002 + File.separator + var1 + File.separator);
      if (!var4.exists()) {
         var4.mkdir();
      }

      String[] var5 = var4.list();
      String[] var6 = var5;
      int var7 = var5.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         String var9 = var6[var8];
         if (var9.contains(".bin") && !var9.equals("map.bin") && !var9.equals("map_p.bin") && !var9.matches("map_p[0-9]+\\.bin") && !var9.equals("map_t.bin") && !var9.equals("map_c.bin") && !var9.equals("map_ver.bin") && !var9.equals("map_sand.bin") && !var9.equals("map_mov.bin") && !var9.equals("map_meta.bin") && !var9.equals("map_cm.bin") && !var9.equals("pc.bin") && !var9.startsWith("zpop_") && !var9.startsWith("chunkdata_")) {
            String[] var10 = var9.replace(".bin", "").replace("map_", "").split("_");
            int var11 = Integer.parseInt(var10[0]);
            int var12 = Integer.parseInt(var10[1]);
            var3.LoadChunkForLater(var11, var12, 0, 0);
            var3.SwapChunkBuffers();
            var3.getChunk(0, 0).Save(true);
         }
      }

   }

   private void convertchunks(String var1, int var2, int var3) throws IOException {
      IsoCell var4 = new IsoCell(IsoCell.CellSizeInSquares, IsoCell.CellSizeInSquares);
      new IsoChunkMap(var4);
      String var10002 = ZomboidFileSystem.instance.getGameModeCacheDir();
      File var6 = new File(var10002 + File.separator + var1 + File.separator);
      if (!var6.exists()) {
         var6.mkdir();
      }

      String[] var7 = var6.list();
      IsoWorld.saveoffsetx = var2;
      IsoWorld.saveoffsety = var3;
      IsoWorld.instance.MetaGrid.Create();
      WorldStreamer.instance.create();
      String[] var8 = var7;
      int var9 = var7.length;

      for(int var10 = 0; var10 < var9; ++var10) {
         String var11 = var8[var10];
         if (var11.contains(".bin") && !var11.equals("map.bin") && !var11.equals("map_p.bin") && !var11.matches("map_p[0-9]+\\.bin") && !var11.equals("map_t.bin") && !var11.equals("map_c.bin") && !var11.equals("map_ver.bin") && !var11.equals("map_sand.bin") && !var11.equals("map_mov.bin") && !var11.equals("map_meta.bin") && !var11.equals("map_cm.bin") && !var11.equals("pc.bin") && !var11.startsWith("zpop_") && !var11.startsWith("chunkdata_")) {
            String[] var12 = var11.replace(".bin", "").replace("map_", "").split("_");
            int var13 = Integer.parseInt(var12[0]);
            int var14 = Integer.parseInt(var12[1]);
            IsoChunk var15 = new IsoChunk(var4);
            var15.refs.add(var4.ChunkMap[0]);
            WorldStreamer.instance.addJobConvert(var15, 0, 0, var13, var14);

            while(!var15.bLoaded) {
               try {
                  Thread.sleep(20L);
               } catch (InterruptedException var18) {
                  var18.printStackTrace();
               }
            }

            var15.wx += var2 * IsoCell.CellSizeInChunks;
            var15.wy += var3 * IsoCell.CellSizeInChunks;
            var15.jobType = IsoChunk.JobType.Convert;
            var15.Save(true);
            File var16 = new File(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + var1 + File.separator + var11);

            while(!ChunkSaveWorker.instance.toSaveQueue.isEmpty()) {
               try {
                  Thread.sleep(13L);
               } catch (InterruptedException var19) {
                  var19.printStackTrace();
               }
            }

            var16.delete();
         }
      }

   }

   private void loadconversionmap(int var1, String var2) {
      String var3 = "media/" + var2 + "_" + var1 + ".tiles";
      File var4 = new File(var3);
      if (var4.exists()) {
         try {
            RandomAccessFile var5 = new RandomAccessFile(var4.getAbsolutePath(), "r");
            int var6 = IsoWorld.readInt(var5);

            for(int var7 = 0; var7 < var6; ++var7) {
               Thread.sleep(4L);
               String var8 = IsoWorld.readString(var5);
               String var9 = var8.trim();
               IsoWorld.readString(var5);
               int var10 = IsoWorld.readInt(var5);
               int var11 = IsoWorld.readInt(var5);
               int var12 = IsoWorld.readInt(var5);

               for(int var13 = 0; var13 < var12; ++var13) {
                  IsoSprite var14 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var9 + "_" + var13);
                  if (this.TilesetConversions == null) {
                     this.TilesetConversions = new HashMap();
                  }

                  this.TilesetConversions.put(this.oldID, var14.ID);
                  ++this.oldID;
                  int var15 = IsoWorld.readInt(var5);

                  for(int var16 = 0; var16 < var15; ++var16) {
                     var8 = IsoWorld.readString(var5);
                     String var17 = var8.trim();
                     var8 = IsoWorld.readString(var5);
                     String var18 = var8.trim();
                  }
               }
            }
         } catch (Exception var19) {
         }
      }

   }

   public void softreset() throws IOException, WorldDictionaryException {
      String var1 = GameServer.ServerName;
      Core.GameSaveWorld = var1;
      IsoCell var2 = new IsoCell(IsoCell.CellSizeInSquares, IsoCell.CellSizeInSquares);
      IsoChunk var3 = new IsoChunk(var2);
      var3.assignLoadID();
      String var10002 = ZomboidFileSystem.instance.getGameModeCacheDir();
      File var4 = new File(var10002 + File.separator + var1 + File.separator);
      if (!var4.exists()) {
         var4.mkdir();
      }

      ArrayList var5 = this.gatherFiles();
      if (CoopSlave.instance != null) {
         CoopSlave.instance.sendMessage("softreset-count", (String)null, Integer.toString(var5.size()));
      }

      IsoWorld.instance.MetaGrid.Create();
      ServerMap.instance.init(IsoWorld.instance.MetaGrid);
      new ErosionIceQueen(IsoSpriteManager.instance);
      ErosionRegions.init();
      WorldStreamer.instance.create();
      VehicleManager.instance = new VehicleManager();
      WorldDictionary.init();
      ScriptManager.instance.PostWorldDictionaryInit();
      GlobalObjectLookup.init(IsoWorld.instance.getMetaGrid());
      LuaEventManager.triggerEvent("OnSGlobalObjectSystemInit");
      int var6 = var5.size();
      DebugLog.log("processing " + var6 + " files");
      Iterator var7 = var5.iterator();

      while(true) {
         while(true) {
            Path var8;
            String var9;
            do {
               if (!var7.hasNext()) {
                  GameServer.ResetID = Rand.Next(10000000);
                  ServerOptions.instance.putSaveOption("ResetID", String.valueOf(GameServer.ResetID));
                  IsoWorld.instance.CurrentCell = null;
                  DebugLog.log("soft-reset complete, server terminated");
                  if (CoopSlave.instance != null) {
                     CoopSlave.instance.sendMessage("softreset-finished", (String)null, "");
                  }

                  SteamUtils.shutdown();
                  System.exit(0);
                  return;
               }

               var8 = (Path)var7.next();
               --var6;
               var9 = var8.toString();
            } while(var9.contains("blam"));

            String var10 = var8.getFileName().toString();
            if (var10.startsWith("zpop_")) {
               deleteFile(var8);
            } else if (var10.equals("map_t.bin")) {
               deleteFile(var8);
            } else if (!var10.equals("map_meta.bin") && !var10.equals("map_zone.bin")) {
               if (var10.equals("reanimated.bin")) {
                  deleteFile(var8);
               } else if (var10.matches("map_[0-9]+_[0-9]+\\.bin")) {
                  System.out.println("Soft clearing chunk: " + var10);
                  String[] var11 = var10.replace(".bin", "").replace("map_", "").split("_");
                  int var12 = Integer.parseInt(var11[0]);
                  int var13 = Integer.parseInt(var11[1]);
                  var3.refs.add(var2.ChunkMap[0]);
                  var3.wx = var12;
                  var3.wy = var13;
                  ServerMap.instance.setSoftResetChunk(var3);
                  WorldStreamer.instance.addJobWipe(var3, 0, 0, var12, var13);

                  while(!var3.bLoaded) {
                     try {
                        Thread.sleep(20L);
                     } catch (InterruptedException var21) {
                        var21.printStackTrace();
                     }
                  }

                  var3.jobType = IsoChunk.JobType.Convert;
                  var3.FloorBloodSplats.clear();

                  try {
                     var3.Save(true);
                  } catch (Exception var20) {
                     var20.printStackTrace();
                  }

                  ServerMap.instance.clearSoftResetChunk(var3);
                  byte var14 = 64;

                  for(int var15 = var3.minLevel; var15 <= var3.maxLevel; ++var15) {
                     for(int var16 = 0; var16 < var14; ++var16) {
                        IsoGridSquare var17 = var3.squares[var3.squaresIndexOfLevel(var15)][var16];
                        if (var17 != null) {
                           for(int var18 = 0; var18 < var17.getObjects().size(); ++var18) {
                              IsoObject var19 = (IsoObject)var17.getObjects().get(var18);
                              var19.removeFromWorld(false);
                           }
                        }
                     }
                  }

                  var3.doReuseGridsquares();
                  IsoChunkMap.chunkStore.remove(var3);
                  if (var6 % 100 == 0) {
                     DebugLog.log("" + var6 + " files to go");
                  }

                  if (CoopSlave.instance != null && var6 % 10 == 0) {
                     CoopSlave.instance.sendMessage("softreset-remaining", (String)null, Integer.toString(var6));
                  }
               }
            } else {
               deleteFile(var8);
            }
         }
      }
   }

   private ArrayList<Path> gatherFiles() throws IOException {
      final ArrayList var1 = new ArrayList();
      Path var2 = Paths.get(ZomboidFileSystem.instance.getCurrentSaveDir());
      Files.walkFileTree(var2, new FileVisitor<Path>() {
         public FileVisitResult preVisitDirectory(Path var1x, BasicFileAttributes var2) throws IOException {
            return FileVisitResult.CONTINUE;
         }

         public FileVisitResult visitFile(Path var1x, BasicFileAttributes var2) throws IOException {
            var1.add(var1x);
            return FileVisitResult.CONTINUE;
         }

         public FileVisitResult visitFileFailed(Path var1x, IOException var2) throws IOException {
            ExceptionLogger.logException(var2);
            return FileVisitResult.CONTINUE;
         }

         public FileVisitResult postVisitDirectory(Path var1x, IOException var2) throws IOException {
            return FileVisitResult.CONTINUE;
         }
      });
      return var1;
   }

   private static void deleteFile(Path var0) throws IOException {
      Files.delete(var0);
   }
}
