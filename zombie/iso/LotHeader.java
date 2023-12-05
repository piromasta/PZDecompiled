package zombie.iso;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;

public final class LotHeader {
   public int cellX;
   public int cellY;
   public int width = 0;
   public int height = 0;
   public int levels = 0;
   public int version = 0;
   public final HashMap<Integer, RoomDef> Rooms = new HashMap();
   public final TLongObjectHashMap<RoomDef> RoomByMetaID = new TLongObjectHashMap();
   public final ArrayList<RoomDef> RoomList = new ArrayList();
   public final ArrayList<BuildingDef> Buildings = new ArrayList();
   public final TLongObjectHashMap<BuildingDef> BuildingByMetaID = new TLongObjectHashMap();
   public final HashMap<Integer, IsoRoom> isoRooms = new HashMap();
   public final HashMap<Integer, IsoBuilding> isoBuildings = new HashMap();
   public boolean bFixed2x;
   protected final ArrayList<String> tilesUsed = new ArrayList();

   public LotHeader() {
   }

   public int getHeight() {
      return this.height;
   }

   public int getWidth() {
      return this.width;
   }

   public int getLevels() {
      return this.levels;
   }

   public IsoRoom getRoom(int var1) {
      RoomDef var2 = (RoomDef)this.Rooms.get(var1);
      IsoRoom var3;
      if (!this.isoRooms.containsKey(var1)) {
         var3 = new IsoRoom();
         var3.rects.addAll(var2.rects);
         var3.RoomDef = var2.name;
         var3.def = var2;
         var3.layer = var2.level;
         IsoWorld.instance.CurrentCell.getRoomList().add(var3);
         if (var2.building == null) {
            var2.building = new BuildingDef();
            var2.building.ID = this.Buildings.size();
            var2.building.rooms.add(var2);
            var2.building.CalculateBounds(new ArrayList());
            var2.building.metaID = var2.building.calculateMetaID(this.cellX, this.cellY);
            this.Buildings.add(var2.building);
         }

         int var4 = var2.building.ID;
         this.isoRooms.put(var1, var3);
         if (!this.isoBuildings.containsKey(var4)) {
            var3.building = new IsoBuilding();
            var3.building.def = var2.building;
            this.isoBuildings.put(var4, var3.building);
            var3.building.CreateFrom(var2.building, this);
         } else {
            var3.building = (IsoBuilding)this.isoBuildings.get(var4);
         }

         return var3;
      } else {
         var3 = (IsoRoom)this.isoRooms.get(var1);
         return var3;
      }
   }

   /** @deprecated */
   @Deprecated
   public int getRoomAt(int var1, int var2, int var3) {
      Iterator var4 = this.Rooms.entrySet().iterator();

      while(var4.hasNext()) {
         Map.Entry var5 = (Map.Entry)var4.next();
         RoomDef var6 = (RoomDef)var5.getValue();

         for(int var7 = 0; var7 < var6.rects.size(); ++var7) {
            RoomDef.RoomRect var8 = (RoomDef.RoomRect)var6.rects.get(var7);
            if (var8.x <= var1 && var8.y <= var2 && var6.level == var3 && var8.getX2() > var1 && var8.getY2() > var2) {
               return (Integer)var5.getKey();
            }
         }
      }

      return -1;
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
