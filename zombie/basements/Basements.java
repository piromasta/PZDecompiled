package zombie.basements;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import zombie.ChunkMapFilenames;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.random.RandInterface;
import zombie.core.random.RandSeeded;
import zombie.debug.DebugLog;
import zombie.iso.BuildingDef;
import zombie.iso.BuildingID;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoLot;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.NewMapBinaryFile;
import zombie.iso.RoomDef;
import zombie.iso.RoomID;
import zombie.iso.worldgen.WGParams;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class Basements {
   private static Basements instance;
   final ArrayList<BasementDefinition> basementDefinitions = new ArrayList();
   final HashMap<String, BasementDefinition> basementDefinitionByName = new HashMap();
   final ArrayList<BasementDefinition> basementAccessDefinitions = new ArrayList();
   final HashMap<String, BasementDefinition> basementAccessDefinitionByName = new HashMap();
   final ArrayList<BasementSpawnLocation> basementSpawnLocations = new ArrayList();
   final ArrayList<BasementPlacement> basementPlacements = new ArrayList();
   private final HashMap<String, BasementsPerMap> basementsPerMap = new HashMap();
   private final BasementsV1 apiV1 = new BasementsV1();
   public static final int SAVEFILE_VERSION = 1;
   private static final byte[] FILE_MAGIC = new byte[]{66, 83, 77, 84};
   private final ArrayList<BuildingDef> buildingDefs = new ArrayList();
   private final ArrayList<BuildingDef> tempBuildingDefs = new ArrayList();
   private final ArrayList<RoomDef> tempRooms = new ArrayList();
   private final HashMap<BuildingDef, MergedRooms> mergedRooms = new HashMap();

   public Basements() {
   }

   public static Basements getInstance() {
      if (instance == null) {
         instance = new Basements();
      }

      return instance;
   }

   public static BasementsV1 getAPIv1() {
      return getInstance().apiV1;
   }

   public BasementsPerMap getPerMap(String var1) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         throw new IllegalArgumentException("invalid mapID \"%s\"".formatted(var1));
      } else {
         var1 = var1.trim();
         return (BasementsPerMap)this.basementsPerMap.get(var1);
      }
   }

   public BasementsPerMap getOrCreatePerMap(String var1) {
      BasementsPerMap var2 = this.getPerMap(var1);
      if (var2 == null) {
         var1 = var1.trim();
         var2 = new BasementsPerMap(var1);
         this.basementsPerMap.put(var1, var2);
      }

      return var2;
   }

   public void beforeOnLoadMapZones() {
      this.basementSpawnLocations.clear();
      Iterator var1 = this.basementsPerMap.values().iterator();

      while(var1.hasNext()) {
         BasementsPerMap var2 = (BasementsPerMap)var1.next();
         var2.basementDefinitions.clear();
         var2.basementDefinitionByName.clear();
         var2.basementAccessDefinitions.clear();
         var2.basementAccessDefinitionByName.clear();
         var2.basementSpawnLocations.clear();
      }

      this.basementsPerMap.clear();
   }

   public void beforeLoadMetaGrid() {
      this.basementDefinitions.clear();
      this.basementDefinitionByName.clear();
      this.basementAccessDefinitions.clear();
      this.basementAccessDefinitionByName.clear();
      this.basementPlacements.clear();
      this.buildingDefs.clear();
      this.parseBasementDefinitions();
      this.parseBasementAccessDefinitions();
      this.parseBasementSpawnLocations();
      if (this.loadSavefile()) {
         this.addBasementBuildingDefsToLotHeaders();
         this.addBasementBuildingDefsToMetaGrid();
      } else {
         this.loadBasementDefinitionHeaders();
         this.loadBasementAccessDefinitionHeaders();
         this.calculateBasementPlacements();
         this.createBasementBuildingDefs();
         this.writeSavefile();
         this.addBasementBuildingDefsToLotHeaders();
         this.addBasementBuildingDefsToMetaGrid();
      }
   }

   public void afterLoadMetaGrid() {
      this.basementSpawnLocations.clear();
      Iterator var1 = this.basementDefinitions.iterator();

      BasementDefinition var2;
      while(var1.hasNext()) {
         var2 = (BasementDefinition)var1.next();
         if (var2.header != null) {
            var2.header.Dispose();
            var2.header = null;
         }
      }

      this.basementDefinitions.clear();
      this.basementDefinitionByName.clear();
      var1 = this.basementAccessDefinitions.iterator();

      while(var1.hasNext()) {
         var2 = (BasementDefinition)var1.next();
         if (var2.header != null) {
            var2.header.Dispose();
            var2.header = null;
         }
      }

      this.basementAccessDefinitions.clear();
      this.basementAccessDefinitionByName.clear();
   }

   boolean loadSavefile() {
      File var1 = ZomboidFileSystem.instance.getFileInCurrentSave("map_basements.bin");

      try {
         FileInputStream var2 = new FileInputStream(var1);

         boolean var4;
         try {
            DataInputStream var3 = new DataInputStream(var2);

            try {
               this.loadSavefile(var1, var3);
               var4 = true;
            } catch (Throwable var8) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }

               throw var8;
            }

            var3.close();
         } catch (Throwable var9) {
            try {
               var2.close();
            } catch (Throwable var6) {
               var9.addSuppressed(var6);
            }

            throw var9;
         }

         var2.close();
         return var4;
      } catch (FileNotFoundException var10) {
      } catch (Exception var11) {
         ExceptionLogger.logException(var11);
      }

      return false;
   }

   void loadSavefile(File var1, DataInputStream var2) throws IOException {
      byte[] var3 = new byte[4];
      int var4 = var2.read(var3);
      if (var4 >= 4 && Arrays.equals(var3, FILE_MAGIC)) {
         int var5 = var2.readInt();
         int var6 = var2.readInt();
         int var7 = var2.readInt();

         for(int var8 = 0; var8 < var7; ++var8) {
            BasementPlacement var9 = new BasementPlacement();
            var9.x = var2.readInt();
            var9.y = var2.readInt();
            var9.z = var2.readInt();
            var9.w = var2.readShort();
            var9.h = var2.readShort();
            var9.name = var2.readUTF();
            this.basementPlacements.add(var9);
         }

         ArrayList var23 = new ArrayList();
         int var24 = var2.readInt();

         for(int var10 = 0; var10 < var24; ++var10) {
            BuildingDef var11 = new BuildingDef();
            var11.ID = (long)var10;
            short var12 = var2.readShort();

            for(int var13 = 0; var13 < var12; ++var13) {
               String var14 = var2.readUTF();
               RoomDef var15 = new RoomDef((long)var13, var14);
               var15.building = var11;
               var15.level = var2.readByte();
               short var16 = var2.readShort();

               for(int var17 = 0; var17 < var16; ++var17) {
                  short var18 = var2.readShort();
                  short var19 = var2.readShort();
                  short var20 = var2.readShort();
                  short var21 = var2.readShort();
                  RoomDef.RoomRect var22 = new RoomDef.RoomRect(var18, var19, var20, var21);
                  var15.rects.add(var22);
               }

               var15.CalculateBounds();
               var11.rooms.add(var15);
            }

            var11.CalculateBounds(var23);
            this.buildingDefs.add(var11);
         }

      } else {
         throw new IOException(var1.getAbsolutePath() + " does not appear to be map_basements.bin");
      }
   }

   void writeSavefile() {
      if (!Core.getInstance().isNoSave()) {
         File var1 = ZomboidFileSystem.instance.getFileInCurrentSave("map_basements.bin");

         try {
            FileOutputStream var2 = new FileOutputStream(var1);

            try {
               DataOutputStream var3 = new DataOutputStream(var2);

               try {
                  this.writeSavefile(var3);
               } catch (Throwable var8) {
                  try {
                     var3.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }

                  throw var8;
               }

               var3.close();
            } catch (Throwable var9) {
               try {
                  var2.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }

               throw var9;
            }

            var2.close();
         } catch (Exception var10) {
            ExceptionLogger.logException(var10);
         }

      }
   }

   void writeSavefile(DataOutputStream var1) throws IOException {
      var1.write(FILE_MAGIC);
      var1.writeInt(219);
      var1.writeInt(1);
      var1.writeInt(this.basementPlacements.size());

      int var2;
      for(var2 = 0; var2 < this.basementPlacements.size(); ++var2) {
         BasementPlacement var3 = (BasementPlacement)this.basementPlacements.get(var2);
         var1.writeInt(var3.x);
         var1.writeInt(var3.y);
         var1.writeInt(var3.z);
         var1.writeShort(var3.w);
         var1.writeShort(var3.h);
         var1.writeUTF(var3.name);
      }

      var1.writeInt(this.buildingDefs.size());

      for(var2 = 0; var2 < this.buildingDefs.size(); ++var2) {
         BuildingDef var8 = (BuildingDef)this.buildingDefs.get(var2);
         var1.writeShort(var8.rooms.size());

         for(int var4 = 0; var4 < var8.rooms.size(); ++var4) {
            RoomDef var5 = (RoomDef)var8.rooms.get(var4);
            var1.writeUTF(var5.name);
            var1.writeByte(var5.level);
            var1.writeShort(var5.rects.size());

            for(int var6 = 0; var6 < var5.rects.size(); ++var6) {
               RoomDef.RoomRect var7 = (RoomDef.RoomRect)var5.rects.get(var6);
               var1.writeShort(var7.x);
               var1.writeShort(var7.y);
               var1.writeShort(var7.w);
               var1.writeShort(var7.h);
            }
         }
      }

   }

   public void parseBasementDefinitions() {
      ArrayList var1 = IsoWorld.instance.getMetaGrid().getLotDirectories();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         BasementsPerMap var4 = this.getPerMap(var3);
         if (var4 != null) {
            this.basementDefinitions.addAll(var4.basementDefinitions);
            this.basementDefinitionByName.putAll(var4.basementDefinitionByName);
         }
      }

   }

   public void parseBasementAccessDefinitions() {
      ArrayList var1 = IsoWorld.instance.getMetaGrid().getLotDirectories();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         BasementsPerMap var4 = this.getPerMap(var3);
         if (var4 != null) {
            this.basementAccessDefinitions.addAll(var4.basementAccessDefinitions);
            this.basementAccessDefinitionByName.putAll(var4.basementAccessDefinitionByName);
         }
      }

   }

   void parseBasementSpawnLocations() {
      ArrayList var1 = IsoWorld.instance.getMetaGrid().getLotDirectories();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         BasementsPerMap var4 = this.getPerMap(var3);
         if (var4 != null) {
            this.basementSpawnLocations.addAll(var4.basementSpawnLocations);
         }
      }

   }

   void calculateBasementPlacements() {
      RandSeeded var1 = new RandSeeded(WGParams.instance.getSeed());
      BasementOverlap var2 = new BasementOverlap();
      Iterator var3 = this.basementsPerMap.values().iterator();

      while(var3.hasNext()) {
         BasementsPerMap var4 = (BasementsPerMap)var3.next();
         this.calculateBasementPlacements(var2, var4, var1);
      }

      var2.Dispose();
   }

   void calculateBasementPlacements(BasementOverlap var1, BasementsPerMap var2, RandInterface var3) {
      ArrayList var4 = new ArrayList();

      for(int var5 = 0; var5 < var2.basementSpawnLocations.size(); ++var5) {
         BasementSpawnLocation var6 = (BasementSpawnLocation)var2.basementSpawnLocations.get(var5);
         var4.clear();
         int var7;
         BasementDefinition var9;
         if (var6.specificBasement != null && !var6.specificBasement.isEmpty()) {
            for(var7 = 0; var7 < var6.specificBasement.size(); ++var7) {
               String var11 = (String)var6.specificBasement.get(var7);
               var9 = (BasementDefinition)var2.basementDefinitionByName.get(var11);
               if (var9 != null && var9.header != null && this.canPlaceAt(var2.mapID, var9, var6, var1)) {
                  var4.add(var9);
               }
            }
         } else {
            for(var7 = 0; var7 < var2.basementDefinitions.size(); ++var7) {
               BasementDefinition var8 = (BasementDefinition)var2.basementDefinitions.get(var7);
               if (this.canPlaceAt(var2.mapID, var8, var6, var1)) {
                  var4.add(var8);
               }
            }
         }

         if (!var4.isEmpty()) {
            BasementDefinition var12 = (BasementDefinition)PZArrayUtil.pickRandom((List)var4, var3);
            BasementPlacement var13 = new BasementPlacement();
            if (var6.w > 1 && var6.h > 1) {
               int var14 = var6.x + var6.stairX;
               int var10 = var6.y + var6.stairY;
               var13.x = var14 - var12.stairx;
               var13.y = var10 - var12.stairy;
            } else {
               var13.x = var6.x - var12.stairx;
               var13.y = var6.y - var12.stairy;
            }

            var13.z = var6.z - var12.header.m_levels;
            var13.w = var12.width;
            var13.h = var12.height;
            var13.name = var12.name;
            this.basementPlacements.add(var13);
            var1.addBasement((BuildingDef)var12.header.m_buildingDefList.get(0), var13.x, var13.y, var13.z);
            if (var6.access != null) {
               var9 = (BasementDefinition)this.basementAccessDefinitionByName.get(var6.access);
               if (var9 != null && var9.header != null) {
                  BasementPlacement var15 = new BasementPlacement();
                  var15.x = var13.x + var12.stairx - var9.stairx;
                  var15.y = var13.y + var12.stairy - var9.stairy;
                  var15.z = var6.z;
                  var15.w = var9.width;
                  var15.h = var9.height;
                  var15.name = var9.name;
                  this.basementPlacements.add(var15);
               }
            }
         }
      }

   }

   boolean canPlaceAt(String var1, BasementDefinition var2, BasementSpawnLocation var3, BasementOverlap var4) {
      if (var2.north != var3.north) {
         return false;
      } else {
         int var5 = var3.x + var3.stairX;
         int var6 = var3.y + var3.stairY;
         int var7 = var5 - var2.stairx;
         int var8 = var6 - var2.stairy;
         int var9 = var3.z - var2.header.m_levels;
         if (var3.w > 1 && var3.h > 1) {
            boolean var10 = var7 >= var3.x && var7 + var2.width <= var3.x + var3.w && var8 >= var3.y && var8 + var2.height <= var3.y + var3.h;
            if (!var10) {
               return false;
            }
         }

         if (!this.isCellFromThisMap(var1, var3.x, var3.y)) {
            return false;
         } else {
            return !var4.checkOverlap((BuildingDef)var2.header.m_buildingDefList.get(0), var7, var8, var9);
         }
      }
   }

   boolean isCellFromThisMap(String var1, int var2, int var3) {
      int var4 = var2 / IsoCell.CellSizeInSquares;
      int var5 = var3 / IsoCell.CellSizeInSquares;
      String var6 = ChunkMapFilenames.instance.getHeader(var4, var5);
      LotHeader var7 = (LotHeader)IsoLot.InfoHeaders.get(var6);
      return var7 != null && var7.mapFiles.mapDirectoryName.equalsIgnoreCase(var1);
   }

   public void onNewChunkLoaded(IsoChunk var1) {
      for(int var2 = 0; var2 < this.basementPlacements.size(); ++var2) {
         BasementPlacement var3 = (BasementPlacement)this.basementPlacements.get(var2);
         int var4 = var3.x;
         int var5 = var3.y;
         int var6 = var3.w + 1;
         int var7 = var3.h + 1;
         if (this.chunkOverlaps(var1, var4, var5, var6, var7)) {
            try {
               NewMapBinaryFile.SpawnBasementInChunk(var1, var3.name, var3.x, var3.y, var3.z);
            } catch (IOException var9) {
               ExceptionLogger.logException(var9);
            }
         }
      }

   }

   boolean chunkOverlaps(IsoChunk var1, int var2, int var3, int var4, int var5) {
      byte var6 = 8;
      int var7 = var1.wx * var6;
      int var8 = var1.wy * var6;
      return var2 + var4 > var7 && var2 < var7 + var6 && var3 + var5 > var8 && var3 < var8 + var6;
   }

   void loadBasementDefinitionHeaders() {
      NewMapBinaryFile var1 = new NewMapBinaryFile(true);

      for(int var2 = 0; var2 < this.basementDefinitions.size(); ++var2) {
         BasementDefinition var3 = (BasementDefinition)this.basementDefinitions.get(var2);
         String var4 = "media/binmap/" + var3.name + ".pzby";

         try {
            NewMapBinaryFile.Header var5 = var1.loadHeader(var4);
            if (var5.m_buildingDefList.size() != 1) {
               this.basementDefinitions.remove(var2--);
               this.basementDefinitionByName.remove(var3.name);
            } else {
               var3.header = var5;
               BuildingDef var6 = (BuildingDef)var5.m_buildingDefList.get(0);
               var3.width = var6.getW();
               var3.height = var6.getH();
            }
         } catch (FileNotFoundException var7) {
            this.basementDefinitions.remove(var2--);
            this.basementDefinitionByName.remove(var3.name);
         } catch (IOException var8) {
            this.basementDefinitions.remove(var2--);
            this.basementDefinitionByName.remove(var3.name);
         }
      }

   }

   void loadBasementAccessDefinitionHeaders() {
      NewMapBinaryFile var1 = new NewMapBinaryFile(true);

      for(int var2 = 0; var2 < this.basementAccessDefinitions.size(); ++var2) {
         BasementDefinition var3 = (BasementDefinition)this.basementAccessDefinitions.get(var2);
         String var4 = "media/basement_access/" + var3.name + ".pzby";

         try {
            NewMapBinaryFile.Header var5 = var1.loadHeader(var4);
            var3.header = var5;
         } catch (FileNotFoundException var6) {
            this.basementAccessDefinitions.remove(var2--);
            this.basementAccessDefinitionByName.remove(var3.name);
         } catch (IOException var7) {
            this.basementAccessDefinitions.remove(var2--);
            this.basementAccessDefinitionByName.remove(var3.name);
         }
      }

   }

   void createBasementBuildingDefs() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.basementPlacements.size(); ++var2) {
         BasementPlacement var3 = (BasementPlacement)this.basementPlacements.get(var2);
         BasementDefinition var4 = (BasementDefinition)this.basementDefinitionByName.get(var3.name);
         if (var4 == null) {
            var4 = (BasementDefinition)this.basementAccessDefinitionByName.get(var3.name);
         }

         if (var4 != null) {
            NewMapBinaryFile.Header var5 = var4.header;
            if (!var5.m_buildingDefList.isEmpty()) {
               int var6 = var3.x / IsoCell.CellSizeInSquares;
               int var7 = var3.y / IsoCell.CellSizeInSquares;
               IsoMetaCell var8 = IsoWorld.instance.MetaGrid.getCellData(var6, var7);
               if (var8 != null) {
                  BuildingDef var9 = new BuildingDef();
                  var9.ID = (long)this.buildingDefs.size();
                  int var10 = var3.z;

                  for(int var11 = 0; var11 < var5.m_roomDefList.size(); ++var11) {
                     RoomDef var12 = (RoomDef)var5.m_roomDefList.get(var11);
                     long var13 = (long)var9.rooms.size();
                     RoomDef var15 = new RoomDef(var13, var12.name);
                     var15.level = var10 + var12.level;
                     Iterator var16 = var12.rects.iterator();

                     while(var16.hasNext()) {
                        RoomDef.RoomRect var17 = (RoomDef.RoomRect)var16.next();
                        var15.rects.add(new RoomDef.RoomRect(var3.x + var17.x, var3.y + var17.y, var17.getW(), var17.getH()));
                     }

                     var15.CalculateBounds();
                     var15.building = var9;
                     if (var12.isEmptyOutside()) {
                        var9.emptyoutside.add(var15);
                     } else {
                        var9.rooms.add(var15);
                     }
                  }

                  var9.CalculateBounds(var1);
                  this.buildingDefs.add(var9);
               }
            }
         }
      }

   }

   private BuildingDef getBuildingToMergeWith(BuildingDef var1) {
      BuildingDef var2 = null;
      if (var1.getMinLevel() < 0) {
         return var2;
      } else {
         this.tempBuildingDefs.clear();
         IsoWorld.instance.MetaGrid.getBuildingsIntersecting(var1.getX() - 1, var1.getY() - 1, var1.getW() + 2, var1.getH() + 2, this.tempBuildingDefs);

         for(int var3 = 0; var3 < this.tempBuildingDefs.size(); ++var3) {
            BuildingDef var4 = (BuildingDef)this.tempBuildingDefs.get(var3);
            if (var4.getMinLevel() >= 0 && var1.isAdjacent(var4)) {
               if (var2 != null) {
                  return null;
               }

               var2 = var4;
            }
         }

         return var2;
      }
   }

   private void removeBuildingFromLotHeader(BuildingDef var1, LotHeader var2) {
      var2.Buildings.remove(var1);
      var2.BuildingByMetaID.remove(var1.metaID);

      assert !var2.isoBuildings.containsKey(var1.ID);

      int var3;
      RoomDef var4;
      for(var3 = 0; var3 < var1.rooms.size(); ++var3) {
         var4 = (RoomDef)var1.rooms.get(var3);
         var2.RoomList.remove(var4);
         var2.RoomByMetaID.remove(var4.metaID);

         assert !var2.isoRooms.containsKey(var4.ID);
      }

      for(var3 = 0; var3 < var1.emptyoutside.size(); ++var3) {
         var4 = (RoomDef)var1.emptyoutside.get(var3);
         var2.RoomList.remove(var4);
         var2.RoomByMetaID.remove(var4.metaID);

         assert !var2.isoRooms.containsKey(var4.ID);
      }

   }

   private void mergeRoomsOntoLotHeader(ArrayList<RoomDef> var1, LotHeader var2) {
      for(int var3 = 0; var3 < var1.size(); ++var3) {
         RoomDef var4 = (RoomDef)var1.get(var3);
         var4.ID = RoomID.makeID(var2.cellX, var2.cellY, var2.RoomList.size());
         var4.metaID = var4.calculateMetaID(var2.cellX, var2.cellY);
         var2.Rooms.put(var4.ID, var4);
         var2.RoomList.add(var4);
         if (var2.RoomByMetaID.contains(var4.metaID)) {
            DebugLog.General.error("duplicate RoomDef.metaID for room at %d,%d,%d", var4.x, var4.y, var4.level);
         }

         var2.RoomByMetaID.put(var4.metaID, var4);
      }

   }

   private void mergeBuildings(BuildingDef var1, BuildingDef var2) {
      int var3 = var1.x / IsoCell.CellSizeInSquares;
      int var4 = var1.y / IsoCell.CellSizeInSquares;
      int var5 = var2.x / IsoCell.CellSizeInSquares;
      int var6 = var2.y / IsoCell.CellSizeInSquares;
      int var7 = PZMath.min(var3, var5);
      int var8 = PZMath.min(var4, var6);
      String var9 = ChunkMapFilenames.instance.getHeader(var7, var8);
      LotHeader var10 = (LotHeader)IsoLot.InfoHeaders.get(var9);
      if (var10 != null) {
         if (var7 != var5 || var8 != var6) {
            String var11 = ChunkMapFilenames.instance.getHeader(var5, var6);
            LotHeader var12 = (LotHeader)IsoLot.InfoHeaders.get(var11);
            if (var12 == null) {
               return;
            }

            this.removeBuildingFromLotHeader(var2, var12);
            var2.ID = BuildingID.makeID(var10.cellX, var10.cellY, var10.Buildings.size());
            var10.Buildings.add(var2);
         }

         this.mergeRoomsOntoLotHeader(var1.rooms, var10);
         this.mergeRoomsOntoLotHeader(var1.emptyoutside, var10);
         MergedRooms var13 = (MergedRooms)this.mergedRooms.get(var2);
         if (var13 == null) {
            var13 = new MergedRooms();
            var13.buildingDef = var2;
            this.mergedRooms.put(var2, var13);
         }

         var13.rooms.addAll(var1.rooms);
         var13.emptyoutside.addAll(var1.emptyoutside);
         var10.BuildingByMetaID.remove(var2.metaID);
         var2.addRoomsOf(var1, this.tempRooms);
         var2.metaID = var2.calculateMetaID(var7, var8);
         var10.BuildingByMetaID.put(var2.metaID, var2);
      }
   }

   void addBasementBuildingDefsToLotHeaders() {
      this.mergedRooms.clear();

      for(int var1 = 0; var1 < this.buildingDefs.size(); ++var1) {
         BuildingDef var2 = (BuildingDef)this.buildingDefs.get(var1);
         int var3 = var2.x / IsoCell.CellSizeInSquares;
         int var4 = var2.y / IsoCell.CellSizeInSquares;
         String var5 = ChunkMapFilenames.instance.getHeader(var3, var4);
         LotHeader var6 = (LotHeader)IsoLot.InfoHeaders.get(var5);
         if (var6 != null) {
            BuildingDef var7 = this.getBuildingToMergeWith(var2);
            if (var7 != null) {
               this.mergeBuildings(var2, var7);
               this.buildingDefs.remove(var1--);
            } else {
               int var8 = var6.Buildings.size();
               var2.ID = BuildingID.makeID(var3, var4, var8);
               var2.metaID = var2.calculateMetaID(var3, var4);
               this.mergeRoomsOntoLotHeader(var2.rooms, var6);
               var6.Buildings.add(var2);
               if (var6.BuildingByMetaID.contains(var2.metaID)) {
                  DebugLog.General.error("duplicate BuildingDef.metaID for building at %d,%d", var2.x, var2.y);
               }

               var6.BuildingByMetaID.put(var2.metaID, var2);
            }
         }
      }

   }

   void addBasementBuildingDefsToMetaGrid() {
      IsoMetaGrid var1 = IsoWorld.instance.getMetaGrid();

      int var5;
      for(int var2 = 0; var2 < this.buildingDefs.size(); ++var2) {
         BuildingDef var3 = (BuildingDef)this.buildingDefs.get(var2);
         int var4 = var3.x / IsoCell.CellSizeInSquares;
         var5 = var3.y / IsoCell.CellSizeInSquares;
         IsoMetaCell var6 = var1.getCellData(var4, var5);
         if (var6 != null) {
            var6.addRooms(var3.rooms, var4 * IsoCell.CellSizeInSquares, var5 * IsoCell.CellSizeInSquares);
            var6.addRooms(var3.emptyoutside, var4 * IsoCell.CellSizeInSquares, var5 * IsoCell.CellSizeInSquares);
            var1.addRoomsToAdjacentCells(var3);
            if (!var1.Buildings.contains(var3)) {
               var1.Buildings.add(var3);
            }
         }
      }

      Iterator var8 = this.mergedRooms.values().iterator();

      while(var8.hasNext()) {
         MergedRooms var9 = (MergedRooms)var8.next();
         BuildingDef var10 = var9.buildingDef;
         var5 = var10.x / IsoCell.CellSizeInSquares;
         int var11 = var10.y / IsoCell.CellSizeInSquares;
         IsoMetaCell var7 = var1.getCellData(var5, var11);
         if (var7 != null) {
            var7.addRooms(var9.rooms, var5 * IsoCell.CellSizeInSquares, var11 * IsoCell.CellSizeInSquares);
            var7.addRooms(var9.emptyoutside, var5 * IsoCell.CellSizeInSquares, var11 * IsoCell.CellSizeInSquares);
            var1.addRoomsToAdjacentCells(var10, var9.rooms);
            var1.addRoomsToAdjacentCells(var10, var9.emptyoutside);
         }
      }

      this.mergedRooms.clear();
   }

   private static final class MergedRooms {
      BuildingDef buildingDef;
      final ArrayList<RoomDef> rooms = new ArrayList();
      final ArrayList<RoomDef> emptyoutside = new ArrayList();

      private MergedRooms() {
      }
   }
}
