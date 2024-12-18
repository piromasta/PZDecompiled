package zombie.iso;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.core.math.PZMath;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;

public final class LotHeader {
   public static final int VERSION0 = 0;
   public static final int VERSION1 = 1;
   public static final int VERSION_LATEST = 1;
   public static final byte[] LOTHEADER_MAGIC = new byte[]{76, 79, 84, 72};
   public static final byte[] LOTPACK_MAGIC = new byte[]{76, 79, 84, 80};
   public final int cellX;
   public final int cellY;
   public int width = 0;
   public int height = 0;
   public int minLevel = 0;
   public int maxLevel = 0;
   public int version = 0;
   public final HashMap<Long, RoomDef> Rooms = new HashMap();
   public final TLongObjectHashMap<RoomDef> RoomByMetaID = new TLongObjectHashMap();
   public final ArrayList<RoomDef> RoomList = new ArrayList();
   public final ArrayList<BuildingDef> Buildings = new ArrayList();
   public final TLongObjectHashMap<BuildingDef> BuildingByMetaID = new TLongObjectHashMap();
   public final HashMap<Long, IsoRoom> isoRooms = new HashMap();
   public final HashMap<Long, IsoBuilding> isoBuildings = new HashMap();
   public boolean bFixed2x;
   protected final ArrayList<String> tilesUsed = new ArrayList();
   public final byte[] ZombieIntensity;
   public MapFiles mapFiles;
   public String fileName;
   public String absoluteFilePath;
   public final boolean[] bAdjacentCells;

   public LotHeader(int var1, int var2) {
      this.ZombieIntensity = new byte[IsoCell.CellSizeInChunks * IsoCell.CellSizeInChunks];
      this.bAdjacentCells = new boolean[8];
      this.cellX = var1;
      this.cellY = var2;
   }

   public int getHeight() {
      return this.height;
   }

   public int getWidth() {
      return this.width;
   }

   public int getMinLevel() {
      return this.minLevel;
   }

   public int getMaxLevel() {
      return this.maxLevel;
   }

   public int getNumLevels() {
      return this.getMaxLevel() - this.getMinLevel() + 1;
   }

   public IsoRoom getRoom(long var1) {
      if (!RoomID.isSameCell(var1, this.cellX, this.cellY)) {
         return IsoWorld.instance.getMetaGrid().getRoomByID(var1);
      } else {
         LotHeader var3 = this;
         int var4 = this.mapFiles.priority;

         while(!var3.Rooms.containsKey(var1) && var4 + 1 < IsoLot.MapFiles.size()) {
            MapFiles var5 = (MapFiles)IsoLot.MapFiles.get(var4 + 1);
            ++var4;
            if (var5 == null) {
               break;
            }

            LotHeader var6 = var5.getLotHeader(this.cellX, this.cellY);
            if (var6 != null) {
               var3 = var6;
            }
         }

         RoomDef var9 = (RoomDef)var3.Rooms.get(var1);
         if (var9 == null) {
            return null;
         } else {
            IsoRoom var10;
            if (!this.isoRooms.containsKey(var1)) {
               var10 = new IsoRoom();
               var10.rects.addAll(var9.rects);
               var10.RoomDef = var9.name;
               var10.def = var9;
               var10.layer = var9.level;
               IsoWorld.instance.CurrentCell.getRoomList().add(var10);
               if (var9.building == null) {
                  var9.building = new BuildingDef();
                  var9.building.ID = BuildingID.makeID(this.cellX, this.cellY, this.Buildings.size());
                  var9.building.rooms.add(var9);
                  var9.building.CalculateBounds(new ArrayList());
                  var9.building.metaID = var9.building.calculateMetaID(this.cellX, this.cellY);
                  this.Buildings.add(var9.building);
               }

               long var7 = var9.building.ID;
               this.isoRooms.put(var1, var10);
               if (!this.isoBuildings.containsKey(var7)) {
                  var10.building = new IsoBuilding();
                  var10.building.def = var9.building;
                  this.isoBuildings.put(var7, var10.building);
                  var10.building.CreateFrom(var9.building, this);
               } else {
                  var10.building = (IsoBuilding)this.isoBuildings.get(var7);
                  var10.building.Rooms.add(var10);
               }

               return var10;
            } else {
               var10 = (IsoRoom)this.isoRooms.get(var1);
               return var10;
            }
         }
      }
   }

   /** @deprecated */
   @Deprecated
   public long getRoomAt(int var1, int var2, int var3) {
      Iterator var4 = this.Rooms.entrySet().iterator();

      while(var4.hasNext()) {
         Map.Entry var5 = (Map.Entry)var4.next();
         RoomDef var6 = (RoomDef)var5.getValue();

         for(int var7 = 0; var7 < var6.rects.size(); ++var7) {
            RoomDef.RoomRect var8 = (RoomDef.RoomRect)var6.rects.get(var7);
            if (var8.x <= var1 && var8.y <= var2 && var6.level == var3 && var8.getX2() > var1 && var8.getY2() > var2) {
               return (Long)var5.getKey();
            }
         }
      }

      return -1L;
   }

   public byte[] getZombieIntensity() {
      return this.ZombieIntensity;
   }

   public byte getZombieIntensity(int var1) {
      return this.ZombieIntensity[var1];
   }

   public static int getZombieIntensityForChunk(LotHeader var0, int var1, int var2) {
      if (var1 >= 0 && var2 >= 0 && var1 < IsoCell.CellSizeInChunks && var2 < IsoCell.CellSizeInChunks) {
         if (var0 == null) {
            return -1;
         } else {
            for(int var3 = var0.mapFiles.priority; var3 < IsoLot.MapFiles.size(); ++var3) {
               MapFiles var4 = (MapFiles)IsoLot.MapFiles.get(var3);
               int var5 = PZMath.fastfloor((float)(var0.cellX * IsoCell.CellSizeInSquares + var1 * 8) / 300.0F);
               int var6 = PZMath.fastfloor((float)(var0.cellY * IsoCell.CellSizeInSquares + var2 * 8) / 300.0F);
               if (var4.bgHasCell300.getValue(var5 - var4.minCell300X, var6 - var4.minCell300Y)) {
                  LotHeader var7 = var4.getLotHeader(var0.cellX, var0.cellY);
                  return var7.getZombieIntensity(var1 + var2 * IsoCell.CellSizeInChunks) & 255;
               }
            }

            return -1;
         }
      } else {
         return -1;
      }
   }

   public void Dispose() {
      this.Rooms.clear();
      this.RoomByMetaID.clear();
      this.RoomList.clear();
      this.Buildings.clear();
      this.BuildingByMetaID.clear();
      this.isoRooms.clear();
      this.isoBuildings.clear();
      this.tilesUsed.clear();
   }
}
