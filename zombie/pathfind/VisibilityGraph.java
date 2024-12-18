package zombie.pathfind;

import gnu.trove.list.array.TIntArrayList;
import java.awt.geom.Line2D;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.joml.Vector2f;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;

public final class VisibilityGraph {
   boolean created;
   public VehicleCluster cluster;
   final ArrayList<Node> nodes = new ArrayList();
   public final ArrayList<Edge> edges = new ArrayList();
   final ArrayList<Obstacle> obstacles = new ArrayList();
   final ArrayList<Node> intersectNodes = new ArrayList();
   public final ArrayList<Node> perimeterNodes = new ArrayList();
   public final ArrayList<Edge> perimeterEdges = new ArrayList();
   final ArrayList<Node> obstacleTraceNodes = new ArrayList();
   final ArrayList<Chunk> overlappedChunks = new ArrayList();
   final TIntArrayList splitXY = new TIntArrayList();
   static final CompareIntersection comparator = new CompareIntersection();
   private static final ClusterOutlineGrid clusterOutlineGrid = new ClusterOutlineGrid();
   private static final ArrayDeque<VisibilityGraph> pool = new ArrayDeque();

   public VisibilityGraph() {
   }

   VisibilityGraph init(VehicleCluster var1) {
      this.created = false;
      this.cluster = var1;
      this.edges.clear();
      this.nodes.clear();
      this.obstacles.clear();
      this.intersectNodes.clear();
      this.perimeterEdges.clear();
      this.perimeterNodes.clear();
      return this;
   }

   public boolean isCreated() {
      return this.created;
   }

   void addEdgesForVehicle(Vehicle var1) {
      VehiclePoly var2 = var1.polyPlusRadius;
      int var3 = PZMath.fastfloor(var2.z);
      Node var4 = Node.alloc().init(var2.x1, var2.y1, var3);
      Node var5 = Node.alloc().init(var2.x2, var2.y2, var3);
      Node var6 = Node.alloc().init(var2.x3, var2.y3, var3);
      Node var7 = Node.alloc().init(var2.x4, var2.y4, var3);
      Obstacle var8 = Obstacle.alloc().init(var1);
      this.obstacles.add(var8);
      Edge var9 = Edge.alloc().init(var4, var5, var8, var8.outer);
      Edge var10 = Edge.alloc().init(var5, var6, var8, var8.outer);
      Edge var11 = Edge.alloc().init(var6, var7, var8, var8.outer);
      Edge var12 = Edge.alloc().init(var7, var4, var8, var8.outer);
      var8.outer.add(var9);
      var8.outer.add(var10);
      var8.outer.add(var11);
      var8.outer.add(var12);
      var8.calcBounds();
      this.nodes.add(var4);
      this.nodes.add(var5);
      this.nodes.add(var6);
      this.nodes.add(var7);
      this.edges.add(var9);
      this.edges.add(var10);
      this.edges.add(var11);
      this.edges.add(var12);
      if (!(var1.upVectorDot < 0.95F)) {
         var8.nodeCrawlFront = Node.alloc().init((var2.x1 + var2.x2) / 2.0F, (var2.y1 + var2.y2) / 2.0F, var3);
         var8.nodeCrawlRear = Node.alloc().init((var2.x3 + var2.x4) / 2.0F, (var2.y3 + var2.y4) / 2.0F, var3);
         Node var10000 = var8.nodeCrawlFront;
         var10000.flags |= 1;
         var10000 = var8.nodeCrawlRear;
         var10000.flags |= 1;
         this.nodes.add(var8.nodeCrawlFront);
         this.nodes.add(var8.nodeCrawlRear);
         Edge var13 = var9.split(var8.nodeCrawlFront);
         Edge var14 = var11.split(var8.nodeCrawlRear);
         this.edges.add(var13);
         this.edges.add(var14);
         BaseVehicle.Vector2fObjectPool var15 = (BaseVehicle.Vector2fObjectPool)BaseVehicle.TL_vector2f_pool.get();
         Vector2f var16 = (Vector2f)var15.alloc();
         Vector2f var17 = (Vector2f)var15.alloc();
         var8.crawlNodes.clear();

         for(int var18 = 0; var18 < var1.crawlOffsets.size(); ++var18) {
            float var19 = var1.crawlOffsets.get(var18);
            var16.set(var6.x, var6.y);
            var17.set(var5.x, var5.y);
            var17.sub(var16).mul(var19).add(var16);
            Node var20 = Node.alloc().init(var17.x, var17.y, var3);
            var20.flags |= 1;
            var16.set(var7.x, var7.y);
            var17.set(var4.x, var4.y);
            var17.sub(var16).mul(var19).add(var16);
            Node var21 = Node.alloc().init(var17.x, var17.y, var3);
            var21.flags |= 1;
            Node var22 = Node.alloc().init((var20.x + var21.x) / 2.0F, (var20.y + var21.y) / 2.0F, var3);
            var22.flags |= 3;
            var8.crawlNodes.add(var20);
            var8.crawlNodes.add(var22);
            var8.crawlNodes.add(var21);
            this.nodes.add(var20);
            this.nodes.add(var22);
            this.nodes.add(var21);
            Edge var23 = var10.split(var20);
            var12 = var12.split(var21);
            this.edges.add(var23);
            this.edges.add(var12);
         }

         var15.release(var16);
         var15.release(var17);
      }
   }

   boolean isVisible(Node var1, Node var2) {
      if (var1.sharesEdge(var2)) {
         return !var1.onSameShapeButDoesNotShareAnEdge(var2);
      } else if (var1.sharesShape(var2)) {
         return false;
      } else {
         int var3;
         Edge var4;
         for(var3 = 0; var3 < this.edges.size(); ++var3) {
            var4 = (Edge)this.edges.get(var3);
            if (this.intersects(var1, var2, var4)) {
               return false;
            }
         }

         for(var3 = 0; var3 < this.perimeterEdges.size(); ++var3) {
            var4 = (Edge)this.perimeterEdges.get(var3);
            if (this.intersects(var1, var2, var4)) {
               return false;
            }
         }

         return true;
      }
   }

   boolean intersects(Node var1, Node var2, Edge var3) {
      return !var3.hasNode(var1) && !var3.hasNode(var2) ? Line2D.linesIntersect((double)var1.x, (double)var1.y, (double)var2.x, (double)var2.y, (double)var3.node1.x, (double)var3.node1.y, (double)var3.node2.x, (double)var3.node2.y) : false;
   }

   public Intersection getIntersection(Edge var1, Edge var2) {
      float var3 = var1.node1.x;
      float var4 = var1.node1.y;
      float var5 = var1.node2.x;
      float var6 = var1.node2.y;
      float var7 = var2.node1.x;
      float var8 = var2.node1.y;
      float var9 = var2.node2.x;
      float var10 = var2.node2.y;
      double var11 = (double)((var10 - var8) * (var5 - var3) - (var9 - var7) * (var6 - var4));
      if (var11 == 0.0) {
         return null;
      } else {
         double var13 = (double)((var9 - var7) * (var4 - var8) - (var10 - var8) * (var3 - var7)) / var11;
         double var15 = (double)((var5 - var3) * (var4 - var8) - (var6 - var4) * (var3 - var7)) / var11;
         if (var13 >= 0.0 && var13 <= 1.0 && var15 >= 0.0 && var15 <= 1.0) {
            float var17 = (float)((double)var3 + var13 * (double)(var5 - var3));
            float var18 = (float)((double)var4 + var13 * (double)(var6 - var4));
            return new Intersection(var1, var2, (float)var13, (float)var15, var17, var18);
         } else {
            return null;
         }
      }
   }

   /** @deprecated */
   @Deprecated
   void addWorldObstacles() {
      VehicleRect var1 = this.cluster.bounds();
      --var1.x;
      --var1.y;
      var1.w += 3;
      var1.h += 3;
      ObjectOutline[][] var2 = new ObjectOutline[var1.w][var1.h];
      int var3 = this.cluster.z;

      int var4;
      int var5;
      for(var4 = var1.top(); var4 < var1.bottom() - 1; ++var4) {
         for(var5 = var1.left(); var5 < var1.right() - 1; ++var5) {
            Square var6 = PolygonalMap2.instance.getSquare(var5, var4, var3);
            if (var6 != null && this.contains(var6, 1)) {
               if (var6.has(504) || var6.isReallySolid()) {
                  ObjectOutline.setSolid(var5 - var1.left(), var4 - var1.top(), var3, var2);
               }

               if (var6.has(2)) {
                  ObjectOutline.setWest(var5 - var1.left(), var4 - var1.top(), var3, var2);
               }

               if (var6.has(4)) {
                  ObjectOutline.setNorth(var5 - var1.left(), var4 - var1.top(), var3, var2);
               }

               if (var6.has(262144)) {
                  ObjectOutline.setWest(var5 - var1.left() + 1, var4 - var1.top(), var3, var2);
               }

               if (var6.has(524288)) {
                  ObjectOutline.setNorth(var5 - var1.left(), var4 - var1.top() + 1, var3, var2);
               }
            }
         }
      }

      for(var4 = 0; var4 < var1.h; ++var4) {
         for(var5 = 0; var5 < var1.w; ++var5) {
            ObjectOutline var12 = ObjectOutline.get(var5, var4, var3, var2);
            if (var12 != null && var12.nw && var12.nw_w && var12.nw_n) {
               var12.trace(var2, this.obstacleTraceNodes);
               if (!var12.nodes.isEmpty()) {
                  Obstacle var7 = Obstacle.alloc().init((IsoGridSquare)null);

                  for(int var8 = 0; var8 < var12.nodes.size() - 1; ++var8) {
                     Node var9 = (Node)var12.nodes.get(var8);
                     Node var10 = (Node)var12.nodes.get(var8 + 1);
                     var9.x += (float)var1.left();
                     var9.y += (float)var1.top();
                     if (!this.contains(var9.x, var9.y, var9.z)) {
                        var9.ignore = true;
                     }

                     Edge var11 = Edge.alloc().init(var9, var10, var7, var7.outer);
                     var7.outer.add(var11);
                     this.nodes.add(var9);
                  }

                  var7.calcBounds();
                  this.obstacles.add(var7);
                  this.edges.addAll(var7.outer);
               }
            }
         }
      }

      for(var4 = 0; var4 < var1.h; ++var4) {
         for(var5 = 0; var5 < var1.w; ++var5) {
            if (var2[var5][var4] != null) {
               var2[var5][var4].release();
            }
         }
      }

      var1.release();
   }

   void addWorldObstaclesClipper() {
      VehicleRect var1 = this.cluster.bounds();
      --var1.x;
      --var1.y;
      var1.w += 2;
      var1.h += 2;
      if (PolygonalMap2.instance.clipperThread == null) {
         PolygonalMap2.instance.clipperThread = new Clipper();
      }

      Clipper var2 = PolygonalMap2.instance.clipperThread;
      var2.clear();
      int var3 = this.cluster.z;

      int var5;
      for(int var4 = var1.top(); var4 < var1.bottom(); ++var4) {
         for(var5 = var1.left(); var5 < var1.right(); ++var5) {
            Square var6 = PolygonalMap2.instance.getSquare(var5, var4, var3);
            if (var6 != null && this.contains(var6, 1)) {
               if (var6.has(504) || var6.isReallySolid()) {
                  var2.addAABB((float)var5 - 0.3F, (float)var4 - 0.3F, (float)(var5 + 1) + 0.3F, (float)(var4 + 1) + 0.3F);
               }

               boolean var7 = var6.has(2);
               var7 |= var6.isSlopedSurfaceEdgeBlocked(IsoDirections.W);
               Square var8 = var6.getAdjacentSquare(IsoDirections.W);
               if (var8 != null && var8.isSlopedSurfaceEdgeBlocked(IsoDirections.E)) {
                  var7 = true;
               }

               if (var7) {
                  var2.addAABB((float)var5 - 0.3F, (float)var4 - 0.3F, (float)var5 + 0.3F, (float)(var4 + 1) + 0.3F);
               }

               boolean var9 = var6.has(4);
               var9 |= var6.isSlopedSurfaceEdgeBlocked(IsoDirections.N);
               Square var10 = var6.getAdjacentSquare(IsoDirections.N);
               if (var10 != null && var10.isSlopedSurfaceEdgeBlocked(IsoDirections.S)) {
                  var9 = true;
               }

               if (var9) {
                  var2.addAABB((float)var5 - 0.3F, (float)var4 - 0.3F, (float)(var5 + 1) + 0.3F, (float)var4 + 0.3F);
               }
            }
         }
      }

      var1.release();
      ByteBuffer var11 = PolygonalMap2.instance.xyBufferThread;
      var5 = var2.generatePolygons();

      for(int var12 = 0; var12 < var5; ++var12) {
         var11.clear();
         var2.getPolygon(var12, var11);
         Obstacle var13 = Obstacle.alloc().init((IsoGridSquare)null);
         this.getEdgesFromBuffer(var11, var13, true, var3);
         short var14 = var11.getShort();

         int var15;
         for(var15 = 0; var15 < var14; ++var15) {
            this.getEdgesFromBuffer(var11, var13, false, var3);
         }

         var13.calcBounds();
         this.obstacles.add(var13);
         this.edges.addAll(var13.outer);

         for(var15 = 0; var15 < var13.inner.size(); ++var15) {
            this.edges.addAll((Collection)var13.inner.get(var15));
         }
      }

   }

   void getEdgesFromBuffer(ByteBuffer var1, Obstacle var2, boolean var3, int var4) {
      short var5 = var1.getShort();
      if (var5 < 3) {
         var1.position(var1.position() + var5 * 4 * 2);
      } else {
         EdgeRing var6 = var2.outer;
         if (!var3) {
            var6 = EdgeRing.alloc();
            var6.clear();
            var2.inner.add(var6);
         }

         int var7 = this.nodes.size();

         int var8;
         for(var8 = var5 - 1; var8 >= 0; --var8) {
            float var9 = var1.getFloat();
            float var10 = var1.getFloat();
            Node var11 = Node.alloc().init(var9, var10, var4);
            this.nodes.add(var11);
         }

         Node var13;
         for(var8 = var7; var8 < this.nodes.size() - 1; ++var8) {
            var13 = (Node)this.nodes.get(var8);
            Node var14 = (Node)this.nodes.get(var8 + 1);
            if (!this.contains(var13.x, var13.y, var13.z)) {
               var13.ignore = true;
            }

            Edge var16 = Edge.alloc().init(var13, var14, var2, var6);
            var6.add(var16);
         }

         Node var12 = (Node)this.nodes.get(this.nodes.size() - 1);
         var13 = (Node)this.nodes.get(var7);
         Edge var15 = Edge.alloc().init(var12, var13, var2, var6);
         var6.add(var15);
      }
   }

   void trySplit(Edge var1, VehicleRect var2, TIntArrayList var3) {
      float var4;
      float var5;
      float var6;
      if (Math.abs(var1.node1.x - var1.node2.x) > Math.abs(var1.node1.y - var1.node2.y)) {
         var4 = Math.min(var1.node1.x, var1.node2.x);
         var5 = Math.max(var1.node1.x, var1.node2.x);
         var6 = var1.node1.y;
         if ((float)var2.left() > var4 && (float)var2.left() < var5 && (float)var2.top() < var6 && (float)var2.bottom() > var6 && !var3.contains(var2.left()) && !this.contains((float)var2.left() - 0.5F, var6, this.cluster.z)) {
            var3.add(var2.left());
         }

         if ((float)var2.right() > var4 && (float)var2.right() < var5 && (float)var2.top() < var6 && (float)var2.bottom() > var6 && !var3.contains(var2.right()) && !this.contains((float)var2.right() + 0.5F, var6, this.cluster.z)) {
            var3.add(var2.right());
         }
      } else {
         var4 = Math.min(var1.node1.y, var1.node2.y);
         var5 = Math.max(var1.node1.y, var1.node2.y);
         var6 = var1.node1.x;
         if ((float)var2.top() > var4 && (float)var2.top() < var5 && (float)var2.left() < var6 && (float)var2.right() > var6 && !var3.contains(var2.top()) && !this.contains(var6, (float)var2.top() - 0.5F, this.cluster.z)) {
            var3.add(var2.top());
         }

         if ((float)var2.bottom() > var4 && (float)var2.bottom() < var5 && (float)var2.left() < var6 && (float)var2.right() > var6 && !var3.contains(var2.bottom()) && !this.contains(var6, (float)var2.bottom() + 0.5F, this.cluster.z)) {
            var3.add(var2.bottom());
         }
      }

   }

   void splitWorldObstacleEdges(EdgeRing var1) {
      for(int var2 = var1.size() - 1; var2 >= 0; --var2) {
         Edge var3 = (Edge)var1.get(var2);
         this.splitXY.clear();

         int var4;
         for(var4 = 0; var4 < this.cluster.rects.size(); ++var4) {
            VehicleRect var5 = (VehicleRect)this.cluster.rects.get(var4);
            this.trySplit(var3, var5, this.splitXY);
         }

         if (!this.splitXY.isEmpty()) {
            this.splitXY.sort();
            Edge var6;
            Node var7;
            if (Math.abs(var3.node1.x - var3.node2.x) > Math.abs(var3.node1.y - var3.node2.y)) {
               if (var3.node1.x < var3.node2.x) {
                  for(var4 = this.splitXY.size() - 1; var4 >= 0; --var4) {
                     var7 = Node.alloc().init((float)this.splitXY.get(var4), var3.node1.y, this.cluster.z);
                     var6 = var3.split(var7);
                     this.nodes.add(var7);
                     this.edges.add(var6);
                  }
               } else {
                  for(var4 = 0; var4 < this.splitXY.size(); ++var4) {
                     var7 = Node.alloc().init((float)this.splitXY.get(var4), var3.node1.y, this.cluster.z);
                     var6 = var3.split(var7);
                     this.nodes.add(var7);
                     this.edges.add(var6);
                  }
               }
            } else if (var3.node1.y < var3.node2.y) {
               for(var4 = this.splitXY.size() - 1; var4 >= 0; --var4) {
                  var7 = Node.alloc().init(var3.node1.x, (float)this.splitXY.get(var4), this.cluster.z);
                  var6 = var3.split(var7);
                  this.nodes.add(var7);
                  this.edges.add(var6);
               }
            } else {
               for(var4 = 0; var4 < this.splitXY.size(); ++var4) {
                  var7 = Node.alloc().init(var3.node1.x, (float)this.splitXY.get(var4), this.cluster.z);
                  var6 = var3.split(var7);
                  this.nodes.add(var7);
                  this.edges.add(var6);
               }
            }
         }
      }

   }

   void getStairSquares(ArrayList<Square> var1) {
      VehicleRect var2 = this.cluster.bounds();
      var2.x -= 4;
      var2.w += 4;
      ++var2.w;
      var2.y -= 4;
      var2.h += 4;
      ++var2.h;

      for(int var3 = var2.top(); var3 < var2.bottom(); ++var3) {
         for(int var4 = var2.left(); var4 < var2.right(); ++var4) {
            Square var5 = PolygonalMap2.instance.getSquare(var4, var3, this.cluster.z);
            if (var5 != null && var5.has(72) && !var1.contains(var5)) {
               var1.add(var5);
            }
         }
      }

      var2.release();
   }

   void getCanPathSquares(ArrayList<Square> var1) {
      VehicleRect var2 = this.cluster.bounds();
      --var2.x;
      var2.w += 2;
      --var2.y;
      var2.h += 2;

      for(int var3 = var2.top(); var3 < var2.bottom(); ++var3) {
         for(int var4 = var2.left(); var4 < var2.right(); ++var4) {
            Square var5 = PolygonalMap2.instance.getSquare(var4, var3, this.cluster.z);
            if (var5 != null && (var5.isCanPathW() || var5.isCanPathN()) && !var1.contains(var5)) {
               var1.add(var5);
            }
         }
      }

      var2.release();
   }

   void connectVehicleCrawlNodes() {
      for(int var1 = 0; var1 < this.obstacles.size(); ++var1) {
         Obstacle var2 = (Obstacle)this.obstacles.get(var1);
         if (var2.vehicle != null && var2.nodeCrawlFront != null) {
            int var3;
            Node var4;
            for(var3 = 0; var3 < var2.crawlNodes.size(); var3 += 3) {
               var4 = (Node)var2.crawlNodes.get(var3);
               Node var5 = (Node)var2.crawlNodes.get(var3 + 1);
               Node var6 = (Node)var2.crawlNodes.get(var3 + 2);
               PolygonalMap2.instance.connectTwoNodes(var4, var5);
               PolygonalMap2.instance.connectTwoNodes(var6, var5);
               if (var3 + 3 < var2.crawlNodes.size()) {
                  Node var7 = (Node)var2.crawlNodes.get(var3 + 3 + 1);
                  PolygonalMap2.instance.connectTwoNodes(var5, var7);
               }
            }

            if (!var2.crawlNodes.isEmpty()) {
               var3 = var2.crawlNodes.size() - 2;
               var4 = (Node)var2.crawlNodes.get(var3);
               PolygonalMap2.instance.connectTwoNodes(var2.nodeCrawlFront, var4);
               byte var37 = 1;
               var4 = (Node)var2.crawlNodes.get(var37);
               PolygonalMap2.instance.connectTwoNodes(var2.nodeCrawlRear, var4);
            }

            if (!var2.crawlNodes.isEmpty()) {
               ImmutableRectF var38 = var2.bounds;
               int var39 = PZMath.fastfloor(var38.left());
               int var40 = PZMath.fastfloor(var38.top());
               int var42 = (int)Math.ceil((double)var38.right());
               int var43 = (int)Math.ceil((double)var38.bottom());

               for(int var8 = var40; var8 < var43; ++var8) {
                  for(int var9 = var39; var9 < var42; ++var9) {
                     Square var10 = PolygonalMap2.instance.getSquare(var9, var8, this.cluster.z);
                     if (var10 != null && var2.isPointInside((float)var9 + 0.5F, (float)var8 + 0.5F)) {
                        Node var11 = PolygonalMap2.instance.getNodeForSquare(var10);

                        for(int var12 = var11.visible.size() - 1; var12 >= 0; --var12) {
                           Connection var13 = (Connection)var11.visible.get(var12);
                           if (var13.has(1)) {
                              Node var14 = var13.otherNode(var11);
                              Node var15 = var2.getClosestInteriorCrawlNode(var11.x, var11.y);

                              for(int var16 = 0; var16 < var2.outer.size(); ++var16) {
                                 Edge var17 = (Edge)var2.outer.get(var16);
                                 float var18 = var17.node1.x;
                                 float var19 = var17.node1.y;
                                 float var20 = var17.node2.x;
                                 float var21 = var17.node2.y;
                                 float var22 = var13.node1.x;
                                 float var23 = var13.node1.y;
                                 float var24 = var13.node2.x;
                                 float var25 = var13.node2.y;
                                 double var26 = (double)((var25 - var23) * (var20 - var18) - (var24 - var22) * (var21 - var19));
                                 if (var26 != 0.0) {
                                    double var28 = (double)((var24 - var22) * (var19 - var23) - (var25 - var23) * (var18 - var22)) / var26;
                                    double var30 = (double)((var20 - var18) * (var19 - var23) - (var21 - var19) * (var18 - var22)) / var26;
                                    if (var28 >= 0.0 && var28 <= 1.0 && var30 >= 0.0 && var30 <= 1.0) {
                                       float var32 = (float)((double)var18 + var28 * (double)(var20 - var18));
                                       float var33 = (float)((double)var19 + var28 * (double)(var21 - var19));
                                       Node var34 = Node.alloc().init(var32, var33, this.cluster.z);
                                       var34.flags |= 1;
                                       boolean var35 = var17.node1.isConnectedTo(var17.node2);
                                       Edge var36 = var17.split(var34);
                                       if (var35) {
                                          PolygonalMap2.instance.connectTwoNodes(var17.node1, var17.node2);
                                          PolygonalMap2.instance.connectTwoNodes(var36.node1, var36.node2);
                                       }

                                       this.edges.add(var36);
                                       this.nodes.add(var34);
                                       PolygonalMap2.instance.connectTwoNodes(var14, var34, var13.flags & 2 | 1);
                                       PolygonalMap2.instance.connectTwoNodes(var34, var15, 0);
                                       break;
                                    }
                                 }
                              }

                              PolygonalMap2.instance.breakConnection(var13);
                           }
                        }
                     }
                  }
               }
            }

            for(var3 = var1 + 1; var3 < this.obstacles.size(); ++var3) {
               Obstacle var41 = (Obstacle)this.obstacles.get(var3);
               if (var41.vehicle != null && var41.nodeCrawlFront != null) {
                  var2.connectCrawlNodes(this, var41);
                  var41.connectCrawlNodes(this, var2);
               }
            }
         }
      }

   }

   void checkEdgeIntersection() {
      int var1;
      Obstacle var2;
      int var3;
      for(var1 = 0; var1 < this.obstacles.size(); ++var1) {
         var2 = (Obstacle)this.obstacles.get(var1);

         for(var3 = var1 + 1; var3 < this.obstacles.size(); ++var3) {
            Obstacle var4 = (Obstacle)this.obstacles.get(var3);
            if (var2.bounds.intersects(var4.bounds)) {
               this.checkEdgeIntersection(var2.outer, var4.outer);

               int var5;
               EdgeRing var6;
               for(var5 = 0; var5 < var4.inner.size(); ++var5) {
                  var6 = (EdgeRing)var4.inner.get(var5);
                  this.checkEdgeIntersection(var2.outer, var6);
               }

               for(var5 = 0; var5 < var2.inner.size(); ++var5) {
                  var6 = (EdgeRing)var2.inner.get(var5);
                  this.checkEdgeIntersection(var6, var4.outer);

                  for(int var7 = 0; var7 < var4.inner.size(); ++var7) {
                     EdgeRing var8 = (EdgeRing)var4.inner.get(var7);
                     this.checkEdgeIntersection(var6, var8);
                  }
               }
            }
         }
      }

      for(var1 = 0; var1 < this.obstacles.size(); ++var1) {
         var2 = (Obstacle)this.obstacles.get(var1);
         this.checkEdgeIntersectionSplit(var2.outer);

         for(var3 = 0; var3 < var2.inner.size(); ++var3) {
            this.checkEdgeIntersectionSplit((EdgeRing)var2.inner.get(var3));
         }
      }

   }

   void checkEdgeIntersection(EdgeRing var1, EdgeRing var2) {
      for(int var3 = 0; var3 < var1.size(); ++var3) {
         Edge var4 = (Edge)var1.get(var3);

         for(int var5 = 0; var5 < var2.size(); ++var5) {
            Edge var6 = (Edge)var2.get(var5);
            if (this.intersects(var4.node1, var4.node2, var6)) {
               Intersection var7 = this.getIntersection(var4, var6);
               if (var7 != null) {
                  var4.intersections.add(var7);
                  var6.intersections.add(var7);
                  this.nodes.add(var7.nodeSplit);
                  this.intersectNodes.add(var7.nodeSplit);
               }
            }
         }
      }

   }

   void checkEdgeIntersectionSplit(EdgeRing var1) {
      for(int var2 = var1.size() - 1; var2 >= 0; --var2) {
         Edge var3 = (Edge)var1.get(var2);
         if (!var3.intersections.isEmpty()) {
            comparator.edge = var3;
            Collections.sort(var3.intersections, comparator);

            for(int var4 = var3.intersections.size() - 1; var4 >= 0; --var4) {
               Intersection var5 = (Intersection)var3.intersections.get(var4);
               Edge var6 = var5.split(var3);
               this.edges.add(var6);
            }
         }
      }

   }

   void checkNodesInObstacles() {
      int var1;
      Node var2;
      int var3;
      Obstacle var4;
      for(var1 = 0; var1 < this.nodes.size(); ++var1) {
         var2 = (Node)this.nodes.get(var1);

         for(var3 = 0; var3 < this.obstacles.size(); ++var3) {
            var4 = (Obstacle)this.obstacles.get(var3);
            if (var4.isNodeInsideOf(var2)) {
               var2.ignore = true;
               break;
            }
         }
      }

      for(var1 = 0; var1 < this.perimeterNodes.size(); ++var1) {
         var2 = (Node)this.perimeterNodes.get(var1);

         for(var3 = 0; var3 < this.obstacles.size(); ++var3) {
            var4 = (Obstacle)this.obstacles.get(var3);
            if (var4.isNodeInsideOf(var2)) {
               var2.ignore = true;
               break;
            }
         }
      }

   }

   void addPerimeterEdges() {
      VehicleRect var1 = this.cluster.bounds();
      --var1.x;
      --var1.y;
      var1.w += 2;
      var1.h += 2;
      ClusterOutlineGrid var2 = clusterOutlineGrid.setSize(var1.w, var1.h);
      int var3 = this.cluster.z;

      int var4;
      for(var4 = 0; var4 < this.cluster.rects.size(); ++var4) {
         VehicleRect var5 = (VehicleRect)this.cluster.rects.get(var4);
         var5 = VehicleRect.alloc().init(var5.x - 1, var5.y - 1, var5.w + 2, var5.h + 2, var5.z);

         for(int var6 = var5.top(); var6 < var5.bottom(); ++var6) {
            for(int var7 = var5.left(); var7 < var5.right(); ++var7) {
               var2.setInner(var7 - var1.left(), var6 - var1.top(), var3);
            }
         }

         var5.release();
      }

      int var12;
      ClusterOutline var13;
      for(var4 = 0; var4 < var1.h; ++var4) {
         for(var12 = 0; var12 < var1.w; ++var12) {
            var13 = var2.get(var12, var4, var3);
            if (var13.inner) {
               if (!var2.isInner(var12 - 1, var4, var3)) {
                  var13.w = true;
               }

               if (!var2.isInner(var12, var4 - 1, var3)) {
                  var13.n = true;
               }

               if (!var2.isInner(var12 + 1, var4, var3)) {
                  var13.e = true;
               }

               if (!var2.isInner(var12, var4 + 1, var3)) {
                  var13.s = true;
               }
            }
         }
      }

      for(var4 = 0; var4 < var1.h; ++var4) {
         for(var12 = 0; var12 < var1.w; ++var12) {
            var13 = var2.get(var12, var4, var3);
            if (var13 != null && (var13.w || var13.n || var13.e || var13.s || var13.innerCorner)) {
               Square var14 = PolygonalMap2.instance.getSquare(var1.x + var12, var1.y + var4, var3);
               if (var14 != null && !var14.isNonThumpableSolid() && !var14.has(504)) {
                  Node var8 = PolygonalMap2.instance.getNodeForSquare(var14);
                  var8.flags |= 8;
                  var8.addGraph(this);
                  this.perimeterNodes.add(var8);
               }
            }

            if (var13 != null && var13.n && var13.w && var13.inner && !(var13.tw | var13.tn | var13.te | var13.ts)) {
               ArrayList var15 = var2.trace(var13);
               if (!var15.isEmpty()) {
                  for(int var16 = 0; var16 < var15.size() - 1; ++var16) {
                     Node var9 = (Node)var15.get(var16);
                     Node var10 = (Node)var15.get(var16 + 1);
                     var9.x += (float)var1.left();
                     var9.y += (float)var1.top();
                     Edge var11 = Edge.alloc().init(var9, var10, (Obstacle)null, (EdgeRing)null);
                     this.perimeterEdges.add(var11);
                  }

                  if (var15.get(var15.size() - 1) != var15.get(0)) {
                     Node var10000 = (Node)var15.get(var15.size() - 1);
                     var10000.x += (float)var1.left();
                     var10000 = (Node)var15.get(var15.size() - 1);
                     var10000.y += (float)var1.top();
                  }
               }
            }
         }
      }

      var2.releaseElements();
      var1.release();
   }

   void calculateNodeVisibility() {
      ArrayList var1 = new ArrayList();
      var1.addAll(this.nodes);
      var1.addAll(this.perimeterNodes);

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         Node var3 = (Node)var1.get(var2);
         if (!var3.ignore && (var3.square == null || !var3.square.has(504))) {
            for(int var4 = var2 + 1; var4 < var1.size(); ++var4) {
               Node var5 = (Node)var1.get(var4);
               if (!var5.ignore && (var5.square == null || !var5.square.has(504)) && (!var3.hasFlag(8) || !var5.hasFlag(8))) {
                  if (var3.isConnectedTo(var5)) {
                     assert var3.square != null && (var3.square.isCanPathW() || var3.square.isCanPathN()) || var5.square != null && (var5.square.isCanPathW() || var5.square.isCanPathN());
                  } else if (this.isVisible(var3, var5)) {
                     PolygonalMap2.instance.connectTwoNodes(var3, var5);
                  }
               }
            }
         }
      }

   }

   public void addNode(Node var1) {
      if (this.created && !var1.ignore) {
         ArrayList var2 = new ArrayList();
         var2.addAll(this.nodes);
         var2.addAll(this.perimeterNodes);

         for(int var3 = 0; var3 < var2.size(); ++var3) {
            Node var4 = (Node)var2.get(var3);
            if (!var4.ignore && this.isVisible(var4, var1)) {
               PolygonalMap2.instance.connectTwoNodes(var1, var4);
            }
         }
      }

      this.nodes.add(var1);
   }

   public void removeNode(Node var1) {
      this.nodes.remove(var1);

      for(int var2 = var1.visible.size() - 1; var2 >= 0; --var2) {
         Connection var3 = (Connection)var1.visible.get(var2);
         PolygonalMap2.instance.breakConnection(var3);
      }

   }

   boolean contains(float var1, float var2, int var3) {
      for(int var4 = 0; var4 < this.cluster.rects.size(); ++var4) {
         VehicleRect var5 = (VehicleRect)this.cluster.rects.get(var4);
         if (var5.containsPoint(var1, var2, (float)var3)) {
            return true;
         }
      }

      return false;
   }

   boolean contains(float var1, float var2, int var3, int var4) {
      for(int var5 = 0; var5 < this.cluster.rects.size(); ++var5) {
         VehicleRect var6 = (VehicleRect)this.cluster.rects.get(var5);
         if (var6.containsPoint(var1, var2, (float)var3, var4)) {
            return true;
         }
      }

      return false;
   }

   public boolean contains(Square var1) {
      for(int var2 = 0; var2 < this.cluster.rects.size(); ++var2) {
         VehicleRect var3 = (VehicleRect)this.cluster.rects.get(var2);
         if (var3.containsPoint((float)var1.x + 0.5F, (float)var1.y + 0.5F, (float)var1.z)) {
            return true;
         }
      }

      return false;
   }

   public boolean contains(Square var1, int var2) {
      for(int var3 = 0; var3 < this.cluster.rects.size(); ++var3) {
         VehicleRect var4 = (VehicleRect)this.cluster.rects.get(var3);
         if (var4.containsPoint((float)var1.x + 0.5F, (float)var1.y + 0.5F, (float)var1.z, var2)) {
            return true;
         }
      }

      return false;
   }

   public boolean intersects(int var1, int var2, int var3, int var4, int var5) {
      for(int var6 = 0; var6 < this.cluster.rects.size(); ++var6) {
         VehicleRect var7 = (VehicleRect)this.cluster.rects.get(var6);
         if (var7.intersects(var1, var2, var3, var4, var5)) {
            return true;
         }
      }

      return false;
   }

   public int getPointOutsideObstacles(float var1, float var2, float var3, AdjustStartEndNodeData var4) {
      ClosestPointOnEdge var5 = PolygonalMap2.instance.closestPointOnEdge;
      double var6 = 1.7976931348623157E308;
      Edge var8 = null;
      Node var9 = null;
      float var10 = 0.0F;
      float var11 = 0.0F;

      for(int var12 = 0; var12 < this.obstacles.size(); ++var12) {
         Obstacle var13 = (Obstacle)this.obstacles.get(var12);
         if (var13.bounds.containsPoint(var1, var2) && var13.isPointInside(var1, var2)) {
            var13.getClosestPointOnEdge(var1, var2, var5);
            if (var5.edge != null && var5.distSq < var6) {
               var6 = var5.distSq;
               var8 = var5.edge;
               var9 = var5.node;
               var10 = var5.point.x;
               var11 = var5.point.y;
            }
         }
      }

      if (var8 != null) {
         var5.edge = var8;
         var5.node = var9;
         var5.point.set(var10, var11);
         var5.distSq = var6;
         if (var8.obstacle.splitEdgeAtNearestPoint(var5, PZMath.fastfloor(var3), var4)) {
            var4.graph = this;
            if (var4.isNodeNew) {
               this.edges.add(var4.newEdge);
               this.addNode(var4.node);
            }

            return 1;
         } else {
            return -1;
         }
      } else {
         return 0;
      }
   }

   Node getClosestNodeTo(float var1, float var2) {
      Node var3 = null;
      float var4 = 3.4028235E38F;

      for(int var5 = 0; var5 < this.nodes.size(); ++var5) {
         Node var6 = (Node)this.nodes.get(var5);
         float var7 = IsoUtils.DistanceToSquared(var6.x, var6.y, var1, var2);
         if (var7 < var4) {
            var3 = var6;
            var4 = var7;
         }
      }

      return var3;
   }

   public void create() {
      int var1;
      for(var1 = 0; var1 < this.cluster.rects.size(); ++var1) {
         VehicleRect var2 = (VehicleRect)this.cluster.rects.get(var1);
         this.addEdgesForVehicle(var2.vehicle);
      }

      this.addWorldObstaclesClipper();

      for(var1 = 0; var1 < this.obstacles.size(); ++var1) {
         Obstacle var4 = (Obstacle)this.obstacles.get(var1);
         if (var4.vehicle == null) {
            this.splitWorldObstacleEdges(var4.outer);

            for(int var3 = 0; var3 < var4.inner.size(); ++var3) {
               this.splitWorldObstacleEdges((EdgeRing)var4.inner.get(var3));
            }
         }
      }

      this.checkEdgeIntersection();
      this.checkNodesInObstacles();
      this.calculateNodeVisibility();
      this.connectVehicleCrawlNodes();
      this.created = true;
   }

   void initOverlappedChunks() {
      this.clearOverlappedChunks();
      VehicleRect var1 = this.cluster.bounds();
      byte var2 = 1;
      int var3 = PZMath.coorddivision(var1.left() - var2, 8);
      int var4 = PZMath.coorddivision(var1.top() - var2, 8);
      int var5 = (int)PZMath.ceil((float)(var1.right() + var2 - 1) / 8.0F);
      int var6 = (int)PZMath.ceil((float)(var1.bottom() + var2 - 1) / 8.0F);

      for(int var7 = var4; var7 < var6; ++var7) {
         for(int var8 = var3; var8 < var5; ++var8) {
            Chunk var9 = PolygonalMap2.instance.getChunkFromChunkPos(var8, var7);
            if (var9 != null && this.intersects(var9.getMinX(), var9.getMinY(), var9.getMaxX() + 1, var9.getMaxY() + 1, var2)) {
               this.overlappedChunks.add(var9);
               var9.addVisibilityGraph(this);
            }
         }
      }

      var1.release();
   }

   void clearOverlappedChunks() {
      for(int var1 = 0; var1 < this.overlappedChunks.size(); ++var1) {
         Chunk var2 = (Chunk)this.overlappedChunks.get(var1);
         var2.removeVisibilityGraph(this);
      }

      this.overlappedChunks.clear();
   }

   static VisibilityGraph alloc() {
      return pool.isEmpty() ? new VisibilityGraph() : (VisibilityGraph)pool.pop();
   }

   void release() {
      int var1;
      for(var1 = 0; var1 < this.nodes.size(); ++var1) {
         if (!PolygonalMap2.instance.squareToNode.containsValue(this.nodes.get(var1))) {
            ((Node)this.nodes.get(var1)).release();
         }
      }

      for(var1 = 0; var1 < this.perimeterEdges.size(); ++var1) {
         ((Edge)this.perimeterEdges.get(var1)).node1.release();
         ((Edge)this.perimeterEdges.get(var1)).release();
      }

      for(var1 = 0; var1 < this.obstacles.size(); ++var1) {
         Obstacle var2 = (Obstacle)this.obstacles.get(var1);
         var2.release();
      }

      for(var1 = 0; var1 < this.cluster.rects.size(); ++var1) {
         ((VehicleRect)this.cluster.rects.get(var1)).release();
      }

      this.cluster.release();
      this.clearOverlappedChunks();

      assert !pool.contains(this);

      pool.push(this);
   }

   void render() {
      int var1 = this.cluster.z - 32;
      float var2 = 1.0F;

      Iterator var3;
      for(var3 = this.perimeterEdges.iterator(); var3.hasNext(); var2 = 1.0F - var2) {
         Edge var4 = (Edge)var3.next();
         LineDrawer.addLine(var4.node1.x, var4.node1.y, (float)var1, var4.node2.x, var4.node2.y, (float)var1, var2, 0.5F, 0.5F, (String)null, true);
      }

      var3 = this.obstacles.iterator();

      while(true) {
         Iterator var5;
         Iterator var7;
         Obstacle var10;
         do {
            if (!var3.hasNext()) {
               var3 = this.perimeterNodes.iterator();

               Node var11;
               Connection var15;
               Node var17;
               while(var3.hasNext()) {
                  var11 = (Node)var3.next();
                  if (DebugOptions.instance.PolymapRenderConnections.getValue()) {
                     var5 = var11.visible.iterator();

                     while(var5.hasNext()) {
                        var15 = (Connection)var5.next();
                        var17 = var15.otherNode(var11);
                        LineDrawer.addLine(var11.x, var11.y, (float)var1, var17.x, var17.y, (float)var1, 0.0F, 0.25F, 0.0F, (String)null, true);
                     }
                  }

                  if (DebugOptions.instance.PolymapRenderNodes.getValue()) {
                     float var13 = 1.0F;
                     float var16 = 0.5F;
                     float var18 = 0.0F;
                     if (var11.ignore) {
                        var16 = 1.0F;
                     }

                     LineDrawer.addLine(var11.x - 0.05F, var11.y - 0.05F, (float)var1, var11.x + 0.05F, var11.y + 0.05F, (float)var1, var13, var16, var18, (String)null, false);
                  }
               }

               var3 = this.nodes.iterator();

               while(true) {
                  do {
                     if (!var3.hasNext()) {
                        var3 = this.intersectNodes.iterator();

                        while(var3.hasNext()) {
                           var11 = (Node)var3.next();
                           LineDrawer.addLine(var11.x - 0.1F, var11.y - 0.1F, (float)var1, var11.x + 0.1F, var11.y + 0.1F, (float)var1, 1.0F, 0.0F, 0.0F, (String)null, false);
                        }

                        return;
                     }

                     var11 = (Node)var3.next();
                     if (DebugOptions.instance.PolymapRenderConnections.getValue()) {
                        var5 = var11.visible.iterator();

                        while(var5.hasNext()) {
                           var15 = (Connection)var5.next();
                           var17 = var15.otherNode(var11);
                           if (this.nodes.contains(var17)) {
                              LineDrawer.addLine(var11.x, var11.y, (float)var1, var17.x, var17.y, (float)var1, 0.0F, 1.0F, 0.0F, (String)null, true);
                           }
                        }
                     }
                  } while(!DebugOptions.instance.PolymapRenderNodes.getValue() && !var11.ignore);

                  LineDrawer.addLine(var11.x - 0.05F, var11.y - 0.05F, (float)var1, var11.x + 0.05F, var11.y + 0.05F, (float)var1, 1.0F, 1.0F, 0.0F, (String)null, false);
               }
            }

            var10 = (Obstacle)var3.next();
            var2 = 1.0F;

            for(var5 = var10.outer.iterator(); var5.hasNext(); var2 = 1.0F - var2) {
               Edge var6 = (Edge)var5.next();
               LineDrawer.addLine(var6.node1.x, var6.node1.y, (float)var1, var6.node2.x, var6.node2.y, (float)var1, var2, 0.5F, 0.5F, (String)null, true);
            }

            var5 = var10.inner.iterator();

            while(var5.hasNext()) {
               EdgeRing var12 = (EdgeRing)var5.next();

               for(var7 = var12.iterator(); var7.hasNext(); var2 = 1.0F - var2) {
                  Edge var8 = (Edge)var7.next();
                  LineDrawer.addLine(var8.node1.x, var8.node1.y, (float)var1, var8.node2.x, var8.node2.y, (float)var1, var2, 0.5F, 0.5F, (String)null, true);
               }
            }
         } while(!DebugOptions.instance.PolymapRenderCrawling.getValue());

         var5 = var10.crawlNodes.iterator();

         while(var5.hasNext()) {
            Node var14 = (Node)var5.next();
            LineDrawer.addLine(var14.x - 0.05F, var14.y - 0.05F, (float)var1, var14.x + 0.05F, var14.y + 0.05F, (float)var1, 0.5F, 1.0F, 0.5F, (String)null, false);
            var7 = var14.visible.iterator();

            while(var7.hasNext()) {
               Connection var19 = (Connection)var7.next();
               Node var9 = var19.otherNode(var14);
               if (var9.hasFlag(1)) {
                  LineDrawer.addLine(var14.x, var14.y, (float)var1, var9.x, var9.y, (float)var1, 0.5F, 1.0F, 0.5F, (String)null, true);
               }
            }
         }
      }
   }

   static final class CompareIntersection implements Comparator<Intersection> {
      Edge edge;

      CompareIntersection() {
      }

      public int compare(Intersection var1, Intersection var2) {
         float var3 = this.edge == var1.edge1 ? var1.dist1 : var1.dist2;
         float var4 = this.edge == var2.edge1 ? var2.dist1 : var2.dist2;
         if (var3 < var4) {
            return -1;
         } else {
            return var3 > var4 ? 1 : 0;
         }
      }
   }
}
