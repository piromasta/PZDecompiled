package zombie.pathfind.nativeCode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.ai.astar.Mover;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.gameStates.DebugChunkState;
import zombie.gameStates.IngameState;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.pathfind.IPathfinder;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.TestRequest;
import zombie.vehicles.BaseVehicle;

public class PathfindNative {
   public static final PathfindNative instance = new PathfindNative();
   public static boolean USE_NATIVE_CODE = true;
   private final HashMap<BaseVehicle, VehicleState> vehicleState = new HashMap();
   private int testZ = 0;
   private final ByteBuffer requestBB = ByteBuffer.allocateDirect(46);
   private final PathFindRequest request = new PathFindRequest();
   private final TestRequest finder = new TestRequest();
   private boolean bTestRequestAdded = false;

   public PathfindNative() {
   }

   public static void init() {
      String var0 = "";
      if ("1".equals(System.getProperty("zomboid.debuglibs.pathfind"))) {
         DebugLog.log("***** Loading debug version of PZPathFind");
         var0 = "d";
      }

      if (System.getProperty("os.name").contains("OS X")) {
         System.loadLibrary("PZPathFind");
      } else if (System.getProperty("sun.arch.data.model").equals("64")) {
         System.loadLibrary("PZPathFind64" + var0);
      } else {
         System.loadLibrary("PZPathFind32" + var0);
      }

   }

   public static native void initWorld(int var0, int var1, int var2, int var3, boolean var4);

   public static native void destroyWorld();

   public static native void freeMemoryAtExit();

   public static native void update();

   public static native void updateChunk(int var0, int var1, int var2, ByteBuffer var3);

   public static native void removeChunk(int var0, int var1);

   public static native void updateSquare(int var0, int var1, int var2, int var3, int var4, short var5, int var6, float var7, float var8);

   public static native void addVehicle(ByteBuffer var0);

   public static native void removeVehicle(int var0);

   public static native void teleportVehicle(ByteBuffer var0);

   public static native int findPath(ByteBuffer var0, ByteBuffer var1);

   public void init(IsoMetaGrid var1) {
      initWorld(var1.getMinX(), var1.getMinY(), var1.getWidth(), var1.getHeight(), GameServer.bServer);
      PathfindNativeThread.instance = new PathfindNativeThread();
      ByteBuffer var2 = PathfindNativeThread.instance.pathBB;
      PathfindNativeThread.instance.pathBB.order(ByteOrder.BIG_ENDIAN);
      var2.clear();
      PathfindNativeThread.instance.setName("PathfindNativeThread");
      PathfindNativeThread.instance.setDaemon(true);
      PathfindNativeThread.instance.start();
   }

   public void stop() {
      PathfindNativeThread.instance.stopThread();
      PathfindNativeThread.instance.cleanup();
      PathfindNativeThread.instance = null;
      Iterator var1 = this.vehicleState.values().iterator();

      while(var1.hasNext()) {
         VehicleState var2 = (VehicleState)var1.next();
         var2.release();
      }

      this.vehicleState.clear();
      this.bTestRequestAdded = false;
      destroyWorld();
   }

   public void checkUseNativeCode() {
      if (USE_NATIVE_CODE != DebugOptions.instance.PathfindUseNativeCode.getValue()) {
         if (USE_NATIVE_CODE) {
            this.stop();
         } else {
            PolygonalMap2.instance.stop();
         }

         USE_NATIVE_CODE = DebugOptions.instance.PathfindUseNativeCode.getValue();
         if (USE_NATIVE_CODE) {
            this.init(IsoWorld.instance.MetaGrid);
         } else {
            PolygonalMap2.instance.init(IsoWorld.instance.MetaGrid);
         }

         for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
            IsoChunkMap var2 = IsoWorld.instance.CurrentCell.getChunkMap(var1);
            if (!var2.ignore) {
               for(int var3 = 0; var3 < IsoChunkMap.ChunkGridWidth; ++var3) {
                  for(int var4 = 0; var4 < IsoChunkMap.ChunkGridWidth; ++var4) {
                     IsoChunk var5 = var2.getChunk(var4, var3);
                     if (var5 != null) {
                        if (USE_NATIVE_CODE) {
                           this.addChunkToWorld(var5);
                        } else {
                           PolygonalMap2.instance.addChunkToWorld(var5);
                        }
                     }
                  }
               }
            }
         }

         ArrayList var6 = IsoWorld.instance.CurrentCell.getVehicles();

         for(int var7 = 0; var7 < var6.size(); ++var7) {
            BaseVehicle var8 = (BaseVehicle)var6.get(var7);
            if (USE_NATIVE_CODE) {
               this.addVehicle(var8);
            } else {
               PolygonalMap2.instance.addVehicleToWorld(var8);
            }
         }

      }
   }

   public void addChunkToWorld(IsoChunk var1) {
      ChunkUpdateTask var2 = ChunkUpdateTask.alloc().init(var1);
      PathfindNativeThread.instance.chunkTaskQueue.add(var2);
      PathfindNativeThread.instance.wake();
      var1.loadedBits = (short)(var1.loadedBits | 2);
   }

   public void removeChunkFromWorld(IsoChunk var1) {
      if (PathfindNativeThread.instance != null) {
         ChunkRemoveTask var2 = ChunkRemoveTask.alloc().init(var1);
         PathfindNativeThread.instance.chunkTaskQueue.add(var2);
         PathfindNativeThread.instance.wake();
      }
   }

   public void squareChanged(IsoGridSquare var1) {
      if ((var1.chunk.loadedBits & 2) != 0) {
         for(int var2 = 0; var2 < 8; ++var2) {
            IsoDirections var3 = IsoDirections.fromIndex(var2);
            IsoGridSquare var4 = var1.getAdjacentSquare(var3);
            if (var4 != null) {
               SquareUpdateTask var5 = SquareUpdateTask.alloc().init(var4);
               PathfindNativeThread.instance.squareTaskQueue.add(var5);
            }
         }

         SquareUpdateTask var6 = SquareUpdateTask.alloc().init(var1);
         PathfindNativeThread.instance.squareTaskQueue.add(var6);
         PathfindNativeThread.instance.wake();
      }
   }

   public void addVehicle(BaseVehicle var1) {
      VehicleState var2 = (VehicleState)this.vehicleState.get(var1);
      if (var2 == null) {
         var2 = VehicleState.alloc();
         this.vehicleState.put(var1, var2);
      } else {
         boolean var3 = true;
      }

      var2.init(var1);
      VehicleAddTask var4 = VehicleAddTask.alloc().init(var1);
      PathfindNativeThread.instance.vehicleTaskQueue.add(var4);
      PathfindNativeThread.instance.wake();
   }

   public void removeVehicle(BaseVehicle var1) {
      VehicleState var2 = (VehicleState)this.vehicleState.remove(var1);
      if (var2 != null) {
         var2.release();
      }

      if (PathfindNativeThread.instance != null) {
         VehicleRemoveTask var3 = VehicleRemoveTask.alloc().init(var1);
         PathfindNativeThread.instance.vehicleTaskQueue.add(var3);
         PathfindNativeThread.instance.wake();
      }
   }

   public void updateVehicle(BaseVehicle var1) {
      VehicleUpdateTask var2 = VehicleUpdateTask.alloc().init(var1);
      PathfindNativeThread.instance.vehicleTaskQueue.add(var2);
      PathfindNativeThread.instance.wake();
   }

   public PathFindRequest addRequest(IPathfinder var1, Mover var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      this.cancelRequest(var2);
      PathFindRequest var9 = PathFindRequest.alloc().init(var1, var2, var3, var4, var5, var6, var7, var8);
      PathfindNativeThread.instance.requestMap.put(var2, var9);
      PathRequestTask var10 = PathRequestTask.alloc().init(var9);
      PathfindNativeThread.instance.requestTaskQueue.add(var10);
      PathfindNativeThread.instance.wake();
      return var9;
   }

   public void cancelRequest(Mover var1) {
      if (PathfindNativeThread.instance != null) {
         PathFindRequest var2 = (PathFindRequest)PathfindNativeThread.instance.requestMap.remove(var1);
         if (var2 != null) {
            var2.cancel = true;
         }

      }
   }

   public void updateMain() {
      ConcurrentLinkedQueue var1 = PathfindNativeThread.instance.taskReturnQueue;

      for(IPathfindTask var2 = (IPathfindTask)var1.poll(); var2 != null; var2 = (IPathfindTask)var1.poll()) {
         var2.release();
      }

      ArrayList var6 = IsoWorld.instance.CurrentCell.getVehicles();

      for(int var3 = 0; var3 < var6.size(); ++var3) {
         BaseVehicle var4 = (BaseVehicle)var6.get(var3);
         VehicleState var5 = (VehicleState)this.vehicleState.get(var4);
         if (var5 != null && var5.check()) {
            this.updateVehicle(var4);
         }
      }

      ConcurrentLinkedQueue var7 = PathfindNativeThread.instance.requestToMain;

      for(PathFindRequest var8 = (PathFindRequest)var7.poll(); var8 != null; var8 = (PathFindRequest)var7.poll()) {
         if (PathfindNativeThread.instance.requestMap.get(var8.mover) == var8) {
            PathfindNativeThread.instance.requestMap.remove(var8.mover);
         }

         if (!var8.cancel) {
            if (var8.path.isEmpty()) {
               var8.finder.Failed(var8.mover);
            } else {
               var8.finder.Succeeded(var8.path, var8.mover);
            }
         }

         if (!var8.doNotRelease) {
            var8.release();
         }
      }

   }

   public int findPath(PathFindRequest var1, ByteBuffer var2, boolean var3) {
      this.requestBB.clear();
      this.requestBB.putFloat(var1.startX);
      this.requestBB.putFloat(var1.startY);
      this.requestBB.putFloat(var1.startZ + 32.0F);
      this.requestBB.putFloat(var1.targetX);
      this.requestBB.putFloat(var1.targetY);
      this.requestBB.putFloat(var1.targetZ + 32.0F);
      boolean var4 = false;
      byte var5 = 0;
      if (var1.mover instanceof IsoPlayer && !(var1.mover instanceof IsoAnimal)) {
         var5 = 1;
         var4 = ((IsoPlayer)var1.mover).isNPC();
      }

      if (var1.mover instanceof IsoZombie) {
         var5 = 2;
      }

      this.requestBB.putInt(var5);
      this.requestBB.put((byte)(var4 ? 1 : 0));
      this.requestBB.put((byte)(var1.bCanCrawl ? 1 : 0));
      this.requestBB.put((byte)(var1.bCrawling ? 1 : 0));
      this.requestBB.put((byte)(var1.bIgnoreCrawlCost ? 1 : 0));
      this.requestBB.put((byte)(var1.bCanThump ? 1 : 0));
      this.requestBB.put((byte)(var1.bCanClimbFences ? 1 : 0));
      this.requestBB.putInt(var1.minLevel);
      this.requestBB.putInt(var1.maxLevel);
      this.requestBB.put((byte)(var3 ? 1 : 0));
      var2.clear();
      return findPath(this.requestBB, var2);
   }

   public void render() {
      if (Core.bDebug) {
         if (IsoCamera.frameState.playerIndex == 0) {
            if (DebugOptions.instance.PathfindPathToMouseEnable.getValue()) {
               IsoPlayer var1 = IsoPlayer.players[0];
               if (var1 == null || var1.isDead()) {
                  return;
               }

               if (GameKeyboard.isKeyPressed(209)) {
                  this.testZ = Math.max(this.testZ - 1, -32);
               }

               if (GameKeyboard.isKeyPressed(201)) {
                  this.testZ = Math.min(this.testZ + 1, 31);
               }

               float var2 = (float)Mouse.getX();
               float var3 = (float)Mouse.getY();
               int var4 = this.testZ;
               float var5 = IsoUtils.XToIso(var2, var3, (float)var4);
               float var6 = IsoUtils.YToIso(var2, var3, (float)var4);
               float var7 = (float)var4;
               this.renderGridAtMouse(var5, var6, var7);
               this.pathToMouse(var1.getX(), var1.getY(), var1.getZ(), var5, var6, var7);
            }

         }
      }
   }

   private void renderGridAtMouse(float var1, float var2, float var3) {
      int var4 = PZMath.fastfloor(var1);
      int var5 = PZMath.fastfloor(var2);
      int var6 = PZMath.fastfloor(var3);

      int var7;
      for(var7 = -1; var7 <= 2; ++var7) {
         LineDrawer.addLine((float)(var4 - 1), (float)(var5 + var7), (float)((int)var3), (float)(var4 + 2), (float)(var5 + var7), (float)var6, 0.3F, 0.3F, 0.3F, (String)null, false);
      }

      for(var7 = -1; var7 <= 2; ++var7) {
         LineDrawer.addLine((float)(var4 + var7), (float)(var5 - 1), (float)((int)var3), (float)(var4 + var7), (float)(var5 + 2), (float)var6, 0.3F, 0.3F, 0.3F, (String)null, false);
      }

      for(var7 = -1; var7 <= 1; ++var7) {
         for(int var8 = -1; var8 <= 1; ++var8) {
            float var9 = 0.3F;
            float var10 = 0.0F;
            float var11 = 0.0F;
            IsoGridSquare var12 = IsoWorld.instance.CurrentCell.getGridSquare(var4 + var8, var5 + var7, var6);
            if (var12 == null || var12.isSolid() || var12.isSolidTrans() || var12.HasStairs()) {
               LineDrawer.addLine((float)(var4 + var8), (float)(var5 + var7), (float)var6, (float)(var4 + var8 + 1), (float)(var5 + var7 + 1), (float)var6, var9, var10, var11, (String)null, false);
            }
         }
      }

      float var13 = 0.5F;
      if (this.testZ < PZMath.fastfloor(IsoPlayer.getInstance().getZ())) {
         LineDrawer.addLine((float)var4 + 0.5F, (float)var5 + 0.5F, (float)var6, (float)var4 + 0.5F, (float)var5 + 0.5F, (float)PZMath.fastfloor(IsoPlayer.getInstance().getZ()), var13, var13, var13, (String)null, true);
      } else if (this.testZ > PZMath.fastfloor(IsoPlayer.getInstance().getZ())) {
         LineDrawer.addLine((float)var4 + 0.5F, (float)var5 + 0.5F, (float)var6, (float)var4 + 0.5F, (float)var5 + 0.5F, (float)PZMath.fastfloor(IsoPlayer.getInstance().getZ()), var13, var13, var13, (String)null, true);
      }

   }

   private void pathToMouse(float var1, float var2, float var3, float var4, float var5, float var6) {
      if (this.bTestRequestAdded) {
         if (this.finder.done) {
            this.bTestRequestAdded = false;
            if (GameWindow.states.current == IngameState.instance && !GameTime.isGamePaused() && Mouse.isButtonDown(0) && GameKeyboard.isKeyDown(42)) {
               IsoPlayer.players[0].StopAllActionQueue();
               Object var7 = LuaManager.env.rawget("ISPathFindAction_pathToLocationF");
               if (var7 != null) {
                  LuaManager.caller.pcall(LuaManager.thread, var7, new Object[]{this.request.targetX, this.request.targetY, this.request.targetZ});
               }
            }
         }
      } else {
         this.finder.path.clear();
         this.finder.done = false;
         this.request.init(this.finder, IsoPlayer.getInstance(), var1, var2, var3, var4, var5, var6);
         this.request.doNotRelease = true;
         if (DebugOptions.instance.PathfindPathToMouseAllowCrawl.getValue()) {
            this.request.bCanCrawl = true;
            if (DebugOptions.instance.PathfindPathToMouseIgnoreCrawlCost.getValue()) {
               this.request.bIgnoreCrawlCost = true;
            }
         }

         if (DebugOptions.instance.PathfindPathToMouseAllowThump.getValue()) {
            this.request.bCanThump = true;
         }

         PathRequestTask var8 = PathRequestTask.alloc();
         var8.init(this.request);
         PathfindNativeThread.instance.requestTaskQueue.add(var8);
         this.bTestRequestAdded = true;
         PathfindNativeThread.instance.wake();
      }

      if (GameWindow.states.current == DebugChunkState.instance) {
         this.updateMain();
      }

   }
}
