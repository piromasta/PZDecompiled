package zombie.iso;

import gnu.trove.list.array.TIntArrayList;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ChunkMapFilenames;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.MapGroups;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalZone;
import zombie.characters.animals.AnimalZoneJunction;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.debug.DebugLog;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.enums.MetaCellPresence;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.worldgen.WGChunk;
import zombie.iso.worldgen.WGParams;
import zombie.iso.worldgen.attachments.AttachmentsHandler;
import zombie.iso.worldgen.blending.Blending;
import zombie.iso.worldgen.maps.BiomeMap;
import zombie.iso.worldgen.zones.WorldGenZone;
import zombie.iso.worldgen.zones.ZoneGenerator;
import zombie.iso.zones.RoomTone;
import zombie.iso.zones.VehicleZone;
import zombie.iso.zones.Zone;
import zombie.iso.zones.ZoneGeometryType;
import zombie.iso.zones.ZoneHandler;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.randomizedWorld.randomizedBuilding.RBBasic;
import zombie.util.BufferedRandomAccessFile;
import zombie.util.SharedStrings;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.RegExFilenameFilter;
import zombie.util.lambda.QuadConsumer;
import zombie.vehicles.ClipperOffset;

public final class IsoMetaGrid {
   private static final int NUM_LOADER_THREADS = 8;
   public static ClipperOffset s_clipperOffset = null;
   public static ByteBuffer s_clipperBuffer = null;
   private static final ThreadLocal<ArrayList<Zone>> TL_ZoneList = ThreadLocal.withInitial(ArrayList::new);
   public static final ThreadLocal<IsoGameCharacter.Location> TL_Location = ThreadLocal.withInitial(IsoGameCharacter.Location::new);
   static Rectangle a = new Rectangle();
   static Rectangle b = new Rectangle();
   static ArrayList<RoomDef> roomChoices = new ArrayList(50);
   private final ArrayList<RoomDef> tempRooms = new ArrayList();
   private final ArrayList<Zone> tempZones1 = new ArrayList();
   private final ArrayList<Zone> tempZones2 = new ArrayList();
   private final MetaGridLoaderThread[] threads = new MetaGridLoaderThread[8];
   public int minX = 10000000;
   public int minY = 10000000;
   public int maxX = -10000000;
   public int maxY = -10000000;
   public int minNonProceduralX;
   public int minNonProceduralY;
   public int maxNonProceduralX;
   public int maxNonProceduralY;
   public final ArrayList<Zone> Zones = new ArrayList();
   public final ArrayList<BuildingDef> Buildings = new ArrayList();
   public final ArrayList<VehicleZone> VehiclesZones = new ArrayList();
   public final ZoneHandler<AnimalZone> animalZoneHandler = new ZoneHandler();
   private IsoMetaCell[][] Grid;
   private Set<IsoMetaCell> cellsToSave = new HashSet();
   public final ArrayList<IsoGameCharacter> MetaCharacters = new ArrayList();
   final ArrayList<Vector2> HighZombieList = new ArrayList();
   private int width;
   private int height;
   private final SharedStrings sharedStrings = new SharedStrings();
   private long createStartTime;
   private boolean bLoaded = false;

   public IsoMetaGrid() {
   }

   public IsoMetaCell getCell(int var1, int var2) {
      return this.Grid[var1][var2];
   }

   public IsoMetaCell getCellOrCreate(int var1, int var2) {
      if (!this.hasCell(var1, var2)) {
         IsoMetaCell var3 = new IsoMetaCell(this.minX + var1, this.minY + var2);
         this.setCell(var1, var2, var3);
      }

      return this.getCell(var1, var2);
   }

   public void setCell(int var1, int var2, IsoMetaCell var3) {
      if (!Core.bDebug || var3 == null || var3.getX() == this.minX + var1 && var3.getY() == this.minY + var2) {
         this.Grid[var1][var2] = var3;
      } else {
         throw new IllegalArgumentException("invalid IsoMetaCell coordinates");
      }
   }

   public boolean hasCell(int var1, int var2) {
      return this.Grid[var1][var2] != null;
   }

   public int gridX() {
      return this.Grid.length;
   }

   public int gridY() {
      return this.Grid[0].length;
   }

   public void AddToMeta(IsoGameCharacter var1) {
      IsoWorld.instance.CurrentCell.Remove(var1);
      if (!this.MetaCharacters.contains(var1)) {
         this.MetaCharacters.add(var1);
      }

   }

   public void RemoveFromMeta(IsoPlayer var1) {
      this.MetaCharacters.remove(var1);
      if (!IsoWorld.instance.CurrentCell.getObjectList().contains(var1)) {
         IsoWorld.instance.CurrentCell.getObjectList().add(var1);
      }

   }

   public int getMinX() {
      return this.minX;
   }

   public int getMinY() {
      return this.minY;
   }

   public int getMaxX() {
      return this.maxX;
   }

   public int getMaxY() {
      return this.maxY;
   }

   public Zone getZoneAt(int var1, int var2, int var3) {
      IsoMetaChunk var4 = this.getChunkDataFromTile(var1, var2);
      return var4 != null ? var4.getZoneAt(var1, var2, var3) : null;
   }

   public ArrayList<Zone> getZonesAt(int var1, int var2, int var3) {
      return this.getZonesAt(var1, var2, var3, new ArrayList());
   }

   public ArrayList<Zone> getZonesAt(int var1, int var2, int var3, ArrayList<Zone> var4) {
      IsoMetaChunk var5 = this.getChunkDataFromTile(var1, var2);
      return var5 != null ? var5.getZonesAt(var1, var2, var3, var4) : var4;
   }

   public ArrayList<Zone> getZonesIntersecting(int var1, int var2, int var3, int var4, int var5) {
      ArrayList var6 = new ArrayList();
      return this.getZonesIntersecting(var1, var2, var3, var4, var5, var6);
   }

   public ArrayList<Zone> getZonesIntersecting(int var1, int var2, int var3, int var4, int var5, ArrayList<Zone> var6) {
      for(int var7 = var2 / IsoCell.CellSizeInSquares; var7 <= (var2 + var5) / IsoCell.CellSizeInSquares; ++var7) {
         for(int var8 = var1 / IsoCell.CellSizeInSquares; var8 <= (var1 + var4) / IsoCell.CellSizeInSquares; ++var8) {
            if (var8 >= this.minX && var8 <= this.maxX && var7 >= this.minY && var7 <= this.maxY && this.hasCell(var8 - this.minX, var7 - this.minY)) {
               this.getCell(var8 - this.minX, var7 - this.minY).getZonesIntersecting(var1, var2, var3, var4, var5, var6);
            }
         }
      }

      return var6;
   }

   public Zone getZoneWithBoundsAndType(int var1, int var2, int var3, int var4, int var5, String var6) {
      ArrayList var7 = (ArrayList)TL_ZoneList.get();
      var7.clear();
      this.getZonesIntersecting(var1, var2, var3, var4, var5, var7);

      for(int var8 = 0; var8 < var7.size(); ++var8) {
         Zone var9 = (Zone)var7.get(var8);
         if (var9.x == var1 && var9.y == var2 && var9.z == var3 && var9.w == var4 && var9.h == var5 && StringUtils.equalsIgnoreCase(var9.type, var6)) {
            return var9;
         }
      }

      return null;
   }

   public VehicleZone getVehicleZoneAt(int var1, int var2, int var3) {
      IsoMetaCell var4 = this.getMetaGridFromTile(var1, var2);
      if (var4 != null && !var4.vehicleZones.isEmpty()) {
         for(int var5 = 0; var5 < var4.vehicleZones.size(); ++var5) {
            VehicleZone var6 = (VehicleZone)var4.vehicleZones.get(var5);
            if (var6.contains(var1, var2, var3)) {
               return var6;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public BuildingDef getBuildingAt(int var1, int var2) {
      for(int var3 = 0; var3 < this.Buildings.size(); ++var3) {
         BuildingDef var4 = (BuildingDef)this.Buildings.get(var3);
         if (var4.x <= var1 && var4.y <= var2 && var4.getW() > var1 - var4.x && var4.getH() > var2 - var4.y) {
            return var4;
         }
      }

      return null;
   }

   public ArrayList<BuildingDef> getBuildings() {
      return this.Buildings;
   }

   public BuildingDef getAssociatedBuildingAt(int var1, int var2) {
      IsoMetaChunk var3 = this.getChunkDataFromTile(var1, var2);
      BuildingDef var4 = null;
      if (var3 != null) {
         var4 = var3.getAssociatedBuildingAt(var1, var2);
         if (var4 != null) {
            return var4;
         }
      }

      byte var5 = 8;
      int var6 = PZMath.coordmodulo(var1, var5);
      int var7 = PZMath.coordmodulo(var2, var5);
      if (var6 == var5 - 1) {
         var4 = this.getAssociatedBuildingAt(var1, var2, IsoDirections.E);
         if (var4 != null) {
            return var4;
         }
      }

      if (var6 == 0) {
         var4 = this.getAssociatedBuildingAt(var1, var2, IsoDirections.W);
         if (var4 != null) {
            return var4;
         }
      }

      if (var7 == 0) {
         var4 = this.getAssociatedBuildingAt(var1, var2, IsoDirections.N);
         if (var4 != null) {
            return var4;
         }
      }

      if (var6 == 0 || var7 == 0) {
         var4 = this.getAssociatedBuildingAt(var1, var2, IsoDirections.NW);
         if (var4 != null) {
            return var4;
         }
      }

      if (var6 == var5 - 1 || var7 == var5 - 1) {
         var4 = this.getAssociatedBuildingAt(var1, var2, IsoDirections.SE);
         if (var4 != null) {
            return var4;
         }
      }

      if (var6 == 0 || var7 == var5 - 1) {
         var4 = this.getAssociatedBuildingAt(var1, var2, IsoDirections.SW);
         if (var4 != null) {
            return var4;
         }
      }

      if (var6 == var5 - 1 || var7 == 0) {
         var4 = this.getAssociatedBuildingAt(var1, var2, IsoDirections.NE);
         if (var4 != null) {
            return var4;
         }
      }

      return null;
   }

   private BuildingDef getAssociatedBuildingAt(int var1, int var2, IsoDirections var3) {
      IsoMetaChunk var4 = this.getChunkDataFromTile(var1 + var3.dx(), var2 + var3.dy());
      return var4 == null ? null : var4.getAssociatedBuildingAt(var1, var2);
   }

   public BuildingDef getBuildingAtRelax(int var1, int var2) {
      for(int var3 = 0; var3 < this.Buildings.size(); ++var3) {
         BuildingDef var4 = (BuildingDef)this.Buildings.get(var3);
         if (var4.x <= var1 + 1 && var4.y <= var2 + 1 && var4.getW() > var1 - var4.x - 1 && var4.getH() > var2 - var4.y - 1) {
            return var4;
         }
      }

      return null;
   }

   public RoomDef getRoomAt(int var1, int var2, int var3) {
      IsoMetaChunk var4 = this.getChunkDataFromTile(var1, var2);
      return var4 != null ? var4.getRoomAt(var1, var2, var3) : null;
   }

   public RoomDef getEmptyOutsideAt(int var1, int var2, int var3) {
      IsoMetaChunk var4 = this.getChunkDataFromTile(var1, var2);
      return var4 != null ? var4.getEmptyOutsideAt(var1, var2, var3) : null;
   }

   public IsoRoom getRoomByID(long var1) {
      int var3 = RoomID.getCellX(var1);
      int var4 = RoomID.getCellY(var1);
      String var5 = ChunkMapFilenames.instance.getHeader(var3, var4);
      LotHeader var6 = (LotHeader)IsoLot.InfoHeaders.get(var5);
      return var6 == null ? null : var6.getRoom(var1);
   }

   public void getBuildingsIntersecting(int var1, int var2, int var3, int var4, ArrayList<BuildingDef> var5) {
      for(int var6 = var2 / IsoCell.CellSizeInSquares; var6 <= (var2 + this.height) / IsoCell.CellSizeInSquares; ++var6) {
         for(int var7 = var1 / IsoCell.CellSizeInSquares; var7 <= (var1 + this.width) / IsoCell.CellSizeInSquares; ++var7) {
            if (var7 >= this.minX && var7 <= this.maxX && var6 >= this.minY && var6 <= this.maxY) {
               IsoMetaCell var8 = this.getCell(var7 - this.minX, var6 - this.minY);
               if (var8 != null) {
                  var8.getBuildingsIntersecting(var1, var2, var3, var4, var5);
               }
            }
         }
      }

   }

   public void getRoomsIntersecting(int var1, int var2, int var3, int var4, ArrayList<RoomDef> var5) {
      for(int var6 = var2 / IsoCell.CellSizeInSquares; var6 <= (var2 + this.height) / IsoCell.CellSizeInSquares; ++var6) {
         for(int var7 = var1 / IsoCell.CellSizeInSquares; var7 <= (var1 + this.width) / IsoCell.CellSizeInSquares; ++var7) {
            if (var7 >= this.minX && var7 <= this.maxX && var6 >= this.minY && var6 <= this.maxY) {
               IsoMetaCell var8 = this.getCell(var7 - this.minX, var6 - this.minY);
               if (var8 != null) {
                  var8.getRoomsIntersecting(var1, var2, var3, var4, var5);
               }
            }
         }
      }

   }

   public int countRoomsIntersecting(int var1, int var2, int var3, int var4) {
      this.tempRooms.clear();

      for(int var5 = var2 / IsoCell.CellSizeInSquares; var5 <= (var2 + this.height) / IsoCell.CellSizeInSquares; ++var5) {
         for(int var6 = var1 / IsoCell.CellSizeInSquares; var6 <= (var1 + this.width) / IsoCell.CellSizeInSquares; ++var6) {
            if (var6 >= this.minX && var6 <= this.maxX && var5 >= this.minY && var5 <= this.maxY) {
               IsoMetaCell var7 = this.getCell(var6 - this.minX, var5 - this.minY);
               if (var7 != null) {
                  var7.getRoomsIntersecting(var1, var2, var3, var4, this.tempRooms);
               }
            }
         }
      }

      return this.tempRooms.size();
   }

   public int countNearbyBuildingsRooms(IsoPlayer var1) {
      int var2 = PZMath.fastfloor(var1.getX()) - 20;
      int var3 = PZMath.fastfloor(var1.getY()) - 20;
      byte var4 = 40;
      byte var5 = 40;
      int var6 = this.countRoomsIntersecting(var2, var3, var4, var5);
      return var6;
   }

   private boolean isInside(Zone var1, BuildingDef var2) {
      a.x = var1.x;
      a.y = var1.y;
      a.width = var1.w;
      a.height = var1.h;
      b.x = var2.x;
      b.y = var2.y;
      b.width = var2.getW();
      b.height = var2.getH();
      return a.contains(b);
   }

   private boolean isAdjacent(Zone var1, Zone var2) {
      if (var1 == var2) {
         return false;
      } else {
         a.x = var1.x;
         a.y = var1.y;
         a.width = var1.w;
         a.height = var1.h;
         b.x = var2.x;
         b.y = var2.y;
         b.width = var2.w;
         b.height = var2.h;
         --a.x;
         --a.y;
         Rectangle var10000 = a;
         var10000.width += 2;
         var10000 = a;
         var10000.height += 2;
         --b.x;
         --b.y;
         var10000 = b;
         var10000.width += 2;
         var10000 = b;
         var10000.height += 2;
         return a.intersects(b);
      }
   }

   public Zone registerZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7) {
      return this.registerZone(var1, var2, var3, var4, var5, var6, var7, ZoneGeometryType.INVALID, (TIntArrayList)null, 0);
   }

   public Zone registerZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, ZoneGeometryType var8, TIntArrayList var9, int var10) {
      var1 = this.sharedStrings.get(var1);
      var2 = this.sharedStrings.get(var2);
      Zone var11 = new Zone(var1, var2, var3, var4, var5, var6, var7);
      var11.geometryType = var8;
      if (var9 != null) {
         var11.points.addAll(var9);
         var11.polylineWidth = var10;
      }

      var11.isPreferredZoneForSquare = Zone.isPreferredZoneForSquare(var2);
      if (var3 >= this.minX * IsoCell.CellSizeInSquares - 100 && var4 >= this.minY * IsoCell.CellSizeInSquares - 100 && var3 + var6 <= (this.maxX + 1) * IsoCell.CellSizeInSquares + 100 && var4 + var7 <= (this.maxY + 1) * IsoCell.CellSizeInSquares + 100 && var5 >= -32 && var5 <= 31 && var6 <= 1202 && var7 <= 1202) {
         this.addZone(var11);
         return var11;
      } else {
         DebugLog.log("111ERROR: not adding suspicious zone \"" + var1 + "\" \"" + var2 + "\" " + var3 + "," + var4 + "," + var5 + " " + var6 + "x" + var7);
         return var11;
      }
   }

   public Zone registerZone(Zone var1) {
      if (var1.x >= this.minX * IsoCell.CellSizeInSquares - 100 && var1.y >= this.minY * IsoCell.CellSizeInSquares - 100 && var1.x + this.width <= (this.maxX + 1) * IsoCell.CellSizeInSquares + 100 && var1.y + this.height <= (this.maxY + 1) * IsoCell.CellSizeInSquares + 100 && var1.z >= -32 && var1.z <= 31 && var1.w <= 1202 && var1.h <= 1202) {
         this.addZone(var1);
         return var1;
      } else {
         DebugLog.log("111ERROR: not adding suspicious zone \"" + var1.name + "\" \"" + var1.type + "\" " + var1.x + "," + var1.y + "," + var1.z + " " + var1.w + "x" + var1.h);
         return var1;
      }
   }

   public Zone registerGeometryZone(String var1, String var2, int var3, String var4, KahluaTable var5, KahluaTable var6) {
      int var7 = 2147483647;
      int var8 = 2147483647;
      int var9 = -2147483648;
      int var10 = -2147483648;
      TIntArrayList var11 = new TIntArrayList(var5.len());

      for(int var12 = 0; var12 < var5.len(); var12 += 2) {
         Object var13 = var5.rawget(var12 + 1);
         Object var14 = var5.rawget(var12 + 2);
         int var15 = ((Double)var13).intValue();
         int var16 = ((Double)var14).intValue();
         var11.add(var15);
         var11.add(var16);
         var7 = Math.min(var7, var15);
         var8 = Math.min(var8, var16);
         var9 = Math.max(var9, var15);
         var10 = Math.max(var10, var16);
      }

      ZoneGeometryType var10000;
      switch (var4) {
         case "point":
            var10000 = ZoneGeometryType.Point;
            break;
         case "polygon":
            var10000 = ZoneGeometryType.Polygon;
            break;
         case "polyline":
            var10000 = ZoneGeometryType.Polyline;
            break;
         default:
            throw new IllegalArgumentException("unknown zone geometry type");
      }

      ZoneGeometryType var17 = var10000;
      Double var18 = var17 == ZoneGeometryType.Polyline && var6 != null ? (Double)Type.tryCastTo(var6.rawget("LineWidth"), Double.class) : null;
      if (var18 != null) {
         int[] var20 = new int[4];
         this.calculatePolylineOutlineBounds(var11, var18.intValue(), var20);
         var7 = var20[0];
         var8 = var20[1];
         var9 = var20[2];
         var10 = var20[3];
      }

      Zone var21;
      if (var2.equals("Animal")) {
         var21 = this.registerAnimalZone(var1, var2, var7, var8, var3, var9 - var7 + 1, var10 - var8 + 1, var6);
         if (var21 != null) {
            var21.geometryType = var17;
            var21.points.addAll(var11);
            var21.polylineWidth = var18 == null ? 0 : var18.intValue();
         }

         return var21;
      } else if (!var2.equals("Vehicle") && !var2.equals("ParkingStall")) {
         if (var2.equals("WorldGen")) {
            var21 = this.registerWorldGenZone(var1, var2, var7, var8, var3, var9 - var7 + 1, var10 - var8 + 1, var6);
            if (var21 != null) {
               var21.geometryType = var17;
               var21.points.addAll(var11);
               var21.polylineWidth = var18 == null ? 0 : var18.intValue();
            }

            return var21;
         } else {
            var21 = this.registerZone(var1, var2, var7, var8, var3, var9 - var7 + 1, var10 - var8 + 1, var17, var11, var18 == null ? 0 : var18.intValue());
            var11.clear();
            return var21;
         }
      } else {
         var21 = this.registerVehiclesZone(var1, var2, var7, var8, var3, var9 - var7 + 1, var10 - var8 + 1, var6);
         if (var21 != null) {
            var21.geometryType = var17;
            var21.points.addAll(var11);
            var21.polylineWidth = var18 == null ? 0 : var18.intValue();
         }

         return var21;
      }
   }

   private void calculatePolylineOutlineBounds(TIntArrayList var1, int var2, int[] var3) {
      if (s_clipperOffset == null) {
         s_clipperOffset = new ClipperOffset();
         s_clipperBuffer = ByteBuffer.allocateDirect(3072);
      }

      s_clipperOffset.clear();
      s_clipperBuffer.clear();
      float var4 = var2 % 2 == 0 ? 0.0F : 0.5F;

      int var5;
      for(var5 = 0; var5 < var1.size(); var5 += 2) {
         int var6 = var1.get(var5);
         int var7 = var1.get(var5 + 1);
         s_clipperBuffer.putFloat((float)var6 + var4);
         s_clipperBuffer.putFloat((float)var7 + var4);
      }

      s_clipperBuffer.flip();
      s_clipperOffset.addPath(var1.size() / 2, s_clipperBuffer, ClipperOffset.JoinType.jtMiter.ordinal(), ClipperOffset.EndType.etOpenButt.ordinal());
      s_clipperOffset.execute((double)((float)var2 / 2.0F));
      var5 = s_clipperOffset.getPolygonCount();
      if (var5 < 1) {
         DebugLog.General.warn("Failed to generate polyline outline");
      } else {
         s_clipperBuffer.clear();
         s_clipperOffset.getPolygon(0, s_clipperBuffer);
         short var14 = s_clipperBuffer.getShort();
         float var15 = 3.4028235E38F;
         float var8 = 3.4028235E38F;
         float var9 = -3.4028235E38F;
         float var10 = -3.4028235E38F;

         for(int var11 = 0; var11 < var14; ++var11) {
            float var12 = s_clipperBuffer.getFloat();
            float var13 = s_clipperBuffer.getFloat();
            var15 = PZMath.min(var15, var12);
            var8 = PZMath.min(var8, var13);
            var9 = PZMath.max(var9, var12);
            var10 = PZMath.max(var10, var13);
         }

         var3[0] = (int)PZMath.floor(var15);
         var3[1] = (int)PZMath.floor(var8);
         var3[2] = (int)PZMath.ceil(var9);
         var3[3] = (int)PZMath.ceil(var10);
      }
   }

   /** @deprecated */
   @Deprecated
   public Zone registerZoneNoOverlap(String var1, String var2, int var3, int var4, int var5, int var6, int var7) {
      if (var3 >= this.minX * IsoCell.CellSizeInSquares - 100 && var4 >= this.minY * IsoCell.CellSizeInSquares - 100 && var3 + var6 <= (this.maxX + 1) * IsoCell.CellSizeInSquares + 100 && var4 + var7 <= (this.maxY + 1) * IsoCell.CellSizeInSquares + 100 && var5 >= 0 && var5 < 8 && var6 <= 601 && var7 <= 601) {
         return this.registerZone(var1, var2, var3, var4, var5, var6, var7);
      } else {
         DebugLog.log("YYYYERROR: not adding suspicious zone \"" + var1 + "\" \"" + var2 + "\" " + var3 + "," + var4 + "," + var5 + " " + var6 + "x" + var7);
         return null;
      }
   }

   public void addZone(Zone var1) {
      this.Zones.add(var1);

      for(int var2 = var1.y / IsoCell.CellSizeInSquares; var2 <= (var1.y + var1.h) / IsoCell.CellSizeInSquares; ++var2) {
         for(int var3 = var1.x / IsoCell.CellSizeInSquares; var3 <= (var1.x + var1.w) / IsoCell.CellSizeInSquares; ++var3) {
            if (var3 >= this.minX && var3 <= this.maxX && var2 >= this.minY && var2 <= this.maxY) {
               this.getCellOrCreate(var3 - this.minX, var2 - this.minY).addZone(var1, var3 * IsoCell.CellSizeInSquares, var2 * IsoCell.CellSizeInSquares);
            }
         }
      }

   }

   public void removeZone(Zone var1) {
      this.Zones.remove(var1);

      for(int var2 = var1.y / IsoCell.CellSizeInSquares; var2 <= (var1.y + var1.h) / IsoCell.CellSizeInSquares; ++var2) {
         for(int var3 = var1.x / IsoCell.CellSizeInSquares; var3 <= (var1.x + var1.w) / IsoCell.CellSizeInSquares; ++var3) {
            if (var3 >= this.minX && var3 <= this.maxX && var2 >= this.minY && var2 <= this.maxY && this.hasCell(var3 - this.minX, var2 - this.minY)) {
               this.getCell(var3 - this.minX, var2 - this.minY).removeZone(var1);
            }
         }
      }

   }

   public void removeZonesForCell(int var1, int var2) {
      IsoMetaCell var3 = this.getCellData(var1, var2);
      if (var3 != null) {
         ArrayList var4 = this.tempZones1;
         var4.clear();

         int var5;
         for(var5 = 0; var5 < IsoCell.CellSizeInChunks * IsoCell.CellSizeInChunks; ++var5) {
            if (var3.hasChunk(var5)) {
               var3.getChunk(var5).getZonesIntersecting(var1 * IsoCell.CellSizeInSquares, var2 * IsoCell.CellSizeInSquares, 0, IsoCell.CellSizeInSquares, IsoCell.CellSizeInSquares, var4);
            }
         }

         for(var5 = 0; var5 < var4.size(); ++var5) {
            Zone var6 = (Zone)var4.get(var5);
            ArrayList var7 = this.tempZones2;
            if (var6.difference(var1 * IsoCell.CellSizeInSquares, var2 * IsoCell.CellSizeInSquares, 0, IsoCell.CellSizeInSquares, IsoCell.CellSizeInSquares, var7)) {
               this.removeZone(var6);

               for(int var8 = 0; var8 < var7.size(); ++var8) {
                  this.addZone((Zone)var7.get(var8));
               }
            }
         }

         if (!var3.vehicleZones.isEmpty()) {
            var3.vehicleZones.clear();
         }

         if (!var3.mannequinZones.isEmpty()) {
            var3.mannequinZones.clear();
         }

         if (var3.worldGenZones != null && !var3.worldGenZones.isEmpty()) {
            var3.worldGenZones.clear();
         }

      }
   }

   public void removeZonesForLotDirectory(String var1) {
      if (!this.Zones.isEmpty()) {
         File var2 = new File(ZomboidFileSystem.instance.getDirectoryString("media/maps/" + var1 + "/"));
         if (var2.isDirectory()) {
            ChooseGameInfo.Map var3 = ChooseGameInfo.getMapDetails(var1);
            if (var3 != null) {
               String[] var4 = var2.list();
               if (var4 != null) {
                  for(int var5 = 0; var5 < var4.length; ++var5) {
                     String var6 = var4[var5];
                     if (var6.endsWith(".lotheader")) {
                        String[] var7 = var6.split("_");
                        var7[1] = var7[1].replace(".lotheader", "");
                        int var8 = Integer.parseInt(var7[0].trim());
                        int var9 = Integer.parseInt(var7[1].trim());
                        this.removeZonesForCell(var8, var9);
                     }
                  }

               }
            }
         }
      }
   }

   public void processZones() {
      int var1 = 0;

      for(int var2 = this.minX; var2 <= this.maxX; ++var2) {
         for(int var3 = this.minY; var3 <= this.maxY; ++var3) {
            if (this.hasCell(var2 - this.minX, var3 - this.minY)) {
               IsoMetaCell var4 = this.getCell(var2 - this.minX, var3 - this.minY);

               for(int var5 = 0; var5 < IsoCell.CellSizeInChunks; ++var5) {
                  for(int var6 = 0; var6 < IsoCell.CellSizeInChunks; ++var6) {
                     if (var4.hasChunk(var6, var5)) {
                        var1 = Math.max(var1, var4.getChunk(var6, var5).getZonesSize());
                     }
                  }
               }
            }
         }
      }

      DebugLog.log("Max #ZONES on one chunk is " + var1);
   }

   public Zone registerVehiclesZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
      if (!var2.equals("Vehicle") && !var2.equals("ParkingStall")) {
         return null;
      } else {
         var1 = this.sharedStrings.get(var1);
         var2 = this.sharedStrings.get(var2);
         VehicleZone var9 = new VehicleZone(var1, var2, var3, var4, var5, var6, var7, var8);
         this.VehiclesZones.add(var9);
         int var10 = (int)Math.ceil((double)((float)(var9.x + var9.w) / (float)IsoCell.CellSizeInSquares));
         int var11 = (int)Math.ceil((double)((float)(var9.y + var9.h) / (float)IsoCell.CellSizeInSquares));

         for(int var12 = var9.y / IsoCell.CellSizeInSquares; var12 < var11; ++var12) {
            for(int var13 = var9.x / IsoCell.CellSizeInSquares; var13 < var10; ++var13) {
               if (var13 >= this.minX && var13 <= this.maxX && var12 >= this.minY && var12 <= this.maxY) {
                  this.getCellOrCreate(var13 - this.minX, var12 - this.minY).vehicleZones.add(var9);
               }
            }
         }

         return var9;
      }
   }

   public Zone registerWorldGenZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
      if (!var2.equals("WorldGen")) {
         return null;
      } else {
         var1 = this.sharedStrings.get(var1);
         var2 = this.sharedStrings.get(var2);
         WorldGenZone var9 = new WorldGenZone(var1, var2, var3, var4, var5, var6, var7, var8);
         int var10 = (int)Math.ceil((double)((float)(var9.x + var9.w) / (float)IsoCell.CellSizeInSquares));
         int var11 = (int)Math.ceil((double)((float)(var9.y + var9.h) / (float)IsoCell.CellSizeInSquares));

         for(int var12 = var9.y / IsoCell.CellSizeInSquares; var12 < var11; ++var12) {
            for(int var13 = var9.x / IsoCell.CellSizeInSquares; var13 < var10; ++var13) {
               if (var13 >= this.minX && var13 <= this.maxX && var12 >= this.minY && var12 <= this.maxY) {
                  IsoMetaCell var14 = this.getCellOrCreate(var13 - this.minX, var12 - this.minY);
                  if (var14.worldGenZones == null) {
                     var14.worldGenZones = new ArrayList();
                  }

                  var14.worldGenZones.add(var9);
               }
            }
         }

         return var9;
      }
   }

   public void checkVehiclesZones() {
      int var4 = 0;

      while(var4 < this.VehiclesZones.size()) {
         boolean var1 = true;

         for(int var5 = 0; var5 < var4; ++var5) {
            Zone var2 = (Zone)this.VehiclesZones.get(var4);
            Zone var3 = (Zone)this.VehiclesZones.get(var5);
            if (var2.getX() == var3.getX() && var2.getY() == var3.getY() && var2.h == var3.h && var2.w == var3.w) {
               var1 = false;
               DebugLog.Vehicle.debugln("checkVehiclesZones: ERROR! Zone '" + var2.name + "':'" + var2.type + "' (" + var2.x + ", " + var2.y + ") duplicate with Zone '" + var3.name + "':'" + var3.type + "' (" + var3.x + ", " + var3.y + ")");
               break;
            }
         }

         if (var1) {
            ++var4;
         } else {
            this.VehiclesZones.remove(var4);
         }
      }

   }

   public Zone registerAnimalZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
      if (!"Animal".equals(var2)) {
         return null;
      } else {
         var1 = this.sharedStrings.get(var1);
         var2 = this.sharedStrings.get(var2);
         return this.registerAnimalZone(new AnimalZone(var1, var2, var3, var4, var5, var6, var7, var8));
      }
   }

   public Zone registerAnimalZone(AnimalZone var1) {
      this.animalZoneHandler.addZone(var1);
      int var2 = (int)Math.ceil((double)((float)(var1.x + var1.w) / (float)IsoCell.CellSizeInSquares));
      int var3 = (int)Math.ceil((double)((float)(var1.y + var1.h) / (float)IsoCell.CellSizeInSquares));

      for(int var4 = var1.y / IsoCell.CellSizeInSquares; var4 < var3; ++var4) {
         for(int var5 = var1.x / IsoCell.CellSizeInSquares; var5 < var2; ++var5) {
            if (var5 >= this.minX && var5 <= this.maxX && var4 >= this.minY && var4 <= this.maxY) {
               IsoMetaCell var6 = this.getCellOrCreate(var5 - this.minX, var4 - this.minY);
               this.addCellToSave(var6);
               var6.addAnimalZone(var1);
            }
         }
      }

      return var1;
   }

   public Zone registerMannequinZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
      if (!"Mannequin".equals(var2)) {
         return null;
      } else {
         var1 = this.sharedStrings.get(var1);
         var2 = this.sharedStrings.get(var2);
         IsoMannequin.MannequinZone var9 = new IsoMannequin.MannequinZone(var1, var2, var3, var4, var5, var6, var7, var8);
         int var10 = (int)Math.ceil((double)((float)(var9.x + var9.w) / (float)IsoCell.CellSizeInSquares));
         int var11 = (int)Math.ceil((double)((float)(var9.y + var9.h) / (float)IsoCell.CellSizeInSquares));

         for(int var12 = var9.y / IsoCell.CellSizeInSquares; var12 < var11; ++var12) {
            for(int var13 = var9.x / IsoCell.CellSizeInSquares; var13 < var10; ++var13) {
               if (var13 >= this.minX && var13 <= this.maxX && var12 >= this.minY && var12 <= this.maxY) {
                  this.getCellOrCreate(var13 - this.minX, var12 - this.minY).mannequinZones.add(var9);
               }
            }
         }

         return var9;
      }
   }

   public void registerRoomTone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
      if ("RoomTone".equals(var2)) {
         IsoMetaCell var9 = this.getCellData(var3 / IsoCell.CellSizeInSquares, var4 / IsoCell.CellSizeInSquares);
         RoomTone var10 = new RoomTone();
         var10.x = var3;
         var10.y = var4;
         var10.z = var5;
         var10.enumValue = var8.getString("RoomTone");
         var10.entireBuilding = Boolean.TRUE.equals(var8.rawget("EntireBuilding"));
         var9.roomTones.add(var10);
      }
   }

   public boolean isZoneAbove(Zone var1, Zone var2, int var3, int var4, int var5) {
      if (var1 != null && var1 != var2) {
         ArrayList var6 = (ArrayList)TL_ZoneList.get();
         var6.clear();
         this.getZonesAt(var3, var4, var5, var6);
         return var6.indexOf(var1) > var6.indexOf(var2);
      } else {
         return false;
      }
   }

   public void save(ByteBuffer var1) {
      this.savePart(var1, 0, false);
      this.savePart(var1, 1, false);
   }

   public void savePart(ByteBuffer var1, int var2, boolean var3) {
      int var4;
      if (var2 == 0) {
         var1.put((byte)77);
         var1.put((byte)69);
         var1.put((byte)84);
         var1.put((byte)65);
         var1.putInt(219);
         var1.putInt(this.minX);
         var1.putInt(this.minY);
         var1.putInt(this.maxX);
         var1.putInt(this.maxY);

         for(var4 = 0; var4 < this.gridX(); ++var4) {
            for(int var5 = 0; var5 < this.gridY(); ++var5) {
               IsoMetaCell var6 = this.Grid[var4][var5];
               int var7 = 0;
               if (var6 != null && var6.info != null) {
                  var7 = var6.info.Rooms.values().size();
               }

               var1.putInt(var7);
               Iterator var8;
               short var11;
               if (var6 != null && var6.info != null) {
                  for(var8 = var6.info.Rooms.entrySet().iterator(); var8.hasNext(); var1.putShort(var11)) {
                     Map.Entry var9 = (Map.Entry)var8.next();
                     RoomDef var10 = (RoomDef)var9.getValue();
                     var1.putLong(var10.metaID);
                     var11 = 0;
                     if (var10.bExplored) {
                        var11 = (short)(var11 | 1);
                     }

                     if (var10.bLightsActive) {
                        var11 = (short)(var11 | 2);
                     }

                     if (var10.bDoneSpawn) {
                        var11 = (short)(var11 | 4);
                     }

                     if (var10.isRoofFixed()) {
                        var11 = (short)(var11 | 8);
                     }
                  }
               }

               if (var6 != null && var6.info != null) {
                  var1.putInt(var6.info.Buildings.size());
               } else {
                  var1.putInt(0);
               }

               if (var6 != null && var6.info != null) {
                  var8 = var6.info.Buildings.iterator();

                  while(var8.hasNext()) {
                     BuildingDef var12 = (BuildingDef)var8.next();
                     var1.putLong(var12.metaID);
                     var1.put((byte)(var12.bAlarmed ? 1 : 0));
                     var1.putInt(var12.getKeyId());
                     var1.put((byte)(var12.seen ? 1 : 0));
                     var1.put((byte)(var12.isHasBeenVisited() ? 1 : 0));
                     var1.putInt(var12.lootRespawnHour);
                     var1.putInt(var12.bAlarmDecay);
                  }
               }
            }
         }

      } else {
         var1.putInt(SafeHouse.getSafehouseList().size());

         for(var4 = 0; var4 < SafeHouse.getSafehouseList().size(); ++var4) {
            ((SafeHouse)SafeHouse.getSafehouseList().get(var4)).save(var1);
         }

         var1.putInt(NonPvpZone.getAllZones().size());

         for(var4 = 0; var4 < NonPvpZone.getAllZones().size(); ++var4) {
            ((NonPvpZone)NonPvpZone.getAllZones().get(var4)).save(var1);
         }

         var1.putInt(Faction.getFactions().size());

         for(var4 = 0; var4 < Faction.getFactions().size(); ++var4) {
            ((Faction)Faction.getFactions().get(var4)).save(var1);
         }

         var1.putInt(DesignationZone.allZones.size());

         for(var4 = 0; var4 < DesignationZone.allZones.size(); ++var4) {
            ((DesignationZone)DesignationZone.allZones.get(var4)).save(var1);
         }

         if (GameServer.bServer) {
            var4 = var1.position();
            var1.putInt(0);
            StashSystem.save(var1);
            var1.putInt(var4, var1.position());
         } else if (!GameClient.bClient) {
            StashSystem.save(var1);
         }

         var1.putInt(RBBasic.getUniqueRDSSpawned().size());

         for(var4 = 0; var4 < RBBasic.getUniqueRDSSpawned().size(); ++var4) {
            GameWindow.WriteString(var1, (String)RBBasic.getUniqueRDSSpawned().get(var4));
         }

      }
   }

   public void load() {
      File var1 = ZomboidFileSystem.instance.getFileInCurrentSave("map_meta.bin");

      try {
         FileInputStream var2 = new FileInputStream(var1);

         try {
            BufferedInputStream var3 = new BufferedInputStream(var2);

            try {
               int var5;
               synchronized(SliceY.SliceBufferLock) {
                  SliceY.SliceBuffer.clear();
                  var5 = var3.read(SliceY.SliceBuffer.array());
                  SliceY.SliceBuffer.limit(var5);
                  this.load(SliceY.SliceBuffer);
               }

               this.bLoaded = true;

               for(int var4 = 0; var4 < this.height; ++var4) {
                  for(var5 = 0; var5 < this.width; ++var5) {
                     IsoMetaCell var6 = this.getCellDataAbs(var5, var4);
                     if (var6 != null && var6.info != null) {
                        var6.info.BuildingByMetaID.compact();
                        var6.info.RoomByMetaID.compact();
                     }
                  }
               }
            } catch (Throwable var10) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var10.addSuppressed(var8);
               }

               throw var10;
            }

            var3.close();
         } catch (Throwable var11) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var11.addSuppressed(var7);
            }

            throw var11;
         }

         var2.close();
      } catch (FileNotFoundException var12) {
      } catch (Exception var13) {
         ExceptionLogger.logException(var13);
      }

   }

   public void load(ByteBuffer var1) {
      var1.mark();
      byte var3 = var1.get();
      byte var4 = var1.get();
      byte var5 = var1.get();
      byte var6 = var1.get();
      int var2 = var1.getInt();
      int var7 = this.minX;
      int var8 = this.minY;
      int var9 = this.maxX;
      int var10 = this.maxY;
      var7 = var1.getInt();
      var8 = var1.getInt();
      var9 = var1.getInt();
      var10 = var1.getInt();
      int var11 = var9 - var7 + 1;
      int var12 = var10 - var8 + 1;
      if (var11 != this.gridX() || var12 != this.gridY()) {
         DebugLog.log("map_meta.bin world size (" + var11 + "x" + var12 + ") does not match the current map size (" + this.gridX() + "x" + this.gridY() + ")");
      }

      int var13 = 0;
      int var14 = 0;

      int var15;
      int var16;
      int var18;
      int var19;
      int var33;
      for(var15 = var7; var15 <= var9; ++var15) {
         for(var16 = var8; var16 <= var10; ++var16) {
            IsoMetaCell var17 = this.getCellData(var15, var16);
            var18 = var1.getInt();

            boolean var23;
            boolean var25;
            for(var19 = 0; var19 < var18; ++var19) {
               long var20 = var1.getLong();
               boolean var22 = false;
               var23 = false;
               boolean var24 = false;
               var25 = false;
               short var26 = var1.getShort();
               var22 = (var26 & 1) != 0;
               var23 = (var26 & 2) != 0;
               var24 = (var26 & 4) != 0;
               var25 = (var26 & 8) != 0;
               if (var17 != null && var17.info != null) {
                  RoomDef var27 = (RoomDef)var17.info.RoomByMetaID.get(var20);
                  if (var27 != null) {
                     var27.setExplored(var22);
                     var27.bLightsActive = var23;
                     var27.bDoneSpawn = var24;
                     var27.setRoofFixed(var25);
                  } else {
                     DebugLog.General.error("invalid room metaID #" + var20 + " in cell " + var15 + "," + var16 + " while reading map_meta.bin");
                  }
               }
            }

            var19 = var1.getInt();
            var13 += var19;

            for(var33 = 0; var33 < var19; ++var33) {
               long var21 = var1.getLong();
               var23 = var1.get() == 1;
               int var36 = var1.getInt();
               var25 = var1.get() == 1;
               boolean var37 = var1.get() == 1;
               int var38 = var1.getInt();
               int var28 = var2 >= 201 ? var1.getInt() : 0;
               if (var17 != null && var17.info != null) {
                  BuildingDef var29 = (BuildingDef)var17.info.BuildingByMetaID.get(var21);
                  if (var29 != null) {
                     if (var23) {
                        ++var14;
                     }

                     var29.bAlarmed = var23;
                     var29.setKeyId(var36);
                     var29.seen = var25;
                     var29.hasBeenVisited = var37;
                     var29.lootRespawnHour = var38;
                     var29.bAlarmDecay = var28;
                  } else {
                     DebugLog.General.error("invalid building metaID #" + var21 + " in cell " + var15 + "," + var16 + " while reading map_meta.bin");
                  }
               }
            }
         }
      }

      SafeHouse.clearSafehouseList();
      var15 = var1.getInt();

      for(var16 = 0; var16 < var15; ++var16) {
         SafeHouse.load(var1, var2);
      }

      NonPvpZone.nonPvpZoneList.clear();
      var16 = var1.getInt();

      int var30;
      for(var30 = 0; var30 < var16; ++var30) {
         NonPvpZone var31 = new NonPvpZone();
         var31.load(var1, var2);
         NonPvpZone.getAllZones().add(var31);
      }

      Faction.factions = new ArrayList();
      var30 = var1.getInt();

      for(var18 = 0; var18 < var30; ++var18) {
         Faction var32 = new Faction();
         var32.load(var1, var2);
         Faction.getFactions().add(var32);
      }

      var18 = var1.getInt();

      for(var19 = 0; var19 < var18; ++var19) {
         DesignationZone.load(var1, var2);
      }

      if (GameServer.bServer) {
         var19 = var1.getInt();
         StashSystem.load(var1, var2);
      } else if (GameClient.bClient) {
         var19 = var1.getInt();
         var1.position(var19);
      } else {
         StashSystem.load(var1, var2);
      }

      ArrayList var35 = RBBasic.getUniqueRDSSpawned();
      var35.clear();
      var33 = var1.getInt();

      for(int var34 = 0; var34 < var33; ++var34) {
         var35.add(GameWindow.ReadString(var1));
      }

   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public boolean wasLoaded() {
      return this.bLoaded;
   }

   public IsoMetaCell getCellData(int var1, int var2) {
      return var1 - this.minX >= 0 && var2 - this.minY >= 0 && var1 - this.minX < this.width && var2 - this.minY < this.height ? this.getCell(var1 - this.minX, var2 - this.minY) : null;
   }

   public MetaCellPresence hasCellData(int var1, int var2) {
      if (var1 - this.minX >= 0 && var2 - this.minY >= 0 && var1 - this.minX < this.width && var2 - this.minY < this.height) {
         return this.hasCell(var1 - this.minX, var2 - this.minY) ? MetaCellPresence.LOADED : MetaCellPresence.NOT_LOADED;
      } else {
         return MetaCellPresence.OUT_OF_BOUNDS;
      }
   }

   public void setCellData(int var1, int var2, IsoMetaCell var3) {
      if (var1 - this.minX >= 0 && var2 - this.minY >= 0 && var1 - this.minX < this.width && var2 - this.minY < this.height) {
         this.setCell(var1 - this.minX, var2 - this.minY, var3);
      }
   }

   public IsoMetaCell getCellDataAbs(int var1, int var2) {
      return this.getCell(var1, var2);
   }

   public IsoMetaCell getCurrentCellData() {
      int var1 = IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldX;
      int var2 = IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldY;
      float var3 = (float)var1;
      float var4 = (float)var2;
      var3 /= (float)IsoCell.CellSizeInSquares / 8.0F;
      var4 /= (float)IsoCell.CellSizeInSquares / 8.0F;
      if (var3 < 0.0F) {
         var3 = (float)PZMath.fastfloor(var3 - 1.0F);
      }

      if (var4 < 0.0F) {
         var4 = (float)PZMath.fastfloor(var4 - 1.0F);
      }

      var1 = PZMath.fastfloor(var3);
      var2 = PZMath.fastfloor(var4);
      return this.getCellData(var1, var2);
   }

   public IsoMetaCell getMetaGridFromTile(int var1, int var2) {
      int var3 = var1 / IsoCell.CellSizeInSquares;
      int var4 = var2 / IsoCell.CellSizeInSquares;
      return this.getCellData(var3, var4);
   }

   public IsoMetaChunk getCurrentChunkData() {
      int var1 = IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldX;
      int var2 = IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldY;
      float var3 = (float)var1;
      float var4 = (float)var2;
      var3 /= (float)IsoCell.CellSizeInSquares / 8.0F;
      var4 /= (float)IsoCell.CellSizeInSquares / 8.0F;
      if (var3 < 0.0F) {
         var3 = (float)(PZMath.fastfloor(var3) - 1);
      }

      if (var4 < 0.0F) {
         var4 = (float)(PZMath.fastfloor(var4) - 1);
      }

      var1 = PZMath.fastfloor(var3);
      var2 = PZMath.fastfloor(var4);
      return this.getCellData(var1, var2).getChunk(IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldX - var1 * IsoCell.CellSizeInChunks, IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldY - var2 * IsoCell.CellSizeInChunks);
   }

   public IsoMetaChunk getChunkData(int var1, int var2) {
      int var3 = PZMath.fastfloor((float)var1 / (float)IsoCell.CellSizeInChunks);
      int var4 = PZMath.fastfloor((float)var2 / (float)IsoCell.CellSizeInChunks);
      IsoMetaCell var5 = this.getCellData(var3, var4);
      return var5 == null ? null : var5.getChunk(var1 - var3 * IsoCell.CellSizeInChunks, var2 - var4 * IsoCell.CellSizeInChunks);
   }

   public IsoMetaChunk getChunkDataFromTile(int var1, int var2) {
      int var3 = PZMath.fastfloor((float)var1 / 8.0F);
      int var4 = PZMath.fastfloor((float)var2 / 8.0F);
      return this.getChunkData(var3, var4);
   }

   public boolean isValidSquare(int var1, int var2) {
      if (var1 < this.minX * IsoCell.CellSizeInSquares) {
         return false;
      } else if (var1 >= (this.maxX + 1) * IsoCell.CellSizeInSquares) {
         return false;
      } else if (var2 < this.minY * IsoCell.CellSizeInSquares) {
         return false;
      } else {
         return var2 < (this.maxY + 1) * IsoCell.CellSizeInSquares;
      }
   }

   public boolean isValidChunk(int var1, int var2) {
      var1 *= 8;
      var2 *= 8;
      if (var1 < this.minX * IsoCell.CellSizeInSquares) {
         return false;
      } else if (var1 >= (this.maxX + 1) * IsoCell.CellSizeInSquares) {
         return false;
      } else if (var2 < this.minY * IsoCell.CellSizeInSquares) {
         return false;
      } else {
         return var2 < (this.maxY + 1) * IsoCell.CellSizeInSquares;
      }
   }

   public void Create() {
      this.CreateStep1();
      this.CreateStep2();
   }

   public void CreateStep1() {
      this.minX = 10000000;
      this.minY = 10000000;
      this.maxX = -10000000;
      this.maxY = -10000000;
      IsoLot.InfoHeaders.clear();
      IsoLot.InfoHeaderNames.clear();
      IsoLot.InfoFileNames.clear();
      IsoLot.MapFiles.clear();
      long var1 = System.currentTimeMillis();
      DebugLog.log("IsoMetaGrid.Create: begin scanning directories");
      ArrayList var3 = this.getLotDirectories();
      DebugLog.log("Looking in these map folders:");

      int var4;
      for(var4 = 0; var4 < var3.size(); ++var4) {
         String var5 = (String)var3.get(var4);
         String var6 = ZomboidFileSystem.instance.getDirectoryString("media/maps/" + var5 + "/");
         File var7 = new File(var6);
         if (!var7.isDirectory()) {
            DebugLog.log("    skipping non-existent map folder " + var6);
         } else {
            MapFiles var8 = new MapFiles(var5, var6, (new File(var6)).getAbsolutePath(), var4);
            IsoLot.MapFiles.add(var8);
            DebugLog.DetailedInfo.trace("    " + var8.mapDirectoryAbsolutePath);
         }
      }

      DebugLog.log("<End of map-folders list>");
      Iterator var13 = IsoLot.MapFiles.iterator();

      while(true) {
         MapFiles var14;
         File var16;
         do {
            if (!var13.hasNext()) {
               for(var4 = IsoLot.MapFiles.size() - 1; var4 >= 0; --var4) {
                  var14 = (MapFiles)IsoLot.MapFiles.get(var4);
                  IsoLot.InfoFileNames.putAll(var14.InfoFileNames);
                  IsoLot.InfoFileModded.putAll(var14.InfoFileModded);
                  IsoLot.InfoHeaders.putAll(var14.InfoHeaders);
                  IsoLot.InfoHeaderNames.removeAll(var14.InfoHeaderNames);
                  IsoLot.InfoHeaderNames.addAll(var14.InfoHeaderNames);
                  this.minX = PZMath.min(this.minX, var14.minX);
                  this.minY = PZMath.min(this.minY, var14.minY);
                  this.maxX = PZMath.max(this.maxX, var14.maxX);
                  this.maxY = PZMath.max(this.maxY, var14.maxY);
               }

               if (this.minX > this.maxX) {
                  this.minX = this.minY = 0;
                  this.maxX = this.maxY = 1;
               }

               this.minNonProceduralX = this.minX;
               this.minNonProceduralY = this.minY;
               this.maxNonProceduralX = this.maxX;
               this.maxNonProceduralY = this.maxY;
               this.minX = Math.min(this.minX, WGParams.instance.getMinXCell());
               this.minY = Math.min(this.minY, WGParams.instance.getMinYCell());
               this.maxX = Math.max(this.maxX, WGParams.instance.getMaxXCell());
               this.maxY = Math.max(this.maxY, WGParams.instance.getMaxYCell());
               DebugLog.log("IsoMetaGrid.Create: X: [ " + this.minX + " " + this.maxX + " ], Y: [ " + this.minY + " " + this.maxY + " ]");
               String var10000 = WGParams.instance.getSeedString();
               DebugLog.log("World seed: " + var10000 + " " + WGParams.instance.getSeed());
               if (this.maxX >= this.minX && this.maxY >= this.minY) {
                  this.Grid = new IsoMetaCell[this.maxX - this.minX + 1][this.maxY - this.minY + 1];
                  this.width = this.maxX - this.minX + 1;
                  this.height = this.maxY - this.minY + 1;
                  long var15 = System.currentTimeMillis() - var1;
                  DebugLog.log("IsoMetaGrid.Create: finished scanning directories in " + (float)var15 / 1000.0F + " seconds");
                  IsoWorld.instance.setWgChunk(new WGChunk(WGParams.instance.getSeed()));
                  IsoWorld.instance.setBlending(new Blending());
                  IsoWorld.instance.setAttachmentsHandler(new AttachmentsHandler());
                  IsoWorld.instance.setBiomeMap(new BiomeMap());
                  IsoWorld.instance.setZoneGenerator(new ZoneGenerator(IsoWorld.instance.getBiomeMap()));
                  DebugLog.log("IsoMetaGrid.Create: begin loading");
                  this.createStartTime = System.currentTimeMillis();

                  for(int var17 = 0; var17 < 8; ++var17) {
                     MetaGridLoaderThread var19 = new MetaGridLoaderThread(this.minY + var17);
                     var19.setDaemon(true);
                     var19.setName("MetaGridLoaderThread" + var17);
                     var19.start();
                     this.threads[var17] = var19;
                  }

                  return;
               }

               throw new IllegalStateException("Failed to find any .lotheader files");
            }

            var14 = (MapFiles)var13.next();
            var16 = new File(var14.mapDirectoryAbsolutePath);
         } while(!var16.isDirectory());

         String var18 = var14.mapDirectoryAbsolutePath;
         ChooseGameInfo.Map var20 = ChooseGameInfo.getMapDetails(var14.mapDirectoryName);
         String[] var9 = var16.list();

         for(int var10 = 0; var10 < var9.length; ++var10) {
            String var11 = var9[var10];
            if (var11.endsWith(".lotheader")) {
               LotHeader var12 = this.createLotHeader(var14, var20, var11);
               var14.InfoFileNames.put(var11, var12.absoluteFilePath);
               var14.InfoFileModded.put(var11, ZomboidFileSystem.instance.isModded(var14.mapDirectoryAbsolutePath));
               var14.InfoHeaders.put(var11, var12);
               var14.InfoHeaderNames.add(var11);
            } else if (var11.endsWith(".lotpack")) {
               var14.InfoFileNames.put(var11, var18 + File.separator + var11);
               var14.InfoFileModded.put(var11, ZomboidFileSystem.instance.isModded(var14.mapDirectoryAbsolutePath));
            } else if (var11.startsWith("chunkdata_")) {
               var14.InfoFileNames.put(var11, var18 + File.separator + var11);
               var14.InfoFileModded.put(var11, ZomboidFileSystem.instance.isModded(var14.mapDirectoryAbsolutePath));
            }
         }

         var14.postLoad();
      }
   }

   private LotHeader createLotHeader(MapFiles var1, ChooseGameInfo.Map var2, String var3) {
      String[] var4 = var3.split("_");
      var4[1] = var4[1].replace(".lotheader", "");
      int var5 = Integer.parseInt(var4[0].trim());
      int var6 = Integer.parseInt(var4[1].trim());
      var1.minX = PZMath.min(var1.minX, var5);
      var1.minY = PZMath.min(var1.minY, var6);
      var1.maxX = PZMath.max(var1.maxX, var5);
      var1.maxY = PZMath.max(var1.maxY, var6);
      LotHeader var7 = new LotHeader(var5, var6);
      var7.bFixed2x = var2.isFixed2x();
      var7.mapFiles = var1;
      var7.fileName = var3;
      var7.absoluteFilePath = var1.mapDirectoryAbsolutePath + File.separator + var3;
      return var7;
   }

   public void CreateStep2() {
      boolean var1 = true;

      while(true) {
         int var2;
         while(var1) {
            var1 = false;

            for(var2 = 0; var2 < 8; ++var2) {
               if (this.threads[var2].isAlive()) {
                  var1 = true;

                  try {
                     Thread.sleep(100L);
                  } catch (InterruptedException var5) {
                  }
                  break;
               }
            }
         }

         this.consolidateBuildings();

         for(var2 = 0; var2 < 8; ++var2) {
            this.threads[var2].postLoad();
            this.threads[var2] = null;
         }

         this.initIncompleteCells();

         for(var2 = 0; var2 < this.Buildings.size(); ++var2) {
            BuildingDef var3 = (BuildingDef)this.Buildings.get(var2);
            this.addRoomsToAdjacentCells(var3);
            if (!Core.GameMode.equals("LastStand") && var3.rooms.size() > 2) {
               int var4 = 11;
               if (SandboxOptions.instance.getElecShutModifier() > -1 && (float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30)) < (float)SandboxOptions.instance.getElecShutModifier()) {
                  var4 = 9;
               }

               if (SandboxOptions.instance.Alarm.getValue() == 1) {
                  var4 = -1;
               } else if (SandboxOptions.instance.Alarm.getValue() == 2) {
                  var4 += 5;
               } else if (SandboxOptions.instance.Alarm.getValue() == 3) {
                  var4 += 3;
               } else if (SandboxOptions.instance.Alarm.getValue() == 5) {
                  var4 -= 3;
               } else if (SandboxOptions.instance.Alarm.getValue() == 6) {
                  var4 -= 5;
               }

               if (var4 > -1) {
                  var3.bAlarmed = Rand.Next(var4) == 0;
               }

               if (var3.bAlarmed) {
                  var3.bAlarmDecay = SandboxOptions.getInstance().randomAlarmDecay(SandboxOptions.getInstance().AlarmDecay.getValue());
               }
            }
         }

         long var6 = System.currentTimeMillis() - this.createStartTime;
         DebugLog.log("IsoMetaGrid.Create: finished loading in " + (float)var6 / 1000.0F + " seconds");
         return;
      }
   }

   private void initIncompleteCells() {
      Iterator var1 = IsoLot.MapFiles.iterator();

      while(var1.hasNext()) {
         MapFiles var2 = (MapFiles)var1.next();
         this.initIncompleteCells(var2);
      }

   }

   private void initIncompleteCells(MapFiles var1) {
      for(int var2 = var1.minY; var2 <= var1.maxY; ++var2) {
         for(int var3 = var1.minX; var3 <= var1.maxX; ++var3) {
            LotHeader var4 = var1.getLotHeader(var3, var2);
            if (var4 != null) {
               if (PZMath.coordmodulo(var3 * 256, 300) > 0) {
                  var4.bAdjacentCells[IsoDirections.W.index()] = var1.hasCell(var3 - 1, var2);
                  var4.bAdjacentCells[IsoDirections.E.index()] = var1.hasCell(var3 + 1, var2);
               }

               if (PZMath.coordmodulo(var2 * 256, 300) > 0) {
                  var4.bAdjacentCells[IsoDirections.N.index()] = var1.hasCell(var3, var2 - 1);
                  var4.bAdjacentCells[IsoDirections.S.index()] = var1.hasCell(var3, var2 + 1);
               }

               if (PZMath.coordmodulo(var3 * 256, 300) > 0 && PZMath.coordmodulo(var2 * 256, 300) > 0) {
                  var4.bAdjacentCells[IsoDirections.NW.index()] = var1.hasCell(var3 - 1, var2 - 1);
                  var4.bAdjacentCells[IsoDirections.NE.index()] = var1.hasCell(var3 + 1, var2 - 1);
                  var4.bAdjacentCells[IsoDirections.SE.index()] = var1.hasCell(var3 + 1, var2 + 1);
                  var4.bAdjacentCells[IsoDirections.SW.index()] = var1.hasCell(var3 - 1, var2 + 1);
               }
            }
         }
      }

   }

   public boolean isChunkLoaded(int var1, int var2) {
      IsoChunk var3 = GameServer.bServer ? ServerMap.instance.getChunk(var1, var2) : IsoWorld.instance.CurrentCell.getChunk(var1, var2);
      return var3 != null && var3.bLoaded;
   }

   public void Dispose() {
      if (this.Grid != null) {
         for(int var1 = 0; var1 < this.gridX(); ++var1) {
            IsoMetaCell[] var2 = this.Grid[var1];

            for(int var3 = 0; var3 < var2.length; ++var3) {
               IsoMetaCell var4 = var2[var3];
               if (var4 != null) {
                  var4.Dispose();
               }
            }

            Arrays.fill(var2, (Object)null);
         }

         Arrays.fill(this.Grid, (Object)null);
         this.Grid = null;
         Iterator var5 = this.Buildings.iterator();

         while(var5.hasNext()) {
            BuildingDef var6 = (BuildingDef)var5.next();
            var6.Dispose();
         }

         this.Buildings.clear();
         this.VehiclesZones.clear();
         var5 = this.Zones.iterator();

         while(var5.hasNext()) {
            Zone var7 = (Zone)var5.next();
            var7.Dispose();
         }

         this.Zones.clear();
         this.animalZoneHandler.Dispose();
         this.sharedStrings.clear();
      }
   }

   public Vector2 getRandomIndoorCoord() {
      return null;
   }

   public RoomDef getRandomRoomBetweenRange(float var1, float var2, float var3, float var4) {
      RoomDef var5 = null;
      float var6 = 0.0F;
      roomChoices.clear();
      LotHeader var7 = null;

      for(int var8 = 0; var8 < IsoLot.InfoHeaderNames.size(); ++var8) {
         var7 = (LotHeader)IsoLot.InfoHeaders.get(IsoLot.InfoHeaderNames.get(var8));
         if (!var7.RoomList.isEmpty()) {
            for(int var9 = 0; var9 < var7.RoomList.size(); ++var9) {
               var5 = (RoomDef)var7.RoomList.get(var9);
               var6 = IsoUtils.DistanceManhatten(var1, var2, (float)var5.x, (float)var5.y);
               if (var6 > var3 && var6 < var4) {
                  roomChoices.add(var5);
               }
            }
         }
      }

      if (!roomChoices.isEmpty()) {
         return (RoomDef)roomChoices.get(Rand.Next(roomChoices.size()));
      } else {
         return null;
      }
   }

   public RoomDef getRandomRoomNotInRange(float var1, float var2, int var3) {
      RoomDef var4 = null;

      do {
         LotHeader var5 = null;

         do {
            var5 = (LotHeader)IsoLot.InfoHeaders.get(IsoLot.InfoHeaderNames.get(Rand.Next(IsoLot.InfoHeaderNames.size())));
         } while(var5.RoomList.isEmpty());

         var4 = (RoomDef)var5.RoomList.get(Rand.Next(var5.RoomList.size()));
      } while(var4 == null || IsoUtils.DistanceManhatten(var1, var2, (float)var4.x, (float)var4.y) < (float)var3);

      return var4;
   }

   public void save() {
      try {
         this.save("map_meta.bin", this::save);
         this.save("map_zone.bin", this::saveZone);
         this.save("map_animals.bin", this::saveAnimalZones);
         this.saveCells("metagrid", "metacell_%d_%d.bin", IsoMetaCell::save);
      } catch (Exception var2) {
         ExceptionLogger.logException(var2);
      }

   }

   public void addCellToSave(IsoMetaCell var1) {
      this.cellsToSave.add(var1);
   }

   private void save(String var1, Consumer<ByteBuffer> var2) throws IOException {
      File var3 = ZomboidFileSystem.instance.getFileInCurrentSave(var1);
      FileOutputStream var4 = new FileOutputStream(var3);

      try {
         BufferedOutputStream var5 = new BufferedOutputStream(var4);

         try {
            synchronized(SliceY.SliceBufferLock) {
               SliceY.SliceBuffer.clear();
               var2.accept(SliceY.SliceBuffer);
               var5.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
            }
         } catch (Throwable var11) {
            try {
               var5.close();
            } catch (Throwable var9) {
               var11.addSuppressed(var9);
            }

            throw var11;
         }

         var5.close();
      } catch (Throwable var12) {
         try {
            var4.close();
         } catch (Throwable var8) {
            var12.addSuppressed(var8);
         }

         throw var12;
      }

      var4.close();
   }

   private void saveCells(String var1, String var2, BiConsumer<IsoMetaCell, ByteBuffer> var3) throws IOException {
      ArrayList var4 = new ArrayList(this.cellsToSave);
      Iterator var5 = var4.iterator();

      while(var5.hasNext()) {
         IsoMetaCell var6 = (IsoMetaCell)var5.next();
         String var7 = String.format(var2, var6.getX(), var6.getY());
         File var8 = ZomboidFileSystem.instance.getFileInCurrentSave(var1, var7);
         FileOutputStream var9 = new FileOutputStream(var8);

         try {
            BufferedOutputStream var10 = new BufferedOutputStream(var9);

            try {
               synchronized(SliceY.SliceBufferLock) {
                  SliceY.SliceBuffer.clear();
                  var3.accept(var6, SliceY.SliceBuffer);
                  var10.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
               }
            } catch (Throwable var16) {
               try {
                  var10.close();
               } catch (Throwable var14) {
                  var16.addSuppressed(var14);
               }

               throw var16;
            }

            var10.close();
         } catch (Throwable var17) {
            try {
               var9.close();
            } catch (Throwable var13) {
               var17.addSuppressed(var13);
            }

            throw var17;
         }

         var9.close();
         this.cellsToSave.remove(var6);
      }

   }

   public void load(String var1, BiConsumer<ByteBuffer, Integer> var2) {
      File var3 = ZomboidFileSystem.instance.getFileInCurrentSave(var1);

      try {
         FileInputStream var4 = new FileInputStream(var3);

         try {
            BufferedInputStream var5 = new BufferedInputStream(var4);

            try {
               synchronized(SliceY.SliceBufferLock) {
                  SliceY.SliceBuffer.clear();
                  int var7 = var5.read(SliceY.SliceBuffer.array());
                  SliceY.SliceBuffer.limit(var7);
                  var2.accept(SliceY.SliceBuffer, -1);
               }
            } catch (Throwable var12) {
               try {
                  var5.close();
               } catch (Throwable var10) {
                  var12.addSuppressed(var10);
               }

               throw var12;
            }

            var5.close();
         } catch (Throwable var13) {
            try {
               var4.close();
            } catch (Throwable var9) {
               var13.addSuppressed(var9);
            }

            throw var13;
         }

         var4.close();
      } catch (FileNotFoundException var14) {
      } catch (Exception var15) {
         ExceptionLogger.logException(var15);
      }

   }

   public void loadCells(String var1, String var2, QuadConsumer<IsoMetaCell, IsoMetaGrid, ByteBuffer, Integer> var3) {
      File var4 = ZomboidFileSystem.instance.getFileInCurrentSave(var1);
      Pattern var5 = Pattern.compile(var2);
      RegExFilenameFilter var6 = new RegExFilenameFilter(var5);
      String[] var7 = var4.list(var6);
      if (var7 != null) {
         String[] var8 = var7;
         int var9 = var7.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            String var11 = var8[var10];
            File var12 = ZomboidFileSystem.instance.getFileInCurrentSave(var1, var11);
            Matcher var13 = var5.matcher(var11);
            var13.matches();
            int var14 = Integer.parseInt(var13.group(1));
            int var15 = Integer.parseInt(var13.group(2));
            IsoMetaCell var16 = this.getCellOrCreate(var14 - this.minX, var15 - this.minY);

            try {
               FileInputStream var17 = new FileInputStream(var12);

               try {
                  BufferedInputStream var18 = new BufferedInputStream(var17);

                  try {
                     synchronized(SliceY.SliceBufferLock) {
                        SliceY.SliceBuffer.clear();
                        int var20 = var18.read(SliceY.SliceBuffer.array());
                        SliceY.SliceBuffer.limit(var20);
                        var3.accept(var16, this, SliceY.SliceBuffer, -1);
                     }
                  } catch (Throwable var25) {
                     try {
                        var18.close();
                     } catch (Throwable var23) {
                        var25.addSuppressed(var23);
                     }

                     throw var25;
                  }

                  var18.close();
               } catch (Throwable var26) {
                  try {
                     var17.close();
                  } catch (Throwable var22) {
                     var26.addSuppressed(var22);
                  }

                  throw var26;
               }

               var17.close();
            } catch (FileNotFoundException var27) {
            } catch (Exception var28) {
               ExceptionLogger.logException(var28);
            }
         }

      }
   }

   public void loadZone(ByteBuffer var1, int var2) {
      int var3;
      int var4;
      int var5;
      int var6;
      if (var2 == -1) {
         var3 = var1.get();
         var4 = var1.get();
         var5 = var1.get();
         var6 = var1.get();
         if (var3 != 90 || var4 != 79 || var5 != 78 || var6 != 69) {
            DebugLog.log("ERROR: expected 'ZONE' at start of map_zone.bin");
            return;
         }

         var2 = var1.getInt();
      }

      var3 = this.Zones.size();
      Iterator var12 = this.Zones.iterator();

      while(var12.hasNext()) {
         Zone var13 = (Zone)var12.next();
         var13.Dispose();
      }

      this.Zones.clear();

      int var7;
      for(var4 = 0; var4 < this.height; ++var4) {
         for(var5 = 0; var5 < this.width; ++var5) {
            if (this.hasCell(var5, var4)) {
               IsoMetaCell var15 = this.getCell(var5, var4);

               for(var7 = 0; var7 < IsoCell.CellSizeInChunks; ++var7) {
                  for(int var8 = 0; var8 < IsoCell.CellSizeInChunks; ++var8) {
                     if (var15.hasChunk(var8 + var7 * IsoCell.CellSizeInChunks)) {
                        var15.getChunk(var8 + var7 * IsoCell.CellSizeInChunks).clearZones();
                        var15.clearChunk(var8 + var7 * IsoCell.CellSizeInChunks);
                     }
                  }
               }
            }
         }
      }

      HashMap var14 = this.loadStringMap(var1);
      var5 = var1.getInt();
      DebugLog.log("loading " + var5 + " zones from map_zone.bin");

      for(var6 = 0; var6 < var5; ++var6) {
         Zone var16 = (new Zone()).load(var1, var2, var14, this.sharedStrings);
         if (!"WorldGen".equalsIgnoreCase(var16.type)) {
            this.registerZone(var16);
         }
      }

      var6 = var1.getInt();

      for(var7 = 0; var7 < var6; ++var7) {
         String var17 = GameWindow.ReadString(var1);
         ArrayList var9 = new ArrayList();
         int var10 = var1.getInt();

         for(int var11 = 0; var11 < var10; ++var11) {
            if (var2 >= 215) {
               var9.add(GameWindow.ReadUUID(var1));
            } else {
               var9.add(UUID.randomUUID());
            }
         }

         IsoWorld.instance.getSpawnedZombieZone().put(var17, var9);
      }

   }

   public void loadAnimalZones(ByteBuffer var1, int var2) {
      int var4;
      int var5;
      int var6;
      if (var2 == -1) {
         byte var3 = var1.get();
         var4 = var1.get();
         var5 = var1.get();
         var6 = var1.get();
         if (var3 != 90 || var4 != 79 || var5 != 78 || var6 != 69) {
            DebugLog.log("ERROR: expected 'ZONE' at start of map_animals.bin");
            return;
         }

         var2 = var1.getInt();
      }

      Collection var9 = this.animalZoneHandler.getZones();
      Iterator var10 = var9.iterator();

      while(var10.hasNext()) {
         AnimalZone var11 = (AnimalZone)var10.next();
         var11.Dispose();
      }

      this.animalZoneHandler.Dispose();

      for(var4 = 0; var4 < this.height; ++var4) {
         for(var5 = 0; var5 < this.width; ++var5) {
            if (this.hasCell(var5, var4)) {
               this.getCell(var5, var4).clearAnimalZones();
            }
         }
      }

      HashMap var12 = this.loadStringMap(var1);
      var5 = var1.getInt();
      DebugLog.log("loading " + var5 + " zones from map_animals.bin");

      for(var6 = 0; var6 < var5; ++var6) {
         this.registerAnimalZone((new AnimalZone()).load(var1, var2, var12, this.sharedStrings));
      }

      var6 = var1.getInt();

      for(int var7 = 0; var7 < var6; ++var7) {
         AnimalZoneJunction var8 = AnimalZoneJunction.load(var1, var2);
         var8.m_zoneSelf.addJunction(var8);
      }

   }

   private HashMap<Integer, String> loadStringMap(ByteBuffer var1) {
      int var2 = var1.getInt();
      HashMap var3 = new HashMap();

      for(int var4 = 0; var4 < var2; ++var4) {
         String var5 = GameWindow.ReadStringUTF(var1);
         var3.put(var4, var5);
      }

      return var3;
   }

   public void saveZone(ByteBuffer var1) {
      var1.put((byte)90);
      var1.put((byte)79);
      var1.put((byte)78);
      var1.put((byte)69);
      var1.putInt(219);
      HashMap var2 = this.saveStringMap(var1, this.Zones);
      var1.putInt(this.Zones.size());
      this.Zones.forEach((var2x) -> {
         var2x.save(var1, var2);
      });
      var2.clear();
      var1.putInt(IsoWorld.instance.getSpawnedZombieZone().size());
      Iterator var3 = IsoWorld.instance.getSpawnedZombieZone().keySet().iterator();

      while(var3.hasNext()) {
         String var4 = (String)var3.next();
         ArrayList var5 = (ArrayList)IsoWorld.instance.getSpawnedZombieZone().get(var4);
         GameWindow.WriteString(var1, var4);
         var1.putInt(var5.size());

         for(int var6 = 0; var6 < var5.size(); ++var6) {
            GameWindow.WriteUUID(var1, (UUID)var5.get(var6));
         }
      }

   }

   public void saveAnimalZones(ByteBuffer var1) {
      var1.put((byte)90);
      var1.put((byte)79);
      var1.put((byte)78);
      var1.put((byte)69);
      var1.putInt(219);
      ArrayList var2 = new ArrayList(this.animalZoneHandler.getZones());
      HashMap var3 = this.saveStringMap(var1, var2);
      var1.putInt(var2.size());
      var2.forEach((var2x) -> {
         var2x.save(var1, var3);
      });
      HashSet var4 = new HashSet();
      Iterator var5 = var2.iterator();

      while(var5.hasNext()) {
         AnimalZone var6 = (AnimalZone)var5.next();
         if (var6.m_junctions != null) {
            var4.addAll(var6.m_junctions);
         }
      }

      var1.putInt(var4.size());
      var4.forEach((var1x) -> {
         var1x.save(var1);
      });
   }

   private HashMap<String, Integer> saveStringMap(ByteBuffer var1, List<? extends Zone> var2) {
      HashSet var3 = new HashSet();

      for(int var4 = 0; var4 < var2.size(); ++var4) {
         Zone var5 = (Zone)var2.get(var4);
         var3.add(var5.getName());
         var3.add(var5.getOriginalName());
         var3.add(var5.getType());
      }

      ArrayList var7 = new ArrayList(var3);
      HashMap var8 = new HashMap();

      int var6;
      for(var6 = 0; var6 < var7.size(); ++var6) {
         var8.put((String)var7.get(var6), var6);
      }

      if (var7.size() > 32767) {
         throw new IllegalStateException("IsoMetaGrid.saveZone() string table is too large");
      } else {
         var1.putInt(var7.size());

         for(var6 = 0; var6 < var7.size(); ++var6) {
            GameWindow.WriteString(var1, (String)var7.get(var6));
         }

         return var8;
      }
   }

   private void getLotDirectories(String var1, ArrayList<String> var2) {
      if (!var2.contains(var1)) {
         ChooseGameInfo.Map var3 = ChooseGameInfo.getMapDetails(var1);
         if (var3 != null) {
            var2.add(var1);
            Iterator var4 = var3.getLotDirectories().iterator();

            while(var4.hasNext()) {
               String var5 = (String)var4.next();
               this.getLotDirectories(var5, var2);
            }

         }
      }
   }

   public ArrayList<String> getLotDirectories() {
      if (GameClient.bClient) {
         Core.GameMap = GameClient.GameMap;
      }

      if (GameServer.bServer) {
         Core.GameMap = GameServer.GameMap;
      }

      if (Core.GameMap.equals("DEFAULT")) {
         MapGroups var1 = new MapGroups();
         var1.createGroups();
         if (var1.getNumberOfGroups() != 1) {
            throw new RuntimeException("GameMap is DEFAULT but there are multiple worlds to choose from");
         }

         var1.setWorld(0);
      }

      ArrayList var5 = new ArrayList();
      if (Core.GameMap.contains(";")) {
         String[] var2 = Core.GameMap.split(";");

         for(int var3 = 0; var3 < var2.length; ++var3) {
            String var4 = var2[var3].trim();
            if (!var4.isEmpty() && !var5.contains(var4)) {
               var5.add(var4);
            }
         }
      } else {
         this.getLotDirectories(Core.GameMap, var5);
      }

      return var5;
   }

   public void addRoomsToAdjacentCells(BuildingDef var1) {
      int var2 = var1.x / IsoCell.CellSizeInSquares;
      int var3 = var1.y / IsoCell.CellSizeInSquares;
      int var4 = (var1.x2 - 1) / IsoCell.CellSizeInSquares;
      int var5 = (var1.y2 - 1) / IsoCell.CellSizeInSquares;
      if (var4 != var2 || var5 != var3) {
         for(int var6 = 0; var6 <= 1; ++var6) {
            for(int var7 = 0; var7 <= 1; ++var7) {
               if (var7 != 0 || var6 != 0) {
                  IsoMetaCell var8 = this.getCellData(var2 + var7, var3 + var6);
                  if (var8 != null) {
                     var8.addRooms(var1.rooms, (var2 + var7) * IsoCell.CellSizeInSquares, (var3 + var6) * IsoCell.CellSizeInSquares);
                     var8.addRooms(var1.emptyoutside, (var2 + var7) * IsoCell.CellSizeInSquares, (var3 + var6) * IsoCell.CellSizeInSquares);
                  }
               }
            }
         }

      }
   }

   public void addRoomsToAdjacentCells(BuildingDef var1, ArrayList<RoomDef> var2) {
      int var3 = var1.x / IsoCell.CellSizeInSquares;
      int var4 = var1.y / IsoCell.CellSizeInSquares;
      int var5 = (var1.x2 - 1) / IsoCell.CellSizeInSquares;
      int var6 = (var1.y2 - 1) / IsoCell.CellSizeInSquares;
      if (var5 != var3 || var6 != var4) {
         for(int var7 = 0; var7 <= 1; ++var7) {
            for(int var8 = 0; var8 <= 1; ++var8) {
               if (var8 != 0 || var7 != 0) {
                  IsoMetaCell var9 = this.getCellData(var3 + var8, var4 + var7);
                  if (var9 != null) {
                     var9.addRooms(var2, (var3 + var8) * IsoCell.CellSizeInSquares, (var4 + var7) * IsoCell.CellSizeInSquares);
                  }
               }
            }
         }

      }
   }

   private void consolidateBuildings() {
      for(int var1 = 0; var1 < IsoLot.MapFiles.size(); ++var1) {
         MapFiles var2 = (MapFiles)IsoLot.MapFiles.get(var1);
         Iterator var3 = var2.InfoHeaders.values().iterator();

         while(var3.hasNext()) {
            LotHeader var4 = (LotHeader)var3.next();
            LotHeader var5 = (LotHeader)IsoLot.InfoHeaders.get(var4.fileName);
            IsoMetaCell var6 = this.getCellData(var4.cellX, var4.cellY);

            for(int var7 = var4.Buildings.size() - 1; var7 >= 0; --var7) {
               BuildingDef var8 = (BuildingDef)var4.Buildings.get(var7);
               int var9 = var8.x / 300;
               int var10 = var8.y / 300;
               Iterator var11;
               RoomDef var12;
               if (this.higherPriority300x300CellExists(var2.priority, var9, var10)) {
                  var11 = var8.rooms.iterator();

                  while(var11.hasNext()) {
                     var12 = (RoomDef)var11.next();
                     var4.Rooms.remove(var12.ID);
                     var4.RoomList.remove(var12);
                     var4.RoomByMetaID.remove(var12.metaID);
                  }

                  var4.Buildings.remove(var8);
                  var4.BuildingByMetaID.remove(var8.metaID);
               } else {
                  if (var4 != var5) {
                     var11 = var8.rooms.iterator();

                     while(var11.hasNext()) {
                        var12 = (RoomDef)var11.next();
                        var12.ID = RoomID.makeID(var4.cellX, var4.cellY, var5.RoomList.size());
                        var5.RoomList.add(var12);
                        var5.Rooms.put(var12.ID, var12);
                     }

                     var8.ID = BuildingID.makeID(var4.cellX, var4.cellY, var5.Buildings.size());
                     var5.Buildings.add(var8);
                     var5.BuildingByMetaID.put(var8.metaID, var8);
                  }

                  this.Buildings.add(var8);
                  var11 = var8.rooms.iterator();

                  while(var11.hasNext()) {
                     var12 = (RoomDef)var11.next();
                     var6.addRoom(var12, var6.getX() * IsoCell.CellSizeInSquares, var6.getY() * IsoCell.CellSizeInSquares);
                  }

                  var11 = var8.emptyoutside.iterator();

                  while(var11.hasNext()) {
                     var12 = (RoomDef)var11.next();
                     var6.addRoom(var12, var6.getX() * IsoCell.CellSizeInSquares, var6.getY() * IsoCell.CellSizeInSquares);
                  }
               }
            }
         }
      }

   }

   private boolean higherPriority300x300CellExists(int var1, int var2, int var3) {
      for(int var4 = 0; var4 < var1; ++var4) {
         MapFiles var5 = (MapFiles)IsoLot.MapFiles.get(var4);
         if (var5.bgHasCell300.getValue(var2 - var5.minCell300X, var3 - var5.minCell300Y)) {
            return true;
         }
      }

      return false;
   }

   private final class MetaGridLoaderThread extends Thread {
      final SharedStrings sharedStrings = new SharedStrings();
      final ArrayList<BuildingDef> Buildings = new ArrayList();
      final ArrayList<RoomDef> tempRooms = new ArrayList();
      int wY;
      final byte[] zombieIntensity;

      MetaGridLoaderThread(int var2) {
         this.zombieIntensity = new byte[IsoCell.CellSizeInChunks * IsoCell.CellSizeInChunks];
         this.wY = var2;
      }

      public void run() {
         try {
            this.runInner();
         } catch (Exception var2) {
            ExceptionLogger.logException(var2);
         }

      }

      void runInner() {
         for(int var1 = this.wY; var1 <= IsoMetaGrid.this.maxY; var1 += 8) {
            for(int var2 = IsoMetaGrid.this.minX; var2 <= IsoMetaGrid.this.maxX; ++var2) {
               this.loadCell(var2, var1);
            }
         }

      }

      void loadCell(int var1, int var2) {
         for(int var3 = 0; var3 < IsoLot.MapFiles.size(); ++var3) {
            MapFiles var4 = (MapFiles)IsoLot.MapFiles.get(var3);
            this.loadCell(var4, var1, var2);
         }

      }

      void loadCell(MapFiles var1, int var2, int var3) {
         boolean var4 = false;
         String var5 = "" + var2 + "_" + var3 + ".lotheader";
         if (var1.InfoFileNames.containsKey(var5)) {
            LotHeader var6 = (LotHeader)var1.InfoHeaders.get(var5);
            if (var6 != null) {
               File var7 = new File((String)var1.InfoFileNames.get(var5));
               if (var7.exists()) {
                  IsoMetaCell var8 = IsoMetaGrid.this.getCell(var2 - IsoMetaGrid.this.minX, var3 - IsoMetaGrid.this.minY);
                  boolean var9 = var8 == null;
                  if (var8 == null) {
                     var8 = new IsoMetaCell(var2, var3);
                     var8.info = var6;
                     IsoMetaGrid.this.setCell(var2 - IsoMetaGrid.this.minX, var3 - IsoMetaGrid.this.minY, var8);
                  }

                  try {
                     BufferedRandomAccessFile var10 = new BufferedRandomAccessFile(var7.getAbsolutePath(), "r", 4096);

                     try {
                        byte[] var11 = new byte[4];
                        var10.read(var11, 0, 4);
                        boolean var12 = Arrays.equals(var11, LotHeader.LOTHEADER_MAGIC);
                        if (!var12) {
                           var10.seek(0L);
                        }

                        var6.version = IsoLot.readInt(var10);
                        if (var6.version < 0 || var6.version > 1) {
                           throw new IOException("Unsupported version " + var6.version);
                        }

                        int var13 = IsoLot.readInt(var10);

                        int var14;
                        for(var14 = 0; var14 < var13; ++var14) {
                           String var15 = IsoLot.readString(var10);
                           var6.tilesUsed.add(this.sharedStrings.get(var15.trim()));
                        }

                        if (var6.version == 0) {
                           var10.read();
                        }

                        var6.width = IsoLot.readInt(var10);
                        var6.height = IsoLot.readInt(var10);
                        if (var6.version == 0) {
                           var6.minLevel = 0;
                           var6.maxLevel = IsoLot.readInt(var10) - 1;
                        } else {
                           var6.minLevel = IsoLot.readInt(var10);
                           var6.maxLevel = IsoLot.readInt(var10);
                        }

                        var14 = IsoLot.readInt(var10);

                        int var18;
                        int var19;
                        int var20;
                        int var28;
                        for(var28 = 0; var28 < var14; ++var28) {
                           String var16 = IsoLot.readString(var10);
                           RoomDef var17 = new RoomDef(RoomID.makeID(var2, var3, var28), this.sharedStrings.get(var16));
                           var17.level = IsoLot.readInt(var10);
                           var18 = IsoLot.readInt(var10);

                           int var21;
                           int var22;
                           int var23;
                           for(var19 = 0; var19 < var18; ++var19) {
                              var20 = IsoLot.readInt(var10);
                              var21 = IsoLot.readInt(var10);
                              var22 = IsoLot.readInt(var10);
                              var23 = IsoLot.readInt(var10);
                              RoomDef.RoomRect var24 = new RoomDef.RoomRect(var20 + var2 * IsoCell.CellSizeInSquares, var21 + var3 * IsoCell.CellSizeInSquares, var22, var23);
                              var17.rects.add(var24);
                           }

                           var17.CalculateBounds();
                           var17.metaID = var17.calculateMetaID(var2, var3);
                           var6.Rooms.put(var17.ID, var17);
                           if (var6.RoomByMetaID.contains(var17.metaID) && !var4) {
                              DebugLog.General.error("duplicate RoomDef.metaID for room at x=%d, y=%d, level=%d, filename=%s", var17.x, var17.y, var17.level, var7.getName());
                              var4 = true;
                           }

                           var6.RoomByMetaID.put(var17.metaID, var17);
                           var6.RoomList.add(var17);
                           var19 = IsoLot.readInt(var10);

                           for(var20 = 0; var20 < var19; ++var20) {
                              var21 = IsoLot.readInt(var10);
                              var22 = IsoLot.readInt(var10);
                              var23 = IsoLot.readInt(var10);
                              var17.objects.add(new MetaObject(var21, var22 + var2 * IsoCell.CellSizeInSquares - var17.x, var23 + var3 * IsoCell.CellSizeInSquares - var17.y, var17));
                           }

                           var17.bLightsActive = Rand.Next(4) == 0;
                        }

                        var28 = IsoLot.readInt(var10);
                        int var29 = 0;

                        label119:
                        while(true) {
                           if (var29 >= var28) {
                              var29 = var10.read(this.zombieIntensity);
                              if (var29 != this.zombieIntensity.length) {
                                 throw new EOFException(String.format("wx=%d, wy=%d, nBytes=%d, this.zombieIntensity.length=%d", var2, var3, var29, this.zombieIntensity.length));
                              }

                              int var31 = 0;

                              while(true) {
                                 if (var31 >= IsoCell.CellSizeInChunks) {
                                    break label119;
                                 }

                                 for(var18 = 0; var18 < IsoCell.CellSizeInChunks; ++var18) {
                                    var19 = this.zombieIntensity[var31 * IsoCell.CellSizeInChunks + var18] & 255;
                                    var19 = PZMath.clamp(var19, 0, 255);
                                    var6.ZombieIntensity[var31 + var18 * IsoCell.CellSizeInChunks] = (byte)var19;
                                    if (var8.hasChunk(var31, var18) && !IsoMetaGrid.this.higherPriority300x300CellExists(var1.priority, (var8.getX() * 256 + var31 * 8) / 300, (var8.getY() * 256 + var18 * 8) / 300)) {
                                       var8.getChunk(var31, var18).setZombieIntensity((byte)var19);
                                    }
                                 }

                                 ++var31;
                              }
                           }

                           BuildingDef var30 = new BuildingDef();
                           var18 = IsoLot.readInt(var10);
                           var30.ID = BuildingID.makeID(var2, var3, var29);

                           for(var19 = 0; var19 < var18; ++var19) {
                              var20 = IsoLot.readInt(var10);
                              long var32 = RoomID.makeID(var2, var3, var20);
                              RoomDef var33 = (RoomDef)var6.Rooms.get(var32);
                              var33.building = var30;
                              if (var33.isEmptyOutside()) {
                                 var30.emptyoutside.add(var33);
                              } else {
                                 var30.rooms.add(var33);
                              }
                           }

                           var30.CalculateBounds(this.tempRooms);
                           var30.metaID = var30.calculateMetaID(var2, var3);
                           var6.Buildings.add(var30);
                           var6.BuildingByMetaID.put(var30.metaID, var30);
                           ++var29;
                        }
                     } catch (Throwable var26) {
                        try {
                           var10.close();
                        } catch (Throwable var25) {
                           var26.addSuppressed(var25);
                        }

                        throw var26;
                     }

                     var10.close();
                  } catch (Exception var27) {
                     DebugLog.log("ERROR loading " + var7.getAbsolutePath());
                     ExceptionLogger.logException(var27);
                  }

               }
            }
         }
      }

      void postLoad() {
         this.Buildings.clear();
         this.sharedStrings.clear();
         this.tempRooms.clear();
      }
   }
}
