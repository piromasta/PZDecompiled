package zombie.iso.areas;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import zombie.VirtualZombieManager;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.ItemContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoWorld;
import zombie.iso.MetaObject;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class IsoRoom {
   private static final ArrayList<IsoGridSquare> tempSquares = new ArrayList();
   public final Vector<IsoGridSquare> Beds = new Vector();
   public Rectangle bounds;
   public IsoBuilding building = null;
   public final ArrayList<ItemContainer> Containers = new ArrayList();
   public final ArrayList<IsoWindow> Windows = new ArrayList();
   public final Vector<IsoRoomExit> Exits = new Vector();
   public int layer;
   public String RoomDef = "none";
   public final Vector<IsoGridSquare> TileList = new Vector();
   public int transparentWalls = 0;
   public final ArrayList<IsoLightSwitch> lightSwitches = new ArrayList();
   public final ArrayList<IsoRoomLight> roomLights = new ArrayList();
   public final ArrayList<IsoObject> WaterSources = new ArrayList();
   public int seen = 1000000000;
   public int visited = 1000000000;
   public RoomDef def;
   public final ArrayList<RoomDef.RoomRect> rects = new ArrayList(1);
   public final ArrayList<IsoGridSquare> Squares = new ArrayList();

   public IsoRoom() {
   }

   public IsoBuilding getBuilding() {
      return this.building;
   }

   public String getName() {
      return this.RoomDef;
   }

   public IsoBuilding CreateBuilding(IsoCell var1) {
      IsoBuilding var2 = new IsoBuilding(var1);
      this.AddToBuilding(var2);
      return var2;
   }

   public boolean isInside(int var1, int var2, int var3) {
      for(int var4 = 0; var4 < this.rects.size(); ++var4) {
         int var5 = ((RoomDef.RoomRect)this.rects.get(var4)).x;
         int var6 = ((RoomDef.RoomRect)this.rects.get(var4)).y;
         int var7 = ((RoomDef.RoomRect)this.rects.get(var4)).getX2();
         int var8 = ((RoomDef.RoomRect)this.rects.get(var4)).getY2();
         if (var1 >= var5 && var2 >= var6 && var1 < var7 && var2 < var8 && var3 == this.layer) {
            return true;
         }
      }

      return false;
   }

   public IsoGridSquare getFreeTile() {
      boolean var1 = false;
      IsoGridSquare var2 = null;
      int var3 = 100;

      while(!var1 && var3 > 0) {
         --var3;
         var1 = true;
         if (this.TileList.isEmpty()) {
            return null;
         }

         var2 = (IsoGridSquare)this.TileList.get(Rand.Next(this.TileList.size()));

         for(int var4 = 0; var4 < this.Exits.size(); ++var4) {
            if (var2.getX() == ((IsoRoomExit)this.Exits.get(var4)).x && var2.getY() == ((IsoRoomExit)this.Exits.get(var4)).y) {
               var1 = false;
            }
         }

         if (var1 && !var2.isFree(true)) {
            var1 = false;
         }
      }

      return var3 < 0 ? null : var2;
   }

   void AddToBuilding(IsoBuilding var1) {
      this.building = var1;
      var1.AddRoom(this);
      Iterator var2 = this.Exits.iterator();

      while(var2.hasNext()) {
         IsoRoomExit var3 = (IsoRoomExit)var2.next();
         if (var3.To.From != null && var3.To.From.building == null) {
            var3.To.From.AddToBuilding(var1);
         }
      }

   }

   public ArrayList<IsoObject> getWaterSources() {
      return this.WaterSources;
   }

   public void setWaterSources(ArrayList<IsoObject> var1) {
      this.WaterSources.clear();
      this.WaterSources.addAll(var1);
   }

   public boolean hasWater() {
      if (this.WaterSources.isEmpty()) {
         return false;
      } else {
         Iterator var1 = this.WaterSources.iterator();

         while(var1 != null && var1.hasNext()) {
            IsoObject var2 = (IsoObject)var1.next();
            if (var2.hasWater()) {
               return true;
            }
         }

         return false;
      }
   }

   public void useWater() {
      if (!this.WaterSources.isEmpty()) {
         Iterator var1 = this.WaterSources.iterator();

         while(var1 != null && var1.hasNext()) {
            IsoObject var2 = (IsoObject)var1.next();
            if (var2.hasWater()) {
               var2.useWater(1);
               break;
            }
         }

      }
   }

   public ArrayList<IsoWindow> getWindows() {
      return this.Windows;
   }

   public void addSquare(IsoGridSquare var1) {
      if (!this.Squares.contains(var1)) {
         this.Squares.add(var1);
      }
   }

   public void refreshSquares() {
      this.Windows.clear();
      this.Containers.clear();
      this.WaterSources.clear();
      this.Exits.clear();
      tempSquares.clear();
      tempSquares.addAll(this.Squares);
      this.Squares.clear();

      for(int var1 = 0; var1 < tempSquares.size(); ++var1) {
         this.addSquare((IsoGridSquare)tempSquares.get(var1));
      }

   }

   private void addExitTo(IsoGridSquare var1, IsoGridSquare var2) {
      IsoRoom var3 = null;
      IsoRoom var4 = null;
      if (var1 != null) {
         var3 = var1.getRoom();
      }

      if (var2 != null) {
         var4 = var2.getRoom();
      }

      if (var3 != null || var4 != null) {
         IsoRoom var5 = var3;
         if (var3 == null) {
            var5 = var4;
         }

         IsoRoomExit var6 = new IsoRoomExit(var5, var1.getX(), var1.getY(), var1.getZ());
         var6.type = IsoRoomExit.ExitType.Door;
         if (var5 == var3) {
            if (var4 != null) {
               IsoRoomExit var7 = var4.getExitAt(var2.getX(), var2.getY(), var2.getZ());
               if (var7 == null) {
                  var7 = new IsoRoomExit(var4, var2.getX(), var2.getY(), var2.getZ());
                  var4.Exits.add(var7);
               }

               var6.To = var7;
            } else {
               var3.building.Exits.add(var6);
               if (var2 != null) {
                  var6.To = new IsoRoomExit(var6, var2.getX(), var2.getY(), var2.getZ());
               }
            }

            var3.Exits.add(var6);
         } else {
            var4.building.Exits.add(var6);
            if (var2 != null) {
               var6.To = new IsoRoomExit(var6, var2.getX(), var2.getY(), var2.getZ());
            }

            var4.Exits.add(var6);
         }

      }
   }

   private IsoRoomExit getExitAt(int var1, int var2, int var3) {
      for(int var4 = 0; var4 < this.Exits.size(); ++var4) {
         IsoRoomExit var5 = (IsoRoomExit)this.Exits.get(var4);
         if (var5.x == var1 && var5.y == var2 && var5.layer == var3) {
            return var5;
         }
      }

      return null;
   }

   public void removeSquare(IsoGridSquare var1) {
      this.Squares.remove(var1);
      IsoRoomExit var2 = this.getExitAt(var1.getX(), var1.getY(), var1.getZ());
      if (var2 != null) {
         this.Exits.remove(var2);
         if (var2.To != null) {
            var2.From = null;
         }

         if (this.building.Exits.contains(var2)) {
            this.building.Exits.remove(var2);
         }
      }

      for(int var3 = 0; var3 < var1.getObjects().size(); ++var3) {
         IsoObject var4 = (IsoObject)var1.getObjects().get(var3);
         if (var4 instanceof IsoLightSwitch) {
            this.lightSwitches.remove(var4);
         }
      }

   }

   public void spawnZombies() {
      VirtualZombieManager.instance.addZombiesToMap(1, this.def, false);
   }

   public void onSee() {
      if (!GameClient.bClient) {
         BuildingDef var1 = this.getBuilding().getDef();
         if (var1 != null && StashSystem.isStashBuilding(var1)) {
            StashSystem.visitedBuilding(var1);
         }
      }

      for(int var3 = 0; var3 < this.getBuilding().Rooms.size(); ++var3) {
         IsoRoom var2 = (IsoRoom)this.getBuilding().Rooms.elementAt(var3);
         if (VirtualZombieManager.instance.shouldSpawnZombiesOnLevel(var2.def.level)) {
            if (var2 != null && !var2.def.bExplored) {
               var2.def.bExplored = true;
            }

            IsoWorld.instance.getCell().roomSpotted(var2);
         }
      }

   }

   public Vector<IsoGridSquare> getTileList() {
      return this.TileList;
   }

   public ArrayList<IsoGridSquare> getSquares() {
      return this.Squares;
   }

   public ArrayList<ItemContainer> getContainer() {
      return this.Containers;
   }

   public IsoGridSquare getRandomSquare() {
      return this.Squares.isEmpty() ? null : (IsoGridSquare)this.Squares.get(Rand.Next(this.Squares.size()));
   }

   public IsoGridSquare getRandomFreeSquare() {
      int var1 = 100;
      IsoGridSquare var2 = null;
      if (GameServer.bServer) {
         while(var1 > 0) {
            var2 = IsoWorld.instance.CurrentCell.getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
            if (var2 != null && var2.getRoom() == this && var2.isFree(true)) {
               return var2;
            }

            --var1;
         }

         return null;
      } else if (this.Squares.isEmpty()) {
         return null;
      } else {
         while(var1 > 0) {
            var2 = (IsoGridSquare)this.Squares.get(Rand.Next(this.Squares.size()));
            if (var2.isFree(true)) {
               return var2;
            }

            --var1;
         }

         return null;
      }
   }

   public IsoGridSquare getRandomDoorFreeSquare() {
      int var1 = 100;
      IsoGridSquare var2 = null;
      if (GameServer.bServer) {
         while(var1 > 0) {
            var2 = IsoWorld.instance.CurrentCell.getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
            if (var2 != null && var2.getRoom() == this && var2.isFree(true) && var2.isGoodSquare()) {
               return var2;
            }

            --var1;
         }

         return null;
      } else if (this.Squares.isEmpty()) {
         return null;
      } else {
         while(var1 > 0) {
            var2 = (IsoGridSquare)this.Squares.get(Rand.Next(this.Squares.size()));
            if (var2.isFree(true) && var2.isGoodSquare()) {
               return var2;
            }

            --var1;
         }

         return null;
      }
   }

   public IsoGridSquare getRandomWallFreeSquare() {
      int var1 = 100;
      IsoGridSquare var2 = null;
      if (GameServer.bServer) {
         while(var1 > 0) {
            var2 = IsoWorld.instance.CurrentCell.getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
            if (var2 != null && var2.getRoom() == this && var2.isFree(true) && !var2.isWallSquare() && var2.isGoodSquare()) {
               return var2;
            }

            --var1;
         }

         return null;
      } else if (this.Squares.isEmpty()) {
         return null;
      } else {
         while(var1 > 0) {
            var2 = (IsoGridSquare)this.Squares.get(Rand.Next(this.Squares.size()));
            if (var2.isFree(true) && !var2.isWallSquare() && var2.isGoodSquare()) {
               return var2;
            }

            --var1;
         }

         return null;
      }
   }

   public IsoGridSquare getRandomWallFreePairSquare(IsoDirections var1, boolean var2) {
      int var3 = 100;
      IsoGridSquare var4 = null;
      if (GameServer.bServer) {
         while(var3 > 0) {
            var4 = IsoWorld.instance.CurrentCell.getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
            if (var4 != null && var4.getRoom() == this && var4.isFree(true) && var4.isFreeWallPair(var1, var2)) {
               return var4;
            }

            --var3;
         }

         return null;
      } else if (this.Squares.isEmpty()) {
         return null;
      } else {
         while(var3 > 0) {
            var4 = (IsoGridSquare)this.Squares.get(Rand.Next(this.Squares.size()));
            if (var4.isFree(true) && var4.isFreeWallPair(var1, var2)) {
               return var4;
            }

            --var3;
         }

         return null;
      }
   }

   public IsoGridSquare getRandomWallSquare() {
      int var1 = 100;
      IsoGridSquare var2 = null;
      if (GameServer.bServer) {
         while(var1 > 0) {
            var2 = IsoWorld.instance.CurrentCell.getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
            if (var2 != null && var2.getRoom() == this && var2.isFree(true) && var2.isFreeWallSquare() && !var2.isDoorSquare()) {
               return var2;
            }

            --var1;
         }

         return null;
      } else if (this.Squares.isEmpty()) {
         return null;
      } else {
         while(var1 > 0) {
            var2 = (IsoGridSquare)this.Squares.get(Rand.Next(this.Squares.size()));
            if (var2.isFree(true) && var2.isFreeWallSquare() && !var2.isDoorSquare()) {
               return var2;
            }

            --var1;
         }

         return null;
      }
   }

   public IsoGridSquare getRandomDoorAndWallFreeSquare() {
      int var1 = 100;
      IsoGridSquare var2 = null;
      if (GameServer.bServer) {
         while(var1 > 0) {
            var2 = IsoWorld.instance.CurrentCell.getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
            if (var2 != null && var2.getRoom() == this && var2.isFree(true) && !var2.isDoorOrWallSquare() && var2.getObjects().size() < 2) {
               return var2;
            }

            --var1;
         }

         return null;
      } else if (this.Squares.isEmpty()) {
         return null;
      } else {
         while(var1 > 0) {
            var2 = (IsoGridSquare)this.Squares.get(Rand.Next(this.Squares.size()));
            if (var2.isFree(true) && !var2.isDoorOrWallSquare() && var2.getObjects().size() < 2) {
               return var2;
            }

            --var1;
         }

         return null;
      }
   }

   public boolean hasLightSwitches() {
      if (!this.lightSwitches.isEmpty()) {
         return true;
      } else {
         for(int var1 = 0; var1 < this.def.objects.size(); ++var1) {
            if (((MetaObject)this.def.objects.get(var1)).getType() == 7) {
               return true;
            }
         }

         return false;
      }
   }

   public void createLights(boolean var1) {
      if (this.roomLights.isEmpty()) {
         for(int var2 = 0; var2 < this.def.rects.size(); ++var2) {
            RoomDef.RoomRect var3 = (RoomDef.RoomRect)this.def.rects.get(var2);
            IsoRoomLight var4 = new IsoRoomLight(this, var3.x, var3.y, this.def.level, var3.w, var3.h);
            this.roomLights.add(var4);
         }

      }
   }

   public IsoRoomLight findRoomLightByID(int var1) {
      for(int var2 = 0; var2 < this.roomLights.size(); ++var2) {
         IsoRoomLight var3 = (IsoRoomLight)this.roomLights.get(var2);
         if (var3.ID == var1) {
            return var3;
         }
      }

      return null;
   }

   public RoomDef getRoomDef() {
      return this.def;
   }

   public ArrayList<IsoLightSwitch> getLightSwitches() {
      return this.lightSwitches;
   }

   public boolean spawnRandomWorkstation() {
      IsoGridSquare var1 = this.getRandomWallSquare();
      if (var1 == null) {
         var1 = this.getRandomDoorFreeSquare();
      }

      if (var1 != null) {
         var1.spawnRandomWorkstation();
         return true;
      } else {
         return false;
      }
   }

   public boolean spawnRandom2TileWorkstation() {
      boolean var1 = false;
      Object var2 = null;
      if (Rand.NextBool(3)) {
         int var3 = Rand.Next(2);
         switch (var3) {
            case 0:
               var1 = this.addMetalWorkbench();
               break;
            case 1:
               var1 = this.addPotteryWheel();
         }
      }

      return var1 ? true : this.spawnRandomWorkstation();
   }

   public boolean addMetalWorkbench() {
      return this.add2TileBench("Base.Metalworkbench", "crafted_02_2", "crafted_02_3", "crafted_02_0", "crafted_02_1", true);
   }

   public boolean addPotteryWheel() {
      return Rand.NextBool(2) ? this.addOldPotteryWheel() : this.addModernPotteryWheel();
   }

   public boolean addOldPotteryWheel() {
      return this.add2TileBench("Base.Pottery_Wheel", "crafted_01_64", "crafted_01_65", "crafted_01_66", "crafted_01_67", false);
   }

   public boolean addModernPotteryWheel() {
      return Rand.NextBool(2) ? this.add2TileBench("Base.Pottery_Wheel_Modern", "crafted_01_92", "crafted_01_93", "crafted_01_94", "crafted_01_95", false) : this.add2TileBench("Base.Pottery_Wheel_Modern", "crafted_01_88", "crafted_01_89", "crafted_01_90", "crafted_01_91", false);
   }

   public boolean add2TileBench(String var1, String var2, String var3, String var4, String var5, boolean var6) {
      IsoGridSquare var7 = null;
      boolean var8 = false;
      if (Rand.NextBool(2)) {
         var8 = true;
         var7 = this.getRandomWallFreePairSquare(IsoDirections.N, var6);
      }

      if (var7 == null) {
         var7 = this.getRandomWallFreePairSquare(IsoDirections.E, var6);
      }

      if (!var8 && var7 == null) {
         var8 = true;
         var7 = this.getRandomWallFreePairSquare(IsoDirections.N, var6);
      }

      if (var7 == null) {
         return false;
      } else {
         IsoGridSquare var9;
         if (var8) {
            var9 = var7.getAdjacentSquare(IsoDirections.N);
            if (var9 == null) {
               return false;
            } else {
               var7.addWorkstationEntity(var1, var2);
               var9.addWorkstationEntity(var1, var3);
               return true;
            }
         } else {
            var9 = var7.getAdjacentSquare(IsoDirections.E);
            if (var9 == null) {
               return false;
            } else {
               var7.addWorkstationEntity(var1, var4);
               var9.addWorkstationEntity(var1, var5);
               return true;
            }
         }
      }
   }
}
