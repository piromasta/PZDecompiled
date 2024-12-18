package zombie.iso;

import gnu.trove.list.array.TShortArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.joml.Vector2f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ChunkMapFilenames;
import zombie.Lua.LuaManager;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.iso.areas.IsoRoom;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.network.ServerMap;

public final class BuildingDef {
   static final ArrayList<IsoGridSquare> squareChoices = new ArrayList();
   public final ArrayList<RoomDef> emptyoutside = new ArrayList();
   public KahluaTable table = null;
   public boolean seen = false;
   public boolean hasBeenVisited = false;
   public String stash = null;
   public int lootRespawnHour = -1;
   public TShortArrayList overlappedChunks;
   public boolean bAlarmed = false;
   public int bAlarmDecay = 10000000;
   public int x = 10000000;
   public int y = 10000000;
   public int x2 = -10000000;
   public int y2 = -10000000;
   public final ArrayList<RoomDef> rooms = new ArrayList();
   public Zone zone;
   public int food;
   public ArrayList<InventoryItem> items = new ArrayList();
   public HashSet<String> itemTypes = new HashSet();
   public long ID = 0L;
   private int keySpawned = 0;
   private int keyId = -1;
   public long metaID;
   private int minLevel = 100;
   private int maxLevel = -100;
   private final HashMap<Integer, Long> roofRoomID = new HashMap();
   public int collapseRectX = -1;
   public int collapseRectY = -1;
   public int collapseRectX2 = -1;
   public int collapseRectY2 = -1;

   public BuildingDef() {
      this.table = LuaManager.platform.newTable();
      this.setKeyId(Rand.Next(100000000));
   }

   public int getMinLevel() {
      if (this.minLevel != 100) {
         return this.minLevel;
      } else {
         for(int var1 = 0; var1 < this.rooms.size(); ++var1) {
            this.minLevel = Math.min(((RoomDef)this.rooms.get(var1)).level, this.minLevel);
         }

         return this.minLevel;
      }
   }

   public int getMaxLevel() {
      if (this.maxLevel != -100) {
         return this.maxLevel;
      } else {
         for(int var1 = 0; var1 < this.rooms.size(); ++var1) {
            this.maxLevel = Math.max(((RoomDef)this.rooms.get(var1)).level, this.maxLevel);
         }

         return this.maxLevel;
      }
   }

   public KahluaTable getTable() {
      return this.table;
   }

   public ArrayList<RoomDef> getRooms() {
      return this.rooms;
   }

   public RoomDef getRoom(String var1) {
      return this.getRoom(var1, false);
   }

   public RoomDef getRoom(String var1, boolean var2) {
      for(int var3 = 0; var3 < this.rooms.size(); ++var3) {
         RoomDef var4 = (RoomDef)this.rooms.get(var3);
         boolean var5 = var2 && var4.isKidsRoom();
         if (!var5 && var4.getName().equalsIgnoreCase(var1)) {
            return var4;
         }
      }

      return null;
   }

   public boolean isAllExplored() {
      for(int var1 = 0; var1 < this.rooms.size(); ++var1) {
         if (!((RoomDef)this.rooms.get(var1)).bExplored) {
            return false;
         }
      }

      return true;
   }

   public void setAllExplored(boolean var1) {
      for(int var2 = 0; var2 < this.rooms.size(); ++var2) {
         RoomDef var3 = (RoomDef)this.rooms.get(var2);
         var3.setExplored(var1);
      }

   }

   public RoomDef getFirstRoom() {
      return (RoomDef)this.rooms.get(0);
   }

   public int getChunkX() {
      return this.x / 8;
   }

   public int getChunkY() {
      return this.y / 8;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getX2() {
      return this.x2;
   }

   public int getY2() {
      return this.y2;
   }

   public int getW() {
      return this.x2 - this.x;
   }

   public int getH() {
      return this.y2 - this.y;
   }

   public long getID() {
      return this.ID;
   }

   public void refreshSquares() {
      for(int var1 = 0; var1 < this.rooms.size(); ++var1) {
         RoomDef var2 = (RoomDef)this.rooms.get(var1);
         var2.refreshSquares();
      }

   }

   public void CalculateBounds(ArrayList<RoomDef> var1) {
      this.x = 2147483647;
      this.y = 2147483647;
      this.x2 = -2147483648;
      this.y2 = -2147483648;

      int var2;
      RoomDef var3;
      int var4;
      RoomDef.RoomRect var5;
      for(var2 = 0; var2 < this.rooms.size(); ++var2) {
         var3 = (RoomDef)this.rooms.get(var2);

         for(var4 = 0; var4 < var3.rects.size(); ++var4) {
            var5 = (RoomDef.RoomRect)var3.rects.get(var4);
            if (var5.x < this.x) {
               this.x = var5.x;
            }

            if (var5.y < this.y) {
               this.y = var5.y;
            }

            if (var5.x + var5.w > this.x2) {
               this.x2 = var5.x + var5.w;
            }

            if (var5.y + var5.h > this.y2) {
               this.y2 = var5.y + var5.h;
            }
         }
      }

      for(var2 = 0; var2 < this.emptyoutside.size(); ++var2) {
         var3 = (RoomDef)this.emptyoutside.get(var2);

         for(var4 = 0; var4 < var3.rects.size(); ++var4) {
            var5 = (RoomDef.RoomRect)var3.rects.get(var4);
            if (var5.x < this.x) {
               this.x = var5.x;
            }

            if (var5.y < this.y) {
               this.y = var5.y;
            }

            if (var5.x + var5.w > this.x2) {
               this.x2 = var5.x + var5.w;
            }

            if (var5.y + var5.h > this.y2) {
               this.y2 = var5.y + var5.h;
            }
         }
      }

      var2 = this.x / 8;
      int var12 = this.y / 8;
      var4 = (this.x2 + 0) / 8;
      int var13 = (this.y2 + 0) / 8;
      this.overlappedChunks = new TShortArrayList((var4 - var2 + 1) * (var13 - var12 + 1) * 2);
      this.overlappedChunks.clear();
      var1.clear();
      var1.addAll(this.rooms);
      var1.addAll(this.emptyoutside);

      for(int var6 = 0; var6 < var1.size(); ++var6) {
         RoomDef var7 = (RoomDef)var1.get(var6);

         for(int var8 = 0; var8 < var7.rects.size(); ++var8) {
            RoomDef.RoomRect var9 = (RoomDef.RoomRect)var7.rects.get(var8);
            var2 = (var9.x - 1) / 8;
            var12 = (var9.y - 1) / 8;
            var4 = (var9.x + var9.w + 0) / 8;
            var13 = (var9.y + var9.h + 0) / 8;

            for(int var10 = var12; var10 <= var13; ++var10) {
               for(int var11 = var2; var11 <= var4; ++var11) {
                  if (!this.overlapsChunk(var11, var10)) {
                     this.overlappedChunks.add((short)var11);
                     this.overlappedChunks.add((short)var10);
                  }
               }
            }
         }
      }

   }

   public long calculateMetaID(int var1, int var2) {
      int var3 = 2147483647;
      int var4 = 2147483647;
      int var5 = 2147483647;
      ArrayList var6 = this.rooms.isEmpty() ? this.emptyoutside : this.rooms;

      for(int var7 = 0; var7 < var6.size(); ++var7) {
         RoomDef var8 = (RoomDef)var6.get(var7);
         if (var8.level <= var5) {
            if (var8.level < var5) {
               var3 = 2147483647;
               var4 = 2147483647;
            }

            var5 = var8.level;

            for(int var9 = 0; var9 < var8.rects.size(); ++var9) {
               RoomDef.RoomRect var10 = (RoomDef.RoomRect)var8.rects.get(var9);
               if (var10.x <= var3 && var10.y < var4) {
                  var3 = var10.x;
                  var4 = var10.y;
               }
            }
         }
      }

      var3 -= var1 * IsoCell.CellSizeInSquares;
      var4 -= var2 * IsoCell.CellSizeInSquares;
      return (long)var5 << 32 | (long)var4 << 16 | (long)var3;
   }

   public void recalculate() {
      this.food = 0;
      this.items.clear();
      this.itemTypes.clear();

      for(int var1 = 0; var1 < this.rooms.size(); ++var1) {
         IsoRoom var2 = ((RoomDef)this.rooms.get(var1)).getIsoRoom();

         for(int var3 = 0; var3 < var2.Containers.size(); ++var3) {
            ItemContainer var4 = (ItemContainer)var2.Containers.get(var3);

            for(int var5 = 0; var5 < var4.Items.size(); ++var5) {
               InventoryItem var6 = (InventoryItem)var4.Items.get(var5);
               this.items.add(var6);
               this.itemTypes.add(var6.getFullType());
               if (var6 instanceof Food) {
                  ++this.food;
               }
            }
         }
      }

   }

   public boolean overlapsChunk(int var1, int var2) {
      for(int var3 = 0; var3 < this.overlappedChunks.size(); var3 += 2) {
         if (var1 == this.overlappedChunks.get(var3) && var2 == this.overlappedChunks.get(var3 + 1)) {
            return true;
         }
      }

      return false;
   }

   public IsoGridSquare getFreeSquareInRoom() {
      squareChoices.clear();

      for(int var1 = 0; var1 < this.rooms.size(); ++var1) {
         RoomDef var2 = (RoomDef)this.rooms.get(var1);

         for(int var3 = 0; var3 < var2.rects.size(); ++var3) {
            RoomDef.RoomRect var4 = (RoomDef.RoomRect)var2.rects.get(var3);

            for(int var5 = var4.getX(); var5 < var4.getX2(); ++var5) {
               for(int var6 = var4.getY(); var6 < var4.getY2(); ++var6) {
                  IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var5, var6, var2.getZ());
                  if (var7 != null && var7.isFree(false)) {
                     squareChoices.add(var7);
                  }
               }
            }
         }
      }

      if (!squareChoices.isEmpty()) {
         return (IsoGridSquare)squareChoices.get(Rand.Next(squareChoices.size()));
      } else {
         return null;
      }
   }

   public boolean containsRoom(String var1) {
      for(int var2 = 0; var2 < this.rooms.size(); ++var2) {
         RoomDef var3 = (RoomDef)this.rooms.get(var2);
         if (var3.name.equals(var1)) {
            return true;
         }
      }

      return false;
   }

   public boolean isFullyStreamedIn() {
      for(int var1 = 0; var1 < this.overlappedChunks.size(); var1 += 2) {
         short var2 = this.overlappedChunks.get(var1);
         short var3 = this.overlappedChunks.get(var1 + 1);
         IsoChunk var4 = GameServer.bServer ? ServerMap.instance.getChunk(var2, var3) : IsoWorld.instance.CurrentCell.getChunk(var2, var3);
         if (var4 == null) {
            return false;
         }
      }

      return true;
   }

   public boolean isAnyChunkNewlyLoaded() {
      for(int var1 = 0; var1 < this.overlappedChunks.size(); var1 += 2) {
         short var2 = this.overlappedChunks.get(var1);
         short var3 = this.overlappedChunks.get(var1 + 1);
         IsoChunk var4 = GameServer.bServer ? ServerMap.instance.getChunk(var2, var3) : IsoWorld.instance.CurrentCell.getChunk(var2, var3);
         if (var4 == null) {
            return false;
         }

         if (var4.isNewChunk()) {
            return true;
         }
      }

      return false;
   }

   public Zone getZone() {
      return this.zone;
   }

   public int getKeyId() {
      return this.keyId;
   }

   public void setKeyId(int var1) {
      this.keyId = var1;
   }

   public int getKeySpawned() {
      return this.keySpawned;
   }

   public void setKeySpawned(int var1) {
      this.keySpawned = var1;
   }

   public boolean isHasBeenVisited() {
      return this.hasBeenVisited;
   }

   public void setHasBeenVisited(boolean var1) {
      if (var1 && !this.hasBeenVisited) {
         StashSystem.visitedBuilding(this);
      }

      this.hasBeenVisited = var1;
   }

   public boolean isAlarmed() {
      return this.bAlarmed;
   }

   public void setAlarmed(boolean var1) {
      this.bAlarmed = var1;
   }

   public RoomDef getRandomRoom(int var1) {
      return this.getRandomRoom(var1, false);
   }

   public RoomDef getRandomRoom(int var1, boolean var2) {
      RoomDef var3 = (RoomDef)this.getRooms().get(Rand.Next(0, this.getRooms().size()));
      boolean var4 = var2 && var3.isKidsRoom();
      if (!var4 && var1 > 0 && var3.area >= var1) {
         return var3;
      } else {
         int var5 = 0;

         do {
            if (var5 > 20) {
               return var3;
            }

            ++var5;
            var3 = (RoomDef)this.getRooms().get(Rand.Next(0, this.getRooms().size()));
            var4 = var2 && var3.isKidsRoom();
         } while(var4 || var3.area < var1);

         return var3;
      }
   }

   public float getClosestPoint(float var1, float var2, Vector2f var3) {
      float var4 = 3.4028235E38F;
      Vector2f var5 = new Vector2f();

      for(int var6 = 0; var6 < this.rooms.size(); ++var6) {
         RoomDef var7 = (RoomDef)this.rooms.get(var6);
         float var8 = var7.getClosestPoint(var1, var2, var5);
         if (var8 < var4) {
            var4 = var8;
            var3.set(var5);
         }
      }

      return var4;
   }

   public void Dispose() {
      Iterator var1 = this.rooms.iterator();

      while(var1.hasNext()) {
         RoomDef var2 = (RoomDef)var1.next();
         var2.Dispose();
      }

      this.emptyoutside.clear();
      this.rooms.clear();
   }

   public boolean containsXYZ(int var1, int var2, int var3) {
      return var1 >= this.x && var2 >= this.y && var1 < this.x + this.getW() && var2 < this.y + this.getH();
   }

   public void addRoomToCollapseRect(RoomDef var1) {
   }

   public void calculateCollapseRect() {
      if (this.collapseRectX2 == -1) {
         int var1 = this.getMaxLevel() + 1;
         float var2 = (float)Math.min(this.x, this.x2);
         float var3 = (float)Math.max(this.x, this.x2);
         float var4 = (float)Math.min(this.y, this.y2);
         float var5 = (float)Math.max(this.y, this.y2);
         int var6 = this.x - var1 * 3;
         int var7 = this.y - var1 * 3;
         int var8 = this.x2 - var1 * 3;
         int var9 = this.y2 - var1 * 3;
         var2 = Math.min(var2, (float)var6);
         var3 = Math.max(var3, (float)var8);
         var4 = Math.min(var4, (float)var7);
         var5 = Math.max(var5, (float)var9);
         if (this.collapseRectX == -1) {
            this.collapseRectX = (int)var2;
            this.collapseRectY = (int)var4;
            this.collapseRectX2 = (int)var3 - 1;
            this.collapseRectY2 = (int)var5 - 1;
         } else {
            this.collapseRectX = (int)Math.min(var2, (float)this.collapseRectX);
            this.collapseRectY = (int)Math.min(var4, (float)this.collapseRectY);
            this.collapseRectX2 = (int)Math.max(var3, (float)this.collapseRectX) - 1;
            this.collapseRectY2 = (int)Math.max(var5, (float)this.collapseRectY) - 1;
         }

      }
   }

   public void setInvalidateCacheForAllChunks(int var1, long var2) {
      IsoChunkMap var4 = IsoCell.getInstance().getChunkMap(var1);

      for(int var5 = 0; var5 < this.overlappedChunks.size(); var5 += 2) {
         short var6 = this.overlappedChunks.get(var5);
         short var7 = this.overlappedChunks.get(var5 + 1);
         IsoChunk var8 = var4.getChunk(var6 - var4.getWorldXMin(), var7 - var4.getWorldYMin());
         if (var8 != null) {
            var8.getRenderLevels(var1).invalidateAll(var2);
         }
      }

   }

   public void invalidateOverlappedChunkLevelsAbove(int var1, int var2, long var3) {
      IsoChunkMap var5 = IsoCell.getInstance().getChunkMap(var1);

      for(int var6 = 0; var6 < this.overlappedChunks.size(); var6 += 2) {
         short var7 = this.overlappedChunks.get(var6);
         short var8 = this.overlappedChunks.get(var6 + 1);
         IsoChunk var9 = var5.getChunk(var7 - var5.getWorldXMin(), var8 - var5.getWorldYMin());
         if (var9 != null) {
            FBORenderLevels var10 = var9.getRenderLevels(var1);

            for(int var11 = PZMath.max(var2, var9.minLevel); var11 <= var9.maxLevel; ++var11) {
               var10.invalidateLevel(var11, var3);
            }
         }
      }

   }

   public boolean isAdjacent(BuildingDef var1) {
      for(int var2 = 0; var2 < this.rooms.size(); ++var2) {
         RoomDef var3 = (RoomDef)this.rooms.get(var2);

         for(int var4 = 0; var4 < var1.rooms.size(); ++var4) {
            RoomDef var5 = (RoomDef)var1.rooms.get(var4);
            if (var3.level == var5.level && var3.isAdjacent(var5)) {
               return true;
            }
         }
      }

      return false;
   }

   public void addRoomsOf(BuildingDef var1, ArrayList<RoomDef> var2) {
      int var3;
      RoomDef var4;
      for(var3 = 0; var3 < var1.rooms.size(); ++var3) {
         var4 = (RoomDef)var1.rooms.get(var3);
         var4.building = this;
         this.rooms.add(var4);
      }

      for(var3 = 0; var3 < var1.emptyoutside.size(); ++var3) {
         var4 = (RoomDef)var1.emptyoutside.get(var3);
         var4.building = this;
         this.emptyoutside.add(var4);
      }

      this.minLevel = 100;
      this.maxLevel = -100;
      this.CalculateBounds(var2);
      var1.minLevel = 100;
      var1.maxLevel = -100;
      var1.rooms.clear();
      var1.emptyoutside.clear();
   }

   public long getRoofRoomID(int var1) {
      Long var2 = (Long)this.roofRoomID.get(var1);
      if (var2 == null) {
         int var3 = this.x / IsoCell.CellSizeInSquares;
         int var4 = this.y / IsoCell.CellSizeInSquares;
         String var5 = ChunkMapFilenames.instance.getHeader(var3, var4);
         LotHeader var6 = (LotHeader)IsoLot.InfoHeaders.get(var5);
         if (var6 == null) {
            this.roofRoomID.put(var1, -1L);
            return -1L;
         }

         int var7 = var6.Buildings.indexOf(this);
         if (var7 == -1) {
            this.roofRoomID.put(var1, -1L);
            return -1L;
         }

         int var8 = var6.Rooms.size() + var7 * 64 + 32 + var1;
         var2 = RoomID.makeID(var3, var4, var8);
         this.roofRoomID.put(var1, var2);
      }

      return var2;
   }

   public boolean isEntirelyEmptyOutside() {
      return this.rooms.isEmpty() && !this.emptyoutside.isEmpty();
   }
}
