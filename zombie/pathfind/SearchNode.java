package zombie.pathfind;

import astar.ASearchNode;
import astar.ISearchNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;

final class SearchNode extends ASearchNode {
   VGAStar astar;
   Node vgNode;
   Square square;
   int unloadedX;
   int unloadedY;
   boolean bInUnloadedArea = false;
   SearchNode parent;
   static int nextID = 1;
   Integer ID;
   private static final double SQRT2 = Math.sqrt(2.0);
   static final ArrayDeque<SearchNode> pool = new ArrayDeque();

   SearchNode() {
      this.ID = nextID++;
   }

   SearchNode init(VGAStar var1, Node var2) {
      this.setG(0.0);
      this.astar = var1;
      this.vgNode = var2;
      this.square = null;
      this.unloadedX = this.unloadedY = -1;
      this.bInUnloadedArea = false;
      this.parent = null;
      return this;
   }

   SearchNode init(VGAStar var1, Square var2) {
      this.setG(0.0);
      this.astar = var1;
      this.vgNode = null;
      this.square = var2;
      this.unloadedX = this.unloadedY = -1;
      this.bInUnloadedArea = false;
      this.parent = null;
      return this;
   }

   SearchNode init(VGAStar var1, int var2, int var3) {
      this.setG(0.0);
      this.astar = var1;
      this.vgNode = null;
      this.square = null;
      this.unloadedX = var2;
      this.unloadedY = var3;
      this.bInUnloadedArea = true;
      this.parent = null;
      return this;
   }

   public double h() {
      return this.dist(this.astar.goalNode.searchNode);
   }

   public double c(ISearchNode var1) {
      SearchNode var2 = (SearchNode)var1;
      if (var2.bInUnloadedArea) {
         return this.dist(var2);
      } else {
         double var3 = 0.0;
         boolean var5 = this.astar.mover.isZombie() && this.astar.mover.bCrawling;
         boolean var6 = !this.astar.mover.isZombie() || this.astar.mover.bCrawling;
         boolean var7 = this.astar.mover.isAnimal() && !this.astar.mover.bCanClimbFences;
         if (var7) {
            var6 = true;
         }

         if (var6 && this.square != null && var2.square != null) {
            if (this.square.x == var2.square.x - 1 && this.square.y == var2.square.y) {
               if (var2.square.has(2048)) {
                  var3 = !var5 && var2.square.has(1048576) ? 20.0 : 200.0;
               }
            } else if (this.square.x == var2.square.x + 1 && this.square.y == var2.square.y) {
               if (this.square.has(2048)) {
                  var3 = !var5 && this.square.has(1048576) ? 20.0 : 200.0;
               }
            } else if (this.square.y == var2.square.y - 1 && this.square.x == var2.square.x) {
               if (var2.square.has(4096)) {
                  var3 = !var5 && var2.square.has(2097152) ? 20.0 : 200.0;
               }
            } else if (this.square.y == var2.square.y + 1 && this.square.x == var2.square.x && this.square.has(4096)) {
               var3 = !var5 && this.square.has(2097152) ? 20.0 : 200.0;
            }
         }

         if (var2.square != null && var2.square.has(131072)) {
            var3 = Math.max(var3, 20.0);
         }

         if (this.vgNode != null && var2.vgNode != null) {
            for(int var8 = 0; var8 < this.vgNode.visible.size(); ++var8) {
               Connection var9 = (Connection)this.vgNode.visible.get(var8);
               if (var9.otherNode(this.vgNode) == var2.vgNode) {
                  if ((this.vgNode.square == null || !this.vgNode.square.has(131072)) && var9.has(2)) {
                     var3 = Math.max(var3, 20.0);
                  }
                  break;
               }
            }
         }

         Square var10 = this.square == null ? PolygonalMap2.instance.getSquare(PZMath.fastfloor(this.vgNode.x), PZMath.fastfloor(this.vgNode.y), this.vgNode.z) : this.square;
         Square var12 = var2.square == null ? PolygonalMap2.instance.getSquare(PZMath.fastfloor(var2.vgNode.x), PZMath.fastfloor(var2.vgNode.y), var2.vgNode.z) : var2.square;
         if (var10 != null && var12 != null) {
            if (var10.x == var12.x - 1 && var10.y == var12.y) {
               if (var12.has(32768)) {
                  var3 = Math.max(var3, 20.0);
               }
            } else if (var10.x == var12.x + 1 && var10.y == var12.y) {
               if (var10.has(32768)) {
                  var3 = Math.max(var3, 20.0);
               }
            } else if (var10.y == var12.y - 1 && var10.x == var12.x) {
               if (var12.has(65536)) {
                  var3 = Math.max(var3, 20.0);
               }
            } else if (var10.y == var12.y + 1 && var10.x == var12.x && var10.has(65536)) {
               var3 = Math.max(var3, 20.0);
            }

            if (var5 || var7) {
               if (var10.x == var12.x - 1 && var10.y == var12.y) {
                  if (var12.has(2) && var12.has(8192) && (!this.astar.mover.isAnimal() || !var12.isUnblockedDoorW())) {
                     var3 = Math.max(var3, 20.0);
                  }
               } else if (var10.x == var12.x + 1 && var10.y == var12.y) {
                  if (var10.has(2) && var10.has(8192) && (!this.astar.mover.isAnimal() || !var10.isUnblockedDoorW())) {
                     var3 = Math.max(var3, 20.0);
                  }
               } else if (var10.y == var12.y - 1 && var10.x == var12.x) {
                  if (var12.has(4) && var12.has(16384) && (!this.astar.mover.isAnimal() || !var12.isUnblockedDoorN())) {
                     var3 = Math.max(var3, 20.0);
                  }
               } else if (var10.y == var12.y + 1 && var10.x == var12.x && var10.has(4) && var10.has(16384) && (!this.astar.mover.isAnimal() || !var10.isUnblockedDoorN())) {
                  var3 = Math.max(var3, 20.0);
               }
            }
         }

         boolean var11 = this.vgNode != null && this.vgNode.hasFlag(2);
         boolean var13 = var2.vgNode != null && var2.vgNode.hasFlag(2);
         if (!var11 && var13 && !this.astar.mover.bIgnoreCrawlCost) {
            var3 += 10.0;
         }

         if (var2.square != null) {
            var3 += (double)var2.square.cost;
         }

         return this.dist(var2) + var3;
      }
   }

   public void getSuccessors(ArrayList<ISearchNode> var1) {
      if (this.astar.goalNode.searchNode.bInUnloadedArea && this.isOnEdgeOfLoadedArea()) {
         var1.add(this.astar.goalNode.searchNode);
      }

      ArrayList var2 = var1;
      int var3;
      SearchNode var6;
      Square var10;
      SearchNode var13;
      boolean var15;
      if (this.vgNode != null) {
         this.vgNode.createGraphsIfNeeded();

         for(var3 = 0; var3 < this.vgNode.visible.size(); ++var3) {
            Connection var4 = (Connection)this.vgNode.visible.get(var3);
            Node var5 = var4.otherNode(this.vgNode);
            var6 = this.astar.getSearchNode(var5);
            if ((this.vgNode.square == null || var6.square == null || !this.astar.isKnownBlocked(this.vgNode.square, var6.square)) && (this.astar.mover.bCanCrawl || !var5.hasFlag(2)) && (this.astar.mover.bCanThump || !var4.has(2))) {
               var2.add(var6);
            }
         }

         if (this.vgNode.graphs != null && !this.vgNode.graphs.isEmpty() && this.vgNode.hasFlag(16)) {
            var10 = PolygonalMap2.instance.getSquare(this.square.x - 1, this.square.y, this.square.z);
            if (var10 != null && var10.has(32)) {
               if (!this.astar.mover.isAllowedChunkLevel(var10)) {
                  return;
               }

               if (this.astar.canMoveBetween(this.square, var10, false)) {
                  var13 = this.astar.getSearchNode(var10);
                  if (var2.contains(var13)) {
                     var15 = false;
                  } else {
                     var2.add(var13);
                  }
               }
            }

            var10 = PolygonalMap2.instance.getSquare(this.square.x, this.square.y - 1, this.square.z);
            if (var10 != null && var10.has(256)) {
               if (!this.astar.mover.isAllowedChunkLevel(var10)) {
                  return;
               }

               if (this.astar.canMoveBetween(this.square, var10, false)) {
                  var13 = this.astar.getSearchNode(var10);
                  if (var2.contains(var13)) {
                     var15 = false;
                  } else {
                     var2.add(var13);
                  }
               }
            }

            return;
         }

         if (this.vgNode.graphs != null && !this.vgNode.graphs.isEmpty() && !this.vgNode.hasFlag(8)) {
            return;
         }
      }

      if (this.square != null) {
         for(var3 = -1; var3 <= 1; ++var3) {
            for(int var8 = -1; var8 <= 1; ++var8) {
               if (var8 != 0 || var3 != 0) {
                  Square var12 = PolygonalMap2.instance.getSquareRawZ(this.square.x + var8, this.square.y + var3, this.square.z);
                  if (var12 != null && this.astar.mover.isAllowedChunkLevel(var12) && (!this.astar.isSquareInCluster(var12) || var12.has(504)) && this.astar.canMoveBetween(this.square, var12, false)) {
                     var6 = this.astar.getSearchNode(var12);
                     if (var2.contains(var6)) {
                        boolean var7 = false;
                     } else {
                        var2.add(var6);
                     }
                  }
               }
            }
         }

         if (this.square.has(288)) {
            IsoDirections var9 = this.square.has(256) ? IsoDirections.N : IsoDirections.W;
            Square var11 = this.square.getAdjacentSquare(var9.Rot180());
            if (var11 != null && PolygonalMap2.instance.getExistingNodeForSquare(var11) != null && PolygonalMap2.instance.getExistingNodeForSquare(var11).hasFlag(16) && this.astar.mover.isAllowedChunkLevel(var11) && this.astar.canMoveBetween(this.square, var11, false)) {
               SearchNode var14 = this.astar.getSearchNode(var11);
               if (var2.contains(var14)) {
                  boolean var16 = false;
               } else {
                  var2.add(var14);
               }
            }
         }

         if (this.square.z > this.astar.mover.minLevel) {
            var10 = PolygonalMap2.instance.getSquare(this.square.x, this.square.y + 1, this.square.z - 1);
            if (var10 != null && var10.hasTransitionToLevelAbove(IsoDirections.N) && !this.astar.isSquareInCluster(var10) && this.astar.mover.isAllowedLevelTransition(IsoDirections.N, this.square, true)) {
               var13 = this.astar.getSearchNode(var10);
               if (var2.contains(var13)) {
                  var15 = false;
               } else {
                  var2.add(var13);
               }
            }

            var10 = PolygonalMap2.instance.getSquare(this.square.x, this.square.y - 1, this.square.z - 1);
            if (var10 != null && var10.hasTransitionToLevelAbove(IsoDirections.S) && !this.astar.isSquareInCluster(var10) && this.astar.mover.isAllowedLevelTransition(IsoDirections.S, this.square, true)) {
               var13 = this.astar.getSearchNode(var10);
               if (var2.contains(var13)) {
                  var15 = false;
               } else {
                  var2.add(var13);
               }
            }

            var10 = PolygonalMap2.instance.getSquare(this.square.x + 1, this.square.y, this.square.z - 1);
            if (var10 != null && var10.hasTransitionToLevelAbove(IsoDirections.W) && !this.astar.isSquareInCluster(var10) && this.astar.mover.isAllowedLevelTransition(IsoDirections.W, this.square, true)) {
               var13 = this.astar.getSearchNode(var10);
               if (var2.contains(var13)) {
                  var15 = false;
               } else {
                  var2.add(var13);
               }
            }

            var10 = PolygonalMap2.instance.getSquare(this.square.x - 1, this.square.y, this.square.z - 1);
            if (var10 != null && var10.hasTransitionToLevelAbove(IsoDirections.E) && !this.astar.isSquareInCluster(var10) && this.astar.mover.isAllowedLevelTransition(IsoDirections.E, this.square, true)) {
               var13 = this.astar.getSearchNode(var10);
               if (var2.contains(var13)) {
                  var15 = false;
               } else {
                  var2.add(var13);
               }
            }
         }

         if (this.square.z < this.astar.mover.maxLevel) {
            if (this.square.hasTransitionToLevelAbove(IsoDirections.N)) {
               var10 = PolygonalMap2.instance.getSquareRawZ(this.square.x, this.square.y - 1, this.square.z + 1);
               if (var10 != null && !this.astar.isSquareInCluster(var10) && this.astar.mover.isAllowedLevelTransition(IsoDirections.N, var10, true)) {
                  var13 = this.astar.getSearchNode(var10);
                  if (var2.contains(var13)) {
                     var15 = false;
                  } else {
                     var2.add(var13);
                  }
               }
            }

            if (this.square.hasTransitionToLevelAbove(IsoDirections.S)) {
               var10 = PolygonalMap2.instance.getSquareRawZ(this.square.x, this.square.y + 1, this.square.z + 1);
               if (var10 != null && !this.astar.isSquareInCluster(var10) && this.astar.mover.isAllowedLevelTransition(IsoDirections.S, var10, true)) {
                  var13 = this.astar.getSearchNode(var10);
                  if (var2.contains(var13)) {
                     var15 = false;
                  } else {
                     var2.add(var13);
                  }
               }
            }

            if (this.square.hasTransitionToLevelAbove(IsoDirections.W)) {
               var10 = PolygonalMap2.instance.getSquareRawZ(this.square.x - 1, this.square.y, this.square.z + 1);
               if (var10 != null && !this.astar.isSquareInCluster(var10) && this.astar.mover.isAllowedLevelTransition(IsoDirections.W, var10, true)) {
                  var13 = this.astar.getSearchNode(var10);
                  if (var2.contains(var13)) {
                     var15 = false;
                  } else {
                     var2.add(var13);
                  }
               }
            }

            if (this.square.hasTransitionToLevelAbove(IsoDirections.E)) {
               var10 = PolygonalMap2.instance.getSquareRawZ(this.square.x + 1, this.square.y, this.square.z + 1);
               if (var10 != null && !this.astar.isSquareInCluster(var10) && this.astar.mover.isAllowedLevelTransition(IsoDirections.E, var10, true)) {
                  var13 = this.astar.getSearchNode(var10);
                  if (var2.contains(var13)) {
                     var15 = false;
                  } else {
                     var2.add(var13);
                  }
               }
            }
         }
      }

   }

   public ISearchNode getParent() {
      return this.parent;
   }

   public void setParent(ISearchNode var1) {
      this.parent = (SearchNode)var1;
   }

   public Integer keyCode() {
      return this.ID;
   }

   public float getX() {
      if (this.square != null) {
         return (float)this.square.x + 0.5F;
      } else {
         return this.vgNode != null ? this.vgNode.x : (float)this.unloadedX;
      }
   }

   public float getY() {
      if (this.square != null) {
         return (float)this.square.y + 0.5F;
      } else {
         return this.vgNode != null ? this.vgNode.y : (float)this.unloadedY;
      }
   }

   public float getZ() {
      if (this.square != null) {
         return (float)this.square.z;
      } else {
         return this.vgNode != null ? (float)this.vgNode.z : 32.0F;
      }
   }

   boolean isOnEdgeOfLoadedArea() {
      int var1 = PZMath.fastfloor(this.getX());
      int var2 = PZMath.fastfloor(this.getY());
      boolean var3 = false;
      if (PZMath.coordmodulo(var1, 8) == 0 && PolygonalMap2.instance.getChunkFromSquarePos(var1 - 1, var2) == null) {
         var3 = true;
      }

      if (PZMath.coordmodulo(var1, 8) == 7 && PolygonalMap2.instance.getChunkFromSquarePos(var1 + 1, var2) == null) {
         var3 = true;
      }

      if (PZMath.coordmodulo(var2, 8) == 0 && PolygonalMap2.instance.getChunkFromSquarePos(var1, var2 - 1) == null) {
         var3 = true;
      }

      if (PZMath.coordmodulo(var2, 8) == 7 && PolygonalMap2.instance.getChunkFromSquarePos(var1, var2 + 1) == null) {
         var3 = true;
      }

      return var3;
   }

   public double dist(SearchNode var1) {
      if (this.square != null && var1.square != null && Math.abs(this.square.x - var1.square.x) <= 1 && Math.abs(this.square.y - var1.square.y) <= 1) {
         return this.square.x != var1.square.x && this.square.y != var1.square.y ? SQRT2 : 1.0;
      } else {
         float var2 = this.getX();
         float var3 = this.getY();
         float var4 = this.getZ();
         float var5 = var1.getX();
         float var6 = var1.getY();
         float var7 = var1.getZ();
         return Math.sqrt(Math.pow((double)(var2 - var5), 2.0) + Math.pow((double)(var3 - var6), 2.0) + Math.pow((double)((var4 - var7) * 2.5F), 2.0));
      }
   }

   float getApparentZ() {
      if (this.square == null) {
         return (float)this.vgNode.z;
      } else if (!this.square.has(8) && !this.square.has(64)) {
         if (!this.square.has(16) && !this.square.has(128)) {
            return !this.square.has(32) && !this.square.has(256) ? (float)this.square.z : (float)this.square.z + 0.25F;
         } else {
            return (float)this.square.z + 0.5F;
         }
      } else {
         return (float)this.square.z + 0.75F;
      }
   }

   static SearchNode alloc() {
      return pool.isEmpty() ? new SearchNode() : (SearchNode)pool.pop();
   }

   void release() {
      assert !pool.contains(this);

      pool.push(this);
   }
}
