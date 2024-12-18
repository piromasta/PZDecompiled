package zombie.pathfind.highLevel;

import astar.AStar;
import astar.ISearchNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import zombie.core.math.PZMath;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoUtils;
import zombie.pathfind.AdjustStartEndNodeData;
import zombie.pathfind.Chunk;
import zombie.pathfind.Connection;
import zombie.pathfind.Edge;
import zombie.pathfind.Node;
import zombie.pathfind.PMMover;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;
import zombie.pathfind.VehicleRect;
import zombie.pathfind.VisibilityGraph;
import zombie.util.list.PZArrayUtil;

public class HLAStar extends AStar {
   static final int CPW = 8;
   public static int ModificationCount = 0;
   public static final PerformanceProfileProbe PerfFindPath = new PerformanceProfileProbe("HLAStar.findPath");
   public static final PerformanceProfileProbe PerfGetSuccessors = new PerformanceProfileProbe("HLAStar.getSuccessors");
   public static final PerformanceProfileProbe PerfGetSuccessors_OnSameChunk = new PerformanceProfileProbe("HLAStar.getSuccessors.OnSameChunk");
   public static final PerformanceProfileProbe PerfGetSuccessors_OnAdjacentChunks = new PerformanceProfileProbe("HLAStar.getSuccessors.OnAdjacentChunks");
   public static final PerformanceProfileProbe PerfGetSuccessors_VisibilityGraphs = new PerformanceProfileProbe("HLAStar.getSuccessors.VisibilityGraphs");
   public static final PerformanceProfileProbe PerfInitStairs = new PerformanceProfileProbe("HLAStar.initStairs");
   HLSearchNode initialNode;
   final HLGoalNode goalNode = new HLGoalNode();
   final HashMap<HLChunkRegion, HLSearchNode> nodeMapChunkRegion = new HashMap();
   final HashMap<HLLevelTransition, HLSearchNode> nodeMapLevelTransition = new HashMap();
   final HashMap<Node, HLSearchNode> nodeMapVisGraph = new HashMap();
   boolean bGoalInUnloadedArea = false;
   HLSearchNode unloadedSearchNode = null;
   final ArrayList<VisibilityGraph> visibilityGraphs = new ArrayList();
   final AdjustVisibilityGraphData adjustVisibilityGraphDataStart = new AdjustVisibilityGraphData();
   final AdjustVisibilityGraphData adjustVisibilityGraphDataGoal = new AdjustVisibilityGraphData();

   public HLAStar() {
   }

   public void findPath(PMMover var1, float var2, float var3, int var4, float var5, float var6, int var7, ArrayList<HLLevelTransition> var8, ArrayList<HLChunkLevel> var9, ArrayList<Boolean> var10, boolean var11) {
      var8.clear();
      var9.clear();
      if (var1 != HLGlobals.mover) {
         HLGlobals.mover.set(var1);
      }

      this.releaseSearchNodes();

      try {
         this.adjustVisibilityGraphDataStart.bAdjusted = false;
         this.adjustVisibilityGraphDataStart.graph = null;
         this.adjustVisibilityGraphDataStart.vgNode = null;
         this.adjustVisibilityGraphDataGoal.bAdjusted = false;
         this.adjustVisibilityGraphDataGoal.graph = null;
         this.adjustVisibilityGraphDataGoal.vgNode = null;
         Chunk var12 = PolygonalMap2.instance.getChunkFromSquarePos(PZMath.fastfloor(var5), PZMath.fastfloor(var6));
         this.bGoalInUnloadedArea = var12 == null;
         this.unloadedSearchNode = null;
         HLSearchNode var13 = this.initStartNode(var2, var3, var4);
         if (var13 != null) {
            HLSearchNode var14 = this.initGoalNode(var5, var6, var7);
            if (var14 == null) {
               return;
            }

            if (var13.chunkRegion == var14.chunkRegion && var13.chunkRegion != null) {
               if (var11) {
                  var13.chunkRegion.renderDebug();
               }

               this.addChunkLevelAndAdjacentToList(var13.chunkRegion.m_levelData, var9);
               return;
            }

            if (var13.levelTransition != null && var13.levelTransition == var14.levelTransition) {
               if (var11) {
                  var13.levelTransition.renderDebug();
               }

               var8.add(var13.levelTransition);
               this.addLevelTransitionChunkLevelData(var13.levelTransition, var9);
               return;
            }

            this.goalNode.init(var14);
            this.initialNode = var13;
            ArrayList var15 = this.shortestPath(var13, this.goalNode);
            if (var15 == null) {
               return;
            }

            var10.clear();

            int var16;
            HLSearchNode var17;
            for(var16 = 0; var16 < var15.size(); ++var16) {
               var17 = (HLSearchNode)var15.get(var16);
               if (var17.chunkRegion != null) {
                  if (var11 && DebugOptions.instance.PolymapRenderClusters.getValue()) {
                     var17.chunkRegion.renderDebug();
                  }

                  this.addChunkLevelAndAdjacentToList(var17.chunkRegion.m_levelData, var9);
               }

               if (var17.levelTransition != null) {
                  if (var11 && DebugOptions.instance.PolymapRenderClusters.getValue()) {
                     var17.levelTransition.renderDebug();
                  }

                  var8.add(var17.levelTransition);
                  this.addLevelTransitionChunkLevelData(var17.levelTransition, var9);
                  boolean var18 = false;
                  if (var16 == var15.size() - 1) {
                     var18 = true;
                  } else {
                     HLSearchNode var19 = (HLSearchNode)var15.get(var16 + 1);
                     var18 = PZMath.fastfloor(var19.getZ()) <= var17.levelTransition.getBottomFloorZ();
                  }

                  var10.add(var18);
               }

               if (var17.vgNode != null) {
                  if (var11) {
                  }

                  var12 = PolygonalMap2.instance.getChunkFromSquarePos(PZMath.fastfloor(var17.vgNode.x), PZMath.fastfloor(var17.vgNode.y));
                  if (var12 != null) {
                     HLChunkLevel var23 = var12.getLevelData(var17.vgNode.z).getHighLevelData();
                     this.addChunkLevelAndAdjacentToList(var23, var9);
                  }
               }
            }

            if (var11) {
               for(var16 = 1; var16 < var15.size(); ++var16) {
                  var17 = (HLSearchNode)var15.get(var16 - 1);
                  HLSearchNode var24 = (HLSearchNode)var15.get(var16);
                  LineDrawer.addRect(var17.getX() - 0.05F, var17.getY() - 0.05F, var17.getZ() - 32.0F, 0.1F, 0.1F, 0.0F, 1.0F, 0.0F);
                  LineDrawer.addLine(var17.getX(), var17.getY(), var17.getZ() - 32.0F, var24.getX(), var24.getY(), var24.getZ() - 32.0F, 0.0F, 1.0F, 0.0F, 1.0F);
               }
            }

            return;
         }
      } finally {
         this.cleanUpVisibilityGraphNode(this.adjustVisibilityGraphDataStart);
         this.cleanUpVisibilityGraphNode(this.adjustVisibilityGraphDataGoal);
         this.releaseSearchNodes();
      }

   }

   void releaseSearchNodes() {
      this.initialNode = null;
      if (this.unloadedSearchNode != null) {
         HLGlobals.searchNodePool.release((Object)this.unloadedSearchNode);
         this.unloadedSearchNode = null;
      }

      HLGlobals.searchNodePool.releaseAll(new ArrayList(this.nodeMapChunkRegion.values()));
      this.nodeMapChunkRegion.clear();
      HLGlobals.searchNodePool.releaseAll(new ArrayList(this.nodeMapLevelTransition.values()));
      this.nodeMapLevelTransition.clear();
      HLGlobals.searchNodePool.releaseAll(new ArrayList(this.nodeMapVisGraph.values()));
      this.nodeMapVisGraph.clear();
   }

   HLSearchNode initStartNode(float var1, float var2, int var3) {
      Node var4 = this.getSquarePolygonalMapNode(PZMath.fastfloor(var1), PZMath.fastfloor(var2), var3);
      if (var4 != null && !this.shouldIgnoreNode(var4)) {
         return this.getSearchNode(var4);
      } else {
         HLChunkRegion var5 = this.getRegionAt(var1, var2, var3);
         if (var5 != null) {
            return this.getSearchNode(var5);
         } else {
            var4 = null;
            VisibilityGraph var6 = PolygonalMap2.instance.getVisGraphAt(var1, var2, var3, 0);
            if (var6 == null) {
               var6 = PolygonalMap2.instance.getVisGraphAt(var1, var2, var3, 1);
               if (var6 != null) {
                  if (!var6.isCreated()) {
                     var6.create();
                  }

                  Square var7 = PolygonalMap2.instance.getSquare(PZMath.fastfloor(var1), PZMath.fastfloor(var2), var3);
                  var4 = PolygonalMap2.instance.getPointOutsideObjects(var7, var1, var2);
                  var6.addNode(var4);
                  if (var4.x != var1 || var4.y != var2) {
                     this.adjustVisibilityGraphDataStart.bAdjusted = true;
                     this.adjustVisibilityGraphDataStart.adjustStartEndNodeData.isNodeNew = false;
                  }

                  this.adjustVisibilityGraphDataStart.vgNode = var4;
                  this.adjustVisibilityGraphDataStart.graph = var6;
               }
            } else {
               var4 = this.initVisibilityGraphNode(var6, var1, var2, (float)var3, this.adjustVisibilityGraphDataStart);
            }

            if (var4 != null) {
               return this.getSearchNode(var4);
            } else {
               HLLevelTransition var8 = this.getLevelTransitionAt(PZMath.fastfloor(var1), PZMath.fastfloor(var2), var3);
               return var8 != null ? this.getSearchNode(var8, true) : null;
            }
         }
      }
   }

   HLSearchNode initGoalNode(float var1, float var2, int var3) {
      Chunk var4 = PolygonalMap2.instance.getChunkFromSquarePos(PZMath.fastfloor(var1), PZMath.fastfloor(var2));
      if (var4 == null) {
         return this.getSearchNode(PZMath.fastfloor(var1), PZMath.fastfloor(var2));
      } else {
         Node var5 = this.getSquarePolygonalMapNode(PZMath.fastfloor(var1), PZMath.fastfloor(var2), var3);
         if (var5 != null && !this.shouldIgnoreNode(var5)) {
            return this.getSearchNode(var5);
         } else {
            HLChunkRegion var6 = this.getRegionAt(var1, var2, var3);
            if (var6 != null) {
               return this.getSearchNode(var6);
            } else {
               var5 = null;
               VisibilityGraph var7 = PolygonalMap2.instance.getVisGraphAt(var1, var2, var3, 0);
               if (var7 == null) {
                  var7 = PolygonalMap2.instance.getVisGraphAt(var1, var2, var3, 1);
                  if (var7 != null) {
                     if (!var7.isCreated()) {
                        var7.create();
                     }

                     Square var8 = PolygonalMap2.instance.getSquare(PZMath.fastfloor(var1), PZMath.fastfloor(var2), var3);
                     var5 = PolygonalMap2.instance.getPointOutsideObjects(var8, var1, var2);
                     var7.addNode(var5);
                     if (var5.x != var1 || var5.y != var2) {
                        this.adjustVisibilityGraphDataGoal.bAdjusted = true;
                        this.adjustVisibilityGraphDataGoal.adjustStartEndNodeData.isNodeNew = false;
                     }

                     this.adjustVisibilityGraphDataGoal.vgNode = var5;
                     this.adjustVisibilityGraphDataGoal.graph = var7;
                  }
               } else {
                  var5 = this.initVisibilityGraphNode(var7, var1, var2, (float)var3, this.adjustVisibilityGraphDataGoal);
               }

               if (var5 != null) {
                  return this.getSearchNode(var5);
               } else {
                  HLLevelTransition var9 = this.getLevelTransitionAt(PZMath.fastfloor(var1), PZMath.fastfloor(var2), var3);
                  return var9 != null ? this.getSearchNode(var9, true) : null;
               }
            }
         }
      }
   }

   public HLLevelTransition getLevelTransitionAt(int var1, int var2, int var3) {
      Chunk var4 = PolygonalMap2.instance.getChunkFromSquarePos(var1, var2);
      if (var4 == null) {
         return null;
      } else if (!var4.isValidLevel(var3)) {
         return null;
      } else {
         HLChunkLevel var5 = var4.getLevelData(var3).getHighLevelData();
         var5.initStairsIfNeeded();
         HLLevelTransition var6 = var5.getLevelTransitionAt(var1, var2);
         return var6;
      }
   }

   Node getSquarePolygonalMapNode(Square var1) {
      if (var1 == null) {
         return null;
      } else {
         Node var2 = PolygonalMap2.instance.getNodeForSquare(var1);
         return var2.visible.isEmpty() ? null : var2;
      }
   }

   Node getSquarePolygonalMapNode(int var1, int var2, int var3) {
      Square var4 = PolygonalMap2.instance.getSquare(var1, var2, var3);
      return this.getSquarePolygonalMapNode(var4);
   }

   Node getStairSquarePolygonalMapNode(Square var1) {
      if (var1 == null) {
         return null;
      } else if (!var1.has(504)) {
         return null;
      } else {
         Node var2 = PolygonalMap2.instance.getNodeForSquare(var1);
         return var2.visible.isEmpty() ? null : var2;
      }
   }

   Node getStairSquarePolygonalMapNode(int var1, int var2, int var3) {
      Square var4 = PolygonalMap2.instance.getSquare(var1, var2, var3);
      return this.getStairSquarePolygonalMapNode(var4);
   }

   boolean isStairSquareWithPolygonalMapNode(Square var1) {
      return this.getStairSquarePolygonalMapNode(var1) != null;
   }

   boolean isStairSquareWithPolygonalMapNode(int var1, int var2, int var3) {
      return this.getStairSquarePolygonalMapNode(var1, var2, var3) != null;
   }

   Node initVisibilityGraphNode(VisibilityGraph var1, float var2, float var3, float var4, AdjustVisibilityGraphData var5) {
      var5.bAdjusted = false;
      var5.graph = null;
      var5.vgNode = null;
      if (!var1.isCreated()) {
         var1.create();
      }

      int var6 = var1.getPointOutsideObstacles(var2, var3, var4, var5.adjustStartEndNodeData);
      if (var6 == -1) {
         return null;
      } else {
         if (var6 == 1) {
            var5.bAdjusted = true;
            var5.vgNode = var5.adjustStartEndNodeData.node;
            if (var5.adjustStartEndNodeData.isNodeNew) {
               var5.graph = var1;
            }
         }

         if (var5.vgNode == null) {
            var5.vgNode = Node.alloc().init(var2, var3, PZMath.fastfloor(var4));
            var1.addNode(var5.vgNode);
            var5.graph = var1;
         }

         return var5.vgNode;
      }
   }

   void cleanUpVisibilityGraphNode(AdjustVisibilityGraphData var1) {
      if (var1.graph != null) {
         var1.graph.removeNode(var1.vgNode);
         var1.vgNode.release();
      }

      if (var1.bAdjusted && var1.adjustStartEndNodeData.isNodeNew) {
         for(int var2 = 0; var2 < var1.vgNode.edges.size(); ++var2) {
            Edge var3 = (Edge)var1.vgNode.edges.get(var2);
            var3.obstacle.unsplit(var1.vgNode, var3.edgeRing);
         }

         var1.graph.edges.remove(var1.adjustStartEndNodeData.newEdge);
      }

   }

   HLChunkRegion getRegionAt(float var1, float var2, int var3) {
      Chunk var4 = PolygonalMap2.instance.getChunkFromChunkPos(PZMath.fastfloor(var1 / 8.0F), PZMath.fastfloor(var2 / 8.0F));
      if (var4 == null) {
         return null;
      } else if (!var4.isValidLevel(var3)) {
         return null;
      } else {
         HLChunkLevel var5 = var4.getLevelData(var3).getHighLevelData();
         return var5.findRegionContainingSquare(PZMath.fastfloor(var1), PZMath.fastfloor(var2));
      }
   }

   void getSuccessors(HLSearchNode var1, ArrayList<ISearchNode> var2) {
      PerfGetSuccessors.invokeAndMeasure(this, var1, var2, HLAStar::getSuccessorsInternal);
   }

   void getSuccessorsInternal(HLSearchNode var1, ArrayList<ISearchNode> var2) {
      HLGlobals.successorPool.releaseAll(var1.successors);
      var1.successors.clear();
      if (!var1.bInUnloadedArea) {
         if (var1.bOnEdgeOfLoadedArea) {
            var2.add(this.unloadedSearchNode);
         }

         if (var1.levelTransition != null) {
            this.getSuccessorChunkRegionsFromLevelTransition(var1, var2);
            this.getSuccessorVisibilityGraphsFromLevelTransition(var1, var2);
         } else if (var1.vgNode != null) {
            this.getSuccessorsFromVisibilityGraph(var1, var2);
         } else {
            PerfGetSuccessors_OnSameChunk.invokeAndMeasure(this, var1, var2, HLAStar::getSuccessorsOnSameChunk);
            PerfGetSuccessors_OnAdjacentChunks.invokeAndMeasure(this, var1, var2, HLAStar::getSuccessorsOnAdjacentChunks);
            this.getSuccessorsGoingUp(var1, var2);
            this.getSuccessorsGoingDown(var1, var2);
            PerfGetSuccessors_VisibilityGraphs.invokeAndMeasure(this, var1, var2, HLAStar::getSuccessorVisibilityGraphs);
         }
      }
   }

   void getSuccessorChunkRegionsFromLevelTransition(HLSearchNode var1, ArrayList<ISearchNode> var2) {
      HLLevelTransition var3 = var1.levelTransition;
      Square var4 = var3.getTopFloorSquare();
      Square var5 = var3.getBottomFloorSquare();
      if (var4 != null && var5 != null) {
         Chunk var6 = var3.getBottomFloorChunk();
         HLChunkLevel var7;
         HLChunkRegion var8;
         double var9;
         if (var6 != null) {
            var7 = var6.getLevelData(var3.getBottomFloorZ()).getHighLevelData();
            var8 = var7.findRegionContainingSquare(var3.getBottomFloorX(), var3.getBottomFloorY());
            if (var8 != null) {
               var9 = this.calculateCost(var1, var4, var8, var5);
               this.addSuccessor(var9, var1, var8, var2);
            }
         }

         var6 = var3.getTopFloorChunk();
         if (var6 != null) {
            var7 = var6.getLevelData(var3.getTopFloorZ()).getHighLevelData();
            var8 = var7.findRegionContainingSquare(var3.getTopFloorX(), var3.getTopFloorY());
            if (var8 != null) {
               var9 = this.calculateCost(var1, var5, var8, var4);
               this.addSuccessor(var9, var1, var8, var2);
            }
         }

      }
   }

   void getSuccessorVisibilityGraphsFromLevelTransition(HLSearchNode var1, ArrayList<ISearchNode> var2) {
      HLLevelTransition var3 = var1.levelTransition;
      Square var4 = var3.getTopFloorSquare();
      Square var5 = var3.getBottomFloorSquare();
      VisibilityGraph var6;
      Node var7;
      double var8;
      if (var4 != null) {
         var6 = PolygonalMap2.instance.getVisGraphAt((float)var4.getX() + 0.5F, (float)var4.getY() + 0.5F, var4.getZ(), 1);
         if (var6 != null) {
            if (!var6.isCreated()) {
               var6.create();
            }

            var7 = PolygonalMap2.instance.getNodeForSquare(var4);
            var8 = 1.0;
            this.addSuccessor(var8, var1, var7, var2);
         }
      }

      if (var5 != null) {
         var6 = PolygonalMap2.instance.getVisGraphAt((float)var5.getX() + 0.5F, (float)var5.getY() + 0.5F, var5.getZ(), 1);
         if (var6 != null) {
            if (!var6.isCreated()) {
               var6.create();
            }

            var7 = PolygonalMap2.instance.getNodeForSquare(var5);
            var8 = 1.0;
            this.addSuccessor(var8, var1, var7, var2);
         }
      }

   }

   void getSuccessorsOnSameChunk(HLSearchNode var1, ArrayList<ISearchNode> var2) {
      HLChunkRegion var3 = var1.chunkRegion;
      HLChunkLevel var4 = var3.m_levelData;
      Chunk var5 = var3.getChunk();
      int var6 = var4.getLevel();

      int var7;
      int var8;
      HLChunkRegion var9;
      for(var7 = 0; var7 < 8; ++var7) {
         if (var7 > 0 && var3.edgeN[var7] != 0) {
            for(var8 = 0; var8 < var4.m_regionList.size(); ++var8) {
               var9 = (HLChunkRegion)var4.m_regionList.get(var8);
               if (var3 != var9 && (var9.edgeS[var7 - 1] & var3.edgeN[var7]) != 0) {
                  this.addSuccessorsOnEdge(var1, var9, var7, var9.edgeS[var7 - 1] & var3.edgeN[var7], var5, IsoDirections.N, var6, var2);
               }
            }
         }

         if (var7 < 7 && var3.edgeS[var7] != 0) {
            for(var8 = 0; var8 < var4.m_regionList.size(); ++var8) {
               var9 = (HLChunkRegion)var4.m_regionList.get(var8);
               if (var3 != var9 && (var9.edgeN[var7 + 1] & var3.edgeS[var7]) != 0) {
                  this.addSuccessorsOnEdge(var1, var9, var7, var9.edgeN[var7 + 1] & var3.edgeS[var7], var5, IsoDirections.S, var6, var2);
               }
            }
         }
      }

      for(var7 = 0; var7 < 8; ++var7) {
         if (var7 > 0 && var3.edgeW[var7] != 0) {
            for(var8 = 0; var8 < var4.m_regionList.size(); ++var8) {
               var9 = (HLChunkRegion)var4.m_regionList.get(var8);
               if (var3 != var9 && (var9.edgeE[var7 - 1] & var3.edgeW[var7]) != 0) {
                  this.addSuccessorsOnEdge(var1, var9, var7, var9.edgeE[var7 - 1] & var3.edgeW[var7], var5, IsoDirections.W, var6, var2);
               }
            }
         }

         if (var7 < 7 && var3.edgeE[var7] != 0) {
            for(var8 = 0; var8 < var4.m_regionList.size(); ++var8) {
               var9 = (HLChunkRegion)var4.m_regionList.get(var8);
               if (var3 != var9 && (var9.edgeW[var7 + 1] & var3.edgeE[var7]) != 0) {
                  this.addSuccessorsOnEdge(var1, var9, var7, var9.edgeW[var7 + 1] & var3.edgeE[var7], var5, IsoDirections.E, var6, var2);
               }
            }
         }
      }

   }

   void getSuccessorsOnAdjacentChunks(HLSearchNode var1, ArrayList<ISearchNode> var2) {
      HLChunkRegion var3 = var1.chunkRegion;
      HLChunkLevel var4 = var3.m_levelData;
      Chunk var5 = var3.getChunk();
      int var6 = var4.getLevel();
      Square[][] var7 = var5.getSquaresForLevel(var6);
      Chunk var8;
      HLChunkLevel var9;
      int var10;
      HLChunkRegion var11;
      if (var3.edgeN[0] != 0) {
         var8 = PolygonalMap2.instance.getChunkFromChunkPos(var5.wx, var5.wy - 1);
         if (var8 != null && var8.isValidLevel(var6)) {
            var9 = var8.getLevelData(var6).getHighLevelData();

            for(var10 = 0; var10 < var9.m_regionList.size(); ++var10) {
               var11 = (HLChunkRegion)var9.m_regionList.get(var10);
               if ((var11.edgeS[7] & var3.edgeN[0]) != 0) {
                  this.addSuccessorsOnEdge(var1, var11, 0, var11.edgeS[7] & var3.edgeN[0], var5, IsoDirections.N, var6, var2);
               }
            }
         }
      }

      if (var3.edgeS[7] != 0) {
         var8 = PolygonalMap2.instance.getChunkFromChunkPos(var5.wx, var5.wy + 1);
         if (var8 != null && var8.isValidLevel(var6)) {
            var9 = var8.getLevelData(var6).getHighLevelData();

            for(var10 = 0; var10 < var9.m_regionList.size(); ++var10) {
               var11 = (HLChunkRegion)var9.m_regionList.get(var10);
               if ((var11.edgeN[0] & var3.edgeS[7]) != 0) {
                  this.addSuccessorsOnEdge(var1, var11, 7, var11.edgeN[0] & var3.edgeS[7], var5, IsoDirections.S, var6, var2);
               }
            }
         }
      }

      int var12;
      if (var3.edgeW[0] != 0) {
         var8 = PolygonalMap2.instance.getChunkFromChunkPos(var5.wx - 1, var5.wy);
         if (var8 != null && var8.isValidLevel(var6)) {
            var9 = var8.getLevelData(var6).getHighLevelData();

            for(var10 = 0; var10 < var9.m_regionList.size(); ++var10) {
               var11 = (HLChunkRegion)var9.m_regionList.get(var10);
               if ((var11.edgeE[7] & var3.edgeW[0]) != 0) {
                  for(var12 = 0; var12 < 8; ++var12) {
                     this.addSuccessorsOnEdge(var1, var11, 0, var11.edgeE[7] & var3.edgeW[0], var5, IsoDirections.W, var6, var2);
                  }
               }
            }
         }
      }

      if (var3.edgeE[7] != 0) {
         var8 = PolygonalMap2.instance.getChunkFromChunkPos(var5.wx + 1, var5.wy);
         if (var8 != null && var8.isValidLevel(var6)) {
            var9 = var8.getLevelData(var6).getHighLevelData();

            for(var10 = 0; var10 < var9.m_regionList.size(); ++var10) {
               var11 = (HLChunkRegion)var9.m_regionList.get(var10);
               if ((var11.edgeW[0] & var3.edgeE[7]) != 0) {
                  for(var12 = 0; var12 < 8; ++var12) {
                     this.addSuccessorsOnEdge(var1, var11, 7, var11.edgeW[0] & var3.edgeE[7], var5, IsoDirections.E, var6, var2);
                  }
               }
            }
         }
      }

      Square var13;
      Square var14;
      double var15;
      int var17;
      int var18;
      HLChunkLevel var19;
      Chunk var20;
      HLChunkRegion var21;
      if (var3.containsSquare(var17 = var5.getMinX(), var18 = var5.getMinY())) {
         var20 = PolygonalMap2.instance.getChunkFromChunkPos(var5.wx - 1, var5.wy - 1);
         if (var20 != null && var20.isValidLevel(var6)) {
            var19 = var20.getLevelData(var6).getHighLevelData();
            var21 = var19.findRegionContainingSquare(var17 - 1, var18 - 1);
            if (var21 != null && PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, var17, var18, var6, var17 - 1, var18 - 1, var6)) {
               var13 = var7[0][0];
               var14 = var20.getSquaresForLevel(var6)[7][7];
               var15 = this.calculateCost(var1, var13, var21, var14);
               this.addSuccessor(var15, var1, var21, var2);
            }
         }
      }

      if (var3.containsSquare(var17 = var5.getMaxX(), var18 = var5.getMinY())) {
         var20 = PolygonalMap2.instance.getChunkFromChunkPos(var5.wx + 1, var5.wy - 1);
         if (var20 != null && var20.isValidLevel(var6)) {
            var19 = var20.getLevelData(var6).getHighLevelData();
            var21 = var19.findRegionContainingSquare(var17 + 1, var18 - 1);
            if (var21 != null && PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, var17, var18, var6, var17 + 1, var18 - 1, var6)) {
               var13 = var7[7][0];
               var14 = var20.getSquaresForLevel(var6)[0][7];
               var15 = this.calculateCost(var1, var13, var21, var14);
               this.addSuccessor(var15, var1, var21, var2);
            }
         }
      }

      if (var3.containsSquare(var17 = var5.getMaxX(), var18 = var5.getMaxY())) {
         var20 = PolygonalMap2.instance.getChunkFromChunkPos(var5.wx + 1, var5.wy + 1);
         if (var20 != null && var20.isValidLevel(var6)) {
            var19 = var20.getLevelData(var6).getHighLevelData();
            var21 = var19.findRegionContainingSquare(var17 + 1, var18 + 1);
            if (var21 != null && PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, var17, var18, var6, var17 + 1, var18 + 1, var6)) {
               var13 = var7[7][7];
               var14 = var20.getSquaresForLevel(var6)[0][0];
               var15 = this.calculateCost(var1, var13, var21, var14);
               this.addSuccessor(var15, var1, var21, var2);
            }
         }
      }

      if (var3.containsSquare(var17 = var5.getMinX(), var18 = var5.getMaxY())) {
         var20 = PolygonalMap2.instance.getChunkFromChunkPos(var5.wx - 1, var5.wy + 1);
         if (var20 != null && var20.isValidLevel(var6)) {
            var19 = var20.getLevelData(var6).getHighLevelData();
            var21 = var19.findRegionContainingSquare(var17 - 1, var18 + 1);
            if (var21 != null && PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, var17, var18, var6, var17 - 1, var18 + 1, var6)) {
               var13 = var7[0][7];
               var14 = var20.getSquaresForLevel(var6)[7][0];
               var15 = this.calculateCost(var1, var13, var21, var14);
               this.addSuccessor(var15, var1, var21, var2);
            }
         }
      }

   }

   void addSuccessorsOnEdge(HLSearchNode var1, HLChunkRegion var2, int var3, int var4, Chunk var5, IsoDirections var6, int var7, ArrayList<ISearchNode> var8) {
      Square[][] var9 = var5.getSquaresForLevel(var7);

      for(int var10 = 0; var10 < 8; ++var10) {
         if ((var4 & 1 << var10) != 0) {
            Square var11 = var6.dx() == 0 ? var9[var10][var3] : var9[var3][var10];
            if (PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, var11.getX(), var11.getY(), var11.getZ(), var11.getX() + var6.dx(), var11.getY() + var6.dy(), var11.getZ())) {
               double var12 = this.calculateCost(var1, var11, var2, var11.getAdjacentSquare(var6));
               this.addSuccessor(var12, var1, var2, var8);
            }
         }
      }

   }

   void addSuccessorChunkRegionsAdjacentToSquare(HLSearchNode var1, Square var2, ArrayList<ISearchNode> var3) {
      for(int var4 = 0; var4 < 8; ++var4) {
         IsoDirections var5 = IsoDirections.fromIndex(var4);
         int var6 = var2.getX() + var5.dx();
         int var7 = var2.getY() + var5.dy();
         Chunk var8 = PolygonalMap2.instance.getChunkFromSquarePos(var6, var7);
         if (var8 != null) {
            Square[][] var9 = var8.getSquaresForLevel(var2.getZ());
            int var10 = PZMath.coordmodulo(var6, 8);
            int var11 = PZMath.coordmodulo(var7, 8);
            Square var12 = var9[var10][var11];
            if (var12 != null && !PolygonalMap2.instance.canNotMoveBetween(HLGlobals.mover, var2.getX(), var2.getY(), var2.getZ(), var12.getX(), var12.getY(), var12.getZ())) {
               HLChunkLevel var13 = var8.getLevelData(var2.getZ()).getHighLevelData();

               for(int var14 = 0; var14 < var13.m_regionList.size(); ++var14) {
                  HLChunkRegion var15 = (HLChunkRegion)var13.m_regionList.get(var14);
                  if (var15.containsSquare(var6, var7)) {
                     double var16 = this.calculateCost(var1, var2, var15, var12);
                     this.addSuccessor(var16, var1, var15, var3);
                  }
               }
            }
         }
      }

   }

   void getSuccessorsGoingUp(HLSearchNode var1, ArrayList<ISearchNode> var2) {
      HLChunkRegion var3 = var1.chunkRegion;
      HLChunkLevel var4 = var3.m_levelData;
      Chunk var5 = var3.getChunk();
      int var6 = var4.getLevel();
      var4.initStairsIfNeeded();
      Square[][] var7 = var5.getSquaresForLevel(var6);

      int var8;
      Square var10;
      double var11;
      Node var13;
      for(var8 = 0; var8 < var4.m_stairs.size(); ++var8) {
         HLStaircase var9 = (HLStaircase)var4.m_stairs.get(var8);
         if (var3.containsSquare(var9.getBottomFloorX(), var9.getBottomFloorY())) {
            var10 = var7[PZMath.coordmodulo(var9.getBottomFloorX(), 8)][PZMath.coordmodulo(var9.getBottomFloorY(), 8)];
            var11 = this.calculateCost(var1, var10);
            var13 = this.getSquarePolygonalMapNode(var10);
            if (var13 != null) {
               this.addSuccessor(var11, var1, var13, var2);
            } else {
               this.addSuccessor(var11, var1, var9, true, var2);
            }
         }
      }

      for(var8 = 0; var8 < var4.m_slopedSurfaces.size(); ++var8) {
         HLSlopedSurface var14 = (HLSlopedSurface)var4.m_slopedSurfaces.get(var8);
         if (var3.containsSquare(var14.getBottomFloorX(), var14.getBottomFloorY())) {
            var10 = var7[PZMath.coordmodulo(var14.getBottomFloorX(), 8)][PZMath.coordmodulo(var14.getBottomFloorY(), 8)];
            var11 = this.calculateCost(var1, var10);
            var13 = this.getSquarePolygonalMapNode(var10);
            if (var13 != null) {
               this.addSuccessor(var11, var1, var13, var2);
            } else {
               this.addSuccessor(var11, var1, var14, true, var2);
            }
         }
      }

   }

   void getSuccessorsGoingDown(HLSearchNode var1, ArrayList<ISearchNode> var2) {
      HLChunkRegion var3 = var1.chunkRegion;
      HLChunkLevel var4 = var3.m_levelData;
      Chunk var5 = var3.getChunk();
      int var6 = var4.getLevel();
      this.getSuccessorsGoingDown_Staircases(var1, var3, var5.wx, var5.wy, var6, var2);
      this.getSuccessorsGoingDown_Staircases(var1, var3, var5.wx, var5.wy + 1, var6, var2);
      this.getSuccessorsGoingDown_Staircases(var1, var3, var5.wx + 1, var5.wy, var6, var2);
      this.getSuccessorsGoingDown_SlopedSurfaces(var1, var3, var5.wx, var5.wy, var6, var2);
      this.getSuccessorsGoingDown_SlopedSurfaces(var1, var3, var5.wx, var5.wy + 1, var6, var2);
      this.getSuccessorsGoingDown_SlopedSurfaces(var1, var3, var5.wx, var5.wy - 1, var6, var2);
      this.getSuccessorsGoingDown_SlopedSurfaces(var1, var3, var5.wx + 1, var5.wy, var6, var2);
      this.getSuccessorsGoingDown_SlopedSurfaces(var1, var3, var5.wx - 1, var5.wy, var6, var2);
   }

   void getSuccessorsGoingDown_Staircases(HLSearchNode var1, HLChunkRegion var2, int var3, int var4, int var5, ArrayList<ISearchNode> var6) {
      Chunk var7 = var2.getChunk();
      Square[][] var8 = var7.getSquaresForLevel(var5);
      Chunk var9 = PolygonalMap2.instance.getChunkFromChunkPos(var3, var4);
      if (var9 != null && var9.isValidLevel(var5 - 1)) {
         HLChunkLevel var10 = var9.getLevelData(var5 - 1).getHighLevelData();
         var10.initStairsIfNeeded();

         for(int var11 = 0; var11 < var10.m_stairs.size(); ++var11) {
            HLStaircase var12 = (HLStaircase)var10.m_stairs.get(var11);
            if (var2.containsSquare(var12.getTopFloorX(), var12.getTopFloorY())) {
               Square var13 = var8[PZMath.coordmodulo(var12.getTopFloorX(), 8)][PZMath.coordmodulo(var12.getTopFloorY(), 8)];
               double var14 = this.calculateCost(var1, var13);
               Node var16 = this.getSquarePolygonalMapNode(var13);
               if (var16 != null) {
                  this.addSuccessor(var14, var1, var16, var6);
               } else {
                  this.addSuccessor(var14, var1, var12, false, var6);
               }
            }
         }

      }
   }

   void getSuccessorsGoingDown_SlopedSurfaces(HLSearchNode var1, HLChunkRegion var2, int var3, int var4, int var5, ArrayList<ISearchNode> var6) {
      Chunk var7 = var2.getChunk();
      Square[][] var8 = var7.getSquaresForLevel(var5);
      Chunk var9 = PolygonalMap2.instance.getChunkFromChunkPos(var3, var4);
      if (var9 != null && var9.isValidLevel(var5 - 1)) {
         HLChunkLevel var10 = var9.getLevelData(var5 - 1).getHighLevelData();
         var10.initStairsIfNeeded();

         for(int var11 = 0; var11 < var10.m_slopedSurfaces.size(); ++var11) {
            HLSlopedSurface var12 = (HLSlopedSurface)var10.m_slopedSurfaces.get(var11);
            if (var2.containsSquare(var12.getTopFloorX(), var12.getTopFloorY())) {
               Square var13 = var8[PZMath.coordmodulo(var12.getTopFloorX(), 8)][PZMath.coordmodulo(var12.getTopFloorY(), 8)];
               double var14 = this.calculateCost(var1, var13);
               Node var16 = this.getSquarePolygonalMapNode(var13);
               if (var16 != null) {
                  this.addSuccessor(var14, var1, var16, var6);
               } else {
                  this.addSuccessor(var14, var1, var12, false, var6);
               }
            }
         }

      }
   }

   void getSuccessorVisibilityGraphs(HLSearchNode var1, ArrayList<ISearchNode> var2) {
      HLChunkRegion var3 = var1.chunkRegion;
      HLChunkLevel var4 = var3.m_levelData;
      Chunk var5 = var3.getChunk();
      int var6 = var4.getLevel();
      Square[][] var7 = var5.getSquaresForLevel(var6);
      int var8 = var5.getMinX();
      int var9 = var5.getMinY();
      int var10 = var5.getMaxX() + 1;
      int var11 = var5.getMaxY() + 1;
      this.visibilityGraphs.clear();
      PolygonalMap2.instance.getVisibilityGraphsOverlappingChunk(var5, var6, this.visibilityGraphs);
      PolygonalMap2.instance.getVisibilityGraphsAdjacentToChunk(var5, var6, this.visibilityGraphs);
      if (!this.visibilityGraphs.isEmpty()) {
         int var12;
         for(var12 = 0; var12 < this.visibilityGraphs.size(); ++var12) {
            VisibilityGraph var13 = (VisibilityGraph)this.visibilityGraphs.get(var12);

            for(int var14 = 0; var14 < var13.perimeterNodes.size(); ++var14) {
               Node var15 = (Node)var13.perimeterNodes.get(var14);
               Square var16 = var15.square;
               if (var16.isInside(var8 - 1, var9 - 1, var10 + 1, var11 + 1)) {
                  for(int var17 = 0; var17 < 8; ++var17) {
                     IsoDirections var18 = IsoDirections.fromIndex(var17);
                     int var19 = var16.getX() + var18.dx();
                     int var20 = var16.getY() + var18.dy();
                     if (var3.containsSquare(var19, var20) && PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, var19, var20, var6, var16.getX(), var16.getY(), var6)) {
                        Square var21 = var7[var19 - var8][var20 - var9];
                        double var22 = this.calculateCost(var1, var21, (float)var16.getX() + 0.5F, (float)var16.getY() + 0.5F, var16);
                        this.addSuccessor(var22, var1, var15, var2);
                     }
                  }
               }
            }
         }

         for(var12 = 0; var12 < 8; ++var12) {
            for(int var24 = 0; var24 < 8; ++var24) {
               Square var25 = var7[var24][var12];
               if (var3.containsSquareLocal(var24, var12) && var25 != null) {
                  boolean var26 = var25.has(256);
                  boolean var27 = var25.has(32);
                  if (var26 || var27) {
                     IsoDirections var28 = var26 ? IsoDirections.N : IsoDirections.W;
                     Square var29 = var25.getAdjacentSquare(var28.Rot180());
                     if (var29 != null) {
                        Node var30 = PolygonalMap2.instance.getExistingNodeForSquare(var29);
                        if (var30 != null && var30.hasFlag(16) && PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, var25.getX(), var25.getY(), var6, var29.getX(), var29.getY(), var6)) {
                           double var31 = this.calculateCost(var1, var25, (float)var29.getX() + 0.5F, (float)var29.getY() + 0.5F, var29);
                           this.addSuccessor(var31, var1, var30, var2);
                        }
                     }
                  }
               }
            }
         }

      }
   }

   void getSuccessorsFromVisibilityGraph(HLSearchNode var1, ArrayList<ISearchNode> var2) {
      Node var3 = var1.vgNode;
      int var4;
      if (var3.graphs != null) {
         for(var4 = 0; var4 < var3.graphs.size(); ++var4) {
            VisibilityGraph var5 = (VisibilityGraph)var3.graphs.get(var4);
            if (!var5.isCreated()) {
               var5.create();
            }
         }
      }

      for(var4 = 0; var4 < var3.visible.size(); ++var4) {
         Connection var10 = (Connection)var3.visible.get(var4);
         Node var6 = var10.otherNode(var3);
         if (!this.shouldIgnoreNode(var6) && (HLGlobals.mover.bCanCrawl || !var6.hasFlag(2)) && (HLGlobals.mover.bCanThump || !var10.has(2))) {
            double var7 = (double)IsoUtils.DistanceTo(var3.x, var3.y, var6.x, var6.y);
            this.addSuccessor(var7, var1, var6, var2);
         }
      }

      Square var9 = var3.square;
      if (var9 != null) {
         if (var3.hasFlag(8) || var3.hasFlag(16)) {
            this.addSuccessorChunkRegionsAdjacentToSquare(var1, var9, var2);
         }

      }
   }

   boolean shouldIgnoreNode(Node var1) {
      if (var1.hasFlag(16)) {
         for(int var2 = 0; var2 < var1.visible.size(); ++var2) {
            Connection var3 = (Connection)var1.visible.get(var2);
            if (!var3.node1.hasFlag(16)) {
               return false;
            }

            if (!var3.node2.hasFlag(16)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   void getStaircasesInVisibilityGraph(VisibilityGraph var1, ArrayList<HLStaircase> var2) {
      int var3 = var1.cluster.z;
      VehicleRect var4 = var1.cluster.bounds();
      byte var5 = 4;
      int var6 = PZMath.fastfloor((float)(var4.left() - var5) / 8.0F);
      int var7 = PZMath.fastfloor((float)(var4.top() - var5) / 8.0F);
      int var8 = (int)PZMath.ceil((float)(var4.right() + var5) / 8.0F);
      int var9 = (int)PZMath.ceil((float)(var4.bottom() + var5) / 8.0F);

      for(int var10 = var7; var10 < var9; ++var10) {
         for(int var11 = var6; var11 < var8; ++var11) {
            Chunk var12 = PolygonalMap2.instance.getChunkFromChunkPos(var11, var10);
            if (var12 != null && var12.isValidLevel(var3)) {
               HLChunkLevel var13 = var12.getLevelData(var3).getHighLevelData();

               int var14;
               HLStaircase var15;
               Square var16;
               for(var14 = 0; var14 < var13.m_stairs.size(); ++var14) {
                  var15 = (HLStaircase)var13.m_stairs.get(var14);
                  if (var15.getBottomFloorZ() == var3) {
                     var16 = var15.getBottomFloorSquare();
                     if (var16 != null && var1.contains(var16)) {
                        var2.add(var15);
                     }
                  }
               }

               if (!var12.isValidLevel(var3 - 1)) {
                  var13 = var12.getLevelData(var3 - 1).getHighLevelData();

                  for(var14 = 0; var14 < var13.m_stairs.size(); ++var14) {
                     var15 = (HLStaircase)var13.m_stairs.get(var14);
                     if (var15.getTopFloorZ() == var3) {
                        var16 = var15.getTopFloorSquare();
                        if (var16 != null && var1.contains(var16)) {
                           var2.add(var15);
                        }
                     }
                  }
               }
            }
         }
      }

      var4.release();
   }

   void addSuccessor(double var1, HLSearchNode var3, HLChunkRegion var4, ArrayList<ISearchNode> var5) {
      if (var4 != null) {
         HLSearchNode var6 = this.getSearchNode(var4);
         if (!var5.contains(var6)) {
            var5.add(var6);
         }

         this.setLowestCostSuccessor(var3, var6, var1);
      }
   }

   void addSuccessor(double var1, HLSearchNode var3, HLLevelTransition var4, boolean var5, ArrayList<ISearchNode> var6) {
      if (var4 != null) {
         HLSearchNode var7 = this.getSearchNode(var4, var5);
         if (!var6.contains(var7)) {
            var6.add(var7);
         }

         this.setLowestCostSuccessor(var3, var7, var1);
      }
   }

   void addSuccessor(double var1, HLSearchNode var3, Node var4, ArrayList<ISearchNode> var5) {
      HLSearchNode var6 = this.getSearchNode(var4);
      if (!var5.contains(var6)) {
         var5.add(var6);
      }

      if (var3.vgNode == null) {
         this.setLowestCostSuccessor(var3, var6, var1);
      }
   }

   void setLowestCostSuccessor(HLSearchNode var1, HLSearchNode var2, double var3) {
      HLSuccessor var5 = (HLSuccessor)PZArrayUtil.find((List)var1.successors, (var1x) -> {
         return var1x.searchNode == var2;
      });
      if (var5 == null || !(var5.cost <= var3)) {
         if (var5 == null) {
            var5 = (HLSuccessor)HLGlobals.successorPool.alloc();
            var1.successors.add(var5);
         }

         var5.searchNode = var2;
         var5.cost = var3;
      }
   }

   double calculateCost(HLSearchNode var1, Square var2, HLChunkRegion var3, Square var4) {
      return this.calculateCost(var1, var2, (float)(var3.minX + var3.maxX + 1) / 2.0F, (float)(var3.minY + var3.maxY + 1) / 2.0F, var4);
   }

   double calculateCost(HLSearchNode var1, Square var2, float var3, float var4, Square var5) {
      float var6 = (float)var2.getX() + 0.5F;
      float var7 = (float)var2.getY() + 0.5F;
      float var8 = (float)var2.getZ();
      float var9 = (float)var2.getX() + 0.5F;
      float var10 = (float)var2.getY() + 0.5F;
      float var11 = (float)var2.getZ();
      double var12 = (double)IsoUtils.DistanceTo(var1.getX(), var1.getY(), var6, var7);
      var12 += Math.sqrt(Math.pow((double)(var6 - var9), 2.0) + Math.pow((double)(var7 - var10), 2.0) + Math.pow((double)((var8 - var11) * 2.5F), 2.0));
      var12 += (double)IsoUtils.DistanceTo(var9, var10, var3, var4);
      boolean var14 = HLGlobals.mover.isZombie() && HLGlobals.mover.bCrawling;
      boolean var15 = !HLGlobals.mover.isZombie() || HLGlobals.mover.bCrawling;
      if (var15) {
         if (this.isAdjacentSquare(var2, var5, IsoDirections.N)) {
            if (!var14 && var2.isUnblockedWindowN()) {
               var12 += 20.0;
            } else if (var2.has(4096)) {
               var12 += 200.0;
            }
         } else if (this.isAdjacentSquare(var2, var5, IsoDirections.S)) {
            if (!var14 && var5.isUnblockedWindowN()) {
               var12 += 20.0;
            } else if (var5.has(4096)) {
               var12 += 200.0;
            }
         } else if (this.isAdjacentSquare(var2, var5, IsoDirections.W)) {
            if (!var14 && var2.isUnblockedWindowW()) {
               var12 += 20.0;
            } else if (var2.has(2048)) {
               var12 += 200.0;
            }
         } else if (this.isAdjacentSquare(var2, var5, IsoDirections.E)) {
            if (!var14 && var5.isUnblockedWindowW()) {
               var12 += 20.0;
            } else if (var5.has(2048)) {
               var12 += 200.0;
            }
         }
      }

      return var12;
   }

   double calculateCost(HLSearchNode var1, Square var2) {
      return (double)IsoUtils.DistanceTo(var1.getX(), var1.getY(), (float)var2.getX() + 0.5F, (float)var2.getY() + 0.5F);
   }

   boolean isAdjacentSquare(Square var1, Square var2, IsoDirections var3) {
      return var1.getX() + var3.dx() == var2.getX() && var1.getY() + var3.dy() == var2.getY() && var1.getZ() == var2.getZ();
   }

   HLSearchNode getSearchNode(HLChunkRegion var1) {
      HLSearchNode var2 = (HLSearchNode)this.nodeMapChunkRegion.get(var1);
      if (var2 == null) {
         var2 = (HLSearchNode)HLGlobals.searchNodePool.alloc();
         var2.setG(0.0);
         var2.astar = this;
         var2.chunkRegion = var1;
         var2.levelTransition = null;
         var2.bBottomOfStaircase = false;
         var2.vgNode = null;
         var2.unloadedX = var2.unloadedY = -1;
         var2.bInUnloadedArea = false;
         var2.bOnEdgeOfLoadedArea = this.bGoalInUnloadedArea && var2.calculateOnEdgeOfLoadedArea();
         var2.parent = null;
         this.nodeMapChunkRegion.put(var1, var2);
      }

      return var2;
   }

   HLSearchNode getSearchNode(HLLevelTransition var1, boolean var2) {
      HLSearchNode var3 = (HLSearchNode)this.nodeMapLevelTransition.get(var1);
      if (var3 == null) {
         var3 = (HLSearchNode)HLGlobals.searchNodePool.alloc();
         var3.setG(0.0);
         var3.astar = this;
         var3.chunkRegion = null;
         var3.levelTransition = var1;
         var3.bBottomOfStaircase = var2;
         var3.vgNode = null;
         var3.unloadedX = var3.unloadedY = -1;
         var3.bInUnloadedArea = false;
         var3.bOnEdgeOfLoadedArea = this.bGoalInUnloadedArea && var3.calculateOnEdgeOfLoadedArea();
         var3.parent = null;
         this.nodeMapLevelTransition.put(var1, var3);
      }

      return var3;
   }

   HLSearchNode getSearchNode(Node var1) {
      HLSearchNode var2 = (HLSearchNode)this.nodeMapVisGraph.get(var1);
      if (var2 == null) {
         var2 = (HLSearchNode)HLGlobals.searchNodePool.alloc();
         var2.setG(0.0);
         var2.astar = this;
         var2.chunkRegion = null;
         var2.levelTransition = null;
         var2.bBottomOfStaircase = false;
         var2.vgNode = var1;
         var2.unloadedX = var2.unloadedY = -1;
         var2.bInUnloadedArea = false;
         var2.bOnEdgeOfLoadedArea = this.bGoalInUnloadedArea && var2.calculateOnEdgeOfLoadedArea();
         var2.parent = null;
         this.nodeMapVisGraph.put(var1, var2);
      }

      return var2;
   }

   HLSearchNode getSearchNode(int var1, int var2) {
      if (this.unloadedSearchNode == null) {
         HLSearchNode var3 = (HLSearchNode)HLGlobals.searchNodePool.alloc();
         var3.setG(0.0);
         var3.astar = this;
         var3.chunkRegion = null;
         var3.levelTransition = null;
         var3.bBottomOfStaircase = false;
         var3.vgNode = null;
         var3.unloadedX = var1;
         var3.unloadedY = var2;
         var3.bInUnloadedArea = true;
         var3.bOnEdgeOfLoadedArea = false;
         var3.parent = null;
         this.unloadedSearchNode = var3;
      }

      return this.unloadedSearchNode;
   }

   void addChunkLevelAndAdjacentToList(HLChunkLevel var1, ArrayList<HLChunkLevel> var2) {
      if (!var2.contains(var1)) {
         var2.add(var1);
      }

      short var3 = var1.getChunk().wx;
      short var4 = var1.getChunk().wy;
      int var5 = var1.getLevel();

      for(int var6 = 0; var6 < 8; ++var6) {
         IsoDirections var7 = IsoDirections.fromIndex(var6);
         Chunk var8 = PolygonalMap2.instance.getChunkFromChunkPos(var3 + var7.dx(), var4 + var7.dy());
         if (var8 != null && var8.isValidLevel(var5)) {
            HLChunkLevel var9 = var8.getLevelData(var5).getHighLevelData();
            if (!var2.contains(var9)) {
               var2.add(var9);
            }
         }
      }

   }

   void addLevelTransitionChunkLevelData(HLLevelTransition var1, ArrayList<HLChunkLevel> var2) {
      this.addLevelTransitionChunkLevelData(var1, var2, var1.getBottomFloorX(), var1.getBottomFloorY(), var1.getBottomFloorZ());
      this.addLevelTransitionChunkLevelData(var1, var2, var1.getTopFloorX(), var1.getTopFloorY(), var1.getTopFloorZ());
   }

   void addLevelTransitionChunkLevelData(HLLevelTransition var1, ArrayList<HLChunkLevel> var2, int var3, int var4, int var5) {
      Chunk var6 = PolygonalMap2.instance.getChunkFromSquarePos(var3, var4);
      if (var6 != null) {
         HLChunkLevel var7 = var6.getLevelData(var5).getHighLevelData();
         this.addChunkLevelAndAdjacentToList(var7, var2);
      }
   }

   static final class AdjustVisibilityGraphData {
      AdjustStartEndNodeData adjustStartEndNodeData = new AdjustStartEndNodeData();
      boolean bAdjusted;
      VisibilityGraph graph;
      Node vgNode;

      AdjustVisibilityGraphData() {
      }
   }
}
