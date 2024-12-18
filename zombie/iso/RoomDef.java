package zombie.iso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.joml.Vector2f;
import zombie.Lua.LuaManager;
import zombie.core.math.PZMath;
import zombie.iso.areas.IsoRoom;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.util.list.PZArrayUtil;

public final class RoomDef {
   private static final ArrayList<IsoGridSquare> squareChoices = new ArrayList();
   public boolean bExplored = false;
   public boolean bDoneSpawn = false;
   public int IndoorZombies = 0;
   public int spawnCount = -1;
   public boolean bLightsActive = false;
   public String name;
   public int level;
   public BuildingDef building;
   public long ID;
   public final ArrayList<RoomRect> rects = new ArrayList(1);
   public final ArrayList<MetaObject> objects = new ArrayList(0);
   public int x = 100000;
   public int y = 100000;
   public int x2 = -10000;
   public int y2 = -10000;
   public int area;
   private final HashMap<String, Integer> proceduralSpawnedContainer = new HashMap();
   private boolean roofFixed = false;
   public long metaID;

   public RoomDef(long var1, String var3) {
      this.ID = var1;
      this.name = var3;
   }

   public long getID() {
      return this.ID;
   }

   public boolean isExplored() {
      return this.bExplored;
   }

   public boolean isInside(int var1, int var2, int var3) {
      int var4 = this.building.x;
      int var5 = this.building.y;

      for(int var6 = 0; var6 < this.rects.size(); ++var6) {
         int var7 = ((RoomRect)this.rects.get(var6)).x;
         int var8 = ((RoomRect)this.rects.get(var6)).y;
         int var9 = ((RoomRect)this.rects.get(var6)).getX2();
         int var10 = ((RoomRect)this.rects.get(var6)).getY2();
         if (var1 >= var7 && var2 >= var8 && var1 < var9 && var2 < var10 && var3 == this.level) {
            return true;
         }
      }

      return false;
   }

   public boolean intersects(int var1, int var2, int var3, int var4) {
      for(int var5 = 0; var5 < this.rects.size(); ++var5) {
         RoomRect var6 = (RoomRect)this.rects.get(var5);
         if (var1 + var3 > var6.getX() && var1 < var6.getX2() && var2 + var4 > var6.getY() && var2 < var6.getY2()) {
            return true;
         }
      }

      return false;
   }

   public boolean isAdjacent(RoomDef var1) {
      for(int var2 = 0; var2 < var1.rects.size(); ++var2) {
         RoomRect var3 = (RoomRect)var1.rects.get(var2);
         if (this.intersects(var3.getX() - 1, var3.getY() - 1, var3.getW() + 2, var3.getH() + 2)) {
            return true;
         }
      }

      return false;
   }

   public float getAreaOverlapping(IsoChunk var1) {
      return this.getAreaOverlapping(var1.wx * 8, var1.wy * 8, 8, 8);
   }

   public float getAreaOverlapping(int var1, int var2, int var3, int var4) {
      int var5 = 0;
      int var6 = 0;

      for(int var7 = 0; var7 < this.rects.size(); ++var7) {
         RoomRect var8 = (RoomRect)this.rects.get(var7);
         var5 += var8.w * var8.h;
         int var9 = Math.max(var1, var8.x);
         int var10 = Math.max(var2, var8.y);
         int var11 = Math.min(var1 + var3, var8.x + var8.w);
         int var12 = Math.min(var2 + var4, var8.y + var8.h);
         if (var11 >= var9 && var12 >= var10) {
            var6 += (var11 - var9) * (var12 - var10);
         }
      }

      if (var6 <= 0) {
         return 0.0F;
      } else {
         return (float)var6 / (float)var5;
      }
   }

   public void forEachChunk(BiConsumer<RoomDef, IsoChunk> var1) {
      HashSet var2 = new HashSet();

      for(int var3 = 0; var3 < this.rects.size(); ++var3) {
         RoomRect var4 = (RoomRect)this.rects.get(var3);
         int var5 = var4.x / 8;
         int var6 = var4.y / 8;
         int var7 = (var4.x + var4.w) / 8;
         int var8 = (var4.y + var4.h) / 8;
         if (PZMath.coordmodulo(var4.x + var4.w, 8) == 0) {
            --var7;
         }

         if (PZMath.coordmodulo(var4.y + var4.h, 8) == 0) {
            --var8;
         }

         for(int var9 = var6; var9 <= var8; ++var9) {
            for(int var10 = var5; var10 <= var7; ++var10) {
               IsoChunk var11 = GameServer.bServer ? ServerMap.instance.getChunk(var10, var9) : IsoWorld.instance.CurrentCell.getChunk(var10, var9);
               if (var11 != null) {
                  var2.add(var11);
               }
            }
         }
      }

      var2.forEach((var2x) -> {
         var1.accept(this, var2x);
      });
      var2.clear();
   }

   public IsoRoom getIsoRoom() {
      return IsoWorld.instance.MetaGrid.getMetaGridFromTile(this.x, this.y).info.getRoom(this.ID);
   }

   public ArrayList<MetaObject> getObjects() {
      return this.objects;
   }

   public ArrayList<MetaObject> getMetaObjects() {
      return this.objects;
   }

   public void refreshSquares() {
      this.getIsoRoom().refreshSquares();
   }

   public BuildingDef getBuilding() {
      return this.building;
   }

   public void setBuilding(BuildingDef var1) {
      this.building = var1;
   }

   public String getName() {
      return this.name;
   }

   public ArrayList<RoomRect> getRects() {
      return this.rects;
   }

   public int getY() {
      return this.y;
   }

   public int getX() {
      return this.x;
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

   public int getZ() {
      return this.level;
   }

   public void CalculateBounds() {
      this.x = 10000000;
      this.y = 10000000;
      this.x2 = -1000000;
      this.y2 = -1000000;
      this.area = 0;

      for(int var1 = 0; var1 < this.rects.size(); ++var1) {
         RoomRect var2 = (RoomRect)this.rects.get(var1);
         if (var2.x < this.x) {
            this.x = var2.x;
         }

         if (var2.y < this.y) {
            this.y = var2.y;
         }

         if (var2.x + var2.w > this.x2) {
            this.x2 = var2.x + var2.w;
         }

         if (var2.y + var2.h > this.y2) {
            this.y2 = var2.y + var2.h;
         }

         this.area += var2.w * var2.h;
      }

   }

   public long calculateMetaID(int var1, int var2) {
      int var3 = 2147483647;
      int var4 = 2147483647;

      for(int var5 = 0; var5 < this.rects.size(); ++var5) {
         RoomRect var6 = (RoomRect)this.rects.get(var5);
         if (var6.x <= var3 && var6.y < var4) {
            var3 = var6.x;
            var4 = var6.y;
         }
      }

      var3 -= var1 * IsoCell.CellSizeInSquares;
      var4 -= var2 * IsoCell.CellSizeInSquares;
      return (long)this.level << 32 | (long)var4 << 16 | (long)var3;
   }

   public void offset(int var1, int var2) {
      this.x += var1;
      this.y += var2;
      this.x2 += var1;
      this.y2 += var2;

      for(int var3 = 0; var3 < this.rects.size(); ++var3) {
         RoomRect var4 = (RoomRect)this.rects.get(var3);
         var4.x += var1;
         var4.y += var2;
      }

   }

   public int getArea() {
      return this.area;
   }

   public void setExplored(boolean var1) {
      this.bExplored = var1;
   }

   public IsoGridSquare getFreeSquare() {
      return this.getRandomSquare((var0) -> {
         return var0.isFree(false);
      });
   }

   public IsoGridSquare getExtraFreeSquare() {
      return this.getRandomSquare((var0) -> {
         return var0.isFree(false) && var0.getObjects().size() < 2 && !var0.HasStairs() && var0.hasFloor();
      });
   }

   public IsoGridSquare getFreeUnoccupiedSquare() {
      return this.getRandomSquare((var0) -> {
         return var0.isFree(true);
      });
   }

   public IsoGridSquare getRandomSquare(Predicate<IsoGridSquare> var1) {
      squareChoices.clear();

      for(int var2 = 0; var2 < this.rects.size(); ++var2) {
         RoomRect var3 = (RoomRect)this.rects.get(var2);

         for(int var4 = var3.getX(); var4 < var3.getX2(); ++var4) {
            for(int var5 = var3.getY(); var5 < var3.getY2(); ++var5) {
               IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var4, var5, this.getZ());
               if (var6 != null && var1 != null && var1.test(var6) || var1 == null) {
                  squareChoices.add(var6);
               }
            }
         }
      }

      return (IsoGridSquare)PZArrayUtil.pickRandom((List)squareChoices);
   }

   public boolean isEmptyOutside() {
      return "emptyoutside".equalsIgnoreCase(this.name);
   }

   public HashMap<String, Integer> getProceduralSpawnedContainer() {
      return this.proceduralSpawnedContainer;
   }

   public RoomRect getRoomRect(int var1, int var2, int var3) {
      ArrayList var4 = this.rects;

      for(int var5 = 0; var5 < var4.size(); ++var5) {
         RoomRect var6 = (RoomRect)var4.get(var5);
         if (var6.x <= var1 && var6.y <= var2 && var6.getX2() >= var1 && var6.getY2() >= var2) {
            return var6;
         }
      }

      return null;
   }

   public boolean isRoofFixed() {
      return this.roofFixed;
   }

   public void setRoofFixed(boolean var1) {
      this.roofFixed = var1;
   }

   public float getClosestPoint(float var1, float var2, Vector2f var3) {
      float var4 = 3.4028235E38F;
      Vector2f var5 = new Vector2f();

      for(int var6 = 0; var6 < this.rects.size(); ++var6) {
         RoomRect var7 = (RoomRect)this.rects.get(var6);
         float var8 = var7.getClosestPoint(var1, var2, var5);
         if (var8 < var4) {
            var4 = var8;
            var3.set(var5);
         }
      }

      return var4;
   }

   public void Dispose() {
      this.building = null;
      this.rects.clear();
      this.objects.clear();
      this.proceduralSpawnedContainer.clear();
   }

   public boolean isKidsRoom() {
      if (Objects.equals(this.name, "kidsbedroom")) {
         return true;
      } else if (this.getBuilding().getRooms().size() > RandomizedBuildingBase.maximumRoomCount) {
         return false;
      } else {
         ArrayList var1 = new ArrayList();
         var1.add("furniture_bedding_01_36");
         var1.add("furniture_bedding_01_38");
         var1.add("furniture_seating_indoor_02_12");
         var1.add("furniture_seating_indoor_02_13");
         var1.add("furniture_seating_indoor_02_14");
         var1.add("furniture_seating_indoor_02_15");
         var1.add("walls_decoration_01_50");
         var1.add("walls_decoration_01_51");
         var1.add("location_community_school_01_62");
         var1.add("location_community_school_01_63");
         var1.add("floors_rugs_01_63");
         var1.add("floors_rugs_01_64");
         var1.add("floors_rugs_01_65");
         var1.add("floors_rugs_01_66");
         var1.add("floors_rugs_01_67");
         var1.add("floors_rugs_01_68");
         var1.add("floors_rugs_01_69");
         var1.add("floors_rugs_01_70");
         var1.add("floors_rugs_01_71");

         for(int var2 = this.x; var2 < this.x2; ++var2) {
            for(int var3 = this.y; var3 < this.y2; ++var3) {
               IsoGridSquare var4 = LuaManager.GlobalObject.getCell().getGridSquare(var2, var3, this.level);
               if (var4 != null) {
                  for(int var5 = 0; var5 < var4.getObjects().size(); ++var5) {
                     IsoObject var6 = (IsoObject)var4.getObjects().get(var5);
                     if (var6 != null && var6.getSprite() != null && var6.getSprite().name != null && var1.contains(var6.getSprite().name)) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   public static final class RoomRect {
      public int x;
      public int y;
      public int w;
      public int h;

      public RoomRect(int var1, int var2, int var3, int var4) {
         this.x = var1;
         this.y = var2;
         this.w = var3;
         this.h = var4;
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }

      public int getX2() {
         return this.x + this.w;
      }

      public int getY2() {
         return this.y + this.h;
      }

      public int getW() {
         return this.w;
      }

      public int getH() {
         return this.h;
      }

      public float getClosestPoint(float var1, float var2, Vector2f var3) {
         float var4 = 3.4028235E38F;
         var4 = this.getClosestPointOnEdge((float)this.x + 0.5F, (float)this.y + 0.5F, (float)(this.x + this.w) - 0.5F, (float)this.y + 0.5F, var1, var2, var4, var3);
         var4 = this.getClosestPointOnEdge((float)(this.x + this.w) - 0.5F, (float)this.y + 0.5F, (float)(this.x + this.w) - 0.5F, (float)(this.y + this.h) - 0.5F, var1, var2, var4, var3);
         var4 = this.getClosestPointOnEdge((float)(this.x + this.w) - 0.5F, (float)(this.y + this.h) - 0.5F, (float)this.x + 0.5F, (float)(this.y + this.h) - 0.5F, var1, var2, var4, var3);
         var4 = this.getClosestPointOnEdge((float)this.x + 0.5F, (float)(this.y + this.h) - 0.5F, (float)this.x + 0.5F, (float)this.y + 0.5F, var1, var2, var4, var3);
         return var4;
      }

      private float getClosestPointOnEdge(float var1, float var2, float var3, float var4, float var5, float var6, float var7, Vector2f var8) {
         double var9 = (double)((var5 - var1) * (var3 - var1) + (var6 - var2) * (var4 - var2)) / (Math.pow((double)(var3 - var1), 2.0) + Math.pow((double)(var4 - var2), 2.0));
         double var11 = (double)var1 + var9 * (double)(var3 - var1);
         double var13 = (double)var2 + var9 * (double)(var4 - var2);
         if (var9 <= 0.0) {
            var11 = (double)var1;
            var13 = (double)var2;
         } else if (var9 >= 1.0) {
            var11 = (double)var3;
            var13 = (double)var4;
         }

         double var15 = ((double)var5 - var11) * ((double)var5 - var11) + ((double)var6 - var13) * ((double)var6 - var13);
         if (var15 < (double)var7) {
            if (var1 == var3) {
               var13 = (double)((float)((int)var13) + 0.5F);
            } else {
               var11 = (double)((float)((int)var11) + 0.5F);
            }

            var8.set(var11, var13);
            return (float)var15;
         } else {
            return var7;
         }
      }
   }
}
