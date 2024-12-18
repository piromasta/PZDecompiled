package zombie.pot;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import zombie.asset.AssetPath;
import zombie.core.math.PZMath;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.iso.BuildingDef;
import zombie.iso.NewMapBinaryFile;
import zombie.iso.SliceY;
import zombie.worldMap.WorldMapBinary;
import zombie.worldMap.WorldMapCell;
import zombie.worldMap.WorldMapData;
import zombie.worldMap.WorldMapDataAssetManager;
import zombie.worldMap.WorldMapFeature;

public final class POT {
   String m_mapDirectoryIn;
   String m_mapDirectoryOut;
   int m_minX;
   int m_minY;
   int m_maxX;
   int m_maxY;
   final TIntObjectHashMap<File> m_lotHeaderFiles = new TIntObjectHashMap();
   final TIntObjectHashMap<File> m_lotPackFiles = new TIntObjectHashMap();
   final TIntObjectHashMap<File> m_chunkDataFiles = new TIntObjectHashMap();
   final byte[] zombieDensityPerSquare = new byte[65536];
   final TIntObjectHashMap<POTLotHeader> m_newLotHeader = new TIntObjectHashMap();
   final TIntObjectHashMap<POTLotHeader> m_oldLotHeader = new TIntObjectHashMap();
   final TIntObjectHashMap<POTLotPack> m_oldLotPack = new TIntObjectHashMap();
   final TIntObjectHashMap<POTChunkData> m_oldChunkData = new TIntObjectHashMap();
   final TIntArrayList m_onlyTheseCells = new TIntArrayList();
   public static final int CHUNK_DIM_OLD = 10;
   public static final int CHUNK_PER_CELL_OLD = 30;
   public static final int CELL_DIM_OLD = 300;
   public static final int CHUNK_DIM_NEW = 8;
   public static final int CHUNK_PER_CELL_NEW = 32;
   public static final int CELL_DIM_NEW = 256;
   static final int LEVELS = 64;

   public POT() {
   }

   public void convertMapDirectory(String var1, String var2) throws Exception {
      Files.createDirectories(Paths.get(var2));
      this.m_mapDirectoryIn = var1;
      this.m_mapDirectoryOut = var2;
      this.readFileNames();
      this.convertLotHeaders();
      this.convertLotPack();
      this.convertChunkData();
      if (this.m_onlyTheseCells.isEmpty()) {
         this.convertObjectsLua();
         this.convertSpawnPointsLua();
         this.convertWorldMapBIN("worldmap.xml.bin");
         this.convertWorldMapBIN("worldmap-forest.xml.bin");
         this.convertWorldMapXML();
      }
   }

   boolean shouldIgnoreCell(int var1, int var2) {
      if (this.m_onlyTheseCells.isEmpty()) {
         return false;
      } else {
         for(int var3 = 0; var3 < this.m_onlyTheseCells.size(); var3 += 2) {
            int var4 = this.m_onlyTheseCells.get(var3);
            int var5 = this.m_onlyTheseCells.get(var3 + 1);
            if (var1 >= var4 - 1 && var1 <= var4 + 1 && var2 >= var5 - 1 && var2 <= var5 + 1) {
               return false;
            }
         }

         return true;
      }
   }

   boolean shouldConvertNewCell(int var1, int var2) {
      if (this.m_onlyTheseCells.isEmpty()) {
         return true;
      } else {
         int var3 = var1 * 256 / 300;
         int var4 = var2 * 256 / 300;
         int var5 = ((var1 + 1) * 256 - 1) / 300;
         int var6 = ((var2 + 1) * 256 - 1) / 300;

         for(int var7 = var4; var7 <= var6; ++var7) {
            for(int var8 = var3; var8 <= var5; ++var8) {
               for(int var9 = 0; var9 < this.m_onlyTheseCells.size(); var9 += 2) {
                  int var10 = this.m_onlyTheseCells.get(var9);
                  int var11 = this.m_onlyTheseCells.get(var9 + 1);
                  if (var8 == var10 && var7 == var11) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   void readFileNames() {
      this.m_minX = 2147483647;
      this.m_minY = 2147483647;
      this.m_maxX = -2147483648;
      this.m_maxY = -2147483648;
      File var1 = new File(this.m_mapDirectoryIn);
      File[] var2 = var1.listFiles();

      for(int var3 = 0; var3 < var2.length; ++var3) {
         String var4 = var2[var3].getName();
         String var5 = var4.substring(var4.lastIndexOf(46));
         var4 = var4.substring(0, var4.lastIndexOf(46));
         String[] var6;
         int var7;
         int var8;
         int var9;
         if (".lotheader".equals(var5)) {
            var6 = var4.split("_");
            var7 = Integer.parseInt(var6[0]);
            var8 = Integer.parseInt(var6[1]);
            if (!this.shouldIgnoreCell(var7, var8)) {
               this.m_minX = PZMath.min(this.m_minX, var7);
               this.m_minY = PZMath.min(this.m_minY, var8);
               this.m_maxX = PZMath.max(this.m_maxX, var7);
               this.m_maxY = PZMath.max(this.m_maxY, var8);
               var9 = var7 + var8 * 1000;
               this.m_lotHeaderFiles.put(var9, var2[var3]);
            }
         } else if (".lotpack".equals(var5)) {
            var6 = var4.replace("world_", "").split("_");
            var7 = Integer.parseInt(var6[0]);
            var8 = Integer.parseInt(var6[1]);
            if (!this.shouldIgnoreCell(var7, var8)) {
               var9 = var7 + var8 * 1000;
               this.m_lotPackFiles.put(var9, var2[var3]);
            }
         } else if (var4.startsWith("chunkdata_")) {
            var6 = var4.replace("chunkdata_", "").split("_");
            var7 = Integer.parseInt(var6[0]);
            var8 = Integer.parseInt(var6[1]);
            if (!this.shouldIgnoreCell(var7, var8)) {
               var9 = var7 + var8 * 1000;
               this.m_chunkDataFiles.put(var9, var2[var3]);
            }
         }
      }

   }

   void convertLotHeaders() {
      for(int var1 = this.m_minY * 300; var1 < (this.m_maxY + 1) * 300; var1 += 256) {
         for(int var2 = this.m_minX * 300; var2 <= (this.m_maxX + 1) * 300; var2 += 256) {
            int var3 = var2 / 256;
            int var4 = var1 / 256;
            if (this.shouldConvertNewCell(var3, var4)) {
               this.convertLotHeader(var3, var4);
            }
         }
      }

   }

   void convertLotHeader(int var1, int var2) {
      POTLotHeader var3 = new POTLotHeader(var1, var2, true);
      int var4 = var1 * 256 / 300;
      int var5 = var2 * 256 / 300;
      int var6 = ((var1 + 1) * 256 - 1) / 300;
      int var7 = ((var2 + 1) * 256 - 1) / 300;
      Arrays.fill(this.zombieDensityPerSquare, (byte)0);

      int var8;
      for(var8 = var5; var8 <= var7; ++var8) {
         for(int var9 = var4; var9 <= var6; ++var9) {
            POTLotHeader var10 = this.getOldLotHeader(var9, var8);
            if (var10 != null) {
               Iterator var11 = var10.Buildings.iterator();

               while(var11.hasNext()) {
                  BuildingDef var12 = (BuildingDef)var11.next();
                  if (var3.containsSquare(var12.x, var12.y)) {
                     var3.addBuilding(var12);
                  }
               }

               for(int var13 = 0; var13 < 256; ++var13) {
                  for(int var14 = 0; var14 < 256; ++var14) {
                     this.zombieDensityPerSquare[var14 + var13 * 256] = var10.getZombieDensityForSquare(var9 * 300 + var14, var8 * 300 + var13);
                  }
               }
            }
         }
      }

      var3.setZombieDensity(this.zombieDensityPerSquare);
      var8 = var1 + var2 * 1000;
      this.m_newLotHeader.put(var8, var3);
   }

   POTLotHeader getNewLotHeader(int var1, int var2) {
      int var3 = var1 + var2 * 1000;
      POTLotHeader var4 = (POTLotHeader)this.m_newLotHeader.get(var3);
      if (var4 == null) {
         var4 = new POTLotHeader(var1, var2, true);
         this.m_newLotHeader.put(var3, var4);
      }

      return var4;
   }

   POTLotHeader getOldLotHeader(int var1, int var2) {
      int var3 = var1 + var2 * 1000;
      File var4 = (File)this.m_lotHeaderFiles.get(var3);
      if (var4 == null) {
         return null;
      } else {
         POTLotHeader var5 = (POTLotHeader)this.m_oldLotHeader.get(var3);
         if (var5 == null) {
            var5 = new POTLotHeader(var1, var2, false);
            var5.load(var4);
            this.m_oldLotHeader.put(var3, var5);
         }

         return var5;
      }
   }

   POTLotPack getOldLotPack(POTLotHeader var1) throws IOException {
      int var2 = var1.x + var1.y * 1000;
      POTLotPack var3 = (POTLotPack)this.m_oldLotPack.get(var2);
      if (var3 == null) {
         var3 = new POTLotPack(var1);
         File var4 = (File)this.m_lotPackFiles.get(var2);
         var3.load(var4);
         this.m_oldLotPack.put(var2, var3);
      }

      return var3;
   }

   void convertLotPack() throws IOException {
      for(int var1 = this.m_minY * 300; var1 < (this.m_maxY + 1) * 300; var1 += 256) {
         for(int var2 = this.m_minX * 300; var2 < (this.m_maxX + 1) * 300; var2 += 256) {
            int var3 = var2 / 256;
            int var4 = var1 / 256;
            if (this.shouldConvertNewCell(var3, var4)) {
               if (var4 == 30) {
                  boolean var5 = true;
               }

               this.convertLotPack(var3, var4);
               int var11 = var2 / 300 - 1;
               int var6 = var1 / 300;

               for(int var7 = this.m_minY; var7 <= this.m_maxY; ++var7) {
                  for(int var8 = this.m_minX; var8 <= this.m_maxX && (var8 != var11 || var7 != var6); ++var8) {
                     POTLotPack var9 = (POTLotPack)this.m_oldLotPack.remove(var8 + var7 * 1000);
                     if (var9 != null) {
                        var9.clear();
                     }

                     POTLotHeader var10 = (POTLotHeader)this.m_oldLotHeader.remove(var8 + var7 * 1000);
                     if (var10 != null) {
                        var10.clear();
                     }
                  }
               }
            }
         }
      }

   }

   void convertLotPack(int var1, int var2) throws IOException {
      POTLotHeader var3 = this.getNewLotHeader(var1, var2);
      if (var3 != null) {
         var3.minLevelNotEmpty = 1000;
         var3.maxLevelNotEmpty = -1000;
         POTLotPack var4 = new POTLotPack(var3);

         for(int var5 = -32; var5 <= 31; ++var5) {
            int var6 = var3.getMinSquareY();

            for(int var7 = var3.getMaxSquareY(); var6 <= var7; ++var6) {
               int var8 = var3.getMinSquareX();

               for(int var9 = var3.getMaxSquareX(); var8 <= var9; ++var8) {
                  var4.setSquareData(var8, var6, var5, this.getOldLotPackSquareData(var8, var6, var5));
               }
            }
         }

         var3.save(String.format("%s%s%d_%d.lotheader", this.m_mapDirectoryOut, File.separator, var3.x, var3.y));
         var4.save(String.format("%s%sworld_%d_%d.lotpack", this.m_mapDirectoryOut, File.separator, var4.x, var4.y));
         this.m_newLotHeader.remove(var1 + var2 * 1000);
         var3.clear();
         var4.clear();
      }
   }

   String[] getOldLotPackSquareData(int var1, int var2, int var3) throws IOException {
      POTLotHeader var4 = this.getOldLotHeader(var1 / 300, var2 / 300);
      if (var4 == null) {
         return null;
      } else if (!var4.containsSquare(var1, var2)) {
         return null;
      } else if (var3 >= var4.minLevel && var3 <= var4.maxLevel) {
         POTLotPack var5 = this.getOldLotPack(var4);
         return var5.getSquareData(var1, var2, var3);
      } else {
         return null;
      }
   }

   void convertChunkData() throws IOException {
      for(int var1 = this.m_minY * 300; var1 < (this.m_maxY + 1) * 300; var1 += 256) {
         for(int var2 = this.m_minX * 300; var2 < (this.m_maxX + 1) * 300; var2 += 256) {
            int var3 = var2 / 256;
            int var4 = var1 / 256;
            if (this.shouldConvertNewCell(var3, var4)) {
               this.convertChunkData(var3, var4);
            }
         }
      }

   }

   void convertChunkData(int var1, int var2) throws IOException {
      POTChunkData var3 = new POTChunkData(var1, var2, true);
      int var4 = var3.getMinSquareY();

      for(int var5 = var3.getMaxSquareY(); var4 <= var5; ++var4) {
         int var6 = var3.getMinSquareX();

         for(int var7 = var3.getMaxSquareX(); var6 <= var7; ++var6) {
            var3.setSquareBits(var6, var4, this.getOldChunkDataBits(var6, var4));
         }
      }

      var3.save(String.format("%s%schunkdata_%d_%d.bin", this.m_mapDirectoryOut, File.separator, var3.x, var3.y));
   }

   POTChunkData getOldChunkData(int var1, int var2) throws IOException {
      int var3 = var1 + var2 * 1000;
      File var4 = (File)this.m_chunkDataFiles.get(var3);
      if (var4 == null) {
         return null;
      } else {
         POTChunkData var5 = (POTChunkData)this.m_oldChunkData.get(var3);
         if (var5 == null) {
            var5 = new POTChunkData(var1, var2, false);
            var5.load(var4);
            this.m_oldChunkData.put(var3, var5);
         }

         return var5;
      }
   }

   byte getOldChunkDataBits(int var1, int var2) throws IOException {
      POTChunkData var3 = this.getOldChunkData(var1 / 300, var2 / 300);
      if (var3 == null) {
         return 0;
      } else {
         return !var3.containsSquare(var1, var2) ? 0 : var3.getSquareBits(var1, var2);
      }
   }

   void convertObjectsLua() {
   }

   void convertSpawnPointsLua() {
   }

   void convertWorldMapBIN(String var1) throws Exception {
      String var2 = this.m_mapDirectoryIn + File.separator + var1;
      File var3 = new File(var2);
      if (var3.exists()) {
         WorldMapBinary var4 = new WorldMapBinary();
         WorldMapData var5 = new WorldMapData(new AssetPath(var2), WorldMapDataAssetManager.instance);
         var4.read(this.m_mapDirectoryIn + File.separator + var1, var5);
         var5.onLoaded();
         POTWorldMapData var6 = new POTWorldMapData();

         for(int var7 = var5.m_minY; var7 <= var5.m_maxY; ++var7) {
            for(int var8 = var5.m_minX; var8 <= var5.m_maxX; ++var8) {
               WorldMapCell var9 = var5.getCell(var8, var7);
               if (var9 != null) {
                  Iterator var10 = var9.m_features.iterator();

                  while(var10.hasNext()) {
                     WorldMapFeature var11 = (WorldMapFeature)var10.next();
                     var6.addFeature(var11);
                  }
               }
            }
         }

         var6.m_minX = 2147483647;
         var6.m_minY = 2147483647;
         var6.m_maxX = -2147483648;
         var6.m_maxY = -2147483648;

         WorldMapCell var13;
         for(Iterator var12 = var6.m_cells.iterator(); var12.hasNext(); var6.m_maxY = Math.max(var6.m_maxY, var13.m_y)) {
            var13 = (WorldMapCell)var12.next();
            var6.m_minX = Math.min(var6.m_minX, var13.m_x);
            var6.m_minY = Math.min(var6.m_minY, var13.m_y);
            var6.m_maxX = Math.max(var6.m_maxX, var13.m_x);
         }

         var6.saveBIN(this.m_mapDirectoryOut + File.separator + var1, true);
      }
   }

   void convertWorldMapXML() {
   }

   void convertNewMapBinaryDirectory(String var1, String var2) throws IOException {
      File var3 = new File(var1);
      File[] var4 = var3.listFiles();

      for(int var5 = 0; var5 < var4.length; ++var5) {
         String var6 = var4[var5].getName();
         String var7 = var6.substring(var6.lastIndexOf(46));
         if (".pzby".equalsIgnoreCase(var7)) {
            this.convertNewMapBinaryFile(var4[var5], new File(var2, var4[var5].getName()));
         }
      }

   }

   void convertNewMapBinaryFile(File var1, File var2) throws IOException {
      NewMapBinaryFile var3 = new NewMapBinaryFile(false);
      NewMapBinaryFile.Header var4 = var3.loadHeader(var1.getAbsolutePath());

      for(int var5 = 0; var5 < var4.m_height; ++var5) {
         for(int var6 = 0; var6 < var4.m_width; ++var6) {
            var3.loadChunk(var4, var6, var5);
         }
      }

   }

   public static void runOnStart() {
      new POT();

      try {
         RandStandard.INSTANCE.init();
         RandLua.INSTANCE.init();
      } catch (Exception var2) {
         var2.printStackTrace();
         SliceY.SliceBuffer.order(ByteOrder.BIG_ENDIAN);
      }

      System.exit(0);
   }
}
