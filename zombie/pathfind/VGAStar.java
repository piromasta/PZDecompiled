package zombie.pathfind;

import astar.AStar;
import astar.ISearchNode;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import java.util.ArrayList;
import zombie.ai.KnownBlockedEdges;
import zombie.iso.IsoDirections;

final class VGAStar extends AStar {
   ArrayList<VisibilityGraph> graphs;
   final ArrayList<SearchNode> searchNodes = new ArrayList();
   final TIntObjectHashMap<SearchNode> nodeMap = new TIntObjectHashMap();
   final GoalNode goalNode = new GoalNode();
   final TIntObjectHashMap<SearchNode> squareToNode = new TIntObjectHashMap();
   PMMover mover = new PMMover();
   final TIntObjectHashMap<KnownBlockedEdges> knownBlockedEdges = new TIntObjectHashMap();
   final InitProc initProc = new InitProc();

   VGAStar() {
   }

   VGAStar init(ArrayList<VisibilityGraph> var1, TIntObjectHashMap<Node> var2) {
      this.setMaxSteps(5000);
      this.graphs = var1;
      this.searchNodes.clear();
      this.nodeMap.clear();
      this.squareToNode.clear();
      var2.forEachEntry(this.initProc);
      return this;
   }

   VisibilityGraph getVisGraphForSquare(Square var1) {
      Chunk var2 = PolygonalMap2.instance.getChunkFromSquarePos(var1.x, var1.y);
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

   boolean isSquareInCluster(Square var1) {
      return this.getVisGraphForSquare(var1) != null;
   }

   SearchNode getSearchNode(Node var1) {
      if (var1.square != null) {
         return this.getSearchNode(var1.square);
      } else {
         SearchNode var2 = (SearchNode)this.nodeMap.get(var1.ID);
         if (var2 == null) {
            var2 = SearchNode.alloc().init(this, var1);
            this.searchNodes.add(var2);
            this.nodeMap.put(var1.ID, var2);
         }

         return var2;
      }
   }

   SearchNode getSearchNode(Square var1) {
      SearchNode var2 = (SearchNode)this.squareToNode.get(var1.ID);
      if (var2 == null) {
         var2 = SearchNode.alloc().init(this, var1);
         this.searchNodes.add(var2);
         this.squareToNode.put(var1.ID, var2);
      }

      return var2;
   }

   SearchNode getSearchNode(int var1, int var2) {
      SearchNode var3 = SearchNode.alloc().init(this, var1, var2);
      this.searchNodes.add(var3);
      return var3;
   }

   ArrayList<ISearchNode> shortestPath(PathFindRequest var1, SearchNode var2, SearchNode var3) {
      this.mover.set(var1);
      this.goalNode.init(var3);
      return this.shortestPath(var2, this.goalNode);
   }

   boolean canMoveBetween(Square var1, Square var2, boolean var3) {
      return !this.canNotMoveBetween(var1, var2, var3);
   }

   boolean canNotMoveBetween(Square var1, Square var2, boolean var3) {
      assert Math.abs(var1.x - var2.x) <= 1;

      assert Math.abs(var1.y - var2.y) <= 1;

      assert var1.z == var2.z;

      assert var1 != var2;

      boolean var4 = var2.x < var1.x;
      boolean var5 = var2.x > var1.x;
      boolean var6 = var2.y < var1.y;
      boolean var7 = var2.y > var1.y;
      if (var2.isNonThumpableSolid() || !this.mover.bCanThump && var2.isReallySolid()) {
         return true;
      } else if (var2.y < var1.y && var1.has(64)) {
         return true;
      } else if (var2.x < var1.x && var1.has(8)) {
         return true;
      } else if (var2.y > var1.y && var2.x == var1.x && var2.has(64)) {
         return true;
      } else if (var2.x > var1.x && var2.y == var1.y && var2.has(8)) {
         return true;
      } else if (var2.x != var1.x && var2.has(448)) {
         return true;
      } else if (var2.y != var1.y && var2.has(56)) {
         return true;
      } else if (var2.x != var1.x && var1.has(448)) {
         return true;
      } else if (var2.y != var1.y && var1.has(56)) {
         return true;
      } else {
         if (var2.z == var1.z) {
            label690: {
               if (var2.x != var1.x || var2.y != var1.y - 1 || !var1.isSlopedSurfaceEdgeBlocked(IsoDirections.N) && !var2.isSlopedSurfaceEdgeBlocked(IsoDirections.S)) {
                  if (var2.x == var1.x && var2.y == var1.y + 1 && (var1.isSlopedSurfaceEdgeBlocked(IsoDirections.S) || var2.isSlopedSurfaceEdgeBlocked(IsoDirections.N))) {
                     return true;
                  }

                  if (var2.x == var1.x - 1 && var2.y == var1.y && (var1.isSlopedSurfaceEdgeBlocked(IsoDirections.W) || var2.isSlopedSurfaceEdgeBlocked(IsoDirections.E))) {
                     return true;
                  }

                  if (var2.x != var1.x + 1 || var2.y != var1.y || !var1.isSlopedSurfaceEdgeBlocked(IsoDirections.E) && !var2.isSlopedSurfaceEdgeBlocked(IsoDirections.W)) {
                     break label690;
                  }

                  return true;
               }

               return true;
            }
         }

         if (!var2.has(512) && !var2.has(504)) {
            return true;
         } else if (this.isKnownBlocked(var1, var2)) {
            return true;
         } else {
            boolean var8;
            boolean var9;
            boolean var10;
            boolean var11;
            boolean var12;
            boolean var13;
            if (this.mover.isAnimal()) {
               var8 = var6 && var1.isUnblockedDoorN();
               var9 = var4 && var1.isUnblockedDoorW();
               var10 = var6 && (var1.isCollideN() || var1.isThumpN()) && !var8;
               var11 = var4 && (var1.isCollideW() || var1.isThumpW()) && !var9;
               if (var6 && var1.isCanPathN() && (var1.x != var2.x || var3)) {
                  return true;
               }

               if (var4 && var1.isCanPathW() && (var1.y != var2.y || var3)) {
                  return true;
               }

               if ((var10 || var11) && !this.canAnimalBreakObstacle(var1, var2, var11, var10, false, false)) {
                  return true;
               }

               var12 = var7 && var2.isUnblockedDoorN();
               var13 = var5 && var2.isUnblockedDoorW();
               boolean var14 = var7 && (var2.isCollideN() || var2.isThumpN()) && !var12;
               boolean var15 = var5 && (var2.isCollideW() || var2.isThumpW()) && !var13;
               if (var7 && var2.isCanPathN() && (var1.x != var2.x || var3)) {
                  return true;
               }

               if (var5 && var2.isCanPathW() && (var1.y != var2.y || var3)) {
                  return true;
               }

               if ((var14 || var15) && !this.canAnimalBreakObstacle(var1, var2, false, false, var15, var14)) {
                  return true;
               }
            } else {
               var8 = var1.isCanPathN() && (this.mover.bCanThump || !var1.isThumpN());
               var9 = var1.isCanPathW() && (this.mover.bCanThump || !var1.isThumpW());
               var10 = var6 && var1.isCollideN() && (var1.x != var2.x || var3 || !var8);
               var11 = var4 && var1.isCollideW() && (var1.y != var2.y || var3 || !var9);
               var8 = var2.isCanPathN() && (this.mover.bCanThump || !var2.isThumpN());
               var9 = var2.isCanPathW() && (this.mover.bCanThump || !var2.isThumpW());
               var12 = var7 && var2.has(131076) && (var1.x != var2.x || var3 || !var8);
               var13 = var5 && var2.has(131074) && (var1.y != var2.y || var3 || !var9);
               if (var10 || var11 || var12 || var13) {
                  return true;
               }
            }

            var8 = var2.x != var1.x && var2.y != var1.y;
            if (var8) {
               Square var16 = PolygonalMap2.instance.getSquareRawZ(var1.x, var2.y, var1.z);
               Square var17 = PolygonalMap2.instance.getSquareRawZ(var2.x, var1.y, var1.z);

               assert var16 != var1 && var16 != var2;

               assert var17 != var1 && var17 != var2;

               if (var2.x == var1.x + 1 && var2.y == var1.y + 1 && var16 != null && var17 != null) {
                  if (var16.has(4096) && var17.has(2048)) {
                     return true;
                  }

                  if (var16.isThumpN() && var17.isThumpW()) {
                     return true;
                  }
               }

               if (var2.x == var1.x - 1 && var2.y == var1.y - 1 && var16 != null && var17 != null) {
                  if (var16.has(2048) && var17.has(4096)) {
                     return true;
                  }

                  if (var16.isThumpW() && var17.isThumpN()) {
                     return true;
                  }
               }

               if (var16 == null || this.canNotMoveBetween(var1, var16, true)) {
                  return true;
               }

               if (var17 == null || this.canNotMoveBetween(var1, var17, true)) {
                  return true;
               }

               if (var16 == null || this.canNotMoveBetween(var2, var16, true)) {
                  return true;
               }

               if (var17 == null || this.canNotMoveBetween(var2, var17, true)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   boolean isKnownBlocked(Square var1, Square var2) {
      if (var1.z != var2.z) {
         return false;
      } else {
         KnownBlockedEdges var3 = (KnownBlockedEdges)this.knownBlockedEdges.get(var1.ID);
         KnownBlockedEdges var4 = (KnownBlockedEdges)this.knownBlockedEdges.get(var2.ID);
         if (var3 != null && var3.isBlocked(var2.x, var2.y)) {
            return true;
         } else {
            return var4 != null && var4.isBlocked(var1.x, var1.y);
         }
      }
   }

   boolean canAnimalBreakObstacle(Square var1, Square var2, boolean var3, boolean var4, boolean var5, boolean var6) {
      if (!this.mover.bCanThump) {
         return false;
      } else if (var3) {
         return var1.has(2) && var1.has(8192);
      } else if (var4) {
         return var1.has(4) && var1.has(16384);
      } else if (var5) {
         return var2.has(2) && var2.has(8192);
      } else if (!var6) {
         return false;
      } else {
         return var2.has(4) && var2.has(16384);
      }
   }

   final class InitProc implements TIntObjectProcedure<Node> {
      InitProc() {
      }

      public boolean execute(int var1, Node var2) {
         SearchNode var3 = SearchNode.alloc().init(VGAStar.this, (Node)var2);
         var3.square = var2.square;
         VGAStar.this.squareToNode.put(var1, var3);
         VGAStar.this.nodeMap.put(var2.ID, var3);
         VGAStar.this.searchNodes.add(var3);
         return true;
      }
   }
}
