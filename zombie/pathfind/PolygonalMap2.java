package zombie.pathfind;

import astar.ISearchNode;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.joml.Vector2f;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.ai.KnownBlockedEdges;
import zombie.ai.astar.Mover;
import zombie.audio.FMODAmbientWalls;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.gameStates.DebugChunkState;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.network.GameClient;
import zombie.network.MPStatistic;
import zombie.pathfind.highLevel.HLAStar;
import zombie.pathfind.highLevel.HLGlobals;
import zombie.pathfind.highLevel.HLLevelTransition;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;

public final class PolygonalMap2 {
   public static final PolygonalMap2 instance = new PolygonalMap2();
   public static final float RADIUS = 0.3F;
   public static final boolean CLOSE_TO_WALLS = true;
   public static final boolean PATHS_UNDER_VEHICLES = true;
   public static final boolean COLLIDE_CLIPPER = false;
   public static final boolean COLLIDE_BEVEL = false;
   public static final int CXN_FLAG_CAN_PATH = 1;
   public static final int CXN_FLAG_THUMP = 2;
   public static final int NODE_FLAG_CRAWL = 1;
   public static final int NODE_FLAG_CRAWL_INTERIOR = 2;
   public static final int NODE_FLAG_IN_CHUNK_DATA = 4;
   public static final int NODE_FLAG_PERIMETER = 8;
   public static final int NODE_FLAG_STAIR = 16;
   public static final int NODE_FLAG_KEEP = 65536;
   public static final int LCC_ZERO = 0;
   public static final int LCC_IGNORE_DOORS = 1;
   public static final int LCC_CLOSE_TO_WALLS = 2;
   public static final int LCC_CHECK_COST = 4;
   public static final int LCC_RENDER = 8;
   public static final int LCC_ALLOW_ON_EDGE = 16;
   static final float RADIUS_DIAGONAL = (float)Math.sqrt(0.18000000715255737);
   static final Vector2 temp = new Vector2();
   static final int SQUARES_PER_CHUNK = 8;
   static final int LEVELS_PER_CHUNK = 64;
   static final int GROUND_LEVEL = 32;
   static final int SQUARES_PER_CELL;
   static final int CHUNKS_PER_CELL;
   static final int BIT_SOLID = 1;
   static final int BIT_COLLIDE_W = 2;
   static final int BIT_COLLIDE_N = 4;
   public static final int BIT_STAIR_TW = 8;
   public static final int BIT_STAIR_MW = 16;
   public static final int BIT_STAIR_BW = 32;
   public static final int BIT_STAIR_TN = 64;
   public static final int BIT_STAIR_MN = 128;
   public static final int BIT_STAIR_BN = 256;
   public static final int BIT_SOLID_FLOOR = 512;
   static final int BIT_SOLID_TRANS = 1024;
   public static final int BIT_WINDOW_W = 2048;
   public static final int BIT_WINDOW_N = 4096;
   public static final int BIT_CAN_PATH_W = 8192;
   public static final int BIT_CAN_PATH_N = 16384;
   static final int BIT_THUMP_W = 32768;
   static final int BIT_THUMP_N = 65536;
   public static final int BIT_THUMPABLE = 131072;
   static final int BIT_DOOR_E = 262144;
   static final int BIT_DOOR_S = 524288;
   static final int BIT_WINDOW_W_UNBLOCKED = 1048576;
   static final int BIT_WINDOW_N_UNBLOCKED = 2097152;
   static final int BIT_DOOR_W_UNBLOCKED = 4194304;
   static final int BIT_DOOR_N_UNBLOCKED = 8388608;
   static final int BIT_HOPPABLE_N = 16777216;
   static final int BIT_HOPPABLE_W = 33554432;
   public static final int ALL_STAIR_BITS = 504;
   private static final int ALL_SOLID_BITS = 1025;
   public final Object renderLock = new Object();
   final ClosestPointOnEdge closestPointOnEdge = new ClosestPointOnEdge();
   final TIntObjectHashMap<Node> squareToNode = new TIntObjectHashMap();
   final ByteBuffer xyBufferThread = ByteBuffer.allocateDirect(8192);
   final ConcurrentLinkedQueue<IChunkTask> chunkTaskQueue = new ConcurrentLinkedQueue();
   final ConcurrentLinkedQueue<SquareUpdateTask> squareTaskQueue = new ConcurrentLinkedQueue();
   final ConcurrentLinkedQueue<IVehicleTask> vehicleTaskQueue = new ConcurrentLinkedQueue();
   final ArrayList<Vehicle> vehicles = new ArrayList();
   final HashMap<BaseVehicle, Vehicle> vehicleMap = new HashMap();
   final Sync sync = new Sync();
   final RequestQueue requests = new RequestQueue();
   final ConcurrentLinkedQueue<PathRequestTask> requestTaskQueue = new ConcurrentLinkedQueue();
   final CollideWithObstaclesPoly collideWithObstaclesPoly = new CollideWithObstaclesPoly();
   private final ArrayList<VehicleCluster> clusters = new ArrayList();
   private final ArrayList<Square> tempSquares = new ArrayList();
   private final ArrayList<VisibilityGraph> graphs = new ArrayList();
   private final AdjustStartEndNodeData adjustStartData = new AdjustStartEndNodeData();
   private final AdjustStartEndNodeData adjustGoalData = new AdjustStartEndNodeData();
   private final LineClearCollide lcc = new LineClearCollide();
   private final VGAStar astar = new VGAStar();
   private final TestRequest testRequest = new TestRequest();
   private final PathFindBehavior2.PointOnPath pointOnPath = new PathFindBehavior2.PointOnPath();
   private final HashMap<BaseVehicle, VehicleState> vehicleState = new HashMap();
   private final TObjectProcedure<Node> releaseNodeProc = new TObjectProcedure<Node>() {
      public boolean execute(Node var1) {
         var1.release();
         return true;
      }
   };
   private final Path shortestPath = new Path();
   private final ConcurrentLinkedQueue<PathFindRequest> requestToMain = new ConcurrentLinkedQueue();
   private final HashMap<Mover, PathFindRequest> requestMap = new HashMap();
   private final LineClearCollideMain lccMain = new LineClearCollideMain();
   private final float[] tempFloats = new float[8];
   private final CollideWithObstacles collideWithObstacles = new CollideWithObstacles();
   Clipper clipperThread;
   boolean rebuild;
   private int testZ = 0;
   private int minX;
   private int minY;
   private int width;
   private int height;
   private Cell[][] cells;
   private PMThread thread;

   public PolygonalMap2() {
   }

   private void createVehicleCluster(VehicleRect var1, ArrayList<VehicleRect> var2, ArrayList<VehicleCluster> var3) {
      for(int var4 = 0; var4 < var2.size(); ++var4) {
         VehicleRect var5 = (VehicleRect)var2.get(var4);
         if (var1 != var5 && var1.z == var5.z && (var1.cluster == null || var1.cluster != var5.cluster) && var1.isAdjacent(var5)) {
            if (var1.cluster != null) {
               if (var5.cluster == null) {
                  var5.cluster = var1.cluster;
                  var5.cluster.rects.add(var5);
               } else {
                  var3.remove(var5.cluster);
                  var1.cluster.merge(var5.cluster);
               }
            } else if (var5.cluster != null) {
               if (var1.cluster == null) {
                  var1.cluster = var5.cluster;
                  var1.cluster.rects.add(var1);
               } else {
                  var3.remove(var1.cluster);
                  var5.cluster.merge(var1.cluster);
               }
            } else {
               VehicleCluster var6 = VehicleCluster.alloc().init();
               var6.z = var1.z;
               var1.cluster = var6;
               var5.cluster = var6;
               var6.rects.add(var1);
               var6.rects.add(var5);
               var3.add(var6);
            }
         }
      }

      if (var1.cluster == null) {
         VehicleCluster var7 = VehicleCluster.alloc().init();
         var7.z = var1.z;
         var1.cluster = var7;
         var7.rects.add(var1);
         var3.add(var7);
      }

   }

   private void createVehicleClusters() {
      this.clusters.clear();
      ArrayList var1 = new ArrayList();

      int var2;
      for(var2 = 0; var2 < this.vehicles.size(); ++var2) {
         Vehicle var3 = (Vehicle)this.vehicles.get(var2);
         VehicleRect var4 = VehicleRect.alloc();
         var3.polyPlusRadius.getAABB(var4);
         var4.vehicle = var3;
         var1.add(var4);
      }

      if (!var1.isEmpty()) {
         for(var2 = 0; var2 < var1.size(); ++var2) {
            VehicleRect var5 = (VehicleRect)var1.get(var2);
            this.createVehicleCluster(var5, var1, this.clusters);
         }

      }
   }

   public Node getNodeForSquare(Square var1) {
      Node var2 = (Node)this.squareToNode.get(var1.ID);
      if (var2 == null) {
         var2 = Node.alloc().init(var1);
         this.squareToNode.put(var1.ID, var2);
      }

      return var2;
   }

   public Node getExistingNodeForSquare(Square var1) {
      return (Node)this.squareToNode.get(var1.ID);
   }

   private VisibilityGraph getVisGraphAt(float var1, float var2, int var3) {
      return this.getVisGraphAt(var1, var2, var3, 0);
   }

   public VisibilityGraph getVisGraphAt(float var1, float var2, int var3, int var4) {
      Chunk var5 = this.getChunkFromSquarePos(PZMath.fastfloor(var1), PZMath.fastfloor(var2));
      if (var5 == null) {
         return null;
      } else {
         for(int var6 = 0; var6 < var5.visibilityGraphs.size(); ++var6) {
            VisibilityGraph var7 = (VisibilityGraph)var5.visibilityGraphs.get(var6);
            if (var7.contains(var1, var2, var3, var4)) {
               return var7;
            }
         }

         return null;
      }
   }

   public VisibilityGraph getVisGraphForSquare(Square var1) {
      Chunk var2 = this.getChunkFromSquarePos(var1.x, var1.y);
      if (var2 == null) {
         return null;
      } else {
         for(int var3 = 0; var3 < var2.visibilityGraphs.size(); ++var3) {
            VisibilityGraph var4 = (VisibilityGraph)var2.visibilityGraphs.get(var3);
            if (var4.contains(var1)) {
               return var4;
            }
         }

         return null;
      }
   }

   public void getVisibilityGraphsOverlappingChunk(Chunk var1, int var2, ArrayList<VisibilityGraph> var3) {
      for(int var4 = 0; var4 < var1.visibilityGraphs.size(); ++var4) {
         VisibilityGraph var5 = (VisibilityGraph)var1.visibilityGraphs.get(var4);
         if (var5.cluster.z == var2 && !var3.contains(var5)) {
            var3.add(var5);
         }
      }

   }

   public void getVisibilityGraphsAdjacentToChunk(Chunk var1, int var2, ArrayList<VisibilityGraph> var3) {
      for(int var4 = 0; var4 < 8; ++var4) {
         IsoDirections var5 = IsoDirections.fromIndex(var4);
         Chunk var6 = this.getChunkFromChunkPos(var1.wx + var5.dx(), var1.wy + var5.dy());
         if (var6 != null) {
            for(int var7 = 0; var7 < var6.visibilityGraphs.size(); ++var7) {
               VisibilityGraph var8 = (VisibilityGraph)var6.visibilityGraphs.get(var7);
               if (var8.cluster.z == var2 && !var3.contains(var8)) {
                  VehicleRect var9 = var8.cluster.bounds();
                  byte var10 = 1;
                  if (var9.intersects(var1.getMinX() - 1, var1.getMinY() - 1, var1.getMaxX() + 2, var1.getMaxY() + 2, var10)) {
                     var3.add(var8);
                  }

                  var9.release();
               }
            }
         }
      }

   }

   Connection connectTwoNodes(Node var1, Node var2, int var3) {
      Connection var4 = Connection.alloc().init(var1, var2, var3);
      var1.visible.add(var4);
      var2.visible.add(var4);
      return var4;
   }

   Connection connectTwoNodes(Node var1, Node var2) {
      return this.connectTwoNodes(var1, var2, 0);
   }

   void breakConnection(Connection var1) {
      var1.node1.visible.remove(var1);
      var1.node2.visible.remove(var1);
      var1.release();
   }

   void breakConnection(Node var1, Node var2) {
      for(int var3 = 0; var3 < var1.visible.size(); ++var3) {
         Connection var4 = (Connection)var1.visible.get(var3);
         if (var4.otherNode(var1) == var2) {
            this.breakConnection(var4);
            break;
         }
      }

   }

   private void addStairNodes() {
      ArrayList var1 = this.tempSquares;
      var1.clear();

      int var2;
      for(var2 = 0; var2 < this.graphs.size(); ++var2) {
         VisibilityGraph var3 = (VisibilityGraph)this.graphs.get(var2);
         var3.getStairSquares(var1);
      }

      for(var2 = 0; var2 < var1.size(); ++var2) {
         Square var14 = (Square)var1.get(var2);
         Square var4 = null;
         Square var5 = null;
         Square var6 = null;
         Square var7 = null;
         Square var8 = null;
         if (var14.has(8)) {
            var4 = this.getSquare(var14.x - 1, var14.y, var14.z + 1);
            var5 = var14;
            var6 = this.getSquare(var14.x + 1, var14.y, var14.z);
            var7 = this.getSquare(var14.x + 2, var14.y, var14.z);
            var8 = this.getSquare(var14.x + 3, var14.y, var14.z);
         }

         if (var14.has(64)) {
            var4 = this.getSquare(var14.x, var14.y - 1, var14.z + 1);
            var5 = var14;
            var6 = this.getSquare(var14.x, var14.y + 1, var14.z);
            var7 = this.getSquare(var14.x, var14.y + 2, var14.z);
            var8 = this.getSquare(var14.x, var14.y + 3, var14.z);
         }

         if (var4 != null && var5 != null && var6 != null && var7 != null && var8 != null) {
            Node var9 = null;
            Node var10 = null;
            VisibilityGraph var11 = this.getVisGraphForSquare(var4);
            Iterator var12;
            Obstacle var13;
            if (var11 == null) {
               var9 = this.getNodeForSquare(var4);
            } else {
               var9 = Node.alloc().init(var4);
               var12 = var11.obstacles.iterator();

               while(var12.hasNext()) {
                  var13 = (Obstacle)var12.next();
                  if (var13.isNodeInsideOf(var9)) {
                     var9.ignore = true;
                  }
               }

               var9.addGraph(var11);
               var11.addNode(var9);
               this.squareToNode.put(var4.ID, var9);
            }

            var9.flags |= 16;
            var11 = this.getVisGraphForSquare(var8);
            if (var11 == null) {
               var10 = this.getNodeForSquare(var8);
            } else {
               var10 = Node.alloc().init(var8);
               var12 = var11.obstacles.iterator();

               while(var12.hasNext()) {
                  var13 = (Obstacle)var12.next();
                  if (var13.isNodeInsideOf(var10)) {
                     var10.ignore = true;
                  }
               }

               var10.addGraph(var11);
               var11.addNode(var10);
               this.squareToNode.put(var8.ID, var10);
            }

            var10.flags |= 16;
         }
      }

   }

   private void addCanPathNodes() {
      ArrayList var1 = this.tempSquares;
      var1.clear();

      int var2;
      for(var2 = 0; var2 < this.graphs.size(); ++var2) {
         VisibilityGraph var3 = (VisibilityGraph)this.graphs.get(var2);
         var3.getCanPathSquares(var1);
      }

      for(var2 = 0; var2 < var1.size(); ++var2) {
         Square var10 = (Square)var1.get(var2);
         if (!var10.isNonThumpableSolid() && !var10.has(504) && var10.has(512)) {
            int var4;
            int var5;
            Square var6;
            Node var7;
            Node var8;
            int var9;
            if (var10.isCanPathW()) {
               var4 = var10.x - 1;
               var5 = var10.y;
               var6 = this.getSquare(var4, var5, var10.z);
               if (var6 != null && !var6.isNonThumpableSolid() && !var6.has(504) && var6.has(512)) {
                  var7 = this.getOrCreateCanPathNode(var10);
                  var8 = this.getOrCreateCanPathNode(var6);
                  var9 = 1;
                  if (var10.has(163840) || var6.has(131072)) {
                     var9 |= 2;
                  }

                  this.connectTwoNodes(var7, var8, var9);
               }
            }

            if (var10.isCanPathN()) {
               var4 = var10.x;
               var5 = var10.y - 1;
               var6 = this.getSquare(var4, var5, var10.z);
               if (var6 != null && !var6.isNonThumpableSolid() && !var6.has(504) && var6.has(512)) {
                  var7 = this.getOrCreateCanPathNode(var10);
                  var8 = this.getOrCreateCanPathNode(var6);
                  var9 = 1;
                  if (var10.has(196608) || var6.has(131072)) {
                     var9 |= 2;
                  }

                  this.connectTwoNodes(var7, var8, var9);
               }
            }
         }
      }

   }

   private Node getOrCreateCanPathNode(Square var1) {
      VisibilityGraph var2 = this.getVisGraphForSquare(var1);
      Node var3 = this.getNodeForSquare(var1);
      if (var2 != null && !var2.nodes.contains(var3)) {
         Iterator var4 = var2.obstacles.iterator();

         while(var4.hasNext()) {
            Obstacle var5 = (Obstacle)var4.next();
            if (var5.isNodeInsideOf(var3)) {
               var3.ignore = true;
               break;
            }
         }

         var2.addNode(var3);
      }

      return var3;
   }

   public Node getPointOutsideObjects(Square var1, float var2, float var3) {
      Square var4 = instance.getSquare(var1.x - 1, var1.y, var1.z);
      Square var5 = instance.getSquare(var1.x - 1, var1.y - 1, var1.z);
      Square var6 = instance.getSquare(var1.x, var1.y - 1, var1.z);
      Square var7 = instance.getSquare(var1.x + 1, var1.y - 1, var1.z);
      Square var8 = instance.getSquare(var1.x + 1, var1.y, var1.z);
      Square var9 = instance.getSquare(var1.x + 1, var1.y + 1, var1.z);
      Square var10 = instance.getSquare(var1.x, var1.y + 1, var1.z);
      Square var11 = instance.getSquare(var1.x - 1, var1.y + 1, var1.z);
      float var12 = (float)var1.x;
      float var13 = (float)var1.y;
      float var14 = (float)(var1.x + 1);
      float var15 = (float)(var1.y + 1);
      if (var1.isCollideW()) {
         var12 += 0.35000002F;
      }

      if (var1.isCollideN()) {
         var13 += 0.35000002F;
      }

      if (var8 != null && (var8.has(2) || var8.has(504) || var8.isReallySolid())) {
         var14 -= 0.35000002F;
      }

      if (var10 != null && (var10.has(4) || var10.has(504) || var10.isReallySolid())) {
         var15 -= 0.35000002F;
      }

      float var16 = PZMath.clamp(var2, var12, var14);
      float var17 = PZMath.clamp(var3, var13, var15);
      boolean var18;
      float var19;
      float var20;
      if (var16 <= (float)var1.x + 0.3F && var17 <= (float)var1.y + 0.3F) {
         var18 = var5 != null && (var5.has(504) || var5.isReallySolid());
         var18 |= var6 != null && var6.has(2);
         var18 |= var4 != null && var4.has(4);
         if (var18) {
            var19 = (float)var1.x + 0.3F + 0.05F;
            var20 = (float)var1.y + 0.3F + 0.05F;
            if (var19 - var16 <= var20 - var17) {
               var16 = var19;
            } else {
               var17 = var20;
            }
         }
      }

      if (var16 >= (float)(var1.x + 1) - 0.3F && var17 <= (float)var1.y + 0.3F) {
         var18 = var7 != null && (var7.has(2) || var7.has(504) || var7.isReallySolid());
         var18 |= var8 != null && var8.has(4);
         if (var18) {
            var19 = (float)(var1.x + 1) - 0.3F - 0.05F;
            var20 = (float)var1.y + 0.3F + 0.05F;
            if (var16 - var19 <= var20 - var17) {
               var16 = var19;
            } else {
               var17 = var20;
            }
         }
      }

      if (var16 <= (float)var1.x + 0.3F && var17 >= (float)(var1.y + 1) - 0.3F) {
         var18 = var11 != null && (var11.has(4) || var11.has(504) || var11.isReallySolid());
         var18 |= var10 != null && var10.has(2);
         if (var18) {
            var19 = (float)var1.x + 0.3F + 0.05F;
            var20 = (float)(var1.y + 1) - 0.3F - 0.05F;
            if (var19 - var16 <= var17 - var20) {
               var16 = var19;
            } else {
               var17 = var20;
            }
         }
      }

      if (var16 >= (float)(var1.x + 1) - 0.3F && var17 >= (float)(var1.y + 1) - 0.3F) {
         var18 = var9 != null && (var9.has(2) || var9.has(4) || var9.has(504) || var9.isReallySolid());
         if (var18) {
            var19 = (float)(var1.x + 1) - 0.3F - 0.05F;
            var20 = (float)(var1.y + 1) - 0.3F - 0.05F;
            if (var16 - var19 <= var17 - var20) {
               var16 = var19;
            } else {
               var17 = var20;
            }
         }
      }

      return Node.alloc().init(var16, var17, var1.z);
   }

   private void createVisibilityGraph(VehicleCluster var1) {
      VisibilityGraph var2 = VisibilityGraph.alloc().init(var1);
      var2.addPerimeterEdges();
      var2.initOverlappedChunks();
      this.graphs.add(var2);
   }

   private void createVisibilityGraphs() {
      this.createVehicleClusters();
      this.squareToNode.clear();

      for(int var1 = 0; var1 < this.clusters.size(); ++var1) {
         VehicleCluster var2 = (VehicleCluster)this.clusters.get(var1);
         this.createVisibilityGraph(var2);
      }

      this.addStairNodes();
      this.addCanPathNodes();
   }

   private boolean findPath(PathFindRequest var1, boolean var2) {
      float var3 = var1.startZ + 32.0F;
      float var4 = var1.targetZ + 32.0F;
      int var5 = 16;
      if (!(var1.mover instanceof IsoZombie)) {
         var5 |= 4;
      }

      VisibilityGraph var26;
      if (PZMath.fastfloor(var3) == PZMath.fastfloor(var4) && !this.lcc.isNotClear(this, var1.startX, var1.startY, var1.targetX, var1.targetY, PZMath.fastfloor(var3), var5)) {
         var1.path.addNode(var1.startX, var1.startY, var1.startZ);
         var1.path.addNode(var1.targetX, var1.targetY, var1.targetZ);
         if (var2) {
            Iterator var28 = this.graphs.iterator();

            while(var28.hasNext()) {
               var26 = (VisibilityGraph)var28.next();
               var26.render();
            }
         }

         return true;
      } else {
         this.astar.init(this.graphs, this.squareToNode);
         this.astar.knownBlockedEdges.clear();

         for(int var6 = 0; var6 < var1.knownBlockedEdges.size(); ++var6) {
            KnownBlockedEdges var7 = (KnownBlockedEdges)var1.knownBlockedEdges.get(var6);
            Square var8 = this.getSquare(var7.x, var7.y, var7.z);
            if (var8 != null) {
               this.astar.knownBlockedEdges.put(var8.ID, var7);
            }
         }

         VisibilityGraph var25 = null;
         var26 = null;
         SearchNode var27 = null;
         SearchNode var9 = null;
         boolean var10 = false;
         boolean var11 = false;
         boolean var23 = false;

         boolean var32;
         int var34;
         VisibilityGraph var37;
         Edge var39;
         Iterator var44;
         label1518: {
            label1519: {
               VisibilityGraph var13;
               label1520: {
                  int var15;
                  boolean var43;
                  label1521: {
                     boolean var36;
                     label1522: {
                        boolean var16;
                        Iterator var17;
                        VisibilityGraph var18;
                        int var42;
                        Edge var45;
                        label1523: {
                           label1524: {
                              try {
                                 label1565: {
                                    var23 = true;
                                    Square var12 = this.getSquare(PZMath.fastfloor(var1.startX), PZMath.fastfloor(var1.startY), PZMath.fastfloor(var3));
                                    if (var12 == null || var12.isReallySolid()) {
                                       var32 = false;
                                       var23 = false;
                                       break label1518;
                                    }

                                    Node var14;
                                    if (var12.has(504)) {
                                       var27 = this.astar.getSearchNode(var12);
                                    } else {
                                       var13 = this.astar.getVisGraphForSquare(var12);
                                       if (var13 != null) {
                                          if (!var13.created) {
                                             var13.create();
                                          }

                                          var14 = null;
                                          var15 = var13.getPointOutsideObstacles(var1.startX, var1.startY, var3, this.adjustStartData);
                                          if (var15 == -1) {
                                             var16 = false;
                                             var23 = false;
                                             break label1565;
                                          }

                                          if (var15 == 1) {
                                             var10 = true;
                                             var14 = this.adjustStartData.node;
                                             if (this.adjustStartData.isNodeNew) {
                                                var25 = var13;
                                             }
                                          }

                                          if (var14 == null) {
                                             var14 = Node.alloc().init(var1.startX, var1.startY, PZMath.fastfloor(var3));
                                             var13.addNode(var14);
                                             var25 = var13;
                                          }

                                          var27 = this.astar.getSearchNode(var14);
                                       }
                                    }

                                    if (var27 == null) {
                                       var27 = this.astar.getSearchNode(var12);
                                    }

                                    if (this.getChunkFromSquarePos(PZMath.fastfloor(var1.targetX), PZMath.fastfloor(var1.targetY)) == null) {
                                       var9 = this.astar.getSearchNode(PZMath.fastfloor(var1.targetX), PZMath.fastfloor(var1.targetY));
                                    } else {
                                       var12 = this.getSquare(PZMath.fastfloor(var1.targetX), PZMath.fastfloor(var1.targetY), PZMath.fastfloor(var4));
                                       if (var12 == null || var12.isReallySolid()) {
                                          var32 = false;
                                          var23 = false;
                                          break label1519;
                                       }

                                       if ((PZMath.fastfloor(var1.startX) != PZMath.fastfloor(var1.targetX) || PZMath.fastfloor(var1.startY) != PZMath.fastfloor(var1.targetY) || PZMath.fastfloor(var1.startZ) != PZMath.fastfloor(var1.targetZ)) && this.isBlockedInAllDirections(PZMath.fastfloor(var1.targetX), PZMath.fastfloor(var1.targetY), PZMath.fastfloor(var4))) {
                                          var32 = false;
                                          var23 = false;
                                          break label1524;
                                       }

                                       if (var12.has(504)) {
                                          var9 = this.astar.getSearchNode(var12);
                                       } else {
                                          var13 = this.astar.getVisGraphForSquare(var12);
                                          if (var13 != null) {
                                             if (!var13.created) {
                                                var13.create();
                                             }

                                             var14 = null;
                                             var15 = var13.getPointOutsideObstacles(var1.targetX, var1.targetY, var4, this.adjustGoalData);
                                             if (var15 == -1) {
                                                var16 = false;
                                                var23 = false;
                                                break label1523;
                                             }

                                             if (var15 == 1) {
                                                var11 = true;
                                                var14 = this.adjustGoalData.node;
                                                if (this.adjustGoalData.isNodeNew) {
                                                   var26 = var13;
                                                }
                                             }

                                             if (var14 == null) {
                                                var14 = Node.alloc().init(var1.targetX, var1.targetY, PZMath.fastfloor(var4));
                                                var13.addNode(var14);
                                                var26 = var13;
                                             }

                                             var9 = this.astar.getSearchNode(var14);
                                          } else {
                                             for(var34 = 0; var34 < this.graphs.size(); ++var34) {
                                                var13 = (VisibilityGraph)this.graphs.get(var34);
                                                if (var13.contains(var12, 1)) {
                                                   Node var33 = this.getPointOutsideObjects(var12, var1.targetX, var1.targetY);
                                                   var13.addNode(var33);
                                                   if (var33.x != var1.targetX || var33.y != var1.targetY) {
                                                      var11 = true;
                                                      this.adjustGoalData.isNodeNew = false;
                                                   }

                                                   var26 = var13;
                                                   var9 = this.astar.getSearchNode(var33);
                                                   break;
                                                }
                                             }
                                          }
                                       }

                                       if (var9 == null) {
                                          var9 = this.astar.getSearchNode(var12);
                                       }
                                    }

                                    ArrayList var31 = this.astar.shortestPath(var1, var27, var9);
                                    if (var31 == null) {
                                       var23 = false;
                                       break label1520;
                                    }

                                    if (var31.size() != 1) {
                                       this.cleanPath(var31, var1, var10, var11, var9);
                                       if (DebugOptions.instance.PathfindSmoothPlayerPath.getValue() && var1.mover instanceof IsoPlayer && !((IsoPlayer)var1.mover).isNPC()) {
                                          this.smoothPath(var1.path);
                                       }

                                       this.fixPathZ(var1.path);
                                       var43 = true;
                                       var23 = false;
                                       break label1521;
                                    }

                                    var1.path.addNode(var27);
                                    if (!var11 && var9.square != null && (float)var9.square.x + 0.5F != var1.targetX && (float)var9.square.y + 0.5F != var1.targetY) {
                                       var1.path.addNode(var1.targetX, var1.targetY, var4, 0);
                                    } else {
                                       var1.path.addNode(var9);
                                    }

                                    this.fixPathZ(var1.path);
                                    var36 = true;
                                    var23 = false;
                                    break label1522;
                                 }
                              } finally {
                                 if (var23) {
                                    if (var2) {
                                       Iterator var20 = this.graphs.iterator();

                                       while(var20.hasNext()) {
                                          VisibilityGraph var21 = (VisibilityGraph)var20.next();
                                          var21.render();
                                       }
                                    }

                                    if (var25 != null) {
                                       var25.removeNode(var27.vgNode);
                                       var27.vgNode.release();
                                    }

                                    if (var26 != null) {
                                       var26.removeNode(var9.vgNode);
                                       var9.vgNode.release();
                                    }

                                    int var50;
                                    for(var50 = 0; var50 < this.astar.searchNodes.size(); ++var50) {
                                       ((SearchNode)this.astar.searchNodes.get(var50)).release();
                                    }

                                    Edge var51;
                                    if (var10 && this.adjustStartData.isNodeNew) {
                                       for(var50 = 0; var50 < this.adjustStartData.node.edges.size(); ++var50) {
                                          var51 = (Edge)this.adjustStartData.node.edges.get(var50);
                                          var51.obstacle.unsplit(this.adjustStartData.node, var51.edgeRing);
                                       }

                                       this.adjustStartData.graph.edges.remove(this.adjustStartData.newEdge);
                                    }

                                    if (var11 && this.adjustGoalData.isNodeNew) {
                                       for(var50 = 0; var50 < this.adjustGoalData.node.edges.size(); ++var50) {
                                          var51 = (Edge)this.adjustGoalData.node.edges.get(var50);
                                          var51.obstacle.unsplit(this.adjustGoalData.node, var51.edgeRing);
                                       }

                                       this.adjustGoalData.graph.edges.remove(this.adjustGoalData.newEdge);
                                    }

                                 }
                              }

                              if (var2) {
                                 var17 = this.graphs.iterator();

                                 while(var17.hasNext()) {
                                    var18 = (VisibilityGraph)var17.next();
                                    var18.render();
                                 }
                              }

                              if (var25 != null) {
                                 var25.removeNode(var27.vgNode);
                                 var27.vgNode.release();
                              }

                              if (var26 != null) {
                                 var26.removeNode(var9.vgNode);
                                 var9.vgNode.release();
                              }

                              for(var42 = 0; var42 < this.astar.searchNodes.size(); ++var42) {
                                 ((SearchNode)this.astar.searchNodes.get(var42)).release();
                              }

                              if (var10 && this.adjustStartData.isNodeNew) {
                                 for(var42 = 0; var42 < this.adjustStartData.node.edges.size(); ++var42) {
                                    var45 = (Edge)this.adjustStartData.node.edges.get(var42);
                                    var45.obstacle.unsplit(this.adjustStartData.node, var45.edgeRing);
                                 }

                                 this.adjustStartData.graph.edges.remove(this.adjustStartData.newEdge);
                              }

                              if (var11 && this.adjustGoalData.isNodeNew) {
                                 for(var42 = 0; var42 < this.adjustGoalData.node.edges.size(); ++var42) {
                                    var45 = (Edge)this.adjustGoalData.node.edges.get(var42);
                                    var45.obstacle.unsplit(this.adjustGoalData.node, var45.edgeRing);
                                 }

                                 this.adjustGoalData.graph.edges.remove(this.adjustGoalData.newEdge);
                              }

                              return var16;
                           }

                           if (var2) {
                              var44 = this.graphs.iterator();

                              while(var44.hasNext()) {
                                 var37 = (VisibilityGraph)var44.next();
                                 var37.render();
                              }
                           }

                           if (var25 != null) {
                              var25.removeNode(var27.vgNode);
                              var27.vgNode.release();
                           }

                           if (var26 != null) {
                              var26.removeNode(var9.vgNode);
                              var9.vgNode.release();
                           }

                           for(var34 = 0; var34 < this.astar.searchNodes.size(); ++var34) {
                              ((SearchNode)this.astar.searchNodes.get(var34)).release();
                           }

                           if (var10 && this.adjustStartData.isNodeNew) {
                              for(var34 = 0; var34 < this.adjustStartData.node.edges.size(); ++var34) {
                                 var39 = (Edge)this.adjustStartData.node.edges.get(var34);
                                 var39.obstacle.unsplit(this.adjustStartData.node, var39.edgeRing);
                              }

                              this.adjustStartData.graph.edges.remove(this.adjustStartData.newEdge);
                           }

                           if (var11 && this.adjustGoalData.isNodeNew) {
                              for(var34 = 0; var34 < this.adjustGoalData.node.edges.size(); ++var34) {
                                 var39 = (Edge)this.adjustGoalData.node.edges.get(var34);
                                 var39.obstacle.unsplit(this.adjustGoalData.node, var39.edgeRing);
                              }

                              this.adjustGoalData.graph.edges.remove(this.adjustGoalData.newEdge);
                           }

                           return var32;
                        }

                        if (var2) {
                           var17 = this.graphs.iterator();

                           while(var17.hasNext()) {
                              var18 = (VisibilityGraph)var17.next();
                              var18.render();
                           }
                        }

                        if (var25 != null) {
                           var25.removeNode(var27.vgNode);
                           var27.vgNode.release();
                        }

                        if (var26 != null) {
                           var26.removeNode(var9.vgNode);
                           var9.vgNode.release();
                        }

                        for(var42 = 0; var42 < this.astar.searchNodes.size(); ++var42) {
                           ((SearchNode)this.astar.searchNodes.get(var42)).release();
                        }

                        if (var10 && this.adjustStartData.isNodeNew) {
                           for(var42 = 0; var42 < this.adjustStartData.node.edges.size(); ++var42) {
                              var45 = (Edge)this.adjustStartData.node.edges.get(var42);
                              var45.obstacle.unsplit(this.adjustStartData.node, var45.edgeRing);
                           }

                           this.adjustStartData.graph.edges.remove(this.adjustStartData.newEdge);
                        }

                        if (var11 && this.adjustGoalData.isNodeNew) {
                           for(var42 = 0; var42 < this.adjustGoalData.node.edges.size(); ++var42) {
                              var45 = (Edge)this.adjustGoalData.node.edges.get(var42);
                              var45.obstacle.unsplit(this.adjustGoalData.node, var45.edgeRing);
                           }

                           this.adjustGoalData.graph.edges.remove(this.adjustGoalData.newEdge);
                        }

                        return var16;
                     }

                     if (var2) {
                        Iterator var38 = this.graphs.iterator();

                        while(var38.hasNext()) {
                           VisibilityGraph var47 = (VisibilityGraph)var38.next();
                           var47.render();
                        }
                     }

                     if (var25 != null) {
                        var25.removeNode(var27.vgNode);
                        var27.vgNode.release();
                     }

                     if (var26 != null) {
                        var26.removeNode(var9.vgNode);
                        var9.vgNode.release();
                     }

                     int var40;
                     for(var40 = 0; var40 < this.astar.searchNodes.size(); ++var40) {
                        ((SearchNode)this.astar.searchNodes.get(var40)).release();
                     }

                     Edge var49;
                     if (var10 && this.adjustStartData.isNodeNew) {
                        for(var40 = 0; var40 < this.adjustStartData.node.edges.size(); ++var40) {
                           var49 = (Edge)this.adjustStartData.node.edges.get(var40);
                           var49.obstacle.unsplit(this.adjustStartData.node, var49.edgeRing);
                        }

                        this.adjustStartData.graph.edges.remove(this.adjustStartData.newEdge);
                     }

                     if (var11 && this.adjustGoalData.isNodeNew) {
                        for(var40 = 0; var40 < this.adjustGoalData.node.edges.size(); ++var40) {
                           var49 = (Edge)this.adjustGoalData.node.edges.get(var40);
                           var49.obstacle.unsplit(this.adjustGoalData.node, var49.edgeRing);
                        }

                        this.adjustGoalData.graph.edges.remove(this.adjustGoalData.newEdge);
                     }

                     return var36;
                  }

                  if (var2) {
                     Iterator var41 = this.graphs.iterator();

                     while(var41.hasNext()) {
                        VisibilityGraph var46 = (VisibilityGraph)var41.next();
                        var46.render();
                     }
                  }

                  if (var25 != null) {
                     var25.removeNode(var27.vgNode);
                     var27.vgNode.release();
                  }

                  if (var26 != null) {
                     var26.removeNode(var9.vgNode);
                     var9.vgNode.release();
                  }

                  for(var15 = 0; var15 < this.astar.searchNodes.size(); ++var15) {
                     ((SearchNode)this.astar.searchNodes.get(var15)).release();
                  }

                  Edge var48;
                  if (var10 && this.adjustStartData.isNodeNew) {
                     for(var15 = 0; var15 < this.adjustStartData.node.edges.size(); ++var15) {
                        var48 = (Edge)this.adjustStartData.node.edges.get(var15);
                        var48.obstacle.unsplit(this.adjustStartData.node, var48.edgeRing);
                     }

                     this.adjustStartData.graph.edges.remove(this.adjustStartData.newEdge);
                  }

                  if (var11 && this.adjustGoalData.isNodeNew) {
                     for(var15 = 0; var15 < this.adjustGoalData.node.edges.size(); ++var15) {
                        var48 = (Edge)this.adjustGoalData.node.edges.get(var15);
                        var48.obstacle.unsplit(this.adjustGoalData.node, var48.edgeRing);
                     }

                     this.adjustGoalData.graph.edges.remove(this.adjustGoalData.newEdge);
                  }

                  return var43;
               }

               if (var2) {
                  Iterator var29 = this.graphs.iterator();

                  while(var29.hasNext()) {
                     var13 = (VisibilityGraph)var29.next();
                     var13.render();
                  }
               }

               if (var25 != null) {
                  var25.removeNode(var27.vgNode);
                  var27.vgNode.release();
               }

               if (var26 != null) {
                  var26.removeNode(var9.vgNode);
                  var9.vgNode.release();
               }

               int var30;
               for(var30 = 0; var30 < this.astar.searchNodes.size(); ++var30) {
                  ((SearchNode)this.astar.searchNodes.get(var30)).release();
               }

               Edge var35;
               if (var10 && this.adjustStartData.isNodeNew) {
                  for(var30 = 0; var30 < this.adjustStartData.node.edges.size(); ++var30) {
                     var35 = (Edge)this.adjustStartData.node.edges.get(var30);
                     var35.obstacle.unsplit(this.adjustStartData.node, var35.edgeRing);
                  }

                  this.adjustStartData.graph.edges.remove(this.adjustStartData.newEdge);
               }

               if (var11 && this.adjustGoalData.isNodeNew) {
                  for(var30 = 0; var30 < this.adjustGoalData.node.edges.size(); ++var30) {
                     var35 = (Edge)this.adjustGoalData.node.edges.get(var30);
                     var35.obstacle.unsplit(this.adjustGoalData.node, var35.edgeRing);
                  }

                  this.adjustGoalData.graph.edges.remove(this.adjustGoalData.newEdge);
               }

               return false;
            }

            if (var2) {
               var44 = this.graphs.iterator();

               while(var44.hasNext()) {
                  var37 = (VisibilityGraph)var44.next();
                  var37.render();
               }
            }

            if (var25 != null) {
               var25.removeNode(var27.vgNode);
               var27.vgNode.release();
            }

            if (var26 != null) {
               var26.removeNode(var9.vgNode);
               var9.vgNode.release();
            }

            for(var34 = 0; var34 < this.astar.searchNodes.size(); ++var34) {
               ((SearchNode)this.astar.searchNodes.get(var34)).release();
            }

            if (var10 && this.adjustStartData.isNodeNew) {
               for(var34 = 0; var34 < this.adjustStartData.node.edges.size(); ++var34) {
                  var39 = (Edge)this.adjustStartData.node.edges.get(var34);
                  var39.obstacle.unsplit(this.adjustStartData.node, var39.edgeRing);
               }

               this.adjustStartData.graph.edges.remove(this.adjustStartData.newEdge);
            }

            if (var11 && this.adjustGoalData.isNodeNew) {
               for(var34 = 0; var34 < this.adjustGoalData.node.edges.size(); ++var34) {
                  var39 = (Edge)this.adjustGoalData.node.edges.get(var34);
                  var39.obstacle.unsplit(this.adjustGoalData.node, var39.edgeRing);
               }

               this.adjustGoalData.graph.edges.remove(this.adjustGoalData.newEdge);
            }

            return var32;
         }

         if (var2) {
            var44 = this.graphs.iterator();

            while(var44.hasNext()) {
               var37 = (VisibilityGraph)var44.next();
               var37.render();
            }
         }

         if (var25 != null) {
            var25.removeNode(var27.vgNode);
            var27.vgNode.release();
         }

         if (var26 != null) {
            var26.removeNode(var9.vgNode);
            var9.vgNode.release();
         }

         for(var34 = 0; var34 < this.astar.searchNodes.size(); ++var34) {
            ((SearchNode)this.astar.searchNodes.get(var34)).release();
         }

         if (var10 && this.adjustStartData.isNodeNew) {
            for(var34 = 0; var34 < this.adjustStartData.node.edges.size(); ++var34) {
               var39 = (Edge)this.adjustStartData.node.edges.get(var34);
               var39.obstacle.unsplit(this.adjustStartData.node, var39.edgeRing);
            }

            this.adjustStartData.graph.edges.remove(this.adjustStartData.newEdge);
         }

         if (var11 && this.adjustGoalData.isNodeNew) {
            for(var34 = 0; var34 < this.adjustGoalData.node.edges.size(); ++var34) {
               var39 = (Edge)this.adjustGoalData.node.edges.get(var34);
               var39.obstacle.unsplit(this.adjustGoalData.node, var39.edgeRing);
            }

            this.adjustGoalData.graph.edges.remove(this.adjustGoalData.newEdge);
         }

         return var32;
      }
   }

   boolean findPathHighLevelThenLowLevel(PathFindRequest var1, boolean var2) {
      this.astar.init(this.graphs, this.squareToNode);
      this.astar.mover.set(var1);
      ArrayList var3 = HLGlobals.levelTransitionList;
      boolean var18 = false;

      try {
         var18 = true;
         HLAStar.PerfFindPath.start();
         HLGlobals.astar.findPath(this.astar.mover, var1.startX, var1.startY, PZMath.fastfloor(var1.startZ + 32.0F), var1.targetX, var1.targetY, PZMath.fastfloor(var1.targetZ + 32.0F), var3, HLGlobals.chunkLevelList, HLGlobals.bottomOfLevelTransition, var2);
         var18 = false;
      } finally {
         if (var18) {
            HLAStar.PerfFindPath.end();
            Iterator var7 = this.astar.searchNodes.iterator();

            while(var7.hasNext()) {
               SearchNode var8 = (SearchNode)var7.next();
               var8.release();
            }

            this.astar.searchNodes.clear();
         }
      }

      HLAStar.PerfFindPath.end();
      Iterator var4 = this.astar.searchNodes.iterator();

      while(var4.hasNext()) {
         SearchNode var5 = (SearchNode)var4.next();
         var5.release();
      }

      this.astar.searchNodes.clear();
      if (HLGlobals.chunkLevelList.isEmpty()) {
         return false;
      } else {
         if (this.astar.mover.allowedChunkLevels != null) {
            this.astar.mover.allowedChunkLevels.clear();
            this.astar.mover.allowedChunkLevels.addAll(HLGlobals.chunkLevelList);
         }

         if (this.astar.mover.allowedLevelTransitions != null) {
            this.astar.mover.allowedLevelTransitions.clear();
            this.astar.mover.allowedLevelTransitions.addAll(var3);
         }

         if (var3.isEmpty()) {
            var1.minLevel = PZMath.fastfloor(var1.startZ + 32.0F);
            var1.maxLevel = PZMath.fastfloor(var1.targetZ + 32.0F);
            return this.findPath(var1, var2);
         } else {
            HLGlobals.path.clear();
            float var20 = var1.startX;
            float var21 = var1.startY;
            float var6 = var1.startZ;
            float var22 = var1.targetX;
            float var23 = var1.targetY;
            float var9 = var1.targetZ;
            int var10 = PZMath.fastfloor(var6 + 32.0F);
            boolean var10000 = HLGlobals.astar.getLevelTransitionAt(PZMath.fastfloor(var20), PZMath.fastfloor(var21), PZMath.fastfloor(var6 + 32.0F)) != null;
            boolean var12 = HLGlobals.astar.getLevelTransitionAt(PZMath.fastfloor(var22), PZMath.fastfloor(var23), PZMath.fastfloor(var9 + 32.0F)) != null;

            for(int var13 = 0; var13 < var3.size(); ++var13) {
               HLLevelTransition var14 = (HLLevelTransition)var3.get(var13);
               boolean var15 = var13 == var3.size() - 1;
               if (var12 && var15) {
                  break;
               }

               boolean var16 = (Boolean)HLGlobals.bottomOfLevelTransition.get(var13);
               var1.startX = var20;
               var1.startY = var21;
               var1.startZ = var6;
               var1.targetX = (float)(var16 ? var14.getBottomFloorX() : var14.getTopFloorX()) + 0.5F;
               var1.targetY = (float)(var16 ? var14.getBottomFloorY() : var14.getTopFloorY()) + 0.5F;
               var1.targetZ = (float)((var16 ? var14.getBottomFloorZ() : var14.getTopFloorZ()) - 32);
               var1.minLevel = var14.getBottomFloorZ();
               var1.maxLevel = var14.getTopFloorZ();
               if (!this.findPath(var1, var2)) {
                  return false;
               }

               this.addPathSegmentNodes(var1.path, HLGlobals.path);
               var1.path.nodes.clear();
               var20 = var1.targetX;
               var21 = var1.targetY;
               var6 = var1.targetZ;
               var10 = PZMath.fastfloor(var6 + 32.0F);
            }

            var1.startX = var20;
            var1.startY = var21;
            var1.startZ = var6;
            var1.targetX = var22;
            var1.targetY = var23;
            var1.targetZ = var9;
            var1.minLevel = (int)PZMath.min(var6, var9) + 32;
            var1.maxLevel = (int)PZMath.max(var6, var9) + 32;
            if (this.findPath(var1, var2)) {
               this.addPathSegmentNodes(var1.path, HLGlobals.path);
               var1.path.nodes.clear();
               var1.path.nodes.addAll(HLGlobals.path.nodes);
               HLGlobals.path.nodes.clear();
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private void addPathSegmentNodes(Path var1, Path var2) {
      for(int var3 = 0; var3 < var1.nodes.size(); ++var3) {
         PathNode var4 = var1.getNode(var3);
         if (var2.nodes.size() < 2 || !var4.isApproximatelyEqual(var2.getLastNode())) {
            var2.nodes.add(var4);
         }
      }

   }

   private void cleanPath(ArrayList<ISearchNode> var1, PathFindRequest var2, boolean var3, boolean var4, SearchNode var5) {
      boolean var6 = var2.mover instanceof IsoPlayer && ((IsoPlayer)var2.mover).isNPC();
      Square var7 = null;
      int var8 = -123;
      int var9 = -123;

      for(int var10 = 0; var10 < var1.size(); ++var10) {
         SearchNode var11 = (SearchNode)var1.get(var10);
         float var12 = var11.getX();
         float var13 = var11.getY();
         float var14 = var11.getZ();
         int var15 = var11.vgNode == null ? 0 : var11.vgNode.flags;
         Square var16 = var11.square;
         boolean var17 = false;
         int var19;
         if (var16 != null && var7 != null && var16.z == var7.z) {
            int var18 = var16.x - var7.x;
            var19 = var16.y - var7.y;
            if (var18 == var8 && var19 == var9) {
               if (var2.path.nodes.size() > 1) {
                  var17 = true;
                  if (var2.path.getLastNode().hasFlag(65536)) {
                     var17 = false;
                  }
               }

               if (var18 == 0 && var19 == -1 && var7.has(16384)) {
                  var17 = false;
               } else if (var18 == 0 && var19 == 1 && var16.has(16384)) {
                  var17 = false;
               } else if (var18 == -1 && var19 == 0 && var7.has(8192)) {
                  var17 = false;
               } else if (var18 == 1 && var19 == 0 && var16.has(8192)) {
                  var17 = false;
               }
            } else {
               var8 = var18;
               var9 = var19;
            }
         } else {
            var9 = -123;
            var8 = -123;
         }

         if (var16 != null) {
            var7 = var16;
         } else {
            var7 = null;
         }

         if (var6) {
            var17 = false;
         }

         PathNode var26;
         if (var17) {
            var26 = var2.path.getLastNode();
            var26.x = (float)var16.x + 0.5F;
            var26.y = (float)var16.y + 0.5F;
         } else {
            if (var2.path.nodes.size() > 1) {
               var26 = var2.path.getLastNode();
               if (var26.isApproximatelyEqual(var12, var13, var14)) {
                  var26.x = var12;
                  var26.y = var13;
                  var26.z = var14;
                  continue;
               }
            }

            if (var10 > 0 && var11.square != null) {
               SearchNode var27 = (SearchNode)var1.get(var10 - 1);
               if (var27.square != null) {
                  var19 = var11.square.x - var27.square.x;
                  int var20 = var11.square.y - var27.square.y;
                  if (var19 == 0 && var20 == -1 && var27.square.has(16384)) {
                     var15 |= 65536;
                  } else if (var19 == 0 && var20 == 1 && var11.square.has(16384)) {
                     var15 |= 65536;
                  } else if (var19 == -1 && var20 == 0 && var27.square.has(8192)) {
                     var15 |= 65536;
                  } else if (var19 == 1 && var20 == 0 && var11.square.has(8192)) {
                     var15 |= 65536;
                  }
               }
            }

            var2.path.addNode(var12, var13, var14, var15);
         }
      }

      if (var2.mover instanceof IsoPlayer && !var6) {
         if (var2.path.isEmpty()) {
            Object var10000 = null;
         } else {
            var2.path.getNode(0);
         }

         if (!var4 && var5.square != null && (double)IsoUtils.DistanceToSquared((float)var5.square.x + 0.5F, (float)var5.square.y + 0.5F, var2.targetX, var2.targetY) > 0.010000000000000002) {
            var2.path.addNode(var2.targetX, var2.targetY, var2.targetZ + 32.0F, 0);
         }
      }

      PathNode var21 = null;

      for(int var22 = 0; var22 < var2.path.nodes.size(); ++var22) {
         PathNode var23 = (PathNode)var2.path.nodes.get(var22);
         PathNode var24 = var22 < var2.path.nodes.size() - 1 ? (PathNode)var2.path.nodes.get(var22 + 1) : null;
         if (var23.hasFlag(1)) {
            boolean var25 = var21 != null && var21.hasFlag(2) || var24 != null && var24.hasFlag(2);
            if (!var25) {
               var23.flags &= -4;
            }
         }

         var21 = var23;
      }

   }

   private void smoothPath(Path var1) {
      int var2 = 0;

      while(true) {
         while(var2 < var1.nodes.size() - 2) {
            PathNode var3 = (PathNode)var1.nodes.get(var2);
            PathNode var4 = (PathNode)var1.nodes.get(var2 + 1);
            PathNode var5 = (PathNode)var1.nodes.get(var2 + 2);
            if (PZMath.fastfloor(var3.z) == PZMath.fastfloor(var4.z) && PZMath.fastfloor(var3.z) == PZMath.fastfloor(var5.z)) {
               if (!this.lcc.isNotClear(this, var3.x, var3.y, var5.x, var5.y, PZMath.fastfloor(var3.z), 20)) {
                  var1.nodes.remove(var2 + 1);
                  var1.nodePool.push(var4);
               } else {
                  ++var2;
               }
            } else {
               ++var2;
            }
         }

         return;
      }
   }

   private void fixPathZ(Path var1) {
      for(int var2 = 0; var2 < var1.nodes.size(); ++var2) {
         PathNode var10000 = (PathNode)var1.nodes.get(var2);
         var10000.z -= 32.0F;
      }

   }

   float getApparentZ(IsoGridSquare var1) {
      if (!var1.Has(IsoObjectType.stairsTW) && !var1.Has(IsoObjectType.stairsTN)) {
         if (!var1.Has(IsoObjectType.stairsMW) && !var1.Has(IsoObjectType.stairsMN)) {
            return !var1.Has(IsoObjectType.stairsBW) && !var1.Has(IsoObjectType.stairsBN) ? (float)var1.z : (float)var1.z + 0.25F;
         } else {
            return (float)var1.z + 0.5F;
         }
      } else {
         return (float)var1.z + 0.75F;
      }
   }

   public void render() {
      if (Core.bDebug) {
         boolean var1 = DebugOptions.instance.PathfindPathToMouseEnable.getValue() && !this.testRequest.done && IsoPlayer.getInstance().getPath2() == null;
         if (DebugOptions.instance.PolymapRenderClusters.getValue()) {
            synchronized(this.renderLock) {
               Iterator var3 = this.clusters.iterator();

               while(var3.hasNext()) {
                  VehicleCluster var4 = (VehicleCluster)var3.next();
                  Iterator var5 = var4.rects.iterator();

                  while(var5.hasNext()) {
                     VehicleRect var6 = (VehicleRect)var5.next();
                     LineDrawer.addRect((float)var6.x, (float)var6.y, (float)(var6.z - 32), (float)var6.w, (float)var6.h, 0.0F, 0.0F, 1.0F);
                  }

                  VehicleRect var34 = var4.bounds();
                  var34.release();
               }

               if (!var1) {
                  var3 = this.graphs.iterator();

                  while(var3.hasNext()) {
                     VisibilityGraph var30 = (VisibilityGraph)var3.next();
                     var30.render();
                  }
               }
            }
         }

         float var7;
         float var13;
         float var29;
         float var38;
         synchronized(this.renderLock) {
            var29 = (float)Mouse.getX();
            float var31 = (float)Mouse.getY();
            int var35 = (int)IsoPlayer.getInstance().getZ();
            var38 = IsoUtils.XToIso(var29, var31, (float)var35);
            var7 = IsoUtils.YToIso(var29, var31, (float)var35);
            Square var8 = this.getSquare(PZMath.fastfloor(var38), PZMath.fastfloor(var7), var35);
            if (var8 != null) {
               VisibilityGraph var9 = this.getVisGraphForSquare(var8);
               if (var9 != null) {
                  Iterator var10 = var9.obstacles.iterator();

                  while(var10.hasNext()) {
                     Obstacle var11 = (Obstacle)var10.next();
                     if (var11.bounds.containsPoint(var38, var7) && var11.isPointInside(var38, var7)) {
                        var11.getClosestPointOnEdge(var38, var7, this.closestPointOnEdge);
                        float var12 = this.closestPointOnEdge.point.x;
                        var13 = this.closestPointOnEdge.point.y;
                        LineDrawer.addLine(var12 - 0.05F, var13 - 0.05F, (float)var35, var12 + 0.05F, var13 + 0.05F, (float)var35, 1.0F, 1.0F, 0.0F, (String)null, false);
                        break;
                     }
                  }
               }
            }
         }

         float var2;
         int var33;
         float var37;
         if (DebugOptions.instance.PolymapRenderLineClearCollide.getValue()) {
            var2 = (float)Mouse.getX();
            var29 = (float)Mouse.getY();
            var33 = (int)IsoPlayer.getInstance().getZ();
            var37 = IsoUtils.XToIso(var2, var29, (float)var33);
            var38 = IsoUtils.YToIso(var2, var29, (float)var33);
            LineDrawer.addLine(IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), (float)var33, var37, var38, (float)var33, 1, 1, 1, (String)null);
            int var39 = 9;
            var39 |= 2;
            if (this.lccMain.isNotClear(this, IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), var37, var38, var33, (BaseVehicle)null, var39)) {
               Vector2f var40 = this.resolveCollision(IsoPlayer.getInstance(), var37, var38, L_render.vector2f);
               LineDrawer.addLine(var40.x - 0.05F, var40.y - 0.05F, (float)var33, var40.x + 0.05F, var40.y + 0.05F, (float)var33, 1.0F, 1.0F, 0.0F, (String)null, false);
            }
         }

         if (GameKeyboard.isKeyDown(209) && !GameKeyboard.wasKeyDown(209)) {
            this.testZ = Math.max(this.testZ - 1, -32);
         }

         if (GameKeyboard.isKeyDown(201) && !GameKeyboard.wasKeyDown(201)) {
            this.testZ = Math.min(this.testZ + 1, 31);
         }

         if (var1) {
            var2 = (float)Mouse.getX();
            var29 = (float)Mouse.getY();
            var33 = this.testZ;
            var37 = IsoUtils.XToIso(var2, var29, (float)var33);
            var38 = IsoUtils.YToIso(var2, var29, (float)var33);
            var7 = (float)var33;
            int var41 = PZMath.fastfloor(var37);
            int var44 = PZMath.fastfloor(var38);
            int var46 = PZMath.fastfloor(var7);

            int var48;
            for(var48 = -1; var48 <= 2; ++var48) {
               LineDrawer.addLine((float)(var41 - 1), (float)(var44 + var48), (float)var46, (float)(var41 + 2), (float)(var44 + var48), (float)var46, 0.3F, 0.3F, 0.3F, (String)null, false);
            }

            for(var48 = -1; var48 <= 2; ++var48) {
               LineDrawer.addLine((float)(var41 + var48), (float)(var44 - 1), (float)var46, (float)(var41 + var48), (float)(var44 + 2), (float)var46, 0.3F, 0.3F, 0.3F, (String)null, false);
            }

            IsoGridSquare var16;
            for(var48 = -1; var48 <= 1; ++var48) {
               for(int var49 = -1; var49 <= 1; ++var49) {
                  var13 = 0.3F;
                  float var14 = 0.0F;
                  float var15 = 0.0F;
                  var16 = IsoWorld.instance.CurrentCell.getGridSquare(var41 + var49, var44 + var48, var46);
                  if (var16 == null || var16.isSolid() || var16.isSolidTrans() || var16.HasStairs()) {
                     LineDrawer.addLine((float)(var41 + var49), (float)(var44 + var48), (float)var46, (float)(var41 + var49 + 1), (float)(var44 + var48 + 1), (float)var46, var13, var14, var15, (String)null, false);
                  }
               }
            }

            float var50 = 0.5F;
            if (var33 < PZMath.fastfloor(IsoPlayer.getInstance().getZ())) {
               LineDrawer.addLine((float)var41 + 0.5F, (float)var44 + 0.5F, (float)var46, (float)var41 + 0.5F, (float)var44 + 0.5F, (float)PZMath.fastfloor(IsoPlayer.getInstance().getZ()), var50, var50, var50, (String)null, true);
            } else if (var33 > PZMath.fastfloor(IsoPlayer.getInstance().getZ())) {
               LineDrawer.addLine((float)var41 + 0.5F, (float)var44 + 0.5F, (float)var46, (float)var41 + 0.5F, (float)var44 + 0.5F, (float)PZMath.fastfloor(IsoPlayer.getInstance().getZ()), var50, var50, var50, (String)null, true);
            }

            PathFindRequest var51 = PathFindRequest.alloc().init(this.testRequest, IsoPlayer.getInstance(), IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), IsoPlayer.getInstance().getZ(), var37, var38, var7);
            if (DebugOptions.instance.PathfindPathToMouseAllowCrawl.getValue()) {
               var51.bCanCrawl = true;
               if (DebugOptions.instance.PathfindPathToMouseIgnoreCrawlCost.getValue()) {
                  var51.bIgnoreCrawlCost = true;
               }
            }

            if (DebugOptions.instance.PathfindPathToMouseAllowThump.getValue()) {
               var51.bCanThump = true;
            }

            this.testRequest.done = false;
            synchronized(this.renderLock) {
               boolean var52 = DebugOptions.instance.PolymapRenderClusters.getValue();
               if (this.findPathHighLevelThenLowLevel(var51, var52) && !var51.path.isEmpty()) {
                  IsoGridSquare var17;
                  float var19;
                  float var20;
                  PathNode var56;
                  for(int var54 = 0; var54 < var51.path.nodes.size() - 1; ++var54) {
                     var56 = (PathNode)var51.path.nodes.get(var54);
                     PathNode var58 = (PathNode)var51.path.nodes.get(var54 + 1);
                     var17 = IsoWorld.instance.CurrentCell.getGridSquare((double)var56.x, (double)var56.y, (double)var56.z);
                     IsoGridSquare var18 = IsoWorld.instance.CurrentCell.getGridSquare((double)var58.x, (double)var58.y, (double)var58.z);
                     var19 = var17 == null ? var56.z : this.getApparentZ(var17);
                     var20 = var18 == null ? var58.z : this.getApparentZ(var18);
                     float var21 = 1.0F;
                     float var22 = 1.0F;
                     float var23 = 0.0F;
                     if (var19 != (float)((int)var19) || var20 != (float)((int)var20)) {
                        var22 = 0.0F;
                     }

                     LineDrawer.addLine(var56.x, var56.y, var19, var58.x, var58.y, var20, var21, var22, var23, (String)null, true);
                     LineDrawer.addRect(var56.x - 0.05F, var56.y - 0.05F, var19, 0.1F, 0.1F, var21, var22, var23);
                  }

                  PathFindBehavior2.closestPointOnPath(IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), IsoPlayer.getInstance().getZ(), IsoPlayer.getInstance(), var51.path, this.pointOnPath);
                  PathNode var55 = (PathNode)var51.path.nodes.get(this.pointOnPath.pathIndex);
                  var56 = (PathNode)var51.path.nodes.get(this.pointOnPath.pathIndex + 1);
                  var16 = IsoWorld.instance.CurrentCell.getGridSquare((double)var55.x, (double)var55.y, (double)var55.z);
                  var17 = IsoWorld.instance.CurrentCell.getGridSquare((double)var56.x, (double)var56.y, (double)var56.z);
                  float var59 = var16 == null ? var55.z : this.getApparentZ(var16);
                  var19 = var17 == null ? var56.z : this.getApparentZ(var17);
                  var20 = var59 + (var19 - var59) * this.pointOnPath.dist;
                  LineDrawer.addLine(this.pointOnPath.x - 0.05F, this.pointOnPath.y - 0.05F, var20, this.pointOnPath.x + 0.05F, this.pointOnPath.y + 0.05F, var20, 0.0F, 1.0F, 0.0F, (String)null, true);
                  LineDrawer.addLine(this.pointOnPath.x - 0.05F, this.pointOnPath.y + 0.05F, var20, this.pointOnPath.x + 0.05F, this.pointOnPath.y - 0.05F, var20, 0.0F, 1.0F, 0.0F, (String)null, true);
                  if (GameKeyboard.isKeyDown(207) && !GameKeyboard.wasKeyDown(207)) {
                     Object var57 = LuaManager.env.rawget("ISPathFindAction_pathToLocationF");
                     if (var57 != null) {
                        LuaManager.caller.pcall(LuaManager.thread, var57, new Object[]{var37, var38, var7});
                     }
                  }
               }

               var51.release();
            }
         } else {
            for(int var28 = 0; var28 < this.testRequest.path.nodes.size() - 1; ++var28) {
               PathNode var32 = (PathNode)this.testRequest.path.nodes.get(var28);
               PathNode var36 = (PathNode)this.testRequest.path.nodes.get(var28 + 1);
               var37 = 1.0F;
               var38 = 1.0F;
               var7 = 0.0F;
               if (var32.z != (float)((int)var32.z) || var36.z != (float)((int)var36.z)) {
                  var38 = 0.0F;
               }

               LineDrawer.addLine(var32.x, var32.y, var32.z, var36.x, var36.y, var36.z, var37, var38, var7, (String)null, true);
            }

            this.testRequest.done = false;
         }

         if (DebugOptions.instance.PolymapRenderConnections.getValue()) {
            var2 = (float)Mouse.getX();
            var29 = (float)Mouse.getY();
            var33 = this.testZ;
            var37 = IsoUtils.XToIso(var2, var29, (float)var33);
            var38 = IsoUtils.YToIso(var2, var29, (float)var33);
            VisibilityGraph var42 = this.getVisGraphAt(var37, var38, var33, 1);
            if (var42 != null) {
               Node var43 = var42.getClosestNodeTo(var37, var38);
               if (var43 != null) {
                  Iterator var45 = var43.visible.iterator();

                  while(var45.hasNext()) {
                     Connection var47 = (Connection)var45.next();
                     Node var53 = var47.otherNode(var43);
                     LineDrawer.addLine(var43.x, var43.y, (float)var33, var53.x, var53.y, (float)var33, 1.0F, 0.0F, 0.0F, (String)null, true);
                  }
               }
            }
         }

         if (GameWindow.states.current == DebugChunkState.instance && var1) {
            this.updateMain();
         }

      }
   }

   public void squareChanged(IsoGridSquare var1) {
      if (PathfindNative.USE_NATIVE_CODE) {
         var1.invalidateVispolyChunkLevel();
         FBORenderCutaways.getInstance().squareChanged(var1);
         FMODAmbientWalls.getInstance().squareChanged(var1);
         PathfindNative.instance.squareChanged(var1);
      } else {
         for(int var2 = 0; var2 < 8; ++var2) {
            IsoDirections var3 = IsoDirections.fromIndex(var2);
            IsoGridSquare var4 = var1.getAdjacentSquare(var3);
            if (var4 != null) {
               SquareUpdateTask var5 = SquareUpdateTask.alloc().init(this, var4);
               this.squareTaskQueue.add(var5);
            }
         }

         SquareUpdateTask var6 = SquareUpdateTask.alloc().init(this, var1);
         this.squareTaskQueue.add(var6);
         this.thread.wake();
         var1.invalidateVispolyChunkLevel();
         FBORenderCutaways.getInstance().squareChanged(var1);
         FMODAmbientWalls.getInstance().squareChanged(var1);
      }
   }

   public void addChunkToWorld(IsoChunk var1) {
      ChunkUpdateTask var2 = ChunkUpdateTask.alloc().init(this, var1);
      this.chunkTaskQueue.add(var2);
      this.thread.wake();
   }

   public void removeChunkFromWorld(IsoChunk var1) {
      if (this.thread != null) {
         ChunkRemoveTask var2 = ChunkRemoveTask.alloc().init(this, var1);
         this.chunkTaskQueue.add(var2);
         this.thread.wake();
      }
   }

   public void addVehicleToWorld(BaseVehicle var1) {
      VehicleAddTask var2 = VehicleAddTask.alloc();
      var2.init(this, var1);
      this.vehicleTaskQueue.add(var2);
      VehicleState var3 = VehicleState.alloc().init(var1);
      this.vehicleState.put(var1, var3);
      this.thread.wake();
   }

   public void updateVehicle(BaseVehicle var1) {
      VehicleUpdateTask var2 = VehicleUpdateTask.alloc();
      var2.init(this, var1);
      this.vehicleTaskQueue.add(var2);
      this.thread.wake();
   }

   public void removeVehicleFromWorld(BaseVehicle var1) {
      if (this.thread != null) {
         VehicleRemoveTask var2 = VehicleRemoveTask.alloc();
         var2.init(this, var1);
         this.vehicleTaskQueue.add(var2);
         VehicleState var3 = (VehicleState)this.vehicleState.remove(var1);
         if (var3 != null) {
            var3.vehicle = null;
            var3.release();
         }

         this.thread.wake();
      }
   }

   private Cell getCellFromSquarePos(int var1, int var2) {
      var1 -= this.minX * SQUARES_PER_CELL;
      var2 -= this.minY * SQUARES_PER_CELL;
      if (var1 >= 0 && var2 >= 0) {
         int var3 = var1 / SQUARES_PER_CELL;
         int var4 = var2 / SQUARES_PER_CELL;
         return var3 < this.width && var4 < this.height ? this.cells[var3][var4] : null;
      } else {
         return null;
      }
   }

   Cell getCellFromChunkPos(int var1, int var2) {
      return this.getCellFromSquarePos(var1 * 8, var2 * 8);
   }

   Chunk allocChunkIfNeeded(int var1, int var2) {
      Cell var3 = this.getCellFromChunkPos(var1, var2);
      return var3 == null ? null : var3.allocChunkIfNeeded(var1, var2);
   }

   public Chunk getChunkFromChunkPos(int var1, int var2) {
      Cell var3 = this.getCellFromChunkPos(var1, var2);
      return var3 == null ? null : var3.getChunkFromChunkPos(var1, var2);
   }

   public Chunk getChunkFromSquarePos(int var1, int var2) {
      Cell var3 = this.getCellFromSquarePos(var1, var2);
      return var3 == null ? null : var3.getChunkFromChunkPos(PZMath.coorddivision(var1, 8), PZMath.coorddivision(var2, 8));
   }

   public Square getSquare(int var1, int var2, int var3) {
      Chunk var4 = this.getChunkFromSquarePos(var1, var2);
      return var4 == null ? null : var4.getSquare(var1, var2, var3);
   }

   Square getSquareRawZ(int var1, int var2, int var3) {
      Chunk var4 = this.getChunkFromSquarePos(var1, var2);
      return var4 == null ? null : var4.getSquare(var1, var2, var3);
   }

   boolean isBlockedInAllDirections(int var1, int var2, int var3) {
      Square var4 = this.getSquare(var1, var2, var3);
      if (var4 == null) {
         return false;
      } else {
         Square var5 = this.getSquare(var1, var2 - 1, var3);
         Square var6 = this.getSquare(var1, var2 + 1, var3);
         Square var7 = this.getSquare(var1 - 1, var2, var3);
         Square var8 = this.getSquare(var1 + 1, var2, var3);
         boolean var9 = var5 != null && this.astar.canNotMoveBetween(var4, var5, false);
         boolean var10 = var6 != null && this.astar.canNotMoveBetween(var4, var6, false);
         boolean var11 = var7 != null && this.astar.canNotMoveBetween(var4, var7, false);
         boolean var12 = var8 != null && this.astar.canNotMoveBetween(var4, var8, false);
         return var9 && var10 && var11 && var12;
      }
   }

   public boolean canMoveBetween(PMMover var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      Square var8 = this.getSquare(var2, var3, var4);
      Square var9 = this.getSquare(var5, var6, var7);
      if (var8 != null && var9 != null) {
         PMMover var10 = this.astar.mover;

         boolean var11;
         try {
            this.astar.mover = var1;
            var11 = this.astar.canMoveBetween(var8, var9, false);
         } finally {
            this.astar.mover = var10;
         }

         return var11;
      } else {
         return false;
      }
   }

   public boolean canNotMoveBetween(PMMover var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      return !this.canMoveBetween(var1, var2, var3, var4, var5, var6, var7);
   }

   public void init(IsoMetaGrid var1) {
      this.minX = var1.getMinX();
      this.minY = var1.getMinY();
      this.width = var1.getWidth();
      this.height = var1.getHeight();
      this.cells = new Cell[this.width][this.height];

      for(int var2 = 0; var2 < this.height; ++var2) {
         for(int var3 = 0; var3 < this.width; ++var3) {
            this.cells[var3][var2] = Cell.alloc().init(this, this.minX + var3, this.minY + var2);
         }
      }

      this.thread = new PMThread();
      this.thread.setName("PolyPathThread");
      this.thread.setDaemon(true);
      this.thread.start();
   }

   public void stop() {
      this.thread.bStop = true;
      this.thread.wake();

      while(this.thread.isAlive()) {
         try {
            Thread.sleep(5L);
         } catch (InterruptedException var3) {
         }
      }

      int var1;
      for(var1 = 0; var1 < this.height; ++var1) {
         for(int var2 = 0; var2 < this.width; ++var2) {
            if (this.cells[var2][var1] != null) {
               this.cells[var2][var1].release();
            }
         }
      }

      for(IChunkTask var4 = (IChunkTask)this.chunkTaskQueue.poll(); var4 != null; var4 = (IChunkTask)this.chunkTaskQueue.poll()) {
         var4.release();
      }

      for(SquareUpdateTask var5 = (SquareUpdateTask)this.squareTaskQueue.poll(); var5 != null; var5 = (SquareUpdateTask)this.squareTaskQueue.poll()) {
         var5.release();
      }

      for(IVehicleTask var7 = (IVehicleTask)this.vehicleTaskQueue.poll(); var7 != null; var7 = (IVehicleTask)this.vehicleTaskQueue.poll()) {
         var7.release();
      }

      for(PathRequestTask var9 = (PathRequestTask)this.requestTaskQueue.poll(); var9 != null; var9 = (PathRequestTask)this.requestTaskQueue.poll()) {
         var9.release();
      }

      while(!this.requests.isEmpty()) {
         this.requests.removeLast().release();
      }

      while(!this.requestToMain.isEmpty()) {
         ((PathFindRequest)this.requestToMain.remove()).release();
      }

      for(var1 = 0; var1 < this.vehicles.size(); ++var1) {
         Vehicle var6 = (Vehicle)this.vehicles.get(var1);
         var6.release();
      }

      Iterator var10 = this.vehicleState.values().iterator();

      while(var10.hasNext()) {
         VehicleState var8 = (VehicleState)var10.next();
         var8.release();
      }

      this.requestMap.clear();
      this.vehicles.clear();
      this.vehicleState.clear();
      this.vehicleMap.clear();
      this.cells = null;
      this.thread = null;
      this.rebuild = true;
   }

   public void updateMain() {
      ArrayList var1 = IsoWorld.instance.CurrentCell.getVehicles();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         BaseVehicle var3 = (BaseVehicle)var1.get(var2);
         VehicleState var4 = (VehicleState)this.vehicleState.get(var3);
         if (var4 != null && var4.check()) {
            this.updateVehicle(var3);
         }
      }

      for(PathFindRequest var5 = (PathFindRequest)this.requestToMain.poll(); var5 != null; var5 = (PathFindRequest)this.requestToMain.poll()) {
         if (this.requestMap.get(var5.mover) == var5) {
            this.requestMap.remove(var5.mover);
         }

         if (!var5.cancel) {
            if (var5.path.isEmpty()) {
               var5.finder.Failed(var5.mover);
            } else {
               var5.finder.Succeeded(var5.path, var5.mover);
            }
         }

         var5.release();
      }

   }

   public void updateThread() {
      for(IChunkTask var1 = (IChunkTask)this.chunkTaskQueue.poll(); var1 != null; var1 = (IChunkTask)this.chunkTaskQueue.poll()) {
         var1.execute();
         var1.release();
         this.rebuild = true;
      }

      for(SquareUpdateTask var10 = (SquareUpdateTask)this.squareTaskQueue.poll(); var10 != null; var10 = (SquareUpdateTask)this.squareTaskQueue.poll()) {
         var10.execute();
         var10.release();
      }

      for(IVehicleTask var11 = (IVehicleTask)this.vehicleTaskQueue.poll(); var11 != null; var11 = (IVehicleTask)this.vehicleTaskQueue.poll()) {
         var11.execute();
         var11.release();
         this.rebuild = true;
      }

      for(PathRequestTask var13 = (PathRequestTask)this.requestTaskQueue.poll(); var13 != null; var13 = (PathRequestTask)this.requestTaskQueue.poll()) {
         var13.execute();
         var13.release();
      }

      int var14;
      if (this.rebuild) {
         for(var14 = 0; var14 < this.graphs.size(); ++var14) {
            VisibilityGraph var2 = (VisibilityGraph)this.graphs.get(var14);
            var2.release();
         }

         this.graphs.clear();
         this.squareToNode.forEachValue(this.releaseNodeProc);
         this.createVisibilityGraphs();
         this.rebuild = false;
         ++ChunkDataZ.EPOCH;
         ++HLAStar.ModificationCount;
      }

      var14 = 2;

      while(!this.requests.isEmpty()) {
         PathFindRequest var12 = this.requests.removeFirst();
         if (var12.cancel) {
            this.requestToMain.add(var12);
         } else {
            try {
               this.findPathHighLevelThenLowLevel(var12, false);
            } catch (Exception var9) {
               ExceptionLogger.logException(var9);
            }

            if (!var12.targetXYZ.isEmpty()) {
               this.shortestPath.copyFrom(var12.path);
               float var3 = var12.targetX;
               float var4 = var12.targetY;
               float var5 = var12.targetZ;
               float var6 = this.shortestPath.isEmpty() ? 3.4028235E38F : this.shortestPath.length();

               for(int var7 = 0; var7 < var12.targetXYZ.size(); var7 += 3) {
                  var12.targetX = var12.targetXYZ.get(var7);
                  var12.targetY = var12.targetXYZ.get(var7 + 1);
                  var12.targetZ = var12.targetXYZ.get(var7 + 2);
                  var12.path.clear();
                  this.findPathHighLevelThenLowLevel(var12, false);
                  if (!var12.path.isEmpty()) {
                     float var8 = var12.path.length();
                     if (var8 < var6) {
                        var6 = var8;
                        this.shortestPath.copyFrom(var12.path);
                        var3 = var12.targetX;
                        var4 = var12.targetY;
                        var5 = var12.targetZ;
                     }
                  }
               }

               var12.path.copyFrom(this.shortestPath);
               var12.targetX = var3;
               var12.targetY = var4;
               var12.targetZ = var5;
            }

            this.requestToMain.add(var12);
            --var14;
            if (var14 == 0) {
               break;
            }
         }
      }

   }

   public PathFindRequest addRequest(IPathfinder var1, Mover var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      this.cancelRequest(var2);
      PathFindRequest var9 = PathFindRequest.alloc().init(var1, var2, var3, var4, var5, var6, var7, var8);
      this.requestMap.put(var2, var9);
      PathRequestTask var10 = PathRequestTask.alloc().init(this, var9);
      this.requestTaskQueue.add(var10);
      this.thread.wake();
      return var9;
   }

   public void cancelRequest(Mover var1) {
      PathFindRequest var2 = (PathFindRequest)this.requestMap.remove(var1);
      if (var2 != null) {
         var2.cancel = true;
      }

   }

   public ArrayList<Point> getPointInLine(float var1, float var2, float var3, float var4, int var5) {
      PointPool var6 = new PointPool();
      ArrayList var7 = new ArrayList();
      this.supercover(var1, var2, var3, var4, var5, var6, var7);
      return var7;
   }

   void supercover(float var1, float var2, float var3, float var4, int var5, PointPool var6, ArrayList<Point> var7) {
      double var8 = (double)Math.abs(var3 - var1);
      double var10 = (double)Math.abs(var4 - var2);
      int var12 = PZMath.fastfloor(var1);
      int var13 = PZMath.fastfloor(var2);
      int var14 = 1;
      byte var15;
      double var17;
      if (var8 == 0.0) {
         var15 = 0;
         var17 = 1.0 / 0.0;
      } else if (var3 > var1) {
         var15 = 1;
         var14 += PZMath.fastfloor(var3) - var12;
         var17 = (double)((float)(PZMath.fastfloor(var1) + 1) - var1) * var10;
      } else {
         var15 = -1;
         var14 += var12 - PZMath.fastfloor(var3);
         var17 = (double)(var1 - (float)PZMath.fastfloor(var1)) * var10;
      }

      byte var16;
      if (var10 == 0.0) {
         var16 = 0;
         var17 -= 1.0 / 0.0;
      } else if (var4 > var2) {
         var16 = 1;
         var14 += PZMath.fastfloor(var4) - var13;
         var17 -= (double)((float)(PZMath.fastfloor(var2) + 1) - var2) * var8;
      } else {
         var16 = -1;
         var14 += var13 - PZMath.fastfloor(var4);
         var17 -= (double)(var2 - (float)PZMath.fastfloor(var2)) * var8;
      }

      for(; var14 > 0; --var14) {
         Point var19 = var6.alloc().init(var12, var13);
         if (var7.contains(var19)) {
            var6.release(var19);
         } else {
            var7.add(var19);
         }

         if (var17 > 0.0) {
            var13 += var16;
            var17 -= var8;
         } else {
            var12 += var15;
            var17 += var10;
         }
      }

   }

   public boolean lineClearCollide(float var1, float var2, float var3, float var4, int var5) {
      return this.lineClearCollide(var1, var2, var3, var4, var5, (IsoMovingObject)null);
   }

   public boolean lineClearCollide(float var1, float var2, float var3, float var4, int var5, IsoMovingObject var6) {
      return this.lineClearCollide(var1, var2, var3, var4, var5, var6, true, true);
   }

   public boolean lineClearCollide(float var1, float var2, float var3, float var4, int var5, IsoMovingObject var6, boolean var7, boolean var8) {
      int var9 = 0;
      if (var7) {
         var9 |= 1;
      }

      if (var8) {
         var9 |= 2;
      }

      if (Core.bDebug && DebugOptions.instance.PolymapRenderLineClearCollide.getValue()) {
         var9 |= 8;
      }

      return this.lineClearCollide(var1, var2, var3, var4, var5, var6, var9);
   }

   public boolean lineClearCollide(float var1, float var2, float var3, float var4, int var5, IsoMovingObject var6, int var7) {
      BaseVehicle var8 = null;
      if (var6 instanceof IsoGameCharacter) {
         var8 = ((IsoGameCharacter)var6).getVehicle();
      } else if (var6 instanceof BaseVehicle) {
         var8 = (BaseVehicle)var6;
      }

      return this.lccMain.isNotClear(this, var1, var2, var3, var4, var5, var8, var7);
   }

   public Vector2 getCollidepoint(float var1, float var2, float var3, float var4, int var5, IsoMovingObject var6, int var7) {
      BaseVehicle var8 = null;
      if (var6 instanceof IsoGameCharacter) {
         var8 = ((IsoGameCharacter)var6).getVehicle();
      } else if (var6 instanceof BaseVehicle) {
         var8 = (BaseVehicle)var6;
      }

      return this.lccMain.getCollidepoint(this, var1, var2, var3, var4, var5, var8, var7);
   }

   public boolean canStandAt(float var1, float var2, int var3, IsoMovingObject var4, boolean var5, boolean var6) {
      BaseVehicle var7 = null;
      if (var4 instanceof IsoGameCharacter) {
         var7 = ((IsoGameCharacter)var4).getVehicle();
      } else if (var4 instanceof BaseVehicle) {
         var7 = (BaseVehicle)var4;
      }

      int var8 = 0;
      if (var5) {
         var8 |= 1;
      }

      if (var6) {
         var8 |= 2;
      }

      if (Core.bDebug && DebugOptions.instance.PolymapRenderLineClearCollide.getValue()) {
         var8 |= 8;
      }

      return this.canStandAt(var1, var2, var3, var7, var8);
   }

   public boolean canStandAt(float var1, float var2, int var3, BaseVehicle var4, int var5) {
      return this.lccMain.canStandAtOld(this, var1, var2, (float)var3, var4, var5);
   }

   public boolean intersectLineWithVehicle(float var1, float var2, float var3, float var4, BaseVehicle var5, Vector2 var6) {
      if (var5 != null && var5.getScript() != null) {
         float[] var7 = this.tempFloats;
         var7[0] = var5.getPoly().x1;
         var7[1] = var5.getPoly().y1;
         var7[2] = var5.getPoly().x2;
         var7[3] = var5.getPoly().y2;
         var7[4] = var5.getPoly().x3;
         var7[5] = var5.getPoly().y3;
         var7[6] = var5.getPoly().x4;
         var7[7] = var5.getPoly().y4;
         float var8 = 3.4028235E38F;

         for(int var9 = 0; var9 < 8; var9 += 2) {
            float var10 = var7[var9 % 8];
            float var11 = var7[(var9 + 1) % 8];
            float var12 = var7[(var9 + 2) % 8];
            float var13 = var7[(var9 + 3) % 8];
            double var14 = (double)((var13 - var11) * (var3 - var1) - (var12 - var10) * (var4 - var2));
            if (var14 == 0.0) {
               return false;
            }

            double var16 = (double)((var12 - var10) * (var2 - var11) - (var13 - var11) * (var1 - var10)) / var14;
            double var18 = (double)((var3 - var1) * (var2 - var11) - (var4 - var2) * (var1 - var10)) / var14;
            if (var16 >= 0.0 && var16 <= 1.0 && var18 >= 0.0 && var18 <= 1.0) {
               float var20 = (float)((double)var1 + var16 * (double)(var3 - var1));
               float var21 = (float)((double)var2 + var16 * (double)(var4 - var2));
               float var22 = IsoUtils.DistanceTo(var1, var2, var20, var21);
               if (var22 < var8) {
                  var6.set(var20, var21);
                  var8 = var22;
               }
            }
         }

         return var8 < 3.4028235E38F;
      } else {
         return false;
      }
   }

   public Vector2f resolveCollision(IsoGameCharacter var1, float var2, float var3, Vector2f var4) {
      return GameClient.bClient && var1.isSkipResolveCollision() ? var4.set(var2, var3) : this.collideWithObstacles.resolveCollision(var1, var2, var3, var4);
   }

   static {
      SQUARES_PER_CELL = IsoCell.CellSizeInSquares;
      CHUNKS_PER_CELL = IsoCell.CellSizeInChunks;
   }

   final class PMThread extends Thread {
      public final Object notifier = new Object();
      public boolean bStop;

      PMThread() {
      }

      public void run() {
         while(!this.bStop) {
            try {
               this.runInner();
            } catch (Exception var2) {
               ExceptionLogger.logException(var2);
            }
         }

      }

      private void runInner() {
         MPStatistic.getInstance().PolyPathThread.Start();
         PolygonalMap2.this.sync.startFrame();
         synchronized(PolygonalMap2.this.renderLock) {
            PolygonalMap2.instance.updateThread();
         }

         PolygonalMap2.this.sync.endFrame();
         MPStatistic.getInstance().PolyPathThread.End();

         while(this.shouldWait()) {
            synchronized(this.notifier) {
               try {
                  this.notifier.wait();
               } catch (InterruptedException var4) {
               }
            }
         }

      }

      private boolean shouldWait() {
         if (this.bStop) {
            return false;
         } else if (!PolygonalMap2.instance.chunkTaskQueue.isEmpty()) {
            return false;
         } else if (!PolygonalMap2.instance.squareTaskQueue.isEmpty()) {
            return false;
         } else if (!PolygonalMap2.instance.vehicleTaskQueue.isEmpty()) {
            return false;
         } else if (!PolygonalMap2.instance.requestTaskQueue.isEmpty()) {
            return false;
         } else {
            return PolygonalMap2.instance.requests.isEmpty();
         }
      }

      void wake() {
         synchronized(this.notifier) {
            this.notifier.notify();
         }
      }
   }
}
